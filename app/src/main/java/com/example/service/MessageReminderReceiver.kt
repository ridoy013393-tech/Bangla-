package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar

/**
 * BroadcastReceiver that fires scheduled message reminders as high-priority notifications
 * with one-tap deep-linking back into the exact conversation.
 */
class MessageReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MESSAGE_REMINDER) return
        val chatId = intent.getStringExtra(EXTRA_REMINDER_CHAT_ID).orEmpty()
        val title = intent.getStringExtra(EXTRA_REMINDER_TITLE).orEmpty().ifBlank { "Bangla Chat" }
        val body = intent.getStringExtra(EXTRA_REMINDER_BODY).orEmpty()
        if (body.isBlank()) return

        NotificationHelper.showMessageNotification(
            context = context.applicationContext,
            chatId = chatId,
            senderName = "⏰ $title",
            body = body
        )
    }

    companion object {
        const val ACTION_MESSAGE_REMINDER = "com.example.action.MESSAGE_REMINDER"
        const val EXTRA_REMINDER_CHAT_ID = "extra_reminder_chat_id"
        const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
        const val EXTRA_REMINDER_BODY = "extra_reminder_body"
    }
}

object MessageReminderScheduler {
    private const val TAG = "MessageReminderScheduler"

    /**
     * Schedules a local reminder using `setAndAllowWhileIdle` (no SCHEDULE_EXACT_ALARM permission required).
     */
    fun scheduleReminder(
        context: Context,
        chatId: String,
        messageId: String,
        chatTitle: String,
        messagePreview: String,
        triggerAtMillis: Long
    ): Boolean {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val requestCode = ("rem_${chatId}_${messageId}_${triggerAtMillis}").hashCode()
        val intent = Intent(appContext, MessageReminderReceiver::class.java).apply {
            action = MessageReminderReceiver.ACTION_MESSAGE_REMINDER
            putExtra(MessageReminderReceiver.EXTRA_REMINDER_CHAT_ID, chatId)
            putExtra(MessageReminderReceiver.EXTRA_REMINDER_TITLE, chatTitle.take(60))
            putExtra(MessageReminderReceiver.EXTRA_REMINDER_BODY, messagePreview.take(220))
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return runCatching {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
            true
        }.onFailure { err ->
            Log.w(TAG, "Unable to schedule reminder: ${err.message}")
        }.getOrDefault(false)
    }

    fun tomorrowMorning9AmMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
