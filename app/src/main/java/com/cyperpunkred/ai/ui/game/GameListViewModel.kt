package com.cyperpunkred.ai.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyperpunkred.ai.data.local.db.entity.SessionEntity
import com.cyperpunkred.ai.data.repository.CharacterRepository
import com.cyperpunkred.ai.data.repository.GameSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameListViewModel @Inject constructor(
    private val sessionRepository: GameSessionRepository,
    private val characterRepository: CharacterRepository
) : ViewModel() {

    val recentSessions: StateFlow<List<SessionEntity>> = sessionRepository.getRecentSessions()
        .map { it.filter { s -> s.id > 0 }.distinctBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createNewSession(onResult: (Long?) -> Unit) {
        viewModelScope.launch {
            val characters = characterRepository.getAllCharacters().first()
            if (characters.isEmpty()) {
                onResult(null)
                return@launch
            }
            val now = System.currentTimeMillis()
            val character = characters.first()
            val session = SessionEntity(
                characterId = character.id,
                title = "${character.name} 的冒险",
                status = "active",
                createdAt = now,
                updatedAt = now
            )
            val id = sessionRepository.insertSession(session)
            onResult(id)
        }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            sessionRepository.deleteSession(id)
        }
    }
}
