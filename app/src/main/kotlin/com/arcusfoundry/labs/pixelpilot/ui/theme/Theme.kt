package com.arcusfoundry.labs.pixelpilot.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PixelPilotDarkColors = darkColorScheme(
    primary = AfLime,
    onPrimary = AfOlive,
    secondary = AfLimeHover,
    onSecondary = AfOlive,
    tertiary = AfOrange,
    background = AfOlive,
    onBackground = AfText,
    surface = AfOliveLight,
    onSurface = AfText,
    surfaceVariant = AfOliveLight,
    onSurfaceVariant = AfTextMuted
)

private val PixelPilotLightColors = lightColorScheme(
    primary = AfOlive,
    onPrimary = AfLime,
    secondary = AfOliveLight,
    onSecondary = AfLime,
    tertiary = AfOrange,
    background = AfText,
    onBackground = AfOlive,
    surface = AfText,
    onSurface = AfOlive
)

@Composable
fun PixelPilotTheme(
    // The app UI always floats on a dark scrim over the live wallpaper preview,
    // so we force the dark color scheme regardless of the device's system theme.
    // Following system theme caused label/heading text to render near-black on
    // dark scrim (primary resolved to AfOlive in light mode).
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) PixelPilotDarkColors else PixelPilotLightColors
    MaterialTheme(
        colorScheme = colors,
        typography = PixelPilotTypography,
        content = content
    )
}
