# Ramble - Android App

Voice-to-text overlay for Android, powered by Soniox real-time transcription.

## Features

### Companion App
- Enter Soniox API key (stored in EncryptedSharedPreferences)
- Transcription testing screen
- Settings and credential management

### Accessibility Overlay
- Floating pill that appears over any app
- Tap to start/stop voice recording
- Real-time transcription inserted into the focused text field
- Drag to reposition, snaps to screen edge

## Requirements

- Android 12+ (API 31)
- Microphone permission
- Accessibility service permission

## Setup

1. Open project in Android Studio
2. Sync Gradle
3. Build and install on device/emulator

## Build from Command Line

```bash
# Debug build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run unit tests
./gradlew testDebugUnitTest

# Release build (requires signing config)
./gradlew assembleRelease
```

## Enabling the Overlay

1. Open Ramble app and enter your Soniox API key
2. Go to Settings > Accessibility > Ramble
3. Enable the accessibility service
4. A floating pill will appear — tap it to start voice input in any text field

## Project Structure

```
app/src/main/kotlin/com/ramble/app/
├── RambleApp.kt          # Application class
├── MainActivity.kt       # Companion app entry
├── ui/                   # Compose screens
├── overlay/              # Accessibility service + floating pill
├── audio/                # Audio capture
├── soniox/               # WebSocket client
└── auth/                 # API key management
```

## Tech Stack

- Kotlin 2.0
- Jetpack Compose
- OkHttp (HTTP + WebSocket)
- EncryptedSharedPreferences
- Material 3
