package com.ramble.app.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ApiKeyManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "ramble_api_key",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _apiKey = MutableStateFlow<String?>(null)
    val apiKey: StateFlow<String?> = _apiKey.asStateFlow()

    val hasApiKey: Boolean
        get() = _apiKey.value != null

    init {
        _apiKey.value = prefs.getString(KEY_API_KEY, null)
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key).apply()
        _apiKey.value = key
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_API_KEY).apply()
        _apiKey.value = null
    }

    companion object {
        private const val KEY_API_KEY = "soniox_api_key"
    }
}
