package com.eventfinder.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Light colour scheme. Every role is mapped explicitly rather than letting
 * Material derive tints from the seed colours, so containers and "on" colours
 * keep a consistent, brand-correct contrast across the app.
 */
private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB6F3DF),
    onPrimaryContainer = Color(0xFF0B3D2E),
    // Amber is too light for white text, so the "on" colour is a dark brown.
    secondary = Amber,
    onSecondary = Color(0xFF3B2F00),
    secondaryContainer = Color(0xFFFFECB3),
    onSecondaryContainer = Color(0xFF4A3B00),
    tertiary = Navy,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD6E3FF),
    onTertiaryContainer = Color(0xFF0A1F3D),
    background = OffWhite,
    onBackground = Charcoal,
    surface = Color.White,
    onSurface = Charcoal,
    surfaceVariant = Color(0xFFE9ECF0),
    onSurfaceVariant = Color(0xFF44474C),
    outline = Color(0xFFB9BDC4),
    outlineVariant = Color(0xFFD6D9DE),
    error = Red,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

/** Dark scheme mirror of [LightColors], tuned for legibility on dark surfaces. */
private val DarkColors = darkColorScheme(
    primary = Color(0xFF3ED9AE),
    onPrimary = Color(0xFF0B3D2E),
    primaryContainer = Color(0xFF0B5C45),
    onPrimaryContainer = Color(0xFFB6F3DF),
    secondary = Color(0xFFFFCA3D),
    onSecondary = Color(0xFF3B2F00),
    secondaryContainer = Color(0xFF5C4700),
    onSecondaryContainer = Color(0xFFFFE7A3),
    tertiary = Color(0xFF8FB6FF),
    onTertiary = Color(0xFF0A1F3D),
    tertiaryContainer = Color(0xFF274063),
    onTertiaryContainer = Color(0xFFD6E3FF),
    background = DarkSurface,
    onBackground = Color(0xFFE3E5E9),
    surface = DarkSurfaceHigh,
    onSurface = Color(0xFFE3E5E9),
    surfaceVariant = Color(0xFF2A2E34),
    onSurfaceVariant = Color(0xFFC2C6CD),
    outline = Color(0xFF565A61),
    outlineVariant = Color(0xFF3A3E44),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3A0000),
    errorContainer = Color(0xFF930006),
    onErrorContainer = Color(0xFFFFDAD6)
)

/**
 * Consistent corner rounding for buttons, chips, text fields, cards and dialogs.
 * Components that do not override their own shape pick these up automatically,
 * giving the whole UI a single, softer visual language.
 */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Material 3 theme. Supports light/dark following the system setting.
 */
@Composable
fun EventFinderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        shapes = AppShapes,
        content = content
    )
}
