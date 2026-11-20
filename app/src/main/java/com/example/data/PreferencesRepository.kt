package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppLanguage
import com.example.model.ChatFontSize
import com.example.model.ChatWallpaper
import com.example.model.ThemePreference
import com.example.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.security.MessageDigest

private val Context.banglaChatDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "bangla_chat_prefs"
)

class PreferencesRepository(context: Context) {
    private val dataStore = context.applicationContext.banglaChatDataStore

    private object Keys {
        val LANGUAGE = stringPreferencesKey("app_language")
        val THEME = stringPreferencesKey("theme_preference")
        val BANNER_DISMISSED = booleanPreferencesKey("online_banner_dismissed")
        val CHAT_WALLPAPER = stringPreferencesKey("chat_wallpaper")
        val CHAT_FONT_SIZE = stringPreferencesKey("chat_font_size")
        val SCREEN_SECURITY = booleanPreferencesKey("screen_security_enabled")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_LOCK_PIN_HASH = stringPreferencesKey("app_lock_pin_hash")
    }

    val preferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            val langName = prefs[Keys.LANGUAGE]
            val language = enumValues<AppLanguage>().firstOrNull { it.name == langName }
                ?: AppLanguage.BANGLA

            val themeName = prefs[Keys.THEME]
            val theme = enumValues<ThemePreference>().firstOrNull { it.name == themeName }
                ?: ThemePreference.SYSTEM

            val wallpaperName = prefs[Keys.CHAT_WALLPAPER]
            val wallpaper = enumValues<ChatWallpaper>().firstOrNull { it.name == wallpaperName }
                ?: ChatWallpaper.DEFAULT

            val fontSizeName = prefs[Keys.CHAT_FONT_SIZE]
            val fontSize = enumValues<ChatFontSize>().firstOrNull { it.name == fontSizeName }
                ?: ChatFontSize.MEDIUM

            val pinHash = prefs[Keys.APP_LOCK_PIN_HASH].orEmpty()
            val lockEnabled = (prefs[Keys.APP_LOCK_ENABLED] ?: false) && pinHash.isNotBlank()

            UserPreferences(
                language = language,
                theme = theme,
                onlineBannerDismissed = prefs[Keys.BANNER_DISMISSED] ?: false,
                chatWallpaper = wallpaper,
                chatFontSize = fontSize,
                screenSecurityEnabled = prefs[Keys.SCREEN_SECURITY] ?: false,
                appLockEnabled = lockEnabled,
                appLockPinHash = pinHash
            )
        }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setTheme(theme: ThemePreference) {
        dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setOnlineBannerDismissed(dismissed: Boolean) {
        dataStore.edit { it[Keys.BANNER_DISMISSED] = dismissed }
    }

    suspend fun setChatWallpaper(wallpaper: ChatWallpaper) {
        dataStore.edit { it[Keys.CHAT_WALLPAPER] = wallpaper.name }
    }

    suspend fun setChatFontSize(fontSize: ChatFontSize) {
        dataStore.edit { it[Keys.CHAT_FONT_SIZE] = fontSize.name }
    }

    suspend fun setScreenSecurityEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.SCREEN_SECURITY] = enabled }
    }

    suspend fun setAppLockPin(rawPin: String) {
        val hash = hashPin(rawPin)
        dataStore.edit {
            it[Keys.APP_LOCK_PIN_HASH] = hash
            it[Keys.APP_LOCK_ENABLED] = true
        }
    }

    suspend fun disableAppLock() {
        dataStore.edit {
            it[Keys.APP_LOCK_ENABLED] = false
            it[Keys.APP_LOCK_PIN_HASH] = ""
        }
    }

    companion object {
        private const val PIN_SALT = "BanglaChat#v1#AppLockSalt"

        fun hashPin(rawPin: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest("$PIN_SALT:${rawPin.trim()}".toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun verifyPin(rawPin: String, storedHash: String): Boolean {
            if (storedHash.isBlank()) return false
            return hashPin(rawPin) == storedHash
        }
    }
}
