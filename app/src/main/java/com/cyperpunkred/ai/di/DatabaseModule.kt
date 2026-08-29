package com.cyperpunkred.ai.di

import android.content.Context
import androidx.room.Room
import com.cyperpunkred.ai.data.local.db.AppDatabase
import com.cyperpunkred.ai.data.local.db.MIGRATION_2_3
import com.cyperpunkred.ai.data.local.db.MIGRATION_3_4
import com.cyperpunkred.ai.data.local.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "cyberpunk_red_ai.db"
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }

    @Provides
    fun provideCharacterDao(database: AppDatabase): CharacterDao = database.characterDao()

    @Provides
    fun provideRulebookDao(database: AppDatabase): RulebookDao = database.rulebookDao()

    @Provides
    fun provideSessionDao(database: AppDatabase): SessionDao = database.sessionDao()

    @Provides
    fun provideChatMessageDao(database: AppDatabase): ChatMessageDao = database.chatMessageDao()

    @Provides
    fun provideCombatLogDao(database: AppDatabase): CombatLogDao = database.combatLogDao()

    @Provides
    fun provideNPCDao(database: AppDatabase): NPCDao = database.npcDao()

    @Provides
    fun provideQuestDao(database: AppDatabase): QuestDao = database.questDao()

    @Provides
    fun provideAgentDao(database: AppDatabase): AgentDao = database.agentDao()
}
