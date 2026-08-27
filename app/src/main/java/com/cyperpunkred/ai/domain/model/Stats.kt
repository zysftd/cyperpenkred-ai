package com.cyperpunkred.ai.domain.model

data class Stats(
    val intelligence: Int = 2,
    val reflex: Int = 2,
    val dexterity: Int = 2,
    val technique: Int = 2,
    val cool: Int = 2,
    val attractiveness: Int = 2,
    val luck: Int = 2,
    val movementAllowance: Int = 2,
    val body: Int = 2,
    val empathy: Int = 2
) {
    val totalPoints: Int
        get() = intelligence + reflex + dexterity + technique + cool +
                attractiveness + luck + movementAllowance + body + empathy

    fun withStat(abbr: String, value: Int): Stats = when (abbr) {
        "INT" -> copy(intelligence = value)
        "REF" -> copy(reflex = value)
        "DEX" -> copy(dexterity = value)
        "TECH" -> copy(technique = value)
        "COOL" -> copy(cool = value)
        "ATTR" -> copy(attractiveness = value)
        "LUCK" -> copy(luck = value)
        "MA" -> copy(movementAllowance = value)
        "BODY" -> copy(body = value)
        "EMP" -> copy(empathy = value)
        else -> this
    }
}
