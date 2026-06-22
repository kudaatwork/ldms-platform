import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import type { PlatformActionChargeRow } from '../../services/platform-wallet-admin.service';

export type PlatformActionChargeDialogMode = 'create' | 'edit' | 'view';

export interface PlatformActionChargeFormDialogData {
  mode: PlatformActionChargeDialogMode;
  row?: PlatformActionChargeRow;
  defaultCategory?: string;
}

const CATEGORIES = [
  'GENERAL',
  'NOTIFICATIONS',
  'TRIPS',
  'DOCUMENTS',
  'BILLING',
  'PLATFORM',
  'IOT',
  'ORDERS',
  'LOGISTICS',
  'FLEET',
  'PROCUREMENT',
  'SUPPORT',
];

const BILLING_TIERS = ['LIGHT', 'STANDARD', 'HEAVY', 'TRACKING', 'MESSAGING'];

@Component({
  selector: 'app-platform-action-charge-form-dialog',
  templateUrl: './platform-action-charge-form-dialog.component.html',
  styleUrl: './platform-action-charge-form-dialog.component.scss',
  standalone: false,
})
export class PlatformActionChargeFormDialogComponent {
  readonly categories = CATEGORIES;
  readonly billingTiers = BILLING_TIERS;
  readonly isView: boolean;
  readonly isEdit: boolean;
  private readonly chargeId?: number;

  form: FormGroup;

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<PlatformActionChargeFormDialogComponent, PlatformActionChargeRow | null>,
    @Inject(MAT_DIALOG_DATA) data: PlatformActionChargeFormDialogData,
  ) {
    this.form = this.fb.group({
      actionCode: ['', [Validators.required, Validators.maxLength(80)]],
      displayName: ['', [Validators.required, Validators.maxLength(200)]],
      description: ['', Validators.maxLength(500)],
      chargeCents: [0, [Validators.required, Validators.min(0)]],
      category: ['GENERAL', Validators.required],
      billingTier: [''],
      active: [true],
    });
    this.isView = data.mode === 'view';
    this.isEdit = data.mode === 'edit';
    this.chargeId = data.row?.id;
    if (data.row) {
      this.form.patchValue({
        actionCode: data.row.actionCode,
        displayName: data.row.displayName,
        description: data.row.description ?? '',
        chargeCents: data.row.chargeCents ?? 0,
        category: data.row.category ?? 'GENERAL',
        billingTier: data.row.billingTier ?? '',
        active: data.row.active !== false,
      });
    } else if (data.defaultCategory) {
      this.form.patchValue({ category: data.defaultCategory });
    }
    if (data.mode === 'edit') {
      this.form.controls['actionCode'].disable();
    }
    if (this.isView) {
      this.form.disable();
    }
  }

  get title(): string {
    if (this.isView) return 'View action charge';
    if (this.isEdit) return 'Edit action charge';
    return 'Add action charge';
  }

  get subtitle(): string {
    if (this.isView) return 'Review global per-action pricing applied to prepaid wallet organisations.';
    if (this.isEdit) return 'Update charge amount, label, or active flag. Action code cannot change.';
    return 'Define a new platform action code and charge in cents.';
  }

  hasError(control: string, error: string): boolean {
    const c = this.form.get(control);
    return !!c && c.hasError(error) && (c.dirty || c.touched);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    if (this.isView) {
      this.cancel();
      return;
    }
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }
    const raw = this.form.getRawValue();
    this.dialogRef.close({
      id: this.isEdit ? this.chargeId : undefined,
      actionCode: String(raw.actionCode ?? '').trim().toUpperCase(),
      displayName: String(raw.displayName ?? '').trim(),
      description: String(raw.description ?? '').trim() || undefined,
      chargeCents: Number(raw.chargeCents ?? 0),
      category: String(raw.category ?? 'GENERAL').trim().toUpperCase(),
      billingTier: String(raw.billingTier ?? '').trim().toUpperCase() || undefined,
      active: raw.active === true,
    });
  }
}
