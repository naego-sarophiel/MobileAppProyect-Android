package com.example.mobileappproyect_android.ui.settings

// import android.app.Activity // NO SE USA AQUÍ para la lógica de idioma
// import android.content.Context // NO SE USA AQUÍ para la lógica de idioma
import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate // Se usa en applyAppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
// Corrected import for ArrowBack based on common Material Icons usage
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // Se usa para applyAppTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// import androidx.core.os.LocaleListCompat // NO SE USA DIRECTAMENTE AQUÍ
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mobileappproyect_android.R
import com.example.mobileappproyect_android.data.AppTheme
import com.example.mobileappproyect_android.data.SessionManager
import com.example.mobileappproyect_android.data.SettingsManager
import com.example.mobileappproyect_android.ui.theme.MobileAppProyectAndroidTheme
import kotlinx.coroutines.launch // Se usa para applyAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController, // Para navegación
    settingsViewModel: SettingsViewModel = viewModel(), // Correcto para AndroidViewModel
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val currentTheme by settingsViewModel.currentTheme.collectAsStateWithLifecycle()
    val currentCurrency by settingsViewModel.currentCurrency.collectAsStateWithLifecycle()
    val currentLanguage by settingsViewModel.currentLanguage.collectAsStateWithLifecycle()

    val context = LocalContext.current // Necesario para applyAppTheme
    val scope = rememberCoroutineScope() // Necesario para applyAppTheme

    // --- Para el selector de tema ---
    var themeDropdownExpanded by remember { mutableStateOf(false) }
    val themeOptions = AppTheme.values()
    val lightThemeName = stringResource(R.string.settings_theme_light)
    val darkThemeName = stringResource(R.string.settings_theme_dark)
    val systemThemeName = stringResource(R.string.settings_theme_system)
    val themeDisplayNames = remember(lightThemeName, darkThemeName, systemThemeName) {
        themeOptions.map { theme ->
            when (theme) {
                AppTheme.LIGHT -> lightThemeName
                AppTheme.DARK -> darkThemeName
                AppTheme.SYSTEM -> systemThemeName
            }
        }
    }
    val currentThemeDisplayName = when (currentTheme) {
        AppTheme.LIGHT -> lightThemeName
        AppTheme.DARK -> darkThemeName
        AppTheme.SYSTEM -> systemThemeName
    }

    // --- Para el selector de moneda ---
    var currencyDropdownExpanded by remember { mutableStateOf(false) }
    val currencyOptions = listOf("$", "€", "£", "¥", "₹") // Deberían ser recursos de string si necesitas localizarlos

    // --- Para el selector de idioma ---
    var languageDropdownExpanded by remember { mutableStateOf(false) }
    val englishLanguageName = stringResource(R.string.settings_language_english)
    val spanishLanguageName = stringResource(R.string.settings_language_spanish)
    val languageOptions = remember(englishLanguageName, spanishLanguageName) {
        mapOf(
            "en" to englishLanguageName,
            "es" to spanishLanguageName
            // Puedes añadir más idiomas aquí
        )
    }
    val currentLanguageDisplayName = languageOptions[currentLanguage] ?: currentLanguage


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            // Assuming AutoMirrored is what you want for back arrow
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_navigate_back_description)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingSectionTitle(stringResource(R.string.settings_section_appearance))
            SettingItemWithDropdown(
                label = stringResource(R.string.settings_theme_title),
                selectedValue = currentThemeDisplayName,
                expanded = themeDropdownExpanded,
                onExpandedChange = { themeDropdownExpanded = it },
                options = themeDisplayNames,
                onOptionSelected = { selectedThemeDisplayName ->
                    val selectedIndex = themeDisplayNames.indexOf(selectedThemeDisplayName)
                    if (selectedIndex != -1) {
                        val selectedTheme = themeOptions[selectedIndex]
                        settingsViewModel.setTheme(selectedTheme)
                        // La aplicación del tema visual (modo noche) generalmente se observa
                        // en MainActivity o MainApplication para actualizar AppCompatDelegate.
                        // Si applyAppTheme solo cambia el modo noche, está bien aquí por ahora.
                        scope.launch { // Esto está bien si applyAppTheme es una suspend function o necesita un scope
                            applyAppTheme(context, selectedTheme)
                        }
                    }
                    themeDropdownExpanded = false
                }
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            SettingSectionTitle(stringResource(R.string.settings_section_preferences))
            SettingItemWithDropdown(
                label = stringResource(R.string.settings_currency_title),
                selectedValue = currentCurrency,
                expanded = currencyDropdownExpanded,
                onExpandedChange = { currencyDropdownExpanded = it },
                options = currencyOptions,
                onOptionSelected = { selectedCurrency ->
                    settingsViewModel.setCurrency(selectedCurrency)
                    currencyDropdownExpanded = false
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingItemWithDropdown(
                label = stringResource(R.string.settings_language_title),
                selectedValue = currentLanguageDisplayName,
                expanded = languageDropdownExpanded,
                onExpandedChange = { languageDropdownExpanded = it },
                options = languageOptions.values.toList(), // Muestra los nombres de los idiomas
                onOptionSelected = { selectedLanguageDisplayName -> // El usuario selecciona el nombre del idioma
                    // Encuentra el código del idioma basado en el nombre seleccionado
                    val selectedLangCode = languageOptions.entries.find { it.value == selectedLanguageDisplayName }?.key
                    selectedLangCode?.let { code ->
                        // Llama a la función del ViewModel que ahora maneja todo
                        settingsViewModel.setLanguage(code)
                    }
                    languageDropdownExpanded = false
                }
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            SettingSectionTitle(stringResource(R.string.settings_section_account))
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(
                    Icons.Filled.ExitToApp,
                    contentDescription = stringResource(R.string.settings_logout_icon_description),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(stringResource(R.string.settings_logout_button))
            }
        }
    }
}

// Esta función aplica el tema (modo noche). Puede quedarse aquí o moverse
// si la lógica de aplicar tema se centraliza más (ej. observando el flow en MainActivity).
fun applyAppTheme(context: Context, theme: AppTheme) {
    val mode = when (theme) {
        AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        AppTheme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
    AppCompatDelegate.setDefaultNightMode(mode)
}

// ESTA FUNCIÓN YA NO ES NECESARIA AQUÍ, SU LÓGICA ESTÁ EN SettingsViewModel
/*
fun applyAppLanguage(context: Context, languageCode: String) {
    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
    AppCompatDelegate.setApplicationLocales(appLocale)
    if (context is Activity) {
        context.recreate()
    }
}
*/

@Composable
fun SettingSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingItemWithDropdown(
    label: String,
    selectedValue: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange,
        ) {
            OutlinedTextField(
                value = selectedValue,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .width(150.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                options.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            onOptionSelected(selectionOption)
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Settings Screen Light")
@Composable
fun SettingsScreenPreview() {
    MobileAppProyectAndroidTheme {
        // Create a NavController instance for the preview
        val previewNavController = NavController(LocalContext.current)

        // It's also good practice to provide a preview version of your ViewModel
        // if its initialization is complex or requires specific dependencies.
        // For SettingsViewModel, since it's an AndroidViewModel,
        // it needs an Application instance.
        val application = LocalContext.current.applicationContext as Application
        val previewSettingsManager = SettingsManager(application) // If SettingsManager is simple
        val previewSessionManager = SessionManager(application)   // If SessionManager is simple

        // If SettingsViewModel has a default constructor or its factory can be easily used:
        // val previewViewModel: SettingsViewModel = viewModel() // This might work if defaults are okay

        // Or, more explicitly for preview if factory is needed:
        // val previewViewModel: SettingsViewModel = viewModel(
        //    factory = SettingsViewModel.provideFactory(
        //        application,
        //        previewSettingsManager,
        //        previewSessionManager
        //    )
        // )
        // For simplicity, if the default viewModel() works or you have a simpler preview setup:

        SettingsScreen(
            navController = previewNavController, // <<< PASS THE PREVIEW NAVCONTROLLER
            // settingsViewModel = previewViewModel, // Pass a preview ViewModel if needed
            onNavigateBack = {},
            onLogout = {}
        )
    }
}

// Optional: If your SettingsViewModel is complex to set up for previews,
// you might create a simpler fake/dummy version or a helper function.
// For instance, if SettingsViewModel doesn't rely heavily on complex constructor
// parameters for its basic preview state, the default `viewModel()` might suffice in preview.
// However, SettingsViewModel requires Application, SettingsManager, and SessionManager through its factory.

// A more robust preview providing the ViewModel explicitly:
@Preview(showBackground = true, name = "Settings Screen Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsScreenDarkPreview() {
    MobileAppProyectAndroidTheme(darkTheme = true) {
        val context = LocalContext.current
        val application = context.applicationContext as Application
        val previewNavController = NavController(context)
        val previewSettingsManager = SettingsManager(application)
        val previewSessionManager = SessionManager(application)

        // Instantiate SettingsViewModel using its factory for the preview
        val previewSettingsViewModel = SettingsViewModel(
            application = application,
            settingsManager = previewSettingsManager,
            sessionManager = previewSessionManager
        ) // Assuming its factory is simple or using direct instantiation for preview if appropriate

        SettingsScreen(
            navController = previewNavController,
            settingsViewModel = previewSettingsViewModel,
            onNavigateBack = {},
            onLogout = {}
        )
    }
}