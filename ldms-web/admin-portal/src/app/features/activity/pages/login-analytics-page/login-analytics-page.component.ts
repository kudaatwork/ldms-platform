import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableDataSource } from '@angular/material/table';
import { Title } from '@angular/platform-browser';
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
import { DEFAULT_TABLE_PAGE_SIZE } from '@shared/constants/table-pagination';
import {
  downloadBlob,
  exportFilename,
  exportRowsAsCsv,
  type LxExportFormat,
} from '@shared/utils/lx-export.util';
import { environment } from '../../../../../environments/environment';
import { AuditLogRequestDetailDialogComponent } from '../../components/audit-log-request-detail-dialog/audit-log-request-detail-dialog.component';
import type { RequestLogRow } from '../../models/request-log-row.model';
import {
  AuditLogAdminService,
  AuditLogDto,
  AuditLogMultipleFiltersRequest,
  USER_ACTIVITY_EXCLUDED_ACTIONS,
} from '../../services/audit-log-admin.service';

export interface LoginEventRow {
  id: number;
  username: string;
  time: string;
  platform: string;
  platformLabel: string;
  action: string;
  actionLabel: string;
  clientIp: string;
  serviceName: string;
  traceId: string;
  traceIdShort: string;
  requestTimestamp?: string;
}

export interface UserActivityRow {
  id: number;
  action: string;
  actionLabel: string;
  serviceName: string;
  time: string;
  resource: string;
  platform: string;
  clientIp: string;
  traceId: string;
  traceIdShort: string;
  responseTimeMs: number | null;
  requestTimestamp?: string;
}

export interface LoginColumnFilters {
  username: string;
  platform: string;
  signInMethod: string;
  traceId: string;
  clientIp: string;
  from: string;
  to: string;
}

export interface ActivityColumnFilters {
  action: string;
  serviceName: string;
  platform: string;
  traceId: string;
  from: string;
  to: string;
}

const PLATFORM_LABELS: Record<string, string> = {
  ADMIN_PORTAL: 'Admin portal',
  PLATFORM_PORTAL: 'Platform portal',
  MOBILE_DRIVER: 'Driver app',
  MOBILE_RECEIVER: 'Receiver app',
  MOBILE_OPS: 'Ops app',
};

const ACTION_LABELS: Record<string, string> = {
  USER_AUTHENTICATION: 'Password sign-in',
  USER_AUTHENTICATION_GOOGLE: 'Google sign-in',
};

const PLATFORM_OPTIONS = [
  { value: '', label: 'All platforms' },
  { value: 'ADMIN_PORTAL', label: 'Admin portal' },
  { value: 'PLATFORM_PORTAL', label: 'Platform portal' },
  { value: 'MOBILE_DRIVER', label: 'Driver app' },
  { value: 'MOBILE_RECEIVER', label: 'Receiver app' },
  { value: 'MOBILE_OPS', label: 'Ops app' },
];

const SIGN_IN_METHOD_OPTIONS = [
  { value: '', label: 'All methods' },
  { value: 'USER_AUTHENTICATION', label: 'Password' },
  { value: 'USER_AUTHENTICATION_GOOGLE', label: 'Google' },
];

/** Default sign-in history window on first load (keeps audit queries fast). */
const DEFAULT_LOGIN_LOOKBACK_DAYS = 30;

@Component({
  selector: 'app-login-analytics-page',
  templateUrl: './login-analytics-page.component.html',
  styleUrl: './login-analytics-page.component.scss',
  standalone: false,
})
export class LoginAnalyticsPageComponent implements OnInit, OnDestroy {
  loginLoading = false;
  activityLoading = false;
  loginExporting = false;
  activityExporting = false;
  loginTotalElements = 0;
  activityTotalElements = 0;

  loginSearchQuery = '';
  loginFilterFieldsOpen = false;
  activityFilterFieldsOpen = false;

  readonly platformOptions = PLATFORM_OPTIONS;
  readonly signInMethodOptions = SIGN_IN_METHOD_OPTIONS;

  loginColumnFilters: LoginColumnFilters = {
    username: '',
    platform: '',
    signInMethod: '',
    traceId: '',
    clientIp: '',
    from: '',
    to: '',
  };

  activitySearchQuery = '';
  activityColumnFilters: ActivityColumnFilters = {
    action: '',
    serviceName: '',
    platform: '',
    traceId: '',
    from: '',
    to: '',
  };

  loginPageIndex = 0;
  loginPageSize = DEFAULT_TABLE_PAGE_SIZE;
  activityPageIndex = 0;
  activityPageSize = DEFAULT_TABLE_PAGE_SIZE;

  selectedUsername: string | null = null;

  loginDisplayedColumns = [
    'id',
    'time',
    'username',
    'platform',
    'action',
    'clientIp',
    'traceId',
    'actions',
  ];
  activityDisplayedColumns = [
    'id',
    'time',
    'action',
    'serviceName',
    'resource',
    'platform',
    'clientIp',
    'traceId',
    'actions',
  ];

  loginTableDataSource = new MatTableDataSource<LoginEventRow>([]);
  activityTableDataSource = new MatTableDataSource<UserActivityRow>([]);

  private readonly destroy$ = new Subject<void>();
  private readonly loginFilterReload$ = new Subject<void>();
  private readonly activityFilterReload$ = new Subject<void>();
  private loginLatestLoadToken = 0;
  private activityLatestLoadToken = 0;
  private lastLoginFilterSignature = '';
  private lastActivityFilterSignature = '';

  constructor(
    private readonly title: Title,
    private readonly auditLogAdmin: AuditLogAdminService,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
    private readonly dialog: MatDialog,
  ) {}

  get platformsOnPage(): string[] {
    const set = new Set<string>();
    for (const row of this.loginTableDataSource.data) {
      if (row.platform && row.platform !== '—') {
        set.add(row.platformLabel);
      }
    }
    return [...set];
  }

  get heroLead(): string {
    return 'Immutable audit trail of sign-in events (who, when, which client, IP address) with drill-down into every recorded business action for investigations and compliance reviews.';
  }

  ngOnInit(): void {
    this.title.setTitle('Login & activity | LX Admin');
    this.loginColumnFilters.from = this.defaultLoginFromFilter();

    if (environment.useMocks) {
      this.applyLoginRows([], 0);
      return;
    }

    this.lastLoginFilterSignature = this.loginFilterSignature();
    this.loginLoading = true;
    merge(of(undefined as void), this.loginFilterReload$.pipe(debounceTime(150)))
      .pipe(
        switchMap(() => {
          this.loginPageIndex = 0;
          return this.runLoginQuery();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loginFiltersActive(): boolean {
    if (this.loginSearchQuery.trim()) {
      return true;
    }
    return Object.values(this.loginColumnFilters).some((v) => String(v ?? '').trim() !== '');
  }

  activityFiltersActive(): boolean {
    if (this.activitySearchQuery.trim()) {
      return true;
    }
    return Object.values(this.activityColumnFilters).some((v) => String(v ?? '').trim() !== '');
  }

  resetLoginFilters(): void {
    this.loginSearchQuery = '';
    this.loginColumnFilters = {
      username: '',
      platform: '',
      signInMethod: '',
      traceId: '',
      clientIp: '',
      from: this.defaultLoginFromFilter(),
      to: '',
    };
    this.onLoginFiltersChanged();
  }

  /** ISO local datetime for {@code datetime-local} inputs (last N days). */
  private defaultLoginFromFilter(): string {
    const d = new Date();
    d.setDate(d.getDate() - DEFAULT_LOGIN_LOOKBACK_DAYS);
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T00:00`;
  }

  resetActivityFilters(): void {
    this.activitySearchQuery = '';
    this.activityColumnFilters = {
      action: '',
      serviceName: '',
      platform: '',
      traceId: '',
      from: '',
      to: '',
    };
    this.onActivityFiltersChanged();
  }

  refreshLoginEvents(): void {
    this.runLoginQuery().pipe(takeUntil(this.destroy$)).subscribe();
  }

  refreshActivity(): void {
    if (!this.selectedUsername) {
      return;
    }
    this.runActivityQuery().pipe(takeUntil(this.destroy$)).subscribe();
  }

  onLoginFiltersChanged(): void {
    const next = this.loginFilterSignature();
    if (next === this.lastLoginFilterSignature) {
      return;
    }
    this.lastLoginFilterSignature = next;
    this.loginFilterReload$.next();
  }

  onActivityFiltersChanged(): void {
    if (!this.selectedUsername) {
      return;
    }
    const next = this.activityFilterSignature();
    if (next === this.lastActivityFilterSignature) {
      return;
    }
    this.lastActivityFilterSignature = next;
    this.activityPageIndex = 0;
    this.activityFilterReload$.next();
    this.runActivityQuery().pipe(takeUntil(this.destroy$)).subscribe();
  }

  onLoginPage(e: PageEvent): void {
    if (e.pageIndex === this.loginPageIndex && e.pageSize === this.loginPageSize) {
      return;
    }
    this.loginPageIndex = e.pageIndex;
    this.loginPageSize = e.pageSize;
    this.runLoginQuery().pipe(takeUntil(this.destroy$)).subscribe();
  }

  onActivityPage(e: PageEvent): void {
    if (!this.selectedUsername) {
      return;
    }
    if (e.pageIndex === this.activityPageIndex && e.pageSize === this.activityPageSize) {
      return;
    }
    this.activityPageIndex = e.pageIndex;
    this.activityPageSize = e.pageSize;
    this.runActivityQuery().pipe(takeUntil(this.destroy$)).subscribe();
  }

  selectUser(row: LoginEventRow, event?: Event): void {
    event?.stopPropagation();
    const username = row.username?.trim();
    if (!username || username === '-') {
      return;
    }
    if (this.selectedUsername === username) {
      return;
    }
    this.selectedUsername = username;
    this.activityPageIndex = 0;
    this.lastActivityFilterSignature = this.activityFilterSignature();
    this.runActivityQuery().pipe(takeUntil(this.destroy$)).subscribe();
  }

  clearSelectedUser(): void {
    this.selectedUsername = null;
    this.activityTableDataSource.data = [];
    this.activityTotalElements = 0;
    this.cdr.markForCheck();
  }

  exportLoginLogs(format: LxExportFormat): void {
    if (environment.useMocks) {
      this.exportLoginClientSide(format);
      return;
    }
    this.loginExporting = true;
    const payload = this.buildLoginPayload(0, 5000);
    this.auditLogAdmin
      .exportAuditLogs(payload, format === 'xlsx' ? 'xlsx' : format)
      .pipe(
        finalize(() => {
          this.loginExporting = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (blob) => {
          downloadBlob(blob, exportFilename('login-audit-events', format));
          this.snackBar.open(`Exported sign-in history as ${format.toUpperCase()}.`, 'Dismiss', {
            duration: 3500,
            panelClass: ['app-snackbar-success'],
          });
        },
        error: () => {
          this.snackBar.open('Export failed. Check filters and try again.', 'Dismiss', {
            duration: 5000,
            panelClass: ['app-snackbar-error'],
          });
        },
      });
  }

  exportActivityLogs(format: LxExportFormat): void {
    const username = this.selectedUsername?.trim();
    if (!username) {
      return;
    }
    if (environment.useMocks) {
      this.exportActivityClientSide(format);
      return;
    }
    this.activityExporting = true;
    const payload = this.buildActivityPayload(0, 5000);
    this.auditLogAdmin
      .exportAuditLogs(payload, format === 'xlsx' ? 'xlsx' : format)
      .pipe(
        finalize(() => {
          this.activityExporting = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (blob) => {
          const safeUser = username.replace(/[^a-zA-Z0-9._-]+/g, '_');
          downloadBlob(blob, exportFilename(`user-activity-${safeUser}`, format));
          this.snackBar.open(`Exported activity for ${username} as ${format.toUpperCase()}.`, 'Dismiss', {
            duration: 3500,
            panelClass: ['app-snackbar-success'],
          });
        },
        error: () => {
          this.snackBar.open('Export failed. Check filters and try again.', 'Dismiss', {
            duration: 5000,
            panelClass: ['app-snackbar-error'],
          });
        },
      });
  }

  copyTraceId(traceId: string, event?: Event): void {
    event?.stopPropagation();
    const value = traceId?.trim();
    if (!value || value === '-') {
      return;
    }
    navigator.clipboard?.writeText(value).then(
      () => {
        this.snackBar.open('Trace ID copied to clipboard.', 'Dismiss', { duration: 2500 });
      },
      () => {
        this.snackBar.open(value, 'Dismiss', { duration: 6000 });
      },
    );
  }

  platformPillClass(platform: string): string {
    const key = (platform || '').toUpperCase();
    if (key.includes('ADMIN')) {
      return 'lx-platform-pill--admin';
    }
    if (key.includes('PLATFORM')) {
      return 'lx-platform-pill--platform';
    }
    if (key.includes('MOBILE') || key.includes('DRIVER') || key.includes('OPS')) {
      return 'lx-platform-pill--mobile';
    }
    return 'lx-platform-pill--unknown';
  }

  methodPillClass(action: string): string {
    return action === 'USER_AUTHENTICATION_GOOGLE' ? 'lx-method-pill--google' : 'lx-method-pill--password';
  }

  trackByLoginId(_index: number, row: LoginEventRow): number {
    return row.id;
  }

  trackByActivityId(_index: number, row: UserActivityRow): number {
    return row.id;
  }

  viewLoginDetail(row: LoginEventRow, event?: Event): void {
    event?.stopPropagation();
    this.openDetailFromDto(this.loginRowToDto(row));
  }

  viewActivityDetail(row: UserActivityRow, event?: Event): void {
    event?.stopPropagation();
    this.openDetailFromDto(this.activityRowToDto(row));
  }

  private exportLoginClientSide(format: LxExportFormat): void {
    const rows = this.loginTableDataSource.data;
    if (format !== 'csv') {
      this.snackBar.open('Demo mode supports CSV export only.', 'Dismiss', { duration: 4000 });
      return;
    }
    const blob = exportRowsAsCsv(rows, [
      { header: 'audit_id', value: (r) => r.id },
      { header: 'timestamp', value: (r) => r.time },
      { header: 'username', value: (r) => r.username },
      { header: 'platform', value: (r) => r.platform },
      { header: 'platform_label', value: (r) => r.platformLabel },
      { header: 'sign_in_method', value: (r) => r.actionLabel },
      { header: 'client_ip', value: (r) => r.clientIp },
      { header: 'trace_id', value: (r) => r.traceId },
    ]);
    downloadBlob(blob, exportFilename('login-audit-events', 'csv'));
  }

  private exportActivityClientSide(format: LxExportFormat): void {
    const rows = this.activityTableDataSource.data;
    if (format !== 'csv') {
      this.snackBar.open('Demo mode supports CSV export only.', 'Dismiss', { duration: 4000 });
      return;
    }
    const blob = exportRowsAsCsv(rows, [
      { header: 'audit_id', value: (r) => r.id },
      { header: 'timestamp', value: (r) => r.time },
      { header: 'action', value: (r) => r.action },
      { header: 'service', value: (r) => r.serviceName },
      { header: 'resource', value: (r) => r.resource },
      { header: 'platform', value: (r) => r.platform },
      { header: 'client_ip', value: (r) => r.clientIp },
      { header: 'trace_id', value: (r) => r.traceId },
    ]);
    downloadBlob(blob, exportFilename('user-activity', 'csv'));
  }

  private runLoginQuery(): Observable<unknown> {
    const loadToken = ++this.loginLatestLoadToken;
    this.loginLoading = true;
    const payload = this.buildLoginPayload();

    return this.auditLogAdmin.queryLoginEventsPage(payload).pipe(
      tap(({ rows, totalElements }) => {
        if (loadToken !== this.loginLatestLoadToken) {
          return;
        }
        if (rows.length === 0 && totalElements > 0 && this.loginPageIndex > 0) {
          this.loginPageIndex = 0;
          this.runLoginQuery().pipe(takeUntil(this.destroy$)).subscribe();
          return;
        }
        this.applyLoginRows(rows, totalElements);
      }),
      catchError((error: unknown) => {
        if (loadToken !== this.loginLatestLoadToken) {
          return EMPTY;
        }
        this.applyLoginRows([], 0);
        this.snackBar.open(this.errorMessage(error, 'Failed to load login events.'), 'Dismiss', {
          duration: 5000,
          panelClass: ['app-snackbar-error'],
        });
        return EMPTY;
      }),
      finalize(() => {
        if (loadToken === this.loginLatestLoadToken) {
          this.loginLoading = false;
          this.cdr.markForCheck();
        }
      }),
    );
  }

  private runActivityQuery(): Observable<unknown> {
    const username = this.selectedUsername?.trim();
    if (!username) {
      return of(null);
    }
    const loadToken = ++this.activityLatestLoadToken;
    this.activityLoading = true;
    const payload = this.buildActivityPayload();

    return this.auditLogAdmin.queryUserActivityPage(payload, username).pipe(
      tap(({ rows, totalElements }) => {
        if (loadToken !== this.activityLatestLoadToken) {
          return;
        }
        if (rows.length === 0 && totalElements > 0 && this.activityPageIndex > 0) {
          this.activityPageIndex = 0;
          this.runActivityQuery().pipe(takeUntil(this.destroy$)).subscribe();
          return;
        }
        this.applyActivityRows(rows, totalElements);
      }),
      catchError((error: unknown) => {
        if (loadToken !== this.activityLatestLoadToken) {
          return EMPTY;
        }
        this.applyActivityRows([], 0);
        this.snackBar.open(this.errorMessage(error, 'Failed to load user activity.'), 'Dismiss', {
          duration: 5000,
          panelClass: ['app-snackbar-error'],
        });
        return EMPTY;
      }),
      finalize(() => {
        if (loadToken === this.activityLatestLoadToken) {
          this.activityLoading = false;
          this.cdr.markForCheck();
        }
      }),
    );
  }

  private buildLoginPayload(page = this.loginPageIndex, size = this.loginPageSize): AuditLogMultipleFiltersRequest {
    const method = this.loginColumnFilters.signInMethod.trim();
    return {
      page,
      size,
      searchValue: this.combinedLoginSearch(),
      serviceName: '',
      username: this.loginColumnFilters.username.trim(),
      eventType: 'SERVICE_METHOD',
      httpStatusCode: null,
      from: this.toApiDateTime(this.loginColumnFilters.from),
      to: this.toApiDateTime(this.loginColumnFilters.to),
      sortBy: 'requestTimestamp',
      sortDir: 'DESC',
      clientPlatform: this.loginColumnFilters.platform.trim() || undefined,
      traceId: this.loginColumnFilters.traceId.trim() || undefined,
      actionsIn: method ? [method] : undefined,
    };
  }

  private buildActivityPayload(
    page = this.activityPageIndex,
    size = this.activityPageSize,
  ): AuditLogMultipleFiltersRequest {
    return {
      page,
      size,
      searchValue: this.activitySearchQuery.trim(),
      serviceName: this.activityColumnFilters.serviceName.trim(),
      username: this.selectedUsername ?? '',
      eventType: 'SERVICE_METHOD',
      httpStatusCode: null,
      from: this.toApiDateTime(this.activityColumnFilters.from),
      to: this.toApiDateTime(this.activityColumnFilters.to),
      sortBy: 'requestTimestamp',
      sortDir: 'DESC',
      action: this.activityColumnFilters.action.trim() || undefined,
      clientPlatform: this.activityColumnFilters.platform.trim() || undefined,
      traceId: this.activityColumnFilters.traceId.trim() || undefined,
      excludeActions: [...USER_ACTIVITY_EXCLUDED_ACTIONS],
    };
  }

  private combinedLoginSearch(): string {
    const parts = [this.loginSearchQuery.trim(), this.loginColumnFilters.clientIp.trim()].filter(Boolean);
    return parts.join(' ').trim();
  }

  private applyLoginRows(logs: AuditLogDto[], totalElements: number): void {
    const mapped = logs.map((log) => this.mapLoginRow(log));
    this.loginTotalElements = totalElements > 0 ? totalElements : mapped.length;
    this.loginTableDataSource.data = mapped;
    this.cdr.markForCheck();
  }

  private applyActivityRows(logs: AuditLogDto[], totalElements: number): void {
    const mapped = logs.map((log) => this.mapActivityRow(log));
    this.activityTotalElements = totalElements > 0 ? totalElements : mapped.length;
    this.activityTableDataSource.data = mapped;
    this.cdr.markForCheck();
  }

  private mapLoginRow(log: AuditLogDto): LoginEventRow {
    const action = log.action ?? '-';
    const platform = (log.clientPlatform ?? '').trim() || '—';
    const traceId = log.traceId ?? '-';
    return {
      id: log.id ?? 0,
      username: log.username ?? '-',
      time: this.formatTimestamp(log.requestTimestamp),
      platform,
      platformLabel: this.platformLabel(platform),
      action,
      actionLabel: ACTION_LABELS[action] ?? this.humanizeAction(action),
      clientIp: log.clientIpAddress ?? '—',
      serviceName: log.serviceName ?? '-',
      traceId,
      traceIdShort: this.shortTrace(traceId),
      requestTimestamp: typeof log.requestTimestamp === 'string' ? log.requestTimestamp : undefined,
    };
  }

  private mapActivityRow(log: AuditLogDto): UserActivityRow {
    const action = log.action ?? '-';
    const platform = (log.clientPlatform ?? '').trim() || '—';
    const traceId = log.traceId ?? '-';
    return {
      id: log.id ?? 0,
      action,
      actionLabel: this.humanizeAction(action),
      serviceName: log.serviceName ?? '-',
      time: this.formatTimestamp(log.requestTimestamp),
      resource: this.deriveResource(log),
      platform: this.platformLabel(platform),
      clientIp: log.clientIpAddress ?? '—',
      traceId,
      traceIdShort: this.shortTrace(traceId),
      responseTimeMs: log.responseTimeMs ?? null,
      requestTimestamp: typeof log.requestTimestamp === 'string' ? log.requestTimestamp : undefined,
    };
  }

  private deriveResource(log: AuditLogDto): string {
    const url = log.requestUrl?.trim();
    if (url) {
      const segments = url.split('/').filter(Boolean);
      return segments.length > 0 ? segments.slice(-3).join('/') : url;
    }
    return log.serviceName ?? '—';
  }

  private shortTrace(traceId: string): string {
    if (!traceId || traceId === '-' || traceId.length <= 14) {
      return traceId;
    }
    return `${traceId.slice(0, 8)}…${traceId.slice(-6)}`;
  }

  private platformLabel(code: string): string {
    if (!code || code === '—') {
      return 'Unknown';
    }
    return PLATFORM_LABELS[code] ?? code.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
  }

  private humanizeAction(action: string): string {
    return action.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
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

  private toApiDateTime(value: string): string | null {
    const trimmed = value?.trim();
    if (!trimmed) {
      return null;
    }
    return trimmed.length === 16 ? `${trimmed}:00` : trimmed;
  }

  private loginFilterSignature(): string {
    return JSON.stringify({
      q: this.loginSearchQuery.trim(),
      filters: this.loginColumnFilters,
    });
  }

  private activityFilterSignature(): string {
    return JSON.stringify({
      q: this.activitySearchQuery.trim(),
      filters: this.activityColumnFilters,
      user: this.selectedUsername,
    });
  }

  private openDetailFromDto(log: AuditLogDto): void {
    const row: RequestLogRow = {
      id: log.id ?? 0,
      action: log.action ?? '-',
      eventType: log.eventType ?? 'SERVICE_METHOD',
      username: log.username ?? '-',
      serviceName: log.serviceName ?? '-',
      requestUrl: log.requestUrl ?? '-',
      clientIpAddress: log.clientIpAddress ?? '-',
      traceId: log.traceId ?? '-',
      exceptionMessage: log.exceptionMessage ?? '-',
      time: this.formatTimestamp(log.requestTimestamp),
      method: (log.httpMethod ?? 'N/A').toUpperCase(),
      statusCode: log.httpStatusCode ?? null,
      responseTimeMs: log.responseTimeMs ?? 0,
      requestTimestamp: log.requestTimestamp,
      responseTimestamp: log.responseTimestamp,
      requestHeaders: log.requestHeaders ?? undefined,
    };
    this.dialog.open(AuditLogRequestDetailDialogComponent, {
      width: '760px',
      maxWidth: '95vw',
      autoFocus: 'first-tabbable',
      panelClass: 'lx-audit-request-dialog-panel',
      data: { row },
    });
  }

  private loginRowToDto(row: LoginEventRow): AuditLogDto {
    return {
      id: row.id,
      username: row.username,
      action: row.action,
      clientIpAddress: row.clientIp,
      clientPlatform: row.platform !== '—' ? row.platform : undefined,
      serviceName: row.serviceName,
      traceId: row.traceId,
      requestTimestamp: row.requestTimestamp,
      eventType: 'SERVICE_METHOD',
    };
  }

  private activityRowToDto(row: UserActivityRow): AuditLogDto {
    return {
      id: row.id,
      username: this.selectedUsername ?? undefined,
      action: row.action,
      serviceName: row.serviceName,
      requestUrl: row.resource,
      traceId: row.traceId,
      clientIpAddress: row.clientIp,
      requestTimestamp: row.requestTimestamp,
      responseTimeMs: row.responseTimeMs ?? undefined,
      eventType: 'SERVICE_METHOD',
    };
  }

  private errorMessage(error: unknown, fallback: string): string {
    return error instanceof Error ? error.message : fallback;
  }
}
