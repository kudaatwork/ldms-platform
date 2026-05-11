import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { Title } from '@angular/platform-browser';
import { Subject, finalize, takeUntil } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { LOCATIONS_TABLE_PAGE_SIZE } from '../../../locations/services/locations.service';
import { DeleteConfirmDialogComponent } from '@shared/components/delete-confirm-dialog/delete-confirm-dialog.component';
import { NotificationTemplateDetailDialogComponent } from '../../components/notification-template-detail-dialog/notification-template-detail-dialog.component';
import { NotificationTemplateFormDialogComponent } from '../../components/notification-template-form-dialog/notification-template-form-dialog.component';
import type {
  NotificationChannel,
  NotificationLogExportFormat,
  NotificationLogRow,
  NotificationLogFilters,
  NotificationTemplateRow,
  UpdateTemplateRequest,
} from '../../models/notification-admin.models';
import { NotificationAdminService } from '../../services/notification-admin.service';

interface PlaceholderGuideItem {
  token: string;
  label: string;
  example: string;
  where: string[];
}

@Component({
  selector: 'app-notifications',
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.scss',
  standalone: false,
})
export class NotificationsComponent implements OnInit, OnDestroy {
  @ViewChild('logSort')
  set logSort(s: MatSort) {
    if (s) this.logData.sort = s;
  }

  @ViewChild('logPaginator')
  set logPaginator(p: MatPaginator) {
    if (p) this.logData.paginator = p;
  }

  fetchingTpl = false;
  fetchingLog = false;
  actionInProgress = false;
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

  logColumns = ['recipientDisplay', 'channel', 'templateKey', 'status', 'sentAt', 'retryCount'];
  logLabels: Record<string, string> = {
    recipientDisplay: 'Recipient',
    channel: 'Channel',
    templateKey: 'Template code',
    status: 'Status',
    sentAt: 'Sent at',
    retryCount: 'Retry count',
  };

  tplData = new MatTableDataSource<NotificationTemplateRow>([]);
  logData = new MatTableDataSource<NotificationLogRow>([]);

  /** Server-driven template grid (same defaults as locations countries table). */
  tplPageIndex = 0;
  tplPageSize = LOCATIONS_TABLE_PAGE_SIZE;
  tplTotalRecords = 0;
  private latestTplLoadToken = 0;

  logSearch = '';
  logFrom: Date | null = null;
  logTo: Date | null = null;
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
      .getTemplatesPage({
        page: this.tplPageIndex,
        size: this.tplPageSize,
      })
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

  loadLog(): void {
    this.logError = null;
    this.fetchingLog = true;
    const filters: NotificationLogFilters = {
      search: this.logSearch,
      from: this.logFrom,
      to: this.logTo,
    };
    this.notificationAdmin
      .getNotificationLog(filters)
      .pipe(
        finalize(() => {
          this.fetchingLog = false;
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.logData.data = rows;
        },
        error: (err) => {
          this.logData.data = [];
          this.logError = this.errorMessage(err, 'Failed to load log.');
        },
      });
  }

  applyLogFilters(): void {
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
      from: this.logFrom,
      to: this.logTo,
    };
    this.notificationAdmin
      .exportNotificationLog(format, filters)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob) => {
          const ext = format === 'excel' ? 'xlsx' : 'csv';
          this.saveBlob(blob, `notification_activity_log.${ext}`);
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

  exportTemplates(format: 'csv' | 'excel'): void {
    this.notificationAdmin
      .exportTemplates(format, { page: 0, size: 10_000 })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob) => {
          const ext = format === 'excel' ? 'xlsx' : 'csv';
          this.saveBlob(blob, `notification_templates.${ext}`);
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

  onToggleActive(row: NotificationTemplateRow, active: boolean): void {
    if (this.actionInProgress) {
      return;
    }
    const previous = row.isActive;
    row.isActive = active;
    this.actionInProgress = true;

    if (environment.useMocks) {
      this.actionInProgress = false;
      return;
    }

    const updateBody: UpdateTemplateRequest = {
      id: row.id,
      isActive: active,
      templateKey: row.templateKey ?? '',
      description: row.description ?? '',
      channels: (row.channels ?? []) as NotificationChannel[],
      emailSubject: row.emailSubject ?? '',
      emailBodyHtml: row.emailBodyHtml ?? '',
      smsBody: row.smsBody ?? '',
      inAppTitle: row.inAppTitle ?? '',
      inAppBody: row.inAppBody ?? '',
      whatsappTemplateName: row.whatsappTemplateName ?? '',
      whatsappBody: row.whatsappBody ?? '',
    };

    this.notificationAdmin
      .updateTemplate(updateBody)
      .pipe(
        finalize(() => {
          this.actionInProgress = false;
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: () => {
          this.snackBar.open('Template status updated.', 'Close', {
            duration: 4000,
            panelClass: ['app-snackbar-success'],
          });
        },
        error: (err) => {
          row.isActive = previous;
          this.snackBar.open(this.errorMessage(err, 'Could not update template status.'), 'Close', {
            duration: 5000,
            panelClass: ['app-snackbar-error'],
          });
        },
      });
  }

  tplStatusClass(row: NotificationTemplateRow): string {
    return row.isActive ? 'active' : 'inactive';
  }

  tplStatusLabel(row: NotificationTemplateRow): string {
    return row.isActive ? 'Active' : 'Inactive';
  }

  logStatusClass(status: string): string {
    switch (status) {
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
