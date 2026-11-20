package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CallFilter
import com.example.model.CallLog
import com.example.model.PublicProfile
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatConversationTime
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.DangerRed
import com.example.ui.theme.OnlineGreen

@Composable
fun CallsTab(
    callLogs: List<CallLog>,
    discoverableProfiles: List<PublicProfile>,
    onStartCall: (PublicProfile, String) -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedFilter by remember { mutableStateOf(CallFilter.ALL) }

    val profileByUid = remember(discoverableProfiles) {
        discoverableProfiles.associateBy { it.uid }
    }

    val filtered = remember(callLogs, selectedFilter) {
        when (selectedFilter) {
            CallFilter.ALL -> callLogs
            CallFilter.MISSED -> callLogs.filter { it.direction == "missed" }
            CallFilter.INCOMING -> callLogs.filter { it.direction == "incoming" }
            CallFilter.OUTGOING -> callLogs.filter { it.direction == "outgoing" }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calls_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Live WebRTC HD Audio & Video Calling Ready Banner
        item(key = "live_calls_banner") {
            val bannerGradient = Brush.horizontalGradient(
                colors = listOf(BrandBlue, BrandPurple)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(bannerGradient)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Videocam,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.liveCallsBannerTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strings.liveCallsBannerDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // Quick Call Friends Horizontal Strip
        if (discoverableProfiles.isNotEmpty()) {
            item(key = "quick_call_strip") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.quickCallFriends,
                        style = MaterialTheme.typography.labelLarge,
                        color = BrandBlue,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(discoverableProfiles.take(15), key = { it.uid }) { peer ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp,
                                shadowElevation = 1.dp,
                                modifier = Modifier.width(126.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    UserAvatar(
                                        name = peer.name,
                                        photoUrl = peer.visiblePhotoUrl(),
                                        size = 48.dp,
                                        showOnlineIndicator = true,
                                        isOnline = peer.showOnlineStatus && peer.online
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = peer.name.substringBefore(" "),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        IconButton(
                                            onClick = { onStartCall(peer, "audio") },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(OnlineGreen.copy(alpha = 0.16f))
                                                .testTag("quick_voice_call_${peer.uid}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Call,
                                                contentDescription = strings.voiceCallAction,
                                                tint = OnlineGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onStartCall(peer, "video") },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(BrandBlue.copy(alpha = 0.16f))
                                                .testTag("quick_video_call_${peer.uid}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Videocam,
                                                contentDescription = strings.videoCallAction,
                                                tint = BrandBlue,
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

        // Filter Chips: All, Missed, Incoming, Outgoing
        item(key = "call_filters") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val options = listOf(
                    CallFilter.ALL to strings.callsFilterAll,
                    CallFilter.MISSED to strings.callsFilterMissed,
                    CallFilter.INCOMING to strings.callsFilterIncoming,
                    CallFilter.OUTGOING to strings.callsFilterOutgoing
                )
                items(options) { (filter, label) ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(label) },
                        modifier = Modifier.testTag("call_filter_${filter.name.lowercase()}")
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item(key = "calls_empty") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = strings.noCallHistoryTitle,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.noCallHistorySubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { log ->
                val resolvedPeer = profileByUid[log.peerUid] ?: PublicProfile(
                    uid = log.peerUid,
                    name = log.peerName,
                    photoUrl = log.peerPhotoUrl
                )
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            name = log.peerName,
                            photoUrl = log.peerPhotoUrl,
                            size = 48.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = log.peerName,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (log.direction == "missed") DangerRed
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val (dirIcon, dirTint) = when (log.direction) {
                                    "missed" -> Icons.AutoMirrored.Rounded.CallMissed to DangerRed
                                    "incoming" -> Icons.AutoMirrored.Rounded.CallReceived to OnlineGreen
                                    else -> Icons.AutoMirrored.Rounded.CallMade to BrandBlue
                                }
                                Icon(
                                    imageVector = dirIcon,
                                    contentDescription = log.direction,
                                    tint = dirTint,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                val durationSuffix = if (log.durationSeconds > 0L) {
                                    val mins = log.durationSeconds / 60L
                                    val secs = log.durationSeconds % 60L
                                    " • ${String.format("%02d:%02d", mins, secs)}"
                                } else ""
                                Text(
                                    text = "${formatConversationTime(log.createdAt)}$durationSuffix",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (log.peerUid.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    onStartCall(
                                        resolvedPeer,
                                        if (log.callType == "video") "video" else "audio"
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = if (log.callType == "video") Icons.Rounded.Videocam else Icons.Rounded.Call,
                                    contentDescription = if (log.callType == "video") strings.videoCallAction else strings.voiceCallAction,
                                    tint = if (log.callType == "video") BrandBlue else OnlineGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
