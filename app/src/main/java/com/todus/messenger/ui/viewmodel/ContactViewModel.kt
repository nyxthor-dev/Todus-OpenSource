package com.todus.messenger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todus.messenger.domain.model.Contact
import com.todus.messenger.domain.repository.ChatRepository
import com.todus.messenger.domain.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de lista de contactos.
 *
 * Gestiona la carga de contactos, la búsqueda por nombre o número de teléfono,
 * y la creación/inicio de chats con un contacto seleccionado.
 *
 * Anotado con [@HiltViewModel] para inyección de dependencias con Hilt.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContactViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    /**
     * Estado de la interfaz de usuario para la pantalla de contactos.
     *
     * @property contacts Lista de contactos a mostrar (filtrada o completa).
     * @property searchQuery Texto de búsqueda actual para filtrar contactos.
     * @property isLoading Indica si los contactos se están cargando por primera vez.
     */
    data class ContactUiState(
        val contacts: List<Contact> = emptyList(),
        val searchQuery: String = "",
        val isLoading: Boolean = true
    )

    // Flujo mutable para el texto de búsqueda.
    // Se utiliza como entrada para el flatMapLatest que cambia la fuente
    // del flujo de contactos según la consulta.
    private val _searchQuery = MutableStateFlow("")

    // Flujo reactivo de contactos que cambia automáticamente según la búsqueda.
    // - Si la búsqueda está vacía: emite todos los contactos (getAllContacts).
    // - Si hay texto de búsqueda: emite los contactos que coinciden (searchContacts).
    // Se usa WhileSubscribed(5000) para mantener activo el flujo 5 segundos
    // después de que el último colector se vaya, evitando recreaciones innecesarias.
    private val contactsFlow: StateFlow<List<Contact>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                contactRepository.getAllContacts()
            } else {
                contactRepository.searchContacts(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Flujo mutable interno que combina el estado de carga con los datos
    private val _uiState = MutableStateFlow(ContactUiState())

    /**
     * Estado de la UI expuesto de forma inmutable a la vista (Jetpack Compose).
     */
    val uiState: StateFlow<ContactUiState> = _uiState.asStateFlow()

    init {
        // Combinar el flujo reactivo de contactos con el estado de la UI.
        // Cada vez que cambian los contactos (por búsqueda o actualización de BD),
        // se actualiza el estado y se desactiva isLoading.
        viewModelScope.launch {
            contactsFlow.collect { contacts ->
                _uiState.update { currentState ->
                    currentState.copy(
                        contacts = contacts,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Actualiza el texto de búsqueda y cambia la fuente del flujo de contactos.
     *
     * Gracias al uso de [flatMapLatest], al cambiar la consulta se cancela
     * automáticamente la suscripción anterior y se crea una nueva con el
     * texto actualizado, optimizando el rendimiento.
     *
     * @param query Nuevo texto de búsqueda ingresado por el usuario.
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * Inicia o recupera un chat con el contacto seleccionado.
     *
     * Invoca [ChatRepository.getOrCreateChat] con el JID del contacto
     * (construido mediante [Contact.toJid]) y su nombre. Si el chat ya
     * existe en la base de datos local, lo devuelve directamente;
     * si no, lo crea e inserta.
     *
     * @param contact Contacto con el que se desea iniciar una conversación.
     * @return El identificador del chat (chatId) para navegar a la pantalla de chat.
     */
    fun startChatWith(contact: Contact): String {
        val chatId = contact.toJid()
        viewModelScope.launch {
            chatRepository.getOrCreateChat(chatId, contact.name)
        }
        return chatId
    }
}
