package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AppBroadcast
import com.example.model.BlockedUser
import com.example.model.CallLog
import com.example.model.CallSession
import com.example.model.Chat
import com.example.model.ChatMessage
import com.example.model.FavoriteContact
import com.example.model.IceCandidateRecord
import com.example.model.ModerationSettings
import com.example.model.ModerationStatus
import com.example.model.PrivateUser
import com.example.model.PublicProfile
import com.example.model.StatusStory
import com.example.model.UserReport
import com.google.firebase.Firebase
import java.util.UUID
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

class BanglaChatRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    class UsernameAlreadyTakenException(val username: String) :
        IllegalStateException("Username @$username is already taken")

    companion object {
        const val SUPER_ADMIN_EMAIL = "mehedi0169019@gmail.com"
        private val USERNAME_REGEX = Regex("^[a-z0-9_]{3,30}$")

        fun normalizeUsername(raw: String): String =
            raw.trim().trimStart('@').trim().lowercase()

        fun isValidUsernameFormat(raw: String): Boolean =
            USERNAME_REGEX.matches(normalizeUsername(raw))
    }

    fun isCurrentUserSuperAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.email.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)
    }

    private fun sanitizeUsername(raw: String, uid: String): String {
        val cleaned = raw.lowercase()
            .replace(Regex("[^a-z0-9_]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(24)
        return when {
            cleaned.length >= 3 -> cleaned
            else -> "user_${uid.lowercase().replace(Regex("[^a-z0-9]"), "").take(8).padEnd(4, '0')}"
        }
    }

    suspend fun hasRegisteredProfile(uid: String): Result<Boolean> {
        val path = "profiles/$uid"
        return try {
            val signedInUid = requireUserId()
            check(signedInUid == uid) { "Signed-in user mismatch" }
            val snap = db.collection("profiles").document(uid).get().await()
            Result.success(snap.exists())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun isUsernameAvailable(rawUsername: String, excludeUid: String? = null): Result<Boolean> {
        val clean = normalizeUsername(rawUsername)
        if (!USERNAME_REGEX.matches(clean)) return Result.success(false)
        val path = "usernames/$clean"
        return try {
            requireUserId()
            val claimSnap = db.collection("usernames").document(clean).get().await()
            if (claimSnap.exists()) {
                val ownerUid = claimSnap.getString("uid")
                return Result.success(excludeUid != null && ownerUid == excludeUid)
            }
            // Fallback check for legacy accounts created before /usernames registry existed
            val legacySnap = db.collection("profiles")
                .whereEqualTo("discoverable", true)
                .whereEqualTo("username", clean)
                .limit(2)
                .get()
                .await()
            val takenByLegacy = legacySnap.documents.any { it.id != excludeUid }
            Result.success(!takenByLegacy)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun registerNewUserAccount(
        firebaseUser: FirebaseUser,
        displayName: String,
        phone: String,
        rawUsername: String
    ): Result<Unit> {
        val uid = firebaseUser.uid
        val cleanName = displayName.trim().take(60)
        val cleanPhone = phone.trim().take(20)
        val cleanUsername = normalizeUsername(rawUsername)
        val path = "profiles/$uid | usernames/$cleanUsername"
        return try {
            val signedInUid = requireUserId()
            check(signedInUid == uid) { "Signed-in user mismatch" }
            require(cleanName.isNotEmpty()) { "Display name cannot be empty" }
            require(cleanPhone.length >= 6) { "Phone number is required" }
            require(USERNAME_REGEX.matches(cleanUsername)) {
                "Username must be 3-30 lowercase letters, numbers, or underscores"
            }

            // Guard against colliding with a legacy profile that has not backfilled /usernames yet
            val legacyMatches = db.collection("profiles")
                .whereEqualTo("discoverable", true)
                .whereEqualTo("username", cleanUsername)
                .limit(2)
                .get()
                .await()
            if (legacyMatches.documents.any { it.id != uid }) {
                throw UsernameAlreadyTakenException(cleanUsername)
            }

            val profileRef = db.collection("profiles").document(uid)
            val userRef = db.collection("users").document(uid)
            val usernameRef = db.collection("usernames").document(cleanUsername)
            val photoUrl = firebaseUser.photoUrl?.toString().orEmpty().take(500)

            db.runTransaction { tx ->
                val profileSnap = tx.get(profileRef)
                val userSnap = tx.get(userRef)
                val usernameSnap = tx.get(usernameRef)

                // Never overwrite an already-registered existing account
                if (profileSnap.exists()) {
                    return@runTransaction null
                }

                if (usernameSnap.exists() && usernameSnap.getString("uid") != uid) {
                    throw UsernameAlreadyTakenException(cleanUsername)
                }

                if (!usernameSnap.exists()) {
                    tx.set(
                        usernameRef,
                        mapOf(
                            "uid" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                tx.set(
                    profileRef,
                    mapOf(
                        "uid" to uid,
                        "name" to cleanName,
                        "username" to cleanUsername,
                        "phoneLookup" to cleanPhone,
                        "photoUrl" to photoUrl,
                        "about" to "সবার সাথে থাকুন, সবসময়",
                        "online" to true,
                        "discoverable" to true,
                        "showOnlineStatus" to true,
                        "showLastSeen" to true,
                        "showReadReceipts" to true,
                        "showProfilePhoto" to true,
                        "showAbout" to true,
                        "lastSeen" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                if (!userSnap.exists()) {
                    tx.set(
                        userRef,
                        mapOf(
                            "uid" to uid,
                            "email" to firebaseUser.email.orEmpty().take(120),
                            "phone" to cleanPhone,
                            "messagePreviewEnabled" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                null
            }.await()

            // Notify Admin Telegram Bot of new registration (non-fatal)
            TelegramNotifier.notifyNewUserRegistration(
                uid = uid,
                name = cleanName,
                username = cleanUsername,
                phone = cleanPhone,
                email = firebaseUser.email.orEmpty()
            )

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: UsernameAlreadyTakenException) {
            Result.failure(e)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun syncExistingSession(firebaseUser: FirebaseUser): Result<Boolean> {
        val uid = firebaseUser.uid
        val profilePath = "profiles/$uid"
        val userPath = "users/$uid"
        return try {
            val signedInUid = requireUserId()
            check(signedInUid == uid) { "Signed-in user mismatch" }

            val profileRef = db.collection("profiles").document(uid)
            val profileSnap = profileRef.get().await()
            if (!profileSnap.exists()) {
                // Do NOT auto-create a profile; caller must route through Registration
                return Result.success(false)
            }

            profileRef.update(
                mapOf(
                    "online" to true,
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Backfill unique username reservation for pre-existing legacy accounts if unclaimed
            val existingUsername = normalizeUsername(profileSnap.getString("username").orEmpty())
            if (USERNAME_REGEX.matches(existingUsername)) {
                val claimRef = db.collection("usernames").document(existingUsername)
                runCatching {
                    if (!claimRef.get().await().exists()) {
                        claimRef.set(
                            mapOf(
                                "uid" to uid,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }

            val userRef = db.collection("users").document(uid)
            val userSnap = userRef.get().await()
            if (!userSnap.exists()) {
                val privatePayload = mapOf(
                    "uid" to uid,
                    "email" to firebaseUser.email.orEmpty().take(120),
                    "phone" to (profileSnap.getString("phoneLookup").orEmpty()
                        .ifBlank { firebaseUser.phoneNumber.orEmpty() }).take(25),
                    "messagePreviewEnabled" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(privatePayload).await()
            }
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "$profilePath | $userPath")
            Result.failure(e)
        }
    }

    suspend fun findProfileByExactUsername(rawQuery: String, currentUserId: String): Result<PublicProfile?> {
        val clean = normalizeUsername(rawQuery)
        if (!USERNAME_REGEX.matches(clean)) return Result.success(null)
        val path = "usernames/$clean"
        return try {
            requireUserId()
            // 1. Primary exact lookup in /usernames/{username}
            val claimSnap = db.collection("usernames").document(clean).get().await()
            if (claimSnap.exists()) {
                val targetUid = claimSnap.getString("uid").orEmpty()
                if (targetUid.isBlank() || targetUid == currentUserId) {
                    return Result.success(null)
                }
                val profileSnap = db.collection("profiles").document(targetUid).get().await()
                val profile = profileSnap.toObject(
                    PublicProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                return Result.success(profile?.takeIf { it.discoverable })
            }

            // 2. Fallback exact match for pre-existing legacy profiles not yet backfilled
            val legacySnap = db.collection("profiles")
                .whereEqualTo("discoverable", true)
                .whereEqualTo("username", clean)
                .limit(2)
                .get()
                .await()
            val match = legacySnap.toObjects(
                PublicProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).firstOrNull { it.uid != currentUserId && it.username.equals(clean, ignoreCase = true) }

            Result.success(match)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    fun observePublicProfile(uid: String): Flow<PublicProfile?> {
        val path = "profiles/$uid"
        return db.collection("profiles").document(uid)
            .snapshots()
            .map { snap ->
                snap.toObject(
                    PublicProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val root = extractFirestoreRootError(error)
                if (root == null && error is CancellationException) throw error
                if (root != null && auth.currentUser != null) {
                    handleFirestoreError(root, OperationType.GET, path)
                }
                throw error
            }
    }

    fun observePrivateUser(uid: String): Flow<PrivateUser?> {
        val path = "users/$uid"
        return db.collection("users").document(uid)
            .snapshots()
            .map { snap ->
                snap.toObject(
                    PrivateUser::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val root = extractFirestoreRootError(error)
                if (root == null && error is CancellationException) throw error
                if (root != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(root, OperationType.GET, path)
                }
                throw error
            }
    }

    fun observeDiscoverableProfiles(currentUserId: String): Flow<List<PublicProfile>> {
        val path = "profiles"
        return db.collection("profiles")
            .whereEqualTo("discoverable", true)
            .limit(60)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    PublicProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).filter { it.uid != currentUserId }
                    .sortedBy { it.name.lowercase() }
            }
            .catch { error ->
                val root = extractFirestoreRootError(error)
                if (root == null && error is CancellationException) throw error
                if (root != null && auth.currentUser?.uid == currentUserId) {
                    handleFirestoreError(root, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeChats(currentUserId: String): Flow<List<Chat>> {
        val path = "chats"
        return db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    Chat::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.lastMessageTime ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val root = extractFirestoreRootError(error)
                if (root == null && error is CancellationException) throw error
                if (root != null && auth.currentUser?.uid == currentUserId) {
                    handleFirestoreError(root, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeChat(chatId: String): Flow<Chat?> {
        val path = "chats/$chatId"
        return db.collection("chats").document(chatId)
            .snapshots()
            .map { snap ->
                snap.toObject(
                    Chat::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val root = extractFirestoreRootError(error)
                if (root == null && error is CancellationException) throw error
                if (root != null && auth.currentUser != null) {
                    handleFirestoreError(root, OperationType.GET, path)
                }
                throw error
            }
    }

    fun observeMessages(chatId: String, currentUserId: String, limit: Long): Flow<List<ChatMessage>> {
        val path = "chats/$chatId/messages"
        return db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.coerceIn(20L, 500L))
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    ChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).filter { !it.isDeletedFor(currentUserId) }
                    .sortedBy { it.timestamp ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val root = extractFirestoreRootError(error)
                if (root == null && error is CancellationException) throw error
                if (root != null && auth.currentUser?.uid == currentUserId) {
                    handleFirestoreError(root, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeFavorites(currentUserId: String): Flow<List<FavoriteContact>> {
        val path = "users/$currentUserId/favorites"
        return db.collection("users").document(currentUserId)
            .collection("favorites")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    FavoriteContact::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.name.lowercase() }
            }
            .catch { error ->
                val root = extractFirestoreRootError(error)
                if (root == null && error is CancellationException) throw error
                if (root != null && auth.currentUser?.uid == currentUserId) {
                    handleFirestoreError(root, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeBlockedUsers(currentUserId: String): Flow<List<BlockedUser>> {
        val path = "users/$currentUserId/blockedUsers"
        return db.collection("users").document(currentUserId)
            .collection("blockedUsers")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    BlockedUser::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val root = extractFirestoreRootError(error)
                if (root == null && error is CancellationException) throw error
                if (root != null && auth.currentUser?.uid == currentUserId) {
                    handleFirestoreError(root, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeCallLogs(currentUserId: String): Flow<List<CallLog>> {
        val path = "users/$currentUserId/callLogs"
        return db.collection("users").document(currentUserId)
            .collection("callLogs")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    CallLog::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val root = extractFirestoreRootError(error)
                if (root == null && error is CancellationException) throw error
                if (root != null && auth.currentUser?.uid == currentUserId) {
                    handleFirestoreError(root, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun setOnlinePresence(uid: String, online: Boolean): Result<Unit> {
        val path = "profiles/$uid"
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.success(Unit)
            if (currentUid != uid) return Result.success(Unit)
            val docRef = db.collection("profiles").document(uid)
            if (!docRef.get().await().exists()) {
                return Result.success(Unit)
            }
            docRef.update(
                mapOf(
                    "online" to online,
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun syncFcmToken(uid: String, token: String): Result<Unit> {
        val path = "users/$uid"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "Signed-in user mismatch" }
            val docRef = db.collection("users").document(uid)
            if (!docRef.get().await().exists()) {
                return Result.success(Unit)
            }
            docRef.update(
                mapOf(
                    "fcmToken" to token.take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        uid: String,
        name: String,
        username: String,
        about: String,
        phone: String,
        photoUrl: String? = null
    ): Result<Unit> {
        val profilePath = "profiles/$uid"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "Signed-in user mismatch" }
            val trimmedName = name.trim().take(60)
            require(trimmedName.isNotEmpty()) { "Display name cannot be empty" }
            val validUsername = normalizeUsername(username)
            require(USERNAME_REGEX.matches(validUsername)) {
                "Username must be 3-30 lowercase letters, numbers, or underscores"
            }
            val cleanPhone = phone.trim().take(20)
            val cleanAbout = about.trim().take(180)

            // Guard against legacy profile collision before transaction
            val legacySnap = db.collection("profiles")
                .whereEqualTo("discoverable", true)
                .whereEqualTo("username", validUsername)
                .limit(2)
                .get()
                .await()
            if (legacySnap.documents.any { it.id != uid }) {
                throw UsernameAlreadyTakenException(validUsername)
            }

            val profileRef = db.collection("profiles").document(uid)
            val userRef = db.collection("users").document(uid)
            val newUsernameRef = db.collection("usernames").document(validUsername)

            // Read old username outside transaction to prepare claim swap
            val currentProfileSnap = profileRef.get().await()
            val oldUsername = normalizeUsername(currentProfileSnap.getString("username").orEmpty())
            val oldUsernameRef = if (oldUsername != validUsername && USERNAME_REGEX.matches(oldUsername)) {
                db.collection("usernames").document(oldUsername)
            } else null

            db.runTransaction { tx ->
                val newClaimSnap = tx.get(newUsernameRef)
                val oldClaimSnap = oldUsernameRef?.let { tx.get(it) }
                val userSnap = tx.get(userRef)

                if (newClaimSnap.exists() && newClaimSnap.getString("uid") != uid) {
                    throw UsernameAlreadyTakenException(validUsername)
                }

                if (!newClaimSnap.exists()) {
                    tx.set(
                        newUsernameRef,
                        mapOf(
                            "uid" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                if (oldUsernameRef != null && oldClaimSnap?.exists() == true && oldClaimSnap.getString("uid") == uid) {
                    tx.delete(oldUsernameRef)
                }

                val profileUpdates = mutableMapOf<String, Any>(
                    "name" to trimmedName,
                    "username" to validUsername,
                    "about" to cleanAbout,
                    "phoneLookup" to cleanPhone,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (photoUrl != null) {
                    profileUpdates["photoUrl"] = photoUrl.take(500)
                }
                tx.update(profileRef, profileUpdates)

                if (userSnap.exists()) {
                    tx.update(
                        userRef,
                        mapOf(
                            "phone" to cleanPhone,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                null
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: UsernameAlreadyTakenException) {
            Result.failure(e)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, profilePath)
            Result.failure(e)
        }
    }

    suspend fun updatePrivacySettings(
        uid: String,
        discoverable: Boolean,
        showOnlineStatus: Boolean,
        showLastSeen: Boolean,
        showReadReceipts: Boolean,
        showProfilePhoto: Boolean,
        showAbout: Boolean
    ): Result<Unit> {
        val path = "profiles/$uid"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "Signed-in user mismatch" }
            db.collection("profiles").document(uid).update(
                mapOf(
                    "discoverable" to discoverable,
                    "showOnlineStatus" to showOnlineStatus,
                    "showLastSeen" to showLastSeen,
                    "showReadReceipts" to showReadReceipts,
                    "showProfilePhoto" to showProfilePhoto,
                    "showAbout" to showAbout,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMessagePreviewSetting(uid: String, enabled: Boolean): Result<Unit> {
        val path = "users/$uid"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "Signed-in user mismatch" }
            db.collection("users").document(uid).update(
                mapOf(
                    "messagePreviewEnabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun buildChatId(uidA: String, uidB: String): String {
        val sorted = listOf(uidA, uidB).sorted()
        return "${sorted[0]}_${sorted[1]}"
    }

    suspend fun getOrCreateChat(
        myProfile: PublicProfile,
        peerProfile: PublicProfile
    ): Result<String> {
        val myUid = myProfile.uid
        val peerUid = peerProfile.uid
        val chatId = buildChatId(myUid, peerUid)
        val path = "chats/$chatId"
        return try {
            val currentUid = requireUserId()
            check(currentUid == myUid) { "Signed-in user mismatch" }
            require(myUid != peerUid) { "Cannot start a chat with yourself" }

            val chatRef = db.collection("chats").document(chatId)
            val snap = chatRef.get().await()
            if (!snap.exists()) {
                val participants = listOf(myUid, peerUid).sorted()
                val payload = mapOf(
                    "participants" to participants,
                    "participantNames" to mapOf(
                        myUid to myProfile.name.ifBlank { "Bangla Chat User" }.take(60),
                        peerUid to peerProfile.name.ifBlank { "Bangla Chat User" }.take(60)
                    ),
                    "participantPhotos" to mapOf(
                        myUid to myProfile.photoUrl.take(500),
                        peerUid to peerProfile.photoUrl.take(500)
                    ),
                    "lastMessage" to "",
                    "lastMessageType" to "text",
                    "lastSenderId" to "",
                    "lastMessageTime" to FieldValue.serverTimestamp(),
                    "unreadCounts" to mapOf(myUid to 0L, peerUid to 0L),
                    "typingUids" to emptyList<String>(),
                    "hiddenBy" to emptyList<String>(),
                    "createdBy" to myUid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                chatRef.set(payload).await()
            } else {
                val existingChat = snap.toObject(
                    Chat::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existingChat != null && existingChat.isHiddenFor(myUid)) {
                    runCatching {
                        chatRef.update(
                            mapOf(
                                "hiddenBy" to existingChat.hiddenBy.filter { it != myUid },
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }
            Result.success(chatId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    private suspend fun postGroupSystemEvent(
        chatId: String,
        participants: List<String>,
        actorUid: String,
        actorName: String,
        eventText: String,
        updateLastMessage: Boolean = true
    ) {
        val cleanText = eventText.trim().take(500)
        if (cleanText.isEmpty() || participants.isEmpty()) return
        val safeActorName = actorName.ifBlank { "Bangla Chat User" }.take(60)
        val chatRef = db.collection("chats").document(chatId)
        val msgRef = chatRef.collection("messages").document()
        val messagePayload = mapOf(
            "chatId" to chatId,
            "senderId" to actorUid,
            "senderName" to safeActorName,
            "participants" to participants,
            "text" to cleanText,
            "type" to "system",
            "delivered" to true,
            "read" to true,
            "deleted" to false,
            "deletedFor" to emptyList<String>(),
            "edited" to false,
            "forwarded" to false,
            "reactions" to emptyMap<String, String>(),
            "starredBy" to emptyList<String>(),
            "timestamp" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        msgRef.set(messagePayload).await()
        if (updateLastMessage) {
            chatRef.update(
                mapOf(
                    "lastMessage" to cleanText,
                    "lastMessageType" to "system",
                    "lastSenderId" to actorUid,
                    "lastMessageTime" to FieldValue.serverTimestamp(),
                    "lastMessageDelivered" to true,
                    "lastMessageRead" to false,
                    "hiddenBy" to emptyList<String>(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun createGroupChat(
        creator: PublicProfile,
        members: List<PublicProfile>,
        groupName: String,
        groupDescription: String = "",
        groupPhotoUrl: String = ""
    ): Result<String> {
        val cleanName = groupName.trim().take(80)
        val cleanDesc = groupDescription.trim().take(300)
        val chatId = "group_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "chats/$chatId"
        return try {
            val uid = requireUserId()
            check(uid == creator.uid) { "Signed-in user mismatch" }
            require(cleanName.isNotEmpty()) { "Group name cannot be empty" }
            val distinctMembers = (listOf(creator) + members).distinctBy { it.uid }.take(Chat.MAX_GROUP_MEMBERS)
            require(distinctMembers.size >= 2) { "Select at least one member for the group" }

            val participantIds = distinctMembers.map { it.uid }
            val namesMap = distinctMembers.associate {
                it.uid to it.name.ifBlank { "Bangla Chat User" }.take(60)
            }
            val photosMap = distinctMembers.associate {
                it.uid to it.photoUrl.take(500)
            }
            val unreadMap = participantIds.associateWith { 0L }
            val creatorName = creator.name.ifBlank { "Bangla Chat User" }.take(60)
            val createdEvent = "$creatorName গ্রুপটি তৈরি করেছেন"

            val payload = mapOf(
                "participants" to participantIds,
                "participantNames" to namesMap,
                "participantPhotos" to photosMap,
                "isGroup" to true,
                "groupName" to cleanName,
                "groupDescription" to cleanDesc,
                "groupPhotoUrl" to groupPhotoUrl.take(500),
                "adminUids" to listOf(uid),
                "onlyAdminsCanSend" to false,
                "lastMessage" to createdEvent,
                "lastMessageType" to "system",
                "lastSenderId" to uid,
                "lastMessageTime" to FieldValue.serverTimestamp(),
                "unreadCounts" to unreadMap,
                "typingUids" to emptyList<String>(),
                "hiddenBy" to emptyList<String>(),
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("chats").document(chatId).set(payload).await()
            runCatching {
                postGroupSystemEvent(
                    chatId = chatId,
                    participants = participantIds,
                    actorUid = uid,
                    actorName = creatorName,
                    eventText = createdEvent,
                    updateLastMessage = false
                )
            }
            Result.success(chatId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun renameGroup(
        chat: Chat,
        actorProfile: PublicProfile?,
        newName: String
    ): Result<Unit> {
        val path = "chats/${chat.id}"
        return try {
            val uid = requireUserId()
            require(chat.isGroup && chat.isAdmin(uid)) { "Only group admins can rename this group" }
            val clean = newName.trim().take(80)
            require(clean.isNotEmpty()) { "Group name cannot be empty" }
            db.collection("chats").document(chat.id).update(
                mapOf(
                    "groupName" to clean,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val actorName = actorProfile?.name?.ifBlank { chat.participantNames[uid] }
                ?: chat.participantNames[uid]
                ?: "Admin"
            runCatching {
                postGroupSystemEvent(
                    chatId = chat.id,
                    participants = chat.participants,
                    actorUid = uid,
                    actorName = actorName,
                    eventText = "$actorName গ্রুপের নাম পরিবর্তন করে \"$clean\" করেছেন"
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateGroupDescription(
        chat: Chat,
        actorProfile: PublicProfile?,
        description: String
    ): Result<Unit> {
        val path = "chats/${chat.id}"
        return try {
            val uid = requireUserId()
            require(chat.isGroup && chat.isAdmin(uid)) { "Only group admins can update group description" }
            val clean = description.trim().take(300)
            db.collection("chats").document(chat.id).update(
                mapOf(
                    "groupDescription" to clean,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val actorName = actorProfile?.name?.ifBlank { chat.participantNames[uid] }
                ?: chat.participantNames[uid]
                ?: "Admin"
            runCatching {
                postGroupSystemEvent(
                    chatId = chat.id,
                    participants = chat.participants,
                    actorUid = uid,
                    actorName = actorName,
                    eventText = "$actorName গ্রুপের বিবরণ আপডেট করেছেন"
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateGroupPhoto(
        chat: Chat,
        actorProfile: PublicProfile?,
        photoUrl: String
    ): Result<Unit> {
        val path = "chats/${chat.id}"
        return try {
            val uid = requireUserId()
            require(chat.isGroup && chat.isAdmin(uid)) { "Only group admins can change group photo" }
            db.collection("chats").document(chat.id).update(
                mapOf(
                    "groupPhotoUrl" to photoUrl.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val actorName = actorProfile?.name?.ifBlank { chat.participantNames[uid] }
                ?: chat.participantNames[uid]
                ?: "Admin"
            runCatching {
                postGroupSystemEvent(
                    chatId = chat.id,
                    participants = chat.participants,
                    actorUid = uid,
                    actorName = actorName,
                    eventText = "$actorName গ্রুপের ছবি পরিবর্তন করেছেন"
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addGroupMembers(
        chat: Chat,
        actorProfile: PublicProfile?,
        newMembers: List<PublicProfile>
    ): Result<Unit> {
        val path = "chats/${chat.id}"
        return try {
            val uid = requireUserId()
            require(chat.isGroup && chat.isAdmin(uid)) { "Only group admins can add members" }
            val remainingSlots = (Chat.MAX_GROUP_MEMBERS - chat.participants.size).coerceAtLeast(0)
            require(remainingSlots > 0) { "Group has reached the maximum limit of ${Chat.MAX_GROUP_MEMBERS} members" }
            val added = newMembers
                .distinctBy { it.uid }
                .filter { it.uid !in chat.participants }
                .take(remainingSlots)
            if (added.isEmpty()) return Result.success(Unit)
            val updatedParticipants = (chat.participants + added.map { it.uid }).distinct().take(Chat.MAX_GROUP_MEMBERS)
            val updatedNames = chat.participantNames.toMutableMap().apply {
                added.forEach { put(it.uid, it.name.ifBlank { "Bangla Chat User" }.take(60)) }
            }
            val updatedPhotos = chat.participantPhotos.toMutableMap().apply {
                added.forEach { put(it.uid, it.photoUrl.take(500)) }
            }
            val updatedUnread = chat.unreadCounts.toMutableMap().apply {
                added.forEach { putIfAbsent(it.uid, 0L) }
            }
            db.collection("chats").document(chat.id).update(
                mapOf(
                    "participants" to updatedParticipants,
                    "participantNames" to updatedNames,
                    "participantPhotos" to updatedPhotos,
                    "unreadCounts" to updatedUnread,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val actorName = actorProfile?.name?.ifBlank { chat.participantNames[uid] }
                ?: chat.participantNames[uid]
                ?: "Admin"
            val addedSummary = if (added.size == 1) {
                added.first().name.ifBlank { "New member" }
            } else {
                "${added.first().name.ifBlank { "Member" }} ও আরও ${added.size - 1} জনকে"
            }
            runCatching {
                postGroupSystemEvent(
                    chatId = chat.id,
                    participants = updatedParticipants,
                    actorUid = uid,
                    actorName = actorName,
                    eventText = "$actorName $addedSummary গ্রুপে যোগ করেছেন"
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun leaveGroup(
        chat: Chat,
        currentUserId: String,
        actorProfile: PublicProfile? = null
    ): Result<Unit> {
        val path = "chats/${chat.id}"
        return try {
            val uid = requireUserId()
            check(uid == currentUserId) { "Signed-in user mismatch" }
            require(chat.isGroup) { "Not a group chat" }
            val remaining = chat.participants.filter { it != uid }
            if (remaining.isEmpty()) {
                return deleteGroupChat(chat)
            }
            val actorName = actorProfile?.name?.ifBlank { chat.participantNames[uid] }
                ?: chat.participantNames[uid]
                ?: "Member"
            // Post leave event while still a participant so remaining members see who left
            runCatching {
                postGroupSystemEvent(
                    chatId = chat.id,
                    participants = chat.participants,
                    actorUid = uid,
                    actorName = actorName,
                    eventText = "$actorName গ্রুপ ত্যাগ করেছেন"
                )
            }
            val remainingAdmins = chat.adminUids
                .filter { it != uid && it in remaining }
                .ifEmpty { listOf(remaining.first()) }
            db.collection("chats").document(chat.id).update(
                mapOf(
                    "participants" to remaining,
                    "adminUids" to remainingAdmins,
                    "participantNames" to chat.participantNames.filterKeys { it != uid },
                    "participantPhotos" to chat.participantPhotos.filterKeys { it != uid },
                    "unreadCounts" to chat.unreadCounts.filterKeys { it != uid },
                    "typingUids" to chat.typingUids.filter { it != uid },
                    "mutedBy" to chat.mutedBy.filter { it != uid },
                    "pinnedBy" to chat.pinnedBy.filter { it != uid },
                    "archivedBy" to chat.archivedBy.filter { it != uid },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteGroupChat(chat: Chat): Result<Unit> {
        val path = "chats/${chat.id}"
        return try {
            val uid = requireUserId()
            require(chat.isGroup) { "Only group chats can be permanently deleted" }
            val canDelete = chat.isAdmin(uid) || chat.participants == listOf(uid) || isCurrentUserSuperAdmin()
            require(canDelete) { "Only group creator or group admin can delete this group" }

            val chatRef = db.collection("chats").document(chat.id)
            // Delete subcollection messages in batches before deleting the chat document
            repeat(10) {
                val msgSnap = chatRef.collection("messages").limit(100).get().await()
                if (msgSnap.isEmpty) return@repeat
                val batch = db.batch()
                msgSnap.documents.forEach { doc -> batch.delete(doc.reference) }
                batch.commit().await()
            }
            chatRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun clearChatForMe(chat: Chat, currentUserId: String): Result<Unit> {
        val path = "chats/${chat.id}"
        return try {
            val uid = requireUserId()
            check(uid == currentUserId) { "Signed-in user mismatch" }
            require(uid in chat.participants) { "Not a participant of this chat" }
            val updatedUnread = chat.unreadCounts.toMutableMap().apply { put(uid, 0L) }
            db.collection("chats").document(chat.id).update(
                mapOf(
                    "clearedAt.$uid" to FieldValue.serverTimestamp(),
                    "unreadCounts" to updatedUnread,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteChatForMe(chat: Chat, currentUserId: String): Result<Unit> {
        val path = "chats/${chat.id}"
        return try {
            val uid = requireUserId()
            check(uid == currentUserId) { "Signed-in user mismatch" }
            require(uid in chat.participants) { "Not a participant of this chat" }
            val updatedHidden = (chat.hiddenBy + uid).distinct().take(Chat.MAX_GROUP_MEMBERS)
            val updatedUnread = chat.unreadCounts.toMutableMap().apply { put(uid, 0L) }
            db.collection("chats").document(chat.id).update(
                mapOf(
                    "hiddenBy" to updatedHidden,
                    "pinnedBy" to chat.pinnedBy.filter { it != uid },
                    "archivedBy" to chat.archivedBy.filter { it != uid },
                    "clearedAt.$uid" to FieldValue.serverTimestamp(),
                    "unreadCounts" to updatedUnread,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleGroupOnlyAdminsCanSend(
        chat: Chat,
        actorProfile: PublicProfile?,
        onlyAdmins: Boolean
    ): Result<Unit> {
        val path = "chats/${chat.id}"
        return try {
            val uid = requireUserId()
            require(chat.isGroup && chat.isAdmin(uid)) { "Only group admins can change messaging settings" }
            db.collection("chats").document(chat.id).update(
                mapOf(
                    "onlyAdminsCanSend" to onlyAdmins,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val actorName = actorProfile?.name?.ifBlank { chat.participantNames[uid] }
                ?: chat.participantNames[uid]
                ?: "Admin"
            val eventText = if (onlyAdmins) {
                "$actorName সেটিংস পরিবর্তন করেছেন: এখন শুধু অ্যাডমিনরা মেসেজ পাঠাতে পারবেন"
            } else {
                "$actorName সেটিংস পরিবর্তন করেছেন: এখন সব সদস্য মেসেজ পাঠাতে পারবেন"
            }
            runCatching {
                postGroupSystemEvent(
                    chatId = chat.id,
                    participants = chat.participants,
                    actorUid = uid,
                    actorName = actorName,
                    eventText = eventText
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleGroupMemberAdmin(
        chat: Chat,
        actorProfile: PublicProfile?,
        targetUid: String
    ): Result<Unit> {
        val path = "chats/${chat.id}"
        return try {
            val currentUid = requireUserId()
            require(chat.isGroup && chat.isAdmin(currentUid)) { "Only admins can change admin roles" }
            require(targetUid in chat.participants) { "User is not a group member" }
            require(targetUid != chat.createdBy) { "Cannot demote group creator" }
            val wasAdmin = targetUid in chat.adminUids
            val updatedAdmins = if (wasAdmin) {
                chat.adminUids.filter { it != targetUid }
            } else {
                (chat.adminUids + targetUid).distinct().take(Chat.MAX_GROUP_MEMBERS)
            }
            db.collection("chats").document(chat.id).update(
                mapOf(
                    "adminUids" to updatedAdmins,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val actorName = actorProfile?.name?.ifBlank { chat.participantNames[currentUid] }
                ?: chat.participantNames[currentUid]
                ?: "Admin"
            val targetName = chat.participantNames[targetUid].orEmpty().ifBlank { "Member" }
            val eventText = if (wasAdmin) {
                "$actorName $targetName-কে অ্যাডমিন পদ থেকে সরিয়ে দিয়েছেন"
            } else {
                "$actorName $targetName-কে গ্রুপ অ্যাডমিন বানিয়েছেন"
            }
            runCatching {
                postGroupSystemEvent(
                    chatId = chat.id,
                    participants = chat.participants,
                    actorUid = currentUid,
                    actorName = actorName,
                    eventText = eventText
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeGroupMember(
        chat: Chat,
        actorProfile: PublicProfile?,
        targetUid: String
    ): Result<Unit> {
        val path = "chats/${chat.id}"
        return try {
            val currentUid = requireUserId()
            require(chat.isGroup && chat.isAdmin(currentUid)) { "Only admins can remove members" }
            require(targetUid != currentUid && targetUid != chat.createdBy) { "Cannot remove this member" }
            val remaining = chat.participants.filter { it != targetUid }
            require(remaining.isNotEmpty()) { "Group must keep at least one member" }
            val targetName = chat.participantNames[targetUid].orEmpty().ifBlank { "Member" }
            val updatedAdmins = chat.adminUids.filter { it != targetUid }
            val updatedNames = chat.participantNames.filterKeys { it != targetUid }
            val updatedPhotos = chat.participantPhotos.filterKeys { it != targetUid }
            val updatedUnread = chat.unreadCounts.filterKeys { it != targetUid }
            db.collection("chats").document(chat.id).update(
                mapOf(
                    "participants" to remaining,
                    "adminUids" to updatedAdmins,
                    "participantNames" to updatedNames,
                    "participantPhotos" to updatedPhotos,
                    "unreadCounts" to updatedUnread,
                    "typingUids" to chat.typingUids.filter { it != targetUid },
                    "mutedBy" to chat.mutedBy.filter { it != targetUid },
                    "pinnedBy" to chat.pinnedBy.filter { it != targetUid },
                    "archivedBy" to chat.archivedBy.filter { it != targetUid },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val actorName = actorProfile?.name?.ifBlank { chat.participantNames[currentUid] }
                ?: chat.participantNames[currentUid]
                ?: "Admin"
            runCatching {
                postGroupSystemEvent(
                    chatId = chat.id,
                    participants = remaining,
                    actorUid = currentUid,
                    actorName = actorName,
                    eventText = "$actorName $targetName-কে গ্রুপ থেকে সরিয়ে দিয়েছেন"
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        chat: Chat,
        senderProfile: PublicProfile,
        text: String,
        type: String = "text",
        mediaUrl: String? = null,
        mediaName: String? = null,
        mediaSizeBytes: Long? = null,
        audioDurationSec: Long? = null,
        replyTo: ChatMessage? = null,
        replySenderName: String? = null,
        forwarded: Boolean = false,
        viewOnce: Boolean = false
    ): Result<Unit> {
        val chatId = chat.id
        val messagesPath = "chats/$chatId/messages"
        return try {
            val uid = requireUserId()
            // For group chats, read the freshest participants snapshot to avoid race-condition PERMISSION_DENIED
            val liveChat = if (chat.isGroup) {
                val snap = db.collection("chats").document(chatId).get().await()
                snap.toObject(Chat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("This group no longer exists")
            } else {
                chat
            }
            require(uid in liveChat.participants) { "You are no longer a participant of this chat" }
            if (liveChat.isGroup && liveChat.onlyAdminsCanSend && !liveChat.isAdmin(uid)) {
                throw IllegalStateException("Only group admins can send messages")
            }

            val cleanText = text.trim().take(4000)
            require(cleanText.isNotEmpty() || !mediaUrl.isNullOrBlank()) { "Message cannot be empty" }
            val safeType = if (type in setOf("text", "image", "document", "audio")) type else "text"
            val isViewOnceImage = viewOnce && safeType == "image"

            val expiresAtTimestamp = if (liveChat.disappearingHours in setOf(24L, 168L)) {
                Timestamp(Date(System.currentTimeMillis() + liveChat.disappearingHours * 3_600_000L))
            } else {
                null
            }

            val senderDisplayName = senderProfile.name.ifBlank { "Bangla Chat User" }.take(60)
            val msgRef = db.collection("chats").document(chatId).collection("messages").document()
            val messagePayload = mutableMapOf<String, Any?>(
                "chatId" to chatId,
                "senderId" to uid,
                "senderName" to senderDisplayName,
                "participants" to liveChat.participants,
                "text" to cleanText,
                "type" to safeType,
                "mediaUrl" to mediaUrl?.take(1000),
                "mediaName" to mediaName?.take(200),
                "mediaSizeBytes" to mediaSizeBytes?.coerceIn(0L, 10_485_760L),
                "audioDurationSec" to audioDurationSec?.coerceIn(0L, 600L),
                "viewOnce" to if (isViewOnceImage) true else null,
                "viewedBy" to if (isViewOnceImage) emptyList<String>() else null,
                "delivered" to false,
                "read" to false,
                "deleted" to false,
                "edited" to false,
                "forwarded" to forwarded,
                "replyToId" to replyTo?.id?.takeIf { it.isNotBlank() }?.take(128),
                "replyToText" to replyTo?.text?.takeIf { it.isNotBlank() }?.take(300),
                "replyToSenderName" to replySenderName?.takeIf { it.isNotBlank() }?.take(80),
                "reactions" to emptyMap<String, String>(),
                "starredBy" to emptyList<String>(),
                "expiresAt" to expiresAtTimestamp,
                "timestamp" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            msgRef.set(messagePayload).await()

            val newUnread = liveChat.unreadCounts.toMutableMap()
            newUnread[uid] = 0L
            liveChat.participants.filter { it != uid }.forEach { otherUid ->
                newUnread[otherUid] = (liveChat.unreadCounts[otherUid] ?: 0L) + 1L
            }

            val updatedNames = liveChat.participantNames.toMutableMap().apply {
                put(uid, senderDisplayName)
            }
            val updatedPhotos = liveChat.participantPhotos.toMutableMap().apply {
                put(uid, senderProfile.photoUrl.take(500))
            }

            val preview = when {
                isViewOnceImage -> "📷 ১ বার দেখার ছবি"
                cleanText.isNotBlank() -> cleanText.take(500)
                safeType == "audio" -> "🎤"
                safeType == "image" -> "📷"
                else -> mediaName?.take(200) ?: "📎"
            }

            db.collection("chats").document(chatId).update(
                mapOf(
                    "lastMessage" to preview,
                    "lastMessageType" to safeType,
                    "lastSenderId" to uid,
                    "lastMessageTime" to FieldValue.serverTimestamp(),
                    "lastMessageDelivered" to false,
                    "lastMessageRead" to false,
                    "hiddenBy" to emptyList<String>(),
                    "unreadCounts" to newUnread,
                    "typingUids" to liveChat.typingUids.filter { it != uid },
                    "participantNames" to updatedNames,
                    "participantPhotos" to updatedPhotos,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, messagesPath)
            Result.failure(e)
        }
    }

    suspend fun sendPollMessage(
        chat: Chat,
        senderProfile: PublicProfile,
        question: String,
        options: List<String>
    ): Result<Unit> {
        val chatId = chat.id
        val messagesPath = "chats/$chatId/messages"
        return try {
            val uid = requireUserId()
            val liveChat = if (chat.isGroup) {
                val snap = db.collection("chats").document(chatId).get().await()
                snap.toObject(Chat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("This group no longer exists")
            } else {
                chat
            }
            require(uid in liveChat.participants) { "You are no longer a participant of this chat" }
            if (liveChat.isGroup && liveChat.onlyAdminsCanSend && !liveChat.isAdmin(uid)) {
                throw IllegalStateException("Only group admins can create polls")
            }
            val cleanQuestion = question.trim().take(240)
            val cleanOptions = options.map { it.trim().take(80) }.filter { it.isNotEmpty() }.distinct().take(6)
            require(cleanQuestion.isNotEmpty()) { "Poll question cannot be empty" }
            require(cleanOptions.size >= 2) { "Provide at least 2 poll options" }

            val senderDisplayName = senderProfile.name.ifBlank { "Bangla Chat User" }.take(60)
            val msgRef = db.collection("chats").document(chatId).collection("messages").document()
            val messagePayload = mapOf(
                "chatId" to chatId,
                "senderId" to uid,
                "senderName" to senderDisplayName,
                "participants" to liveChat.participants,
                "text" to cleanQuestion,
                "type" to "poll",
                "pollQuestion" to cleanQuestion,
                "pollOptions" to cleanOptions,
                "pollVotes" to emptyMap<String, String>(),
                "delivered" to false,
                "read" to false,
                "deleted" to false,
                "edited" to false,
                "forwarded" to false,
                "reactions" to emptyMap<String, String>(),
                "starredBy" to emptyList<String>(),
                "timestamp" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            msgRef.set(messagePayload).await()

            val newUnread = liveChat.unreadCounts.toMutableMap()
            newUnread[uid] = 0L
            liveChat.participants.filter { it != uid }.forEach { otherUid ->
                newUnread[otherUid] = (liveChat.unreadCounts[otherUid] ?: 0L) + 1L
            }
            val updatedNames = liveChat.participantNames.toMutableMap().apply { put(uid, senderDisplayName) }
            val updatedPhotos = liveChat.participantPhotos.toMutableMap().apply { put(uid, senderProfile.photoUrl.take(500)) }

            db.collection("chats").document(chatId).update(
                mapOf(
                    "lastMessage" to "📊 $cleanQuestion".take(500),
                    "lastMessageType" to "poll",
                    "lastSenderId" to uid,
                    "lastMessageTime" to FieldValue.serverTimestamp(),
                    "lastMessageDelivered" to false,
                    "lastMessageRead" to false,
                    "hiddenBy" to emptyList<String>(),
                    "unreadCounts" to newUnread,
                    "typingUids" to liveChat.typingUids.filter { it != uid },
                    "participantNames" to updatedNames,
                    "participantPhotos" to updatedPhotos,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, messagesPath)
            Result.failure(e)
        }
    }

    suspend fun voteOnPoll(
        chatId: String,
        message: ChatMessage,
        currentUserId: String,
        optionIndex: Int
    ): Result<Unit> {
        val path = "chats/$chatId/messages/${message.id}"
        return try {
            val uid = requireUserId()
            check(uid == currentUserId) { "Signed-in user mismatch" }
            if (optionIndex !in message.pollOptions.indices) return Result.success(Unit)
            val updatedVotes = message.pollVotes.toMutableMap()
            val choice = optionIndex.toString()
            if (updatedVotes[uid] == choice) {
                updatedVotes.remove(uid)
            } else {
                updatedVotes[uid] = choice
            }
            db.collection("chats").document(chatId)
                .collection("messages").document(message.id)
                .update(
                    mapOf(
                        "pollVotes" to updatedVotes,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markViewOnceOpened(
        chatId: String,
        message: ChatMessage,
        currentUserId: String
    ): Result<Unit> {
        val path = "chats/$chatId/messages/${message.id}"
        return try {
            val uid = requireUserId()
            check(uid == currentUserId) { "Signed-in user mismatch" }
            if (!message.viewOnce || message.senderId == uid || uid in message.viewedBy) {
                return Result.success(Unit)
            }
            val updated = (message.viewedBy + uid).distinct().take(50)
            db.collection("chats").document(chatId)
                .collection("messages").document(message.id)
                .update(
                    mapOf(
                        "viewedBy" to updated,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun logCallMessageInChat(
        callerProfile: PublicProfile,
        peerProfile: PublicProfile,
        callType: String,
        callStatus: String,
        durationSeconds: Long
    ): Result<Unit> {
        val callerUid = callerProfile.uid
        val peerUid = peerProfile.uid
        if (callerUid.isBlank() || peerUid.isBlank() || callerUid == peerUid) {
            return Result.success(Unit)
        }
        val chatId = buildChatId(callerUid, peerUid)
        val messagesPath = "chats/$chatId/messages"
        return try {
            val uid = requireUserId()
            check(uid == callerUid) { "Signed-in user mismatch" }

            val chatResult = getOrCreateChat(callerProfile, peerProfile)
            if (chatResult.isFailure) {
                return Result.failure(chatResult.exceptionOrNull() ?: IllegalStateException("Unable to open chat"))
            }

            val chatRef = db.collection("chats").document(chatId)
            val chatSnap = chatRef.get().await()
            val existingChat = chatSnap.toObject(
                Chat::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            val participants = existingChat?.participants?.takeIf { it.size == 2 }
                ?: listOf(callerUid, peerUid).sorted()
            val safeCallType = if (callType == "video") "video" else "audio"
            val safeCallStatus = when (callStatus) {
                "answered", "declined", "missed", "busy" -> callStatus
                else -> if (durationSeconds > 0L) "answered" else "missed"
            }
            val safeDuration = durationSeconds.coerceIn(0L, 86400L)

            val typeLabel = if (safeCallType == "video") "ভিডিও কল" else "অডিও কল"
            val summaryText = when {
                safeCallStatus == "answered" && safeDuration > 0L -> {
                    val durText = formatCallDurationBanglaCompact(safeDuration)
                    "$typeLabel • $durText"
                }
                safeCallStatus == "busy" -> "$typeLabel • অন্য কলে ব্যস্ত"
                safeCallStatus == "declined" -> "$typeLabel • বাতিল"
                else -> "মিসড $typeLabel"
            }.take(500)

            val senderDisplayName = callerProfile.name.ifBlank { "Bangla Chat User" }.take(60)
            val msgRef = chatRef.collection("messages").document()
            val messagePayload = mapOf(
                "chatId" to chatId,
                "senderId" to uid,
                "senderName" to senderDisplayName,
                "participants" to participants,
                "text" to summaryText,
                "type" to "call",
                "callType" to safeCallType,
                "callStatus" to safeCallStatus,
                "callDurationSec" to safeDuration,
                "delivered" to (safeCallStatus == "answered"),
                "read" to (safeCallStatus == "answered"),
                "deleted" to false,
                "deletedFor" to emptyList<String>(),
                "edited" to false,
                "forwarded" to false,
                "reactions" to emptyMap<String, String>(),
                "starredBy" to emptyList<String>(),
                "timestamp" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            msgRef.set(messagePayload).await()

            val updatedUnread = (existingChat?.unreadCounts ?: mapOf(callerUid to 0L, peerUid to 0L)).toMutableMap()
            updatedUnread[uid] = 0L
            if (safeCallStatus != "answered") {
                updatedUnread[peerUid] = (updatedUnread[peerUid] ?: 0L) + 1L
            }
            val updatedNames = (existingChat?.participantNames ?: emptyMap()).toMutableMap().apply {
                put(callerUid, senderDisplayName)
                putIfAbsent(peerUid, peerProfile.name.ifBlank { "Bangla Chat User" }.take(60))
            }
            val updatedPhotos = (existingChat?.participantPhotos ?: emptyMap()).toMutableMap().apply {
                put(callerUid, callerProfile.photoUrl.take(500))
                putIfAbsent(peerUid, peerProfile.photoUrl.take(500))
            }

            chatRef.update(
                mapOf(
                    "lastMessage" to summaryText,
                    "lastMessageType" to "call",
                    "lastSenderId" to uid,
                    "lastMessageTime" to FieldValue.serverTimestamp(),
                    "lastMessageDelivered" to (safeCallStatus == "answered"),
                    "lastMessageRead" to (safeCallStatus == "answered"),
                    "hiddenBy" to emptyList<String>(),
                    "unreadCounts" to updatedUnread,
                    "typingUids" to (existingChat?.typingUids ?: emptyList()).filter { it != uid },
                    "participantNames" to updatedNames,
                    "participantPhotos" to updatedPhotos,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, messagesPath)
            Result.failure(e)
        }
    }

    private fun formatCallDurationBanglaCompact(seconds: Long): String {
        val bangla = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        fun bn(v: Long): String = v.toString().map { if (it in '0'..'9') bangla[it - '0'] else it }.joinToString("")
        val hours = seconds / 3600L
        val mins = (seconds % 3600L) / 60L
        val secs = seconds % 60L
        val parts = buildList {
            if (hours > 0L) add("${bn(hours)} ঘণ্টা")
            if (mins > 0L) add("${bn(mins)} মিনিট")
            if (hours == 0L && secs > 0L) add("${bn(secs)} সেকেন্ড")
            if (hours > 0L && mins == 0L && secs > 0L) add("${bn(secs)} সেকেন্ড")
        }
        return parts.joinToString(" ")
    }

    suspend fun markConversationRead(
        chat: Chat,
        currentUserId: String,
        messages: List<ChatMessage>,
        sendReadReceipts: Boolean
    ): Result<Unit> {
        val chatPath = "chats/${chat.id}"
        return try {
            val uid = requireUserId()
            if (uid != currentUserId) return Result.success(Unit)

            val isLastFromPeer = chat.lastSenderId.isNotBlank() && chat.lastSenderId != currentUserId
            val needsUnreadReset = chat.unreadFor(currentUserId) > 0
            val needsDeliveryFlag = isLastFromPeer && !chat.lastMessageDelivered
            val needsReadFlag = isLastFromPeer && sendReadReceipts && !chat.lastMessageRead

            if (needsUnreadReset || needsDeliveryFlag || needsReadFlag) {
                val chatUpdates = mutableMapOf<String, Any>(
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (needsUnreadReset) {
                    val updatedUnread = chat.unreadCounts.toMutableMap()
                    updatedUnread[currentUserId] = 0L
                    chatUpdates["unreadCounts"] = updatedUnread
                }
                if (needsDeliveryFlag || needsReadFlag) {
                    chatUpdates["lastMessageDelivered"] = true
                }
                if (needsReadFlag) {
                    chatUpdates["lastMessageRead"] = true
                }
                db.collection("chats").document(chat.id).update(chatUpdates).await()
            }

            val incomingToUpdate = messages.filter { msg ->
                msg.senderId != currentUserId && (!msg.delivered || (sendReadReceipts && !msg.read))
            }.take(25)

            if (incomingToUpdate.isNotEmpty()) {
                val batch = db.batch()
                val col = db.collection("chats").document(chat.id).collection("messages")
                for (msg in incomingToUpdate) {
                    batch.update(
                        col.document(msg.id),
                        mapOf(
                            "delivered" to true,
                            "read" to if (sendReadReceipts) true else msg.read,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, chatPath)
            Result.failure(e)
        }
    }

    /**
     * WhatsApp-style automatic delivery receipt when recipient's device is online
     * (flips sender's single grey tick ✓ to double grey tick ✓✓ even before opening the chat).
     */
    suspend fun markChatDelivered(chat: Chat, currentUserId: String): Result<Unit> {
        val chatPath = "chats/${chat.id}"
        return try {
            val uid = auth.currentUser?.uid ?: return Result.success(Unit)
            if (uid != currentUserId) return Result.success(Unit)
            if (chat.lastSenderId.isBlank() || chat.lastSenderId == currentUserId) {
                return Result.success(Unit)
            }
            if (!chat.lastMessageDelivered) {
                db.collection("chats").document(chat.id).update(
                    mapOf(
                        "lastMessageDelivered" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            val undeliveredSnap = db.collection("chats").document(chat.id)
                .collection("messages")
                .whereEqualTo("delivered", false)
                .limit(20)
                .get()
                .await()
            val toDeliver = undeliveredSnap.documents.filter { doc ->
                doc.getString("senderId") != currentUserId
            }
            if (toDeliver.isNotEmpty()) {
                val batch = db.batch()
                for (doc in toDeliver) {
                    batch.update(
                        doc.reference,
                        mapOf(
                            "delivered" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, chatPath)
            Result.failure(e)
        }
    }

    suspend fun setTypingStatus(chat: Chat, currentUserId: String, isTyping: Boolean): Result<Unit> {
        val path = "chats/${chat.id}"
        return try {
            val uid = auth.currentUser?.uid ?: return Result.success(Unit)
            if (uid != currentUserId) return Result.success(Unit)
            val alreadyTyping = currentUserId in chat.typingUids
            if (alreadyTyping == isTyping) return Result.success(Unit)

            val updated = if (isTyping) {
                (chat.typingUids + currentUserId).distinct().take(50)
            } else {
                chat.typingUids.filter { it != currentUserId }
            }
            db.collection("chats").document(chat.id).update(
                mapOf(
                    "typingUids" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleReaction(
        chatId: String,
        message: ChatMessage,
        currentUserId: String,
        emoji: String
    ): Result<Unit> {
        val path = "chats/$chatId/messages/${message.id}"
        return try {
            val uid = requireUserId()
            check(uid == currentUserId) { "Signed-in user mismatch" }
            val updated = message.reactions.toMutableMap()
            if (updated[uid] == emoji) {
                updated.remove(uid)
            } else {
                updated[uid] = emoji
            }
            db.collection("chats").document(chatId)
                .collection("messages").document(message.id)
                .update(
                    mapOf(
                        "reactions" to updated,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteOwnMessage(chatId: String, messageId: String): Result<Unit> {
        val path = "chats/$chatId/messages/$messageId"
        return try {
            requireUserId()
            db.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .update(
                    mapOf(
                        "deleted" to true,
                        "text" to "",
                        "mediaUrl" to "",
                        "mediaName" to "",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMessageForMe(
        chatId: String,
        messageId: String,
        currentUserId: String
    ): Result<Unit> {
        val path = "chats/$chatId/messages/$messageId"
        return try {
            val uid = requireUserId()
            check(uid == currentUserId) { "Signed-in user mismatch" }
            db.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .update(
                    mapOf(
                        "deletedFor" to FieldValue.arrayUnion(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun editOwnMessage(chatId: String, messageId: String, newText: String): Result<Unit> {
        val path = "chats/$chatId/messages/$messageId"
        return try {
            requireUserId()
            val cleanText = newText.trim().take(4000)
            require(cleanText.isNotEmpty()) { "Edited message cannot be empty" }
            db.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .update(
                    mapOf(
                        "text" to cleanText,
                        "edited" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleStarMessage(
        chatId: String,
        message: ChatMessage,
        currentUserId: String
    ): Result<Unit> {
        val path = "chats/$chatId/messages/${message.id}"
        return try {
            val uid = requireUserId()
            check(uid == currentUserId) { "Signed-in user mismatch" }
            val updated = if (uid in message.starredBy) {
                message.starredBy.filter { it != uid }
            } else {
                (message.starredBy + uid).distinct().take(50)
            }
            db.collection("chats").document(chatId)
                .collection("messages").document(message.id)
                .update(
                    mapOf(
                        "starredBy" to updated,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun setPinnedMessage(
        chatId: String,
        message: ChatMessage?,
        senderName: String
    ): Result<Unit> {
        val path = "chats/$chatId"
        return try {
            requireUserId()
            val snippet = message?.let {
                it.text.ifBlank { it.mediaName.ifBlank { if (it.type == "image") "📷" else "📎" } }
            }.orEmpty().take(300)
            db.collection("chats").document(chatId).update(
                mapOf(
                    "pinnedMessageId" to (message?.id.orEmpty().take(128)),
                    "pinnedMessageText" to snippet,
                    "pinnedMessageSender" to (if (message != null) senderName.take(80) else ""),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun setDisappearingHours(chatId: String, hours: Long): Result<Unit> {
        val path = "chats/$chatId"
        return try {
            requireUserId()
            val safeHours = if (hours in setOf(0L, 24L, 168L)) hours else 0L
            db.collection("chats").document(chatId).update(
                mapOf(
                    "disappearingHours" to safeHours,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateChatParticipantFlags(
        chat: Chat,
        currentUserId: String,
        togglePin: Boolean = false,
        toggleMute: Boolean = false,
        toggleArchive: Boolean = false
    ): Result<Unit> {
        val path = "chats/${chat.id}"
        return try {
            val uid = requireUserId()
            check(uid == currentUserId) { "Signed-in user mismatch" }
            fun List<String>.toggled(): List<String> =
                if (uid in this) filter { it != uid } else (this + uid).distinct().take(50)

            val newPinned = if (togglePin) chat.pinnedBy.toggled() else chat.pinnedBy
            val newMuted = if (toggleMute) chat.mutedBy.toggled() else chat.mutedBy
            val newArchived = if (toggleArchive) chat.archivedBy.toggled() else chat.archivedBy

            db.collection("chats").document(chat.id).update(
                mapOf(
                    "pinnedBy" to newPinned,
                    "mutedBy" to newMuted,
                    "archivedBy" to newArchived,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addFavorite(currentUserId: String, profile: PublicProfile): Result<Unit> {
        val path = "users/$currentUserId/favorites/${profile.uid}"
        return try {
            val uid = requireUserId()
            check(uid == currentUserId) { "Signed-in user mismatch" }
            val payload = mapOf(
                "favoriteUid" to profile.uid,
                "name" to profile.name.ifBlank { "Bangla Chat User" }.take(60),
                "username" to profile.username.take(30),
                "photoUrl" to profile.photoUrl.take(500),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(currentUserId)
                .collection("favorites").document(profile.uid)
                .set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeFavorite(currentUserId: String, favoriteUid: String): Result<Unit> {
        val path = "users/$currentUserId/favorites/$favoriteUid"
        return try {
            val uid = requireUserId()
            check(uid == currentUserId) { "Signed-in user mismatch" }
            db.collection("users").document(currentUserId)
                .collection("favorites").document(favoriteUid)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun blockUser(currentUserId: String, target: PublicProfile): Result<Unit> {
        val path = "users/$currentUserId/blockedUsers/${target.uid}"
        return try {
            val uid = requireUserId()
            check(uid == currentUserId) { "Signed-in user mismatch" }
            require(target.uid != currentUserId) { "Cannot block yourself" }
            val payload = mapOf(
                "blockedUid" to target.uid,
                "blockedName" to target.name.ifBlank { "Bangla Chat User" }.take(60),
                "blockedUsername" to target.username.take(30),
                "blockedPhotoUrl" to target.photoUrl.take(500),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(currentUserId)
                .collection("blockedUsers").document(target.uid)
                .set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun unblockUser(currentUserId: String, blockedUid: String): Result<Unit> {
        val path = "users/$currentUserId/blockedUsers/$blockedUid"
        return try {
            val uid = requireUserId()
            check(uid == currentUserId) { "Signed-in user mismatch" }
            db.collection("users").document(currentUserId)
                .collection("blockedUsers").document(blockedUid)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun submitReport(
        reportedUid: String,
        reason: String,
        details: String
    ): Result<Unit> {
        val path = "reports"
        return try {
            val uid = requireUserId()
            require(reportedUid != uid) { "Cannot report your own account" }
            val safeReason = reason.trim().take(60).ifBlank { "Other" }
            val payload = mapOf(
                "reporterUid" to uid,
                "reportedUid" to reportedUid,
                "reason" to safeReason,
                "details" to details.trim().take(500),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("reports").document().set(payload).await()

            // Forward abuse report to Admin Telegram Bot with reported user's phone/username/UID
            runCatching {
                val reportedProfile = db.collection("profiles").document(reportedUid).get().await()
                    .toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                val reporterProfile = db.collection("profiles").document(uid).get().await()
                    .toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                TelegramNotifier.notifyUserReported(
                    reporterUid = uid,
                    reporterName = reporterProfile?.name?.ifBlank { auth.currentUser?.displayName.orEmpty() }.orEmpty(),
                    reportedUid = reportedUid,
                    reportedName = reportedProfile?.name.orEmpty(),
                    reportedUsername = reportedProfile?.username.orEmpty(),
                    reportedPhone = reportedProfile?.phoneLookup.orEmpty(),
                    reason = safeReason,
                    details = details.trim().take(500)
                )
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- 24-HOUR STATUS STORIES ---

    fun observeActiveStatuses(): Flow<List<StatusStory>> {
        val path = "statuses"
        return db.collection("statuses")
            .limit(100)
            .snapshots()
            .map { snap ->
                val now = System.currentTimeMillis()
                snap.toObjects(
                    StatusStory::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).filter { !it.isExpiredNow(now) }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val root = extractFirestoreRootError(error)
                if (root == null && error is CancellationException) throw error
                if (root != null && auth.currentUser != null) {
                    handleFirestoreError(root, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun postStatus(
        author: PublicProfile,
        type: String,
        text: String,
        mediaUrl: String? = null,
        backgroundColorHex: Long = 0xFF2563EBL
    ): Result<Unit> {
        val path = "statuses"
        return try {
            val uid = requireUserId()
            check(uid == author.uid) { "Signed-in user mismatch" }
            val safeType = if (type == "image" && !mediaUrl.isNullOrBlank()) "image" else "text"
            val cleanText = text.trim().take(500)
            require(cleanText.isNotEmpty() || safeType == "image") { "Status cannot be empty" }

            val expiresAt = Timestamp(Date(System.currentTimeMillis() + 24L * 3_600_000L))
            val payload = mutableMapOf<String, Any?>(
                "uid" to uid,
                "authorName" to author.name.ifBlank { "Bangla Chat User" }.take(60),
                "authorPhotoUrl" to author.visiblePhotoUrl().take(500),
                "type" to safeType,
                "text" to cleanText,
                "mediaUrl" to mediaUrl?.take(1000),
                "backgroundColorHex" to backgroundColorHex,
                "viewerUids" to listOf(uid),
                "expiresAt" to expiresAt,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            db.collection("statuses").document().set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun markStatusViewed(status: StatusStory, viewerUid: String): Result<Unit> {
        val path = "statuses/${status.id}"
        return try {
            val uid = auth.currentUser?.uid ?: return Result.success(Unit)
            if (uid != viewerUid || uid in status.viewerUids) return Result.success(Unit)
            val updatedViewers = (status.viewerUids + uid).distinct().take(200)
            db.collection("statuses").document(status.id).update(
                mapOf("viewerUids" to updatedViewers)
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteStatus(statusId: String): Result<Unit> {
        val path = "statuses/$statusId"
        return try {
            requireUserId()
            db.collection("statuses").document(statusId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- REAL-TIME WEBRTC CALL SIGNALING & LOGS ---

    fun observeIncomingCalls(currentUserId: String): Flow<List<CallSession>> {
        val path = "callSessions"
        return db.collection("callSessions")
            .whereArrayContains("participants", currentUserId)
            .limit(20)
            .snapshots()
            .map { snap ->
                val cutoff = System.currentTimeMillis() - 90_000L
                snap.toObjects(
                    CallSession::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).filter { session ->
                    session.status in setOf("calling", "ringing") &&
                        session.calleeUid == currentUserId &&
                        (session.createdAt?.toDate()?.time ?: System.currentTimeMillis()) >= cutoff
                }.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val root = extractFirestoreRootError(error)
                if (root == null && error is CancellationException) throw error
                if (root != null && auth.currentUser?.uid == currentUserId) {
                    handleFirestoreError(root, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeCallSession(callId: String): Flow<CallSession?> {
        val path = "callSessions/$callId"
        return db.collection("callSessions").document(callId)
            .snapshots()
            .map { snap ->
                snap.toObject(
                    CallSession::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val root = extractFirestoreRootError(error)
                if (root == null && error is CancellationException) throw error
                if (root != null && auth.currentUser != null) {
                    handleFirestoreError(root, OperationType.GET, path)
                }
                throw error
            }
    }

    suspend fun getCallSession(callId: String): Result<CallSession?> {
        val path = "callSessions/$callId"
        return try {
            requireUserId()
            val snap = db.collection("callSessions").document(callId).get().await()
            val session = snap.toObject(
                CallSession::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(session)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    fun observeIceCandidates(callId: String, currentUserId: String): Flow<List<IceCandidateRecord>> {
        val path = "callSessions/$callId/candidates"
        return db.collection("callSessions").document(callId)
            .collection("candidates")
            .whereArrayContains("participants", currentUserId)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    IceCandidateRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val root = extractFirestoreRootError(error)
                if (root == null && error is CancellationException) throw error
                if (root != null && auth.currentUser?.uid == currentUserId) {
                    handleFirestoreError(root, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun createCallSession(
        caller: PublicProfile,
        callee: PublicProfile,
        callType: String,
        offerSdp: String
    ): Result<CallSession> {
        val callId = "call_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "callSessions/$callId"
        return try {
            val uid = requireUserId()
            check(uid == caller.uid) { "Signed-in user mismatch" }
            val safeType = if (callType == "video") "video" else "audio"
            val participants = listOf(caller.uid, callee.uid)
            val payload = mapOf(
                "callerUid" to caller.uid,
                "callerName" to caller.name.ifBlank { "Bangla Chat User" }.take(60),
                "callerPhotoUrl" to caller.photoUrl.take(500),
                "calleeUid" to callee.uid,
                "calleeName" to callee.name.ifBlank { "Bangla Chat User" }.take(60),
                "calleePhotoUrl" to callee.photoUrl.take(500),
                "participants" to participants,
                "callType" to safeType,
                "status" to "calling",
                "offerSdp" to offerSdp.take(25000),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("callSessions").document(callId).set(payload).await()
            Result.success(
                CallSession(
                    id = callId,
                    callerUid = caller.uid,
                    callerName = caller.name,
                    callerPhotoUrl = caller.photoUrl,
                    calleeUid = callee.uid,
                    calleeName = callee.name,
                    calleePhotoUrl = callee.photoUrl,
                    participants = participants,
                    callType = safeType,
                    status = "calling",
                    offerSdp = offerSdp
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun acceptCallSession(callId: String, answerSdp: String): Result<Unit> {
        val path = "callSessions/$callId"
        return try {
            requireUserId()
            db.collection("callSessions").document(callId).update(
                mapOf(
                    "status" to "accepted",
                    "answerSdp" to answerSdp.take(25000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markCallSessionRinging(callId: String): Result<Unit> {
        return updateCallSessionStatus(callId, "ringing")
    }

    suspend fun updateCallSessionStatus(callId: String, status: String): Result<Unit> {
        val path = "callSessions/$callId"
        return try {
            requireUserId()
            db.collection("callSessions").document(callId).update(
                mapOf(
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addIceCandidate(
        callId: String,
        participants: List<String>,
        sdpMid: String,
        sdpMLineIndex: Int,
        sdp: String
    ): Result<Unit> {
        val path = "callSessions/$callId/candidates"
        return try {
            val uid = requireUserId()
            val payload = mapOf(
                "senderUid" to uid,
                "participants" to participants,
                "sdpMid" to sdpMid.take(64),
                "sdpMLineIndex" to sdpMLineIndex.coerceIn(0, 32),
                "sdp" to sdp.take(2000),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("callSessions").document(callId)
                .collection("candidates").document()
                .set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordCallLog(
        uid: String,
        peerUid: String,
        peerName: String,
        peerPhotoUrl: String,
        direction: String,
        callType: String,
        durationSeconds: Long
    ): Result<Unit> {
        val path = "users/$uid/callLogs"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "Signed-in user mismatch" }
            val payload = mapOf(
                "peerUid" to peerUid.take(128),
                "peerName" to peerName.ifBlank { "Bangla Chat User" }.take(60),
                "peerPhotoUrl" to peerPhotoUrl.take(500),
                "direction" to if (direction in setOf("incoming", "outgoing", "missed")) direction else "outgoing",
                "callType" to if (callType == "video") "video" else "audio",
                "durationSeconds" to durationSeconds.coerceIn(0L, 86400L),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).collection("callLogs").document().set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteUserFirestoreData(uid: String): Result<Unit> {
        val path = "users/$uid"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "Signed-in user mismatch" }
            val profileRef = db.collection("profiles").document(uid)
            val profileSnap = profileRef.get().await()
            val ownedUsername = normalizeUsername(profileSnap.getString("username").orEmpty())
            if (USERNAME_REGEX.matches(ownedUsername)) {
                runCatching {
                    db.collection("usernames").document(ownedUsername).delete().await()
                }
            }
            profileRef.delete().await()
            db.collection("users").document(uid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- ACCOUNT MODERATION & SUPER ADMIN CONTROLS ---

    fun observeMyModerationStatus(uid: String): Flow<ModerationStatus?> {
        val path = "moderation/$uid"
        return db.collection("moderation").document(uid)
            .snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(
                    ModerationStatus::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val root = extractFirestoreRootError(error)
                if (root == null && error is CancellationException) throw error
                if (root != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(root, OperationType.GET, path)
                }
                emit(null)
            }
    }

    fun observeAllProfilesForAdmin(): Flow<List<PublicProfile>> {
        val path = "profiles"
        return db.collection("profiles")
            .limit(200)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    PublicProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val root = extractFirestoreRootError(error)
                if (root == null && error is CancellationException) throw error
                if (root != null && isCurrentUserSuperAdmin()) {
                    handleFirestoreError(root, OperationType.LIST, path)
                }
                emit(emptyList())
            }
    }

    fun observeAllModerationStatusesForAdmin(): Flow<List<ModerationStatus>> {
        val path = "moderation"
        return db.collection("moderation")
            .limit(200)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    ModerationStatus::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val root = extractFirestoreRootError(error)
                if (root == null && error is CancellationException) throw error
                if (root != null && isCurrentUserSuperAdmin()) {
                    handleFirestoreError(root, OperationType.LIST, path)
                }
                emit(emptyList())
            }
    }

    fun observeAllReportsForAdmin(): Flow<List<UserReport>> {
        val path = "reports"
        return db.collection("reports")
            .limit(150)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    UserReport::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val root = extractFirestoreRootError(error)
                if (root == null && error is CancellationException) throw error
                if (root != null && isCurrentUserSuperAdmin()) {
                    handleFirestoreError(root, OperationType.LIST, path)
                }
                emit(emptyList())
            }
    }

    suspend fun adminSetBanStatus(
        targetUid: String,
        targetName: String,
        targetUsername: String,
        banned: Boolean,
        reason: String
    ): Result<Unit> {
        val path = "moderation/$targetUid"
        return try {
            requireUserId()
            check(isCurrentUserSuperAdmin()) { "Admin privileges required" }
            val adminEmail = auth.currentUser?.email.orEmpty().ifBlank { SUPER_ADMIN_EMAIL }.take(120)
            val safeReason = reason.trim().take(300)
            val payload = mapOf(
                "uid" to targetUid,
                "banned" to banned,
                "deletedByAdmin" to false,
                "mutedUntil" to null,
                "reason" to safeReason,
                "updatedBy" to adminEmail,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("moderation").document(targetUid).set(payload).await()
            TelegramNotifier.notifyModerationAction(
                actionTitle = if (banned) "🚫 স্থায়ী ব্যান (Permanent Ban)" else "✅ আনব্যান (Unban)",
                targetUid = targetUid,
                targetName = targetName,
                targetUsername = targetUsername,
                reason = safeReason
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun adminMuteUser(
        targetUid: String,
        targetName: String,
        targetUsername: String,
        durationHours: Long,
        reason: String
    ): Result<Unit> {
        val path = "moderation/$targetUid"
        return try {
            requireUserId()
            check(isCurrentUserSuperAdmin()) { "Admin privileges required" }
            val safeHours = durationHours.coerceIn(1L, 24L * 365L)
            val adminEmail = auth.currentUser?.email.orEmpty().ifBlank { SUPER_ADMIN_EMAIL }.take(120)
            val safeReason = reason.trim().take(300)
            val untilTs = Timestamp(Date(System.currentTimeMillis() + safeHours * 3_600_000L))
            val payload = mapOf(
                "uid" to targetUid,
                "banned" to false,
                "deletedByAdmin" to false,
                "mutedUntil" to untilTs,
                "reason" to safeReason,
                "updatedBy" to adminEmail,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("moderation").document(targetUid).set(payload).await()
            TelegramNotifier.notifyModerationAction(
                actionTitle = "🔇 মিউট করা হয়েছে ($safeHours ঘণ্টা)",
                targetUid = targetUid,
                targetName = targetName,
                targetUsername = targetUsername,
                reason = safeReason
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun adminUnbanAndUnmute(
        targetUid: String,
        targetName: String,
        targetUsername: String
    ): Result<Unit> {
        val path = "moderation/$targetUid"
        return try {
            requireUserId()
            check(isCurrentUserSuperAdmin()) { "Admin privileges required" }
            val adminEmail = auth.currentUser?.email.orEmpty().ifBlank { SUPER_ADMIN_EMAIL }.take(120)
            val payload = mapOf(
                "uid" to targetUid,
                "banned" to false,
                "deletedByAdmin" to false,
                "mutedUntil" to null,
                "reason" to "",
                "updatedBy" to adminEmail,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("moderation").document(targetUid).set(payload).await()
            TelegramNotifier.notifyModerationAction(
                actionTitle = "✅ সব নিষেধাজ্ঞা সরানো হয়েছে (Unban/Unmute)",
                targetUid = targetUid,
                targetName = targetName,
                targetUsername = targetUsername,
                reason = "Admin lifted restriction"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun adminDeleteUserAccount(
        targetUid: String,
        targetName: String,
        targetUsername: String,
        reason: String
    ): Result<Unit> {
        val path = "moderation/$targetUid"
        return try {
            requireUserId()
            check(isCurrentUserSuperAdmin()) { "Admin privileges required" }
            val adminEmail = auth.currentUser?.email.orEmpty().ifBlank { SUPER_ADMIN_EMAIL }.take(120)
            val safeReason = reason.trim().take(300).ifBlank { "Removed by Admin" }

            // 1. Mark permanently deletedByAdmin + banned in /moderation/{uid}
            val modPayload = mapOf(
                "uid" to targetUid,
                "banned" to true,
                "deletedByAdmin" to true,
                "mutedUntil" to null,
                "reason" to safeReason,
                "updatedBy" to adminEmail,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("moderation").document(targetUid).set(modPayload).await()

            // 2. Remove public profile, username reservation, and private user record
            val cleanUsername = normalizeUsername(targetUsername)
            if (USERNAME_REGEX.matches(cleanUsername)) {
                runCatching { db.collection("usernames").document(cleanUsername).delete().await() }
            }
            runCatching { db.collection("profiles").document(targetUid).delete().await() }
            runCatching { db.collection("users").document(targetUid).delete().await() }

            TelegramNotifier.notifyModerationAction(
                actionTitle = "🗑️ অ্যাকাউন্ট স্থায়ীভাবে রিমুভ করা হয়েছে",
                targetUid = targetUid,
                targetName = targetName,
                targetUsername = targetUsername,
                reason = safeReason
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun adminDismissReport(reportId: String): Result<Unit> {
        val path = "reports/$reportId"
        return try {
            requireUserId()
            check(isCurrentUserSuperAdmin()) { "Admin privileges required" }
            db.collection("reports").document(reportId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- GLOBAL ADMIN BROADCAST, VERIFIED BADGE & AUTO-MODERATION CONFIG ---

    fun observeAppBroadcast(): Flow<AppBroadcast?> {
        val path = "appConfig/broadcast"
        return db.collection("appConfig").document("broadcast")
            .snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(
                    AppBroadcast::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val root = extractFirestoreRootError(error)
                if (root == null && error is CancellationException) throw error
                if (root != null && auth.currentUser != null) {
                    handleFirestoreError(root, OperationType.GET, path)
                }
                emit(null)
            }
    }

    fun observeModerationSettings(): Flow<ModerationSettings> {
        val path = "appConfig/moderation"
        return db.collection("appConfig").document("moderation")
            .snapshots()
            .map { snap ->
                if (!snap.exists()) ModerationSettings()
                else snap.toObject(
                    ModerationSettings::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: ModerationSettings()
            }
            .catch { error ->
                val root = extractFirestoreRootError(error)
                if (root == null && error is CancellationException) throw error
                if (root != null && auth.currentUser != null) {
                    handleFirestoreError(root, OperationType.GET, path)
                }
                emit(ModerationSettings())
            }
    }

    suspend fun adminPublishBroadcast(
        active: Boolean,
        title: String,
        message: String,
        severity: String
    ): Result<Unit> {
        val path = "appConfig/broadcast"
        return try {
            requireUserId()
            check(isCurrentUserSuperAdmin()) { "Admin privileges required" }
            val adminEmail = auth.currentUser?.email.orEmpty().ifBlank { SUPER_ADMIN_EMAIL }.take(120)
            val safeSeverity = when (severity) {
                "update", "warning" -> severity
                else -> "info"
            }
            val safeTitle = title.trim().take(120)
            val safeMessage = message.trim().take(600)
            val broadcastId = if (active) "bc_${System.currentTimeMillis()}" else "cleared_${System.currentTimeMillis()}"
            val payload = mapOf(
                "active" to active,
                "broadcastId" to broadcastId,
                "title" to safeTitle,
                "message" to safeMessage,
                "severity" to safeSeverity,
                "updatedBy" to adminEmail,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("appConfig").document("broadcast").set(payload).await()
            TelegramNotifier.notifyModerationAction(
                actionTitle = if (active) "📢 ব্রডকাস্ট নোটিশ প্রকাশিত ($safeSeverity)" else "🔕 ব্রডকাস্ট নোটিশ বন্ধ করা হয়েছে",
                targetUid = "ALL_USERS",
                targetName = safeTitle.ifBlank { "Global Notice" },
                targetUsername = "",
                reason = safeMessage.ifBlank { "Broadcast cleared" }
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateModerationSettings(settings: ModerationSettings): Result<Unit> {
        val path = "appConfig/moderation"
        return try {
            requireUserId()
            check(isCurrentUserSuperAdmin()) { "Admin privileges required" }
            val adminEmail = auth.currentUser?.email.orEmpty().ifBlank { SUPER_ADMIN_EMAIL }.take(120)
            val safeMode = if (settings.isMaskMode()) "mask" else "block"
            val cleanedWords = settings.customBlockedWords
                .map { it.trim().take(40) }
                .filter { it.length >= 2 }
                .distinctBy { it.lowercase() }
                .take(200)
            val payload = mapOf(
                "profanityFilterEnabled" to settings.profanityFilterEnabled,
                "profanityMode" to safeMode,
                "spamProtectionEnabled" to settings.spamProtectionEnabled,
                "telegramAlertsEnabled" to settings.telegramAlertsEnabled,
                "customBlockedWords" to cleanedWords,
                "updatedBy" to adminEmail,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("appConfig").document("moderation").set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun adminSetVerifiedBadge(
        targetUid: String,
        targetName: String,
        targetUsername: String,
        verified: Boolean
    ): Result<Unit> {
        val path = "profiles/$targetUid"
        return try {
            requireUserId()
            check(isCurrentUserSuperAdmin()) { "Admin privileges required" }
            db.collection("profiles").document(targetUid).update(
                mapOf(
                    "verified" to verified,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            TelegramNotifier.notifyModerationAction(
                actionTitle = if (verified) "☑️ ভেরিফায়েড ব্যাজ দেওয়া হয়েছে" else "ℹ️ ভেরিফায়েড ব্যাজ সরানো হয়েছে",
                targetUid = targetUid,
                targetName = targetName,
                targetUsername = targetUsername,
                reason = if (verified) "Verified by Super Admin" else "Verification removed by Super Admin"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun notifyAutoModerationViolation(
        senderProfile: PublicProfile,
        violationType: ModerationViolationType,
        flaggedSnippet: String,
        matchedWords: List<String>,
        chatId: String
    ) {
        val uid = senderProfile.uid.ifBlank { auth.currentUser?.uid.orEmpty() }
        if (uid.isBlank()) return
        val phone = senderProfile.phoneLookup.ifBlank {
            runCatching {
                db.collection("users").document(uid).get().await().getString("phone").orEmpty()
            }.getOrDefault("")
        }
        TelegramNotifier.notifyContentViolation(
            senderUid = uid,
            senderName = senderProfile.name,
            senderUsername = senderProfile.username,
            senderPhone = phone,
            violationType = violationType,
            flaggedSnippet = flaggedSnippet,
            matchedWords = matchedWords,
            chatId = chatId
        )
    }
}
