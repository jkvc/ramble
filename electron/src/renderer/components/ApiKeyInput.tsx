import React, { useState } from "react";

export function ApiKeyInput({ onSave }: { onSave: (key: string) => void }) {
  const [key, setKey] = useState("");

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = key.trim();
    if (trimmed) {
      onSave(trimmed);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <p style={{ color: "var(--text-muted)", marginBottom: 16 }}>
        Enter your Soniox API key to get started. You can get one at{" "}
        <a
          href="https://soniox.com"
          target="_blank"
          rel="noopener"
          style={{ color: "var(--accent)" }}
        >
          soniox.com
        </a>
      </p>
      <input
        type="password"
        value={key}
        onChange={(e) => setKey(e.target.value)}
        placeholder="Enter your Soniox API key"
        style={{ marginBottom: 12 }}
      />
      <button type="submit" className="btn-primary" style={{ width: "100%" }}>
        Save API Key
      </button>
    </form>
  );
}
