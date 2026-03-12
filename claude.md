# Claude Rules

## Git Operations

**NEVER automatically commit or push code.** Only commit and push when the user explicitly requests it in a separate message. This applies even after completing a task or fixing an error.

## Architecture

- **No backend API routes** — the web and Android apps connect directly to Soniox using the user's own API key
- Users provide their Soniox API key on first use; it's stored in browser localStorage (web) and EncryptedSharedPreferences (Android)

## Build Validation

**Don't run `pnpm build` after every small change.** Rely on the pre-push Husky hook to catch build errors. Only run build manually when:
- Making complex or large-scale changes
- The user explicitly asks to verify the build
- Debugging a build failure

**When running build manually, use the separate output directory** to avoid interfering with the running dev server:
```bash
cd vercel
NEXT_BUILD_DIR=.next-prepush pnpm build
rm -rf .next-prepush
```

## Testing

**Vercel tests:**
```bash
cd vercel && pnpm test
```

**Android unit tests:**
```bash
cd android && ./gradlew testDebugUnitTest
```

## Avoiding Disruption to Running Servers/Apps

- **Vercel builds/tests**: Always use `NEXT_BUILD_DIR=.next-prepush` when building outside of normal dev workflow. This uses a separate output directory so a running `pnpm dev` server on port 3000 is not affected.
- **Android builds/tests**: Gradle unit tests (`testDebugUnitTest`) run in a separate process and do not affect a running Android Studio session or emulator. Do not use `assembleDebug` for verification unless explicitly asked — it can trigger reload on connected devices.
- The pre-push hook handles both Vercel and Android verification automatically with these safeguards.
