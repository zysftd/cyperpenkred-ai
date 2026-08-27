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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyperpunkred.ai.domain.engine.CharacterEngine
import com.cyperpunkred.ai.domain.model.*
import com.cyperpunkred.ai.data.local.db.entity.CharacterEntity
import com.cyperpunkred.ai.data.repository.CharacterRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CharacterCreateViewModel @Inject constructor(
    private val characterRepository: CharacterRepository,
    private val characterEngine: CharacterEngine
) : ViewModel() {
    var currentStep by mutableIntStateOf(0)
    var selectedRole by mutableStateOf<Role?>(null)
    var characterName by mutableStateOf("")
    var stats by mutableStateOf(Stats())
    var skills by mutableStateOf(Skills())

    fun createCharacter(onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            val role = selectedRole ?: return@launch
            val hp = characterEngine.calculateMaxHP(role, stats.body)
            val character = CharacterEntity(
                name = characterName,
                role = role.displayName,
                age = 20,
                statsJson = Gson().toJson(stats),
                skillsJson = Gson().toJson(skills),
                abilitiesJson = "[]",
                cyberwareJson = "[]",
                equipmentJson = "[]",
                backstoryJson = "{}",
                lifepathJson = "{}",
                humanity = 10 * stats.empathy,
                currentHP = hp,
                maxHP = hp,
                armorSP = 0,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val id = characterRepository.insertCharacter(character)
            onSuccess(id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterCreateScreen(
    onCharacterCreated: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: CharacterCreateViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创建角色") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
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
            // Step indicator
            LinearProgressIndicator(
                progress = { (viewModel.currentStep + 1) / 4f },
                modifier = Modifier.fillMaxWidth()
            )
            Text("步骤 ${viewModel.currentStep + 1}/4")

            when (viewModel.currentStep) {
                0 -> RoleSelectionStep(
                    selectedRole = viewModel.selectedRole,
                    onRoleSelected = { viewModel.selectedRole = it },
                    onNext = { viewModel.currentStep = 1 }
                )
                1 -> NameInputStep(
                    name = viewModel.characterName,
                    onNameChanged = { viewModel.characterName = it },
                    onNext = { viewModel.currentStep = 2 },
                    onBack = { viewModel.currentStep = 0 }
                )
                2 -> StatsAssignmentStep(
                    stats = viewModel.stats,
                    onStatsChanged = { viewModel.stats = it },
                    onNext = { viewModel.currentStep = 3 },
                    onBack = { viewModel.currentStep = 1 }
                )
                3 -> ReviewStep(
                    viewModel = viewModel,
                    onConfirm = { viewModel.createCharacter(onCharacterCreated) },
                    onBack = { viewModel.currentStep = 2 }
                )
            }
        }
    }
}

@Composable
fun RoleSelectionStep(
    selectedRole: Role?,
    onRoleSelected: (Role) -> Unit,
    onNext: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("选择职业", style = MaterialTheme.typography.titleLarge)
        Text("选择你的边缘行者职业")

        Role.entries.forEach { role ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedRole == role)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ),
                onClick = { onRoleSelected(role) }
            ) {
                ListItem(
                    headlineContent = { Text(role.displayName) },
                    supportingContent = { Text(role.description) },
                    leadingContent = { Icon(Icons.Default.Person, null) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNext,
            enabled = selectedRole != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("下一步")
        }
    }
}

@Composable
fun NameInputStep(
    name: String,
    onNameChanged: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("角色名称", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = name,
            onValueChange = onNameChanged,
            label = { Text("输入角色名称") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("上一步") }
            Button(
                onClick = onNext,
                enabled = name.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) { Text("下一步") }
        }
    }
}

@Composable
fun StatsAssignmentStep(
    stats: Stats,
    onStatsChanged: (Stats) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("属性分配", style = MaterialTheme.typography.titleLarge)
        Text("总点数: ${stats.totalPoints}/60")

        val statFields = listOf(
            "INT" to "智力" to stats.intelligence,
            "REF" to "反应" to stats.reflex,
            "DEX" to "敏捷" to stats.dexterity,
            "TECH" to "技术" to stats.technique,
            "COOL" to "酷" to stats.cool,
            "ATTR" to "魅力" to stats.attractiveness,
            "LUCK" to "运气" to stats.luck,
            "MA" to "移速" to stats.movementAllowance,
            "BODY" to "体格" to stats.body,
            "EMP" to "共情" to stats.empathy
        )

        statFields.forEach { (abbr, name, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$abbr ($name)", modifier = Modifier.weight(1f))
                Row {
                    IconButton(onClick = {
                        if (value > 1) onStatsChanged(stats.withStat(abbr, value - 1))
                    }) { Icon(Icons.Default.Remove, null) }
                    Text("$value", modifier = Modifier.padding(horizontal = 8.dp))
                    IconButton(onClick = {
                        if (value < 10 && stats.totalPoints < 60)
                            onStatsChanged(stats.withStat(abbr, value + 1))
                    }) { Icon(Icons.Default.Add, null) }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("上一步") }
            Button(
                onClick = onNext,
                enabled = stats.totalPoints == 60,
                modifier = Modifier.weight(1f)
            ) { Text("下一步") }
        }
    }
}

@Composable
fun ReviewStep(
    viewModel: CharacterCreateViewModel,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("确认角色", style = MaterialTheme.typography.titleLarge)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("名称: ${viewModel.characterName}")
                Text("职业: ${viewModel.selectedRole?.displayName}")
                Text("属性: ${viewModel.stats}")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("上一步") }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f)
            ) { Text("创建角色") }
        }
    }
}
