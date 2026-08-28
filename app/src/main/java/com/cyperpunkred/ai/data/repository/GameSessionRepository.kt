package com.cyperpunkred.ai.data.repository

import com.cyperpunkred.ai.data.local.db.dao.SessionDao
import com.cyperpunkred.ai.data.local.db.dao.ChatMessageDao
import com.cyperpunkred.ai.data.local.db.dao.CombatLogDao
import com.cyperpunkred.ai.data.local.db.entity.SessionEntity
import com.cyperpunkred.ai.data.local.db.entity.ChatMessageEntity
import com.cyperpunkred.ai.data.local.db.entity.CombatLogEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Outcome of trying to start a new game for a character. Strong
 * binding between character and session is enforced here:
 *   - [NoSuchCharacter]    the character id doesn't exist
 *   - [ExistingActive]     the character already has an active
 *                          session; reuse it rather than start a
 *                          parallel adventure
 *   - [Created]            a brand-new session row was inserted
 */
sealed interface StartSessionResult {
    data class Created(val sessionId: Long) : StartSessionResult
    data class ExistingActive(val sessionId: Long) : StartSessionResult
    object NoSuchCharacter : StartSessionResult
}

@Singleton
class GameSessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val chatMessageDao: ChatMessageDao,
    private val combatLogDao: CombatLogDao
) {
    fun getAllSessions(): Flow<List<SessionEntity>> = sessionDao.getAllSessions()

    fun getRecentSessions(): Flow<List<SessionEntity>> = sessionDao.getRecentSessions()

    fun getSessionById(id: Long): Flow<SessionEntity?> = sessionDao.getSessionById(id)

    suspend fun getSessionByIdOnce(id: Long): SessionEntity? = sessionDao.getSessionByIdOnce(id)

    /**
     * Start a new game bound to [characterId]. Returns
     * [StartSessionResult.ExistingActive] if that character already
     * has an in-progress adventure so the caller can resume it
     * instead of opening a duplicate.
     */
    suspend fun startSession(characterId: Long): StartSessionResult {
        val existing = sessionDao.getActiveSessionForCharacter(characterId)
        if (existing != null) return StartSessionResult.ExistingActive(existing.id)
        val now = System.currentTimeMillis()
        val id = sessionDao.insertSession(
            SessionEntity(
                characterId = characterId,
                title = "新的冒险",
                status = "active",
                createdAt = now,
                updatedAt = now
            )
        )
        return StartSessionResult.Created(id)
    }

    suspend fun insertSession(session: SessionEntity): Long = sessionDao.insertSession(session)

    suspend fun updateSession(session: SessionEntity) = sessionDao.updateSession(session)

    suspend fun deleteSession(id: Long) = sessionDao.deleteSession(id)

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>> =
        chatMessageDao.getMessagesForSession(sessionId)

    suspend fun addMessage(message: ChatMessageEntity): Long =
        chatMessageDao.insertMessage(message)

    fun getCombatLogs(sessionId: Long): Flow<List<CombatLogEntity>> =
        combatLogDao.getLogsForSession(sessionId)

    suspend fun addCombatLog(log: CombatLogEntity): Long =
        combatLogDao.insertLog(log)
}
