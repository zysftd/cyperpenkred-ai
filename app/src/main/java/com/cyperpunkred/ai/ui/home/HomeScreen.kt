package com.cyperpunkred.ai.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyperpunkred.ai.data.local.db.entity.SessionEntity
import com.cyperpunkred.ai.data.repository.CharacterRepository
import com.cyperpunkred.ai.data.repository.GameSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val characterRepository: CharacterRepository,
    private val sessionRepository: GameSessionRepository
) : ViewModel() {

    val characters: StateFlow<List<com.cyperpunkred.ai.data.local.db.entity.CharacterEntity>> =
        characterRepository.getAllCharacters()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSessions: StateFlow<List<SessionEntity>> = sessionRepository.getRecentSessions()
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartGame: (Long) -> Unit,
    onViewCharacter: (Long) -> Unit,
    onCreateCharacter: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val characters by viewModel.characters.collectAsState()
    val recentSessions by viewModel.recentSessions.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var missingCharacterMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(missingCharacterMsg) {
        val msg = missingCharacterMsg ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = msg,
            actionLabel = "去创建",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            onCreateCharacter()
        }
        missingCharacterMsg = null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
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
                        viewModel.createNewSession { newId ->
                            if (newId != null) {
                                onStartGame(newId)
                            } else {
                                missingCharacterMsg = "请先创建角色再开始冒险"
                            }
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
                            Text(
                                if (characters.isEmpty()) "需要先创建角色"
                                else "与 AI GM 一起探索夜之城",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (recentSessions.isNotEmpty()) {
                item {
                    Text("最近的游戏", style = MaterialTheme.typography.titleMedium)
                }
                items(recentSessions, key = { it.id }) { session ->
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
                items(characters, key = { it.id }) { character ->
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
            } else {
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onCreateCharacter
                    ) {
                        ListItem(
                            headlineContent = { Text("还没有角色") },
                            supportingContent = { Text("点击创建一个角色开始冒险") },
                            leadingContent = { Icon(Icons.Default.Add, null) },
                            trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowRight, null) }
                        )
                    }
                }
            }
        }
    }
}
