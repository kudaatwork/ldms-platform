import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';
import { Title } from '@angular/platform-browser';
import { PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableDataSource } from '@angular/material/table';
import { catchError, finalize, of } from 'rxjs';
import { filterByGlobalAndColumns } from '@shared/utils/table-search.util';
import { environment } from '../../../../../environments/environment';
import {
  AuditLogAdminService,
  AuditLogMultipleFiltersRequest,
  AuditLogResponse,
} from '../../services/audit-log-admin.service';
import { AuditLogRequestDetailDialogComponent } from '../../components/audit-log-request-detail-dialog/audit-log-request-detail-dialog.component';
import type { RequestLogRow } from '../../models/request-log-row.model';

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
export class ActivityPageComponent implements OnInit {
  loading = true;
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
  activityPageSize = 10;

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
  requestPageSize = 25;

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

  constructor(
    private readonly title: Title,
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
    this.requestPageIndex = e.pageIndex;
    this.requestPageSize = e.pageSize;
    this.loadRequestLogs();
  }

  onRequestFiltersChanged(): void {
    this.requestPageIndex = 0;
    this.loadRequestLogs();
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
    this.title.setTitle('Audit & activity | LX Admin');
    this.dataSource = [...this.mockRows];
    this.loading = false;
    if (environment.useMocks) {
      this.applyRequestRows([...this.mockRequestRows], this.mockRequestRows.length);
      return;
    }
    this.loadRequestLogs();
  }

  requestFiltersActive(): boolean {
    if (this.requestSearchQuery.trim() !== '') {
      return true;
    }
    return Object.values(this.requestColumnFilters).some((v) => String(v ?? '').trim() !== '');
  }

  stubImport(): void {}

  stubExport(): void {}

  stubRequestImport(): void {}

  loadRequestLogs(): void {
    this.requestLoading = true;
    const payload = this.buildRequestFiltersPayload();

    this.auditLogAdmin
      .findByMultipleFilters(payload)
      .pipe(
        catchError((_error: HttpErrorResponse) => {
          this.applyRequestRows([], 0);
          return of({ auditLogPage: { content: [], totalElements: 0 } });
        }),
        finalize(() => {
          this.requestLoading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe((response) => {
        this.applyRequestLogResponse(response);
      });
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
    this.snackBar.open(
      'Churn Out is not wired yet. This action will eventually delete all request log rows from the database.',
      'Dismiss',
      { duration: 6000 },
    );
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
