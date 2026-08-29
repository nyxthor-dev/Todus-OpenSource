package com.todus.messenger.di

import android.content.Context
import com.todus.messenger.data.local.database.AppDatabase
import com.todus.messenger.data.remote.xmpp.ToDusXmppClient
import com.todus.messenger.data.repository.MessageRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt que proporciona los componentes de red y el repositorio
 * de mensajes que depende directamente del cliente XMPP.
 *
 * Se instala en [SingletonComponent] para que el cliente XMPP y el
 * repositorio de mensajes tengan el mismo ciclo de vida que la aplicación.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Proporciona la instancia singleton del cliente XMPP de ToDus.
     *
     * Se conecta al servidor todus.cu en el puerto 1756 (producción),
     * sin TLS, según la configuración predeterminada de [ToDusXmppClient].
     *
     * @param context Contexto de la aplicación Android.
     * @return Instancia única de [ToDusXmppClient].
     */
    @Provides
    @Singleton
    fun provideXmppClient(@ApplicationContext context: Context): ToDusXmppClient {
        return ToDusXmppClient(context)
    }

    /**
     * Proporciona la instancia singleton de [MessageRepositoryImpl].
     *
     * Este repositorio se construye aquí porque depende directamente
     * del [ToDusXmppClient], que es un componente de red. La vinculación
     * a la interfaz [com.todus.messenger.domain.repository.MessageRepository]
     * se realiza en [RepositoryModule].
     *
     * @param xmppClient Cliente XMPP inyectado.
     * @param db Base de datos Room inyectada.
     * @param context Contexto de la aplicación.
     * @return Instancia única de [MessageRepositoryImpl].
     */
    @Provides
    @Singleton
    fun provideMessageRepository(
        xmppClient: ToDusXmppClient,
        db: AppDatabase,
        @ApplicationContext context: Context
    ): MessageRepositoryImpl {
        return MessageRepositoryImpl(
            context = context,
            database = db,
            xmppClient = xmppClient
        )
    }
}
