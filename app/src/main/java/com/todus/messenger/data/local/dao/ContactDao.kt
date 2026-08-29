package com.todus.messenger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.todus.messenger.data.local.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

/**
 * Objeto de acceso a datos (DAO) para la entidad [ContactEntity].
 * Proporciona operaciones CRUD y consultas para la tabla de contactos.
 */
@Dao
interface ContactDao {

    /**
     * Inserta o reemplaza un contacto en la base de datos.
     *
     * @param contact La entidad del contacto a insertar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    /**
     * Inserta o reemplaza una lista de contactos de forma transaccional.
     * Útil para importar la agenda del dispositivo.
     *
     * @param contacts Lista de entidades de contactos a insertar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    /**
     * Obtiene todos los contactos ordenados alfabéticamente por nombre.
     * Emite un [Flow] reactivo que se actualiza con cada cambio.
     *
     * @return Flujo reactivo con la lista completa de contactos.
     */
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    /**
     * Busca un contacto específico por su número de teléfono.
     *
     * @param phoneNumber Número de teléfono del contacto.
     * @return La entidad del contacto, o null si no existe.
     */
    @Query("SELECT * FROM contacts WHERE phoneNumber = :phoneNumber")
    suspend fun getContactByPhone(phoneNumber: String): ContactEntity?

    /**
     * Busca contactos cuyo nombre o número de teléfono contenga el texto dado.
     * La búsqueda es insensible a mayúsculas/minúsculas gracias al LIKE.
     * Emite un [Flow] reactivo que se actualiza con cada cambio.
     *
     * @param query Texto de búsqueda.
     * @return Flujo reactivo con los contactos que coinciden.
     */
    @Query(
        """SELECT * FROM contacts 
           WHERE name LIKE '%' || :query || '%' 
           OR phoneNumber LIKE '%' || :query || '%'"""
    )
    fun searchContacts(query: String): Flow<List<ContactEntity>>

    /**
     * Obtiene únicamente los contactos que son usuarios de ToDus,
     * ordenados alfabéticamente por nombre.
     * Emite un [Flow] reactivo que se actualiza con cada cambio.
     *
     * @return Flujo reactivo con la lista de contactos usuarios de ToDus.
     */
    @Query("SELECT * FROM contacts WHERE isToDusUser = 1 ORDER BY name ASC")
    fun getToDusContacts(): Flow<List<ContactEntity>>

    /**
     * Actualiza el estado de usuario ToDus y la conexión en línea
     * de un contacto identificado por su número de teléfono.
     *
     * @param phoneNumber Número de teléfono del contacto.
     * @param isUser Indica si el contacto es usuario de ToDus.
     * @param isOnline Indica si el contacto está en línea.
     */
    @Query(
        """UPDATE contacts SET isToDusUser = :isUser, isOnline = :isOnline 
           WHERE phoneNumber = :phoneNumber"""
    )
    suspend fun updateToDusStatus(phoneNumber: String, isUser: Boolean, isOnline: Boolean)
}