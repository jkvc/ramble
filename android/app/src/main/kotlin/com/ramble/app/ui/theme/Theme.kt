package com.ramble.app.ui.theme

import androidx.compose.material3.MaterialTheme
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
    onBackground = Color(0xFF1A1A1A),    // Dark text (matches web --foreground)
    onSurface = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFF6B7280), // Muted text (matches web --muted)
    error = Color(0xFFDC2626),           // Error red (matches web --error)
    outline = Color(0xFFE5E7EB),         // Border color (matches web --border)
    outlineVariant = Color(0xFFE5E7EB),
)

@Composable
fun RambleTheme(
    content: @Composable () -> Unit
) {
    // Use light theme to match web's minimalistic bright design
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
