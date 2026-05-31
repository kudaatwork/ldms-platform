import { Chart, registerables } from 'chart.js';

let registered = false;

/** Registers Chart.js once (lazy — not in main.ts to avoid Vite prebundle chunk drift). */
export function ensureChartJsRegistered(): void {
  if (registered) {
    return;
  }
  Chart.register(...registerables);
  registered = true;
}
