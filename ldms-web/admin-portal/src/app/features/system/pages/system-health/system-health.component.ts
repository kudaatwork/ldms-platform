import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { Title } from '@angular/platform-browser';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PageEvent } from '@angular/material/paginator';
import { filterByGlobalAndColumns } from '@shared/utils/table-search.util';
import {
  LxExportFormat,
  exportClientTableAsCsv,
  exportFormatLabel,
} from '@shared/utils/lx-export.util';
import {
  PlatformHealthAdminService,
  PlatformHealthSnapshot,
  PlatformOverallStatus,
  ServiceHealthSnapshot,
} from '../../services/platform-health-admin.service';
import { Subject, EMPTY, catchError, finalize, interval, startWith, switchMap, takeUntil } from 'rxjs';

interface HealthTableRow {
  service: string;
  serviceId: string;
  detail: string;
  status: string;
  statusLabel: string;
  latencyMs: number;
  snapshot: ServiceHealthSnapshot;
}

@Component({
  selector: 'app-system-health',
  templateUrl: './system-health.component.html',
  styleUrl: './system-health.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class SystemHealthComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly manualRefresh$ = new Subject<void>();

  loading = true;
  refreshing = false;
  loadError = '';
  autoRefresh = true;

  snapshot: PlatformHealthSnapshot | null = null;
  expandedServiceId: string | null = null;

  displayedColumns = ['service', 'detail', 'status', 'latency', 'actions'];
  dataSource: HealthTableRow[] = [];

  searchQuery = '';
  filterFieldsOpen = false;
  statusFilter = 'ALL';

  columnFilters = {
    service: '',
    detail: '',
    statusLabel: '',
  };

  pageIndex = 0;
  pageSize = 50;

  readonly statusFilters = ['ALL', 'UP', 'DOWN', 'UNKNOWN'] as const;

  constructor(
    private readonly title: Title,
    private readonly snackBar: MatSnackBar,
    private readonly healthApi: PlatformHealthAdminService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  get filteredRows(): HealthTableRow[] {
    let rows = filterByGlobalAndColumns(this.dataSource, this.searchQuery, this.columnFilters);
    if (this.statusFilter !== 'ALL') {
      rows = rows.filter((r) => r.snapshot.status.toUpperCase() === this.statusFilter);
    }
    return rows;
  }

  get clampedPageIndex(): number {
    const total = this.filteredRows.length;
    if (total === 0) return 0;
    const max = Math.max(0, Math.ceil(total / this.pageSize) - 1);
    return Math.min(this.pageIndex, max);
  }

  get pagedRows(): HealthTableRow[] {
    const start = this.clampedPageIndex * this.pageSize;
    return this.filteredRows.slice(start, start + this.pageSize);
  }

  get overallTone(): string {
    if (this.loadError && !this.snapshot) {
      return 'outage';
    }
    return (this.snapshot?.overallStatus ?? 'DEGRADED').toLowerCase();
  }

  get showErrorHero(): boolean {
    return !!this.loadError && !this.snapshot;
  }

  get initialLoad(): boolean {
    return this.loading && !this.snapshot;
  }

  /** Hero KPI tiles while the first probe cycle is in flight. */
  statDisplay(value: number | undefined): string | number {
    return this.initialLoad ? '—' : (value ?? 0);
  }

  ngOnInit(): void {
    this.title.setTitle('System Health | LX Admin');
    interval(30_000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (this.autoRefresh && !this.loading) {
          this.refresh(true);
        }
      });

    this.manualRefresh$
      .pipe(
        startWith(undefined),
        switchMap(() =>
          this.healthApi.fetchSnapshot().pipe(
            catchError((err: Error) => {
              this.applyError(err.message ?? 'Failed to load platform health');
              return EMPTY;
            }),
            finalize(() => {
              this.loading = false;
              this.refreshing = false;
              this.cdr.markForCheck();
            }),
          ),
        ),
        takeUntil(this.destroy$),
      )
      .subscribe((snapshot) => this.applySnapshot(snapshot));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  refresh(background = false): void {
    if (background) {
      this.refreshing = true;
    } else {
      this.loading = !this.snapshot;
      this.loadError = '';
    }
    this.cdr.markForCheck();
    this.manualRefresh$.next();
  }

  toggleAutoRefresh(): void {
    this.autoRefresh = !this.autoRefresh;
    this.cdr.markForCheck();
  }

  setStatusFilter(filter: (typeof this.statusFilters)[number]): void {
    this.statusFilter = filter;
    this.resetPaging();
    this.cdr.markForCheck();
  }

  resetPaging(): void {
    this.pageIndex = 0;
  }

  onPage(e: PageEvent): void {
    this.pageIndex = e.pageIndex;
    this.pageSize = e.pageSize;
  }

  toggleExpanded(serviceId: string): void {
    this.expandedServiceId = this.expandedServiceId === serviceId ? null : serviceId;
    this.cdr.markForCheck();
  }

  isExpanded(serviceId: string): boolean {
    return this.expandedServiceId === serviceId;
  }

  overallLabel(status: PlatformOverallStatus | undefined): string {
    switch (status) {
      case 'OPERATIONAL':
        return 'All systems operational';
      case 'OUTAGE':
        return 'Major outage detected';
      default:
        return 'Partial degradation';
    }
  }

  statusPresentation(status: string): { css: string; label: string } {
    const upper = status.toUpperCase();
    if (upper === 'UP') {
      return { css: 'active', label: 'Operational' };
    }
    if (upper === 'DEGRADED') {
      return { css: 'pending', label: 'Degraded' };
    }
    if (upper === 'DOWN' || upper === 'OUT_OF_SERVICE') {
      return { css: 'rejected', label: 'Down' };
    }
    return { css: 'pending', label: 'Unknown' };
  }

  latencyClass(ms: number): string {
    if (ms <= 0) return 'latency--na';
    if (ms < 120) return 'latency--fast';
    if (ms < 400) return 'latency--ok';
    return 'latency--slow';
  }

  serviceIcon(serviceId: string): string {
    const id = serviceId.toLowerCase();
    if (id.includes('gateway')) return 'hub';
    if (id.includes('auth')) return 'lock';
    if (id.includes('user')) return 'people';
    if (id.includes('organization')) return 'corporate_fare';
    if (id.includes('location')) return 'map';
    if (id.includes('notification')) return 'notifications';
    if (id.includes('audit')) return 'policy';
    if (id.includes('file') || id.includes('upload')) return 'cloud_upload';
    if (id.includes('shipment')) return 'local_shipping';
    if (id.includes('trip') || id.includes('tracking')) return 'route';
    if (id.includes('fuel') || id.includes('expense')) return 'local_gas_station';
    if (id.includes('billing') || id.includes('payment')) return 'payments';
    if (id.includes('inventory')) return 'inventory_2';
    if (id.includes('fleet')) return 'local_shipping';
    if (id.includes('messaging') || id.includes('bot')) return 'smart_toy';
    return 'dns';
  }

  infraIcon(component: string): string {
    const c = component.toLowerCase();
    if (c.includes('db') || c.includes('mysql') || c.includes('jdbc')) return 'storage';
    if (c.includes('redis')) return 'memory';
    if (c.includes('rabbit')) return 'sync_alt';
    if (c.includes('disk')) return 'sd_storage';
    if (c.includes('ping')) return 'network_check';
    return 'settings';
  }

  formatCheckedAt(iso: string | undefined): string {
    if (!iso) return '—';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return iso;
    return d.toLocaleString(undefined, {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  }

  trackByServiceId = (_: number, row: HealthTableRow): string => row.serviceId;

  trackBySnapshotId = (_: number, row: ServiceHealthSnapshot): string => row.serviceId;

  exportAs(format: LxExportFormat): void {
    const ok = exportClientTableAsCsv(
      format,
      this.filteredRows,
      [
        { header: 'service', value: (r) => r.service },
        { header: 'detail', value: (r) => r.detail },
        { header: 'status', value: (r) => r.statusLabel },
        { header: 'latencyMs', value: (r) => String(r.latencyMs) },
      ],
      'system-health',
      (message) => this.snackBar.open(message, 'Close', { duration: 4500 }),
    );
    if (ok) {
      this.snackBar.open(`Exported system health snapshot as ${exportFormatLabel(format)}.`, 'Close', {
        duration: 3500,
        panelClass: ['app-snackbar-success'],
      });
    }
  }

  private applySnapshot(snapshot: PlatformHealthSnapshot): void {
    this.snapshot = snapshot;
    this.dataSource = snapshot.services.map((s) => {
      const presentation = this.statusPresentation(s.status);
      const probe = s.managementPortUsed ? `${s.host}:${s.port} (mgmt)` : `${s.host}:${s.port}`;
      const componentHint = Object.keys(s.components ?? {}).slice(0, 3).join(' · ');
      const detailParts = [probe, s.message?.trim(), componentHint].filter(Boolean);
      return {
        service: s.displayName,
        serviceId: s.serviceId,
        detail: detailParts.join(' · ') || 'Actuator probe',
        status: presentation.css,
        statusLabel: presentation.label,
        latencyMs: s.latencyMs,
        snapshot: s,
      };
    });
    this.loading = false;
    this.refreshing = false;
    this.loadError = '';
    this.cdr.markForCheck();
  }

  private applyError(message: string): void {
    this.loading = false;
    this.refreshing = false;
    this.loadError = message;
    if (this.snapshot) {
      this.snackBar.open(message, 'Close', { duration: 5000 });
    }
    this.cdr.markForCheck();
  }
}
