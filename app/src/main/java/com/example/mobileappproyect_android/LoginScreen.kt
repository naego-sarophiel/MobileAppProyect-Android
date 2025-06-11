package com.example.mobileappproyect_android

import android.app.Application
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import com.example.mobileappproyect_android.ui.login.LoginViewModel

// --- Login Screen Composable ---
@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = viewModel(),
    onLoginSuccessNavigation: (userId: String) -> Unit // Updated to take userId
) {
    var showPassword by remember { mutableStateOf(false) }

    // Collect the stored email and remember me status to initialize UI
    // This ensures that if the user re-opens the app, these values are pre-filled
    // before the viewModel's init block might fully complete its first pass from DataStore.
    // However, the viewModel's init block is the primary source for setting initial state.
    // This is more for immediate UI reflection if needed.
    //val initialRememberMe by loginViewModel.storedRememberMeStatus.collectAsState()
    //val initialEmail by loginViewModel.storedRememberedEmail.collectAsState()

    // Use LaunchedEffect to set initial values in ViewModel once from DataStore flow
    // This helps avoid potential recomposition loops if directly assigning in Composable body
//    LaunchedEffect(initialRememberMe, initialEmail) {
//        if (loginViewModel.email.isEmpty() && initialEmail != null && initialRememberMe) {
//            loginViewModel.onEmailChange(initialEmail ?: "")
//        }
//        // ViewModel's `rememberMe` state is updated by its `init` block reacting to `storedRememberMeStatus`
//        // and also by `onRememberMeChange`
//    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.login_title_screen),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = loginViewModel.email, // Use email from ViewModel
            onValueChange = { loginViewModel.onEmailChange(it) },
            label = { Text(stringResource(R.string.login_username_label)) }, // Consider changing R.string to "Email"
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = loginViewModel.loginError != null && loginViewModel.email.isBlank()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = loginViewModel.password,
            onValueChange = { loginViewModel.onPasswordChange(it) },
            label = { Text(stringResource(R.string.login_password_label)) },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val description = if (showPassword) {
                    stringResource(R.string.login_password_hide_description)
                } else {
                    stringResource(R.string.login_password_show_description)
                }
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            isError = loginViewModel.loginError != null && loginViewModel.password.isBlank()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = loginViewModel.rememberMe,
                onCheckedChange = { loginViewModel.onRememberMeChange(it) }
            )
            Text(
                text = stringResource(R.string.login_remember_me_checkbox), // Add this string resource
                modifier = Modifier.padding(start = 8.dp)
            )
        }


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
                loginViewModel.onLoginClick { userId ->
                    onLoginSuccessNavigation(userId)
                }
            },
            enabled = !loginViewModel.loginInProgress
        ) {
            Text(if (loginViewModel.loginInProgress) "Logging in..." else "Log In")
        }
        loginViewModel.loginError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// Add to your strings.xml:
// <string name="login_username_label">Email</string>
// <string name="login_remember_me_checkbox">Remember Me</string>
// (Update existing login_title_screen etc. as needed)

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    // Basic preview without DataStore or full ViewModel logic
    MaterialTheme { // Wrap in your app's theme
        LoginScreen(
            loginViewModel = LoginViewModel(Application()), // Basic instance for preview
            onLoginSuccessNavigation = { }
        )
    }
}