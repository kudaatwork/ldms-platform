import { Component, OnInit } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';
import type { NotificationChannel } from '../../models/notification-admin.models';
import { NotificationAdminService } from '../../services/notification-admin.service';

@Component({
  selector: 'app-notification-template-form-dialog',
  templateUrl: './notification-template-form-dialog.component.html',
  styleUrl: './notification-template-form-dialog.component.scss',
  standalone: false,
})
export class NotificationTemplateFormDialogComponent implements OnInit {
  form!: FormGroup;
  submitting = false;
  loadingMeta = true;

  channelOptions: { value: NotificationChannel; label: string; description?: string | undefined }[] =
    [];

  readonly allChannels: NotificationChannel[] = [
    'EMAIL',
    'SMS',
    'WHATSAPP',
    'IN_APP',
    'SLACK',
    'TEAMS',
  ];

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<NotificationTemplateFormDialogComponent, boolean>,
    private readonly notificationAdmin: NotificationAdminService,
    private readonly snackBar: MatSnackBar,
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      templateKey: ['', [Validators.required, Validators.maxLength(100)]],
      description: ['', Validators.required],
      channels: [<NotificationChannel[]>[], this.channelsRequired],
      emailSubject: [''],
      emailBodyHtml: [''],
      smsBody: [''],
      inAppTitle: [''],
      inAppBody: [''],
      whatsappTemplateName: [''],
    });

    this.notificationAdmin
      .getAddTemplateMetadata()
      .pipe(finalize(() => (this.loadingMeta = false)))
      .subscribe({
        next: (meta) => {
          let opts = (meta.channelOptions ?? []).map((o) => ({
            value: o.value as NotificationChannel,
            label: o.label,
            description: o.description,
          }));
          if (!opts.length) {
            opts = this.allChannels.map((c) => ({
              value: c,
              label: c.replace(/_/g, ' '),
              description: undefined,
            }));
          }
          this.channelOptions = opts;
        },
        error: () => {
          this.channelOptions = this.allChannels.map((c) => ({
            value: c,
            label: c.replace(/_/g, ' '),
            description: undefined,
          }));
        },
      });
  }

  hasChannel(ch: NotificationChannel): boolean {
    const v = this.form.get('channels')?.value as NotificationChannel[] | undefined;
    return Array.isArray(v) && v.includes(ch);
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.snackBar.open('Please complete required fields', 'Dismiss', { duration: 4000 });
      return;
    }

    const raw = this.form.getRawValue();
    const channels = (raw.channels as NotificationChannel[]) ?? [];
    if (channels.length === 0) {
      this.snackBar.open('Select at least one channel', 'Dismiss', { duration: 4000 });
      return;
    }

    const err = this.validateChannelContent(channels, raw);
    if (err) {
      this.snackBar.open(err, 'Dismiss', { duration: 6000 });
      return;
    }

    const body = {
      templateKey: String(raw.templateKey).trim(),
      description: String(raw.description).trim(),
      channels,
      emailSubject: raw.emailSubject?.trim() || undefined,
      emailBodyHtml: raw.emailBodyHtml?.trim() || undefined,
      smsBody: raw.smsBody?.trim() || undefined,
      inAppTitle: raw.inAppTitle?.trim() || undefined,
      inAppBody: raw.inAppBody?.trim() || undefined,
      whatsappTemplateName: raw.whatsappTemplateName?.trim() || undefined,
    };

    this.submitting = true;
    this.notificationAdmin
      .createTemplate(body)
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: () => {
          this.snackBar.open('Template created', 'Dismiss', { duration: 4000 });
          this.dialogRef.close(true);
        },
        error: () => {
          this.snackBar.open('Could not create template', 'Dismiss', { duration: 5000 });
        },
      });
  }

  private readonly channelsRequired = (
    ctrl: AbstractControl,
  ): ValidationErrors | null => {
    const v = ctrl.value as NotificationChannel[] | undefined;
    return Array.isArray(v) && v.length > 0 ? null : { channels: true };
  };

  private validateChannelContent(
    channels: NotificationChannel[],
    raw: Record<string, unknown>,
  ): string | null {
    for (const ch of channels) {
      switch (ch) {
        case 'EMAIL': {
          if (!String(raw['emailSubject'] ?? '').trim() || !String(raw['emailBodyHtml'] ?? '').trim()) {
            return 'Email channel requires subject and HTML body.';
          }
          break;
        }
        case 'SMS': {
          if (!String(raw['smsBody'] ?? '').trim()) {
            return 'SMS channel requires a message body.';
          }
          const sms = String(raw['smsBody'] ?? '');
          if (sms.length > 320) {
            return 'SMS body must be at most 320 characters.';
          }
          break;
        }
        case 'IN_APP': {
          if (!String(raw['inAppTitle'] ?? '').trim() || !String(raw['inAppBody'] ?? '').trim()) {
            return 'In-app channel requires title and body.';
          }
          break;
        }
        case 'WHATSAPP': {
          if (!String(raw['whatsappTemplateName'] ?? '').trim()) {
            return 'WhatsApp channel requires a template name (Content SID).';
          }
          break;
        }
        case 'SLACK':
        case 'TEAMS': {
          const body = String(raw['inAppBody'] ?? '').trim();
          const sms = String(raw['smsBody'] ?? '').trim();
          const desc = String(raw['description'] ?? '').trim();
          if (!body && !sms && !desc) {
            return 'Slack/Teams requires message content (use In-app body, SMS body, or description).';
          }
          break;
        }
        default:
          break;
      }
    }
    return null;
  }
}
