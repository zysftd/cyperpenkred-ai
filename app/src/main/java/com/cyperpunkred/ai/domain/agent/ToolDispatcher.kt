package com.cyperpunkred.ai.domain.agent

import com.cyperpunkred.ai.domain.engine.CharacterEngine
import com.cyperpunkred.ai.domain.engine.CombatEngine
import com.cyperpunkred.ai.domain.engine.DiceEngine
import com.cyperpunkred.ai.domain.engine.DiceEngine.DamageRollResult
import com.cyperpunkred.ai.domain.engine.NetrunningEngine
import com.cyperpunkred.ai.domain.knowledge.RulebookQueryEngine
import com.google.gson.Gson
import com.google.gson.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of a single tool execution. The string is appended to the
 * conversation as a `role: tool` message so the model can incorporate
 * the outcome in its next turn.
 */
data class ToolResult(
    val toolCallId: String,
    val name: String,
    val content: String,
    val isError: Boolean = false
)

/**
 * Maps a tool call from the model to actual side effects (dice rolls,
 * rulebook lookups, character sheet updates) and returns a string the
 * model can read on its next turn.
 */
@Singleton
class ToolDispatcher @Inject constructor(
    private val diceEngine: DiceEngine,
    private val combatEngine: CombatEngine,
    private val characterEngine: CharacterEngine,
    private val netrunningEngine: NetrunningEngine,
    private val rulebookQueryEngine: RulebookQueryEngine
) {
    private val gson = Gson()

    suspend fun dispatch(callId: String, name: String, argsJson: String): ToolResult {
        return try {
            val args = if (argsJson.isBlank()) JsonObject() else gson.fromJson(argsJson, JsonObject::class.java)
            val text = when (name) {
                "roll_dice" -> rollDice(args)
                "roll_initiative" -> rollInitiative(args)
                "roll_damage" -> rollDamage(args)
                "apply_damage" -> applyDamage(args)
                "check_inventory" -> checkInventory(args)
                "lookup_rule" -> lookupRule(args)
                "netrunner_backdoor" -> netrunnerBackdoor(args)
                "netrunner_zap" -> netrunnerZap(args)
                "netrunner_erode" -> netrunnerErode(args)
                "netrunner_escape" -> netrunnerEscape(args)
                "tech_fabricate" -> techFabricate(args)
                "tech_invent" -> techInvent(args)
                "roll_death_save" -> rollDeathSave(args)
                else -> "ERROR: unknown tool '$name'"
            }
            ToolResult(callId, name, text, isError = text.startsWith("ERROR"))
        } catch (e: Exception) {
            ToolResult(callId, name, "ERROR: ${e.javaClass.simpleName}: ${e.message}", isError = true)
        }
    }

    private fun rollDice(args: JsonObject): String {
        val stat = args.get("stat")?.asInt ?: 0
        val skill = args.get("skill")?.asInt ?: 0
        val modifier = args.get("modifier")?.asInt ?: 0
        val dv = args.get("dv")?.asInt ?: 0
        val result = diceEngine.rollSkillCheck(stat, skill, modifier)
        val outcome = when {
            result.isCritical -> "大成功！(骰出10，效果翻倍)"
            result.isFumble -> "大失败！(骰出1，严重负面效果)"
            result.total >= dv -> "成功"
            else -> "失败"
        }
        return "1d10(${result.dice.first()}) + ${stat}(属性) + ${skill}(技能) + ${modifier}(调整) = ${result.total} vs DV${dv} → $outcome"
    }

    private fun rollInitiative(args: JsonObject): String {
        val ref = args.get("ref")?.asInt ?: 0
        val modifier = args.get("modifier")?.asInt ?: 0
        val init = diceEngine.rollInitiative(ref, modifier)
        return "1d10(${init - ref - modifier}) + REF($ref) + 调整($modifier) = 先攻 $init"
    }

    private fun rollDamage(args: JsonObject): String {
        val dice = args.get("dice")?.asString ?: "1d6"
        val bonus = args.get("bonus")?.asInt ?: 0
        val r: DamageRollResult = diceEngine.rollDamage(dice)
        return "${dice}: [${r.dice.joinToString("+")}] = ${r.total} + ${bonus} = 伤害 ${r.total + bonus}（之后由调用方扣减护甲SP）"
    }

    private fun applyDamage(args: JsonObject): String {
        val target = args.get("target")?.asString ?: return "ERROR: missing target"
        val amount = args.get("amount")?.asInt ?: 0
        // Combat engine keeps the in-fight state; the AI updates the
        // narrative alongside this tool call. The return value is a
        // template the GM narrates verbatim.
        return "对 $target 造成 $amount 点伤害。HP 减少 $amount。"
    }

    private fun checkInventory(args: JsonObject): String {
        val character = args.get("character")?.asString ?: "未知"
        val item = args.get("item")?.asString ?: "未知"
        return "$character 的库存中查询 \"$item\"：默认认为角色拥有常见夜之城物资；稀有装备需要在叙事中明确给出。"
    }

    private suspend fun lookupRule(args: JsonObject): String {
        val query = args.get("query")?.asString ?: return "ERROR: missing query"
        val entries = rulebookQueryEngine.queryByKeywords(
            rulebookQueryEngine.extractKeywords(query).ifEmpty { listOf(query) },
            limit = 3
        )
        if (entries.isEmpty()) {
            return "在规则书知识库中没有找到与 \"$query\" 直接相关的内容，请基于赛博朋克RED公开规则合理推断并明确标注为'非规则书原文'。"
        }
        return entries.joinToString("\n\n") { e ->
            "### ${e.title}\n${e.content}"
        }
    }

    private fun netrunnerBackdoor(args: JsonObject): String {
        val int = args.get("intelligence")?.asInt ?: 0
        val iface = args.get("interface")?.asInt ?: 0
        val dv = args.get("architecture_dv")?.asInt ?: 8
        val result = diceEngine.rollSkillCheck(int, iface)
        val outcome = if (result.total >= dv) "后门植入成功（可重入）" else "后门植入失败"
        return "Interface: 1d10(${result.dice.first()}) + INT($int) + Interface($iface) = ${result.total} vs NET架构DV$dv → $outcome"
    }

    private fun netrunnerZap(args: JsonObject): String {
        val weapon = args.get("weapon_skill")?.asInt ?: 5
        val program = args.get("program_skill")?.asInt ?: 0
        val defense = args.get("defender_skill")?.asInt ?: 0
        val attack = diceEngine.rollSkillCheck(weapon, program)
        val def = diceEngine.rollSkillCheck(5, defense)
        val damage = netrunningEngine.calculateNetDamage(attack, def)
        val outcome = if (damage > 0) "命中，造成 $damage 点网域伤害" else "未穿透防御"
        return "网行者攻击: 1d10(${attack.dice.first()}) + WEAPON($weapon) + Program($program) = ${attack.total}\n" +
            "防御方: 1d10(${def.dice.first()}) + 防御($defense) = ${def.total}\n" +
            "→ $outcome"
    }

    private fun netrunnerErode(args: JsonObject): String {
        val int = args.get("intelligence")?.asInt ?: 0
        val iface = args.get("interface")?.asInt ?: 0
        val defense = args.get("program_defense")?.asInt ?: 0
        val attack = diceEngine.rollSkillCheck(int, iface)
        val outcome = if (attack.total >= defense) "程序被削掉1级" else "侵蚀失败"
        return "Erode: 1d10(${attack.dice.first()}) + INT($int) + Interface($iface) = ${attack.total} vs 程序防御$defense → $outcome"
    }

    private fun netrunnerEscape(args: JsonObject): String {
        val int = args.get("intelligence")?.asInt ?: 0
        val iface = args.get("interface")?.asInt ?: 0
        val dv = args.get("dv")?.asInt ?: 15
        val result = diceEngine.rollSkillCheck(int, iface)
        val outcome = if (result.total >= dv) "成功脱离网络" else "脱离失败，承受黑冰追踪伤害"
        return "Jack out: 1d10(${result.dice.first()}) + INT($int) + Interface($iface) = ${result.total} vs DV$dv → $outcome"
    }

    private fun techFabricate(args: JsonObject): String {
        val tech = args.get("tech_skill")?.asInt ?: 0
        val skill = args.get("tech_skill_rank")?.asInt ?: 0
        val dv = args.get("dv")?.asInt ?: 13
        val result = diceEngine.rollSkillCheck(tech, skill)
        val outcome = when {
            result.isCritical -> "大成功！额外奖励槽位 + 1"
            result.isFumble -> "大失败，材料报废"
            result.total >= dv -> "制造成功"
            else -> "制造失败"
        }
        return "Fabricate: 1d10(${result.dice.first()}) + TECH($tech) + Tech技能($skill) = ${result.total} vs DV$dv → $outcome"
    }

    private fun techInvent(args: JsonObject): String {
        val tech = args.get("tech_skill")?.asInt ?: 0
        val skill = args.get("tech_skill_rank")?.asInt ?: 0
        val dv = args.get("dv")?.asInt ?: 20
        val result = diceEngine.rollSkillCheck(tech, skill)
        val outcome = when {
            result.isCritical -> "突破性发明！额外效果"
            result.total >= dv -> "发明成功，定义新物品"
            else -> "失败，需要更多研究"
        }
        return "Invent: 1d10(${result.dice.first()}) + TECH($tech) + Tech技能($skill) = ${result.total} vs DV$dv → $outcome"
    }

    private fun rollDeathSave(args: JsonObject): String {
        val body = args.get("body")?.asInt ?: 4
        val r = diceEngine.rollDeathSave()
        val outcome = if (r.total >= body) "死亡豁免通过" else "死亡豁免失败"
        return "死亡豁免: 1d10(${r.total}) vs BODY($body) → $outcome"
    }
}
