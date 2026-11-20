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
}

