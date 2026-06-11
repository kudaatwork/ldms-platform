import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import type { SubscriptionPackageRow } from '../../services/platform-wallet-admin.service';

export type SubscriptionPackageDialogMode = 'create' | 'edit' | 'view';

export interface SubscriptionPackageFormDialogData {
  mode: SubscriptionPackageDialogMode;
  row?: SubscriptionPackageRow;
}

@Component({
  selector: 'app-subscription-package-form-dialog',
  templateUrl: './subscription-package-form-dialog.component.html',
  styleUrl: './subscription-package-form-dialog.component.scss',
  standalone: false,
})
export class SubscriptionPackageFormDialogComponent {
  readonly isView: boolean;
  readonly isEdit: boolean;

  form: FormGroup;

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<SubscriptionPackageFormDialogComponent, SubscriptionPackageRow | null>,
    @Inject(MAT_DIALOG_DATA) data: SubscriptionPackageFormDialogData,
  ) {
    this.form = this.fb.group({
      code: ['', [Validators.required, Validators.maxLength(50)]],
      name: ['', [Validators.required, Validators.maxLength(200)]],
      description: ['', Validators.maxLength(1000)],
      monthlyPriceCents: [0, [Validators.required, Validators.min(0)]],
      currencyCode: ['USD', [Validators.required, Validators.maxLength(3)]],
      sortOrder: [0, Validators.min(0)],
      featured: [false],
      active: [true],
    });
    this.isView = data.mode === 'view';
    this.isEdit = data.mode === 'edit';
    if (data.row) {
      this.form.patchValue({
        code: data.row.code,
        name: data.row.name,
        description: data.row.description ?? '',
        monthlyPriceCents: data.row.monthlyPriceCents ?? 0,
        currencyCode: data.row.currencyCode ?? 'USD',
        sortOrder: data.row.sortOrder ?? 0,
        featured: data.row.featured === true,
        active: data.row.active !== false,
      });
    }
    if (data.mode === 'edit') {
      this.form.controls['code'].disable();
    }
    if (this.isView) {
      this.form.disable();
    }
  }

  get title(): string {
    if (this.isView) return 'View subscription package';
    if (this.isEdit) return 'Edit subscription package';
    return 'Add subscription package';
  }

  get subtitle(): string {
    if (this.isView) return 'Review monthly subscription alternative to prepaid wallet billing.';
    if (this.isEdit) return 'Update pricing and marketing copy. Package code cannot change.';
    return 'Create a monthly package organisations can select instead of prepaid wallet.';
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
      code: String(raw.code ?? '').trim().toUpperCase(),
      name: String(raw.name ?? '').trim(),
      description: String(raw.description ?? '').trim() || undefined,
      monthlyPriceCents: Number(raw.monthlyPriceCents ?? 0),
      currencyCode: String(raw.currencyCode ?? 'USD').trim().toUpperCase(),
      sortOrder: Number(raw.sortOrder ?? 0),
      featured: raw.featured === true,
      active: raw.active === true,
    });
  }
}
