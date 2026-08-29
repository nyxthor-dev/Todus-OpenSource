package com.todus.messenger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todus.messenger.data.remote.xmpp.InMessage
import com.todus.messenger.data.remote.xmpp.ToDusXmppClient
import com.todus.messenger.domain.model.Chat
import com.todus.messenger.domain.model.ConnectionState
import com.todus.messenger.domain.repository.ChatRepository
import com.todus.messenger.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de lista de chats (conversaciones activas).
 *
 * Expone de forma reactiva la lista de chats, el estado de conexión XMPP
 * y gestiona la búsqueda, eliminación de chats y el procesamiento automático
 * de mensajes entrantes.
 *
 * Anotado con [@HiltViewModel] para que Hilt pueda inyectar las dependencias
 * ([ChatRepository], [MessageRepository], [ToDusXmppClient]) automáticamente.
 */
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
    private val xmppClient: ToDusXmppClient
) : ViewModel() {

    /**
     * Estado de la interfaz de usuario para la pantalla de lista de chats.
     *
     * @property chats Lista de conversaciones activas del usuario.
     * @property isLoading Indica si los chats se están cargando por primera vez.
     * @property connectionState Estado actual de la conexión con el servidor XMPP de ToDus.
     * @property searchQuery Texto de búsqueda actual para filtrar chats por nombre.
     */
    data class UiState(
        val chats: List<Chat> = emptyList(),
        val isLoading: Boolean = true,
        val connectionState: ConnectionState = ConnectionState.Disconnected,
        val searchQuery: String = ""
    )

    // Flujo mutable interno que se actualiza desde las corrutinas
    private val _uiState = MutableStateFlow(UiState())

    /**
     * Estado de la UI expuesto de forma inmutable a la vista (Jetpack Compose).
     * La vista solo puede observar, no modificar.
     */
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Formateador de hora reutilizable para el último mensaje de cada chat
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    init {
        // Observar la lista de chats de forma reactiva desde el repositorio.
        // Cada cambio en la base de datos (nuevo chat, mensaje recibido, etc.)
        // actualiza automáticamente la lista mostrada.
        viewModelScope.launch {
            chatRepository.getAllChats().collect { chats ->
                _uiState.update { currentState ->
                    // Aplicar filtro de búsqueda si hay una consulta activa
                    val filteredChats = if (currentState.searchQuery.isBlank()) {
                        chats
                    } else {
                        chats.filter { chat ->
                            chat.name.contains(
                                currentState.searchQuery,
                                ignoreCase = true
                            )
                        }
                    }
                    currentState.copy(
                        chats = filteredChats,
                        isLoading = false
                    )
                }
            }
        }

        // Observar el estado de la conexión XMPP para mostrarlo en la UI.
        // Permite al usuario saber si está conectado, desconectado, etc.
        viewModelScope.launch {
            xmppClient.connectionState.collect { state ->
                _uiState.update { currentState ->
                    currentState.copy(connectionState = state)
                }
            }
        }

        // Escuchar mensajes entrantes de XMPP y procesarlos automáticamente.
        // Cada mensaje recibido se convierte a modelo de dominio, se almacena
        // localmente y se actualiza el chat correspondiente.
        viewModelScope.launch {
            messageRepository.observeIncomingMessages().collect { inMessage ->
                messageRepository.processIncomingMessage(inMessage)
            }
        }
    }

    /**
     * Actualiza el texto de búsqueda y filtra la lista de chats.
     *
     * La búsqueda se realiza de forma local comparando el nombre del chat
     * con el texto proporcionado (insensible a mayúsculas/minúsculas).
     *
     * @param query Nuevo texto de búsqueda ingresado por el usuario.
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.update { currentState ->
            // Si la búsqueda está vacía, restaurar la lista completa;
            // de lo contrario, filtrar por nombre del chat
            val filteredChats = if (query.isBlank()) {
                // Se vuelve a cargar la lista completa; el collect de getAllChats
                // se encargará de re-popular sin filtro.
                currentState.chats
            } else {
                currentState.chats.filter { chat ->
                    chat.name.contains(query, ignoreCase = true)
                }
            }
            currentState.copy(
                searchQuery = query,
                chats = filteredChats
            )
        }
    }

    /**
     * Elimina un chat y todos sus mensajes asociados.
     *
     * La eliminación se realiza en la base de datos local a través del
     * repositorio de chats. Si el usuario tiene una búsqueda activa,
     * el chat también se elimina de la lista filtrada visible.
     *
     * @param chatId Identificador del chat a eliminar (JID del contacto o ID de grupo).
     */
    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            chatRepository.deleteChat(chatId)
        }
    }

    /**
     * Formatea una marca de tiempo epoch a una cadena legible de hora.
     *
     * Se utiliza para mostrar la hora del último mensaje en cada chat
     * de la lista. Si el timestamp es null, devuelve una cadena vacía.
     *
     * Formato de salida: "HH:mm" (ej: "14:30", "09:05").
     *
     * @param timestamp Marca de tiempo en epoch millis, o null.
     * @return Hora formateada como cadena, o cadena vacía si es null.
     */
    fun getFormattedTime(timestamp: Long?): String {
        if (timestamp == null) return ""
        return timeFormatter.format(Date(timestamp))
    }
}
