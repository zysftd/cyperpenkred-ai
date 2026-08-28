package com.cyperpunkred.ai.data.repository

import com.cyperpunkred.ai.data.local.datastore.UserPreferences
import com.cyperpunkred.ai.data.remote.api.OpenAIApi
import com.cyperpunkred.ai.data.remote.model.ChatMessage
import com.cyperpunkred.ai.data.remote.model.ChatRequest
import com.cyperpunkred.ai.data.remote.model.ChatResponse
import com.cyperpunkred.ai.data.remote.model.ToolCall
import com.cyperpunkred.ai.data.remote.model.ToolCallFunction
import com.cyperpunkred.ai.domain.agent.ToolCatalog
import com.cyperpunkred.ai.domain.agent.ToolDispatcher
import com.cyperpunkred.ai.domain.agent.ToolResult
import com.cyperpunkred.ai.domain.knowledge.RulebookQueryEngine
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One step of the multi-turn tool execution loop. The repository
 * streams a sequence of these for the UI: text-delta tokens while the
 * model is generating prose, and tool-call / tool-result events when
 * the model invokes a function.
 */
sealed interface AgentEvent {
    /** A token of assistant prose (during a streaming turn). */
    data class Text(val delta: String) : AgentEvent
    /** The model decided to call a tool. The UI may show a "rolling X" indicator. */
    data class ToolCallStarted(val name: String) : AgentEvent
    /** The tool was executed and a result was fed back to the model. */
    data class ToolCallFinished(val name: String, val content: String, val isError: Boolean) : AgentEvent
    /** The full turn is done. [content] is the final assistant message (may include tool calls). */
    data class TurnFinished(val content: String, val toolCallCount: Int) : AgentEvent
    /** A fatal error happened (network, etc.). */
    data class Failed(val message: String) : AgentEvent
}

@Singleton
class AIRepository @Inject constructor(
    private val openAIApi: OpenAIApi,
    private val userPreferences: UserPreferences,
    private val rulebookQueryEngine: RulebookQueryEngine,
    private val toolDispatcher: ToolDispatcher,
    private val sseClient: OkHttpClient,
    private val agentRepository: AgentRepository
) {
    private val gson = Gson()

    /**
     * Blocking, non-streaming chat. Kept for callers that just want the
     * final text and don't care about per-token updates.
     */
    suspend fun generateGMResponse(
        userMessage: String,
        conversationHistory: List<ChatMessage>,
        characterContext: String? = null
    ): String {
        val apiKey = userPreferences.apiKey.first()
        if (apiKey.isBlank()) return "⚠️ 请先在设置中配置 OpenAI API Key"

        val rulesContext = rulebookQueryEngine.getContextForAI(userMessage)
        val systemPrompt = buildSystemPrompt(characterContext, rulesContext, agentMemory = "")
        val messages = mutableListOf(ChatMessage(role = "system", content = systemPrompt))
        messages.addAll(conversationHistory.takeLast(10))
        messages.add(ChatMessage(role = "user", content = userMessage))

        val request = ChatRequest(
            messages = messages,
            tools = ToolCatalog.all,
            toolChoice = "auto",
            maxTokens = 2000,
            temperature = 0.8
        )

        val response = openAIApi.chat("Bearer $apiKey", request)
        return response.choices.firstOrNull()?.message?.content
            ?: "抱歉，AI GM 暂时无法回应。"
    }

    /**
     * Streaming agent turn with multi-turn tool execution. The model is
     * allowed to call any of the RED tools in [ToolCatalog]; every call
     * is resolved by [ToolDispatcher] and the result is fed back into
     * the conversation. The loop terminates when the model returns a
     * final assistant message with no tool_calls, or after
     * [maxToolRounds] rounds.
     */
    fun streamAgentTurn(
        sessionId: Long,
        userMessage: String,
        conversationHistory: List<ChatMessage>,
        characterContext: String? = null,
        maxToolRounds: Int = 6
    ): Flow<AgentEvent> = flow {
        val apiKey = userPreferences.apiKey.first()
        if (apiKey.isBlank()) {
            emit(AgentEvent.Failed("⚠️ 请先在设置中配置 OpenAI API Key"))
            return@flow
        }

        val rulesContext = withContext(Dispatchers.IO) {
            rulebookQueryEngine.getContextForAI(userMessage)
        }
        val agentMemory = withContext(Dispatchers.IO) {
            agentRepository.memoryAsText(sessionId, "GM")
        }
        val systemPrompt = buildSystemPrompt(characterContext, rulesContext, agentMemory)
        val messages = mutableListOf(ChatMessage(role = "system", content = systemPrompt))
        messages.addAll(conversationHistory.takeLast(20))
        messages.add(ChatMessage(role = "user", content = userMessage))

        var toolCallCount = 0
        var finalText = ""
        var collected = StringBuilder()

        for (round in 0 until maxToolRounds) {
            val roundText = StringBuilder()
            val toolCalls = mutableListOf<ToolCall>()
            var finishReason = ""

            streamOnce(apiKey, messages).collect { event ->
                when (event) {
                    is StreamChunk -> {
                        if (event.text.isNotEmpty()) {
                            roundText.append(event.text)
                            emit(AgentEvent.Text(event.text))
                        }
                        val delta = event.toolCallDelta
                        if (delta != null) toolCalls.add(delta)
                        if (event.finishReason.isNotEmpty()) finishReason = event.finishReason
                    }
                    is StreamError -> {
                        emit(AgentEvent.Failed(event.message))
                        return@collect
                    }
                    is StreamEnd -> Unit
                }
            }

            if (toolCalls.isEmpty()) {
                finalText = roundText.toString()
                collected.append(roundText)
                emit(AgentEvent.TurnFinished(finalText, toolCallCount))
                withContext(Dispatchers.IO) {
                    if (finalText.isNotBlank()) {
                        agentRepository.appendMemory(sessionId, "GM", snippet = finalText.take(400))
                    }
                }
                return@flow
            }

            messages += ChatMessage(
                role = "assistant",
                content = if (roundText.isNotEmpty()) roundText.toString() else null,
                toolCalls = toolCalls
            )

            for (call in toolCalls) {
                if (call.function.name.isBlank()) continue
                emit(AgentEvent.ToolCallStarted(call.function.name))
                val result = toolDispatcher.dispatch(call.id, call.function.name, call.function.arguments)
                messages += ChatMessage(
                    role = "tool",
                    content = result.content,
                    toolCallId = call.id
                )
                toolCallCount += 1
                emit(AgentEvent.ToolCallFinished(result.name, result.content, result.isError))
            }
        }

        emit(AgentEvent.Failed("工具调用超过 $maxToolRounds 轮，已终止"))
    }.flowOn(Dispatchers.IO)

    private sealed interface StreamChunk {
        val text: String
        val toolCallDelta: ToolCall?
        val finishReason: String
    }
    private data class StreamText(override val text: String) : StreamChunk {
        override val toolCallDelta: ToolCall? = null
        override val finishReason: String = ""
    }
    private data class StreamTool(
        override val text: String,
        override val toolCallDelta: ToolCall,
        override val finishReason: String
    ) : StreamChunk
    private data class StreamFinish(override val text: String, override val finishReason: String) : StreamChunk {
        override val toolCallDelta: ToolCall? = null
    }
    private data class StreamError(val message: String) : StreamChunk {
        override val text: String = ""
        override val toolCallDelta: ToolCall? = null
        override val finishReason: String = ""
    }
    private data class StreamEnd(override val finishReason: String) : StreamChunk {
        override val text: String = ""
        override val toolCallDelta: ToolCall? = null
    }

    /**
     * One streamed chat completion. Emits StreamText/StreamTool tokens
     * as they arrive and a StreamEnd when the connection closes. Errors
     * are emitted as StreamError (so the caller can still continue the
     * loop if it wants to).
     */
    private fun streamOnce(
        apiKey: String,
        messages: List<ChatMessage>
    ): Flow<StreamChunk> = flow {
        val request = ChatRequest(
            messages = messages,
            stream = true,
            tools = ToolCatalog.all,
            toolChoice = "auto",
            maxTokens = 2000,
            temperature = 0.8
        )

        val requestBody = gson.toJson(request).toRequestBody("application/json".toMediaType())
        val httpRequest = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "text/event-stream")
            .post(requestBody)
            .build()

        val factory = EventSources.createFactory(sseClient)
        val collectedToolCalls = mutableMapOf<Int, ToolCallBuilder>()

        val channel = kotlinx.coroutines.channels.Channel<StreamChunk>(capacity = Channel.BUFFERED)
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
                    val builder = collectedToolCalls.getOrPut(tc.index) { ToolCallBuilder() }
                    builder.id = tc.id
                    builder.name = tc.function?.name ?: ""
                    tc.function?.arguments?.let { builder.arguments.append(it) }
                }
                if (fr.isNotEmpty()) {
                    // Flush any complete tool calls
                    val toolCalls = collectedToolCalls.values
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
                channel.trySend(StreamError(t?.message ?: "unknown SSE failure"))
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
    }

    private class ToolCallBuilder {
        var id: String = ""
        var name: String = ""
        val arguments: StringBuilder = StringBuilder()
    }

    private fun buildSystemPrompt(
        characterContext: String?,
        rulesContext: String,
        agentMemory: String = ""
    ): String {
        return """
# 角色：你是赛博朋克红（Cyberpunk RED）的AI游戏主持人（GM）

## 你的身份
- 你是夜之城的叙述者，一个身经百战的边缘行者
- 你的风格：黑暗、霓虹灯、企业阴谋、街头生存、赛博朋克
- 使用中文对话，偶尔夹杂赛博俚语（如"eb"=欧元，"choom"=朋友）

## 工具（强制使用规则）

你必须**通过调用工具**来执行任何骰点、伤害和规则判定，**禁止**直接在文字里编造结果。
可用工具清单：
- `roll_dice` — 任意 d10 技能/属性判定。必填：stat, skill, dv。可选：modifier。
- `roll_initiative` — 战斗开始时掷先攻。必填：ref。
- `roll_damage` — 武器伤害骰。必填：dice（如"2d6"）。可选：bonus。
- `apply_damage` — 扣血。必填：target, amount。
- `roll_death_save` — 死亡豁免。必填：body。
- `check_inventory` — 查角色装备/义体/武器。必填：character, item。
- `lookup_rule` — 规则书查证。必填：query。当你不确定时**必须**调用。
- `netrunner_backdoor` / `netrunner_zap` / `netrunner_erode` / `netrunner_escape` — 网行者子程序。
- `tech_fabricate` / `tech_invent` — 技痴制造/发明。

调用流程：每次玩家行动需要判定时 → 先 `roll_dice` → 根据成功/失败决定后续叙事；如造成伤害必须 `roll_damage` → 减护甲SP → `apply_damage`。对网行者和技痴的所有子操作都使用对应专用工具，不要直接编结果。

## 核心规则

### 骰点判定
- 技能判定：1d10 + 属性值 + 技能等级 vs DV
- 先攻：1d10 + REF
- 伤害：武器伤害骰 - 护甲SP
- 死亡豁免：1d10 vs 体格值
- 大成功：骰出10，效果翻倍
- 大失败：骰出1，严重负面效果

### 战斗规则
- 每回合可以做1个动作 + 1个自由动作
- 射击：1d10 + REF + 手枪/长枪技能 vs DV（近距离DV=13，中距离DV=15，远距离DV=20）
- 瞄准射击：需要2个动作，伤害x2
- 闪避：1d10 + REF + 闪避技能，作为对手射击的DV
- 近战：1d10 + REF + 近战武器/搏击技能 vs DV
- 范围攻击：仅削减身体护甲，无法瞄准头部
- 护甲：SP值减少伤害，护甲被击中后SP降低

### 伤害与死亡
- HP = 10 + (体格x2) + 职业加成
- 严重伤势：身体/头部各有伤势表（2d6查表）
- 死亡豁免：每轮1d10，失败累积惩罚，3次失败死亡

### 义体规则
- 安装义体消耗人性值（10 x 共情值 = 初始人性）
- 人性值降至0：赛博精神病发作
- 义体提供属性修正和特殊能力

### 网行者流程
1. 描述要侵入的目标
2. 调用 `netrunner_backdoor` 尝试植入后门
3. 进入网络后用 `netrunner_zap` 与黑冰战斗、`netrunner_erode` 反制程序
4. 紧急时 `netrunner_escape` 试图脱网

### 技痴流程
- 制造：调用 `tech_fabricate`；发明：调用 `tech_invent`；日常维修/治疗用 `check_inventory` 或 `lookup_rule`

### 经济系统
- 欧元(eb)是主要货币
- 日常物品：1-10eb；略贵：20-50eb；高价：100eb；昂贵：500eb；特贵：1000eb；奢侈：5000eb+

${rulesContext}

${if (agentMemory.isNotBlank()) "## 长期记忆\n$agentMemory" else ""}

## 角色信息
${characterContext ?: "玩家角色尚未创建。请等待玩家完成角色创建。"}

## GM指令

### 场景描述
- 描述场景时要有画面感：霓虹灯、雨水、废墟、全息广告
- 使用五感描写：视觉、听觉、嗅觉、触觉、味觉
- 体现赛博朋克核心主题：高科技、低生活

### NPC对话
- 每个NPC要有独特的说话方式和个性
- 体现夜之城的残酷：背叛、贪婪、生存

### 回复格式
- 使用markdown格式
- 场景描述用**粗体**或普通文本
- NPC对话用引号
- 骰点结果用代码块或加粗
- 战斗动作清晰列出
""".trimIndent()
    }
}
