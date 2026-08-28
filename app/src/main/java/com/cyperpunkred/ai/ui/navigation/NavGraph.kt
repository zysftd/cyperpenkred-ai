package com.cyperpunkred.ai.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cyperpunkred.ai.ui.home.HomeScreen
import com.cyperpunkred.ai.ui.character.CharacterListScreen
import com.cyperpunkred.ai.ui.character.CharacterDetailScreen
import com.cyperpunkred.ai.ui.game.GameSessionScreen
import com.cyperpunkred.ai.ui.game.CombatScreen
import com.cyperpunkred.ai.ui.quest.QuestScreen
import com.cyperpunkred.ai.ui.settings.SettingsScreen
import com.cyperpunkred.ai.ui.rulebook.QuickRefScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object QuickRef : Screen("quick_ref")
    object CharacterList : Screen("character_list")
    object CharacterCreate : Screen("character_create")
    object CharacterDetail : Screen("character_detail/{characterId}")
    object GameList : Screen("game_list")
    object GameSession : Screen("game_session/{sessionId}")
    object Combat : Screen("combat/{sessionId}")
    object QuestList : Screen("quest_list")
    object Settings : Screen("settings")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "首页", Icons.Default.Home),
    BottomNavItem(Screen.QuickRef, "速查", Icons.AutoMirrored.Filled.List),
    BottomNavItem(Screen.CharacterList, "角色", Icons.Default.Person),
    BottomNavItem(Screen.GameList, "游戏", Icons.Default.PlayArrow),
    BottomNavItem(Screen.Settings, "设置", Icons.Default.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberpunkRedNavHost(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { it.screen.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { dest ->
                                dest.route == item.screen.route ||
                                    (item.screen == Screen.GameList &&
                                        (dest.route?.startsWith("game_session/") == true ||
                                            dest.route?.startsWith("combat/") == true))
                            } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onStartGame = { sessionId ->
                        navController.navigate("game_session/$sessionId")
                    },
                    onViewCharacter = { characterId ->
                        navController.navigate("character_detail/$characterId")
                    }
                )
            }
            composable(Screen.QuickRef.route) {
                QuickRefScreen()
            }
            composable(Screen.CharacterList.route) {
                CharacterListScreen(
                    onCharacterClick = { characterId ->
                        navController.navigate("character_detail/$characterId")
                    },
                    onCreateCharacter = {
                        navController.navigate(Screen.CharacterCreate.route)
                    }
                )
            }
            composable(Screen.CharacterCreate.route) {
                com.cyperpunkred.ai.ui.character.CharacterCreateScreen(
                    onCharacterCreated = { characterId ->
                        navController.popBackStack()
                        navController.navigate("character_detail/$characterId")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.CharacterDetail.route,
                arguments = listOf(navArgument("characterId") { type = NavType.LongType })
            ) {
                CharacterDetailScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.GameList.route) {
                com.cyperpunkred.ai.ui.game.GameListScreen(
                    onSessionClick = { sessionId ->
                        navController.navigate("game_session/$sessionId")
                    },
                    onCreateCharacter = {
                        navController.navigate(Screen.CharacterCreate.route)
                    }
                )
            }
            composable(
                route = Screen.GameSession.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
                GameSessionScreen(
                    sessionId = sessionId,
                    onCombat = {
                        navController.navigate("combat/$sessionId")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.Combat.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) {
                CombatScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.QuestList.route) {
                QuestScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
