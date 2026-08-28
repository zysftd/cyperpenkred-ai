package com.cyperpunkred.ai.ui.character

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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyperpunkred.ai.data.local.db.entity.CharacterEntity
import com.cyperpunkred.ai.data.repository.CharacterRepository
import com.cyperpunkred.ai.data.repository.GameSessionRepository
import com.cyperpunkred.ai.data.repository.StartSessionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CharacterListViewModel @Inject constructor(
    private val characterRepository: CharacterRepository,
    private val sessionRepository: GameSessionRepository
) : ViewModel() {
    val characters = characterRepository.getAllCharacters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteCharacter(id: Long) {
        viewModelScope.launch {
            characterRepository.deleteCharacter(id)
        }
    }

    /**
     * Start (or resume) a game for [characterId]. Caller passes
     * [onResult] to be told the new or resumed session id, or
     * null if the character no longer exists.
     */
    fun startGameFor(characterId: Long, onResult: (Long?) -> Unit) {
        viewModelScope.launch {
            when (val r = sessionRepository.startSession(characterId)) {
                is StartSessionResult.Created -> onResult(r.sessionId)
                is StartSessionResult.ExistingActive -> onResult(r.sessionId)
                StartSessionResult.NoSuchCharacter -> onResult(null)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    onCharacterClick: (Long) -> Unit,
    onCreateCharacter: () -> Unit,
    onSessionStarted: (Long) -> Unit,
    viewModel: CharacterListViewModel = hiltViewModel()
) {
    val characters by viewModel.characters.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(errorMessage) {
        val msg = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        errorMessage = null
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("角色管理") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateCharacter) {
                Icon(Icons.Default.Add, contentDescription = "创建角色")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (characters.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("还没有角色", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(onClick = onCreateCharacter) {
                        Text("创建第一个角色")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(characters, key = { "character-${it.id}" }) { character ->
                    CharacterRow(
                        character = character,
                        onView = { onCharacterClick(character.id) },
                        onStart = {
                            viewModel.startGameFor(character.id) { newId ->
                                if (newId != null) onSessionStarted(newId)
                                else errorMessage = "无法开始游戏：角色不存在"
                            }
                        },
                        onDelete = { viewModel.deleteCharacter(character.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CharacterRow(
    character: CharacterEntity,
    onView: () -> Unit,
    onStart: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onView) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ListItem(
                    headlineContent = { Text(character.name) },
                    supportingContent = {
                        Text(
                            "${character.role} | HP: ${character.currentHP}/${character.maxHP} | SP ${character.armorSP}"
                        )
                    },
                    leadingContent = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除角色")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(onClick = onStart) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("开始新游戏")
                }
            }
        }
    }
}
