package com.todus.messenger.data.repository

import android.util.Log
import com.todus.messenger.data.local.database.AppDatabase
import com.todus.messenger.data.local.entity.toDomain
import com.todus.messenger.data.local.entity.toEntity
import com.todus.messenger.domain.model.Chat
import com.todus.messenger.domain.model.MessageStatus
import com.todus.messenger.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación concreta del repositorio de chats.
 *
 * Gestiona las operaciones CRUD sobre la tabla de chats en Room,
 * mapeando entre entidades locales ([com.todus.messenger.data.local.entity.ChatEntity])
 * y modelos de dominio ([Chat]).
 *
 * Todas las operaciones de base de datos se ejecutan en [Dispatchers.IO].
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val database: AppDatabase
) : ChatRepository {

    companion object {
        private const val TAG = "ChatRepositoryImpl"
    }

    /** DAO para operaciones sobre chats en Room */
    private val chatDao = database.chatDao()

    /** DAO para operaciones sobre mensajes (usado al eliminar un chat) */
    private val messageDao = database.messageDao()

    /**
     * Obtiene todos los chats de forma reactiva, ordenados por el momento
     * del último mensaje (los más recientes primero).
     *
     * @return Flujo reactivo con la lista de chats mapeados al dominio.
     */
    override fun getAllChats(): Flow<List<Chat>> {
        return chatDao.getAllChats().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Obtiene un chat existente por su JID, o lo crea si no existe.
     *
     * Si el chat no se encuentra en la base de datos local, se crea
     * uno nuevo usando [Chat.createFromJid] y se inserta en Room.
     *
     * @param jid JID del contacto (ej: "53XXXXXXXX@todus.cu").
     * @param name Nombre a mostrar del chat.
     * @return El chat existente o recién creado.
     */
    override suspend fun getOrCreateChat(jid: String, name: String): Chat =
        withContext(Dispatchers.IO) {
            // Buscar el chat existente por su JID
            val existingChat = chatDao.getChatById(jid)
            if (existingChat != null) {
                Log.d(TAG, "Chat existente encontrado: $jid")
                return@withContext existingChat.toDomain()
            }

            // No existe: crear un nuevo chat individual desde el JID
            val newChat = Chat.createFromJid(jid = jid, name = name)
            chatDao.insertChat(newChat.toEntity())
            Log.d(TAG, "Nuevo chat creado e insertado: $jid")
            newChat
        }

    /**
     * Actualiza los datos del último mensaje de un chat.
     *
     * @param chatId Identificador del chat.
     * @param lastMessage Texto del último mensaje.
     * @param time Marca temporal del último mensaje (epoch millis).
     * @param status Estado del último mensaje (opcional).
     */
    override suspend fun updateLastMessage(
        chatId: String,
        lastMessage: String,
        time: Long,
        status: MessageStatus?
    ) = withContext(Dispatchers.IO) {
        chatDao.updateLastMessage(
            chatId = chatId,
            lastMessage = lastMessage,
            time = time,
            status = status?.name()
        )
        Log.d(TAG, "Último mensaje actualizado para el chat $chatId")
    }

    /**
     * Marca un chat como leído, reiniciando el contador de mensajes
     * no leídos a cero.
     *
     * @param chatId Identificador del chat a marcar como leído.
     */
    override suspend fun markAsRead(chatId: String) = withContext(Dispatchers.IO) {
        chatDao.markAsRead(chatId)
        Log.d(TAG, "Chat $chatId marcado como leído")
    }

    /**
     * Elimina un chat y todos sus mensajes asociados.
     *
     * Primero elimina los mensajes del chat y luego el chat en sí.
     *
     * @param chatId Identificador del chat a eliminar.
     */
    override suspend fun deleteChat(chatId: String) = withContext(Dispatchers.IO) {
        // Eliminar primero los mensajes asociados al chat
        messageDao.deleteChatMessages(chatId)
        // Luego eliminar el chat
        chatDao.deleteChat(chatId)
        Log.d(TAG, "Chat $chatId y sus mensajes eliminados")
    }
}
