import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { ChartData, ChartOptions } from 'chart.js';
import { Subject, finalize, takeUntil } from 'rxjs';
import { PlatformOpsAdminService } from '../../../../core/services/platform-ops-admin.service';
import type { PlatformCompanyOps, PlatformOpsSummary } from '../../../../core/services/platform-ops-mock.data';
import { PlatformWalletAdminService } from '../../../settings/services/platform-wallet-admin.service';
import { ensureChartJsRegistered } from '@shared/charts/chartjs-register';
import { LX_CHART_COLORS } from '@shared/charts/lx-chart-palettes';
import {
  lxChartAreaGradient,
  lxDoughnutChartOptions,
  lxLineChartOptions,
} from '@shared/charts/lx-chart-theme';

@Component({
  selector: 'app-analytics-overview-page',
  templateUrl: './analytics-overview-page.component.html',
  styleUrl: './analytics-overview-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class AnalyticsOverviewPageComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();

  loading = true;
  summary: PlatformOpsSummary | null = null;
  search = signal('');
  statusTotal = 0;

  volumeChartData: ChartData<'line'> = { labels: [], datasets: [] };
  statusChartData: ChartData<'doughnut'> = { labels: [], datasets: [] };

  volumeChartOptions: ChartOptions<'line'> = lxLineChartOptions();
  statusChartOptions: ChartOptions<'doughnut'> = lxDoughnutChartOptions({
    cutout: '70%',
    legendPosition: 'bottom',
  });

  constructor(
    private readonly platformOps: PlatformOpsAdminService,
    private readonly wallet: PlatformWalletAdminService,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
    private readonly title: Title,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Shipment analytics | LX Admin');
    ensureChartJsRegistered();
    this.platformOps
      .refresh()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (s) => {
          this.summary = s;
          this.buildCharts(s);
          this.cdr.markForCheck();
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get filteredCompanies(): PlatformCompanyOps[] {
    const rows = (this.summary?.companies ?? []).filter(
      (c) => c.activeShipments > 0 || c.activeTrips > 0 || c.completedShipments > 0,
    );
    const q = this.search().trim().toLowerCase();
    if (!q) {
      return rows;
    }
    return rows.filter(
      (c) =>
        c.organizationName.toLowerCase().includes(q) ||
        c.classificationLabel.toLowerCase().includes(q),
    );
  }

  formatMoney(cents: number): string {
    return this.wallet.formatCents(cents);
  }

  openCompany(company: PlatformCompanyOps): void {
    void this.router.navigate(['/analytics/companies', company.organizationId]);
  }

  trackByOrgId(_i: number, row: PlatformCompanyOps): number {
    return row.organizationId;
  }

  private buildCharts(summary: PlatformOpsSummary): void {
    const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    this.volumeChartData = {
      labels: days,
      datasets: [
        {
          label: 'Shipments',
          data: summary.weeklyVolume,
          borderColor: LX_CHART_COLORS.primary,
          backgroundColor: (ctx) =>
            lxChartAreaGradient(ctx.chart, 'rgba(59, 130, 246, 0.32)', 'rgba(59, 130, 246, 0)'),
          fill: true,
          tension: 0.42,
          pointRadius: 4,
          pointBackgroundColor: LX_CHART_COLORS.primary,
          pointBorderColor: '#fff',
          pointHoverBackgroundColor: LX_CHART_COLORS.analytics,
          pointHoverBorderColor: '#fff',
        },
      ],
    };

    const statusRows = summary.shipmentsByStatus;
    this.statusTotal = statusRows.reduce((sum, row) => sum + row.count, 0);
    this.statusChartData = {
      labels: statusRows.map((s) => s.label),
      datasets: [
        {
          data: statusRows.map((s) => s.count),
          backgroundColor: statusRows.map((s) => s.color),
          borderWidth: 2,
          borderColor: LX_CHART_COLORS.donutBorder,
          hoverOffset: 10,
        },
      ],
    };
  }
}
