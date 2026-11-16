package com.softdevcode.finflow_android.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import com.softdevcode.finflow_android.domain.model.AppTheme
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "finflow_android_prefs")

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cipher = TokenCipher()
    private object Keys {
        val REFRESH = stringPreferencesKey("auth_refresh")
        val TOKEN     = stringPreferencesKey("auth_token")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL= stringPreferencesKey("user_email")
        val USER_ID   = intPreferencesKey("user_id")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val THEME = stringPreferencesKey("theme_mode")
        val LAST_DARK_THEME = stringPreferencesKey("last_dark_theme")
    }

    val token: Flow<String?> = context.dataStore.data.map { cipher.read(it[Keys.TOKEN]) }
    val refreshToken: Flow<String?> = context.dataStore.data.map { cipher.read(it[Keys.REFRESH]) }
    val userName: Flow<String?> = context.dataStore.data.map { cipher.read(it[Keys.USER_NAME]) }
    val userEmail: Flow<String?> = context.dataStore.data.map { cipher.read(it[Keys.USER_EMAIL]) }
    val userId: Flow<Int?> = context.dataStore.data.map { it[Keys.USER_ID] }
    val themeMode: Flow<AppTheme> = context.dataStore.data.map {
        AppTheme.resolve(it[Keys.THEME], it[Keys.DARK_MODE])
    }.distinctUntilChanged()
    val darkMode: Flow<Boolean?> = themeMode.map { it.isDark }

    suspend fun saveAuth(token: String, name: String, email: String, id: Int, refreshToken: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOKEN]      = cipher.encrypt(token)
            prefs[Keys.USER_NAME]  = cipher.encrypt(name)
            prefs[Keys.USER_EMAIL] = cipher.encrypt(email)
            prefs[Keys.USER_ID]    = id
            if (refreshToken != null) prefs[Keys.REFRESH] = cipher.encrypt(refreshToken) else prefs.remove(Keys.REFRESH)
        }
    }

    suspend fun clearAuth() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.REFRESH)
            prefs.remove(Keys.TOKEN)
            prefs.remove(Keys.USER_NAME)
            prefs.remove(Keys.USER_EMAIL)
            prefs.remove(Keys.USER_ID)
        }
    }

    suspend fun setDarkMode(dark: Boolean) {
        context.dataStore.edit { prefs ->
            writeTheme(prefs, if (dark) preferredDarkTheme(prefs) else AppTheme.LIGHT)
        }
    }

    suspend fun setThemeMode(theme: AppTheme) {
        context.dataStore.edit { writeTheme(it, theme) }
    }

    suspend fun toggleDarkMode() {
        context.dataStore.edit { prefs ->
            val current = AppTheme.resolve(prefs[Keys.THEME], prefs[Keys.DARK_MODE])
            writeTheme(prefs, if (current.isDark) AppTheme.LIGHT else preferredDarkTheme(prefs))
        }
    }

    private fun preferredDarkTheme(prefs: Preferences): AppTheme =
        AppTheme.resolve(prefs[Keys.LAST_DARK_THEME], true).takeIf { it.isDark } ?: AppTheme.DARK

    private fun writeTheme(prefs: MutablePreferences, theme: AppTheme) {
        prefs[Keys.THEME] = theme.name
        prefs[Keys.DARK_MODE] = theme.isDark
        if (theme.isDark) prefs[Keys.LAST_DARK_THEME] = theme.name
    }

    suspend fun getTokenOnce(): String? {
        // Most requests only READ the encrypted token. Avoid a DataStore write
        // transaction per API call (previously a source of unnecessary latency).
        val current = context.dataStore.data.first()[Keys.TOKEN] ?: return null
        if (current.startsWith("enc:v1:")) return cipher.read(current)

        // One-time, concurrency-safe migration of historical plaintext values.
        context.dataStore.edit { prefs ->
            listOf(Keys.TOKEN, Keys.REFRESH, Keys.USER_NAME, Keys.USER_EMAIL).forEach { key ->
                prefs[key]?.takeUnless { it.startsWith("enc:v1:") }?.let { prefs[key] = cipher.encrypt(it) }
            }
        }
        return token.first()
    }

    suspend fun replaceTokensIfCurrent(expected: String, response: com.softdevcode.finflow_android.domain.model.AuthResponse): Boolean {
        var saved = false
        context.dataStore.edit { prefs ->
            if (cipher.read(prefs[Keys.TOKEN]) == expected && prefs[Keys.USER_ID] == response.user.id) {
                prefs[Keys.TOKEN] = cipher.encrypt(response.token)
                response.refreshToken?.let { prefs[Keys.REFRESH] = cipher.encrypt(it) }
                saved = true
            }
        }
        return saved
    }

    suspend fun clearIfTokenMatches(expected: String) {
        context.dataStore.edit { prefs ->
            if (cipher.read(prefs[Keys.TOKEN]) == expected) {
                listOf(Keys.TOKEN, Keys.REFRESH, Keys.USER_NAME, Keys.USER_EMAIL).forEach { prefs.remove(it) }
                prefs.remove(Keys.USER_ID)
            }
        }
    }
}
