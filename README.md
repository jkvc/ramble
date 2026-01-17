# Ramble

Voice-to-text input for Android, powered by [Soniox](https://soniox.com) real-time transcription.

## Overview

Ramble is an Android keyboard that transcribes speech to text in real-time. Audio streams directly from device to Soniox API for lowest latency—no backend proxy.

## Architecture

- **Android IME** - Custom keyboard with voice input
- **Vercel Backend** - Auth, access control, token issuance
- **Supabase** - User database and authentication
- **Soniox** - Real-time speech-to-text

## Quick Start

```bash
cd vercel
pnpm install
pnpm dev
```

See [vercel/README.md](./vercel/README.md) for setup details and [plan.md](./plan.md) for development phases.

## License

Apache 2.0 - See [LICENSE](./LICENSE)
