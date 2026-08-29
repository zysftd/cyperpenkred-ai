package com.cyperpunkred.ai.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyperpunkred.ai.data.local.db.entity.SessionEntity
import com.cyperpunkred.ai.data.repository.CharacterRepository
import com.cyperpunkred.ai.data.repository.GameSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A game session paired with the character sheet that drives it. */
data class SessionSummary(
    val session: SessionEntity,
    val characterName: String
)

@HiltViewModel
class GameListViewModel @Inject constructor(
    private val sessionRepository: GameSessionRepository,
    characterRepository: CharacterRepository
) : ViewModel() {

    val recentSessions: StateFlow<List<SessionSummary>> = combine(
        sessionRepository.getRecentSessions(),
        characterRepository.getAllCharacters()
    ) { sessions, characters ->
        val byId = characters.associateBy { it.id }
        sessions.map { s ->
            SessionSummary(
                session = s,
                characterName = byId[s.characterId]?.name ?: "未知角色"
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            sessionRepository.deleteSession(id)
        }
    }
}
