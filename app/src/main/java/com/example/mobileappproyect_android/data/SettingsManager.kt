package com.example.mobileappproyect_android.data // O tu paquete preferido

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Define el DataStore a nivel de top-level (recomendado)
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(context: Context) {

    private val appContext = context.applicationContext

    companion object {
        val UI_THEME_KEY = stringPreferencesKey("ui_theme")
        val CURRENCY_KEY = stringPreferencesKey("currency_symbol")
        val LANGUAGE_KEY = stringPreferencesKey("app_language") // ej: "en", "es"
    }

    // --- Theme ---
    val uiThemeFlow: Flow<AppTheme> = appContext.dataStore.data
        .map { preferences ->
            // AppTheme.fromKey ya no necesita displayName
            AppTheme.fromKey(preferences[UI_THEME_KEY] ?: AppTheme.SYSTEM.key)
        }

    suspend fun setUiTheme(theme: AppTheme) {
        appContext.dataStore.edit { settings ->
            settings[UI_THEME_KEY] = theme.key
        }
    }

    // --- Currency ---
    val currencyFlow: Flow<String> = appContext.dataStore.data
        .map { preferences ->
            preferences[CURRENCY_KEY] ?: "€" // Default currency
        }

    suspend fun setCurrency(currencySymbol: String) {
        appContext.dataStore.edit { settings ->
            settings[CURRENCY_KEY] = currencySymbol
        }
    }

    // --- Language ---
    val languageFlow: Flow<String> = appContext.dataStore.data
        .map { preferences ->
            preferences[LANGUAGE_KEY] ?: "en" // Default language (Spanish, cambiar a "en" si prefieres inglés por defecto)
        }

    suspend fun setLanguage(languageCode: String) {
        appContext.dataStore.edit { settings ->
            settings[LANGUAGE_KEY] = languageCode
        }
    }
}

// Enum para representar las opciones de tema (solo con 'key')
enum class AppTheme(val key: String) { // <--- ELIMINADO displayName de aquí
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system");

    companion object {
        fun fromKey(key: String): AppTheme {
            return values().find { it.key == key } ?: SYSTEM
        }
    }
}