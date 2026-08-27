package com.cyperpunkred.ai.data.remote.api

import com.cyperpunkred.ai.data.remote.model.ChatRequest
import com.cyperpunkred.ai.data.remote.model.ChatResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAIApi {
    @POST("v1/chat/completions")
    suspend fun chat(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest
    ): ChatResponse
}
