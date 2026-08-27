package com.cyperpunkred.ai.data.local.db.dao

import androidx.room.*
import com.cyperpunkred.ai.data.local.db.entity.RulebookEntryEntity
import com.cyperpunkred.ai.data.local.db.entity.KeywordIndexEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RulebookDao {
    @Query("SELECT * FROM rulebook_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): RulebookEntryEntity?

    @Query("SELECT * FROM rulebook_entries WHERE category = :category")
    suspend fun getEntriesByCategory(category: String): List<RulebookEntryEntity>

    @Query("""
        SELECT re.* FROM rulebook_entries re
        INNER JOIN keyword_index ki ON re.id = ki.entryId
        WHERE ki.keyword IN (:keywords)
        ORDER BY ki.weight DESC
        LIMIT :limit
    """)
    suspend fun searchByKeywords(keywords: List<String>, limit: Int = 10): List<RulebookEntryEntity>

    @Query("SELECT * FROM keyword_index WHERE keyword = :keyword")
    suspend fun getKeywordEntries(keyword: String): List<KeywordIndexEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: RulebookEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeywordIndex(keywordIndex: KeywordIndexEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<RulebookEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllKeywords(keywords: List<KeywordIndexEntity>)

    @Query("SELECT COUNT(*) FROM rulebook_entries")
    suspend fun getEntryCount(): Int
}
