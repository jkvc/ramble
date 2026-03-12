import { describe, it, expect, vi, beforeEach } from "vitest";
import type { Session, Settings } from "../src/shared/types";
import { DEFAULT_SETTINGS } from "../src/shared/types";

// Test store logic without requiring electron-store
// We simulate the store as a simple in-memory object

describe("Store - Settings", () => {
  let data: { settings: Settings; sessions: Session[] };

  function getSettings(): Settings {
    return data.settings;
  }

  function saveSettings(updates: Partial<Settings>): Settings {
    data.settings = { ...data.settings, ...updates };
    return data.settings;
  }

  function getApiKey(): string | null {
    return data.settings.apiKey;
  }

  beforeEach(() => {
    data = {
      settings: { ...DEFAULT_SETTINGS },
      sessions: [],
    };
  });

  it("returns default settings initially", () => {
    const settings = getSettings();
    expect(settings.apiKey).toBeNull();
    expect(settings.shortcut).toBe("CommandOrControl+Shift+Space");
    expect(settings.languageHints).toContain("en");
  });

  it("saves and retrieves API key", () => {
    saveSettings({ apiKey: "test-key-123" });
    expect(getApiKey()).toBe("test-key-123");
  });

  it("clears API key", () => {
    saveSettings({ apiKey: "key" });
    saveSettings({ apiKey: null });
    expect(getApiKey()).toBeNull();
  });

  it("updates shortcut without affecting other settings", () => {
    saveSettings({ apiKey: "my-key" });
    saveSettings({ shortcut: "F6" });

    const settings = getSettings();
    expect(settings.shortcut).toBe("F6");
    expect(settings.apiKey).toBe("my-key");
  });

  it("updates language hints", () => {
    saveSettings({ languageHints: ["en", "es"] });
    expect(getSettings().languageHints).toEqual(["en", "es"]);
  });
});

describe("Store - Sessions", () => {
  let sessions: Session[];

  function startSession(appName: string = ""): Session {
    const session: Session = {
      id: Date.now().toString(36) + Math.random().toString(36).slice(2, 6),
      startedAt: new Date().toISOString(),
      endedAt: null,
      transcript: "",
      appName,
    };
    sessions.unshift(session);
    return session;
  }

  function appendToSession(id: string, text: string): void {
    const session = sessions.find((s) => s.id === id);
    if (session) session.transcript += text;
  }

  function endSession(id: string): void {
    const session = sessions.find((s) => s.id === id);
    if (session) session.endedAt = new Date().toISOString();
  }

  function deleteSession(id: string): void {
    sessions = sessions.filter((s) => s.id !== id);
  }

  beforeEach(() => {
    sessions = [];
  });

  it("creates a new session", () => {
    const session = startSession("VS Code");
    expect(session.id).toBeTruthy();
    expect(session.startedAt).toBeTruthy();
    expect(session.endedAt).toBeNull();
    expect(session.transcript).toBe("");
    expect(session.appName).toBe("VS Code");
    expect(sessions).toHaveLength(1);
  });

  it("appends text to session transcript", () => {
    const session = startSession();
    appendToSession(session.id, "Hello ");
    appendToSession(session.id, "world");
    expect(sessions[0].transcript).toBe("Hello world");
  });

  it("ends a session with timestamp", () => {
    const session = startSession();
    expect(session.endedAt).toBeNull();

    endSession(session.id);
    expect(sessions[0].endedAt).toBeTruthy();
  });

  it("deletes a session", () => {
    const s1 = startSession();
    const s2 = startSession();
    expect(sessions).toHaveLength(2);

    deleteSession(s1.id);
    expect(sessions).toHaveLength(1);
    expect(sessions[0].id).toBe(s2.id);
  });

  it("new sessions appear at the front (newest first)", () => {
    const s1 = startSession("App1");
    const s2 = startSession("App2");
    expect(sessions[0].appName).toBe("App2");
    expect(sessions[1].appName).toBe("App1");
  });

  it("handles append to non-existent session gracefully", () => {
    appendToSession("non-existent", "text");
    expect(sessions).toHaveLength(0);
  });

  it("handles end of non-existent session gracefully", () => {
    endSession("non-existent");
    // No error thrown
    expect(sessions).toHaveLength(0);
  });
});
