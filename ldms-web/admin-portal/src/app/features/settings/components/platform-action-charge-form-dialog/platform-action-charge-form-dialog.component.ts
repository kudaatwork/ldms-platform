import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import type { PlatformActionChargeRow } from '../../services/platform-wallet-admin.service';
import { PlatformWalletAdminService } from '../../services/platform-wallet-admin.service';
import { moduleLabel } from '../../utils/platform-billing-modules.util';
import {
  searchPlatformActionCatalog,
  type PlatformActionCatalogEntry,
} from '../../utils/platform-action-catalog.util';

export type PlatformActionChargeDialogMode = 'create' | 'edit' | 'view';

export interface PlatformActionChargeFormDialogData {
  mode: PlatformActionChargeDialogMode;
  row?: PlatformActionChargeRow;
  defaultCategory?: string;
  preset?: PlatformActionCatalogEntry;
  existingCharges?: PlatformActionChargeRow[];
}

const CATEGORIES = [
  'GENERAL',
  'NOTIFICATIONS',
  'BOT_SERVICE',
  'SUPPORT',
  'TRIPS',
  'DOCUMENTS',
  'BILLING',
  'ANALYTICS',
  'PLATFORM',
  'IOT',
  'ORDERS',
  'LOGISTICS',
  'FLEET',
  'PROCUREMENT',
];

const BILLING_TIERS = [
  'INCLUDED',
  'MILESTONE',
  'TRACKING',
  'TELEMETRY',
  'MESSAGING',
  'LIGHT',
  'STANDARD',
  'HEAVY',
];

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
  readonly isCreate: boolean;
  saving = false;
  private readonly chargeId?: number;
  private readonly existingChargeCodes: Set<string>;

  form: FormGroup;

  constructor(
    private readonly fb: FormBuilder,
    private readonly walletAdmin: PlatformWalletAdminService,
    private readonly snackBar: MatSnackBar,
    private readonly dialogRef: MatDialogRef<PlatformActionChargeFormDialogComponent, PlatformActionChargeRow | null>,
    @Inject(MAT_DIALOG_DATA) data: PlatformActionChargeFormDialogData,
  ) {
    this.existingChargeCodes = new Set(
      (data.existingCharges ?? []).map((row) => row.actionCode.trim().toUpperCase()),
    );
    this.form = this.fb.group({
      actionCode: ['', [Validators.required, Validators.maxLength(80)]],
      displayName: ['', [Validators.required, Validators.maxLength(200)]],
      description: ['', Validators.maxLength(500)],
      chargeAmountDollars: [null, [Validators.required, Validators.min(0)]],
      category: ['GENERAL', Validators.required],
      billingTier: [''],
      active: [true],
    });
    this.isView = data.mode === 'view';
    this.isEdit = data.mode === 'edit';
    this.isCreate = !this.isView && !this.isEdit;
    this.chargeId = data.row?.id;
    if (data.row) {
      this.form.patchValue({
        actionCode: data.row.actionCode,
        displayName: data.row.displayName,
        description: data.row.description ?? '',
        chargeAmountDollars: this.centsToDollars(data.row.chargeCents ?? 0),
        category: data.row.category ?? 'GENERAL',
        billingTier: data.row.billingTier ?? '',
        active: data.row.active !== false,
      });
    } else if (data.preset) {
      this.applyCatalogEntry(data.preset);
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

  get modeLabel(): string {
    if (this.isView) return 'View only';
    if (this.isEdit) return 'Editing';
    return 'New charge';
  }

  get headerIcon(): string {
    if (this.isView) return 'visibility';
    if (this.isEdit) return 'edit_note';
    return 'add_card';
  }

  get chargePreview(): string {
    const raw = this.form.get('chargeAmountDollars')?.value;
    if (raw === null || raw === '' || raw === undefined) {
      return this.walletAdmin.formatCents(0);
    }
    const dollars = Number(raw);
    const cents = this.dollarsToCents(Number.isFinite(dollars) ? dollars : 0);
    return this.walletAdmin.formatCents(cents);
  }

  get storedCents(): number {
    const raw = this.form.get('chargeAmountDollars')?.value;
    if (raw === null || raw === '' || raw === undefined) {
      return 0;
    }
    const dollars = Number(raw);
    return this.dollarsToCents(Number.isFinite(dollars) ? dollars : 0);
  }

  get catalogSuggestions(): PlatformActionCatalogEntry[] {
    if (!this.isCreate) {
      return [];
    }
    const codeQ = String(this.form.get('actionCode')?.value ?? '');
    const nameQ = String(this.form.get('displayName')?.value ?? '');
    const query = codeQ.length >= nameQ.length ? codeQ : nameQ;
    return searchPlatformActionCatalog(query, 18);
  }

  categoryLabel(category: string): string {
    return moduleLabel(category);
  }

  get title(): string {
    if (this.isView) return 'View action charge';
    if (this.isEdit) return 'Edit action charge';
    return 'Add action charge';
  }

  get subtitle(): string {
    if (this.isView) return 'Review global per-action pricing applied to prepaid wallet organisations.';
    if (this.isEdit) return 'Update charge amount, label, or active flag. Action code cannot change.';
    return 'Search by action name or code, pick from the catalog, then set pricing.';
  }

  catalogEntryConfigured(entry: PlatformActionCatalogEntry): boolean {
    return this.existingChargeCodes.has(entry.actionCode);
  }

  displayCatalogEntry(entry: PlatformActionCatalogEntry | null): string {
    return entry ? `${entry.actionCode} — ${entry.displayName}` : '';
  }

  hasError(control: string, error: string): boolean {
    const c = this.form.get(control);
    return !!c && c.hasError(error) && (c.dirty || c.touched);
  }

  applyCatalogEntry(entry: PlatformActionCatalogEntry): void {
    this.form.patchValue({
      actionCode: entry.actionCode,
      displayName: entry.displayName,
      description: entry.description,
      category: entry.category,
      billingTier: entry.billingTier ?? '',
      chargeAmountDollars: this.centsToDollars(entry.suggestedCents ?? 0),
    });
  }

  onCatalogEntryPicked(entry: PlatformActionCatalogEntry): void {
    if (!entry) {
      return;
    }
    this.applyCatalogEntry(entry);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    if (this.isView || this.saving) {
      if (this.isView) {
        this.cancel();
      }
      return;
    }
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.snackBar.open('Fix the highlighted fields before saving.', 'Close', { duration: 4000 });
      return;
    }
    const raw = this.form.getRawValue();
    const chargeCents = this.dollarsToCents(Number(raw.chargeAmountDollars ?? 0));
    const payload: PlatformActionChargeRow = {
      id: this.isEdit ? this.chargeId : undefined,
      actionCode: String(raw.actionCode ?? '').trim().toUpperCase(),
      displayName: String(raw.displayName ?? '').trim(),
      description: String(raw.description ?? '').trim() || undefined,
      chargeCents,
      category: String(raw.category ?? 'GENERAL').trim().toUpperCase(),
      billingTier: String(raw.billingTier ?? '').trim().toUpperCase() || undefined,
      active: raw.active === true,
    };
    this.saving = true;
    this.walletAdmin.saveActionCharge(payload).subscribe({
      next: (saved) => {
        this.saving = false;
        this.dialogRef.close(saved);
      },
      error: (err: unknown) => {
        this.saving = false;
        const message = err instanceof Error ? err.message : 'Could not save action charge.';
        this.snackBar.open(message, 'Close', { duration: 6000 });
      },
    });
  }

  private centsToDollars(cents: number): number {
    return Math.round(Number(cents ?? 0)) / 100;
  }

  private dollarsToCents(dollars: number): number {
    if (!Number.isFinite(dollars) || dollars < 0) {
      return 0;
    }
    return Math.round(dollars * 100);
  }
}
