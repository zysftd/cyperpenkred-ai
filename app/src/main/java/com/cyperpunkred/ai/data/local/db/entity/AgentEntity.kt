package com.cyperpunkred.ai.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persistent memory for an in-world AI agent (the GM, an NPC, or a
 * future AI player). Each row is a (sessionId, agentName) pair holding
 * the agent's personality, current goals, and a JSON-encoded list of
 * memory snippets. The GM agent's [agentName] is conventionally "GM".
 */
@Entity(tableName = "agents")
data class AgentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val agentName: String,
    val role: String = "npc",
    val personality: String = "",
    val goalsJson: String = "[]",
    val memoriesJson: String = "[]",
    val updatedAt: Long
)
