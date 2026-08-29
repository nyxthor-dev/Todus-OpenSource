package com.todus.messenger.data.repository

import android.content.Context
import android.util.Log
import com.todus.messenger.data.local.database.AppDatabase
import com.todus.messenger.data.local.dao.ChatDao
import com.todus.messenger.data.local.dao.MessageDao
import com.todus.messenger.data.local.entity.toDomain
import com.todus.messenger.data.local.entity.toEntity
import com.todus.messenger.data.remote.xmpp.InMessage
import com.todus.messenger.data.remote.xmpp.ToDusXmppClient
import com.todus.messenger.data.remote.xmpp.XmppMessageMapper
import com.todus.messenger.domain.model.Message
import com.todus.messenger.domain.model.MessageStatus
import com.todus.messenger.domain.model.MessageType
import com.todus.messenger.domain.repository.MessageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación concreta del repositorio de mensajes.
 *
 * Orquesta las operaciones entre la base de datos local (Room) y el
 * servicio de mensajería remoto (XMPP) para enviar, recibir y gestionar
 * el estado de los mensajes.
 *
 * Todas las operaciones de base de datos y XMPP se ejecutan en
 * [Dispatchers.IO] para no bloquear el hilo principal.
 *
 * @param context Contexto de la aplicación Android.
 * @param database Base de datos Room de la aplicación.
 * @param xmppClient Cliente XMPP para comunicación con el servidor ToDus.
 */
@Singleton
class MessageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val xmppClient: ToDusXmppClient
) : MessageRepository {

    companion object {
        private const val TAG = "MessageRepositoryImpl"
    }

    /** DAO para operaciones sobre mensajes en Room */
    private val messageDao: MessageDao = database.messageDao()

    /** DAO para operaciones sobre chats en Room */
    private val chatDao: ChatDao = database.chatDao()

    /**
     * JID del chat actualmente abierto por el usuario.
     * Se usa para decidir si se incrementa el contador de no leídos
     * al recibir un mensaje entrante.
     */
    @Volatile
    var activeChatId: String? = null

    /**
     * Flujo puente que re-emite los mensajes entrantes del cliente XMPP.
     * Se crea a partir del listener del XMPP client, ya que el SharedFlow
     * interno de [ToDusXmppClient] es privado.
     */
    private val _incomingMessages = MutableSharedFlow<InMessage>(
        replay = 0,
        extraBufferCapacity = 64
    )

    /** Listener Job para la suscripción al XMPP client */
    private val listenerJob = xmppClient.addMessageListener { inMessage ->
        _incomingMessages.tryEmit(inMessage)
    }

    /**
     * Envía un mensaje de texto a un destinatario.
     *
     * Flujo de operación:
     * 1. Genera un UUID como identificador del mensaje.
     * 2. Crea un [Message] con los datos proporcionados y estado SENDING.
     * 3. Inserta la entidad en Room mediante [MessageDao.insertMessage].
     * 4. Envía el mensaje por XMPP mediante [ToDusXmppClient.sendMessage].
     * 5. Si el envío es exitoso, actualiza el estado a SENT en Room.
     * 6. Retorna el mensaje creado.
     *
     * @param toJid JID del destinatario (ej: "53XXXXXXXX@todus.cu").
     * @param body Contenido textual del mensaje.
     * @return [Result.success] con el [Message] enviado,
     *         o [Result.failure] si el envío por XMPP falló.
     */
    override suspend fun sendMessage(toJid: String, body: String): Result<Message> =
        withContext(Dispatchers.IO) {
            try {
                // Paso 1: Generar un UUID único como identificador del mensaje
                val messageId = UUID.randomUUID().toString()

                // Construir el JID del remitente con el número autenticado
                val senderJid = "${XmppMessageMapper.currentPhoneNumber}@todus.cu"

                // Paso 2: Crear el modelo de dominio con estado SENDING
                val message = Message(
                    id = messageId,
                    chatId = toJid,
                    senderJid = senderJid,
                    receiverJid = toJid,
                    body = body,
                    timestamp = System.currentTimeMillis(),
                    type = MessageType.TEXT,
                    status = MessageStatus.SENDING,
                    isFromMe = true
                )

                // Paso 3: Insertar en Room con estado SENDING
                messageDao.insertMessage(message.toEntity())
                Log.d(TAG, "Mensaje $messageId insertado en Room con estado SENDING")

                // Paso 4: Enviar por XMPP
                val sendResult = xmppClient.sendMessage(toJid, body, messageId)

                if (sendResult.isSuccess) {
                    // Paso 5: Actualizar estado a SENT si el envío fue exitoso
                    messageDao.updateMessageStatus(messageId, MessageStatus.SENT.name)
                    Log.d(TAG, "Mensaje $messageId enviado y actualizado a SENT")
                    Result.success(message.withStatus(MessageStatus.SENT))
                } else {
                    // El envío falló: actualizar estado a FAILED
                    messageDao.updateMessageStatus(messageId, MessageStatus.FAILED.name)
                    Log.e(
                        TAG,
                        "Error al enviar mensaje $messageId: ${sendResult.exceptionOrNull()?.message}"
                    )
                    Result.failure(sendResult.exceptionOrNull() ?: Exception("Error desconocido al enviar"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado al enviar mensaje: ${e.message}", e)
                Result.failure(e)
            }
        }

    /**
     * Obtiene todos los mensajes de un chat de forma reactiva.
     *
     * Los mensajes se mapean de [com.todus.messenger.data.local.entity.MessageEntity]
     * a [Message] mediante la función de extensión [toDomain].
     *
     * @param chatId Identificador del chat.
     * @return Flujo reactivo con la lista de mensajes del chat.
     */
    override fun getMessages(chatId: String): Flow<List<Message>> {
        return messageDao.getMessagesByChatId(chatId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Actualiza el estado de un mensaje específico en la base de datos.
     *
     * @param messageId Identificador único del mensaje.
     * @param status Nuevo estado del mensaje.
     */
    override suspend fun updateMessageStatus(messageId: String, status: MessageStatus) =
        withContext(Dispatchers.IO) {
            messageDao.updateMessageStatus(messageId, status.name)
            Log.d(TAG, "Estado del mensaje $messageId actualizado a $status")
        }

    /**
     * Marca todos los mensajes no leídos de un chat como leídos.
     *
     * Actualiza tanto la tabla de mensajes (via [MessageDao.markAllAsRead])
     * como el contador de no leídos del chat (via [ChatDao.markAsRead]).
     *
     * @param chatId Identificador del chat cuyos mensajes se marcarán como leídos.
     */
    override suspend fun markMessagesAsRead(chatId: String) =
        withContext(Dispatchers.IO) {
            messageDao.markAllAsRead(chatId)
            chatDao.markAsRead(chatId)
            Log.d(TAG, "Todos los mensajes del chat $chatId marcados como leídos")
        }

    /**
     * Expone el flujo de mensajes entrantes del cliente XMPP.
     *
     * Retorna un [SharedFlow] que emite cada [InMessage] recibido
     * del servidor ToDus. Los consumidores pueden coleccionar este
     * flujo para procesar los mensajes a medida que llegan.
     *
     * @return Flujo compartido de mensajes entrantes.
     */
    override fun observeIncomingMessages(): SharedFlow<InMessage> = _incomingMessages

    /**
     * Procesa un mensaje entrante recibido por XMPP.
     *
     * Realiza las siguientes operaciones:
     * 1. Extrae el chatId del remitente (JID bare, sin recurso).
     * 2. Convierte el [InMessage] a [Message] usando
     *    [XmppMessageMapper.toDomainMessage].
     * 3. Inserta el mensaje en Room.
     * 4. Actualiza el chat correspondiente con el último mensaje,
     *    la marca temporal y, si aplica, el estado.
     * 5. Incrementa el contador de no leídos si el chat no es el activo
     *    y el mensaje no es propio.
     *
     * @param inMessage Mensaje XMPP entrante a procesar.
     */
    override suspend fun processIncomingMessage(inMessage: InMessage) =
        withContext(Dispatchers.IO) {
            try {
                // Paso 1: Extraer el chatId del remitente (JID bare, sin recurso)
                // Formato esperado: "53XXXXXXXX@todus.cu/Recurso" -> "53XXXXXXXX@todus.cu"
                val chatId = inMessage.from.split("/").firstOrNull() ?: inMessage.from

                // Paso 2: Convertir el InMessage a modelo de dominio usando el mapper
                val domainMessage = inMessage.toDomainMessage(chatId)

                // Paso 3: Insertar el mensaje en Room
                messageDao.insertMessage(domainMessage.toEntity())
                Log.d(TAG, "Mensaje entrante ${domainMessage.id} insertado en Room (chat: $chatId)")

                // Paso 4: Actualizar el chat correspondiente con el último mensaje
                chatDao.updateLastMessage(
                    chatId = chatId,
                    lastMessage = domainMessage.body,
                    time = domainMessage.timestamp,
                    status = if (domainMessage.isFromMe) domainMessage.status.name else null
                )

                // Paso 5: Incrementar unreadCount si el chat no es el activo
                // y el mensaje no es propio
                if (!domainMessage.isFromMe && chatId != activeChatId) {
                    chatDao.incrementUnreadCount(chatId)
                    Log.d(TAG, "UnreadCount incrementado para el chat $chatId")
                }

                Log.d(TAG, "Mensaje entrante procesado correctamente: ${domainMessage.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error al procesar mensaje entrante: ${e.message}", e)
            }
        }
}
