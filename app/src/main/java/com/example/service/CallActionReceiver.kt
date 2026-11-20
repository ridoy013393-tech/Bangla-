package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DECLINE_CALL) return
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

    companion object {
        const val ACTION_DECLINE_CALL = "com.example.action.DECLINE_INCOMING_CALL"
        const val EXTRA_CALL_ID = "extra_call_id"
    }
}
