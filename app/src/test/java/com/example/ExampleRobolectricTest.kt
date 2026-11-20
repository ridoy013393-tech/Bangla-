package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AiAssistantRepository
import com.example.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Bangla Chat", appName)
    }

    @Test
    fun `firestore database id resource is configured`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbId = context.getString(R.string.firestore_database_id)
        assertTrue("Expected non-blank named Firestore database ID", dbId.isNotBlank())
    }

    @Test
    fun `deepSeek json response parsing extracts reply and handles errors`() {
        val sample = """
            {
              "success": true,
              "status": "ok",
              "owner": "Xrahat-dev",
              "model": "DeepSeek",
              "reply": "<think>internal reasoning</think>ভালো আছি, আপনি কেমন আছেন?"
            }
        """.trimIndent()
        val parsed = AiAssistantRepository.parseDeepSeekResponse(sample, AppLanguage.BANGLA)
        assertTrue(parsed.isSuccess)
        assertEquals("ভালো আছি, আপনি কেমন আছেন?", parsed.getOrNull())

        val failed = AiAssistantRepository.parseDeepSeekResponse(
            """{"success": false, "reply": ""}""",
            AppLanguage.ENGLISH
        )
        assertTrue(failed.isFailure)
    }

    @Test
    fun `deepSeek json quick replies parse cleanly`() {
        val jsonArrayReply = """```json
            ["হ্যাঁ ঠিক আছে!", "একটু পরে জানাচ্ছি", "অসংখ্য ধন্যবাদ"]
            ```""".trimIndent()
        assertEquals(
            listOf("হ্যাঁ ঠিক আছে!", "একটু পরে জানাচ্ছি", "অসংখ্য ধন্যবাদ"),
            AiAssistantRepository.parseQuickReplies(jsonArrayReply)
        )

        val jsonObjectReply = """{"replies": ["ঠিক আছে", "আসছি একটু পর", "ধন্যবাদ"]}"""
        assertEquals(
            listOf("ঠিক আছে", "আসছি একটু পর", "ধন্যবাদ"),
            AiAssistantRepository.parseQuickReplies(jsonObjectReply)
        )
    }
}
