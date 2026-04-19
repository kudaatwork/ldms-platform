import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import type { NotificationTemplateRow } from '../../models/notification-admin.models';

@Component({
  selector: 'app-notification-template-detail-dialog',
  templateUrl: './notification-template-detail-dialog.component.html',
  styleUrl: './notification-template-detail-dialog.component.scss',
  standalone: false,
})
export class NotificationTemplateDetailDialogComponent {
  constructor(
    @Inject(MAT_DIALOG_DATA) readonly data: { template: NotificationTemplateRow },
  ) {}

  channelsText(t: NotificationTemplateRow): string {
    return t.channels.join(', ');
  }
}
