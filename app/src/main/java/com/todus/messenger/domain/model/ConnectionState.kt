package com.todus.messenger.domain.model

/**
 * Interfaz sellada que representa los posibles estados de la conexión
 * con el servidor XMPP de ToDus (todus.cu, puerto 1756).
 *
 * Cada estado carry información contextual relevante, como mensajes de error.
 * Al ser una sealed interface, el compilador garantiza que todas las
 * variantes son manejadas en las expresiones `when`.
 */
sealed interface ConnectionState {
    /**
     * La conexión está activa y el usuario está autenticado.
     * La app puede enviar y recibir mensajes normalmente.
     */
    data object Connected : ConnectionState {
        override fun toString(): String = "Conectado al servidor ToDus"
    }

    /**
     * No hay conexión activa con el servidor.
     * Puede deberse a falta de red, cierre de sesión o desconexión forzada.
     */
    data object Disconnected : ConnectionState {
        override fun toString(): String = "Desconectado del servidor ToDus"
    }

    /**
     * Se está estableciendo la conexión con el servidor.
     * El cliente está intentando conectar pero aún no ha completado el handshake TCP/TLS.
     */
    data object Connecting : ConnectionState {
        override fun toString(): String = "Conectando al servidor ToDus..."
    }

    /**
     * La conexión TCP está establecida y se está realizando la autenticación
     * con el servidor XMPP (SASL). El usuario ya está conectado pero no autenticado.
     */
    data object Authenticating : ConnectionState {
        override fun toString(): String = "Autenticando con el servidor ToDus..."
    }

    /**
     * Ocurrió un error durante la conexión o autenticación.
     *
     * @param message Descripción legible del error que ocurrió.
     */
    data class Error(val message: String) : ConnectionState {
        override fun toString(): String = "Error de conexión: $message"
    }

    companion object {
        /**
         * Crea una instancia de [ConnectionState.Error] con un mensaje.
         *
         * @param message Descripción del error.
         * @return Instancia de [ConnectionState.Error].
         */
        fun error(message: String): ConnectionState = Error(message)

        /**
         * Crea una instancia de [ConnectionState.Error] a partir de una excepción.
         *
         * @param throwable Excepción que causó el error.
         * @return Instancia de [ConnectionState.Error] con el mensaje de la excepción.
         */
        fun fromException(throwable: Throwable): ConnectionState {
            val message = throwable.message ?: "Error desconocido de conexión"
            return Error(message)
        }
    }

    /**
     * Indica si el estado actual permite enviar y recibir mensajes.
     *
     * @return true solo si el estado es [Connected].
     */
    fun isConnected(): Boolean = this is Connected

    /**
     * Indica si la conexión está en un estado transitorio (conectando o autenticando).
     *
     * @return true si el estado es [Connecting] o [Authenticating].
     */
    fun isTransitioning(): Boolean = this is Connecting || this is Authenticating

    /**
     * Indica si hubo un error de conexión.
     *
     * @return true solo si el estado es [Error].
     */
    fun hasError(): Boolean = this is Error

    /**
     * Obtiene el mensaje de error si el estado es [Error], o null en caso contrario.
     *
     * @return El mensaje de error o null.
     */
    fun getErrorMessage(): String? {
        return when (this) {
            is Error -> message
            else -> null
        }
    }

    /**
     * Indica si se puede intentar reconectar a partir de este estado.
     * Solo se puede reconectar cuando se está desconectado o hubo un error.
     *
     * @return true si se puede intentar reconectar.
     */
    fun canRetry(): Boolean {
        return this is Disconnected || this is Error
    }
}