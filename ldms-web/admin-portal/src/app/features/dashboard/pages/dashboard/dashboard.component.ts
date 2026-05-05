import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ChartData, ChartOptions } from 'chart.js';
import { Subject, takeUntil, timer } from 'rxjs';

interface KpiCard {
  label: string;
  value: string;
  trend: string;
  up: boolean;
  icon: string;
  iconBg: string;
  iconColor: string;
  spark: number[];
}

interface ShipmentRow {
  reg: string;
  from: string;
  to: string;
  status: string;
  statusLabel: string;
  eta: string;
}

interface PipelineRow {
  label: string;
  color: string;
  count: number;
  pct: number;
}

interface MapPin {
  x: number;
  y: number;
  type: string;
  label: string;
}

interface QuickAction {
  icon: string;
  label: string;
  count: string;
  route: string;
}

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class DashboardComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly kycBaseData = [12, 19, 8, 24, 16, 31, 13];
  private liveTick = 0;

  loading = true;
  activePeriod = '1M';
  searchTerm = '';
  selectedRegion = 'All Hubs';
  selectedWindow = 'Last 30 days';

  readonly periods = ['1W', '1M', '3M', '1Y'] as const;
  readonly regions = ['All Hubs', 'Harare Hub', 'Bulawayo Hub', 'Mutare Hub', 'Beitbridge Hub'];
  readonly timeWindows = ['Today', 'Last 7 days', 'Last 30 days', 'Quarter to date'];

  kpiCards: KpiCard[] = [
    {
      label: 'Total Organizations',
      value: '247',
      trend: '+12.4%',
      up: true,
      icon: 'corporate_fare',
      iconBg: 'var(--primary-light)',
      iconColor: 'var(--primary)',
      spark: [40, 65, 45, 80, 55, 90, 70, 100],
    },
    {
      label: 'KYC Pending',
      value: '13',
      trend: '-3.1%',
      up: false,
      icon: 'pending_actions',
      iconBg: 'var(--warning-light)',
      iconColor: 'var(--warning)',
      spark: [80, 60, 75, 50, 85, 45, 70, 55],
    },
    {
      label: 'Active Trips',
      value: '38',
      trend: '+8.7%',
      up: true,
      icon: 'local_shipping',
      iconBg: 'var(--success-light)',
      iconColor: 'var(--success)',
      spark: [30, 55, 40, 70, 45, 80, 60, 90],
    },
    {
      label: 'Invoices Due',
      value: '$84K',
      trend: '+5.2%',
      up: true,
      icon: 'receipt',
      iconBg: 'var(--analytics-light)',
      iconColor: 'var(--analytics)',
      spark: [60, 40, 80, 55, 70, 45, 85, 65],
    },
  ];

  shipments: ShipmentRow[] = [
    {
      reg: 'ZW-1234-A',
      from: 'Harare',
      to: 'Bulawayo',
      status: 'transit',
      statusLabel: 'In Transit',
      eta: '14:30',
    },
    {
      reg: 'ZW-5678-B',
      from: 'Mutare',
      to: 'Beitbridge',
      status: 'approved',
      statusLabel: 'Approved',
      eta: '17:00',
    },
    {
      reg: 'ZW-9012-C',
      from: 'Gweru',
      to: 'Harare',
      status: 'stage1',
      statusLabel: 'Stage 1',
      eta: '09:00',
    },
    {
      reg: 'ZW-3456-D',
      from: 'Masvingo',
      to: 'Bulawayo',
      status: 'submitted',
      statusLabel: 'Submitted',
      eta: '11:45',
    },
    {
      reg: 'ZW-7890-E',
      from: 'Harare',
      to: 'Chirundu',
      status: 'transit',
      statusLabel: 'In Transit',
      eta: '16:15',
    },
  ];

  pipeline: PipelineRow[] = [
    { label: 'Submitted', color: '#F59E0B', count: 4, pct: 30 },
    { label: 'Stage 1', color: '#3B82F6', count: 5, pct: 38 },
    { label: 'Stage 2', color: '#8B5CF6', count: 4, pct: 30 },
    { label: 'Approved', color: '#22C55E', count: 48, pct: 100 },
    { label: 'Rejected', color: '#EF4444', count: 3, pct: 23 },
  ];

  private readonly mapPinOrigins: MapPin[] = [
    { x: 28, y: 38, type: 'primary', label: 'ZW-1234-A' },
    { x: 62, y: 25, type: 'secondary', label: 'ZW-5678-B' },
    { x: 72, y: 58, type: 'warning', label: 'ZW-9012-C' },
    { x: 18, y: 68, type: 'primary', label: 'ZW-3456-D' },
  ];

  mapPins: MapPin[] = [...this.mapPinOrigins];

  /** Shown on the map stat chip; nudged on each live tick. */
  liveOnTimePct = 96.2;

  quickActions: QuickAction[] = [
    { icon: 'verified_user', label: 'Review KYC', count: '13', route: '/kyc/applications' },
    { icon: 'corporate_fare', label: 'Organizations', count: '247', route: '/organizations' },
    { icon: 'folder_open', label: 'Documents', count: '8', route: '/kyc/documents' },
    { icon: 'people_outline', label: 'Users', count: '42', route: '/users' },
  ];

  kycChartData: ChartData<'bar'> = {
    labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul'],
    datasets: [
      {
        label: 'Applications',
        data: [12, 19, 8, 24, 16, 31, 13],
        backgroundColor: ['#3B82F6', '#60A5FA', '#8B5CF6', '#3B82F6', '#93C5FD', '#8B5CF6', '#3B82F6'],
        borderRadius: 6,
        hoverBackgroundColor: '#1E3A8A',
      },
    ],
  };

  kycChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    animation: {
      duration: 700,
      easing: 'easeOutQuart',
    },
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: (ctx) => ` ${ctx.parsed.y} applications`,
        },
      },
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: {
          font: { size: 11, family: "'Plus Jakarta Sans'" },
          color: '#9CA3AF',
        },
      },
      y: {
        grid: { color: '#F3F4F6' },
        ticks: {
          font: { size: 11, family: "'Plus Jakarta Sans'" },
          color: '#9CA3AF',
        },
      },
    },
  };

  constructor(
    private readonly cdr: ChangeDetectorRef,
    private readonly title: Title,
  ) {}

  get totalPipelineApplications(): number {
    return this.pipeline.reduce((a, b) => a + b.count, 0);
  }

  ngOnInit(): void {
    this.title.setTitle('Dashboard | LX Admin');
    this.loading = false;
    this.cdr.markForCheck();

    timer(400, 2600)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.applyLiveTick());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private applyLiveTick(): void {
    this.liveTick += 1;
    const t = this.liveTick * 0.35;

    const ds0 = this.kycChartData.datasets[0];
    const nextData = this.kycBaseData.map((v, i) => {
      const wave = Math.sin(t + i * 0.55) * 4;
      const noise = (Math.random() - 0.5) * 2.5;
      return Math.max(3, Math.round(v + wave + noise));
    });
    this.kycChartData = {
      labels: [...(this.kycChartData.labels ?? [])],
      datasets: [
        {
          ...ds0,
          data: nextData,
        },
      ],
    };

    this.kpiCards = this.kpiCards.map((k) => ({
      ...k,
      spark: [...k.spark.slice(1), 18 + Math.round(Math.random() * 82)],
    }));

    this.mapPins = this.mapPinOrigins.map((p, i) => ({
      ...p,
      x: Math.min(88, Math.max(12, p.x + Math.sin(t * 0.8 + i * 1.1) * 2.8)),
      y: Math.min(82, Math.max(18, p.y + Math.cos(t * 0.7 + i * 0.9) * 2.2)),
    }));

    this.liveOnTimePct = Math.min(99.4, Math.max(93.5, 96.2 + Math.sin(t * 0.4) * 1.8 + (Math.random() - 0.5) * 0.35));

    this.cdr.markForCheck();
  }

  setPeriod(p: string): void {
    this.activePeriod = p;
    this.cdr.markForCheck();
  }

  trackByReg = (_: number, s: ShipmentRow): string => s.reg;

  trackByLabel = (_: number, p: PipelineRow): string => p.label;
}
