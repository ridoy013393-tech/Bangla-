package com.example.data

import com.example.model.ModerationSettings
import java.util.Locale

enum class ModerationViolationType {
    PROFANITY,
    BURST_SPAM,
    DUPLICATE_SPAM
}

sealed interface MessageModerationOutcome {
    data class Allow(
        val finalText: String,
        val wasMasked: Boolean = false,
        val matchedWords: List<String> = emptyList(),
        val shouldAlertAdmin: Boolean = false,
        val violationType: ModerationViolationType? = null
    ) : MessageModerationOutcome

    data class Block(
        val violationType: ModerationViolationType,
        val matchedWords: List<String> = emptyList(),
        val shouldAlertAdmin: Boolean = false,
        val cooldownSeconds: Int = 0
    ) : MessageModerationOutcome
}

/**
 * Pure Kotlin auto-moderation engine for Bangla Chat:
 * - Detects Bangla, Banglish, and English profanity/abusive keywords + admin custom blocked words.
 * - Supports both strict "block" mode and censoring "mask" (***) mode.
 * - Detects rapid burst flooding and repeated duplicate spam using a per-user sliding window.
 * - Throttles Telegram admin alerts so moderators get actionable alerts without flooding.
 */
object ContentModerationEngine {

    // Core multilingual abusive terms (Bangla script, common Banglish transliterations, and English).
    // Kept focused on explicit slurs/harassment to minimize false positives in everyday Bangla chat.
    private val DEFAULT_LATIN_BLOCKED_WORDS = setOf(
        "fuck", "fucking", "fucker", "motherfucker", "shit", "bullshit", "bitch", "bastard",
        "asshole", "dickhead", "slut", "whore", "cunt", "faggot", "rape", "rapist",
        "madarchod", "madarcod", "bhenchod", "behenchod", "choda", "chudi", "chudir",
        "khanki", "khankir", "magir", "haramjada", "haramzada", "kuttarbaccha", "baler",
        "bokachoda", "chudna", "chudbo", "gandu", "randi", "suorerbaccha", "shalarput"
    )

    private val DEFAULT_UNICODE_BLOCKED_WORDS = setOf(
        "মাদারচোদ", "মাদারচুদ", "বাইনচোদ", "ভোদাই", "খানকি", "খানকির", "মাগী", "মাগীর",
        "হারামজাদা", "হারামজাদী", "কুত্তারবাচ্চা", "শুয়োরেরবাচ্চা", "শূয়োরেরবাচ্চা",
        "বোকাচোদা", "চোদা", "চুদির", "চুদনা", "রান্ডি", "রান্ডির", "গাধাচোদা", "বালের"
    )

    private const val BURST_WINDOW_MS = 8_000L
    private const val BURST_MAX_MESSAGES = 6
    private const val DUPLICATE_WINDOW_MS = 20_000L
    private const val DUPLICATE_MAX_REPEATS = 3
    private const val ALERT_COOLDOWN_MS = 45_000L

    private data class UserActivityState(
        val recentTimestamps: ArrayDeque<Long> = ArrayDeque(),
        val recentNormalizedTexts: ArrayDeque<Pair<Long, String>> = ArrayDeque(),
        var profanityStrikes: Int = 0,
        var spamStrikes: Int = 0,
        var lastAdminAlertMs: Long = 0L
    )

    private val activityLock = Any()
    private val userActivity = mutableMapOf<String, UserActivityState>()

    fun resetTrackingForTests() {
        synchronized(activityLock) {
            userActivity.clear()
        }
    }

    /**
     * Normalizes elongated repeated characters (e.g., "fuuuuck" -> "fuck") for Latin profanity checks.
     */
    private fun collapseRepeatedChars(token: String): String {
        if (token.length <= 2) return token
        val sb = StringBuilder(token.length)
        var prev = '\u0000'
        var count = 0
        for (ch in token) {
            if (ch == prev) {
                count++
                if (count <= 1) sb.append(ch)
            } else {
                prev = ch
                count = 1
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun normalizeLatinToken(raw: String): String {
        val lowered = raw.lowercase(Locale.US)
            .replace('0', 'o')
            .replace('1', 'i')
            .replace('3', 'e')
            .replace('4', 'a')
            .replace('5', 's')
            .replace('@', 'a')
            .replace('$', 's')
        return lowered.filter { it in 'a'..'z' }
    }

    private fun isAsciiLatinWord(word: String): Boolean =
        word.isNotEmpty() && word.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '_' }

    data class ProfanityScanResult(
        val matchedWords: List<String>,
        val maskedText: String
    ) {
        val hasProfanity: Boolean get() = matchedWords.isNotEmpty()
    }

    fun scanProfanity(
        text: String,
        customBlockedWords: List<String> = emptyList()
    ): ProfanityScanResult {
        if (text.isBlank()) return ProfanityScanResult(emptyList(), text)

        val latinBlockSet = HashSet<String>(DEFAULT_LATIN_BLOCKED_WORDS.size + customBlockedWords.size)
        latinBlockSet.addAll(DEFAULT_LATIN_BLOCKED_WORDS)

        val unicodeBlockSet = HashSet<String>(DEFAULT_UNICODE_BLOCKED_WORDS.size + customBlockedWords.size)
        unicodeBlockSet.addAll(DEFAULT_UNICODE_BLOCKED_WORDS)

        for (rawCustom in customBlockedWords) {
            val cleaned = rawCustom.trim().lowercase(Locale.ROOT)
            if (cleaned.length < 2) continue
            if (isAsciiLatinWord(cleaned)) {
                val norm = normalizeLatinToken(cleaned)
                if (norm.length >= 2) {
                    latinBlockSet.add(norm)
                    latinBlockSet.add(collapseRepeatedChars(norm))
                }
            } else {
                unicodeBlockSet.add(cleaned)
            }
        }

        val matched = linkedSetOf<String>()
        val tokenRegex = Regex("\\S+")
        val maskedBuilder = StringBuilder(text.length)
        var lastIndex = 0

        for (match in tokenRegex.findAll(text)) {
            maskedBuilder.append(text, lastIndex, match.range.first)
            val token = match.value
            val strippedPunct = token.trim { ch ->
                ch in ".,!?;:\"'()[]{}<>~`^*+-=\\/|—–…"
            }

            var isBadToken = false
            if (strippedPunct.isNotEmpty()) {
                val latinNorm = normalizeLatinToken(strippedPunct)
                val latinCollapsed = collapseRepeatedChars(latinNorm)
                if (latinNorm.length >= 3 && (latinNorm in latinBlockSet || latinCollapsed in latinBlockSet)) {
                    isBadToken = true
                    matched.add(strippedPunct)
                } else {
                    val lowerUnicode = strippedPunct.lowercase(Locale.ROOT)
                    if (lowerUnicode in unicodeBlockSet ||
                        unicodeBlockSet.any { bad -> bad.length >= 3 && lowerUnicode.contains(bad) }
                    ) {
                        isBadToken = true
                        matched.add(strippedPunct)
                    }
                }
            }

            if (isBadToken) {
                maskedBuilder.append(maskTokenPreservingPunctuation(token))
            } else {
                maskedBuilder.append(token)
            }
            lastIndex = match.range.last + 1
        }
        maskedBuilder.append(text, lastIndex, text.length)

        // Also check multi-word custom phrases (e.g. "free money scam")
        var finalMasked = maskedBuilder.toString()
        for (rawCustom in customBlockedWords) {
            val phrase = rawCustom.trim()
            if (phrase.contains(' ') && phrase.length >= 4) {
                val phraseRegex = Regex(Regex.escape(phrase), RegexOption.IGNORE_CASE)
                if (phraseRegex.containsMatchIn(finalMasked)) {
                    matched.add(phrase)
                    finalMasked = phraseRegex.replace(finalMasked, "***")
                }
            }
        }

        return ProfanityScanResult(
            matchedWords = matched.toList(),
            maskedText = finalMasked
        )
    }

    private fun maskTokenPreservingPunctuation(token: String): String {
        val coreLen = token.count { it.isLetterOrDigit() }
        if (coreLen <= 1) return "***"
        return "***"
    }

    /**
     * Evaluates an outgoing message against the active ModerationSettings.
     */
    fun evaluateOutgoingMessage(
        senderUid: String,
        rawText: String,
        settings: ModerationSettings,
        nowMillis: Long = System.currentTimeMillis()
    ): MessageModerationOutcome {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return MessageModerationOutcome.Allow(rawText)

        // 1. Profanity check
        if (settings.profanityFilterEnabled) {
            val scan = scanProfanity(trimmed, settings.customBlockedWords)
            if (scan.hasProfanity) {
                val shouldAlert = recordProfanityStrikeAndCheckAlert(
                    uid = senderUid,
                    alertsEnabled = settings.telegramAlertsEnabled,
                    nowMillis = nowMillis
                )
                return if (settings.isMaskMode()) {
                    MessageModerationOutcome.Allow(
                        finalText = scan.maskedText,
                        wasMasked = true,
                        matchedWords = scan.matchedWords,
                        shouldAlertAdmin = shouldAlert,
                        violationType = ModerationViolationType.PROFANITY
                    )
                } else {
                    MessageModerationOutcome.Block(
                        violationType = ModerationViolationType.PROFANITY,
                        matchedWords = scan.matchedWords,
                        shouldAlertAdmin = shouldAlert
                    )
                }
            }
        }

        // 2. Anti-Spam check (burst flooding + repeated identical messages)
        if (settings.spamProtectionEnabled && senderUid.isNotBlank()) {
            val spamViolation = recordAndDetectSpam(
                uid = senderUid,
                normalizedText = trimmed.lowercase(Locale.ROOT),
                alertsEnabled = settings.telegramAlertsEnabled,
                nowMillis = nowMillis
            )
            if (spamViolation != null) {
                return spamViolation
            }
        }

        return MessageModerationOutcome.Allow(rawText)
    }

    private fun recordProfanityStrikeAndCheckAlert(
        uid: String,
        alertsEnabled: Boolean,
        nowMillis: Long
    ): Boolean {
        if (!alertsEnabled || uid.isBlank()) return false
        synchronized(activityLock) {
            val state = userActivity.getOrPut(uid) { UserActivityState() }
            state.profanityStrikes++
            val cooldownElapsed = (nowMillis - state.lastAdminAlertMs) >= ALERT_COOLDOWN_MS
            // Alert admin immediately on 1st offense or after cooldown so admin stays informed without spam
            if (state.profanityStrikes == 1 || cooldownElapsed) {
                state.lastAdminAlertMs = nowMillis
                return true
            }
            return false
        }
    }

    private fun recordAndDetectSpam(
        uid: String,
        normalizedText: String,
        alertsEnabled: Boolean,
        nowMillis: Long
    ): MessageModerationOutcome.Block? {
        synchronized(activityLock) {
            val state = userActivity.getOrPut(uid) { UserActivityState() }

            // Evict expired timestamps from burst window
            while (state.recentTimestamps.isNotEmpty() &&
                nowMillis - state.recentTimestamps.first() > BURST_WINDOW_MS
            ) {
                state.recentTimestamps.removeFirst()
            }

            // Evict expired messages from duplicate window
            while (state.recentNormalizedTexts.isNotEmpty() &&
                nowMillis - state.recentNormalizedTexts.first().first > DUPLICATE_WINDOW_MS
            ) {
                state.recentNormalizedTexts.removeFirst()
            }

            // Check burst flood first
            if (state.recentTimestamps.size >= BURST_MAX_MESSAGES) {
                state.spamStrikes++
                val shouldAlert = alertsEnabled &&
                    (state.spamStrikes >= 2) &&
                    ((nowMillis - state.lastAdminAlertMs) >= ALERT_COOLDOWN_MS)
                if (shouldAlert) {
                    state.lastAdminAlertMs = nowMillis
                }
                val oldest = state.recentTimestamps.first()
                val remainingSec = (((oldest + BURST_WINDOW_MS) - nowMillis) / 1000L)
                    .toInt()
                    .coerceIn(2, 10)
                return MessageModerationOutcome.Block(
                    violationType = ModerationViolationType.BURST_SPAM,
                    shouldAlertAdmin = shouldAlert,
                    cooldownSeconds = remainingSec
                )
            }

            // Check duplicate message flooding (only for messages >= 3 chars so short "ok"/"হ্যাঁ" aren't over-flagged unless spammed)
            if (normalizedText.length >= 3) {
                val duplicateCount = state.recentNormalizedTexts.count { it.second == normalizedText }
                if (duplicateCount >= DUPLICATE_MAX_REPEATS) {
                    state.spamStrikes++
                    val shouldAlert = alertsEnabled &&
                        ((nowMillis - state.lastAdminAlertMs) >= ALERT_COOLDOWN_MS)
                    if (shouldAlert) {
                        state.lastAdminAlertMs = nowMillis
                    }
                    return MessageModerationOutcome.Block(
                        violationType = ModerationViolationType.DUPLICATE_SPAM,
                        shouldAlertAdmin = shouldAlert,
                        cooldownSeconds = 10
                    )
                }
            }

            // Message allowed — record activity
            state.recentTimestamps.addLast(nowMillis)
            state.recentNormalizedTexts.addLast(nowMillis to normalizedText)
            return null
        }
    }
}
