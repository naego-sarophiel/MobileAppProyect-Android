package com.example.mobileappproyect_android

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobileappproyect_android.data.AppTheme
import com.example.mobileappproyect_android.data.SettingsManager
import com.example.mobileappproyect_android.ui.settings.SettingsScreen
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mobileappproyect_android.ui.theme.MobileAppProyectAndroidTheme
import com.example.mobileappproyect_android.ui.home.HomeScreen
import com.example.mobileappproyect_android.ui.home.HomeViewModel
import com.example.mobileappproyect_android.ui.edit.EditSubscriptionScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import android.widget.Toast
//import androidx.compose.ui.text.intl.Locale
import android.content.res.Configuration
import androidx.activity.viewModels
import com.example.mobileappproyect_android.ui.settings.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var settingsManager: SettingsManager
    companion object {
        fun applySelectedLanguageToContext(baseContext: Context, languageCode: String): Context {
            val localeToApply = Locale(languageCode)
            Locale.setDefault(localeToApply)

            val currentConfig = baseContext.resources.configuration
            // AQUÍ ES DONDE PODRÍA HABER ESTADO EL ERROR:
            val newConfig = Configuration(currentConfig) // Asegúrate de que esto sea android.content.res.Configuration
            newConfig.setLocale(localeToApply)
            // newConfig.setLayoutDirection(localeToApply)

            Log.d("ContextWrapper", "Aplicando Locale '$languageCode' al contexto. Config original: ${currentConfig.locales[0]}, Nueva config: ${newConfig.locales[0]}")
            return baseContext.createConfigurationContext(newConfig)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        // Este método se llama antes de onCreate().
        // Es el lugar ideal para configurar el Locale de la Activity.

        // 1. Inicializa SettingsManager aquí para poder leer el idioma.
        //    Nota: applicationContext está disponible en newBase.
        val localSettingsManager = SettingsManager(newBase.applicationContext)

        // 2. Lee el idioma guardado de forma síncrona.
        //    runBlocking es aceptable aquí porque attachBaseContext debe ser rápido y esto es crucial.
        val languageCodeToApply = runBlocking {
            localSettingsManager.languageFlow.first()
        }
        Log.d("MainActivity", "attachBaseContext - Idioma leído de SettingsManager: $languageCodeToApply")

        // 3. Crea un nuevo contexto con el idioma aplicado y pásalo a super.attachBaseContext.
        val contextWithUpdatedLanguage = applySelectedLanguageToContext(newBase, languageCodeToApply)
        super.attachBaseContext(contextWithUpdatedLanguage)

        // Opcional: También puedes llamar a AppCompatDelegate aquí si quieres ser redundante,
        // pero el cambio de contexto de la Activity es lo más directo.
        // val appLocale = LocaleListCompat.forLanguageTags(languageCodeToApply)
        // AppCompatDelegate.setApplicationLocales(appLocale) // Podría ser útil para otros componentes de AppCompat
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsManager = SettingsManager(applicationContext)

        val initialActivityLocale = resources.configuration.locales[0].toLanguageTag()
        Log.d("MainActivity", "onCreate - Idioma inicial de resources: $initialActivityLocale")

        lifecycleScope.launch {
            // Observar el flujo del idioma guardado
            settingsManager.languageFlow
                .collect { savedLanguageCode ->
                    Log.d("MainActivity", "Observado nuevo languageCode de SettingsManager: $savedLanguageCode")
                    val currentActivityLocale = resources.configuration.locales[0].toLanguageTag()
                    Log.d("MainActivity", "Idioma actual de la actividad: $currentActivityLocale")

                    // Solo recrear si el idioma guardado es diferente al idioma actual de la actividad
                    // y no es la configuración inicial donde podrían ser iguales tras una recreación previa.
                    // Esta condición puede necesitar ajuste para evitar bucles si el cambio de idioma no es perfecto.
                    if (savedLanguageCode != currentActivityLocale.take(2)) { // Compara solo el código base "en", "es"
                        Log.d("MainActivity", "El idioma guardado ($savedLanguageCode) es diferente al de la actividad ($currentActivityLocale). Recreando...")
                        recreate()
                    } else {
                        Log.d("MainActivity", "El idioma guardado ($savedLanguageCode) coincide con el de la actividad ($currentActivityLocale) o es configuración inicial. No se recrea por esta vía.")
                    }
                }
        }

        // --- Lógica del tema (puede quedarse) ---
        lifecycleScope.launch {
            settingsManager.uiThemeFlow.collect { theme ->
                val mode = when (theme) {
                    AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    AppTheme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }

        setContent {
            val currentThemeSetting by settingsManager.uiThemeFlow.collectAsStateWithLifecycle(initialValue = AppTheme.SYSTEM)

            MobileAppProyectAndroidTheme(
                darkTheme = when (currentThemeSetting) {
                    AppTheme.LIGHT -> false
                    AppTheme.DARK -> true
                    AppTheme.SYSTEM -> isSystemInDarkTheme()
                }
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column { // Envuelve en una columna si tienes más elementos
                        AppNavigation(settingsManager = settingsManager)
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(settingsManager: SettingsManager) {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
                // Asumo que LoginScreen internamente usa stringResources para sus textos
            )
        }
        composable("home") {
            HomeScreen(
                homeViewModel = homeViewModel,
                onSubscriptionClick = { subscriptionId ->
                    val subscriptionObject = homeViewModel.subscriptions.value.find { it.id == subscriptionId }
                    if (subscriptionObject != null) {
                        homeViewModel.onSubscriptionSelectedForEdit(subscriptionObject)
                        navController.navigate("edit_subscription")
                    } else {
                        // Este es un mensaje de error/log, podría quedar como está o usar un string si se muestra al usuario
                        Log.e("AppNavigation", "Error: Subscription with ID $subscriptionId not found.")
                        // Si fuera un Toast:
                        // Toast.makeText(LocalContext.current, stringResource(R.string.error_subscription_not_found, subscriptionId), Toast.LENGTH_SHORT).show()
                    }
                },
                onAddSubscriptionClick = {
                    homeViewModel.clearSelectedSubscription()
                    navController.navigate("edit_subscription")
                },
                navController = navController
                // Asumo que HomeScreen y sus componentes internos (como el menú de ordenación) ya usan stringResources
            )
        }
        composable("edit_subscription") {
            EditSubscriptionScreen(
                subscriptionToEdit = homeViewModel.selectedSubscriptionForEdit,
                onSaveSubscription = { updatedSubscription ->
                    if (homeViewModel.subscriptions.value.any { it.id == updatedSubscription.id }) {
                        homeViewModel.updateSubscription(updatedSubscription)
                    } else {
                        homeViewModel.addSubscription(updatedSubscription)
                    }
                    navController.popBackStack()
                },
                onNavigateBack = {
                    Log.d("EditScreen", "onNavigateBack: Clearing selection and popping backstack") // Log, puede quedar
                    homeViewModel.clearSelectedSubscription()
                    navController.popBackStack()
                    Log.d("EditScreen", "onNavigateBack: PopBackStack called") // Log, puede quedar
                }
                // Asumo que EditSubscriptionScreen internamente usa stringResources para sus etiquetas y botones
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
                // Asumo que SettingsScreen internamente usa stringResources
            )
        }
    }
}


@Preview(showBackground = true, name = "App Navigation Preview")
@Composable
fun DefaultPreview() {
    MobileAppProyectAndroidTheme {
        val context = LocalContext.current // No necesitas obtener el contexto aquí si no lo usas directamente
        // Pasamos una nueva instancia para la preview, no tiene textos visibles directos aquí
        val previewSettingsManager = remember { SettingsManager(context.applicationContext) }
        AppNavigation(settingsManager = previewSettingsManager)
    }
}