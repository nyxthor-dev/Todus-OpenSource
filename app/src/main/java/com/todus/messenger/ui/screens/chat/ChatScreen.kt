package com.todus.messenger.ui.screens.chat

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.todus.messenger.domain.model.Message
import com.todus.messenger.domain.model.MessageStatus
import com.todus.messenger.domain.model.MessageType
import com.todus.messenger.ui.theme.MessageBubbleMe
import com.todus.messenger.ui.theme.MessageBubbleShape
import com.todus.messenger.ui.theme.TodusBlue
import com.todus.messenger.ui.theme.TodusGray
import com.todus.messenger.ui.theme.TodusGreen
import com.todus.messenger.ui.theme.messageBubbleColor
import com.todus.messenger.ui.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================================
// Pantalla principal del chat individual
// ============================================================================

/**
 * Pantalla de chat individual de ToDus Messenger.
 *
 * Muestra la conversación con un contacto específico, permitiendo
 * visualizar el historial de mensajes y enviar nuevos mensajes de texto.
 *
 * Utiliza [ChatViewModel] para gestionar el estado reactivo de la pantalla.
 * El ViewModel se inyecta automáticamente con Hilt mediante [hiltViewModel].
 *
 * @param viewModel Instancia del ViewModel del chat, inyectada por Hilt.
 * @param onNavigateBack Acción lambda para navegar hacia atrás (a la lista de chats).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    // --------------------------------------------------------------------------
    // Estado reactivo: observar el flujo del ViewModel
    // --------------------------------------------------------------------------
    val uiState by viewModel.uiState.collectAsState()

    // Estado para controlar el scroll automático hacia el último mensaje
    val listState = rememberLazyListState()

    // --------------------------------------------------------------------------
    // Auto-scroll: cuando llega un nuevo mensaje, desplazar al índice 0
    // (el primer item de la lista invertida se muestra en la parte inferior).
    // --------------------------------------------------------------------------
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(index = 0)
        }
    }

    // --------------------------------------------------------------------------
    // Estructura principal con Scaffold (topBar + contenido + bottomBar)
    // --------------------------------------------------------------------------
    Scaffold(
        // Barra superior con nombre del contacto y estado de conexión
        topBar = {
            ChatTopBar(
                chatName = uiState.chat?.name ?: "",
                isOnline = uiState.chat?.isOnline ?: false,
                onNavigateBack = onNavigateBack
            )
        },
        // Barra inferior con campo de texto y botón de enviar
        bottomBar = {
            ChatInputBar(
                messageText = uiState.messageText,
                isSending = uiState.isSending,
                onMessageTextChanged = viewModel::onMessageTextChanged,
                onSendMessage = viewModel::sendMessage
            )
        },
        // Color de fondo del Scaffold según el tema
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        // ----------------------------------------------------------------------
        // Contenido principal
        // ----------------------------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                // Indicador de carga mientras se obtienen los mensajes
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center),
                    color = TodusBlue,
                    strokeWidth = 4.dp
                )
            } else {
                // Lista invertida de mensajes (el más reciente se muestra abajo)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // Invertir el layout: el primer item de la lista se muestra abajo
                    reverseLayout = true,
                    // Estado del scroll para controlar la posición
                    state = listState,
                    // Padding vertical de 8.dp y espaciado entre mensajes de 4.dp
                    contentPadding = PaddingValues(
                        vertical = 8.dp,
                        horizontal = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Renderizar cada mensaje como una burbuja
                    items(
                        items = uiState.messages,
                        key = { it.id }
                    ) { message ->
                        MessageBubble(message = message)
                    }
                }
            }
        }
    }
}

// ============================================================================
// TopAppBar: Barra superior con nombre del contacto y estado de conexión
// ============================================================================

/**
 * Barra superior de la pantalla de chat.
 *
 * Muestra el nombre del contacto y su estado de conexión (en línea/desconectado),
 * junto con un botón de flecha para navegar hacia atrás.
 *
 * @param chatName Nombre del contacto con el que se está chateando.
 * @param isOnline Indica si el contacto está actualmente en línea.
 * @param onNavigateBack Acción lambda para retroceder a la pantalla anterior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    chatName: String,
    isOnline: Boolean,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                // Nombre del contacto
                Text(
                    text = chatName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Subtítulo: estado de conexión
                // Verde ('En línea') si está conectado, gris ('Desconectado') si no
                Text(
                    text = if (isOnline) "En línea" else "Desconectado",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOnline) TodusGreen else TodusGray
                )
            }
        },
        // Botón de retroceso
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver atrás",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    )
}

// ============================================================================
// MessageBubble: Burbuja individual de mensaje
// ============================================================================

/**
 * Composable que representa una burbuja de mensaje individual dentro del chat.
 *
 * La burbuja cambia de estilo según si el mensaje fue enviado por mí ([Message.isFromMe])
 * o por el contacto. Los mensajes eliminados ([Message.deleted]) se muestran centrados
 * con un estilo especial (texto en cursiva gris con fondo translúcido).
 *
 * Las burbujas de mensajes propios se alinean a la derecha con fondo azul ToDus
 * y texto blanco. Las burbujas de otros se alinean a la izquierda con fondo
 * blanco (tema claro) o gris oscuro (tema oscuro) y texto oscuro/blanco.
 *
 * La forma de la burbuja tiene esquinas redondeadas de 16.dp excepto la esquina
 * inferior del lado del remitente (4.dp), creando un efecto de bocadillo de chat.
 *
 * @param message El mensaje a renderizar en la burbuja.
 */
@Composable
fun MessageBubble(message: Message) {
    // ----------------------------------------------------------------------
    // Caso especial: mensaje eliminado (estilo centrado y distinto)
    // ----------------------------------------------------------------------
    if (message.deleted) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Text(
                    text = "Mensaje eliminado",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        color = TodusGray
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    // ----------------------------------------------------------------------
    // Determinar colores, forma y alineación según el remitente
    // ----------------------------------------------------------------------
    val isFromMe = message.isFromMe
    val isDark = isSystemInDarkTheme()

    // Color de fondo de la burbuja: azul ToDus si es mío, blanco/gris oscuro si no
    val bubbleColor = if (isFromMe) {
        MessageBubbleMe
    } else {
        messageBubbleColor(isDarkTheme = isDark, isMessageFromMe = false)
    }

    // Color del texto: blanco si es mío, oscuro/blanco según tema si es del otro
    val textColor = if (isFromMe) {
        Color.White
    } else {
        if (isDark) Color.White else Color(0xFF1A1C1E)
    }

    // Forma de la burbuja con efecto bocadillo: esquina inferior del lado
    // del remitente con menor redondeo (4.dp en lugar de 16.dp)
    val bubbleShape = if (isFromMe) {
        // Mis mensajes: esquina inferior derecha menos redondeada
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 4.dp
        )
    } else {
        // Mensajes del otro: esquina inferior izquierda menos redondeada
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 4.dp,
            bottomEnd = 16.dp
        )
    }

    // Alineación horizontal según el remitente
    val horizontalAlignment = if (isFromMe) Arrangement.End else Arrangement.Start

    // ----------------------------------------------------------------------
    // Contenedor principal del mensaje (Row + Column anidados)
    // ----------------------------------------------------------------------
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = horizontalAlignment
    ) {
        Column(
            // Limitar el ancho de la burbuja al 80% de la pantalla
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalAlignment = horizontalAlignment
        ) {
            // --------------------------------------------------------------
            // Burbuja de mensaje: Surface con forma, color y contenido
            // --------------------------------------------------------------
            Surface(
                shape = bubbleShape,
                color = bubbleColor,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    )
                ) {
                    // Texto del mensaje con estilo bodyLarge
                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )

                    // Indicador de mensaje editado (en cursiva, más pequeño)
                    if (message.edited) {
                        Text(
                            text = "(editado)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            color = if (isFromMe) {
                                Color.White.copy(alpha = 0.7f)
                            } else {
                                TodusGray
                            }
                        )
                    }
                }
            }

            // --------------------------------------------------------------
            // Fila inferior: timestamp (HH:mm) e icono de estado del mensaje
            // --------------------------------------------------------------
            Row(
                modifier = Modifier.padding(
                    top = 2.dp,
                    start = 4.dp,
                    end = 4.dp,
                    bottom = 2.dp
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isFromMe) {
                    Arrangement.spacedBy(4.dp, Alignment.End)
                } else {
                    Arrangement.spacedBy(4.dp, Alignment.Start)
                }
            ) {
                // Hora del mensaje formateada como HH:mm
                Text(
                    text = formatMessageTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp
                    ),
                    color = TodusGray
                )

                // Icono de estado (solo para mensajes enviados por mí):
                // - SENDING:    reloj gris
                // - SENT:       check gris
                // - DELIVERED:  doble check gris
                // - READ:       doble check azul
                // - FAILED:     X rojo
                if (isFromMe) {
                    MessageStatusIcon(status = message.status)
                }
            }
        }
    }
}

// ============================================================================
// MessageStatusIcon: Ícono de estado de entrega del mensaje
// ============================================================================

/**
 * Ícono visual que indica el estado de entrega de un mensaje enviado por mí.
 *
 * Sigue el patrón visual de ToDus/WhatsApp:
 * - [MessageStatus.SENDING]: reloj gris (el mensaje se está enviando).
 * - [MessageStatus.SENT]: un solo check gris (recibido por el servidor).
 * - [MessageStatus.DELIVERED]: doble check gris (entregado al dispositivo).
 * - [MessageStatus.READ]: doble check azul (leído por el destinatario).
 * - [MessageStatus.FAILED]: ícono de error rojo (fallo en el envío).
 *
 * @param status Estado actual del mensaje.
 */
@Composable
private fun MessageStatusIcon(status: MessageStatus) {
    when (status) {
        MessageStatus.SENDING -> {
            // Reloj: el mensaje se está enviando al servidor
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = "Enviando",
                modifier = Modifier.size(16.dp),
                tint = TodusGray
            )
        }
        MessageStatus.SENT -> {
            // Un solo check gris: el servidor recibió el mensaje
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = "Enviado",
                modifier = Modifier.size(16.dp),
                tint = TodusGray
            )
        }
        MessageStatus.DELIVERED -> {
            // Doble check gris: el mensaje llegó al dispositivo del destinatario
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Entregado",
                modifier = Modifier.size(16.dp),
                tint = TodusGray
            )
        }
        MessageStatus.READ -> {
            // Doble check azul: el destinatario abrió y leyó el mensaje
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Leído",
                modifier = Modifier.size(16.dp),
                tint = TodusBlue
            )
        }
        MessageStatus.FAILED -> {
            // Ícono de error rojo: no se pudo enviar el mensaje
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error al enviar",
                modifier = Modifier.size(16.dp),
                tint = Color(0xFFBA1A1A) // Rojo de error del tema
            )
        }
    }
}

// ============================================================================
// ChatInputBar: Barra inferior de entrada de mensaje
// ============================================================================

/**
 * Barra de entrada de mensajes ubicada en la parte inferior de la pantalla.
 *
 * Contiene un [OutlinedTextField] para escribir el mensaje y un [IconButton]
 * para enviarlo. El botón de enviar cambia de color según si hay texto escrito:
 * - Con texto: ícono azul ([TodusBlue]).
 * - Sin texto: ícono gris ([TodusGray]).
 *
 * Cuando [isSending] es true, se muestra un [CircularProgressIndicator] pequeño
 * en lugar del botón de enviar.
 *
 * @param messageText Texto actual del campo de entrada.
 * @param isSending Indica si se está enviando un mensaje (muestra un spinner).
 * @param onMessageTextChanged Callback invocado al cambiar el texto del campo.
 * @param onSendMessage Callback invocado al presionar el botón de enviar.
 */
@Composable
private fun ChatInputBar(
    messageText: String,
    isSending: Boolean,
    onMessageTextChanged: (String) -> Unit,
    onSendMessage: () -> Unit
) {
    // Fondo de la barra de entrada con sombra superior
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ------------------------------------------------------------------
            // Campo de texto para escribir el mensaje
            // ------------------------------------------------------------------
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageTextChanged,
                placeholder = {
                    Text(
                        text = "Escribe un mensaje...",
                        color = TodusGray
                    )
                },
                // Máximo 4 líneas antes de hacer scroll interno
                maxLines = 4,
                // Forma redondeada (píldora) para el campo de texto
                shape = RoundedCornerShape(24.dp),
                // Colores personalizados del campo de texto
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TodusBlue,
                    unfocusedBorderColor = TodusGray.copy(alpha = 0.5f),
                    cursorColor = TodusBlue
                ),
                // Ocupar el espacio restante con peso 1
                modifier = Modifier.weight(1f)
            )

            // ------------------------------------------------------------------
            // Botón de enviar mensaje
            // ------------------------------------------------------------------
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp)
            ) {
                if (isSending) {
                    // Mientras se envía: indicador de carga circular azul
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = TodusBlue,
                        strokeWidth = 2.dp
                    )
                } else {
                    // Botón de enviar con ícono de avión de papel
                    IconButton(
                        onClick = onSendMessage,
                        enabled = messageText.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Enviar mensaje",
                            // Tinte azul si hay texto, gris si está vacío
                            tint = if (messageText.isNotBlank()) TodusBlue else TodusGray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// Utilidades
// ============================================================================

/**
 * Formatea una marca de tiempo en epoch millis a una cadena de texto
 * con formato "HH:mm" (hora y minutos en 24 horas).
 *
 * Ejemplo: 1697040000000L -> "14:00"
 *
 * Nota: Se crea una nueva instancia de [SimpleDateFormat] en cada invocación
 * para evitar problemas con el uso de [remember] fuera de un Composable.
 *
 * @param timestamp Marca de tiempo en epoch millis.
 * @return Cadena con la hora formateada como "HH:mm".
 */
fun formatMessageTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
