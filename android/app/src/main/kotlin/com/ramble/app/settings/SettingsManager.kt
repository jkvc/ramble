package com.ramble.app.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val languageHint: String = "en",
    val wordContext: String = ""
)

class SettingsManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "ramble_settings",
        Context.MODE_PRIVATE
    )
    
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    
    private fun loadSettings(): AppSettings {
        return AppSettings(
            languageHint = prefs.getString(KEY_LANGUAGE_HINT, "en") ?: "en",
            wordContext = prefs.getString(KEY_WORD_CONTEXT, "") ?: ""
        )
    }
    
    fun updateLanguageHint(language: String) {
        prefs.edit().putString(KEY_LANGUAGE_HINT, language).apply()
        _settings.value = _settings.value.copy(languageHint = language)
    }
    
    fun updateWordContext(context: String) {
        prefs.edit().putString(KEY_WORD_CONTEXT, context).apply()
        _settings.value = _settings.value.copy(wordContext = context)
    }
    
    companion object {
        private const val KEY_LANGUAGE_HINT = "language_hint"
        private const val KEY_WORD_CONTEXT = "word_context"
        
        val SUPPORTED_LANGUAGES = listOf(
            "en" to "English",
            "es" to "Spanish",
            "fr" to "French",
            "de" to "German",
            "it" to "Italian",
            "pt" to "Portuguese",
            "zh" to "Chinese",
            "ja" to "Japanese",
            "ko" to "Korean",
            "ar" to "Arabic",
            "hi" to "Hindi",
            "ru" to "Russian"
        )
    }
}
