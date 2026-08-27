package com.cyperpunkred.ai.domain.model

data class Character(
    val id: Long = 0,
    val name: String,
    val role: Role,
    val age: Int = 20,
    val stats: Stats = Stats(),
    val skills: Skills = Skills(),
    val abilities: List<String> = emptyList(),
    val cyberware: List<Cyberware> = emptyList(),
    val equipment: List<String> = emptyList(),
    val humanity: Int = 100,
    val currentHP: Int = 40,
    val maxHP: Int = 40,
    val armorSP: Int = 0
)
