package com.todus.messenger.domain.model

/**
 * Modelo de dominio que representa un contacto de la agenda del usuario.
 *
 * Los números de teléfono cubanos tienen prefijo +53 y 8 dígitos.
 * Formato almacenado: 53XXXXXXXX (10 dígitos sin el símbolo +).
 */
data class Contact(
    /** Número de teléfono en formato 53XXXXXXXX (10 dígitos, sin +) */
    val phoneNumber: String,

    /** Nombre completo del contacto */
    val name: String,

    /** URL del avatar del contacto */
    val avatarUrl: String? = null,

    /** Indica si el contacto tiene cuenta activa en ToDus */
    val isToDusUser: Boolean = false,

    /** Última vez que el contacto estuvo en línea (epoch millis). Null si no está disponible. */
    val lastSeen: Long? = null,

    /** Indica si el contacto está actualmente en línea */
    val isOnline: Boolean = false
) {
    companion object {
        /** Dominio del servidor ToDus para construir JIDs */
        private const val TODUS_DOMAIN = "todus.cu"

        /** Longitud esperada de un número cubano con prefijo (53 + 8 dígitos) */
        private const val PHONE_LENGTH = 10

        /** Prefijo internacional de Cuba */
        private const val CUBA_PREFIX = "53"

        /**
         * Crea un contacto a partir de un número de teléfono con validación.
         *
         * @param phoneNumber Número de teléfono cubano (formato: 53XXXXXXXX).
         * @param name Nombre del contacto.
         * @param avatarUrl URL del avatar (opcional).
         * @param isToDusUser Si el contacto usa ToDus (por defecto false).
         * @return Una instancia de [Contact].
         * @throws IllegalArgumentException si el número no tiene el formato correcto.
         */
        fun create(
            phoneNumber: String,
            name: String,
            avatarUrl: String? = null,
            isToDusUser: Boolean = false
        ): Contact {
            require(phoneNumber.length == PHONE_LENGTH) {
                "El número de teléfono debe tener $PHONE_LENGTH dígitos (prefijo 53 + 8 dígitos). " +
                "Valor recibido: $phoneNumber (${phoneNumber.length} caracteres)"
            }
            require(phoneNumber.startsWith(CUBA_PREFIX)) {
                "El número de teléfono debe comenzar con el prefijo de Cuba ($CUBA_PREFIX). " +
                "Valor recibido: $phoneNumber"
            }
            require(phoneNumber.all { it.isDigit() }) {
                "El número de teléfono debe contener solo dígitos. " +
                "Valor recibido: $phoneNumber"
            }

            return Contact(
                phoneNumber = phoneNumber,
                name = name,
                avatarUrl = avatarUrl,
                isToDusUser = isToDusUser
            )
        }
    }

    /**
     * Construye el JID (Jabber ID) del contacto para el servidor ToDus.
     *
     * Formato: 53XXXXXXXX@todus.cu
     *
     * @return El JID completo del contacto.
     */
    fun toJid(): String = "$phoneNumber@$TODUS_DOMAIN"

    /**
     * Obtiene el número de teléfono en formato internacional con +.
     *
     * @return Número en formato +53XXXXXXXX.
     */
    fun toInternationalFormat(): String = "+$phoneNumber"

    /**
     * Obtiene el número local (8 dígitos, sin prefijo 53).
     *
     * @return Número local de 8 dígitos.
     */
    fun toLocalNumber(): String = phoneNumber.substring(CUBA_PREFIX.length)

    /**
     * Indica si el contacto es un usuario activo de ToDus y está en línea.
     */
    fun isAvailable(): Boolean = isToDusUser && isOnline

    /**
     * Crea una copia del contacto con el estado ToDus actualizado.
     *
     * @param isToDusUser Nuevo estado de usuario ToDus.
     * @return Nueva instancia de [Contact] con el estado actualizado.
     */
    fun withToDusStatus(isToDusUser: Boolean): Contact = copy(isToDusUser = isToDusUser)

    /**
     * Crea una copia del contacto con el estado en línea actualizado.
     *
     * @param online Nuevo estado de conexión.
     * @return Nueva instancia de [Contact] con el estado actualizado.
     */
    fun withOnlineStatus(online: Boolean): Contact = copy(isOnline = online, lastSeen = if (!online) System.currentTimeMillis() else lastSeen)

    /**
     * Crea una copia del contacto con la última vez visto actualizada.
     *
     * @param lastSeenTimestamp Nueva marca de tiempo de última conexión.
     * @return Nueva instancia de [Contact] con lastSeen actualizado.
     */
    fun withLastSeen(lastSeenTimestamp: Long): Contact = copy(lastSeen = lastSeenTimestamp, isOnline = false)

    /**
     * Crea una copia del contacto con el avatar actualizado.
     *
     * @param url Nueva URL del avatar.
     * @return Nueva instancia de [Contact] con avatar actualizado.
     */
    fun withAvatar(url: String?): Contact = copy(avatarUrl = url)

    /**
     * Crea una copia del contacto con el nombre actualizado.
     *
     * @param newName Nuevo nombre del contacto.
     * @return Nueva instancia de [Contact] con nombre actualizado.
     */
    fun withName(newName: String): Contact = copy(name = newName)
}