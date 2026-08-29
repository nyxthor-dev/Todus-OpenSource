package com.todus.messenger.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Sistema tipográfico de ToDus Messenger.
 *
 * Utiliza la familia de fuentes por defecto del sistema (Roboto en la mayoría
 * de dispositivos Android) para garantizar consistencia visual y rendimiento.
 *
 * Escala definida según las necesidades de la aplicación de mensajería:
 * - headlineLarge: Títulos principales de pantallas (24sp)
 * - titleLarge: Títulos de secciones (20sp)
 * - titleMedium: Nombres de contacto, encabezados de chat (16sp, semibold)
 * - bodyLarge: Texto del cuerpo, mensajes principales (16sp)
 * - bodyMedium: Texto secundario, previews de mensajes (14sp)
 * - labelSmall: Timestamps, contadores, texto auxiliar (12sp)
 */
val TodusTypography = Typography(
    // ------------------------------------------------------------------------
    // Headlines: Títulos de pantalla y contenido destacado
    // ------------------------------------------------------------------------

    /** Título principal de pantalla, usado en cabeceras y pantallas vacías */
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),

    /** Título secundario de pantalla */
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),

    /** Título pequeño de pantalla o énfasis en secciones */
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
    ),

    // ------------------------------------------------------------------------
    // Titles: Títulos de secciones, tarjetas y elementos de lista
    // ------------------------------------------------------------------------

    /** Título grande para secciones principales y nombres de contacto */
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
    ),

    /** Título medio para encabezados de chat y nombres en lista de chats */
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp,
    ),

    /** Título pequeño para subsecciones y elementos compactos */
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),

    // ------------------------------------------------------------------------
    // Body: Texto principal del contenido
    // ------------------------------------------------------------------------

    /** Texto grande del cuerpo, usado para mensajes principales y contenido */
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.5.sp,
    ),

    /** Texto medio del cuerpo, usado para previews de mensajes y descripciones */
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),

    /** Texto pequeño del cuerpo, usado para información detallada */
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),

    // ------------------------------------------------------------------------
    // Labels: Texto auxiliar, timestamps, contadores
    // ------------------------------------------------------------------------

    /** Etiqueta grande para botones y etiquetas prominentes */
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),

    /** Etiqueta media para badges y tags */
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),

    /** Etiqueta pequeña para timestamps, contadores de mensajes y texto auxiliar */
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),

    // ------------------------------------------------------------------------
    // Display: Texto decorativo y de gran énfasis
    // ------------------------------------------------------------------------

    /** Texto de display grande para splash o pantallas de bienvenida */
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),

    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
    ),

    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
    ),
)