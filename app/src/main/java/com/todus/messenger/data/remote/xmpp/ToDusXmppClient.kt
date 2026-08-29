package com.todus.messenger.data.remote.xmpp

import android.content.Context
import android.util.Log
import com.todus.messenger.domain.model.ConnectionState
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jivesoftware.smack.ConnectionListener
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.ConnectionConfiguration
import org.jivesoftware.smack.filter.MessageTypeFilter
import org.jivesoftware.smack.filter.PresenceTypeFilter
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jivesoftware.smackx.chatstates.ChatState
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension
import org.jivesoftware.smackx.delay.packet.DelayInformation
import org.jivesoftware.smackx.receipts.DeliveryReceipt
import org.jxmpp.util.Resourcepart
import org.jxmpp.jid.JidCreate

/**
 * Cliente XMPP principal para la conexión con el servidor ToDus.
 *
 * Esta clase envuelve la biblioteca Smack (XMPPTCPConnection) y proporciona
 * una API reactiva basada en Kotlin Coroutines y Flow para:
 * - Conectar y autenticar con el servidor ToDus (SASL PLAIN, sin TLS)
 * - Enviar y recibir mensajes de texto
 * - Enviar notificaciones de estado de chat (composing, paused, etc.)
 * - Enviar confirmaciones de lectura (XEP-0184)
 * - Monitorear la presencia de contactos (online/offline)
 * - Emitir estados de conexión reactivos
 *
 * ## Configuración del servidor ToDus
 * - **Host:** todus.cu
 * - **Puerto producción:** 1756 (XMPP sobre TCP, sin TLS)
 * - **Formato JID:** {phoneNumber}@todus.cu
 * - **Autenticación:** SASL PLAIN (usuario = número de teléfono)
 * - **TLS:** Deshabilitado
 */
class ToDusXmppClient(
    private val context: Context,
    private val host: String = "todus.cu",
    private val port: Int = 1756
) {
    companion object {
        private const val TAG = "ToDusXmppClient"
        private const val RESOURCE = "ToDusMessenger"
        private const val XMPP_DOMAIN = "todus.cu"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val REPLY_TIMEOUT_MS = 10_000
        private const val PRESENCE_PRIORITY = 1
    }

    // --- Alcance de corrutinas para operaciones asíncronas internas ---
    private val scope = CoroutineScope(Dispatchers.IO)

    // --- Conexión Smack subyacente ---
    private var connection: XMPPTCPConnection? = null

    // --- Número de teléfono del usuario autenticado (sin dominio) ---
    private var authenticatedPhoneNumber: String = ""

    // --- Flujo reactivo del estado de conexión (tipado explícitamente) ---
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    /**
     * Flujo de solo lectura que emite el estado actual de la conexión XMPP.
     */
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // --- Flujo de mensajes entrantes (sin replay, buffer de 64) ---
    private val _incomingMessages: MutableSharedFlow<InMessage> =
        MutableSharedFlow(replay = 0, extraBufferCapacity = 64)

    // --- Flujo de eventos de presencia de contactos ---
    private val _contactPresenceEvents = MutableSharedFlow<Pair<String, Boolean>>(
        replay = 0, extraBufferCapacity = 64
    )

    /**
     * Conecta al servidor XMPP de ToDus y autentica al usuario.
     *
     * @param phoneNumber Número de teléfono del usuario (sin @todus.cu)
     * @param password Contraseña/Token JWT de la cuenta ToDus
     * @return [Result.success] si la conexión y autenticación fueron exitosas,
     *         [Result.failure] con la excepción correspondiente en caso de error
     */
    fun connect(phoneNumber: String, password: String): Result<Unit> {
        return try {
            Log.d(TAG, "Iniciando conexión XMPP a $host:$port")
            _connectionState.value = ConnectionState.Connecting

            // --- Construir configuración de conexión TCP ---
            val config = XMPPTCPConnectionConfiguration.builder()
                .setHost(host)
                .setPort(port)
                // ToDus NO utiliza TLS
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                .setXmppDomain(JidCreate.domainBareFrom(XMPP_DOMAIN))
                .setResourcepart(Resourcepart.from(RESOURCE))
                .setConnectTimeout(CONNECT_TIMEOUT_MS)
                .setReplyTimeout(REPLY_TIMEOUT_MS)
                .build()

            // --- Crear la conexión XMPP ---
            connection = XMPPTCPConnection(config)
            val conn = connection!!

            // --- Registrar listener de ciclo de vida de la conexión ---
            conn.addConnectionListener(connectionListener)

            // --- Conectar al servidor (bloqueante) ---
            conn.connect()
            Log.d(TAG, "Conexión TCP establecida con $host:$port")
            _connectionState.value = ConnectionState.Connected

            // --- Autenticar con SASL PLAIN ---
            conn.login(phoneNumber, password)
            Log.d(TAG, "Autenticación SASL PLAIN exitosa para $phoneNumber")
            _connectionState.value = ConnectionState.Connected

            // --- Guardar credenciales para uso interno ---
            authenticatedPhoneNumber = phoneNumber
            XmppMessageMapper.currentPhoneNumber = phoneNumber

            // --- Registrar listeners de estanzas entrantes ---
            registerStanzaListeners(conn)

            // --- Enviar presencia disponible ---
            sendPresence()

            Log.d(TAG, "Cliente XMPP completamente inicializado")
            Result.success(Unit)
        } catch (e: XMPPException.XMPPErrorException) {
            Log.e(TAG, "Error XMPP al conectar: ${e.message}", e)
            _connectionState.value = ConnectionState.Error("Error de conexion")
            Result.failure(e)
        } catch (e: SmackException) {
            Log.e(TAG, "Error de Smack al conectar: ${e.message}", e)
            _connectionState.value = ConnectionState.Error("Error de conexion")
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Error de E/S al conectar: ${e.message}", e)
            _connectionState.value = ConnectionState.Error("Error de conexion")
            Result.failure(e)
        } catch (e: InterruptedException) {
            Log.e(TAG, "Conexión interrumpida", e)
            _connectionState.value = ConnectionState.Disconnected
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al conectar: ${e.message}", e)
            _connectionState.value = ConnectionState.Error("Error de conexion")
            Result.failure(e)
        }
    }

    /**
     * Desconecta del servidor XMPP de forma ordenada.
     */
    fun disconnect() {
        val conn = connection ?: run {
            Log.w(TAG, "disconnect() llamado sin conexión activa")
            _connectionState.value = ConnectionState.Disconnected
            return
        }

        try {
            val unavailablePresence = Presence(Presence.Type.unavailable)
            conn.sendStanza(unavailablePresence)
            Log.d(TAG, "Presencia unavailable enviada")
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo enviar presencia unavailable: ${e.message}")
        }

        try {
            conn.disconnect()
            Log.d(TAG, "Conexión XMPP cerrada")
        } catch (e: Exception) {
            Log.w(TAG, "Error al cerrar la conexión: ${e.message}")
        }

        authenticatedPhoneNumber = ""
        connection = null
        _connectionState.value = ConnectionState.Disconnected
    }

    /**
     * Envía un mensaje de texto a un destinatario.
     */
    fun sendMessage(toJid: String, body: String, messageId: String): Result<Unit> {
        return try {
            val conn = connection
                ?: return Result.failure(IllegalStateException("No hay conexión XMPP activa"))

            val message = XmppMessageMapper.createXmppMessage(toJid, body, messageId)
            conn.sendStanza(message)
            Log.d(TAG, "Mensaje enviado a $toJid con ID=$messageId")

            Result.success(Unit)
        } catch (e: SmackException.NotConnectedException) {
            Log.e(TAG, "No conectado al enviar mensaje a $toJid", e)
            _connectionState.value = ConnectionState.Error("Error de envio")
            Result.failure(e)
        } catch (e: InterruptedException) {
            Log.e(TAG, "Envío de mensaje interrumpido", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error al enviar mensaje a $toJid: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Envía una notificación de estado de chat a un contacto (XEP-0085).
     */
    fun sendChatState(toJid: String, state: ChatState): Result<Unit> {
        return try {
            val conn = connection
                ?: return Result.failure(IllegalStateException("No hay conexión XMPP activa"))

            val message = Message()
            message.setTo(JidCreate.entityBareFrom(toJid))
            message.addExtension(ChatStateExtension(state))

            conn.sendStanza(message)
            Log.d(TAG, "Estado de chat '$state' enviado a $toJid")

            Result.success(Unit)
        } catch (e: SmackException.NotConnectedException) {
            Log.e(TAG, "No conectado al enviar estado de chat", e)
            _connectionState.value = ConnectionState.Error("Error de envio")
            Result.failure(e)
        } catch (e: InterruptedException) {
            Log.e(TAG, "Envío de estado de chat interrumpido", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error al enviar estado de chat: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Envía una confirmación de lectura para un mensaje recibido (XEP-0184).
     */
    fun sendReadReceipt(messageId: String, toJid: String): Result<Unit> {
        return try {
            val conn = connection
                ?: return Result.failure(IllegalStateException("No hay conexión XMPP activa"))

            val receiptMessage = Message()
            receiptMessage.setTo(JidCreate.entityBareFrom(toJid))
            receiptMessage.addExtension(DeliveryReceipt(messageId))

            conn.sendStanza(receiptMessage)
            Log.d(TAG, "Confirmación de lectura enviada a $toJid para mensaje $messageId")

            Result.success(Unit)
        } catch (e: SmackException.NotConnectedException) {
            Log.e(TAG, "No conectado al enviar confirmación de lectura", e)
            _connectionState.value = ConnectionState.Error("Error de envio")
            Result.failure(e)
        } catch (e: InterruptedException) {
            Log.e(TAG, "Envío de confirmación interrumpido", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error al enviar confirmación: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Envía presencia disponible al servidor.
     */
    fun sendPresence(): Result<Unit> {
        return try {
            val conn = connection
                ?: return Result.failure(IllegalStateException("No hay conexión XMPP activa"))

            val presence = Presence(Presence.Type.available)
            presence.setPriority(PRESENCE_PRIORITY)

            conn.sendStanza(presence)
            Log.d(TAG, "Presencia disponible enviada (prioridad=$PRESENCE_PRIORITY)")

            Result.success(Unit)
        } catch (e: SmackException.NotConnectedException) {
            Log.e(TAG, "No conectado al enviar presencia", e)
            _connectionState.value = ConnectionState.Error("Error de envio")
            Result.failure(e)
        } catch (e: InterruptedException) {
            Log.e(TAG, "Envío de presencia interrumpido", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error al enviar presencia: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Agrega un listener que se invoca cada vez que se recibe un mensaje XMPP.
     */
    fun addMessageListener(listener: (InMessage) -> Unit): Job {
        return scope.launch(Dispatchers.IO) {
            _incomingMessages.collect { message ->
                try {
                    listener(message)
                } catch (e: Exception) {
                    Log.e(TAG, "Error en listener de mensaje: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Agrega un listener para eventos de presencia de contactos.
     */
    fun addPresenceListener(listener: (String, Boolean) -> Unit): Job {
        return scope.launch(Dispatchers.IO) {
            _contactPresenceEvents.collect { (jid, isOnline) ->
                try {
                    listener(jid, isOnline)
                } catch (e: Exception) {
                    Log.e(TAG, "Error en listener de presencia: ${e.message}", e)
                }
            }
        }
    }

    // ==================================================================
    // Métodos privados
    // ==================================================================

    /**
     * Registra los listeners de estanzas para procesar mensajes y presencia.
     */
    private fun registerStanzaListeners(conn: XMPPTCPConnection) {
        // --- Listener para stanzas de tipo Message ---
        conn.addAsyncStanzaListener(
            StanzaListener { stanza ->
                if (stanza !is Message) return@StanzaListener

                val body = stanza.body
                if (body.isNullOrEmpty()) return@StanzaListener

                val from = stanza.from?.toString() ?: return@StanzaListener
                val to = stanza.to?.toString()

                val stanzaId = stanza.stanzaId ?: UUID.randomUUID().toString()

                val delayInfo = DelayInformation.from(stanza)
                val timestamp = delayInfo?.stamp?.time ?: System.currentTimeMillis()

                val isGroupMessage = InMessage.isGroupJid(from)

                val inMessage = InMessage(
                    from = from,
                    to = to,
                    body = body,
                    messageId = stanzaId,
                    timestamp = timestamp,
                    isGroupMessage = isGroupMessage
                )

                val emitted = _incomingMessages.tryEmit(inMessage)
                if (!emitted) {
                    Log.w(TAG, "Buffer de mensajes lleno, mensaje descartado: $stanzaId")
                }

                Log.d(TAG, "Mensaje recibido de $from (ID=$stanzaId, grupo=$isGroupMessage)")
            },
            MessageTypeFilter.NORMAL_OR_CHAT
        )

        // --- Listener para stanzas de tipo Presence ---
        conn.addAsyncStanzaListener(
            StanzaListener { stanza ->
                if (stanza !is Presence) return@StanzaListener

                val from = stanza.from?.toString() ?: return@StanzaListener

                if (from.contains(authenticatedPhoneNumber)) return@StanzaListener

                val isOnline = when (stanza.type) {
                    Presence.Type.available, Presence.Type.subscribe -> true
                    Presence.Type.unavailable, Presence.Type.unsubscribe -> false
                    else -> stanza.type == null || stanza.type == Presence.Type.available
                }

                _contactPresenceEvents.tryEmit(Pair(from, isOnline))
                Log.d(TAG, "Presencia de $from: ${if (isOnline) "en línea" else "desconectado"}")
            },
            PresenceTypeFilter.AVAILABLE
        )

        Log.d(TAG, "Listeners de estanzas registrados correctamente")
    }

    /**
     * Listener del ciclo de vida de la conexión XMPP.
     * Solo implementa los métodos de [ConnectionListener].
     * Los métodos de re-conexión se manejan por separado si se necesita.
     */
    private val connectionListener = object : ConnectionListener {

        override fun connected(connection: XMPPConnection?) {
            Log.d(TAG, "Evento: conexión TCP establecida")
            _connectionState.value = ConnectionState.Connected
        }

        override fun authenticated(connection: XMPPConnection?, resumed: Boolean) {
            Log.d(TAG, "Evento: autenticación exitosa (resumed=$resumed)")
            _connectionState.value = ConnectionState.Connected
        }

        override fun connectionClosed() {
            Log.d(TAG, "Evento: conexión cerrada normalmente")
            _connectionState.value = ConnectionState.Disconnected
        }

        override fun connectionClosedOnError(e: Exception?) {
            Log.e(TAG, "Evento: conexión cerrada por error: ${e?.message}", e)
            _connectionState.value = ConnectionState.Error("Error de conexion")
        }
    }
}
