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
     *
     * The key is trimmed and any embedded newline / carriage return
     * stripped, because pasting an API key from a text editor often
     * drags a trailing "\n" along.  A literal newline makes the
     * HTTP/2 header encoder throw
     *   IllegalArgumentException: Unexpected char 0x0a in
     *   Authorization value
     * at request time (the "OkHttp Dispatcher" crash the user saw),
     * so we guarantee the value only contains header-safe chars.
     */
    val authorizationHeader: String
        get() = "Bearer " + apiKey.sanitizeForHeader()

    /** Header-safe representation: trim whitespace, drop \r\n and
     *  any other C0 control characters. */
    val cleanApiKey: String get() = apiKey.sanitizeForHeader()
}

/** Remove control chars (\n, \r, tab, etc.) and trim the result. */
internal fun String.sanitizeForHeader(): String {
    if (none { it.isISOControl() }) return trim()
    return buildString {
        for (ch in this@sanitizeForHeader) {
            if (!ch.isISOControl()) append(ch)
        }
    }.trim()
}
