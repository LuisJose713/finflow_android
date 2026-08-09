package com.softdevcode.finflow_android.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "finflow_android_prefs")

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val TOKEN     = stringPreferencesKey("auth_token")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL= stringPreferencesKey("user_email")
        val USER_ID   = intPreferencesKey("user_id")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
    }

    val token: Flow<String?> = context.dataStore.data.map { it[Keys.TOKEN] }
    val userName: Flow<String?> = context.dataStore.data.map { it[Keys.USER_NAME] }
    val userEmail: Flow<String?> = context.dataStore.data.map { it[Keys.USER_EMAIL] }
    val userId: Flow<Int?> = context.dataStore.data.map { it[Keys.USER_ID] }
    val darkMode: Flow<Boolean?> = context.dataStore.data.map { it[Keys.DARK_MODE] }

    suspend fun saveAuth(token: String, name: String, email: String, id: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOKEN]      = token
            prefs[Keys.USER_NAME]  = name
            prefs[Keys.USER_EMAIL] = email
            prefs[Keys.USER_ID]    = id
        }
    }

    suspend fun clearAuth() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.TOKEN)
            prefs.remove(Keys.USER_NAME)
            prefs.remove(Keys.USER_EMAIL)
            prefs.remove(Keys.USER_ID)
        }
    }

    suspend fun setDarkMode(dark: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = dark }
    }

    suspend fun getTokenOnce(): String? =
        context.dataStore.data.map { it[Keys.TOKEN] }.first()
}