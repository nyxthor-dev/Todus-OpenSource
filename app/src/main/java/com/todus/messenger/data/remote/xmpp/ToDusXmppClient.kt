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
import org.jivesoftware.smack.AbstractXMPPConnection
import org.jivesoftware.smack.ConnectionListener
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.filter.MessageTypeFilter
import org.jivesoftware.smack.filter.PresenceTypeFilter
import org.jivesoftware.smack.filter.StanzaTypeFilter
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.sasl.SASLAuthentication
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jivesoftware.smackx.chatstates.ChatState
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension
import org.jivesoftware.smackx.delay.packet.DelayInformation
import org.jivesoftware.smackx.receipts.Receipt
import org.jxmpp.jid.EntityBareJid
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
 * - **Puerto alternativo:** 5443
 * - **Formato JID:** {phoneNumber}@todus.cu
 * - **Autenticación:** SASL PLAIN (usuario = número de teléfono)
 * - **TLS:** Deshabilitado (seguridad a nivel de aplicación)
 *
 * ## Dependencias Smack requeridas en build.gradle:
 * ```groovy
 * implementation 'org.igniterealtime.smack:smack-core:4.4.6'
 * implementation 'org.igniterealtime.smack:smack-tcp:4.4.6'
 * implementation 'org.igniterealtime.smack:smack-extensions:4.4.6'
 * implementation 'org.igniterealtime.smack:smack-sasl-provided:4.4.6'
 * ```
 *
 * @param context Contexto de la aplicación Android (reservado para uso futuro
 *               con servicios del sistema si es necesario)
 * @param host Dirección del servidor XMPP (por defecto "todus.cu")
 * @param port Puerto de conexión TCP (por defecto 1756, producción;
 *               usar 5443 como alternativa)
 *
 * @property connectionState Flujo reactivo que emite el estado actual de la
 *           conexión XMPP ([ConnectionState])
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

    // --- Flujo reactivo del estado de conexión ---
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)

    /**
     * Flujo de solo lectura que emite el estado actual de la conexión XMPP.
     * Valores posibles: [ConnectionState.DISCONNECTED], [ConnectionState.CONNECTING],
     * [ConnectionState.CONNECTED], [ConnectionState.AUTHENTICATED],
     * [ConnectionState.CLOSED], [ConnectionState.CONNECTION_ERROR]
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
     * El flujo de conexión es:
     * 1. Emitir estado CONNECTING
     * 2. Configurar mecanismo SASL PLAIN (deshabilitar otros)
     * 3. Construir [XMPPTCPConnectionConfiguration] sin TLS
     * 4. Crear y conectar [XMPPTCPConnection]
     * 5. Emitir estado CONNECTED
     * 6. Autenticar con login(número, contraseña) vía SASL PLAIN
     * 7. Emitir estado AUTHENTICATED
     * 8. Registrar listeners de estanzas (mensajes y presencia)
     * 9. Enviar presencia disponible
     *
     * Este método es bloqueante y debe invocarse desde una corrutina
     * con [Dispatchers.IO] o un hilo de fondo.
     *
     * @param phoneNumber Número de teléfono del usuario (sin @todus.cu)
     * @param password Contraseña de la cuenta ToDus
     * @return [Result.success] si la conexión y autenticación fueron exitosas,
     *         [Result.failure] con la excepción correspondiente en caso de error
     */
    fun connect(phoneNumber: String, password: String): Result<Unit> {
        return try {
            Log.d(TAG, "Iniciando conexión XMPP a $host:$port")
            _connectionState.value = ConnectionState.CONNECTING

            // --- Paso 1: Configurar mecanismo SASL PLAIN ---
            // ToDus no usa TLS, por lo que necesitamos forzar SASL PLAIN.
            // Poner en lista negra los mecanismos más fuertes para que
            // Smack negocie únicamente PLAIN.
            configureSaslPlain()

            // --- Paso 2: Construir configuración de conexión TCP ---
            val config = XMPPTCPConnectionConfiguration.builder()
                .setHost(host)
                .setPort(port)
                // ToDus NO utiliza TLS; la seguridad se maneja a nivel de aplicación
                .setSecurityMode(
                    XMPPTCPConnectionConfiguration.SecurityMode.disabled
                )
                .setXmppDomain(JidCreate.domainBareFrom(XMPP_DOMAIN))
                .setResource(RESOURCE)
                .setConnectTimeout(CONNECT_TIMEOUT_MS)
                .setReplyTimeout(REPLY_TIMEOUT_MS)
                .build()

            // --- Paso 3: Crear la conexión XMPP ---
            connection = XMPPTCPConnection(config)
            val conn = connection!!

            // --- Paso 4: Registrar listener de ciclo de vida de la conexión ---
            conn.addConnectionListener(connectionListener)

            // --- Paso 5: Conectar al servidor (bloqueante) ---
            conn.connect()
            Log.d(TAG, "Conexión TCP establecida con $host:$port")
            _connectionState.value = ConnectionState.CONNECTED

            // --- Paso 6: Autenticar con SASL PLAIN ---
            // En Smack, login(username, password) realiza la negociación SASL.
            // El username es solo la parte local del JID (número de teléfono).
            conn.login(phoneNumber, password)
            Log.d(TAG, "Autenticación SASL PLAIN exitosa para $phoneNumber")
            _connectionState.value = ConnectionState.AUTHENTICATED

            // --- Paso 7: Guardar credenciales para uso interno ---
            authenticatedPhoneNumber = phoneNumber
            XmppMessageMapper.currentPhoneNumber = phoneNumber

            // --- Paso 8: Registrar listeners de estanzas entrantes ---
            registerStanzaListeners(conn)

            // --- Paso 9: Enviar presencia disponible ---
            sendPresence()

            Log.d(TAG, "Cliente XMPP completamente inicializado")
            Result.success(Unit)
        } catch (e: XMPPException.XMPPErrorException) {
            Log.e(TAG, "Error XMPP al conectar: ${e.xmppError?.condition}", e)
            _connectionState.value = ConnectionState.CONNECTION_ERROR
            Result.failure(e)
        } catch (e: SmackException) {
            Log.e(TAG, "Error de Smack al conectar: ${e.message}", e)
            _connectionState.value = ConnectionState.CONNECTION_ERROR
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Error de E/S al conectar: ${e.message}", e)
            _connectionState.value = ConnectionState.CONNECTION_ERROR
            Result.failure(e)
        } catch (e: InterruptedException) {
            Log.e(TAG, "Conexión interrumpida", e)
            _connectionState.value = ConnectionState.DISCONNECTED
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al conectar: ${e.message}", e)
            _connectionState.value = ConnectionState.CONNECTION_ERROR
            Result.failure(e)
        }
    }

    /**
     * Desconecta del servidor XMPP de forma ordenada.
     *
     * 1. Envía una presencia de tipo "unavailable" para notificar a los contactos
     * 2. Cierra la conexión XMPP
     * 3. Emite estado [ConnectionState.DISCONNECTED]
     *
     * Este método es seguro de invocar incluso si no hay conexión activa.
     * Los errores se capturan silenciosamente para evitar excepciones.
     */
    fun disconnect() {
        val conn = connection ?: run {
            Log.w(TAG, "disconnect() llamado sin conexión activa")
            _connectionState.value = ConnectionState.DISCONNECTED
            return
        }

        try {
            // Enviar presencia unavailable para que los contactos nos vean desconectados
            val unavailablePresence = Presence(Presence.Type.unavailable)
            conn.sendStanza(unavailablePresence)
            Log.d(TAG, "Presencia unavailable enviada")
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo enviar presencia unavailable: ${e.message}")
        }

        try {
            // Cerrar la conexión Smack (envía </stream:stream> y cierra el socket)
            conn.disconnect()
            Log.d(TAG, "Conexión XMPP cerrada")
        } catch (e: Exception) {
            Log.w(TAG, "Error al cerrar la conexión: ${e.message}")
        }

        // Limpiar estado interno
        authenticatedPhoneNumber = ""
        connection = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    /**
     * Envía un mensaje de texto a un destinatario.
     *
     * Crea un stanza <message> de XMPP con:
     * - Cuerpo del mensaje (texto plano)
     * - JID del destinatario
     * - Identificador único (stanzaId) para seguimiento
     *
     * @param toJid JID completo del destinatario (ej: "53555555@todus.cu")
     * @param body Cuerpo del mensaje de texto
     * @param messageId Identificador único para el mensaje (se usa como stanzaId
     *                 y para correlacionar confirmaciones de entrega XEP-0184)
     * @return [Result.success] si el mensaje se envió correctamente,
     *         [Result.failure] con la excepción en caso de error
     */
    fun sendMessage(toJid: String, body: String, messageId: String): Result<Unit> {
        return try {
            val conn = connection
                ?: return Result.failure(IllegalStateException("No hay conexión XMPP activa"))

            // Crear el stanza de mensaje usando el mapper
            val message = XmppMessageMapper.createXmppMessage(toJid, body, messageId)

            // Enviar el stanza a través de la conexión
            conn.sendStanza(message)
            Log.d(TAG, "Mensaje enviado a $toJid con ID=$messageId")

            Result.success(Unit)
        } catch (e: SmackException.NotConnectedException) {
            Log.e(TAG, "No conectado al enviar mensaje a $toJid", e)
            _connectionState.value = ConnectionState.CONNECTION_ERROR
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
     *
     * Permite informar al otro usuario sobre la actividad actual en la
     * conversación: escribiendo, activo, pausado, etc.
     *
     * @param toJid JID del contacto al que se le notifica el estado
     * @param state Estado de chat a enviar:
     *   - [ChatState.composing]: El usuario está escribiendo un mensaje
     *   - [ChatState.active]: El usuario está participando activamente
     *   - [ChatState.paused]: El usuario ha dejado de escribir temporalmente
     *   - [ChatState.gone]: El usuario ha cerrado la conversación
     * @return [Result.success] si la notificación se envió correctamente,
     *         [Result.failure] en caso de error
     */
    fun sendChatState(toJid: String, state: ChatState): Result<Unit> {
        return try {
            val conn = connection
                ?: return Result.failure(IllegalStateException("No hay conexión XMPP activa"))

            // Crear mensaje vacío con la extensión de estado de chat
            val message = Message()
            message.setTo(JidCreate.entityBareFrom(toJid))
            message.addExtension(ChatStateExtension(state))

            conn.sendStanza(message)
            Log.d(TAG, "Estado de chat '$state' enviado a $toJid")

            Result.success(Unit)
        } catch (e: SmackException.NotConnectedException) {
            Log.e(TAG, "No conectado al enviar estado de chat", e)
            _connectionState.value = ConnectionState.CONNECTION_ERROR
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
     *
     * Cuando el servidor o el cliente solicita una confirmación de entrega,
     * este método envía un <received> con el ID del mensaje original.
     *
     * Formato del stanza enviado:
     * ```xml
     * <message to='remitente@todus.cu'>
     *   <received xmlns='urn:xmpp:receipts' id='id-mensaje-original'/>
     * </message>
     * ```
     *
     * @param messageId ID del mensaje original que se está confirmando
     * @param toJid JID del remitente del mensaje original
     * @return [Result.success] si la confirmación se envió correctamente,
     *         [Result.failure] en caso de error
     */
    fun sendReadReceipt(messageId: String, toJid: String): Result<Unit> {
        return try {
            val conn = connection
                ?: return Result.failure(IllegalStateException("No hay conexión XMPP activa"))

            // Crear mensaje con extensión <received> de XEP-0184
            val receiptMessage = Message()
            receiptMessage.setTo(JidCreate.entityBareFrom(toJid))
            // Receipt(id) crea <received xmlns='urn:xmpp:receipts' id='messageId'/>
            receiptMessage.addExtension(Receipt(messageId))

            conn.sendStanza(receiptMessage)
            Log.d(TAG, "Confirmación de lectura enviada a $toJid para mensaje $messageId")

            Result.success(Unit)
        } catch (e: SmackException.NotConnectedException) {
            Log.e(TAG, "No conectado al enviar confirmación de lectura", e)
            _connectionState.value = ConnectionState.CONNECTION_ERROR
            Result.failure(e)
        } catch (e: InterruptedException) {
            Log.e(TAG, "Envío de confirmación interrumpido", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error al enviar confirmación de lectura: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Envía presencia disponible al servidor.
     *
     * Notifica al servidor y a los contactos que el usuario está en línea
     * con prioridad 1 (prioridad estándar para clientes móviles).
     *
     * @return [Result.success] si la presencia se envió correctamente,
     *         [Result.failure] en caso de error
     */
    fun sendPresence(): Result<Unit> {
        return try {
            val conn = connection
                ?: return Result.failure(IllegalStateException("No hay conexión XMPP activa"))

            // Crear stanza de presencia disponible con prioridad
            val presence = Presence(Presence.Type.available)
            presence.setPriority(PRESENCE_PRIORITY)

            conn.sendStanza(presence)
            Log.d(TAG, "Presencia disponible enviada (prioridad=$PRESENCE_PRIORITY)")

            Result.success(Unit)
        } catch (e: SmackException.NotConnectedException) {
            Log.e(TAG, "No conectado al enviar presencia", e)
            _connectionState.value = ConnectionState.CONNECTION_ERROR
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
     *
     * El listener se ejecuta en el contexto de corrutinas [Dispatchers.IO]
     * y recibe cada [InMessage] a medida que llega del servidor.
     *
     * El [Job] retornado puede cancelarse para detener la escucha:
     * ```kotlin
     * val listenerJob = xmppClient.addMessageListener { message ->
     *     println("Nuevo mensaje de ${message.from}: ${message.body}")
     * }
     * // Para detener:
     * listenerJob.cancel()
     * ```
     *
     * @param listener Función que recibe cada mensaje entrante.
     *                 Se ejecuta en un hilo de fondo (Dispatchers.IO)
     * @return [Job] que puede cancelarse para remover el listener
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
     *
     * Recibe pares (jid, isOnline) donde:
     * - jid: JID completo del contacto
     * - isOnline: true si el contacto está disponible, false si no
     *
     * @param listener Función callback que recibe los eventos de presencia
     * @return [Job] que puede cancelarse para detener la escucha
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
    // Métodos privados de configuración interna
    // ==================================================================

    /**
     * Configura Smack para usar exclusivamente SASL PLAIN.
     *
     * ToDus no soporta TLS, por lo que los mecanismos SASL que requieren
     * seguridad de capa de transporte (SCRAM-SHA-1, SCRAM-SHA-256, DIGEST-MD5)
     * deben ponerse en lista negra, dejando solo PLAIN disponible.
     */
    private fun configureSaslPlain() {
        try {
            // Poner en lista negra los mecanismos que requieren canal seguro
            SASLAuthentication.blacklistSASLMechanism("SCRAM-SHA-1")
            SASLAuthentication.blacklistSASLMechanism("SCRAM-SHA-256")
            SASLAuthentication.blacklistSASLMechanism("SCRAM-SHA-512")
            SASLAuthentication.blacklistSASLMechanism("DIGEST-MD5")
            SASLAuthentication.blacklistSASLMechanism("X-OAUTH2")
            // Asegurar que PLAIN no está en la lista negra
            SASLAuthentication.unblacklistSASLMechanism("PLAIN")
            Log.d(TAG, "SASL configurado para usar exclusivamente PLAIN")
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo configurar SASL PLAIN explícitamente: ${e.message}")
        }
    }

    /**
     * Registra los listeners de estanzas para procesar mensajes y presencia.
     *
     * Se registran dos listeners asíncronos:
     * 1. **Listener de mensajes**: Filtra stanzas de tipo [Message], extrae
     *    el cuerpo, remitente, stanzaId y marca temporal (XEP-0203), y emite
     *    a [_incomingMessages]. Solo emite mensajes que tienen cuerpo (filtra
     *    notificaciones de estado de chat y confirmaciones).
     * 2. **Listener de presencia**: Filtra stanzas de tipo [Presence] y emite
     *    eventos a [_contactPresenceEvents] indicando si el contacto está
     *    disponible o no.
     *
     * @param conn Conexión XMPP ya autenticada
     */
    private fun registerStanzaListeners(conn: XMPPTCPConnection) {
        // --- Listener para stanzas de tipo Message ---
        conn.addAsyncStanzaListener(
            StanzaListener { stanza ->
                if (stanza !is Message) return@StanzaListener

                val body = stanza.body
                // Filtrar mensajes vacíos (notificaciones de estado de chat,
                // confirmaciones de entrega, etc.)
                if (body.isNullOrEmpty()) return@StanzaListener

                // Extraer JID del remitente (completo, con recurso)
                val from = stanza.from?.toString() ?: return@StanzaListener
                // Extraer JID del destinatario
                val to = stanza.to?.toString()

                // Obtener el stanzaId (ID único del mensaje en XMPP).
                // Si no viene en el stanza, generar uno para identificarlo localmente.
                val stanzaId = stanza.stanzaId ?: UUID.randomUUID().toString()

                // Intentar obtener la marca temporal del mensaje usando XEP-0203.
                // DelayInformation puede estar presente en mensajes offline
                // (almacenados en el servidor mientras el usuario estaba desconectado).
                val delayInfo = DelayInformation.from(stanza)
                val timestamp = delayInfo?.stamp?.time ?: System.currentTimeMillis()

                // Detectar si es mensaje de grupo (MUC) verificando el dominio del JID
                val isGroupMessage = InMessage.isGroupJid(from)

                // Construir el modelo de datos y emitir al flujo compartido
                val inMessage = InMessage(
                    from = from,
                    to = to,
                    body = body,
                    messageId = stanzaId,
                    timestamp = timestamp,
                    isGroupMessage = isGroupMessage
                )

                // tryEmit es no-bloqueante; si el buffer está lleno, descarta
                // el mensaje (comportamiento aceptable para no bloquear el hilo de Smack)
                val emitted = _incomingMessages.tryEmit(inMessage)
                if (!emitted) {
                    Log.w(TAG, "Buffer de mensajes lleno, mensaje descartado: $stanzaId")
                }

                Log.d(TAG, "Mensaje recibido de $from (ID=$stanzaId, grupo=$isGroupMessage)")
            },
            // Filtrar solo stanzas de tipo Message (incluye chat, groupchat, error, etc.)
            MessageTypeFilter.NORMAL_OR_CHAT
        )

        // --- Listener para stanzas de tipo Presence ---
        // Detecta cuando los contactos se conectan (available) o desconectan (unavailable)
        conn.addAsyncStanzaListener(
            StanzaListener { stanza ->
                if (stanza !is Presence) return@StanzaListener

                val from = stanza.from?.toString() ?: return@StanzaListener

                // Ignorar presencia del propio usuario
                if (from.contains(authenticatedPhoneNumber)) return@StanzaListener

                // Determinar el estado del contacto basado en el tipo de presencia
                // Presence.Type.available y null = el contacto está en línea
                // Presence.Type.unavailable = el contacto se desconectó
                val isOnline = when (stanza.type) {
                    Presence.Type.available, Presence.Type.subscribe -> true
                    Presence.Type.unavailable, Presence.Type.unsubscribe -> false
                    else -> stanza.type == null || stanza.type == Presence.Type.available
                }

                _contactPresenceEvents.tryEmit(Pair(from, isOnline))
                Log.d(TAG, "Presencia de $from: ${if (isOnline) "en línea" else "desconectado"}")
            },
            // Filtrar stanzas de tipo Presence
            PresenceTypeFilter.AVAILABLE
        )

        Log.d(TAG, "Listeners de estanzas registrados correctamente")
    }

    /**
     * Listener del ciclo de vida de la conexión XMPP.
     *
     * Actualiza [_connectionState] en respuesta a eventos de Smack:
     * - [connected]: La conexión TCP fue establecida
     * - [authenticated]: La autenticación SASL fue exitosa
     * - [connectionClosed]: La conexión se cerró normalmente
     * - [connectionClosedOnError]: La conexión se cerró por un error
     * - [reconnectingIn] / [reconnectionSuccessful] / [reconnectionFailed]:
     *   Smack intenta reconectarse automáticamente
     */
    private val connectionListener = object : ConnectionListener {

        override fun connected(connection: XMPPConnection?) {
            Log.d(TAG, "Evento: conexión TCP establecida")
            _connectionState.value = ConnectionState.CONNECTED
        }

        override fun authenticated(connection: XMPPConnection?, resumed: Boolean) {
            Log.d(TAG, "Evento: autenticación exitosa (resumed=$resumed)")
            _connectionState.value = ConnectionState.AUTHENTICATED
        }

        override fun connectionClosed() {
            Log.d(TAG, "Evento: conexión cerrada normalmente")
            _connectionState.value = ConnectionState.CLOSED
        }

        override fun connectionClosedOnError(e: Exception?) {
            Log.e(TAG, "Evento: conexión cerrada por error: ${e?.message}", e)
            _connectionState.value = ConnectionState.CONNECTION_ERROR
        }

        override fun reconnectingIn(seconds: Int) {
            Log.d(TAG, "Evento: reconectando en $seconds segundos...")
        }

        override fun reconnectionSuccessful() {
            Log.d(TAG, "Evento: reconexión exitosa")
            _connectionState.value = ConnectionState.CONNECTED
        }

        override fun reconnectionFailed(e: Exception?) {
            Log.e(TAG, "Evento: reconexión fallida: ${e?.message}", e)
            _connectionState.value = ConnectionState.CONNECTION_ERROR
        }
    }
}
