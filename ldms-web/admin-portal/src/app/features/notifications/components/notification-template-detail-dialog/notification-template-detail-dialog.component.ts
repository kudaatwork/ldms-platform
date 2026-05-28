import { formatDate } from '@angular/common';
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import type { NotificationTemplateRow } from '../../models/notification-admin.models';
import { formatChannelsWithDelivery } from '../../utils/notification-channel.util';

@Component({
  selector: 'app-notification-template-detail-dialog',
  templateUrl: './notification-template-detail-dialog.component.html',
  styleUrl: './notification-template-detail-dialog.component.scss',
  standalone: false,
})
export class NotificationTemplateDetailDialogComponent {
  constructor(
    @Inject(MAT_DIALOG_DATA) readonly data: { template: NotificationTemplateRow },
    private readonly snackBar: MatSnackBar,
  ) {}

  channelsText(t: NotificationTemplateRow): string {
    return formatChannelsWithDelivery(t);
  }

  asText(value: string | null | undefined): string {
    const next = String(value ?? '').trim();
    return next.length ? next : '—';
  }

  hasEmailBodyHtml(): boolean {
    return Boolean(String(this.data.template.emailBodyHtml ?? '').trim());
  }

  /** True when the value is non-empty and not the empty-state em dash. */
  copyable(value: string | number | null | undefined): boolean {
    const t = String(value ?? '').trim();
    return t.length > 0 && t !== '—';
  }

  copyableDate(value: Date | string | null | undefined): boolean {
    return value != null && String(value).trim().length > 0;
  }

  async copyToClipboard(value: string, label: string): Promise<void> {
    const text = value === '—' ? '' : value.trim();
    if (!text) {
      this.snackBar.open('Nothing to copy.', 'Close', { duration: 2500 });
      return;
    }
    try {
      if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(text);
      } else {
        const ta = document.createElement('textarea');
        ta.value = text;
        ta.setAttribute('readonly', '');
        ta.style.position = 'fixed';
        ta.style.left = '-9999px';
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
      }
      this.snackBar.open(`${label} copied to clipboard.`, 'Close', { duration: 2500 });
    } catch {
      this.snackBar.open('Copy failed. Select the text and copy manually.', 'Close', {
        duration: 3500,
      });
    }
  }

  copyId(): void {
    void this.copyToClipboard(String(this.data.template.id), 'ID');
  }

  copyTemplateKey(): void {
    void this.copyToClipboard(this.data.template.templateKey, 'Template code');
  }

  copyStatus(): void {
    void this.copyToClipboard(this.data.template.isActive ? 'Active' : 'Inactive', 'Status');
  }

  copyCreatedAt(): void {
    const v = this.data.template.createdAt;
    if (v == null) {
      return;
    }
    void this.copyToClipboard(this.formatDateTime(v), 'Created at');
  }

  copyUpdatedAt(): void {
    const v = this.data.template.updatedAt;
    if (v == null) {
      return;
    }
    void this.copyToClipboard(this.formatDateTime(v), 'Updated at');
  }

  copyEmailSubject(): void {
    void this.copyToClipboard(String(this.data.template.emailSubject ?? ''), 'Subject');
  }

  copyChannels(): void {
    void this.copyToClipboard(this.channelsText(this.data.template), 'Channels');
  }

  copyDescription(): void {
    void this.copyToClipboard(String(this.data.template.description ?? ''), 'Description');
  }

  copyEmailBodyHtml(): void {
    void this.copyToClipboard(String(this.data.template.emailBodyHtml ?? ''), 'Email HTML body');
  }

  copySmsBody(): void {
    void this.copyToClipboard(String(this.data.template.smsBody ?? ''), 'SMS body');
  }

  copyInAppTitle(): void {
    void this.copyToClipboard(String(this.data.template.inAppTitle ?? ''), 'In-app title');
  }

  copyInAppBody(): void {
    void this.copyToClipboard(String(this.data.template.inAppBody ?? ''), 'In-app body');
  }

  copyWhatsappTemplateName(): void {
    void this.copyToClipboard(String(this.data.template.whatsappTemplateName ?? ''), 'WhatsApp template name');
  }

  copyWhatsappBody(): void {
    void this.copyToClipboard(String(this.data.template.whatsappBody ?? ''), 'WhatsApp body');
  }

  private formatDateTime(value: Date | string): string {
    const d = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(d.getTime())) {
      return String(value);
    }
    return formatDate(d, 'medium', 'en-US');
  }
}
