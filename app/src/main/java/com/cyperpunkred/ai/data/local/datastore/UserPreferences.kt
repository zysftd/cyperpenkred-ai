package com.cyperpunkred.ai.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode { RED, DYNAMIC }

enum class ProviderType {
    /** Public OpenAI (https://api.openai.com/v1) */
    OPENAI,
    /** Azure OpenAI / cognitive services deployments */
    AZURE,
    /** Local proxy that speaks the OpenAI protocol (Ollama, LM Studio, vLLM, LocalAI) */
    LOCAL,
    /** Other OpenAI-compatible endpoint (DeepSeek, Moonshot, OpenRouter, etc.) */
    CUSTOM
}

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACTIVE_PROVIDER_ID = stringPreferencesKey("active_provider_id")
        val DEFAULT_MODEL = stringPreferencesKey("default_model")
        val STRICT_JSON_MODE = stringPreferencesKey("strict_json_mode")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        when (preferences[Keys.THEME_MODE]) {
            ThemeMode.RED.name -> ThemeMode.RED
            else -> ThemeMode.DYNAMIC
        }
    }

    val activeProviderId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[Keys.ACTIVE_PROVIDER_ID]?.takeIf { it.isNotBlank() }
    }

    val defaultModel: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[Keys.DEFAULT_MODEL]?.takeIf { it.isNotBlank() }
    }

    val strictJsonMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.STRICT_JSON_MODE] == "true"
    }

    suspend fun saveThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun saveActiveProviderId(id: String?) {
        context.dataStore.edit { preferences ->
            if (id.isNullOrBlank()) preferences.remove(Keys.ACTIVE_PROVIDER_ID)
            else preferences[Keys.ACTIVE_PROVIDER_ID] = id
        }
    }

    suspend fun saveDefaultModel(model: String?) {
        context.dataStore.edit { preferences ->
            if (model.isNullOrBlank()) preferences.remove(Keys.DEFAULT_MODEL)
            else preferences[Keys.DEFAULT_MODEL] = model
        }
    }

    suspend fun saveStrictJsonMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.STRICT_JSON_MODE] = enabled.toString()
        }
    }
}
