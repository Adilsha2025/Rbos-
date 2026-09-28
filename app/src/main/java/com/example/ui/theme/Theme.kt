package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GreenPrimaryDarkTheme,
    onPrimary = Color(0xFF052E16),
    primaryContainer = GreenContainerDark,
    onPrimaryContainer = Color(0xFFBBF7D0),
    secondary = GoldAccent,
    onSecondary = Color.Black,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = TextSecondaryDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    error = DangerRed,
    errorContainer = DangerContainer,
    onErrorContainer = DangerOnContainer
)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenPrimaryContainer,
    onPrimaryContainer = GreenOnPrimaryContainer,
    secondary = GoldAccent,
    onSecondary = Color.White,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondaryLight,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    error = DangerRed,
    errorContainer = DangerContainer,
    onErrorContainer = DangerOnContainer,
    outline = BorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
