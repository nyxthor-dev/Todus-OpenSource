package com.todus.messenger.domain.model

/**
 * Enum que representa los posibles estados de un mensaje en ToDus.
 *
 * El flujo de estados para un mensaje enviado es:
 * SENDING → SENT → DELIVERED → READ
 *
 * Si ocurre un error, el estado pasa a FAILED.
 */
enum class MessageStatus {
    /**
     * El mensaje está siendo enviado al servidor XMPP.
     * Aún no se ha confirmado la recepción por el servidor.
     */
    SENDING {
        override fun getIconDescription(): String = "Reloj de arena: mensaje enviando"
        override fun isFinal(): Boolean = false
    },

    /**
     * El mensaje fue recibido por el servidor ToDus.
     * El servidor confirmó la recepción pero aún no se entregó al destinatario.
     */
    SENT {
        override fun getIconDescription(): String = "Una sola marca de verificación (✓): mensaje enviado al servidor"
        override fun isFinal(): Boolean = false
    },

    /**
     * El mensaje fue entregado al dispositivo del destinatario.
     * El destinatario recibió el mensaje pero aún no lo ha abierto.
     */
    DELIVERED {
        override fun getIconDescription(): String = "Doble marca de verificación (✓✓) gris: mensaje entregado al destinatario"
        override fun isFinal(): Boolean = false
    },

    /**
     * El mensaje fue leído por el destinatario.
     * El destinatario abrió la conversación y visualizó el mensaje.
     */
    READ {
        override fun getIconDescription(): String = "Doble marca de verificación (✓✓) azul: mensaje leído por el destinatario"
        override fun isFinal(): Boolean = true
    },

    /**
     * El mensaje no pudo ser enviado.
     * Puede deberse a problemas de conexión o un error en el servidor.
     */
    FAILED {
        override fun getIconDescription(): String = "Signo de exclamación rojo (!): error al enviar el mensaje"
        override fun isFinal(): Boolean = true
    };

    /**
     * Devuelve una descripción textual del ícono que representa visualmente
     * este estado del mensaje en la interfaz de usuario.
     *
     * Las descripciones siguen el patrón de WhatsApp/ToDus:
     * - SENDING:  ⏳ (reloj de arena)
     * - SENT:     ✓  (una marca gris)
     * - DELIVERED: ✓✓ (dos marcas grises)
     * - READ:     ✓✓ (dos marcas azules)
     * - FAILED:    ⚠  (signo de exclamación rojo)
     *
     * @return Descripción del recurso/icono asociado a este estado.
     */
    abstract fun getIconDescription(): String

    /**
     * Indica si este es un estado final, es decir, que ya no cambiará
     * automáticamente por actualizaciones del servidor.
     *
     * Los estados finales son READ y FAILED.
     *
     * @return true si el estado es final, false si puede cambiar.
     */
    abstract fun isFinal(): Boolean

    /**
     * Indica si el mensaje está en proceso de envío.
     *
     * @return true si el estado es SENDING.
     */
    fun isPending(): Boolean = this == SENDING

    /**
     * Indica si el envío del mensaje falló.
     *
     * @return true si el estado es FAILED.
     */
    fun hasFailed(): Boolean = this == FAILED

    /**
     * Indica si el mensaje fue entregado o leído.
     *
     * @return true si el estado es DELIVERED o READ.
     */
    fun isDeliveredOrRead(): Boolean = this == DELIVERED || this == READ

    /**
     * Obtiene el siguiente estado esperado en el flujo normal de entrega.
     *
     * @return El siguiente [MessageStatus] o null si no hay siguiente estado.
     */
    fun nextExpectedStatus(): MessageStatus? {
        return when (this) {
            SENDING -> SENT
            SENT -> DELIVERED
            DELIVERED -> READ
            READ -> null
            FAILED -> null
        }
    }
}