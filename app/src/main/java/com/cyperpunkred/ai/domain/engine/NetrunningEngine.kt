package com.cyperpunkred.ai.domain.engine

import com.cyperpunkred.ai.domain.model.DiceResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetrunningEngine @Inject constructor(
    private val diceEngine: DiceEngine
) {
    fun rollInterfaceCheck(intelligence: Int, cybersecSkill: Int): DiceResult {
        return diceEngine.rollSkillCheck(intelligence, cybersecSkill)
    }

    fun rollAttack(programSkill: Int): DiceResult {
        return diceEngine.rollSkillCheck(5, programSkill)
    }

    fun rollDefense(blackiceDefense: Int): DiceResult {
        return diceEngine.rollSkillCheck(5, blackiceDefense)
    }

    fun calculateNetDamage(attackResult: DiceResult, defenseResult: DiceResult): Int {
        val damage = maxOf(0, attackResult.total - defenseResult.total)
        return damage
    }
}
