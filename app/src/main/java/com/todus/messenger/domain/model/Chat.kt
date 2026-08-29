package com.todus.messenger.domain.model

/**
 * Modelo de dominio que representa una conversación (chat individual o grupal).
 *
 * Para chats individuales, [id] es el JID del otro usuario (53XXXXXXXX@todus.cu).
 * Para grupos (MUC Light), [id] es el identificador del grupo.
 *
 * Utiliza el protocolo XMPP de ToDus (servidor todus.cu, puerto 1756).
 */
data class Chat(
    /** Identificador único del chat: JID del otro usuario o groupId para grupos MUC Light */
    val id: String,

    /** Nombre a mostrar del chat (nombre del contacto o nombre del grupo) */
    val name: String,

    /** URL del avatar del chat (contacto o grupo) */
    val avatarUrl: String? = null,

    /** Texto del último mensaje recibido o enviado */
    val lastMessage: String? = null,

    /** Marca de tiempo del último mensaje en epoch millis (XEP-0082) */
    val lastMessageTime: Long? = null,

    /** Cantidad de mensajes no leídos */
    val unreadCount: Int = 0,

    /** Indica si el chat es un grupo MUC Light */
    val isGroup: Boolean = false,

    /** Indica si el contacto está en línea (solo para chats individuales) */
    val isOnline: Boolean = false,

    /** Lista de JIDs de los miembros del grupo (solo para chats grupales) */
    val members: List<String>? = null,

    /** Estado del último mensaje (solo relevante para mensajes enviados por mí) */
    val lastMessageStatus: MessageStatus? = null
) {
    companion object {
        /** Dominio del servidor ToDus */
        private const val TODUS_DOMAIN = "todus.cu"

        /** Longitud de un número de teléfono cubano completo con prefijo (53 + 8 dígitos) */
        private const val PHONE_LENGTH = 10

        /** Longitud del prefijo internacional de Cuba */
        private const val PREFIX_LENGTH = 2

        /**
         * Crea un chat individual a partir del número de teléfono del contacto.
         *
         * @param phoneNumber Número de teléfono cubano (formato: 53XXXXXXXX).
         * @param name Nombre a mostrar del contacto.
         * @param avatarUrl URL del avatar del contacto (opcional).
         * @param isOnline Indica si el contacto está en línea (por defecto false).
         * @return Una instancia de [Chat] para chat individual.
         */
        fun createIndividualChat(
            phoneNumber: String,
            name: String,
            avatarUrl: String? = null,
            isOnline: Boolean = false
        ): Chat {
            require(phoneNumber.length == PHONE_LENGTH) {
                "El número de teléfono debe tener $PHONE_LENGTH dígitos (prefijo 53 + 8 dígitos). " +
                "Valor recibido: $phoneNumber (${phoneNumber.length} caracteres)"
            }
            require(phoneNumber.startsWith("53")) {
                "El número de teléfono debe comenzar con el prefijo de Cuba (53). " +
                "Valor recibido: $phoneNumber"
            }

            val jid = "$phoneNumber@$TODUS_DOMAIN"

            return Chat(
                id = jid,
                name = name,
                avatarUrl = avatarUrl,
                isGroup = false,
                isOnline = isOnline
            )
        }

        /**
         * Crea un chat individual a partir de un JID completo.
         *
         * @param jid JID del contacto (formato: 53XXXXXXXX@todus.cu).
         * @param name Nombre a mostrar del contacto.
         * @param avatarUrl URL del avatar del contacto (opcional).
         * @param isOnline Indica si el contacto está en línea (por defecto false).
         * @return Una instancia de [Chat] para chat individual.
         */
        fun createFromJid(
            jid: String,
            name: String,
            avatarUrl: String? = null,
            isOnline: Boolean = false
        ): Chat {
            require(jid.endsWith("@$TODUS_DOMAIN")) {
                "El JID debe pertenecer al dominio $TODUS_DOMAIN. Valor recibido: $jid"
            }

            return Chat(
                id = jid,
                name = name,
                avatarUrl = avatarUrl,
                isGroup = false,
                isOnline = isOnline
            )
        }

        /**
         * Crea un chat grupal MUC Light.
         *
         * @param groupId Identificador único del grupo.
         * @param groupName Nombre del grupo.
         * @param avatarUrl URL del avatar del grupo (opcional).
         * @param members Lista de JIDs de los miembros del grupo.
         * @return Una instancia de [Chat] para grupo.
         */
        fun createGroupChat(
            groupId: String,
            groupName: String,
            avatarUrl: String? = null,
            members: List<String> = emptyList()
        ): Chat {
            return Chat(
                id = groupId,
                name = groupName,
                avatarUrl = avatarUrl,
                isGroup = true,
                members = members
            )
        }
    }

    /**
     * Extrae el número de teléfono del JID (solo para chats individuales).
     *
     * @return El número de teléfono sin el dominio, o null si es un grupo.
     */
    fun extractPhoneNumber(): String? {
        if (isGroup) return null
        return id.substringBefore("@")
    }

    /**
     * Indica si el chat tiene mensajes no leídos.
     */
    fun hasUnreadMessages(): Boolean = unreadCount > 0

    /**
     * Crea una copia del chat con el último mensaje actualizado.
     *
     * @param message Texto del último mensaje.
     * @param time Marca de tiempo del mensaje.
     * @param status Estado del mensaje (opcional, para mensajes enviados por mí).
     * @return Nueva instancia de [Chat] con el último mensaje actualizado.
     */
    fun withLastMessage(
        message: String,
        time: Long,
        status: MessageStatus? = null
    ): Chat = copy(
        lastMessage = message,
        lastMessageTime = time,
        lastMessageStatus = status
    )

    /**
     * Crea una copia del chat con la cantidad de mensajes no leídos actualizada.
     *
     * @param count Nueva cantidad de mensajes no leídos.
     * @return Nueva instancia de [Chat] con el conteo actualizado.
     */
    fun withUnreadCount(count: Int): Chat = copy(unreadCount = count)

    /**
     * Crea una copia del chat con el estado de conexión actualizado.
     *
     * @param online Nuevo estado de conexión del contacto.
     * @return Nueva instancia de [Chat] con el estado actualizado.
     */
    fun withOnlineStatus(online: Boolean): Chat = copy(isOnline = online)

    /**
     * Incrementa en 1 la cantidad de mensajes no leídos.
     *
     * @return Nueva instancia de [Chat] con unreadCount incrementado.
     */
    fun incrementUnreadCount(): Chat = copy(unreadCount = unreadCount + 1)

    /**
     * Reinicia a cero la cantidad de mensajes no leídos.
     *
     * @return Nueva instancia de [Chat] con unreadCount en 0.
     */
    fun markAsRead(): Chat = copy(unreadCount = 0)

    /**
     * Obtiene la cantidad de miembros del chat.
     * Para chats individuales devuelve 2 (yo + el contacto).
     * Para grupos devuelve la cantidad de miembros o 0 si es nulo.
     *
     * @return Cantidad de participantes en el chat.
     */
    fun getMemberCount(): Int {
        return if (isGroup) {
            members?.size ?: 0
        } else {
            2 // Yo y el contacto
        }
    }
}
