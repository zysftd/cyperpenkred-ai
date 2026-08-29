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

    /**
     * Returns the number of game sessions still bound to
     * [characterId] via [onResult].  The UI shows the count in the
     * delete-character confirmation dialog so the user knows what
     * else will be swept away.
     */
    fun getSessionCountFor(characterId: Long, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            onResult(characterRepository.getSessionCountForCharacter(characterId))
        }
    }

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
    var pendingDelete by remember { mutableStateOf<CharacterEntity?>(null) }
    var pendingDeleteSessionCount by remember { mutableStateOf(0) }

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
                    Text(
                        text = "还没有角色",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onCreateCharacter) {
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
                        onDelete = {
                            pendingDelete = character
                            viewModel.getSessionCountFor(character.id) { count ->
                                pendingDeleteSessionCount = count
                            }
                        }
                    )
                }
            }
        }
    }

    pendingDelete?.let { character ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除角色？") },
            text = {
                Column {
                    Text("将永久删除角色卡《${character.name}》。")
                    if (pendingDeleteSessionCount > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "该角色还有 ${pendingDeleteSessionCount} 场游戏，删除角色也会一起删除这些游戏及其全部对话记录和战斗日志。",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "此操作不可撤销。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCharacter(character.id)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun CharacterRow(
    character: CharacterEntity,
    onView: () -> Unit,
    onStart: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onView,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = character.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${character.role} · HP ${character.currentHP}/${character.maxHP} · SP ${character.armorSP}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除角色",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("开始新游戏")
            }
        }
    }
}
