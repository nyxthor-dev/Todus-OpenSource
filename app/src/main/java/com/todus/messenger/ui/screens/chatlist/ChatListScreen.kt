package com.todus.messenger.ui.screens.chatlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgeDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.todus.messenger.domain.model.Chat
import com.todus.messenger.domain.model.ConnectionState
import com.todus.messenger.domain.model.MessageStatus
import com.todus.messenger.ui.navigation.Screen
import com.todus.messenger.ui.theme.TodusBlue
import com.todus.messenger.ui.theme.TodusGray
import com.todus.messenger.ui.theme.TodusGreen
import com.todus.messenger.ui.theme.messageBubbleColor
import com.todus.messenger.ui.viewmodel.ChatListViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ============================================================================
// Función principal de la pantalla de lista de chats
// ============================================================================

/**
 * Pantalla principal que muestra la lista de conversaciones (chats) del usuario.
 *
 * Esta pantalla se compone de:
 * - Una barra superior (TopAppBar) con el título "ToDus", indicador de conexión
 *   y barra de búsqueda para filtrar chats por nombre.
 * - Una lista lazy de items de chat, cada uno mostrando avatar, nombre,
 *   último mensaje, hora, contador de no leídos y estado del mensaje.
 * - Un FAB (botón flotante) en la esquina inferior derecha para navegar
 *   a la pantalla de contactos e iniciar un nuevo chat.
 *
 * El estado reactivo se obtiene del [ChatListViewModel] a través de
 * [collectAsState], y las acciones de navegación se delegan a los callbacks
 * [onNavigateToChat] y [onNavigateToContacts].
 *
 * @param viewModel Instancia de [ChatListViewModel] inyectada por Hilt.
 * @param navController Controlador de navegación (disponible pero no se usa
 *   directamente; la navegación se realiza mediante los callbacks).
 * @param onNavigateToChat Callback que recibe el ID del chat al hacer click en uno.
 * @param onNavigateToContacts Callback invocado al presionar el FAB de agregar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel = hiltViewModel(),
    navController: NavController = rememberNavController(),
    onNavigateToChat: (String) -> Unit,
    onNavigateToContacts: () -> Unit
) {
    // Observar el estado reactivo de la UI desde el ViewModel
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        // ----------------------------------------------------------------
        // Barra superior: título, indicador de conexión y búsqueda
        // ----------------------------------------------------------------
        topBar = {
            ChatListTopBar(
                connectionState = uiState.connectionState,
                searchQuery = uiState.searchQuery,
                onSearchQueryChanged = viewModel::onSearchQueryChanged
            )
        },
        // ----------------------------------------------------------------
        // Botón flotante para agregar nuevo chat
        // ----------------------------------------------------------------
        floatingActionButton = {
            // SmallFloatingActionButton con animación de entrada suave
            SmallFloatingActionButton(
                onClick = onNavigateToContacts,
                containerColor = TodusBlue,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar nuevo chat"
                )
            }
        }
    ) { paddingValues ->
        // ----------------------------------------------------------------
        // Contenido principal según el estado de la UI
        // ----------------------------------------------------------------
        when {
            // Estado de carga inicial: mostrar indicador circular centrado
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TodusBlue)
                }
            }

            // Lista vacía: mostrar mensaje indicando que no hay conversaciones
            uiState.chats.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = TodusGray
                        )
                        Text(
                            text = "No hay conversaciones",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TodusGray
                        )
                    }
                }
            }

            // Lista de chats disponible: mostrar LazyColumn con los items
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(
                        items = uiState.chats,
                        key = { chat -> chat.id }
                    ) { chat ->
                        ChatItem(
                            chat = chat,
                            onClick = { onNavigateToChat(chat.id) },
                            formattedTime = formatTimestamp(chat.lastMessageTime)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// Barra superior (TopAppBar) con título, conexión y búsqueda
// ============================================================================

/**
 * Barra superior personalizada de la pantalla de lista de chats.
 *
 * Muestra:
 * - El título "ToDus" en color [TodusBlue] con fuente titleLarge.
 * - Un indicador de estado de conexión (punto verde si [ConnectionState.Connected],
 *   rojo si está desconectado, amarillo si está en transición).
 * - Una barra de búsqueda expandible ([OutlinedTextField]) que filtra los chats
 *   por nombre. Cuando la consulta está vacía se muestra un ícono de búsqueda;
 *   cuando hay texto se muestra un ícono para limpiar la búsqueda.
 *
 * @param connectionState Estado actual de la conexión con el servidor XMPP.
 * @param searchQuery Texto de búsqueda actual ingresado por el usuario.
 * @param onSearchQueryChanged Callback invocado al modificar el texto de búsqueda.
 */
@Composable
private fun ChatListTopBar(
    connectionState: ConnectionState,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit
) {
    // Determinar el color del indicador de conexión según el estado
    val connectionIndicatorColor = when (connectionState) {
        is ConnectionState.Connected -> TodusGreen
        is ConnectionState.Disconnected -> Color.Red
        is ConnectionState.Connecting,
        is ConnectionState.Authenticating -> Color(0xFFFFC107) // Amarillo: estado transitorio
        is ConnectionState.Error -> Color.Red
    }

    Column {
        // Fila del título y el indicador de conexión
        Surface(
            shadowElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Título "ToDus" en azul institucional
                    Text(
                        text = "ToDus",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TodusBlue,
                        modifier = Modifier.weight(1f)
                    )

                    // Indicador de estado de conexión (punto coloreado)
                    // Verde = conectado, Rojo = desconectado/error, Amarillo = transicionando
                    Surface(
                        modifier = Modifier.size(12.dp),
                        shape = CircleShape,
                        color = connectionIndicatorColor
                    ) {
                        // Superficie vacía que actúa como punto de indicador
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }

                // Barra de búsqueda que filtra los chats por nombre
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = {
                        Text(
                            text = "Buscar chat...",
                            color = TodusGray
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = TodusGray
                        )
                    },
                    // Mostrar ícono de cerrar solo cuando hay texto en la búsqueda
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar búsqueda",
                                    tint = TodusGray
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TodusBlue,
                        unfocusedBorderColor = TodusGray,
                        cursorColor = TodusBlue
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// ============================================================================
// Item individual de chat en la lista
// ============================================================================

/**
 * Representa visualmente un chat individual dentro de la lista de conversaciones.
 *
 * Cada item muestra:
 * - **Avatar**: Imagen circular de 48.dp cargada desde la URL ([AsyncImage] de Coil)
 *   si [Chat.avatarUrl] no es nulo; de lo contrario, un círculo con las iniciales
 *   del nombre del chat en fondo [TodusBlue] y texto blanco.
 * - **Indicador online**: Un pequeño punto verde visible solo si el chat no es
 *   grupo ([Chat.isGroup] == false) y el contacto está en línea ([Chat.isOnline]).
 * - **Información del chat**: Nombre en estilo titleMedium y último mensaje
 *   en bodyMedium con color gris, truncado a una sola línea.
 * - **Metadatos**: Hora formateada del último mensaje, badge con contador de
 *   mensajes no leídos (si aplica), e ícono de estado del último mensaje enviado.
 *
 * @param chat Datos del chat a mostrar.
 * @param onClick Callback invocado al hacer click en el item.
 * @param formattedTime Hora formateada del último mensaje (procesada por [formatTimestamp]).
 */
@Composable
private fun ChatItem(
    chat: Chat,
    onClick: () -> Unit,
    formattedTime: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ------------------------------------------------------------------
        // Avatar: imagen remota o iniciales como respaldo
        // ------------------------------------------------------------------
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            if (chat.avatarUrl != null) {
                // Cargar la imagen del avatar desde la URL usando Coil
                AsyncImage(
                    model = chat.avatarUrl,
                    contentDescription = "Avatar de ${chat.name}",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
            } else {
                // Mostrar iniciales del nombre en un círculo azul de ToDus
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = TodusBlue
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getInitials(chat.name),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Indicador de estado online: punto verde pequeño en la esquina
            // inferior derecha del avatar. Solo visible si NO es grupo y está en línea.
            if (chat.isOnline && !chat.isGroup) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp),
                    shape = CircleShape,
                    color = Color.White
                ) {
                    // Borde blanco alrededor del punto verde para mayor visibilidad
                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(8.dp),
                        shape = CircleShape,
                        color = TodusGreen
                    ) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // ------------------------------------------------------------------
        // Columna central: nombre y último mensaje
        // ------------------------------------------------------------------
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Nombre del chat (contacto o grupo)
            Text(
                text = chat.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Último mensaje del chat, truncado a una línea
            Text(
                text = chat.lastMessage ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = TodusGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // ------------------------------------------------------------------
        // Columna derecha: hora, badge de no leídos e ícono de estado
        // ------------------------------------------------------------------
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Hora del último mensaje
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = TodusGray
            )

            // Fila con el badge de no leídos y el ícono de estado del mensaje
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Badge con la cantidad de mensajes no leídos
                if (chat.unreadCount > 0) {
                    Badge(
                        containerColor = TodusBlue,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = chat.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Ícono de estado del último mensaje enviado
                // Solo se muestra si hay un estado definido (mensajes enviados por mí)
                if (chat.lastMessageStatus != null) {
                    MessageStatusIcon(status = chat.lastMessageStatus)
                }
            }
        }
    }
}

// ============================================================================
// Ícono de estado del mensaje (enviado, entregado, leído, etc.)
// ============================================================================

/**
 * Muestra un ícono pequeño que representa el estado de entrega del último
 * mensaje enviado por el usuario en la lista de chats.
 *
 * Los íconos siguen el patrón visual de ToDus/WhatsApp:
 * - [MessageStatus.SENDING]: Reloj (⏳) en gris.
 * - [MessageStatus.SENT]: Una sola marca de verificación (✓) en gris.
 * - [MessageStatus.DELIVERED]: Doble marca de verificación (✓✓) en gris.
 * - [MessageStatus.READ]: Doble marca de verificación (✓✓) en azul.
 * - [MessageStatus.FAILED]: Ícono de error (X) en rojo.
 *
 * @param status Estado del mensaje a representar visualmente.
 */
@Composable
private fun MessageStatusIcon(status: MessageStatus) {
    when (status) {
        MessageStatus.SENDING -> {
            // Mensaje enviando: ícono de reloj en gris
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Enviando",
                modifier = Modifier.size(16.dp),
                tint = TodusGray
            )
        }
        MessageStatus.SENT -> {
            // Mensaje enviado al servidor: una sola marca de verificación en gris
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = "Enviado",
                modifier = Modifier.size(16.dp),
                tint = TodusGray
            )
        }
        MessageStatus.DELIVERED -> {
            // Mensaje entregado al destinatario: doble marca en gris
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Entregado",
                modifier = Modifier.size(16.dp),
                tint = TodusGray
            )
        }
        MessageStatus.READ -> {
            // Mensaje leído por el destinatario: doble marca en azul (ToDus Blue)
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Leído",
                modifier = Modifier.size(16.dp),
                tint = TodusBlue
            )
        }
        MessageStatus.FAILED -> {
            // Error al enviar: ícono de cierre (X) en rojo
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Error al enviar",
                modifier = Modifier.size(16.dp),
                tint = Color.Red
            )
        }
    }
}

// ============================================================================
// Funciones auxiliares
// ============================================================================

/**
 * Formatea una marca de tiempo epoch millis a una cadena legible relativa.
 *
 * La lógica de formato es:
 * - Si la fecha es **hoy**: retorna la hora en formato "HH:mm" (ej: "14:30").
 * - Si la fecha es **de esta semana** (entre hoy y hace 7 días): retorna
 *   el día y mes en formato "dd/MM" (ej: "15/06").
 * - Si la fecha es **más antigua** (más de 7 días): retorna día, mes y año
 *   abreviado en formato "dd/MM/yy" (ej: "10/05/24").
 *
 * @param timestamp Marca de tiempo en epoch millis, o null.
 * @return Cadena formateada según la antigüedad del timestamp, o cadena vacía si es null.
 */
fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null) return ""

    val messageDate = Calendar.getInstance().apply {
        timeInMillis = timestamp
    }

    val now = Calendar.getInstance()

    // Obtener solo la parte de fecha (sin hora) para comparar
    val messageDay = Calendar.getInstance().apply {
        set(Calendar.YEAR, messageDate.get(Calendar.YEAR))
        set(Calendar.MONTH, messageDate.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, messageDate.get(Calendar.DAY_OF_MONTH))
    }

    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // Calcular la diferencia en días
    val diffMillis = today.timeInMillis - messageDay.timeInMillis
    val diffDays = diffMillis / (24 * 60 * 60 * 1000)

    val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    val dayMonthFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    return when {
        // Mismo día: mostrar solo la hora
        diffDays == 0L -> timeFormat.format(Date(timestamp))
        // Esta semana (hasta 7 días atrás): mostrar día y mes
        diffDays in 1..6 -> dayMonthFormat.format(Date(timestamp))
        // Más de una semana: mostrar día, mes y año abreviado
        else -> dateFormat.format(Date(timestamp))
    }
}

/**
 * Extrae las primeras dos letras del nombre proporcionado para usarlas
 * como iniciales del avatar cuando no hay imagen disponible.
 *
 * La lógica de extracción es:
 * 1. Dividir el nombre por espacios para obtener las palabras.
 * 2. Si hay dos o más palabras, tomar la primera letra de la primera
 *    y la primera letra de la segunda palabra.
 * 3. Si hay una sola palabra, tomar los primeros dos caracteres.
 *
 * El resultado se retorna en mayúsculas.
 *
 * Ejemplos:
 * - "Carlos García" → "CG"
 * - "María" → "MA"
 * - "A" → "A"
 *
 * @param name Nombre del contacto o grupo.
 * @return Cadena de hasta 2 letras en mayúsculas.
 */
fun getInitials(name: String): String {
    if (name.isBlank()) return ""

    val words = name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }

    return when {
        // Dos o más palabras: primera letra de cada una de las dos primeras
        words.size >= 2 -> {
            (words[0].firstOrNull()?.uppercaseChar()?.toString() ?: "") +
                (words[1].firstOrNull()?.uppercaseChar()?.toString() ?: "")
        }
        // Una sola palabra: primeros dos caracteres
        words.size == 1 -> {
            val word = words[0]
            word.take(2).uppercase()
        }
        // Caso improbable: nombre vacío después de limpiar
        else -> ""
    }
}
