import { CommonModule } from '@angular/common';
import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs';
import { OrganizationsAdminService } from '../../services/organizations-admin.service';
import type { IndustryPayload } from '../../models/industry.model';
import type { IndustryUsageRow } from '../../models/organization-directory.model';

export type IndustryFormDialogAction = 'create' | 'edit' | 'view';

export interface IndustryFormDialogData {
  action: IndustryFormDialogAction;
  row?: IndustryUsageRow | null;
}

export interface IndustryFormDialogResult {
  saved: boolean;
}

@Component({
  selector: 'app-industry-form-dialog',
  templateUrl: './industry-form-dialog.component.html',
  styleUrl: './industry-form-dialog.component.scss',
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
export class IndustryFormDialogComponent {
  form: FormGroup;
  submitting = false;
  saveError: string | null = null;

  constructor(
    private readonly fb: FormBuilder,
    private readonly orgService: OrganizationsAdminService,
    private readonly snackBar: MatSnackBar,
    private readonly dialogRef: MatDialogRef<IndustryFormDialogComponent, IndustryFormDialogResult>,
    @Inject(MAT_DIALOG_DATA) readonly data: IndustryFormDialogData,
  ) {
    const row = data.row;
    this.form = this.fb.group({
      name: [row?.name ?? '', [Validators.required, Validators.maxLength(200)]],
      industryCode: [row?.industryCode ?? '', Validators.maxLength(50)],
      description: [row?.description ?? '', Validators.maxLength(500)],
      regulatoryBodyName: [row?.regulatoryBodyName ?? '', Validators.maxLength(200)],
      regulatoryBodyContactInfo: [row?.regulatoryBodyContactInfo ?? '', Validators.maxLength(300)],
      complianceRequirements: [row?.complianceRequirements ?? '', Validators.maxLength(1000)],
      active: [row?.active ?? true],
    });

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

  get title(): string {
    if (this.isView) return 'View industry';
    return this.isEdit ? 'Edit industry' : 'Add industry';
  }

  get subtitle(): string {
    if (this.isView) return 'Industry details — read only.';
    return this.isEdit
      ? 'Update sector metadata used when classifying organisations.'
      : 'Add an industry sector for organisation registration and reporting.';
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
        ? this.orgService.updateIndustry(this.data.row.id, payload)
        : this.orgService.createIndustry(payload);

    request$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
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
          this.snackBar.open(response.message ?? (this.isEdit ? 'Industry updated.' : 'Industry created.'), 'Close', {
            duration: 3000,
            panelClass: ['app-snackbar-success'],
          });
          this.dialogRef.close({ saved: true });
        },
        error: (err: { message?: string; status?: number }) => {
          this.saveError =
            (err?.status === 0
              ? 'Request failed before the server response reached the browser. Please retry.'
              : undefined) ??
            err?.message ??
            'Failed to save this industry. Please check inputs and try again.';
          this.snackBar.open(this.saveError, 'Close', {
            duration: 5000,
            panelClass: ['app-snackbar-error'],
          });
        },
      });
  }

  private toPayload(): IndustryPayload {
    const v = this.form.getRawValue();
    return {
      name: String(v.name ?? '').trim(),
      industryCode: this.optionalString(v.industryCode),
      description: this.optionalString(v.description),
      regulatoryBodyName: this.optionalString(v.regulatoryBodyName),
      regulatoryBodyContactInfo: this.optionalString(v.regulatoryBodyContactInfo),
      complianceRequirements: this.optionalString(v.complianceRequirements),
      active: Boolean(v.active),
    };
  }

  private optionalString(value: unknown): string | undefined {
    const s = String(value ?? '').trim();
    return s.length > 0 ? s : undefined;
  }
}
