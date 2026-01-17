# Ramble: Voice-to-Text Android Keyboard

---

## Quick Start (Phase 1 - Vercel Backend)

### Prerequisites
- Soniox API key (you have this)
- Supabase account (free tier works)
- Vercel account (free tier works)
- pnpm installed locally

### Step 1: Set up Supabase
1. Go to [supabase.com](https://supabase.com) and create a new project
2. Go to **SQL Editor** and run the contents of `vercel/supabase/schema.sql`
3. Go to **Settings > API** and copy:
   - Project URL (`https://xxx.supabase.co`)
   - `anon` public key
   - `service_role` secret key

### Step 2: Set up Vercel
1. Push this repo to GitHub
2. Go to [vercel.com](https://vercel.com) and import the repo
3. Set the **Root Directory** to `vercel`
4. Add environment variables in Vercel dashboard:
   ```
   SONIOX_API_KEY=your_soniox_api_key
   NEXT_PUBLIC_SUPABASE_URL=https://your-project.supabase.co
   NEXT_PUBLIC_SUPABASE_ANON_KEY=your_anon_key
   SUPABASE_SERVICE_ROLE_KEY=your_service_role_key
   ADMIN_EMAILS=your-email@example.com
   ```
5. Deploy!

### Step 3: Local Development
```bash
cd vercel
pnpm install
vercel env pull .env.local  # Pull env vars from Vercel
pnpm dev
```

### Step 4: Test Admin Access
1. Go to `/signup` and create an account with an email in `ADMIN_EMAILS`
2. Verify your email
3. Go to `/login` and sign in
4. Navigate to `/admin` → you'll have admin access
5. Create a voucher in `/admin/vouchers`

### Step 5: Test User Flow
1. Go to `/signup` and create a test account
2. Go to `/dashboard/redeem` and enter a voucher code
3. Go to `/dashboard` and click the record button
4. Speak → see real-time transcription!

---

## Project Overview

**Ramble** is an Android Input Method Editor (IME) keyboard that enables real-time speech-to-text transcription via the Soniox API. Users can speak into their phone and see text appear in real-time in any text input field, interleaved with manual typing.

### Core Features
- **Real-time transcription**: Stream audio directly to Soniox, receive provisional and final tokens
- **Interleaved input**: Mix speech and typing seamlessly
- **Subscription-based**: Monthly subscription via Google Play Billing
- **Secure architecture**: API keys never exposed to client; temporary tokens issued by backend

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              ARCHITECTURE                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐         ┌─────────────────┐         ┌───────────────┐ │
│  │  Android IME    │◄───────►│  Vercel Backend │◄───────►│   Supabase    │ │
│  │  (Keyboard App) │         │  (Next.js API)  │         │  (Auth + DB)  │ │
│  └────────┬────────┘         └────────┬────────┘         └───────────────┘ │
│           │                           │                                     │
│           │ 1. Request temp token     │ 2. Verify user, subscription        │
│           │    (authenticated)        │    → Issue temp Soniox key          │
│           │                           │                                     │
│           ▼                           │                                     │
│  ┌─────────────────┐                  │         ┌─────────────────┐         │
│  │  WebSocket      │──────────────────┴────────►│  Soniox API     │         │
│  │  (Direct Stream)│◄────────────────────────────  (Real-time STT)│         │
│  └─────────────────┘         Audio + Tokens     └─────────────────┘         │
│                              (NO PROXY!)                                    │
│                                                                             │
│  ┌─────────────────┐                                                        │
│  │ Google Play     │─────► Subscription verification via Vercel            │
│  │ Billing         │                                                        │
│  └─────────────────┘                                                        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Key Design Decisions

1. **No audio proxy**: Audio streams directly from client → Soniox (lowest latency)
2. **Temporary tokens**: Backend issues short-lived Soniox credentials to authenticated users
3. **Vercel-first development**: Build and test the entire backend + Soniox integration in browser before touching Android
4. **Supabase for auth/data**: User management, subscription state, usage tracking

---

## Technology Stack

| Component | Technology | Purpose |
|-----------|------------|---------|
| **Backend** | Next.js on Vercel | API routes, admin portal, test platform |
| **Database** | Supabase (PostgreSQL) | Users, subscriptions, usage logs |
| **Auth** | Supabase Auth | Email/password authentication |
| **Speech-to-Text** | Soniox Real-time API | WebSocket streaming transcription |
| **Android App** | Kotlin + InputMethodService | Custom keyboard IME |
| **Payments** | Google Play Billing | In-app subscriptions |

---

## Development Phases

### Phase 1: Vercel Backend + Admin Portal + Test Platform
**Duration: 2-3 weeks**

Build the complete backend infrastructure and a browser-based test platform to validate Soniox integration before any Android development.

#### 1.1 Project Setup ✅
- [x] Initialize Next.js project in `/vercel`
- [x] Configure Vercel deployment
- [x] Set up environment variables
- [x] Install dependencies (Supabase client, etc.)

#### 1.2 Supabase Setup ✅
- [x] Create Supabase project
- [x] Design database schema:
  - `profiles` (extends auth.users)
  - `subscriptions` (user_id, status, expires_at, purchase_token)
  - `vouchers` (code, description, max_redemptions, is_active)
  - `voucher_redemptions` (user_id, voucher_id)
  - `usage_logs` (user_id, session_id, duration_seconds, created_at)
- [x] Configure Row Level Security (RLS) policies
- [x] Set up auth providers (email/password + magic link)

#### 1.3 API Routes ✅
- [x] `POST /api/auth/signup` - User registration (via Supabase client)
- [x] `POST /api/auth/login` - User login (via Supabase client)
- [x] `POST /api/auth/logout` - User logout
- [x] `GET /api/auth/me` - Get current user
- [x] `POST /api/soniox/token` - Issue Soniox credentials (authenticated + access check)
- [x] `POST /api/vouchers/redeem` - Redeem voucher code
- [x] `GET/POST /api/admin/vouchers` - Admin voucher management
- [ ] `POST /api/billing/verify` - Verify Google Play purchase (Phase 3)
- [ ] `POST /api/billing/webhook` - Google Play RTDN webhook (Phase 3)

#### 1.4 Soniox Test Platform ✅
Browser-based testing page to validate the entire Soniox flow:

- [x] **Test page at `/admin/transcribe` and `/dashboard`**:
  - Record button (uses browser `MediaRecorder` / `getUserMedia`)
  - Text output area showing real-time transcription
  - Status indicators (connecting, streaming, error states)
  - Endpoint detection (visual markers for sentence boundaries)
  
- [x] **WebSocket integration**:
  - Connect to Soniox WebSocket API (`wss://stt-rt.soniox.com/transcribe-websocket`)
  - Stream audio (WebM Opus via MediaRecorder + `audio_format: "auto"`)
  - Handle non-final tokens (`nfw`) - display provisional text in gray
  - Handle final tokens (`fw`) - commit to output in white
  - Multi-language support (en, zh, es, fr, de, ja, ko)
  - Model: `stt-rt-v3`
  
- [ ] **Interleave testing** (deferred to Android phase):
  - Typing in text field while speaking
  - Cursor position management
  - Merging typed and spoken text

This test platform serves as:
1. ✅ Proof that Soniox integration works
2. ✅ Reference implementation for Android
3. ✅ Debugging tool during Android development

#### 1.5 Admin Portal ✅
- [x] **Dashboard page `/admin`**:
  - Total users count
  - Active vouchers count
  - Quick links to management pages
  
- [x] **Users page `/admin/users`**:
  - List all users
  - View email and signup date
  
- [x] **Vouchers page `/admin/vouchers`**:
  - Create new vouchers (auto-generate codes)
  - List all vouchers with redemption counts
  - Toggle voucher active status

- [x] **Transcribe page `/admin/transcribe`**:
  - Soniox test tool (same as user dashboard)

- [x] **Protected routes**:
  - Admin authentication via magic link
  - Email whitelist (`ADMIN_EMAILS` env var)

#### 1.6 UI/UX ✅
- [x] Landing page with app description
- [x] Login/signup forms (email + password)
- [x] Admin login (magic link)
- [x] Responsive design
- [x] Dark mode (default)

---

### Phase 2: Android IME Development (In Progress)
**Duration: 3-4 weeks**

With the backend fully working and Soniox integration validated in browser, build the Android keyboard.

#### 2.1 Android Project Setup ✅
- [x] Initialize Android project in `/android`
- [x] Configure Gradle (Kotlin 2.0, AGP 8.7, Compose BOM)
- [x] Set up InputMethodService boilerplate
- [x] Configure AndroidManifest.xml with IME declarations

#### 2.2 Keyboard UI (Record Button Only - MVP) ✅
- [x] Custom keyboard view layout (Jetpack Compose)
- [x] Large, prominent record toggle button
- [x] Recording state indicators (idle, recording, processing)
- [x] Microphone permission request flow

#### 2.3 Audio Capture ✅
- [x] Implement `AudioRecord` for raw PCM capture
- [x] Configure: 16kHz sample rate, mono, 16-bit PCM
- [x] Buffer management for low-latency streaming
- [ ] Handle audio focus and interruptions

#### 2.4 Backend Integration ✅
- [x] API client for Vercel backend
- [x] Auth via backend login endpoint
- [x] Token storage (EncryptedSharedPreferences)
- [ ] Refresh token handling

#### 2.5 Soniox WebSocket Client ✅
- [x] WebSocket connection management (OkHttp)
- [x] Audio chunk streaming
- [x] Token parsing (fw/nfw format)
- [x] Error handling and reconnection

#### 2.6 Text Insertion ✅
- [x] Use `InputConnection.setComposingText()` for provisional tokens
- [x] Use `InputConnection.commitText()` for final tokens
- [x] Handle cursor position
- [x] Finish composing text on disconnect

#### 2.7 Testing
- [ ] Test on various Android versions (12, 13, 14, 15)
- [ ] Test with different apps (messaging, email, notes)
- [ ] Test network conditions (WiFi, cellular, poor connection)
- [ ] Test permission flows

---

### Phase 3: Google Play Billing Integration
**Duration: 1-2 weeks**

Add subscription management for monetization.

#### 3.1 Google Play Console Setup
- [ ] Create app listing
- [ ] Configure subscription products (e.g., `ramble_monthly`)
- [ ] Set up service account for server verification
- [ ] Configure Real-time Developer Notifications (RTDN)

#### 3.2 Android Billing
- [ ] Integrate Play Billing Library
- [ ] Implement subscription purchase flow
- [ ] Handle purchase verification
- [ ] Implement subscription status checks

#### 3.3 Backend Billing
- [ ] Implement purchase token verification via Google Play API
- [ ] Handle RTDN webhooks (subscription events)
- [ ] Update subscription status in Supabase
- [ ] Enforce subscription requirements for Soniox token issuance

---

### Phase 4: Polish & Launch
**Duration: 1-2 weeks**

#### 4.1 Android Polish
- [ ] Add minimal typing capability (optional)
- [ ] Improve UI/UX based on testing
- [ ] Add onboarding flow
- [ ] Performance optimization

#### 4.2 Privacy & Compliance
- [ ] Privacy policy page
- [ ] Terms of service
- [ ] GDPR compliance (if applicable)
- [ ] Play Store policy compliance review

#### 4.3 Launch Preparation
- [ ] Play Store listing assets (screenshots, description)
- [ ] Beta testing program
- [ ] Analytics integration (optional)
- [ ] Crash reporting (Crashlytics or similar)

---

## Project Structure

```
ramble/
├── android/
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/ramble/keyboard/
│   │   │   │   ├── RambleKeyboardService.kt      # Main IME service
│   │   │   │   ├── audio/
│   │   │   │   │   └── AudioRecorder.kt          # Mic capture
│   │   │   │   ├── soniox/
│   │   │   │   │   └── SonioxWebSocket.kt        # WebSocket client
│   │   │   │   ├── api/
│   │   │   │   │   └── RambleApiClient.kt        # Backend API calls
│   │   │   │   ├── billing/
│   │   │   │   │   └── BillingManager.kt         # Google Play Billing
│   │   │   │   └── ui/
│   │   │   │       ├── KeyboardView.kt           # Custom keyboard view
│   │   │   │       └── RecordButton.kt           # Toggle button
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   └── keyboard_view.xml
│   │   │   │   ├── drawable/
│   │   │   │   ├── values/
│   │   │   │   └── xml/
│   │   │   │       └── method.xml                # IME declaration
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle.kts
│   ├── gradle/
│   ├── build.gradle.kts
│   └── settings.gradle.kts
│
├── vercel/
│   ├── app/                                      # Next.js App Router
│   │   ├── api/
│   │   │   ├── auth/
│   │   │   │   ├── signup/route.ts
│   │   │   │   ├── login/route.ts
│   │   │   │   └── me/route.ts
│   │   │   ├── soniox/
│   │   │   │   └── token/route.ts                # Issue temp Soniox key
│   │   │   └── billing/
│   │   │       ├── verify/route.ts               # Verify Play purchase
│   │   │       └── webhook/route.ts              # RTDN webhook
│   │   ├── admin/                                # Admin portal
│   │   │   ├── page.tsx                          # Dashboard
│   │   │   ├── users/page.tsx
│   │   │   └── layout.tsx
│   │   ├── test/                                 # Soniox test platform
│   │   │   └── page.tsx                          # Browser-based testing
│   │   ├── (auth)/                               # Auth pages
│   │   │   ├── login/page.tsx
│   │   │   └── signup/page.tsx
│   │   ├── layout.tsx
│   │   ├── page.tsx                              # Landing page
│   │   └── globals.css
│   ├── components/
│   │   ├── AudioRecorder.tsx                     # Browser audio capture
│   │   ├── TranscriptionOutput.tsx               # Real-time text display
│   │   └── ui/                                   # Shared UI components
│   ├── lib/
│   │   ├── supabase/
│   │   │   ├── client.ts                         # Browser client
│   │   │   └── server.ts                         # Server client
│   │   ├── soniox.ts                             # Soniox API helper
│   │   └── google-play.ts                        # Play Developer API
│   ├── package.json
│   ├── tsconfig.json
│   ├── tailwind.config.ts
│   ├── next.config.js
│   └── .env.local                                # Environment variables
│
├── plan.md                                       # This file
└── README.md
```

---

## Environment Variables

### Vercel (.env.local)

```env
# Soniox API
SONIOX_API_KEY=your_soniox_api_key_here

# Supabase
NEXT_PUBLIC_SUPABASE_URL=https://your-project.supabase.co
NEXT_PUBLIC_SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
SUPABASE_SERVICE_ROLE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# Google Play (Phase 3)
GOOGLE_PLAY_PACKAGE_NAME=com.ramble.keyboard
GOOGLE_PLAY_CREDENTIALS={"type":"service_account","project_id":"..."}

# Admin
ADMIN_EMAILS=your-email@example.com
```

### Android (local.properties / BuildConfig)

```properties
RAMBLE_API_URL=https://your-app.vercel.app
```

---

## API Reference

### Authentication

#### POST /api/auth/signup
```json
// Request
{
  "email": "user@example.com",
  "password": "securepassword"
}

// Response
{
  "user": { "id": "uuid", "email": "user@example.com" },
  "session": { "access_token": "...", "refresh_token": "..." }
}
```

#### POST /api/auth/login
```json
// Request
{
  "email": "user@example.com",
  "password": "securepassword"
}

// Response
{
  "user": { "id": "uuid", "email": "user@example.com" },
  "session": { "access_token": "...", "refresh_token": "..." }
}
```

### Soniox

#### POST /api/soniox/token
Requires authentication.

```json
// Request
// Headers: Authorization: Bearer <access_token>
{}

// Response
{
  "token": "temporary_soniox_key",
  "expires_at": 1704067200000,
  "websocket_url": "wss://api.soniox.com/transcribe-websocket"
}
```

### Billing

#### POST /api/billing/verify
```json
// Request
{
  "purchase_token": "google_play_purchase_token",
  "product_id": "ramble_monthly"
}

// Response
{
  "success": true,
  "subscription": {
    "active": true,
    "expires_at": "2024-02-15T00:00:00Z"
  }
}
```

---

## Soniox Integration Details

### WebSocket Connection

```javascript
// Connect to Soniox real-time API
const ws = new WebSocket('wss://api.soniox.com/transcribe-websocket');

// Send config on connect
ws.onopen = () => {
  ws.send(JSON.stringify({
    api_key: temporaryToken,
    // Audio format config
    audio_format: 'pcm_s16le',
    sample_rate: 16000,
    channels: 1,
    // Transcription config
    model: 'en_v2',
    include_nonfinal: true,
    max_non_final_tokens_duration_ms: 500
  }));
};

// Handle incoming tokens
ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  
  if (data.tokens) {
    data.tokens.forEach(token => {
      if (token.is_final) {
        // Commit this text permanently
        commitText(token.text);
      } else {
        // Show provisional text (may change)
        setComposingText(token.text);
      }
    });
  }
};

// Stream audio chunks
function sendAudio(audioData: ArrayBuffer) {
  ws.send(audioData);
}
```

### Audio Format Requirements

| Parameter | Value | Notes |
|-----------|-------|-------|
| Format | PCM signed 16-bit little-endian | Raw audio, no container |
| Sample Rate | 16000 Hz | 16kHz recommended for speech |
| Channels | 1 (mono) | Mono audio |
| Chunk Size | 100-200ms | Balance latency vs overhead |

---

## Key Technical Challenges

### 1. Non-final → Final Token Transition

When Soniox sends non-final tokens, they may be revised in subsequent messages. Strategy:

```
User speaks: "Hello world"

1. Non-final: "Hel" → display as composing text
2. Non-final: "Hello" → update composing text  
3. Non-final: "Hello wor" → update composing text
4. Final: "Hello world" → commit text, clear composing
```

On Android, use:
- `InputConnection.setComposingText()` for non-final
- `InputConnection.commitText()` for final

### 2. Interleaved Typing + Speech

When user types while speech is being transcribed:

```
Strategy A (Simple): 
- Typing commits immediately
- Speech tokens insert at cursor position
- May result in interleaved words

Strategy B (Buffered):
- Maintain separate buffers for typed and spoken text
- Merge intelligently on final tokens
- More complex but cleaner output
```

Start with Strategy A for MVP.

### 3. Android Microphone Permissions

```kotlin
// In InputMethodService, before starting recording
if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
    != PackageManager.PERMISSION_GRANTED) {
    // Need to launch a separate activity to request permission
    // IME cannot directly request permissions
}
```

### 4. Token Expiration Handling

Temporary Soniox tokens have limited lifetime. Strategy:

```
1. Request token when recording starts
2. Token includes expires_at timestamp
3. If session runs long, refresh token before expiry
4. On token refresh, reconnect WebSocket seamlessly
```

---

## Security Considerations

1. **Never embed Soniox API key in Android app**
   - Key is only in Vercel environment variables
   - Only temporary tokens sent to client

2. **Validate subscription before issuing tokens**
   - Check subscription status in Supabase
   - Deny token if subscription expired

3. **Rate limiting**
   - Limit token requests per user
   - Limit concurrent sessions

4. **Secure token storage on Android**
   - Use EncryptedSharedPreferences
   - Clear on logout

5. **HTTPS everywhere**
   - All API calls over HTTPS
   - WebSocket over WSS

---

## Estimated Timeline

| Phase | Duration | Deliverable |
|-------|----------|-------------|
| **Phase 1** | 2-3 weeks | Vercel backend + admin portal + Soniox test platform |
| **Phase 2** | 3-4 weeks | Working Android IME with voice input |
| **Phase 3** | 1-2 weeks | Google Play Billing integration |
| **Phase 4** | 1-2 weeks | Polish + Play Store launch |
| **Total** | 7-11 weeks | Production app on Play Store |

---

## Next Steps

### ✅ Phase 1 Complete!

All Phase 1 deliverables are working:
- Vercel backend deployed
- Supabase database configured
- User signup/login flow
- Admin portal with magic link auth
- Voucher creation and redemption
- Soniox real-time transcription (multi-language)

### Current Status: Phase 2 - Android IME (In Progress)

**Completed:**
- ✅ Android project setup (Kotlin, Jetpack Compose, Gradle 8.7)
- ✅ Companion app with login and transcription testing
- ✅ Audio capture via `AudioRecord` (16kHz, mono, 16-bit PCM)
- ✅ Soniox WebSocket client (stt-rt-v3 model, multi-language)
- ✅ Backend authentication via Bearer token
- ✅ Real-time transcription working in companion app

**Up Next:**
1. Test keyboard as an Input Method (IME)
2. Test on various Android versions and apps
3. Handle audio focus and interruptions
4. Add refresh token handling
5. Polish keyboard UI

---

## Resources & Documentation

- [Soniox Real-time API Docs](https://soniox.com/docs/stt/real-time-API/transcription)
- [Soniox Live Demo (Reference Architecture)](https://soniox.com/docs/stt/demo-apps/soniox-live)
- [Android InputMethodService Guide](https://developer.android.com/guide/topics/text/creating-input-method)
- [Supabase Auth Documentation](https://supabase.com/docs/guides/auth)
- [Google Play Billing Library](https://developer.android.com/google/play/billing)
- [Vercel Next.js Deployment](https://vercel.com/docs/frameworks/nextjs)
