import WebSocket from "ws";
import { EventEmitter } from "events";
import type { SonioxMessage, Settings } from "../shared/types";

const SONIOX_WS_URL = "wss://stt-rt.soniox.com/transcribe-websocket";

function filterSpecialTokens(text: string): string {
  return text.replace(/<[^>]+>/g, "");
}

export interface RecorderEvents {
  connected: [];
  "final-words": [text: string];
  "provisional-words": [text: string];
  error: [message: string];
  disconnected: [];
}

export class Recorder extends EventEmitter {
  private ws: WebSocket | null = null;
  private isConnected = false;
  private audioBuffer: Buffer[] = [];
  private isBuffering = false;

  startBuffering() {
    this.isBuffering = true;
    this.audioBuffer = [];
  }

  sendAudio(data: Buffer) {
    if (this.isConnected && this.ws) {
      this.ws.send(data);
    } else if (this.isBuffering) {
      this.audioBuffer.push(Buffer.from(data));
    }
  }

  connect(apiKey: string, languageHints: string[]) {
    if (this.isConnected) return;

    this.ws = new WebSocket(SONIOX_WS_URL);

    this.ws.on("open", () => {
      this.isConnected = true;

      const config = {
        api_key: apiKey,
        model: "stt-rt-v3",
        audio_format: "pcm_s16le",
        sample_rate: 16000,
        num_channels: 1,
        language_hints: languageHints,
        enable_endpoint_detection: true,
      };
      this.ws!.send(JSON.stringify(config));

      // Flush buffered audio
      for (const chunk of this.audioBuffer) {
        this.ws!.send(chunk);
      }
      this.audioBuffer = [];
      this.isBuffering = false;

      this.emit("connected");
    });

    this.ws.on("message", (data: WebSocket.Data) => {
      try {
        const message: SonioxMessage = JSON.parse(data.toString());

        if (message.error) {
          this.emit("error", message.error);
          return;
        }

        // New format: tokens array
        if (message.tokens) {
          let finalText = "";
          let provisionalText = "";

          for (const token of message.tokens) {
            const clean = filterSpecialTokens(token.text);
            if (token.is_final) {
              finalText += clean;
            } else {
              provisionalText += clean;
            }
          }

          if (finalText) {
            this.emit("final-words", finalText);
          }
          if (provisionalText) {
            this.emit("provisional-words", provisionalText);
          } else if (finalText) {
            this.emit("provisional-words", "");
          }
        }

        // Legacy format: fw/nfw
        if (message.fw && message.fw.length > 0) {
          const text = message.fw.map((w) => filterSpecialTokens(w.t)).join("");
          if (text) this.emit("final-words", text);
        }
        if (message.nfw && message.nfw.length > 0) {
          const text = message.nfw
            .map((w) => filterSpecialTokens(w.t))
            .join("");
          if (text) this.emit("provisional-words", text);
        }
      } catch {
        // Ignore parse errors
      }
    });

    this.ws.on("error", (err) => {
      this.isConnected = false;
      this.emit("error", err.message || "WebSocket error");
    });

    this.ws.on("close", () => {
      this.isConnected = false;
      this.emit("disconnected");
    });
  }

  disconnect() {
    this.isConnected = false;
    this.isBuffering = false;
    this.audioBuffer = [];
    if (this.ws) {
      this.ws.close(1000, "User stopped");
      this.ws = null;
    }
  }
}
