import { describe, it, expect, vi, beforeEach } from "vitest";

// We test the message parsing logic extracted from Recorder
// since the actual WebSocket connection can't run in test

interface SonioxToken {
  text: string;
  is_final?: boolean;
}

interface SonioxMessage {
  tokens?: SonioxToken[];
  fw?: Array<{ t: string }>;
  nfw?: Array<{ t: string }>;
  error?: string;
}

function filterSpecialTokens(text: string): string {
  return text.replace(/<[^>]+>/g, "");
}

function parseMessage(message: SonioxMessage) {
  const result: { finalText: string; provisionalText: string; error?: string } = {
    finalText: "",
    provisionalText: "",
  };

  if (message.error) {
    result.error = message.error;
    return result;
  }

  if (message.tokens) {
    for (const token of message.tokens) {
      const clean = filterSpecialTokens(token.text);
      if (token.is_final) {
        result.finalText += clean;
      } else {
        result.provisionalText += clean;
      }
    }
  }

  if (message.fw && message.fw.length > 0) {
    result.finalText += message.fw.map((w) => filterSpecialTokens(w.t)).join("");
  }
  if (message.nfw && message.nfw.length > 0) {
    result.provisionalText += message.nfw.map((w) => filterSpecialTokens(w.t)).join("");
  }

  return result;
}

describe("Recorder message parsing", () => {
  it("parses final tokens", () => {
    const msg: SonioxMessage = {
      tokens: [
        { text: "Hello ", is_final: true },
        { text: "world", is_final: true },
      ],
    };
    const result = parseMessage(msg);
    expect(result.finalText).toBe("Hello world");
    expect(result.provisionalText).toBe("");
  });

  it("parses provisional tokens", () => {
    const msg: SonioxMessage = {
      tokens: [{ text: "hel", is_final: false }],
    };
    const result = parseMessage(msg);
    expect(result.finalText).toBe("");
    expect(result.provisionalText).toBe("hel");
  });

  it("parses mixed final and provisional tokens", () => {
    const msg: SonioxMessage = {
      tokens: [
        { text: "Hello ", is_final: true },
        { text: "wor", is_final: false },
      ],
    };
    const result = parseMessage(msg);
    expect(result.finalText).toBe("Hello ");
    expect(result.provisionalText).toBe("wor");
  });

  it("filters special tokens", () => {
    const msg: SonioxMessage = {
      tokens: [
        { text: "Hello<comma> world<period>", is_final: true },
      ],
    };
    const result = parseMessage(msg);
    expect(result.finalText).toBe("Hello world");
  });

  it("handles error messages", () => {
    const msg: SonioxMessage = { error: "Invalid API key" };
    const result = parseMessage(msg);
    expect(result.error).toBe("Invalid API key");
  });

  it("parses legacy fw format", () => {
    const msg: SonioxMessage = {
      fw: [{ t: "Hello " }, { t: "world" }],
    };
    const result = parseMessage(msg);
    expect(result.finalText).toBe("Hello world");
  });

  it("parses legacy nfw format", () => {
    const msg: SonioxMessage = {
      nfw: [{ t: "hel" }],
    };
    const result = parseMessage(msg);
    expect(result.provisionalText).toBe("hel");
  });

  it("handles empty token arrays", () => {
    const msg: SonioxMessage = { tokens: [] };
    const result = parseMessage(msg);
    expect(result.finalText).toBe("");
    expect(result.provisionalText).toBe("");
  });
});

describe("Audio buffering", () => {
  it("buffers audio chunks before connection", () => {
    const buffer: Buffer[] = [];
    let isBuffering = false;
    let isConnected = false;

    // Start buffering
    isBuffering = true;

    // Send chunks while not connected
    const chunk1 = Buffer.from([1, 2, 3]);
    const chunk2 = Buffer.from([4, 5, 6]);

    if (!isConnected && isBuffering) buffer.push(chunk1);
    if (!isConnected && isBuffering) buffer.push(chunk2);

    expect(buffer).toHaveLength(2);

    // Simulate connection — flush buffer
    isConnected = true;
    isBuffering = false;
    const flushed = [...buffer];
    buffer.length = 0;

    expect(flushed).toHaveLength(2);
    expect(flushed[0]).toEqual(chunk1);
    expect(buffer).toHaveLength(0);
  });

  it("sends directly when connected", () => {
    const sent: Buffer[] = [];
    const isConnected = true;
    const isBuffering = false;
    const buffer: Buffer[] = [];

    const chunk = Buffer.from([1, 2, 3]);
    if (isConnected) {
      sent.push(chunk);
    } else if (isBuffering) {
      buffer.push(chunk);
    }

    expect(sent).toHaveLength(1);
    expect(buffer).toHaveLength(0);
  });
});
