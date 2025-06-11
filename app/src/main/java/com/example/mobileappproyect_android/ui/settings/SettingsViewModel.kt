package com.example.mobileappproyect_android.ui.settings

import android.app.Application
import android.util.Log // Para logging
import androidx.appcompat.app.AppCompatDelegate // Importante para cambiar el Locale
import androidx.core.os.LocaleListCompat // Importante para cambiar el Locale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider // Needed for the factory
import androidx.lifecycle.viewModelScope
import com.example.mobileappproyect_android.data.AppTheme
import com.example.mobileappproyect_android.data.SessionManager // Import SessionManager
import com.example.mobileappproyect_android.data.SettingsManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Updated constructor to include SessionManager
class SettingsViewModel(
    application: Application,
    private val settingsManager: SettingsManager, // Keep this if SettingsManager is directly used
    private val sessionManager: SessionManager // Add SessionManager
) : AndroidViewModel(application) {

    // If SettingsManager is only initialized here and not passed via constructor,
    // then the constructor above needs to be:
    // class SettingsViewModel(
    //    application: Application,
    //    private val sessionManager: SessionManager // Add SessionManager
    // ) : AndroidViewModel(application) {
    //    private val settingsManager = SettingsManager(application) // Initialized internally
    //
    // However, for consistency with how it's being provided via factory in MainActivity,
    // let's assume settingsManager is also injected.

    val currentTheme: StateFlow<AppTheme> = settingsManager.uiThemeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.SYSTEM
        )

    val currentCurrency: StateFlow<String> = settingsManager.currencyFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "$"
        )

    private val _recreateActivityEvent = MutableSharedFlow<Unit>(replay = 0)
    val recreateActivityEvent: SharedFlow<Unit> = _recreateActivityEvent.asSharedFlow()


    val currentLanguage: StateFlow<String> = settingsManager.languageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "en"
        )


    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsManager.setUiTheme(theme)
        }
    }

    fun setCurrency(currencySymbol: String) {
        viewModelScope.launch {
            settingsManager.setCurrency(currencySymbol)
        }
    }

    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            settingsManager.setLanguage(languageCode)
            Log.d("SettingsViewModel", "Idioma '$languageCode' guardado en DataStore.")

            val application = getApplication<Application>()
            val appLocale = LocaleListCompat.forLanguageTags(languageCode)
            AppCompatDelegate.setApplicationLocales(appLocale)
            Log.d("SettingsViewModel", "AppCompatDelegate.setApplicationLocales llamado con: ${appLocale.toLanguageTags()}")
        }
    }

    // Function to handle logout using SessionManager
    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
            // You might want to emit an event here to signal the UI that logout is complete
            // if navigation isn't handled directly by the caller.
            Log.d("SettingsViewModel", "User session cleared.")
        }
    }

    companion object {
        fun provideFactory(
            application: Application,
            settingsManager: SettingsManager,
            sessionManager: SessionManager
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                        return SettingsViewModel(application, settingsManager, sessionManager) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}