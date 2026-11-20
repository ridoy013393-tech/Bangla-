package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ChatMessage
import com.example.model.ChatWallpaper
import com.example.model.PublicProfile
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatFullDate
import com.example.ui.components.formatMessageTime
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.DangerRed

fun chatWallpaperBrush(wallpaper: ChatWallpaper, isDark: Boolean, fallbackBg: Color): Brush {
    return when (wallpaper) {
        ChatWallpaper.DEFAULT -> Brush.verticalGradient(listOf(fallbackBg, fallbackBg))
        ChatWallpaper.MINT -> if (isDark) {
            Brush.verticalGradient(listOf(Color(0xFF0A1F1C), Color(0xFF0F2925)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFECFDF5), Color(0xFFD1FAE5)))
        }
        ChatWallpaper.OCEAN -> if (isDark) {
            Brush.verticalGradient(listOf(Color(0xFF09192B), Color(0xFF0E253D)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE)))
        }
        ChatWallpaper.VIOLET -> if (isDark) {
            Brush.verticalGradient(listOf(Color(0xFF1A102B), Color(0xFF25153D)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFF5F3FF), Color(0xFFEDE9FE)))
        }
        ChatWallpaper.PEACH -> if (isDark) {
            Brush.verticalGradient(listOf(Color(0xFF241410), Color(0xFF331B16)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5)))
        }
        ChatWallpaper.MIDNIGHT -> Brush.verticalGradient(
            listOf(Color(0xFF090D16), Color(0xFF111827))
        )
    }
}

@Composable
fun DisappearingMessagesDialog(
    currentHours: Long,
    onSelectHours: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val options = listOf(
        0L to strings.disappearingOff,
        24L to strings.disappearing24h,
        168L to strings.disappearing7d
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.disappearingMessages) },
        text = {
            Column {
                options.forEach { (hours, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectHours(hours)
                                onDismiss()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentHours == hours,
                            onClick = {
                                onSelectHours(hours)
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.close)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarredMessagesSheet(
    starredMessages: List<ChatMessage>,
    onJumpToMessage: (ChatMessage) -> Unit,
    onUnstar: (ChatMessage) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "${strings.starredMessagesTitle} (${starredMessages.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (starredMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.noStarredMessages,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    items(starredMessages, key = { it.id }) { msg ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onJumpToMessage(msg)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = msg.text.ifBlank {
                                            msg.mediaName.ifBlank { strings.photoMessagePreview }
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatMessageTime(msg.timestamp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onUnstar(msg) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = strings.unstarMessage,
                                        tint = Color(0xFFF59E0B)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPeerProfileSheet(
    peerName: String,
    peerPhoto: String,
    peerProfile: PublicProfile?,
    isPeerBlocked: Boolean,
    onBlockToggle: () -> Unit,
    onReport: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UserAvatar(
                name = peerName,
                photoUrl = peerPhoto,
                size = 84.dp,
                showOnlineIndicator = true,
                isOnline = peerProfile?.showOnlineStatus == true && peerProfile.online
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = peerName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (!peerProfile?.username.isNullOrBlank()) {
                Text(
                    text = "@${peerProfile?.username.orEmpty()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandBlue
                )
            }
            val visibleBio = peerProfile?.visibleAbout().orEmpty()
            if (visibleBio.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = visibleBio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (peerProfile?.createdAt != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${strings.accountCreatedLabel}: ${formatFullDate(peerProfile.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onBlockToggle()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isPeerBlocked) strings.unblockUser else strings.blockUser)
                }
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onReport()
                    },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Rounded.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.reportUser)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
