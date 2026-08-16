package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ElegantDarkColorScheme = darkColorScheme(
    primary = ElegantPrimaryLavender,
    onPrimary = ElegantOnPrimaryDark,
    primaryContainer = ElegantSurfaceVariant,
    onPrimaryContainer = ElegantTextPrimary,
    secondary = ElegantSecondaryLavender,
    onSecondary = ElegantOnPrimaryDark,
    tertiary = ElegantPrimaryLavender,
    background = ElegantDarkCanvas,
    onBackground = ElegantTextPrimary,
    surface = ElegantSurface,
    onSurface = ElegantTextPrimary,
    surfaceVariant = ElegantSurfaceVariant,
    onSurfaceVariant = ElegantTextSecondary,
    outline = ElegantBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ElegantDarkColorScheme,
        typography = Typography,
        content = content
    )
}

