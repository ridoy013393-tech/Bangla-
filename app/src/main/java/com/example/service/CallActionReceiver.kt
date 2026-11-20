package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.model.Chat
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DECLINE_CALL -> handleDeclineCall(context, intent)
            ACTION_DIRECT_REPLY -> handleDirectReply(context, intent)
        }
    }

    private fun handleDeclineCall(context: Context, intent: Intent) {
        val callId = intent.getStringExtra(EXTRA_CALL_ID).orEmpty()
        if (callId.isBlank()) return

        NotificationHelper.cancelIncomingCallNotification(context, callId)

        val uid = Firebase.auth.currentUser?.uid ?: return
        val pendingResult = goAsync()
        val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        val path = "callSessions/$callId"
        db.collection("callSessions")
            .document(callId)
            .update(
                mapOf(
                    "status" to "declined",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnFailureListener { error ->
                if (Firebase.auth.currentUser?.uid == uid) {
                    handleFirestoreError(error, OperationType.UPDATE, path)
                }
            }
            .addOnCompleteListener {
                pendingResult.finish()
            }
    }

    private fun handleDirectReply(context: Context, intent: Intent) {
        val chatId = intent.getStringExtra(EXTRA_CHAT_ID).orEmpty()
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, chatId.hashCode())
        val replyText = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(KEY_TEXT_REPLY)
            ?.toString()
            ?.trim()
            ?.take(4000)
            .orEmpty()

        if (chatId.isBlank() || replyText.isBlank()) return
        val user = Firebase.auth.currentUser ?: return
        val uid = user.uid

        val pendingResult = goAsync()
        val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        val chatRef = db.collection("chats").document(chatId)

        chatRef.get()
            .addOnSuccessListener { snap ->
                val chat = snap.toObject(Chat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (chat == null || uid !in chat.participants) {
                    pendingResult.finish()
                    return@addOnSuccessListener
                }
                val senderName = chat.participantNames[uid]
                    ?.takeIf { it.isNotBlank() }
                    ?: user.displayName?.takeIf { it.isNotBlank() }
                    ?: "Bangla Chat User"
                val senderPhoto = chat.participantPhotos[uid]
                    ?: user.photoUrl?.toString().orEmpty()

                val msgRef = chatRef.collection("messages").document()
                val msgPayload = mapOf(
                    "chatId" to chatId,
                    "senderId" to uid,
                    "senderName" to senderName.take(60),
                    "participants" to chat.participants,
                    "text" to replyText,
                    "type" to "text",
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

                val newUnread = chat.unreadCounts.toMutableMap()
                newUnread[uid] = 0L
                chat.participants.filter { it != uid }.forEach { otherUid ->
                    newUnread[otherUid] = (chat.unreadCounts[otherUid] ?: 0L) + 1L
                }
                val updatedNames = chat.participantNames.toMutableMap().apply { put(uid, senderName.take(60)) }
                val updatedPhotos = chat.participantPhotos.toMutableMap().apply { put(uid, senderPhoto.take(500)) }

                msgRef.set(msgPayload)
                    .addOnSuccessListener {
                        chatRef.update(
                            mapOf(
                                "lastMessage" to replyText.take(500),
                                "lastMessageType" to "text",
                                "lastSenderId" to uid,
                                "lastMessageTime" to FieldValue.serverTimestamp(),
                                "lastMessageDelivered" to false,
                                "lastMessageRead" to false,
                                "unreadCounts" to newUnread,
                                "typingUids" to chat.typingUids.filter { it != uid },
                                "participantNames" to updatedNames,
                                "participantPhotos" to updatedPhotos,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).addOnCompleteListener {
                            runCatching {
                                NotificationManagerCompat.from(context).cancel(notificationId)
                            }
                            pendingResult.finish()
                        }
                    }
                    .addOnFailureListener { err ->
                        Log.w(TAG, "Direct reply message write failed", err)
                        pendingResult.finish()
                    }
            }
            .addOnFailureListener { err ->
                Log.w(TAG, "Direct reply chat fetch failed", err)
                pendingResult.finish()
            }
    }

    companion object {
        private const val TAG = "CallActionReceiver"
        const val ACTION_DECLINE_CALL = "com.example.action.DECLINE_INCOMING_CALL"
        const val EXTRA_CALL_ID = "extra_call_id"

        const val ACTION_DIRECT_REPLY = "com.example.action.DIRECT_REPLY_MESSAGE"
        const val EXTRA_CHAT_ID = "extra_direct_reply_chat_id"
        const val EXTRA_NOTIFICATION_ID = "extra_direct_reply_notification_id"
        const val KEY_TEXT_REPLY = "key_direct_text_reply"
    }
}
