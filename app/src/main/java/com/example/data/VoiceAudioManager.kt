package com.example.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class VoiceRecordingResult(
    val file: File,
    val durationSec: Int
)

class VoiceNoteRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTimeMs: Long = 0L

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private var meterJob: Job? = null

    fun startRecording(scope: CoroutineScope): Result<Unit> {
        return try {
            cancelRecording()
            val dir = File(context.cacheDir, "voice_notes").apply { mkdirs() }
            val file = File(dir, "voice_${UUID.randomUUID()}.m4a")
            outputFile = file

            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = mediaRecorder
            startTimeMs = System.currentTimeMillis()
            _elapsedSeconds.value = 0
            _amplitude.value = 0.15f
            _isRecording.value = true

            meterJob?.cancel()
            meterJob = scope.launch(Dispatchers.Default) {
                while (isActive && _isRecording.value) {
                    val elapsed = ((System.currentTimeMillis() - startTimeMs) / 1000L).toInt()
                    _elapsedSeconds.value = elapsed
                    val amp = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                    _amplitude.value = (amp / 22_000f).coerceIn(0.12f, 1f)
                    delay(150L)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start voice recording", e)
            cancelRecording()
            Result.failure(e)
        }
    }

    fun stopAndFinalize(): VoiceRecordingResult? {
        meterJob?.cancel()
        meterJob = null
        val file = outputFile
        val duration = ((System.currentTimeMillis() - startTimeMs) / 1000L).toInt().coerceAtLeast(1)
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            _isRecording.value = false
            _elapsedSeconds.value = 0
            _amplitude.value = 0f
            outputFile = null
            if (file != null && file.exists() && file.length() > 0L) {
                VoiceRecordingResult(file = file, durationSec = duration)
            } else {
                runCatching { file?.delete() }
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop voice recording", e)
            runCatching { recorder?.release() }
            recorder = null
            _isRecording.value = false
            runCatching { file?.delete() }
            outputFile = null
            null
        }
    }

    fun cancelRecording() {
        meterJob?.cancel()
        meterJob = null
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        runCatching { outputFile?.delete() }
        outputFile = null
        _isRecording.value = false
        _elapsedSeconds.value = 0
        _amplitude.value = 0f
    }

    private companion object {
        const val TAG = "VoiceNoteRecorder"
    }
}

data class VoicePlaybackState(
    val activeMessageId: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val progress: Float = 0f,
    val currentSec: Int = 0,
    val totalSec: Int = 0,
    val playbackSpeed: Float = 1.0f
)

class VoiceNotePlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var preferredSpeed: Float = 1.0f

    private val _state = MutableStateFlow(VoicePlaybackState())
    val state: StateFlow<VoicePlaybackState> = _state.asStateFlow()

    fun cycleSpeed(): Float {
        preferredSpeed = nextPlaybackSpeed(preferredSpeed)
        applySpeedToActivePlayer(preferredSpeed)
        _state.value = _state.value.copy(playbackSpeed = preferredSpeed)
        return preferredSpeed
    }

    private fun applySpeedToActivePlayer(speed: Float) {
        val mp = mediaPlayer ?: return
        runCatching {
            val wasPlaying = mp.isPlaying
            val params = mp.playbackParams
            params.speed = speed
            mp.playbackParams = params
            if (!wasPlaying && mp.isPlaying) {
                mp.pause()
            }
        }.onFailure { e ->
            Log.w(TAG, "Playback speed adjustment unsupported on this device", e)
        }
    }

    fun togglePlay(
        messageId: String,
        url: String,
        fallbackDurationSec: Int,
        scope: CoroutineScope
    ) {
        val current = _state.value
        if (current.activeMessageId == messageId && mediaPlayer != null) {
            val mp = mediaPlayer ?: return
            if (mp.isPlaying) {
                mp.pause()
                _state.value = current.copy(isPlaying = false, playbackSpeed = preferredSpeed)
            } else {
                mp.start()
                applySpeedToActivePlayer(preferredSpeed)
                _state.value = current.copy(isPlaying = true, playbackSpeed = preferredSpeed)
                startProgressLoop(scope, messageId, fallbackDurationSec)
            }
            return
        }

        stop()
        _state.value = VoicePlaybackState(
            activeMessageId = messageId,
            isPlaying = false,
            isBuffering = true,
            progress = 0f,
            currentSec = 0,
            totalSec = fallbackDurationSec,
            playbackSpeed = preferredSpeed
        )

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { mp ->
                    val total = (mp.duration / 1000).coerceAtLeast(fallbackDurationSec.coerceAtLeast(1))
                    mp.start()
                    if (preferredSpeed != 1.0f) {
                        runCatching {
                            val params = mp.playbackParams
                            params.speed = preferredSpeed
                            mp.playbackParams = params
                        }
                    }
                    _state.value = VoicePlaybackState(
                        activeMessageId = messageId,
                        isPlaying = true,
                        isBuffering = false,
                        progress = 0f,
                        currentSec = 0,
                        totalSec = total,
                        playbackSpeed = preferredSpeed
                    )
                    startProgressLoop(scope, messageId, total)
                }
                setOnCompletionListener {
                    stop()
                }
                setOnErrorListener { _, _, _ ->
                    stop()
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play voice note", e)
            stop()
        }
    }

    private fun startProgressLoop(scope: CoroutineScope, messageId: String, totalFallback: Int) {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val mp = mediaPlayer
                if (mp == null || !mp.isPlaying || _state.value.activeMessageId != messageId) {
                    break
                }
                val durMs = mp.duration.takeIf { it > 0 } ?: (totalFallback * 1000).coerceAtLeast(1000)
                val posMs = mp.currentPosition.coerceIn(0, durMs)
                _state.value = _state.value.copy(
                    isPlaying = true,
                    isBuffering = false,
                    progress = (posMs.toFloat() / durMs.toFloat()).coerceIn(0f, 1f),
                    currentSec = posMs / 1000,
                    totalSec = durMs / 1000,
                    playbackSpeed = preferredSpeed
                )
                delay(200L)
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        runCatching {
            mediaPlayer?.stop()
        }
        runCatching {
            mediaPlayer?.release()
        }
        mediaPlayer = null
        _state.value = VoicePlaybackState(playbackSpeed = preferredSpeed)
    }

    companion object {
        private const val TAG = "VoiceNotePlayer"

        fun nextPlaybackSpeed(current: Float): Float = when {
            current < 1.25f -> 1.5f
            current < 1.75f -> 2.0f
            else -> 1.0f
        }

        fun formatSpeedLabel(speed: Float): String = when {
            speed >= 1.9f -> "2x"
            speed >= 1.4f -> "1.5x"
            else -> "1x"
        }
    }
}
