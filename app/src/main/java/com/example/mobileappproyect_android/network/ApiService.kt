package com.example.mobileappproyect_android.network

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import kotlinx.serialization.serializer
// Make sure this is the correct Ktor Logger import
import io.ktor.client.plugins.logging.Logger as KtorLogger // <-- You can use an import alias
// OR ensure no other Logger is imported that could cause a clash.

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object ApiService {
    private val client = HttpClient(Android) {
        install(Logging) {
            // Option 1: If using an import alias
             logger = object : KtorLogger {
                 override fun log(message: String) {
                     Log.d("KtorLogger", message)
                 }
             }

            // Option 2: Explicitly qualify (if no alias and there's a clash)
//            logger = object : io.ktor.client.plugins.logging.Logger { // <-- Full qualification
//                override fun log(message: String) {
//                    Log.d("KtorLogger", message)
//                }
//            }
            level = LogLevel.ALL
        }

        install(ContentNegotiation) {
            json(Json { // This is your primary configuration
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                // explicitModules = serializersModuleOf(LoginRequest::class, LoginResponse::class) // Not usually needed if classes are @Serializable
            })
        }
    }

    private const val BASE_URL = "http://10.0.2.2:8080/api/usuarios/"

    suspend fun login(loginRequest: LoginRequest): Result<LoginResponse> {
        return try {
            val response: LoginResponse = client.post("${BASE_URL}login") {
                contentType(ContentType.Application.Json)
                setBody(loginRequest)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Log.e("ApiService", "Login failed", e)
            Result.failure(e)
        }
    }
}