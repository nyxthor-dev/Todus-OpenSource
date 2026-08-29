package com.todus.messenger.di

import com.todus.messenger.data.repository.ChatRepositoryImpl
import com.todus.messenger.data.repository.ContactRepositoryImpl
import com.todus.messenger.data.repository.MessageRepositoryImpl
import com.todus.messenger.domain.repository.ChatRepository
import com.todus.messenger.domain.repository.ContactRepository
import com.todus.messenger.domain.repository.MessageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt que vincula las interfaces del dominio con sus
 * implementaciones concretas de la capa de datos.
 *
 * Utiliza [Binds] en lugar de [Provides] para generar código de
 * inyección más eficiente, ya que evita la creación de fábricas
 * adicionales en tiempo de compilación.
 *
 * Se instala en [SingletonComponent] para que los repositorios
 * tengan alcance de singleton a nivel de aplicación.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Vincula la interfaz [MessageRepository] con su implementación
     * [MessageRepositoryImpl].
     *
     * La instancia de [MessageRepositoryImpl] es proporcionada por
     * [NetworkModule.provideMessageRepository].
     *
     * @param impl Implementación concreta del repositorio de mensajes.
     * @return La interfaz del repositorio de mensajes.
     */
    @Binds
    @Singleton
    abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository

    /**
     * Vincula la interfaz [ChatRepository] con su implementación
     * [ChatRepositoryImpl].
     *
     * @param impl Implementación concreta del repositorio de chats.
     * @return La interfaz del repositorio de chats.
     */
    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    /**
     * Vincula la interfaz [ContactRepository] con su implementación
     * [ContactRepositoryImpl].
     *
     * @param impl Implementación concreta del repositorio de contactos.
     * @return La interfaz del repositorio de contactos.
     */
    @Binds
    @Singleton
    abstract fun bindContactRepository(impl: ContactRepositoryImpl): ContactRepository
}
