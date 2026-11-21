package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LooksOne
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.VoiceNotePlayer
import com.example.data.VoicePlaybackState
import com.example.model.ChatMessage
import com.example.ui.components.AnimatedVoiceNoteWaveform
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.OnlineGreen
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Right-swipe container for message bubbles that triggers quick reply with haptic feedback.
 */
@Composable
fun SwipeToReplyContainer(
    enabled: Boolean,
    onSwipeReply: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        Box(modifier = modifier) { content() }
        return
    }
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val maxOffsetPx = with(density) { 68.dp.toPx() }
    val triggerThresholdPx = with(density) { 46.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    var triggeredHaptic by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(enabled) {
                detectHorizontalDragGestures(
                    onDragStart = { triggeredHaptic = false },
                    onDragEnd = {
                        val shouldReply = offsetX.value >= triggerThresholdPx
                        if (shouldReply) {
                            onSwipeReply()
                        }
                        scope.launch {
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            offsetX.animateTo(0f)
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        val next = (offsetX.value + dragAmount).coerceIn(0f, maxOffsetPx)
                        if (next >= triggerThresholdPx && !triggeredHaptic) {
                            triggeredHaptic = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        scope.launch {
                            offsetX.snapTo(next)
                        }
                    }
                )
            }
    ) {
        val progress = (offsetX.value / triggerThresholdPx).coerceIn(0f, 1f)
        if (progress > 0.08f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp)
                    .size(34.dp)
                    .alpha(progress)
                    .clip(CircleShape)
                    .background(BrandBlue.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Reply,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
        ) {
            content()
        }
    }
}

/**
 * Enhanced voice note player bubble with deterministic 20-bar waveform and 1x / 1.5x / 2x speed pill.
 */
@Composable
fun VoiceWaveformBubble(
    message: ChatMessage,
    isMe: Boolean,
    textColor: Color,
    metaColor: Color,
    playbackState: VoicePlaybackState,
    onTogglePlay: () -> Unit,
    onCycleSpeed: () -> Unit
) {
    val isThisActive = playbackState.activeMessageId == message.id
    val isThisPlaying = isThisActive && playbackState.isPlaying
    val isThisBuffering = isThisActive && playbackState.isBuffering
    val progress = if (isThisActive) playbackState.progress else 0f
    val totalSec = if (isThisActive && playbackState.totalSec > 0) {
        playbackState.totalSec
    } else {
        message.audioDurationSec.toInt().coerceAtLeast(1)
    }
    val displaySec = if (isThisActive && (isThisPlaying || playbackState.currentSec > 0)) {
        playbackState.currentSec
    } else {
        totalSec
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .widthIn(min = 215.dp, max = 265.dp)
            .padding(vertical = 2.dp)
    ) {
        Surface(
            onClick = onTogglePlay,
            shape = CircleShape,
            color = if (isMe) Color.White.copy(alpha = 0.22f) else BrandBlue.copy(alpha = 0.14f),
            modifier = Modifier
                .size(40.dp)
                .testTag("voice_play_button_${message.id}")
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isThisBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = if (isMe) Color.White else BrandBlue
                    )
                } else {
                    Icon(
                        imageVector = if (isThisPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isThisPlaying) "Pause" else "Play",
                        tint = if (isMe) Color.White else BrandBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            AnimatedVoiceNoteWaveform(
                seedKey = message.id,
                progress = progress,
                isActive = isThisActive,
                isPlaying = isThisPlaying,
                isMe = isMe
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(Locale.US, "%d:%02d", displaySec / 60, displaySec % 60),
                    style = MaterialTheme.typography.labelSmall,
                    color = metaColor
                )
                Surface(
                    onClick = onCycleSpeed,
                    shape = RoundedCornerShape(10.dp),
                    color = if (isMe) Color.White.copy(alpha = 0.22f) else BrandBlue.copy(alpha = 0.14f),
                    modifier = Modifier.testTag("voice_speed_button_${message.id}")
                ) {
                    Text(
                        text = VoiceNotePlayer.formatSpeedLabel(playbackState.playbackSpeed),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isMe) Color.White else BrandBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Sealed "View Once" photo bubble shown inline in the conversation.
 */
@Composable
fun ViewOncePhotoBubble(
    message: ChatMessage,
    isMe: Boolean,
    currentUserId: String,
    textColor: Color,
    metaColor: Color,
    strings: AppStrings,
    onOpenViewOnce: () -> Unit
) {
    val alreadyOpened = if (isMe) {
        message.isOpenedByRecipient()
    } else {
        message.isViewedBy(currentUserId)
    }
    val canOpen = !alreadyOpened && !isMe && message.mediaUrl.isNotBlank()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isMe) {
            Color.White.copy(alpha = if (alreadyOpened) 0.10f else 0.18f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = if (alreadyOpened) 0.5f else 0.9f)
        },
        modifier = Modifier
            .widthIn(min = 195.dp, max = 250.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = canOpen, onClick = onOpenViewOnce)
            .testTag("view_once_bubble_${message.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (alreadyOpened) {
                            metaColor.copy(alpha = 0.2f)
                        } else if (isMe) {
                            Color.White.copy(alpha = 0.24f)
                        } else {
                            OnlineGreen.copy(alpha = 0.18f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (alreadyOpened) Icons.Rounded.VisibilityOff else Icons.Rounded.LooksOne,
                    contentDescription = strings.viewOncePhotoLabel,
                    tint = if (alreadyOpened) metaColor else if (isMe) Color.White else OnlineGreen,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = if (alreadyOpened) strings.viewOnceOpenedLabel else strings.viewOncePhotoLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (alreadyOpened) metaColor else textColor
                )
                if (!alreadyOpened && !isMe) {
                    Text(
                        text = strings.viewOncePreviewSubtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = metaColor
                    )
                }
            }
        }
    }
}

/**
 * Full-screen screenshot-protected viewer for View Once photos.
 */
@Composable
fun ViewOncePhotoDialog(
    message: ChatMessage,
    onMarkOpened: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    LaunchedEffect(message.id) {
        onMarkOpened()
    }

    // Temporarily enforce FLAG_SECURE while the View Once photo is open
    DisposableEffect(Unit) {
        val activity = findHostingActivity(context)
        val window = activity?.window
        val hadSecureFlag = ((window?.attributes?.flags ?: 0) and WindowManager.LayoutParams.FLAG_SECURE) != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            if (!hadSecureFlag) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = message.mediaUrl,
                contentDescription = strings.viewOncePhotoLabel,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = OnlineGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = strings.viewOnceViewerHint,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_view_once_dialog_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = strings.close,
                        tint = Color.White
                    )
                }
            }
        }
    }
}

private fun findHostingActivity(context: Context): Activity? {
    var current = context
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

/**
 * Interactive Poll bubble with animated percentage progress bars and one-tap voting.
 */
@Composable
fun PollMessageCard(
    message: ChatMessage,
    currentUserId: String,
    isMe: Boolean,
    textColor: Color,
    metaColor: Color,
    strings: AppStrings,
    onVote: (Int) -> Unit
) {
    val question = message.pollQuestion.ifBlank { message.text }
    val options = message.pollOptions
    val totalVotes = message.pollVotes.size
    val myChoiceIndex = message.pollVotes[currentUserId]?.toIntOrNull()

    Column(
        modifier = Modifier
            .widthIn(min = 225.dp, max = 285.dp)
            .padding(vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Poll,
                contentDescription = null,
                tint = if (isMe) Color.White else BrandBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = question,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
        Spacer(Modifier.height(8.dp))
        options.forEachIndexed { index, optionText ->
            val votesForOption = message.pollVotes.values.count { it == index.toString() }
            val fraction = if (totalVotes > 0) votesForOption.toFloat() / totalVotes.toFloat() else 0f
            val animatedFraction by animateFloatAsState(targetValue = fraction, label = "poll_bar_$index")
            val isSelected = myChoiceIndex == index

            Surface(
                onClick = { onVote(index) },
                shape = RoundedCornerShape(12.dp),
                color = if (isMe) {
                    Color.White.copy(alpha = if (isSelected) 0.24f else 0.12f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = if (isSelected) 0.95f else 0.65f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = when {
                            isSelected && isMe -> Color.White
                            isSelected && !isMe -> BrandBlue
                            isMe -> Color.White.copy(alpha = 0.24f)
                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .testTag("poll_option_${message.id}_$index")
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (animatedFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .fillMaxWidth(animatedFraction)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedFraction)
                                    .background(
                                        if (isMe) Color.White.copy(alpha = 0.20f)
                                        else BrandBlue.copy(alpha = 0.14f)
                                    )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) {
                                if (isMe) Color.White else BrandBlue
                            } else {
                                metaColor
                            },
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = optionText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = textColor,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "$votesForOption",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) textColor else metaColor
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (totalVotes == 0) strings.pollTapToVote else "$totalVotes ${strings.pollVotesSuffix}",
            style = MaterialTheme.typography.labelSmall,
            color = metaColor
        )
    }
}

/**
 * Dialog for authoring a new interactive poll (question + 2 to 5 options).
 */
@Composable
fun CreatePollDialog(
    onDismiss: () -> Unit,
    onCreatePoll: (question: String, options: List<String>) -> Unit
) {
    val strings = LocalAppStrings.current
    var question by remember { mutableStateOf("") }
    val options = remember { mutableStateListOf("", "") }

    val validOptions = options.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    val canSend = question.trim().isNotEmpty() && validOptions.size >= 2

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Poll,
                contentDescription = null,
                tint = BrandBlue
            )
        },
        title = {
            Text(text = strings.createPollTitle, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { if (it.length <= 240) question = it },
                    placeholder = { Text(strings.pollQuestionHint) },
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("poll_question_input")
                )
                options.forEachIndexed { idx, value ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = { if (it.length <= 80) options[idx] = it },
                        placeholder = { Text("${strings.pollOptionHintPrefix} ${idx + 1}") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("poll_option_input_$idx")
                    )
                }
                if (options.size < 5) {
                    TextButton(
                        onClick = { options.add("") },
                        modifier = Modifier.testTag("poll_add_option_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.pollAddOption)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSend) {
                        onCreatePoll(question.trim(), validOptions)
                        onDismiss()
                    }
                },
                enabled = canSend,
                modifier = Modifier.testTag("poll_send_button")
            ) {
                Text(strings.pollSendAction)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

/**
 * Shared Media, Documents & Links gallery bottom sheet for any chat or group.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedMediaGallerySheet(
    messages: List<ChatMessage>,
    onOpenImage: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val photos = remember(messages) {
        messages.filter { !it.deleted && it.type == "image" && !it.viewOnce && it.mediaUrl.isNotBlank() }
            .reversed()
    }
    val docs = remember(messages) {
        messages.filter { !it.deleted && it.type == "document" && it.mediaUrl.isNotBlank() }
            .reversed()
    }
    val links = remember(messages) {
        val urlRegex = Regex("""https?://[^\s<>"]+""", RegexOption.IGNORE_CASE)
        messages.filter { !it.deleted && it.text.isNotBlank() }
            .reversed()
            .flatMap { msg ->
                urlRegex.findAll(msg.text).map { match ->
                    match.value to msg.senderName
                }.toList()
            }
            .distinctBy { it.first }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = strings.sharedMediaTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("${strings.sharedMediaPhotosTab} (${photos.size})") },
                    modifier = Modifier.testTag("shared_media_tab_photos")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("${strings.sharedMediaDocsTab} (${docs.size})") },
                    modifier = Modifier.testTag("shared_media_tab_docs")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("${strings.sharedMediaLinksTab} (${links.size})") },
                    modifier = Modifier.testTag("shared_media_tab_links")
                )
            }
            Spacer(Modifier.height(12.dp))
            when (selectedTab) {
                0 -> {
                    if (photos.isEmpty()) {
                        SharedMediaEmptyPlaceholder(strings.sharedMediaEmpty, Icons.Rounded.Photo)
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(photos, key = { it.id }) { item ->
                                AsyncImage(
                                    model = item.mediaUrl,
                                    contentDescription = item.mediaName.ifBlank { strings.photoMessagePreview },
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onOpenImage(item.mediaUrl) }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    if (docs.isEmpty()) {
                        SharedMediaEmptyPlaceholder(strings.sharedMediaEmpty, Icons.Rounded.Description)
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(docs, key = { it.id }) { doc ->
                                Surface(
                                    onClick = { onOpenUrl(doc.mediaUrl) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Description,
                                            contentDescription = null,
                                            tint = BrandBlue,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = doc.mediaName.ifBlank { strings.documentMessagePreview },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (doc.mediaSizeBytes > 0L) {
                                                val kb = (doc.mediaSizeBytes / 1024L).coerceAtLeast(1L)
                                                Text(
                                                    text = if (kb >= 1024L) "${kb / 1024L} MB" else "$kb KB",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Rounded.OpenInNew,
                                            contentDescription = strings.openAttachment,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    if (links.isEmpty()) {
                        SharedMediaEmptyPlaceholder(strings.sharedMediaEmpty, Icons.Rounded.Link)
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(links, key = { it.first }) { (url, sender) ->
                                Surface(
                                    onClick = { onOpenUrl(url) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Link,
                                            contentDescription = null,
                                            tint = BrandPurple,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = url,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = BrandBlue,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (sender.isNotBlank()) {
                                                Text(
                                                    text = sender,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Rounded.OpenInNew,
                                            contentDescription = strings.openAttachment,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedMediaEmptyPlaceholder(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Generates a UTF-8 `.txt` chat transcript in cacheDir/exports/ and launches Android's Share Sheet.
 */
fun exportChatToTxtAndShare(
    context: Context,
    chatTitle: String,
    messages: List<ChatMessage>,
    participantNames: Map<String, String>,
    chooserTitle: String,
    emptyMessage: String
) {
    val activeMessages = messages.filter { !it.deleted }
    if (activeMessages.isEmpty()) {
        Toast.makeText(context, emptyMessage, Toast.LENGTH_SHORT).show()
        return
    }
    val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val builder = StringBuilder()
    builder.appendLine("=== Bangla Chat Transcript: $chatTitle ===")
    builder.appendLine("Exported: ${dateFmt.format(Date())}")
    builder.appendLine("Total Messages: ${activeMessages.size}")
    builder.appendLine("--------------------------------------------------")
    activeMessages.forEach { msg ->
        val time = msg.timestamp?.toDate()?.let { dateFmt.format(it) } ?: "--"
        val sender = msg.senderName.ifBlank { participantNames[msg.senderId] ?: "User" }
        val body = when (msg.type) {
            "image" -> if (msg.viewOnce) "[View Once Photo]" else "[Photo] ${msg.text}".trim()
            "audio" -> "[Voice Note ${msg.audioDurationSec}s]"
            "document" -> "[Document: ${msg.mediaName}] ${msg.text}".trim()
            "poll" -> "[Poll: ${msg.pollQuestion.ifBlank { msg.text }}] Options: ${msg.pollOptions.joinToString(", ")}"
            "sticker" -> "[Sticker] ${msg.text}"
            "location" -> "[Location: ${msg.locationLabel.ifBlank { "GPS" }} (${msg.latitude}, ${msg.longitude})]"
            "contact" -> "[Contact: ${msg.sharedContactName} @${msg.sharedContactUsername}]"
            "call" -> "[Call] ${msg.text}"
            else -> msg.text
        }
        builder.appendLine("[$time] $sender: $body")
    }

    runCatching {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = chatTitle.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(24).ifBlank { "chat" }
        val file = File(exportsDir, "BanglaChat_${safeName}_${System.currentTimeMillis()}.txt")
        file.writeText(builder.toString(), Charsets.UTF_8)

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Bangla Chat - $chatTitle")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    }.onFailure { err ->
        Toast.makeText(context, err.localizedMessage ?: "Export failed", Toast.LENGTH_SHORT).show()
    }
}
