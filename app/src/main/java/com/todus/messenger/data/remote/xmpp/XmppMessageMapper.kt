package com.todus.messenger.data.remote.xmpp

import com.todus.messenger.domain.model.Message
import com.todus.messenger.domain.model.MessageStatus
import com.todus.messenger.domain.model.MessageType
import org.jivesoftware.smack.packet.Message as XmppMessage
import org.jxmpp.jid.JidCreate

/**
 * Objeto encargado de transformar mensajes entre la capa XMPP (Smack)
 * y los modelos de dominio de la aplicación.
 *
 * Se encarga de:
 * - Convertir mensajes entrantes [InMessage] al modelo de dominio [Message]
 * - Construir stanzas [XmppMessage] de Smack para envío
 * - Determinar si un mensaje es propio (enviado por el usuario actual)
 *
 * Requiere que se establezca [currentPhoneNumber] antes de usar [toDomainMessage]
 * para que la detección de mensajes propios funcione correctamente.
 */
object XmppMessageMapper {

    /**
     * Número de teléfono del usuario autenticado (sin el @todus.cu).
     * Se debe establecer después del login exitoso para que
     * [InMessage.toDomainMessage] pueda determinar si un mensaje
     * fue enviado por el usuario actual.
     */
    var currentPhoneNumber: String = ""

    /**
     * Convierte un mensaje XMPP entrante ([InMessage]) al modelo de dominio [Message].
     *
     * Realiza las siguientes transformaciones:
     * - Extrae la parte local del JID (número de teléfono) del remitente
     * - Determina [Message.isFromMe] comparando el remitente con el número
     *   del usuario actual ([currentPhoneNumber])
     * - Asigna tipo [MessageType.TEXT] (ToDus utiliza solo texto plano)
     * - Asigna estado [MessageStatus.DELIVERED] a los mensajes entrantes
     * - Usa el [chatId] proporcionado como identificador de la conversación
     *
     * @receiver Mensaje XMPP entrante a convertir
     * @param chatId Identificador del chat al que pertenece el mensaje.
     *               Normalmente es el número de teléfono del otro participante
     *               o el JID de la sala de grupo
     * @return Instancia de [Message] del modelo de dominio
     */
    fun InMessage.toDomainMessage(chatId: String): Message {
        // Extraer la parte local del JID (número de teléfono) del remitente
        // Formato esperado: "53555555@todus.cu/Recurso" -> "53555555"
        val senderPhone = from.split("@").firstOrNull() ?: ""

        // Determinar si el mensaje fue enviado por el usuario actual
        // comparando el número de teléfono del remitente con el nuestro
        val isFromMe = senderPhone == currentPhoneNumber

        // Construir el ID único del mensaje usando el stanzaId de XMPP
        val domainMessageId = messageId

        // Mapear el cuerpo del mensaje, asignar cadena vacía si es null
        val messageBody = body ?: ""

        return Message(
            id = domainMessageId,
            chatId = chatId,
            body = messageBody,
            from = senderPhone,
            isFromMe = isFromMe,
            type = MessageType.TEXT,
            status = MessageStatus.DELIVERED,
            timestamp = timestamp
        )
    }

    /**
     * Crea un stanza XMPP [XmppMessage] de Smack listo para ser enviado.
     *
     * Configura:
     * - El cuerpo del mensaje (texto plano)
     * - El destinatario como JID de entidad bare (usuario@dominio)
     * - El identificador del stanza para seguimiento y confirmaciones
     *
     * El mensaje creado NO se envía automáticamente; debe pasarse a
     * [ToDusXmppClient.sendMessage] o usar [org.jivesoftware.smack.XMPPConnection.sendStanza]
     * directamente.
     *
     * @param to JID del destinatario (ej: "53555555@todus.cu")
     * @param body Cuerpo del mensaje de texto a enviar
     * @param messageId Identificador único para el mensaje.
     *               Se usará como stanzaId y para correlacionar
     *               confirmaciones de entrega (XEP-0184)
     * @return Instancia de [XmppMessage] configurada y lista para envío
     * @throws IllegalArgumentException si el JID del destinatario es inválido
     */
    fun createXmppMessage(to: String, body: String, messageId: String): XmppMessage {
        val message = XmppMessage()
        message.setBody(body)
        message.setTo(JidCreate.entityBareFrom(to))
        message.setStanzaId(messageId)
        return message
    }
}
