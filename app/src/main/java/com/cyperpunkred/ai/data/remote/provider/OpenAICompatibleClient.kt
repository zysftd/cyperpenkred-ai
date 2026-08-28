package com.cyperpunkred.ai.data.remote.provider

import com.cyperpunkred.ai.data.remote.model.ChatRequest
import com.cyperpunkred.ai.data.remote.model.ChatResponse
import com.cyperpunkred.ai.data.remote.model.ToolCall
import com.cyperpunkred.ai.data.remote.model.ToolCallFunction
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-provider HTTP client.  All methods are stateless w.r.t. network
 * state; [client] is built lazily from the current [ProviderConfig]
 * once per [OpenAICompatibleClient] instance and re-used for the
 * lifetime of the call.
 *
 * The base URL is whatever the user configured (e.g. an OpenAI
 * protocol-compatible proxy, a local LLM server, Azure with a
 * deployment-specific path, etc.).  We always call
 * `<baseUrl>/chat/completions` and `<baseUrl>/models`.
 */
@Singleton
class OpenAICompatibleClient @Inject constructor(
    private val baseClient: OkHttpClient,
    private val gson: Gson
) {
    private data class Call(val config: ProviderConfig, val client: OkHttpClient)

    private fun callFor(config: ProviderConfig): Call {
        val client = baseClient.newBuilder()
            .connectTimeout(config.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout((config.timeoutSeconds * 2).toLong(), TimeUnit.SECONDS)
            .writeTimeout(config.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("Authorization", config.authorizationHeader)
                    .build()
                chain.proceed(req)
            }
            .build()
        return Call(config, client)
    }

    /** Non-streaming chat completion. Throws on transport/HTTP errors. */
    suspend fun chat(config: ProviderConfig, request: ChatRequest): ChatResponse {
        val (cfg, client) = callFor(config)
        val url = "${cfg.normalizedBaseUrl}/chat/completions"
        val body = gson.toJson(request).toRequestBody("application/json".toMediaType())
        val httpRequest = Request.Builder()
            .url(url)
            .post(body)
            .build()
        client.newCall(httpRequest).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} from ${cfg.name}: ${text.take(400)}")
            }
            return gson.fromJson(text, ChatResponse::class.java)
        }
    }

    sealed interface StreamEvent {
        val text: String
        val toolCallDelta: ToolCall?
        val finishReason: String
    }
    data class StreamText(override val text: String) : StreamEvent {
        override val toolCallDelta: ToolCall? = null
        override val finishReason: String = ""
    }
    data class StreamTool(
        override val text: String,
        override val toolCallDelta: ToolCall,
        override val finishReason: String
    ) : StreamEvent
    data class StreamFinish(override val text: String, override val finishReason: String) : StreamEvent {
        override val toolCallDelta: ToolCall? = null
    }
    data class StreamError(val message: String) : StreamEvent {
        override val text: String = ""
        override val toolCallDelta: ToolCall? = null
        override val finishReason: String = ""
    }
    data class StreamEnd(override val finishReason: String) : StreamEvent {
        override val text: String = ""
        override val toolCallDelta: ToolCall? = null
    }

    /**
     * Streaming chat completion. Emits incremental text/tool events
     * and a synthetic StreamEnd when the connection closes (or [DONE]
     * arrives). Throws on connection setup failure only.
     */
    fun stream(config: ProviderConfig, request: ChatRequest): Flow<StreamEvent> = flow {
        val (cfg, client) = callFor(config)
        val url = "${cfg.normalizedBaseUrl}/chat/completions"
        val body = gson.toJson(request).toRequestBody("application/json".toMediaType())
        val httpRequest = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .post(body)
            .build()

        val factory = EventSources.createFactory(client)
        val collected = mutableMapOf<Int, ToolCallBuilder>()
        val channel = kotlinx.coroutines.channels.Channel<StreamEvent>(
            capacity = kotlinx.coroutines.channels.Channel.BUFFERED
        )
        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    channel.trySend(StreamEnd(""))
                    return
                }
                val parsed = runCatching { gson.fromJson(data, ChatResponse::class.java) }.getOrNull()
                val choice = parsed?.choices?.firstOrNull() ?: return
                val delta = choice.delta ?: return
                val fr = choice.finishReason.orEmpty()
                val text = delta.content.orEmpty()
                if (text.isNotEmpty()) {
                    channel.trySend(StreamText(text))
                }
                delta.toolCalls?.forEach { tc ->
                    val builder = collected.getOrPut(tc.index) { ToolCallBuilder() }
                    builder.id = tc.id
                    builder.name = tc.function?.name ?: ""
                    tc.function?.arguments?.let { builder.arguments.append(it) }
                }
                if (fr.isNotEmpty()) {
                    val toolCalls = collected.values
                        .filter { it.id.isNotBlank() && it.name.isNotBlank() }
                        .map {
                            ToolCall(
                                id = it.id,
                                function = ToolCallFunction(
                                    name = it.name,
                                    arguments = it.arguments.toString()
                                )
                            )
                        }
                    toolCalls.forEach { channel.trySend(StreamTool(text = "", toolCallDelta = it, finishReason = fr)) }
                    channel.trySend(StreamFinish(text = text, finishReason = fr))
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
                val msg = buildString {
                    append(t?.message ?: "unknown SSE failure")
                    if (response != null) {
                        append(" (HTTP ${response.code})")
                        response.body?.string()?.let { append(": ").append(it.take(200)) }
                    }
                }
                channel.trySend(StreamError(msg))
                channel.close()
            }

            override fun onClosed(eventSource: EventSource) {
                channel.trySend(StreamEnd(""))
            }
        }

        val eventSource: EventSource = factory.newEventSource(httpRequest, listener)
        try {
            for (chunk in channel) {
                if (chunk is StreamEnd) break
                emit(chunk)
            }
        } finally {
            eventSource.cancel()
        }
    }.flowOn(Dispatchers.IO)

    /** GET /v1/models for the model picker. */
    suspend fun listModels(config: ProviderConfig): List<String> {
        val (cfg, client) = callFor(config)
        val url = "${cfg.normalizedBaseUrl}/models"
        val httpRequest = Request.Builder().url(url).get().build()
        client.newCall(httpRequest).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: ${text.take(200)}")
            }
            val parsed = runCatching {
                gson.fromJson(text, ModelsResponse::class.java)
            }.getOrNull()
            return parsed?.data?.map { it.id }.orEmpty().sorted()
        }
    }

    /** Test that a provider config is reachable: just lists models. */
    suspend fun ping(config: ProviderConfig): String {
        val models = listModels(config)
        return if (models.isEmpty()) "✅ 连接成功（但模型列表为空）"
        else "✅ 连接成功，发现 ${models.size} 个模型：${models.take(5).joinToString(", ")}"
    }

    private class ToolCallBuilder {
        var id: String = ""
        var name: String = ""
        val arguments: StringBuilder = StringBuilder()
    }
}

private data class ModelsResponse(
    @SerializedName("data") val data: List<ModelEntry>?
)

private data class ModelEntry(
    @SerializedName("id") val id: String
)
