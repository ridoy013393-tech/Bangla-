package com.example

import com.example.data.AiAssistantRepository
import com.example.data.BanglaChatRepository
import com.example.data.normalizePhoneNumber
import com.example.model.AppLanguage
import com.example.ui.i18n.BanglaStrings
import com.example.ui.i18n.EnglishStrings
import com.example.ui.i18n.stringsForLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun usernameNormalizationAndValidation() {
        assertEquals("ridoy1234", BanglaChatRepository.normalizeUsername("  @Ridoy1234 "))
        assertTrue(BanglaChatRepository.isValidUsernameFormat("@ridoy1234"))
        assertTrue(BanglaChatRepository.isValidUsernameFormat("bangla_user_01"))
        assertFalse(BanglaChatRepository.isValidUsernameFormat("ab"))
        assertFalse(BanglaChatRepository.isValidUsernameFormat("bad name!"))
    }

    @Test
    fun bangladeshPhoneNormalization() {
        assertEquals("+8801712345678", normalizePhoneNumber("01712345678"))
        assertEquals("+8801712345678", normalizePhoneNumber("8801712345678"))
        assertEquals("+8801712345678", normalizePhoneNumber("+880 1712-345678"))
        assertEquals("+14155552671", normalizePhoneNumber("+1 (415) 555-2671"))
    }

    @Test
    fun localizationTablesInitializeCleanly() {
        assertSame(BanglaStrings, stringsForLanguage(AppLanguage.BANGLA))
        assertSame(EnglishStrings, stringsForLanguage(AppLanguage.ENGLISH))
        assertEquals("ফোন দিয়ে লগইন", BanglaStrings.loginWithPhone)
        assertEquals("ইমেইল দিয়ে লগইন", BanglaStrings.loginWithEmail)
        assertEquals("Google দিয়ে লগইন", BanglaStrings.loginWithGoogle)
        assertEquals("নতুন অ্যাকাউন্ট তৈরি করুন", BanglaStrings.createNewAccount)
        assertEquals("Login with Phone", EnglishStrings.loginWithPhone)
        assertEquals("Create New Account", EnglishStrings.createNewAccount)
        assertEquals("Bangla AI • DeepSeek", BanglaStrings.aiAssistantTitle)
    }

    @Test
    fun deepSeekTextHelpersWorkInPureJvm() {
        assertEquals(
            "ভালো আছি, আপনি কেমন আছেন?",
            AiAssistantRepository.stripThinkBlocks("<think>internal reasoning</think>ভালো আছি, আপনি কেমন আছেন?")
        )
        assertEquals(
            "হ্যালো বন্ধু",
            AiAssistantRepository.cleanQuotedOutput("\"হ্যালো বন্ধু\"")
        )
        val numberedReply = """
            1. Sure, sounds good!
            2. I'll be there soon
            3. Thanks a lot!
        """.trimIndent()
        assertEquals(
            listOf("Sure, sounds good!", "I'll be there soon", "Thanks a lot!"),
            AiAssistantRepository.parseQuickReplies(numberedReply)
        )

        val longPrompt = "a".repeat(2500)
        assertEquals(1800, AiAssistantRepository.truncateForGetUrl(longPrompt).length)
    }

    @Test
    fun callDurationFormattingInBanglaAndEnglish() {
        assertEquals("", com.example.ui.components.formatCallDuration(0L, AppLanguage.BANGLA))
        assertEquals("৪৫ সেকেন্ড", com.example.ui.components.formatCallDuration(45L, AppLanguage.BANGLA))
        assertEquals("১ মিনিট", com.example.ui.components.formatCallDuration(60L, AppLanguage.BANGLA))
        assertEquals("২ মিনিট ১৫ সেকেন্ড", com.example.ui.components.formatCallDuration(135L, AppLanguage.BANGLA))
        assertEquals("১ ঘণ্টা ১ মিনিট", com.example.ui.components.formatCallDuration(3665L, AppLanguage.BANGLA))

        assertEquals("", com.example.ui.components.formatCallDuration(0L, AppLanguage.ENGLISH))
        assertEquals("45 sec", com.example.ui.components.formatCallDuration(45L, AppLanguage.ENGLISH))
        assertEquals("1 min", com.example.ui.components.formatCallDuration(60L, AppLanguage.ENGLISH))
        assertEquals("2 min 15 sec", com.example.ui.components.formatCallDuration(135L, AppLanguage.ENGLISH))
        assertEquals("1 hr 1 min", com.example.ui.components.formatCallDuration(3665L, AppLanguage.ENGLISH))
    }

    @Test
    fun deleteForMeIsolationOnChatMessage() {
        val msg = com.example.model.ChatMessage(
            id = "m1",
            senderId = "alice",
            text = "Hello",
            deletedFor = listOf("bob")
        )
        assertTrue(msg.isDeletedFor("bob"))
        assertFalse(msg.isDeletedFor("alice"))
    }

    @Test
    fun staleAwareOnlinePresenceChecks() {
        val nowMs = 1_750_000_000_000L
        val freshHeartbeat = com.google.firebase.Timestamp((nowMs - 30_000L) / 1000L, 0)
        val staleHeartbeat = com.google.firebase.Timestamp((nowMs - 180_000L) / 1000L, 0)

        val activeUser = com.example.model.PublicProfile(
            uid = "u1",
            name = "Active",
            online = true,
            showOnlineStatus = true,
            lastSeen = freshHeartbeat
        )
        assertTrue(activeUser.isEffectivelyOnline(nowMs))
        assertTrue(activeUser.isVisiblyOnline(nowMs))

        val hiddenOnlineUser = activeUser.copy(showOnlineStatus = false)
        assertTrue(hiddenOnlineUser.isEffectivelyOnline(nowMs))
        assertFalse(hiddenOnlineUser.isVisiblyOnline(nowMs))

        val staleStuckUser = activeUser.copy(lastSeen = staleHeartbeat)
        assertFalse(staleStuckUser.isEffectivelyOnline(nowMs))
        assertFalse(staleStuckUser.isVisiblyOnline(nowMs))

        val offlineUser = activeUser.copy(online = false)
        assertFalse(offlineUser.isEffectivelyOnline(nowMs))
        assertFalse(offlineUser.isVisiblyOnline(nowMs))
    }

    @Test
    fun lastSeenFormattingInBanglaAndEnglish() {
        val nowMs = 1_750_000_000_000L
        val justNowTs = com.google.firebase.Timestamp((nowMs - 20_000L) / 1000L, 0)
        val fiveMinAgoTs = com.google.firebase.Timestamp((nowMs - 5 * 60_000L) / 1000L, 0)

        assertEquals(
            "সর্বশেষ দেখা এইমাত্র",
            com.example.ui.components.formatLastSeenText(justNowTs, AppLanguage.BANGLA, nowMs)
        )
        assertEquals(
            "Last seen just now",
            com.example.ui.components.formatLastSeenText(justNowTs, AppLanguage.ENGLISH, nowMs)
        )
        assertEquals(
            "সর্বশেষ দেখা ৫ মিনিট আগে",
            com.example.ui.components.formatLastSeenText(fiveMinAgoTs, AppLanguage.BANGLA, nowMs)
        )
        assertEquals(
            "Last seen 5 min ago",
            com.example.ui.components.formatLastSeenText(fiveMinAgoTs, AppLanguage.ENGLISH, nowMs)
        )
    }

    @Test
    fun appUpdateCheckerComparisonWorks() {
        assertTrue(
            com.example.data.AppUpdateChecker.isUpdateNewer(
                currentCode = 6,
                currentName = "1.5",
                remoteCode = 7,
                remoteName = "1.6"
            )
        )
        assertFalse(
            com.example.data.AppUpdateChecker.isUpdateNewer(
                currentCode = 6,
                currentName = "1.5",
                remoteCode = 6,
                remoteName = "1.5"
            )
        )
        assertFalse(
            com.example.data.AppUpdateChecker.isUpdateNewer(
                currentCode = 7,
                currentName = "1.6",
                remoteCode = 6,
                remoteName = "1.5"
            )
        )
        assertTrue(
            com.example.data.AppUpdateChecker.isUpdateNewer(
                currentCode = 6,
                currentName = "1.5",
                remoteCode = 6,
                remoteName = "1.5.1"
            )
        )
        // Remote code 0 (from tag-only GitHub release like v1.9) still compares SemVer properly
        assertTrue(
            com.example.data.AppUpdateChecker.isUpdateNewer(
                currentCode = 8,
                currentName = "1.7",
                remoteCode = 0,
                remoteName = "1.9"
            )
        )

        val parsedRelease = com.example.data.AppUpdateChecker.parseGitHubReleaseMetadata(
            tagName = "latest-apk",
            releaseName = "Bangla Chat v1.9 (Latest APK)",
            bodyNotes = "Bangla Chat v1.9 (Build 9) — নতুন আপডেট",
            apkUrl = "https://github.com/ridoy013393-tech/Bangla-/releases/download/latest-apk/BanglaChat.apk",
            releaseUrl = "https://github.com/ridoy013393-tech/Bangla-/releases/tag/latest-apk"
        )
        assertTrue(parsedRelease != null)
        assertEquals(9, parsedRelease!!.latestVersionCode)
        assertEquals("1.9", parsedRelease.latestVersionName)
        assertTrue(parsedRelease.forceUpdate)

        val fsConfig = com.example.model.AppUpdateConfig(
            active = true,
            latestVersionCode = 10,
            latestVersionName = "2.0",
            forceUpdate = true,
            apkUrl = "https://example.com/BanglaChat.apk"
        )
        val evaluated = com.example.data.AppUpdateChecker.evaluateFirestoreConfig(
            config = fsConfig,
            currentVersionCode = 9,
            currentVersionName = "1.9"
        )
        assertTrue(evaluated != null)
        assertEquals(10, evaluated!!.latestVersionCode)
        assertEquals("2.0", evaluated.latestVersionName)

        val winner = com.example.data.AppUpdateChecker.pickNewestUpdate(listOf(parsedRelease, evaluated))
        assertEquals(10, winner?.latestVersionCode)
    }

    @Test
    fun qrPayloadCodecRoundTripAndValidation() {
        val payload = com.example.ui.screens.QrPayloadCodec.encodeUsername("@Ridoy_01")
        assertEquals("banglachat://user/@ridoy_01", payload)
        assertEquals("ridoy_01", com.example.ui.screens.QrPayloadCodec.extractUsername(payload))
        assertEquals("hasan99", com.example.ui.screens.QrPayloadCodec.extractUsername("@Hasan99"))
        assertEquals(null, com.example.ui.screens.QrPayloadCodec.extractUsername("https://example.com/random"))
    }

    @Test
    fun voicePlaybackSpeedCyclingAndFormatting() {
        assertEquals(1.5f, com.example.data.VoiceNotePlayer.nextPlaybackSpeed(1.0f))
        assertEquals(2.0f, com.example.data.VoiceNotePlayer.nextPlaybackSpeed(1.5f))
        assertEquals(1.0f, com.example.data.VoiceNotePlayer.nextPlaybackSpeed(2.0f))
        assertEquals("1x", com.example.data.VoiceNotePlayer.formatSpeedLabel(1.0f))
        assertEquals("1.5x", com.example.data.VoiceNotePlayer.formatSpeedLabel(1.5f))
        assertEquals("2x", com.example.data.VoiceNotePlayer.formatSpeedLabel(2.0f))
    }

    @Test
    fun viewOnceOpenedTracking() {
        val unopened = com.example.model.ChatMessage(
            id = "vo1",
            senderId = "alice",
            type = "image",
            viewOnce = true,
            viewedBy = emptyList()
        )
        assertFalse(unopened.isOpenedByRecipient())
        assertFalse(unopened.isViewedBy("bob"))

        val opened = unopened.copy(viewedBy = listOf("bob"))
        assertTrue(opened.isOpenedByRecipient())
        assertTrue(opened.isViewedBy("bob"))
        assertFalse(opened.isViewedBy("alice"))
    }

    @Test
    fun moderationStatusChecksAndMuteCountdownWork() {
        val nowMs = 1_750_000_000_000L
        val futureMute = com.google.firebase.Timestamp((nowMs + 2 * 3_600_000L + 15 * 60_000L) / 1000L, 0)
        val pastMute = com.google.firebase.Timestamp((nowMs - 60_000L) / 1000L, 0)

        val muted = com.example.model.ModerationStatus(
            uid = "u1",
            banned = false,
            deletedByAdmin = false,
            mutedUntil = futureMute,
            reason = "Spam"
        )
        assertFalse(muted.isSuspended())
        assertTrue(muted.isMutedNow(nowMs))
        assertEquals(
            "2h 15m",
            com.example.ui.screens.formatRemainingMuteDuration(muted.remainingMuteMillis(nowMs), AppLanguage.ENGLISH)
        )
        assertEquals(
            "2 ঘণ্টা 15 মিনিট",
            com.example.ui.screens.formatRemainingMuteDuration(muted.remainingMuteMillis(nowMs), AppLanguage.BANGLA)
        )

        val expiredMute = muted.copy(mutedUntil = pastMute)
        assertFalse(expiredMute.isMutedNow(nowMs))
        assertEquals(0L, expiredMute.remainingMuteMillis(nowMs))

        val banned = muted.copy(banned = true)
        assertTrue(banned.isSuspended())
        assertFalse(banned.isMutedNow(nowMs))

        val deleted = com.example.model.ModerationStatus(uid = "u2", deletedByAdmin = true)
        assertTrue(deleted.isSuspended())
    }

    @Test
    fun telegramNotifierFormattingAndConfigurationGuards() {
        assertFalse(com.example.data.TelegramNotifier.isConfigured("YOUR_TELEGRAM_BOT_TOKEN_HERE", "123456"))
        assertFalse(com.example.data.TelegramNotifier.isConfigured("12345:sample", "YOUR_TELEGRAM_ADMIN_CHAT_ID_HERE"))
        assertFalse(com.example.data.TelegramNotifier.isConfigured("", ""))
        assertTrue(com.example.data.TelegramNotifier.isConfigured("12345:sampleTokenValue", "1234567890"))

        val escaped = com.example.data.TelegramNotifier.escapeHtml("A <script> & B")
        assertEquals("A &lt;script&gt; &amp; B", escaped)

        val regMsg = com.example.data.TelegramNotifier.formatNewUserMessage(
            uid = "uid_999",
            name = "Mehedi <Admin>",
            username = "@ridoy123",
            phone = "+8801712345678",
            email = "test@example.com"
        )
        assertTrue(regMsg.contains("Mehedi &lt;Admin&gt;"))
        assertTrue(regMsg.contains("@ridoy123"))
        assertTrue(regMsg.contains("+8801712345678"))
        assertTrue(regMsg.contains("uid_999"))

        val markup = com.example.data.TelegramNotifier.buildModerationReplyMarkupJson("uid_999")
        assertTrue(markup.contains("\"callback_data\":\"ban:uid_999\""))
        assertTrue(markup.contains("\"callback_data\":\"m24:uid_999\""))
        assertTrue(markup.contains("\"callback_data\":\"del:uid_999\""))
    }

    @Test
    fun contentModerationProfanityMaskingAndCustomWordsWork() {
        com.example.data.ContentModerationEngine.resetTrackingForTests()

        // Clean Bangla & English messages pass untouched
        val clean = com.example.data.ContentModerationEngine.scanProfanity("আসসালামু আলাইকুম, কেমন আছেন?")
        assertFalse(clean.hasProfanity)
        assertEquals("আসসালামু আলাইকুম, কেমন আছেন?", clean.maskedText)

        // Built-in English & Bangla profanity detection + masking
        val enScan = com.example.data.ContentModerationEngine.scanProfanity("You are a fuuuuck idiot")
        assertTrue(enScan.hasProfanity)
        assertTrue(enScan.maskedText.contains("***"))

        val bnScan = com.example.data.ContentModerationEngine.scanProfanity("তুই একটা মাদারচোদ লোক")
        assertTrue(bnScan.hasProfanity)
        assertTrue(bnScan.maskedText.contains("***"))

        // Custom admin blocked words & multi-word phrases
        val customScan = com.example.data.ContentModerationEngine.scanProfanity(
            text = "Join free money scam right now",
            customBlockedWords = listOf("free money scam")
        )
        assertTrue(customScan.hasProfanity)
        assertTrue(customScan.maskedText.contains("***"))

        // Block mode blocks message & flags Telegram alert
        val blockSettings = com.example.model.ModerationSettings(
            profanityFilterEnabled = true,
            profanityMode = "block",
            spamProtectionEnabled = true,
            telegramAlertsEnabled = true
        )
        val blocked = com.example.data.ContentModerationEngine.evaluateOutgoingMessage(
            senderUid = "user_bad_1",
            rawText = "what the fuck",
            settings = blockSettings,
            nowMillis = 1_000_000L
        )
        assertTrue(blocked is com.example.data.MessageModerationOutcome.Block)
        val blockOutcome = blocked as com.example.data.MessageModerationOutcome.Block
        assertEquals(com.example.data.ModerationViolationType.PROFANITY, blockOutcome.violationType)
        assertTrue(blockOutcome.shouldAlertAdmin)

        // Mask mode allows cleaned message through
        val maskSettings = blockSettings.copy(profanityMode = "mask")
        val masked = com.example.data.ContentModerationEngine.evaluateOutgoingMessage(
            senderUid = "user_bad_2",
            rawText = "hey fuck you",
            settings = maskSettings,
            nowMillis = 1_000_000L
        )
        assertTrue(masked is com.example.data.MessageModerationOutcome.Allow)
        val allowOutcome = masked as com.example.data.MessageModerationOutcome.Allow
        assertTrue(allowOutcome.wasMasked)
        assertEquals("hey *** you", allowOutcome.finalText)
    }

    @Test
    fun contentModerationDetectsBurstAndDuplicateSpam() {
        com.example.data.ContentModerationEngine.resetTrackingForTests()
        val settings = com.example.model.ModerationSettings(
            profanityFilterEnabled = true,
            spamProtectionEnabled = true,
            telegramAlertsEnabled = true
        )

        // Duplicate spam: 3 identical allowed, 4th blocked
        val baseTime = 2_000_000L
        for (i in 0 until 3) {
            val res = com.example.data.ContentModerationEngine.evaluateOutgoingMessage(
                senderUid = "dup_user",
                rawText = "buy cheap followers",
                settings = settings,
                nowMillis = baseTime + i * 2_000L
            )
            assertTrue(res is com.example.data.MessageModerationOutcome.Allow)
        }
        val dupBlocked = com.example.data.ContentModerationEngine.evaluateOutgoingMessage(
            senderUid = "dup_user",
            rawText = "buy cheap followers",
            settings = settings,
            nowMillis = baseTime + 7_000L
        )
        assertTrue(dupBlocked is com.example.data.MessageModerationOutcome.Block)
        assertEquals(
            com.example.data.ModerationViolationType.DUPLICATE_SPAM,
            (dupBlocked as com.example.data.MessageModerationOutcome.Block).violationType
        )

        // Burst flood: 6 distinct messages in 3s allowed, 7th blocked
        val burstBase = 3_000_000L
        for (i in 0 until 6) {
            val res = com.example.data.ContentModerationEngine.evaluateOutgoingMessage(
                senderUid = "burst_user",
                rawText = "message number $i",
                settings = settings,
                nowMillis = burstBase + i * 500L
            )
            assertTrue(res is com.example.data.MessageModerationOutcome.Allow)
        }
        val burstBlocked = com.example.data.ContentModerationEngine.evaluateOutgoingMessage(
            senderUid = "burst_user",
            rawText = "message number 7",
            settings = settings,
            nowMillis = burstBase + 3_500L
        )
        assertTrue(burstBlocked is com.example.data.MessageModerationOutcome.Block)
        assertEquals(
            com.example.data.ModerationViolationType.BURST_SPAM,
            (burstBlocked as com.example.data.MessageModerationOutcome.Block).violationType
        )
    }

    @Test
    fun appBroadcastDisplayabilityAndTelegramViolationAlertFormat() {
        val activeNotice = com.example.model.AppBroadcast(
            active = true,
            broadcastId = "bc_101",
            title = "জরুরি আপডেট",
            message = "নতুন ফিচার চালু হয়েছে!",
            severity = "update"
        )
        assertTrue(activeNotice.isDisplayable(dismissedId = ""))
        assertFalse(activeNotice.isDisplayable(dismissedId = "bc_101"))
        assertTrue(activeNotice.copy(broadcastId = "bc_102").isDisplayable(dismissedId = "bc_101"))
        assertFalse(activeNotice.copy(active = false).isDisplayable(dismissedId = ""))

        val alertHtml = com.example.data.TelegramNotifier.formatContentViolationMessage(
            senderUid = "u_77",
            senderName = "Test User",
            senderUsername = "testuser",
            senderPhone = "+8801700000000",
            violationType = com.example.data.ModerationViolationType.PROFANITY,
            flaggedSnippet = "bad <word>",
            matchedWords = listOf("bad"),
            chatId = "chat_1"
        )
        assertTrue(alertHtml.contains("Test User"))
        assertTrue(alertHtml.contains("u_77"))
        assertTrue(alertHtml.contains("bad &lt;word&gt;"))
    }
}
