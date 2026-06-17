import type { Chart, ChartOptions, TooltipItem } from 'chart.js';
import { LX_CHART_ANIMATION, LX_CHART_COLORS, LX_CHART_FONT } from './lx-chart-palettes';

type DoughnutLegendPosition = 'bottom' | 'right';

export interface LxDoughnutChartOptions {
  cutout?: string;
  legendPosition?: DoughnutLegendPosition;
  showLegend?: boolean;
}

function lxBasePlugins() {
  return {
    legend: { display: false },
    tooltip: {
      backgroundColor: LX_CHART_COLORS.tooltipBg,
      titleFont: { family: LX_CHART_FONT, size: 12, weight: 'bold' as const },
      bodyFont: { family: LX_CHART_FONT, size: 12 },
      padding: 12,
      cornerRadius: 10,
      displayColors: true,
      boxPadding: 4,
    },
  };
}

function lxCartesianScales(maxTicksLimit = 5) {
  return {
    x: {
      grid: { display: false },
      border: { display: false },
      ticks: {
        font: { size: 11, family: LX_CHART_FONT, weight: 'bold' as const },
        color: LX_CHART_COLORS.tick,
        padding: 8,
      },
    },
    y: {
      beginAtZero: true,
      grid: { color: LX_CHART_COLORS.grid, drawTicks: false },
      border: { display: false, dash: [4, 4] as number[] },
      ticks: {
        font: { size: 11, family: LX_CHART_FONT },
        color: LX_CHART_COLORS.tick,
        padding: 10,
        maxTicksLimit,
      },
    },
  };
}

/** Vertical gradient fill for line/area charts. */
export function lxChartAreaGradient(
  chart: Chart,
  topColor: string,
  bottomColor = 'rgba(59, 130, 246, 0)',
): CanvasGradient | string {
  const { ctx, chartArea } = chart;
  if (!chartArea) {
    return topColor;
  }
  const gradient = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);
  gradient.addColorStop(0, topColor);
  gradient.addColorStop(1, bottomColor);
  return gradient;
}

/** Per-bar vertical gradient for bar charts. */
export function lxChartBarGradient(chart: Chart, baseColor: string): CanvasGradient | string {
  const { ctx, chartArea } = chart;
  if (!chartArea) {
    return baseColor;
  }
  const gradient = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);
  gradient.addColorStop(0, baseColor);
  gradient.addColorStop(1, baseColor.replace(/[\d.]+\)$/, '0.45)').replace('rgb(', 'rgba('));
  return gradient;
}

export function lxDoughnutPercentLabel(ctx: TooltipItem<'doughnut'>): string {
  const total = (ctx.dataset.data as number[]).reduce((a, b) => a + b, 0);
  const pct = total > 0 ? Math.round(((ctx.parsed as number) / total) * 100) : 0;
  return ` ${ctx.parsed} (${pct}%)`;
}

export function lxMoneyAxisTick(value: string | number): string {
  const n = Number(value);
  if (!Number.isFinite(n)) {
    return String(value);
  }
  if (n >= 100_000) {
    return '$' + (n / 100).toLocaleString(undefined, { maximumFractionDigits: 0 });
  }
  return '$' + (n / 100).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 0 });
}

export function lxBarChartOptions(
  overrides?: Partial<ChartOptions<'bar'>>,
): ChartOptions<'bar'> {
  return {
    responsive: true,
    maintainAspectRatio: false,
    animation: LX_CHART_ANIMATION,
    plugins: {
      ...lxBasePlugins(),
      legend: { display: false },
      ...overrides?.plugins,
    },
    scales: lxCartesianScales(),
    ...overrides,
  };
}

export function lxGroupedBarChartOptions(
  overrides?: Partial<ChartOptions<'bar'>>,
): ChartOptions<'bar'> {
  return lxBarChartOptions({
    plugins: {
      legend: {
        display: true,
        position: 'top',
        align: 'end',
        labels: {
          usePointStyle: true,
          pointStyle: 'circle',
          padding: 16,
          font: { family: LX_CHART_FONT, size: 11, weight: 'bold' },
          color: LX_CHART_COLORS.legend,
        },
      },
    },
    scales: {
      ...lxCartesianScales(),
      y: {
        ...lxCartesianScales().y,
        ticks: {
          ...lxCartesianScales().y.ticks,
          callback: (v) => lxMoneyAxisTick(v),
        },
      },
    },
    ...overrides,
  });
}

export function lxLineChartOptions(
  overrides?: Partial<ChartOptions<'line'>>,
): ChartOptions<'line'> {
  return {
    responsive: true,
    maintainAspectRatio: false,
    animation: LX_CHART_ANIMATION,
    interaction: { mode: 'index', intersect: false },
    plugins: {
      ...lxBasePlugins(),
      tooltip: {
        ...lxBasePlugins().tooltip,
        callbacks: {
          label: (ctx) => ` ${ctx.parsed.y} shipments`,
        },
      },
      ...overrides?.plugins,
    },
    scales: lxCartesianScales(),
    elements: {
      line: { borderWidth: 2.5, tension: 0.42 },
      point: {
        radius: 4,
        hoverRadius: 7,
        borderWidth: 2,
        borderColor: '#fff',
        hoverBorderWidth: 2,
      },
    },
    ...overrides,
  };
}

export function lxDoughnutChartOptions(
  options: LxDoughnutChartOptions = {},
): ChartOptions<'doughnut'> {
  const { cutout = '70%', legendPosition = 'bottom', showLegend = true } = options;
  return {
    responsive: true,
    maintainAspectRatio: false,
    cutout,
    animation: {
      animateRotate: true,
      animateScale: true,
      ...LX_CHART_ANIMATION,
    },
    plugins: {
      legend: {
        display: showLegend,
        position: legendPosition,
        labels: {
          usePointStyle: true,
          pointStyle: 'circle',
          padding: legendPosition === 'bottom' ? 14 : 10,
          font: { family: LX_CHART_FONT, size: 10, weight: 'bold' },
          color: LX_CHART_COLORS.legend,
          boxWidth: legendPosition === 'right' ? 8 : 12,
        },
      },
      tooltip: {
        ...lxBasePlugins().tooltip,
        callbacks: {
          label: lxDoughnutPercentLabel,
        },
      },
    },
  };
}
