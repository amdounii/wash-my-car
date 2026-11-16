package com.washmycar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Couleurs de la marque (bleu « eau ») et couleurs de statut partagées par les écrans. */
object WmcColors {
    val Brand = Color(0xFF0369A1)
    val BrandLight = Color(0xFF38BDF8)
    val Success = Color(0xFF15803D)
    val Warning = Color(0xFFB45309)
    val Danger = Color(0xFFB91C1C)
    val Neutral = Color(0xFF64748B)
    val Star = Color(0xFFF59E0B)
}

private val LightColors = lightColorScheme(
    primary = WmcColors.Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0C4A6E),
    secondary = Color(0xFF0E7490),
    secondaryContainer = Color(0xFFCFFAFE),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    error = WmcColors.Danger,
)

private val DarkColors = darkColorScheme(
    primary = WmcColors.BrandLight,
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF075985),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFF67E8F9),
    error = Color(0xFFF87171),
)

@Composable
fun WashMyCarTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
