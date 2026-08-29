package com.todus.messenger.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.todus.messenger.domain.model.Message
import com.todus.messenger.domain.model.MessageStatus
import com.todus.messenger.domain.model.MessageType

/**
 * Entidad de Room que representa un mensaje almacenado localmente.
 * Mapeable a/desde el modelo de dominio [Message].
 */
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["chatId"]),
        Index(value = ["senderJid"]),
        Index(value = ["receiverJid"]),
        Index(value = ["timestamp"]),
        // Índice compuesto para consultas rápidas de mensajes por chat ordenados por tiempo
        Index(value = ["chatId", "timestamp"])
    ]
)
data class MessageEntity(
    /** Identificador único del mensaje */
    @PrimaryKey
    val id: String,

    /** Identificador del chat al que pertenece el mensaje */
    val chatId: String,

    /** JID del remitente */
    val senderJid: String,

    /** JID del destinatario */
    val receiverJid: String,

    /** Contenido textual del mensaje */
    val body: String,

    /** Marca temporal del mensaje en milisegundos */
    val timestamp: Long,

    /** Tipo de mensaje mapeado como nombre del enum [MessageType] */
    val type: String,

    /** Estado del mensaje mapeado como nombre del enum [MessageStatus] */
    val status: String,

    /** Indica si el mensaje fue enviado por el usuario actual */
    val isFromMe: Boolean,

    /** ID del mensaje al que se responde, o null si no es respuesta */
    val replyTo: String? = null,

    /** Indica si el mensaje fue editado */
    val edited: Boolean = false,

    /** Indica si el mensaje fue eliminado (borrado para todos) */
    val deleted: Boolean = false,

    /** URL del archivo multimedia asociado al mensaje */
    val mediaUrl: String? = null,

    /** URL de la miniatura del archivo multimedia */
    val mediaThumbnail: String? = null,

    /** Duración del archivo multimedia en segundos (para audio y video) */
    val duration: Int? = null,

    /** Latitud para mensajes de tipo ubicación */
    val latitude: Double? = null,

    /** Longitud para mensajes de tipo ubicación */
    val longitude: Double? = null
)

/**
 * Convierte esta entidad de Room al modelo de dominio [Message].
 */
fun MessageEntity.toDomain(): Message = Message(
    id = id,
    chatId = chatId,
    senderJid = senderJid,
    receiverJid = receiverJid,
    body = body,
    timestamp = timestamp,
    type = MessageType.valueOf(type),
    status = MessageStatus.valueOf(status),
    isFromMe = isFromMe,
    replyTo = replyTo,
    edited = edited,
    deleted = deleted,
    mediaUrl = mediaUrl,
    mediaThumbnail = mediaThumbnail,
    duration = duration,
    latitude = latitude,
    longitude = longitude
)

/**
 * Convierte el modelo de dominio [Message] a una entidad de Room [MessageEntity].
 */
fun Message.toEntity(): MessageEntity = MessageEntity(
    id = id,
    chatId = chatId,
    senderJid = senderJid,
    receiverJid = receiverJid,
    body = body,
    timestamp = timestamp,
    type = type.name(),
    status = status.name(),
    isFromMe = isFromMe,
    replyTo = replyTo,
    edited = edited,
    deleted = deleted,
    mediaUrl = mediaUrl,
    mediaThumbnail = mediaThumbnail,
    duration = duration,
    latitude = latitude,
    longitude = longitude
)
