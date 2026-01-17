package com.ramble.app.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class User(
    val id: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String
)

class AuthManager(context: Context) {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "ramble_auth",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    val isLoggedIn: Boolean
        get() = _currentUser.value != null
    
    init {
        // Load saved user on init
        loadUser()
    }
    
    private fun loadUser() {
        val userJson = prefs.getString(KEY_USER, null)
        if (userJson != null) {
            try {
                _currentUser.value = json.decodeFromString<User>(userJson)
            } catch (e: Exception) {
                // Invalid stored data, clear it
                logout()
            }
        }
    }
    
    fun saveUser(user: User) {
        val userJson = json.encodeToString(User.serializer(), user)
        prefs.edit().putString(KEY_USER, userJson).apply()
        _currentUser.value = user
    }
    
    fun logout() {
        prefs.edit().remove(KEY_USER).apply()
        _currentUser.value = null
    }
    
    fun getAccessToken(): String? {
        return _currentUser.value?.accessToken
    }
    
    companion object {
        private const val KEY_USER = "user"
    }
}
