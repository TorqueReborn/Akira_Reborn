package com.ghostreborn.akira.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme

private val AkiraLightColorScheme = lightColorScheme(
    primary = AkiraPrimary,
    onPrimary = AkiraSurface,

    primaryContainer = AkiraAccentBackground,
    onPrimaryContainer = AkiraPrimaryDark,

    secondary = AkiraPrimaryLight,
    onSecondary = AkiraSurface,

    background = AkiraBackground,
    onBackground = AkiraTextPrimary,

    surface = AkiraSurface,
    onSurface = AkiraTextPrimary,

    surfaceVariant = AkiraAccentBackground,
    onSurfaceVariant = AkiraTextSecondary,

    outline = AkiraSurfaceBorder
)

@Composable
fun AkiraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AkiraLightColorScheme,
        typography = Typography,
        content = content
    )
}