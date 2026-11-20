package com.example.data

import android.util.Log
import com.example.model.AiRewriteTone
import com.example.model.AppLanguage
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * AI Assistant powered by DeepSeek Free API (https://xrahat-deepseek.vercel.app/api/chat).
 *
 * Supports conversational memory via `uid` for the dedicated DeepSeek AI chat assistant,
 * while isolating stateless utility calls (translate, smart reply, summary, rewrite, explain)
 * with fresh ephemeral `uid`s so unrelated prompts never pollute each other.
 */
class AiAssistantRepository(
    private val httpClient: OkHttpClient = defaultHttpClient
) {

    /**
     * Direct conversational chat with DeepSeek AI using persistent per-session memory (`sessionUid`).
     */
    suspend fun askAiAssistant(
        sessionUid: String,
        userPrompt: String,
        language: AppLanguage
    ): Result<String> {
        val trimmed = userPrompt.trim().take(MAX_DIRECT_PROMPT_CHARS)
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("প্রশ্ন খালি রাখা যাবে না।"))
        }
        val cleanUid = sessionUid.trim().ifBlank { newEphemeralUid("chat") }
        return callDeepSeekApi(
            uid = cleanUid,
            prompt = trimmed,
            language = language
        )
    }

    /**
     * One-tap Bangla <-> English message translation.
     */
    suspend fun translateMessage(
        originalText: String,
        preferredUiLanguage: AppLanguage
    ): Result<String> {
        val trimmed = originalText.trim().take(1200)
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Empty message"))
        }

        val hasBanglaChars = trimmed.any { it in '\u0980'..'\u09FF' }
        val targetLang = when {
            hasBanglaChars -> "natural English"
            preferredUiLanguage == AppLanguage.BANGLA -> "natural Bengali (বাংলা)"
            else -> "natural Bengali (বাংলা)"
        }

        val prompt = buildString {
            append("Translate the following chat message into ")
            append(targetLang)
            append(". Return ONLY the translated text without quotes, notes, or explanations.\n\n")
            append(trimmed)
        }

        return callDeepSeekApi(
            uid = newEphemeralUid("tr"),
            prompt = prompt,
            language = preferredUiLanguage
        ).map { cleanQuotedOutput(it) }
    }

    /**
     * Suggests 3 short, context-aware tap-to-send replies for the active chat.
     */
    suspend fun suggestQuickReplies(
        recentMessages: List<Pair<String, String>>,
        language: AppLanguage
    ): Result<List<String>> {
        if (recentMessages.isEmpty()) {
            val msg = if (language == AppLanguage.BANGLA) {
                "স্মার্ট রিপ্লাইয়ের জন্য চ্যাটে কমপক্ষে ১টি মেসেজ থাকতে হবে।"
            } else {
                "No conversation context yet."
            }
            return Result.failure(IllegalStateException(msg))
        }

        val transcript = recentMessages.takeLast(6).joinToString("\n") { (sender, text) ->
            "${sender.take(24)}: ${text.replace('\n', ' ').take(150)}"
        }
        val langRule = if (language == AppLanguage.BANGLA) {
            "Write all 3 replies in natural, friendly Bengali (বাংলা)."
        } else {
            "Write all 3 replies in natural, friendly English."
        }
        val prompt = """
            Suggest exactly 3 short chat replies (2 to 6 words each) that "Me" can send next.
            $langRule
            Return ONLY a JSON array of 3 strings like ["reply1","reply2","reply3"] with no extra text.

            Chat:
            $transcript
        """.trimIndent()

        return callDeepSeekApi(
            uid = newEphemeralUid("qr"),
            prompt = prompt,
            language = language
        ).mapCatching { reply ->
            val parsed = parseQuickReplies(reply)
            if (parsed.isEmpty()) {
                val msg = if (language == AppLanguage.BANGLA) {
                    "সাজেশন তৈরি করা যায়নি, আবার চেষ্টা করুন।"
                } else {
                    "No reply suggestions returned."
                }
                throw IllegalStateException(msg)
            }
            parsed
        }
    }

    /**
     * Summarizes recent chat messages into clean bullet points.
     */
    fun summarizeConversationStream(
        recentMessages: List<Pair<String, String>>,
        language: AppLanguage
    ): Flow<String> = flow {
        val transcript = recentMessages.takeLast(16).joinToString("\n") { (sender, text) ->
            "${sender.take(20)}: ${text.replace('\n', ' ').take(110)}"
        }.take(1350)

        val langInstruction = if (language == AppLanguage.BANGLA) {
            "Write a short, clear bullet-point summary in Bengali (বাংলা) highlighting key topics and action items."
        } else {
            "Write a short, clear bullet-point summary in English highlighting key topics and action items."
        }
        val prompt = """
            Summarize this chat conversation concisely.
            $langInstruction

            Conversation:
            $transcript
        """.trimIndent()

        val result = callDeepSeekApi(
            uid = newEphemeralUid("sum"),
            prompt = prompt,
            language = language
        )
        val text = result.getOrThrow()
        emit(text)
    }

    /**
     * Rewrites or polishes a draft message in the requested tone or language.
     */
    suspend fun rewriteMessage(
        draftText: String,
        tone: AiRewriteTone,
        language: AppLanguage
    ): Result<String> {
        val trimmed = draftText.trim().take(1000)
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("মেসেজ খালি।"))
        }

        val toneInstruction = when (tone) {
            AiRewriteTone.POLITE ->
                "Rewrite this chat message to sound warm, polite, and respectful in the same language (Bengali if Bengali, English if English)."
            AiRewriteTone.CASUAL ->
                "Rewrite this chat message to sound friendly, natural, and casual in the same language."
            AiRewriteTone.PROFESSIONAL ->
                "Rewrite this chat message to sound clear, formal, and professional in the same language."
            AiRewriteTone.BANGLA_POLISH ->
                "Fix spelling, grammar, and phrasing to make this clean, natural Bengali (শুদ্ধ বাংলা). If written in Banglish/English, convert it to natural Bengali."
            AiRewriteTone.ENGLISH ->
                "Rewrite and translate this chat message into clear, natural English."
        }

        val prompt = """
            $toneInstruction
            Return ONLY the rewritten message text without quotes or commentary.

            Message:
            $trimmed
        """.trimIndent()

        return callDeepSeekApi(
            uid = newEphemeralUid("rw"),
            prompt = prompt,
            language = language
        ).map { cleanQuotedOutput(it) }
    }

    /**
     * Generates a ready-to-send chat message from a brief topic/idea.
     */
    suspend fun draftMessageFromIdea(
        idea: String,
        language: AppLanguage
    ): Result<String> {
        val trimmed = idea.trim().take(600)
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("বিষয় লিখুন।"))
        }
        val langInstruction = if (language == AppLanguage.BANGLA) {
            "Write the message in natural, warm Bengali (বাংলা) unless the topic asks for English."
        } else {
            "Write the message in natural, clear English unless the topic asks for Bengali."
        }
        val prompt = """
            Write a ready-to-send chat message for the following request.
            $langInstruction
            Keep it concise and natural for a messaging app. Return ONLY the message text with no quotes or preamble.

            Request: $trimmed
        """.trimIndent()

        return callDeepSeekApi(
            uid = newEphemeralUid("dr"),
            prompt = prompt,
            language = language
        ).map { cleanQuotedOutput(it) }
    }

    /**
     * Explains a complex, English, or technical chat message in simple terms.
     */
    suspend fun explainMessage(
        messageText: String,
        language: AppLanguage
    ): Result<String> {
        val trimmed = messageText.trim().take(1200)
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Empty message"))
        }
        val langInstruction = if (language == AppLanguage.BANGLA) {
            "Explain the meaning of this message clearly in simple Bengali (সহজ বাংলায়) in 2 to 4 short sentences."
        } else {
            "Explain the meaning of this message clearly in simple English in 2 to 4 short sentences."
        }
        val prompt = """
            $langInstruction

            Message:
            $trimmed
        """.trimIndent()

        return callDeepSeekApi(
            uid = newEphemeralUid("ex"),
            prompt = prompt,
            language = language
        )
    }

    private suspend fun callDeepSeekApi(
        uid: String,
        prompt: String,
        language: AppLanguage
    ): Result<String> = withContext(Dispatchers.IO) {
        val safePrompt = truncateForGetUrl(prompt, MAX_GET_PROMPT_CHARS)
        val url = DEEPSEEK_CHAT_ENDPOINT.toHttpUrl()
            .newBuilder()
            .addQueryParameter("uid", uid)
            .addQueryParameter("text", safePrompt)
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .header("User-Agent", "BanglaChat-Android/1.2")
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.w(TAG, "DeepSeek HTTP ${response.code}: ${bodyString.take(200)}")
                    val errMsg = if (language == AppLanguage.BANGLA) {
                        "DeepSeek সার্ভার ব্যস্ত আছে (HTTP ${response.code}), একটু পর আবার চেষ্টা করুন।"
                    } else {
                        "DeepSeek server error (HTTP ${response.code}). Please retry."
                    }
                    return@withContext Result.failure(IllegalStateException(errMsg))
                }
                parseDeepSeekResponse(bodyString, language)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "DeepSeek network error", e)
            val errMsg = if (language == AppLanguage.BANGLA) {
                "ইন্টারনেট সংযোগ বা সার্ভার সমস্যা হয়েছে। আবার চেষ্টা করুন।"
            } else {
                "Network error reaching DeepSeek AI. Please check your connection."
            }
            Result.failure(IllegalStateException(errMsg, e))
        } catch (e: Exception) {
            Log.e(TAG, "DeepSeek unexpected error", e)
            val errMsg = if (language == AppLanguage.BANGLA) {
                "AI উত্তর আনতে সমস্যা হয়েছে: ${e.localizedMessage ?: "অজানা ত্রুটি"}"
            } else {
                e.localizedMessage ?: "DeepSeek AI request failed"
            }
            Result.failure(IllegalStateException(errMsg, e))
        }
    }

    companion object {
        private const val TAG = "AiAssistantRepo"
        const val DEEPSEEK_CHAT_ENDPOINT = "https://xrahat-deepseek.vercel.app/api/chat"
        const val MAX_DIRECT_PROMPT_CHARS = 1600
        const val MAX_GET_PROMPT_CHARS = 1800

        private val THINK_BLOCK_REGEX = Regex("(?is)<think>.*?</think>")
        private val CODE_FENCE_REGEX = Regex("(?s)^```(?:json)?\\s*|\\s*```$")
        private val LEADING_LIST_MARKER_REGEX = Regex("^\\s*(?:\\d+[.)]|[-*•▪︎])\\s*")

        private val defaultHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(25, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .writeTimeout(25, TimeUnit.SECONDS)
                .callTimeout(55, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }

        fun newEphemeralUid(prefix: String): String =
            "bc_${prefix}_${UUID.randomUUID().toString().replace("-", "").take(10)}"

        fun truncateForGetUrl(prompt: String, maxChars: Int = MAX_GET_PROMPT_CHARS): String {
            val normalized = prompt.trim()
            return if (normalized.length <= maxChars) normalized else normalized.take(maxChars)
        }

        fun stripThinkBlocks(raw: String): String {
            return raw.replace(THINK_BLOCK_REGEX, "").trim()
        }

        fun cleanQuotedOutput(raw: String): String {
            val cleaned = stripThinkBlocks(raw)
            if (cleaned.length >= 2 &&
                ((cleaned.startsWith("\"") && cleaned.endsWith("\"")) ||
                    (cleaned.startsWith("'") && cleaned.endsWith("'")))
            ) {
                return cleaned.substring(1, cleaned.length - 1).trim()
            }
            return cleaned
        }

        fun parseDeepSeekResponse(
            rawBody: String,
            language: AppLanguage = AppLanguage.BANGLA
        ): Result<String> {
            val trimmedBody = rawBody.trim()
            if (trimmedBody.isEmpty()) {
                val msg = if (language == AppLanguage.BANGLA) {
                    "DeepSeek থেকে কোনো উত্তর পাওয়া যায়নি।"
                } else {
                    "Empty response from DeepSeek AI."
                }
                return Result.failure(IllegalStateException(msg))
            }

            return try {
                val json = JSONObject(trimmedBody)
                val success = json.optBoolean("success", true)
                val replyRaw = json.optString("reply").ifBlank {
                    json.optString("response").ifBlank { json.optString("message") }
                }
                val cleaned = stripThinkBlocks(replyRaw)
                if (!success && cleaned.isBlank()) {
                    val err = json.optString("error").ifBlank {
                        if (language == AppLanguage.BANGLA) "DeepSeek AI এখন উত্তর দিতে পারেনি।"
                        else "DeepSeek AI could not generate a response."
                    }
                    Result.failure(IllegalStateException(err))
                } else if (cleaned.isNotBlank()) {
                    Result.success(cleaned)
                } else {
                    val msg = if (language == AppLanguage.BANGLA) {
                        "DeepSeek থেকে খালি উত্তর এসেছে, আবার চেষ্টা করুন।"
                    } else {
                        "DeepSeek returned an empty reply."
                    }
                    Result.failure(IllegalStateException(msg))
                }
            } catch (e: Exception) {
                // Fallback if the endpoint ever returns plain text
                val plain = stripThinkBlocks(trimmedBody)
                val looksLikeHtmlOrJson = plain.startsWith("<html", ignoreCase = true) ||
                    plain.startsWith("<!DOCTYPE", ignoreCase = true) ||
                    plain.startsWith("{") ||
                    plain.startsWith("[")
                if (plain.isNotBlank() && !looksLikeHtmlOrJson) {
                    Result.success(plain)
                } else {
                    val msg = if (language == AppLanguage.BANGLA) {
                        "AI সার্ভারের উত্তর পড়া যায়নি, আবার চেষ্টা করুন।"
                    } else {
                        "Unable to parse DeepSeek AI response."
                    }
                    Result.failure(IllegalStateException(msg, e))
                }
            }
        }

        fun parseQuickReplies(rawReply: String): List<String> {
            val stripped = stripThinkBlocks(rawReply)
                .replace(CODE_FENCE_REGEX, "")
                .trim()
            if (stripped.isEmpty()) return emptyList()

            // 1. Try JSON array directly: ["reply1", "reply2", "reply3"]
            val arrStart = stripped.indexOf('[')
            val arrEnd = stripped.lastIndexOf(']')
            if (arrStart >= 0 && arrEnd > arrStart) {
                val candidate = stripped.substring(arrStart, arrEnd + 1)
                try {
                    val arr = JSONArray(candidate)
                    val parsed = buildList {
                        for (i in 0 until arr.length()) {
                            val item = cleanQuotedOutput(arr.optString(i)).take(80)
                            if (item.isNotBlank()) add(item)
                        }
                    }.distinct().take(3)
                    if (parsed.isNotEmpty()) return parsed
                } catch (_: Exception) {
                    // Fall through to object/line parsing
                }
            }

            // 2. Try JSON object {"replies": [...]}
            val objStart = stripped.indexOf('{')
            val objEnd = stripped.lastIndexOf('}')
            if (objStart >= 0 && objEnd > objStart) {
                val candidate = stripped.substring(objStart, objEnd + 1)
                try {
                    val obj = JSONObject(candidate)
                    val arr = obj.optJSONArray("replies") ?: obj.optJSONArray("suggestions")
                    if (arr != null) {
                        val parsed = buildList {
                            for (i in 0 until arr.length()) {
                                val item = cleanQuotedOutput(arr.optString(i)).take(80)
                                if (item.isNotBlank()) add(item)
                            }
                        }.distinct().take(3)
                        if (parsed.isNotEmpty()) return parsed
                    }
                } catch (_: Exception) {
                    // Fall through to line parsing
                }
            }

            // 3. Fallback: numbered or bulleted lines
            return stripped.lines()
                .map { line ->
                    cleanQuotedOutput(line.replace(LEADING_LIST_MARKER_REGEX, "")).trim()
                }
                .filter { it.length in 2..80 && !it.endsWith(":") }
                .distinct()
                .take(3)
        }
    }
}
