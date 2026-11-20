package com.example.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
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
        createMessageNotificationChannel(applicationContext)

        val chatId = message.data["chatId"].orEmpty()
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("FCM", "POST_NOTIFICATIONS not granted; skipping local notification")
            return
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (chatId.isNotBlank()) {
                putExtra(EXTRA_CHAT_ID, chatId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            chatId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = getString(R.string.notification_channel_messages_id)
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(senderName)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this)
            .notify(chatId.ifBlank { senderName }.hashCode(), notification)
    }

    companion object {
        const val EXTRA_CHAT_ID = "extra_open_chat_id"

        fun createMessageNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channelId = context.getString(R.string.notification_channel_messages_id)
                val channel = NotificationChannel(
                    channelId,
                    context.getString(R.string.notification_channel_messages_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_messages_desc)
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
