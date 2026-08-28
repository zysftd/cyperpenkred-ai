package com.cyperpunkred.ai.data.repository

import com.cyperpunkred.ai.data.local.db.dao.AgentDao
import com.cyperpunkred.ai.data.local.db.entity.AgentEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent memory for in-world AI agents (GM, NPCs, future AI
 * players). Memory is stored as a JSON array of short snippets; the
 * [AIRepository] reads the agent's memory and goals into the system
 * prompt on each turn, and writes new snippets back as the story
 * progresses.
 */
@Singleton
class AgentRepository @Inject constructor(
    private val agentDao: AgentDao
) {
    private val gson = Gson()

    fun observeForSession(sessionId: Long): Flow<List<AgentEntity>> =
        agentDao.getAgentsForSession(sessionId)

    suspend fun getOrCreate(sessionId: Long, name: String, role: String, personality: String = ""): AgentEntity {
        agentDao.findAgent(sessionId, name)?.let { return it }
        val fresh = AgentEntity(
            sessionId = sessionId,
            agentName = name,
            role = role,
            personality = personality,
            updatedAt = System.currentTimeMillis()
        )
        agentDao.upsertAgent(fresh)
        return fresh
    }

    suspend fun appendMemory(sessionId: Long, name: String, snippet: String, keep: Int = 30) {
        val agent = getOrCreate(sessionId, name, role = "gm")
        val existing: MutableList<String> = decode(agent.memoriesJson).toMutableList()
        existing += snippet
        val trimmed = if (existing.size > keep) existing.takeLast(keep) else existing
        agentDao.updateAgent(agent.copy(memoriesJson = encode(trimmed), updatedAt = System.currentTimeMillis()))
    }

    suspend fun setGoals(sessionId: Long, name: String, goals: List<String>) {
        val agent = getOrCreate(sessionId, name, role = "gm")
        agentDao.updateAgent(agent.copy(goalsJson = encode(goals), updatedAt = System.currentTimeMillis()))
    }

    suspend fun memoryAsText(sessionId: Long, name: String): String {
        val agent = agentDao.findAgent(sessionId, name) ?: return ""
        val memories = decode(agent.memoriesJson)
        val goals = decode(agent.goalsJson)
        val sb = StringBuilder()
        if (agent.personality.isNotBlank()) sb.appendLine("Personality: ${agent.personality}")
        if (goals.isNotEmpty()) sb.appendLine("Goals: ${goals.joinToString("; ")}")
        if (memories.isNotEmpty()) sb.appendLine("Recent memory:\n- " + memories.joinToString("\n- "))
        return sb.toString()
    }

    private fun decode(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return runCatching { gson.fromJson<List<String>>(json, type) }.getOrNull() ?: emptyList()
    }

    private fun encode(list: List<String>): String = gson.toJson(list)
}
