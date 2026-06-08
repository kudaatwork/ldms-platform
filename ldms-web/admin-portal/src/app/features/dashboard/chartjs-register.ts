import {
  ArcElement,
  BarController,
  BarElement,
  CategoryScale,
  Chart,
  DoughnutController,
  Legend,
  LinearScale,
  Tooltip,
} from 'chart.js';

let registered = false;

/** Registers chart pieces the dashboard uses (avoids heavy full registerables on login). */
export function ensureChartJsRegistered(): void {
  if (registered) {
    return;
  }
  Chart.register(
    BarController,
    BarElement,
    DoughnutController,
    ArcElement,
    CategoryScale,
    LinearScale,
    Tooltip,
    Legend,
  );
  registered = true;
}
