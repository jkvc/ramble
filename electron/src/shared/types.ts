export interface Session {
  id: string;
  startedAt: string;
  endedAt: string | null;
  transcript: string;
  appName: string;
}

export interface Settings {
  apiKey: string | null;
  shortcut: string;
  languageHints: string[];
}

export const DEFAULT_SETTINGS: Settings = {
  apiKey: null,
  shortcut: "CommandOrControl+Shift+Space",
  languageHints: ["en", "zh", "es", "fr", "de", "ja", "ko"],
};

export const SHORTCUT_PRESETS = [
  "CommandOrControl+Shift+Space",
  "CommandOrControl+Shift+R",
  "F6",
  "CommandOrControl+Shift+D",
];

export type RecordingState = "idle" | "connecting" | "recording";

export interface SonioxToken {
  text: string;
  is_final?: boolean;
}

export interface SonioxMessage {
  tokens?: SonioxToken[];
  fw?: Array<{ t: string }>;
  nfw?: Array<{ t: string }>;
  error?: string;
}

// IPC channel names
export const IPC = {
  START_AUDIO_CAPTURE: "start-audio-capture",
  STOP_AUDIO_CAPTURE: "stop-audio-capture",
  AUDIO_CHUNK: "audio-chunk",
  RECORDING_STATE: "recording-state",
  PROVISIONAL_TEXT: "provisional-text",
  FINAL_TEXT: "final-text",
  GET_SETTINGS: "get-settings",
  SAVE_SETTINGS: "save-settings",
  GET_SESSIONS: "get-sessions",
  DELETE_SESSION: "delete-session",
  TOGGLE_RECORDING: "toggle-recording",
} as const;
