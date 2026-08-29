package com.todus.messenger.domain.repository

import com.todus.messenger.data.remote.xmpp.InMessage
import com.todus.messenger.domain.model.Message
import com.todus.messenger.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Interfaz del repositorio de mensajes.
 *
 * Define las operaciones disponibles para gestionar mensajes:
 * envío, recepción, consulta y actualización de estados.
 * Las implementaciones concretas orquestan la base de datos local (Room)
 * y el servicio remoto (XMPP).
 */
interface MessageRepository {

    /**
     * Envía un mensaje de texto a un destinatario.
     *
     * El flujo interno es:
     * 1. Genera un UUID como identificador del mensaje.
     * 2. Crea el modelo de dominio [Message] y lo inserta en Room con
     *    estado [MessageStatus.SENDING].
     * 3. Envía el mensaje por XMPP.
     * 4. Si el envío es exitoso, actualiza el estado a [MessageStatus.SENT].
     *
     * @param toJid JID del destinatario (ej: "53XXXXXXXX@todus.cu").
     * @param body Contenido textual del mensaje.
     * @return [Result.success] con el [Message] enviado,
     *         o [Result.failure] si ocurrió un error.
     */
    suspend fun sendMessage(toJid: String, body: String): Result<Message>

    /**
     * Obtiene todos los mensajes de un chat de forma reactiva.
     *
     * Emite una lista actualizada automáticamente cada vez que se
     * inserta, modifica o elimina un mensaje del chat indicado.
     *
     * @param chatId Identificador del chat (JID del otro usuario o ID de grupo).
     * @return Flujo reactivo con la lista de mensajes del chat.
     */
    fun getMessages(chatId: String): Flow<List<Message>>

    /**
     * Actualiza el estado de un mensaje específico.
     *
     * Se utiliza para reflejar cambios en el flujo de entrega:
     * SENDING → SENT → DELIVERED → READ, o FAILED en caso de error.
     *
     * @param messageId Identificador único del mensaje.
     * @param status Nuevo estado del mensaje.
     */
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)

    /**
     * Marca todos los mensajes no leídos de un chat como leídos.
     *
     * Actualiza tanto los mensajes en la tabla de mensajes como
     * el contador de no leídos en la tabla de chats.
     *
     * @param chatId Identificador del chat cuyos mensajes se marcarán como leídos.
     */
    suspend fun markMessagesAsRead(chatId: String)

    /**
     * Expone el flujo de mensajes entrantes recibidos por XMPP.
     *
     * Los consumidores (normalmente un caso de uso o ViewModel) pueden
     * coleccionar este flujo para procesar los mensajes a medida que llegan.
     *
     * @return [SharedFlow] que emite cada [InMessage] recibido del servidor.
     */
    fun observeIncomingMessages(): SharedFlow<InMessage>

    /**
     * Procesa un mensaje entrante recibido por XMPP.
     *
     * Realiza las siguientes operaciones:
     * 1. Convierte el [InMessage] a un modelo de dominio [Message] vía
     *    [com.todus.messenger.data.remote.xmpp.XmppMessageMapper.toDomainMessage].
     * 2. Inserta el mensaje en la base de datos local (Room).
     * 3. Actualiza el chat correspondiente con el último mensaje y su marca temporal.
     * 4. Incrementa el contador de mensajes no leídos si el chat no es el activo.
     *
     * @param inMessage Mensaje XMPP entrante a procesar.
     */
    suspend fun processIncomingMessage(inMessage: InMessage)
}
