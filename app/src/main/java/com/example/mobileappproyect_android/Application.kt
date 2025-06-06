package com.example.mobileappproyect_android

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.mobileappproyect_android.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    lateinit var settingsManager: SettingsManager

    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager(this)

        // Es todavía una buena práctica establecer el Locale a nivel de aplicación
        // para componentes de AppCompat y consistencia general,
        // pero MainActivity.attachBaseContext será más directo para la UI de la Activity.
        applicationScope.launch {
            val savedLang = settingsManager.languageFlow.first()
            val appLocale = LocaleListCompat.forLanguageTags(savedLang)
            AppCompatDelegate.setApplicationLocales(appLocale)
            Log.d("MainApplication", "AppCompatDelegate.setApplicationLocales llamado con: ${appLocale.toLanguageTags()}")
        }
    }
}