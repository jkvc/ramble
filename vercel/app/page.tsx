"use client";

import { useState, useEffect } from "react";
import { getApiKey, setApiKey, clearApiKey } from "../lib/api-key";
import { ApiKeyInput } from "../components/ApiKeyInput";
import { TranscriptionTool } from "../components/TranscriptionTool";

export default function Home() {
  const [apiKey, setApiKeyState] = useState<string | null>(null);
  const [loaded, setLoaded] = useState(false);
  const [showSettings, setShowSettings] = useState(false);

  useEffect(() => {
    setApiKeyState(getApiKey());
    setLoaded(true);
  }, []);

  const handleSaveKey = (key: string) => {
    setApiKey(key);
    setApiKeyState(key);
    setShowSettings(false);
  };

  const handleClearKey = () => {
    clearApiKey();
    setApiKeyState(null);
    setShowSettings(false);
  };

  if (!loaded) return null;

  return (
    <main className="min-h-screen flex flex-col">
      {/* Header */}
      <header className="border-b border-[var(--border)] px-4 sm:px-6 py-3 glass">
        <div className="max-w-2xl mx-auto flex items-center justify-between">
          <h1 className="text-lg font-bold gradient-text">Ramble</h1>
          {apiKey && (
            <button
              onClick={() => setShowSettings(!showSettings)}
              className="p-2 hover:bg-[var(--surface)] rounded-lg transition-colors"
              title="Settings"
            >
              <svg className="w-5 h-5 text-[var(--muted)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            </button>
          )}
        </div>
      </header>

      {/* Content */}
      <div className="flex-1 flex flex-col items-center justify-center px-4 sm:px-6 py-8">
        <div className="w-full max-w-2xl mx-auto">
          {showSettings ? (
            <div className="w-full max-w-md mx-auto space-y-4">
              <div className="glass border border-[var(--border)] rounded-2xl p-6">
                <h2 className="text-lg font-medium mb-4">Settings</h2>
                <div className="space-y-3">
                  <div>
                    <label className="text-sm text-[var(--muted)]">Soniox API Key</label>
                    <p className="text-sm font-mono mt-1">{"*".repeat(20)}</p>
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => setShowSettings(false)}
                      className="flex-1 px-4 py-2.5 bg-[var(--surface-hover)] hover:bg-[var(--border)] rounded-full transition-colors text-sm"
                    >
                      Back
                    </button>
                    <button
                      onClick={handleClearKey}
                      className="flex-1 px-4 py-2.5 bg-[var(--error)]/10 hover:bg-[var(--error)]/20 text-[var(--error)] rounded-full transition-colors text-sm"
                    >
                      Remove Key
                    </button>
                  </div>
                </div>
              </div>
            </div>
          ) : apiKey ? (
            <TranscriptionTool apiKey={apiKey} />
          ) : (
            <div className="text-center space-y-8">
              <div>
                <h2 className="text-3xl sm:text-4xl font-semibold tracking-tight mb-2">Ramble</h2>
                <p className="text-[var(--muted)]">
                  Speak naturally. Watch your words appear in real-time.
                </p>
              </div>
              <ApiKeyInput onSave={handleSaveKey} />
            </div>
          )}
        </div>
      </div>
    </main>
  );
}
