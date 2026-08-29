package com.todus.messenger

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Clase Application principal de ToDus Messenger.
 *
 * Anotada con [@HiltAndroidApp][HiltAndroidApp] para habilitar la inyección
 * de dependencias de Hilt en toda la aplicación. Hilt genera el componente
 * raíz (SingletonComponent) que gestiona las dependencias de nivel aplicación.
 *
 * Esta clase se registra en el AndroidManifest.xml mediante
 * android:name=".ToDusMessengerApp".
 *
 * Responsabilidades:
 * - Inicializar el contenedor de dependencias de Hilt.
 * - Proveer el contexto de aplicación para módulos DI (DatabaseModule, NetworkModule, etc.).
 * - Punto centralizado para inicialización global (futuro: WorkManager, Timber, etc.).
 */
@HiltAndroidApp
class ToDusMessengerApp : Application() {

    /**
     * Called when the application is starting, before any activity, service,
     * or receiver objects (excluding content providers) have been created.
     *
     * Aquí se pueden realizar inicializaciones globales que requieran el
     * contexto de la aplicación, como:
     * - Configuración de libraries de logging (Timber)
     * - Inicialización de WorkManager
     * - Configuración de Crashlytics
     */
    override fun onCreate() {
        super.onCreate()
        // Las inicializaciones globales se añadirán aquí
    }
}
