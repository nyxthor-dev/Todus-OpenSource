package com.todus.messenger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.todus.messenger.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Objeto de acceso a datos (DAO) para la entidad [MessageEntity].
 * Proporciona todas las operaciones CRUD y consultas especializadas
 * para la tabla de mensajes.
 */
@Dao
interface MessageDao {

    /**
     * Inserta un único mensaje en la base de datos.
     * Si ya existe un mensaje con el mismo ID, se reemplaza.
     *
     * @param message La entidad del mensaje a insertar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    /**
     * Inserta una lista de mensajes en la base de datos de forma transaccional.
     * Si ya existe un mensaje con el mismo ID, se reemplaza.
     *
     * @param messages Lista de entidades de mensajes a insertar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    /**
     * Obtiene todos los mensajes de un chat ordenados por marca temporal descendente.
     * Emite un [Flow] reactivo que se actualiza automáticamente cuando
     * cambian los mensajes del chat.
     *
     * @param chatId Identificador del chat del cual obtener los mensajes.
     * @return Flujo reactivo con la lista de mensajes del chat.
     */
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp DESC")
    fun getMessagesByChatId(chatId: String): Flow<List<MessageEntity>>

    /**
     * Obtiene una página de mensajes de un chat para paginación infinita.
     * Los mensajes se devuelven ordenados por marca temporal descendente.
     *
     * @param chatId Identificador del chat.
     * @param limit Cantidad máxima de mensajes a devolver.
     * @param offset Cantidad de mensajes a saltar (para cargar páginas siguientes).
     * @return Lista de mensajes de la página solicitada.
     */
    @Query(
        "SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp DESC LIMIT :limit OFFSET :offset"
    )
    suspend fun getMessagesPaginated(
        chatId: String,
        limit: Int,
        offset: Int
    ): List<MessageEntity>

    /**
     * Actualiza el estado de un mensaje específico.
     *
     * @param messageId Identificador del mensaje a actualizar.
     * @param status Nuevo estado del mensaje (nombre del enum [MessageStatus]).
     */
    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    /**
     * Edita el contenido textual de un mensaje y lo marca como editado.
     *
     * @param messageId Identificador del mensaje a editar.
     * @param newBody Nuevo contenido textual del mensaje.
     */
    @Query("UPDATE messages SET body = :newBody, edited = 1 WHERE id = :messageId")
    suspend fun editMessage(messageId: String, newBody: String)

    /**
     * Marca un mensaje como eliminado (borrado para todos).
     * El contenido del cuerpo se vacía para no mostrar información sensible.
     *
     * @param messageId Identificador del mensaje a eliminar.
     */
    @Query("UPDATE messages SET deleted = 1, body = '' WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    /**
     * Elimina todos los mensajes pertenecientes a un chat.
     *
     * @param chatId Identificador del chat cuyos mensajes se eliminarán.
     */
    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteChatMessages(chatId: String)

    /**
     * Obtiene la cantidad de mensajes no leídos en un chat.
     * Se consideran no leídos los mensajes que NO son míos y cuyo
     * estado NO es 'READ'.
     *
     * @param chatId Identificador del chat.
     * @return Cantidad de mensajes no leídos.
     */
    @Query(
        """SELECT COUNT(*) FROM messages 
           WHERE chatId = :chatId AND isFromMe = 0 AND status != 'READ'"""
    )
    suspend fun getUnreadCount(chatId: String): Int

    /**
     * Busca un mensaje específico por su identificador.
     *
     * @param id Identificador del mensaje.
     * @return La entidad del mensaje, o null si no existe.
     */
    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: String): MessageEntity?

    /**
     * Marca todos los mensajes no leídos de un chat como leídos.
     * Solo afecta a mensajes que NO son míos y cuyo estado NO es 'READ'.
     *
     * @param chatId Identificador del chat cuyos mensajes se marcarán como leídos.
     */
    @Query(
        """UPDATE messages SET status = 'READ' 
           WHERE chatId = :chatId AND isFromMe = 0 AND status != 'READ'"""
    )
    suspend fun markAllAsRead(chatId: String)
}