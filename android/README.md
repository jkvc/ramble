# Ramble - Android App

Voice-to-text keyboard for Android, powered by Soniox real-time transcription.

## Features

### Companion App
- Login with email/password (same account as web)
- Transcription testing (like web dashboard)
- Settings and credential management

### IME Keyboard
- Record button for voice input
- Real-time transcription to any text field
- Provisional text display while speaking

## Requirements

- Android 12+ (API 31)
- Microphone permission

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

# Release build (requires signing config)
./gradlew assembleRelease
```

## Configuration

Update backend URL in `app/build.gradle.kts`:
```kotlin
buildConfigField("String", "BACKEND_URL", "\"https://your-app.vercel.app\"")
```

Or create `local.properties` with:
```properties
BACKEND_URL=https://your-app.vercel.app
```

## Enabling the Keyboard

1. Open Ramble app and log in
2. Go to Settings > System > Languages & input > On-screen keyboard
3. Enable "Ramble Voice"
4. When typing, switch to Ramble keyboard
5. Tap the microphone button to speak

## Project Structure

```
app/src/main/kotlin/com/ramble/app/
├── RambleApp.kt          # Application class
├── MainActivity.kt       # Companion app entry
├── ui/                   # Compose screens
├── ime/                  # Keyboard service
├── audio/                # Audio capture
├── soniox/               # WebSocket client
├── auth/                 # Auth management
└── network/              # API client
```

## Tech Stack

- Kotlin 2.0
- Jetpack Compose
- OkHttp (HTTP + WebSocket)
- EncryptedSharedPreferences
- Material 3
