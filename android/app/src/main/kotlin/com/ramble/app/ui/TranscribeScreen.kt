package com.ramble.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.ramble.app.RambleApp
import com.ramble.app.audio.AudioRecorder
import com.ramble.app.overlay.RambleAccessibilityService
import com.ramble.app.soniox.SonioxWebSocketClient
import kotlinx.coroutines.launch

@Composable
fun TranscribeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isRecording by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var transcript by rememberSaveable { mutableStateOf("") }
    var provisional by remember { mutableStateOf("") }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            error = "Microphone permission is required"
        }
    }

    val sonioxClient = remember { SonioxWebSocketClient() }
    val audioRecorder = remember { AudioRecorder() }

    LaunchedEffect(Unit) {
        sonioxClient.events.collect { event ->
            when (event) {
                is SonioxWebSocketClient.Event.Connected -> {
                    isConnecting = false
                }
                is SonioxWebSocketClient.Event.FinalWords -> {
                    transcript += event.text
                    provisional = ""
                }
                is SonioxWebSocketClient.Event.ProvisionalWords -> {
                    provisional = event.text
                }
                is SonioxWebSocketClient.Event.Error -> {
                    error = event.message
                    isRecording = false
                    isConnecting = false
                    audioRecorder.stop()
                }
                is SonioxWebSocketClient.Event.Disconnected -> {
                    isRecording = false
                    isConnecting = false
                    audioRecorder.stop()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            sonioxClient.disconnect()
            audioRecorder.stop()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Transcribe",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        val serviceRunning by RambleAccessibilityService.isRunning.collectAsState()

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (serviceRunning)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (serviceRunning) {
                    Text(
                        text = "Floating button active",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable floating button",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Voice input in any app",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        },
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Enable")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (transcript.isEmpty() && provisional.isEmpty() && !isRecording) {
                    Text(
                        text = "Press the microphone button to start speaking",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = buildAnnotatedString {
                            append(transcript)
                            if (provisional.isNotEmpty()) {
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                    append(provisional)
                                }
                            }
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

            Spacer(modifier = Modifier.height(16.dp))

            error?.let { errorMessage ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (transcript.isNotEmpty()) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Transcript", transcript)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = transcript.isNotEmpty(),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy",
                        tint = if (transcript.isNotEmpty())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }

                // Gradient record button with glow pulse when recording
                val infiniteTransition = rememberInfiniteTransition(label = "glow")
                val glowAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 0.7f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = EaseInOut),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "glowAlpha"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(80.dp)
                ) {
                    // Glow ring when recording
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .shadow(
                                    elevation = (12 * glowAlpha).dp,
                                    shape = CircleShape,
                                    ambientColor = Color(0xFF0066FF).copy(alpha = glowAlpha),
                                    spotColor = Color(0xFF9933FF).copy(alpha = glowAlpha)
                                )
                        )
                    }
                    Button(
                        onClick = {
                            if (!hasPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@Button
                            }

                            if (isRecording || isConnecting) {
                                sonioxClient.disconnect()
                                audioRecorder.stop()
                                isRecording = false
                                isConnecting = false
                            } else {
                                val apiKey = RambleApp.instance.apiKeyManager.apiKey.value
                                if (apiKey == null) {
                                    error = "Please set your API key in Settings"
                                    return@Button
                                }

                                error = null
                                isConnecting = true
                                isRecording = true

                                sonioxClient.startBuffering()

                                audioRecorder.start { audioData ->
                                    sonioxClient.sendAudio(audioData)
                                }

                                scope.launch {
                                    sonioxClient.connect(apiKey)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF0066FF), Color(0xFF9933FF))
                                ),
                                shape = CircleShape
                            ),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        )
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                            contentDescription = if (isRecording) "Stop" else "Record",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                }

                IconButton(
                    onClick = {
                        transcript = ""
                        provisional = ""
                    },
                    enabled = transcript.isNotEmpty(),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Clear",
                        tint = if (transcript.isNotEmpty())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }

            if (isRecording) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isConnecting) "Listening... (connecting)" else "Listening...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
}
