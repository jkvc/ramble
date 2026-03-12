import React, { useState } from "react";
import type { Settings } from "../../shared/types";
import { SHORTCUT_PRESETS } from "../../shared/types";

export function SettingsPanel({
  settings,
  onSave,
}: {
  settings: Settings;
  onSave: (updates: Partial<Settings>) => void;
}) {
  const [customShortcut, setCustomShortcut] = useState("");
  const isCustom = !SHORTCUT_PRESETS.includes(settings.shortcut);

  const maskedKey = settings.apiKey
    ? "••••" + settings.apiKey.slice(-4)
    : null;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
      <h2 style={{ fontSize: 16 }}>Settings</h2>

      {/* API Key */}
      <section>
        <label style={{ fontSize: 13, color: "var(--text-muted)", display: "block", marginBottom: 8 }}>
          Soniox API Key
        </label>
        {maskedKey ? (
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <code
              style={{
                background: "var(--surface)",
                padding: "6px 12px",
                borderRadius: "var(--radius)",
                border: "1px solid var(--border)",
                flex: 1,
              }}
            >
              {maskedKey}
            </code>
            <button
              className="btn-danger"
              onClick={() => onSave({ apiKey: null })}
              style={{ fontSize: 12 }}
            >
              Remove
            </button>
          </div>
        ) : (
          <p style={{ color: "var(--text-muted)" }}>No API key set</p>
        )}
      </section>

      {/* Shortcut */}
      <section>
        <label style={{ fontSize: 13, color: "var(--text-muted)", display: "block", marginBottom: 8 }}>
          Recording Shortcut
        </label>
        <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
          {SHORTCUT_PRESETS.map((preset) => {
            const display = preset
              .replace("CommandOrControl", "Ctrl")
              .replace(/\+/g, " + ");
            return (
              <label
                key={preset}
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 8,
                  padding: "6px 8px",
                  borderRadius: "var(--radius)",
                  cursor: "pointer",
                  background:
                    settings.shortcut === preset
                      ? "var(--surface-hover)"
                      : "transparent",
                }}
              >
                <input
                  type="radio"
                  name="shortcut"
                  checked={settings.shortcut === preset}
                  onChange={() => onSave({ shortcut: preset })}
                />
                <span>{display}</span>
              </label>
            );
          })}
          <label
            style={{
              display: "flex",
              alignItems: "center",
              gap: 8,
              padding: "6px 8px",
              borderRadius: "var(--radius)",
              cursor: "pointer",
              background: isCustom ? "var(--surface-hover)" : "transparent",
            }}
          >
            <input
              type="radio"
              name="shortcut"
              checked={isCustom}
              onChange={() => {
                if (customShortcut.trim()) {
                  onSave({ shortcut: customShortcut.trim() });
                }
              }}
            />
            <span>Custom:</span>
            <input
              type="text"
              value={isCustom ? settings.shortcut : customShortcut}
              onChange={(e) => setCustomShortcut(e.target.value)}
              onBlur={() => {
                if (isCustom && customShortcut.trim()) {
                  onSave({ shortcut: customShortcut.trim() });
                }
              }}
              placeholder="e.g. Alt+Shift+S"
              style={{ flex: 1, padding: "4px 8px", fontSize: 13 }}
            />
          </label>
        </div>
      </section>

      {/* Version */}
      <section style={{ color: "var(--text-muted)", fontSize: 12, marginTop: 16 }}>
        Ramble Desktop v1.0.0
      </section>
    </div>
  );
}
