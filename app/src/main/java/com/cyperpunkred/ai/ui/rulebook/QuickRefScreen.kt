package com.cyperpunkred.ai.ui.rulebook

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickRefScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("武器", "护甲", "技能", "属性", "战斗")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("速查表") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> WeaponRefTab()
                1 -> ArmorRefTab()
                2 -> SkillRefTab()
                3 -> StatRefTab()
                4 -> CombatRefTab()
            }
        }
    }
}

@Composable
fun WeaponRefTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("武器速查", style = MaterialTheme.typography.titleMedium)

        data class WeaponEntry(val name: String, val damage: String, val rof: String, val ammo: String, val cost: String)
        val weapons = listOf(
            WeaponEntry("重型手枪", "2d6", "2", "12", "100eb"),
            WeaponEntry("超重型手枪", "3d6", "1", "6", "100eb"),
            WeaponEntry("突击步枪", "5d6", "30", "30", "500eb"),
            WeaponEntry("冲锋枪", "3d6", "30", "20", "100eb"),
            WeaponEntry("霰弹枪", "6d6", "1", "2", "100eb"),
            WeaponEntry("长枪", "5d6", "1", "5", "150eb")
        )

        weapons.forEach { weapon ->
            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(weapon.name) },
                    supportingContent = {
                        Text("伤害: ${weapon.damage} | 射速: ${weapon.rof} | 弹药: ${weapon.ammo}")
                    },
                    trailingContent = { Text(weapon.cost) }
                )
            }
        }
    }
}

@Composable
fun ArmorRefTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("护甲速查", style = MaterialTheme.typography.titleMedium)

        data class ArmorEntry(val name: String, val sp: String, val penalty: String, val cost: String)
        val armors = listOf(
            ArmorEntry("轻型装甲夹克", "11", "无", "100eb"),
            ArmorEntry("中型装甲夹克", "12", "REF/DEX -2", "200eb"),
            ArmorEntry("重型装甲夹克", "13", "REF/DEX -2", "500eb"),
            ArmorEntry("防弹背心", "15", "REF/DEX -4", "500eb"),
            ArmorEntry("合金装备", "18", "REF/DEX -4", "5000eb")
        )

        armors.forEach { armor ->
            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(armor.name) },
                    supportingContent = { Text("SP: ${armor.sp} | 惩罚: ${armor.penalty}") },
                    trailingContent = { Text(armor.cost) }
                )
            }
        }
    }
}

@Composable
fun SkillRefTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("技能速查", style = MaterialTheme.typography.titleMedium)

        data class SkillEntry(val name: String, val stat: String, val description: String)
        val skills = listOf(
            SkillEntry("运动", " BODY", "跑跳攀爬"),
            SkillEntry("搏击", " BODY", "近身格斗"),
            SkillEntry("手枪", " REF", "手枪射击"),
            SkillEntry("长枪射击", " REF", "步枪射击"),
            SkillEntry("闪避", " REF", "躲避攻击"),
            SkillEntry("潜行", " DEX", "隐匿行动"),
            SkillEntry("察言观色", " EMP", "读懂他人"),
            SkillEntry("说服", " COOL", "说服他人"),
            SkillEntry("街头智慧", " COOL", "街头生存"),
            SkillEntry("急救", " TECH", "医疗救治")
        )

        skills.forEach { skill ->
            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(skill.name) },
                    supportingContent = { Text(skill.description) },
                    trailingContent = { Text(skill.stat) }
                )
            }
        }
    }
}

@Composable
fun StatRefTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("属性说明", style = MaterialTheme.typography.titleMedium)

        data class StatEntry(val abbr: String, val name: String, val description: String)
        val stats = listOf(
            StatEntry("INT", "智力", "思考、学习、分析"),
            StatEntry("REF", "反应", "反射、速度、射击"),
            StatEntry("DEX", "敏捷", "协调、平衡、灵巧"),
            StatEntry("TECH", "技术", "修理、制造、医术"),
            StatEntry("COOL", "酷", "意志、镇定、魅力"),
            StatEntry("ATTR", "魅力", "外貌、吸引力"),
            StatEntry("LUCK", "运气", "随机好运"),
            StatEntry("MA", "移速", "移动速度"),
            StatEntry("BODY", "体格", "力量、耐力、HP"),
            StatEntry("EMP", "共情", "理解他人、人性值")
        )

        stats.forEach { stat ->
            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("${stat.abbr} - ${stat.name}") },
                    supportingContent = { Text(stat.description) }
                )
            }
        }
    }
}

@Composable
fun CombatRefTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("战斗动作", style = MaterialTheme.typography.titleMedium)

        data class CombatAction(val name: String, val description: String, val cost: String)
        val actions = listOf(
            CombatAction("射击", "远程攻击敌人", "1 动作"),
            CombatAction("近战攻击", "近身攻击敌人", "1 动作"),
            CombatAction("瞄准射击", "瞄准要害，伤害 x2", "2 动作"),
            CombatAction("闪避", "尝试躲避攻击", "1 反应"),
            CombatAction("掩护", "躲在掩体后", "1 动作"),
            CombatAction("装填", "重新装填弹药", "1 动作"),
            CombatAction("逃跑", "尝试脱离战斗", "1 动作")
        )

        actions.forEach { action ->
            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(action.name) },
                    supportingContent = { Text(action.description) },
                    trailingContent = { Text(action.cost) }
                )
            }
        }
    }
}
