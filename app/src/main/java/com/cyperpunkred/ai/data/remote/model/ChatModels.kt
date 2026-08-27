package com.cyperpunkred.ai.data.remote.model

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    val model: String = "gpt-4",
    val messages: List<ChatMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 2000,
    val temperature: Double = 0.8
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatResponse(
    val id: String,
    val choices: List<Choice>,
    val usage: Usage? = null
)

data class Choice(
    val index: Int,
    val message: ChatMessage,
    @SerializedName("finish_reason") val finishReason: String
)

data class Usage(
    @SerializedName("prompt_tokens") val promptTokens: Int,
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int
)
