package com.cyperpunkred.ai.domain.model

data class Combatant(
    val name: String,
    val isPlayer: Boolean,
    val initiative: Int = 0,
    val currentHP: Int = 0,
    val maxHP: Int = 0,
    val armorSP: Int = 0,
    val statusEffects: List<String> = emptyList()
)

data class CombatRoundResult(
    val round: Int,
    val entries: List<CombatEntry>,
    val isOver: Boolean,
    val winner: String? = null
)

data class CombatEntry(
    val actor: String,
    val action: String,
    val diceResult: Int,
    val damage: Int = 0,
    val target: String? = null,
    val description: String
)
