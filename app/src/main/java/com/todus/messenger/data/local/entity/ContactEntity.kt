package com.todus.messenger.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.todus.messenger.domain.model.Contact

/**
 * Entidad de Room que representa un contacto almacenado localmente.
 * Mapeable a/desde el modelo de dominio [Contact].
 */
@Entity(tableName = "contacts")
data class ContactEntity(
    /** Número de teléfono del contacto, usado como clave primaria */
    @PrimaryKey
    val phoneNumber: String,

    /** Nombre del contacto */
    val name: String,

    /** URL del avatar del contacto */
    val avatarUrl: String? = null,

    /** Indica si el contacto es usuario de ToDus */
    val isToDusUser: Boolean = false,

    /** Marca temporal de la última conexión en milisegundos */
    val lastSeen: Long? = null,

    /** Indica si el contacto está actualmente en línea */
    val isOnline: Boolean = false
)

/**
 * Convierte esta entidad de Room al modelo de dominio [Contact].
 */
fun ContactEntity.toDomain(): Contact = Contact(
    phoneNumber = phoneNumber,
    name = name,
    avatarUrl = avatarUrl,
    isToDusUser = isToDusUser,
    lastSeen = lastSeen,
    isOnline = isOnline
)

/**
 * Convierte el modelo de dominio [Contact] a una entidad de Room [ContactEntity].
 */
fun Contact.toEntity(): ContactEntity = ContactEntity(
    phoneNumber = phoneNumber,
    name = name,
    avatarUrl = avatarUrl,
    isToDusUser = isToDusUser,
    lastSeen = lastSeen,
    isOnline = isOnline
)
