# Ramble

Voice-to-text transcription app (web + Android). Users provide their own Soniox API key — there is no backend authentication or user management. Clients connect directly to Soniox's WebSocket API.

## Architecture

- **vercel/**: Next.js web app. Single-page app with API key stored in localStorage. No backend API routes.
- **android/**: Kotlin/Compose app with IME keyboard. API key stored in EncryptedSharedPreferences.
- Both connect directly to `wss://stt-rt.soniox.com/transcribe-websocket` using the user's Soniox API key.

## Running

- **Web dev server**: `cd vercel && pnpm dev` (runs on port 3000)
- **Android**: Open `android/` in Android Studio

## Testing

- **Web**: `cd vercel && pnpm test` (vitest)
- **Android**: `cd android && ./gradlew testDebugUnitTest`

## Building

- **Web**: `cd vercel && pnpm build`
- **Android**: `cd android && ./gradlew assembleDebug`

## Pre-push hook

The `.husky/pre-push` hook runs builds and tests before push. It is designed to not disrupt running dev servers:

- **Vercel**: Uses `NEXT_BUILD_DIR=.next-prepush` to build into a separate directory, avoiding interference with a running `pnpm dev` server on port 3000.
- **Android**: Uses `./gradlew testDebugUnitTest --daemon` which runs unit tests only (no device needed) and does not affect a running Android Studio instance or emulator.

When running builds or tests manually (e.g., for verification), follow the same pattern:
- For Vercel: always use `NEXT_BUILD_DIR=.next-prepush pnpm build` if a dev server might be running, then `rm -rf .next-prepush` after.
- For Android: `./gradlew testDebugUnitTest` is safe to run alongside Android Studio.
