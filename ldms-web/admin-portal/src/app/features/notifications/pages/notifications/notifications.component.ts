import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PageEvent } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { Title } from '@angular/platform-browser';
import { Subject, finalize, takeUntil } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { LOCATIONS_TABLE_PAGE_SIZE } from '../../../locations/services/locations.service';
import { DeleteConfirmDialogComponent } from '@shared/components/delete-confirm-dialog/delete-confirm-dialog.component';
import { NotificationLogDetailDialogComponent } from '../../components/notification-log-detail-dialog/notification-log-detail-dialog.component';
import { NotificationTemplateDetailDialogComponent } from '../../components/notification-template-detail-dialog/notification-template-detail-dialog.component';
import { NotificationTemplateFormDialogComponent } from '../../components/notification-template-form-dialog/notification-template-form-dialog.component';
import type {
  NotificationLogExportFormat,
  NotificationLogRow,
  NotificationLogFilters,
  NotificationTemplateRow,
  TemplateExportFormat,
  TemplateMultipleFiltersRequest,
} from '../../models/notification-admin.models';
import { NotificationAdminService } from '../../services/notification-admin.service';

interface PlaceholderGuideItem {
  token: string;
  label: string;
  example: string;
  where: string[];
}

/** Matches ldms-notifications {@code Channel} enum (system/backoffice APIs). */
const NOTIFICATION_CHANNEL_FILTER_OPTIONS = ['EMAIL', 'SMS', 'WHATSAPP', 'IN_APP', 'SLACK', 'TEAMS'] as const;

const NOTIFICATION_LOG_STATUS_OPTIONS = ['QUEUED', 'PENDING', 'SENT', 'FAILED', 'SKIPPED'] as const;

@Component({
  selector: 'app-notifications',
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.scss',
  standalone: false,
})
export class NotificationsComponent implements OnInit, OnDestroy {
  fetchingTpl = false;
  fetchingLog = false;
  actionInProgress = false;
  /** Template IDs with an in-flight active/inactive toggle (does not block the whole table). */
  private readonly tplStatusUpdatingIds = new Set<number>();
  tplError: string | null = null;
  logError: string | null = null;
  importingTpl = false;
  logLoaded = false;

  private readonly dialogOpts = {
    width: '640px',
    maxWidth: '95vw',
    panelClass: 'lx-location-dialog-panel',
    autoFocus: 'first-tabbable' as const,
  };

  tplColumns = ['templateKey', 'emailSubject', 'channels', 'isActive', 'actions'];
  tplLabels: Record<string, string> = {
    templateKey: 'Template code',
    emailSubject: 'Subject',
    channels: 'Applicable channels',
    isActive: 'Status',
    actions: 'Actions',
  };

  logColumns = [
    'recipientDisplay',
    'channel',
    'templateKey',
    'status',
    'errorMessage',
    'sentAt',
    'retryCount',
    'actions',
  ];
  logLabels: Record<string, string> = {
    recipientDisplay: 'Recipient',
    channel: 'Channel',
    templateKey: 'Template code',
    status: 'Status',
    errorMessage: 'Detail',
    sentAt: 'Sent at',
    retryCount: 'Retry count',
    actions: 'Actions',
  };

  tplData = new MatTableDataSource<NotificationTemplateRow>([]);
  logData = new MatTableDataSource<NotificationLogRow>([]);

  /** Server-driven template grid (same defaults as locations countries table). */
  tplPageIndex = 0;
  tplPageSize = LOCATIONS_TABLE_PAGE_SIZE;
  tplTotalRecords = 0;
  private latestTplLoadToken = 0;

  /** Server-driven log grid (same pattern as templates). */
  logPageIndex = 0;
  logPageSize = LOCATIONS_TABLE_PAGE_SIZE;
  logTotalRecords = 0;
  queueSummary: { queueName?: string; messagesReady?: number } | null = null;
  private latestLogLoadToken = 0;

  logSearch = '';
  /** Optional filters sent to {@code NotificationLogMultipleFiltersRequest}. */
  logTemplateKey = '';
  logChannel = '';
  logStatus = '';
  logRecipientId = '';
  logProvider = '';
  logFrom: Date | null = null;
  logTo: Date | null = null;

  /** Collapsible column filters (same pattern as locations / provinces table). */
  tplFilterFieldsOpen = false;
  logFilterFieldsOpen = false;

  /** Template list filters (backend {@code TemplateMultipleFiltersRequest}). */
  tplSearch = '';
  tplTemplateKey = '';
  /** Single channel filter (backend {@code channels} list with one entry). */
  tplChannel = '';
  /** any | active | inactive */
  tplActiveFilter: 'any' | 'active' | 'inactive' = 'any';

  readonly notificationChannelOptions = NOTIFICATION_CHANNEL_FILTER_OPTIONS;
  readonly notificationLogStatusOptions = NOTIFICATION_LOG_STATUS_OPTIONS;

  showPlaceholderGuide = false;
  readonly placeholderGuide: PlaceholderGuideItem[] = [
    {
      token: '{{firstName}}',
      label: 'Recipient first name',
      example: 'Tariro',
      where: ['Email subject', 'Email HTML body', 'SMS body', 'In-app body', 'WhatsApp body'],
    },
    {
      token: '{{userName}}',
      label: 'Recipient username',
      example: 'tariro.ncube',
      where: ['Email HTML body', 'SMS body', 'WhatsApp body'],
    },
    {
      token: '{{Email}}',
      label: 'Recipient email',
      example: 'tariro@example.com',
      where: ['Email HTML body'],
    },
    {
      token: '{{organizationName}}',
      label: 'Organization name',
      example: 'Project LX Logistics',
      where: ['Email subject', 'Email HTML body', 'In-app title/body'],
    },
    {
      token: '{{orderNumber}}',
      label: 'Order number',
      example: 'ORD-2026-00421',
      where: ['Email subject', 'Email HTML body', 'SMS body', 'In-app body', 'WhatsApp body'],
    },
    {
      token: '{{branchName}}',
      label: 'Branch name',
      example: 'Harare South Depot',
      where: ['SMS body', 'In-app body', 'WhatsApp body'],
    },
    {
      token: '{{driverName}}',
      label: 'Assigned driver',
      example: 'M. Dube',
      where: ['In-app body', 'WhatsApp body'],
    },
    {
      token: '{{resetLink}}',
      label: 'Reset URL',
      example: 'https://app.example.com/reset/abc123',
      where: ['Email HTML body', 'WhatsApp body'],
    },
  ];

  private destroy$ = new Subject<void>();
  private reloadLog$ = new Subject<void>();

  constructor(
    private readonly notificationAdmin: NotificationAdminService,
    private readonly title: Title,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Notification Management | LX Admin');
    this.loadTemplates();
    this.reloadLog$.pipe(takeUntil(this.destroy$)).subscribe(() => this.loadLog());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadTemplates(): void {
    this.tplError = null;
    const loadToken = ++this.latestTplLoadToken;
    this.fetchingTpl = true;
    this.notificationAdmin
      .getTemplatesPage(this.buildTplFilters())
      .pipe(
        finalize(() => {
          if (loadToken === this.latestTplLoadToken) {
            this.fetchingTpl = false;
          }
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: ({ rows, totalElements }) => {
          if (loadToken !== this.latestTplLoadToken) {
            return;
          }
          if (rows.length === 0 && this.tplPageIndex > 0) {
            this.tplPageIndex = 0;
            this.loadTemplates();
            return;
          }
          this.tplTotalRecords = totalElements > 0 ? totalElements : rows.length;
          this.tplData.data = rows;
        },
        error: (err) => {
          if (loadToken !== this.latestTplLoadToken) {
            return;
          }
          this.tplData.data = [];
          this.tplTotalRecords = 0;
          this.tplError = this.errorMessage(err, 'Failed to load templates.');
        },
      });
  }

  onTplPage(ev: PageEvent): void {
    if (ev.pageIndex === this.tplPageIndex && ev.pageSize === this.tplPageSize) {
      return;
    }
    this.tplPageIndex = ev.pageIndex;
    this.tplPageSize = ev.pageSize;
    this.loadTemplates();
  }

  applyTplFilters(): void {
    this.tplPageIndex = 0;
    this.loadTemplates();
  }

  clearTplFilters(): void {
    this.tplSearch = '';
    this.tplTemplateKey = '';
    this.tplChannel = '';
    this.tplActiveFilter = 'any';
    this.tplPageIndex = 0;
    this.loadTemplates();
  }

  private buildTplFilters(): TemplateMultipleFiltersRequest {
    const req: TemplateMultipleFiltersRequest = {
      page: this.tplPageIndex,
      size: this.tplPageSize,
      searchValue: this.tplSearch.trim(),
    };
    const tk = this.tplTemplateKey.trim();
    if (tk) {
      req.templateKey = tk;
    }
    const ch = this.tplChannel.trim();
    if (ch) {
      req.channels = [ch];
    }
    if (this.tplActiveFilter === 'active') {
      req.isActive = true;
    } else if (this.tplActiveFilter === 'inactive') {
      req.isActive = false;
    }
    return req;
  }

  loadLog(): void {
    this.logError = null;
    const loadToken = ++this.latestLogLoadToken;
    this.fetchingLog = true;
    const filters: NotificationLogFilters = {
      page: this.logPageIndex,
      size: this.logPageSize,
      search: this.logSearch,
      templateKey: this.logTemplateKey.trim() || undefined,
      channel: this.logChannel.trim() || undefined,
      status: this.logStatus.trim() || undefined,
      recipientId: this.logRecipientId.trim() || undefined,
      provider: this.logProvider.trim() || undefined,
      from: this.logFrom,
      to: this.logTo,
    };
    this.notificationAdmin
      .getNotificationLogPage(filters)
      .pipe(
        finalize(() => {
          if (loadToken === this.latestLogLoadToken) {
            this.fetchingLog = false;
          }
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: ({ rows, totalElements, queueSummary }) => {
          if (loadToken !== this.latestLogLoadToken) {
            return;
          }
          if (rows.length === 0 && this.logPageIndex > 0) {
            this.logPageIndex = 0;
            this.loadLog();
            return;
          }
          this.logTotalRecords = totalElements > 0 ? totalElements : rows.length;
          this.logData.data = rows;
          this.queueSummary = queueSummary;
        },
        error: (err) => {
          if (loadToken !== this.latestLogLoadToken) {
            return;
          }
          this.logData.data = [];
          this.logTotalRecords = 0;
          this.logError = this.errorMessage(err, 'Failed to load log.');
        },
      });
  }

  onLogPage(ev: PageEvent): void {
    if (ev.pageIndex === this.logPageIndex && ev.pageSize === this.logPageSize) {
      return;
    }
    this.logPageIndex = ev.pageIndex;
    this.logPageSize = ev.pageSize;
    this.loadLog();
  }

  applyLogFilters(): void {
    this.logPageIndex = 0;
    this.reloadLog$.next();
  }

  clearLogFilters(): void {
    this.logSearch = '';
    this.logTemplateKey = '';
    this.logChannel = '';
    this.logStatus = '';
    this.logRecipientId = '';
    this.logProvider = '';
    this.logFrom = null;
    this.logTo = null;
    this.logPageIndex = 0;
    this.reloadLog$.next();
  }

  onTabChange(index: number): void {
    if (index === 1 && !this.logLoaded) {
      this.logLoaded = true;
      this.reloadLog$.next();
    }
  }

  exportLog(format: NotificationLogExportFormat): void {
    const filters: NotificationLogFilters = {
      search: this.logSearch,
      templateKey: this.logTemplateKey.trim() || undefined,
      channel: this.logChannel.trim() || undefined,
      status: this.logStatus.trim() || undefined,
      recipientId: this.logRecipientId.trim() || undefined,
      provider: this.logProvider.trim() || undefined,
      from: this.logFrom,
      to: this.logTo,
    };
    this.notificationAdmin
      .exportNotificationLog(format, filters)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob) => {
          const ext = format === 'excel' ? 'xlsx' : format === 'pdf' ? 'pdf' : 'csv';
          this.saveBlob(blob, `notification_activity_log.${ext}`);
          this.snackBar.open(`Exported notification log as ${ext.toUpperCase()}.`, 'Close', {
            duration: 3500,
            panelClass: ['app-snackbar-success'],
          });
        },
        error: (err) => {
          this.snackBar.open(this.errorMessage(err, 'Log export failed.'), 'Close', {
            duration: 5000,
            panelClass: ['app-snackbar-error'],
          });
        },
      });
  }

  viewTemplate(t: NotificationTemplateRow): void {
    this.dialog.open(NotificationTemplateDetailDialogComponent, {
      ...this.dialogOpts,
      maxHeight: '90vh',
      data: { template: t },
    });
  }

  viewLog(row: NotificationLogRow): void {
    this.dialog.open(NotificationLogDetailDialogComponent, {
      ...this.dialogOpts,
      maxHeight: '90vh',
      data: { log: row },
    });
  }

  openAddTemplate(): void {
    this.dialog
      .open(NotificationTemplateFormDialogComponent, {
        ...this.dialogOpts,
        maxHeight: '90vh',
      })
      .afterClosed()
      .subscribe((created) => {
        if (created) {
          this.snackBar.open('Template created successfully.', 'Close', {
            duration: 5000,
            panelClass: ['app-snackbar-success'],
          });
          this.loadTemplates();
        }
      });
  }

  openEditTemplate(row: NotificationTemplateRow): void {
    this.dialog
      .open(NotificationTemplateFormDialogComponent, {
        ...this.dialogOpts,
        maxHeight: '90vh',
        data: { template: row },
      })
      .afterClosed()
      .subscribe((updated) => {
        if (updated) {
          this.snackBar.open('Template updated successfully.', 'Close', {
            duration: 5000,
            panelClass: ['app-snackbar-success'],
          });
          this.loadTemplates();
        }
      });
  }

  deleteTemplate(row: NotificationTemplateRow): void {
    this.dialog
      .open(DeleteConfirmDialogComponent, {
        width: '420px',
        maxWidth: '92vw',
        data: { entityLabel: 'template' },
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.actionInProgress = true;
        this.notificationAdmin
          .deleteTemplate(row.id)
          .pipe(
            finalize(() => {
              this.actionInProgress = false;
            }),
            takeUntil(this.destroy$),
          )
          .subscribe({
            next: () => {
              this.snackBar.open('Template deleted', 'Close', {
                duration: 5000,
                panelClass: ['app-snackbar-success'],
              });
              this.loadTemplates();
            },
            error: (err) => {
              this.snackBar.open(this.errorMessage(err, 'Could not delete template.'), 'Close', {
                duration: 5000,
                panelClass: ['app-snackbar-error'],
              });
            },
          });
      });
  }

  exportTemplates(format: TemplateExportFormat): void {
    this.notificationAdmin
      .exportTemplates(format, { ...this.buildTplFilters(), page: 0, size: 10_000 })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob) => {
          const ext = format === 'excel' ? 'xlsx' : format === 'pdf' ? 'pdf' : 'csv';
          this.saveBlob(blob, `notification_templates.${ext}`);
          this.snackBar.open(`Exported templates as ${ext.toUpperCase()}.`, 'Close', {
            duration: 3500,
            panelClass: ['app-snackbar-success'],
          });
        },
        error: (err) => {
          this.snackBar.open(this.errorMessage(err, 'Export failed.'), 'Close', {
            duration: 5000,
            panelClass: ['app-snackbar-error'],
          });
        },
      });
  }

  openImportPicker(input: HTMLInputElement): void {
    if (this.importingTpl || this.fetchingTpl || this.actionInProgress) {
      return;
    }
    input.value = '';
    input.click();
  }

  onImportCsvSelected(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.[0];
    if (!file) {
      return;
    }
    if (!file.name.toLowerCase().endsWith('.csv')) {
      this.snackBar.open('Please select a CSV file.', 'Close', {
        duration: 5000,
        panelClass: ['app-snackbar-error'],
      });
      input.value = '';
      return;
    }

    this.importingTpl = true;
    this.notificationAdmin
      .importTemplatesCsv(file)
      .pipe(
        finalize(() => {
          this.importingTpl = false;
          if (input) {
            input.value = '';
          }
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (summary) => {
          const msg = summary.message || `Import complete. Success: ${summary.success ?? 0}, Failed: ${summary.failed ?? 0}.`;
          this.snackBar.open(msg, 'Close', {
            duration: 6000,
            panelClass: [summary.isSuccess === false ? 'app-snackbar-error' : 'app-snackbar-success'],
          });
          this.loadTemplates();
        },
        error: (err) => {
          this.snackBar.open(this.errorMessage(err, 'Import failed.'), 'Close', {
            duration: 5000,
            panelClass: ['app-snackbar-error'],
          });
        },
      });
  }

  private saveBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }

  isTplStatusUpdating(row: NotificationTemplateRow): boolean {
    return this.tplStatusUpdatingIds.has(row.id);
  }

  onToggleActive(row: NotificationTemplateRow, active: boolean): void {
    if (this.tplStatusUpdatingIds.has(row.id)) {
      return;
    }
    const previous = row.isActive;
    row.isActive = active;

    if (environment.useMocks) {
      this.snackBar.open(this.tplStatusSnackMessage(active), 'Close', {
        duration: 3500,
        panelClass: ['app-snackbar-success'],
      });
      return;
    }

    this.tplStatusUpdatingIds.add(row.id);
    this.cdr.markForCheck();
    this.notificationAdmin
      .setTemplateActive(row.id, active)
      .pipe(
        finalize(() => {
          this.tplStatusUpdatingIds.delete(row.id);
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: () => {
          this.snackBar.open(this.tplStatusSnackMessage(active), 'Close', {
            duration: 3500,
            panelClass: ['app-snackbar-success'],
          });
        },
        error: (err) => {
          row.isActive = previous;
          this.snackBar.open(this.errorMessage(err, 'Could not update template status.'), 'Close', {
            duration: 5000,
            panelClass: ['app-snackbar-error'],
          });
          this.cdr.markForCheck();
        },
      });
  }

  private tplStatusSnackMessage(active: boolean): string {
    return active ? 'Template is now active.' : 'Template is now inactive.';
  }

  tplStatusClass(row: NotificationTemplateRow): string {
    return row.isActive ? 'active' : 'inactive';
  }

  tplStatusLabel(row: NotificationTemplateRow): string {
    return row.isActive ? 'Active' : 'Inactive';
  }

  logStatusClass(status: string): string {
    switch (status) {
      case 'QUEUED':
        return 'pending';
      case 'PENDING':
        return 'pending';
      case 'SENT':
        return 'active';
      case 'FAILED':
        return 'rejected';
      case 'SKIPPED':
        return 'inactive';
      default:
        return 'pending';
    }
  }

  channelsText(row: NotificationTemplateRow): string {
    return row.channels.join(', ');
  }

  private errorMessage(error: unknown, fallback: string): string {
    const err = error as {
      status?: number;
      statusCode?: number;
      error?: { messageResponse?: string; message?: string; error?: string };
      message?: string;
    };
    if (err?.status === 0) {
      return 'Request failed before the server response reached the browser. Please retry.';
    }
    if (typeof err?.message === 'string' && err.message.length > 0 && err.statusCode != null && err.statusCode >= 400) {
      return err.message;
    }
    return err?.error?.messageResponse || err?.error?.message || err?.error?.error || err?.message || fallback;
  }
}
