package com.cinevault.tv.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cinevault_tv_prefs")

@Singleton
class SessionManager @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val USER_ID = stringPreferencesKey("user_id")
        private val USER_NAME = stringPreferencesKey("user_name")
        private val USER_EMAIL = stringPreferencesKey("user_email")
        private val USER_AVATAR = stringPreferencesKey("user_avatar")
        private val IS_PREMIUM = booleanPreferencesKey("is_premium")
        private val PREMIUM_PLAN = stringPreferencesKey("premium_plan")
        private val PREMIUM_EXPIRES_AT = stringPreferencesKey("premium_expires_at")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN] }
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[ACCESS_TOKEN] != null }
    val userId: Flow<String?> = context.dataStore.data.map { it[USER_ID] }
    val isPremium: Flow<Boolean> = context.dataStore.data.map { it[IS_PREMIUM] ?: false }
    val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }
    val userEmail: Flow<String?> = context.dataStore.data.map { it[USER_EMAIL] }
    val userAvatar: Flow<String?> = context.dataStore.data.map { it[USER_AVATAR] }

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String?,
        userId: String,
        name: String,
        email: String,
        avatar: String?,
        isPremium: Boolean,
        premiumPlan: String?,
        premiumExpiresAt: String?,
    ) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = accessToken
            refreshToken?.let { prefs[REFRESH_TOKEN] = it }
            prefs[USER_ID] = userId
            prefs[USER_NAME] = name
            prefs[USER_EMAIL] = email
            avatar?.let { prefs[USER_AVATAR] = it }
            prefs[IS_PREMIUM] = isPremium
            premiumPlan?.let { prefs[PREMIUM_PLAN] = it }
            premiumExpiresAt?.let { prefs[PREMIUM_EXPIRES_AT] = it }
        }
    }

    suspend fun updateToken(token: String) {
        context.dataStore.edit { prefs -> prefs[ACCESS_TOKEN] = token }
    }

    suspend fun saveRefreshToken(token: String) {
        context.dataStore.edit { prefs -> prefs[REFRESH_TOKEN] = token }
    }

    suspend fun updatePremiumStatus(isPremium: Boolean, plan: String?, expiresAt: String?) {
        context.dataStore.edit { prefs ->
            prefs[IS_PREMIUM] = isPremium
            plan?.let { prefs[PREMIUM_PLAN] = it }
            expiresAt?.let { prefs[PREMIUM_EXPIRES_AT] = it }
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}
