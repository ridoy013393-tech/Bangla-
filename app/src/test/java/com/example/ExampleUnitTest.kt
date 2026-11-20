package com.example

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
    }
}
