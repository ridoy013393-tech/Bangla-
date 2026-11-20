package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Forward
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.VoiceNotePlayer
import com.example.data.VoiceNoteRecorder
import com.example.data.VoicePlaybackState
import com.example.model.Chat
import com.example.model.ChatMessage
import com.example.model.PublicProfile
import com.example.model.UiState
import com.example.model.UploadState
import com.example.model.UserPreferences
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatConversationTime
import com.example.ui.components.formatFileSize
import com.example.ui.components.formatMessageTime
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.DangerRed
import com.example.ui.theme.IncomingBubbleDark
import com.example.ui.theme.IncomingBubbleLight
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.ReadTickBlue
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.io.File

private val QUICK_REACTIONS = listOf("❤️", "👍", "😂", "😮", "😢", "🙏")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    myProfile: PublicProfile?,
    preferences: UserPreferences,
    allChats: List<Chat>,
    discoverableContacts: List<PublicProfile>,
    blockedUids: Set<String>,
    onBack: () -> Unit,
    onStartVoiceCall: (PublicProfile) -> Unit,
    onStartVideoCall: (PublicProfile) -> Unit,
    onBlockPeer: (PublicProfile) -> Unit,
    onUnblockPeer: (String) -> Unit,
    onReportPeer: (String, String, String) -> Unit
) {
    BackHandler(onBack = onBack)

    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val chat by viewModel.chat.collectAsStateWithLifecycle()
    val peerProfile by viewModel.peerProfile.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val pageLimit by viewModel.pageLimit.collectAsStateWithLifecycle()
    val replyTarget by viewModel.replyTarget.collectAsStateWithLifecycle()
    val editingMessage by viewModel.editingMessage.collectAsStateWithLifecycle()
    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()
    val translatedMessages by viewModel.translatedMessages.collectAsStateWithLifecycle()
    val translatingMessageIds by viewModel.translatingMessageIds.collectAsStateWithLifecycle()
    val quickReplies by viewModel.quickReplies.collectAsStateWithLifecycle()
    val quickRepliesLoading by viewModel.quickRepliesLoading.collectAsStateWithLifecycle()
    val summaryState by viewModel.summaryState.collectAsStateWithLifecycle()

    val voiceRecorder = remember(context) { VoiceNoteRecorder(context) }
    val voicePlayer = remember { VoiceNotePlayer() }
    val isRecording by voiceRecorder.isRecording.collectAsStateWithLifecycle()
    val recordingSec by voiceRecorder.elapsedSeconds.collectAsStateWithLifecycle()
    val recordingAmp by voiceRecorder.amplitude.collectAsStateWithLifecycle()
    val voicePlayback by voicePlayer.state.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            voiceRecorder.cancelRecording()
            voicePlayer.stop()
        }
    }

    var messageText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showAttachSheet by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var forwardingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showPeerProfileSheet by remember { mutableStateOf(false) }
    var showGroupInfoSheet by remember { mutableStateOf(false) }
    var showStarredSheet by remember { mutableStateOf(false) }
    var showDisappearingDialog by remember { mutableStateOf(false) }
    var showAiSummarySheet by remember { mutableStateOf(false) }
    var inChatSearchOpen by remember { mutableStateOf(false) }
    var inChatSearchQuery by remember { mutableStateOf("") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val effectiveMyProfile = myProfile ?: PublicProfile(
        uid = viewModel.currentUserId,
        name = "Bangla Chat User"
    )
    val isGroupChat = chat?.isGroup == true
    val peerUid = chat?.peerUid(viewModel.currentUserId).orEmpty()
    val peerName = if (isGroupChat) {
        chat?.groupName?.ifBlank { strings.newGroup } ?: strings.newGroup
    } else {
        peerProfile?.name?.ifBlank { chat?.peerName(viewModel.currentUserId).orEmpty() }
            ?: chat?.peerName(viewModel.currentUserId).orEmpty().ifBlank { "Bangla Chat" }
    }
    val peerPhoto = if (isGroupChat) {
        chat?.groupPhotoUrl.orEmpty()
    } else {
        peerProfile?.let { it.visiblePhotoUrl() }
            ?: chat?.peerPhotoUrl(viewModel.currentUserId).orEmpty()
    }
    val isPeerBlocked = !isGroupChat && peerUid.isNotBlank() && peerUid in blockedUids
    val isPeerTyping = if (isGroupChat) {
        chat?.typingUids?.any { it != viewModel.currentUserId } == true
    } else {
        peerUid.isNotBlank() && chat?.typingUids?.contains(peerUid) == true
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voicePlayer.stop()
            voiceRecorder.startRecording(scope).onFailure { err ->
                scope.launch {
                    snackbarHostState.showSnackbar(err.localizedMessage ?: strings.micPermissionRequired)
                }
            }
        } else {
            scope.launch { snackbarHostState.showSnackbar(strings.micPermissionRequired) }
        }
    }

    fun startVoiceNoteCapture() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            voicePlayer.stop()
            voiceRecorder.startRecording(scope).onFailure { err ->
                scope.launch {
                    snackbarHostState.showSnackbar(err.localizedMessage ?: strings.micPermissionRequired)
                }
            }
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Zero-permission Photo Picker for Gallery Images
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadAndSendMedia(effectiveMyProfile, uri, isImage = true)
        }
    }

    // Document / PDF picker via Storage Access Framework (zero broad storage permissions)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadAndSendMedia(effectiveMyProfile, uri, isImage = false)
        }
    }

    // Camera photo capture via FileProvider
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            viewModel.uploadAndSendMedia(effectiveMyProfile, uri, isImage = true)
        }
        pendingCameraUri = null
    }

    fun launchCameraCapture() {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingCameraUri = uri
        takePictureLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraCapture()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val rawMessages = (messagesState as? UiState.Success)?.data.orEmpty()
    val messages = remember(rawMessages) {
        val now = System.currentTimeMillis()
        rawMessages.filter { !it.isExpiredNow(now) }
    }
    val searchMatchIds = remember(messages, inChatSearchQuery) {
        val q = inChatSearchQuery.trim().lowercase()
        if (q.isBlank()) emptySet()
        else messages.filter { !it.deleted && (it.text.lowercase().contains(q) || it.mediaName.lowercase().contains(q)) }
            .map { it.id }
            .toSet()
    }
    val starredMessages = remember(messages, viewModel.currentUserId) {
        messages.filter { !it.deleted && it.isStarredFor(viewModel.currentUserId) }
    }

    LaunchedEffect(messages.size, chat?.id) {
        if (messages.isNotEmpty() && !inChatSearchOpen) {
            viewModel.markVisibleMessagesRead(myProfile, messages)
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val statusText = when {
        isPeerTyping -> strings.typingIndicator
        isGroupChat -> "${chat?.participants?.size ?: 0} ${strings.groupMembersSuffix}"
        peerProfile?.showOnlineStatus == true && peerProfile?.online == true -> strings.onlineStatus
        peerProfile?.showLastSeen == true && peerProfile?.lastSeen != null ->
            "${strings.lastSeenPrefix} ${formatConversationTime(peerProfile?.lastSeen)}"
        else -> strings.offlineStatus
    }

    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val wallpaperBrush = chatWallpaperBrush(
        wallpaper = preferences.chatWallpaper,
        isDark = isDark,
        fallbackBg = MaterialTheme.colorScheme.background
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("chat_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (isGroupChat) showGroupInfoSheet = true
                                    else showPeerProfileSheet = true
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isGroupChat && peerPhoto.isBlank()) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(BrandBlue, BrandPurple))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Group,
                                        contentDescription = peerName,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else {
                                UserAvatar(
                                    name = peerName,
                                    photoUrl = peerPhoto,
                                    size = 42.dp,
                                    showOnlineIndicator = !isGroupChat,
                                    isOnline = !isGroupChat && peerProfile?.showOnlineStatus == true && peerProfile?.online == true
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = peerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        isPeerTyping -> BrandPurple
                                        statusText == strings.onlineStatus -> OnlineGreen
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }

                        // 1-on-1 Live Voice & Video Call Buttons
                        if (!isGroupChat && peerUid.isNotBlank() && !isPeerBlocked) {
                            val callablePeer = peerProfile ?: PublicProfile(
                                uid = peerUid,
                                name = peerName,
                                photoUrl = peerPhoto
                            )
                            IconButton(
                                onClick = { onStartVoiceCall(callablePeer) },
                                modifier = Modifier.testTag("chat_voice_call_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Call,
                                    contentDescription = strings.voiceCallAction,
                                    tint = OnlineGreen
                                )
                            }
                            IconButton(
                                onClick = { onStartVideoCall(callablePeer) },
                                modifier = Modifier.testTag("chat_video_call_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Videocam,
                                    contentDescription = strings.videoCallAction,
                                    tint = BrandBlue
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                inChatSearchOpen = !inChatSearchOpen
                                if (!inChatSearchOpen) inChatSearchQuery = ""
                            },
                            modifier = Modifier.testTag("chat_search_button")
                        ) {
                            Icon(
                                imageVector = if (inChatSearchOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                                contentDescription = strings.searchInChat
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.testTag("chat_overflow_button")
                            ) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(if (isGroupChat) strings.groupInfoTitle else strings.viewProfile) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isGroupChat) Icons.Rounded.Group else Icons.Rounded.Person,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        if (isGroupChat) showGroupInfoSheet = true
                                        else showPeerProfileSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.aiSummarizeChatAction) },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = BrandPurple)
                                    },
                                    onClick = {
                                        showMenu = false
                                        showAiSummarySheet = true
                                        viewModel.summarizeConversation(messages, preferences.language)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("${strings.starredMessagesTitle} (${starredMessages.size})") },
                                    leadingIcon = { Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFF59E0B)) },
                                    onClick = {
                                        showMenu = false
                                        showStarredSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.disappearingMessages) },
                                    leadingIcon = { Icon(Icons.Rounded.Timer, contentDescription = null, tint = BrandPurple) },
                                    onClick = {
                                        showMenu = false
                                        showDisappearingDialog = true
                                    }
                                )
                                if (!isGroupChat && peerUid.isNotBlank()) {
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text(if (isPeerBlocked) strings.unblockUser else strings.blockUser) },
                                        leadingIcon = { Icon(Icons.Rounded.Block, contentDescription = null) },
                                        onClick = {
                                            showMenu = false
                                            if (isPeerBlocked) {
                                                onUnblockPeer(peerUid)
                                            } else {
                                                onBlockPeer(
                                                    peerProfile ?: PublicProfile(uid = peerUid, name = peerName)
                                                )
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(strings.reportUser) },
                                        leadingIcon = { Icon(Icons.Rounded.Flag, contentDescription = null) },
                                        onClick = {
                                            showMenu = false
                                            showReportDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // In-Chat Search Input
                    if (inChatSearchOpen) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            OutlinedTextField(
                                value = inChatSearchQuery,
                                onValueChange = { inChatSearchQuery = it },
                                placeholder = { Text(strings.searchInChatHint) },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(22.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("in_chat_search_input")
                            )
                            if (inChatSearchQuery.isNotBlank()) {
                                Text(
                                    text = "${searchMatchIds.size} ${strings.matchesFound}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandBlue,
                                    modifier = Modifier.padding(start = 10.dp, top = 4.dp)
                                )
                            }
                        }
                    }

                    // Pinned Message Banner
                    val pinnedText = chat?.pinnedMessageText.orEmpty()
                    if (pinnedText.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val idx = messages.indexOfFirst { it.id == chat?.pinnedMessageId }
                                    if (idx >= 0) {
                                        scope.launch { listState.animateScrollToItem(idx) }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PushPin,
                                    contentDescription = null,
                                    tint = BrandBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${strings.pinnedMessageLabel}${
                                            chat?.pinnedMessageSender?.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty()
                                        }",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = pinnedText,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.unpinMessage() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = strings.unpinMessage,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Active Disappearing Messages Banner
                    val disappearing = chat?.disappearingHours ?: 0L
                    if (disappearing > 0L) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BrandPurple.copy(alpha = 0.12f))
                                .clickable { showDisappearingDialog = true }
                                .padding(horizontal = 14.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = BrandPurple,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (disappearing == 24L) strings.disappearingBanner24h else strings.disappearingBanner7d,
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandPurple,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Upload Progress or Retry Banner
                    when (val u = uploadState) {
                        is UploadState.Uploading -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${strings.uploadingMedia} ${(u.progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandBlue
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { u.progress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        is UploadState.Failed -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DangerRed.copy(alpha = 0.12f))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${strings.uploadFailedRetry}: ${u.message}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DangerRed,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2
                                )
                                IconButton(onClick = { viewModel.retryLastUpload(effectiveMyProfile) }) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = strings.retry, tint = DangerRed)
                                }
                                IconButton(onClick = { viewModel.dismissUploadError() }) {
                                    Icon(Icons.Rounded.Close, contentDescription = strings.close, tint = DangerRed)
                                }
                            }
                        }
                        UploadState.Idle -> Unit
                    }

                    // Active Edit Message Strip
                    editingMessage?.let { editing ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BrandPurple.copy(alpha = 0.14f))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                tint = BrandPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.editingMessage,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandPurple,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = editing.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.setEditingMessage(null)
                                    messageText = ""
                                }
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = strings.cancel)
                            }
                        }
                    }

                    // Active Reply Preview Strip
                    replyTarget?.let { reply ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Reply,
                                contentDescription = null,
                                tint = BrandBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.replyingTo,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandBlue,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = reply.text.ifBlank { reply.mediaName.ifBlank { strings.photoMessagePreview } },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { viewModel.setReplyTarget(null) }) {
                                Icon(Icons.Rounded.Close, contentDescription = strings.cancel)
                            }
                        }
                    }

                    if (isPeerBlocked) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DangerRed.copy(alpha = 0.1f))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.blockedChatBanner,
                                style = MaterialTheme.typography.bodySmall,
                                color = DangerRed
                            )
                        }
                    } else if (isRecording) {
                        // Active Voice Note Recording Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DangerRed.copy(alpha = 0.08f))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("voice_recording_bar"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { voiceRecorder.cancelRecording() },
                                modifier = Modifier.testTag("cancel_voice_recording_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = strings.cancel,
                                    tint = DangerRed
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(DangerRed)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format("%02d:%02d", recordingSec / 60, recordingSec % 60),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = DangerRed
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            LinearProgressIndicator(
                                progress = { recordingAmp.coerceIn(0.1f, 1f) },
                                color = BrandPurple,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            IconButton(
                                onClick = {
                                    val rec = voiceRecorder.stopAndFinalize()
                                    if (rec != null) {
                                        viewModel.uploadAndSendVoiceNote(
                                            senderProfile = effectiveMyProfile,
                                            audioFile = rec.file,
                                            durationSec = rec.durationSec
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(BrandBlue)
                                    .testTag("send_voice_recording_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = strings.send,
                                    tint = Color.White
                                )
                            }
                        }
                    } else {
                        // Smart Bangla AI Quick Replies Strip
                        if (messages.isNotEmpty() && editingMessage == null) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item(key = "ai_smart_reply_trigger") {
                                    AssistChip(
                                        onClick = {
                                            if (!quickRepliesLoading) {
                                                viewModel.generateQuickReplies(messages, preferences.language)
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = if (quickRepliesLoading) strings.aiGenerating
                                                else strings.aiQuickRepliesAction,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        },
                                        leadingIcon = {
                                            if (quickRepliesLoading) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    strokeWidth = 2.dp,
                                                    color = BrandPurple
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Rounded.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = BrandPurple,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        modifier = Modifier.testTag("ai_quick_reply_trigger")
                                    )
                                }
                                items(quickReplies, key = { it }) { suggestion ->
                                    AssistChip(
                                        onClick = {
                                            viewModel.sendTextMessage(effectiveMyProfile, suggestion)
                                            viewModel.clearQuickReplies()
                                        },
                                        label = {
                                            Text(
                                                text = suggestion,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    )
                                }
                                if (quickReplies.isNotEmpty()) {
                                    item(key = "clear_quick_replies") {
                                        IconButton(
                                            onClick = { viewModel.clearQuickReplies() },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = strings.close,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showAttachSheet = true },
                                enabled = editingMessage == null,
                                modifier = Modifier.testTag("chat_attach_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddCircle,
                                    contentDescription = strings.attachMediaTitle,
                                    tint = if (editingMessage == null) BrandBlue
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            OutlinedTextField(
                                value = messageText,
                                onValueChange = {
                                    messageText = it.take(4000)
                                    if (editingMessage == null) {
                                        viewModel.onInputChanged(messageText)
                                    }
                                },
                                placeholder = { Text(strings.typeMessageHint) },
                                maxLines = 4,
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_message_input")
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            val hasText = messageText.trim().isNotEmpty()
                            if (hasText || editingMessage != null) {
                                IconButton(
                                    onClick = {
                                        val toSend = messageText.trim()
                                        if (toSend.isNotEmpty()) {
                                            messageText = ""
                                            if (editingMessage != null) {
                                                viewModel.saveEditedMessage(toSend)
                                            } else {
                                                viewModel.sendTextMessage(effectiveMyProfile, toSend)
                                                viewModel.clearQuickReplies()
                                            }
                                        }
                                    },
                                    enabled = hasText,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (hasText) {
                                                if (editingMessage != null) BrandPurple else BrandBlue
                                            } else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .testTag("chat_send_button")
                                ) {
                                    Icon(
                                        imageVector = if (editingMessage != null) Icons.Rounded.Check
                                        else Icons.AutoMirrored.Rounded.Send,
                                        contentDescription = strings.send,
                                        tint = if (hasText) Color.White
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { startVoiceNoteCapture() },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(BrandPurple)
                                        .testTag("chat_voice_record_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Mic,
                                        contentDescription = strings.recordVoiceNote,
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        when (messagesState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandBlue)
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (messagesState as UiState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            is UiState.Success -> {
                val displayedMessages = remember(messages, inChatSearchQuery, searchMatchIds) {
                    if (inChatSearchQuery.isBlank()) messages
                    else messages.filter { it.id in searchMatchIds }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(wallpaperBrush)
                        .padding(innerPadding)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("messages_list"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (inChatSearchQuery.isBlank() && messages.size >= pageLimit) {
                            item(key = "load_more") {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.loadOlderMessages() },
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.testTag("load_older_messages_button")
                                    ) {
                                        Text(strings.loadOlderMessages)
                                    }
                                }
                            }
                        }

                        items(displayedMessages, key = { it.id }) { msg ->
                            val isMe = msg.senderId == viewModel.currentUserId
                            val resolvedSenderName = if (isGroupChat && !isMe) {
                                msg.senderName.ifBlank { chat?.participantNames?.get(msg.senderId).orEmpty() }
                            } else ""
                            MessageBubbleItem(
                                message = msg,
                                isMe = isMe,
                                groupSenderName = resolvedSenderName,
                                isStarred = msg.isStarredFor(viewModel.currentUserId),
                                isSearchMatch = msg.id in searchMatchIds,
                                fontScale = preferences.chatFontSize.scale,
                                translatedText = translatedMessages[msg.id],
                                isTranslating = msg.id in translatingMessageIds,
                                voicePlayback = voicePlayback,
                                onToggleVoicePlay = {
                                    voicePlayer.togglePlay(
                                        messageId = msg.id,
                                        url = msg.mediaUrl,
                                        fallbackDurationSec = msg.audioDurationSec.toInt().coerceAtLeast(1),
                                        scope = scope
                                    )
                                },
                                onLongClick = { if (!msg.deleted) selectedMessage = msg },
                                onImageClick = { url -> fullscreenImageUrl = url },
                                onDocumentClick = { url ->
                                    try {
                                        val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(openIntent)
                                    } catch (_: Exception) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Unable to open attachment")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Attachment Picker Modal Sheet
    if (showAttachSheet) {
        ModalBottomSheet(onDismissRequest = { showAttachSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = strings.attachMediaTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                AttachOptionRow(
                    icon = Icons.Rounded.PhotoLibrary,
                    tint = BrandBlue,
                    label = strings.pickFromGallery,
                    onClick = {
                        showAttachSheet = false
                        galleryPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
                AttachOptionRow(
                    icon = Icons.Rounded.CameraAlt,
                    tint = BrandPurple,
                    label = strings.takeCameraPhoto,
                    onClick = {
                        showAttachSheet = false
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            launchCameraCapture()
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                )
                AttachOptionRow(
                    icon = Icons.Rounded.Description,
                    tint = Color(0xFF0EA5E9),
                    label = strings.shareDocumentPdf,
                    onClick = {
                        showAttachSheet = false
                        documentPickerLauncher.launch(
                            arrayOf(
                                "application/pdf",
                                "text/plain",
                                "application/msword",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                            )
                        )
                    }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Long-press Message Action Sheet (React, Reply, Copy, Forward, Delete Own)
    selectedMessage?.let { msg ->
        ModalBottomSheet(onDismissRequest = { selectedMessage = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                // Quick Reaction Emoji Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QUICK_REACTIONS.forEach { emoji ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable {
                                    viewModel.toggleReaction(msg, emoji)
                                    selectedMessage = null
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 22.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()

                AttachOptionRow(
                    icon = Icons.AutoMirrored.Rounded.Reply,
                    tint = BrandBlue,
                    label = strings.reply,
                    onClick = {
                        viewModel.setReplyTarget(msg)
                        selectedMessage = null
                    }
                )
                val isStarred = msg.isStarredFor(viewModel.currentUserId)
                AttachOptionRow(
                    icon = if (isStarred) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    tint = Color(0xFFF59E0B),
                    label = if (isStarred) strings.unstarMessage else strings.starMessage,
                    onClick = {
                        viewModel.toggleStarMessage(msg)
                        selectedMessage = null
                    }
                )
                val isCurrentlyPinned = chat?.pinnedMessageId == msg.id
                AttachOptionRow(
                    icon = Icons.Rounded.PushPin,
                    tint = BrandBlue,
                    label = if (isCurrentlyPinned) strings.unpinMessage else strings.pinMessage,
                    onClick = {
                        if (isCurrentlyPinned) {
                            viewModel.unpinMessage()
                        } else {
                            val senderLabel = if (msg.senderId == viewModel.currentUserId) {
                                effectiveMyProfile.name
                            } else peerName
                            viewModel.pinMessage(msg, senderLabel)
                        }
                        selectedMessage = null
                    }
                )
                if (msg.senderId == viewModel.currentUserId && msg.type == "text" && msg.text.isNotBlank()) {
                    AttachOptionRow(
                        icon = Icons.Rounded.Edit,
                        tint = BrandPurple,
                        label = strings.editMessage,
                        onClick = {
                            viewModel.setEditingMessage(msg)
                            messageText = msg.text
                            selectedMessage = null
                        }
                    )
                }
                if (msg.text.isNotBlank()) {
                    AttachOptionRow(
                        icon = Icons.Rounded.Translate,
                        tint = BrandPurple,
                        label = strings.aiTranslateAction,
                        onClick = {
                            viewModel.translateMessage(msg, preferences.language)
                            selectedMessage = null
                        }
                    )
                    AttachOptionRow(
                        icon = Icons.Rounded.ContentCopy,
                        tint = MaterialTheme.colorScheme.onSurface,
                        label = strings.copy,
                        onClick = {
                            clipboard.setText(AnnotatedString(msg.text))
                            selectedMessage = null
                            scope.launch { snackbarHostState.showSnackbar(strings.copiedToClipboard) }
                        }
                    )
                }
                AttachOptionRow(
                    icon = Icons.AutoMirrored.Rounded.Forward,
                    tint = BrandPurple,
                    label = strings.forward,
                    onClick = {
                        forwardingMessage = msg
                        selectedMessage = null
                    }
                )
                if (msg.senderId == viewModel.currentUserId) {
                    AttachOptionRow(
                        icon = Icons.Rounded.Delete,
                        tint = DangerRed,
                        label = strings.deleteMessage,
                        onClick = {
                            viewModel.deleteOwnMessage(msg)
                            selectedMessage = null
                        }
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }

    // Forward Message Conversation Picker Dialog
    forwardingMessage?.let { msg ->
        val otherChats = remember(allChats, viewModel.chatId) {
            allChats.filter { it.id != viewModel.chatId }
        }
        AlertDialog(
            onDismissRequest = { forwardingMessage = null },
            title = { Text(strings.forwardToTitle) },
            text = {
                if (otherChats.isEmpty()) {
                    Text(strings.noChatsToForward)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        otherChats.take(8).forEach { targetChat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.forwardMessage(
                                            targetChat = targetChat,
                                            senderProfile = effectiveMyProfile,
                                            original = msg,
                                            successMessage = strings.forwardedLabel
                                        )
                                        forwardingMessage = null
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(
                                    name = targetChat.peerName(viewModel.currentUserId),
                                    photoUrl = targetChat.peerPhotoUrl(viewModel.currentUserId),
                                    size = 38.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = targetChat.peerName(viewModel.currentUserId),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { forwardingMessage = null }) {
                    Text(strings.close)
                }
            }
        )
    }

    // Fullscreen Image Preview Dialog
    fullscreenImageUrl?.let { url ->
        Dialog(onDismissRequest = { fullscreenImageUrl = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(
                        model = url,
                        contentDescription = strings.photoMessagePreview,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                } catch (_: Exception) {
                                    // Ignore missing handler
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.downloadAttachment, color = Color.White)
                        }
                        TextButton(onClick = { fullscreenImageUrl = null }) {
                            Text(strings.close, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showReportDialog && peerUid.isNotBlank()) {
        ReportUserDialog(
            targetName = peerName,
            onDismiss = { showReportDialog = false },
            onSubmit = { reason, details ->
                onReportPeer(peerUid, reason, details)
                showReportDialog = false
            }
        )
    }

    if (showDisappearingDialog) {
        DisappearingMessagesDialog(
            currentHours = chat?.disappearingHours ?: 0L,
            onSelectHours = { hours -> viewModel.setDisappearingHours(hours) },
            onDismiss = { showDisappearingDialog = false }
        )
    }

    if (showStarredSheet) {
        StarredMessagesSheet(
            starredMessages = starredMessages,
            onJumpToMessage = { target ->
                val idx = messages.indexOfFirst { it.id == target.id }
                if (idx >= 0) {
                    scope.launch { listState.animateScrollToItem(idx) }
                }
            },
            onUnstar = { target -> viewModel.toggleStarMessage(target) },
            onDismiss = { showStarredSheet = false }
        )
    }

    if (showPeerProfileSheet && !isGroupChat) {
        ChatPeerProfileSheet(
            peerName = peerName,
            peerPhoto = peerPhoto,
            peerProfile = peerProfile,
            isPeerBlocked = isPeerBlocked,
            onBlockToggle = {
                if (peerUid.isNotBlank()) {
                    if (isPeerBlocked) onUnblockPeer(peerUid)
                    else onBlockPeer(peerProfile ?: PublicProfile(uid = peerUid, name = peerName))
                }
            },
            onReport = { showReportDialog = true },
            onDismiss = { showPeerProfileSheet = false }
        )
    }

    val currentGroupChat = chat
    if (showGroupInfoSheet && currentGroupChat != null && currentGroupChat.isGroup) {
        GroupInfoSheet(
            chat = currentGroupChat,
            currentUserId = viewModel.currentUserId,
            discoverableContacts = discoverableContacts,
            onRenameGroup = { newName -> viewModel.renameGroup(newName) },
            onAddMembers = { newMembers -> viewModel.addGroupMembers(newMembers) },
            onLeaveGroup = { viewModel.leaveGroup(onLeft = onBack) },
            onDismiss = { showGroupInfoSheet = false }
        )
    }

    if (showAiSummarySheet) {
        AiSummarySheet(
            summaryState = summaryState,
            onRetry = { viewModel.summarizeConversation(messages, preferences.language) },
            onDismiss = {
                showAiSummarySheet = false
                viewModel.resetSummaryState()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubbleItem(
    message: ChatMessage,
    isMe: Boolean,
    groupSenderName: String,
    isStarred: Boolean,
    isSearchMatch: Boolean,
    fontScale: Float,
    translatedText: String?,
    isTranslating: Boolean,
    voicePlayback: VoicePlaybackState,
    onToggleVoicePlay: () -> Unit,
    onLongClick: () -> Unit,
    onImageClick: (String) -> Unit,
    onDocumentClick: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val outgoingBrush = Brush.linearGradient(colors = listOf(BrandBlue, BrandPurple))
    val incomingBg = if (isDark) IncomingBubbleDark else IncomingBubbleLight
    val bubbleShape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (isMe) 20.dp else 6.dp,
        bottomEnd = if (isMe) 6.dp else 20.dp
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 296.dp)
                .clip(bubbleShape)
                .then(
                    if (isSearchMatch) Modifier.border(2.dp, Color(0xFFF59E0B), bubbleShape)
                    else Modifier
                )
                .then(
                    if (isMe && !message.deleted) Modifier.background(outgoingBrush)
                    else Modifier.background(incomingBg)
                )
                .combinedClickable(
                    onClick = {
                        if (!message.deleted && message.type == "image" && message.mediaUrl.isNotBlank()) {
                            onImageClick(message.mediaUrl)
                        } else if (!message.deleted && message.type == "document" && message.mediaUrl.isNotBlank()) {
                            onDocumentClick(message.mediaUrl)
                        } else if (!message.deleted && message.type == "audio" && message.mediaUrl.isNotBlank()) {
                            onToggleVoicePlay()
                        }
                    },
                    onLongClick = onLongClick
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            val contentColor = if (isMe && !message.deleted) Color.White
            else MaterialTheme.colorScheme.onSurface

            Column {
                if (groupSenderName.isNotBlank() && !isMe && !message.deleted) {
                    Text(
                        text = groupSenderName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrandPurple
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                }

                if (message.forwarded && !message.deleted) {
                    Text(
                        text = "↪ ${strings.forwardedLabel}",
                        style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                        color = contentColor.copy(alpha = 0.78f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (message.replyToText.isNotBlank() && !message.deleted) {
                    Surface(
                        color = if (isMe) Color.White.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            if (message.replyToSenderName.isNotBlank()) {
                                Text(
                                    text = message.replyToSenderName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMe) Color.White else BrandBlue
                                )
                            }
                            Text(
                                text = message.replyToText,
                                style = MaterialTheme.typography.bodySmall,
                                color = contentColor.copy(alpha = 0.85f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                when {
                    message.deleted -> {
                        Text(
                            text = strings.messageDeletedPreview,
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    message.type == "audio" && message.mediaUrl.isNotBlank() -> {
                        val isThisActive = voicePlayback.activeMessageId == message.id
                        val isPlaying = isThisActive && voicePlayback.isPlaying
                        val isBuffering = isThisActive && voicePlayback.isBuffering
                        val progress = if (isThisActive) voicePlayback.progress else 0f
                        val shownSec = if (isThisActive && voicePlayback.currentSec > 0) {
                            voicePlayback.currentSec
                        } else {
                            message.audioDurationSec.toInt().coerceAtLeast(1)
                        }

                        Row(
                            modifier = Modifier
                                .widthIn(min = 210.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isMe) Color.White.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.surface
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onToggleVoicePlay,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isMe) Color.White else BrandBlue)
                            ) {
                                if (isBuffering) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = if (isMe) BrandBlue else Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = strings.voiceMessagePreview,
                                        tint = if (isMe) BrandBlue else Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    color = if (isMe) Color.White else BrandBlue,
                                    trackColor = contentColor.copy(alpha = 0.25f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = String.format("%02d:%02d", shownSec / 60, shownSec % 60),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = contentColor.copy(alpha = 0.85f)
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.Mic,
                                        contentDescription = null,
                                        tint = contentColor.copy(alpha = 0.75f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    message.type == "image" && message.mediaUrl.isNotBlank() -> {
                        AsyncImage(
                            model = message.mediaUrl,
                            contentDescription = message.mediaName.ifBlank { strings.photoMessagePreview },
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        if (message.text.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = contentColor
                            )
                        }
                    }

                    message.type == "document" && message.mediaUrl.isNotBlank() -> {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isMe) Color.White.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.surface
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AttachFile,
                                contentDescription = null,
                                tint = contentColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = message.mediaName.ifBlank { strings.documentMessagePreview },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = contentColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val sizeLabel = formatFileSize(message.mediaSizeBytes)
                                if (sizeLabel.isNotBlank()) {
                                    Text(
                                        text = sizeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = contentColor.copy(alpha = 0.78f)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Rounded.OpenInNew,
                                contentDescription = strings.openAttachment,
                                tint = contentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    else -> {
                        val baseBody = MaterialTheme.typography.bodyLarge
                        Text(
                            text = message.text,
                            style = baseBody.copy(
                                fontSize = (baseBody.fontSize.value * fontScale).sp,
                                lineHeight = (baseBody.lineHeight.value * fontScale).sp
                            ),
                            color = contentColor
                        )
                    }
                }

                // Inline Smart Bangla AI Translation Box
                if (!message.deleted && (isTranslating || !translatedText.isNullOrBlank())) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = if (isMe) Color.White.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Translate,
                                    contentDescription = null,
                                    tint = if (isMe) Color.White else BrandPurple,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.aiTranslatedLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMe) Color.White else BrandPurple
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            if (isTranslating) {
                                Text(
                                    text = strings.aiGenerating,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = contentColor.copy(alpha = 0.85f)
                                )
                            } else {
                                Text(
                                    text = translatedText.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = contentColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isStarred && !message.deleted) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = strings.starMessage,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (message.edited && !message.deleted) {
                        Text(
                            text = "${strings.editedLabel} • ",
                            style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                            color = contentColor.copy(alpha = 0.72f)
                        )
                    }
                    Text(
                        text = formatMessageTime(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.75f)
                    )
                    if (isMe && !message.deleted) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val tickIcon = if (message.delivered || message.read) Icons.Rounded.DoneAll
                        else Icons.Rounded.Done
                        val tickTint = if (message.read) Color(0xFF93C5FD) else Color.White.copy(alpha = 0.75f)
                        Icon(
                            imageVector = tickIcon,
                            contentDescription = null,
                            tint = tickTint,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        // Reactions bar under bubble
        if (message.reactions.isNotEmpty() && !message.deleted) {
            val grouped = message.reactions.values.groupingBy { it }.eachCount()
            Row(
                modifier = Modifier.padding(top = 3.dp, start = 6.dp, end = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                grouped.forEach { (emoji, count) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp
                    ) {
                        Text(
                            text = if (count > 1) "$emoji $count" else emoji,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (tint == DangerRed) DangerRed else MaterialTheme.colorScheme.onSurface
        )
    }
}
