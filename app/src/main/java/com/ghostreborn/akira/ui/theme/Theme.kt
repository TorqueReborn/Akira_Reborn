package com.ghostreborn.akira.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AkiraDarkColorScheme = darkColorScheme(
    primary = AkiraPrimaryLight,
    onPrimary = Color.White,

    primaryContainer = AkiraDarkAccentBackground,
    onPrimaryContainer = AkiraDarkTextPrimary,

    secondary = AkiraPrimary,
    onSecondary = Color.White,

    background = AkiraDarkBackground,
    onBackground = AkiraDarkTextPrimary,

    surface = AkiraDarkSurface,
    onSurface = AkiraDarkTextPrimary,

    surfaceVariant = AkiraDarkSurfaceVariant,
    onSurfaceVariant = AkiraDarkTextSecondary,

    outline = AkiraDarkSurfaceBorder,
    outlineVariant = AkiraDarkSurfaceBorder
)

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

    outline = AkiraSurfaceBorder,
    outlineVariant = AkiraSurfaceBorder
)

@Composable
fun AkiraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AkiraDarkColorScheme else AkiraLightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}