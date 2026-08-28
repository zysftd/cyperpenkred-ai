package com.cyperpunkred.ai.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyperpunkred.ai.data.local.db.entity.ChatMessageEntity
import com.cyperpunkred.ai.data.local.db.entity.SessionEntity
import com.cyperpunkred.ai.data.remote.model.ChatMessage
import com.cyperpunkred.ai.data.repository.AIRepository
import com.cyperpunkred.ai.data.repository.AgentEvent
import com.cyperpunkred.ai.data.repository.GameSessionRepository
import com.cyperpunkred.ai.ui.common.MarkdownText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

/**
 * One tool invocation that the streaming loop emitted. We keep a
 * short history so the UI bubble can show e.g.
 *   "lookup_rule" -> "命中《夜之城》：……"
 * above the assistant prose as it streams in.
 */
data class ToolCallUi(
    val name: String,
    val result: String? = null,
    val isError: Boolean = false,
    val finished: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GameSessionViewModel @Inject constructor(
    private val sessionRepository: GameSessionRepository,
    private val aiRepository: AIRepository
) : ViewModel() {
    private val _sessionId = MutableStateFlow(0L)
    val sessionId: StateFlow<Long> = _sessionId.asStateFlow()
    var isLoading by mutableStateOf(false)
        private set

    /**
     * Live streaming state. When non-null the bottom of the chat shows
     * a "streaming" bubble fed by [streamingText] / [streamingTools]
     * rather than waiting for the full turn to land in the DB.
     */
    private val _streamingText = MutableStateFlow<String?>(null)
    val streamingText: StateFlow<String?> = _streamingText.asStateFlow()

    private val _streamingTools = MutableStateFlow<List<ToolCallUi>>(emptyList())
    val streamingTools: StateFlow<List<ToolCallUi>> = _streamingTools.asStateFlow()

    fun initSession(id: Long) {
        if (_sessionId.value == id && _sessionId.value > 0L) return
        _sessionId.value = id
        viewModelScope.launch {
            val existing = sessionRepository.getSessionById(id).first()
            if (existing == null) {
                val newId = sessionRepository.insertSession(
                    SessionEntity(
                        characterId = 0L,
                        title = "新的冒险",
                        status = "active",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
                _sessionId.value = newId
                sessionRepository.addMessage(
                    ChatMessageEntity(
                        sessionId = newId,
                        role = "assistant",
                        content = """
霓虹灯在雨水中闪烁，夜之城的天际线被全息广告切割成碎片。你站在 Kabuki 区的一条小巷里，空气中弥漫着合成拉面和臭氧的味道。

**欢迎来到夜之城，choom。**

我是你的游戏主持人。在这里，每个人都在为生存而战——有人用枪，有人用键盘，有人用出卖灵魂。

你想做什么？告诉我你的行动，我会引导你在这座钢铁丛林中前行。

> 提示：你可以描述你的角色在做什么，或者告诉我你想去哪里、找谁、做什么。我会根据规则书判定结果。
                        """.trimIndent(),
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    val messages: StateFlow<List<ChatMessageEntity>> = _sessionId
        .flatMapLatest { id ->
            if (id <= 0L) flowOf(emptyList())
            else sessionRepository.getMessagesForSession(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sendMessage(content: String) {
        val id = _sessionId.value
        if (id <= 0L) return
        viewModelScope.launch {
            sessionRepository.addMessage(
                ChatMessageEntity(
                    sessionId = id,
                    role = "user",
                    content = content,
                    timestamp = System.currentTimeMillis()
                )
            )

            isLoading = true
            _streamingText.value = ""
            _streamingTools.value = emptyList()

            val history = messages.value.map {
                ChatMessage(role = it.role, content = it.content)
            }

            val pendingBuilder = StringBuilder()
            val pendingTools = mutableListOf<ToolCallUi>()

            try {
                aiRepository.streamAgentTurn(
                    sessionId = id,
                    userMessage = content,
                    conversationHistory = history
                ).collect { event ->
                    when (event) {
                        is AgentEvent.Text -> {
                            pendingBuilder.append(event.delta)
                            _streamingText.value = (_streamingText.value ?: "") + event.delta
                        }
                        is AgentEvent.ToolCallStarted -> {
                            val ui = ToolCallUi(name = event.name)
                            pendingTools.add(ui)
                            _streamingTools.value = pendingTools.toList()
                        }
                        is AgentEvent.ToolCallFinished -> {
                            val idx = pendingTools.indexOfLast { it.name == event.name && !it.finished }
                            if (idx >= 0) {
                                pendingTools[idx] = pendingTools[idx].copy(
                                    result = event.content,
                                    isError = event.isError,
                                    finished = true
                                )
                                _streamingTools.value = pendingTools.toList()
                            }
                        }
                        is AgentEvent.TurnFinished -> Unit
                        is AgentEvent.Failed -> {
                            pendingBuilder.append("\n\n⚠️ ${event.message}")
                            _streamingText.value = (_streamingText.value ?: "") + "\n\n⚠️ ${event.message}"
                        }
                    }
                }
            } catch (e: Exception) {
                val msg = "\n\n⚠️ AI GM 暂时无法回应：${e.message}"
                pendingBuilder.append(msg)
                _streamingText.value = (_streamingText.value ?: "") + msg
            }

            val finalContent = pendingBuilder.toString().trim()
            if (finalContent.isNotEmpty()) {
                sessionRepository.addMessage(
                    ChatMessageEntity(
                        sessionId = id,
                        role = "assistant",
                        content = finalContent,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
            _streamingText.value = null
            _streamingTools.value = emptyList()
            isLoading = false
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
    val streamingText by viewModel.streamingText.collectAsState()
    val streamingTools by viewModel.streamingTools.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to bottom whenever the message list or the streaming
    // bubble changes length.
    LaunchedEffect(messages.size, streamingText, streamingTools.size) {
        val lastIndex = if (streamingText != null) messages.size else messages.size - 1
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("夜之城冒险") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送")
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
            items(messages, key = { it.id }) { message ->
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
                            MarkdownText(
                                text = message.content,
                                modifier = Modifier
                            )
                        }
                    }
                }
            }

            // Streaming bubble (only while a turn is in flight)
            if (streamingText != null) {
                item(key = "streaming-bubble") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            modifier = Modifier.widthIn(max = 340.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "🤖 AI GM",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(10.dp),
                                        strokeWidth = 1.5.dp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                if (streamingTools.isNotEmpty()) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) {
                                        streamingTools.forEach { tc ->
                                            ToolBadge(tc)
                                        }
                                    }
                                }
                                if (streamingText!!.isNotEmpty()) {
                                    MarkdownText(text = streamingText!!)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolBadge(tc: ToolCallUi) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val (label, color) = when {
            !tc.finished -> "🔧 ${tc.name} …" to MaterialTheme.colorScheme.tertiary
            tc.isError -> "❌ ${tc.name}" to MaterialTheme.colorScheme.error
            else -> "✅ ${tc.name}" to MaterialTheme.colorScheme.primary
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
