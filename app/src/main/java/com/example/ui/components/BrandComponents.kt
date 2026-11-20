package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueDark
import com.example.ui.theme.BrandBlueLight
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleLight
import com.example.ui.theme.DisplayLatinFontFamily
import com.example.ui.theme.OfflineAmber
import com.example.ui.theme.OnlineGreen
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import androidx.compose.ui.draw.drawWithCache

@Composable
fun BanglaChatLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val w = this.size.width
                val h = this.size.height

                // Left vibrant blue bubble (plump oval chat bubble matching mockup)
                val leftBubbleWidth = w * 0.64f
                val leftBubbleHeight = h * 0.54f
                val leftTopLeft = Offset(w * 0.05f, h * 0.08f)
                val blueBrush = Brush.linearGradient(
                    colors = listOf(Color(0xFF00A6FF), Color(0xFF0F72FA), Color(0xFF0A4ED6)),
                    start = leftTopLeft,
                    end = Offset(leftTopLeft.x + leftBubbleWidth, leftTopLeft.y + leftBubbleHeight)
                )
                val leftTail = Path().apply {
                    moveTo(w * 0.18f, h * 0.52f)
                    quadraticTo(w * 0.14f, h * 0.66f, w * 0.11f, h * 0.73f)
                    quadraticTo(w * 0.24f, h * 0.69f, w * 0.34f, h * 0.59f)
                    close()
                }
                val leftDotY = leftTopLeft.y + leftBubbleHeight * 0.50f
                val leftDotRadius = w * 0.039f
                val leftCorner = CornerRadius(leftBubbleHeight * 0.50f, leftBubbleHeight * 0.50f)

                // Right overlapping vivid purple bubble with soft white halo
                val rightBubbleWidth = w * 0.52f
                val rightBubbleHeight = h * 0.44f
                val rightTopLeft = Offset(w * 0.43f, h * 0.34f)
                val purpleBrush = Brush.linearGradient(
                    colors = listOf(Color(0xFFB15DFF), Color(0xFF8C2DF6), Color(0xFF6B21D9)),
                    start = rightTopLeft,
                    end = Offset(rightTopLeft.x + rightBubbleWidth, rightTopLeft.y + rightBubbleHeight)
                )
                val rightTail = Path().apply {
                    moveTo(w * 0.73f, h * 0.71f)
                    quadraticTo(w * 0.81f, h * 0.82f, w * 0.89f, h * 0.86f)
                    quadraticTo(w * 0.88f, h * 0.74f, w * 0.86f, h * 0.66f)
                    close()
                }
                val rightDotY = rightTopLeft.y + rightBubbleHeight * 0.50f
                val rightDotRadius = w * 0.031f
                val haloPadX = w * 0.034f
                val haloPadY = h * 0.034f
                val rightHaloCorner = CornerRadius(
                    (rightBubbleHeight + haloPadY * 2f) * 0.50f,
                    (rightBubbleHeight + haloPadY * 2f) * 0.50f
                )
                val rightCorner = CornerRadius(rightBubbleHeight * 0.50f, rightBubbleHeight * 0.50f)

                onDrawBehind {
                    drawRoundRect(
                        brush = blueBrush,
                        topLeft = leftTopLeft,
                        size = Size(leftBubbleWidth, leftBubbleHeight),
                        cornerRadius = leftCorner
                    )
                    drawPath(leftTail, brush = blueBrush)
                    drawCircle(Color.White, leftDotRadius, Offset(w * 0.24f, leftDotY))
                    drawCircle(Color.White, leftDotRadius, Offset(w * 0.37f, leftDotY))
                    drawCircle(Color.White, leftDotRadius, Offset(w * 0.50f, leftDotY))

                    // Clean white halo separating overlapping bubbles
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.94f),
                        topLeft = Offset(rightTopLeft.x - haloPadX, rightTopLeft.y - haloPadY),
                        size = Size(rightBubbleWidth + haloPadX * 2f, rightBubbleHeight + haloPadY * 2f),
                        cornerRadius = rightHaloCorner
                    )
                    drawRoundRect(
                        brush = purpleBrush,
                        topLeft = rightTopLeft,
                        size = Size(rightBubbleWidth, rightBubbleHeight),
                        cornerRadius = rightCorner
                    )
                    drawPath(rightTail, brush = purpleBrush)
                    drawCircle(Color.White, rightDotRadius, Offset(w * 0.58f, rightDotY))
                    drawCircle(Color.White, rightDotRadius, Offset(w * 0.69f, rightDotY))
                    drawCircle(Color.White, rightDotRadius, Offset(w * 0.80f, rightDotY))
                }
            }
    )
}

@Composable
fun BanglaChatWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp
) {
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = Color(0xFF0A49CA),
                    fontFamily = DisplayLatinFontFamily,
                    fontWeight = FontWeight.ExtraBold
                )
            ) {
                append("Bangla ")
            }
            withStyle(
                SpanStyle(
                    color = Color(0xFF8C19F5),
                    fontFamily = DisplayLatinFontFamily,
                    fontWeight = FontWeight.ExtraBold
                )
            ) {
                append("Chat")
            }
        },
        fontSize = fontSize,
        modifier = modifier
    )
}

@Composable
fun GoogleGLogo(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val strokeWidth = this.size.minDimension * 0.20f
                val inset = strokeWidth * 0.58f
                val arcSize = Size(
                    width = this.size.width - inset * 2f,
                    height = this.size.height - inset * 2f
                )
                val arcTopLeft = Offset(inset, inset)
                val stroke = Stroke(width = strokeWidth)
                val centerY = this.size.height * 0.50f
                val centerX = this.size.width * 0.50f
                val barEndX = this.size.width - inset * 0.65f

                onDrawBehind {
                    // Red top arc
                    drawArc(
                        color = Color(0xFFEA4335),
                        startAngle = -145f,
                        sweepAngle = 100f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = stroke
                    )
                    // Yellow left arc
                    drawArc(
                        color = Color(0xFFFBBC05),
                        startAngle = 145f,
                        sweepAngle = 72f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = stroke
                    )
                    // Green bottom arc
                    drawArc(
                        color = Color(0xFF34A853),
                        startAngle = 45f,
                        sweepAngle = 104f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = stroke
                    )
                    // Blue right arc
                    drawArc(
                        color = Color(0xFF4285F4),
                        startAngle = -8f,
                        sweepAngle = 60f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = stroke
                    )
                    // Blue horizontal crossbar of the "G"
                    drawLine(
                        color = Color(0xFF4285F4),
                        start = Offset(centerX, centerY),
                        end = Offset(barEndX, centerY),
                        strokeWidth = strokeWidth
                    )
                }
            }
    )
}

/**
 * Full-bleed atmospheric artwork for the Login hero matching the mockup:
 * soft translucent world map, looped dashed paper airplane trail, two-tone blue
 * origami paper plane, and ornate royal-blue Dhaka/mosque skyline silhouette.
 */
@Composable
fun AuthHeroSceneBackdrop(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false
) {
    Box(
        modifier = modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val baseY = h * 0.96f

            // 1. Subtle stylized world map continent blobs
            val mapColor = if (isDarkTheme) {
                Color(0xFF6A8BE8).copy(alpha = 0.16f)
            } else {
                Color(0xFF9EB5F0).copy(alpha = 0.28f)
            }
            val mapPath = Path().apply {
                // Americas silhouette (left)
                moveTo(w * 0.10f, h * 0.24f)
                cubicTo(w * 0.16f, h * 0.14f, w * 0.27f, h * 0.16f, w * 0.30f, h * 0.25f)
                cubicTo(w * 0.32f, h * 0.32f, w * 0.26f, h * 0.38f, w * 0.29f, h * 0.47f)
                cubicTo(w * 0.33f, h * 0.56f, w * 0.29f, h * 0.69f, w * 0.24f, h * 0.72f)
                cubicTo(w * 0.19f, h * 0.68f, w * 0.17f, h * 0.55f, w * 0.14f, h * 0.45f)
                cubicTo(w * 0.08f, h * 0.38f, w * 0.07f, h * 0.30f, w * 0.10f, h * 0.24f)
                close()

                // Eurasia & Africa silhouette (center-right)
                moveTo(w * 0.38f, h * 0.26f)
                cubicTo(w * 0.46f, h * 0.16f, w * 0.64f, h * 0.15f, w * 0.76f, h * 0.23f)
                cubicTo(w * 0.85f, h * 0.29f, w * 0.84f, h * 0.44f, w * 0.74f, h * 0.49f)
                cubicTo(w * 0.66f, h * 0.53f, w * 0.58f, h * 0.48f, w * 0.53f, h * 0.54f)
                cubicTo(w * 0.50f, h * 0.62f, w * 0.45f, h * 0.70f, w * 0.41f, h * 0.64f)
                cubicTo(w * 0.37f, h * 0.55f, w * 0.35f, h * 0.42f, w * 0.38f, h * 0.26f)
                close()

                // Australia silhouette (lower right)
                moveTo(w * 0.76f, h * 0.56f)
                cubicTo(w * 0.82f, h * 0.54f, w * 0.88f, h * 0.58f, w * 0.86f, h * 0.64f)
                cubicTo(w * 0.82f, h * 0.68f, w * 0.74f, h * 0.64f, w * 0.76f, h * 0.56f)
                close()
            }

            // 2. Looped dashed flight trail curving up to the paper airplane
            val trailPath = Path().apply {
                moveTo(w * 0.51f, h * 0.56f)
                cubicTo(w * 0.58f, h * 0.62f, w * 0.69f, h * 0.58f, w * 0.71f, h * 0.47f)
                cubicTo(w * 0.72f, h * 0.39f, w * 0.66f, h * 0.36f, w * 0.64f, h * 0.41f)
                cubicTo(w * 0.62f, h * 0.46f, w * 0.68f, h * 0.51f, w * 0.75f, h * 0.42f)
                cubicTo(w * 0.78f, h * 0.38f, w * 0.80f, h * 0.34f, w * 0.82f, h * 0.30f)
            }
            val trailStroke = Stroke(
                width = 3.2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(11f, 9f), 0f)
            )
            val trailColor = if (isDarkTheme) Color(0xFF64A4FF) else Color(0xFF1D6DF0)

            // 3. Two-tone origami paper airplane flying up-right
            val planeTopWing = Path().apply {
                moveTo(w * 0.90f, h * 0.14f)
                lineTo(w * 0.73f, h * 0.25f)
                lineTo(w * 0.79f, h * 0.29f)
                close()
            }
            val planeFoldShadow = Path().apply {
                moveTo(w * 0.90f, h * 0.14f)
                lineTo(w * 0.79f, h * 0.29f)
                lineTo(w * 0.78f, h * 0.36f)
                lineTo(w * 0.82f, h * 0.31f)
                close()
            }
            val planeBottomWing = Path().apply {
                moveTo(w * 0.90f, h * 0.14f)
                lineTo(w * 0.82f, h * 0.31f)
                lineTo(w * 0.86f, h * 0.37f)
                close()
            }

            // 4. Ornate multi-minaret & grand dome royal-blue mosque skyline
            val skylineBack = Path().apply {
                moveTo(0f, h)
                lineTo(0f, baseY - h * 0.14f)
                lineTo(w * 0.05f, baseY - h * 0.16f)
                cubicTo(w * 0.08f, baseY - h * 0.24f, w * 0.13f, baseY - h * 0.24f, w * 0.16f, baseY - h * 0.15f)
                lineTo(w * 0.24f, baseY - h * 0.16f)
                cubicTo(w * 0.29f, baseY - h * 0.28f, w * 0.37f, baseY - h * 0.28f, w * 0.42f, baseY - h * 0.16f)
                lineTo(w * 0.58f, baseY - h * 0.16f)
                cubicTo(w * 0.63f, baseY - h * 0.29f, w * 0.72f, baseY - h * 0.29f, w * 0.77f, baseY - h * 0.15f)
                lineTo(w * 0.84f, baseY - h * 0.16f)
                cubicTo(w * 0.88f, baseY - h * 0.25f, w * 0.94f, baseY - h * 0.25f, w * 0.97f, baseY - h * 0.15f)
                lineTo(w, baseY - h * 0.14f)
                lineTo(w, h)
                close()
            }

            fun Path.addMinaret(cx: Float, topY: Float, halfW: Float, baseLine: Float) {
                lineTo(cx - halfW, baseLine)
                lineTo(cx - halfW * 0.9f, topY + h * 0.09f)
                lineTo(cx - halfW * 1.35f, topY + h * 0.08f)
                lineTo(cx - halfW * 0.85f, topY + h * 0.06f)
                lineTo(cx - halfW * 0.70f, topY + h * 0.03f)
                lineTo(cx, topY)
                lineTo(cx + halfW * 0.70f, topY + h * 0.03f)
                lineTo(cx + halfW * 0.85f, topY + h * 0.06f)
                lineTo(cx + halfW * 1.35f, topY + h * 0.08f)
                lineTo(cx + halfW * 0.9f, topY + h * 0.09f)
                lineTo(cx + halfW, baseLine)
            }

            fun Path.addOnionDome(startX: Float, endX: Float, peakY: Float, baseLine: Float) {
                val midX = (startX + endX) * 0.5f
                val span = endX - startX
                lineTo(startX, baseLine)
                cubicTo(
                    startX - span * 0.06f, baseLine - (baseLine - peakY) * 0.55f,
                    midX - span * 0.20f, peakY + (baseLine - peakY) * 0.15f,
                    midX, peakY
                )
                lineTo(midX, peakY - h * 0.055f)
                lineTo(midX + w * 0.004f, peakY)
                cubicTo(
                    midX + span * 0.20f, peakY + (baseLine - peakY) * 0.15f,
                    endX + span * 0.06f, baseLine - (baseLine - peakY) * 0.55f,
                    endX, baseLine
                )
            }

            val roofY = baseY - h * 0.12f
            val skylineFront = Path().apply {
                moveTo(0f, h)
                lineTo(0f, roofY + h * 0.02f)
                lineTo(w * 0.045f, roofY + h * 0.01f)
                addMinaret(cx = w * 0.072f, topY = baseY - h * 0.36f, halfW = w * 0.014f, baseLine = roofY)
                addOnionDome(startX = w * 0.095f, endX = w * 0.155f, peakY = baseY - h * 0.23f, baseLine = roofY)
                addMinaret(cx = w * 0.178f, topY = baseY - h * 0.30f, halfW = w * 0.012f, baseLine = roofY)
                addOnionDome(startX = w * 0.205f, endX = w * 0.275f, peakY = baseY - h * 0.25f, baseLine = roofY)
                addMinaret(cx = w * 0.295f, topY = baseY - h * 0.28f, halfW = w * 0.011f, baseLine = roofY)
                addOnionDome(startX = w * 0.320f, endX = w * 0.400f, peakY = baseY - h * 0.27f, baseLine = roofY)
                addMinaret(cx = w * 0.418f, topY = baseY - h * 0.31f, halfW = w * 0.011f, baseLine = roofY)
                // Grand central mosque onion dome
                addOnionDome(startX = w * 0.438f, endX = w * 0.592f, peakY = baseY - h * 0.37f, baseLine = roofY)
                addMinaret(cx = w * 0.612f, topY = baseY - h * 0.32f, halfW = w * 0.012f, baseLine = roofY)
                addOnionDome(startX = w * 0.635f, endX = w * 0.715f, peakY = baseY - h * 0.25f, baseLine = roofY)
                addMinaret(cx = w * 0.742f, topY = baseY - h * 0.29f, halfW = w * 0.012f, baseLine = roofY)
                addOnionDome(startX = w * 0.768f, endX = w * 0.840f, peakY = baseY - h * 0.24f, baseLine = roofY)
                addMinaret(cx = w * 0.865f, topY = baseY - h * 0.35f, halfW = w * 0.014f, baseLine = roofY)
                addOnionDome(startX = w * 0.892f, endX = w * 0.955f, peakY = baseY - h * 0.22f, baseLine = roofY)
                lineTo(w, roofY + h * 0.01f)
                lineTo(w, h)
                close()
            }

            val backBrush = Brush.verticalGradient(
                colors = if (isDarkTheme) {
                    listOf(Color(0xFF2855C8).copy(alpha = 0.45f), Color(0xFF163278).copy(alpha = 0.25f))
                } else {
                    listOf(Color(0xFF5D9BFF).copy(alpha = 0.52f), Color(0xFF8AB8FF).copy(alpha = 0.24f))
                },
                startY = baseY - h * 0.30f,
                endY = h
            )

            val frontBrush = Brush.verticalGradient(
                colors = if (isDarkTheme) {
                    listOf(Color(0xFF2166E8).copy(alpha = 0.88f), Color(0xFF123E9C).copy(alpha = 0.92f))
                } else {
                    listOf(Color(0xFF1968F2).copy(alpha = 0.92f), Color(0xFF4F94FF).copy(alpha = 0.86f))
                },
                startY = baseY - h * 0.42f,
                endY = h
            )

            onDrawBehind {
                drawPath(path = mapPath, color = mapColor)
                drawPath(
                    path = trailPath,
                    color = trailColor.copy(alpha = 0.70f),
                    style = trailStroke
                )
                drawPath(path = planeTopWing, color = Color(0xFF117CF7))
                drawPath(path = planeBottomWing, color = Color(0xFF0A66E2))
                drawPath(path = planeFoldShadow, color = Color(0xFF0644B0))
                drawPath(path = skylineBack, brush = backBrush)
                drawPath(path = skylineFront, brush = frontBrush)
            }
        }
    )
}

@Composable
fun DhakaSkylineCanvas(
    modifier: Modifier = Modifier,
    silhouetteColor: Color = BrandBlue.copy(alpha = 0.20f),
    planeColor: Color = BrandBlue
) {
    AuthHeroSceneBackdrop(modifier = modifier)
}

@Composable
fun UserAvatar(
    name: String,
    photoUrl: String,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    showOnlineIndicator: Boolean = false,
    isOnline: Boolean = false
) {
    val initials = name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "BC" }

    val avatarBrush = Brush.linearGradient(
        colors = listOf(BrandBlueLight, BrandPurpleLight)
    )

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(avatarBrush),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.34f).sp
            )
            if (photoUrl.isNotBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (showOnlineIndicator && isOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size.value * 0.28f).coerceAtLeast(12f).dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(OnlineGreen)
            )
        }
    }
}

@Composable
fun OfflineConnectivityBanner(
    isOnline: Boolean,
    offlineMessage: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = !isOnline, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OfflineAmber.copy(alpha = 0.18f))
                .border(0.5.dp, OfflineAmber.copy(alpha = 0.45f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.WifiOff,
                contentDescription = null,
                tint = OfflineAmber,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = offlineMessage,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun formatConversationTime(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    val date = timestamp.toDate()
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { time = date }
    return when {
        now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR) -> {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
        }
        now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - target.get(Calendar.DAY_OF_YEAR) == 1 -> {
            "Yesterday"
        }
        else -> {
            SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
        }
    }
}

fun formatMessageTime(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(timestamp.toDate())
}

fun formatFullDate(timestamp: Timestamp?): String {
    if (timestamp == null) return "—"
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(timestamp.toDate())
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return ""
    return when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
        else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
