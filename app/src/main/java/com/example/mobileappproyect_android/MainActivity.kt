package com.example.mobileappproyect_android

import android.app.Application // Needed for ViewModelProvider.Factory
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels // For by viewModels()
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box // For loading indicator
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator // For loading
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment // For loading indicator
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel // For getting ViewModels in Composables
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mobileappproyect_android.data.AppTheme
import com.example.mobileappproyect_android.data.SessionManager // Import SessionManager
import com.example.mobileappproyect_android.data.SettingsManager
import com.example.mobileappproyect_android.ui.edit.EditSubscriptionScreen // Corrected import
import com.example.mobileappproyect_android.ui.home.HomeScreen
import com.example.mobileappproyect_android.ui.home.HomeViewModel
import com.example.mobileappproyect_android.ui.settings.SettingsScreen
import com.example.mobileappproyect_android.ui.settings.SettingsViewModel
import com.example.mobileappproyect_android.ui.theme.MobileAppProyectAndroidTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale
import android.content.res.Configuration
import androidx.lifecycle.viewModelScope
import com.example.mobileappproyect_android.ui.edit.EditSubscriptionViewModel

// ViewModel to hold the initial route logic
class MainViewModel(sessionManager: SessionManager) : ViewModel() {
    val initialRoute: StateFlow<String?> = sessionManager.userSessionFlow
        .map { userSession ->
            if (userSession.rememberMe && userSession.userId != null) {
                Log.d("MainViewModel", "User session found, navigating to home. UserID: ${userSession.userId}")
                "home" // Navigate to home if session is remembered and valid
            } else {
                Log.d("MainViewModel", "No valid user session found, navigating to login.")
                "login" // Default to login
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null // Start with null, show loading until determined
        )
}

class MainActivity : ComponentActivity() {
    private lateinit var settingsManager: SettingsManager
    private lateinit var sessionManager: SessionManager // Make SessionManager accessible

    // Initialize MainViewModel using by viewModels with a factory
    private val mainViewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                    return MainViewModel(sessionManager) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }

    companion object {
        fun applySelectedLanguageToContext(baseContext: Context, languageCode: String): Context {
            val localeToApply = Locale(languageCode)
            Locale.setDefault(localeToApply)
            val currentConfig = baseContext.resources.configuration
            val newConfig = Configuration(currentConfig)
            newConfig.setLocale(localeToApply)
            Log.d("ContextWrapper", "Applying Locale '$languageCode'. Original: ${currentConfig.locales[0]}, New: ${newConfig.locales[0]}")
            return baseContext.createConfigurationContext(newConfig)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        // Initialize settingsManager here as it's needed for language before super.attachBaseContext
        val localSettingsManager = SettingsManager(newBase.applicationContext)
        val languageCodeToApply = runBlocking { localSettingsManager.languageFlow.first() }
        Log.d("MainActivity", "attachBaseContext - Language from SettingsManager: $languageCodeToApply")
        val contextWithUpdatedLanguage = applySelectedLanguageToContext(newBase, languageCodeToApply)
        super.attachBaseContext(contextWithUpdatedLanguage)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsManager = SettingsManager(applicationContext) // Initialize for the rest of the activity
        sessionManager = SessionManager(applicationContext) // Initialize SessionManager

        // Language change monitoring
        lifecycleScope.launch {
            settingsManager.languageFlow.collect { savedLanguageCode ->
                val currentActivityLocale = resources.configuration.locales[0].toLanguageTag()
                if (savedLanguageCode != currentActivityLocale.take(2)) {
                    Log.d("MainActivity", "Saved language ($savedLanguageCode) differs from activity ($currentActivityLocale). Recreating...")
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
            val initialRoute by mainViewModel.initialRoute.collectAsStateWithLifecycle()

            MobileAppProyectAndroidTheme( // Pass settingsManager if your theme needs it
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
                    if (initialRoute == null) {
                        // Show a loading indicator while determining the route
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        // Pass sessionManager to AppNavigation if any composables down the line need it directly
                        // (though typically they'd get it via their ViewModels)
                        AppNavigation(
                            settingsManager = settingsManager,
                            sessionManager = sessionManager, // Pass SessionManager
                            startDestination = initialRoute!!
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    settingsManager: SettingsManager,
    sessionManager: SessionManager, // Receive SessionManager
    startDestination: String
) {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()

    // Factory for EditSubscriptionViewModel
    val editSubscriptionViewModelFactory = remember { // Remember the factory
        EditSubscriptionViewModel.Factory(
            navController.context.applicationContext as Application,
            homeViewModel
        )
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                // LoginViewModel is created by default inside LoginScreen
                onLoginSuccessNavigation = { userId ->
                    Log.d("AppNavigation", "Login successful, User ID: $userId, navigating to home.")
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true } // Clear login from back stack
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                homeViewModel = homeViewModel,
                onSubscriptionClick = { subscriptionUiModel -> // Assuming HomeScreen now passes the full UiModel
                    homeViewModel.onSubscriptionSelectedForEdit(subscriptionUiModel)
                    navController.navigate("edit_subscription/${subscriptionUiModel.id}") // Navigate with ID
                },
                onAddSubscriptionClick = {
                    homeViewModel.clearSelectedSubscription()
                    navController.navigate("edit_subscription/new") // Use "new" or a placeholder for new subs
                },
                navController = navController
            )
        }
        composable("edit_subscription/{subscriptionId}") { backStackEntry ->
            // The EditSubscriptionViewModel is created with the factory,
            // which will handle extracting subscriptionId via SavedStateHandle.
            val editSubscriptionViewModel: EditSubscriptionViewModel = viewModel(
                factory = editSubscriptionViewModelFactory
            )
            EditSubscriptionScreen(
                navController = navController,
                editSubscriptionViewModel = editSubscriptionViewModel
                // subscriptionId is handled by the ViewModel's factory and SavedStateHandle
            )
        }
        composable("settings") {
            val settingsViewModel: SettingsViewModel = viewModel {
                SettingsViewModel.provideFactory(
                    application = navController.context.applicationContext as Application,
                    settingsManager = settingsManager,
                    sessionManager = sessionManager // Pass SessionManager to SettingsViewModel factory
                ).create(SettingsViewModel::class.java) // Create with KClass
            }
            SettingsScreen(
                // Pass settingsManager if needed, or rely on SettingsViewModel
                navController = navController, // Pass NavController
                settingsViewModel = settingsViewModel, // Pass SettingsViewModel
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    // Logout logic is handled by settingsViewModel.logout()
                    // Navigation after logout is handled within settingsViewModel.logout() or by observing a state.
                    // For now, assume settingsViewModel.logout() will also trigger navigation.
                    // If not, you might navigate here after calling logout.
                    // For example:
                    // settingsViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("home") {
                            inclusive = true
                            saveState = false // Don't save state of home/login when logging out
                        }
                        launchSingleTop = true // Avoid multiple login screens
                        restoreState = false // Don't restore state for login
                    }
                }
            )
        }
    }
}


@Preview(showBackground = true, name = "App Navigation Preview - Login Start")
@Composable
fun DefaultPreviewLogin() {
    MobileAppProyectAndroidTheme {
        val context = LocalContext.current
        val previewSettingsManager = remember { SettingsManager(context.applicationContext) }
        val previewSessionManager = remember { SessionManager(context.applicationContext) }
        AppNavigation(
            settingsManager = previewSettingsManager,
            sessionManager = previewSessionManager,
            startDestination = "login"
        )
    }
}

@Preview(showBackground = true, name = "App Navigation Preview - Home Start")
@Composable
fun DefaultPreviewHome() {
    MobileAppProyectAndroidTheme {
        val context = LocalContext.current
        val previewSettingsManager = remember { SettingsManager(context.applicationContext) }
        val previewSessionManager = remember { SessionManager(context.applicationContext) }
        // To simulate being logged in for preview, you'd ideally have a way to
        // set a dummy session in previewSessionManager or mock MainViewModel's initialRoute.
        // For simplicity, we just set startDestination.
        AppNavigation(
            settingsManager = previewSettingsManager,
            sessionManager = previewSessionManager,
            startDestination = "home"
        )
    }
}