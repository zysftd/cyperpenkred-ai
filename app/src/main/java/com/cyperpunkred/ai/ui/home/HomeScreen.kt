package com.cyperpunkred.ai.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyperpunkred.ai.data.local.db.entity.CharacterEntity
import com.cyperpunkred.ai.data.local.db.entity.SessionEntity
import com.cyperpunkred.ai.data.repository.CharacterRepository
import com.cyperpunkred.ai.data.repository.GameSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    characterRepository: CharacterRepository,
    sessionRepository: GameSessionRepository
) : ViewModel() {
    val characters = characterRepository.getAllCharacters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSessions = sessionRepository.getRecentSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartGame: (Long) -> Unit,
    onViewCharacter: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val characters by viewModel.characters.collectAsState()
    val recentSessions by viewModel.recentSessions.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "赛博朋克红 AI",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "AI 驱动的桌面角色扮演",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                onClick = {
                    if (characters.isNotEmpty()) {
                        onStartGame(System.currentTimeMillis())
                    }
                }
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("开始冒险", style = MaterialTheme.typography.titleLarge)
                        Text("与 AI GM 一起探索夜之城", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (recentSessions.isNotEmpty()) {
            item {
                Text("最近的游戏", style = MaterialTheme.typography.titleMedium)
            }
            items(recentSessions) { session ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onStartGame(session.id) }
                ) {
                    ListItem(
                        headlineContent = { Text(session.title) },
                        supportingContent = { Text("状态: ${session.status}") },
                        leadingContent = { Icon(Icons.Default.Favorite, null) }
                    )
                }
            }
        }

        if (characters.isNotEmpty()) {
            item {
                Text("我的角色", style = MaterialTheme.typography.titleMedium)
            }
            items(characters) { character ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onViewCharacter(character.id) }
                ) {
                    ListItem(
                        headlineContent = { Text(character.name) },
                        supportingContent = { Text("${character.role} | Lv.${character.age}") },
                        leadingContent = { Icon(Icons.Default.Person, null) }
                    )
                }
            }
        }
    }
}
