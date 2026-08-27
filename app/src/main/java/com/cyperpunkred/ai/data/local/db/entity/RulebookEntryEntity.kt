package com.cyperpunkred.ai.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rulebook_entries")
data class RulebookEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val section: String,
    val title: String,
    val content: String,
    val keywords: String,
    val tablesJson: String? = null,
    val relatedEntryIds: String? = null,
    val pageNumber: Int? = null
)

@Entity(tableName = "keyword_index")
data class KeywordIndexEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keyword: String,
    val entryId: Long,
    val weight: Int = 1
)
