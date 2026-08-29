package com.todus.messenger.data.remote.xmpp

/**
 * Modelo de datos que representa un mensaje entrante recibido a través de XMPP.
 *
 * Contiene toda la información extraída de un stanza <message> de XMPP,
 * incluyendo metadatos como el JID del remitente, marca temporal y
 * detección automática de mensajes de grupo (MUC).
 *
 * @property from JID completo del remitente (ej: "53555555@todus.cu/ToDusMessenger")
 * @property to JID completo del destinatario (null si no está disponible)
 * @property body Cuerpo del mensaje de texto (null si es un mensaje sin cuerpo,
 *           por ejemplo notificaciones de estado de chat)
 * @property messageId Identificador único del mensaje (stanzaId). Si el stanza
 *           no contiene ID, se genera uno con UUID
 * @property timestamp Marca temporal en milisegundos desde epoch. Utiliza
 *           XEP-0203 (DelayInformation) si está presente; de lo contrario,
 *           usa el momento actual del sistema
 * @property isGroupMessage true si el mensaje proviene de una sala de grupo
 *           (JID contiene @conference. o @muc.), false en caso contrario
 */
data class InMessage(
    val from: String,
    val to: String?,
    val body: String?,
    val messageId: String,
    val timestamp: Long,
    val isGroupMessage: Boolean
) {
    companion object {
        /**
         * Sufijos de dominio utilizados por servidores XMPP para identificar
         * salas de conferencia (Multi-User Chat / MUC) en ToDus.
         */
        private val MUC_DOMAIN_SUFFIXES = arrayOf("@conference.", "@muc.")

        /**
         * Determina si un JID pertenece a una sala de grupo (MUC).
         *
         * @param jid JID del remitente o de la sala
         * @return true si el JID contiene un sufijo de dominio MUC
         */
        fun isGroupJid(jid: String): Boolean {
            return MUC_DOMAIN_SUFFIXES.any { suffix ->
                jid.contains(suffix, ignoreCase = true)
            }
        }
    }
}
