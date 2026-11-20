package com.example.data

import android.util.Log
import com.example.model.AppLanguage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class AiAssistantRepository {

    private val generalModel by lazy {
        Firebase.ai.generativeModel(modelName = MODEL_NAME)
    }

    private val quickReplyModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "replies" to Schema.array(
                    items = Schema.string(description = "A concise natural chat reply under 40 characters"),
                    description = "Exactly 3 short, contextually relevant chat reply suggestions"
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    suspend fun translateMessage(
        originalText: String,
        preferredUiLanguage: AppLanguage
    ): Result<String> {
        val trimmed = originalText.trim().take(2000)
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("Empty message"))

        val hasBanglaChars = trimmed.any { it in '\u0980'..'\u09FF' }
        val targetLang = when {
            hasBanglaChars -> "English"
            preferredUiLanguage == AppLanguage.BANGLA -> "natural Bengali (বাংলা)"
            else -> "natural Bengali (বাংলা)"
        }

        val prompt = """
            Translate the following chat message into $targetLang.
            Return ONLY the translated text without quotes, notes, or explanations.

            Message:
            $trimmed
        """.trimIndent()

        return try {
            val response = withTimeout(TIMEOUT_MS) {
                generalModel.generateContent(prompt)
            }
            val translated = response.text?.trim().orEmpty()
            if (translated.isNotBlank()) {
                Result.success(translated)
            } else {
                Result.failure(IllegalStateException("AI returned an empty translation."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Translation timed out", e)
            Result.failure(IllegalStateException("অনুবাদ করতে সময় বেশি লাগছে, আবার চেষ্টা করুন।"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Translation failed", e)
            Result.failure(e)
        }
    }

    suspend fun suggestQuickReplies(
        recentMessages: List<Pair<String, String>>,
        language: AppLanguage
    ): Result<List<String>> {
        if (recentMessages.isEmpty()) {
            return Result.failure(IllegalStateException("No conversation context yet"))
        }
        val transcript = recentMessages.takeLast(8).joinToString("\n") { (sender, text) ->
            "$sender: ${text.take(240)}"
        }
        val langInstruction = if (language == AppLanguage.BANGLA) {
            "Reply in natural, friendly Bengali (বাংলা)."
        } else {
            "Reply in natural, friendly English."
        }
        val prompt = """
            Based on this recent chat conversation, suggest 3 short, helpful replies the user can tap to send next.
            $langInstruction
            Keep each reply brief (2 to 7 words).

            Conversation:
            $transcript
        """.trimIndent()

        return try {
            val response = withTimeout(TIMEOUT_MS) {
                quickReplyModel.generateContent(prompt)
            }
            val rawJson = response.text?.trim().orEmpty()
            if (rawJson.isBlank()) {
                return Result.failure(IllegalStateException("AI returned empty suggestions."))
            }
            val json = JSONObject(rawJson)
            val arr = json.getJSONArray("replies")
            val suggestions = buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.optString(i).trim()
                    if (item.isNotBlank()) add(item)
                }
            }.take(3)
            if (suggestions.isEmpty()) {
                Result.failure(IllegalStateException("No reply suggestions returned."))
            } else {
                Result.success(suggestions)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Quick replies timed out", e)
            Result.failure(IllegalStateException("সাজেশন লোড হতে সময় বেশি লাগছে।"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Quick reply generation failed", e)
            Result.failure(e)
        }
    }

    fun summarizeConversationStream(
        recentMessages: List<Pair<String, String>>,
        language: AppLanguage
    ): Flow<String> = flow {
        val transcript = recentMessages.takeLast(30).joinToString("\n") { (sender, text) ->
            "$sender: ${text.take(300)}"
        }
        val langInstruction = if (language == AppLanguage.BANGLA) {
            "Write a clean, concise bullet-point summary in Bengali (বাংলা) highlighting key topics, decisions, and action items."
        } else {
            "Write a clean, concise bullet-point summary in English highlighting key topics, decisions, and action items."
        }
        val prompt = """
            Summarize the following chat conversation.
            $langInstruction

            Conversation:
            $transcript
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(TIMEOUT_MS) {
            generalModel.generateContentStream(prompt).collect { chunk ->
                val part = chunk.text.orEmpty()
                if (part.isNotEmpty()) {
                    accumulated.append(part)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("AI returned an empty summary.")
        }
    }

    private companion object {
        const val TAG = "AiAssistantRepo"
        const val MODEL_NAME = "gemini-3.8-flash"
        const val TIMEOUT_MS = 45_000L
    }
}
