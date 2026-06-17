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
import { PlatformOpsAdminService } from '@core/services/platform-ops-admin.service';
import type { PlatformCompanyOps, PlatformOpsSummary, PlatformShipmentOps } from '@core/services/platform-ops-mock.data';
import { PlatformWalletAdminService } from '../../../settings/services/platform-wallet-admin.service';
import {
  LxExportFormat,
  exportClientTableAsCsv,
  exportFormatLabel,
} from '@shared/utils/lx-export.util';
import { ChartData, ChartOptions } from 'chart.js';
import { Subject, takeUntil, timer } from 'rxjs';
import { ensureChartJsRegistered } from '@shared/charts/chartjs-register';
import {
  lxBarChartOptions,
  lxDoughnutChartOptions,
} from '@shared/charts/lx-chart-theme';
import type { LxMapMarker } from '@shared/components/lx-leaflet-map/lx-leaflet-map.model';

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
  progress: number;
  organizationName?: string;
  shipmentRef?: string;
  organizationId?: number;
  shipmentId?: number;
}

interface PipelineRow {
  label: string;
  color: string;
  count: number;
  pct: number;
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
  opsLoading = true;
  kycLoading = true;
  opsSnapshot: PlatformOpsSummary | null = null;
  topActiveCompanies: PlatformCompanyOps[] = [];
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
      value: '—',
      trend: 'Cross-tenant',
      up: true,
      icon: 'local_shipping',
      iconBg: 'var(--success-light)',
      iconColor: 'var(--success)',
      spark: [30, 55, 40, 70, 45, 80, 60, 90],
    },
    {
      label: 'Invoices Due',
      value: '—',
      trend: 'Platform-wide',
      up: true,
      icon: 'receipt',
      iconBg: 'var(--analytics-light)',
      iconColor: 'var(--analytics)',
      spark: [60, 40, 80, 55, 70, 45, 85, 65],
    },
  ];

  shipments: ShipmentRow[] = [];

  pipeline: PipelineRow[] = [];

  /** Shown on the map stat chip. */
  liveOnTimePct = 0;

  quickActions: QuickAction[] = [
    { icon: 'verified_user', label: 'Review KYC', count: '—', route: '/kyc/applications' },
    { icon: 'corporate_fare', label: 'Organizations', count: '—', route: '/organizations' },
    { icon: 'folder_open', label: 'Documents', count: '—', route: '/kyc/documents' },
    { icon: 'people_outline', label: 'Users', count: '—', route: '/users' },
  ];

  chartView: 'bars' | 'donut' = 'bars';

  kycChartData: ChartData<'bar'> = {
    labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul'],
    datasets: [
      {
        label: 'Applications',
        data: [12, 19, 8, 24, 16, 31, 13],
        backgroundColor: ['#3B82F6', '#60A5FA', '#8B5CF6', '#3B82F6', '#93C5FD', '#8B5CF6', '#3B82F6'],
        borderRadius: 10,
        borderSkipped: false,
        hoverBackgroundColor: '#1E3A8A',
        maxBarThickness: 42,
      },
    ],
  };

  kycDonutData: ChartData<'doughnut'> = {
    labels: [],
    datasets: [
      {
        data: [],
        backgroundColor: [],
        borderWidth: 0,
        hoverOffset: 8,
      },
    ],
  };

  kycChartOptions: ChartOptions<'bar'> = lxBarChartOptions({
    plugins: {
      tooltip: {
        callbacks: {
          label: (ctx) => ` ${ctx.parsed.y} applications`,
        },
      },
    },
  });

  kycDonutOptions: ChartOptions<'doughnut'> = lxDoughnutChartOptions({
    cutout: '72%',
    legendPosition: 'bottom',
  });

  constructor(
    private readonly cdr: ChangeDetectorRef,
    private readonly title: Title,
    private readonly snackBar: MatSnackBar,
    private readonly currentUser: CurrentUserService,
    private readonly kycStats: KycQueueStatsService,
    private readonly usersAdmin: UsersAdminService,
    private readonly kycDocuments: KycDocumentsAdminService,
    private readonly storage: StorageService,
    private readonly platformOps: PlatformOpsAdminService,
    private readonly walletAdmin: PlatformWalletAdminService,
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

  get liveMapMarkers(): LxMapMarker[] {
    return (this.opsSnapshot?.liveShipments ?? [])
      .filter((row) => Number.isFinite(row.lat) && Number.isFinite(row.lng))
      .map((row) => ({
        id: row.shipmentId,
        lat: row.lat,
        lng: row.lng,
        label: row.vehicleReg || row.shipmentRef,
        tone:
          row.status === 'AT_BORDER'
            ? 'warning'
            : row.status === 'IN_TRANSIT'
              ? 'primary'
              : 'secondary',
      }));
  }

  get onTimeRingDash(): string {
    const circumference = 2 * Math.PI * 15.5;
    const filled = (this.liveOnTimePct / 100) * circumference;
    return `${filled} ${circumference}`;
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
    this.platformOps
      .refresh()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (summary) => {
          this.applyOpsSummary(summary);
          this.opsLoading = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.opsLoading = false;
          this.cdr.markForCheck();
        },
      });
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
    const chartLabels = base.map((r) => r.label);
    this.kycChartData = {
      labels: chartLabels,
      datasets: [
        {
          label: 'Applications',
          data: chartCounts,
          backgroundColor: chartColors.map((c) => `${c}CC`),
          borderRadius: 10,
          borderSkipped: false,
          hoverBackgroundColor: chartColors,
          maxBarThickness: 42,
        },
      ],
    };
    this.kycDonutData = {
      labels: chartLabels,
      datasets: [
        {
          data: chartCounts,
          backgroundColor: chartColors,
          borderWidth: 2,
          borderColor: 'transparent',
          hoverOffset: 10,
        },
      ],
    };
  }

  private applyOpsSummary(summary: PlatformOpsSummary): void {
    this.opsSnapshot = summary;
    this.topActiveCompanies = [...summary.companies];
    this.shipments = summary.liveShipments.slice(0, 8).map((s) => this.mapShipmentRow(s));
    this.liveOnTimePct = summary.onTimePct;

    const volumeSpark = this.normalizeSpark(summary.weeklyVolume);

    this.kpiCards = this.kpiCards.map((card) => {
      if (card.label === 'Active Trips') {
        return {
          ...card,
          value: String(summary.activeTrips),
          trend: `${summary.activeShipments} active shipments`,
          up: summary.activeTrips > 0,
          spark: volumeSpark,
        };
      }
      if (card.label === 'Invoices Due') {
        return {
          ...card,
          value: this.walletAdmin.formatCents(summary.pendingInvoicesCents),
          trend: summary.pendingInvoicesCents > 0 ? 'Outstanding balance' : 'All clear',
          up: summary.pendingInvoicesCents === 0,
          spark: volumeSpark,
        };
      }
      if (card.label === 'Total Organizations') {
        return { ...card, spark: volumeSpark };
      }
      return card;
    });
  }

  companyPerformancePct(co: PlatformCompanyOps): number {
    const max = Math.max(
      ...this.topActiveCompanies.map(
        (row) => row.completedShipments * 2 + row.activeShipments + row.activeTrips,
      ),
      1,
    );
    const score = co.completedShipments * 2 + co.activeShipments + co.activeTrips;
    return Math.round((score / max) * 100);
  }

  private normalizeSpark(values: number[]): number[] {
    if (!values?.length) {
      return [0, 0, 0, 0, 0, 0, 0, 0];
    }
    const max = Math.max(...values, 1);
    return values.map((v) => Math.round((v / max) * 100));
  }

  private mapShipmentRow(s: PlatformShipmentOps): ShipmentRow {
    return {
      reg: s.vehicleReg,
      from: s.origin,
      to: s.destination,
      status: this.shipmentStatusClass(s.status),
      statusLabel: s.statusLabel,
      eta: s.eta,
      progress: s.progressPct,
      organizationName: s.organizationName,
      shipmentRef: s.shipmentRef,
      organizationId: s.organizationId,
      shipmentId: s.shipmentId,
    };
  }

  private shipmentStatusClass(status: string): string {
    switch (status) {
      case 'IN_TRANSIT':
        return 'transit';
      case 'AT_BORDER':
        return 'stage1';
      case 'APPROVED':
        return 'approved';
      case 'SUBMITTED':
        return 'submitted';
      case 'DELIVERED':
        return 'approved';
      default:
        return 'submitted';
    }
  }

  private applyLiveTick(): void {
    this.liveTick += 1;

    if (this.opsSnapshot) {
      this.platformOps
        .liveSummary()
        .pipe(takeUntil(this.destroy$))
        .subscribe((summary) => {
          this.opsSnapshot = summary;
          this.shipments = summary.liveShipments.slice(0, 8).map((s) => this.mapShipmentRow(s));
          this.liveOnTimePct = summary.onTimePct;
          this.cdr.markForCheck();
        });
    }
  }

  setPeriod(p: string): void {
    this.activePeriod = p;
    this.cdr.markForCheck();
  }

  setChartView(view: 'bars' | 'donut'): void {
    this.chartView = view;
    this.cdr.markForCheck();
  }

  sparklinePoints(values: number[]): string {
    if (!values.length) {
      return '';
    }
    const max = Math.max(...values, 1);
    const step = 100 / (values.length - 1 || 1);
    return values
      .map((v, i) => {
        const x = i * step;
        const y = 22 - (v / max) * 18;
        return `${x},${y}`;
      })
      .join(' ');
  }

  sparklineAreaPoints(values: number[]): string {
    const line = this.sparklinePoints(values);
    if (!line) {
      return '';
    }
    return `0,24 ${line} 100,24`;
  }

  kpiAccentClass(index: number): string {
    return ['primary', 'warning', 'success', 'analytics'][index] ?? 'primary';
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
      this.snackBar.open(`Exported dashboard shipments as ${exportFormatLabel(format)}.`, 'Close', {
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
