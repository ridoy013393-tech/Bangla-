package com.example.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BanglaChatRepository
import com.example.data.NetworkMonitor
import com.example.data.PreferencesRepository
import com.example.data.StorageRepository
import com.example.data.extractFirestoreRootError
import com.example.model.AppLanguage
import com.example.model.BlockedUser
import com.example.model.CallLog
import com.example.model.CallSession
import com.example.model.Chat
import com.example.model.ChatFontSize
import com.example.model.ChatWallpaper
import com.example.model.FavoriteContact
import com.example.model.PrivateUser
import com.example.model.PublicProfile
import com.example.model.StatusStory
import com.example.model.ThemePreference
import com.example.model.UiState
import com.example.model.UploadState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface UsernameSearchState {
    data object Idle : UsernameSearchState
    data class Searching(val normalizedQuery: String) : UsernameSearchState
    data class Found(val profile: PublicProfile) : UsernameSearchState
    data class NotFound(val normalizedQuery: String) : UsernameSearchState
    data class Error(val message: String) : UsernameSearchState
}

class MainViewModel(
    private val repository: BanglaChatRepository,
    private val storageRepository: StorageRepository,
    private val preferencesRepository: PreferencesRepository,
    networkMonitor: NetworkMonitor,
    val currentUserId: String
) : ViewModel() {

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    private val _avatarUploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val avatarUploadState: StateFlow<UploadState> = _avatarUploadState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val isNetworkOnline: StateFlow<Boolean> = networkMonitor.observeOnline()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    val myProfile: StateFlow<PublicProfile?> = repository.observePublicProfile(currentUserId)
        .catch { error ->
            val root = extractFirestoreRootError(error)
            if (root == null && error is CancellationException) throw error
            Log.w(TAG, "Profile stream error", root ?: error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val myPrivateUser: StateFlow<PrivateUser?> = repository.observePrivateUser(currentUserId)
        .catch { error ->
            val root = extractFirestoreRootError(error)
            if (root == null && error is CancellationException) throw error
            Log.w(TAG, "Private user stream error", root ?: error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val chatsState: StateFlow<UiState<List<Chat>>> = repository.observeChats(currentUserId)
        .map<List<Chat>, UiState<List<Chat>>> { UiState.Success(it) }
        .catch { error ->
            val root = extractFirestoreRootError(error)
            if (root == null && error is CancellationException) throw error
            val actual = root ?: error
            Log.w(TAG, "Chats stream error", actual)
            emit(UiState.Error(actual.localizedMessage ?: "Failed to load conversations"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val discoverableUsersState: StateFlow<UiState<List<PublicProfile>>> =
        repository.observeDiscoverableProfiles(currentUserId)
            .map<List<PublicProfile>, UiState<List<PublicProfile>>> { UiState.Success(it) }
            .catch { error ->
                val root = extractFirestoreRootError(error)
                if (root == null && error is CancellationException) throw error
                val actual = root ?: error
                Log.w(TAG, "Discoverable profiles stream error", actual)
                emit(UiState.Error(actual.localizedMessage ?: "Failed to load contacts"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val favorites: StateFlow<List<FavoriteContact>> = repository.observeFavorites(currentUserId)
        .catch { error ->
            val root = extractFirestoreRootError(error)
            if (root == null && error is CancellationException) throw error
            Log.w(TAG, "Favorites stream error", root ?: error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val blockedUsers: StateFlow<List<BlockedUser>> = repository.observeBlockedUsers(currentUserId)
        .catch { error ->
            val root = extractFirestoreRootError(error)
            if (root == null && error is CancellationException) throw error
            Log.w(TAG, "Blocked users stream error", root ?: error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val callLogs: StateFlow<List<CallLog>> = repository.observeCallLogs(currentUserId)
        .catch { error ->
            val root = extractFirestoreRootError(error)
            if (root == null && error is CancellationException) throw error
            Log.w(TAG, "Call logs stream error", root ?: error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeStatuses: StateFlow<List<StatusStory>> = repository.observeActiveStatuses()
        .catch { error ->
            val root = extractFirestoreRootError(error)
            if (root == null && error is CancellationException) throw error
            Log.w(TAG, "Statuses stream error", root ?: error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val incomingCalls: StateFlow<List<CallSession>> = repository.observeIncomingCalls(currentUserId)
        .catch { error ->
            val root = extractFirestoreRootError(error)
            if (root == null && error is CancellationException) throw error
            Log.w(TAG, "Incoming calls stream error", root ?: error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _isPostingStatus = MutableStateFlow(false)
    val isPostingStatus: StateFlow<Boolean> = _isPostingStatus.asStateFlow()

    private val _usernameSearchState = MutableStateFlow<UsernameSearchState>(UsernameSearchState.Idle)
    val usernameSearchState: StateFlow<UsernameSearchState> = _usernameSearchState.asStateFlow()
    private var usernameSearchJob: Job? = null

    private val bootstrapJob = bootstrapSession()

    private fun bootstrapSession() = viewModelScope.launch {
        val user = Firebase.auth.currentUser
        if (user != null && user.uid == currentUserId) {
            val synced = repository.syncExistingSession(user)
            if (synced.getOrNull() == true) {
                try {
                    val token = FirebaseMessaging.getInstance().token.await()
                    if (token.isNotBlank()) {
                        repository.syncFcmToken(currentUserId, token)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "FCM token sync deferred: ${e.message}")
                }
            }
        }
    }

    fun searchUserByExactUsername(rawQuery: String) {
        val clean = BanglaChatRepository.normalizeUsername(rawQuery)
        usernameSearchJob?.cancel()
        if (clean.isBlank()) {
            _usernameSearchState.value = UsernameSearchState.Idle
            return
        }
        if (!BanglaChatRepository.isValidUsernameFormat(clean)) {
            _usernameSearchState.value = UsernameSearchState.NotFound(clean)
            return
        }
        usernameSearchJob = viewModelScope.launch {
            _usernameSearchState.value = UsernameSearchState.Searching(clean)
            repository.findProfileByExactUsername(clean, currentUserId)
                .onSuccess { profile ->
                    _usernameSearchState.value = if (profile != null) {
                        UsernameSearchState.Found(profile)
                    } else {
                        UsernameSearchState.NotFound(clean)
                    }
                }
                .onFailure { err ->
                    _usernameSearchState.value = UsernameSearchState.Error(
                        err.localizedMessage ?: "Search failed"
                    )
                }
        }
    }

    fun clearUsernameSearch() {
        usernameSearchJob?.cancel()
        _usernameSearchState.value = UsernameSearchState.Idle
    }

    fun setOnlinePresence(online: Boolean) {
        viewModelScope.launch {
            bootstrapJob.join()
            repository.setOnlinePresence(currentUserId, online)
        }
    }

    suspend fun setOfflineBeforeSignOut() {
        bootstrapJob.join()
        repository.setOnlinePresence(currentUserId, false)
    }

    fun refreshChats() {
        viewModelScope.launch {
            _isRefreshing.value = true
            bootstrapJob.join()
            repository.setOnlinePresence(currentUserId, true)
            _isRefreshing.value = false
        }
    }

    fun startChatWith(peer: PublicProfile, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            val me = myProfile.value ?: PublicProfile(
                uid = currentUserId,
                name = Firebase.auth.currentUser?.displayName ?: "Bangla Chat User",
                photoUrl = Firebase.auth.currentUser?.photoUrl?.toString().orEmpty()
            )
            repository.getOrCreateChat(me, peer)
                .onSuccess { chatId -> onChatReady(chatId) }
                .onFailure { err ->
                    _snackbarEvents.tryEmit(err.localizedMessage ?: "Unable to start chat")
                }
        }
    }

    fun createGroupChat(
        groupName: String,
        selectedMembers: List<PublicProfile>,
        photoUri: Uri?,
        successToast: String,
        onGroupCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val me = myProfile.value ?: PublicProfile(
                uid = currentUserId,
                name = Firebase.auth.currentUser?.displayName ?: "Bangla Chat User",
                photoUrl = Firebase.auth.currentUser?.photoUrl?.toString().orEmpty()
            )
            var photoUrl = ""
            if (photoUri != null) {
                storageRepository.uploadGroupPhoto(currentUserId, photoUri) {}
                    .onSuccess { photoUrl = it.downloadUrl }
                    .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Group photo upload failed") }
            }
            repository.createGroupChat(me, selectedMembers, groupName, photoUrl)
                .onSuccess { chatId ->
                    _snackbarEvents.tryEmit(successToast)
                    onGroupCreated(chatId)
                }
                .onFailure { err ->
                    _snackbarEvents.tryEmit(err.localizedMessage ?: "Unable to create group")
                }
        }
    }

    fun postTextStatus(text: String, backgroundColorHex: Long, successToast: String) {
        viewModelScope.launch {
            _isPostingStatus.value = true
            val me = myProfile.value ?: PublicProfile(
                uid = currentUserId,
                name = Firebase.auth.currentUser?.displayName ?: "Bangla Chat User",
                photoUrl = Firebase.auth.currentUser?.photoUrl?.toString().orEmpty()
            )
            repository.postStatus(me, "text", text, null, backgroundColorHex)
                .onSuccess { _snackbarEvents.tryEmit(successToast) }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to post status") }
            _isPostingStatus.value = false
        }
    }

    fun postPhotoStatus(uri: Uri, caption: String, successToast: String) {
        viewModelScope.launch {
            _isPostingStatus.value = true
            val me = myProfile.value ?: PublicProfile(
                uid = currentUserId,
                name = Firebase.auth.currentUser?.displayName ?: "Bangla Chat User",
                photoUrl = Firebase.auth.currentUser?.photoUrl?.toString().orEmpty()
            )
            storageRepository.uploadStatusImage(currentUserId, uri) {}
                .onSuccess { uploaded ->
                    repository.postStatus(me, "image", caption, uploaded.downloadUrl)
                        .onSuccess { _snackbarEvents.tryEmit(successToast) }
                        .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to post status") }
                }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Status image upload failed") }
            _isPostingStatus.value = false
        }
    }

    fun markStatusViewed(status: StatusStory) {
        viewModelScope.launch {
            repository.markStatusViewed(status, currentUserId)
        }
    }

    fun deleteStatus(statusId: String) {
        viewModelScope.launch {
            repository.deleteStatus(statusId)
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to delete status") }
        }
    }

    fun declineIncomingCall(session: CallSession) {
        viewModelScope.launch {
            repository.updateCallSessionStatus(session.id, "declined")
            repository.recordCallLog(
                uid = currentUserId,
                peerUid = session.callerUid,
                peerName = session.callerName,
                peerPhotoUrl = session.callerPhotoUrl,
                direction = "missed",
                callType = session.callType,
                durationSeconds = 0L
            )
        }
    }

    fun toggleFavorite(profile: PublicProfile, isCurrentlyFavorite: Boolean) {
        viewModelScope.launch {
            val res = if (isCurrentlyFavorite) {
                repository.removeFavorite(currentUserId, profile.uid)
            } else {
                repository.addFavorite(currentUserId, profile)
            }
            res.onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update favorites")
            }
        }
    }

    fun blockUser(profile: PublicProfile) {
        viewModelScope.launch {
            repository.blockUser(currentUserId, profile)
                .onFailure {
                    _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to block user")
                }
        }
    }

    fun unblockUser(blockedUid: String) {
        viewModelScope.launch {
            repository.unblockUser(currentUserId, blockedUid)
                .onFailure {
                    _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to unblock user")
                }
        }
    }

    fun submitReport(reportedUid: String, reason: String, details: String, successMessage: String) {
        viewModelScope.launch {
            repository.submitReport(reportedUid, reason, details)
                .onSuccess { _snackbarEvents.tryEmit(successMessage) }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to submit report") }
        }
    }

    fun updateProfile(
        name: String,
        username: String,
        about: String,
        phone: String,
        successMessage: String,
        usernameTakenMessage: String? = null
    ) {
        viewModelScope.launch {
            repository.updateProfile(currentUserId, name, username, about, phone)
                .onSuccess { _snackbarEvents.tryEmit(successMessage) }
                .onFailure { err ->
                    val message = if (err is BanglaChatRepository.UsernameAlreadyTakenException && usernameTakenMessage != null) {
                        usernameTakenMessage
                    } else {
                        err.localizedMessage ?: "Failed to update profile"
                    }
                    _snackbarEvents.tryEmit(message)
                }
        }
    }

    fun uploadNewProfilePhoto(uri: Uri, successMessage: String) {
        viewModelScope.launch {
            _avatarUploadState.value = UploadState.Uploading(0.05f, "avatar.jpg")
            val result = storageRepository.uploadProfilePhoto(currentUserId, uri) { progress ->
                _avatarUploadState.value = UploadState.Uploading(progress, "avatar.jpg")
            }
            result.onSuccess { uploaded ->
                val current = myProfile.value
                val phone = myPrivateUser.value?.phone.orEmpty()
                repository.updateProfile(
                    uid = currentUserId,
                    name = current?.name?.ifBlank { "Bangla Chat User" } ?: "Bangla Chat User",
                    username = current?.username?.ifBlank { "user_${currentUserId.take(6).lowercase()}" }
                        ?: "user_${currentUserId.take(6).lowercase()}",
                    about = current?.about.orEmpty(),
                    phone = phone,
                    photoUrl = uploaded.downloadUrl
                ).onSuccess {
                    _avatarUploadState.value = UploadState.Idle
                    _snackbarEvents.tryEmit(successMessage)
                }.onFailure { err ->
                    _avatarUploadState.value = UploadState.Failed(err.localizedMessage ?: "Profile update failed")
                }
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Photo upload failed (verify Cloudinary Secrets)"
                _avatarUploadState.value = UploadState.Failed(msg)
                _snackbarEvents.tryEmit(msg)
            }
        }
    }

    fun updatePrivacySettings(
        discoverable: Boolean,
        showOnlineStatus: Boolean,
        showLastSeen: Boolean,
        showReadReceipts: Boolean,
        showProfilePhoto: Boolean,
        showAbout: Boolean
    ) {
        viewModelScope.launch {
            repository.updatePrivacySettings(
                currentUserId,
                discoverable,
                showOnlineStatus,
                showLastSeen,
                showReadReceipts,
                showProfilePhoto,
                showAbout
            ).onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to save privacy settings")
            }
        }
    }

    fun toggleChatPin(chat: Chat) {
        viewModelScope.launch {
            repository.updateChatParticipantFlags(chat, currentUserId, togglePin = true)
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update chat") }
        }
    }

    fun toggleChatMute(chat: Chat) {
        viewModelScope.launch {
            repository.updateChatParticipantFlags(chat, currentUserId, toggleMute = true)
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update chat") }
        }
    }

    fun toggleChatArchive(chat: Chat) {
        viewModelScope.launch {
            repository.updateChatParticipantFlags(chat, currentUserId, toggleArchive = true)
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update chat") }
        }
    }

    fun updateMessagePreviewSetting(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateMessagePreviewSetting(currentUserId, enabled)
                .onFailure {
                    _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update notification setting")
                }
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { preferencesRepository.setLanguage(language) }
    }

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch { preferencesRepository.setTheme(theme) }
    }

    fun setChatWallpaper(wallpaper: ChatWallpaper) {
        viewModelScope.launch { preferencesRepository.setChatWallpaper(wallpaper) }
    }

    fun setChatFontSize(fontSize: ChatFontSize) {
        viewModelScope.launch { preferencesRepository.setChatFontSize(fontSize) }
    }

    fun setScreenSecurityEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setScreenSecurityEnabled(enabled) }
    }

    fun enableAppLock(rawPin: String, toastMessage: String) {
        viewModelScope.launch {
            preferencesRepository.setAppLockPin(rawPin)
            _snackbarEvents.tryEmit(toastMessage)
        }
    }

    fun disableAppLock(toastMessage: String) {
        viewModelScope.launch {
            preferencesRepository.disableAppLock()
            _snackbarEvents.tryEmit(toastMessage)
        }
    }

    fun dismissOnlineBanner() {
        viewModelScope.launch { preferencesRepository.setOnlineBannerDismissed(true) }
    }

    suspend fun deleteUserFirestoreRecords(uid: String) {
        repository.deleteUserFirestoreData(uid)
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}
