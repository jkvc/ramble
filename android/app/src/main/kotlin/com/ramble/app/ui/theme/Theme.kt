package com.ramble.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6366F1),       // Accent purple
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4F46E5),
    secondary = Color(0xFF818CF8),
    background = Color(0xFF0A0A0B),     // Dark background
    surface = Color(0xFF18181B),        // Surface/card
    surfaceVariant = Color(0xFF27272A), // Border color
    onBackground = Color(0xFFFAFAFA),   // Text
    onSurface = Color(0xFFFAFAFA),
    onSurfaceVariant = Color(0xFFA1A1AA), // Muted text
    error = Color(0xFFEF4444),
    outline = Color(0xFF3F3F46),
)

@Composable
fun RambleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Always use dark theme to match web
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
