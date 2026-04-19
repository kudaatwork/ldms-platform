import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { Title } from '@angular/platform-browser';
import { Subject, takeUntil } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { NotificationTemplateDetailDialogComponent } from '../../components/notification-template-detail-dialog/notification-template-detail-dialog.component';
import { NotificationTemplateFormDialogComponent } from '../../components/notification-template-form-dialog/notification-template-form-dialog.component';
import type {
  NotificationLogRow,
  NotificationLogFilters,
  NotificationTemplateRow,
} from '../../models/notification-admin.models';
import { NotificationAdminService } from '../../services/notification-admin.service';

@Component({
  selector: 'app-notifications',
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.scss',
  standalone: false,
})
export class NotificationsComponent implements OnInit, OnDestroy {
  @ViewChild('tplSort')
  set tplSort(s: MatSort) {
    if (s) this.tplData.sort = s;
  }

  @ViewChild('tplPaginator')
  set tplPaginator(p: MatPaginator) {
    if (p) this.tplData.paginator = p;
  }

  @ViewChild('logSort')
  set logSort(s: MatSort) {
    if (s) this.logData.sort = s;
  }

  @ViewChild('logPaginator')
  set logPaginator(p: MatPaginator) {
    if (p) this.logData.paginator = p;
  }

  loadingTpl = true;
  loadingLog = true;

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

  logSearch = '';
  logFrom: Date | null = null;
  logTo: Date | null = null;

  private destroy$ = new Subject<void>();

  constructor(
    private readonly notificationAdmin: NotificationAdminService,
    private readonly title: Title,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Notification Management | LX Admin');
    this.loadTemplates();
    this.loadLog();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadTemplates(): void {
    this.loadingTpl = true;
    this.notificationAdmin
      .getTemplates()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (rows) => {
          this.tplData.data = rows;
          this.loadingTpl = false;
        },
        error: () => {
          this.tplData.data = [];
          this.loadingTpl = false;
        },
      });
  }

  loadLog(): void {
    this.loadingLog = true;
    const filters: NotificationLogFilters = {
      search: this.logSearch,
      from: this.logFrom,
      to: this.logTo,
    };
    this.notificationAdmin
      .getNotificationLog(filters)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (rows) => {
          this.logData.data = rows;
          this.loadingLog = false;
        },
        error: () => {
          this.logData.data = [];
          this.loadingLog = false;
        },
      });
  }

  applyLogFilters(): void {
    this.loadLog();
  }

  viewTemplate(t: NotificationTemplateRow): void {
    this.dialog.open(NotificationTemplateDetailDialogComponent, {
      width: '480px',
      data: { template: t },
    });
  }

  openAddTemplate(): void {
    this.dialog
      .open(NotificationTemplateFormDialogComponent, {
        width: '560px',
        maxHeight: '92vh',
        autoFocus: 'first-heading',
      })
      .afterClosed()
      .subscribe((created) => {
        if (created) {
          this.loadTemplates();
        }
      });
  }

  exportTemplates(format: 'csv' | 'excel' | 'pdf'): void {
    this.notificationAdmin
      .exportTemplates(format, { page: 0, size: 10_000 })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob) => {
          const ext = format === 'excel' ? 'xlsx' : format === 'pdf' ? 'pdf' : 'csv';
          this.saveBlob(blob, `notification_templates.${ext}`);
        },
        error: () => {
          this.snackBar.open('Export failed', 'Dismiss', { duration: 5000 });
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
    row.isActive = active;
    if (!environment.useMocks) {
      this.notificationAdmin.setTemplateActive(row.id, active).pipe(takeUntil(this.destroy$)).subscribe();
    }
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
}
