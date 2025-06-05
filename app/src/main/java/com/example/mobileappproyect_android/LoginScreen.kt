package com.example.mobileappproyect_android // Your project's package name

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay // For simulation
import kotlinx.coroutines.launch // For simulation

// --- ViewModel (for managing UI state and logic) ---
class LoginViewModel : ViewModel() {
    var username by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var loginInProgress by mutableStateOf(false)
        private set

    var loginError by mutableStateOf<String?>(null)
        private set

    fun onUsernameChange(newUsername: String) {
        username = newUsername
        loginError = null // Clear error on input change
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
        loginError = null // Clear error on input change
    }

    fun onLoginClick(onLoginSuccess: () -> Unit) {
        if (username.isBlank() || password.isBlank()) {
            loginError = "Username and password cannot be empty."
            return
        }

        loginInProgress = true
        loginError = null

        // **IMPORTANT:** In a real app, you would make an API call here.
        // For now, we'll simulate a delay and a simple check.
        // Replace this with your actual API call logic.
        kotlinx.coroutines.MainScope().launch { // Use a coroutine scope
            delay(2000) // Simulate network delay
            if (username == "testuser" && password == "password123") {
                onLoginSuccess()
            } else {
                loginError = "Invalid username or password."
            }
            loginInProgress = false
        }
    }
}


// --- Login Screen Composable ---
@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = viewModel(),
    onLoginSuccess: () -> Unit // Callback for successful login
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
            text = "Login",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = loginViewModel.username,
            onValueChange = { loginViewModel.onUsernameChange(it) },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = loginViewModel.loginError != null && loginViewModel.username.isBlank()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = loginViewModel.password,
            onValueChange = { loginViewModel.onPasswordChange(it) },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val description = if (showPassword) "Hide password" else "Show password"
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(imageVector = image, description)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            isError = loginViewModel.loginError != null && loginViewModel.password.isBlank()
        )

        if (loginViewModel.loginError != null) {
            Text(
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
                Text("Login")
            }
        }
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    // If you have a theme defined in your project (e.g., in ui.theme/Theme.kt)
    // com.example.mobileappproyect_android.ui.theme.MobileAppProyect_AndroidTheme {
    LoginScreen(onLoginSuccess = { /* Handle preview success */ })
    // }
}