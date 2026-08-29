package com.todus.messenger.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.todus.messenger.domain.model.Chat
import com.todus.messenger.domain.model.MessageStatus

/**
 * Entidad de Room que representa un chat almacenado localmente.
 * Mapeable a/desde el modelo de dominio [Chat].
 */
@Entity(tableName = "chats")
data class ChatEntity(
    /** Identificador único del chat (JID del contacto o ID del grupo) */
    @PrimaryKey
    val id: String,

    /** Nombre del chat o del contacto */
    val name: String,

    /** URL del avatar del chat o del contacto */
    val avatarUrl: String? = null,

    /** Texto del último mensaje recibido en el chat */
    val lastMessage: String? = null,

    /** Marca temporal del último mensaje en milisegundos */
    val lastMessageTime: Long? = null,

    /** Cantidad de mensajes no leídos en el chat */
    val unreadCount: Int = 0,

    /** Indica si el chat es un grupo */
    val isGroup: Boolean = false,

    /** Indica si el contacto está en línea */
    val isOnline: Boolean = false,

    /** Estado del último mensaje, almacenado como nombre del enum [MessageStatus] */
    val lastMessageStatus: String? = null
)

/**
 * Convierte esta entidad de Room al modelo de dominio [Chat].
 */
fun ChatEntity.toDomain(): Chat = Chat(
    id = id,
    name = name,
    avatarUrl = avatarUrl,
    lastMessage = lastMessage,
    lastMessageTime = lastMessageTime,
    unreadCount = unreadCount,
    isGroup = isGroup,
    isOnline = isOnline,
    lastMessageStatus = lastMessageStatus?.let { MessageStatus.valueOf(it) }
)

/**
 * Convierte el modelo de dominio [Chat] a una entidad de Room [ChatEntity].
 */
fun Chat.toEntity(): ChatEntity = ChatEntity(
    id = id,
    name = name,
    avatarUrl = avatarUrl,
    lastMessage = lastMessage,
    lastMessageTime = lastMessageTime,
    unreadCount = unreadCount,
    isGroup = isGroup,
    isOnline = isOnline,
    lastMessageStatus = lastMessageStatus?.name()
)
