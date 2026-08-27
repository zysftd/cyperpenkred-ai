package com.cyperpunkred.ai.ui.settings

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyperpunkred.ai.data.local.datastore.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {
    val apiKey = userPreferences.apiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            userPreferences.saveApiKey(key)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val apiKey by viewModel.apiKey.collectAsState()
    var editingApiKey by remember { mutableStateOf(false) }
    var tempApiKey by remember { mutableStateOf("") }

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
            // API Key section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("OpenAI API Key", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (editingApiKey) {
                        OutlinedTextField(
                            value = tempApiKey,
                            onValueChange = { tempApiKey = it },
                            label = { Text("API Key") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { editingApiKey = false }) {
                                Text("取消")
                            }
                            Button(onClick = {
                                viewModel.saveApiKey(tempApiKey)
                                editingApiKey = false
                            }) {
                                Text("保存")
                            }
                        }
                    } else {
                        Text(
                            text = if (apiKey.isNotBlank()) "已配置" else "未配置",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = {
                            tempApiKey = apiKey
                            editingApiKey = true
                        }) {
                            Text(if (apiKey.isNotBlank()) "更新" else "设置 API Key")
                        }
                    }
                }
            }

            // About section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("关于", style = MaterialTheme.typography.titleMedium)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("赛博朋克红 AI v1.0")
                    Text("基于《赛博朋克红》核心规则")
                    Text("AI GM Powered by GPT-4")
                }
            }
        }
    }
}
