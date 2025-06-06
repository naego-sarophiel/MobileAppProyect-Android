package com.example.mobileappproyect_android.ui.settings

import android.app.Application
import android.util.Log // Para logging
import androidx.appcompat.app.AppCompatDelegate // Importante para cambiar el Locale
import androidx.core.os.LocaleListCompat // Importante para cambiar el Locale
import androidx.lifecycle.AndroidViewModel // Ya lo usas, está bien
import androidx.lifecycle.viewModelScope
import com.example.mobileappproyect_android.data.AppTheme
import com.example.mobileappproyect_android.data.SettingsManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager(application)

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

    // Esta función parece ser un marcador de posición.
    // La lógica principal estará en setLanguage.
    // Si la usas desde tu UI, asegúrate de que llame a setLanguage
    // o mueve la lógica de setLanguage aquí.
    // Por ahora, asumiré que tu UI llamará a setLanguage directamente.
    /*
    fun onLanguageChangedAndSaved() {
        // Esta función podría simplemente llamar a setLanguage si la UI la usa,
        // o ser eliminada si la UI llama a setLanguage(code) directamente.
        // Ejemplo: si tienes un botón "Guardar Idioma" separado después de seleccionar.
        // Si la selección de RadioButton llama directamente a setLanguage, esta no es necesaria.
        viewModelScope.launch {
            // Aquí no se está guardando ni cambiando el idioma realmente
            _recreateActivityEvent.emit(Unit)
        }
    }
    */

    val currentLanguage: StateFlow<String> = settingsManager.languageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "en" // Es buena práctica que el default aquí coincida con el de SettingsManager
        )


    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsManager.setUiTheme(theme)
            // Lógica de AppCompatDelegate.setDefaultNightMode() para el tema
            // normalmente va en MainActivity/MainApplication al observar el flow.
        }
    }

    fun setCurrency(currencySymbol: String) {
        viewModelScope.launch {
            settingsManager.setCurrency(currencySymbol)
        }
    }

    /**
     * Llamada cuando el usuario selecciona un nuevo idioma en la UI.
     * Guarda el idioma, actualiza el Locale de la aplicación y
     * emite un evento para que MainActivity se recree.
     */
    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            settingsManager.setLanguage(languageCode)
            Log.d("SettingsViewModel", "Idioma '$languageCode' guardado en DataStore.")

            val application = getApplication<Application>()
            val appLocale = LocaleListCompat.forLanguageTags(languageCode)
            AppCompatDelegate.setApplicationLocales(appLocale)
            Log.d("SettingsViewModel", "AppCompatDelegate.setApplicationLocales llamado con: ${appLocale.toLanguageTags()}")

            // YA NO SE EMITE _recreateActivityEvent DESDE AQUÍ
        }
    }
}