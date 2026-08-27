package com.cyperpunkred.ai.data.repository

import com.cyperpunkred.ai.data.local.db.dao.NPCDao
import com.cyperpunkred.ai.data.local.db.dao.QuestDao
import com.cyperpunkred.ai.data.local.db.entity.NPCEntity
import com.cyperpunkred.ai.data.local.db.entity.QuestEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NPCRepository @Inject constructor(
    private val npcDao: NPCDao
) {
    fun getNPCsForSession(sessionId: Long): Flow<List<NPCEntity>> =
        npcDao.getNPCsForSession(sessionId)

    suspend fun insertNPC(npc: NPCEntity): Long = npcDao.insertNPC(npc)

    suspend fun updateNPC(npc: NPCEntity) = npcDao.updateNPC(npc)

    suspend fun deleteNPC(id: Long) = npcDao.deleteNPC(id)
}

@Singleton
class QuestRepository @Inject constructor(
    private val questDao: QuestDao
) {
    fun getAllQuests(): Flow<List<QuestEntity>> = questDao.getAllQuests()

    fun getQuestsForSession(sessionId: Long): Flow<List<QuestEntity>> =
        questDao.getQuestsForSession(sessionId)

    suspend fun insertQuest(quest: QuestEntity): Long = questDao.insertQuest(quest)

    suspend fun updateQuest(quest: QuestEntity) = questDao.updateQuest(quest)

    suspend fun deleteQuest(id: Long) = questDao.deleteQuest(id)
}
