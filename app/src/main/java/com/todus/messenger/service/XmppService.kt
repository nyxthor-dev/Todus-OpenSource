package com.todus.messenger.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.todus.messenger.data.remote.xmpp.ToDusXmppClient
import com.todus.messenger.domain.model.ConnectionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Servicio en segundo plano que mantiene la conexión XMPP con ToDus activa.
 *
 * Se ejecuta como Foreground Service para que la conexión no sea
 * interrumpida por Android cuando la app está en segundo plano.
 *
 * Ciclo de vida:
 * 1. onStartCommand → conectar con credenciales guardadas
 * 2. Mantener conexión viva con reconnection automática
 * 3. onDestroy → desconectar limpiamente
 */
@AndroidEntryPoint
class XmppService : Service() {

    @Inject lateinit var xmppClient: ToDusXmppClient

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        /** Acción para iniciar el servicio con credenciales */
        const val ACTION_CONNECT = "com.todus.messenger.action.CONNECT"
        /** Acción para desconectar */
        const val ACTION_DISCONNECT = "com.todus.messenger.action.DISCONNECT"
        /** Extra: número de teléfono del usuario */
        const val EXTRA_PHONE = "extra_phone"
        /** Extra: contraseña del usuario */
        const val EXTRA_PASSWORD = "extra_password"
    }

    /** Observa el estado de la conexión XMPP */
    val connectionState: StateFlow<ConnectionState>
        get() = xmppClient.connectionState

    override fun onCreate() {
        super.onCreate()
        // Las notificaciones foreground se agregarán con el sistema completo
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val phone = intent.getStringExtra(EXTRA_PHONE) ?: return START_NOT_STICKY
                val password = intent.getStringExtra(EXTRA_PASSWORD) ?: return START_NOT_STICKY

                serviceScope.launch {
                    xmppClient.connect(phone, password)
                }
            }
            ACTION_DISCONNECT -> {
                serviceScope.launch {
                    xmppClient.disconnect()
                }
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
