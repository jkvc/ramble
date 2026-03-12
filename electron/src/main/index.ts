import { app, BrowserWindow, ipcMain } from "electron";
import path from "path";
import { Recorder } from "./recorder";
import { typeFinalText, typeProvisionalText, resetProvisional } from "./typer";
import { registerShortcut, unregisterAll } from "./shortcut";
import { createTray, updateTrayState, destroyTray } from "./tray";
import {
  getSettings,
  saveSettings,
  getApiKey,
  getSessions,
  startSession,
  appendToSession,
  endSession,
  deleteSession,
} from "./store";
import { IPC, RecordingState, Settings } from "../shared/types";

let mainWindow: BrowserWindow | null = null;
let recorder: Recorder | null = null;
let currentSessionId: string | null = null;
let recordingState: RecordingState = "idle";

function createWindow(): BrowserWindow {
  const win = new BrowserWindow({
    width: 480,
    height: 640,
    show: false,
    webPreferences: {
      preload: path.join(__dirname, "../preload/index.js"),
      contextIsolation: true,
      nodeIntegration: false,
    },
    title: "Ramble",
  });

  if (process.env.NODE_ENV === "development") {
    win.loadURL("http://localhost:5173");
  } else {
    win.loadFile(path.join(__dirname, "../renderer/index.html"));
  }

  win.on("close", (e) => {
    e.preventDefault();
    win.hide();
  });

  return win;
}

function setRecordingState(state: RecordingState): void {
  recordingState = state;
  const settings = getSettings();
  mainWindow?.webContents.send(IPC.RECORDING_STATE, state);
  if (mainWindow) {
    updateTrayState(mainWindow, toggleRecording, state, !!settings.apiKey);
  }
}

function toggleRecording(): void {
  if (recordingState === "recording" || recordingState === "connecting") {
    stopRecording();
  } else {
    startRecording();
  }
}

function startRecording(): void {
  const apiKey = getApiKey();
  if (!apiKey) return;

  const settings = getSettings();
  recorder = new Recorder();
  resetProvisional();

  const session = startSession();
  currentSessionId = session.id;

  setRecordingState("connecting");

  recorder.on("connected", () => {
    setRecordingState("recording");
  });

  recorder.on("final-words", async (text: string) => {
    if (currentSessionId) {
      appendToSession(currentSessionId, text);
    }
    mainWindow?.webContents.send(IPC.FINAL_TEXT, text);

    try {
      await typeFinalText(text);
    } catch (err) {
      console.error("Failed to type text:", err);
    }
  });

  recorder.on("provisional-words", async (text: string) => {
    mainWindow?.webContents.send(IPC.PROVISIONAL_TEXT, text);

    try {
      await typeProvisionalText(text);
    } catch (err) {
      console.error("Failed to type provisional:", err);
    }
  });

  recorder.on("error", (message: string) => {
    console.error("Recorder error:", message);
    stopRecording();
  });

  recorder.on("disconnected", () => {
    stopRecording();
  });

  // Tell renderer to start audio capture
  mainWindow?.webContents.send(IPC.START_AUDIO_CAPTURE);

  recorder.startBuffering();
  recorder.connect(apiKey, settings.languageHints);
}

function stopRecording(): void {
  if (recordingState === "idle") return;

  resetProvisional();

  mainWindow?.webContents.send(IPC.STOP_AUDIO_CAPTURE);

  if (recorder) {
    recorder.disconnect();
    recorder.removeAllListeners();
    recorder = null;
  }

  if (currentSessionId) {
    endSession(currentSessionId);
    currentSessionId = null;
  }

  setRecordingState("idle");
}

function setupIPC(): void {
  ipcMain.handle(IPC.GET_SETTINGS, () => {
    return getSettings();
  });

  ipcMain.handle(IPC.SAVE_SETTINGS, (_event, updates: Partial<Settings>) => {
    const updated = saveSettings(updates);

    // Re-register shortcut if it changed
    if (updates.shortcut) {
      registerShortcut(updated.shortcut, toggleRecording);
    }

    // Update tray state if API key changed
    if (mainWindow) {
      updateTrayState(
        mainWindow,
        toggleRecording,
        recordingState,
        !!updated.apiKey
      );
    }

    return updated;
  });

  ipcMain.handle(IPC.GET_SESSIONS, () => {
    return getSessions();
  });

  ipcMain.handle(IPC.DELETE_SESSION, (_event, sessionId: string) => {
    deleteSession(sessionId);
    return getSessions();
  });

  ipcMain.handle(IPC.TOGGLE_RECORDING, () => {
    toggleRecording();
  });

  ipcMain.on(IPC.AUDIO_CHUNK, (_event, data: ArrayBuffer) => {
    if (recorder) {
      recorder.sendAudio(Buffer.from(data));
    }
  });
}

app.whenReady().then(() => {
  mainWindow = createWindow();

  const settings = getSettings();

  // Register global shortcut
  registerShortcut(settings.shortcut, toggleRecording);

  // Create tray
  createTray(mainWindow, toggleRecording, !!settings.apiKey);

  setupIPC();

  // Show window on first launch if no API key
  if (!settings.apiKey) {
    mainWindow.show();
  }
});

app.on("will-quit", () => {
  unregisterAll();
  destroyTray();
  if (recorder) {
    recorder.disconnect();
  }
});

app.on("window-all-closed", (e: Event) => {
  // Don't quit — keep running in tray
  e.preventDefault();
});
