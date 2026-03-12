// Node 22+ ships a built-in localStorage that lacks standard Web Storage API methods.
// jsdom provides a proper implementation, but Node's global can shadow it.
// This setup ensures the jsdom localStorage is used.
import { JSDOM } from "jsdom";

const dom = new JSDOM("", { url: "http://localhost" });
Object.defineProperty(globalThis, "localStorage", {
  value: dom.window.localStorage,
  writable: true,
  configurable: true,
});
