package com.todus.messenger.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ============================================================================
// Colores principales de la marca ToDus
// ============================================================================

/** Azul institucional de ToDus */
val TodusBlue = Color(0xFF0066CC)

/** Verde para indicar estado en línea / activo */
val TodusGreen = Color(0xFF00A86B)

/** Gris para elementos secundarios y texto tenue */
val TodusGray = Color(0xFF9E9E9E)

// ============================================================================
// Colores de burbujas de mensaje
// ============================================================================

/** Burbuja de mensaje enviado por mí (azul ToDus) */
val MessageBubbleMe = Color(0xFF0066CC)

// ============================================================================
// Esquema de colores claro (Light)
// ============================================================================

/** Esquema de colores para el tema claro de ToDus */
private val LightColorScheme = lightColorScheme(
    // Colores primarios
    primary = Color(0xFF0066CC),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D3D),

    // Colores secundarios (verde para estados online)
    secondary = Color(0xFF00A86B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFA5F5C8),
    onSecondaryContainer = Color(0xFF002111),

    // Colores terciarios
    tertiary = Color(0xFF6B5CE7),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8E0FF),
    onTertiaryContainer = Color(0xFF20005A),

    // Fondo y superficies
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF43474E),

    // Error
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    // Contorno y borde
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),

    // Inverso
    inverseSurface = Color(0xFF2F3033),
    inverseOnSurface = Color(0xFFF1F0F4),
    inversePrimary = Color(0xFFA0C9FF),

    // Burbuja de mensaje del otro usuario en tema claro
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDCD9DD),
)

// ============================================================================
// Esquema de colores oscuro (Dark)
// ============================================================================

/** Esquema de colores para el tema oscuro de ToDus */
private val DarkColorScheme = darkColorScheme(
    // Colores primarios
    primary = Color(0xFFA0C9FF),
    onPrimary = Color(0xFF00325E),
    primaryContainer = Color(0xFF004A87),
    onPrimaryContainer = Color(0xFFD1E4FF),

    // Colores secundarios
    secondary = Color(0xFF80D8A8),
    onSecondary = Color(0xFF003822),
    secondaryContainer = Color(0xFF005234),
    onSecondaryContainer = Color(0xFFA5F5C8),

    // Colores terciarios
    tertiary = Color(0xFFCFC1FF),
    onTertiary = Color(0xFF36137A),
    tertiaryContainer = Color(0xFF4F3DA0),
    onTertiaryContainer = Color(0xFFE8E0FF),

    // Fondo y superficies
    background = Color(0xFF121212),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC4C6D0),

    // Error
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    // Contorno y borde
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF43474E),

    // Inverso
    inverseSurface = Color(0xFFE3E2E6),
    inverseOnSurface = Color(0xFF2F3033),
    inversePrimary = Color(0xFF0066CC),

    // Superficies adicionales
    surfaceBright = Color(0xFF3B3A3E),
    surfaceDim = Color(0xFF1E1E1E),
)

// ============================================================================
// Shapes (Formas)
// ============================================================================

/** Forma redondeada para las burbujas de mensaje */
val MessageBubbleShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)

// ============================================================================
// Composable principal del tema ToDus
// ============================================================================

/**
 * Tema principal de la aplicación ToDus Messenger.
 *
 * Soporta temas claro y oscuro, además de colores dinámicos en dispositivos
 * con Android 12+ (API 31+). Los colores dinámicos se generan a partir del
 * fondo de pantalla del dispositivo y se mezclan con la paleta ToDus cuando
 * están disponibles.
 *
 * @param darkTheme Si true, se utiliza el tema oscuro. Por defecto sigue
 *   la configuración del sistema.
 * @param dynamicColor Si true y el dispositivo lo soporta (Android 12+),
 *   se utilizan colores dinámicos del sistema. Por defecto true.
 * @param content El contenido composable envuelto por el tema.
 */
@Composable
fun ToDusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // Determinar el esquema de colores según la configuración
    val colorScheme = when {
        // Colores dinámicos solo disponibles en Android 12+
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        // Tema oscuro con paleta ToDus
        darkTheme -> DarkColorScheme
        // Tema claro con paleta ToDus
        else -> LightColorScheme
    }

    // Sincronizar la barra de estado con el tema
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            // Asegurar que el contenido se dibuje detrás de las barras del sistema
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TodusTypography,
        shapes = androidx.compose.material3.Shapes(
            extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
            small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        ),
        content = content
    )
}

/**
 * Obtiene el color de burbuja de mensaje según el tema actual.
 *
 * @param isDarkTheme Si el tema actual es oscuro.
 * @param isMessageFromMe Si el mensaje fue enviado por mí.
 * @return El color de fondo para la burbuja del mensaje.
 */
@Composable
fun messageBubbleColor(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    isMessageFromMe: Boolean
): Color {
    return if (isMessageFromMe) {
        MessageBubbleMe
    } else {
        // Burbuja del otro usuario: blanco en claro, gris oscuro en oscuro
        if (isDarkTheme) {
            Color(0xFF2A2A2A)
        } else {
            Color(0xFFFFFFFF)
        }
    }
}
