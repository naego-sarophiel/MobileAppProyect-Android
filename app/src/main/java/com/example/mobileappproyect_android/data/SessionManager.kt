package com.example.mobileappproyect_android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Rename the extension property for clarity and to avoid conflicts
val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")

data class UserSession(
    val email: String?,
    val userId: String?,
    val rememberMe: Boolean
)

class SessionManager(context: Context) {
    private val appContext = context.applicationContext

    companion object {
        private val REMEMBER_ME_KEY = booleanPreferencesKey("remember_me")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_ID_KEY = stringPreferencesKey("user_id") // Or session token
    }

    // Flow for the entire user session
    val userSessionFlow: Flow<UserSession> = appContext.sessionDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val rememberMe = preferences[REMEMBER_ME_KEY] ?: false
            val email = if (rememberMe) preferences[USER_EMAIL_KEY] else null
            val userId = if (rememberMe) preferences[USER_ID_KEY] else null
            UserSession(email, userId, rememberMe)
        }

    suspend fun saveSession(email: String, userId: String, rememberMe: Boolean) {
        appContext.sessionDataStore.edit { preferences ->
            preferences[REMEMBER_ME_KEY] = rememberMe
            if (rememberMe) {
                preferences[USER_EMAIL_KEY] = email
                preferences[USER_ID_KEY] = userId
            } else {
                // If not rememberMe, clear saved credentials
                preferences.remove(USER_EMAIL_KEY)
                preferences.remove(USER_ID_KEY)
            }
        }
    }

    suspend fun updateRememberMe(rememberMe: Boolean) {
        appContext.sessionDataStore.edit { preferences ->
            preferences[REMEMBER_ME_KEY] = rememberMe
            if (!rememberMe) {
                // If "Remember Me" is unchecked, clear saved credentials
                preferences.remove(USER_EMAIL_KEY)
                preferences.remove(USER_ID_KEY)
            }
        }
    }

    suspend fun clearSession() {
        appContext.sessionDataStore.edit { preferences ->
            preferences.clear() // Clears all session preferences
        }
    }
}