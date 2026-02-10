// Polyfills for Node.js globals in browser environment
// This must be loaded BEFORE any other code

// Define global immediately
if (typeof (window as any).global === 'undefined') {
  (window as any).global = window;
}

// Define process
if (typeof (window as any).process === 'undefined') {
  (window as any).process = {
    env: { DEBUG: undefined },
    version: '',
    nextTick: (fn: Function) => setTimeout(fn, 0)
  };
}

// Import and define Buffer
import { Buffer } from 'buffer';
if (typeof (window as any).Buffer === 'undefined') {
  (window as any).Buffer = Buffer;
}

export {};