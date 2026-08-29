package com.todus.messenger.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todus.messenger.data.remote.xmpp.ToDusXmppClient
import com.todus.messenger.domain.model.Chat
import com.todus.messenger.domain.model.Message
import com.todus.messenger.domain.repository.ChatRepository
import com.todus.messenger.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de chat individual (conversación con un contacto).
 *
 * Gestiona la carga de mensajes, el envío de nuevos mensajes,
 * el marcado como leídos y la actualización del texto del campo de entrada.
 *
 * El [chatId] se obtiene de los argumentos de navegación mediante
 * [SavedStateHandle] (clave "chatId").
 *
 * Anotado con [@HiltViewModel] para inyección de dependencias con Hilt.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageRepository: MessageRepository,
    private val chatRepository: ChatRepository,
    private val xmppClient: ToDusXmppClient
) : ViewModel() {

    /**
     * Estado de la interfaz de usuario para la pantalla de chat.
     *
     * @property messages Lista de mensajes del chat actual, ordenados cronológicamente.
     * @property chat Datos del chat actual (nombre, avatar, JID, etc.).
     * @property messageText Texto actual del campo de entrada de mensaje.
     * @property isLoading Indica si los datos del chat se están cargando por primera vez.
     * @property isSending Indica si actualmente se está enviando un mensaje.
     */
    data class ChatUiState(
        val messages: List<Message> = emptyList(),
        val chat: Chat? = null,
        val messageText: String = "",
        val isLoading: Boolean = true,
        val isSending: Boolean = false
    )

    // Identificador del chat obtenido de los argumentos de navegación
    private val chatId: String = savedStateHandle["chatId"]
        ?: throw IllegalArgumentException("Se requiere el argumento 'chatId' para ChatViewModel")

    // Flujo mutable interno que se actualiza desde las corrutinas
    private val _uiState = MutableStateFlow(ChatUiState())

    /**
     * Estado de la UI expuesto de forma inmutable a la vista (Jetpack Compose).
     */
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Cargar los datos del chat (nombre, avatar, etc.) desde el repositorio.
        // Si el chat no existe, se crea uno nuevo con el JID proporcionado.
        viewModelScope.launch {
            val chat = chatRepository.getOrCreateChat(chatId, chatId)
            _uiState.update { it.copy(chat = chat) }
        }

        // Observar los mensajes del chat de forma reactiva.
        // Cada nuevo mensaje insertado en la base de datos se refleja
        // automáticamente en la lista que se muestra en pantalla.
        viewModelScope.launch {
            messageRepository.getMessages(chatId).collect { messages ->
                _uiState.update { currentState ->
                    currentState.copy(
                        messages = messages,
                        isLoading = false
                    )
                }
            }
        }

        // Marcar todos los mensajes del chat como leídos al abrir la conversación.
        // Esto reinicia el contador de mensajes no leídos tanto en la tabla
        // de mensajes como en la tabla de chats.
        viewModelScope.launch {
            chatRepository.markAsRead(chatId)
            messageRepository.markMessagesAsRead(chatId)
        }
    }

    /**
     * Actualiza el texto del campo de entrada de mensaje.
     *
     * Se invoca desde la vista cada vez que el usuario escribe o borra
     * texto en el TextField de Compose.
     *
     * @param text Nuevo texto del campo de entrada.
     */
    fun onMessageTextChanged(text: String) {
        _uiState.update { it.copy(messageText = text) }
    }

    /**
     * Envía el mensaje de texto que el usuario ha escrito en el campo de entrada.
     *
     * Flujo de ejecución:
     * 1. Verifica que el texto no esté vacío (después de trim).
     * 2. Activa el indicador de envío ([isSending] = true).
     * 3. Invoca [MessageRepository.sendMessage] que orquesta:
     *    - Generación de UUID para el mensaje.
     *    - Inserción local con estado SENDING.
     *    - Envío por XMPP.
     *    - Actualización del estado a SENT si fue exitoso.
     * 4. Actualiza el último mensaje del chat en el repositorio.
     * 5. Limpia el campo de texto y desactiva el indicador de envío.
     */
    fun sendMessage() {
        val text = _uiState.value.messageText.trim()
        if (text.isEmpty()) return

        // Activar indicador de envío mientras se procesa el mensaje
        _uiState.update { it.copy(isSending = true) }

        viewModelScope.launch {
            // Enviar el mensaje a través del repositorio (almacenamiento local + XMPP)
            val result = messageRepository.sendMessage(chatId, text)

            if (result.isSuccess) {
                // Actualizar el último mensaje mostrado en el chat
                chatRepository.updateLastMessage(
                    chatId = chatId,
                    lastMessage = text,
                    time = System.currentTimeMillis(),
                    status = null
                )
            }

            // Limpiar el campo de texto y desactivar el indicador de envío
            _uiState.update {
                it.copy(
                    messageText = "",
                    isSending = false
                )
            }
        }
    }
}
