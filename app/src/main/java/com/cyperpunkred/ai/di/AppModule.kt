package com.cyperpunkred.ai.di

import com.cyperpunkred.ai.data.local.datastore.UserPreferences
import com.cyperpunkred.ai.data.remote.api.OpenAIApi
import com.cyperpunkred.ai.data.repository.*
import com.cyperpunkred.ai.domain.engine.CombatEngine
import com.cyperpunkred.ai.domain.engine.CharacterEngine
import com.cyperpunkred.ai.domain.engine.DiceEngine
import com.cyperpunkred.ai.domain.engine.NetrunningEngine
import com.cyperpunkred.ai.domain.knowledge.RulebookQueryEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDiceEngine(): DiceEngine = DiceEngine

    @Provides
    @Singleton
    fun provideCharacterEngine(): CharacterEngine = CharacterEngine()

    @Provides
    @Singleton
    fun provideCombatEngine(diceEngine: DiceEngine): CombatEngine = CombatEngine(diceEngine)

    @Provides
    @Singleton
    fun provideNetrunningEngine(diceEngine: DiceEngine): NetrunningEngine = NetrunningEngine(diceEngine)

    @Provides
    @Singleton
    fun provideRulebookQueryEngine(rulebookRepository: RulebookRepository): RulebookQueryEngine =
        RulebookQueryEngine(rulebookRepository)

    @Provides
    @Singleton
    fun provideAIRepository(
        openAIApi: OpenAIApi,
        userPreferences: UserPreferences,
        rulebookQueryEngine: RulebookQueryEngine
    ): AIRepository = AIRepository(openAIApi, userPreferences, rulebookQueryEngine)
}
