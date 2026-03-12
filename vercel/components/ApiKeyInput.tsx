"use client";

import { useState } from "react";

interface ApiKeyInputProps {
  onSave: (key: string) => void;
}

export function ApiKeyInput({ onSave }: ApiKeyInputProps) {
  const [key, setKey] = useState("");

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = key.trim();
    if (trimmed) {
      onSave(trimmed);
    }
  };

  return (
    <div className="w-full max-w-md mx-auto">
      <div className="bg-[var(--surface)] border border-[var(--border)] rounded-lg p-6">
        <h2 className="text-lg font-medium mb-2">Soniox API Key</h2>
        <p className="text-sm text-[var(--muted)] mb-4">
          Enter your Soniox API key to start transcribing. You can get one from{" "}
          <a
            href="https://soniox.com"
            target="_blank"
            rel="noopener noreferrer"
            className="text-[var(--accent)] hover:underline"
          >
            soniox.com
          </a>
          .
        </p>
        <form onSubmit={handleSubmit} className="space-y-3">
          <input
            type="password"
            value={key}
            onChange={(e) => setKey(e.target.value)}
            placeholder="Paste your API key here"
            className="w-full px-3 py-2 bg-[var(--background)] border border-[var(--border)] rounded-lg focus:outline-none focus:ring-2 focus:ring-[var(--accent)] focus:border-transparent text-sm"
            autoFocus
          />
          <button
            type="submit"
            disabled={!key.trim()}
            className="w-full px-4 py-2 bg-[var(--accent)] hover:bg-[var(--accent-hover)] text-white font-medium rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed text-sm"
          >
            Save & Start
          </button>
        </form>
      </div>
    </div>
  );
}
