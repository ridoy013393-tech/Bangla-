package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.PublicProfile
import com.example.model.StatusStory
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatConversationTime
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import kotlinx.coroutines.delay

private val STATUS_BG_COLORS = listOf(
    0xFF2563EBL,
    0xFF7C3AEDL,
    0xFF059669L,
    0xFFDB2777L,
    0xFFEA580CL,
    0xFF0F172AL
)

@Composable
fun StatusStoriesStrip(
    currentUserId: String,
    myProfile: PublicProfile?,
    statuses: List<StatusStory>,
    onAddStatusClick: () -> Unit,
    onOpenAuthorStatuses: (List<StatusStory>) -> Unit
) {
    val strings = LocalAppStrings.current
    val groupedByAuthor = remember(statuses, currentUserId) {
        val grouped = statuses.groupBy { it.uid }
        val mine = grouped[currentUserId]
        val others = grouped.filterKeys { it != currentUserId }.values.toList()
        buildList {
            if (!mine.isNullOrEmpty()) add(mine)
            addAll(others)
        }
    }
    val myStories = groupedByAuthor.firstOrNull { it.firstOrNull()?.uid == currentUserId }.orEmpty()

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("status_stories_strip")
    ) {
        // First item: My Status (tap to view own or add new; '+' always adds)
        item(key = "my_status_tile") {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(68.dp)
                    .clickable {
                        if (myStories.isNotEmpty()) onOpenAuthorStatuses(myStories)
                        else onAddStatusClick()
                    }
                    .testTag("my_status_button")
            ) {
                Box(modifier = Modifier.size(60.dp)) {
                    val ringBrush = if (myStories.isNotEmpty()) {
                        Brush.linearGradient(listOf(BrandBlue, BrandPurple))
                    } else {
                        Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.4f), Color.Gray.copy(alpha = 0.4f)))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(2.5.dp, ringBrush, CircleShape)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        UserAvatar(
                            name = myProfile?.name.orEmpty().ifBlank { "Me" },
                            photoUrl = myProfile?.photoUrl.orEmpty(),
                            size = 50.dp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(BrandBlue)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .clickable(onClick = onAddStatusClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = strings.addStatus,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.myStatus,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Other friends' 24h status stories
        val friendGroups = groupedByAuthor.filter { it.firstOrNull()?.uid != currentUserId }
        items(friendGroups, key = { it.first().uid }) { authorStories ->
            val latest = authorStories.first()
            val hasUnviewed = authorStories.any { currentUserId !in it.viewerUids }
            val ringBrush = if (hasUnviewed) {
                Brush.linearGradient(listOf(BrandBlue, BrandPurple, Color(0xFFEC4899)))
            } else {
                Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.45f), Color.Gray.copy(alpha = 0.45f)))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(68.dp)
                    .clickable { onOpenAuthorStatuses(authorStories) }
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, ringBrush, CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    UserAvatar(
                        name = latest.authorName,
                        photoUrl = latest.authorPhotoUrl,
                        size = 50.dp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = latest.authorName.substringBefore(" "),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (hasUnviewed) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CreateStatusDialog(
    isPosting: Boolean,
    onPostText: (String, Long) -> Unit,
    onPostPhoto: (Uri, String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var isPhotoTab by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var selectedColor by remember { mutableLongStateOf(STATUS_BG_COLORS.first()) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) selectedImageUri = uri }

    AlertDialog(
        onDismissRequest = { if (!isPosting) onDismiss() },
        title = { Text(strings.addStatus, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isPhotoTab,
                        onClick = { isPhotoTab = false },
                        label = { Text(strings.textStatusTab) }
                    )
                    FilterChip(
                        selected = isPhotoTab,
                        onClick = { isPhotoTab = true },
                        label = { Text(strings.photoStatusTab) }
                    )
                }

                if (!isPhotoTab) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(selectedColor))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = text.ifBlank { strings.statusTextHint },
                            color = Color.White.copy(alpha = if (text.isBlank()) 0.65f else 1f),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        STATUS_BG_COLORS.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .then(
                                        if (selectedColor == hex) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { selectedColor = hex }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.take(500) },
                        placeholder = { Text(strings.statusTextHint) },
                        maxLines = 3,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.pickFromGallery)
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.take(300) },
                        placeholder = { Text(strings.statusTextHint) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isPosting) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(strings.uploadingMedia, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isPosting && (if (isPhotoTab) selectedImageUri != null else text.trim().isNotEmpty()),
                onClick = {
                    if (isPhotoTab) {
                        selectedImageUri?.let { uri ->
                            onPostPhoto(uri, text.trim())
                            onDismiss()
                        }
                    } else {
                        onPostText(text.trim(), selectedColor)
                        onDismiss()
                    }
                }
            ) {
                Text(strings.postStatusAction)
            }
        },
        dismissButton = {
            TextButton(enabled = !isPosting, onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun StatusViewerDialog(
    stories: List<StatusStory>,
    currentUserId: String,
    onMarkViewed: (StatusStory) -> Unit,
    onDeleteStatus: (StatusStory) -> Unit,
    onAddAnother: () -> Unit,
    onDismiss: () -> Unit
) {
    if (stories.isEmpty()) {
        onDismiss()
        return
    }
    val strings = LocalAppStrings.current
    var currentIndex by remember { mutableIntStateOf(0) }
    val safeIndex = currentIndex.coerceIn(0, stories.lastIndex)
    val story = stories[safeIndex]
    var progress by remember(story.id) { mutableFloatStateOf(0f) }

    LaunchedEffect(story.id) {
        onMarkViewed(story)
        progress = 0f
        val steps = 50
        for (i in 1..steps) {
            delay(100L)
            progress = i / steps.toFloat()
        }
        if (safeIndex < stories.lastIndex) {
            currentIndex = safeIndex + 1
        } else {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (story.type == "image") Color.Black
                    else Color(story.backgroundColorHex)
                )
                .pointerInput(stories.size, safeIndex) {
                    detectTapGestures { offset ->
                        if (offset.x < size.width * 0.35f) {
                            if (safeIndex > 0) currentIndex = safeIndex - 1
                        } else {
                            if (safeIndex < stories.lastIndex) currentIndex = safeIndex + 1
                            else onDismiss()
                        }
                    }
                }
        ) {
            if (story.type == "image" && story.mediaUrl.isNotBlank()) {
                AsyncImage(
                    model = story.mediaUrl,
                    contentDescription = story.text.ifBlank { strings.myStatus },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                if (story.text.isNotBlank()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.58f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 20.dp, vertical = 76.dp)
                    ) {
                        Text(
                            text = story.text,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = story.text,
                    color = Color.White,
                    fontSize = 26.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 28.dp)
                )
            }

            // Top segmented progress + header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stories.forEachIndexed { idx, _ ->
                        val segProgress = when {
                            idx < safeIndex -> 1f
                            idx == safeIndex -> progress
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { segProgress },
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        name = story.authorName,
                        photoUrl = story.authorPhotoUrl,
                        size = 40.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = story.authorName,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatConversationTime(story.createdAt),
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    if (story.uid == currentUserId) {
                        IconButton(onClick = {
                            onDismiss()
                            onAddAnother()
                        }) {
                            Icon(Icons.Rounded.Add, contentDescription = strings.addStatus, tint = Color.White)
                        }
                        IconButton(onClick = {
                            onDeleteStatus(story)
                            onDismiss()
                        }) {
                            Icon(Icons.Rounded.Delete, contentDescription = strings.deleteStatus, tint = Color.White)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = strings.close, tint = Color.White)
                    }
                }
            }

            // Bottom viewer count
            Surface(
                color = Color.Black.copy(alpha = 0.45f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Visibility,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val viewerCount = (story.viewerUids.size - 1).coerceAtLeast(0)
                    Text(
                        text = "$viewerCount ${strings.statusViewersLabel}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
