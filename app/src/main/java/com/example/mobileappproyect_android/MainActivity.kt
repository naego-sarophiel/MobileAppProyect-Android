package com.example.mobileappproyect_android // Your project's package name

import android.os.Bundle
import android.util.Log
// import android.widget.Toast // Uncomment if you want to use Toast for simple feedback
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.appcompat.app.AppCompatDelegate // Para el tema
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle // Para observar el tema
import com.example.mobileappproyect_android.data.AppTheme
import com.example.mobileappproyect_android.data.SettingsManager
import com.example.mobileappproyect_android.ui.settings.SettingsScreen // Importar SettingsScreen
import com.example.mobileappproyect_android.ui.settings.SettingsViewModel
// import androidx.compose.ui.platform.LocalContext // Uncomment for Toast
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel // Import for viewModel()
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
// Make sure you have a theme defined, or remove this import and the Theme wrapper
import com.example.mobileappproyect_android.ui.theme.MobileAppProyectAndroidTheme // Your project's theme

// Import your LoginScreen, HomeScreen, and HomeViewModel
import com.example.mobileappproyect_android.LoginScreen // Assuming LoginScreen.kt is in this package
import com.example.mobileappproyect_android.ui.home.HomeScreen // Assuming HomeScreen.kt is in ui.home
import com.example.mobileappproyect_android.ui.home.HomeViewModel // Assuming HomeViewModel.kt is in ui.home
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.mobileappproyect_android.ui.edit.EditSubscriptionScreen // Import EditScreen
import androidx.compose.runtime.getValue // Importación para el delegado 'by'
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch // Importación para CoroutineScope.launch

class MainActivity : ComponentActivity() {
    private lateinit var settingsManager: SettingsManager // Para observar el tema
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsManager = SettingsManager(applicationContext)

        // Observar el flujo del tema para aplicarlo dinámicamente
        // Esto es un enfoque. Otro es que SettingsScreen lo maneje con AppCompatDelegate
        // y la Activity se recree si es necesario, o que el Composable raíz lea el tema.
        // Este ejemplo aplica el tema directamente a AppCompatDelegate.
        lifecycleScope.launch { // Necesitas 'androidx.lifecycle:lifecycle-runtime-ktx'
            settingsManager.uiThemeFlow.collect { theme ->
                val mode = when (theme) {
                    AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    AppTheme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }
        // Aplicar el idioma guardado al inicio
        // Esto debe hacerse antes de setContent si afecta a los recursos iniciales.
        // El cambio de idioma a través de AppCompatDelegate.setApplicationLocales
        // ya se encarga de reiniciar la Activity.
        // Si tienes una lógica más compleja, puedes necesitar un BaseActivity.
        // Por ahora, el cambio desde SettingsScreen que reinicia la Activity es suficiente.

        setContent {
            // Recolectar el estado del tema para pasarlo al Composable del tema de Compose
            val currentThemeSetting by settingsManager.uiThemeFlow.collectAsStateWithLifecycle(initialValue = AppTheme.SYSTEM)

            MobileAppProyectAndroidTheme(
                darkTheme = when (currentThemeSetting) {
                    AppTheme.LIGHT -> false
                    AppTheme.DARK -> true
                    AppTheme.SYSTEM -> isSystemInDarkTheme() // Usa el sistema si es la opción
                }
                // dynamicColor: Boolean = true, // Si usas color dinámico
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(settingsManager = settingsManager) // Pasa settingsManager si es necesario
                }
            }
        }
    }
}

@Composable
fun AppNavigation(settingsManager: SettingsManager) { // settingsManager opcional aquí, depende de dónde lo necesites
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
            )
        }
        composable("home") {
            // homeViewModel is already available if hoisted, or use viewModel() here again
            HomeScreen(
                homeViewModel = homeViewModel,
                onSubscriptionClick = { subscriptionId -> // subscriptionId es el String del NavItem
                    val subscriptionObject = homeViewModel.subscriptions.value.find { it.id == subscriptionId }
                    if (subscriptionObject != null) {
                        // --- CORRECCIÓN AQUÍ ---
                        // Si onSubscriptionSelectedForEdit ahora espera un Subscription?,
                        // entonces pasa el objeto `subscriptionObject` que encontraste.
                        homeViewModel.onSubscriptionSelectedForEdit(subscriptionObject)
                        navController.navigate("edit_subscription")
                    } else {
                        // Opcional: Manejar el caso donde no se encuentra la suscripción,
                        // aunque esto no debería ocurrir si el ID viene de un elemento de la lista.
                        println("Error: Subscription with ID $subscriptionId not found.")
                    }
                },
                onAddSubscriptionClick = {
                    homeViewModel.clearSelectedSubscription() // Ensure no old selection
                    navController.navigate("edit_subscription") // Navigate for adding new
                },
                navController = navController
            )
        }
        composable("edit_subscription") {
            // EditSubscriptionScreen ahora toma el objeto Subscription? directamente
            // y HomeViewModel.selectedSubscriptionForEdit es un Subscription?
            EditSubscriptionScreen(
                subscriptionToEdit = homeViewModel.selectedSubscriptionForEdit,
                onSaveSubscription = { updatedSubscription ->
                    // Aquí decides si llamas a updateSubscription o addSubscription
                    // basado en si updatedSubscription.id ya existe o es nuevo.
                    // O podrías tener lógica separada en HomeViewModel para esto.
                    if (homeViewModel.subscriptions.value.any { it.id == updatedSubscription.id }) {
                        homeViewModel.updateSubscription(updatedSubscription)
                    } else {
                        homeViewModel.addSubscription(updatedSubscription)
                    }
                    navController.popBackStack()
                },
                onNavigateBack = {
                    Log.d("EditScreen", "onNavigateBack: Clearing selection and popping backstack")
                    homeViewModel.clearSelectedSubscription()
                    navController.popBackStack()
                    Log.d("EditScreen", "onNavigateBack: PopBackStack called")
                }
            )
        }
        composable("settings") { // Nueva ruta para la configuración
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    // Aquí puedes limpiar cualquier estado de sesión si es necesario
                    // (ej. limpiar tokens guardados)
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true } // Limpia el backstack hasta home
                        // O popUpTo(navController.graph.startDestinationId) { inclusive = true } para ir al inicio del grafo
                    }
                }
            )
        }
    }
}

// The simple HomeScreen for previewing individual components might still be useful,
// but the one used in navigation will now be the more complex one from ui.home.
// You can keep this or remove it if not needed for isolated previews.
@Composable
fun SimplePreviewHomeScreen() { // Renamed to avoid conflict if you keep it
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Welcome to the Home Screen! (Simple Preview)")
    }
}


@Preview(showBackground = true, name = "App Navigation Preview")
@Composable
fun DefaultPreview() {
    MobileAppProyectAndroidTheme {
        // You can preview AppNavigation or a specific screen
        val context = LocalContext.current
        val previewSettingsManager = remember { SettingsManager(context.applicationContext) }

        // Ahora pasamos la instancia de prueba a AppNavigation
        AppNavigation(settingsManager = previewSettingsManager)
        // Or, to preview the detailed HomeScreen directly with its ViewModel:
        // val homeViewModel: HomeViewModel = viewModel()
        // HomeScreen(homeViewModel = homeViewModel)
    }
}