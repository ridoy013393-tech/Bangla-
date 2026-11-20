package com.example

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.data.NetworkMonitor
import com.example.data.StorageRepository
import com.example.data.authStateFlow
import com.example.data.configureAppCheck
import com.example.data.deleteCurrentAccountWithReauth
import com.example.data.signOutUser
import com.example.model.PublicProfile
import com.example.model.UiState
import com.example.model.UserPreferences
import com.example.service.BanglaChatMessagingService
import com.example.ui.components.BanglaChatLogoMark
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.stringsForLanguage
import com.example.ui.screens.ActiveCallRequest
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.AuthEntryMode
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IncomingCallBanner
import com.example.ui.screens.LiveCallScreen
import com.example.ui.screens.RegistrationCompletionScreen
import com.example.ui.theme.BanglaChatTheme
import com.example.ui.theme.BrandBlue
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val deepLinkChatId = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        extractChatDeepLink(intent)

        setContent {
            val context = LocalContext.current
            val app = remember(context) { context.applicationContext as BanglaChatApplication }
            val prefsRepository = app.preferencesRepository
            val preferences by prefsRepository.preferencesFlow
                .collectAsStateWithLifecycle(initialValue = UserPreferences())
            val pendingChatId by deepLinkChatId.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()

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
                    if (event == Lifecycle.Event.ON_STOP && preferences.appLockEnabled) {
                        isUnlocked = false
                    }
                }
                activityLifecycle.lifecycle.addObserver(observer)
                onDispose { activityLifecycle.lifecycle.removeObserver(observer) }
            }

            BanglaChatTheme(themePreference = preferences.theme) {
                CompositionLocalProvider(
                    LocalAppStrings provides stringsForLanguage(preferences.language)
                ) {
                    if (preferences.appLockEnabled && !isUnlocked) {
                        AppLockScreen(
                            expectedPinHash = preferences.appLockPinHash,
                            onUnlocked = { isUnlocked = true }
                        )
                    } else {
                        BanglaChatAppRoot(
                            preferences = preferences,
                            pendingDeepLinkChatId = pendingChatId,
                            onConsumeDeepLink = { deepLinkChatId.value = null },
                            onSetLanguage = { lang -> scope.launch { prefsRepository.setLanguage(lang) } }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractChatDeepLink(intent)
    }

    private fun extractChatDeepLink(intent: Intent?) {
        val chatId = intent?.getStringExtra(BanglaChatMessagingService.EXTRA_CHAT_ID)
        if (!chatId.isNullOrBlank()) {
            deepLinkChatId.value = chatId
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

    val user = currentUser
    if (user == null) {
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BanglaChatLogoMark(size = 84.dp)
                    Spacer(modifier = Modifier.height(20.dp))
                    CircularProgressIndicator(color = BrandBlue)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = strings.checkingAccount,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                        onConsumeDeepLink = onConsumeDeepLink
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
    onConsumeDeepLink: () -> Unit
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

    // Lifecycle-driven online presence updates
    DisposableEffect(lifecycleOwner, currentUserId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mainViewModel.setOnlinePresence(true)
                Lifecycle.Event.ON_STOP -> mainViewModel.setOnlinePresence(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var activeChatId by rememberSaveable(currentUserId) { mutableStateOf<String?>(null) }

    LaunchedEffect(pendingDeepLinkChatId) {
        if (!pendingDeepLinkChatId.isNullOrBlank()) {
            activeChatId = pendingDeepLinkChatId
            onConsumeDeepLink()
        }
    }

    val myProfile by mainViewModel.myProfile.collectAsStateWithLifecycle()
    val chatsState by mainViewModel.chatsState.collectAsStateWithLifecycle()
    val discoverableUsersState by mainViewModel.discoverableUsersState.collectAsStateWithLifecycle()
    val blockedUsers by mainViewModel.blockedUsers.collectAsStateWithLifecycle()
    val incomingCalls by mainViewModel.incomingCalls.collectAsStateWithLifecycle()
    val blockedUids = remember(blockedUsers) { blockedUsers.map { it.blockedUid }.toSet() }
    val allChats = (chatsState as? UiState.Success)?.data.orEmpty()
    val discoverableContacts = (discoverableUsersState as? UiState.Success)?.data.orEmpty()
    val strings = LocalAppStrings.current

    val callRepository = remember(app) { app.chatRepository }
    var activeCallRequest by remember { mutableStateOf<ActiveCallRequest?>(null) }

    val effectiveMyProfile = myProfile ?: PublicProfile(
        uid = currentUserId,
        name = Firebase.auth.currentUser?.displayName ?: "Bangla Chat User",
        photoUrl = Firebase.auth.currentUser?.photoUrl?.toString().orEmpty()
    )

    val currentCall = activeCallRequest
    if (currentCall != null) {
        LiveCallScreen(
            request = currentCall,
            myProfile = effectiveMyProfile,
            repository = callRepository,
            onCallClosed = { activeCallRequest = null }
        )
        return
    }

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
                    onBack = { activeChatId = null },
                    onStartVoiceCall = { peer ->
                        activeCallRequest = ActiveCallRequest.Outgoing(peer, "audio")
                    },
                    onStartVideoCall = { peer ->
                        activeCallRequest = ActiveCallRequest.Outgoing(peer, "video")
                    },
                    onBlockPeer = { peer -> mainViewModel.blockUser(peer) },
                    onUnblockPeer = { uid -> mainViewModel.unblockUser(uid) },
                    onReportPeer = { uid, reason, details ->
                        mainViewModel.submitReport(uid, reason, details, strings.reportSubmitted)
                    }
                )
            }
        } else {
            HomeScreen(
                viewModel = mainViewModel,
                preferences = preferences,
                onOpenChat = { chatId -> activeChatId = chatId },
                onStartCall = { peer, callType ->
                    activeCallRequest = ActiveCallRequest.Outgoing(peer, callType)
                },
                onLogout = {
                    signOutUser(
                        credentialManager = credentialManager,
                        scope = scope,
                        onBeforeSignOut = { mainViewModel.setOfflineBeforeSignOut() }
                    )
                },
                onDeleteAccount = {
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

        incomingCalls.firstOrNull()?.let { ringing ->
            Box(modifier = Modifier.align(Alignment.TopCenter)) {
                IncomingCallBanner(
                    session = ringing,
                    onAccept = {
                        activeCallRequest = ActiveCallRequest.Incoming(ringing)
                    },
                    onDecline = {
                        mainViewModel.declineIncomingCall(ringing)
                    }
                )
            }
        }
    }
}
