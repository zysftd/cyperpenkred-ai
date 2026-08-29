package com.cyperpunkred.ai.data.remote.provider

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.cyperpunkred.ai.data.local.datastore.UserPreferences
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists [ProviderConfig]s in EncryptedSharedPreferences so that the
 * API keys never land in the on-device plaintext SharedPreferences
 * (which is world-readable on a rooted device and exposed to anyone
 * who can `adb backup`).
 *
 * Schema:
 *   key "provider_ids" -> comma-separated provider ids in display order
 *   key "provider:<id>" -> JSON-encoded [ProviderConfig] (incl. API key)
 *
 * The active provider id lives in the regular DataStore (it is not a
 * secret).
 */
@Singleton
class ProviderStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences
) {
    private val gson = Gson()

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "providers_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private object Keys {
        const val IDS = "provider_ids"
        fun provider(id: String) = "provider:$id"
    }

    private val changeTrigger = MutableStateFlow(0)

    val providers: Flow<List<ProviderConfig>> = changeTrigger.map { readAll() }

    val active: Flow<ProviderConfig?> = combine(
        userPreferences.activeProviderId,
        providers
    ) { activeId, all ->
        all.firstOrNull { it.id == activeId } ?: all.firstOrNull()
    }

    suspend fun upsert(config: ProviderConfig) = withContext(Dispatchers.IO) {
        val idList = currentIds().toMutableList()
        if (config.id !in idList) idList.add(config.id)
        encryptedPrefs.edit()
            .putString(Keys.IDS, idList.joinToString(","))
            .putString(Keys.provider(config.id), gson.toJson(config))
            .apply()
        changeTrigger.update { it + 1 }
        Unit
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        val idList = currentIds().toMutableList()
        idList.remove(id)
        encryptedPrefs.edit()
            .remove(Keys.provider(id))
            .putString(Keys.IDS, idList.joinToString(","))
            .apply()
        val activeId = userPreferences.activeProviderId.firstOrNull()
        if (activeId == id) {
            val remaining = readAll()
            userPreferences.saveActiveProviderId(remaining.firstOrNull()?.id)
        }
        changeTrigger.update { it + 1 }
        Unit
    }

    suspend fun setActive(id: String) {
        userPreferences.saveActiveProviderId(id)
    }

    suspend fun readSnapshot(): List<ProviderConfig> = withContext(Dispatchers.IO) { readAll() }

    suspend fun activeSnapshot(): ProviderConfig? = withContext(Dispatchers.IO) {
        val activeId = userPreferences.activeProviderId.firstOrNull()
        val all = readAll()
        all.firstOrNull { it.id == activeId } ?: all.firstOrNull()
    }

    fun newId(): String = UUID.randomUUID().toString()

    private fun currentIds(): List<String> = encryptedPrefs.getString(Keys.IDS, "")
        ?.split(",")
        ?.filter { it.isNotBlank() }
        ?: emptyList()

    private fun readAll(): List<ProviderConfig> = currentIds().mapNotNull { id ->
        runCatching {
            encryptedPrefs.getString(Keys.provider(id), null)?.let { json ->
                gson.fromJson(json, ProviderConfig::class.java)?.let { cfg ->
                    cfg.copy(apiKey = cfg.apiKey.sanitizeForHeader())
                }
            }
        }.getOrNull()
    }
}
