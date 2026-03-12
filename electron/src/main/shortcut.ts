import { globalShortcut } from "electron";

let currentShortcut: string | null = null;

export function registerShortcut(
  accelerator: string,
  callback: () => void
): boolean {
  unregisterShortcut();

  try {
    const success = globalShortcut.register(accelerator, callback);
    if (success) {
      currentShortcut = accelerator;
    }
    return success;
  } catch {
    return false;
  }
}

export function unregisterShortcut(): void {
  if (currentShortcut) {
    try {
      globalShortcut.unregister(currentShortcut);
    } catch {
      // Ignore
    }
    currentShortcut = null;
  }
}

export function unregisterAll(): void {
  globalShortcut.unregisterAll();
  currentShortcut = null;
}
