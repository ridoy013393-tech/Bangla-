package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppLanguage
import com.example.model.ChatFontSize
import com.example.model.ChatWallpaper
import com.example.model.HomeTab
import com.example.model.PublicProfile
import com.example.model.StatusStory
import com.example.model.ThemePreference
import com.example.model.UiState
import com.example.model.UserPreferences
import com.example.ui.components.BanglaChatWordmark
import com.example.ui.components.OfflineConnectivityBanner
import com.example.ui.components.UserAvatar
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.BrandBlue
import com.example.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    preferences: UserPreferences,
    onOpenChat: (String) -> Unit,
    onStartCall: (PublicProfile, String) -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val strings = LocalAppStrings.current
    val snackbarHostState = remember { SnackbarHostState() }

    val myProfile by viewModel.myProfile.collectAsStateWithLifecycle()
    val myPrivateUser by viewModel.myPrivateUser.collectAsStateWithLifecycle()
    val chatsState by viewModel.chatsState.collectAsStateWithLifecycle()
    val discoverableUsersState by viewModel.discoverableUsersState.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val blockedUsers by viewModel.blockedUsers.collectAsStateWithLifecycle()
    val callLogs by viewModel.callLogs.collectAsStateWithLifecycle()
    val activeStatuses by viewModel.activeStatuses.collectAsStateWithLifecycle()
    val isPostingStatus by viewModel.isPostingStatus.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isNetworkOnline by viewModel.isNetworkOnline.collectAsStateWithLifecycle()
    val avatarUploadState by viewModel.avatarUploadState.collectAsStateWithLifecycle()
    val usernameSearchState by viewModel.usernameSearchState.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.CHATS) }
    var selectedChatFilter by rememberSaveable { mutableStateOf(ChatListFilter.ALL) }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showCreateStatusDialog by remember { mutableStateOf(false) }
    var viewingStories by remember { mutableStateOf<List<StatusStory>?>(null) }

    val favoriteUids = remember(favorites) { favorites.map { it.favoriteUid }.toSet() }

    val allActiveChats = remember(chatsState, viewModel.currentUserId) {
        (chatsState as? UiState.Success)?.data
            ?.filter { !it.isArchivedFor(viewModel.currentUserId) }
            .orEmpty()
    }

    val totalUnread = remember(chatsState, viewModel.currentUserId) {
        (chatsState as? UiState.Success)?.data?.sumOf { it.unreadFor(viewModel.currentUserId) } ?: 0
    }
    val unreadChatsCount = remember(allActiveChats, viewModel.currentUserId) {
        allActiveChats.count { it.unreadFor(viewModel.currentUserId) > 0 }
    }
    val groupsCount = remember(allActiveChats) {
        allActiveChats.count { it.isGroup }
    }
    val favoriteChatsCount = remember(allActiveChats, favoriteUids, viewModel.currentUserId) {
        allActiveChats.count { !it.isGroup && it.peerUid(viewModel.currentUserId) in favoriteUids }
    }

    val discoverableProfiles = (discoverableUsersState as? UiState.Success)?.data.orEmpty()

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val tabs = listOf(
        Triple(HomeTab.CHATS, strings.tabChats, Icons.Rounded.ChatBubble to Icons.Outlined.ChatBubbleOutline),
        Triple(HomeTab.CONTACTS, strings.tabContacts, Icons.Rounded.Group to Icons.Outlined.Group),
        Triple(HomeTab.CALLS, strings.tabCalls, Icons.Rounded.Call to Icons.Outlined.Call),
        Triple(HomeTab.SETTINGS, strings.tabSettings, Icons.Rounded.Settings to Icons.Outlined.Settings)
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    tabs.forEach { (tab, label, icons) ->
                        val selected = selectedTab == tab
                        NavigationRailItem(
                            selected = selected,
                            onClick = { selectedTab = tab },
                            icon = {
                                if (tab == HomeTab.CHATS && totalUnread > 0) {
                                    BadgedBox(badge = { Badge { Text(totalUnread.toString()) } }) {
                                        Icon(if (selected) icons.first else icons.second, contentDescription = label)
                                    }
                                } else {
                                    Icon(if (selected) icons.first else icons.second, contentDescription = label)
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }

            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.statusBars)
                        ) {
                            // Top Messenger-inspired header row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BanglaChatWordmark(
                                    fontSize = 24.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (selectedTab == HomeTab.CHATS) {
                                    IconButton(
                                        onClick = { showCreateGroupDialog = true },
                                        modifier = Modifier.testTag("home_new_group_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.GroupAdd,
                                            contentDescription = strings.newGroup,
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        searchExpanded = !searchExpanded
                                        if (!searchExpanded) searchQuery = ""
                                    },
                                    modifier = Modifier.testTag("home_search_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (searchExpanded) Icons.Rounded.Close else Icons.Rounded.Search,
                                        contentDescription = strings.searchChatsHint,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { selectedTab = HomeTab.SETTINGS }
                                        .testTag("home_profile_avatar")
                                ) {
                                    UserAvatar(
                                        name = myProfile?.name.orEmpty().ifBlank { "BC" },
                                        photoUrl = myProfile?.photoUrl.orEmpty(),
                                        size = 40.dp,
                                        showOnlineIndicator = true,
                                        isOnline = isNetworkOnline
                                    )
                                }
                            }

                            AnimatedVisibility(visible = searchExpanded && selectedTab == HomeTab.CHATS) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text(strings.searchChatsHint) },
                                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                        .testTag("home_search_input")
                                )
                            }

                            // WhatsApp-style live Chat Filter pills (All, Unread count, Groups count, Favorites count)
                            if (selectedTab == HomeTab.CHATS) {
                                val chatFilterPills = listOf(
                                    Triple(ChatListFilter.ALL, strings.chatFilterAll, allActiveChats.size),
                                    Triple(ChatListFilter.UNREAD, strings.chatFilterUnread, unreadChatsCount),
                                    Triple(ChatListFilter.GROUPS, strings.chatFilterGroups, groupsCount),
                                    Triple(ChatListFilter.FAVORITES, strings.chatFilterFavorites, favoriteChatsCount)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(chatFilterPills, key = { it.first.name }) { (filter, label, count) ->
                                        val selected = selectedChatFilter == filter
                                        val highlightUnread = filter == ChatListFilter.UNREAD && count > 0 && !selected
                                        Surface(
                                            shape = RoundedCornerShape(22.dp),
                                            color = if (selected) BrandBlue
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(22.dp))
                                                .clickable { selectedChatFilter = filter }
                                                .testTag("chat_filter_pill_${filter.name.lowercase()}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelLarge.copy(
                                                        fontWeight = if (selected || highlightUnread) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = when {
                                                        selected -> Color.White
                                                        highlightUnread -> BrandBlue
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                                Surface(
                                                    shape = CircleShape,
                                                    color = when {
                                                        selected -> Color.White.copy(alpha = 0.24f)
                                                        highlightUnread -> BrandBlue
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f)
                                                    }
                                                ) {
                                                    Text(
                                                        text = count.toString(),
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = when {
                                                            selected -> Color.White
                                                            highlightUnread -> Color.White
                                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                        },
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            OfflineConnectivityBanner(
                                isOnline = isNetworkOnline,
                                offlineMessage = strings.offlineBanner
                            )
                        }
                    }
                },
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            tabs.forEach { (tab, label, icons) ->
                                val selected = selectedTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { selectedTab = tab },
                                    icon = {
                                        if (tab == HomeTab.CHATS && totalUnread > 0) {
                                            BadgedBox(badge = { Badge { Text(totalUnread.toString()) } }) {
                                                Icon(
                                                    if (selected) icons.first else icons.second,
                                                    contentDescription = label
                                                )
                                            }
                                        } else {
                                            Icon(
                                                if (selected) icons.first else icons.second,
                                                contentDescription = label
                                            )
                                        }
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("bottom_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (selectedTab == HomeTab.CHATS) {
                        FloatingActionButton(
                            onClick = { selectedTab = HomeTab.CONTACTS },
                            containerColor = BrandBlue,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.testTag("new_chat_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = strings.newChat
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (selectedTab) {
                        HomeTab.CHATS -> ChatsTab(
                            currentUserId = viewModel.currentUserId,
                            myProfile = myProfile,
                            chatsState = chatsState,
                            discoverableProfiles = discoverableProfiles,
                            activeStatuses = activeStatuses,
                            favoriteUids = favoriteUids,
                            selectedFilter = selectedChatFilter,
                            searchQuery = searchQuery,
                            isRefreshing = isRefreshing,
                            showOnlineBanner = isNetworkOnline && !preferences.onlineBannerDismissed,
                            onDismissOnlineBanner = { viewModel.dismissOnlineBanner() },
                            onRefresh = { viewModel.refreshChats() },
                            onResetFilter = { selectedChatFilter = ChatListFilter.ALL },
                            onChatClick = onOpenChat,
                            onTogglePinChat = { chat -> viewModel.toggleChatPin(chat) },
                            onToggleMuteChat = { chat -> viewModel.toggleChatMute(chat) },
                            onToggleArchiveChat = { chat -> viewModel.toggleChatArchive(chat) },
                            onExploreContacts = { selectedTab = HomeTab.CONTACTS },
                            onCreateGroupClick = { showCreateGroupDialog = true },
                            onAddStatusClick = { showCreateStatusDialog = true },
                            onOpenAuthorStatuses = { stories -> viewingStories = stories }
                        )

                        HomeTab.CONTACTS -> ContactsTab(
                            usernameSearchState = usernameSearchState,
                            knownProfiles = discoverableProfiles,
                            favorites = favorites,
                            blockedUsers = blockedUsers,
                            onSearchUsername = { query -> viewModel.searchUserByExactUsername(query) },
                            onClearUsernameSearch = { viewModel.clearUsernameSearch() },
                            onStartChat = { peer: PublicProfile ->
                                viewModel.startChatWith(peer, onChatReady = onOpenChat)
                            },
                            onStartCall = onStartCall,
                            onToggleFavorite = { profile, isFav ->
                                viewModel.toggleFavorite(profile, isFav)
                            },
                            onBlockUser = { profile -> viewModel.blockUser(profile) },
                            onUnblockUser = { uid -> viewModel.unblockUser(uid) },
                            onSubmitReport = { uid, reason, details ->
                                viewModel.submitReport(uid, reason, details, strings.reportSubmitted)
                            }
                        )

                        HomeTab.CALLS -> CallsTab(
                            callLogs = callLogs,
                            discoverableProfiles = discoverableProfiles,
                            onStartCall = onStartCall
                        )

                        HomeTab.SETTINGS -> SettingsTab(
                            myProfile = myProfile,
                            myPrivateUser = myPrivateUser,
                            preferences = preferences,
                            blockedUsers = blockedUsers,
                            avatarUploadState = avatarUploadState,
                            onUpdateProfile = { name, username, about, phone ->
                                viewModel.updateProfile(
                                    name = name,
                                    username = username,
                                    about = about,
                                    phone = phone,
                                    successMessage = strings.profileUpdated,
                                    usernameTakenMessage = strings.usernameTaken
                                )
                            },
                            onUploadAvatar = { uri: Uri ->
                                viewModel.uploadNewProfilePhoto(uri, strings.profileUpdated)
                            },
                            onUpdatePrivacy = { discoverable, showOnline, showLastSeen, showReadReceipts, showPhoto, showAbout ->
                                viewModel.updatePrivacySettings(
                                    discoverable,
                                    showOnline,
                                    showLastSeen,
                                    showReadReceipts,
                                    showPhoto,
                                    showAbout
                                )
                            },
                            onUpdateMessagePreview = { enabled ->
                                viewModel.updateMessagePreviewSetting(enabled)
                            },
                            onChangeLanguage = { lang: AppLanguage -> viewModel.setLanguage(lang) },
                            onChangeTheme = { theme: ThemePreference -> viewModel.setTheme(theme) },
                            onChangeWallpaper = { wp: ChatWallpaper -> viewModel.setChatWallpaper(wp) },
                            onChangeFontSize = { sz: ChatFontSize -> viewModel.setChatFontSize(sz) },
                            onToggleScreenSecurity = { enabled -> viewModel.setScreenSecurityEnabled(enabled) },
                            onEnableAppLock = { pin -> viewModel.enableAppLock(pin, strings.appLockEnabledToast) },
                            onDisableAppLock = { viewModel.disableAppLock(strings.appLockDisabledToast) },
                            onUnblockUser = { uid -> viewModel.unblockUser(uid) },
                            onLogout = onLogout,
                            onDeleteAccount = onDeleteAccount
                        )
                    }
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            availableContacts = discoverableProfiles,
            onCreateGroup = { name, members, photoUri ->
                viewModel.createGroupChat(
                    groupName = name,
                    selectedMembers = members,
                    photoUri = photoUri,
                    successToast = strings.groupCreatedToast,
                    onGroupCreated = onOpenChat
                )
            },
            onDismiss = { showCreateGroupDialog = false }
        )
    }

    if (showCreateStatusDialog) {
        CreateStatusDialog(
            isPosting = isPostingStatus,
            onPostText = { text, colorHex ->
                viewModel.postTextStatus(text, colorHex, strings.statusPostedToast)
            },
            onPostPhoto = { uri, caption ->
                viewModel.postPhotoStatus(uri, caption, strings.statusPostedToast)
            },
            onDismiss = { showCreateStatusDialog = false }
        )
    }

    viewingStories?.let { stories ->
        StatusViewerDialog(
            stories = stories,
            currentUserId = viewModel.currentUserId,
            onMarkViewed = { story -> viewModel.markStatusViewed(story) },
            onDeleteStatus = { story -> viewModel.deleteStatus(story.id) },
            onAddAnother = { showCreateStatusDialog = true },
            onDismiss = { viewingStories = null }
        )
    }
}
