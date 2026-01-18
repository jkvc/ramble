package com.ramble.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ramble.app.RambleApp
import com.ramble.app.audio.AudioRecorder
import com.ramble.app.soniox.SonioxWebSocketClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscribeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var isRecording by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var transcript by remember { mutableStateOf("") }
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
    
    // Observe Soniox events
    LaunchedEffect(Unit) {
        sonioxClient.events.collect { event ->
            when (event) {
                is SonioxWebSocketClient.Event.Connected -> {
                    isConnecting = false
                    isRecording = true
                    // Start audio capture
                    audioRecorder.start { audioData ->
                        sonioxClient.sendAudio(audioData)
                    }
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
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            sonioxClient.disconnect()
            audioRecorder.stop()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Transcribe",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Transcript display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
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
            
            // Error display
            error?.let { errorMessage ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
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
            
            // Record button
            Button(
                onClick = {
                    if (!hasPermission) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        return@Button
                    }
                    
                    if (isRecording || isConnecting) {
                        // Stop recording
                        sonioxClient.disconnect()
                        audioRecorder.stop()
                        isRecording = false
                        isConnecting = false
                    } else {
                        // Start recording
                        error = null
                        isConnecting = true
                        scope.launch {
                            sonioxClient.connect()
                        }
                    }
                },
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                ),
                enabled = !isConnecting
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (isRecording) "Stop" else "Record",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when {
                    isConnecting -> "Connecting..."
                    isRecording -> "Recording... tap to stop"
                    else -> "Tap to start recording"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Clear button
            if (transcript.isNotEmpty()) {
                TextButton(
                    onClick = {
                        transcript = ""
                        provisional = ""
                    }
                ) {
                    Text("Clear transcript")
                }
            }
        }
    }
}
