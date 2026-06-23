import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ChartData, ChartOptions } from 'chart.js';
import { Subject, finalize, takeUntil } from 'rxjs';
import {
  PlatformDashboardAdminService,
  type PlatformRevenueOrgRow,
  type PlatformRevenueReportApi,
} from '../../../../core/services/platform-dashboard-admin.service';
import { PlatformWalletAdminService } from '../../../settings/services/platform-wallet-admin.service';
import { ensureChartJsRegistered } from '@shared/charts/chartjs-register';
import { LX_CHART_COLORS } from '@shared/charts/lx-chart-palettes';
import { lxMoneyDoughnutChartOptions, lxRevenueDualAxisBarChartOptions } from '@shared/charts/lx-chart-theme';

@Component({
  selector: 'app-revenue-report-page',
  templateUrl: './revenue-report-page.component.html',
  styleUrl: './revenue-report-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class RevenueReportPageComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();

  loading = true;
  error = '';
  report: PlatformRevenueReportApi | null = null;
  selectedOrgId = signal<number | null>(null);
  costTotalCents = 0;

  revenueChartData: ChartData<'bar'> = { labels: [], datasets: [] };
  costDonutData: ChartData<'doughnut'> = { labels: [], datasets: [] };

  revenueChartOptions: ChartOptions<'bar'> = lxRevenueDualAxisBarChartOptions();
  costDonutOptions: ChartOptions<'doughnut'> = lxMoneyDoughnutChartOptions({
    cutout: '70%',
    legendPosition: 'bottom',
  });

  constructor(
    private readonly platformDashboard: PlatformDashboardAdminService,
    private readonly wallet: PlatformWalletAdminService,
    private readonly cdr: ChangeDetectorRef,
    private readonly title: Title,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Platform revenue | LX Admin');
    ensureChartJsRegistered();
    this.platformDashboard
      .fetchRevenueReport()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (r) => {
          this.report = r;
          this.buildCharts(r);
          if (r.byOrganization.length) {
            this.selectedOrgId.set(r.byOrganization[0].organizationId);
          }
          this.cdr.markForCheck();
        },
        error: () => {
          this.error = 'Could not load platform revenue. Check that billing-payments is running and you are signed in.';
          this.cdr.markForCheck();
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  formatMoney(cents: number): string {
    return this.wallet.formatCents(cents);
  }

  selectOrg(orgId: number): void {
    this.selectedOrgId.set(orgId);
    this.cdr.markForCheck();
  }

  get selectedOrg(): PlatformRevenueOrgRow | null {
    const id = this.selectedOrgId();
    return this.report?.byOrganization.find((o) => o.organizationId === id) ?? null;
  }

  get filteredCharges() {
    const org = this.selectedOrg;
    if (!org || !this.report) {
      return [];
    }
    return this.report.recentCharges.filter((c) => c.organizationId === org.organizationId);
  }

  billingModeLabel(mode?: string): string {
    return mode === 'PREMIUM_SUBSCRIPTION' ? 'Monthly subscription' : 'Prepaid wallet';
  }

  trackByOrgId(_i: number, row: { organizationId: number }): number {
    return row.organizationId;
  }

  earnBarWidth(cents: number): number {
    const max = Math.max(...(this.report?.byOrganization.map((o) => o.earnedCents) ?? [1]));
    return max > 0 ? Math.round((cents / max) * 100) : 0;
  }

  usageShare(cents: number, org: PlatformRevenueOrgRow): number {
    const total = org.totalUsageCents ?? 0;
    if (total <= 0) {
      return 0;
    }
    return Math.round((cents / total) * 100);
  }

  private buildCharts(report: PlatformRevenueReportApi): void {
    const deposits = report.walletDepositsSeries ?? [];
    const charges = report.usageSeries ?? report.earnedSeries ?? [];
    const subscription = report.costSeries ?? [];
    const hasSubscription = subscription.some((value) => value > 0);

    const datasets: ChartData<'bar'>['datasets'] = [
      {
        label: 'Wallet deposits',
        data: deposits,
        yAxisID: 'yDeposits',
        backgroundColor: LX_CHART_COLORS.revenue.deposits,
        hoverBackgroundColor: LX_CHART_COLORS.revenue.depositsHover,
        borderRadius: 10,
        borderSkipped: false,
        maxBarThickness: 32,
      },
      {
        label: 'Platform charges',
        data: charges,
        yAxisID: 'yCharges',
        backgroundColor: LX_CHART_COLORS.revenue.earned,
        hoverBackgroundColor: LX_CHART_COLORS.revenue.earnedHover,
        borderRadius: 10,
        borderSkipped: false,
        maxBarThickness: 32,
      },
    ];

    if (hasSubscription) {
      datasets.push({
        label: 'Subscription usage',
        data: subscription,
        yAxisID: 'yCharges',
        backgroundColor: LX_CHART_COLORS.revenue.costs,
        hoverBackgroundColor: LX_CHART_COLORS.revenue.costsHover,
        borderRadius: 10,
        borderSkipped: false,
        maxBarThickness: 32,
      });
    }

    this.revenueChartData = {
      labels: report.monthLabels,
      datasets,
    };

    this.revenueChartOptions = lxRevenueDualAxisBarChartOptions();

    const breakdown = report.costBreakdown;
    this.costTotalCents = breakdown.reduce((sum, row) => sum + row.amountCents, 0);
    this.costDonutData = {
      labels: breakdown.map((c) => c.category),
      datasets: [
        {
          data: breakdown.map((c) => c.amountCents),
          backgroundColor: breakdown.map((c) => c.color),
          borderWidth: 2,
          borderColor: LX_CHART_COLORS.donutBorder,
          hoverOffset: 10,
        },
      ],
    };
  }
}
