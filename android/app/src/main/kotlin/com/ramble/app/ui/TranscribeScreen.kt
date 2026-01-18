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
import com.ramble.app.RambleApp
import com.ramble.app.audio.AudioRecorder
import com.ramble.app.soniox.SonioxWebSocketClient
import kotlinx.coroutines.launch

@Composable
fun TranscribeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var isRecording by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // Use rememberSaveable to persist transcript across navigation
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
    
    // Observe Soniox events
    LaunchedEffect(Unit) {
        sonioxClient.events.collect { event ->
            when (event) {
                is SonioxWebSocketClient.Event.Connected -> {
                    // Connection established, audio was already started
                    isConnecting = false
                    // isRecording is already true from start
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title matching Settings page style
        Text(
            text = "Transcribe",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
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
            
            // Record button row with copy (left), record (center), delete (right)
            // Evenly distributed across the full width
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Copy button (left quarter)
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
                
                // Record button (center)
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
                            // Start recording immediately
                            error = null
                            isConnecting = true
                            isRecording = true  // Show recording state immediately
                            
                            // Start buffering audio immediately
                            sonioxClient.startBuffering()
                            
                            // Start audio capture immediately (buffers while connecting)
                            audioRecorder.start { audioData ->
                                sonioxClient.sendAudio(audioData)
                            }
                            
                            // Connect to Soniox in parallel
                            scope.launch {
                                sonioxClient.connect()
                            }
                        }
                    },
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (isRecording) "Stop" else "Record",
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Delete button (right quarter)
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
            
            // Show status only when recording
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
