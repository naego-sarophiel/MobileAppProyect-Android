package com.example.mobileappproyect_android.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobileappproyect_android.data.AppTheme
import com.example.mobileappproyect_android.data.SettingsManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager(application)

    val currentTheme: StateFlow<AppTheme> = settingsManager.uiThemeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.SYSTEM // Default or a sensible initial
        )

    val currentCurrency: StateFlow<String> = settingsManager.currencyFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "$"
        )

    val currentLanguage: StateFlow<String> = settingsManager.languageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "en" // Default language code
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
            // IMPORTANTE: Cambiar el idioma de la app requiere recrear la Activity
            // o usar APIs específicas de localización de Android.
            // Esto es un punto complejo que se abordará más adelante.
        }
    }
}