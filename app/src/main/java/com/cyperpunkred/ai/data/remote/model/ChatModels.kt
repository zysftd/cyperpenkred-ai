package com.cyperpunkred.ai.data.remote.model

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    val model: String = "gpt-4",
    val messages: List<ChatMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 2000,
    val temperature: Double = 0.8,
    val stream: Boolean = false,
    val tools: List<Tool>? = null,
    @SerializedName("tool_choice") val toolChoice: Any? = null,
    @SerializedName("response_format") val responseFormat: ResponseFormat? = null
)

data class ChatMessage(
    val role: String,
    val content: String? = null,
    val name: String? = null,
    @SerializedName("tool_calls") val toolCalls: List<ToolCall>? = null,
    @SerializedName("tool_call_id") val toolCallId: String? = null
)

data class Tool(
    val type: String = "function",
    val function: ToolFunction
)

data class ToolFunction(
    val name: String,
    val description: String,
    val parameters: ToolParameters
)

data class ToolParameters(
    val type: String = "object",
    val properties: Map<String, ToolProperty>,
    val required: List<String> = emptyList()
)

data class ToolProperty(
    val type: String,
    val description: String,
    val enum: List<String>? = null
)

data class ToolCall(
    val index: Int = 0,
    val id: String,
    val type: String = "function",
    val function: ToolCallFunction
)

data class ToolCallFunction(
    val name: String,
    val arguments: String
)

data class ResponseFormat(
    val type: String = "text"
) {
    companion object {
        fun json() = ResponseFormat(type = "json_object")
    }
}

data class ChatResponse(
    val id: String,
    val choices: List<Choice>,
    val usage: Usage? = null
)

data class Choice(
    val index: Int,
    val message: ChatMessage? = null,
    val delta: ChatMessage? = null,
    @SerializedName("finish_reason") val finishReason: String? = null
)

data class Usage(
    @SerializedName("prompt_tokens") val promptTokens: Int,
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int
)
