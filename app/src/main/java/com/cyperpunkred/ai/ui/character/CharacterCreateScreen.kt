package com.cyperpunkred.ai.ui.character

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyperpunkred.ai.data.local.db.entity.CharacterEntity
import com.cyperpunkred.ai.data.repository.CharacterRepository
import com.cyperpunkred.ai.domain.engine.CharacterEngine
import com.cyperpunkred.ai.domain.model.Role
import com.cyperpunkred.ai.domain.model.Skills
import com.cyperpunkred.ai.domain.model.Stats
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CharacterCreateViewModel @Inject constructor(
    private val characterRepository: CharacterRepository,
    private val characterEngine: CharacterEngine
) : ViewModel() {
    fun createCharacter(
        name: String,
        role: Role,
        stats: Stats,
        skills: Skills,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val hp = characterEngine.calculateMaxHP(role, stats.body)
            val character = CharacterEntity(
                name = name,
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
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var selectedRoleName by rememberSaveable { mutableStateOf<String?>(null) }
    var characterName by rememberSaveable { mutableStateOf("") }
    var intVal by rememberSaveable { mutableIntStateOf(2) }
    var refVal by rememberSaveable { mutableIntStateOf(2) }
    var dexVal by rememberSaveable { mutableIntStateOf(2) }
    var techVal by rememberSaveable { mutableIntStateOf(2) }
    var coolVal by rememberSaveable { mutableIntStateOf(2) }
    var attrVal by rememberSaveable { mutableIntStateOf(2) }
    var luckVal by rememberSaveable { mutableIntStateOf(2) }
    var maVal by rememberSaveable { mutableIntStateOf(2) }
    var bodyVal by rememberSaveable { mutableIntStateOf(2) }
    var empVal by rememberSaveable { mutableIntStateOf(2) }

    val selectedRole = remember(selectedRoleName) {
        selectedRoleName?.let { name -> Role.entries.firstOrNull { it.name == name } }
    }
    val stats = remember(intVal, refVal, dexVal, techVal, coolVal, attrVal, luckVal, maVal, bodyVal, empVal) {
        Stats(
            intelligence = intVal, reflex = refVal, dexterity = dexVal,
            technique = techVal, cool = coolVal, attractiveness = attrVal,
            luck = luckVal, movementAllowance = maVal, body = bodyVal, empathy = empVal
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创建角色") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            LinearProgressIndicator(
                progress = { (currentStep + 1) / 4f },
                modifier = Modifier.fillMaxWidth()
            )
            Text("步骤 ${currentStep + 1}/4")

            when (currentStep) {
                0 -> RoleSelectionStep(
                    selectedRole = selectedRole,
                    onRoleSelected = { selectedRoleName = it.name },
                    onNext = { currentStep = 1 }
                )
                1 -> NameInputStep(
                    name = characterName,
                    onNameChanged = { characterName = it },
                    onNext = { currentStep = 2 },
                    onBack = { currentStep = 0 }
                )
                2 -> StatsAssignmentStep(
                    intVal = intVal, refVal = refVal, dexVal = dexVal,
                    techVal = techVal, coolVal = coolVal, attrVal = attrVal,
                    luckVal = luckVal, maVal = maVal, bodyVal = bodyVal, empVal = empVal,
                    onStatChanged = { abbr, value ->
                        when (abbr) {
                            "INT" -> intVal = value
                            "REF" -> refVal = value
                            "DEX" -> dexVal = value
                            "TECH" -> techVal = value
                            "COOL" -> coolVal = value
                            "ATTR" -> attrVal = value
                            "LUCK" -> luckVal = value
                            "MA" -> maVal = value
                            "BODY" -> bodyVal = value
                            "EMP" -> empVal = value
                        }
                    },
                    onNext = { currentStep = 3 },
                    onBack = { currentStep = 1 }
                )
                3 -> ReviewStep(
                    characterName = characterName,
                    role = selectedRole,
                    stats = stats,
                    onConfirm = {
                        val role = selectedRole ?: return@ReviewStep
                        viewModel.createCharacter(characterName, role, stats, Skills()) { id ->
                            onCharacterCreated(id)
                        }
                    },
                    onBack = { currentStep = 2 }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionStep(
    selectedRole: Role?,
    onRoleSelected: (Role) -> Unit,
    onNext: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("选择职业", style = MaterialTheme.typography.titleLarge)
        Text("选择你的边缘行者职业", style = MaterialTheme.typography.bodyMedium)

        Role.entries.forEach { role ->
            val isSelected = selectedRole == role
            val containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
            val borderColor = if (isSelected)
                MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onRoleSelected(role) },
                color = containerColor,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = borderColor
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(role.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            role.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
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
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
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
    intVal: Int, refVal: Int, dexVal: Int,
    techVal: Int, coolVal: Int, attrVal: Int,
    luckVal: Int, maVal: Int, bodyVal: Int, empVal: Int,
    onStatChanged: (abbr: String, value: Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val statFields = listOf(
        Triple("INT", "智力", intVal),
        Triple("REF", "反应", refVal),
        Triple("DEX", "敏捷", dexVal),
        Triple("TECH", "技术", techVal),
        Triple("COOL", "酷", coolVal),
        Triple("ATTR", "魅力", attrVal),
        Triple("LUCK", "运气", luckVal),
        Triple("MA", "移速", maVal),
        Triple("BODY", "体格", bodyVal),
        Triple("EMP", "共情", empVal)
    )
    val totalPoints = statFields.sumOf { it.third }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("属性分配", style = MaterialTheme.typography.titleLarge)
        Text("总点数: $totalPoints/60")

        statFields.forEach { (abbr, name, value) ->
            val remaining = 60 - (totalPoints - value)
            StatRow(
                abbr = abbr,
                name = name,
                value = value,
                maxAllowed = minOf(10, remaining),
                onStatChanged = onStatChanged
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("上一步") }
            Button(
                onClick = onNext,
                enabled = totalPoints == 60,
                modifier = Modifier.weight(1f)
            ) { Text("下一步") }
        }
    }
}

@Composable
private fun StatRow(
    abbr: String,
    name: String,
    value: Int,
    maxAllowed: Int,
    onStatChanged: (abbr: String, value: Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$abbr ($name)",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            // Fixed-width pill so the value "10" never wraps to two
            // lines even at the maximum.  softWrap=false + maxLines=1
            // are belt-and-suspenders for very dense font scales.
            Text(
                text = "$value / 10",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false,
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { newVal ->
                val intVal = newVal.toInt().coerceIn(1, maxAllowed)
                onStatChanged(abbr, intVal)
            },
            valueRange = 1f..10f,
            steps = 8,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ReviewStep(
    characterName: String,
    role: Role?,
    stats: Stats,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("确认角色", style = MaterialTheme.typography.titleLarge)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("名称: $characterName", style = MaterialTheme.typography.bodyLarge)
                Text("职业: ${role?.displayName ?: "-"}", style = MaterialTheme.typography.bodyLarge)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("属性", style = MaterialTheme.typography.titleSmall)
                Text("INT ${stats.intelligence}  REF ${stats.reflex}  DEX ${stats.dexterity}")
                Text("TECH ${stats.technique}  COOL ${stats.cool}  ATTR ${stats.attractiveness}")
                Text("LUCK ${stats.luck}  MA ${stats.movementAllowance}")
                Text("BODY ${stats.body}  EMP ${stats.empathy}")
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
