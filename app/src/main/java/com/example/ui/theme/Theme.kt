package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NousDarkColorScheme = darkColorScheme(
    primary = NousCyanNeon,
    onPrimary = NousObsidianDark,
    primaryContainer = NousSurfaceVariant,
    onPrimaryContainer = NousCyanGlow,
    secondary = NousCyanGlow,
    onSecondary = NousObsidianDark,
    secondaryContainer = NousSurfaceVariant,
    onSecondaryContainer = NousTextPrimary,
    background = NousObsidianDark,
    onBackground = NousTextPrimary,
    surface = NousSurfaceDark,
    onSurface = NousTextPrimary,
    surfaceVariant = NousSurfaceVariant,
    onSurfaceVariant = NousTextSecondary,
    error = NousRedAlert,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Enforce high-tech HUD aesthetic
    MaterialTheme(
        colorScheme = NousDarkColorScheme,
        typography = Typography,
        content = content
    )
}
