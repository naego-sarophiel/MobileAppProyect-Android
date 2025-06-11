package com.example.mobileappproyect_android.network // Or a suitable package

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String, // Assuming your API uses "email" for the username field
    val password: String
)

@Serializable
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val userId: String?, // Or Int? depending on your API
    val nombre: String?
)