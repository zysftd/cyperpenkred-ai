package com.cyperpunkred.ai.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "npcs")
data class NPCEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long? = null,
    val name: String,
    val role: String,
    val faction: String? = null,
    val statsJson: String? = null,
    val description: String,
    val relationship: String = "陌生人",
    val isAlive: Boolean = true
)

@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long? = null,
    val title: String,
    val description: String,
    val status: String = "active",
    val objectivesJson: String,
    val rewards: String? = null,
    val giver: String? = null,
    val createdAt: Long
)
