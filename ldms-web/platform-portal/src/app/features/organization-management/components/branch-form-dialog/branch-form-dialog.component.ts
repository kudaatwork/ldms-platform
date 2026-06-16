import { CommonModule } from '@angular/common';
import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs';
import { BranchDetail } from '../../../../core/services/organization.service';
import { OrgManagementPortalService } from '../../services/org-management-portal.service';

export type BranchFormDialogAction = 'create' | 'edit' | 'view';
export type BranchFormDialogMode = 'top-level' | 'sub-level';

export interface BranchFormDialogData {
  action: BranchFormDialogAction;
  mode: BranchFormDialogMode;
  row?: BranchDetail | null;
  parentBranches?: BranchDetail[];
}

export interface BranchFormDialogResult {
  saved: boolean;
}

@Component({
  selector: 'app-branch-form-dialog',
  templateUrl: './branch-form-dialog.component.html',
  styleUrl: './branch-form-dialog.component.scss',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
  ],
})
export class BranchFormDialogComponent {
  form: FormGroup;
  submitting = false;
  saveError: string | null = null;

  constructor(
    private readonly fb: FormBuilder,
    private readonly orgMgmt: OrgManagementPortalService,
    private readonly snackBar: MatSnackBar,
    private readonly dialogRef: MatDialogRef<BranchFormDialogComponent, BranchFormDialogResult>,
    @Inject(MAT_DIALOG_DATA) readonly data: BranchFormDialogData,
  ) {
    const row = data.row;
    this.form = this.fb.group({
      branchName: [row?.branchName ?? '', [Validators.required, Validators.maxLength(200)]],
      branchCode: [row?.branchCode ?? '', Validators.maxLength(50)],
      region: [row?.region ?? '', Validators.maxLength(100)],
      email: [row?.email ?? '', [Validators.email, Validators.maxLength(255)]],
      phoneNumber: [row?.phoneNumber ?? '', Validators.maxLength(50)],
      businessHours: [row?.businessHours ?? '', Validators.maxLength(200)],
      headOffice: [row?.headOffice ?? false],
      active: [row?.active ?? true],
      parentBranchId: [row?.parentBranchId ?? null],
      depot: [row?.depot ?? false],
    });

    if (data.mode === 'sub-level' && data.action === 'create') {
      this.form.get('parentBranchId')?.setValidators([Validators.required]);
    }
    if (data.action === 'view') {
      this.form.disable();
    }
  }

  get isEdit(): boolean {
    return this.data.action === 'edit';
  }

  get isView(): boolean {
    return this.data.action === 'view';
  }

  get isSubLevel(): boolean {
    return this.data.mode === 'sub-level';
  }

  get title(): string {
    if (this.isView) {
      return this.isSubLevel ? 'View sub-branch' : 'View branch';
    }
    if (this.isEdit) {
      return this.isSubLevel ? 'Edit sub-branch' : 'Edit branch';
    }
    return this.isSubLevel ? 'Add sub-branch or depot' : 'Add branch';
  }

  get subtitle(): string {
    if (this.isView) {
      return 'Branch details — read only.';
    }
    if (this.isEdit) {
      return 'Update the branch details below.';
    }
    return this.isSubLevel
      ? 'Register a sub-branch or depot under an existing top-level branch.'
      : 'Register a new top-level branch for your organisation.';
  }

  get primaryActionLabel(): string {
    return this.isEdit ? 'Update' : 'Save';
  }

  get primaryActionLoadingLabel(): string {
    return this.isEdit ? 'Updating…' : 'Saving…';
  }

  get parentBranchOptions(): BranchDetail[] {
    return this.data.parentBranches ?? [];
  }

  hasError(control: string, error: string): boolean {
    const c = this.form.get(control);
    return !!(c && (c.touched || c.dirty) && c.hasError(error));
  }

  cancel(): void {
    this.dialogRef.close({ saved: false });
  }

  save(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const payload = this.toPayload();
    this.submitting = true;
    this.saveError = null;

    const request$ =
      this.isEdit && this.data.row?.id
        ? this.orgMgmt.updateBranch(this.data.row.id, payload)
        : this.orgMgmt.addBranch(payload);

    request$.pipe(finalize(() => (this.submitting = false))).subscribe({
      next: () => {
        this.snackBar.open(
          this.isEdit ? 'Branch updated.' : 'Branch created.',
          'Close',
          { duration: 3000 },
        );
        this.dialogRef.close({ saved: true });
      },
      error: (err: Error) => {
        this.saveError = err.message ?? 'Failed to save this branch. Please check inputs and try again.';
        this.snackBar.open(this.saveError, 'Close', { duration: 5000 });
      },
    });
  }

  private toPayload() {
    const v = this.form.getRawValue();
    return {
      branchName: String(v.branchName ?? '').trim(),
      branchCode: this.optionalString(v.branchCode),
      region: this.optionalString(v.region),
      email: this.optionalString(v.email),
      phoneNumber: this.optionalString(v.phoneNumber),
      businessHours: this.optionalString(v.businessHours),
      headOffice: Boolean(v.headOffice),
      active: Boolean(v.active),
      parentBranchId: this.isSubLevel && v.parentBranchId ? Number(v.parentBranchId) : undefined,
      depot: this.isSubLevel ? Boolean(v.depot) : false,
    };
  }

  private optionalString(value: unknown): string | undefined {
    const s = String(value ?? '').trim();
    return s.length > 0 ? s : undefined;
  }
}
