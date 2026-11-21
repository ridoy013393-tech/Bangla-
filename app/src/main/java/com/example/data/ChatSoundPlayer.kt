package com.example.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Lightweight PCM chime synthesizer for in-chat Send & Receive sound effects.
 * Zero extra binary assets or permissions needed; automatically respects device silent/vibrate mode.
 */
object ChatSoundPlayer {
    private const val TAG = "ChatSoundPlayer"
    private const val SAMPLE_RATE = 22_050
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun playSendSound(context: Context, enabled: Boolean) {
        if (!enabled) return
        if (!isRingerNormal(context)) return
        scope.launch {
            playTwoToneChime(
                freq1Hz = 540.0,
                freq2Hz = 810.0,
                firstMs = 45,
                secondMs = 65,
                volume = 0.24
            )
        }
    }

    fun playReceiveSound(context: Context, enabled: Boolean) {
        if (!enabled) return
        if (!isRingerNormal(context)) return
        scope.launch {
            playTwoToneChime(
                freq1Hz = 660.0,
                freq2Hz = 990.0,
                firstMs = 55,
                secondMs = 90,
                volume = 0.28
            )
        }
    }

    private fun isRingerNormal(context: Context): Boolean {
        return runCatching {
            val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            (audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL) == AudioManager.RINGER_MODE_NORMAL
        }.getOrDefault(false)
    }

    private fun playTwoToneChime(
        freq1Hz: Double,
        freq2Hz: Double,
        firstMs: Int,
        secondMs: Int,
        volume: Double
    ) {
        runCatching {
            val samples1 = (SAMPLE_RATE * firstMs) / 1000
            val samples2 = (SAMPLE_RATE * secondMs) / 1000
            val totalSamples = samples1 + samples2
            val buffer = ShortArray(totalSamples)

            for (i in 0 until samples1) {
                val env = envelope(i, samples1)
                val sample = sin(2.0 * PI * freq1Hz * i / SAMPLE_RATE) * volume * env
                buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            for (i in 0 until samples2) {
                val env = envelope(i, samples2)
                val sample = sin(2.0 * PI * freq2Hz * i / SAMPLE_RATE) * volume * env
                buffer[samples1 + i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val byteCount = buffer.size * 2
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(byteCount)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep((firstMs + secondMs + 40).toLong())
            track.stop()
            track.release()
        }.onFailure { err ->
            Log.d(TAG, "Chat sound skipped: ${err.message}")
        }
    }

    private fun envelope(index: Int, length: Int): Double {
        if (length <= 0) return 0.0
        val attack = (length * 0.14).coerceAtLeast(1.0)
        val release = (length * 0.45).coerceAtLeast(1.0)
        return when {
            index < attack -> index / attack
            index > length - release -> ((length - index).toDouble() / release).coerceIn(0.0, 1.0)
            else -> 1.0
        }
    }
}
