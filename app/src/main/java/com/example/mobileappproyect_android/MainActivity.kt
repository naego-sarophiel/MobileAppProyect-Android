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
// Removed: import com.example.mobileappproyect_android.data.Subscription // No longer used directly here
import com.example.mobileappproyect_android.data.AppTheme
import com.example.mobileappproyect_android.data.SettingsManager
import com.example.mobileappproyect_android.ui.edit.EditSubscriptionScreen
import com.example.mobileappproyect_android.ui.home.HomeScreen
import com.example.mobileappproyect_android.ui.home.HomeViewModel
import com.example.mobileappproyect_android.ui.home.SubscriptionUiModel // IMPORT THIS
import com.example.mobileappproyect_android.ui.settings.SettingsScreen
import com.example.mobileappproyect_android.ui.theme.MobileAppProyectAndroidTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale
import android.content.res.Configuration

class MainActivity : ComponentActivity() {
    private lateinit var settingsManager: SettingsManager
    companion object {
        fun applySelectedLanguageToContext(baseContext: Context, languageCode: String): Context {
            val localeToApply = Locale(languageCode)
            Locale.setDefault(localeToApply)

            val currentConfig = baseContext.resources.configuration
            val newConfig = Configuration(currentConfig)
            newConfig.setLocale(localeToApply)

            Log.d("ContextWrapper", "Aplicando Locale '$languageCode' al contexto. Config original: ${currentConfig.locales[0]}, Nueva config: ${newConfig.locales[0]}")
            return baseContext.createConfigurationContext(newConfig)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val localSettingsManager = SettingsManager(newBase.applicationContext)
        val languageCodeToApply = runBlocking {
            localSettingsManager.languageFlow.first()
        }
        Log.d("MainActivity", "attachBaseContext - Idioma leído de SettingsManager: $languageCodeToApply")
        val contextWithUpdatedLanguage = applySelectedLanguageToContext(newBase, languageCodeToApply)
        super.attachBaseContext(contextWithUpdatedLanguage)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsManager = SettingsManager(applicationContext)

        // Language change monitoring
        lifecycleScope.launch {
            settingsManager.languageFlow.collect { savedLanguageCode ->
                val currentActivityLocale = resources.configuration.locales[0].toLanguageTag()
                if (savedLanguageCode != currentActivityLocale.take(2)) {
                    Log.d("MainActivity", "El idioma guardado ($savedLanguageCode) es diferente al de la actividad ($currentActivityLocale). Recreando...")
                    recreate()
                }
            }
        }

        // Theme monitoring
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
                    Column {
                        AppNavigation(settingsManager = settingsManager) // Pass settingsManager if needed by AppNavigation or screens within
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(settingsManager: SettingsManager) { // Added settingsManager parameter
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()
    // val settingsViewModel: SettingsViewModel = viewModel() // Consider if needed directly or if settingsManager is enough

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            // Assuming LoginScreen is independent or uses its own ViewModel
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                homeViewModel = homeViewModel,
                onSubscriptionClick = { subscriptionId ->
                    // homeViewModel.subscriptions.value is List<SubscriptionUiModel>
                    val subscriptionObject = homeViewModel.subscriptions.value.find { it.id == subscriptionId }
                    // homeViewModel.onSubscriptionSelectedForEdit now expects SubscriptionUiModel?
                    homeViewModel.onSubscriptionSelectedForEdit(subscriptionObject)
                    if (subscriptionObject != null) {
                        navController.navigate("edit_subscription")
                    } else {
                        Log.e("AppNavigation", "Error: Subscription with ID $subscriptionId not found.")
                        // Consider showing a Toast with a stringResource for user feedback
                    }
                },
                onAddSubscriptionClick = {
                    homeViewModel.clearSelectedSubscription() // This should be fine
                    navController.navigate("edit_subscription")
                },
                navController = navController
            )
        }
        composable("edit_subscription") {
            EditSubscriptionScreen(
                // homeViewModel.selectedSubscriptionForEdit is SubscriptionUiModel?
                subscriptionToEdit = homeViewModel.selectedSubscriptionForEdit,
                onSaveSubscription = { updatedSubscriptionUiModel -> // This lambda now provides SubscriptionUiModel
                    // Check if the subscription exists in the current list of UiModels
                    if (homeViewModel.subscriptions.value.any { it.id == updatedSubscriptionUiModel.id }) {
                        // homeViewModel.updateSubscription now expects SubscriptionUiModel
                        homeViewModel.updateSubscription(updatedSubscriptionUiModel)
                    } else {
                        // homeViewModel.addSubscription now expects SubscriptionUiModel
                        homeViewModel.addSubscription(updatedSubscriptionUiModel)
                    }
                    navController.popBackStack()
                },
                onNavigateBack = {
                    homeViewModel.clearSelectedSubscription()
                    navController.popBackStack()
                }
            )
        }
        composable("settings") {
            // If SettingsScreen takes SettingsViewModel or directly uses settingsManager
            SettingsScreen(
                // Pass settingsManager or instantiate SettingsViewModel here
                // settingsViewModel = settingsViewModel, // or
                // settingsManager = settingsManager,
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}


@Preview(showBackground = true, name = "App Navigation Preview")
@Composable
fun DefaultPreview() {
    MobileAppProyectAndroidTheme {
        val context = LocalContext.current
        val previewSettingsManager = remember { SettingsManager(context.applicationContext) }
        AppNavigation(settingsManager = previewSettingsManager) // Pass it here too
    }
}