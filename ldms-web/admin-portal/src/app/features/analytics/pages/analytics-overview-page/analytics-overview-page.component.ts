import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { ChartData, ChartOptions } from 'chart.js';
import { Subject, finalize, takeUntil } from 'rxjs';
import { PlatformOpsAdminService } from '../../../../core/services/platform-ops-admin.service';
import type { PlatformCompanyOps, PlatformOpsSummary } from '../../../../core/services/platform-ops-mock.data';
import { PlatformWalletAdminService } from '../../../settings/services/platform-wallet-admin.service';
import { ensureChartJsRegistered } from '../../../dashboard/chartjs-register';

@Component({
  selector: 'app-analytics-overview-page',
  templateUrl: './analytics-overview-page.component.html',
  styleUrl: './analytics-overview-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class AnalyticsOverviewPageComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly chartFont = "'Plus Jakarta Sans', sans-serif";

  loading = true;
  summary: PlatformOpsSummary | null = null;
  search = signal('');

  volumeChartData: ChartData<'line'> = { labels: [], datasets: [] };
  statusChartData: ChartData<'doughnut'> = { labels: [], datasets: [] };

  volumeChartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: { grid: { display: false }, ticks: { font: { family: this.chartFont, size: 11 } } },
      y: { grid: { color: 'rgba(148,163,184,0.15)' }, ticks: { font: { family: this.chartFont, size: 11 } } },
    },
  };

  statusChartOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '68%',
    plugins: {
      legend: {
        position: 'bottom',
        labels: { usePointStyle: true, font: { family: this.chartFont, size: 10 } },
      },
    },
  };

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
    const rows = this.summary?.companies ?? [];
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
          borderColor: '#6366f1',
          backgroundColor: 'rgba(99, 102, 241, 0.12)',
          fill: true,
          tension: 0.42,
          pointRadius: 4,
          pointBackgroundColor: '#4f46e5',
        },
      ],
    };
    this.statusChartData = {
      labels: summary.shipmentsByStatus.map((s) => s.label),
      datasets: [
        {
          data: summary.shipmentsByStatus.map((s) => s.count),
          backgroundColor: summary.shipmentsByStatus.map((s) => s.color),
          borderWidth: 0,
          hoverOffset: 6,
        },
      ],
    };
  }
}
