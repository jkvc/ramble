import React, { useState, useEffect, useRef, useCallback } from "react";
import type { Settings, Session, RecordingState } from "../shared/types";
import { ApiKeyInput } from "./components/ApiKeyInput";
import { TranscriptHistory } from "./components/TranscriptHistory";
import { SessionDetail } from "./components/SessionDetail";
import { SettingsPanel } from "./components/Settings";

declare global {
  interface Window {
    ramble: {
      getSettings: () => Promise<Settings>;
      saveSettings: (updates: Partial<Settings>) => Promise<Settings>;
      getSessions: () => Promise<Session[]>;
      deleteSession: (id: string) => Promise<Session[]>;
      toggleRecording: () => Promise<void>;
      sendAudioChunk: (data: ArrayBuffer) => void;
      onRecordingState: (cb: (state: RecordingState) => void) => () => void;
      onProvisionalText: (cb: (text: string) => void) => () => void;
      onFinalText: (cb: (text: string) => void) => () => void;
      onStartAudioCapture: (cb: () => void) => () => void;
      onStopAudioCapture: (cb: () => void) => () => void;
    };
  }
}

type Tab = "live" | "history" | "settings";

export function App() {
  const [settings, setSettings] = useState<Settings | null>(null);
  const [sessions, setSessions] = useState<Session[]>([]);
  const [tab, setTab] = useState<Tab>("live");
  const [recordingState, setRecordingState] = useState<RecordingState>("idle");
  const [provisional, setProvisional] = useState("");
  const [sessionTranscript, setSessionTranscript] = useState("");
  const [selectedSession, setSelectedSession] = useState<Session | null>(null);

  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const streamRef = useRef<MediaStream | null>(null);

  // Load initial data
  useEffect(() => {
    window.ramble.getSettings().then(setSettings);
    window.ramble.getSessions().then(setSessions);
  }, []);

  // Subscribe to main process events
  useEffect(() => {
    const unsubs = [
      window.ramble.onRecordingState((state) => {
        setRecordingState(state);
        if (state === "idle") {
          setProvisional("");
          setSessionTranscript("");
          // Refresh sessions list
          window.ramble.getSessions().then(setSessions);
        }
      }),
      window.ramble.onProvisionalText(setProvisional),
      window.ramble.onFinalText((text) => {
        setSessionTranscript((prev) => prev + text);
      }),
      window.ramble.onStartAudioCapture(() => startAudioCapture()),
      window.ramble.onStopAudioCapture(() => stopAudioCapture()),
    ];
    return () => unsubs.forEach((fn) => fn());
  }, []);

  const startAudioCapture = useCallback(async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          sampleRate: 16000,
          channelCount: 1,
          echoCancellation: true,
          noiseSuppression: true,
        },
      });
      streamRef.current = stream;

      const recorder = new MediaRecorder(stream, {
        mimeType: "audio/webm;codecs=opus",
      });
      mediaRecorderRef.current = recorder;

      recorder.ondataavailable = (e) => {
        if (e.data.size > 0) {
          e.data.arrayBuffer().then((buffer) => {
            window.ramble.sendAudioChunk(buffer);
          });
        }
      };

      recorder.start(100); // Send chunks every 100ms
    } catch (err) {
      console.error("Failed to start audio capture:", err);
    }
  }, []);

  const stopAudioCapture = useCallback(() => {
    if (mediaRecorderRef.current) {
      mediaRecorderRef.current.stop();
      mediaRecorderRef.current = null;
    }
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((t) => t.stop());
      streamRef.current = null;
    }
  }, []);

  const handleSaveSettings = async (updates: Partial<Settings>) => {
    const updated = await window.ramble.saveSettings(updates);
    setSettings(updated);
  };

  const handleDeleteSession = async (id: string) => {
    const updated = await window.ramble.deleteSession(id);
    setSessions(updated);
    if (selectedSession?.id === id) {
      setSelectedSession(null);
    }
  };

  if (!settings) {
    return <div style={{ padding: 24, color: "var(--text-muted)" }}>Loading...</div>;
  }

  if (!settings.apiKey) {
    return (
      <div style={{ padding: 24 }}>
        <h1 className="gradient-text" style={{ marginBottom: 16, fontSize: 24, fontWeight: 700 }}>Ramble</h1>
        <ApiKeyInput
          onSave={(key) => handleSaveSettings({ apiKey: key })}
        />
      </div>
    );
  }

  if (selectedSession) {
    return (
      <SessionDetail
        session={selectedSession}
        onBack={() => setSelectedSession(null)}
        onDelete={() => handleDeleteSession(selectedSession.id)}
      />
    );
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", height: "100vh" }}>
      {/* Tab bar */}
      <div
        style={{
          display: "flex",
          borderBottom: "1px solid var(--border)",
          background: "var(--surface)",
        }}
      >
        {(["live", "history", "settings"] as Tab[]).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            style={{
              flex: 1,
              padding: "12px 0",
              background: tab === t ? "var(--bg)" : "transparent",
              color: tab === t ? "var(--accent)" : "var(--text-muted)",
              borderBottom: tab === t ? "2px solid transparent" : "2px solid transparent",
              borderImage: tab === t ? "linear-gradient(90deg, #0066FF, #9933FF) 1" : "none",
              borderRadius: 0,
              fontWeight: tab === t ? 600 : 400,
              textTransform: "capitalize",
            }}
          >
            {t}
          </button>
        ))}
      </div>

      {/* Content */}
      <div style={{ flex: 1, overflow: "auto", padding: 16 }}>
        {tab === "live" && (
          <LiveTab
            recordingState={recordingState}
            provisional={provisional}
            transcript={sessionTranscript}
            shortcut={settings.shortcut}
          />
        )}
        {tab === "history" && (
          <TranscriptHistory
            sessions={sessions}
            onSelect={setSelectedSession}
            onDelete={handleDeleteSession}
          />
        )}
        {tab === "settings" && (
          <SettingsPanel
            settings={settings}
            onSave={handleSaveSettings}
          />
        )}
      </div>
    </div>
  );
}

function LiveTab({
  recordingState,
  provisional,
  transcript,
  shortcut,
}: {
  recordingState: RecordingState;
  provisional: string;
  transcript: string;
  shortcut: string;
}) {
  const stateColors: Record<RecordingState, string> = {
    idle: "var(--accent)",
    connecting: "var(--amber)",
    recording: "linear-gradient(135deg, #0066FF, #9933FF)",
  };

  const stateLabels: Record<RecordingState, string> = {
    idle: "Ready",
    connecting: "Connecting...",
    recording: "Recording",
  };

  const displayShortcut = shortcut
    .replace("CommandOrControl", "Ctrl")
    .replace("+", " + ");

  return (
    <div>
      {/* Status indicator */}
      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: 8,
          marginBottom: 16,
        }}
      >
        <div
          style={{
            width: 12,
            height: 12,
            borderRadius: "50%",
            background: stateColors[recordingState],
            animation:
              recordingState === "recording"
                ? "pulse 1.5s infinite"
                : undefined,
          }}
        />
        <span style={{ fontWeight: 600 }}>{stateLabels[recordingState]}</span>
        <span style={{ color: "var(--text-muted)", marginLeft: "auto", fontSize: 12 }}>
          {displayShortcut}
        </span>
      </div>

      {/* Transcript area */}
      <div
        className="glass"
        style={{
          borderRadius: 16,
          padding: 16,
          minHeight: 200,
          border: "1px solid var(--border)",
          animation: "fade-in 0.3s ease-out",
        }}
      >
        {transcript || provisional ? (
          <div>
            <span>{transcript}</span>
            {provisional && (
              <span style={{ color: "var(--text-muted)", fontStyle: "italic" }}>
                {provisional}
              </span>
            )}
          </div>
        ) : (
          <div style={{ color: "var(--text-muted)" }}>
            {recordingState === "idle"
              ? `Press ${displayShortcut} to start dictating`
              : "Listening..."}
          </div>
        )}
      </div>

    </div>
  );
}
