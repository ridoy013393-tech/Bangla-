package com.example.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiAssistantRepository
import com.example.data.BanglaChatRepository
import com.example.data.StorageRepository
import com.example.data.extractFirestoreRootError
import com.example.model.AiTaskState
import com.example.model.AppLanguage
import com.example.model.Chat
import com.example.model.ChatMessage
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

    val messagesState: StateFlow<UiState<List<ChatMessage>>> = _pageLimit
        .flatMapLatest { limit ->
            repository.observeMessages(chatId, currentUserId, limit)
                .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
                .catch { error ->
                    val root = extractFirestoreRootError(error)
                    if (root == null && error is CancellationException) throw error
                    val actual = root ?: error
                    Log.w(TAG, "Messages stream error", actual)
                    emit(UiState.Error(actual.localizedMessage ?: "Failed to load messages"))
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

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

    fun saveEditedMessage(newText: String) {
        val target = _editingMessage.value ?: return
        _editingMessage.value = null
        viewModelScope.launch {
            repository.editOwnMessage(chatId, target.id, newText)
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Edit failed") }
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

    fun sendTextMessage(senderProfile: PublicProfile, text: String) {
        val currentChat = chat.value ?: return
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
                text = text,
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
        caption: String = ""
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
                    replySenderName = replySender
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

    fun renameGroup(newName: String) {
        viewModelScope.launch {
            repository.renameGroup(chatId, newName)
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Rename failed") }
        }
    }

    fun addGroupMembers(members: List<PublicProfile>) {
        val currentChat = chat.value ?: return
        viewModelScope.launch {
            repository.addGroupMembers(currentChat, members)
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to add members") }
        }
    }

    fun leaveGroup(onLeft: () -> Unit) {
        val currentChat = chat.value ?: return
        viewModelScope.launch {
            repository.leaveGroup(currentChat, currentUserId)
                .onSuccess { onLeft() }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to leave group") }
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
            .filter { !it.deleted && it.text.isNotBlank() }
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
            .filter { !it.deleted && it.text.isNotBlank() }
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

    override fun onCleared() {
        super.onCleared()
        typingStopJob?.cancel()
    }

    private companion object {
        const val TAG = "ChatViewModel"
    }
}
