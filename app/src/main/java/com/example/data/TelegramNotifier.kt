package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Sends real-time admin alerts (new account registrations, abuse reports, moderation logs)
 * to the configured Telegram Bot with inline moderation buttons.
 *
 * Credentials are read strictly from BuildConfig (populated via AI Studio Secrets panel / .env).
 * If unconfigured or left at placeholder values, calls gracefully no-op without failing user flows.
 */
object TelegramNotifier {

    private const val TAG = "TelegramNotifier"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    fun isConfigured(
        token: String = BuildConfig.TELEGRAM_BOT_TOKEN,
        chatId: String = BuildConfig.TELEGRAM_ADMIN_CHAT_ID
    ): Boolean {
        val cleanToken = token.trim()
        val cleanChat = chatId.trim()
        return cleanToken.isNotEmpty() &&
            cleanChat.isNotEmpty() &&
            !cleanToken.startsWith("YOUR_") &&
            !cleanChat.startsWith("YOUR_")
    }

    fun escapeHtml(raw: String): String = raw
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    private fun escapeJsonString(raw: String): String = buildString(raw.length + 16) {
        for (ch in raw) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }

    fun formatNewUserMessage(
        uid: String,
        name: String,
        username: String,
        phone: String,
        email: String
    ): String {
        val safeName = escapeHtml(name.ifBlank { "Unknown" })
        val safeUsername = escapeHtml(username.trimStart('@'))
        val safePhone = escapeHtml(phone.ifBlank { "N/A" })
        val safeEmail = escapeHtml(email.ifBlank { "N/A" })
        val safeUid = escapeHtml(uid)
        return buildString {
            appendLine("🆕 <b>নতুন অ্যাকাউন্ট তৈরি হয়েছে (Bangla Chat)</b>")
            appendLine()
            appendLine("👤 <b>নাম:</b> $safeName")
            appendLine("🔖 <b>ইউজারনেম:</b> @$safeUsername")
            appendLine("📱 <b>ফোন নম্বর:</b> <code>$safePhone</code>")
            appendLine("📧 <b>ইমেইল:</b> <code>$safeEmail</code>")
            appendLine("🆔 <b>User ID:</b> <code>$safeUid</code>")
            appendLine()
            append("⚡ <i>নিচের বাটন অথবা <code>/ban $safeUid</code> / <code>/mute $safeUid 24</code> দিয়ে নিয়ন্ত্রণ করুন।</i>")
        }
    }

    fun formatReportMessage(
        reporterUid: String,
        reporterName: String,
        reportedUid: String,
        reportedName: String,
        reportedUsername: String,
        reportedPhone: String,
        reason: String,
        details: String
    ): String {
        val safeReporter = escapeHtml(reporterName.ifBlank { reporterUid })
        val safeReportedName = escapeHtml(reportedName.ifBlank { "Unknown User" })
        val safeReportedUser = escapeHtml(reportedUsername.trimStart('@'))
        val safePhone = escapeHtml(reportedPhone.ifBlank { "N/A" })
        val safeReason = escapeHtml(reason.ifBlank { "Other" })
        val safeDetails = escapeHtml(details.ifBlank { "—" })
        val safeReportedUid = escapeHtml(reportedUid)
        return buildString {
            appendLine("🚨 <b>নতুন রিপোর্ট এসেছে (Bangla Chat)</b>")
            appendLine()
            appendLine("⚠️ <b>অভিযুক্ত:</b> $safeReportedName (@$safeReportedUser)")
            appendLine("📱 <b>ফোন:</b> <code>$safePhone</code>")
            appendLine("🆔 <b>অভিযুক্ত UID:</b> <code>$safeReportedUid</code>")
            appendLine("📌 <b>কারণ:</b> $safeReason")
            appendLine("📝 <b>বিস্তারিত:</b> $safeDetails")
            appendLine("🙋 <b>রিপোর্ট করেছেন:</b> $safeReporter (<code>${escapeHtml(reporterUid)}</code>)")
            appendLine()
            append("🛡️ <i>নিচের বাটন থেকে ব্যান, মিউট বা ডিলিট করুন।</i>")
        }
    }

    fun formatAdminActionMessage(
        actionTitle: String,
        targetUid: String,
        targetName: String,
        targetUsername: String,
        reason: String
    ): String {
        val safeName = escapeHtml(targetName.ifBlank { targetUid })
        val safeUser = targetUsername.trimStart('@').takeIf { it.isNotBlank() }?.let { " (@${escapeHtml(it)})" }.orEmpty()
        val safeReason = escapeHtml(reason.ifBlank { "—" })
        return buildString {
            appendLine("🛡️ <b>Admin Moderation Log</b>")
            appendLine("⚡ <b>অ্যাকশন:</b> ${escapeHtml(actionTitle)}")
            appendLine("👤 <b>ইউজার:</b> $safeName$safeUser")
            appendLine("🆔 <b>UID:</b> <code>${escapeHtml(targetUid)}</code>")
            append("📝 <b>কারণ:</b> $safeReason")
        }
    }

    /**
     * Builds Telegram InlineKeyboardMarkup JSON with one-tap moderation callbacks.
     * Note: Callback data stays strictly <= 64 bytes per Telegram Bot API limits.
     */
    fun buildModerationReplyMarkupJson(uid: String): String {
        val safeUid = uid.take(48)
        return """{"inline_keyboard":[""" +
            """[{"text":"🔇 মিউট ১ ঘণ্টা","callback_data":"m1:${escapeJsonString(safeUid)}"},""" +
            """{"text":"🔇 মিউট ২৪ ঘণ্টা","callback_data":"m24:${escapeJsonString(safeUid)}"}],""" +
            """[{"text":"🔇 মিউট ৭ দিন","callback_data":"m7d:${escapeJsonString(safeUid)}"},""" +
            """{"text":"🚫 স্থায়ী ব্যান","callback_data":"ban:${escapeJsonString(safeUid)}"}],""" +
            """[{"text":"🗑️ অ্যাকাউন্ট রিমুভ","callback_data":"del:${escapeJsonString(safeUid)}"},""" +
            """{"text":"✅ আনব্যান/আনমিউট","callback_data":"unb:${escapeJsonString(safeUid)}"}]""" +
            """]}"""
    }

    fun buildSendMessagePayload(
        chatId: String,
        htmlText: String,
        replyMarkupJson: String? = null
    ): String {
        val escapedChat = escapeJsonString(chatId.trim())
        val escapedText = escapeJsonString(htmlText)
        return if (!replyMarkupJson.isNullOrBlank()) {
            """{"chat_id":"$escapedChat","text":"$escapedText","parse_mode":"HTML","reply_markup":$replyMarkupJson}"""
        } else {
            """{"chat_id":"$escapedChat","text":"$escapedText","parse_mode":"HTML"}"""
        }
    }

    suspend fun notifyNewUserRegistration(
        uid: String,
        name: String,
        username: String,
        phone: String,
        email: String
    ) {
        val html = formatNewUserMessage(uid, name, username, phone, email)
        val markup = buildModerationReplyMarkupJson(uid)
        sendHtmlMessage(html, markup)
    }

    suspend fun notifyUserReported(
        reporterUid: String,
        reporterName: String,
        reportedUid: String,
        reportedName: String,
        reportedUsername: String,
        reportedPhone: String,
        reason: String,
        details: String
    ) {
        val html = formatReportMessage(
            reporterUid = reporterUid,
            reporterName = reporterName,
            reportedUid = reportedUid,
            reportedName = reportedName,
            reportedUsername = reportedUsername,
            reportedPhone = reportedPhone,
            reason = reason,
            details = details
        )
        val markup = buildModerationReplyMarkupJson(reportedUid)
        sendHtmlMessage(html, markup)
    }

    suspend fun notifyModerationAction(
        actionTitle: String,
        targetUid: String,
        targetName: String,
        targetUsername: String,
        reason: String
    ) {
        val html = formatAdminActionMessage(actionTitle, targetUid, targetName, targetUsername, reason)
        sendHtmlMessage(html, replyMarkupJson = null)
    }

    private suspend fun sendHtmlMessage(htmlText: String, replyMarkupJson: String?) {
        val token = BuildConfig.TELEGRAM_BOT_TOKEN.trim()
        val chatId = BuildConfig.TELEGRAM_ADMIN_CHAT_ID.trim()
        if (!isConfigured(token, chatId)) {
            Log.d(TAG, "Telegram bot credentials not configured in AI Studio Secrets; skipping alert.")
            return
        }
        withContext(Dispatchers.IO) {
            runCatching {
                val url = "https://api.telegram.org/bot$token/sendMessage"
                val jsonBody = buildSendMessagePayload(chatId, htmlText, replyMarkupJson)
                val request = Request.Builder()
                    .url(url)
                    .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Telegram sendMessage returned HTTP ${response.code}")
                    }
                }
            }.onFailure { err ->
                Log.w(TAG, "Telegram alert failed (non-fatal): ${err.message}")
            }
        }
    }
}
