import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ChartData, ChartOptions } from 'chart.js';
import { Subject, finalize, takeUntil } from 'rxjs';
import { PlatformOpsAdminService } from '../../../../core/services/platform-ops-admin.service';
import type { PlatformRevenueReport } from '../../../../core/services/platform-ops-mock.data';
import { PlatformWalletAdminService } from '../../../settings/services/platform-wallet-admin.service';
import { ensureChartJsRegistered } from '../../../dashboard/chartjs-register';

@Component({
  selector: 'app-revenue-report-page',
  templateUrl: './revenue-report-page.component.html',
  styleUrl: './revenue-report-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class RevenueReportPageComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly chartFont = "'Plus Jakarta Sans', sans-serif";

  loading = true;
  report: PlatformRevenueReport | null = null;
  selectedOrgId = signal<number | null>(null);

  revenueChartData: ChartData<'bar'> = { labels: [], datasets: [] };
  costDonutData: ChartData<'doughnut'> = { labels: [], datasets: [] };

  revenueChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'top', labels: { font: { family: this.chartFont, size: 11 } } } },
    scales: {
      x: { grid: { display: false }, ticks: { font: { family: this.chartFont, size: 11 } } },
      y: {
        grid: { color: 'rgba(148,163,184,0.12)' },
        ticks: {
          font: { family: this.chartFont, size: 11 },
          callback: (v) => '$' + Number(v) / 100,
        },
      },
    },
  };

  costDonutOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '62%',
    plugins: {
      legend: {
        position: 'right',
        labels: { usePointStyle: true, font: { family: this.chartFont, size: 10 }, boxWidth: 8 },
      },
    },
  };

  constructor(
    private readonly platformOps: PlatformOpsAdminService,
    private readonly wallet: PlatformWalletAdminService,
    private readonly cdr: ChangeDetectorRef,
    private readonly title: Title,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Platform revenue | LX Admin');
    ensureChartJsRegistered();
    this.platformOps
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

  get selectedOrg() {
    const id = this.selectedOrgId();
    return this.report?.byOrganization.find((o) => o.organizationId === id) ?? null;
  }

  get filteredCharges() {
    const org = this.selectedOrg;
    if (!org || !this.report) {
      return [];
    }
    return this.report.recentCharges.filter((c) => c.organizationName === org.organizationName);
  }

  trackByOrgId(_i: number, row: { organizationId: number }): number {
    return row.organizationId;
  }

  earnBarWidth(cents: number): number {
    const max = Math.max(...(this.report?.byOrganization.map((o) => o.earnedCents) ?? [1]));
    return max > 0 ? Math.round((cents / max) * 100) : 0;
  }

  private buildCharts(report: PlatformRevenueReport): void {
    this.revenueChartData = {
      labels: report.monthLabels,
      datasets: [
        {
          label: 'Earned',
          data: report.earnedSeries,
          backgroundColor: 'rgba(99, 102, 241, 0.85)',
          borderRadius: 10,
          maxBarThickness: 36,
        },
        {
          label: 'Platform costs',
          data: report.costSeries,
          backgroundColor: 'rgba(244, 63, 94, 0.75)',
          borderRadius: 10,
          maxBarThickness: 36,
        },
      ],
    };
    this.costDonutData = {
      labels: report.costBreakdown.map((c) => c.category),
      datasets: [
        {
          data: report.costBreakdown.map((c) => c.amountCents),
          backgroundColor: report.costBreakdown.map((c) => c.color),
          borderWidth: 0,
          hoverOffset: 8,
        },
      ],
    };
  }
}
