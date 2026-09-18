package com.example.core.config

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ApiKeyPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nous_api_config", Context.MODE_PRIVATE)

    private val _apiKeyFlow = MutableStateFlow(getStoredApiKey())
    val apiKeyFlow: StateFlow<String> = _apiKeyFlow.asStateFlow()

    fun getStoredApiKey(): String {
        return prefs.getString("gemini_api_key", "")?.trim() ?: ""
    }

    fun saveApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString("gemini_api_key", trimmed).apply()
        _apiKeyFlow.value = trimmed
    }

    fun clearApiKey() {
        prefs.edit().remove("gemini_api_key").apply()
        _apiKeyFlow.value = ""
    }
}
