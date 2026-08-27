package com.cyperpunkred.ai.ui.character

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyperpunkred.ai.data.local.db.entity.CharacterEntity
import com.cyperpunkred.ai.data.repository.CharacterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CharacterDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    characterRepository: CharacterRepository
) : ViewModel() {
    private val characterId = savedStateHandle.get<Long>("characterId") ?: 0L

    val character = characterRepository.getCharacterById(characterId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailScreen(
    onBack: () -> Unit,
    viewModel: CharacterDetailViewModel = hiltViewModel()
) {
    val character by viewModel.character.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(character?.name ?: "角色详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        character?.let { char ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("基本信息", style = MaterialTheme.typography.titleMedium)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("名称: ${char.name}")
                        Text("职业: ${char.role}")
                        Text("年龄: ${char.age}")
                        Text("HP: ${char.currentHP}/${char.maxHP}")
                        Text("护甲 SP: ${char.armorSP}")
                        Text("人性值: ${char.humanity}")
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("属性", style = MaterialTheme.typography.titleMedium)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(char.statsJson)
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("技能", style = MaterialTheme.typography.titleMedium)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(char.skillsJson)
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("义体", style = MaterialTheme.typography.titleMedium)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(char.cyberwareJson)
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("装备", style = MaterialTheme.typography.titleMedium)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(char.equipmentJson)
                    }
                }
            }
        }
    }
}
