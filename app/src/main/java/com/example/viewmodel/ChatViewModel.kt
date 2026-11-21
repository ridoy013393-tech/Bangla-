package com.example.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiAssistantRepository
import com.example.data.BanglaChatRepository
import com.example.data.ContentModerationEngine
import com.example.data.MessageModerationOutcome
import com.example.data.ModerationViolationType
import com.example.data.StorageRepository
import com.example.data.extractFirestoreRootError
import com.example.model.AiRewriteTone
import com.example.model.AiTaskState
import com.example.model.AppLanguage
import com.example.model.Chat
import com.example.model.ChatMessage
import com.example.model.ModerationSettings
import com.example.model.PublicProfile
import com.example.model.UiState
import com.example.model.UploadState
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val repository: BanglaChatRepository,
    private val storageRepository: StorageRepository,
    private val aiRepository: AiAssistantRepository = AiAssistantRepository(),
    val chatId: String,
    val currentUserId: String
) : ViewModel() {

    private val _pageLimit = MutableStateFlow(50L)
    val pageLimit: StateFlow<Long> = _pageLimit.asStateFlow()

    private val _replyTarget = MutableStateFlow<ChatMessage?>(null)
    val replyTarget: StateFlow<ChatMessage?> = _replyTarget.asStateFlow()

    private val _editingMessage = MutableStateFlow<ChatMessage?>(null)
    val editingMessage: StateFlow<ChatMessage?> = _editingMessage.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    private val _groupPhotoUploading = MutableStateFlow(false)
    val groupPhotoUploading: StateFlow<Boolean> = _groupPhotoUploading.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    // Smart Bangla AI states
    private val _translatedMessages = MutableStateFlow<Map<String, String>>(emptyMap())
    val translatedMessages: StateFlow<Map<String, String>> = _translatedMessages.asStateFlow()

    private val _translatingMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val translatingMessageIds: StateFlow<Set<String>> = _translatingMessageIds.asStateFlow()

    private val _quickReplies = MutableStateFlow<List<String>>(emptyList())
    val quickReplies: StateFlow<List<String>> = _quickReplies.asStateFlow()

    private val _quickRepliesLoading = MutableStateFlow(false)
    val quickRepliesLoading: StateFlow<Boolean> = _quickRepliesLoading.asStateFlow()

    private val _summaryState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val summaryState: StateFlow<AiTaskState> = _summaryState.asStateFlow()

    private val _writerState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val writerState: StateFlow<AiTaskState> = _writerState.asStateFlow()

    private val _explainState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val explainState: StateFlow<AiTaskState> = _explainState.asStateFlow()

    private val _explainSourceText = MutableStateFlow("")
    val explainSourceText: StateFlow<String> = _explainSourceText.asStateFlow()

    private var typingStopJob: Job? = null
    private var pendingRetryUri: Pair<Uri, Boolean>? = null

    val chat: StateFlow<Chat?> = repository.observeChat(chatId)
        .catch { error ->
            val root = extractFirestoreRootError(error)
            if (root == null && error is CancellationException) throw error
            Log.w(TAG, "Chat stream error", root ?: error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val peerProfile: StateFlow<PublicProfile?> = chat
        .map { it?.peerUid(currentUserId).orEmpty() }
        .flatMapLatest { peerUid ->
            if (peerUid.isBlank()) flowOf(null) else repository.observePublicProfile(peerUid)
        }
        .catch { error ->
            val root = extractFirestoreRootError(error)
            if (root == null && error is CancellationException) throw error
            Log.w(TAG, "Peer profile stream error", root ?: error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val moderationSettings: StateFlow<ModerationSettings> = repository.observeModerationSettings()
        .catch { error ->
            val root = extractFirestoreRootError(error)
            if (root == null && error is CancellationException) throw error
            emit(ModerationSettings())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ModerationSettings())

    val messagesState: StateFlow<UiState<List<ChatMessage>>> = combine(
        _pageLimit.flatMapLatest { limit ->
            repository.observeMessages(chatId, currentUserId, limit)
                .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
                .catch { error ->
                    val root = extractFirestoreRootError(error)
                    if (root == null && error is CancellationException) throw error
                    val actual = root ?: error
                    Log.w(TAG, "Messages stream error", actual)
                    emit(UiState.Error(actual.localizedMessage ?: "Failed to load messages"))
                }
        },
        chat
    ) { state, currentChat ->
        val clearedMs = currentChat?.clearedAtMillisFor(currentUserId) ?: 0L
        if (state is UiState.Success && clearedMs > 0L) {
            val visible = state.data.filter { msg ->
                val msgMs = msg.timestamp?.toDate()?.time ?: Long.MAX_VALUE
                msgMs > clearedMs
            }
            UiState.Success(visible)
        } else {
            state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    fun loadOlderMessages() {
        _pageLimit.value = (_pageLimit.value + 50L).coerceAtMost(500L)
    }

    fun setReplyTarget(message: ChatMessage?) {
        if (message != null) _editingMessage.value = null
        _replyTarget.value = message
    }

    fun setEditingMessage(message: ChatMessage?) {
        if (message != null) _replyTarget.value = null
        _editingMessage.value = message
    }

    fun saveEditedMessage(
        newText: String,
        senderProfile: PublicProfile? = null,
        language: AppLanguage = AppLanguage.BANGLA
    ) {
        val target = _editingMessage.value ?: return
        val settings = moderationSettings.value
        val outcome = ContentModerationEngine.evaluateOutgoingMessage(
            senderUid = currentUserId,
            rawText = newText,
            settings = settings.copy(spamProtectionEnabled = false)
        )
        when (outcome) {
            is MessageModerationOutcome.Block -> {
                _snackbarEvents.tryEmit(formatViolationWarning(outcome.violationType, outcome.cooldownSeconds, language))
                if (outcome.shouldAlertAdmin && senderProfile != null) {
                    viewModelScope.launch {
                        repository.notifyAutoModerationViolation(
                            senderProfile = senderProfile,
                            violationType = outcome.violationType,
                            flaggedSnippet = newText,
                            matchedWords = outcome.matchedWords,
                            chatId = chatId
                        )
                    }
                }
                return
            }
            is MessageModerationOutcome.Allow -> {
                _editingMessage.value = null
                if (outcome.wasMasked) {
                    _snackbarEvents.tryEmit(
                        if (language == AppLanguage.BANGLA) "অশ্লীল শব্দ স্বয়ংক্রিয়ভাবে ফিল্টার করা হয়েছে"
                        else "Restricted words were automatically masked"
                    )
                }
                viewModelScope.launch {
                    repository.editOwnMessage(chatId, target.id, outcome.finalText)
                        .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Edit failed") }
                }
            }
        }
    }

    fun onInputChanged(text: String) {
        val currentChat = chat.value ?: return
        typingStopJob?.cancel()
        if (text.isNotBlank()) {
            viewModelScope.launch {
                repository.setTypingStatus(currentChat, currentUserId, true)
            }
            typingStopJob = viewModelScope.launch {
                delay(3000L)
                chat.value?.let { repository.setTypingStatus(it, currentUserId, false) }
            }
        } else {
            viewModelScope.launch {
                repository.setTypingStatus(currentChat, currentUserId, false)
            }
        }
    }

    fun markVisibleMessagesRead(myProfile: PublicProfile?, messages: List<ChatMessage>) {
        val currentChat = chat.value ?: return
        val sendReadReceipts = myProfile?.showReadReceipts ?: true
        viewModelScope.launch {
            repository.markConversationRead(currentChat, currentUserId, messages, sendReadReceipts)
        }
    }

    private fun formatViolationWarning(
        type: ModerationViolationType,
        cooldownSeconds: Int,
        language: AppLanguage
    ): String = when (type) {
        ModerationViolationType.PROFANITY -> if (language == AppLanguage.BANGLA) {
            "অশ্লীল বা নিষিদ্ধ শব্দ ব্যবহার করায় মেসেজটি পাঠানো যায়নি।"
        } else {
            "Message blocked: prohibited or abusive language detected."
        }
        ModerationViolationType.BURST_SPAM -> if (language == AppLanguage.BANGLA) {
            "অনেক দ্রুত মেসেজ পাঠাচ্ছেন! দয়া করে ${cooldownSeconds.coerceAtLeast(3)} সেকেন্ড অপেক্ষা করুন।"
        } else {
            "You're sending messages too fast. Please wait ${cooldownSeconds.coerceAtLeast(3)}s."
        }
        ModerationViolationType.DUPLICATE_SPAM -> if (language == AppLanguage.BANGLA) {
            "একই মেসেজ বারবার স্প্যাম করা নিষিদ্ধ।"
        } else {
            "Repeated duplicate spam messages are blocked."
        }
    }

    fun sendTextMessage(
        senderProfile: PublicProfile,
        text: String,
        language: AppLanguage = AppLanguage.BANGLA
    ) {
        val currentChat = chat.value ?: return
        val settings = moderationSettings.value
        val outcome = ContentModerationEngine.evaluateOutgoingMessage(
            senderUid = currentUserId,
            rawText = text,
            settings = settings
        )

        val finalMessageText = when (outcome) {
            is MessageModerationOutcome.Block -> {
                _snackbarEvents.tryEmit(
                    formatViolationWarning(outcome.violationType, outcome.cooldownSeconds, language)
                )
                if (outcome.shouldAlertAdmin) {
                    viewModelScope.launch {
                        repository.notifyAutoModerationViolation(
                            senderProfile = senderProfile,
                            violationType = outcome.violationType,
                            flaggedSnippet = text,
                            matchedWords = outcome.matchedWords,
                            chatId = chatId
                        )
                    }
                }
                return
            }
            is MessageModerationOutcome.Allow -> {
                if (outcome.wasMasked) {
                    _snackbarEvents.tryEmit(
                        if (language == AppLanguage.BANGLA) "অশ্লীল শব্দ স্বয়ংক্রিয়ভাবে ফিল্টার করা হয়েছে"
                        else "Restricted words were automatically masked"
                    )
                }
                if (outcome.shouldAlertAdmin && outcome.violationType != null) {
                    viewModelScope.launch {
                        repository.notifyAutoModerationViolation(
                            senderProfile = senderProfile,
                            violationType = outcome.violationType,
                            flaggedSnippet = text,
                            matchedWords = outcome.matchedWords,
                            chatId = chatId
                        )
                    }
                }
                outcome.finalText
            }
        }

        val reply = _replyTarget.value
        val replySender = reply?.let { r ->
            if (r.senderId == currentUserId) senderProfile.name
            else currentChat.participantNames[r.senderId].orEmpty()
        }
        _replyTarget.value = null
        typingStopJob?.cancel()

        viewModelScope.launch {
            repository.sendMessage(
                chat = currentChat,
                senderProfile = senderProfile,
                text = finalMessageText,
                type = "text",
                replyTo = reply,
                replySenderName = replySender
            ).onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to send message")
            }
        }
    }

    fun uploadAndSendMedia(
        senderProfile: PublicProfile,
        uri: Uri,
        isImage: Boolean,
        caption: String = "",
        viewOnce: Boolean = false
    ) {
        val currentChat = chat.value ?: return
        pendingRetryUri = uri to isImage
        val reply = _replyTarget.value
        val replySender = reply?.let { r ->
            if (r.senderId == currentUserId) senderProfile.name
            else currentChat.participantNames[r.senderId].orEmpty()
        }

        viewModelScope.launch {
            _uploadState.value = UploadState.Uploading(0.05f, if (isImage) "image.jpg" else "document")
            val uploadResult = storageRepository.uploadChatMedia(
                chatId = chatId,
                senderUid = currentUserId,
                uri = uri,
                isImage = isImage
            ) { progress ->
                _uploadState.value = UploadState.Uploading(progress, if (isImage) "image.jpg" else "document")
            }

            uploadResult.onSuccess { uploaded ->
                _replyTarget.value = null
                repository.sendMessage(
                    chat = currentChat,
                    senderProfile = senderProfile,
                    text = caption,
                    type = if (isImage) "image" else "document",
                    mediaUrl = uploaded.downloadUrl,
                    mediaName = uploaded.fileName,
                    mediaSizeBytes = uploaded.sizeBytes,
                    replyTo = reply,
                    replySenderName = replySender,
                    viewOnce = viewOnce && isImage
                ).onSuccess {
                    pendingRetryUri = null
                    _uploadState.value = UploadState.Idle
                }.onFailure { err ->
                    _uploadState.value = UploadState.Failed(err.localizedMessage ?: "Message send failed")
                }
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Upload failed — verify Cloudinary Secrets are set"
                _uploadState.value = UploadState.Failed(msg)
                _snackbarEvents.tryEmit(msg)
            }
        }
    }

    fun sendPollMessage(senderProfile: PublicProfile, question: String, options: List<String>) {
        val currentChat = chat.value ?: return
        viewModelScope.launch {
            repository.sendPollMessage(currentChat, senderProfile, question, options)
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to create poll") }
        }
    }

    fun sendStickerMessage(senderProfile: PublicProfile, stickerId: String, stickerText: String) {
        val currentChat = chat.value ?: return
        val reply = _replyTarget.value
        val replySender = reply?.let { r ->
            if (r.senderId == currentUserId) senderProfile.name
            else currentChat.participantNames[r.senderId].orEmpty()
        }
        _replyTarget.value = null
        viewModelScope.launch {
            repository.sendMessage(
                chat = currentChat,
                senderProfile = senderProfile,
                text = stickerText,
                type = "sticker",
                stickerId = stickerId,
                replyTo = reply,
                replySenderName = replySender
            ).onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to send sticker") }
        }
    }

    fun sendLocationMessage(
        senderProfile: PublicProfile,
        latitude: Double,
        longitude: Double,
        label: String
    ) {
        val currentChat = chat.value ?: return
        val reply = _replyTarget.value
        val replySender = reply?.let { r ->
            if (r.senderId == currentUserId) senderProfile.name
            else currentChat.participantNames[r.senderId].orEmpty()
        }
        _replyTarget.value = null
        viewModelScope.launch {
            repository.sendMessage(
                chat = currentChat,
                senderProfile = senderProfile,
                text = label,
                type = "location",
                latitude = latitude,
                longitude = longitude,
                locationLabel = label,
                replyTo = reply,
                replySenderName = replySender
            ).onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to send location") }
        }
    }

    fun sendContactCardMessage(senderProfile: PublicProfile, contact: PublicProfile) {
        val currentChat = chat.value ?: return
        val reply = _replyTarget.value
        val replySender = reply?.let { r ->
            if (r.senderId == currentUserId) senderProfile.name
            else currentChat.participantNames[r.senderId].orEmpty()
        }
        _replyTarget.value = null
        viewModelScope.launch {
            repository.sendMessage(
                chat = currentChat,
                senderProfile = senderProfile,
                text = "${contact.name} (@${contact.username})",
                type = "contact",
                sharedContactUid = contact.uid,
                sharedContactName = contact.name,
                sharedContactUsername = contact.username,
                sharedContactPhotoUrl = contact.visiblePhotoUrl(),
                replyTo = reply,
                replySenderName = replySender
            ).onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to send contact") }
        }
    }

    fun voteOnPoll(message: ChatMessage, optionIndex: Int) {
        viewModelScope.launch {
            repository.voteOnPoll(chatId, message, currentUserId, optionIndex)
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to vote") }
        }
    }

    fun markViewOnceOpened(message: ChatMessage) {
        viewModelScope.launch {
            repository.markViewOnceOpened(chatId, message, currentUserId)
        }
    }

    fun retryLastUpload(senderProfile: PublicProfile) {
        val pair = pendingRetryUri ?: return
        uploadAndSendMedia(senderProfile, pair.first, pair.second)
    }

    fun dismissUploadError() {
        _uploadState.value = UploadState.Idle
    }

    fun toggleReaction(message: ChatMessage, emoji: String) {
        viewModelScope.launch {
            repository.toggleReaction(chatId, message, currentUserId, emoji)
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Reaction failed") }
        }
    }

    fun deleteOwnMessage(message: ChatMessage) {
        viewModelScope.launch {
            repository.deleteOwnMessage(chatId, message.id)
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Delete failed") }
        }
    }

    fun deleteMessageForMe(message: ChatMessage, successToast: String) {
        viewModelScope.launch {
            repository.deleteMessageForMe(chatId, message.id, currentUserId)
                .onSuccess { _snackbarEvents.tryEmit(successToast) }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Delete failed") }
        }
    }

    fun toggleStarMessage(message: ChatMessage) {
        viewModelScope.launch {
            repository.toggleStarMessage(chatId, message, currentUserId)
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update star") }
        }
    }

    fun pinMessage(message: ChatMessage, senderName: String) {
        viewModelScope.launch {
            repository.setPinnedMessage(chatId, message, senderName)
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to pin message") }
        }
    }

    fun unpinMessage() {
        viewModelScope.launch {
            repository.setPinnedMessage(chatId, null, "")
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to unpin message") }
        }
    }

    fun setDisappearingHours(hours: Long) {
        viewModelScope.launch {
            repository.setDisappearingHours(chatId, hours)
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update timer") }
        }
    }

    fun forwardMessage(
        targetChat: Chat,
        senderProfile: PublicProfile,
        original: ChatMessage,
        successMessage: String
    ) {
        viewModelScope.launch {
            repository.sendMessage(
                chat = targetChat,
                senderProfile = senderProfile,
                text = original.text,
                type = original.type,
                mediaUrl = original.mediaUrl.takeIf { it.isNotBlank() },
                mediaName = original.mediaName.takeIf { it.isNotBlank() },
                mediaSizeBytes = original.mediaSizeBytes.takeIf { it > 0L },
                stickerId = original.stickerId.takeIf { it.isNotBlank() },
                latitude = if (original.type == "location") original.latitude else null,
                longitude = if (original.type == "location") original.longitude else null,
                locationLabel = original.locationLabel.takeIf { it.isNotBlank() },
                sharedContactUid = original.sharedContactUid.takeIf { it.isNotBlank() },
                sharedContactName = original.sharedContactName.takeIf { it.isNotBlank() },
                sharedContactUsername = original.sharedContactUsername.takeIf { it.isNotBlank() },
                sharedContactPhotoUrl = original.sharedContactPhotoUrl.takeIf { it.isNotBlank() },
                forwarded = true
            ).onSuccess {
                _snackbarEvents.tryEmit(successMessage)
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Forward failed")
            }
        }
    }

    fun uploadAndSendVoiceNote(
        senderProfile: PublicProfile,
        audioFile: File,
        durationSec: Int
    ) {
        val currentChat = chat.value ?: return
        val reply = _replyTarget.value
        val replySender = reply?.let { r ->
            if (r.senderId == currentUserId) senderProfile.name
            else currentChat.participantNames[r.senderId].orEmpty()
        }
        viewModelScope.launch {
            _uploadState.value = UploadState.Uploading(0.05f, "voice.m4a")
            val uploadResult = storageRepository.uploadVoiceNote(
                chatId = chatId,
                senderUid = currentUserId,
                audioFile = audioFile
            ) { progress ->
                _uploadState.value = UploadState.Uploading(progress, "voice.m4a")
            }
            uploadResult.onSuccess { uploaded ->
                _replyTarget.value = null
                repository.sendMessage(
                    chat = currentChat,
                    senderProfile = senderProfile,
                    text = "",
                    type = "audio",
                    mediaUrl = uploaded.downloadUrl,
                    mediaName = uploaded.fileName,
                    mediaSizeBytes = uploaded.sizeBytes,
                    audioDurationSec = durationSec.toLong(),
                    replyTo = reply,
                    replySenderName = replySender
                ).onSuccess {
                    _uploadState.value = UploadState.Idle
                }.onFailure { err ->
                    _uploadState.value = UploadState.Failed(err.localizedMessage ?: "Voice send failed")
                }
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Voice upload failed"
                _uploadState.value = UploadState.Failed(msg)
                _snackbarEvents.tryEmit(msg)
            }
        }
    }

    suspend fun lookupUserByExactUsername(rawQuery: String): Result<PublicProfile?> =
        repository.findProfileByExactUsername(rawQuery, currentUserId)

    fun renameGroup(actorProfile: PublicProfile?, newName: String, successToast: String = "") {
        val currentChat = chat.value ?: return
        viewModelScope.launch {
            repository.renameGroup(currentChat, actorProfile, newName)
                .onSuccess { if (successToast.isNotBlank()) _snackbarEvents.tryEmit(successToast) }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Rename failed") }
        }
    }

    fun updateGroupDescription(actorProfile: PublicProfile?, description: String, successToast: String = "") {
        val currentChat = chat.value ?: return
        viewModelScope.launch {
            repository.updateGroupDescription(currentChat, actorProfile, description)
                .onSuccess { if (successToast.isNotBlank()) _snackbarEvents.tryEmit(successToast) }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update description") }
        }
    }

    fun uploadAndUpdateGroupPhoto(actorProfile: PublicProfile?, uri: Uri, successToast: String) {
        val currentChat = chat.value ?: return
        viewModelScope.launch {
            _groupPhotoUploading.value = true
            storageRepository.uploadGroupPhoto(currentUserId, uri) {}
                .onSuccess { uploaded ->
                    repository.updateGroupPhoto(currentChat, actorProfile, uploaded.downloadUrl)
                        .onSuccess { _snackbarEvents.tryEmit(successToast) }
                        .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to save group photo") }
                }
                .onFailure { err ->
                    _snackbarEvents.tryEmit(err.localizedMessage ?: "Group photo upload failed")
                }
            _groupPhotoUploading.value = false
        }
    }

    fun addGroupMembers(
        actorProfile: PublicProfile?,
        members: List<PublicProfile>,
        successToast: String = ""
    ) {
        val currentChat = chat.value ?: return
        viewModelScope.launch {
            repository.addGroupMembers(currentChat, actorProfile, members)
                .onSuccess { if (successToast.isNotBlank()) _snackbarEvents.tryEmit(successToast) }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to add members") }
        }
    }

    fun leaveGroup(actorProfile: PublicProfile?, onLeft: () -> Unit) {
        val currentChat = chat.value ?: return
        viewModelScope.launch {
            repository.leaveGroup(currentChat, currentUserId, actorProfile)
                .onSuccess { onLeft() }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to leave group") }
        }
    }

    fun deleteGroup(successToast: String, onDeleted: () -> Unit) {
        val currentChat = chat.value ?: return
        viewModelScope.launch {
            repository.deleteGroupChat(currentChat)
                .onSuccess {
                    if (successToast.isNotBlank()) _snackbarEvents.tryEmit(successToast)
                    onDeleted()
                }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to delete group") }
        }
    }

    fun toggleGroupOnlyAdminsCanSend(actorProfile: PublicProfile?, onlyAdmins: Boolean) {
        val currentChat = chat.value ?: return
        viewModelScope.launch {
            repository.toggleGroupOnlyAdminsCanSend(currentChat, actorProfile, onlyAdmins)
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update group setting") }
        }
    }

    fun toggleGroupMemberAdmin(actorProfile: PublicProfile?, targetUid: String, toastMessage: String) {
        val currentChat = chat.value ?: return
        viewModelScope.launch {
            repository.toggleGroupMemberAdmin(currentChat, actorProfile, targetUid)
                .onSuccess { _snackbarEvents.tryEmit(toastMessage) }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update admin role") }
        }
    }

    fun removeGroupMember(actorProfile: PublicProfile?, targetUid: String, toastMessage: String) {
        val currentChat = chat.value ?: return
        viewModelScope.launch {
            repository.removeGroupMember(currentChat, actorProfile, targetUid)
                .onSuccess { _snackbarEvents.tryEmit(toastMessage) }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to remove member") }
        }
    }

    fun clearChatForMe(successToast: String) {
        val currentChat = chat.value ?: return
        viewModelScope.launch {
            repository.clearChatForMe(currentChat, currentUserId)
                .onSuccess { _snackbarEvents.tryEmit(successToast) }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to clear chat") }
        }
    }

    fun deleteChatForMe(successToast: String, onDeleted: () -> Unit) {
        val currentChat = chat.value ?: return
        viewModelScope.launch {
            repository.deleteChatForMe(currentChat, currentUserId)
                .onSuccess {
                    if (successToast.isNotBlank()) _snackbarEvents.tryEmit(successToast)
                    onDeleted()
                }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to delete chat") }
        }
    }

    fun translateMessage(message: ChatMessage, preferredLanguage: AppLanguage) {
        if (message.text.isBlank()) return
        if (message.id in _translatingMessageIds.value) return
        viewModelScope.launch {
            _translatingMessageIds.value = _translatingMessageIds.value + message.id
            aiRepository.translateMessage(message.text, preferredLanguage)
                .onSuccess { translated ->
                    _translatedMessages.value = _translatedMessages.value + (message.id to translated)
                }
                .onFailure { err ->
                    _snackbarEvents.tryEmit(err.localizedMessage ?: "AI translation failed")
                }
            _translatingMessageIds.value = _translatingMessageIds.value - message.id
        }
    }

    fun generateQuickReplies(messages: List<ChatMessage>, language: AppLanguage) {
        val currentChat = chat.value ?: return
        val recentText = messages
            .filter { !it.deleted && it.type != "call" && it.type != "system" && it.text.isNotBlank() }
            .takeLast(8)
            .map { msg ->
                val name = if (msg.senderId == currentUserId) "Me"
                else msg.senderName.ifBlank { currentChat.participantNames[msg.senderId] ?: "Friend" }
                name to msg.text
            }
        if (recentText.isEmpty()) return

        viewModelScope.launch {
            _quickRepliesLoading.value = true
            aiRepository.suggestQuickReplies(recentText, language)
                .onSuccess { suggestions -> _quickReplies.value = suggestions }
                .onFailure { err ->
                    _snackbarEvents.tryEmit(err.localizedMessage ?: "Smart reply failed")
                }
            _quickRepliesLoading.value = false
        }
    }

    fun clearQuickReplies() {
        _quickReplies.value = emptyList()
    }

    fun summarizeConversation(messages: List<ChatMessage>, language: AppLanguage) {
        val currentChat = chat.value ?: return
        val recentText = messages
            .filter { !it.deleted && it.type != "call" && it.type != "system" && it.text.isNotBlank() }
            .takeLast(30)
            .map { msg ->
                val name = if (msg.senderId == currentUserId) "Me"
                else msg.senderName.ifBlank { currentChat.participantNames[msg.senderId] ?: "Friend" }
                name to msg.text
            }
        if (recentText.isEmpty()) {
            _summaryState.value = AiTaskState.Error("No text messages to summarize yet.")
            return
        }

        viewModelScope.launch {
            _summaryState.value = AiTaskState.Loading
            try {
                var latest = ""
                aiRepository.summarizeConversationStream(recentText, language).collect { chunk ->
                    latest = chunk
                    _summaryState.value = AiTaskState.Streaming(chunk)
                }
                _summaryState.value = if (latest.isNotBlank()) {
                    AiTaskState.Success(latest)
                } else {
                    AiTaskState.Error("Summary returned empty.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI summary failed", e)
                _summaryState.value = AiTaskState.Error(e.localizedMessage ?: "AI summary failed")
            }
        }
    }

    fun resetSummaryState() {
        _summaryState.value = AiTaskState.Idle
    }

    fun rewriteDraft(draftText: String, tone: AiRewriteTone, language: AppLanguage) {
        if (draftText.isBlank()) return
        viewModelScope.launch {
            _writerState.value = AiTaskState.Loading
            aiRepository.rewriteMessage(draftText, tone, language)
                .onSuccess { result -> _writerState.value = AiTaskState.Success(result) }
                .onFailure { err ->
                    _writerState.value = AiTaskState.Error(err.localizedMessage ?: "AI rewrite failed")
                }
        }
    }

    fun draftMessageFromIdea(idea: String, language: AppLanguage) {
        if (idea.isBlank()) return
        viewModelScope.launch {
            _writerState.value = AiTaskState.Loading
            aiRepository.draftMessageFromIdea(idea, language)
                .onSuccess { result -> _writerState.value = AiTaskState.Success(result) }
                .onFailure { err ->
                    _writerState.value = AiTaskState.Error(err.localizedMessage ?: "AI draft failed")
                }
        }
    }

    fun resetWriterState() {
        _writerState.value = AiTaskState.Idle
    }

    fun explainMessage(messageText: String, language: AppLanguage) {
        val clean = messageText.trim()
        if (clean.isEmpty()) return
        _explainSourceText.value = clean
        viewModelScope.launch {
            _explainState.value = AiTaskState.Loading
            aiRepository.explainMessage(clean, language)
                .onSuccess { explanation -> _explainState.value = AiTaskState.Success(explanation) }
                .onFailure { err ->
                    _explainState.value = AiTaskState.Error(err.localizedMessage ?: "AI explanation failed")
                }
        }
    }

    fun resetExplainState() {
        _explainState.value = AiTaskState.Idle
        _explainSourceText.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        typingStopJob?.cancel()
    }

    private companion object {
        const val TAG = "ChatViewModel"
    }
}
