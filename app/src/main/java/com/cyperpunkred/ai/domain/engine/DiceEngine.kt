package com.cyperpunkred.ai.domain.engine

import com.cyperpunkred.ai.domain.model.DiceResult
import kotlin.random.Random

object DiceEngine {

    fun rollD10(): Int = Random.nextInt(1, 11)

    fun roll2D6(): Int = Random.nextInt(1, 7) + Random.nextInt(1, 7)

    fun rollD6(): Int = Random.nextInt(1, 7)

    fun rollSkillCheck(stat: Int, skill: Int, modifier: Int = 0): DiceResult {
        val dice = rollD10()
        val total = dice + stat + skill + modifier
        val isCritical = dice == 10
        val isFumble = dice == 1
        return DiceResult(
            dice = listOf(dice),
            modifier = stat + skill + modifier,
            total = total,
            isCritical = isCritical,
            isFumble = isFumble
        )
    }

    fun rollInitiative(ref: Int, modifier: Int = 0): Int {
        return rollD10() + ref + modifier
    }

    fun rollDamage(damageDice: String): DamageRollResult {
        val parts = damageDice.lowercase().replace(" ", "").split("d")
        val count = parts[0].toIntOrNull() ?: 1
        val sides = parts.getOrNull(1)?.toIntOrNull() ?: 6
        val results = (1..count).map { Random.nextInt(1, sides + 1) }
        return DamageRollResult(
            dice = results,
            total = results.sum()
        )
    }

    fun rollDeathSave(): DiceResult {
        val dice = rollD10()
        return DiceResult(
            dice = listOf(dice),
            modifier = 0,
            total = dice,
            isCritical = dice >= 10,
            isFumble = dice <= 2
        )
    }

    data class DamageRollResult(
        val dice: List<Int>,
        val total: Int
    )
}
