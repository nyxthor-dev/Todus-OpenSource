package com.todus.messenger.domain.model

/**
 * Modelo de dominio que representa un mensaje en la aplicación ToDus.
 *
 * Utiliza el protocolo XMPP de ToDus (servidor todus.cu, puerto 1756).
 * Los timestamps se basan en XEP-0082 (almacenados como epoch millis para facilidad de uso).
 */
data class Message(
    /** Identificador único del mensaje */
    val id: String,

    /** Identificador del chat (JID del otro usuario o groupId) */
    val chatId: String,

    /** JID del remitente (formato: 53XXXXXXXX@todus.cu) */
    val senderJid: String,

    /** JID del destinatario (formato: 53XXXXXXXX@todus.cu) */
    val receiverJid: String,

    /** Contenido textual del mensaje */
    val body: String,

    /** Marca de tiempo en epoch millis (XEP-0082) */
    val timestamp: Long,

    /** Tipo de mensaje (texto, imagen, video, audio, etc.) */
    val type: MessageType,

    /** Estado de entrega del mensaje */
    val status: MessageStatus,

    /** Indica si el mensaje fue enviado por el usuario actual */
    val isFromMe: Boolean,

    /** ID del mensaje al que se responde (nulo si no es respuesta) */
    val replyTo: String? = null,

    /** Indica si el mensaje fue editado */
    val edited: Boolean = false,

    /** Indica si el mensaje fue eliminado para todos */
    val deleted: Boolean = false,

    /** URL del archivo multimedia asociado al mensaje */
    val mediaUrl: String? = null,

    /** URL de la miniatura del archivo multimedia */
    val mediaThumbnail: String? = null,

    /** Duración en segundos (para mensajes de audio o video) */
    val duration: Int? = null,

    /** Latitud de la ubicación compartida */
    val latitude: Double? = null,

    /** Longitud de la ubicación compartida */
    val longitude: Double? = null
) {
    /**
     * Funciones de utilidad para crear instancias de [Message] de forma rápida.
     */
    companion object {
        /**
         * Crea un mensaje de texto simple.
         *
         * @param id Identificador único del mensaje.
         * @param chatId JID del chat al que pertenece.
         * @param senderJid JID del remitente.
         * @param receiverJid JID del destinatario.
         * @param body Contenido textual del mensaje.
         * @param isFromMe Indica si el usuario actual es el remitente.
         * @param timestamp Marca de tiempo (por defecto, el momento actual).
         * @return Una instancia de [Message] de tipo texto.
         */
        fun createTextMessage(
            id: String,
            chatId: String,
            senderJid: String,
            receiverJid: String,
            body: String,
            isFromMe: Boolean,
            timestamp: Long = System.currentTimeMillis()
        ): Message {
            return Message(
                id = id,
                chatId = chatId,
                senderJid = senderJid,
                receiverJid = receiverJid,
                body = body,
                timestamp = timestamp,
                type = MessageType.TEXT,
                status = if (isFromMe) MessageStatus.SENDING else MessageStatus.DELIVERED,
                isFromMe = isFromMe
            )
        }

        /**
         * Crea un mensaje con ubicación.
         *
         * @param id Identificador único del mensaje.
         * @param chatId JID del chat al que pertenece.
         * @param senderJid JID del remitente.
         * @param receiverJid JID del destinatario.
         * @param body Texto descriptivo de la ubicación.
         * @param latitude Latitud de la ubicación.
         * @param longitude Longitud de la ubicación.
         * @param isFromMe Indica si el usuario actual es el remitente.
         * @param timestamp Marca de tiempo (por defecto, el momento actual).
         * @return Una instancia de [Message] de tipo ubicación.
         */
        fun createLocationMessage(
            id: String,
            chatId: String,
            senderJid: String,
            receiverJid: String,
            body: String,
            latitude: Double,
            longitude: Double,
            isFromMe: Boolean,
            timestamp: Long = System.currentTimeMillis()
        ): Message {
            return Message(
                id = id,
                chatId = chatId,
                senderJid = senderJid,
                receiverJid = receiverJid,
                body = body,
                timestamp = timestamp,
                type = MessageType.LOCATION,
                status = if (isFromMe) MessageStatus.SENDING else MessageStatus.DELIVERED,
                isFromMe = isFromMe,
                latitude = latitude,
                longitude = longitude
            )
        }

        /**
         * Crea un mensaje multimedia.
         *
         * @param id Identificador único del mensaje.
         * @param chatId JID del chat al que pertenece.
         * @param senderJid JID del remitente.
         * @param receiverJid JID del destinatario.
         * @param body Texto descriptivo o pie de foto.
         * @param type Tipo de medio (IMAGE, VIDEO, AUDIO, STICKER).
         * @param mediaUrl URL del archivo multimedia.
         * @param mediaThumbnail URL de la miniatura.
         * @param duration Duración en segundos (para audio/video).
         * @param isFromMe Indica si el usuario actual es el remitente.
         * @param timestamp Marca de tiempo (por defecto, el momento actual).
         * @return Una instancia de [Message] del tipo multimedia indicado.
         */
        fun createMediaMessage(
            id: String,
            chatId: String,
            senderJid: String,
            receiverJid: String,
            body: String,
            type: MessageType,
            mediaUrl: String,
            mediaThumbnail: String? = null,
            duration: Int? = null,
            isFromMe: Boolean,
            timestamp: Long = System.currentTimeMillis()
        ): Message {
            require(type.isMediaType()) { "El tipo debe ser un tipo de medio: $type" }

            return Message(
                id = id,
                chatId = chatId,
                senderJid = senderJid,
                receiverJid = receiverJid,
                body = body,
                timestamp = timestamp,
                type = type,
                status = if (isFromMe) MessageStatus.SENDING else MessageStatus.DELIVERED,
                isFromMe = isFromMe,
                mediaUrl = mediaUrl,
                mediaThumbnail = mediaThumbnail,
                duration = duration
            )
        }
    }

    /**
     * Indica si este mensaje contiene medios.
     */
    fun hasMedia(): Boolean = mediaUrl != null

    /**
     * Indica si este mensaje contiene una ubicación.
     */
    fun hasLocation(): Boolean = latitude != null && longitude != null

    /**
     * Indica si este mensaje es una respuesta a otro mensaje.
     */
    fun isReply(): Boolean = replyTo != null

    /**
     * Indica si el mensaje puede ser editado.
     * Solo los mensajes de texto enviados por mí pueden editarse.
     */
    fun canBeEdited(): Boolean = isFromMe && !deleted && type == MessageType.TEXT

    /**
     * Crea una copia del mensaje con el estado actualizado.
     *
     * @param newStatus Nuevo estado del mensaje.
     * @return Nueva instancia de [Message] con el estado actualizado.
     */
    fun withStatus(newStatus: MessageStatus): Message = copy(status = newStatus)

    /**
     * Crea una copia del mensaje marcado como editado.
     *
     * @param newBody Nuevo contenido textual.
     * @return Nueva instancia de [Message] editado.
     */
    fun asEdited(newBody: String): Message = copy(body = newBody, edited = true)

    /**
     * Crea una copia del mensaje marcado como eliminado.
     *
     * @return Nueva instancia de [Message] eliminado.
     */
    fun asDeleted(): Message = copy(
        body = "",
        mediaUrl = null,
        mediaThumbnail = null,
        latitude = null,
        longitude = null,
        deleted = true
    )
}
