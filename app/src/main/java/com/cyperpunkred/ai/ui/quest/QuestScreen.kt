package com.cyperpunkred.ai.ui.quest

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
import com.cyperpunkred.ai.data.local.db.entity.QuestEntity
import com.cyperpunkred.ai.data.repository.QuestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuestViewModel @Inject constructor(
    private val questRepository: QuestRepository
) : ViewModel() {
    val quests = questRepository.getAllQuests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestScreen(
    viewModel: QuestViewModel = hiltViewModel()
) {
    val quests by viewModel.quests.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("任务追踪") })
        }
    ) { padding ->
        if (quests.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default Assignment,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("暂无任务", style = MaterialTheme.typography.titleMedium)
                    Text("AI GM 会在游戏中分配任务", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quests) { quest ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text(quest.title) },
                            supportingContent = { Text(quest.description) },
                            leadingContent = {
                                Icon(
                                    if (quest.status == "completed") Icons.Default.CheckCircle
                                    else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null
                                )
                            },
                            trailingContent = { Text(quest.status) }
                        )
                    }
                }
            }
        }
    }
}
