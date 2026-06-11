import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Subject, finalize, takeUntil } from 'rxjs';
import {
  PlatformWalletService,
  type UsageChargeBreakdownRow,
  type UsageChargeRecordRow,
  type UsageChargeReport,
} from '../../../../core/services/platform-wallet.service';

@Component({
  selector: 'app-usage-charge-report-page',
  templateUrl: './usage-charge-report-page.component.html',
  styleUrl: './usage-charge-report-page.component.scss',
  standalone: false,
})
export class UsageChargeReportPageComponent implements OnInit, OnDestroy {
  loading = false;
  exporting = false;
  error = '';
  report: UsageChargeReport | null = null;

  filterFieldsOpen = false;
  showReportHint = false;
  tableSearchQuery = '';
  actionFilter = '';
  deductedFilter = '' as '' | 'yes' | 'no';

  tripIdInput = '';
  seasonIdInput = '';
  from = '';
  to = '';

  readonly displayedColumns = ['time', 'action', 'charge', 'deducted', 'trip', 'season'];
  private readonly palette = ['#2563eb', '#06b6d4', '#8b5cf6', '#f59e0b', '#10b981', '#ef4444', '#64748b'];

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly wallet: PlatformWalletService,
    private readonly title: Title,
    private readonly cdr: ChangeDetectorRef,
  ) {
    this.title.setTitle('Usage charges | LX Platform');
  }

  ngOnInit(): void {
    this.loadReport();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get billingModeLabel(): string {
    return this.report?.billingMode === 'PREMIUM_SUBSCRIPTION' ? 'Monthly subscription' : 'Prepaid wallet';
  }

  get hasActiveFilters(): boolean {
    return !!(
      this.tripIdInput.trim() ||
      this.seasonIdInput.trim() ||
      this.from ||
      this.to ||
      this.actionFilter.trim() ||
      this.deductedFilter ||
      this.tableSearchQuery.trim()
    );
  }

  get filteredRecords(): UsageChargeRecordRow[] {
    const rows = this.report?.records ?? [];
    const q = this.tableSearchQuery.trim().toLowerCase();
    const action = this.actionFilter.trim().toLowerCase();
    return rows.filter((row) => {
      if (q) {
        const haystack = `${row.actionCode} ${row.actionDisplayName ?? ''} ${row.tripId ?? ''} ${row.seasonId ?? ''} ${row.createdAt ?? ''}`.toLowerCase();
        if (!haystack.includes(q)) {
          return false;
        }
      }
      if (action && !row.actionCode.toLowerCase().includes(action) && !(row.actionDisplayName ?? '').toLowerCase().includes(action)) {
        return false;
      }
      if (this.deductedFilter === 'yes' && !row.deducted) {
        return false;
      }
      if (this.deductedFilter === 'no' && row.deducted) {
        return false;
      }
      return true;
    });
  }

  loadReport(): void {
    this.loading = true;
    this.error = '';
    const tripId = this.parseOptionalId(this.tripIdInput);
    const seasonId = this.parseOptionalId(this.seasonIdInput);
    this.wallet
      .getUsageReport({
        tripId,
        seasonId,
        from: this.from || undefined,
        to: this.to || undefined,
      })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (report: UsageChargeReport) => {
          this.report = report;
        },
        error: () => {
          this.error = 'Could not load usage charge report.';
        },
      });
  }

  clearFilters(): void {
    this.tripIdInput = '';
    this.seasonIdInput = '';
    this.from = '';
    this.to = '';
    this.actionFilter = '';
    this.deductedFilter = '';
    this.tableSearchQuery = '';
  }

  exportCsv(): void {
    const rows = this.filteredRecords;
    if (!rows.length || this.exporting) {
      return;
    }
    this.exporting = true;
    try {
      const header = 'createdAt,actionCode,actionDisplayName,chargeCents,deducted,tripId,seasonId';
      const lines = rows.map((row) =>
        [
          row.createdAt ?? '',
          row.actionCode,
          `"${(row.actionDisplayName ?? '').replace(/"/g, '""')}"`,
          row.chargeCents ?? 0,
          row.deducted ? 'true' : 'false',
          row.tripId ?? '',
          row.seasonId ?? '',
        ].join(','),
      );
      this.downloadCsv('platform-usage-charges-report.csv', [header, ...lines].join('\n'));
    } finally {
      this.exporting = false;
      this.cdr.markForCheck();
    }
  }

  formatMoney(cents: number): string {
    return this.wallet.formatCents(cents ?? 0);
  }

  chartBarHeight(value: number, values: number[] | undefined): number {
    if (!values?.length) {
      return 0;
    }
    const max = Math.max(...values, 1);
    return Math.max(8, Math.round((value / max) * 100));
  }

  donutGradient(): string {
    const breakdown = this.report?.breakdown ?? [];
    if (!breakdown.length) {
      return 'conic-gradient(#94a3b8 0% 100%)';
    }
    const total = breakdown.reduce((sum: number, row: UsageChargeBreakdownRow) => sum + row.totalChargeCents, 0) || 1;
    let acc = 0;
    return `conic-gradient(${breakdown
      .map((row: UsageChargeBreakdownRow, i: number) => {
        const start = acc;
        acc += (row.totalChargeCents / total) * 100;
        return `${this.palette[i % this.palette.length]} ${start}% ${acc}%`;
      })
      .join(', ')})`;
  }

  legendColor(index: number): string {
    return this.palette[index % this.palette.length];
  }

  private parseOptionalId(value: string): number | null {
    const trimmed = value.trim();
    if (!trimmed) {
      return null;
    }
    const parsed = Number(trimmed);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
  }

  private downloadCsv(filename: string, contents: string): void {
    const blob = new Blob([contents], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    URL.revokeObjectURL(url);
  }
}
