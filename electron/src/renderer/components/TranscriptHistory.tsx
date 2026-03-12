import React from "react";
import type { Session } from "../../shared/types";

export function TranscriptHistory({
  sessions,
  onSelect,
  onDelete,
}: {
  sessions: Session[];
  onSelect: (session: Session) => void;
  onDelete: (id: string) => void;
}) {
  if (sessions.length === 0) {
    return (
      <div style={{ color: "var(--text-muted)", textAlign: "center", paddingTop: 40 }}>
        No transcripts yet. Start recording to create your first session.
      </div>
    );
  }

  return (
    <div>
      <h2 style={{ fontSize: 16, marginBottom: 12 }}>Transcript History</h2>
      <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
        {sessions.map((session) => {
          const date = new Date(session.startedAt);
          const preview = session.transcript.slice(0, 80) || "(empty)";
          const duration = session.endedAt
            ? formatDuration(
                new Date(session.endedAt).getTime() - date.getTime()
              )
            : "in progress";

          return (
            <div
              key={session.id}
              onClick={() => onSelect(session)}
              className="glass"
              style={{
                border: "1px solid var(--border)",
                borderRadius: 16,
                padding: 12,
                cursor: "pointer",
                transition: "all 0.2s ease",
              }}
              onMouseEnter={(e) =>
                (e.currentTarget.style.background = "var(--surface-hover)")
              }
              onMouseLeave={(e) =>
                (e.currentTarget.style.background = "var(--surface)")
              }
            >
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  marginBottom: 4,
                }}
              >
                <span style={{ fontSize: 12, color: "var(--text-muted)" }}>
                  {date.toLocaleDateString()} {date.toLocaleTimeString()}
                  {session.appName && ` — ${session.appName}`}
                </span>
                <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                  <span style={{ fontSize: 11, color: "var(--text-muted)" }}>
                    {duration}
                  </span>
                  <button
                    className="btn-ghost"
                    onClick={(e) => {
                      e.stopPropagation();
                      onDelete(session.id);
                    }}
                    style={{ fontSize: 12, padding: "2px 6px" }}
                  >
                    Delete
                  </button>
                </div>
              </div>
              <div
                style={{
                  fontSize: 13,
                  overflow: "hidden",
                  textOverflow: "ellipsis",
                  whiteSpace: "nowrap",
                }}
              >
                {preview}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function formatDuration(ms: number): string {
  const seconds = Math.floor(ms / 1000);
  if (seconds < 60) return `${seconds}s`;
  const minutes = Math.floor(seconds / 60);
  const remaining = seconds % 60;
  return `${minutes}m ${remaining}s`;
}
