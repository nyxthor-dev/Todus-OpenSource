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
import com.todus.messenger.data.remote.xmpp.XmppMessageMapper.toDomainMessage
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

    private val messageDao: MessageDao = database.messageDao()
    private val chatDao: ChatDao = database.chatDao()

    @Volatile
    var activeChatId: String? = null

    private val _incomingMessages = MutableSharedFlow<InMessage>(
        replay = 0,
        extraBufferCapacity = 64
    )

    private val listenerJob = xmppClient.addMessageListener { inMessage ->
        _incomingMessages.tryEmit(inMessage)
    }

    override suspend fun sendMessage(toJid: String, body: String): Result<Message> =
        withContext(Dispatchers.IO) {
            try {
                val messageId = UUID.randomUUID().toString()
                val senderJid = "${XmppMessageMapper.currentPhoneNumber}@todus.cu"

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

                messageDao.insertMessage(message.toEntity())
                Log.d(TAG, "Mensaje $messageId insertado en Room con estado SENDING")

                val sendResult = xmppClient.sendMessage(toJid, body, messageId)

                if (sendResult.isSuccess) {
                    messageDao.updateMessageStatus(messageId, MessageStatus.SENT.name)
                    Log.d(TAG, "Mensaje $messageId enviado y actualizado a SENT")
                    Result.success(message.withStatus(MessageStatus.SENT))
                } else {
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

    override fun getMessages(chatId: String): Flow<List<Message>> {
        return messageDao.getMessagesByChatId(chatId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updateMessageStatus(messageId: String, status: MessageStatus) {
        withContext(Dispatchers.IO) {
            messageDao.updateMessageStatus(messageId, status.name)
        }
        Log.d(TAG, "Estado del mensaje $messageId actualizado a $status")
    }

    override suspend fun markMessagesAsRead(chatId: String) {
        withContext(Dispatchers.IO) {
            messageDao.markAllAsRead(chatId)
            chatDao.markAsRead(chatId)
        }
        Log.d(TAG, "Todos los mensajes del chat $chatId marcados como leídos")
    }

    override fun observeIncomingMessages(): SharedFlow<InMessage> = _incomingMessages

    override suspend fun processIncomingMessage(inMessage: InMessage) {
        withContext(Dispatchers.IO) {
            try {
                val chatId = inMessage.from.split("/").firstOrNull() ?: inMessage.from

                val domainMessage = inMessage.toDomainMessage(chatId)

                messageDao.insertMessage(domainMessage.toEntity())
                Log.d(TAG, "Mensaje entrante ${domainMessage.id} insertado en Room (chat: $chatId)")

                chatDao.updateLastMessage(
                    chatId = chatId,
                    lastMessage = domainMessage.body,
                    time = domainMessage.timestamp,
                    status = if (domainMessage.isFromMe) domainMessage.status.name else null
                )

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
}
