package com.cyperpunkred.ai.data.repository

import com.cyperpunkred.ai.data.local.datastore.UserPreferences
import com.cyperpunkred.ai.data.remote.api.OpenAIApi
import com.cyperpunkred.ai.data.remote.model.ChatMessage
import com.cyperpunkred.ai.data.remote.model.ChatRequest
import com.cyperpunkred.ai.domain.knowledge.RulebookQueryEngine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIRepository @Inject constructor(
    private val openAIApi: OpenAIApi,
    private val userPreferences: UserPreferences,
    private val rulebookQueryEngine: RulebookQueryEngine
) {
    suspend fun generateGMResponse(
        userMessage: String,
        conversationHistory: List<ChatMessage>,
        characterContext: String? = null
    ): String {
        val apiKey = userPreferences.apiKey.first()

        val keywords = rulebookQueryEngine.extractKeywords(userMessage)
        val relevantRules = rulebookQueryEngine.queryByKeywords(keywords)

        val systemPrompt = buildSystemPrompt(characterContext, relevantRules)

        val messages = mutableListOf(
            ChatMessage(role = "system", content = systemPrompt)
        )
        messages.addAll(conversationHistory.takeLast(10))
        messages.add(ChatMessage(role = "user", content = userMessage))

        val request = ChatRequest(
            messages = messages,
            maxTokens = 2000,
            temperature = 0.8
        )

        val response = openAIApi.chat(
            authorization = "Bearer $apiKey",
            request = request
        )

        return response.choices.firstOrNull()?.message?.content
            ?: "抱歉，AI GM 暂时无法回应。"
    }

    private fun buildSystemPrompt(
        characterContext: String?,
        rules: List<com.cyperpunkred.ai.data.local.db.entity.RulebookEntryEntity>
    ): String {
        val rulesText = rules.joinToString("\n\n") { entry ->
            "【${entry.title}】\n${entry.content}"
        }

        return """
你是赛博朋克红的AI游戏主持人（GM）。

## 你的角色
- 你是夜之城的叙述者，负责推进剧情、扮演NPC、判定骰点
- 你的风格是赛博朋克风格：黑暗、霓虹、企业阴谋、街头生存
- 严格基于规则书判定，但描述可以自由发挥增加趣味性
- 使用中文对话

## 游戏规则
$rulesText

## 角色信息
${characterContext ?: "玩家角色尚未创建"}

## 对话要求
- 描述场景时要有画面感，使用赛博朋克元素
- NPC对话要有个性，体现夜之城的残酷
- 骰点判定要准确，展示计算过程
- 战斗描述要紧张刺激
- 保持游戏的紧张感和戏剧性
""".trimIndent()
    }
}
