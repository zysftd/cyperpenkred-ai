package com.cyperpunkred.ai.domain.model

data class RulebookEntry(
    val id: Long = 0,
    val category: String,
    val section: String,
    val title: String,
    val content: String,
    val keywords: List<String>,
    val tables: String? = null,
    val relatedEntryIds: List<Long> = emptyList(),
    val pageNumber: Int? = null
)

data class DiceResult(
    val dice: List<Int>,
    val modifier: Int,
    val total: Int,
    val isCritical: Boolean = false,
    val isFumble: Boolean = false
)

data class DamageResult(
    val baseDamage: Int,
    val armorReduction: Int,
    val finalDamage: Int,
    val isHeadshot: Boolean = false,
    val isCritical: Boolean = false
)
