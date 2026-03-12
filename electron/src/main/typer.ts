import { clipboard } from "electron";

/**
 * Types text into the currently focused field by simulating keyboard input.
 *
 * For final words: uses clipboard paste for reliability (saves and restores clipboard).
 * For provisional words: uses clipboard paste with tracking for replacement.
 */

let provisionalLength = 0;
let savedClipboard: string | null = null;

function saveClipboard(): void {
  try {
    savedClipboard = clipboard.readText();
  } catch {
    savedClipboard = null;
  }
}

function restoreClipboard(): void {
  if (savedClipboard !== null) {
    try {
      clipboard.writeText(savedClipboard);
    } catch {
      // Ignore
    }
    savedClipboard = null;
  }
}

/**
 * Remove the current provisional text by sending backspace keys.
 * Returns a promise that resolves after the key simulation.
 */
async function removeProvisional(): Promise<void> {
  if (provisionalLength === 0) return;

  // We need to use keyboard simulation for backspaces
  // Import dynamically to avoid issues in test environments
  const { keyboard, Key } = await import("@nut-tree-fork/nut-js");

  for (let i = 0; i < provisionalLength; i++) {
    await keyboard.pressKey(Key.Backspace);
    await keyboard.releaseKey(Key.Backspace);
  }
  provisionalLength = 0;
}

/**
 * Paste text via clipboard, then restore the original clipboard contents.
 */
async function pasteText(text: string): Promise<void> {
  const { keyboard, Key } = await import("@nut-tree-fork/nut-js");

  saveClipboard();
  clipboard.writeText(text);

  // Ctrl+V / Cmd+V
  const modifier =
    process.platform === "darwin" ? Key.LeftSuper : Key.LeftControl;
  await keyboard.pressKey(modifier, Key.V);
  await keyboard.releaseKey(modifier, Key.V);

  // Small delay before restoring clipboard
  await new Promise((resolve) => setTimeout(resolve, 50));
  restoreClipboard();
}

/**
 * Insert final transcribed text into the focused field.
 * Removes any existing provisional text first.
 */
export async function typeFinalText(text: string): Promise<void> {
  await removeProvisional();
  await pasteText(text);
  provisionalLength = 0;
}

/**
 * Insert provisional (non-final) text into the focused field.
 * Removes the previous provisional text first, then pastes new provisional.
 */
export async function typeProvisionalText(text: string): Promise<void> {
  await removeProvisional();
  if (text.length > 0) {
    await pasteText(text);
    provisionalLength = text.length;
  }
}

/**
 * Reset provisional tracking (e.g., when recording stops).
 */
export function resetProvisional(): void {
  provisionalLength = 0;
  savedClipboard = null;
}
