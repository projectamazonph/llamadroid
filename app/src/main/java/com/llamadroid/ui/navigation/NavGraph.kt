package com.llamadroid.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.llamadroid.core.AppPreferences
import com.llamadroid.ui.screens.benchmark.BenchmarkScreen
import com.llamadroid.ui.screens.chat.ChatScreen
import com.llamadroid.ui.screens.chat.ConversationListScreen
import com.llamadroid.ui.screens.hub.HubScreen
import com.llamadroid.ui.screens.library.ModelLibraryScreen
import com.llamadroid.ui.screens.onboarding.OnboardingScreen
import com.llamadroid.ui.screens.rag.RagScreen
import com.llamadroid.ui.screens.settings.SettingsScreen
import com.llamadroid.ui.screens.settings.SystemPromptScreen
import com.llamadroid.ui.screens.server.ServerScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != Routes.ONBOARDING

    val bottomNavItems = listOf(
        BottomNavItem("Chat", androidx.compose.material.icons.Icons.Filled.Chat, Routes.CONVERSATIONS),
        BottomNavItem("Library", androidx.compose.material.icons.Icons.Filled.Folder, Routes.LIBRARY),
        BottomNavItem("Hub", androidx.compose.material.icons.Icons.Filled.Cloud, Routes.HUB),
        BottomNavItem("Settings", androidx.compose.material.icons.Icons.Filled.Settings, Routes.SETTINGS),
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val currentDestination = navBackStackEntry?.destination
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
            startDestination = Routes.ONBOARDING,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(onComplete = {
                    navController.navigate(Routes.CONVERSATIONS) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                })
            }

            composable(Routes.CONVERSATIONS) {
                ConversationListScreen(
                    onChatSelected = { navController.navigate(Routes.chat(it)) },
                    onNewChat = { navController.navigate(Routes.CHAT_NEW) }
                )
            }

            composable(Routes.CHAT, arguments = listOf(navArgument("conversationId") { type = NavType.StringType })) {
                ChatScreen(conversationId = it.arguments?.getString("conversationId"))
            }
            composable(Routes.CHAT_NEW) { ChatScreen() }
            composable(Routes.LIBRARY) { ModelLibraryScreen() }
            composable(Routes.HUB) { HubScreen() }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onServerClick = { navController.navigate(Routes.SERVER) },
                    onBenchmarkClick = { navController.navigate(Routes.BENCHMARK) },
                    onPromptsClick = { navController.navigate(Routes.SYSTEM_PROMPTS) },
                    onRagClick = { navController.navigate(Routes.RAG) }
                )
            }

            composable(Routes.SERVER) { ServerScreen() }
            composable(Routes.BENCHMARK) { BenchmarkScreen() }
            composable(Routes.SYSTEM_PROMPTS) { SystemPromptScreen() }
            composable(Routes.RAG) { RagScreen() }
        }
    }
}

data class BottomNavItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val route: String)
