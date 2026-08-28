package com.cyperpunkred.ai.data.repository

import com.cyperpunkred.ai.data.local.datastore.UserPreferences
import com.cyperpunkred.ai.data.remote.model.ChatMessage
import com.cyperpunkred.ai.data.remote.model.ChatRequest
import com.cyperpunkred.ai.data.remote.model.ChatResponse
import com.cyperpunkred.ai.data.remote.model.ResponseFormat
import com.cyperpunkred.ai.data.remote.model.ToolCall
import com.cyperpunkred.ai.data.remote.provider.OpenAICompatibleClient
import com.cyperpunkred.ai.data.remote.provider.ProviderConfig
import com.cyperpunkred.ai.data.remote.provider.ProviderStore
import com.cyperpunkred.ai.domain.agent.ToolCatalog
import com.cyperpunkred.ai.domain.agent.ToolDispatcher
import com.cyperpunkred.ai.domain.knowledge.RulebookQueryEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One step of the multi-turn tool execution loop. The repository
 * streams a sequence of these for the UI: text-delta tokens while the
 * model is generating prose, and tool-call / tool-result events when
 * the model invokes a function.
 */
sealed interface AgentEvent {
    data class Text(val delta: String) : AgentEvent
    data class ToolCallStarted(val name: String) : AgentEvent
    data class ToolCallFinished(val name: String, val content: String, val isError: Boolean) : AgentEvent
    data class TurnFinished(val content: String, val toolCallCount: Int) : AgentEvent
    data class Failed(val message: String) : AgentEvent
}

@Singleton
class AIRepository @Inject constructor(
    private val providerStore: ProviderStore,
    private val userPreferences: UserPreferences,
    private val rulebookQueryEngine: RulebookQueryEngine,
    private val toolDispatcher: ToolDispatcher,
    private val openAICompatibleClient: OpenAICompatibleClient,
    private val agentRepository: AgentRepository
) {
    /**
     * Blocking, non-streaming chat. Kept for callers that just want the
     * final text and don't care about per-token updates.
     */
    suspend fun generateGMResponse(
        userMessage: String,
        conversationHistory: List<ChatMessage>,
        characterContext: String? = null
    ): String {
        val provider = providerStore.activeSnapshot()
            ?: return "⚠️ 请先在设置中配置一个 AI 提供商"

        val strictJson = userPreferences.strictJsonMode.firstOrNull() == true
        val model = userPreferences.defaultModel.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: provider.defaultModel

        val rulesContext = rulebookQueryEngine.getContextForAI(userMessage)
        val systemPrompt = buildSystemPrompt(characterContext, rulesContext, agentMemory = "", strictJson = strictJson)
        val messages = mutableListOf(ChatMessage(role = "system", content = systemPrompt))
        messages.addAll(conversationHistory.takeLast(10))
        messages.add(ChatMessage(role = "user", content = userMessage))

        val request = ChatRequest(
            model = model,
            messages = messages,
            tools = if (provider.supportsTools && !strictJson) ToolCatalog.all else null,
            toolChoice = if (provider.supportsTools && !strictJson) "auto" else null,
            responseFormat = if (strictJson) ResponseFormat.json() else null,
            maxTokens = 2000,
            temperature = 0.8
        )

        return try {
            val response = openAICompatibleClient.chat(provider, request)
            response.choices.firstOrNull()?.message?.content
                ?: "抱歉，AI GM 暂时无法回应。"
        } catch (e: Exception) {
            "⚠️ ${provider.name} 调用失败：${e.message}"
        }
    }

    /**
     * Streaming agent turn with multi-turn tool execution.
     *
     * Resolves the active provider from [ProviderStore] on each call so
     * the user can switch providers in the middle of a session.  If
     * [providerStore]'s active provider does not support tools/stream,
     * the call degrades gracefully to a single non-streaming round.
     */
    fun streamAgentTurn(
        sessionId: Long,
        userMessage: String,
        conversationHistory: List<ChatMessage>,
        characterContext: String? = null,
        maxToolRounds: Int = 6
    ): Flow<AgentEvent> = flow {
        val provider = providerStore.activeSnapshot()
        if (provider == null) {
            emit(AgentEvent.Failed("⚠️ 请先在设置中配置一个 AI 提供商"))
            return@flow
        }
        if (provider.apiKey.isBlank()) {
            emit(AgentEvent.Failed("⚠️ 提供商「${provider.name}」未填写 API Key"))
            return@flow
        }

        val strictJson = userPreferences.strictJsonMode.firstOrNull() == true
        val model = userPreferences.defaultModel.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: provider.defaultModel

        val rulesContext = withContext(Dispatchers.IO) {
            rulebookQueryEngine.getContextForAI(userMessage)
        }
        val agentMemory = withContext(Dispatchers.IO) {
            agentRepository.memoryAsText(sessionId, "GM")
        }
        val systemPrompt = buildSystemPrompt(characterContext, rulesContext, agentMemory, strictJson)
        val messages = mutableListOf(ChatMessage(role = "system", content = systemPrompt))
        messages.addAll(conversationHistory.takeLast(20))
        messages.add(ChatMessage(role = "user", content = userMessage))

        val canTools = provider.supportsTools && !strictJson
        var toolCallCount = 0
        var finalText = ""

        for (round in 0 until maxToolRounds) {
            val roundText = StringBuilder()
            val toolCalls = mutableListOf<ToolCall>()
            var finishReason = ""

            val request = ChatRequest(
                model = model,
                messages = messages,
                stream = provider.supportsStream,
                tools = if (canTools) ToolCatalog.all else null,
                toolChoice = if (canTools) "auto" else null,
                responseFormat = if (strictJson) ResponseFormat.json() else null,
                maxTokens = 2000,
                temperature = 0.8
            )

            if (provider.supportsStream) {
                openAICompatibleClient.stream(provider, request).collect { event ->
                    when (event) {
                        is OpenAICompatibleClient.StreamText -> {
                            if (event.text.isNotEmpty()) {
                                roundText.append(event.text)
                                emit(AgentEvent.Text(event.text))
                            }
                        }
                        is OpenAICompatibleClient.StreamTool -> {
                            toolCalls.add(event.toolCallDelta)
                        }
                        is OpenAICompatibleClient.StreamFinish -> {
                            finishReason = event.finishReason
                        }
                        is OpenAICompatibleClient.StreamEnd -> {
                            finishReason = event.finishReason
                        }
                        is OpenAICompatibleClient.StreamError -> {
                            emit(AgentEvent.Failed(event.message))
                            return@collect
                        }
                    }
                }
            } else {
                try {
                    val response = openAICompatibleClient.chat(provider, request)
                    val text = response.choices.firstOrNull()?.message?.content.orEmpty()
                    if (text.isNotEmpty()) {
                        roundText.append(text)
                        emit(AgentEvent.Text(text))
                    }
                    response.choices.firstOrNull()?.message?.toolCalls?.forEach { toolCalls.add(it) }
                    finishReason = response.choices.firstOrNull()?.finishReason.orEmpty()
                } catch (e: Exception) {
                    emit(AgentEvent.Failed(e.message ?: "unknown error"))
                    return@flow
                }
            }

            if (toolCalls.isEmpty()) {
                finalText = roundText.toString()
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

    private fun buildSystemPrompt(
        characterContext: String?,
        rulesContext: String,
        agentMemory: String,
        strictJson: Boolean = false
    ): String {
        val strictDirective = if (strictJson) {
            """
            |
            |## 响应格式
            |你**必须**只返回合法的 JSON 对象，结构如下：
            |{
            |  "narration": "你的叙事文本（中文，使用 markdown）",
            |  "tool_calls": [{"name": "roll_dice", "arguments": "{\"stat\":1,\"skill\":1,\"dv\":13}"}]
            |}
            |不要添加 JSON 之外的任何文本。不要使用 markdown 代码块包裹。
            |""".trimMargin()
        } else ""

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
$strictDirective

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
