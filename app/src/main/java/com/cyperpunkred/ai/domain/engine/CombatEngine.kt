package com.cyperpunkred.ai.domain.engine

import com.cyperpunkred.ai.domain.model.Combatant
import com.cyperpunkred.ai.domain.model.CombatEntry
import com.cyperpunkred.ai.domain.model.CombatRoundResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CombatEngine @Inject constructor(
    private val diceEngine: DiceEngine
) {
    fun processRound(
        combatants: List<Combatant>,
        round: Int
    ): CombatRoundResult {
        val entries = mutableListOf<CombatEntry>()
        val updatedCombatants = combatants.toMutableList()

        for ((index, combatant) in combatants.withIndex()) {
            if (combatant.currentHP <= 0) continue

            val attackRoll = diceEngine.rollSkillCheck(5, 6)
            val target = combatants.firstOrNull {
                it != combatant && it.currentHP > 0
            }

            if (target != null) {
                val damageResult = diceEngine.roll2D6()
                val finalDamage = maxOf(0, damageResult - target.armorSP)

                entries.add(
                    CombatEntry(
                        actor = combatant.name,
                        action = "攻击",
                        diceResult = attackRoll.total,
                        damage = finalDamage,
                        target = target.name,
                        description = "${combatant.name} 攻击 ${target.name}，造成 $finalDamage 点伤害"
                    )
                )

                val targetIndex = combatants.indexOfFirst { it.name == target.name }
                if (targetIndex >= 0) {
                    updatedCombatants[targetIndex] = target.copy(
                        currentHP = maxOf(0, target.currentHP - finalDamage)
                    )
                }
            }
        }

        val aliveCount = updatedCombatants.count { it.currentHP > 0 }
        val isOver = aliveCount <= 1
        val winner = if (isOver) updatedCombatants.firstOrNull { it.currentHP > 0 }?.name else null

        return CombatRoundResult(
            round = round,
            entries = entries,
            isOver = isOver,
            winner = winner
        )
    }
}
