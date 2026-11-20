package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AppLanguage { BANGLA, ENGLISH }

enum class ThemePreference { SYSTEM, LIGHT, DARK }

enum class ChatWallpaper { DEFAULT, MINT, OCEAN, VIOLET, PEACH, MIDNIGHT }

enum class ChatFontSize(val scale: Float) {
    SMALL(0.9f),
    MEDIUM(1.0f),
    LARGE(1.16f)
}

enum class HomeTab { CHATS, CONTACTS, CALLS, SETTINGS }

enum class CallFilter { ALL, MISSED, INCOMING, OUTGOING }

data class UserPreferences(
    val language: AppLanguage = AppLanguage.BANGLA,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val onlineBannerDismissed: Boolean = false,
    val chatWallpaper: ChatWallpaper = ChatWallpaper.DEFAULT,
    val chatFontSize: ChatFontSize = ChatFontSize.MEDIUM,
    val screenSecurityEnabled: Boolean = false,
    val appLockEnabled: Boolean = false,
    val appLockPinHash: String = ""
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface UploadState {
    data object Idle : UploadState
    data class Uploading(val progress: Float, val fileName: String) : UploadState
    data class Failed(val message: String) : UploadState
}

sealed interface AiTaskState {
    data object Idle : AiTaskState
    data object Loading : AiTaskState
    data class Streaming(val text: String) : AiTaskState
    data class Success(val text: String) : AiTaskState
    data class Error(val message: String) : AiTaskState
}

@Keep
@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val name: String = "",
    val username: String = "",
    val phoneLookup: String = "",
    val photoUrl: String = "",
    val about: String = "",
    val online: Boolean = false,
    val discoverable: Boolean = true,
    val showOnlineStatus: Boolean = true,
    val showLastSeen: Boolean = true,
    val showReadReceipts: Boolean = true,
    val showProfilePhoto: Boolean = true,
    val showAbout: Boolean = true,
    @ServerTimestamp val lastSeen: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun visiblePhotoUrl(): String = if (showProfilePhoto) photoUrl else ""
    fun visibleAbout(): String = if (showAbout) about else ""
}

@Keep
@IgnoreExtraProperties
data class PrivateUser(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val phone: String = "",
    val fcmToken: String = "",
    val messagePreviewEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BlockedUser(
    @DocumentId val id: String = "",
    val blockedUid: String = "",
    val blockedName: String = "",
    val blockedUsername: String = "",
    val blockedPhotoUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FavoriteContact(
    @DocumentId val id: String = "",
    val favoriteUid: String = "",
    val name: String = "",
    val username: String = "",
    val photoUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CallLog(
    @DocumentId val id: String = "",
    val peerUid: String = "",
    val peerName: String = "",
    val peerPhotoUrl: String = "",
    val direction: String = "missed",
    val callType: String = "audio",
    val durationSeconds: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Chat(
    @DocumentId val id: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantPhotos: Map<String, String> = emptyMap(),
    val isGroup: Boolean = false,
    val groupName: String = "",
    val groupPhotoUrl: String = "",
    val adminUids: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageType: String = "text",
    val lastSenderId: String = "",
    @ServerTimestamp val lastMessageTime: Timestamp? = null,
    val unreadCounts: Map<String, Long> = emptyMap(),
    val typingUids: List<String> = emptyList(),
    val pinnedMessageId: String = "",
    val pinnedMessageText: String = "",
    val pinnedMessageSender: String = "",
    val disappearingHours: Long = 0L,
    val mutedBy: List<String> = emptyList(),
    val pinnedBy: List<String> = emptyList(),
    val archivedBy: List<String> = emptyList(),
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun peerUid(currentUid: String): String =
        if (isGroup) "" else (participants.firstOrNull { it != currentUid } ?: "")

    fun peerName(currentUid: String): String {
        if (isGroup) return groupName.ifBlank { "Group Chat" }
        val other = peerUid(currentUid)
        return participantNames[other].orEmpty().ifBlank { "Bangla Chat User" }
    }

    fun peerPhotoUrl(currentUid: String): String {
        if (isGroup) return groupPhotoUrl
        val other = peerUid(currentUid)
        return participantPhotos[other].orEmpty()
    }

    fun isAdmin(uid: String): Boolean = isGroup && (uid in adminUids || createdBy == uid)

    fun unreadFor(currentUid: String): Int =
        (unreadCounts[currentUid] ?: 0L).toInt().coerceAtLeast(0)

    fun isPinnedFor(uid: String): Boolean = uid in pinnedBy
    fun isMutedFor(uid: String): Boolean = uid in mutedBy
    fun isArchivedFor(uid: String): Boolean = uid in archivedBy
}

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val participants: List<String> = emptyList(),
    val text: String = "",
    val type: String = "text",
    val mediaUrl: String = "",
    val mediaName: String = "",
    val mediaSizeBytes: Long = 0L,
    val audioDurationSec: Long = 0L,
    val delivered: Boolean = false,
    val read: Boolean = false,
    val deleted: Boolean = false,
    val edited: Boolean = false,
    val forwarded: Boolean = false,
    val replyToId: String = "",
    val replyToText: String = "",
    val replyToSenderName: String = "",
    val reactions: Map<String, String> = emptyMap(),
    val starredBy: List<String> = emptyList(),
    val expiresAt: Timestamp? = null,
    @ServerTimestamp val timestamp: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isStarredFor(uid: String): Boolean = uid in starredBy

    fun isExpiredNow(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val exp = expiresAt ?: return false
        return exp.toDate().time <= nowMillis
    }
}

@Keep
@IgnoreExtraProperties
data class StatusStory(
    @DocumentId val id: String = "",
    val uid: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val type: String = "text",
    val text: String = "",
    val mediaUrl: String = "",
    val backgroundColorHex: Long = 0xFF2563EBL,
    val viewerUids: List<String> = emptyList(),
    val expiresAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun isExpiredNow(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val exp = expiresAt ?: return false
        return exp.toDate().time <= nowMillis
    }
}

@Keep
@IgnoreExtraProperties
data class CallSession(
    @DocumentId val id: String = "",
    val callerUid: String = "",
    val callerName: String = "",
    val callerPhotoUrl: String = "",
    val calleeUid: String = "",
    val calleeName: String = "",
    val calleePhotoUrl: String = "",
    val participants: List<String> = emptyList(),
    val callType: String = "audio",
    val status: String = "ringing",
    val offerSdp: String = "",
    val answerSdp: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isIncomingFor(uid: String): Boolean = calleeUid == uid
    fun remoteUid(currentUid: String): String = if (callerUid == currentUid) calleeUid else callerUid
    fun remoteName(currentUid: String): String = if (callerUid == currentUid) calleeName else callerName
    fun remotePhoto(currentUid: String): String = if (callerUid == currentUid) calleePhotoUrl else callerPhotoUrl
}

@Keep
@IgnoreExtraProperties
data class IceCandidateRecord(
    @DocumentId val id: String = "",
    val senderUid: String = "",
    val participants: List<String> = emptyList(),
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0,
    val sdp: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
