package com.cyperpunkred.ai.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.cyperpunkred.ai.data.local.db.entity.ChatMessageEntity
import com.cyperpunkred.ai.data.local.db.entity.SessionEntity
import com.cyperpunkred.ai.data.remote.model.ChatMessage
import com.cyperpunkred.ai.data.repository.AIRepository
import com.cyperpunkred.ai.data.repository.GameSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameSessionViewModel @Inject constructor(
    private val sessionRepository: GameSessionRepository,
    private val aiRepository: AIRepository
) : ViewModel() {
    private var sessionId = 0L
    var isLoading by mutableStateOf(false)
        private set

    fun initSession(id: Long) {
        sessionId = id
        viewModelScope.launch {
            val existing = sessionRepository.getSessionById(id).first()
            if (existing == null) {
                sessionRepository.insertSession(
                    SessionEntity(
                        id = id,
                        characterId = 0,
                        title = "新的冒险",
                        status = "active",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
                // Send welcome message
                sessionRepository.addMessage(
                    ChatMessageEntity(
                        sessionId = sessionId,
                        role = "assistant",
                        content = """
霓虹灯在雨水中闪烁，夜之城的天际线被全息广告切割成碎片。你站在 Kabuki 区的一条小巷里，空气中弥漫着合成拉面和臭氧的味道。

**欢迎来到夜之城，choom。**

我是你的游戏主持人。在这里，每个人都在为生存而战——有人用枪，有人用键盘，有人用出卖灵魂。

你想做什么？告诉我你的行动，我会引导你在这座钢铁丛林中前行。

> 💡 提示：你可以描述你的角色在做什么，或者告诉我你想去哪里、找谁、做什么。我会根据规则书判定结果。
                        """.trimIndent(),
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    val messages = sessionRepository.getMessagesForSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sendMessage(content: String) {
        viewModelScope.launch {
            // Add user message
            sessionRepository.addMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = "user",
                    content = content,
                    timestamp = System.currentTimeMillis()
                )
            )

            isLoading = true

            try {
                // Get conversation history
                val history = messages.value.map {
                    ChatMessage(role = it.role, content = it.content)
                }

                // Generate AI response
                val response = aiRepository.generateGMResponse(
                    userMessage = content,
                    conversationHistory = history
                )

                // Add AI response
                sessionRepository.addMessage(
                    ChatMessageEntity(
                        sessionId = sessionId,
                        role = "assistant",
                        content = response,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                sessionRepository.addMessage(
                    ChatMessageEntity(
                        sessionId = sessionId,
                        role = "assistant",
                        content = "⚠️ AI GM 暂时无法回应：${e.message}",
                        timestamp = System.currentTimeMillis()
                    )
                )
            } finally {
                isLoading = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSessionScreen(
    sessionId: Long,
    onCombat: () -> Unit,
    onBack: () -> Unit,
    viewModel: GameSessionViewModel = hiltViewModel()
) {
    LaunchedEffect(sessionId) {
        viewModel.initSession(sessionId)
    }

    val messages by viewModel.messages.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("夜之城冒险") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onCombat) {
                        Icon(Icons.Default.Build, contentDescription = "战斗")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入你的行动...") },
                    enabled = !viewModel.isLoading
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !viewModel.isLoading) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !viewModel.isLoading
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "发送")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                val isUser = message.role == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        modifier = Modifier.widthIn(max = 340.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (!isUser) {
                                Text(
                                    "🤖 AI GM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
