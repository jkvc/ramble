package com.ramble.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

// Unified dark palette — matches web & Electron surfaces
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF0066FF),         // Accent blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0055DD),
    secondary = Color(0xFF9933FF),       // Accent purple
    background = Color(0xFF0A0A14),      // --bg
    surface = Color(0xFF12121E),         // --surface
    surfaceVariant = Color(0xFF1A1A2E),  // --surface-hover
    surfaceContainer = Color(0xFF1A1A2E),
    onBackground = Color(0xFFF0F0F5),    // --text
    onSurface = Color(0xFFF0F0F5),
    onSurfaceVariant = Color(0xFF8888A0), // --text-muted
    onPrimaryContainer = Color(0xFFF0F0F5),
    error = Color(0xFFFF4466),           // --error
    outline = Color(0xFF2A2A3E),         // --border
    outlineVariant = Color(0xFF2A2A3E),
)

// Light palette — branded blue/purple with light backgrounds
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0066FF),         // Same accent blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0055DD),
    secondary = Color(0xFF9933FF),       // Same accent purple
    background = Color(0xFFF8F9FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F3F8),
    surfaceContainer = Color(0xFFF1F3F8),
    onBackground = Color(0xFF0A0A14),
    onSurface = Color(0xFF0A0A14),
    onSurfaceVariant = Color(0xFF6B7280),
    onPrimaryContainer = Color.White,
    error = Color(0xFFFF4466),
    outline = Color(0xFFE0E0EA),
    outlineVariant = Color(0xFFE0E0EA),
)

@Composable
fun RambleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    // Control system UI bars (status bar and navigation bar)
    val systemUiController = rememberSystemUiController()
    LaunchedEffect(darkTheme) {
        systemUiController.setStatusBarColor(
            color = Color.Transparent,
            darkIcons = !darkTheme
        )
        systemUiController.setNavigationBarColor(
            color = Color.Transparent,
            darkIcons = !darkTheme
        )
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
