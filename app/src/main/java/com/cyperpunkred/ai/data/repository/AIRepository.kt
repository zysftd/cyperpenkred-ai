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
        if (apiKey.isBlank()) {
            return "⚠️ 请先在设置中配置 OpenAI API Key"
        }

        val rulesContext = rulebookQueryEngine.getContextForAI(userMessage)

        val systemPrompt = buildSystemPrompt(characterContext, rulesContext)

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
        rulesContext: String
    ): String {
        return """
# 角色：你是赛博朋克红的AI游戏主持人（GM）

## 你的身份
- 你是夜之城的叙述者，一个身经百战的边缘行者
- 你的风格：黑暗、霓虹灯、企业阴谋、街头生存、赛博朋克
- 使用中文对话，偶尔夹杂赛博俚语（如"pling"表示欧元，"choom"表示朋友）

## 核心规则（必须遵守）

### 骰点判定
- **技能判定**：1d10 + 属性值 + 技能等级 vs 目标值(DV)
- **先攻**：1d10 + REF（反应）
- **伤害**：武器伤害骰 - 护甲SP
- **死亡豁免**：1d10 vs 体格值（HP≤0时触发）
- **大成功**：骰出10，效果翻倍
- **大失败**：骰出1，严重负面效果

### 战斗规则
- 每回合可以做1个动作 + 1个自由动作
- **射击**：1d10 + REF + 手枪/长枪技能 vs DV（近距离DV=13，中距离DV=15，远距离DV=20）
- **瞄准射击**：需要2个动作，伤害x2
- **闪避**：1d10 + REF + 闪避技能，作为对手射击的DV
- **近战**：1d10 + REF + 近战武器/搏击技能 vs DV
- **范围攻击**：仅削减身体护甲，无法瞄准头部
- **护甲**：SP值减少伤害，护甲被击中后SP降低

### 伤害与死亡
- HP = 10 + (体格x2) + 职业加成
- 严重伤势：身体/头部各有伤势表（2d6查表）
- 死亡豁免：每轮1d10，失败累积惩罚，3次失败死亡
- 稳定伤势：急救技能可以稳定濒死角色

### 义体规则
- 安装义体消耗人性值（10 x 共情值 = 初始人性）
- 人性值降至0：赛博精神病发作
- 义体提供属性修正和特殊能力

### 经济系统
- 欧元(eb)是主要货币
- 日常物品：1-10eb
- 略贵：20-50eb
- 高价：100eb
- 昂贵：500eb
- 特贵：1000eb
- 奢侈：5000eb+

${rulesContext}

## 角色信息
${characterContext ?: "玩家角色尚未创建。请等待玩家完成角色创建。"}

## GM指令

### 场景描述
- 描述场景时要有画面感：霓虹灯、雨水、废墟、全息广告
- 使用五感描写：视觉（霓虹）、听觉（枪声/音乐）、嗅觉（垃圾/合成食物）、触觉（雨水/金属）、味觉（廉价威士忌）
- 体现赛博朋克核心主题：高科技、低生活

### NPC对话
- 每个NPC要有独特的说话方式和个性
- 体现夜之城的残酷：背叛、贪婪、生存
- 重要NPC可以有赛博义体的描述

### 骰点判定
- 当玩家行动需要判定时，明确说明判定类型和目标值
- 展示骰点计算过程（如：1d10(7) + REF(5) + 手枪(6) = 18 vs DV15，成功！）
- 大成功/大失败要特别描述效果

### 剧情推进
- 使用节拍图表推进剧情：开端→发展→悬念→高潮→解决
- 适当引入随机遭遇增加紧张感
- 企业阴谋、帮派冲突、街头生存是核心主题

### 战斗描述
- 战斗要紧张刺激，描述动作细节
- 展示伤害计算过程
- 严重伤势要戏剧化描述
- 保持快节奏

### 回复格式
- 使用markdown格式
- 场景描述用**粗体**或普通文本
- NPC对话用引号
- 骰点结果用代码块或加粗
- 战斗动作清晰列出
""".trimIndent()
    }
}
