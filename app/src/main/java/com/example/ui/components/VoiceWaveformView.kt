package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * 24-bar deterministic voice waveform visualizer with smooth live wave animation while playing.
 */
@Composable
fun AnimatedVoiceNoteWaveform(
    seedKey: String,
    progress: Float,
    isActive: Boolean,
    isPlaying: Boolean,
    isMe: Boolean,
    modifier: Modifier = Modifier
) {
    val bars = remember(seedKey) {
        val seed = seedKey.hashCode()
        List(24) { idx ->
            val v = abs((seed * 31 + idx * 19 + idx * idx * 7) % 100)
            (0.22f + (v / 100f) * 0.78f)
        }
    }

    val wavePhase = if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "voice_wave")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 820, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "voice_wave_phase"
        )
        phase
    } else {
        0f
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        bars.forEachIndexed { idx, baseHeight ->
            val barThreshold = (idx + 1).toFloat() / bars.size.toFloat()
            val played = isActive && progress >= barThreshold
            val animatedHeight = if (isPlaying) {
                val dance = 0.24f * sin((wavePhase + idx * 0.55f).toDouble()).toFloat()
                (baseHeight + dance).coerceIn(0.18f, 1.0f)
            } else {
                baseHeight
            }
            val barColor = when {
                played && isMe -> Color.White
                played && !isMe -> BrandBlue
                isPlaying && isMe -> Color.White.copy(alpha = 0.55f)
                isPlaying && !isMe -> BrandPurple.copy(alpha = 0.50f)
                isMe -> Color.White.copy(alpha = 0.36f)
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.34f)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(animatedHeight)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
    }
}

/**
 * Animated multi-bar live recording waveform reacting to microphone amplitude.
 */
@Composable
fun LiveRecordingWaveform(
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "rec_wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rec_wave_phase"
    )
    val clampedAmp = amplitude.coerceIn(0.15f, 1f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        modifier = modifier.height(26.dp)
    ) {
        for (i in 0 until 20) {
            val wave = (sin((phase + i * 0.52f).toDouble()).toFloat() + 1f) * 0.5f
            val h = (0.20f + clampedAmp * 0.60f * (0.45f + 0.55f * wave)).coerceIn(0.18f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(h)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (i % 2 == 0) BrandPurple else BrandBlue)
            )
        }
    }
}
