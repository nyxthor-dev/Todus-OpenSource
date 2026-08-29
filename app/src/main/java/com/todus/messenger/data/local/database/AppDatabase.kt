package com.todus.messenger.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.todus.messenger.data.local.dao.ChatDao
import com.todus.messenger.data.local.dao.ContactDao
import com.todus.messenger.data.local.dao.MessageDao
import com.todus.messenger.data.local.entity.ChatEntity
import com.todus.messenger.data.local.entity.ContactEntity
import com.todus.messenger.data.local.entity.MessageEntity

/**
 * Base de datos principal de la aplicación ToDus Messenger.
 * Gestiona las entidades de mensajes, chats y contactos mediante Room.
 *
 * @property messageDao DAO para operaciones sobre mensajes.
 * @property chatDao DAO para operaciones sobre chats.
 * @property contactDao DAO para operaciones sobre contactos.
 */
@Database(
    entities = [
        MessageEntity::class,
        ChatEntity::class,
        ContactEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /** Proporciona el DAO para acceder a la tabla de mensajes */
    abstract fun messageDao(): MessageDao

    /** Proporciona el DAO para acceder a la tabla de chats */
    abstract fun chatDao(): ChatDao

    /** Proporciona el DAO para acceder a la tabla de contactos */
    abstract fun contactDao(): ContactDao

    companion object {
        /** Nombre del archivo de la base de datos */
        private const val DATABASE_NAME = "todus_messenger_db"

        /** Instancia única de la base de datos, volátil para visibilidad entre hilos */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Obtiene la instancia singleton de [AppDatabase].
         * Si no existe, la crea de forma segura usando double-checked locking.
         *
         * @param context Contexto de la aplicación, usado para construir la BD.
         * @return La instancia única de [AppDatabase].
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).build().also { INSTANCE = it }
            }
        }
    }
}
