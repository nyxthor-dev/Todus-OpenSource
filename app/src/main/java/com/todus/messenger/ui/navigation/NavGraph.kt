package com.todus.messenger.ui.navigation

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

/**
 * Rutas de navegación de la aplicación ToDus Messenger.
 *
 * Utiliza una clase sellada para garantizar que todas las pantallas
 * estén definidas en un solo lugar y sean seguras en tiempo de compilación.
 */
sealed class Screen(val route: String) {

    /** Pantalla principal: lista de chats (conversaciones). */
    data object ChatList : Screen(route = "chatlist")

    /**
     * Pantalla de chat individual con un contacto o grupo.
     * @param chatId Identificador único del chat (JID del contacto o groupId).
     */
    data class Chat(val chatId: String) : Screen(route = "chat/{chatId}") {
        fun createRoute(id: String): String = "chat/$id"
        companion object {
            const val ARG_CHAT_ID = "chatId"
        }
    }

    /** Pantalla de selección de contactos para iniciar nuevo chat. */
    data object Contacts : Screen(route = "contacts")

    /** Pantalla de login/conexión. */
    data object Login : Screen(route = "login")
}

// ============================================================================
// Grafo de navegación principal
// ============================================================================

/**
 * Grafo de navegación principal de ToDus Messenger.
 *
 * Define las tres pantallas principales y las transiciones:
 * - ChatList → Chat (al hacer click en un chat)
 * - ChatList → Contacts (al presionar el FAB +)
 * - Contacts → Chat (al seleccionar un contacto)
 * - Chat → ChatList (al presionar atrás)
 * - Contacts → ChatList (al presionar atrás)
 *
 * @param navController Controlador de navegación de Compose.
 */
@Composable
fun ToDusNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        // ----------------------------------------------------------------
        // Pantalla: Login
        // ----------------------------------------------------------------
        composable(route = Screen.Login.route) {
            LoginScreen(
                onConnected = {
                    navController.navigate(Screen.ChatList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // ----------------------------------------------------------------
        // Pantalla: Lista de chats
        // ----------------------------------------------------------------
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

        // ----------------------------------------------------------------
        // Pantalla: Chat individual
        // ----------------------------------------------------------------
        composable(
            route = Screen.Chat("{${Screen.Chat.ARG_CHAT_ID}}").route,
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

        // ----------------------------------------------------------------
        // Pantalla: Selección de contactos
        // ----------------------------------------------------------------
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
