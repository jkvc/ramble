# Ramble - Web App

Real-time voice transcription web app, powered by Next.js and Soniox.

## Features

- Enter your Soniox API key (stored in browser localStorage)
- Real-time transcription via WebSocket streaming
- Multi-language support (en, zh, es, fr, de, ja, ko)
- Copy/clear transcribed text

## Local Development

```bash
pnpm install
pnpm dev
```

No backend or environment variables required — the app connects directly to Soniox using the user's API key.

## Testing

```bash
pnpm test
```

## Deployment

1. Push to GitHub
2. Import in Vercel (set root directory to `vercel`)
3. Deploy — no server-side environment variables needed
