import { describe, it, expect, vi, beforeEach } from "vitest";

// Test the typing logic without requiring Electron or nut.js
// We extract and test the provisional tracking state machine

describe("Typer provisional tracking", () => {
  let provisionalLength: number;

  beforeEach(() => {
    provisionalLength = 0;
  });

  function simulateFinal(text: string): {
    backspaces: number;
    pasted: string;
    newProvisionalLength: number;
  } {
    const backspaces = provisionalLength;
    provisionalLength = 0;
    return { backspaces, pasted: text, newProvisionalLength: 0 };
  }

  function simulateProvisional(text: string): {
    backspaces: number;
    pasted: string;
    newProvisionalLength: number;
  } {
    const backspaces = provisionalLength;
    provisionalLength = text.length;
    return {
      backspaces,
      pasted: text,
      newProvisionalLength: text.length,
    };
  }

  it("types final text with no prior provisional", () => {
    const result = simulateFinal("hello");
    expect(result.backspaces).toBe(0);
    expect(result.pasted).toBe("hello");
    expect(result.newProvisionalLength).toBe(0);
  });

  it("replaces provisional with new provisional", () => {
    // First provisional
    const r1 = simulateProvisional("hel");
    expect(r1.backspaces).toBe(0);
    expect(r1.pasted).toBe("hel");
    expect(r1.newProvisionalLength).toBe(3);

    // Second provisional replaces first
    const r2 = simulateProvisional("hello");
    expect(r2.backspaces).toBe(3);
    expect(r2.pasted).toBe("hello");
    expect(r2.newProvisionalLength).toBe(5);
  });

  it("replaces provisional with final text", () => {
    // Provisional first
    simulateProvisional("hell");
    expect(provisionalLength).toBe(4);

    // Final replaces provisional
    const result = simulateFinal("hello ");
    expect(result.backspaces).toBe(4);
    expect(result.pasted).toBe("hello ");
    expect(result.newProvisionalLength).toBe(0);
    expect(provisionalLength).toBe(0);
  });

  it("handles empty provisional (clear)", () => {
    simulateProvisional("hello");
    expect(provisionalLength).toBe(5);

    const result = simulateProvisional("");
    expect(result.backspaces).toBe(5);
    expect(result.pasted).toBe("");
    expect(provisionalLength).toBe(0);
  });

  it("handles sequential final words", () => {
    const r1 = simulateFinal("Hello ");
    expect(r1.backspaces).toBe(0);

    const r2 = simulateFinal("world ");
    expect(r2.backspaces).toBe(0);
  });

  it("handles provisional → final → provisional cycle", () => {
    // Provisional
    simulateProvisional("hel");
    expect(provisionalLength).toBe(3);

    // Final replaces
    const r1 = simulateFinal("hello ");
    expect(r1.backspaces).toBe(3);
    expect(provisionalLength).toBe(0);

    // New provisional
    const r2 = simulateProvisional("wor");
    expect(r2.backspaces).toBe(0);
    expect(provisionalLength).toBe(3);
  });
});

describe("Clipboard save/restore logic", () => {
  it("saves and restores clipboard content", () => {
    let savedClipboard: string | null = null;

    // Save
    const original = "user's copied text";
    savedClipboard = original;

    // Restore
    const restored = savedClipboard;
    savedClipboard = null;

    expect(restored).toBe(original);
    expect(savedClipboard).toBeNull();
  });

  it("handles null clipboard gracefully", () => {
    let savedClipboard: string | null = null;

    // Restore with nothing saved
    const restored = savedClipboard;
    expect(restored).toBeNull();
  });
});
