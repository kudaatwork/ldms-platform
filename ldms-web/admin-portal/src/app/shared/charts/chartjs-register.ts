import {
  ArcElement,
  BarController,
  BarElement,
  CategoryScale,
  Chart,
  DoughnutController,
  Filler,
  Legend,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
} from 'chart.js';

let registered = false;

/** Registers Chart.js pieces used across dashboard and analytics (lazy, once). */
export function ensureChartJsRegistered(): void {
  if (registered) {
    return;
  }
  Chart.register(
    BarController,
    BarElement,
    LineController,
    LineElement,
    PointElement,
    Filler,
    DoughnutController,
    ArcElement,
    CategoryScale,
    LinearScale,
    Tooltip,
    Legend,
  );
  registered = true;
}
