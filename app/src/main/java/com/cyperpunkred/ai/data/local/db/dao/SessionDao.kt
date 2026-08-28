package com.cyperpunkred.ai.data.local.db.dao

import androidx.room.*
import com.cyperpunkred.ai.data.local.db.entity.SessionEntity
import com.cyperpunkred.ai.data.local.db.entity.ChatMessageEntity
import com.cyperpunkred.ai.data.local.db.entity.CombatLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM game_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM game_sessions ORDER BY updatedAt DESC LIMIT 5")
    fun getRecentSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM game_sessions WHERE id = :id")
    fun getSessionById(id: Long): Flow<SessionEntity?>

    @Query("SELECT * FROM game_sessions WHERE id = :id")
    suspend fun getSessionByIdOnce(id: Long): SessionEntity?

    /**
     * Returns the most recent active session for [characterId], or
     * null if the character has no running adventure. Used to
     * enforce the 1-character-per-1-active-session rule when the
     * user tries to start a new game.
     */
    @Query("SELECT * FROM game_sessions WHERE characterId = :characterId AND status = 'active' ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getActiveSessionForCharacter(characterId: Long): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("DELETE FROM game_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Long)
}

@Dao
interface CombatLogDao {
    @Query("SELECT * FROM combat_logs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getLogsForSession(sessionId: Long): Flow<List<CombatLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CombatLogEntity): Long

    @Query("DELETE FROM combat_logs WHERE sessionId = :sessionId")
    suspend fun deleteLogsForSession(sessionId: Long)
}
