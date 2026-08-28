package com.cyperpunkred.ai.data.remote.provider

import com.cyperpunkred.ai.data.local.datastore.ProviderType

/**
 * Configuration for a user-configurable OpenAI-compatible API endpoint.
 * The app stores one [ProviderConfig] per provider id in
 * [ProviderStore]; the active one is selected by [ProviderStore.active].
 *
 * Every provider is expected to implement the OpenAI chat completions
 * protocol (POST /v1/chat/completions, GET /v1/models).  Some providers
 * gate advanced features behind capability flags: [supportsTools] and
 * [supportsStream] should be set to false for endpoints that return
 * errors on tools/streaming (so the UI can fall back).
 */
data class ProviderConfig(
    val id: String,
    val name: String,
    val type: ProviderType,
    val baseUrl: String,
    val apiKey: String,
    val defaultModel: String,
    val timeoutSeconds: Int = 60,
    val supportsTools: Boolean = true,
    val supportsStream: Boolean = true
) {
    /**
     * Normalized base URL with no trailing slash, used to build
     * `chat/completions`, `models`, etc. paths.
     */
    val normalizedBaseUrl: String
        get() = baseUrl.trim().trimEnd('/')

    /**
     * Authorization header value. The default Bearer scheme works
     * for OpenAI, DeepSeek, Moonshot, OpenRouter, etc. – all of which
     * accept `Authorization: Bearer <key>`.
     */
    val authorizationHeader: String
        get() = "Bearer $apiKey"
}
