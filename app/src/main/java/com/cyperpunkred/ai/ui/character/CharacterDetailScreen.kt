package com.cyperpunkred.ai.ui.character

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyperpunkred.ai.data.local.db.entity.CharacterEntity
import com.cyperpunkred.ai.data.repository.CharacterRepository
import com.cyperpunkred.ai.data.repository.GameSessionRepository
import com.cyperpunkred.ai.data.repository.StartSessionResult
import com.cyperpunkred.ai.domain.model.Skills
import com.cyperpunkred.ai.domain.model.Stats
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CharacterDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    characterRepository: CharacterRepository,
    private val sessionRepository: GameSessionRepository
) : ViewModel() {
    private val characterId = savedStateHandle.get<Long>("characterId") ?: 0L

    val character = characterRepository.getCharacterById(characterId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Start (or resume) a game for this character. Returns the
     * session id via [onResult], or null if the character vanished.
     */
    fun startGame(onResult: (Long?) -> Unit) {
        viewModelScope.launch {
            val r = sessionRepository.startSession(characterId)
            onResult(
                when (r) {
                    is StartSessionResult.Created -> r.sessionId
                    is StartSessionResult.ExistingActive -> r.sessionId
                    StartSessionResult.NoSuchCharacter -> null
                }
            )
        }
    }
}

private val gson = Gson()

private fun parseStats(json: String): Stats? =
    runCatching { gson.fromJson(json, Stats::class.java) }.getOrNull()

private fun parseSkills(json: String): Skills? =
    runCatching { gson.fromJson(json, Skills::class.java) }.getOrNull()

private val statLabels: List<Triple<String, String, (Stats) -> Int>> = listOf(
    Triple("INT", "智力", { it.intelligence }),
    Triple("REF", "反应", { it.reflex }),
    Triple("DEX", "敏捷", { it.dexterity }),
    Triple("TECH", "技术", { it.technique }),
    Triple("COOL", "酷", { it.cool }),
    Triple("ATTR", "魅力", { it.attractiveness }),
    Triple("LUCK", "运气", { it.luck }),
    Triple("MA", "移速", { it.movementAllowance }),
    Triple("BODY", "体格", { it.body }),
    Triple("EMP", "共情", { it.empathy })
)

private val skillGroups: List<Pair<String, List<Pair<String, (Skills) -> Int>>>> = listOf(
    "身体" to listOf(
        "运动" to { it: Skills -> it.athletics },
        "拳击" to { it: Skills -> it.brawling },
        "闪避" to { it: Skills -> it.evasion },
        "潜行" to { it: Skills -> it.stealth },
        "耐力" to { it: Skills -> it.survival },
        "动物驯养" to { it: Skills -> it.animalHandling },
        "追踪" to { it: Skills -> it.tracking },
        "骑乘" to { it: Skills -> it.riding },
        "驾驶" to { it: Skills -> it.driving },
        "地面载具" to { it: Skills -> it.pilotGround },
        "空中载具" to { it: Skills -> it.pilotAir }
    ),
    "感知" to listOf(
        "警觉" to { it: Skills -> it.perception },
        "专注" to { it: Skills -> it.concentration },
        "教育" to { it: Skills -> it.education },
        "本地专家" to { it: Skills -> it.localExpert },
        "人体感知" to { it: Skills -> it.humanPerception }
    ),
    "社交" to listOf(
        "说服" to { it: Skills -> it.persuasion },
        "交谈" to { it: Skills -> it.conversation },
        "街头信誉" to { it: Skills -> it.streetwise },
        "街头语言" to { it: Skills -> it.languageStreet },
        "语言" to { it: Skills -> it.language }
    ),
    "战斗" to listOf(
        "手枪" to { it: Skills -> it.pistol },
        "步枪" to { it: Skills -> it.rifle },
        "霰弹枪" to { it: Skills -> it.shotgun },
        "近战武器" to { it: Skills -> it.meleeWeapon },
        "武术" to { it: Skills -> it.martialArt },
        "重型武器" to { it: Skills -> it.heavyWeapon }
    ),
    "医疗" to listOf(
        "急救" to { it: Skills -> it.firstAid },
        "药剂师" to { it: Skills -> it.paramedic },
        "验尸" to { it: Skills -> it.autopsy }
    ),
    "技术" to listOf(
        "电子学" to { it: Skills -> it.electronics },
        "基础技术" to { it: Skills -> it.basicTech },
        "赛博技术" to { it: Skills -> it.cybertech },
        "网络空间" to { it: Skills -> it.cybersecurity },
        "破解" to { it: Skills -> it.decryption },
        "开锁" to { it: Skills -> it.lockpicking },
        "撬锁" to { it: Skills -> it.pickLock },
        "扒窃" to { it: Skills -> it.pickPocket },
        "技术武器" to { it: Skills -> it.techWeapon },
        "护甲技术" to { it: Skills -> it.armorTech },
        "爆破" to { it: Skills -> it.demolition }
    ),
    "表演" to listOf(
        "演奏" to { it: Skills -> it.playInstrument },
        "作曲" to { it: Skills -> it.composition },
        "摄影" to { it: Skills -> it.photography }
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailScreen(
    onBack: () -> Unit,
    onSessionStarted: (Long) -> Unit,
    viewModel: CharacterDetailViewModel = hiltViewModel()
) {
    val character by viewModel.character.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(character?.name ?: "角色详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    character?.let {
                        TextButton(
                            onClick = {
                                viewModel.startGame { newId ->
                                    if (newId != null) onSessionStarted(newId)
                                }
                            }
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("开始新游戏")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                BasicInfoCard(char)
                StatsCard(char)
                SkillsCard(char)
                Button(
                    onClick = {
                        viewModel.startGame { newId ->
                            if (newId != null) onSessionStarted(newId)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("开始新游戏（若已有进行中冒险，将直接续玩）")
                }
            }
        }
    }
}

@Composable
private fun BasicInfoCard(char: CharacterEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("基本信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            InfoRow("名称", char.name)
            InfoRow("职业", char.role)
            InfoRow("年龄", char.age.toString())
            InfoRow("HP", "${char.currentHP}/${char.maxHP}")
            InfoRow("护甲 SP", char.armorSP.toString())
            InfoRow("人性值", char.humanity.toString())
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatsCard(char: CharacterEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("属性", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            val stats = parseStats(char.statsJson)
            if (stats == null) {
                Text(char.statsJson, color = MaterialTheme.colorScheme.error)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    statLabels.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { (abbr, label, getter) ->
                                StatCell(
                                    modifier = Modifier.weight(1f),
                                    abbr = abbr,
                                    label = label,
                                    value = getter(stats)
                                )
                            }
                            if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCell(modifier: Modifier, abbr: String, label: String, value: Int) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    abbr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                value.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (value >= 8) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SkillsCard(char: CharacterEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("技能", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            val skills = parseSkills(char.skillsJson)
            if (skills == null) {
                Text(char.skillsJson, color = MaterialTheme.colorScheme.error)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    skillGroups.forEach { (category, entries) ->
                        Text(
                            category,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            entries.chunked(2).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { (name, getter) ->
                                        SkillCell(
                                            modifier = Modifier.weight(1f),
                                            name = name,
                                            value = getter(skills)
                                        )
                                    }
                                    if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
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
private fun SkillCell(modifier: Modifier, name: String, value: Int) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (value > 0) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    value.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        } else {
            Text(
                "0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
