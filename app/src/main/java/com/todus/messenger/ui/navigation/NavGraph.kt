package com.todus.messenger.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.todus.messenger.ui.screens.chat.ChatScreen
import com.todus.messenger.ui.screens.chatlist.ChatListScreen
import com.todus.messenger.ui.screens.contacts.ContactSelectorScreen
import com.todus.messenger.ui.screens.login.LoginScreen

// ============================================================================
// Rutas de navegación selladas (sealed class)
// ============================================================================

sealed class Screen(val route: String) {

    data object ChatList : Screen(route = "chatlist")

    data class Chat(val chatId: String) : Screen(route = "chat/{chatId}") {
        fun createRoute(id: String): String = "chat/$id"
        companion object {
            const val ARG_CHAT_ID = "chatId"
        }
    }

    data object Contacts : Screen(route = "contacts")

    data object Login : Screen(route = "login")
}

// ============================================================================
// Grafo de navegación principal
// ============================================================================

@Composable
fun ToDusNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(route = Screen.Login.route) {
            LoginScreen(
                onConnected = {
                    navController.navigate(Screen.ChatList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.ChatList.route) {
            ChatListScreen(
                onNavigateToChat = { chatId ->
                    navController.navigate(Screen.Chat(chatId).createRoute(chatId))
                },
                onNavigateToContacts = {
                    navController.navigate(Screen.Contacts.route)
                }
            )
        }

        composable(
            route = "chat/{chatId}",
            arguments = listOf(
                navArgument(Screen.Chat.ARG_CHAT_ID) {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) {
            ChatScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.Contacts.route) {
            ContactSelectorScreen(
                onContactSelected = { chatId ->
                    navController.navigate(Screen.Chat(chatId).createRoute(chatId)) {
                        popUpTo(Screen.ChatList.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
