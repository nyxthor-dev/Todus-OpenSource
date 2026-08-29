package com.todus.messenger.domain.repository

import com.todus.messenger.domain.model.Contact
import kotlinx.coroutines.flow.Flow

/**
 * Interfaz del repositorio de contactos.
 *
 * Define las operaciones disponibles para gestionar los contactos
 * del usuario: consulta, búsqueda, almacenamiento y filtrado.
 */
interface ContactRepository {

    /**
     * Obtiene todos los contactos de forma reactiva,
     * ordenados alfabéticamente por nombre.
     *
     * @return Flujo reactivo con la lista completa de contactos.
     */
    fun getAllContacts(): Flow<List<Contact>>

    /**
     * Busca contactos cuyo nombre o número de teléfono contengan
     * el texto de búsqueda proporcionado.
     *
     * La búsqueda es insensible a mayúsculas y minúsculas.
     *
     * @param query Texto de búsqueda.
     * @return Flujo reactivo con los contactos que coinciden.
     */
    fun searchContacts(query: String): Flow<List<Contact>>

    /**
     * Guarda un contacto en la base de datos local.
     * Si ya existe un contacto con el mismo número de teléfono,
     * se reemplaza (UPSERT).
     *
     * @param contact Contacto a guardar.
     */
    suspend fun saveContact(contact: Contact)

    /**
     * Busca un contacto por su número de teléfono.
     *
     * @param phone Número de teléfono del contacto (formato 53XXXXXXXX).
     * @return El contacto encontrado, o null si no existe.
     */
    suspend fun getContactByPhone(phone: String): Contact?

    /**
     * Obtiene únicamente los contactos que son usuarios registrados
     * en ToDus, ordenados alfabéticamente por nombre.
     *
     * Útil para mostrar la lista de contactos con los que se puede
     * chatear dentro de la aplicación.
     *
     * @return Flujo reactivo con la lista de contactos usuarios de ToDus.
     */
    fun getToDusContacts(): Flow<List<Contact>>
}
