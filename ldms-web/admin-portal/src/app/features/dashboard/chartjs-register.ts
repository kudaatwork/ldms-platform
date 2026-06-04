import {
  BarController,
  BarElement,
  CategoryScale,
  Chart,
  Legend,
  LinearScale,
  Tooltip,
} from 'chart.js';

let registered = false;

/** Registers only the bar chart pieces the dashboard uses (avoids heavy full registerables on login). */
export function ensureChartJsRegistered(): void {
  if (registered) {
    return;
  }
  Chart.register(BarController, BarElement, CategoryScale, LinearScale, Tooltip, Legend);
  registered = true;
}
