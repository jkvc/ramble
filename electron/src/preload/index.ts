import { contextBridge, ipcRenderer } from "electron";
import { IPC } from "../shared/types";
import type { Settings, Session, RecordingState } from "../shared/types";

contextBridge.exposeInMainWorld("ramble", {
  // Settings
  getSettings: (): Promise<Settings> => ipcRenderer.invoke(IPC.GET_SETTINGS),
  saveSettings: (updates: Partial<Settings>): Promise<Settings> =>
    ipcRenderer.invoke(IPC.SAVE_SETTINGS, updates),

  // Sessions
  getSessions: (): Promise<Session[]> => ipcRenderer.invoke(IPC.GET_SESSIONS),
  deleteSession: (id: string): Promise<Session[]> =>
    ipcRenderer.invoke(IPC.DELETE_SESSION, id),

  // Recording
  toggleRecording: (): Promise<void> =>
    ipcRenderer.invoke(IPC.TOGGLE_RECORDING),
  sendAudioChunk: (data: ArrayBuffer): void =>
    ipcRenderer.send(IPC.AUDIO_CHUNK, data),

  // Events from main
  onRecordingState: (callback: (state: RecordingState) => void) => {
    const handler = (_event: any, state: RecordingState) => callback(state);
    ipcRenderer.on(IPC.RECORDING_STATE, handler);
    return () => ipcRenderer.removeListener(IPC.RECORDING_STATE, handler);
  },
  onProvisionalText: (callback: (text: string) => void) => {
    const handler = (_event: any, text: string) => callback(text);
    ipcRenderer.on(IPC.PROVISIONAL_TEXT, handler);
    return () => ipcRenderer.removeListener(IPC.PROVISIONAL_TEXT, handler);
  },
  onFinalText: (callback: (text: string) => void) => {
    const handler = (_event: any, text: string) => callback(text);
    ipcRenderer.on(IPC.FINAL_TEXT, handler);
    return () => ipcRenderer.removeListener(IPC.FINAL_TEXT, handler);
  },
  onStartAudioCapture: (callback: () => void) => {
    const handler = () => callback();
    ipcRenderer.on(IPC.START_AUDIO_CAPTURE, handler);
    return () =>
      ipcRenderer.removeListener(IPC.START_AUDIO_CAPTURE, handler);
  },
  onStopAudioCapture: (callback: () => void) => {
    const handler = () => callback();
    ipcRenderer.on(IPC.STOP_AUDIO_CAPTURE, handler);
    return () =>
      ipcRenderer.removeListener(IPC.STOP_AUDIO_CAPTURE, handler);
  },
});
