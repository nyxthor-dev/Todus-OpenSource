package com.todus.messenger.ui.screens.contacts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.todus.messenger.domain.model.Contact
import com.todus.messenger.ui.theme.TodusBlue
import com.todus.messenger.ui.theme.TodusGray
import com.todus.messenger.ui.theme.TodusGreen
import com.todus.messenger.ui.viewmodel.ContactViewModel
import kotlinx.coroutines.launch

// ============================================================================
// Función auxiliar para formatear números de teléfono cubanos
// ============================================================================

/**
 * Formatea un número de teléfono cubano al formato legible '+53 X XXX XXXX'.
 *
 * Se espera que el número tenga 10 dígitos (prefijo 53 + 8 dígitos).
 * Si el número no tiene la longitud esperada, se devuelve tal cual.
 *
 * Ejemplo:
 * - Entrada: '5351234567' → Salida: '+53 5 123 4567'
 *
 * @param phone Número de teléfono sin formato (10 dígitos, sin el símbolo +).
 * @return Número formateado para mostrar en la UI.
 */
fun formatPhoneNumber(phone: String): String {
    // Validar que tengamos al menos 10 dígitos (formato cubano completo)
    return if (phone.length >= 10) {
        // Prefijo país (53) + primer dígito del número local + resto en grupos de 3 y 4
        "+53 ${phone[2]} ${phone.substring(3, 6)} ${phone.substring(6, 10)}"
    } else {
        // Si no tiene el formato esperado, devolver con el prefijo internacional
        "+$phone"
    }
}

// ============================================================================
// Pantalla principal de selección de contactos
// ============================================================================

/**
 * Pantalla de selección de contactos para iniciar un nuevo chat.
 *
 * Muestra la lista de contactos del dispositivo con indicadores visuales de
 * quiénes son usuarios de ToDus. Permite buscar por nombre o número, y al
 * seleccionar un contacto que usa ToDus se crea o recupera el chat
 * correspondiente.
 *
 * Estructura:
 * - TopAppBar con botón de retroceso, título 'Nuevo chat' e ícono de agregar.
 * - Barra de búsqueda para filtrar contactos.
 * - Lista de contactos con avatar, nombre, teléfono e indicador ToDus.
 * - Snackbar para mostrar mensajes informativos (ej. 'Este contacto no usa ToDus').
 *
 * @param viewModel Instancia de [ContactViewModel] inyectada por Hilt.
 * @param onContactSelected Callback invocado con el chatId al seleccionar un contacto ToDus.
 * @param onNavigateBack Callback para navegar hacia atrás.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSelectorScreen(
    viewModel: ContactViewModel = hiltViewModel(),
    onContactSelected: (chatId: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    // Observar el estado reactivo de la UI desde el ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // Host para mostrar Snackbars (mensajes temporales en la parte inferior)
    val snackbarHostState = remember { SnackbarHostState() }
    // Alcance de corrutinas para lanzar el Snackbar desde un evento onClick
    val scope = rememberCoroutineScope()

    Scaffold(
        // ----------------------------------------------------------------
        // Barra superior con navegación y título
        // ----------------------------------------------------------------
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nuevo chat",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    // Botón de retroceso (flecha hacia atrás)
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver atrás"
                        )
                    }
                },
                // Ícono opcional de persona agregando (a la derecha)
                actions = {
                    IconButton(onClick = { /* Acción futura: agregar nuevo contacto */ }) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Agregar contacto",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        // Host de Snackbars para mensajes informativos
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        // Contenido principal con el padding de la barra superior y Snackbars
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // -------------------------------------------------------------
            // Barra de búsqueda
            // -------------------------------------------------------------
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = {
                    Text(
                        text = "Buscar contacto...",
                        color = TodusGray
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar contacto",
                        tint = TodusGray
                    )
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = TodusBlue
                )
            )

            // -------------------------------------------------------------
            // Contenido según el estado actual de la UI
            // -------------------------------------------------------------
            when {
                // Estado de carga: mostrar indicador circular centrado
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TodusBlue)
                    }
                }

                // Lista vacía (sin resultados de búsqueda)
                uiState.contacts.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Contenido centrado: ícono y texto informativo
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = TodusGray
                            )
                            Text(
                                text = "No se encontraron contactos",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TodusGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Lista de contactos disponible
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.contacts,
                            key = { it.phoneNumber } // Clave única por número de teléfono
                        ) { contact ->
                            // Item individual de contacto
                            ContactItem(
                                contact = contact,
                                onClick = {
                                    if (contact.isToDusUser) {
                                        // El contacto usa ToDus: iniciar o recuperar el chat
                                        val chatId = viewModel.startChatWith(contact)
                                        onContactSelected(chatId)
                                    } else {
                                        // El contacto no usa ToDus: mostrar Snackbar informativo
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Este contacto no usa ToDus"
                                            )
                                        }
                                    }
                                }
                            )

                            // Separador entre items (divisor horizontal)
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// Composable para cada item de la lista de contactos
// ============================================================================

/**
 * Representa visualmente un contacto individual dentro de la lista.
 *
 * Muestra:
 * - Avatar circular (imagen de Coil o iniciales con fondo gris).
 * - Borde verde si el contacto es usuario de ToDus.
 * - Punto verde pequeño si está en línea.
 * - Nombre del contacto y número de teléfono formateado.
 * - Ícono de mensaje (chat) si es usuario ToDus, o texto 'Invitar' si no lo es.
 *
 * @param contact Datos del contacto a mostrar.
 * @param onClick Acción a ejecutar al hacer clic en el item.
 */
@Composable
fun ContactItem(
    contact: Contact,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // -------------------------------------------------------------
        // Avatar circular con bordes e indicadores de estado
        // -------------------------------------------------------------
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            // Superficie circular para el avatar.
            // Si es usuario ToDus, se le añade un borde verde fino.
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                // Borde verde si es usuario ToDus, sin borde si no lo es
                border = if (contact.isToDusUser) {
                    BorderStroke(2.dp, TodusGreen)
                } else {
                    null
                }
            ) {
                if (contact.avatarUrl != null) {
                    // Avatar con imagen cargada desde URL usando Coil
                    AsyncImage(
                        model = contact.avatarUrl,
                        contentDescription = "Avatar de ${contact.name}",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Sin imagen: mostrar iniciales del nombre sobre fondo TodusGray
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = TodusGray.copy(alpha = 0.3f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = contact.name
                                    .split(" ")
                                    .take(2)
                                    .map { it.firstOrNull()?.toString() ?: "" }
                                    .joinToString("")
                                    .uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Punto indicador de estado en línea (esquina inferior derecha del avatar)
            if (contact.isOnline) {
                Surface(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomEnd),
                    shape = CircleShape,
                    color = TodusGreen
                ) {
                    // Superficie vacía que actúa como punto verde de "en línea"
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }

        // Espacio entre el avatar y la información del contacto
        Spacer(modifier = Modifier.width(12.dp))

        // -------------------------------------------------------------
        // Columna de información: nombre y teléfono
        // -------------------------------------------------------------
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Nombre del contacto
            Text(
                text = contact.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Número de teléfono formateado
            Text(
                text = formatPhoneNumber(contact.phoneNumber),
                style = MaterialTheme.typography.bodyMedium,
                color = TodusGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // -------------------------------------------------------------
        // Indicador derecho: ícono de chat o texto 'Invitar'
        // -------------------------------------------------------------
        Spacer(modifier = Modifier.width(8.dp))

        if (contact.isToDusUser) {
            // Es usuario ToDus: mostrar ícono de burbuja de chat en azul
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = "Iniciar conversación",
                tint = TodusBlue,
                modifier = Modifier.size(24.dp)
            )
        } else {
            // No es usuario ToDus: mostrar texto 'Invitar' en gris
            Text(
                text = "Invitar",
                style = MaterialTheme.typography.labelSmall,
                color = TodusGray
            )
        }
    }
}
