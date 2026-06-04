import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { Title } from '@angular/platform-browser';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CurrentUserService } from '@core/services/current-user.service';
import { StorageService } from '@core/services/storage.service';
import { KycQueueStatsService } from '@core/services/kyc-queue-stats.service';
import type { KycQueueSummary } from '../../../organizations/services/organizations-admin.service';
import { UsersAdminService } from '../../../users/services/users-admin.service';
import { KycDocumentsAdminService } from '../../../kyc/services/kyc-documents-admin.service';
import { formatWelcomeMessage } from '@core/utils/welcome-message.util';
import {
  LxExportFormat,
  exportClientTableAsCsv,
} from '@shared/utils/lx-export.util';
import { ChartData, ChartOptions } from 'chart.js';
import { Subject, takeUntil, timer } from 'rxjs';
import { ensureChartJsRegistered } from '../../chartjs-register';

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

interface DashboardStatTile {
  key: string;
  label: string;
  value: number;
  icon: string;
  tone: string;
  filterStatus?: string;
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
  kycLoading = true;
  kycSnapshot: KycQueueSummary | null = null;
  kycStatTiles: DashboardStatTile[] = [];
  welcomeMessage = 'Welcome';
  greetingFirstName = '';
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
      value: '—',
      trend: 'Live sync',
      up: true,
      icon: 'corporate_fare',
      iconBg: 'var(--primary-light)',
      iconColor: 'var(--primary)',
      spark: [40, 65, 45, 80, 55, 90, 70, 100],
    },
    {
      label: 'KYC Pending',
      value: '—',
      trend: 'In review queue',
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

  pipeline: PipelineRow[] = [];

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
    { icon: 'verified_user', label: 'Review KYC', count: '—', route: '/kyc/applications' },
    { icon: 'corporate_fare', label: 'Organizations', count: '—', route: '/organizations' },
    { icon: 'folder_open', label: 'Documents', count: '—', route: '/kyc/documents' },
    { icon: 'people_outline', label: 'Users', count: '—', route: '/users' },
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
    private readonly snackBar: MatSnackBar,
    private readonly currentUser: CurrentUserService,
    private readonly kycStats: KycQueueStatsService,
    private readonly usersAdmin: UsersAdminService,
    private readonly kycDocuments: KycDocumentsAdminService,
    private readonly storage: StorageService,
  ) {}

  get filteredShipments(): ShipmentRow[] {
    const q = this.searchTerm.trim().toLowerCase();
    if (!q) {
      return this.shipments;
    }
    return this.shipments.filter(
      (s) =>
        s.reg.toLowerCase().includes(q) ||
        s.from.toLowerCase().includes(q) ||
        s.to.toLowerCase().includes(q) ||
        s.statusLabel.toLowerCase().includes(q),
    );
  }

  get totalPipelineApplications(): number {
    return this.pipeline.reduce((a, b) => a + b.count, 0);
  }

  ngOnInit(): void {
    this.title.setTitle('Dashboard | LX Admin');
    this.currentUser.user$.pipe(takeUntil(this.destroy$)).subscribe((user) => {
      const firstName = this.resolveFirstName(user?.firstName);
      this.greetingFirstName = firstName;
      this.welcomeMessage = formatWelcomeMessage({
        firstName,
        displayName: user?.displayName,
        email: user?.email,
      });
      this.cdr.markForCheck();
    });
    this.kycStats.summary$.pipe(takeUntil(this.destroy$)).subscribe((summary) => {
      this.kycSnapshot = summary;
      this.kycLoading = summary == null;
      this.applySummaryToShell(summary);
      this.applyKycPipelineSummary(summary);
      this.cdr.markForCheck();
    });
    this.loading = false;
    this.cdr.markForCheck();
    setTimeout(() => this.bootstrapDashboardData(), 0);
  }

  private bootstrapDashboardData(): void {
    ensureChartJsRegistered();
    const token = this.storage.getToken();
    const needsProfile =
      token &&
      !token.startsWith('mock-token-') &&
      (this.storage.getRoles().length === 0 || !this.currentUser.snapshot);
    if (needsProfile) {
      this.currentUser.refreshFromApi().pipe(takeUntil(this.destroy$)).subscribe(() => {
        this.cdr.markForCheck();
      });
    }
    if (this.kycStats.snapshot == null) {
      this.kycStats.refresh().pipe(takeUntil(this.destroy$)).subscribe({
        next: () => {
          this.kycLoading = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.kycLoading = false;
          this.cdr.markForCheck();
        },
      });
    } else {
      this.kycLoading = false;
    }
    this.loadQuickActionCounts();
    timer(400, 2600)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.applyLiveTick());
    this.cdr.markForCheck();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private applySummaryToShell(summary: KycQueueSummary | null): void {
    if (!summary) {
      this.kycStatTiles = [];
      return;
    }

    const totalOrgs = summary.totalOrganizations ?? 0;
    const pending = summary.totalInQueue ?? 0;
    const approved = summary.approvedCount ?? 0;

    this.kpiCards = this.kpiCards.map((card) => {
      if (card.label === 'Total Organizations') {
        return {
          ...card,
          value: String(totalOrgs),
          trend: approved > 0 ? `${approved} approved` : 'All organisations',
          up: true,
        };
      }
      if (card.label === 'KYC Pending') {
        return {
          ...card,
          value: String(pending),
          trend: pending > 0 ? 'Awaiting review' : 'Queue clear',
          up: pending === 0,
        };
      }
      return card;
    });

    this.setQuickActionCount('/kyc/applications', pending);
    this.setQuickActionCount('/organizations', totalOrgs);

    this.kycStatTiles = [
      { key: 'queue', label: 'In queue', value: pending, icon: 'inbox', tone: 'queue' },
      { key: 'draft', label: 'Draft', value: summary.draftCount ?? 0, icon: 'edit_note', tone: 'draft', filterStatus: 'DRAFT' },
      {
        key: 'submitted',
        label: 'Submitted',
        value: summary.submittedCount ?? 0,
        icon: 'outgoing_mail',
        tone: 'submitted',
        filterStatus: 'SUBMITTED',
      },
      {
        key: 'stage1',
        label: 'Stage 1',
        value: summary.stage1Count ?? 0,
        icon: 'looks_one',
        tone: 'stage1',
        filterStatus: 'STAGE_1_REVIEW',
      },
      {
        key: 'stage2',
        label: 'Stage 2',
        value: summary.stage2Count ?? 0,
        icon: 'looks_two',
        tone: 'stage2',
        filterStatus: 'STAGE_2_REVIEW',
      },
      {
        key: 'stage3',
        label: 'Stage 3',
        value: summary.stage3Count ?? 0,
        icon: 'looks_3',
        tone: 'stage3',
        filterStatus: 'STAGE_3_REVIEW',
      },
      {
        key: 'stage4',
        label: 'Stage 4',
        value: summary.stage4Count ?? 0,
        icon: 'looks_4',
        tone: 'stage4',
        filterStatus: 'STAGE_4_REVIEW',
      },
      {
        key: 'stage5',
        label: 'Stage 5',
        value: summary.stage5Count ?? 0,
        icon: 'looks_5',
        tone: 'stage5',
        filterStatus: 'STAGE_5_REVIEW',
      },
      {
        key: 'rejected',
        label: 'Rejected',
        value: summary.rejectedCount ?? 0,
        icon: 'block',
        tone: 'rejected',
        filterStatus: 'REJECTED',
      },
      {
        key: 'approved',
        label: 'Approved',
        value: approved,
        icon: 'verified',
        tone: 'approved',
        filterStatus: 'APPROVED',
      },
    ];
  }

  private loadQuickActionCounts(): void {
    this.usersAdmin
      .queryUsers({ page: 0, size: 1, searchQuery: '', columnFilters: {} })
      .pipe(takeUntil(this.destroy$))
      .subscribe(({ totalElements }) => {
        this.setQuickActionCount('/users', totalElements);
        this.cdr.markForCheck();
      });

    this.kycDocuments
      .countTotal()
      .pipe(takeUntil(this.destroy$))
      .subscribe((total) => {
        this.setQuickActionCount('/kyc/documents', total);
        this.cdr.markForCheck();
      });
  }

  private setQuickActionCount(route: string, count: number | null | undefined): void {
    this.quickActions = this.quickActions.map((action) =>
      action.route === route
        ? { ...action, count: count == null ? '—' : String(count) }
        : action,
    );
  }

  private applyKycPipelineSummary(summary: KycQueueSummary | null): void {
    if (!summary) {
      this.pipeline = [];
      return;
    }
    const base = [
      { label: 'Draft', color: '#94A3B8', count: summary.draftCount ?? 0 },
      { label: 'Submitted', color: '#F59E0B', count: summary.submittedCount ?? 0 },
      { label: 'Stage 1', color: '#3B82F6', count: summary.stage1Count ?? 0 },
      { label: 'Stage 2', color: '#8B5CF6', count: summary.stage2Count ?? 0 },
      { label: 'Stage 3', color: '#6366F1', count: summary.stage3Count ?? 0 },
      { label: 'Stage 4', color: '#0EA5E9', count: summary.stage4Count ?? 0 },
      { label: 'Stage 5', color: '#14B8A6', count: summary.stage5Count ?? 0 },
      { label: 'Approved', color: '#22C55E', count: summary.approvedCount ?? 0 },
      { label: 'Rejected', color: '#EF4444', count: summary.rejectedCount ?? 0 },
    ].filter((row) => row.count > 0 || row.label === 'Draft' || row.label === 'Submitted');
    const max = Math.max(1, ...base.map((r) => r.count));
    this.pipeline = base.map((r) => ({
      ...r,
      pct: Math.round((r.count / max) * 100),
    }));

    const chartCounts = base.map((r) => r.count);
    const chartColors = base.map((r) => r.color);
    this.kycChartData = {
      labels: base.map((r) => r.label),
      datasets: [
        {
          label: 'Applications',
          data: chartCounts,
          backgroundColor: chartColors,
          borderRadius: 6,
          hoverBackgroundColor: '#1E3A8A',
        },
      ],
    };
  }

  private applyLiveTick(): void {
    this.liveTick += 1;
    const t = this.liveTick * 0.35;

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

  trackByStatKey = (_: number, tile: DashboardStatTile): string => tile.key;

  trackByRecentId = (_: number, row: { id: number }): number => row.id;

  kycFilterLink(tile: DashboardStatTile): string[] {
    return ['/kyc/applications'];
  }

  kycFilterQuery(tile: DashboardStatTile): Record<string, string> | null {
    if (!tile.filterStatus) {
      return null;
    }
    return { status: tile.filterStatus };
  }

  exportSnapshot(format: LxExportFormat): void {
    const ok = exportClientTableAsCsv(
      format,
      this.filteredShipments,
      [
        { header: 'reg', value: (r) => r.reg },
        { header: 'from', value: (r) => r.from },
        { header: 'to', value: (r) => r.to },
        { header: 'status', value: (r) => r.statusLabel },
        { header: 'eta', value: (r) => r.eta },
      ],
      'dashboard-shipments',
      (message) => this.snackBar.open(message, 'Close', { duration: 4500 }),
    );
    if (ok) {
      this.snackBar.open('Exported dashboard shipments as CSV.', 'Close', {
        duration: 3500,
        panelClass: ['app-snackbar-success'],
      });
    }
  }

  private resolveFirstName(firstName?: string | null): string {
    const direct = String(firstName ?? '').trim();
    if (direct && direct.toLowerCase() !== 'user') {
      return direct;
    }
    return '';
  }
}
