package com.cyperpunkred.ai.data.local.db.dao

import androidx.room.*
import com.cyperpunkred.ai.data.local.db.entity.NPCEntity
import com.cyperpunkred.ai.data.local.db.entity.QuestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NPCDao {
    @Query("SELECT * FROM npcs WHERE sessionId = :sessionId")
    fun getNPCsForSession(sessionId: Long): Flow<List<NPCEntity>>

    @Query("SELECT * FROM npcs WHERE id = :id")
    fun getNPCById(id: Long): Flow<NPCEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNPC(npc: NPCEntity): Long

    @Update
    suspend fun updateNPC(npc: NPCEntity)

    @Query("DELETE FROM npcs WHERE id = :id")
    suspend fun deleteNPC(id: Long)
}

@Dao
interface QuestDao {
    @Query("SELECT * FROM quests ORDER BY createdAt DESC")
    fun getAllQuests(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE sessionId = :sessionId")
    fun getQuestsForSession(sessionId: Long): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE id = :id")
    fun getQuestById(id: Long): Flow<QuestEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: QuestEntity): Long

    @Update
    suspend fun updateQuest(quest: QuestEntity)

    @Query("DELETE FROM quests WHERE id = :id")
    suspend fun deleteQuest(id: Long)
}
