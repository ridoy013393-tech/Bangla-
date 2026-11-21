package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.Chat
import com.example.model.PublicProfile
import com.example.model.StatusStory
import com.example.model.UiState
import com.example.ui.components.UserAvatar
import com.example.ui.components.VerifiedBadge
import com.example.ui.components.formatConversationTime
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.DangerRed
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.ReadTickBlue
import kotlinx.coroutines.delay

enum class ChatListFilter {
    ALL,
    UNREAD,
    GROUPS,
    FAVORITES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsTab(
    currentUserId: String,
    myProfile: PublicProfile?,
    chatsState: UiState<List<Chat>>,
    discoverableProfiles: List<PublicProfile>,
    activeStatuses: List<StatusStory>,
    favoriteUids: Set<String>,
    lockedChatIds: Set<String> = emptySet(),
    selectedFilter: ChatListFilter,
    searchQuery: String,
    isRefreshing: Boolean,
    showOnlineBanner: Boolean,
    onDismissOnlineBanner: () -> Unit,
    onRefresh: () -> Unit,
    onResetFilter: () -> Unit,
    onChatClick: (String) -> Unit,
    onTogglePinChat: (Chat) -> Unit,
    onToggleMuteChat: (Chat) -> Unit,
    onToggleArchiveChat: (Chat) -> Unit,
    onToggleLockChat: (Chat) -> Unit = {},
    onClearChat: (Chat) -> Unit = {},
    onDeleteChat: (Chat) -> Unit = {},
    onLeaveGroup: (Chat) -> Unit = {},
    onDeleteGroup: (Chat) -> Unit = {},
    onRequestUnlockChat: (Chat) -> Unit = { onChatClick(it.id) },
    onExploreContacts: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onAddStatusClick: () -> Unit,
    onOpenAuthorStatuses: (List<StatusStory>) -> Unit
) {
    val strings = LocalAppStrings.current
    val profileByUid = remember(discoverableProfiles) {
        discoverableProfiles.associateBy { it.uid }
    }
    var actionTargetChat by remember { mutableStateOf<Chat?>(null) }
    var pendingDeleteChat by remember { mutableStateOf<Chat?>(null) }
    var pendingClearChat by remember { mutableStateOf<Chat?>(null) }
    var pendingLeaveGroup by remember { mutableStateOf<Chat?>(null) }
    var pendingDeleteGroup by remember { mutableStateOf<Chat?>(null) }
    var showArchived by remember { mutableStateOf(false) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        when (chatsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = BrandBlue)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = strings.loadingText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = chatsState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(onClick = onRefresh) {
                            Text(strings.retry)
                        }
                    }
                }
            }

            is UiState.Success -> {
                val filteredAll = remember(chatsState.data, searchQuery, currentUserId) {
                    val visible = chatsState.data.filter { !it.isHiddenFor(currentUserId) }
                    val matched = if (searchQuery.isBlank()) {
                        visible
                    } else {
                        val q = searchQuery.trim().lowercase()
                        visible.filter { chat ->
                            chat.peerName(currentUserId).lowercase().contains(q) ||
                                chat.lastMessage.lowercase().contains(q)
                        }
                    }
                    // Pin chats first, preserving recency order within each group
                    matched.sortedByDescending { it.isPinnedFor(currentUserId) }
                }

                val activeChats = remember(filteredAll, currentUserId, selectedFilter, favoriteUids) {
                    val nonArchived = filteredAll.filter { !it.isArchivedFor(currentUserId) }
                    when (selectedFilter) {
                        ChatListFilter.ALL -> nonArchived
                        ChatListFilter.UNREAD -> nonArchived.filter { it.unreadFor(currentUserId) > 0 }
                        ChatListFilter.GROUPS -> nonArchived.filter { it.isGroup }
                        ChatListFilter.FAVORITES -> nonArchived.filter {
                            !it.isGroup && it.peerUid(currentUserId) in favoriteUids
                        }
                    }
                }
                val archivedChats = remember(filteredAll, currentUserId, selectedFilter) {
                    if (selectedFilter == ChatListFilter.ALL) {
                        filteredAll.filter { it.isArchivedFor(currentUserId) }
                    } else {
                        emptyList()
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("chats_list"),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    item(key = "status_stories") {
                        StatusStoriesStrip(
                            currentUserId = currentUserId,
                            myProfile = myProfile,
                            statuses = activeStatuses,
                            onAddStatusClick = onAddStatusClick,
                            onOpenAuthorStatuses = onOpenAuthorStatuses
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                    }

                    item(key = "online_banner") {
                        AnimatedVisibility(visible = showOnlineBanner) {
                            OnlineStatusGradientBanner(
                                title = strings.onlineBannerTitle,
                                subtitle = strings.onlineBannerSubtitle,
                                onDismiss = onDismissOnlineBanner,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }

                    if (archivedChats.isNotEmpty()) {
                        item(key = "archived_header") {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { showArchived = !showArchived }
                                    .testTag("archived_chats_toggle")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Archive,
                                        contentDescription = null,
                                        tint = BrandPurple
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "${strings.archivedChatsTitle} (${archivedChats.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = if (showArchived) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (showArchived) {
                            items(archivedChats, key = { "arch_${it.id}" }) { chat ->
                                val peerUid = chat.peerUid(currentUserId)
                                val peerProfile = if (chat.isGroup) null else profileByUid[peerUid]
                                val peerPhoto = if (chat.isGroup) chat.groupPhotoUrl
                                else peerProfile?.let { it.visiblePhotoUrl() }
                                    ?: chat.peerPhotoUrl(currentUserId)
                                val isPeerTyping = if (chat.isGroup) {
                                    chat.typingUids.any { it != currentUserId }
                                } else {
                                    peerUid.isNotBlank() && peerUid in chat.typingUids
                                }
                                val isLocked = chat.id in lockedChatIds
                                ConversationRow(
                                    chat = chat,
                                    currentUserId = currentUserId,
                                    peerName = if (chat.isGroup) chat.peerName(currentUserId)
                                    else peerProfile?.name?.ifBlank { chat.peerName(currentUserId) }
                                        ?: chat.peerName(currentUserId),
                                    peerPhoto = peerPhoto,
                                    peerOnline = !chat.isGroup && peerProfile?.isVisiblyOnline(nowMillis) == true,
                                    peerVerified = !chat.isGroup && peerProfile?.verified == true,
                                    isPeerTyping = isPeerTyping,
                                    isLocked = isLocked,
                                    onClick = {
                                        if (isLocked) onRequestUnlockChat(chat) else onChatClick(chat.id)
                                    },
                                    onLongClick = { actionTargetChat = chat }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 84.dp, end = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    if (activeChats.isEmpty() && archivedChats.isEmpty()) {
                        item(key = "empty_chats") {
                            val (emptyTitle, emptySubtitle, actionLabel, onAction) = when (selectedFilter) {
                                ChatListFilter.UNREAD -> Quadruple(
                                    strings.noUnreadChatsTitle,
                                    strings.noUnreadChatsSubtitle,
                                    strings.showAllChats,
                                    onResetFilter
                                )
                                ChatListFilter.GROUPS -> Quadruple(
                                    strings.noGroupChatsTitle,
                                    strings.noGroupChatsSubtitle,
                                    strings.createGroupAction,
                                    onCreateGroupClick
                                )
                                ChatListFilter.FAVORITES -> Quadruple(
                                    strings.noFavoriteChatsTitle,
                                    strings.noFavoriteChatsSubtitle,
                                    strings.startFirstChat,
                                    onExploreContacts
                                )
                                ChatListFilter.ALL -> Quadruple(
                                    strings.noChatsTitle,
                                    strings.noChatsSubtitle,
                                    strings.startFirstChat,
                                    onExploreContacts
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp, vertical = 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (selectedFilter == ChatListFilter.GROUPS) {
                                            Icons.Rounded.Group
                                        } else {
                                            Icons.Rounded.ChatBubbleOutline
                                        },
                                        contentDescription = null,
                                        tint = BrandBlue,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    text = emptyTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = emptySubtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = onAction,
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.testTag("empty_start_chat_button")
                                ) {
                                    Text(actionLabel)
                                }
                            }
                        }
                    } else {
                        items(activeChats, key = { it.id }) { chat ->
                            val peerUid = chat.peerUid(currentUserId)
                            val peerProfile = if (chat.isGroup) null else profileByUid[peerUid]
                            val peerOnline = !chat.isGroup && peerProfile?.isVisiblyOnline(nowMillis) == true
                            val isPeerTyping = if (chat.isGroup) {
                                chat.typingUids.any { it != currentUserId }
                            } else {
                                peerUid.isNotBlank() && peerUid in chat.typingUids
                            }
                            val peerPhoto = if (chat.isGroup) chat.groupPhotoUrl
                            else peerProfile?.let { it.visiblePhotoUrl() }
                                ?: chat.peerPhotoUrl(currentUserId)

                            val isLocked = chat.id in lockedChatIds
                            ConversationRow(
                                chat = chat,
                                currentUserId = currentUserId,
                                peerName = if (chat.isGroup) chat.peerName(currentUserId)
                                else peerProfile?.name?.ifBlank { chat.peerName(currentUserId) }
                                    ?: chat.peerName(currentUserId),
                                peerPhoto = peerPhoto,
                                peerOnline = peerOnline,
                                peerVerified = !chat.isGroup && peerProfile?.verified == true,
                                isPeerTyping = isPeerTyping,
                                isLocked = isLocked,
                                onClick = {
                                    if (isLocked) onRequestUnlockChat(chat) else onChatClick(chat.id)
                                },
                                onLongClick = { actionTargetChat = chat }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 84.dp, end = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Long-press Chat Management Sheet (Pin / Mute / Lock / Archive / Clear / Delete / Leave / Delete Group)
    actionTargetChat?.let { target ->
        val pinned = target.isPinnedFor(currentUserId)
        val muted = target.isMutedFor(currentUserId)
        val archived = target.isArchivedFor(currentUserId)
        val locked = target.id in lockedChatIds
        val canDeleteGroup = target.isGroup && (target.isAdmin(currentUserId) || target.participants == listOf(currentUserId))

        ModalBottomSheet(onDismissRequest = { actionTargetChat = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = target.peerName(currentUserId),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                ChatFlagOptionRow(
                    icon = Icons.Rounded.PushPin,
                    tint = BrandBlue,
                    label = if (pinned) strings.unpinChat else strings.pinChat,
                    onClick = {
                        onTogglePinChat(target)
                        actionTargetChat = null
                    }
                )
                ChatFlagOptionRow(
                    icon = if (muted) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                    tint = BrandPurple,
                    label = if (muted) strings.unmuteChat else strings.muteChat,
                    onClick = {
                        onToggleMuteChat(target)
                        actionTargetChat = null
                    }
                )
                ChatFlagOptionRow(
                    icon = if (locked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                    tint = BrandBlue,
                    label = if (locked) strings.unlockChatAction else strings.lockChatAction,
                    onClick = {
                        onToggleLockChat(target)
                        actionTargetChat = null
                    }
                )
                ChatFlagOptionRow(
                    icon = if (archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                    tint = MaterialTheme.colorScheme.onSurface,
                    label = if (archived) strings.unarchiveChat else strings.archiveChat,
                    onClick = {
                        onToggleArchiveChat(target)
                        actionTargetChat = null
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                ChatFlagOptionRow(
                    icon = Icons.Rounded.DeleteSweep,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    label = strings.clearChatAction,
                    testTag = "chat_action_clear_history",
                    onClick = {
                        pendingClearChat = target
                        actionTargetChat = null
                    }
                )
                if (target.isGroup) {
                    ChatFlagOptionRow(
                        icon = Icons.AutoMirrored.Rounded.ExitToApp,
                        tint = DangerRed,
                        label = strings.leaveGroup,
                        labelColor = DangerRed,
                        testTag = "chat_action_leave_group",
                        onClick = {
                            pendingLeaveGroup = target
                            actionTargetChat = null
                        }
                    )
                    if (canDeleteGroup) {
                        ChatFlagOptionRow(
                            icon = Icons.Rounded.DeleteForever,
                            tint = DangerRed,
                            label = strings.deleteGroupAction,
                            labelColor = DangerRed,
                            testTag = "chat_action_delete_group",
                            onClick = {
                                pendingDeleteGroup = target
                                actionTargetChat = null
                            }
                        )
                    }
                } else {
                    ChatFlagOptionRow(
                        icon = Icons.Rounded.Delete,
                        tint = DangerRed,
                        label = strings.deleteChatAction,
                        labelColor = DangerRed,
                        testTag = "chat_action_delete_chat",
                        onClick = {
                            pendingDeleteChat = target
                            actionTargetChat = null
                        }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    pendingClearChat?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingClearChat = null },
            title = { Text(strings.clearChatConfirmTitle, fontWeight = FontWeight.Bold) },
            text = { Text(strings.clearChatConfirmMessage) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    onClick = {
                        onClearChat(target)
                        pendingClearChat = null
                    }
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingClearChat = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    pendingDeleteChat?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteChat = null },
            title = { Text(strings.deleteChatConfirmTitle, fontWeight = FontWeight.Bold) },
            text = { Text(strings.deleteChatConfirmMessage) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    onClick = {
                        onDeleteChat(target)
                        pendingDeleteChat = null
                    },
                    modifier = Modifier.testTag("confirm_delete_chat_button")
                ) {
                    Text(strings.deleteChatAction)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteChat = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    pendingLeaveGroup?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingLeaveGroup = null },
            title = { Text(strings.leaveGroupConfirmTitle, fontWeight = FontWeight.Bold) },
            text = { Text(strings.leaveGroupConfirmMessage) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    onClick = {
                        onLeaveGroup(target)
                        pendingLeaveGroup = null
                    }
                ) {
                    Text(strings.leaveGroup)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingLeaveGroup = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    pendingDeleteGroup?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteGroup = null },
            title = { Text(strings.deleteGroupConfirmTitle, fontWeight = FontWeight.Bold) },
            text = { Text(strings.deleteGroupConfirmMessage) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    onClick = {
                        onDeleteGroup(target)
                        pendingDeleteGroup = null
                    }
                ) {
                    Text(strings.deleteGroupAction)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteGroup = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun ChatFlagOptionRow(
    icon: ImageVector,
    tint: Color,
    label: String,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    testTag: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .let { if (testTag != null) it.testTag(testTag) else it },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = labelColor)
    }
}

@Composable
private fun OnlineStatusGradientBanner(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF4F46E5), BrandPurple)
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(gradient)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(OnlineGreen)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFFDE047),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.92f)
                )
            }
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("dismiss_online_banner_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Dismiss",
                tint = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationRow(
    chat: Chat,
    currentUserId: String,
    peerName: String,
    peerPhoto: String,
    peerOnline: Boolean,
    peerVerified: Boolean = false,
    isPeerTyping: Boolean,
    isLocked: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val unread = chat.unreadFor(currentUserId)
    val sentByMe = chat.lastSenderId == currentUserId
    val isPinned = chat.isPinnedFor(currentUserId)
    val isMuted = chat.isMutedFor(currentUserId)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("chat_row_${chat.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (chat.isGroup && peerPhoto.isBlank()) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(BrandBlue, BrandPurple))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Group,
                    contentDescription = peerName,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        } else {
            UserAvatar(
                name = peerName,
                photoUrl = peerPhoto,
                size = 56.dp,
                showOnlineIndicator = !chat.isGroup,
                isOnline = peerOnline
            )
        }
        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (chat.isGroup) {
                        Icon(
                            imageVector = Icons.Rounded.Group,
                            contentDescription = null,
                            tint = BrandPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                    }
                    if (isLocked) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = strings.lockChatAction,
                            tint = BrandBlue,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = peerName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (unread > 0) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (peerVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        VerifiedBadge(
                            contentDescription = strings.verifiedBadgeDesc,
                            size = 16.dp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatConversationTime(chat.lastMessageTime ?: chat.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (unread > 0) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLocked) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = strings.lockedChatPreview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else if (isPeerTyping) {
                    Text(
                        text = strings.typingIndicator,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = BrandPurple,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val clearedMs = chat.clearedAtMillisFor(currentUserId)
                        val lastMsgMs = chat.lastMessageTime?.toDate()?.time ?: 0L
                        val isPreviewCleared = clearedMs > 0L && lastMsgMs <= clearedMs

                        if (!isPreviewCleared) {
                            when (chat.lastMessageType) {
                                "image" -> {
                                    Icon(
                                        imageVector = Icons.Rounded.Photo,
                                        contentDescription = null,
                                        tint = BrandBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                "document" -> {
                                    Icon(
                                        imageVector = Icons.Rounded.AttachFile,
                                        contentDescription = null,
                                        tint = BrandPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                "audio" -> {
                                    Icon(
                                        imageVector = Icons.Rounded.Mic,
                                        contentDescription = null,
                                        tint = OnlineGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                "poll" -> {
                                    Icon(
                                        imageVector = Icons.Rounded.Poll,
                                        contentDescription = null,
                                        tint = BrandPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                "call" -> {
                                    Icon(
                                        imageVector = Icons.Rounded.Call,
                                        contentDescription = null,
                                        tint = OnlineGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                "system" -> {
                                    Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription = null,
                                        tint = BrandBlue,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                            }
                        }

                        val previewText = when {
                            isPreviewCleared -> strings.chatStarted
                            chat.lastMessageType == "image" && (chat.lastMessage.isBlank() || chat.lastMessage == "📷") ->
                                strings.photoMessagePreview
                            chat.lastMessageType == "document" && chat.lastMessage == "📎" ->
                                strings.documentMessagePreview
                            chat.lastMessageType == "audio" && (chat.lastMessage.isBlank() || chat.lastMessage == "🎤") ->
                                strings.voiceMessagePreview
                            chat.lastMessage.isBlank() -> strings.chatStarted
                            else -> chat.lastMessage
                        }

                        Text(
                            text = previewText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (unread > 0) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (unread > 0) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isPinned) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = strings.pinChat,
                        tint = BrandBlue.copy(alpha = 0.75f),
                        modifier = Modifier.size(15.dp)
                    )
                }
                if (isMuted) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.NotificationsOff,
                        contentDescription = strings.muteChat,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (unread > 0) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) MaterialTheme.colorScheme.onSurfaceVariant else BrandBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unread > 99) "99+" else unread.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                } else if (sentByMe && chat.lastMessage.isNotBlank()) {
                    val tickIcon = if (chat.lastMessageRead || chat.lastMessageDelivered) {
                        Icons.Rounded.DoneAll
                    } else {
                        Icons.Rounded.Done
                    }
                    val tickTint = if (chat.lastMessageRead) {
                        ReadTickBlue
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Icon(
                        imageVector = tickIcon,
                        contentDescription = when {
                            chat.lastMessageRead -> strings.messageStatusRead
                            chat.lastMessageDelivered -> strings.messageStatusDelivered
                            else -> strings.messageStatusSentOffline
                        },
                        tint = tickTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
