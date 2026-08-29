package com.todus.messenger.data.repository

import android.util.Log
import com.todus.messenger.data.local.database.AppDatabase
import com.todus.messenger.data.local.entity.toDomain
import com.todus.messenger.data.local.entity.toEntity
import com.todus.messenger.domain.model.Contact
import com.todus.messenger.domain.repository.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación concreta del repositorio de contactos.
 *
 * Gestiona las operaciones de consulta, búsqueda y almacenamiento
 * de contactos en la base de datos local (Room), mapeando entre
 * entidades ([com.todus.messenger.data.local.entity.ContactEntity])
 * y modelos de dominio ([Contact]).
 *
 * Todas las operaciones de base de datos se ejecutan en [Dispatchers.IO].
 */
@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val database: AppDatabase
) : ContactRepository {

    companion object {
        private const val TAG = "ContactRepositoryImpl"
    }

    /** DAO para operaciones sobre contactos en Room */
    private val contactDao = database.contactDao()

    /**
     * Obtiene todos los contactos de forma reactiva,
     * ordenados alfabéticamente por nombre.
     *
     * @return Flujo reactivo con la lista de contactos mapeados al dominio.
     */
    override fun getAllContacts(): Flow<List<Contact>> {
        return contactDao.getAllContacts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Busca contactos cuyo nombre o número de teléfono contengan
     * el texto de búsqueda proporcionado.
     *
     * La búsqueda es insensible a mayúsculas y minúsculas gracias
     * al operador LIKE de SQLite.
     *
     * @param query Texto de búsqueda.
     * @return Flujo reactivo con los contactos que coinciden.
     */
    override fun searchContacts(query: String): Flow<List<Contact>> {
        return contactDao.searchContacts(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Guarda un contacto en la base de datos local.
     * Si ya existe un contacto con el mismo número de teléfono,
     * se reemplaza (UPSERT gracias a OnConflictStrategy.REPLACE).
     *
     * @param contact Contacto a guardar.
     */
    override suspend fun saveContact(contact: Contact) = withContext(Dispatchers.IO) {
        contactDao.insertContact(contact.toEntity())
        Log.d(TAG, "Contacto guardado: ${contact.phoneNumber} - ${contact.name}")
    }

    /**
     * Busca un contacto por su número de teléfono.
     *
     * @param phone Número de teléfono del contacto (formato 53XXXXXXXX).
     * @return El contacto encontrado, o null si no existe.
     */
    override suspend fun getContactByPhone(phone: String): Contact? =
        withContext(Dispatchers.IO) {
            contactDao.getContactByPhone(phone)?.toDomain()
        }

    /**
     * Obtiene únicamente los contactos que son usuarios registrados
     * en ToDus, ordenados alfabéticamente por nombre.
     *
     * @return Flujo reactivo con la lista de contactos usuarios de ToDus.
     */
    override fun getToDusContacts(): Flow<List<Contact>> {
        return contactDao.getToDusContacts().map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
