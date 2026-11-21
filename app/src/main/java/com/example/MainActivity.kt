package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AppUpdateChecker
import com.example.data.NetworkMonitor
import com.example.data.StorageRepository
import com.example.data.UpdateCheckResult
import com.example.data.authStateFlow
import com.example.data.configureAppCheck
import com.example.data.deleteCurrentAccountWithReauth
import com.example.data.signOutUser
import com.example.model.PublicProfile
import com.example.model.UiState
import com.example.model.UserPreferences
import com.example.service.ActiveAppTracker
import com.example.service.BanglaChatMessagingService
import com.example.service.BanglaChatRealtimeService
import com.example.service.NotificationHelper
import com.example.ui.components.BanglaChatLogoMark
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.stringsForLanguage
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import com.example.ui.screens.AccountSuspendedScreen
import com.example.ui.screens.ActiveCallRequest
import com.example.ui.screens.AdminModerationScreen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AnimatedSplashScreen
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.AuthEntryMode
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ForceUpdateScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IncomingCallBanner
import com.example.ui.screens.LiveCallScreen
import com.example.ui.screens.RegistrationCompletionScreen
import com.example.ui.theme.BanglaChatTheme
import com.example.ui.theme.BrandBlue
import com.example.viewmodel.AiAssistantViewModel
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {

    private val deepLinkChatId = MutableStateFlow<String?>(null)
    private val deepLinkAcceptCallId = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        extractDeepLinks(intent)

        setContent {
            val context = LocalContext.current
            val app = remember(context) { context.applicationContext as BanglaChatApplication }
            val prefsRepository = app.preferencesRepository
            val loadedPreferences by prefsRepository.preferencesFlow
                .collectAsStateWithLifecycle(initialValue = null)
            val preferences = loadedPreferences ?: UserPreferences()
            val pendingChatId by deepLinkChatId.collectAsStateWithLifecycle()
            val pendingAcceptCallId by deepLinkAcceptCallId.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()

            // Brief branded splash showcase on cold launch so the animated intro plays smoothly
            // and DataStore preferences finish loading before any Home/Banner/AppLock UI renders
            var splashIntroFinished by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(1150L)
                splashIntroFinished = true
            }

            // Only update FLAG_SECURE when the actual window flag state differs to avoid redundant WMS surface syncs
            LaunchedEffect(preferences.screenSecurityEnabled) {
                val isCurrentlySecure =
                    (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
                if (preferences.screenSecurityEnabled && !isCurrentlySecure) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else if (!preferences.screenSecurityEnabled && isCurrentlySecure) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            // App Lock state (re-locks when app moves to background)
            var isUnlocked by rememberSaveable { mutableStateOf(false) }
            val activityLifecycle = LocalLifecycleOwner.current
            DisposableEffect(activityLifecycle, preferences.appLockEnabled) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> ActiveAppTracker.isAppInForeground = true
                        Lifecycle.Event.ON_STOP -> {
                            ActiveAppTracker.isAppInForeground = false
                            if (preferences.appLockEnabled) {
                                isUnlocked = false
                            }
                        }
                        else -> Unit
                    }
                }
                activityLifecycle.lifecycle.addObserver(observer)
                onDispose {
                    ActiveAppTracker.isAppInForeground = false
                    activityLifecycle.lifecycle.removeObserver(observer)
                }
            }

            val showInitialSplash = !splashIntroFinished || loadedPreferences == null

            BanglaChatTheme(themePreference = preferences.theme) {
                CompositionLocalProvider(
                    LocalAppStrings provides stringsForLanguage(preferences.language)
                ) {
                    Crossfade(
                        targetState = showInitialSplash,
                        animationSpec = tween(durationMillis = 360),
                        label = "root_splash_transition"
                    ) { showingSplash ->
                        if (showingSplash) {
                            AnimatedSplashScreen()
                        } else if (preferences.appLockEnabled && !isUnlocked) {
                            AppLockScreen(
                                expectedPinHash = preferences.appLockPinHash,
                                biometricEnabled = preferences.biometricLockEnabled,
                                onUnlocked = { isUnlocked = true }
                            )
                        } else {
                            BanglaChatAppRoot(
                                preferences = preferences,
                                pendingDeepLinkChatId = pendingChatId,
                                onConsumeDeepLink = { deepLinkChatId.value = null },
                                pendingAcceptCallId = pendingAcceptCallId,
                                onConsumeAcceptCall = { deepLinkAcceptCallId.value = null },
                                onSetLanguage = { lang -> scope.launch { prefsRepository.setLanguage(lang) } }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractDeepLinks(intent)
    }

    private fun extractDeepLinks(intent: Intent?) {
        val chatId = intent?.getStringExtra(BanglaChatMessagingService.EXTRA_CHAT_ID)
        if (!chatId.isNullOrBlank()) {
            deepLinkChatId.value = chatId
        }
        val acceptCallId = intent?.getStringExtra(NotificationHelper.EXTRA_ACCEPT_CALL_ID)
        if (!acceptCallId.isNullOrBlank()) {
            deepLinkAcceptCallId.value = acceptCallId
        }
    }
}

private sealed interface AccountRegistrationState {
    data object Checking : AccountRegistrationState
    data object Registered : AccountRegistrationState
    data object NeedsRegistration : AccountRegistrationState
    data class Error(val message: String) : AccountRegistrationState
}

@Composable
private fun BanglaChatAppRoot(
    preferences: UserPreferences,
    pendingDeepLinkChatId: String?,
    onConsumeDeepLink: () -> Unit,
    pendingAcceptCallId: String?,
    onConsumeAcceptCall: () -> Unit,
    onSetLanguage: (com.example.model.AppLanguage) -> Unit
) {
    val auth = remember { Firebase.auth }
    val authFlow = remember(auth) { auth.authStateFlow() }
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as BanglaChatApplication }
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var selectedAuthMode by rememberSaveable { mutableStateOf(AuthEntryMode.LOGIN) }
    var pendingPhoneHint by rememberSaveable { mutableStateOf("") }

    // Pre-login GitHub Releases check so even logged-out old builds must update immediately
    var preLoginUpdateState by remember { mutableStateOf<UpdateCheckResult>(UpdateCheckResult.Idle) }
    var preLoginCheckNonce by remember { mutableIntStateOf(0) }
    LaunchedEffect(currentUser, preLoginCheckNonce) {
        if (currentUser == null) {
            preLoginUpdateState = UpdateCheckResult.Checking
            preLoginUpdateState = AppUpdateChecker.checkForUpdate()
        } else {
            preLoginUpdateState = UpdateCheckResult.Idle
        }
    }

    val user = currentUser
    if (user == null) {
        val preLoginUpdate = (preLoginUpdateState as? UpdateCheckResult.UpdateAvailable)?.info
        if (preLoginUpdate != null && preLoginUpdate.forceUpdate) {
            ForceUpdateScreen(
                info = preLoginUpdate,
                isChecking = preLoginUpdateState is UpdateCheckResult.Checking,
                isSuperAdmin = false,
                onRecheck = { preLoginCheckNonce++ }
            )
            return
        }
        AuthScreen(
            currentLanguage = preferences.language,
            onToggleLanguage = onSetLanguage,
            onSelectAuthMode = { mode -> selectedAuthMode = mode },
            onPendingPhoneHint = { phone -> pendingPhoneHint = phone },
            onAuthSuccess = { /* AuthStateListener drives reactive transition */ }
        )
    } else {
        var checkNonce by remember(user.uid) { mutableIntStateOf(0) }
        var registrationState by remember(user.uid) {
            mutableStateOf<AccountRegistrationState>(AccountRegistrationState.Checking)
        }
        val rootModerationFlow = remember(user.uid) {
            app.chatRepository.observeMyModerationStatus(user.uid)
        }
        val rootModerationStatus by rootModerationFlow.collectAsStateWithLifecycle(initialValue = null)

        if (rootModerationStatus?.isSuspended() == true) {
            LaunchedEffect(user.uid) {
                BanglaChatRealtimeService.stop(context)
            }
            AccountSuspendedScreen(
                status = rootModerationStatus!!,
                onLogout = {
                    BanglaChatRealtimeService.stop(context)
                    signOutUser(
                        credentialManager = credentialManager,
                        scope = scope
                    )
                }
            )
            return
        }

        LaunchedEffect(user.uid, checkNonce) {
            registrationState = AccountRegistrationState.Checking
            val result = withContext(Dispatchers.IO) {
                app.chatRepository.hasRegisteredProfile(user.uid)
            }
            result
                .onSuccess { exists ->
                    registrationState = if (exists) {
                        AccountRegistrationState.Registered
                    } else {
                        AccountRegistrationState.NeedsRegistration
                    }
                }
                .onFailure { err ->
                    registrationState = AccountRegistrationState.Error(
                        err.localizedMessage ?: "Failed to verify account"
                    )
                }
        }

        when (val state = registrationState) {
            AccountRegistrationState.Checking -> {
                AnimatedSplashScreen(statusText = strings.checkingAccount)
            }

            is AccountRegistrationState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { checkNonce++ }) {
                        Text(strings.retry)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            signOutUser(
                                credentialManager = credentialManager,
                                scope = scope
                            )
                        }
                    ) {
                        Text(strings.switchGoogleAccount)
                    }
                }
            }

            AccountRegistrationState.NeedsRegistration -> {
                RegistrationCompletionScreen(
                    firebaseUser = user,
                    cameFromLoginAttempt = selectedAuthMode == AuthEntryMode.LOGIN,
                    initialPhoneHint = pendingPhoneHint,
                    currentLanguage = preferences.language,
                    repository = app.chatRepository,
                    onToggleLanguage = onSetLanguage,
                    onRegistrationComplete = {
                        pendingPhoneHint = ""
                        registrationState = AccountRegistrationState.Registered
                    },
                    onSwitchAccount = {
                        pendingPhoneHint = ""
                        signOutUser(
                            credentialManager = credentialManager,
                            scope = scope
                        )
                    }
                )
            }

            AccountRegistrationState.Registered -> {
                // Key composition and ViewModels strictly by authenticated UID
                key(user.uid) {
                    AuthenticatedBanglaChatHost(
                        currentUserId = user.uid,
                        preferences = preferences,
                        pendingDeepLinkChatId = pendingDeepLinkChatId,
                        onConsumeDeepLink = onConsumeDeepLink,
                        pendingAcceptCallId = pendingAcceptCallId,
                        onConsumeAcceptCall = onConsumeAcceptCall
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedBanglaChatHost(
    currentUserId: String,
    preferences: UserPreferences,
    pendingDeepLinkChatId: String?,
    onConsumeDeepLink: () -> Unit,
    pendingAcceptCallId: String?,
    onConsumeAcceptCall: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val app = remember(context) { context.applicationContext as BanglaChatApplication }

    val mainViewModel: MainViewModel = viewModel(
        key = "main_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                } as BanglaChatApplication
                MainViewModel(
                    repository = application.chatRepository,
                    storageRepository = StorageRepository(application),
                    preferencesRepository = application.preferencesRepository,
                    networkMonitor = NetworkMonitor(application),
                    currentUserId = currentUserId
                )
            }
        }
    )

    // Request POST_NOTIFICATIONS runtime permission on Android 13+ once signed in
    var notificationPermRequested by rememberSaveable(currentUserId) { mutableStateOf(false) }
    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            BanglaChatRealtimeService.start(context)
        }
    }

    LaunchedEffect(currentUserId) {
        NotificationHelper.createAllChannels(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                BanglaChatRealtimeService.start(context)
            } else if (!notificationPermRequested) {
                notificationPermRequested = true
                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            BanglaChatRealtimeService.start(context)
        }
    }

    // Lifecycle-driven online presence updates & foreground sync service activation
    DisposableEffect(lifecycleOwner, currentUserId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    mainViewModel.setOnlinePresence(true)
                    BanglaChatRealtimeService.start(context)
                }
                Lifecycle.Event.ON_STOP -> mainViewModel.setOnlinePresence(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var activeChatId by rememberSaveable(currentUserId) { mutableStateOf<String?>(null) }
    var isAiAssistantOpen by rememberSaveable(currentUserId) { mutableStateOf(false) }
    var isAdminPanelOpen by rememberSaveable(currentUserId) { mutableStateOf(false) }

    val aiAssistantViewModel: AiAssistantViewModel = viewModel(
        key = "ai_assistant_$currentUserId",
        factory = viewModelFactory {
            initializer {
                AiAssistantViewModel(currentUserId = currentUserId)
            }
        }
    )

    // Keep ActiveAppTracker synced with whichever chat is currently open & clear its notification
    DisposableEffect(activeChatId) {
        ActiveAppTracker.activeChatId = activeChatId
        if (!activeChatId.isNullOrBlank()) {
            NotificationHelper.cancelChatNotification(context, activeChatId!!)
        }
        onDispose {
            if (ActiveAppTracker.activeChatId == activeChatId) {
                ActiveAppTracker.activeChatId = null
            }
        }
    }

    LaunchedEffect(pendingDeepLinkChatId) {
        if (!pendingDeepLinkChatId.isNullOrBlank()) {
            isAiAssistantOpen = false
            isAdminPanelOpen = false
            activeChatId = pendingDeepLinkChatId
            onConsumeDeepLink()
        }
    }

    val myProfile by mainViewModel.myProfile.collectAsStateWithLifecycle()
    val myModerationStatus by mainViewModel.myModerationStatus.collectAsStateWithLifecycle()
    val adminAllProfiles by mainViewModel.adminAllProfiles.collectAsStateWithLifecycle()
    val adminAllModerations by mainViewModel.adminAllModerations.collectAsStateWithLifecycle()
    val adminAllReports by mainViewModel.adminAllReports.collectAsStateWithLifecycle()
    val appBroadcast by mainViewModel.appBroadcast.collectAsStateWithLifecycle()
    val moderationSettings by mainViewModel.moderationSettings.collectAsStateWithLifecycle()
    val appUpdateConfig by mainViewModel.appUpdateConfig.collectAsStateWithLifecycle()
    val chatsState by mainViewModel.chatsState.collectAsStateWithLifecycle()
    val discoverableUsersState by mainViewModel.discoverableUsersState.collectAsStateWithLifecycle()
    val blockedUsers by mainViewModel.blockedUsers.collectAsStateWithLifecycle()
    val incomingCalls by mainViewModel.incomingCalls.collectAsStateWithLifecycle()
    val isNetworkOnline by mainViewModel.isNetworkOnline.collectAsStateWithLifecycle()
    val updateCheckResult by mainViewModel.updateCheckResult.collectAsStateWithLifecycle()
    val blockedUids = remember(blockedUsers) { blockedUsers.map { it.blockedUid }.toSet() }
    val allChats = remember(chatsState, currentUserId) {
        (chatsState as? UiState.Success)?.data.orEmpty().filter { !it.isHiddenFor(currentUserId) }
    }
    val discoverableContacts = (discoverableUsersState as? UiState.Success)?.data.orEmpty()
    val strings = LocalAppStrings.current

    if (myModerationStatus?.isSuspended() == true) {
        LaunchedEffect(currentUserId) {
            BanglaChatRealtimeService.stop(context)
        }
        AccountSuspendedScreen(
            status = myModerationStatus!!,
            onLogout = {
                BanglaChatRealtimeService.stop(context)
                signOutUser(
                    credentialManager = credentialManager,
                    scope = scope,
                    onBeforeSignOut = { mainViewModel.setOfflineBeforeSignOut() }
                )
            }
        )
        return
    }

    // Auto-check Firestore + GitHub releases on launch and whenever app returns to foreground
    LaunchedEffect(currentUserId, isNetworkOnline) {
        if (isNetworkOnline) {
            mainViewModel.checkForAppUpdate(manual = false)
        }
    }
    val updateLifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(updateLifecycleOwner, isNetworkOnline) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isNetworkOnline) {
                mainViewModel.checkForAppUpdate(manual = false, forceRecheck = true)
            }
        }
        updateLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { updateLifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Mandatory full-screen Force Update Gate — blocks entire app until the new APK is installed
    val mandatoryUpdateInfo = (updateCheckResult as? UpdateCheckResult.UpdateAvailable)?.info
        ?.takeIf { it.forceUpdate }
    if (mandatoryUpdateInfo != null && !(isAdminPanelOpen && mainViewModel.isSuperAdmin)) {
        LaunchedEffect(mandatoryUpdateInfo.latestVersionCode, mandatoryUpdateInfo.latestVersionName) {
            BanglaChatRealtimeService.stop(context)
        }
        ForceUpdateScreen(
            info = mandatoryUpdateInfo,
            isChecking = updateCheckResult is UpdateCheckResult.Checking,
            isSuperAdmin = mainViewModel.isSuperAdmin,
            onRecheck = {
                mainViewModel.checkForAppUpdate(
                    manual = true,
                    upToDateToast = strings.appUpToDateToast,
                    forceRecheck = true
                )
            },
            onOpenAdminPanel = if (mainViewModel.isSuperAdmin) {
                { isAdminPanelOpen = true }
            } else null
        )
        return
    }

    // WhatsApp-style foreground presence heartbeat every 45s so stale/offline devices never appear stuck online
    LaunchedEffect(currentUserId, isNetworkOnline) {
        if (!isNetworkOnline) return@LaunchedEffect
        while (true) {
            if (ActiveAppTracker.isAppInForeground) {
                mainViewModel.setOnlinePresence(true)
            }
            delay(45_000L)
        }
    }

    // Automatically acknowledge incoming messages as delivered (✓ -> ✓✓) when device is online
    LaunchedEffect(allChats, isNetworkOnline) {
        if (isNetworkOnline && allChats.isNotEmpty()) {
            mainViewModel.acknowledgeIncomingChatsDelivered(allChats)
        }
    }

    val callRepository = remember(app) { app.chatRepository }
    var activeCallRequest by remember { mutableStateOf<ActiveCallRequest?>(null) }
    var isCallMinimized by remember { mutableStateOf(false) }

    // WhatsApp-style handshake: mark 'busy' if already in another call, otherwise transition 'calling' -> 'ringing'
    LaunchedEffect(incomingCalls, activeCallRequest) {
        val activeSid = (activeCallRequest as? ActiveCallRequest.Incoming)?.session?.id
            ?: ActiveAppTracker.activeCallId
        incomingCalls.forEach { session ->
            if (activeCallRequest != null && session.id != activeSid) {
                mainViewModel.markIncomingCallBusy(session)
            } else if (session.status == "calling") {
                mainViewModel.acknowledgeIncomingCallRinging(session)
            }
        }
    }

    // Handle "Accept" button tapped directly on an incoming call notification
    LaunchedEffect(pendingAcceptCallId) {
        val callId = pendingAcceptCallId
        if (!callId.isNullOrBlank()) {
            onConsumeAcceptCall()
            NotificationHelper.cancelIncomingCallNotification(context, callId)
            val liveSession = incomingCalls.firstOrNull { it.id == callId }
                ?: callRepository.getCallSession(callId).getOrNull()
            if (liveSession != null && liveSession.status in setOf("calling", "ringing")) {
                isCallMinimized = false
                activeCallRequest = ActiveCallRequest.Incoming(liveSession)
            }
        }
    }

    val effectiveMyProfile = myProfile ?: PublicProfile(
        uid = currentUserId,
        name = Firebase.auth.currentUser?.displayName ?: "Bangla Chat User",
        photoUrl = Firebase.auth.currentUser?.photoUrl?.toString().orEmpty()
    )

    Box(modifier = Modifier.fillMaxSize()) {
        val openChatId = activeChatId
        if (openChatId != null) {
            key(openChatId) {
                val chatViewModel: ChatViewModel = viewModel(
                    key = "chat_${currentUserId}_$openChatId",
                    factory = viewModelFactory {
                        initializer {
                            val application = checkNotNull(this[APPLICATION_KEY]) {
                                "APPLICATION_KEY missing from CreationExtras"
                            } as BanglaChatApplication
                            ChatViewModel(
                                repository = application.chatRepository,
                                storageRepository = StorageRepository(application),
                                chatId = openChatId,
                                currentUserId = currentUserId
                            )
                        }
                    }
                )

                ChatScreen(
                    viewModel = chatViewModel,
                    myProfile = myProfile,
                    preferences = preferences,
                    allChats = allChats,
                    discoverableContacts = discoverableContacts,
                    blockedUids = blockedUids,
                    myModerationStatus = myModerationStatus,
                    onBack = { activeChatId = null },
                    onStartVoiceCall = { peer ->
                        isCallMinimized = false
                        activeCallRequest = ActiveCallRequest.Outgoing(peer, "audio")
                    },
                    onStartVideoCall = { peer ->
                        isCallMinimized = false
                        activeCallRequest = ActiveCallRequest.Outgoing(peer, "video")
                    },
                    onBlockPeer = { peer -> mainViewModel.blockUser(peer) },
                    onUnblockPeer = { uid -> mainViewModel.unblockUser(uid) },
                    onReportPeer = { uid, reason, details ->
                        mainViewModel.submitReport(uid, reason, details, strings.reportSubmitted)
                    },
                    onOpenDirectChatWithMember = { member ->
                        mainViewModel.startChatWith(member) { directChatId ->
                            activeChatId = directChatId
                        }
                    }
                )
            }
        } else if (isAdminPanelOpen && mainViewModel.isSuperAdmin) {
            AdminModerationScreen(
                currentAdminUid = currentUserId,
                profiles = adminAllProfiles,
                moderations = adminAllModerations,
                reports = adminAllReports,
                currentBroadcast = appBroadcast,
                currentModerationSettings = moderationSettings,
                currentAppUpdateConfig = appUpdateConfig,
                language = preferences.language,
                onBanUser = { uid, name, username, reason ->
                    mainViewModel.adminBanUser(uid, name, username, reason, strings.adminActionSuccessToast)
                },
                onMuteUser = { uid, name, username, hours, reason ->
                    mainViewModel.adminMuteUser(uid, name, username, hours, reason, strings.adminActionSuccessToast)
                },
                onUnbanUser = { uid, name, username ->
                    mainViewModel.adminUnbanUser(uid, name, username, strings.adminActionSuccessToast)
                },
                onDeleteUser = { uid, name, username, reason ->
                    mainViewModel.adminDeleteUser(uid, name, username, reason, strings.adminActionSuccessToast)
                },
                onSetVerifiedBadge = { uid, name, username, verified ->
                    mainViewModel.adminSetVerifiedBadge(uid, name, username, verified, strings.adminActionSuccessToast)
                },
                onPublishBroadcast = { active, title, message, severity ->
                    mainViewModel.adminPublishBroadcast(active, title, message, severity, strings.adminBroadcastSavedToast)
                },
                onSaveModerationSettings = { newSettings ->
                    mainViewModel.adminSaveModerationSettings(newSettings, strings.adminFilterSavedToast)
                },
                onSaveAppUpdateConfig = { newUpdateConfig ->
                    mainViewModel.adminSaveAppUpdateConfig(newUpdateConfig, strings.adminAppUpdateSavedToast)
                },
                onDismissReport = { reportId ->
                    mainViewModel.adminDismissReport(reportId, strings.adminActionSuccessToast)
                },
                onBack = { isAdminPanelOpen = false }
            )
        } else if (isAiAssistantOpen) {
            AiAssistantScreen(
                viewModel = aiAssistantViewModel,
                language = preferences.language,
                onBack = { isAiAssistantOpen = false }
            )
        } else {
            HomeScreen(
                viewModel = mainViewModel,
                preferences = preferences,
                onOpenChat = { chatId ->
                    isAiAssistantOpen = false
                    isAdminPanelOpen = false
                    activeChatId = chatId
                },
                onOpenAiAssistant = { isAiAssistantOpen = true },
                onOpenAdminPanel = {
                    if (mainViewModel.isSuperAdmin) {
                        isAdminPanelOpen = true
                    }
                },
                onStartCall = { peer, callType ->
                    isCallMinimized = false
                    activeCallRequest = ActiveCallRequest.Outgoing(peer, callType)
                },
                onLogout = {
                    BanglaChatRealtimeService.stop(context)
                    signOutUser(
                        credentialManager = credentialManager,
                        scope = scope,
                        onBeforeSignOut = { mainViewModel.setOfflineBeforeSignOut() }
                    )
                },
                onDeleteAccount = {
                    BanglaChatRealtimeService.stop(context)
                    scope.launch {
                        deleteCurrentAccountWithReauth(
                            context = context,
                            credentialManager = credentialManager,
                            onBeforeAuthDelete = { uid -> mainViewModel.deleteUserFirestoreRecords(uid) }
                        )
                    }
                }
            )
        }

        // Keep LiveCallScreen mounted inside root Box so WebRTC stays live while minimized into floating pill
        val currentCall = activeCallRequest
        if (currentCall != null) {
            LiveCallScreen(
                request = currentCall,
                myProfile = effectiveMyProfile,
                repository = callRepository,
                language = preferences.language,
                isMinimized = isCallMinimized,
                onToggleMinimize = { minimized -> isCallMinimized = minimized },
                onCallClosed = {
                    isCallMinimized = false
                    activeCallRequest = null
                }
            )
        } else {
            incomingCalls.firstOrNull()?.let { ringing ->
                Box(modifier = Modifier.align(Alignment.TopCenter)) {
                    IncomingCallBanner(
                        session = ringing,
                        onAccept = {
                            NotificationHelper.cancelIncomingCallNotification(context, ringing.id)
                            isCallMinimized = false
                            activeCallRequest = ActiveCallRequest.Incoming(ringing)
                        },
                        onDecline = {
                            NotificationHelper.cancelIncomingCallNotification(context, ringing.id)
                            mainViewModel.declineIncomingCall(ringing)
                        }
                    )
                }
            }
        }

        // Optional In-App Update Dialog (only shown if forceUpdate == false; mandatory updates use ForceUpdateScreen)
        val optionalUpdate = (updateCheckResult as? UpdateCheckResult.UpdateAvailable)?.info
            ?.takeIf { !it.forceUpdate }
        if (optionalUpdate != null) {
            AlertDialog(
                onDismissRequest = { mainViewModel.dismissUpdateDialog() },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.SystemUpdate,
                        contentDescription = null,
                        tint = BrandBlue
                    )
                },
                title = {
                    Text("${strings.updateAvailableTitle} (v${optionalUpdate.latestVersionName})")
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(strings.updateAvailableDesc)
                        if (optionalUpdate.releaseNotes.isNotBlank()) {
                            Text(
                                text = optionalUpdate.releaseNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            mainViewModel.dismissUpdateDialog()
                            AppUpdateChecker.openDirectDownload(context, optionalUpdate)
                        }
                    ) {
                        Text(strings.updateDownloadNow)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mainViewModel.dismissUpdateDialog() }) {
                        Text(strings.updateLater)
                    }
                }
            )
        }
    }
}
