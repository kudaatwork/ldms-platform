import { ChangeDetectorRef, Component, Inject, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormControl,
  FormGroup,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subscription, forkJoin, of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import type {
  CreateTemplateRequest,
  NotificationChannel,
  NotificationTemplateRow,
  TemplateCreationMetadataDto,
  UpdateTemplateRequest,
} from '../../models/notification-admin.models';
import { NotificationAdminService } from '../../services/notification-admin.service';
import {
  type ChannelDeliveryToggleRow,
  buildChannelDeliveryToggleRows,
  hasPerChannelDeliveryConfig,
  isChannelDeliveryActive,
  isOrganizationNotificationTemplateKey,
  normalizeChannelDeliveryFlags,
} from '../../utils/notification-channel.util';

type PlaceholderUseArea = 'EMAIL_SUBJECT' | 'EMAIL_HTML' | 'SMS' | 'IN_APP' | 'WHATSAPP';

interface PlaceholderCatalogItem {
  key: string;
  label: string;
  example: string;
  useIn: PlaceholderUseArea[];
}

@Component({
  selector: 'app-notification-template-form-dialog',
  templateUrl: './notification-template-form-dialog.component.html',
  styleUrl: './notification-template-form-dialog.component.scss',
  standalone: false,
})
export class NotificationTemplateFormDialogComponent implements OnInit, OnDestroy {
  form!: FormGroup;
  /** Per-channel delivery toggles (bound with formControlName). */
  channelDeliveryForm!: FormGroup;
  submitting = false;
  loadingShell = true;
  listLoadError: string | null = null;
  readonly isEditMode: boolean;
  private readonly editTemplateId: number | null;
  private editTemplateActive = true;
  private channelDeliverySub: Subscription | null = null;

  channelOptions: { value: NotificationChannel; label: string; description?: string | undefined }[] = [];
  channelDeliveryEnabled: Record<string, boolean> | null = null;
  /** Stable list for *ngFor (avoids toggle flicker from getter re-runs). */
  deliveryRows: ChannelDeliveryToggleRow[] = [];
  channelDeliverySaving = false;
  private suppressChannelDeliverySync = false;
  private lastSavedDeliveryFlags: Record<string, boolean> | null = null;
  showPlaceholderCatalog = false;
  readonly placeholderCatalog: PlaceholderCatalogItem[] = [
    { key: 'firstName', label: 'Recipient first name', example: 'Tariro', useIn: ['EMAIL_SUBJECT', 'EMAIL_HTML', 'SMS', 'IN_APP', 'WHATSAPP'] },
    { key: 'userName', label: 'Recipient username', example: 'tariro.ncube', useIn: ['EMAIL_HTML', 'SMS', 'WHATSAPP'] },
    { key: 'Email', label: 'Recipient email address', example: 'tariro@example.com', useIn: ['EMAIL_HTML'] },
    { key: 'organizationName', label: 'Organization name', example: 'Project LX Logistics', useIn: ['EMAIL_SUBJECT', 'EMAIL_HTML', 'IN_APP'] },
    { key: 'orderNumber', label: 'Order number', example: 'ORD-2026-00421', useIn: ['EMAIL_SUBJECT', 'EMAIL_HTML', 'SMS', 'IN_APP', 'WHATSAPP'] },
    { key: 'branchName', label: 'Branch name', example: 'Harare South Depot', useIn: ['SMS', 'IN_APP', 'WHATSAPP'] },
    { key: 'driverName', label: 'Assigned driver name', example: 'M. Dube', useIn: ['IN_APP', 'WHATSAPP'] },
    { key: 'resetLink', label: 'Password reset URL', example: 'https://app.example.com/reset/abc123', useIn: ['EMAIL_HTML', 'WHATSAPP'] },
  ];

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
    private readonly cdr: ChangeDetectorRef,
    private readonly dialogRef: MatDialogRef<NotificationTemplateFormDialogComponent, boolean>,
    private readonly notificationAdmin: NotificationAdminService,
    private readonly snackBar: MatSnackBar,
    @Inject(MAT_DIALOG_DATA)
    readonly data: { template?: NotificationTemplateRow } | null,
  ) {
    this.isEditMode = !!data?.template;
    this.editTemplateId = data?.template?.id ?? null;
    this.editTemplateActive = data?.template?.isActive ?? true;
  }

  get dialogTitle(): string {
    return this.isEditMode ? 'Edit notification template' : 'Add notification template';
  }

  get dialogSubtitle(): string {
    return this.isEditMode
      ? 'Update channels and content. Template code must stay unique.'
      : 'Define a unique key, pick channels, then add the content required for each channel.';
  }

  get isOrganizationNotificationTemplate(): boolean {
    const key = String(this.form?.get('templateKey')?.value ?? '').trim();
    return isOrganizationNotificationTemplateKey(key);
  }

  get hasPerChannelDeliveryControls(): boolean {
    const key = String(this.form?.get('templateKey')?.value ?? '').trim();
    if (isOrganizationNotificationTemplateKey(key)) {
      return true;
    }
    return Object.keys(this.channelDeliveryForm.controls).length > 0;
  }

  trackDeliveryRow(_index: number, row: ChannelDeliveryToggleRow): string {
    return row.channel;
  }

  ngOnInit(): void {
    this.channelDeliveryForm = this.fb.group({});
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
      whatsappBody: [''],
    });

    this.form.get('channels')?.valueChanges.subscribe((channels) => {
      this.syncChannelDeliveryWithChannels((channels as NotificationChannel[]) ?? []);
    });

    this.form.get('templateKey')?.valueChanges.subscribe(() => {
      const channels = (this.form.get('channels')?.value as NotificationChannel[]) ?? [];
      this.syncChannelDeliveryWithChannels(channels);
    });

    if (this.isEditMode && this.editTemplateId) {
      forkJoin({
        meta: this.notificationAdmin.getAddTemplateMetadata().pipe(
          catchError(() => of({ channelOptions: [] } as TemplateCreationMetadataDto)),
        ),
        row: this.notificationAdmin.getTemplateById(this.editTemplateId).pipe(
          catchError(() => {
            const fallback = this.data?.template;
            return fallback ? of(fallback) : of(null as NotificationTemplateRow | null);
          }),
        ),
      })
        .pipe(finalize(() => (this.loadingShell = false)))
        .subscribe({
          next: ({ meta, row }) => {
            if (!row) {
              this.listLoadError = 'Could not load this template from the server.';
              return;
            }
            this.applyChannelOptions(meta.channelOptions ?? []);
            this.patchFormFromTemplate(row);
          },
          error: () => {
            this.listLoadError = 'Could not load this template.';
            this.applyChannelOptions([]);
          },
        });
      return;
    }

    this.notificationAdmin
      .getAddTemplateMetadata()
      .pipe(
        catchError(() => of({ channelOptions: [] } as TemplateCreationMetadataDto)),
        finalize(() => (this.loadingShell = false)),
      )
      .subscribe({
        next: (meta) => {
          this.applyChannelOptions(meta.channelOptions ?? []);
        },
      });
  }

  ngOnDestroy(): void {
    this.channelDeliverySub?.unsubscribe();
  }

  private applyChannelOptions(opts: { value: string; label: string; description?: string }[]): void {
    let mapped = (opts ?? []).map((o) => ({
      value: o.value as NotificationChannel,
      label: o.label,
      description: o.description,
    }));
    if (!mapped.length) {
      mapped = this.allChannels.map((c) => ({
        value: c,
        label: c.replace(/_/g, ' '),
        description: undefined,
      }));
    }
    this.channelOptions = mapped;
  }

  private patchFormFromTemplate(t: NotificationTemplateRow): void {
    const channels = (t.channels ?? []) as NotificationChannel[];
    const flags = normalizeChannelDeliveryFlags(channels, t.channelDeliveryEnabled, t.templateKey ?? '');
    this.channelDeliveryEnabled = flags;
    this.suppressChannelDeliverySync = true;
    this.form.patchValue(
      {
        templateKey: t.templateKey ?? '',
        description: t.description ?? '',
        channels: t.channels ?? [],
        emailSubject: t.emailSubject ?? '',
        emailBodyHtml: t.emailBodyHtml ?? '',
        smsBody: t.smsBody ?? '',
        inAppTitle: t.inAppTitle ?? '',
        inAppBody: t.inAppBody ?? '',
        whatsappTemplateName: t.whatsappTemplateName ?? '',
        whatsappBody: t.whatsappBody ?? '',
      },
      { emitEvent: false },
    );
    this.suppressChannelDeliverySync = false;
    this.lastSavedDeliveryFlags = { ...flags };
    this.initChannelDeliveryForm(channels, flags);
    this.cdr.markForCheck();
  }

  private initChannelDeliveryForm(
    channels: NotificationChannel[],
    flags: Record<string, boolean>,
  ): void {
    this.channelDeliverySub?.unsubscribe();
    const controls: Record<string, FormControl<boolean>> = {};
    for (const ch of channels) {
      controls[ch] = this.fb.nonNullable.control(flags[ch] === true);
    }
    this.channelDeliveryForm = this.fb.group(controls);
    this.channelDeliveryEnabled = { ...flags };
    this.refreshDeliveryRows();
    this.channelDeliverySub = this.channelDeliveryForm.valueChanges.subscribe(() => {
      this.channelDeliveryEnabled = this.channelDeliveryForm.getRawValue() as Record<string, boolean>;
      this.refreshDeliveryRows();
      this.cdr.markForCheck();
    });
  }

  private refreshDeliveryRows(): void {
    const channels = (this.form?.get('channels')?.value as NotificationChannel[] | undefined) ?? [];
    const flags = this.readChannelDeliveryFlags(channels);
    this.deliveryRows = buildChannelDeliveryToggleRows(channels, flags);
  }

  private readChannelDeliveryFlags(channels: NotificationChannel[]): Record<string, boolean> {
    const fromForm = this.channelDeliveryForm.getRawValue() as Record<string, boolean>;
    const templateKey = String(this.form.get('templateKey')?.value ?? '').trim();
    return normalizeChannelDeliveryFlags(channels, fromForm ?? this.channelDeliveryEnabled, templateKey);
  }

  hasChannel(ch: NotificationChannel): boolean {
    const v = this.form.get('channels')?.value as NotificationChannel[] | undefined;
    return Array.isArray(v) && v.includes(ch);
  }

  isChannelDeliveryActive(ch: NotificationChannel): boolean {
    const ctrl = this.channelDeliveryForm.get(ch);
    if (ctrl) {
      return ctrl.value === true;
    }
    return isChannelDeliveryActive(ch, this.channelDeliveryEnabled);
  }

  isInactiveChannelSection(ch: NotificationChannel): boolean {
    return this.hasChannel(ch) && !this.isChannelDeliveryActive(ch);
  }

  onChannelDeliveryToggle(channel: NotificationChannel, active: boolean): void {
    const channels = (this.form.get('channels')?.value as NotificationChannel[]) ?? [];
    const flags = this.readChannelDeliveryFlags(channels);
    flags[channel] = active;

    const anyActive = channels.some((ch) => flags[ch] === true);
    if (!anyActive) {
      this.channelDeliveryForm.get(channel)?.setValue(false, { emitEvent: false });
      this.refreshDeliveryRows();
      this.snackBar.open('At least one channel must stay on for delivery.', 'Close', {
        duration: 4000,
        panelClass: ['app-snackbar-error'],
      });
      this.cdr.detectChanges();
      return;
    }

    this.channelDeliveryEnabled = { ...flags };
    this.refreshDeliveryRows();

    if (this.isEditMode && this.editTemplateId) {
      this.persistChannelDelivery(flags);
    } else {
      this.cdr.markForCheck();
    }
  }

  private persistChannelDelivery(flags: Record<string, boolean>): void {
    if (!this.editTemplateId || this.channelDeliverySaving) {
      return;
    }
    this.channelDeliverySaving = true;
    this.notificationAdmin
      .setTemplateChannelDelivery(this.editTemplateId, flags)
      .pipe(finalize(() => (this.channelDeliverySaving = false)))
      .subscribe({
        next: () => {
          this.lastSavedDeliveryFlags = { ...flags };
          this.snackBar.open('Channel delivery updated', 'Close', {
            duration: 2500,
            panelClass: ['app-snackbar-success'],
          });
          this.cdr.markForCheck();
        },
        error: (e) => {
          this.snackBar.open(this.errorMessage(e, 'Could not save channel delivery.'), 'Close', {
            duration: 5000,
            panelClass: ['app-snackbar-error'],
          });
          const channels = (this.form.get('channels')?.value as NotificationChannel[]) ?? [];
          const templateKey = String(this.form.get('templateKey')?.value ?? '').trim();
          const restored = normalizeChannelDeliveryFlags(
            channels,
            this.lastSavedDeliveryFlags,
            templateKey,
          );
          this.initChannelDeliveryForm(channels, restored);
          this.cdr.markForCheck();
        },
      });
  }

  private syncChannelDeliveryWithChannels(channels: NotificationChannel[]): void {
    if (this.suppressChannelDeliverySync || !channels.length) {
      return;
    }
    const templateKey = String(this.form.get('templateKey')?.value ?? '').trim();
    if (!hasPerChannelDeliveryConfig(templateKey, this.channelDeliveryEnabled)) {
      return;
    }
    const flags = normalizeChannelDeliveryFlags(
      channels,
      this.readChannelDeliveryFlags(channels),
      templateKey,
    );
    this.initChannelDeliveryForm(channels, flags);
    this.cdr.markForCheck();
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  submit(): void {
    if (this.form.invalid || this.channelDeliveryForm.invalid) {
      this.form.markAllAsTouched();
      this.snackBar.open('Please complete required fields', 'Close', { duration: 4000 });
      return;
    }

    const raw = this.form.getRawValue();
    const channels = (raw.channels as NotificationChannel[]) ?? [];
    if (channels.length === 0) {
      this.snackBar.open('Select at least one channel', 'Close', { duration: 4000 });
      return;
    }

    const deliveryFlags = this.channelDeliveryEnabledForPayload(
      String(raw['templateKey'] ?? '').trim(),
      channels,
    );
    const deliveryErr = this.validateChannelDelivery(channels, deliveryFlags);
    if (deliveryErr) {
      this.snackBar.open(deliveryErr, 'Close', { duration: 6000 });
      return;
    }

    const err = this.validateChannelContent(channels, raw, deliveryFlags ?? null);
    if (err) {
      this.snackBar.open(err, 'Close', { duration: 6000 });
      return;
    }

    const body = this.buildCreatePayload(raw, channels);

    this.submitting = true;
    if (this.isEditMode && this.editTemplateId) {
      const updateBody: UpdateTemplateRequest = {
        id: this.editTemplateId,
        isActive: this.editTemplateActive,
        ...body,
      };
      this.notificationAdmin
        .updateTemplate(updateBody)
        .pipe(finalize(() => (this.submitting = false)))
        .subscribe({
          next: () => {
            this.snackBar.open('Template updated', 'Close', {
              duration: 4000,
              panelClass: ['app-snackbar-success'],
            });
            this.dialogRef.close(true);
          },
          error: (e) => {
            this.snackBar.open(this.errorMessage(e, 'Could not update template.'), 'Close', {
              duration: 5000,
              panelClass: ['app-snackbar-error'],
            });
          },
        });
      return;
    }

    this.notificationAdmin
      .createTemplate(body)
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: () => {
          this.snackBar.open('Template created', 'Close', {
            duration: 4000,
            panelClass: ['app-snackbar-success'],
          });
          this.dialogRef.close(true);
        },
        error: (e) => {
          this.snackBar.open(this.errorMessage(e, 'Could not create template.'), 'Close', {
            duration: 5000,
            panelClass: ['app-snackbar-error'],
          });
        },
      });
  }

  private buildCreatePayload(
    raw: Record<string, unknown>,
    channels: NotificationChannel[],
  ): CreateTemplateRequest {
    const templateKey = String(raw['templateKey'] ?? '').trim();
    return {
      templateKey,
      description: String(raw['description'] ?? '').trim(),
      channels,
      channelDeliveryEnabled: this.channelDeliveryEnabledForPayload(templateKey, channels),
      emailSubject: String(raw['emailSubject'] ?? '').trim(),
      emailBodyHtml: String(raw['emailBodyHtml'] ?? '').trim(),
      smsBody: String(raw['smsBody'] ?? '').trim(),
      inAppTitle: String(raw['inAppTitle'] ?? '').trim(),
      inAppBody: String(raw['inAppBody'] ?? '').trim(),
      whatsappTemplateName: String(raw['whatsappTemplateName'] ?? '').trim(),
      whatsappBody: String(raw['whatsappBody'] ?? '').trim(),
    };
  }

  private readonly channelsRequired = (
    ctrl: AbstractControl,
  ): ValidationErrors | null => {
    const v = ctrl.value as NotificationChannel[] | undefined;
    return Array.isArray(v) && v.length > 0 ? null : { channels: true };
  };

  private channelDeliveryEnabledForPayload(
    templateKey: string,
    channels: NotificationChannel[],
  ): Record<string, boolean> | null | undefined {
    if (!hasPerChannelDeliveryConfig(templateKey, this.channelDeliveryEnabled)) {
      return undefined;
    }
    return this.readChannelDeliveryFlags(channels);
  }

  private validateChannelDelivery(
    channels: NotificationChannel[],
    deliveryFlags: Record<string, boolean> | null | undefined,
  ): string | null {
    if (!deliveryFlags || !channels.length) {
      return null;
    }
    const anyActive = channels.some((ch) => isChannelDeliveryActive(ch, deliveryFlags));
    if (!anyActive) {
      return 'Turn on delivery for at least one channel.';
    }
    return null;
  }

  private validateChannelContent(
    channels: NotificationChannel[],
    raw: Record<string, unknown>,
    deliveryFlags: Record<string, boolean> | null,
  ): string | null {
    for (const ch of channels) {
      if (!isChannelDeliveryActive(ch, deliveryFlags)) {
        continue;
      }
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
          if (!String(raw['whatsappTemplateName'] ?? '').trim() || !String(raw['whatsappBody'] ?? '').trim()) {
            return 'WhatsApp channel requires template name (Content SID) and message body.';
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

  private errorMessage(error: unknown, fallback: string): string {
    const err = error as {
      status?: number;
      error?: { messageResponse?: string; message?: string; error?: string };
      message?: string;
    };
    if (err?.status === 0) {
      return 'Request failed before the server response reached the browser. Please retry.';
    }
    return err?.error?.messageResponse || err?.error?.message || err?.error?.error || err?.message || fallback;
  }

  placeholderToken(key: string): string {
    return `{{${key}}}`;
  }

  placeholderAreaLabel(area: PlaceholderUseArea): string {
    switch (area) {
      case 'EMAIL_SUBJECT':
        return 'Email subject';
      case 'EMAIL_HTML':
        return 'Email HTML body';
      case 'SMS':
        return 'SMS body';
      case 'IN_APP':
        return 'In-app title/body';
      case 'WHATSAPP':
        return 'WhatsApp body';
      default:
        return area;
    }
  }

  async copyPlaceholder(key: string): Promise<void> {
    const token = this.placeholderToken(key);
    try {
      if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(token);
      }
      this.snackBar.open(`Copied ${token}`, 'Close', {
        duration: 2000,
        panelClass: ['app-snackbar-success'],
      });
    } catch {
      this.snackBar.open(`Copy failed. Use ${token}`, 'Close', {
        duration: 4000,
      });
    }
  }
}
