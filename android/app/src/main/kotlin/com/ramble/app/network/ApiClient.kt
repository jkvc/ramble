package com.ramble.app.network

import com.ramble.app.BuildConfig
import com.ramble.app.RambleApp
import com.ramble.app.auth.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val user: UserData,
    val session: SessionData
)

@Serializable
data class UserData(
    val id: String,
    val email: String
)

@Serializable
data class SessionData(
    val access_token: String,
    val refresh_token: String
)

@Serializable
data class SonioxTokenResponse(
    val token: String,
    val websocketUrl: String
)

@Serializable
data class RefreshRequest(
    val refresh_token: String
)

@Serializable
data class ErrorResponse(
    val error: String
)

object ApiClient {
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val baseUrl: String
        get() = BuildConfig.BACKEND_URL
    
    private val jsonMediaType = "application/json".toMediaType()
    
    suspend fun login(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            // Use Supabase Auth directly via the anon key
            // For now, we'll call a simplified login endpoint
            val requestBody = json.encodeToString(LoginRequest.serializer(), LoginRequest(email, password))
            
            val request = Request.Builder()
                .url("$baseUrl/api/auth/login")
                .post(requestBody.toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val loginResponse = json.decodeFromString<LoginResponse>(body)
                Result.success(
                    User(
                        id = loginResponse.user.id,
                        email = loginResponse.user.email,
                        accessToken = loginResponse.session.access_token,
                        refreshToken = loginResponse.session.refresh_token
                    )
                )
            } else {
                val errorResponse = try {
                    json.decodeFromString<ErrorResponse>(body)
                } catch (e: Exception) {
                    ErrorResponse("Login failed")
                }
                Result.failure(Exception(errorResponse.error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getSonioxToken(): Result<SonioxTokenResponse> = withContext(Dispatchers.IO) {
        try {
            val result = getSonioxTokenInternal()
            
            // If we get "Invalid token", try to refresh and retry once
            if (result.isFailure && result.exceptionOrNull()?.message == "Invalid token") {
                val refreshResult = refreshToken()
                if (refreshResult.isSuccess) {
                    // Retry with new token
                    return@withContext getSonioxTokenInternal()
                } else {
                    // Refresh failed, log out user
                    RambleApp.instance.authManager.logout()
                    return@withContext Result.failure(Exception("Session expired. Please log in again."))
                }
            }
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun getSonioxTokenInternal(): Result<SonioxTokenResponse> = withContext(Dispatchers.IO) {
        try {
            val accessToken = RambleApp.instance.authManager.getAccessToken()
                ?: return@withContext Result.failure(Exception("Not authenticated"))
            
            val request = Request.Builder()
                .url("$baseUrl/api/soniox/token")
                .post("{}".toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $accessToken")
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val tokenResponse = json.decodeFromString<SonioxTokenResponse>(body)
                Result.success(tokenResponse)
            } else {
                val errorResponse = try {
                    json.decodeFromString<ErrorResponse>(body)
                } catch (e: Exception) {
                    ErrorResponse("Failed to get token")
                }
                Result.failure(Exception(errorResponse.error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun refreshToken(): Result<User> = withContext(Dispatchers.IO) {
        try {
            val currentUser = RambleApp.instance.authManager.currentUser.value
                ?: return@withContext Result.failure(Exception("Not authenticated"))
            
            val requestBody = json.encodeToString(
                RefreshRequest.serializer(), 
                RefreshRequest(currentUser.refreshToken)
            )
            
            val request = Request.Builder()
                .url("$baseUrl/api/auth/refresh")
                .post(requestBody.toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val loginResponse = json.decodeFromString<LoginResponse>(body)
                val newUser = User(
                    id = loginResponse.user.id,
                    email = loginResponse.user.email,
                    accessToken = loginResponse.session.access_token,
                    refreshToken = loginResponse.session.refresh_token
                )
                // Save the new tokens
                RambleApp.instance.authManager.saveUser(newUser)
                Result.success(newUser)
            } else {
                val errorResponse = try {
                    json.decodeFromString<ErrorResponse>(body)
                } catch (e: Exception) {
                    ErrorResponse("Refresh failed")
                }
                Result.failure(Exception(errorResponse.error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
