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
 */
@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val database: AppDatabase
) : ContactRepository {

    companion object {
        private const val TAG = "ContactRepositoryImpl"
    }

    private val contactDao = database.contactDao()

    override fun getAllContacts(): Flow<List<Contact>> {
        return contactDao.getAllContacts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchContacts(query: String): Flow<List<Contact>> {
        return contactDao.searchContacts(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveContact(contact: Contact) {
        withContext(Dispatchers.IO) {
            contactDao.insertContact(contact.toEntity())
        }
        Log.d(TAG, "Contacto guardado: ${contact.phoneNumber} - ${contact.name}")
    }

    override suspend fun getContactByPhone(phone: String): Contact? =
        withContext(Dispatchers.IO) {
            contactDao.getContactByPhone(phone)?.toDomain()
        }

    override fun getToDusContacts(): Flow<List<Contact>> {
        return contactDao.getToDusContacts().map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
