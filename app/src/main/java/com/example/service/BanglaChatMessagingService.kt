package com.example.service

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class BanglaChatMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = Firebase.auth.currentUser?.uid ?: return
        val dbId = getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(dbId)
        val path = "users/$uid"
        db.collection("users").document(uid)
            .update(
                mapOf(
                    "fcmToken" to token.take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnFailureListener { error ->
                handleFirestoreError(error, OperationType.UPDATE, path)
            }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        NotificationHelper.createAllChannels(applicationContext)

        val type = message.data["type"].orEmpty()
        if (type == "call") {
            val callId = message.data["callId"].orEmpty()
            val callerName = message.data["callerName"]
                ?: message.notification?.title
                ?: getString(R.string.notification_sender_default)
            val callType = message.data["callType"] ?: "audio"
            if (callId.isNotBlank() && ActiveAppTracker.activeCallId != callId) {
                CallRingtonePlayer.startRinging(applicationContext, callId)
                NotificationHelper.showIncomingCallNotification(
                    context = applicationContext,
                    callId = callId,
                    callerName = callerName,
                    callType = callType
                )
            }
            return
        }

        val chatId = message.data["chatId"].orEmpty()
        if (ActiveAppTracker.isAppInForeground && ActiveAppTracker.activeChatId == chatId && chatId.isNotBlank()) {
            return
        }

        val senderName = message.data["senderName"]
            ?: message.notification?.title
            ?: getString(R.string.notification_sender_default)
        val showPreview = message.data["showPreview"]?.toBooleanStrictOrNull() ?: true
        val body = if (showPreview) {
            message.data["previewText"]
                ?: message.notification?.body
                ?: getString(R.string.notification_new_message_hidden)
        } else {
            getString(R.string.notification_new_message_hidden)
        }

        NotificationHelper.showMessageNotification(
            context = applicationContext,
            chatId = chatId,
            senderName = senderName,
            body = body
        )
    }

    companion object {
        const val EXTRA_CHAT_ID = NotificationHelper.EXTRA_CHAT_ID

        fun createMessageNotificationChannel(context: Context) {
            NotificationHelper.createAllChannels(context)
        }
    }
}
