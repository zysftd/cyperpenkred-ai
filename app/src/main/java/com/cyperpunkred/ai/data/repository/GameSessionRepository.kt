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

@Singleton
class GameSessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val chatMessageDao: ChatMessageDao,
    private val combatLogDao: CombatLogDao
) {
    fun getAllSessions(): Flow<List<SessionEntity>> = sessionDao.getAllSessions()

    fun getRecentSessions(): Flow<List<SessionEntity>> = sessionDao.getRecentSessions()

    fun getSessionById(id: Long): Flow<SessionEntity?> = sessionDao.getSessionById(id)

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
