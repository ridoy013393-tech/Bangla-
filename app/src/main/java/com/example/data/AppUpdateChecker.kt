package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import com.example.model.AppUpdateConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val apkUrl: String,
    val releaseUrl: String,
    val releaseNotes: String,
    val forceUpdate: Boolean = true
)

sealed interface UpdateCheckResult {
    data object Idle : UpdateCheckResult
    data object Checking : UpdateCheckResult
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckResult
    data class UpToDate(val currentVersionName: String) : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

object AppUpdateChecker {
    private const val REPO_SLUG = "ridoy013393-tech/Bangla-"
    private const val VERSION_JSON_URL =
        "https://github.com/$REPO_SLUG/releases/download/latest-apk/version.json"
    private const val GITHUB_LATEST_RELEASE_API_URL =
        "https://api.github.com/repos/$REPO_SLUG/releases/latest"
    private const val GITHUB_RELEASES_LIST_API_URL =
        "https://api.github.com/repos/$REPO_SLUG/releases?per_page=6"
    private const val GITHUB_TAG_LATEST_APK_API_URL =
        "https://api.github.com/repos/$REPO_SLUG/releases/tags/latest-apk"

    const val FALLBACK_APK_URL =
        "https://github.com/$REPO_SLUG/releases/download/latest-apk/BanglaChat.apk"
    const val FALLBACK_RELEASE_URL =
        "https://github.com/$REPO_SLUG/releases/latest"

    private val SEMVER_REGEX = Regex("""(?i)(?:v|version[\s:]*)?(\d+\.\d+(?:\.\d+)?)""")
    private val EXPLICIT_BODY_SEMVER_REGEX = Regex("""(?i)(?:^|[\s(\[])(?:v|version[\s:]+)(\d+\.\d+(?:\.\d+)?)\b""")
    private val VERSION_CODE_REGEX = Regex("""(?i)(?:versionCode|build|code)[\s:=#]*(\d{1,6})""")

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    /**
     * Converts a live Firestore [AppUpdateConfig] (managed from Super Admin panel) into an
     * [AppUpdateInfo] if active and valid.
     */
    fun fromFirestoreConfig(config: AppUpdateConfig?): AppUpdateInfo? {
        if (config == null || !config.active) return null
        if (config.latestVersionCode <= 0 && config.latestVersionName.isBlank()) return null
        val apk = config.apkUrl.trim().ifBlank { FALLBACK_APK_URL }
        val release = config.releaseUrl.trim().ifBlank { apk }
        return AppUpdateInfo(
            latestVersionCode = config.latestVersionCode,
            latestVersionName = config.latestVersionName.trim().ifBlank { "1.${config.latestVersionCode}" },
            apkUrl = apk,
            releaseUrl = release,
            releaseNotes = config.releaseNotes.trim(),
            forceUpdate = config.forceUpdate
        )
    }

    /**
     * Evaluates a live Firestore [AppUpdateConfig] immediately without waiting for network I/O.
     */
    fun evaluateFirestoreConfig(
        config: AppUpdateConfig?,
        currentVersionCode: Int = BuildConfig.VERSION_CODE,
        currentVersionName: String = BuildConfig.VERSION_NAME
    ): AppUpdateInfo? {
        val info = fromFirestoreConfig(config) ?: return null
        return if (isUpdateNewer(currentVersionCode, currentVersionName, info.latestVersionCode, info.latestVersionName)) {
            info
        } else {
            null
        }
    }

    /**
     * Checks ALL live update sources (Firestore live config + GitHub latest-apk version.json +
     * GitHub Releases API `/releases/latest` & `/releases`) with strict cache-busting so a newly
     * published release or APK is ALWAYS detected regardless of tag name or CDN caching.
     */
    suspend fun checkForUpdate(
        firestoreConfig: AppUpdateConfig? = null,
        currentVersionCode: Int = BuildConfig.VERSION_CODE,
        currentVersionName: String = BuildConfig.VERSION_NAME
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        val cacheBuster = System.currentTimeMillis()
        val candidates = mutableListOf<AppUpdateInfo>()

        // 1. Live Firestore Admin Update Config (instant real-time override)
        fromFirestoreConfig(firestoreConfig)?.let { candidates.add(it) }

        // 2. Direct version.json on the rolling `latest-apk` release (cache-busted)
        fetchUrl("$VERSION_JSON_URL?t=$cacheBuster", accept = "application/json")
            ?.let { parseVersionJson(it) }
            ?.let { candidates.add(it) }

        // 3. GitHub Releases API `/releases/latest` (supports versioned tags like v1.9, v2.0, etc.)
        fetchUrl("$GITHUB_LATEST_RELEASE_API_URL?t=$cacheBuster", accept = "application/vnd.github+json")
            ?.let { parseGitHubReleaseObject(it, cacheBuster) }
            ?.let { candidates.add(it) }

        // 4. GitHub Releases API list (catches pre-releases, custom tags, or updated `latest-apk` tag)
        if (candidates.isEmpty() || candidates.none {
                isUpdateNewer(currentVersionCode, currentVersionName, it.latestVersionCode, it.latestVersionName)
            }
        ) {
            val listBody = fetchUrl(
                "$GITHUB_RELEASES_LIST_API_URL&t=$cacheBuster",
                accept = "application/vnd.github+json"
            )
            if (listBody != null) {
                parseGitHubReleasesList(listBody, cacheBuster).forEach { candidates.add(it) }
            } else {
                // Final fallback to tags/latest-apk
                fetchUrl("$GITHUB_TAG_LATEST_APK_API_URL?t=$cacheBuster", accept = "application/vnd.github+json")
                    ?.let { parseGitHubReleaseObject(it, cacheBuster) }
                    ?.let { candidates.add(it) }
            }
        }

        if (candidates.isEmpty()) {
            return@withContext UpdateCheckResult.Failed("আপডেট সার্ভারের সাথে সংযোগ করা যায়নি।")
        }

        val best = pickNewestUpdate(candidates)
        if (best != null && isUpdateNewer(currentVersionCode, currentVersionName, best.latestVersionCode, best.latestVersionName)) {
            UpdateCheckResult.UpdateAvailable(best)
        } else {
            UpdateCheckResult.UpToDate(currentVersionName)
        }
    }

    private fun fetchUrl(url: String, accept: String): String? = runCatching {
        val req = Request.Builder()
            .url(url)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .header("Accept", accept)
            .header("Cache-Control", "no-cache, no-store, must-revalidate")
            .header("Pragma", "no-cache")
            .header("User-Agent", "BanglaChat-Android/${BuildConfig.VERSION_NAME}")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@runCatching null
            resp.body?.string()
        }
    }.getOrNull()

    fun parseVersionJson(rawJson: String): AppUpdateInfo? {
        if (rawJson.isBlank()) return null
        return runCatching {
            val obj = JSONObject(rawJson)
            val code = obj.optInt("versionCode", 0)
            val name = obj.optString("versionName", "").trim()
            if (code <= 0 && name.isBlank()) return null
            val apk = obj.optString("apkUrl", "").trim().ifBlank { FALLBACK_APK_URL }
            val release = obj.optString("releaseUrl", "").trim().ifBlank { FALLBACK_RELEASE_URL }
            val notes = obj.optString("notes", obj.optString("releaseNotes", "")).trim()
            // Default to mandatory force update so users stay on the latest build
            val force = if (obj.has("forceUpdate")) obj.optBoolean("forceUpdate", true) else true
            AppUpdateInfo(
                latestVersionCode = code,
                latestVersionName = name.ifBlank { "1.$code" },
                apkUrl = apk,
                releaseUrl = release,
                releaseNotes = notes,
                forceUpdate = force
            )
        }.getOrNull()
    }

    private fun parseGitHubReleasesList(rawJson: String, cacheBuster: Long): List<AppUpdateInfo> {
        if (rawJson.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(rawJson)
            val results = mutableListOf<AppUpdateInfo>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                if (obj.optBoolean("draft", false)) continue
                parseGitHubReleaseJsonObject(obj, cacheBuster)?.let { results.add(it) }
            }
            results
        }.getOrDefault(emptyList())
    }

    private fun parseGitHubReleaseObject(rawJson: String, cacheBuster: Long): AppUpdateInfo? {
        if (rawJson.isBlank()) return null
        return runCatching {
            val obj = JSONObject(rawJson)
            if (obj.optBoolean("draft", false)) return@runCatching null
            parseGitHubReleaseJsonObject(obj, cacheBuster)
        }.getOrNull()
    }

    private fun parseGitHubReleaseJsonObject(obj: JSONObject, cacheBuster: Long): AppUpdateInfo? {
        val htmlUrl = obj.optString("html_url", "").trim().ifBlank { FALLBACK_RELEASE_URL }
        val tagName = obj.optString("tag_name", "").trim()
        val releaseName = obj.optString("name", "").trim()
        val bodyNotes = obj.optString("body", "").trim()
        val assets = obj.optJSONArray("assets")

        var apkDownload = ""
        var versionJsonAssetUrl = ""
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val assetName = asset.optString("name", "")
                val dlUrl = asset.optString("browser_download_url", "").trim()
                if (assetName.endsWith(".apk", ignoreCase = true) && apkDownload.isBlank()) {
                    apkDownload = dlUrl
                } else if (assetName.equals("version.json", ignoreCase = true) && versionJsonAssetUrl.isBlank()) {
                    versionJsonAssetUrl = dlUrl
                }
            }
        }

        // If this specific release attaches its own version.json, prefer its exact version metadata
        if (versionJsonAssetUrl.isNotBlank()) {
            val sep = if (versionJsonAssetUrl.contains("?")) "&" else "?"
            val fromAsset = fetchUrl("$versionJsonAssetUrl${sep}t=$cacheBuster", accept = "application/json")
                ?.let { parseVersionJson(it) }
            if (fromAsset != null) {
                return fromAsset.copy(
                    apkUrl = apkDownload.ifBlank { fromAsset.apkUrl },
                    releaseUrl = htmlUrl.ifBlank { fromAsset.releaseUrl }
                )
            }
        }

        return parseGitHubReleaseMetadata(
            tagName = tagName,
            releaseName = releaseName,
            bodyNotes = bodyNotes,
            apkUrl = apkDownload.ifBlank { FALLBACK_APK_URL },
            releaseUrl = htmlUrl
        )
    }

    /**
     * Pure parser (easily unit-testable) that extracts versionName and optional versionCode
     * from a GitHub Release's tag_name, title, and body notes.
     */
    fun parseGitHubReleaseMetadata(
        tagName: String,
        releaseName: String,
        bodyNotes: String,
        apkUrl: String,
        releaseUrl: String
    ): AppUpdateInfo? {
        val extractedName = extractSemVer(tagName)
            ?: extractSemVer(releaseName)
            ?: extractExplicitBodySemVer(bodyNotes)
        val extractedCode = VERSION_CODE_REGEX.find(bodyNotes)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: VERSION_CODE_REGEX.find(releaseName)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: 0

        if (extractedName.isNullOrBlank() && extractedCode <= 0) return null

        return AppUpdateInfo(
            latestVersionCode = extractedCode,
            latestVersionName = extractedName ?: "1.$extractedCode",
            apkUrl = apkUrl.ifBlank { FALLBACK_APK_URL },
            releaseUrl = releaseUrl.ifBlank { FALLBACK_RELEASE_URL },
            releaseNotes = bodyNotes.trim(),
            forceUpdate = true
        )
    }

    private fun extractSemVer(source: String): String? {
        if (source.isBlank()) return null
        return SEMVER_REGEX.find(source)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun extractExplicitBodySemVer(source: String): String? {
        if (source.isBlank()) return null
        return EXPLICIT_BODY_SEMVER_REGEX.find(source)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    fun pickNewestUpdate(candidates: List<AppUpdateInfo>): AppUpdateInfo? {
        if (candidates.isEmpty()) return null
        return candidates.reduce { best, next ->
            if (isUpdateNewer(best.latestVersionCode, best.latestVersionName, next.latestVersionCode, next.latestVersionName)) {
                next
            } else {
                best
            }
        }
    }

    fun isUpdateNewer(
        currentCode: Int,
        currentName: String,
        remoteCode: Int,
        remoteName: String
    ): Boolean {
        if (remoteCode > 0 && currentCode > 0) {
            if (remoteCode > currentCode) return true
            if (remoteCode < currentCode) return false
        }
        return compareSemVer(remoteName, currentName) > 0
    }

    private fun compareSemVer(a: String, b: String): Int {
        if (a.isBlank() && b.isBlank()) return 0
        if (a.isBlank()) return -1
        if (b.isBlank()) return 1
        val partsA = a.trim().removePrefix("v").removePrefix("V")
            .split(".")
            .map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val partsB = b.trim().removePrefix("v").removePrefix("V")
            .split(".")
            .map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val maxLen = maxOf(partsA.size, partsB.size)
        for (i in 0 until maxLen) {
            val va = partsA.getOrElse(i) { 0 }
            val vb = partsB.getOrElse(i) { 0 }
            if (va != vb) return va.compareTo(vb)
        }
        return 0
    }

    /**
     * Launches the direct APK download URL in the system browser / download handler
     * (with automatic fallback to the release page URL).
     */
    fun openDirectDownload(context: Context, info: AppUpdateInfo): Boolean {
        val primary = info.apkUrl.trim().ifBlank { FALLBACK_APK_URL }
        val openedPrimary = runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(primary)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
        if (openedPrimary) return true

        val fallback = info.releaseUrl.trim().ifBlank { FALLBACK_RELEASE_URL }
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fallback)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun openReleasePage(context: Context, info: AppUpdateInfo): Boolean {
        val target = info.releaseUrl.trim().ifBlank { FALLBACK_RELEASE_URL }
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    /**
     * Resolves the best direct `.apk` download link to share with friends.
     * Uses the live Super Admin configured APK URL when valid, otherwise falls back
     * to the permanent rolling `latest-apk/BanglaChat.apk` direct download URL.
     */
    fun resolveDirectApkUrl(config: AppUpdateConfig? = null): String {
        val customUrl = config?.apkUrl?.trim().orEmpty()
        return if (customUrl.startsWith("https://", ignoreCase = true) ||
            customUrl.startsWith("http://", ignoreCase = true)
        ) {
            customUrl
        } else {
            FALLBACK_APK_URL
        }
    }

    /**
     * Formats a clean invite message containing the direct APK download URL so recipients
     * go straight to downloading the app upon tapping the link.
     */
    fun buildAppShareMessage(
        downloadUrl: String,
        username: String = "",
        isBangla: Boolean = true
    ): String {
        val cleanUrl = downloadUrl.trim().ifBlank { FALLBACK_APK_URL }
        val cleanUsername = username.trim().removePrefix("@")
        return if (isBangla) {
            buildString {
                append("Bangla Chat অ্যাপ ডাউনলোড করুন — নিরাপদ, দ্রুত ও বিজ্ঞাপন-মুক্ত বাংলা চ্যাট অ্যাপ!\n\n")
                append("সরাসরি ডাউনলোড লিংক (APK):\n")
                append(cleanUrl)
                if (cleanUsername.isNotBlank()) {
                    append("\n\nঅ্যাপ ইনস্টল করে আমাকে খুঁজুন: @$cleanUsername")
                }
            }
        } else {
            buildString {
                append("Download Bangla Chat — fast, secure & ad-free Bangla messaging app!\n\n")
                append("Direct APK Download:\n")
                append(cleanUrl)
                if (cleanUsername.isNotBlank()) {
                    append("\n\nFind me on Bangla Chat: @$cleanUsername")
                }
            }
        }
    }

    fun shareAppDownloadLink(
        context: Context,
        chooserTitle: String,
        shareText: String
    ): Boolean = runCatching {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Bangla Chat APK Download")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        true
    }.getOrDefault(false)
}
