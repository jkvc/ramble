import React from "react";
import type { Session } from "../../shared/types";

export function SessionDetail({
  session,
  onBack,
  onDelete,
}: {
  session: Session;
  onBack: () => void;
  onDelete: () => void;
}) {
  const date = new Date(session.startedAt);

  const handleCopy = () => {
    navigator.clipboard.writeText(session.transcript);
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", height: "100vh" }}>
      {/* Header */}
      <div
        className="glass"
        style={{
          display: "flex",
          alignItems: "center",
          gap: 12,
          padding: 16,
          borderBottom: "1px solid var(--border)",
        }}
      >
        <button className="btn-ghost" onClick={onBack}>
          Back
        </button>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 13, color: "var(--text-muted)" }}>
            {date.toLocaleDateString()} {date.toLocaleTimeString()}
            {session.appName && ` — ${session.appName}`}
          </div>
        </div>
        <button className="btn-ghost" onClick={handleCopy}>
          Copy
        </button>
        <button
          className="btn-ghost"
          onClick={onDelete}
          style={{ color: "var(--error)" }}
        >
          Delete
        </button>
      </div>

      {/* Transcript */}
      <div style={{ flex: 1, overflow: "auto", padding: 16 }}>
        <div
          className="glass"
          style={{
            borderRadius: 16,
            padding: 16,
            border: "1px solid var(--border)",
            whiteSpace: "pre-wrap",
            lineHeight: 1.7,
          }}
        >
          {session.transcript || (
            <span style={{ color: "var(--text-muted)" }}>(empty transcript)</span>
          )}
        </div>
      </div>
    </div>
  );
}
