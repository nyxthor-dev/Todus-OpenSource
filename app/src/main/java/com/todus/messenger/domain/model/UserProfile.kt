package com.todus.messenger.domain.model

/**
 * Modelo de dominio que representa el perfil del usuario autenticado en ToDus.
 *
 * Contiene la información personal del usuario y su token de autenticación
 * para el servidor XMPP de ToDus (todus.cu, puerto 1756).
 */
data class UserProfile(
    /** Número de teléfono del usuario en formato 53XXXXXXXX */
    val phoneNumber: String,

    /** Nombre a mostrar del usuario */
    val name: String,

    /** URL del avatar del usuario */
    val avatarUrl: String? = null,

    /** Texto de estado o biografía corta del usuario */
    val about: String? = null,

    /** Token JWT o API token para autenticación con el servidor ToDus */
    val token: String? = null
) {
    companion object {
        /** Dominio del servidor ToDus */
        private const val TODUS_DOMAIN = "todus.cu"

        /** Longitud esperada de un número cubano con prefijo */
        private const val PHONE_LENGTH = 10

        /** Prefijo internacional de Cuba */
        private const val CUBA_PREFIX = "53"

        /**
         * Crea un perfil de usuario con validación del número de teléfono.
         *
         * @param phoneNumber Número de teléfono cubano (formato: 53XXXXXXXX).
         * @param name Nombre del usuario.
         * @param avatarUrl URL del avatar (opcional).
         * @param about Texto de estado (opcional).
         * @param token Token de autenticación (opcional).
         * @return Una instancia de [UserProfile].
         * @throws IllegalArgumentException si el número no tiene el formato correcto.
         */
        fun create(
            phoneNumber: String,
            name: String,
            avatarUrl: String? = null,
            about: String? = null,
            token: String? = null
        ): UserProfile {
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

            return UserProfile(
                phoneNumber = phoneNumber,
                name = name,
                avatarUrl = avatarUrl,
                about = about,
                token = token
            )
        }
    }

    /**
     * Construye el JID (Jabber ID) del usuario para el servidor ToDus.
     *
     * Formato: 53XXXXXXXX@todus.cu
     *
     * @return El JID completo del usuario.
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
     * Indica si el usuario tiene un nombre configurado.
     */
    fun hasName(): Boolean = name.isNotBlank()

    /**
     * Indica si el usuario tiene un avatar configurado.
     */
    fun hasAvatar(): Boolean = !avatarUrl.isNullOrBlank()

    /**
     * Indica si el usuario tiene un texto de estado configurado.
     */
    fun hasAbout(): Boolean = !about.isNullOrBlank()

    /**
     * Indica si el usuario tiene un token de autenticación válido.
     */
    fun hasValidToken(): Boolean = !token.isNullOrBlank()

    /**
     * Crea una copia del perfil con el nombre actualizado.
     *
     * @param newName Nuevo nombre del usuario.
     * @return Nueva instancia de [UserProfile] con nombre actualizado.
     */
    fun withName(newName: String): UserProfile = copy(name = newName)

    /**
     * Crea una copia del perfil con el avatar actualizado.
     *
     * @param url Nueva URL del avatar.
     * @return Nueva instancia de [UserProfile] con avatar actualizado.
     */
    fun withAvatar(url: String?): UserProfile = copy(avatarUrl = url)

    /**
     * Crea una copia del perfil con el texto de estado actualizado.
     *
     * @param newAbout Nuevo texto de estado.
     * @return Nueva instancia de [UserProfile] con estado actualizado.
     */
    fun withAbout(newAbout: String?): UserProfile = copy(about = newAbout)

    /**
     * Crea una copia del perfil con el token de autenticación actualizado.
     *
     * @param newToken Nuevo token de autenticación.
     * @return Nueva instancia de [UserProfile] con token actualizado.
     */
    fun withToken(newToken: String?): UserProfile = copy(token = newToken)

    /**
     * Crea una copia del perfil sin el token (para cerrar sesión).
     *
     * @return Nueva instancia de [UserProfile] sin token.
     */
    fun clearToken(): UserProfile = copy(token = null)
}