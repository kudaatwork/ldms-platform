import { CommonModule } from '@angular/common';
import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs';
import { OrganizationsAdminService } from '../../services/organizations-admin.service';
import type { BranchPayload } from '../../models/branch.model';
import type { BranchListRow } from '../../models/organization-directory.model';

export type BranchFormDialogAction = 'create' | 'edit' | 'view';

export interface BranchFormDialogData {
  action: BranchFormDialogAction;
  row?: BranchListRow | null;
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
export class BranchFormDialogComponent implements OnInit {
  form: FormGroup;
  submitting = false;
  loading = false;
  saveError: string | null = null;
  loadError: string | null = null;
  organizationDisplayName = '';
  organizations: Array<{ id: number; name: string }> = [];

  constructor(
    private readonly fb: FormBuilder,
    private readonly orgService: OrganizationsAdminService,
    private readonly snackBar: MatSnackBar,
    private readonly dialogRef: MatDialogRef<BranchFormDialogComponent, BranchFormDialogResult>,
    @Inject(MAT_DIALOG_DATA) readonly data: BranchFormDialogData,
  ) {
    const row = data.row;
    this.form = this.fb.group({
      organizationId: [row?.organizationId ?? null, [Validators.required]],
      branchName: [row?.branchName ?? '', [Validators.required, Validators.maxLength(200)]],
      branchCode: [row?.branchCode ?? '', Validators.maxLength(50)],
      region: [row?.region ?? '', Validators.maxLength(200)],
      email: [row?.email ?? '', [Validators.email, Validators.maxLength(200)]],
      phoneNumber: [row?.phoneNumber ?? '', Validators.maxLength(50)],
      headOffice: [row?.headOffice ?? false],
      active: [row?.active ?? true],
      businessHours: [row?.businessHours ?? '', Validators.maxLength(200)],
    });

    if (data.action === 'view') {
      this.organizationDisplayName = row?.organizationName ?? '';
      this.form.disable();
    }
  }

  ngOnInit(): void {
    if (this.isView && this.data.row?.id) {
      this.loadBranchDetails(this.data.row.id);
      return;
    }
    if (!this.isView) {
      this.orgService.fetchOrganizationsForSelect().subscribe({
        next: (orgs) => (this.organizations = orgs),
        error: () => (this.organizations = []),
      });
    }
  }

  get isEdit(): boolean {
    return this.data.action === 'edit';
  }

  get isView(): boolean {
    return this.data.action === 'view';
  }

  get title(): string {
    if (this.isView) return 'View branch';
    return this.isEdit ? 'Edit branch' : 'Add branch';
  }

  get subtitle(): string {
    if (this.isView) return 'Branch details — read only.';
    return this.isEdit
      ? 'Update the branch details below.'
      : 'Register a new branch under an existing organisation.';
  }

  get primaryActionLabel(): string {
    return this.isEdit ? 'Update' : 'Save';
  }

  get primaryActionLoadingLabel(): string {
    return this.isEdit ? 'Updating…' : 'Saving…';
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
        ? this.orgService.updateBranch(this.data.row.id, payload)
        : this.orgService.createBranch(payload);

    request$.pipe(finalize(() => (this.submitting = false))).subscribe({
      next: (response) => {
        if (!response.ok) {
          this.saveError =
            response.message ?? 'The server rejected this save. Check required fields and try again.';
          this.snackBar.open(this.saveError, 'Close', {
            duration: 5000,
            panelClass: ['app-snackbar-error'],
          });
          return;
        }
        this.snackBar.open(
          response.message ?? (this.isEdit ? 'Branch updated.' : 'Branch created.'),
          'Close',
          { duration: 3000, panelClass: ['app-snackbar-success'] },
        );
        this.dialogRef.close({ saved: true });
      },
      error: (err: { message?: string; status?: number }) => {
        this.saveError =
          (err?.status === 0
            ? 'Request failed before the server response reached the browser. Please retry.'
            : undefined) ??
          err?.message ??
          'Failed to save this branch. Please check inputs and try again.';
        this.snackBar.open(this.saveError, 'Close', {
          duration: 5000,
          panelClass: ['app-snackbar-error'],
        });
      },
    });
  }

  private toPayload(): BranchPayload {
    const v = this.form.getRawValue();
    return {
      organizationId: Number(v.organizationId),
      branchName: String(v.branchName ?? '').trim(),
      branchCode: this.optionalString(v.branchCode),
      region: this.optionalString(v.region),
      email: this.optionalString(v.email),
      phoneNumber: this.optionalString(v.phoneNumber),
      headOffice: Boolean(v.headOffice),
      active: Boolean(v.active),
      businessHours: this.optionalString(v.businessHours),
    };
  }

  private optionalString(value: unknown): string | undefined {
    const s = String(value ?? '').trim();
    return s.length > 0 ? s : undefined;
  }

  private loadBranchDetails(id: number): void {
    this.loading = true;
    this.loadError = null;
    this.form.disable();
    this.orgService
      .getBranch(id)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (row) => {
          this.organizationDisplayName = this.displayText(row.organizationName);
          this.patchFormFromRow(row);
          this.form.disable();
        },
        error: (err: { message?: string; status?: number }) => {
          this.loadError =
            (err?.status === 0
              ? 'Request failed before the server response reached the browser. Please retry.'
              : undefined) ??
            err?.message ??
            'Failed to load branch details.';
        },
      });
  }

  private patchFormFromRow(row: BranchListRow): void {
    this.form.patchValue({
      organizationId: row.organizationId,
      branchName: row.branchName,
      branchCode: row.branchCode,
      region: this.displayText(row.region, ''),
      email: this.displayText(row.email, ''),
      phoneNumber: this.displayText(row.phoneNumber, ''),
      headOffice: row.headOffice,
      active: row.active,
      businessHours: row.businessHours ?? '',
    });
  }

  private displayText(value: string | undefined | null, fallback = '—'): string {
    const s = String(value ?? '').trim();
    return s.length > 0 && s !== '—' ? s : fallback;
  }
}
