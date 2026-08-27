package com.cyperpunkred.ai.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val role: String,
    val age: Int,
    val statsJson: String,
    val skillsJson: String,
    val abilitiesJson: String,
    val cyberwareJson: String,
    val equipmentJson: String,
    val backstoryJson: String,
    val lifepathJson: String,
    val humanity: Int,
    val currentHP: Int,
    val maxHP: Int,
    val armorSP: Int,
    val createdAt: Long,
    val updatedAt: Long
)
