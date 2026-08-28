package com.cyperpunkred.ai.data.repository

import com.cyperpunkred.ai.data.local.db.dao.CharacterDao
import com.cyperpunkred.ai.data.local.db.entity.CharacterEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterRepository @Inject constructor(
    private val characterDao: CharacterDao
) {
    fun getAllCharacters(): Flow<List<CharacterEntity>> = characterDao.getAllCharacters()

    fun getCharacterById(id: Long): Flow<CharacterEntity?> = characterDao.getCharacterById(id)

    suspend fun getCharacterByIdOnce(id: Long): CharacterEntity? = characterDao.getCharacterByIdOnce(id)

    suspend fun insertCharacter(character: CharacterEntity): Long = characterDao.insertCharacter(character)

    suspend fun updateCharacter(character: CharacterEntity) = characterDao.updateCharacter(character)

    suspend fun deleteCharacter(id: Long) = characterDao.deleteCharacter(id)
}
