package com.example.mobileappproyect_android

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource // ¡IMPORTANTE!
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- ViewModel (for managing UI state and logic) ---
class LoginViewModel : ViewModel() {
    var username by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var loginInProgress by mutableStateOf(false)
        private set

    var loginError by mutableStateOf<String?>(null) // Este podría seguir siendo un String directo si viene de lógica interna
        private set                                    // O podrías mapear códigos de error a R.string IDs.

    fun onUsernameChange(newUsername: String) {
        username = newUsername
        loginError = null
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
        loginError = null
    }

    // Para los mensajes de error, tienes dos opciones:
    // 1. Dejar que el ViewModel establezca el texto literal del error (como está ahora).
    // 2. Hacer que el ViewModel establezca un ID de recurso de string o un código de error,
    // y que el Composable use stringResource() para mostrar el mensaje localizado.
    // La opción 1 es más simple para errores que no necesitan localización compleja.
    // La opción 2 es mejor para la localización completa.
    // Por ahora, mantendré la lógica actual del ViewModel para los mensajes de error,
    // pero si estos mensajes necesitaran ser localizados, deberías cambiar el ViewModel
    // para que maneje IDs de error o de string.
    fun onLoginClick(onLoginSuccess: () -> Unit) {
        if (username.isBlank() || password.isBlank()) {
            // Este mensaje podría venir de stringResource si quisieras localizarlo
            // loginError = context.getString(R.string.login_error_empty_fields) // Necesitarías 'context'
            loginError = "Username and password cannot be empty." // Dejamos como está por simplicidad del ViewModel
            return
        }

        loginInProgress = true
        loginError = null

        kotlinx.coroutines.MainScope().launch {
            delay(2000)
            if (username == "testuser" && password == "password123") {
                onLoginSuccess()
            } else {
                // loginError = context.getString(R.string.login_error_invalid_credentials)
                loginError = "Invalid username or password." // Dejamos como está
            }
            loginInProgress = false
        }
    }
}


// --- Login Screen Composable ---
@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = viewModel(),
    onLoginSuccess: () -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.login_title_screen), // Usar stringResource
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = loginViewModel.username,
            onValueChange = { loginViewModel.onUsernameChange(it) },
            label = { Text(stringResource(R.string.login_username_label)) }, // Usar stringResource
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = loginViewModel.loginError != null && loginViewModel.username.isBlank()
            // Podrías añadir un placeholder también con stringResource
            // placeholder = { Text(stringResource(R.string.login_username_placeholder)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = loginViewModel.password,
            onValueChange = { loginViewModel.onPasswordChange(it) },
            label = { Text(stringResource(R.string.login_password_label)) }, // Usar stringResource
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val description = if (showPassword) {
                    stringResource(R.string.login_password_hide_description) // Usar stringResource
                } else {
                    stringResource(R.string.login_password_show_description) // Usar stringResource
                }
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            isError = loginViewModel.loginError != null && loginViewModel.password.isBlank()
            // placeholder = { Text(stringResource(R.string.login_password_placeholder)) }
        )

        if (loginViewModel.loginError != null) {
            Text(
                // Como loginError viene directamente del ViewModel como String, lo usamos tal cual.
                // Si el ViewModel proporcionara un ID de error, harías:
                // text = stringResource(id = loginViewModel.loginErrorStringId),
                text = loginViewModel.loginError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                loginViewModel.onLoginClick(onLoginSuccess)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loginViewModel.loginInProgress
        ) {
            if (loginViewModel.loginInProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(R.string.login_button_text_login)) // Usar stringResource
            }
        }
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    // Si tuvieras un tema general para la app, lo envolverías aquí:
    // com.example.mobileappproyect_android.ui.theme.MobileAppProyectAndroidTheme {
    LoginScreen(onLoginSuccess = { /* Handle preview success */ })
    // }
}