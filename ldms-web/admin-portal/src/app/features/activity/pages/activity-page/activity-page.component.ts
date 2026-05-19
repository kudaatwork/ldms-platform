import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';
import { Title } from '@angular/platform-browser';
import { PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableDataSource } from '@angular/material/table';
import { ActivatedRoute } from '@angular/router';
import {
  EMPTY,
  Observable,
  Subject,
  catchError,
  debounceTime,
  finalize,
  merge,
  of,
  switchMap,
  takeUntil,
  tap,
} from 'rxjs';
import { filterByGlobalAndColumns } from '@shared/utils/table-search.util';
import { environment } from '../../../../../environments/environment';
import {
  AuditLogAdminService,
  AuditLogMultipleFiltersRequest,
  AuditLogResponse,
} from '../../services/audit-log-admin.service';
import { AuditLogRequestDetailDialogComponent } from '../../components/audit-log-request-detail-dialog/audit-log-request-detail-dialog.component';
import { ChurnOutConfirmDialogComponent } from '../../components/churn-out-confirm-dialog/churn-out-confirm-dialog.component';
import type { RequestLogRow } from '../../models/request-log-row.model';

import { DEFAULT_TABLE_PAGE_SIZE } from '@shared/constants/table-pagination';
import {
  LxExportFormat,
  exportClientTableAsCsv,
} from '@shared/utils/lx-export.util';
export interface ActivityRow {
  time: string;
  actor: string;
  action: string;
  resource: string;
}

export interface RequestColumnFilters {
  action: string;
  eventType: string;
  username: string;
  serviceName: string;
  method: string;
  statusCode: string;
  requestUrl: string;
  traceId: string;
}

@Component({
  selector: 'app-activity-page',
  templateUrl: './activity-page.component.html',
  styleUrl: './activity-page.component.scss',
  standalone: false,
})
export class ActivityPageComponent implements OnInit, OnDestroy {
  loading = true;
  showActivitySection = true;
  showRequestSection = true;
  requestLoading = false;
  requestExporting = false;
  requestTotalElements = 0;

  displayedColumns = ['time', 'actor', 'action', 'resource', 'actions'];

  dataSource: ActivityRow[] = [];

  searchQuery = '';
  filterFieldsOpen = false;

  activityColumnFilters = {
    time: '',
    actor: '',
    action: '',
    resource: '',
  };

  activityPageIndex = 0;
  activityPageSize = DEFAULT_TABLE_PAGE_SIZE;

  /* ── Requests log ─────────────────────────────────────────── */
  requestDisplayedColumns = [
    'id',
    'action',
    'eventType',
    'username',
    'serviceName',
    'requestUrl',
    'time',
    'method',
    'statusCode',
    'responseTimeMs',
    'actions',
  ];

  /** Same pattern as {@link LocationTablePageComponent}: MatTableDataSource + rawRows for reliable renders. */
  requestTableDataSource = new MatTableDataSource<RequestLogRow>([]);
  requestRawRows: RequestLogRow[] = [];

  requestSearchQuery = '';
  requestFilterFieldsOpen = false;

  requestColumnFilters: RequestColumnFilters = {
    action: '',
    eventType: '',
    username: '',
    serviceName: '',
    method: '',
    statusCode: '',
    requestUrl: '',
    traceId: '',
  };

  requestPageIndex = 0;
  requestPageSize = DEFAULT_TABLE_PAGE_SIZE;

  private readonly mockRows: ActivityRow[] = [
    {
      time: '28 Mar, 09:42',
      actor: 'ops.reviewer@projectlx.co.zw',
      action: 'Approved KYC application',
      resource: 'Nexus Logistics (Pvt) Ltd',
    },
    {
      time: '28 Mar, 09:18',
      actor: 'admin@projectlx.co.zw',
      action: 'Updated organization',
      resource: 'Chirundu Cold Chain Co.',
    },
    {
      time: '27 Mar, 16:05',
      actor: 'ops.reviewer@projectlx.co.zw',
      action: 'Requested document resubmit',
      resource: 'Masvingo Grain Brokers',
    },
    {
      time: '27 Mar, 11:22',
      actor: 'admin@projectlx.co.zw',
      action: 'Created user',
      resource: 'finance@projectlx.co.zw',
    },
    {
      time: '26 Mar, 14:50',
      actor: 'system',
      action: 'Scheduled health check',
      resource: 'API Gateway',
    },
    {
      time: '26 Mar, 10:03',
      actor: 'admin@projectlx.co.zw',
      action: 'Exported audit trail',
      resource: 'CSV · Q1 scope',
    },
    {
      time: '25 Mar, 17:41',
      actor: 'ops.reviewer@projectlx.co.zw',
      action: 'Rejected KYC application',
      resource: 'Mutare Fresh Produce',
    },
    {
      time: '25 Mar, 08:55',
      actor: 'finance@projectlx.co.zw',
      action: 'Downloaded invoice pack',
      resource: 'INV-2026-0142',
    },
    {
      time: '24 Mar, 15:20',
      actor: 'system',
      action: 'Password rotation reminder',
      resource: 'viewer@projectlx.co.zw',
    },
    {
      time: '24 Mar, 09:00',
      actor: 'admin@projectlx.co.zw',
      action: 'Disabled user account',
      resource: 'legacy.vendor@example.com',
    },
    {
      time: '23 Mar, 13:12',
      actor: 'ops.reviewer@projectlx.co.zw',
      action: 'Commented on application',
      resource: 'Great North Hauliers',
    },
  ];

  private readonly mockRequestRows: RequestLogRow[] = [];

  private destroy$ = new Subject<void>();
  /** Mirrors LocationTablePageComponent: debounced filter changes cancel superseded HTTP calls. */
  private readonly requestFilterReload$ = new Subject<void>();
  /** Monotonic token to drop stale in-flight responses (same guard as LocationTablePageComponent). */
  private requestLatestLoadToken = 0;
  /** Prevents duplicate reload when the filter set didn't actually change. */
  private lastRequestFilterSignature = '';

  constructor(
    private readonly title: Title,
    private readonly route: ActivatedRoute,
    private readonly auditLogAdmin: AuditLogAdminService,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
    private readonly dialog: MatDialog,
  ) {}

  get filteredRows(): ActivityRow[] {
    return filterByGlobalAndColumns(
      this.dataSource,
      this.searchQuery,
      this.activityColumnFilters,
    );
  }

  get activityClampedPageIndex(): number {
    const total = this.filteredRows.length;
    if (total === 0) return 0;
    const max = Math.max(0, Math.ceil(total / this.activityPageSize) - 1);
    return Math.min(this.activityPageIndex, max);
  }

  get pagedActivityRows(): ActivityRow[] {
    const all = this.filteredRows;
    const start = this.activityClampedPageIndex * this.activityPageSize;
    return all.slice(start, start + this.activityPageSize);
  }

  resetActivityPaging(): void {
    this.activityPageIndex = 0;
  }

  onActivityPage(e: PageEvent): void {
    this.activityPageIndex = e.pageIndex;
    this.activityPageSize = e.pageSize;
  }

  resetRequestPaging(): void {
    this.requestPageIndex = 0;
  }

  onRequestPage(e: PageEvent): void {
    if (e.pageIndex === this.requestPageIndex && e.pageSize === this.requestPageSize) {
      return;
    }
    this.requestPageIndex = e.pageIndex;
    this.requestPageSize = e.pageSize;
    this.runRequestTableQuery({ background: false }).pipe(takeUntil(this.destroy$)).subscribe();
  }

  onRequestFiltersChanged(): void {
    const next = this.currentRequestFilterSignature();
    if (next === this.lastRequestFilterSignature) {
      return;
    }
    this.lastRequestFilterSignature = next;
    this.requestFilterReload$.next();
  }

  trackByRequestLogId(_index: number, row: RequestLogRow): number {
    return row.id;
  }

  requestHttpMethodClass(method: string): string {
    const m = (method || '').toLowerCase();
    const allowed = ['get', 'post', 'put', 'patch', 'delete', 'head'];
    if (allowed.includes(m)) {
      return `lx-http-method--${m}`;
    }
    return 'lx-http-method--na';
  }

  requestStatusClass(code: number): string {
    if (code >= 500) {
      return 'rejected';
    }
    if (code >= 400) {
      return 'pending';
    }
    if (code >= 200 && code < 300) {
      return 'approved';
    }
    return 'submitted';
  }

  ngOnInit(): void {
    const viewMode = String(this.route.snapshot.data['activityView'] ?? 'request').toLowerCase();
    this.showRequestSection = viewMode !== 'activity';
    this.showActivitySection = viewMode !== 'request';

    this.title.setTitle(this.showRequestSection ? 'Request logs | LX Admin' : 'Activity logs | LX Admin');
    this.dataSource = [...this.mockRows];
    this.loading = false;

    if (environment.useMocks || !this.showRequestSection) {
      this.applyRequestRows([...this.mockRequestRows], this.mockRequestRows.length);
      return;
    }

    // Mirror LocationTablePageComponent: initial load + debounced filter-change reloads share
    // the same switchMap pipeline so superseded in-flight requests are automatically cancelled.
    this.lastRequestFilterSignature = this.currentRequestFilterSignature();
    merge(
      of(undefined as void),
      this.requestFilterReload$.pipe(debounceTime(150)),
    )
      .pipe(
        switchMap(() => {
          this.requestPageIndex = 0;
          return this.runRequestTableQuery({ background: false });
        }),
        takeUntil(this.destroy$),
      )
      .subscribe();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  requestFiltersActive(): boolean {
    if (this.requestSearchQuery.trim() !== '') {
      return true;
    }
    return Object.values(this.requestColumnFilters).some((v) => String(v ?? '').trim() !== '');
  }

  stubImport(): void {}

  exportActivityLogs(format: LxExportFormat): void {
    const ok = exportClientTableAsCsv(
      format,
      this.filteredRows,
      [
        { header: 'time', value: (r) => r.time },
        { header: 'actor', value: (r) => r.actor },
        { header: 'action', value: (r) => r.action },
        { header: 'resource', value: (r) => r.resource },
      ],
      'activity-logs',
      (message) => this.snackBar.open(message, 'Dismiss', { duration: 4500 }),
    );
    if (ok) {
      this.snackBar.open('Exported activity logs as CSV.', 'Dismiss', {
        duration: 3500,
        panelClass: ['app-snackbar-success'],
      });
    }
  }

  stubRequestImport(): void {}

  /** Public entry-point kept for the Refresh button and post-churn reload. */
  loadRequestLogs(): void {
    this.runRequestTableQuery({ background: false }).pipe(takeUntil(this.destroy$)).subscribe();
  }

  /**
   * Single observable for request-log data — mirrors LocationTablePageComponent.runTableQuery().
   * Callers that go through the filter-reload switchMap pipeline get automatic cancellation;
   * direct callers (page nav, Refresh) create an independent subscription guarded by the token.
   */
  private runRequestTableQuery(opts?: { background?: boolean }): Observable<AuditLogResponse> {
    const loadToken = ++this.requestLatestLoadToken;
    const background = opts?.background === true;
    if (!background || this.requestTotalElements === 0) {
      this.requestLoading = true;
    }
    const payload = this.buildRequestFiltersPayload();

    return this.auditLogAdmin.findByMultipleFilters(payload).pipe(
      tap((response) => {
        if (loadToken === this.requestLatestLoadToken) {
          this.applyRequestLogResponse(response);
        }
      }),
      catchError((_error: HttpErrorResponse) => {
        if (loadToken !== this.requestLatestLoadToken) {
          return EMPTY;
        }
        this.applyRequestRows([], 0);
        this.snackBar.open('Failed to load request logs. Please try again.', 'Dismiss', {
          duration: 5000,
          panelClass: ['app-snackbar-error'],
        });
        return EMPTY;
      }),
      finalize(() => {
        if (loadToken === this.requestLatestLoadToken) {
          this.requestLoading = false;
          this.cdr.markForCheck();
        }
      }),
    );
  }

  exportRequestLogs(format: 'csv' | 'xlsx' | 'pdf'): void {
    if (environment.useMocks) {
      this.snackBar.open('Export is disabled while mocks are enabled.', 'Dismiss', { duration: 4000 });
      return;
    }
    this.requestExporting = true;
    const payload = this.buildRequestFiltersPayload();
    payload.page = 0;
    payload.size = 100;

    this.auditLogAdmin
      .exportAuditLogs(payload, format)
      .pipe(
        finalize(() => {
          this.requestExporting = false;
        }),
      )
      .subscribe({
        next: (blob) => {
          const ext = format === 'xlsx' ? 'xlsx' : format;
          this.downloadBlob(blob, `audit-requests-${new Date().toISOString().slice(0, 10)}.${ext}`);
          this.snackBar.open(`Exported requests log as ${ext.toUpperCase()}.`, 'Dismiss', { duration: 3500 });
        },
        error: () => {
          this.snackBar.open('Export failed. Check filters and try again.', 'Dismiss', { duration: 5000 });
        },
      });
  }

  onRequestLogView(row: RequestLogRow): void {
    this.openRequestLogDetailDialog(row);
  }

  /** Placeholder: will call a backend endpoint to truncate / purge persisted request logs. */
  onRequestLogChurnOut(): void {
    if (environment.useMocks) {
      this.snackBar.open('Churn Out is disabled while mocks are enabled.', 'Dismiss', { duration: 4000 });
      return;
    }
    this.dialog
      .open(ChurnOutConfirmDialogComponent, {
        width: '420px',
        maxWidth: '95vw',
        autoFocus: 'first-tabbable',
        data: { entityLabel: 'request logs' },
      })
      .afterClosed()
      .subscribe((confirmed: boolean | undefined) => {
        if (confirmed !== true) {
          return;
        }
        this.executeRequestLogChurnOut();
      });
  }

  private executeRequestLogChurnOut(): void {
    this.requestLoading = true;
    this.auditLogAdmin
      .churnOutRequestLogs()
      .pipe(
        finalize(() => {
          this.requestLoading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (response) => {
          const launch = response.churnLaunch;
          if (launch?.jobExecutionId != null) {
            this.snackBar.open(
              `Churn job started (${launch.triggerType ?? 'MANUAL'}) — execution #${launch.jobExecutionId}, batch ${launch.batchReference}. Logs will clear when the job finishes.`,
              'Dismiss',
              { duration: 8000 },
            );
          } else {
            this.snackBar.open(response.message ?? 'Churn request accepted.', 'Dismiss', { duration: 6000 });
          }
          this.requestPageIndex = 0;
          setTimeout(() => this.loadRequestLogs(), 1500);
        },
        error: (err: HttpErrorResponse) => {
          const body = err.error as AuditLogResponse | undefined;
          const msg =
            body?.message ??
            (err.status === 409
              ? 'A churn job is already running. Try again later.'
              : 'Churn out failed. Please try again or contact support.');
          this.snackBar.open(msg, 'Dismiss', { duration: 6000 });
        },
      });
  }

  private openRequestLogDetailDialog(row: RequestLogRow): void {
    if (!row?.id) {
      this.snackBar.open('This row has no valid id.', 'Dismiss', { duration: 4000 });
      return;
    }
    this.dialog.open(AuditLogRequestDetailDialogComponent, {
      width: '720px',
      maxWidth: '95vw',
      autoFocus: 'first-tabbable',
      panelClass: 'lx-audit-request-dialog-panel',
      data: { row },
    });
  }

  private currentRequestFilterSignature(): string {
    return JSON.stringify({
      q: this.requestSearchQuery.trim(),
      filters: this.requestColumnFilters,
    });
  }

  private buildRequestFiltersPayload(): AuditLogMultipleFiltersRequest {
    return {
      page: this.requestPageIndex,
      size: this.requestPageSize,
      searchValue: this.requestSearchQuery.trim(),
      serviceName: this.requestColumnFilters.serviceName.trim(),
      username: this.requestColumnFilters.username.trim(),
      eventType: this.requestColumnFilters.eventType.trim(),
      httpStatusCode: this.parseNullableNumber(this.requestColumnFilters.statusCode),
      from: null,
      to: null,
      sortBy: 'requestTimestamp',
      sortDir: 'DESC',
      action: this.requestColumnFilters.action.trim() || undefined,
      requestUrl: this.requestColumnFilters.requestUrl.trim() || undefined,
      httpMethod: this.requestColumnFilters.method.trim() || undefined,
      traceId: this.requestColumnFilters.traceId.trim() || undefined,
    };
  }

  private applyRequestLogResponse(response: AuditLogResponse): void {
    const page = response.auditLogPage;
    const rows = page?.content ?? [];
    const total = page?.totalElements ?? rows.length;
    const mapped = rows.map((log) => ({
      id: log.id ?? 0,
      action: log.action ?? '-',
      eventType: log.eventType ?? '-',
      username: log.username ?? '-',
      serviceName: log.serviceName ?? '-',
      requestUrl: log.requestUrl ?? '-',
      clientIpAddress: log.clientIpAddress ?? '-',
      traceId: log.traceId ?? '-',
      exceptionMessage: log.exceptionMessage ?? '-',
      time: this.formatTimestamp(log.requestTimestamp as string | undefined),
      method: (log.httpMethod ?? 'N/A').toUpperCase(),
      statusCode: log.httpStatusCode ?? null,
      responseTimeMs: log.responseTimeMs ?? 0,
      requestTimestamp: typeof log.requestTimestamp === 'string' ? log.requestTimestamp : undefined,
      responseTimestamp: typeof log.responseTimestamp === 'string' ? log.responseTimestamp : undefined,
      requestHeaders: log.requestHeaders ?? undefined,
    }));
    this.applyRequestRows(mapped, total);
  }

  private applyRequestRows(mapped: RequestLogRow[], totalElements: number): void {
    this.requestTotalElements = totalElements > 0 ? totalElements : mapped.length;
    this.requestRawRows = mapped;
    this.requestTableDataSource.data = mapped;
    this.cdr.markForCheck();
  }

  private parseNullableNumber(value: string | number): number | null {
    const trimmed = String(value ?? '').trim();
    if (!trimmed) {
      return null;
    }
    const numeric = Number(trimmed);
    return Number.isFinite(numeric) ? numeric : null;
  }

  private formatTimestamp(value?: string): string {
    if (!value) {
      return '-';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }
    return date.toLocaleString();
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 60_000);
  }
}
