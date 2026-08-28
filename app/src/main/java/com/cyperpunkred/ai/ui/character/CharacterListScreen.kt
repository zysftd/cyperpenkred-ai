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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CharacterListViewModel @Inject constructor(
    private val characterRepository: CharacterRepository
) : ViewModel() {
    val characters = characterRepository.getAllCharacters()
        .map { it.filter { c -> c.id > 0 }.distinctBy { it.id } }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteCharacter(id: Long) {
        viewModelScope.launch {
            try {
                characterRepository.deleteCharacter(id)
            } catch (e: Throwable) {
                android.util.Log.e("CharListVM", "deleteCharacter($id) failed", e)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    onCharacterClick: (Long) -> Unit,
    onCreateCharacter: () -> Unit,
    viewModel: CharacterListViewModel = hiltViewModel()
) {
    val characters by viewModel.characters.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("角色管理") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateCharacter) {
                Icon(Icons.Default.Add, contentDescription = "创建角色")
            }
        }
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
                items(characters, key = { it.id }) { character ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onCharacterClick(character.id) }
                    ) {
                        ListItem(
                            headlineContent = { Text(character.name) },
                            supportingContent = { Text("${character.role} | HP: ${character.currentHP}/${character.maxHP}") },
                            leadingContent = { Icon(Icons.Default.Person, null) },
                            trailingContent = {
                                IconButton(onClick = { viewModel.deleteCharacter(character.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
