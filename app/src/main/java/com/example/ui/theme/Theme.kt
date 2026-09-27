package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AfsDarkColorScheme = darkColorScheme(
    primary = AfsCyan,
    onPrimary = Color(0xFF002229),
    primaryContainer = AfsCyanDark,
    onPrimaryContainer = Color(0xFFB8F5FF),
    secondary = AfsAmber,
    onSecondary = Color(0xFF332000),
    secondaryContainer = AfsAmberDark,
    onSecondaryContainer = Color(0xFFFFE082),
    tertiary = AfsGreen,
    onTertiary = Color(0xFF003816),
    tertiaryContainer = AfsGreenDark,
    onTertiaryContainer = Color(0xFFB9F6CA),
    error = AfsRed,
    onError = Color(0xFF450A0A),
    errorContainer = AfsRedDark,
    onErrorContainer = Color(0xFFFFCDD2),
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
    darkTheme: Boolean = true, // Force dark utility style per design requirements
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AfsDarkColorScheme,
        typography = Typography,
        content = content
    )
}
