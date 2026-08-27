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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.cyperpunkred.ai.domain.engine.DiceEngine
import com.cyperpunkred.ai.domain.model.Combatant
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CombatViewModel @Inject constructor(
    private val diceEngine: DiceEngine
) : ViewModel() {
    var combatants by mutableStateOf(listOf<Combatant>())
    var currentTurn by mutableIntStateOf(0)
    var round by mutableIntStateOf(1)
    var combatLog by mutableStateOf(listOf<String>())

    fun initializeCombat() {
        // Example combatants for demo
        combatants = listOf(
            Combatant(name = "玩家", isPlayer = true, initiative = diceEngine.rollInitiative(5), currentHP = 40, maxHP = 40, armorSP = 11),
            Combatant(name = "敌人1", isPlayer = false, initiative = diceEngine.rollInitiative(4), currentHP = 25, maxHP = 25, armorSP = 7),
            Combatant(name = "敌人2", isPlayer = false, initiative = diceEngine.rollInitiative(3), currentHP = 30, maxHP = 30, armorSP = 9)
        )
        combatants = combatants.sortedByDescending { it.initiative }
        combatLog = listOf("战斗开始！先攻顺序已确定。")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CombatScreen(
    onBack: () -> Unit,
    viewModel: CombatViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.initializeCombat()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("战斗 - 回合 ${viewModel.round}") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Initiative order
            Text("先攻顺序", style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(viewModel.combatants.withIndex().toList()) { (index, combatant) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (index == viewModel.currentTurn)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        ListItem(
                            headlineContent = { Text(combatant.name) },
                            supportingContent = {
                                Text("HP: ${combatant.currentHP}/${combatant.maxHP} | SP: ${combatant.armorSP}")
                            },
                            leadingContent = {
                                Text(
                                    "先攻: ${combatant.initiative}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                    }
                }
            }

            Divider()

            // Combat log
            Text("战斗日志", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(viewModel.combatLog) { entry ->
                    Text(entry, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { /* TODO: Attack action */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Build, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("攻击")
                }
                OutlinedButton(
                    onClick = { /* TODO: Defend action */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Lock, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("防御")
                }
            }
        }
    }
}
