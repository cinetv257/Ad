package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    secondary = NeonCyan,
    tertiary = HotPink,
    background = DarkBg,
    surface = DarkSurface,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = DarkCard,
    onSurfaceVariant = Color.White,
    outline = DarkBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark Mode for ClipForge premium styling
    dynamicColor: Boolean = false, // Disable dynamic colors to keep brand colors intact
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
