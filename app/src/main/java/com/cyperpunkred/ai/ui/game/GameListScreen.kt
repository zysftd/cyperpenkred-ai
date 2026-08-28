package com.cyperpunkred.ai.ui.game

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
import com.cyperpunkred.ai.data.local.db.entity.SessionEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameListScreen(
    onSessionClick: (Long) -> Unit,
    onPickCharacterForGame: () -> Unit,
    viewModel: GameListViewModel = hiltViewModel()
) {
    val sessions by viewModel.recentSessions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("游戏") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onPickCharacterForGame) {
                Icon(Icons.Default.Add, contentDescription = "新建游戏")
            }
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            EmptyState(
                modifier = Modifier.fillMaxSize().padding(padding),
                onPickCharacter = onPickCharacterForGame
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    SessionCard(
                        session = session,
                        onClick = { onSessionClick(session.id) },
                        onDelete = { viewModel.deleteSession(session.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    onPickCharacter: () -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("还没有游戏记录", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "选一个角色开始新游戏",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            FilledTonalButton(onClick = onPickCharacter) {
                Text("选择角色开始")
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: SessionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        ListItem(
            headlineContent = { Text(session.title) },
            supportingContent = {
                val statusLabel = when (session.status) {
                    "active" -> "进行中"
                    "completed" -> "已完成"
                    "paused" -> "暂停"
                    else -> session.status
                }
                Text("状态: $statusLabel")
            },
            leadingContent = { Icon(Icons.Default.PlayArrow, null) },
            trailingContent = {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
        )
    }
}
