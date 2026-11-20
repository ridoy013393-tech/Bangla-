package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object ActiveAppTracker {
    @Volatile
    var isAppInForeground: Boolean = false

    @Volatile
    var activeChatId: String? = null

    @Volatile
    var activeCallId: String? = null
}

object CallRingtonePlayer {
    private const val TAG = "CallRingtonePlayer"
    private const val MAX_RING_DURATION_MS = 60_000L

    private val lock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var fallbackRingtone: Ringtone? = null
    private var activeVibrator: Vibrator? = null
    private var currentCallId: String? = null

    private val autoStopRunnable = Runnable {
        stopRinging()
    }

    fun startRinging(context: Context, callId: String) {
        synchronized(lock) {
            if (currentCallId == callId && (mediaPlayer?.isPlaying == true || fallbackRingtone?.isPlaying == true)) {
                return
            }
            stopLocked()
            currentCallId = callId

            val appContext = context.applicationContext
            val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL

            if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
                return
            }

            // Start rhythmic vibration for both NORMAL and VIBRATE modes
            startVibrationLocked(appContext)

            if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                if (ringtoneUri != null) {
                    val startedWithMediaPlayer = try {
                        val player = MediaPlayer().apply {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                            )
                            setDataSource(appContext, ringtoneUri)
                            isLooping = true
                            prepare()
                            start()
                        }
                        mediaPlayer = player
                        true
                    } catch (e: Exception) {
                        Log.w(TAG, "MediaPlayer ringtone fallback: ${e.message}")
                        false
                    }

                    if (!startedWithMediaPlayer) {
                        runCatching {
                            val ringtone = RingtoneManager.getRingtone(appContext, ringtoneUri)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                ringtone?.isLooping = true
                            }
                            ringtone?.play()
                            fallbackRingtone = ringtone
                        }.onFailure { err ->
                            Log.w(TAG, "Fallback ringtone failed: ${err.message}")
                        }
                    }
                }
            }

            mainHandler.removeCallbacks(autoStopRunnable)
            mainHandler.postDelayed(autoStopRunnable, MAX_RING_DURATION_MS)
        }
    }

    fun stopRinging(callId: String? = null) {
        synchronized(lock) {
            if (callId != null && currentCallId != null && currentCallId != callId) {
                return
            }
            stopLocked()
        }
    }

    private fun startVibrationLocked(context: Context) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            activeVibrator = vibrator
            if (vibrator?.hasVibrator() == true) {
                val pattern = longArrayOf(0L, 700L, 600L)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, 0)
                }
            }
        }.onFailure { err ->
            Log.w(TAG, "Call vibration unavailable: ${err.message}")
        }
    }

    private fun stopLocked() {
        mainHandler.removeCallbacks(autoStopRunnable)
        currentCallId = null
        runCatching {
            mediaPlayer?.let { player ->
                if (player.isPlaying) player.stop()
                player.reset()
                player.release()
            }
        }
        mediaPlayer = null

        runCatching {
            fallbackRingtone?.stop()
        }
        fallbackRingtone = null

        runCatching {
            activeVibrator?.cancel()
        }
        activeVibrator = null
    }
}

/**
 * WhatsApp-style outgoing ringback tone player.
 * Strictly ONLY started when the callee's device acknowledges the call (`status == "ringing"`).
 * While the peer is offline (`status == "calling"`), no ringback tone plays.
 */
object CallRingbackPlayer {
    private const val TAG = "CallRingbackPlayer"
    private const val RING_BURST_MS = 1_200L
    private const val RING_PAUSE_MS = 2_600L

    private val lock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var toneGenerator: ToneGenerator? = null
    private var activeSessionId: String? = null
    private var isRunning: Boolean = false

    private val pulseRunnable = object : Runnable {
        override fun run() {
            synchronized(lock) {
                if (!isRunning) return
                runCatching {
                    toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, RING_BURST_MS.toInt())
                }.onFailure { err ->
                    Log.w(TAG, "Ringback tone pulse failed: ${err.message}")
                }
                mainHandler.postDelayed(this, RING_BURST_MS + RING_PAUSE_MS)
            }
        }
    }

    fun startRingback(sessionId: String) {
        synchronized(lock) {
            if (isRunning && activeSessionId == sessionId) return
            stopLocked()
            activeSessionId = sessionId
            isRunning = true
            runCatching {
                toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 55)
            }.onFailure { err ->
                Log.w(TAG, "Unable to initialize ringback ToneGenerator: ${err.message}")
                isRunning = false
                activeSessionId = null
                return
            }
            mainHandler.post(pulseRunnable)
        }
    }

    fun stopRingback(sessionId: String? = null) {
        synchronized(lock) {
            if (sessionId != null && activeSessionId != null && activeSessionId != sessionId) {
                return
            }
            stopLocked()
        }
    }

    private fun stopLocked() {
        isRunning = false
        activeSessionId = null
        mainHandler.removeCallbacks(pulseRunnable)
        runCatching {
            toneGenerator?.stopTone()
            toneGenerator?.release()
        }
        toneGenerator = null
    }
}

object NotificationHelper {
    private const val TAG = "NotificationHelper"

    const val EXTRA_CHAT_ID = "extra_open_chat_id"
    const val EXTRA_INCOMING_CALL_ID = "extra_incoming_call_id"
    const val EXTRA_ACCEPT_CALL_ID = "extra_accept_call_id"

    const val FOREGROUND_SYNC_NOTIFICATION_ID = 41001
    private const val CALL_NOTIFICATION_BASE_ID = 52000

    fun createAllChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. High-importance message notifications with audible default alert tone + vibration
        val messageSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val messageAudioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val messageChannel = NotificationChannel(
            context.getString(R.string.notification_channel_messages_id),
            context.getString(R.string.notification_channel_messages_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_messages_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 220L, 120L, 220L)
            enableLights(true)
            setSound(messageSoundUri, messageAudioAttrs)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        manager.createNotificationChannel(messageChannel)

        // 2. High-importance incoming call heads-up channel (ringtone audio handled continuously by CallRingtonePlayer)
        val callChannel = NotificationChannel(
            context.getString(R.string.notification_channel_calls_id),
            context.getString(R.string.notification_channel_calls_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_calls_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 600L, 400L)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(callChannel)

        // 3. Quiet ongoing channel for background realtime sync listener
        val syncChannel = NotificationChannel(
            context.getString(R.string.notification_channel_sync_id),
            context.getString(R.string.notification_channel_sync_name),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = context.getString(R.string.notification_channel_sync_desc)
            enableVibration(false)
            setSound(null, null)
            setShowBadge(false)
        }
        manager.createNotificationChannel(syncChannel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun buildRealtimeSyncNotification(context: Context): Notification {
        createAllChannels(context)
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            FOREGROUND_SYNC_NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, context.getString(R.string.notification_channel_sync_id))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notification_sync_title))
            .setContentText(context.getString(R.string.notification_sync_body))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun showMessageNotification(
        context: Context,
        chatId: String,
        senderName: String,
        body: String
    ) {
        createAllChannels(context)
        if (!hasNotificationPermission(context)) {
            Log.d(TAG, "POST_NOTIFICATIONS not granted; skipping message notification")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (chatId.isNotBlank()) {
                putExtra(EXTRA_CHAT_ID, chatId)
            }
        }
        val notificationId = chatId.ifBlank { senderName }.hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notification = NotificationCompat.Builder(
            context,
            context.getString(R.string.notification_channel_messages_id)
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(senderName.ifBlank { context.getString(R.string.notification_sender_default) })
            .setContentText(body.ifBlank { context.getString(R.string.notification_new_message_hidden) })
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(defaultSound)
            .setVibrate(longArrayOf(0L, 220L, 120L, 220L))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }.onFailure { err ->
            Log.w(TAG, "Failed to post message notification: ${err.message}")
        }
    }

    fun cancelChatNotification(context: Context, chatId: String) {
        if (chatId.isBlank()) return
        runCatching {
            NotificationManagerCompat.from(context).cancel(chatId.hashCode())
        }
    }

    fun showIncomingCallNotification(
        context: Context,
        callId: String,
        callerName: String,
        callType: String
    ) {
        createAllChannels(context)
        if (!hasNotificationPermission(context)) {
            Log.d(TAG, "POST_NOTIFICATIONS not granted; skipping call notification")
            return
        }

        val notificationId = callNotificationId(callId)
        val subtitle = if (callType == "video") {
            context.getString(R.string.notification_incoming_video_call)
        } else {
            context.getString(R.string.notification_incoming_voice_call)
        }

        val openCallIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(EXTRA_INCOMING_CALL_ID, callId)
        }
        val openCallPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openCallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val acceptIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(EXTRA_ACCEPT_CALL_ID, callId)
        }
        val acceptPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 1,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declineIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_DECLINE_CALL
            putExtra(CallActionReceiver.EXTRA_CALL_ID, callId)
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            context.getString(R.string.notification_channel_calls_id)
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(callerName.ifBlank { context.getString(R.string.notification_sender_default) })
            .setContentText(subtitle)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setTimeoutAfter(60_000L)
            .setContentIntent(openCallPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_call,
                context.getString(R.string.notification_action_accept_call),
                acceptPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.notification_action_decline_call),
                declinePendingIntent
            )
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }.onFailure { err ->
            Log.w(TAG, "Failed to post incoming call notification: ${err.message}")
        }
    }

    fun cancelIncomingCallNotification(context: Context, callId: String) {
        if (callId.isBlank()) return
        CallRingtonePlayer.stopRinging(callId)
        runCatching {
            NotificationManagerCompat.from(context).cancel(callNotificationId(callId))
        }
    }

    private fun callNotificationId(callId: String): Int =
        CALL_NOTIFICATION_BASE_ID + (callId.hashCode() and 0x0FFF)
}
