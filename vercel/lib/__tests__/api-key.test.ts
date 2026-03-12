import { describe, it, expect, beforeEach } from "vitest";
import { getApiKey, setApiKey, clearApiKey } from "../api-key";

describe("api-key", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("returns null when no key is stored", () => {
    expect(getApiKey()).toBeNull();
  });

  it("stores and retrieves a key", () => {
    setApiKey("test-key-123");
    expect(getApiKey()).toBe("test-key-123");
  });

  it("overwrites an existing key", () => {
    setApiKey("first-key");
    setApiKey("second-key");
    expect(getApiKey()).toBe("second-key");
  });

  it("clears a stored key", () => {
    setApiKey("test-key");
    clearApiKey();
    expect(getApiKey()).toBeNull();
  });

  it("clearing when no key exists does not throw", () => {
    expect(() => clearApiKey()).not.toThrow();
  });
});
