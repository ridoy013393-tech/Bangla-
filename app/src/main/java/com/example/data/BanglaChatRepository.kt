package com.example.data

import android.content.Context
import com.example.R
import com.example.model.BlockedUser
import com.example.model.CallLog
import com.example.model.CallSession
import com.example.model.Chat
import com.example.model.ChatMessage
import com.example.model.FavoriteContact
import com.example.model.IceCandidateRecord
import com.example.model.PrivateUser
import com.example.model.PublicProfile
import com.example.model.StatusStory
import com.google.firebase.Firebase
import java.util.UUID
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
        private val USERNAME_REGEX = Regex("^[a-z0-9_]{3,30}$")

        fun normalizeUsername(raw: String): String =
            raw.trim().trimStart('@').trim().lowercase()

        fun isValidUsernameFormat(raw: String): Boolean =
            USERNAME_REGEX.matches(normalizeUsername(raw))
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
            .whereArrayContains("participants", currentUserId)
            .limit(limit.coerceIn(20L, 500L))
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    ChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.timestamp ?: Timestamp(0, 0) }
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
                    "createdBy" to myUid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                chatRef.set(payload).await()
            }
            Result.success(chatId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createGroupChat(
        creator: PublicProfile,
        members: List<PublicProfile>,
        groupName: String,
        groupPhotoUrl: String = ""
    ): Result<String> {
        val cleanName = groupName.trim().take(80)
        val chatId = "group_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "chats/$chatId"
        return try {
            val uid = requireUserId()
            check(uid == creator.uid) { "Signed-in user mismatch" }
            require(cleanName.isNotEmpty()) { "Group name cannot be empty" }
            val distinctMembers = (listOf(creator) + members).distinctBy { it.uid }.take(50)
            require(distinctMembers.size >= 2) { "Select at least one member for the group" }

            val participantIds = distinctMembers.map { it.uid }
            val namesMap = distinctMembers.associate {
                it.uid to it.name.ifBlank { "Bangla Chat User" }.take(60)
            }
            val photosMap = distinctMembers.associate {
                it.uid to it.photoUrl.take(500)
            }
            val unreadMap = participantIds.associateWith { 0L }

            val payload = mapOf(
                "participants" to participantIds,
                "participantNames" to namesMap,
                "participantPhotos" to photosMap,
                "isGroup" to true,
                "groupName" to cleanName,
                "groupPhotoUrl" to groupPhotoUrl.take(500),
                "adminUids" to listOf(uid),
                "lastMessage" to "",
                "lastMessageType" to "text",
                "lastSenderId" to uid,
                "lastMessageTime" to FieldValue.serverTimestamp(),
                "unreadCounts" to unreadMap,
                "typingUids" to emptyList<String>(),
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("chats").document(chatId).set(payload).await()
            Result.success(chatId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun renameGroup(chatId: String, newName: String): Result<Unit> {
        val path = "chats/$chatId"
        return try {
            requireUserId()
            val clean = newName.trim().take(80)
            require(clean.isNotEmpty()) { "Group name cannot be empty" }
            db.collection("chats").document(chatId).update(
                mapOf(
                    "groupName" to clean,
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

    suspend fun addGroupMembers(chat: Chat, newMembers: List<PublicProfile>): Result<Unit> {
        val path = "chats/${chat.id}"
        return try {
            requireUserId()
            val added = newMembers.filter { it.uid !in chat.participants }
            if (added.isEmpty()) return Result.success(Unit)
            val updatedParticipants = (chat.participants + added.map { it.uid }).distinct().take(50)
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
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun leaveGroup(chat: Chat, currentUserId: String): Result<Unit> {
        val path = "chats/${chat.id}"
        return try {
            val uid = requireUserId()
            check(uid == currentUserId) { "Signed-in user mismatch" }
            val remaining = chat.participants.filter { it != uid }
            if (remaining.isEmpty()) return Result.success(Unit)
            val remainingAdmins = chat.adminUids.filter { it != uid }.ifEmpty { listOf(remaining.first()) }
            db.collection("chats").document(chat.id).update(
                mapOf(
                    "participants" to remaining,
                    "adminUids" to remainingAdmins,
                    "typingUids" to chat.typingUids.filter { it != uid },
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
        forwarded: Boolean = false
    ): Result<Unit> {
        val chatId = chat.id
        val messagesPath = "chats/$chatId/messages"
        return try {
            val uid = requireUserId()
            require(uid in chat.participants) { "Not a participant of this chat" }
            val cleanText = text.trim().take(4000)
            require(cleanText.isNotEmpty() || !mediaUrl.isNullOrBlank()) { "Message cannot be empty" }
            val safeType = if (type in setOf("text", "image", "document", "audio")) type else "text"

            val expiresAtTimestamp = if (chat.disappearingHours in setOf(24L, 168L)) {
                Timestamp(Date(System.currentTimeMillis() + chat.disappearingHours * 3_600_000L))
            } else {
                null
            }

            val senderDisplayName = senderProfile.name.ifBlank { "Bangla Chat User" }.take(60)
            val msgRef = db.collection("chats").document(chatId).collection("messages").document()
            val messagePayload = mutableMapOf<String, Any?>(
                "chatId" to chatId,
                "senderId" to uid,
                "senderName" to senderDisplayName,
                "participants" to chat.participants,
                "text" to cleanText,
                "type" to safeType,
                "mediaUrl" to mediaUrl?.take(1000),
                "mediaName" to mediaName?.take(200),
                "mediaSizeBytes" to mediaSizeBytes?.coerceIn(0L, 10_485_760L),
                "audioDurationSec" to audioDurationSec?.coerceIn(0L, 600L),
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

            val newUnread = chat.unreadCounts.toMutableMap()
            newUnread[uid] = 0L
            chat.participants.filter { it != uid }.forEach { otherUid ->
                newUnread[otherUid] = (chat.unreadCounts[otherUid] ?: 0L) + 1L
            }

            val updatedNames = chat.participantNames.toMutableMap().apply {
                put(uid, senderDisplayName)
            }
            val updatedPhotos = chat.participantPhotos.toMutableMap().apply {
                put(uid, senderProfile.photoUrl.take(500))
            }

            val preview = when {
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
                    "unreadCounts" to newUnread,
                    "typingUids" to chat.typingUids.filter { it != uid },
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

            if (chat.unreadFor(currentUserId) > 0) {
                val updatedUnread = chat.unreadCounts.toMutableMap()
                updatedUnread[currentUserId] = 0L
                db.collection("chats").document(chat.id).update(
                    mapOf(
                        "unreadCounts" to updatedUnread,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
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
                    session.status == "ringing" &&
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
                "status" to "ringing",
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
                    status = "ringing",
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
}
