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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameListScreen(
    onSessionClick: (Long) -> Unit,
    onPickCharacterForGame: () -> Unit,
    viewModel: GameListViewModel = hiltViewModel()
) {
    val sessions by viewModel.recentSessions.collectAsState()
    var pendingDelete by remember { mutableStateOf<SessionSummary?>(null) }

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
                items(sessions, key = { it.session.id }) { summary ->
                    SessionCard(
                        summary = summary,
                        onClick = { onSessionClick(summary.session.id) },
                        onDelete = { pendingDelete = summary }
                    )
                }
            }
        }
    }

    pendingDelete?.let { summary ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除游戏？") },
            text = {
                Column {
                    Text("将永久删除这场冒险，包括全部对话记录和战斗日志。")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "游戏：${summary.session.title}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "角色：${summary.characterName}（不会被删除，可继续用于其他游戏）",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession(summary.session.id)
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
            Button(onClick = onPickCharacter) {
                Text("选择角色开始")
            }
        }
    }
}

@Composable
private fun SessionCard(
    summary: SessionSummary,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = summary.session.title,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            supportingContent = {
                val statusLabel = when (summary.session.status) {
                    "active" -> "进行中"
                    "completed" -> "已完成"
                    "paused" -> "暂停"
                    else -> summary.session.status
                }
                Text(
                    text = "$statusLabel · 角色：${summary.characterName}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingContent = {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingContent = {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除游戏",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}
