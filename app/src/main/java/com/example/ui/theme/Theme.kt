package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AfsLightColorScheme = lightColorScheme(
    primary = AfsCyan,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = AfsCyanDark,
    onPrimaryContainer = Color(0xFF14336B),
    secondary = AfsAmber,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = AfsAmberDark,
    onSecondaryContainer = Color(0xFF4A2E00),
    tertiary = AfsGreen,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = AfsGreenDark,
    onTertiaryContainer = Color(0xFF0B5A2E),
    error = AfsRed,
    onError = Color(0xFFFFFFFF),
    errorContainer = AfsRedDark,
    onErrorContainer = Color(0xFF7A1410),
    background = AfsBackground,
    onBackground = AfsTextPrimary,
    surface = AfsSurface,
    onSurface = AfsTextPrimary,
    surfaceVariant = AfsSurfaceVariant,
    onSurfaceVariant = AfsTextSecondary,
    outline = AfsOutline,
    outlineVariant = AfsOutlineVariant,
    surfaceContainer = AfsSurfaceContainer,
    surfaceContainerHigh = AfsSurfaceContainerHigh
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Simple, clean light utility style across every screen
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AfsLightColorScheme,
        typography = Typography,
        content = content
    )
}
