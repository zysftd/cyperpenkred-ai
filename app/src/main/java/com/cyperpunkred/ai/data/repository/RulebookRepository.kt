package com.cyperpunkred.ai.data.repository

import com.cyperpunkred.ai.data.local.db.dao.RulebookDao
import com.cyperpunkred.ai.data.local.db.entity.RulebookEntryEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RulebookRepository @Inject constructor(
    private val rulebookDao: RulebookDao
) {
    suspend fun searchByKeywords(keywords: List<String>, limit: Int = 10): List<RulebookEntryEntity> =
        rulebookDao.searchByKeywords(keywords, limit)

    suspend fun getEntriesByCategory(category: String): List<RulebookEntryEntity> =
        rulebookDao.getEntriesByCategory(category)

    suspend fun getEntryById(id: Long): RulebookEntryEntity? =
        rulebookDao.getEntryById(id)

    suspend fun insertAll(entries: List<RulebookEntryEntity>) =
        rulebookDao.insertAll(entries)

    suspend fun getEntryCount(): Int = rulebookDao.getEntryCount()
}
