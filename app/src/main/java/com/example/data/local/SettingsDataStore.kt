package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "insta_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val RATE_LIMIT_DELAY_SECONDS = intPreferencesKey("rate_limit_delay_seconds")
        val MAX_DAILY_MESSAGES = intPreferencesKey("max_daily_messages")
        val META_APP_ID = stringPreferencesKey("meta_app_id")
        val META_APP_SECRET = stringPreferencesKey("meta_app_secret")
        val DEFAULT_SENDER_ACCOUNT_ID = longPreferencesKey("default_sender_account_id")
        val AUTO_ASSIGN_ACCOUNTS = booleanPreferencesKey("auto_assign_accounts")
        val INSTAGRAM_COOKIES = stringPreferencesKey("instagram_cookies")
        val INSTAGRAM_CSRF_TOKEN = stringPreferencesKey("instagram_csrf_token")
        val INSTAGRAM_SESSION_USER = stringPreferencesKey("instagram_session_user")
        val APP_THEME_MODE = stringPreferencesKey("app_theme_mode")
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[APP_THEME_MODE] ?: "SYSTEM"
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[APP_THEME_MODE] = mode
        }
    }

    val rateLimitDelay: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[RATE_LIMIT_DELAY_SECONDS] ?: 12
    }

    val maxDailyMessages: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[MAX_DAILY_MESSAGES] ?: 50
    }

    val metaAppId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[META_APP_ID] ?: ""
    }

    val metaAppSecret: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[META_APP_SECRET] ?: ""
    }

    val defaultSenderAccountId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[DEFAULT_SENDER_ACCOUNT_ID]
    }

    val autoAssignAccounts: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTO_ASSIGN_ACCOUNTS] ?: true
    }

    val instagramCookies: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[INSTAGRAM_COOKIES] ?: ""
    }

    val instagramCsrfToken: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[INSTAGRAM_CSRF_TOKEN] ?: ""
    }

    val instagramSessionUser: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[INSTAGRAM_SESSION_USER] ?: ""
    }

    suspend fun saveSessionCookies(cookies: String, csrfToken: String, username: String) {
        context.dataStore.edit { prefs ->
            prefs[INSTAGRAM_COOKIES] = cookies
            prefs[INSTAGRAM_CSRF_TOKEN] = csrfToken
            prefs[INSTAGRAM_SESSION_USER] = username
        }
    }

    suspend fun clearSessionCookies() {
        context.dataStore.edit { prefs ->
            prefs[INSTAGRAM_COOKIES] = ""
            prefs[INSTAGRAM_CSRF_TOKEN] = ""
            prefs[INSTAGRAM_SESSION_USER] = ""
        }
    }

    suspend fun updateSettings(
        delaySec: Int,
        maxDaily: Int,
        appId: String,
        appSecret: String,
        autoAssign: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[RATE_LIMIT_DELAY_SECONDS] = delaySec
            prefs[MAX_DAILY_MESSAGES] = maxDaily
            prefs[META_APP_ID] = appId
            prefs[META_APP_SECRET] = appSecret
            prefs[AUTO_ASSIGN_ACCOUNTS] = autoAssign
        }
    }
}
