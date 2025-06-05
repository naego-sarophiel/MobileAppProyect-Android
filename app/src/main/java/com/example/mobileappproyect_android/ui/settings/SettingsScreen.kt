package com.example.mobileappproyect_android.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobileappproyect_android.MainActivity // Necesitarás reiniciar esta
import com.example.mobileappproyect_android.data.AppTheme
import com.example.mobileappproyect_android.ui.theme.MobileAppProyectAndroidTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val currentTheme by settingsViewModel.currentTheme.collectAsStateWithLifecycle()
    val currentCurrency by settingsViewModel.currentCurrency.collectAsStateWithLifecycle()
    val currentLanguage by settingsViewModel.currentLanguage.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- Para el selector de tema ---
    var themeDropdownExpanded by remember { mutableStateOf(false) }
    val themeOptions = AppTheme.values()

    // --- Para el selector de moneda ---
    var currencyDropdownExpanded by remember { mutableStateOf(false) }
    val currencyOptions = listOf("$", "€", "£", "¥", "₹") // Ejemplo de monedas

    // --- Para el selector de idioma ---
    var languageDropdownExpanded by remember { mutableStateOf(false) }
    val languageOptions = mapOf("en" to "English", "es" to "Español", "fr" to "Français") // Código -> Nombre legible


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
            // Sección de Tema
            SettingSectionTitle("Appearance")
            SettingItemWithDropdown(
                label = "Theme",
                selectedValue = currentTheme.displayName,
                expanded = themeDropdownExpanded,
                onExpandedChange = { themeDropdownExpanded = it },
                options = themeOptions.map { it.displayName },
                onOptionSelected = { selectedThemeName ->
                    val selectedTheme = themeOptions.find { it.displayName == selectedThemeName }
                    selectedTheme?.let {
                        settingsViewModel.setTheme(it)
                        // Aplicar el tema inmediatamente (esto es un enfoque simplificado)
                        // Un enfoque más robusto está en MainActivity observando el flow del tema.
                        scope.launch {
                            applyAppTheme(context, it) // Función helper
                        }
                    }
                    themeDropdownExpanded = false
                }
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Sección de Moneda
            SettingSectionTitle("Preferences")
            SettingItemWithDropdown(
                label = "Currency Symbol",
                selectedValue = currentCurrency,
                expanded = currencyDropdownExpanded,
                onExpandedChange = { currencyDropdownExpanded = it },
                options = currencyOptions,
                onOptionSelected = { selectedCurrency ->
                    settingsViewModel.setCurrency(selectedCurrency)
                    currencyDropdownExpanded = false
                    // Nota: Necesitarás actualizar la UI donde se muestra la moneda para que refleje este cambio.
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sección de Idioma
            SettingItemWithDropdown(
                label = "Language",
                selectedValue = languageOptions[currentLanguage] ?: currentLanguage, // Muestra nombre legible
                expanded = languageDropdownExpanded,
                onExpandedChange = { languageDropdownExpanded = it },
                options = languageOptions.values.toList(),
                onOptionSelected = { selectedLanguageName ->
                    val selectedLangCode = languageOptions.entries.find { it.value == selectedLanguageName }?.key
                    selectedLangCode?.let {
                        settingsViewModel.setLanguage(it)
                        // IMPORTANTE: El cambio de idioma requiere reiniciar la Activity
                        applyAppLanguage(context, it) // Función helper para aplicar y reiniciar
                    }
                    languageDropdownExpanded = false
                }
            )


            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Sección de Cuenta
            SettingSectionTitle("Account")
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Filled.ExitToApp, contentDescription = "Logout Icon", modifier = Modifier.padding(end = 8.dp))
                Text("Logout")
            }
        }
    }
}

// Función helper para aplicar el tema (simplificado para Compose puro)
// La aplicación de tema a nivel de sistema es más compleja y se hace en MainActivity
fun applyAppTheme(context: Context, theme: AppTheme) {
    val mode = when (theme) {
        AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        AppTheme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
    AppCompatDelegate.setDefaultNightMode(mode) // Esto afecta a las Activities basadas en AppCompat
    // Para Compose puro, el tema se aplica en el Composable raíz (MainActivity)
    // basado en la preferencia guardada.
}


fun applyAppLanguage(context: Context, languageCode: String) {
    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
    AppCompatDelegate.setApplicationLocales(appLocale)

    // Para forzar la recreación de la Activity inmediatamente:
    // Esto asegura que los nuevos recursos de idioma se carguen.
    if (context is Activity) {
        context.recreate() // Llama a recreate() en la Activity actual
    }
    // Alternativamente, si quieres reiniciar la app completamente:
    // val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    // intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    // context.startActivity(intent)
    // if (context is Activity) {
    //     context.finishAffinity() // Cierra todas las activities de la app
    // }
    // System.exit(0) // No recomendado, pero es una forma drástica
}


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
                    .width(150.dp) // Ajusta el ancho según sea necesario
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
        SettingsScreen(onNavigateBack = {}, onLogout = {})
    }
}