import Store from "electron-store";
import { Session, Settings, DEFAULT_SETTINGS } from "../shared/types";

interface StoreSchema {
  settings: Settings;
  sessions: Session[];
}

const store = new Store<StoreSchema>({
  defaults: {
    settings: DEFAULT_SETTINGS,
    sessions: [],
  },
});

export function getSettings(): Settings {
  return store.get("settings");
}

export function saveSettings(updates: Partial<Settings>): Settings {
  const current = getSettings();
  const updated = { ...current, ...updates };
  store.set("settings", updated);
  return updated;
}

export function getApiKey(): string | null {
  return getSettings().apiKey;
}

export function getSessions(): Session[] {
  return store.get("sessions");
}

export function startSession(appName: string = ""): Session {
  const session: Session = {
    id: Date.now().toString(36) + Math.random().toString(36).slice(2, 6),
    startedAt: new Date().toISOString(),
    endedAt: null,
    transcript: "",
    appName,
  };
  const sessions = getSessions();
  sessions.unshift(session);
  store.set("sessions", sessions);
  return session;
}

export function appendToSession(sessionId: string, text: string): void {
  const sessions = getSessions();
  const session = sessions.find((s) => s.id === sessionId);
  if (session) {
    session.transcript += text;
    store.set("sessions", sessions);
  }
}

export function endSession(sessionId: string): void {
  const sessions = getSessions();
  const session = sessions.find((s) => s.id === sessionId);
  if (session) {
    session.endedAt = new Date().toISOString();
    store.set("sessions", sessions);
  }
}

export function deleteSession(sessionId: string): void {
  const sessions = getSessions().filter((s) => s.id !== sessionId);
  store.set("sessions", sessions);
}
