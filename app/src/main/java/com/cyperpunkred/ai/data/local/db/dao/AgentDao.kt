package com.cyperpunkred.ai.data.local.db.dao

import androidx.room.*
import com.cyperpunkred.ai.data.local.db.entity.AgentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentDao {
    @Query("SELECT * FROM agents WHERE sessionId = :sessionId")
    fun getAgentsForSession(sessionId: Long): Flow<List<AgentEntity>>

    @Query("SELECT * FROM agents WHERE sessionId = :sessionId AND agentName = :agentName LIMIT 1")
    suspend fun findAgent(sessionId: Long, agentName: String): AgentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAgent(agent: AgentEntity): Long

    @Update
    suspend fun updateAgent(agent: AgentEntity)

    @Query("DELETE FROM agents WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)
}
