package com.todus.messenger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.todus.messenger.data.local.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

/**
 * Objeto de acceso a datos (DAO) para la entidad [ChatEntity].
 * Proporciona operaciones CRUD y consultas para la tabla de chats.
 */
@Dao
interface ChatDao {

    /**
     * Inserta o reemplaza un chat en la base de datos.
     *
     * @param chat La entidad del chat a insertar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    /**
     * Obtiene todos los chats ordenados por el momento del último mensaje.
     * Los chats sin mensajes se colocan al final.
     * Emite un [Flow] reactivo que se actualiza con cada cambio.
     *
     * @return Flujo reactivo con la lista completa de chats.
     */
    @Query(
        """SELECT * FROM chats ORDER BY 
           CASE WHEN lastMessageTime IS NULL THEN 0 ELSE lastMessageTime END DESC"""
    )
    fun getAllChats(): Flow<List<ChatEntity>>

    /**
     * Busca un chat específico por su identificador.
     *
     * @param chatId Identificador del chat.
     * @return La entidad del chat, o null si no existe.
     */
    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: String): ChatEntity?

    /**
     * Actualiza los datos del último mensaje de un chat.
     * Se utiliza al recibir o enviar un nuevo mensaje para mantener
     * la vista de la lista de chats actualizada.
     *
     * @param chatId Identificador del chat.
     * @param lastMessage Texto del último mensaje.
     * @param time Marca temporal del último mensaje.
     * @param status Estado del último mensaje (nombre del enum [MessageStatus]), o null.
     */
    @Query(
        """UPDATE chats SET lastMessage = :lastMessage, 
           lastMessageTime = :time, lastMessageStatus = :status 
           WHERE id = :chatId"""
    )
    suspend fun updateLastMessage(
        chatId: String,
        lastMessage: String,
        time: Long,
        status: String?
    )

    /**
     * Marca todos los mensajes de un chat como leídos poniendo
     * el contador de no leídos a cero.
     *
     * @param chatId Identificador del chat.
     */
    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun markAsRead(chatId: String)

    /**
     * Elimina un chat de la base de datos.
     *
     * @param chatId Identificador del chat a eliminar.
     */
    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChat(chatId: String)

    /**
     * Incrementa en uno el contador de mensajes no leídos de un chat.
     *
     * @param chatId Identificador del chat.
     */
    @Query("UPDATE chats SET unreadCount = unreadCount + 1 WHERE id = :chatId")
    suspend fun incrementUnreadCount(chatId: String)
}