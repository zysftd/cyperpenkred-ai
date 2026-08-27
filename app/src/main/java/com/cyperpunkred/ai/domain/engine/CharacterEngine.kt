package com.cyperpunkred.ai.domain.engine

import com.cyperpunkred.ai.domain.model.Role
import com.cyperpunkred.ai.domain.model.Stats
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterEngine @Inject constructor() {

    fun calculateMaxHP(role: Role, body: Int): Int {
        val baseHP = 10
        val bodyBonus = body * 2
        val roleBonus = when (role) {
            Role.SOLO -> 5
            Role.NOMAD -> 3
            Role.TECH -> 2
            Role.MEDTECH -> 2
            else -> 0
        }
        return baseHP + bodyBonus + roleBonus
    }

    fun calculateDamageBonus(body: Int): Int {
        return when {
            body <= 2 -> -2
            body <= 4 -> -1
            body <= 6 -> 0
            body <= 8 -> 1
            else -> 2
        }
    }

    fun calculateMoveSpeed(ma: Int): Int {
        return when {
            ma <= 2 -> 2
            ma <= 4 -> 3
            ma <= 6 -> 4
            ma <= 8 -> 5
            else -> 6
        }
    }

    fun calculateHumanityLoss(cyberwareCost: Int): Int {
        return cyberwareCost
    }

    fun getRoleStatBonuses(role: Role): Stats {
        return when (role) {
            Role.ROCKERBOY -> Stats(cool = 4, attractiveness = 2, empathy = 2)
            Role.SOLO -> Stats(reflex = 4, body = 2, cool = 2)
            Role.NETRUNNER -> Stats(intelligence = 4, technique = 2, reflex = 2)
            Role.TECH -> Stats(technique = 4, intelligence = 2, dexterity = 2)
            Role.MEDTECH -> Stats(technique = 4, empathy = 2, intelligence = 2)
            Role.MEDIA -> Stats(cool = 4, intelligence = 2, empathy = 2)
            Role.EXEC -> Stats(cool = 4, empathy = 2, intelligence = 2)
            Role.LAWMAN -> Stats(reflex = 4, cool = 2, body = 2)
            Role.FIXER -> Stats(cool = 4, intelligence = 2, attractiveness = 2)
            Role.NOMAD -> Stats(body = 4, reflex = 2, cool = 2)
        }
    }
}
