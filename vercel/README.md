# Ramble - Vercel Backend

Backend and admin portal for Ramble, powered by Next.js, Supabase, and Soniox.

## Features

### User
- Email/password signup and login
- Dashboard with real-time transcription
- Voucher redemption for access

### Admin
- Magic link authentication (email whitelist)
- User management
- Voucher creation and management
- Transcription testing tool

## Local Development

```bash
pnpm install
pnpm dev
```

Create `.env.local` with your Soniox, Supabase, and admin email config. See Vercel dashboard for required variables.

## Database Setup

1. Create a Supabase project
2. Run `supabase/schema.sql` in the SQL Editor
3. Copy your project URL and keys to environment variables

## Soniox Integration

The `TranscriptionTool` component handles:
- Browser microphone via `MediaRecorder`
- WebSocket streaming to Soniox real-time API
- Multi-language support (en, zh, es, fr, de, ja, ko)
- Provisional and final token display

## Deployment

1. Push to GitHub
2. Import in Vercel (set root directory to `vercel`)
3. Add environment variables
4. Deploy

## Access Flows

**Users**: Sign up → Verify email → Redeem voucher → Use transcription

**Admins**: Go to `/admin/login` → Enter whitelisted email → Click magic link → Access admin tools
