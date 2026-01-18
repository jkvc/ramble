package com.ramble.app.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val languageHints: Set<String> = setOf("en"),
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
        val hintsString = prefs.getString(KEY_LANGUAGE_HINTS, "en") ?: "en"
        val hints = hintsString.split(",").filter { it.isNotBlank() }.toSet()
        return AppSettings(
            languageHints = if (hints.isEmpty()) setOf("en") else hints,
            wordContext = prefs.getString(KEY_WORD_CONTEXT, "") ?: ""
        )
    }
    
    fun updateLanguageHints(languages: Set<String>) {
        val hintsToSave = if (languages.isEmpty()) setOf("en") else languages
        prefs.edit().putString(KEY_LANGUAGE_HINTS, hintsToSave.joinToString(",")).apply()
        _settings.value = _settings.value.copy(languageHints = hintsToSave)
    }
    
    fun toggleLanguageHint(language: String) {
        val current = _settings.value.languageHints.toMutableSet()
        if (current.contains(language)) {
            // Don't remove if it's the last one
            if (current.size > 1) {
                current.remove(language)
            }
        } else {
            current.add(language)
        }
        updateLanguageHints(current)
    }
    
    fun updateWordContext(context: String) {
        prefs.edit().putString(KEY_WORD_CONTEXT, context).apply()
        _settings.value = _settings.value.copy(wordContext = context)
    }
    
    companion object {
        private const val KEY_LANGUAGE_HINTS = "language_hints"
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
