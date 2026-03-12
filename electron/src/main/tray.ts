import { Tray, Menu, nativeImage, BrowserWindow, app } from "electron";
import path from "path";
import type { RecordingState } from "../shared/types";

let tray: Tray | null = null;

const COLORS: Record<string, string> = {
  idle: "#2196F3",
  connecting: "#FFC107",
  recording: "#F44336",
  nokey: "#9E9E9E",
};

function createTrayIcon(color: string): Electron.NativeImage {
  // Create a simple 16x16 colored circle as tray icon
  const size = 16;
  const canvas = `
    <svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" xmlns="http://www.w3.org/2000/svg">
      <circle cx="${size / 2}" cy="${size / 2}" r="${size / 2 - 1}" fill="${color}" />
    </svg>
  `;
  return nativeImage.createFromBuffer(
    Buffer.from(canvas.trim()),
    { width: size, height: size }
  );
}

export function createTray(
  mainWindow: BrowserWindow,
  onToggleRecording: () => void,
  hasApiKey: boolean
): Tray {
  const color = hasApiKey ? COLORS.idle : COLORS.nokey;
  tray = new Tray(createTrayIcon(color));
  tray.setToolTip("Ramble — Voice Dictation");

  updateTrayMenu(mainWindow, onToggleRecording, "idle");

  tray.on("click", () => {
    if (mainWindow.isVisible()) {
      mainWindow.hide();
    } else {
      mainWindow.show();
      mainWindow.focus();
    }
  });

  return tray;
}

export function updateTrayState(
  mainWindow: BrowserWindow,
  onToggleRecording: () => void,
  state: RecordingState,
  hasApiKey: boolean
): void {
  if (!tray) return;

  const color = !hasApiKey ? COLORS.nokey : COLORS[state] || COLORS.idle;
  tray.setImage(createTrayIcon(color));

  const tooltip =
    state === "recording"
      ? "Ramble — Recording..."
      : state === "connecting"
        ? "Ramble — Connecting..."
        : "Ramble — Ready";
  tray.setToolTip(tooltip);

  updateTrayMenu(mainWindow, onToggleRecording, state);
}

function updateTrayMenu(
  mainWindow: BrowserWindow,
  onToggleRecording: () => void,
  state: RecordingState
): void {
  if (!tray) return;

  const menu = Menu.buildFromTemplate([
    {
      label: "Open Ramble",
      click: () => {
        mainWindow.show();
        mainWindow.focus();
      },
    },
    { type: "separator" },
    {
      label:
        state === "recording" || state === "connecting"
          ? "Stop Recording"
          : "Start Recording",
      click: onToggleRecording,
    },
    { type: "separator" },
    {
      label: "Quit",
      click: () => {
        app.quit();
      },
    },
  ]);
  tray.setContextMenu(menu);
}

export function destroyTray(): void {
  if (tray) {
    tray.destroy();
    tray = null;
  }
}
