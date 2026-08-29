package com.todus.messenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.todus.messenger.ui.navigation.ToDusNavGraph
import com.todus.messenger.ui.theme.ToDusTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Actividad principal de ToDus Messenger.
 *
 * Anotada con [@AndroidEntryPoint][AndroidEntryPoint] para que Hilt pueda
 * inyectar dependencias en esta actividad y en cualquier ViewModel
 * asociado a los composables que se rendericen dentro de ella.
 *
 * Configuración:
 * - Habilita el modo edge-to-edge (dibujo detrás de las barras del sistema).
 * - Aplica el tema ToDus (claro/oscuro + colores dinámicos en Android 12+).
 * - Establece el grafo de navegación principal como contenido raíz.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Habilitar dibujo edge-to-edge para aprovechar toda la pantalla
        enableEdgeToEdge()

        setContent {
            // Envolver toda la UI en el tema ToDus
            ToDusTheme {
                // Superficie base que respeta el esquema de colores del tema
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Controlador de navegación recordado a través de recomposiciones
                    val navController = rememberNavController()

                    // Grafo de navegación principal de la aplicación
                    ToDusNavGraph(navController = navController)
                }
            }
        }
    }
}
