package com.ramble.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light theme colors matching web app's minimalistic bright theme
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),        // Blue accent (matches web --accent)
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1D4ED8), // Darker blue for hover
    secondary = Color(0xFF6B7280),       // Muted gray
    background = Color(0xFFFFFFFF),      // White background (matches web --background)
    surface = Color(0xFFF8F9FA),         // Light gray surface (matches web --surface)
    surfaceVariant = Color(0xFFF1F3F5),  // Surface hover
    surfaceContainer = Color(0xFFF1F3F5),
    onBackground = Color(0xFF1A1A1A),    // Dark text (matches web --foreground)
    onSurface = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFF6B7280), // Muted text (matches web --muted)
    error = Color(0xFFDC2626),           // Error red (matches web --error)
    outline = Color(0xFFE5E7EB),         // Border color (matches web --border)
    outlineVariant = Color(0xFFE5E7EB),
)

// Dark theme colors
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),         // Lighter blue for dark mode
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF3B82F6),
    secondary = Color(0xFF9CA3AF),       // Lighter muted gray
    background = Color(0xFF121212),      // Dark background
    surface = Color(0xFF1E1E1E),         // Dark surface
    surfaceVariant = Color(0xFF2A2A2A),  // Slightly lighter surface
    surfaceContainer = Color(0xFF2A2A2A),
    onBackground = Color(0xFFE5E7EB),    // Light text
    onSurface = Color(0xFFE5E7EB),
    onSurfaceVariant = Color(0xFF9CA3AF), // Muted light text
    error = Color(0xFFF87171),           // Lighter error red
    outline = Color(0xFF3F3F3F),         // Dark border
    outlineVariant = Color(0xFF3F3F3F),
)

@Composable
fun RambleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
