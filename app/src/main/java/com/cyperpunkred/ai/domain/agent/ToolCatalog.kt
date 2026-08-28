package com.cyperpunkred.ai.domain.agent

import com.cyperpunkred.ai.data.remote.model.Tool
import com.cyperpunkred.ai.data.remote.model.ToolFunction
import com.cyperpunkred.ai.data.remote.model.ToolParameters
import com.cyperpunkred.ai.data.remote.model.ToolProperty

/**
 * Catalogue of every tool the GM (or any other AI agent) can call. The
 * shape mirrors the OpenAI Function-Calling schema so it can be passed
 * straight through to /v1/chat/completions as the `tools` array.
 */
object ToolCatalog {

    private fun p(type: String, desc: String, enum: List<String>? = null) =
        ToolProperty(type = type, description = desc, enum = enum)

    val rollDice = Tool(
        function = ToolFunction(
            name = "roll_dice",
            description = "Roll a d10 skill check. stat + skill + modifier vs a difficulty value (DV). Returns the dice, total, and whether the roll was a critical (10) or fumble (1).",
            parameters = ToolParameters(
                properties = mapOf(
                    "stat" to p("integer", "The governing stat (e.g. REF, DEX, INT, COOL, BODY, TECH, EMP, WILL, LUCK, MOVE)."),
                    "skill" to p("integer", "The skill rank at this action (0 if untrained)."),
                    "modifier" to p("integer", "Any situational modifier (positive or negative). Default 0."),
                    "dv" to p("integer", "The difficulty value the roll must meet or exceed.")
                ),
                required = listOf("stat", "skill", "dv")
            )
        )
    )

    val rollInitiative = Tool(
        function = ToolFunction(
            name = "roll_initiative",
            description = "Roll 1d10 + REF for a combatant's initiative at the start of combat.",
            parameters = ToolParameters(
                properties = mapOf(
                    "ref" to p("integer", "The combatant's REF stat."),
                    "modifier" to p("integer", "Optional situational modifier. Default 0.")
                ),
                required = listOf("ref")
            )
        )
    )

    val rollDamage = Tool(
        function = ToolFunction(
            name = "roll_damage",
            description = "Roll weapon damage dice, e.g. 2d6 for a medium pistol. The caller subtracts the target's armor SP.",
            parameters = ToolParameters(
                properties = mapOf(
                    "dice" to p("string", "Dice expression such as '1d6', '2d6', '3d6', '4d6', '5d10'."),
                    "bonus" to p("integer", "Flat bonus to add to the rolled total. Default 0.")
                ),
                required = listOf("dice")
            )
        )
    )

    val applyDamage = Tool(
        function = ToolFunction(
            name = "apply_damage",
            description = "Subtract damage from a combatant. Use after roll_damage and after subtracting armor SP. If HP drops to 0 the target is mortally wounded and must roll death saves.",
            parameters = ToolParameters(
                properties = mapOf(
                    "target" to p("string", "Name of the combatant taking the damage."),
                    "amount" to p("integer", "Damage after armor SP has been subtracted.")
                ),
                required = listOf("target", "amount")
            )
        )
    )

    val checkInventory = Tool(
        function = ToolFunction(
            name = "check_inventory",
            description = "Look up whether the named character currently has a piece of equipment, cyberware, or weapon in their sheet.",
            parameters = ToolParameters(
                properties = mapOf(
                    "character" to p("string", "Character name."),
                    "item" to p("string", "Item name to look up.")
                ),
                required = listOf("character", "item")
            )
        )
    )

    val lookupRule = Tool(
        function = ToolFunction(
            name = "lookup_rule",
            description = "Search the bundled Cyberpunk RED rulebook (112 wiki entries) for any mechanic the AI is unsure about. Returns the matching section verbatim so the answer is grounded in the official text.",
            parameters = ToolParameters(
                properties = mapOf(
                    "query" to p("string", "Free-text query, e.g. '死亡豁免', '黑冰 电火花', '网行者 后门', '义体 人性'.")
                ),
                required = listOf("query")
            )
        )
    )

    val netrunnerBackdoor = Tool(
        function = ToolFunction(
            name = "netrunner_backdoor",
            description = "Attempt to plant a backdoor on a NET Architecture. Resolution: Interface check (1d10 + INT + Interface skill, DV = NET Architecture's NET check). On success the netrunner can later re-enter without another Interface check.",
            parameters = ToolParameters(
                properties = mapOf(
                    "intelligence" to p("integer", "Netrunner's INT stat."),
                    "interface" to p("integer", "Netrunner's Interface skill rank."),
                    "architecture_dv" to p("integer", "The NET Architecture's NET rating (typically 7-12).")
                ),
                required = listOf("intelligence", "interface", "architecture_dv")
            )
        )
    )

    val netrunnerZap = Tool(
        function = ToolFunction(
            name = "netrunner_zap",
            description = "Run a single Netrunner program (ZAP / banhammer / etc.) against a Black ICE or netrunner defender. Resolution: 1d10 + WEAPON + program skill, defender rolls 1d10 + Defense. Damage = max(0, attack - defense).",
            parameters = ToolParameters(
                properties = mapOf(
                    "weapon_skill" to p("integer", "Netrunner's WEAPON stat (1d10 base)."),
                    "program_skill" to p("integer", "Netrunner's rank in the program being used."),
                    "defender_skill" to p("integer", "Defender's Defense (Black ICE) or WEAPON + program skill (other netrunner).")
                ),
                required = listOf("weapon_skill", "program_skill", "defender_skill")
            )
        )
    )

    val netrunnerErode = Tool(
        function = ToolFunction(
            name = "netrunner_erode",
            description = "Resolve an 'erode' event (anti-program attack by a Black ICE). 1d10 + INT + Interface vs the program's defense. On hit, the program loses 1 rank (or is destroyed at 0).",
            parameters = ToolParameters(
                properties = mapOf(
                    "intelligence" to p("integer", "Netrunner's INT."),
                    "interface" to p("integer", "Netrunner's Interface skill rank."),
                    "program_defense" to p("integer", "Defense of the target program.")
                ),
                required = listOf("intelligence", "interface", "program_defense")
            )
        )
    )

    val netrunnerEscape = Tool(
        function = ToolFunction(
            name = "netrunner_escape",
            description = "Attempt to jack out of the NET under pressure. 1d10 + INT + Interface vs DV based on active threats. Failure means additional damage or a black ICE trace.",
            parameters = ToolParameters(
                properties = mapOf(
                    "intelligence" to p("integer", "Netrunner's INT."),
                    "interface" to p("integer", "Netrunner's Interface skill rank."),
                    "dv" to p("integer", "Difficulty (10 = light pressure, 15 = hot NET, 20 = black ICE on trace).")
                ),
                required = listOf("intelligence", "interface", "dv")
            )
        )
    )

    val techFabricate = Tool(
        function = ToolFunction(
            name = "tech_fabricate",
            description = "A Tech-role character fabricates a custom item: pick a fabrication DV, pay time and cost, and roll 1d10 + TECH + relevant Tech skill. On 10 (critical) the item gains a bonus slot.",
            parameters = ToolParameters(
                properties = mapOf(
                    "tech_skill" to p("integer", "Tech character's TECH stat."),
                    "tech_skill_rank" to p("integer", "Tech character's relevant Tech skill rank (Weapon Tech, Armor Tech, Basic Tech, Cybertech)."),
                    "dv" to p("integer", "Fabrication DV from the table (typically 13 for a basic custom job, 16 for masterwork, 20+ for legendary).")
                ),
                required = listOf("tech_skill", "tech_skill_rank", "dv")
            )
        )
    )

    val techInvent = Tool(
        function = ToolFunction(
            name = "tech_invent",
            description = "Invent a brand-new schematic the world has never seen. Roll 1d10 + TECH + relevant skill at a high DV (20+). On success define the item's stats inline; on 10 it's a breakthrough with a bonus effect.",
            parameters = ToolParameters(
                properties = mapOf(
                    "tech_skill" to p("integer", "Tech character's TECH stat."),
                    "tech_skill_rank" to p("integer", "Tech character's relevant Tech skill rank."),
                    "dv" to p("integer", "Always 20 or higher for invention.")
                ),
                required = listOf("tech_skill", "tech_skill_rank", "dv")
            )
        )
    )

    val rollDeathSave = Tool(
        function = ToolFunction(
            name = "roll_death_save",
            description = "Roll a Death Save when a character is at 0 HP. 1d10 vs BODY. On 1-2 the save is failed (track failure). Three failures mean death.",
            parameters = ToolParameters(
                properties = mapOf(
                    "body" to p("integer", "The character's BODY stat.")
                ),
                required = listOf("body")
            )
        )
    )

    val all: List<Tool> = listOf(
        rollDice,
        rollInitiative,
        rollDamage,
        applyDamage,
        checkInventory,
        lookupRule,
        netrunnerBackdoor,
        netrunnerZap,
        netrunnerErode,
        netrunnerEscape,
        techFabricate,
        techInvent,
        rollDeathSave
    )
}
