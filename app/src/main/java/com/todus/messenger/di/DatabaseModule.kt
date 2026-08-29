package com.todus.messenger.di

import android.content.Context
import androidx.room.Room
import com.todus.messenger.data.local.dao.ChatDao
import com.todus.messenger.data.local.dao.ContactDao
import com.todus.messenger.data.local.dao.MessageDao
import com.todus.messenger.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt que proporciona la instancia de [AppDatabase] y sus DAOs.
 *
 * Se instala en [SingletonComponent] para que la base de datos y los DAOs
 * tengan el mismo ciclo de vida que la aplicación (singleton).
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Proporciona la instancia singleton de [AppDatabase].
     *
     * Utiliza [Room.databaseBuilder] con el contexto de la aplicación
     * para construir la base de datos Room.
     *
     * @param context Contexto de la aplicación.
     * @return Instancia única de [AppDatabase].
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "todus_messenger_db"
        ).build()
    }

    /**
     * Proporciona el DAO de mensajes.
     *
     * @param db Instancia de [AppDatabase].
     * @return Instancia de [MessageDao].
     */
    @Provides
    fun provideMessageDao(db: AppDatabase): MessageDao {
        return db.messageDao()
    }

    /**
     * Proporciona el DAO de chats.
     *
     * @param db Instancia de [AppDatabase].
     * @return Instancia de [ChatDao].
     */
    @Provides
    fun provideChatDao(db: AppDatabase): ChatDao {
        return db.chatDao()
    }

    /**
     * Proporciona el DAO de contactos.
     *
     * @param db Instancia de [AppDatabase].
     * @return Instancia de [ContactDao].
     */
    @Provides
    fun provideContactDao(db: AppDatabase): ContactDao {
        return db.contactDao()
    }
}
