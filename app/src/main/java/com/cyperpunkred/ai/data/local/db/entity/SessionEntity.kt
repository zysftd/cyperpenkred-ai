package com.cyperpunkred.ai.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterId: Long,
    val title: String,
    val status: String = "active",
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val role: String,
    val content: String,
    val diceResultJson: String? = null,
    val timestamp: Long
)

@Entity(tableName = "combat_logs")
data class CombatLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val round: Int,
    val actor: String,
    val action: String,
    val diceResultJson: String? = null,
    val damage: Int? = null,
    val target: String? = null,
    val timestamp: Long
)
