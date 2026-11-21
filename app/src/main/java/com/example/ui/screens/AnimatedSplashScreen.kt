package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.components.BanglaChatLogoMark
import com.example.ui.components.BanglaChatWordmark
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueLight
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Branded animated launch & loading screen for Bangla Chat.
 * Replaces generic loading spinners with concentric sonar rings, floating aurora backdrop,
 * spring-animated logo emblem, orbiting chat sparks, and a 3-dot wave typing indicator.
 */
@Composable
fun AnimatedSplashScreen(
    statusText: String = "",
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Staggered spring entrance animations
    val logoScale = remember { Animatable(0.55f) }
    val logoAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(24f) }
    val footerAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            logoAlpha.animateTo(1f, animationSpec = tween(360, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        delay(180L)
        launch {
            titleAlpha.animateTo(1f, animationSpec = tween(420, easing = FastOutSlowInEasing))
        }
        launch {
            titleOffsetY.animateTo(0f, animationSpec = tween(420, easing = FastOutSlowInEasing))
        }
        delay(160L)
        footerAlpha.animateTo(1f, animationSpec = tween(380, easing = FastOutSlowInEasing))
    }

    // Continuous ambient motion
    val infinite = rememberInfiniteTransition(label = "splash_motion")
    val pulseOne by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_one"
    )
    val pulseTwo by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, delayMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_two"
    )
    val floatY by infinite.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_float"
    )
    val orbitAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )
    val dotWave by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "typing_dots_wave"
    )

    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF070C1B),
                Color(0xFF0F1936),
                Color(0xFF151E42),
                Color(0xFF0A1124)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE7F2FF),
                Color(0xFFF6F2FF),
                Color(0xFFECEBFF),
                Color(0xFFE1EEFF)
            )
        )
    }

    val primaryTextColor = if (isDark) Color(0xFFF2F6FF) else Color(0xFF172245)
    val secondaryTextColor = if (isDark) Color(0xFFAEC0EB) else Color(0xFF4F5E88)
    val emblemSurface = if (isDark) Color(0xFF172244).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.92f)
    val emblemBorder = if (isDark) Color(0xFF3A5291).copy(alpha = 0.7f) else Color.White

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .testTag("animated_splash_screen")
    ) {
        // Atmospheric glowing aurora orbs & subtle constellation rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val radAngle = orbitAngle * (PI / 180.0)
            val driftX = (cos(radAngle) * w * 0.06f).toFloat()
            val driftY = (sin(radAngle) * h * 0.04f).toFloat()

            // Top-left royal blue glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        (if (isDark) BrandBlue else BrandBlueLight).copy(alpha = if (isDark) 0.28f else 0.24f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.18f + driftX, h * 0.20f + driftY),
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = Offset(w * 0.18f + driftX, h * 0.20f + driftY)
            )

            // Bottom-right vivid violet glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        (if (isDark) BrandPurple else BrandPurpleLight).copy(alpha = if (isDark) 0.26f else 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.84f - driftX, h * 0.78f - driftY),
                    radius = w * 0.72f
                ),
                radius = w * 0.72f,
                center = Offset(w * 0.84f - driftX, h * 0.78f - driftY)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 28.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Center Animated Emblem + Wordmark + Tagline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(230.dp)
                ) {
                    // Expanding sonar chat-wave rings + orbiting spark dots
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val minRadius = size.minDimension * 0.30f
                        val maxRadius = size.minDimension * 0.49f

                        val r1 = minRadius + (maxRadius - minRadius) * pulseOne
                        drawCircle(
                            color = BrandBlue.copy(alpha = (1f - pulseOne) * (if (isDark) 0.45f else 0.34f)),
                            radius = r1,
                            center = center,
                            style = Stroke(width = 2.6.dp.toPx())
                        )

                        val r2 = minRadius + (maxRadius - minRadius) * pulseTwo
                        drawCircle(
                            color = BrandPurple.copy(alpha = (1f - pulseTwo) * (if (isDark) 0.38f else 0.28f)),
                            radius = r2,
                            center = center,
                            style = Stroke(width = 2.2.dp.toPx())
                        )

                        // Subtle orbit track
                        val orbitRadius = size.minDimension * 0.40f
                        drawCircle(
                            color = (if (isDark) Color.White else BrandBlue).copy(alpha = 0.08f),
                            radius = orbitRadius,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // Three orbiting luminous message sparks
                        val sparkColors = listOf(BrandBlueLight, BrandPurpleLight, Color(0xFF22C55E))
                        sparkColors.forEachIndexed { idx, sparkColor ->
                            val theta = (orbitAngle + idx * 120f) * (PI / 180.0)
                            val sx = center.x + (cos(theta) * orbitRadius).toFloat()
                            val sy = center.y + (sin(theta) * orbitRadius).toFloat()
                            drawCircle(
                                color = sparkColor.copy(alpha = 0.26f),
                                radius = 9.dp.toPx(),
                                center = Offset(sx, sy)
                            )
                            drawCircle(
                                color = sparkColor,
                                radius = 4.dp.toPx(),
                                center = Offset(sx, sy)
                            )
                        }
                    }

                    // Elevated glass circular emblem holding BanglaChatLogoMark
                    Surface(
                        shape = CircleShape,
                        color = emblemSurface,
                        shadowElevation = 14.dp,
                        modifier = Modifier
                            .offset(y = floatY.dp)
                            .scale(logoScale.value)
                            .alpha(logoAlpha.value)
                            .size(134.dp)
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        BrandBlue.copy(alpha = 0.65f),
                                        BrandPurple.copy(alpha = 0.65f),
                                        emblemBorder
                                    )
                                ),
                                shape = CircleShape
                            )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            BanglaChatLogoMark(size = 92.dp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .alpha(titleAlpha.value)
                        .offset(y = titleOffsetY.value.dp)
                ) {
                    BanglaChatWordmark(fontSize = 38.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.taglineLatin,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.1.sp
                        ),
                        color = primaryTextColor.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${strings.taglineLineOne} • ${strings.taglineLineTwo}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryTextColor,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom Animated 3-Dot Typing Loader & Status Pill
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(footerAlpha.value)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(28.dp)
                ) {
                    val dotColors = listOf(BrandBlue, Color(0xFF5A5DF6), BrandPurple)
                    dotColors.forEachIndexed { idx, dotColor ->
                        val wave = sin((dotWave - idx * 0.85f).toDouble()).toFloat()
                        val offsetY = (-wave * 6f).dp
                        val scale = 0.82f + ((wave + 1f) / 2f) * 0.36f
                        Box(
                            modifier = Modifier
                                .offset(y = offsetY)
                                .scale(scale)
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val caption = statusText.ifBlank { strings.splashConnectingStatus }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = (if (isDark) Color.White else BrandBlue).copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = caption,
                            style = MaterialTheme.typography.labelMedium,
                            color = secondaryTextColor,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
