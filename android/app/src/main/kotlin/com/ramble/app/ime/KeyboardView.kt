package com.ramble.app.ime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
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
import androidx.compose.ui.unit.sp

/**
 * Keyboard view with QWERTY layout and voice input toggle.
 * Voice toggle is positioned in the top right corner.
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
    var isShiftEnabled by remember { mutableStateOf(false) }
    
    val row1Keys = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val row2Keys = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    val row3Keys = listOf("z", "x", "c", "v", "b", "n", "m")
    
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            // Top bar with settings and voice toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Settings button (left)
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Status text or error (center)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    error?.let { errorMessage ->
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    } ?: run {
                        if (!isLoggedIn) {
                            Text(
                                text = "Log in to use voice",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else if (isRecording) {
                            Text(
                                text = "Listening...",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Voice toggle button (right)
                Box(
                    modifier = Modifier
                        .size(36.dp)
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
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                            contentDescription = if (isRecording) "Stop" else "Voice input",
                            modifier = Modifier.size(18.dp),
                            tint = if (!isLoggedIn) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Row 1: Q W E R T Y U I O P
            KeyboardRow(
                keys = row1Keys,
                isShiftEnabled = isShiftEnabled,
                onKeyClick = { key ->
                    onKeyClick(if (isShiftEnabled) key.uppercase() else key)
                    isShiftEnabled = false
                }
            )
            
            // Row 2: A S D F G H J K L
            KeyboardRow(
                keys = row2Keys,
                isShiftEnabled = isShiftEnabled,
                onKeyClick = { key ->
                    onKeyClick(if (isShiftEnabled) key.uppercase() else key)
                    isShiftEnabled = false
                },
                horizontalPadding = 20.dp
            )
            
            // Row 3: Shift Z X C V B N M Backspace
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Shift key
                SpecialKey(
                    text = "⇧",
                    modifier = Modifier.weight(1.5f),
                    isActive = isShiftEnabled,
                    onClick = { isShiftEnabled = !isShiftEnabled }
                )
                
                // Letter keys
                row3Keys.forEach { key ->
                    KeyButton(
                        key = if (isShiftEnabled) key.uppercase() else key,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onKeyClick(if (isShiftEnabled) key.uppercase() else key)
                            isShiftEnabled = false
                        }
                    )
                }
                
                // Backspace key
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onBackspace() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // Row 4: Numbers toggle, comma, space, period, enter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Number/symbol toggle
                SpecialKey(
                    text = "123",
                    modifier = Modifier.weight(1.2f),
                    onClick = { /* TODO: Toggle number/symbol mode */ }
                )
                
                // Comma
                KeyButton(
                    key = ",",
                    modifier = Modifier.weight(0.8f),
                    onClick = { onKeyClick(",") }
                )
                
                // Space bar
                Box(
                    modifier = Modifier
                        .weight(4f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onSpace() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "space",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                
                // Period
                KeyButton(
                    key = ".",
                    modifier = Modifier.weight(0.8f),
                    onClick = { onKeyClick(".") }
                )
                
                // Enter key
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onEnter() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                        contentDescription = "Enter",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyboardRow(
    keys: List<String>,
    isShiftEnabled: Boolean,
    onKeyClick: (String) -> Unit,
    horizontalPadding: androidx.compose.ui.unit.Dp = 4.dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keys.forEach { key ->
            KeyButton(
                key = if (isShiftEnabled) key.uppercase() else key,
                modifier = Modifier.weight(1f),
                onClick = { onKeyClick(key) }
            )
        }
    }
}

@Composable
private fun KeyButton(
    key: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = key,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun SpecialKey(
    text: String,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
