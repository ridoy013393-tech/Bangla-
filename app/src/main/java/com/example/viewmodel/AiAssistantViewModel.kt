package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiAssistantRepository
import com.example.model.AiAssistantMessage
import com.example.model.AppLanguage
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the dedicated DeepSeek AI Assistant screen.
 * Keeps conversational memory per session using a deterministic `sessionUid` passed to
 * `https://xrahat-deepseek.vercel.app/api/chat?uid=...&text=...`.
 */
class AiAssistantViewModel(
    private val currentUserId: String,
    private val aiRepository: AiAssistantRepository = AiAssistantRepository()
) : ViewModel() {

    private var sessionNonce: Int = 1

    private val _messages = MutableStateFlow<List<AiAssistantMessage>>(emptyList())
    val messages: StateFlow<List<AiAssistantMessage>> = _messages.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private var lastUserPrompt: String? = null

    private fun activeSessionUid(): String {
        val cleanUser = currentUserId.replace(Regex("[^a-zA-Z0-9_-]"), "").take(18).ifBlank { "guest" }
        return "banglachat_ai_${cleanUser}_s$sessionNonce"
    }

    fun sendPrompt(rawPrompt: String, language: AppLanguage) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty() || _isThinking.value) return
        lastUserPrompt = prompt

        val userMsg = AiAssistantMessage(
            id = "u_${UUID.randomUUID()}",
            text = prompt,
            isUser = true
        )
        _messages.value = _messages.value + userMsg
        requestReply(prompt, language)
    }

    fun retryLast(language: AppLanguage) {
        val prompt = lastUserPrompt ?: return
        if (_isThinking.value) return
        // Drop trailing error bubble before retrying
        val current = _messages.value
        if (current.lastOrNull()?.isError == true) {
            _messages.value = current.dropLast(1)
        }
        requestReply(prompt, language)
    }

    fun startNewConversation() {
        if (_isThinking.value) return
        sessionNonce += 1
        lastUserPrompt = null
        _messages.value = emptyList()
    }

    private fun requestReply(prompt: String, language: AppLanguage) {
        viewModelScope.launch {
            _isThinking.value = true
            aiRepository.askAiAssistant(
                sessionUid = activeSessionUid(),
                userPrompt = prompt,
                language = language
            ).onSuccess { reply ->
                _messages.value = _messages.value + AiAssistantMessage(
                    id = "a_${UUID.randomUUID()}",
                    text = reply,
                    isUser = false,
                    isError = false
                )
            }.onFailure { err ->
                _messages.value = _messages.value + AiAssistantMessage(
                    id = "e_${UUID.randomUUID()}",
                    text = err.localizedMessage ?: "DeepSeek AI উত্তর দিতে পারেনি।",
                    isUser = false,
                    isError = true
                )
            }
            _isThinking.value = false
        }
    }
}
