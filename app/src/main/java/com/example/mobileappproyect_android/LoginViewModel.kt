package com.example.mobileappproyect_android.ui.login

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobileappproyect_android.data.SessionManager
import com.example.mobileappproyect_android.network.ApiService
import com.example.mobileappproyect_android.network.LoginRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

open class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var rememberMe by mutableStateOf(false) // State for the checkbox
        private set
    var showPassword by mutableStateOf(false)
        private set

    var loginError by mutableStateOf<String?>(null)
        private set
    var loginInProgress by mutableStateOf(false)
        private set

    init {
        // Load the initial state of "Remember Me" and potentially email
        viewModelScope.launch {
            val currentSession = sessionManager.userSessionFlow.first()
            rememberMe = currentSession.rememberMe
            if (rememberMe && currentSession.email != null) {
                email = currentSession.email
                // Password is not re-filled for security reasons
            }
        }
    }

    fun onEmailChange(newEmail: String) {
        email = newEmail
        loginError = null
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
        loginError = null
    }

    fun onRememberMeChange(shouldRemember: Boolean) {
        rememberMe = shouldRemember
        viewModelScope.launch {
            // If rememberMe is unchecked, we might want to clear saved email/userId
            // The SessionManager logic now handles clearing credentials if rememberMe becomes false.
            sessionManager.updateRememberMe(shouldRemember)
            if (!shouldRemember) {
                // If rememberMe is unchecked after previously being checked and email was filled,
                // we don't clear the email field here, user might still want to use it for current login
                // But the actual stored email in DataStore will be cleared by sessionManager.updateRememberMe
            }
        }
    }

    fun onToggleShowPassword() {
        showPassword = !showPassword
    }

    open fun onLoginClick(onLoginSuccess: (userId: String) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            loginError = "Email and password cannot be empty."
            return
        }
        loginInProgress = true
        loginError = null

        viewModelScope.launch {
            try {
                val request = LoginRequest(email, password)
                val result = ApiService.login(request)

                if (result.isSuccess) {
                    val response = result.getOrNull()
                    if (response != null && response.success && response.userId != null) {
                        // Save session if rememberMe is checked
                        if (rememberMe) {
                            sessionManager.saveSession(email, response.userId, true)
                        } else {
                            // If remember me is not checked, ensure no session is stored
                            // or if it was checked before but now isn't.
                            sessionManager.saveSession("", "", false) // Clears stored email/userId
                        }
                        onLoginSuccess(response.userId)
                    } else {
                        loginError = response?.message ?: "Login failed: Unknown error"
                    }
                } else {
                    loginError = result.exceptionOrNull()?.message ?: "Login failed: Network error"
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Login exception", e)
                loginError = "Login failed: ${e.localizedMessage}"
            } finally {
                loginInProgress = false
            }
        }
    }

    // Call this from settings or profile if user logs out
    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
            // Potentially navigate back to login screen after logout
        }
    }
}