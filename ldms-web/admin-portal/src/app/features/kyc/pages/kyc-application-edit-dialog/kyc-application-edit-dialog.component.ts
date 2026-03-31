import { CommonModule } from '@angular/common';
import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import type { KycApplicationRow } from '../../models/kyc-application.model';

export interface KycApplicationEditDialogData {
  row: KycApplicationRow;
}

export interface KycApplicationEditResult {
  applicant: string;
  status: string;
  statusLabel: string;
}

const STATUS_OPTIONS: ReadonlyArray<{ value: string; label: string }> = [
  { value: 'submitted', label: 'Submitted' },
  { value: 'pending', label: 'Awaiting docs' },
  { value: 'stage1', label: 'Stage 1 review' },
  { value: 'stage2', label: 'Stage 2 review' },
  { value: 'approved', label: 'Approved' },
  { value: 'rejected', label: 'Rejected' },
];

@Component({
  selector: 'app-kyc-application-edit-dialog',
  templateUrl: './kyc-application-edit-dialog.component.html',
  styleUrl: './kyc-application-edit-dialog.component.scss',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
})
export class KycApplicationEditDialogComponent {
  readonly form: FormGroup;
  readonly statusOptions = STATUS_OPTIONS;

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<
      KycApplicationEditDialogComponent,
      KycApplicationEditResult | undefined
    >,
    @Inject(MAT_DIALOG_DATA) readonly data: KycApplicationEditDialogData,
  ) {
    const row = data.row;
    this.form = this.fb.group({
      applicant: [row.applicant, [Validators.required, Validators.maxLength(200)]],
      status: [row.status, Validators.required],
    });
  }

  cancel(): void {
    this.dialogRef.close(undefined);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const applicant = String(this.form.value.applicant ?? '').trim();
    const status = this.form.value.status as string;
    const label = STATUS_OPTIONS.find((o) => o.value === status)?.label ?? status;
    this.dialogRef.close({ applicant, status, statusLabel: label });
  }
}
