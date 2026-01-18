package com.ramble.app.ime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Note: Color import kept for Color.White used in icons

/**
 * Minimal voice input keyboard view similar to Google's native voice input.
 * - Settings button on the left
 * - Large record toggle in the center
 * - Backspace button on the right
 */
@Composable
fun KeyboardView(
    isRecording: Boolean,
    isConnecting: Boolean,
    error: String?,
    isLoggedIn: Boolean,
    onRecordClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onKeyClick: (String) -> Unit = {},
    onBackspace: () -> Unit = {},
    onEnter: () -> Unit = {},
    onSpace: () -> Unit = {}
) {
    // Use surfaceVariant for keyboard background (matches system keyboard)
    val keyboardBackground = MaterialTheme.colorScheme.surfaceVariant
    // Dark gray for side button icons
    val sideIconColor = MaterialTheme.colorScheme.onSurfaceVariant
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = keyboardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding() // Account for system navigation bar
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Error or status text at the top
            error?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } ?: run {
                if (!isLoggedIn) {
                    Text(
                        text = "Log in via Ramble app to use voice",
                        color = sideIconColor,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else if (isRecording) {
                    Text(
                        text = "Listening...",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
            
            // Main row: Settings | Record | Backspace
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Settings button (left) - no background
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onSettingsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = sideIconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Record toggle button (center) - larger
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isRecording -> MaterialTheme.colorScheme.error
                                !isLoggedIn -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                        .clickable(enabled = !isConnecting) { onRecordClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                            contentDescription = if (isRecording) "Stop" else "Voice input",
                            modifier = Modifier.size(32.dp),
                            tint = if (!isLoggedIn) sideIconColor.copy(alpha = 0.5f) else Color.White
                        )
                    }
                }
                
                // Backspace button (right) - no background
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onBackspace() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = sideIconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
