package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.model.CallSession
import com.example.model.Chat
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class BanglaChatRealtimeService : Service() {

    private var listeningUid: String? = null
    private var chatsRegistration: ListenerRegistration? = null
    private var callsRegistration: ListenerRegistration? = null
    private var userPrefsRegistration: ListenerRegistration? = null

    private var messagePreviewEnabled: Boolean = true
    private var chatsBaselineLoaded: Boolean = false
    private val lastNotifiedMessageKey = mutableMapOf<String, String>()
    private val activeRingingCallIds = mutableSetOf<String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createAllChannels(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAndCleanup()
            return START_NOT_STICKY
        }

        val uid = Firebase.auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            stopAndCleanup()
            return START_NOT_STICKY
        }

        val notification = NotificationHelper.buildRealtimeSyncNotification(applicationContext)
        val startedForeground = try {
            val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
            ServiceCompat.startForeground(
                this,
                NotificationHelper.FOREGROUND_SYNC_NOTIFICATION_ID,
                notification,
                fgsType
            )
            true
        } catch (e: Exception) {
            Log.w(TAG, "Unable to enter foreground mode cleanly: ${e.message}")
            false
        }

        if (!startedForeground) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (listeningUid != uid) {
            attachListenersForUser(uid)
        }
        return START_STICKY
    }

    // Android 15+ (API 35+) requires dataSync foreground services to stop on timeout
    override fun onTimeout(startId: Int, fgsType: Int) {
        Log.i(TAG, "Foreground dataSync timeout reached; stopping cleanly")
        stopAndCleanup()
    }

    override fun onDestroy() {
        detachListeners()
        super.onDestroy()
    }

    private fun stopAndCleanup() {
        detachListeners()
        stopSelf()
    }

    private fun attachListenersForUser(uid: String) {
        detachListeners()
        listeningUid = uid
        chatsBaselineLoaded = false
        lastNotifiedMessageKey.clear()
        activeRingingCallIds.clear()

        val db = FirebaseFirestore.getInstance(getString(R.string.firestore_database_id))

        // 1. Track user's private notification preview preference
        userPrefsRegistration = db.collection("users").document(uid)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    if (Firebase.auth.currentUser?.uid == uid) {
                        handleFirestoreError(error, OperationType.GET, "users/$uid")
                    }
                    return@addSnapshotListener
                }
                if (snap != null && snap.exists()) {
                    messagePreviewEnabled = snap.getBoolean("messagePreviewEnabled") ?: true
                }
            }

        // 2. Listen for new incoming messages in user's chats
        chatsRegistration = db.collection("chats")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (Firebase.auth.currentUser?.uid == uid) {
                        handleFirestoreError(error, OperationType.LIST, "chats")
                    }
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents ?: return@addSnapshotListener
                val chats = docs.mapNotNull { doc ->
                    runCatching {
                        doc.toObject(Chat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.getOrNull()
                }

                // Acknowledge WhatsApp-style delivery receipts as soon as this device receives chats online
                for (chat in chats) {
                    if (chat.lastSenderId.isNotBlank() && chat.lastSenderId != uid && !chat.lastMessageDelivered) {
                        acknowledgeChatDelivered(db, chat.id, uid)
                    }
                }

                if (!chatsBaselineLoaded) {
                    chats.forEach { chat ->
                        lastNotifiedMessageKey[chat.id] = buildMessageDedupKey(chat)
                    }
                    chatsBaselineLoaded = true
                    return@addSnapshotListener
                }

                for (chat in chats) {
                    val newKey = buildMessageDedupKey(chat)
                    val previousKey = lastNotifiedMessageKey[chat.id]
                    lastNotifiedMessageKey[chat.id] = newKey

                    if (newKey == previousKey) continue
                    if (chat.lastSenderId.isBlank() || chat.lastSenderId == uid) continue
                    if (chat.isMutedFor(uid)) continue

                    // Suppress alert only when the user is actively viewing this exact chat in foreground
                    val isViewingThisChat = ActiveAppTracker.isAppInForeground &&
                        ActiveAppTracker.activeChatId == chat.id
                    if (isViewingThisChat) continue

                    val senderName = if (chat.isGroup) {
                        val memberName = chat.participantNames[chat.lastSenderId].orEmpty()
                        val groupTitle = chat.groupName.ifBlank { getString(R.string.notification_sender_default) }
                        if (memberName.isNotBlank()) "$groupTitle • $memberName" else groupTitle
                    } else {
                        chat.participantNames[chat.lastSenderId]
                            ?.takeIf { it.isNotBlank() }
                            ?: getString(R.string.notification_sender_default)
                    }

                    val bodyText = if (messagePreviewEnabled) {
                        when {
                            chat.lastMessage.isNotBlank() -> chat.lastMessage
                            chat.lastMessageType == "image" -> "📷 ছবি পাঠানো হয়েছে"
                            chat.lastMessageType == "audio" -> "🎤 ভয়েস মেসেজ"
                            chat.lastMessageType == "document" -> "📎 ডকুমেন্ট পাঠানো হয়েছে"
                            chat.lastMessageType == "poll" -> "📊 নতুন পোল"
                            else -> getString(R.string.notification_new_message_hidden)
                        }
                    } else {
                        getString(R.string.notification_new_message_hidden)
                    }

                    NotificationHelper.showMessageNotification(
                        context = applicationContext,
                        chatId = chat.id,
                        senderName = senderName,
                        body = bodyText
                    )
                }
            }

        // 3. Listen for incoming voice & video calls in real time
        callsRegistration = db.collection("callSessions")
            .whereArrayContains("participants", uid)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (Firebase.auth.currentUser?.uid == uid) {
                        handleFirestoreError(error, OperationType.LIST, "callSessions")
                    }
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents ?: return@addSnapshotListener
                val cutoffMs = System.currentTimeMillis() - 90_000L
                val ringingSessions = docs.mapNotNull { doc ->
                    runCatching {
                        doc.toObject(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.getOrNull()
                }.filter { session ->
                    session.status in setOf("calling", "ringing") &&
                        session.calleeUid == uid &&
                        (session.createdAt?.toDate()?.time ?: System.currentTimeMillis()) >= cutoffMs
                }

                val currentRingingIds = ringingSessions.map { it.id }.toSet()

                // Stop ringing & cancel notification for any call that ended, was declined, or was answered
                val endedIds = activeRingingCallIds.filter { it !in currentRingingIds }
                for (endedId in endedIds) {
                    activeRingingCallIds.remove(endedId)
                    NotificationHelper.cancelIncomingCallNotification(applicationContext, endedId)
                }

                // Acknowledge delivery ('calling' -> 'ringing') or auto-mark 'busy' if already in another call
                for (session in ringingSessions) {
                    val currentActiveCall = ActiveAppTracker.activeCallId
                    if (currentActiveCall != null && currentActiveCall != session.id) {
                        db.collection("callSessions").document(session.id).update(
                            mapOf(
                                "status" to "busy",
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        continue
                    }
                    if (session.status == "calling") {
                        db.collection("callSessions").document(session.id).update(
                            mapOf(
                                "status" to "ringing",
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    if (currentActiveCall == session.id) continue
                    val isNewRing = activeRingingCallIds.add(session.id)
                    if (isNewRing) {
                        CallRingtonePlayer.startRinging(applicationContext, session.id)
                        NotificationHelper.showIncomingCallNotification(
                            context = applicationContext,
                            callId = session.id,
                            callerName = session.callerName,
                            callType = session.callType
                        )
                    }
                }
            }
    }

    private fun acknowledgeChatDelivered(db: FirebaseFirestore, chatId: String, currentUid: String) {
        db.collection("chats").document(chatId).update(
            mapOf(
                "lastMessageDelivered" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        db.collection("chats").document(chatId)
            .collection("messages")
            .whereArrayContains("participants", currentUid)
            .whereEqualTo("delivered", false)
            .limit(15)
            .get()
            .addOnSuccessListener { snap ->
                val toDeliver = snap.documents.filter { it.getString("senderId") != currentUid }
                if (toDeliver.isEmpty()) return@addOnSuccessListener
                val batch = db.batch()
                for (doc in toDeliver) {
                    batch.update(
                        doc.reference,
                        mapOf(
                            "delivered" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit()
            }
    }

    private fun buildMessageDedupKey(chat: Chat): String {
        val timeMs = chat.lastMessageTime?.toDate()?.time ?: 0L
        return "${chat.lastSenderId}|$timeMs|${chat.lastMessageType}|${chat.lastMessage.take(64)}"
    }

    private fun detachListeners() {
        chatsRegistration?.remove()
        chatsRegistration = null
        callsRegistration?.remove()
        callsRegistration = null
        userPrefsRegistration?.remove()
        userPrefsRegistration = null

        for (callId in activeRingingCallIds.toList()) {
            NotificationHelper.cancelIncomingCallNotification(applicationContext, callId)
        }
        activeRingingCallIds.clear()
        listeningUid = null
        chatsBaselineLoaded = false
    }

    companion object {
        private const val TAG = "BanglaChatRealtimeSvc"
        private const val ACTION_STOP = "com.example.action.STOP_REALTIME_SYNC"

        fun start(context: Context) {
            if (Firebase.auth.currentUser == null) return
            val appContext = context.applicationContext
            NotificationHelper.createAllChannels(appContext)
            val intent = Intent(appContext, BanglaChatRealtimeService::class.java)
            runCatching {
                ContextCompat.startForegroundService(appContext, intent)
            }.onFailure { err ->
                Log.w(TAG, "Deferred starting realtime service: ${err.message}")
            }
        }

        fun stop(context: Context) {
            val appContext = context.applicationContext
            CallRingtonePlayer.stopRinging()
            runCatching {
                appContext.stopService(Intent(appContext, BanglaChatRealtimeService::class.java))
            }
        }
    }
}
