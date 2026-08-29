package com.cyperpunkred.ai.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyperpunkred.ai.data.local.datastore.ProviderType
import com.cyperpunkred.ai.data.local.datastore.ThemeMode
import com.cyperpunkred.ai.data.local.datastore.UserPreferences
import com.cyperpunkred.ai.data.remote.provider.OpenAICompatibleClient
import com.cyperpunkred.ai.data.remote.provider.ProviderConfig
import com.cyperpunkred.ai.data.remote.provider.ProviderStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProviderTestState(
    val message: String? = null,
    val running: Boolean = false,
    val models: List<String> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val providerStore: ProviderStore,
    private val openAICompatibleClient: OpenAICompatibleClient
) : ViewModel() {
    val themeMode = userPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.DYNAMIC)

    val defaultModel = userPreferences.defaultModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val strictJsonMode = userPreferences.strictJsonMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val providers: StateFlow<List<ProviderConfig>> = providerStore.providers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProvider: StateFlow<ProviderConfig?> = providerStore.active
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _testStates = MutableStateFlow<Map<String, ProviderTestState>>(emptyMap())
    val testStates: StateFlow<Map<String, ProviderTestState>> = _testStates.asStateFlow()

    fun saveThemeMode(mode: ThemeMode) {
        viewModelScope.launch { userPreferences.saveThemeMode(mode) }
    }

    fun saveDefaultModel(model: String?) {
        viewModelScope.launch { userPreferences.saveDefaultModel(model) }
    }

    fun saveStrictJsonMode(enabled: Boolean) {
        viewModelScope.launch { userPreferences.saveStrictJsonMode(enabled) }
    }

    fun upsertProvider(config: ProviderConfig) {
        viewModelScope.launch {
            providerStore.upsert(config)
            providerStore.setActive(config.id)
        }
    }

    fun deleteProvider(id: String) {
        viewModelScope.launch { providerStore.delete(id) }
    }

    fun setActiveProvider(id: String) {
        viewModelScope.launch { providerStore.setActive(id) }
    }

    /**
     * Set the default model for a specific provider. If that provider
     * is the currently active one, also persist the model globally so
     * [AIRepository] picks it up immediately on the next chat turn.
     */
    fun setProviderDefaultModel(provider: ProviderConfig, modelId: String) {
        viewModelScope.launch {
            val updated = provider.copy(defaultModel = modelId)
            providerStore.upsert(updated)
            val active = providerStore.activeSnapshot()
            if (active?.id == provider.id) {
                userPreferences.saveDefaultModel(modelId)
            }
        }
    }

    fun testProvider(config: ProviderConfig) {
        viewModelScope.launch {
            updateTest(config.id) { it.copy(running = true, message = null) }
            val outcome = runCatching { openAICompatibleClient.ping(config) }
            updateTest(config.id) { current ->
                current.copy(
                    running = false,
                    message = outcome.getOrElse { "❌ ${it.message ?: "连接失败"}" }
                )
            }
        }
    }

    fun listModels(config: ProviderConfig) {
        viewModelScope.launch {
            updateTest(config.id) { it.copy(running = true, message = null) }
            val outcome = runCatching { openAICompatibleClient.listModels(config) }
            outcome.fold(
                onSuccess = { models ->
                    updateTest(config.id) { it.copy(running = false, message = "已获取 ${models.size} 个模型", models = models) }
                },
                onFailure = { e ->
                    updateTest(config.id) { it.copy(running = false, message = "❌ ${e.message ?: "失败"}") }
                }
            )
        }
    }

    /**
     * Fetch the model list for a not-yet-saved provider (called from
     * the provider editor dialog while it is being filled in). Needs
     * only the values currently in the form, so it can query the
     * endpoint before the provider row exists.
     */
    suspend fun probeModels(baseUrl: String, apiKey: String, timeoutSeconds: Int): List<String> {
        val stub = ProviderConfig(
            id = "__probe__",
            name = "probe",
            type = ProviderType.CUSTOM,
            baseUrl = baseUrl,
            apiKey = apiKey,
            defaultModel = "",
            timeoutSeconds = timeoutSeconds
        )
        return runCatching { openAICompatibleClient.listModels(stub) }.getOrDefault(emptyList())
    }

    suspend fun testConnection(baseUrl: String, apiKey: String, timeoutSeconds: Int): String {
        val stub = ProviderConfig(
            id = "__probe__",
            name = "probe",
            type = ProviderType.CUSTOM,
            baseUrl = baseUrl,
            apiKey = apiKey,
            defaultModel = "",
            timeoutSeconds = timeoutSeconds
        )
        return runCatching { openAICompatibleClient.ping(stub) }
            .getOrElse { "❌ ${it.message ?: "连接失败"}" }
    }

    private fun updateTest(id: String, block: (ProviderTestState) -> ProviderTestState) {
        val current = _testStates.value.toMutableMap()
        current[id] = block(current[id] ?: ProviderTestState())
        _testStates.value = current
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val active by viewModel.activeProvider.collectAsState()
    val defaultModel by viewModel.defaultModel.collectAsState()
    val strictJsonMode by viewModel.strictJsonMode.collectAsState()
    val testStates by viewModel.testStates.collectAsState()
    var showProviderDialog by remember { mutableStateOf(false) }
    var editingProvider by remember { mutableStateOf<ProviderConfig?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("设置") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI Providers section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("AI 提供商", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        FilledTonalButton(onClick = {
                            editingProvider = null
                            showProviderDialog = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("新增")
                        }
                    }
                    if (providers.isEmpty()) {
                        Text(
                            "尚未配置提供商，点击「新增」开始。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    providers.forEach { provider ->
                        val state = testStates[provider.id] ?: ProviderTestState()
                        ProviderRow(
                            provider = provider,
                            isActive = provider.id == active?.id,
                            testState = state,
                            defaultModel = defaultModel,
                            onSelect = { viewModel.setActiveProvider(provider.id) },
                            onEdit = {
                                editingProvider = provider
                                showProviderDialog = true
                            },
                            onTest = { viewModel.testProvider(provider) },
                            onList = { viewModel.listModels(provider) },
                            onDelete = { viewModel.deleteProvider(provider.id) },
                            onSelectModel = { modelId ->
                                viewModel.setProviderDefaultModel(provider, modelId)
                            }
                        )
                    }
                }
            }

            // Default model + Strict JSON mode
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("生成设置", style = MaterialTheme.typography.titleMedium)
                    val dm = defaultModel.orEmpty()
                    val activeModelCandidates = active?.let { testStates[it.id]?.models } ?: emptyList()
                    var modelMenuExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = modelMenuExpanded,
                        onExpandedChange = { modelMenuExpanded = !modelMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = dm,
                            onValueChange = { viewModel.saveDefaultModel(it) },
                            label = { Text("默认模型（覆盖提供商默认）") },
                            placeholder = { Text(active?.defaultModel.orEmpty()) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuExpanded) },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = modelMenuExpanded,
                            onDismissRequest = { modelMenuExpanded = false }
                        ) {
                            if (activeModelCandidates.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("暂无可用模型，请先在提供商卡片里「刷新模型」") },
                                    onClick = { modelMenuExpanded = false }
                                )
                            } else {
                                activeModelCandidates.forEach { modelId ->
                                    DropdownMenuItem(
                                        text = { Text(modelId) },
                                        onClick = {
                                            viewModel.saveDefaultModel(modelId)
                                            modelMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = strictJsonMode,
                                onClick = { viewModel.saveStrictJsonMode(!strictJsonMode) },
                                role = Role.Switch
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(checked = strictJsonMode, onCheckedChange = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("严格 JSON 模式")
                            Text(
                                "返回纯 JSON（与工具调用互斥）。小模型/不支持 tools 时可用。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Theme section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("主题", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    ThemeOption(
                        title = "红色主题",
                        description = "使用赛博朋克红霓虹色系",
                        selected = themeMode == ThemeMode.RED,
                        onClick = { viewModel.saveThemeMode(ThemeMode.RED) }
                    )
                    ThemeOption(
                        title = "跟随壁纸取色",
                        description = "Android 12+ 取自系统壁纸；旧版本回退到红色主题",
                        selected = themeMode == ThemeMode.DYNAMIC,
                        onClick = { viewModel.saveThemeMode(ThemeMode.DYNAMIC) }
                    )
                }
            }

            // About section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("关于", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("赛博朋克红 AI v1.0")
                    Text("基于《赛博朋克红》核心规则")
                }
            }
        }
    }

    if (showProviderDialog) {
        ProviderEditorDialog(
            initial = editingProvider,
            onDismiss = { showProviderDialog = false },
            onSave = { cfg ->
                viewModel.upsertProvider(cfg)
                showProviderDialog = false
            },
            probeModels = viewModel::probeModels,
            testConnection = viewModel::testConnection
        )
    }
}

@Composable
private fun ProviderRow(
    provider: ProviderConfig,
    isActive: Boolean,
    testState: ProviderTestState,
    defaultModel: String?,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onTest: () -> Unit,
    onList: () -> Unit,
    onDelete: () -> Unit,
    onSelectModel: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = isActive, onClick = onSelect)
                Column(modifier = Modifier.weight(1f)) {
                    Text(provider.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${provider.type.name.lowercase()} · ${provider.baseUrl}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
            Text(
                "模型: ${provider.defaultModel} · 超时 ${provider.timeoutSeconds}s" +
                    if (!provider.supportsTools) " · 不支持工具" else "" +
                    if (!provider.supportsStream) " · 不支持流式" else "",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onTest, enabled = !testState.running) {
                    if (testState.running) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp)
                    } else {
                        Text("测试连接")
                    }
                }
                OutlinedButton(onClick = onList, enabled = !testState.running) {
                    Text("刷新模型")
                }
            }
            testState.message?.let { msg ->
                Text(
                    msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (msg.startsWith("❌")) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
            if (testState.models.isNotEmpty()) {
                val currentDefault = defaultModel?.takeIf { it.isNotBlank() } ?: provider.defaultModel
                Text(
                    "可用模型（点击设为默认，当前：${currentDefault}）：",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    testState.models.forEach { modelId ->
                        val selected = modelId == currentDefault
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectModel(modelId) },
                            label = { Text(modelId, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderEditorDialog(
    initial: ProviderConfig?,
    onDismiss: () -> Unit,
    onSave: (ProviderConfig) -> Unit,
    probeModels: suspend (baseUrl: String, apiKey: String, timeoutSeconds: Int) -> List<String>,
    testConnection: suspend (baseUrl: String, apiKey: String, timeoutSeconds: Int) -> String
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: ProviderType.CUSTOM) }
    var baseUrl by remember { mutableStateOf(initial?.baseUrl ?: "") }
    var apiKey by remember { mutableStateOf(initial?.apiKey ?: "") }
    var defaultModel by remember { mutableStateOf(initial?.defaultModel ?: "") }
    var timeout by remember { mutableStateOf(initial?.timeoutSeconds?.toString() ?: "60") }
    var supportsTools by remember { mutableStateOf(initial?.supportsTools ?: true) }
    var supportsStream by remember { mutableStateOf(initial?.supportsStream ?: true) }
    var showKey by remember { mutableStateOf(false) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var modelMenuExpanded by remember { mutableStateOf(false) }
    var fetchedModels by remember { mutableStateOf<List<String>>(emptyList()) }
    var testing by remember { mutableStateOf(false) }
    var testMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val currentTimeout = timeout.toIntOrNull()?.coerceIn(5, 600) ?: 60
    val canProbe = baseUrl.isNotBlank() && apiKey.isNotBlank()

    fun refreshModels() {
        if (!canProbe) return
        scope.launch {
            testing = true
            testMessage = null
            val models = probeModels(baseUrl.trim(), apiKey.trim(), currentTimeout)
            fetchedModels = models
            // Auto-pick the newest model if none is selected yet,
            // and always include the currently-typed value so it
            // isn't dropped.
            if (defaultModel.isBlank() && models.isNotEmpty()) defaultModel = models.first()
            testing = false
            testMessage = if (models.isEmpty()) "❌ 未获取到模型（请检查 Base URL / API Key）"
            else "✅ 获取到 ${models.size} 个模型"
        }
    }

    fun testNow() {
        if (!canProbe) return
        scope.launch {
            testing = true
            testMessage = null
            testMessage = testConnection(baseUrl.trim(), apiKey.trim(), currentTimeout)
            testing = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新增提供商" else "编辑提供商") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = typeMenuExpanded,
                    onExpandedChange = { typeMenuExpanded = !typeMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = type.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("类型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false }
                    ) {
                        ProviderType.values().forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.name) },
                                onClick = {
                                    type = t
                                    typeMenuExpanded = false
                                    if (baseUrl.isBlank()) {
                                        baseUrl = when (t) {
                                            ProviderType.OPENAI -> "https://api.openai.com/v1"
                                            ProviderType.AZURE -> "https://YOUR_RESOURCE.openai.azure.com/openai/deployments/YOUR_DEPLOYMENT"
                                            ProviderType.LOCAL -> "http://10.0.2.2:11434/v1"
                                            ProviderType.CUSTOM -> ""
                                        }
                                    }
                                    if (defaultModel.isBlank() && t == ProviderType.OPENAI) {
                                        defaultModel = "gpt-4o-mini"
                                    }
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL（无需尾斜杠）") },
                    placeholder = { Text("https://api.openai.com/v1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = ::testNow, enabled = canProbe && !testing) {
                        if (testing) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp)
                        } else {
                            Text("测试连接")
                        }
                    }
                    OutlinedButton(onClick = ::refreshModels, enabled = canProbe && !testing) {
                        Text("刷新模型列表")
                    }
                }
                testMessage?.let { msg ->
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (msg.startsWith("❌")) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                }

                // Editable dropdown: pick from fetched models OR type
                // a custom id manually.
                ExposedDropdownMenuBox(
                    expanded = modelMenuExpanded,
                    onExpandedChange = { modelMenuExpanded = !modelMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = defaultModel,
                        onValueChange = { defaultModel = it },
                        readOnly = false,
                        label = { Text("默认模型") },
                        placeholder = { Text("gpt-4o-mini") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = modelMenuExpanded,
                        onDismissRequest = { modelMenuExpanded = false }
                    ) {
                        if (fetchedModels.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("点击「刷新模型列表」获取可选模型") },
                                onClick = { modelMenuExpanded = false }
                            )
                        } else {
                            fetchedModels.forEach { modelId ->
                                DropdownMenuItem(
                                    text = { Text(modelId) },
                                    onClick = {
                                        defaultModel = modelId
                                        modelMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = timeout,
                    onValueChange = { timeout = it.filter { c -> c.isDigit() } },
                    label = { Text("超时（秒）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = supportsTools, onCheckedChange = { supportsTools = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("支持工具调用")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = supportsStream, onCheckedChange = { supportsStream = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("支持流式输出")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cfg = (initial ?: ProviderConfig(
                        id = java.util.UUID.randomUUID().toString(),
                        name = name,
                        type = type,
                        baseUrl = baseUrl,
                        apiKey = apiKey,
                        defaultModel = defaultModel
                    )).copy(
                        name = name,
                        type = type,
                        baseUrl = baseUrl,
                        apiKey = apiKey,
                        defaultModel = defaultModel,
                        timeoutSeconds = timeout.toIntOrNull()?.coerceIn(5, 600) ?: 60,
                        supportsTools = supportsTools,
                        supportsStream = supportsStream
                    )
                    onSave(cfg)
                },
                enabled = name.isNotBlank() && baseUrl.isNotBlank() && apiKey.isNotBlank() && defaultModel.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun ThemeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
