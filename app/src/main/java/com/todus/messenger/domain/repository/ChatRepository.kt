package com.todus.messenger.domain.repository

import com.todus.messenger.domain.model.Chat
import com.todus.messenger.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow

/**
 * Interfaz del repositorio de chats.
 *
 * Define las operaciones disponibles para gestionar las conversaciones:
 * obtención, creación, actualización y eliminación de chats.
 */
interface ChatRepository {

    /**
     * Obtiene todos los chats de forma reactiva, ordenados por el
     * momento del último mensaje (los más recientes primero).
     *
     * @return Flujo reactivo con la lista completa de chats.
     */
    fun getAllChats(): Flow<List<Chat>>

    /**
     * Obtiene un chat existente por su JID, o lo crea si no existe.
     *
     * Si el chat no se encuentra en la base de datos local, se crea
     * uno nuevo usando [Chat.createIndividualChat] con el JID y nombre
     * proporcionados, y se inserta en Room.
     *
     * @param jid JID del contacto (ej: "53XXXXXXXX@todus.cu").
     * @param name Nombre a mostrar del chat.
     * @return El chat existente o recién creado.
     */
    suspend fun getOrCreateChat(jid: String, name: String): Chat

    /**
     * Actualiza los datos del último mensaje de un chat.
     *
     * Se invoca automáticamente al enviar o recibir un mensaje para
     * mantener actualizada la vista de la lista de chats.
     *
     * @param chatId Identificador del chat.
     * @param lastMessage Texto del último mensaje.
     * @param time Marca temporal del último mensaje (epoch millis).
     * @param status Estado del último mensaje (opcional, relevante para mensajes enviados por mí).
     */
    suspend fun updateLastMessage(
        chatId: String,
        lastMessage: String,
        time: Long,
        status: MessageStatus?
    )

    /**
     * Marca un chat como leído, reiniciando el contador de mensajes no leídos a cero.
     *
     * @param chatId Identificador del chat a marcar como leído.
     */
    suspend fun markAsRead(chatId: String)

    /**
     * Elimina un chat y todos sus mensajes asociados de la base de datos local.
     *
     * @param chatId Identificador del chat a eliminar.
     */
    suspend fun deleteChat(chatId: String)
}
