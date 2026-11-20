package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val apkUrl: String,
    val releaseUrl: String,
    val releaseNotes: String
)

sealed interface UpdateCheckResult {
    data object Idle : UpdateCheckResult
    data object Checking : UpdateCheckResult
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckResult
    data class UpToDate(val currentVersionName: String) : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

object AppUpdateChecker {
    private const val VERSION_JSON_URL =
        "https://github.com/ridoy013393-tech/Bangla-/releases/download/latest-apk/version.json"
    private const val GITHUB_RELEASE_API_URL =
        "https://api.github.com/repos/ridoy013393-tech/Bangla-/releases/tags/latest-apk"
    const val FALLBACK_APK_URL =
        "https://github.com/ridoy013393-tech/Bangla-/releases/download/latest-apk/BanglaChat.apk"
    const val FALLBACK_RELEASE_URL =
        "https://github.com/ridoy013393-tech/Bangla-/releases/tag/latest-apk"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    suspend fun checkForUpdate(
        currentVersionCode: Int = BuildConfig.VERSION_CODE,
        currentVersionName: String = BuildConfig.VERSION_NAME
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        val fromManifest = runCatching {
            val req = Request.Builder()
                .url(VERSION_JSON_URL)
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching null
                val body = resp.body?.string().orEmpty()
                parseVersionJson(body)
            }
        }.getOrNull()

        val info = fromManifest ?: runCatching {
            val req = Request.Builder()
                .url(GITHUB_RELEASE_API_URL)
                .header("Accept", "application/vnd.github+json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching null
                val body = resp.body?.string().orEmpty()
                parseGitHubReleaseApiFallback(body, currentVersionCode, currentVersionName)
            }
        }.getOrNull()

        if (info == null) {
            return@withContext UpdateCheckResult.Failed("আপডেট সার্ভারের সাথে সংযোগ করা যায়নি।")
        }

        if (isUpdateNewer(currentVersionCode, currentVersionName, info.latestVersionCode, info.latestVersionName)) {
            UpdateCheckResult.UpdateAvailable(info)
        } else {
            UpdateCheckResult.UpToDate(currentVersionName)
        }
    }

    fun parseVersionJson(rawJson: String): AppUpdateInfo? {
        if (rawJson.isBlank()) return null
        return runCatching {
            val obj = JSONObject(rawJson)
            val code = obj.optInt("versionCode", 0)
            val name = obj.optString("versionName", "").trim()
            if (code <= 0 && name.isBlank()) return null
            val apk = obj.optString("apkUrl", "").trim().ifBlank { FALLBACK_APK_URL }
            val release = obj.optString("releaseUrl", "").trim().ifBlank { FALLBACK_RELEASE_URL }
            val notes = obj.optString("notes", "").trim()
            AppUpdateInfo(
                latestVersionCode = code,
                latestVersionName = name.ifBlank { "1.$code" },
                apkUrl = apk,
                releaseUrl = release,
                releaseNotes = notes
            )
        }.getOrNull()
    }

    fun parseGitHubReleaseApiFallback(
        rawJson: String,
        currentVersionCode: Int,
        currentVersionName: String
    ): AppUpdateInfo? {
        if (rawJson.isBlank()) return null
        return runCatching {
            val obj = JSONObject(rawJson)
            val htmlUrl = obj.optString("html_url", "").trim().ifBlank { FALLBACK_RELEASE_URL }
            val bodyNotes = obj.optString("body", "").trim()
            val assets = obj.optJSONArray("assets")
            var apkDownload = FALLBACK_APK_URL
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkDownload = asset.optString("browser_download_url", apkDownload)
                        break
                    }
                }
            }
            AppUpdateInfo(
                latestVersionCode = currentVersionCode,
                latestVersionName = currentVersionName,
                apkUrl = apkDownload,
                releaseUrl = htmlUrl,
                releaseNotes = bodyNotes
            )
        }.getOrNull()
    }

    fun isUpdateNewer(
        currentCode: Int,
        currentName: String,
        remoteCode: Int,
        remoteName: String
    ): Boolean {
        if (remoteCode > currentCode) return true
        if (remoteCode in 1 until currentCode) return false
        return compareSemVer(remoteName, currentName) > 0
    }

    private fun compareSemVer(a: String, b: String): Int {
        val partsA = a.removePrefix("v").split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val partsB = b.removePrefix("v").split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val maxLen = maxOf(partsA.size, partsB.size)
        for (i in 0 until maxLen) {
            val va = partsA.getOrElse(i) { 0 }
            val vb = partsB.getOrElse(i) { 0 }
            if (va != vb) return va.compareTo(vb)
        }
        return 0
    }
}
