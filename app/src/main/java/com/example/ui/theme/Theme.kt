package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color.Black,
    primaryContainer = CyberCyanDark,
    onPrimaryContainer = Color.White,
    secondary = CyberAmber,
    onSecondary = Color.Black,
    tertiary = CyberGreen,
    background = DarkObsidian,
    onBackground = TextPrimary,
    surface = DarkNavy,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder
)

private val LightColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color.Black,
    background = DarkObsidian,
    surface = DarkNavy,
    surfaceVariant = DarkCard
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to dark game engine theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

