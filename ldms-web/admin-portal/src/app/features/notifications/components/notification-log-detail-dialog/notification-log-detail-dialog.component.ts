import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import type { NotificationLogRow } from '../../models/notification-admin.models';

@Component({
  selector: 'app-notification-log-detail-dialog',
  templateUrl: './notification-log-detail-dialog.component.html',
  styleUrl: './notification-log-detail-dialog.component.scss',
  standalone: false,
})
export class NotificationLogDetailDialogComponent {
  constructor(@Inject(MAT_DIALOG_DATA) readonly data: { log: NotificationLogRow }) {}

  get log(): NotificationLogRow {
    return this.data.log;
  }

  asText(value: string | number | null | undefined): string {
    if (value === null || value === undefined) {
      return '—';
    }
    const next = String(value).trim();
    return next.length ? next : '—';
  }

  statusClass(status: string): string {
    switch (status) {
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
}
