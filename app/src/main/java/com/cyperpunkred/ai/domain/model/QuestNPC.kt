package com.cyperpunkred.ai.domain.model

data class NPC(
    val id: Long = 0,
    val name: String,
    val role: String,
    val faction: String? = null,
    val stats: Stats? = null,
    val description: String,
    val relationship: String = "陌生人",
    val isAlive: Boolean = true
)

data class Quest(
    val id: Long = 0,
    val title: String,
    val description: String,
    val status: QuestStatus = QuestStatus.ACTIVE,
    val objectives: List<String> = emptyList(),
    val rewards: String? = null,
    val giver: String? = null,
    val sessionId: Long? = null
)

enum class QuestStatus {
    ACTIVE,
    COMPLETED,
    FAILED
}
