package com.cyperpunkred.ai.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cyperpunkred.ai.data.local.db.dao.*
import com.cyperpunkred.ai.data.local.db.entity.*

@Database(
    entities = [
        CharacterEntity::class,
        RulebookEntryEntity::class,
        KeywordIndexEntity::class,
        SessionEntity::class,
        ChatMessageEntity::class,
        CombatLogEntity::class,
        NPCEntity::class,
        QuestEntity::class,
        AgentEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun rulebookDao(): RulebookDao
    abstract fun sessionDao(): SessionDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun combatLogDao(): CombatLogDao
    abstract fun npcDao(): NPCDao
    abstract fun questDao(): QuestDao
    abstract fun agentDao(): AgentDao
}
