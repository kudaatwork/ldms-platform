import { CommonModule } from '@angular/common';
import { Component, Inject, OnDestroy, Optional } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { Subject, finalize, takeUntil } from 'rxjs';
import type {
  CreateFleetCompliancePayload,
  EditFleetCompliancePayload,
  FleetComplianceRow,
  FleetComplianceSubjectType,
  FleetComplianceType,
  FleetDriverRow,
  FleetVehicleRow,
} from '../../models/fleet.model';
import { FleetPortalService } from '../../services/fleet-portal.service';

export type FleetComplianceDialogData = {
  record?: FleetComplianceRow;
  assets: FleetVehicleRow[];
  drivers: FleetDriverRow[];
};

const COMPLIANCE_TYPES: { value: FleetComplianceType; label: string }[] = [
  { value: 'insurance', label: 'Insurance' },
  { value: 'license', label: 'License' },
  { value: 'maintenance', label: 'Maintenance' },
  { value: 'roadworthiness', label: 'Roadworthiness' },
  { value: 'permit', label: 'Permit' },
  { value: 'other', label: 'Other' },
];

const SUBJECT_TYPES: { value: FleetComplianceSubjectType; label: string }[] = [
  { value: 'asset', label: 'Fleet asset' },
  { value: 'driver', label: 'Driver' },
];

@Component({
  selector: 'app-fleet-compliance-dialog',
  templateUrl: './fleet-compliance-dialog.component.html',
  styleUrl: './fleet-compliance-dialog.component.scss',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatDialogModule, MatIconModule],
})
export class FleetComplianceDialogComponent implements OnDestroy {
  readonly complianceTypes = COMPLIANCE_TYPES;
  readonly subjectTypes = SUBJECT_TYPES;
  readonly isEdit: boolean;
  readonly title: string;
  readonly subtitle: string;
  readonly assets: FleetVehicleRow[];
  readonly drivers: FleetDriverRow[];
  readonly form: FormGroup;
  submitting = false;
  saveError = '';

  private readonly recordId?: number;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<FleetComplianceDialogComponent, FleetComplianceRow | undefined>,
    private readonly fleet: FleetPortalService,
    @Optional() @Inject(MAT_DIALOG_DATA) data: FleetComplianceDialogData | null,
  ) {
    this.dialogRef.disableClose = true;
    const record = data?.record;
    this.isEdit = !!record;
    this.assets = data?.assets ?? [];
    this.drivers = data?.drivers ?? [];
    this.title = record ? 'Edit compliance record' : 'Add compliance record';
    this.subtitle = record
      ? 'Update expiry, status, or document reference for this compliance item.'
      : 'Track insurance, licenses, maintenance, and other compliance documents.';
    this.recordId = record?.id;

    const expiresAt = record?.expiresAt ? record.expiresAt.slice(0, 10) : '';

    this.form = this.fb.group({
      subjectType: [{ value: record?.subjectType ?? 'asset', disabled: this.isEdit }, Validators.required],
      subjectId: [{ value: record?.subjectId ?? null, disabled: this.isEdit }, Validators.required],
      complianceType: [record?.complianceType ?? 'insurance', Validators.required],
      fileUploadId: [record?.fileUploadId ?? null],
      expiresAt: [expiresAt],
      status: [record?.status ?? 'ACTIVE'],
      notes: [record?.notes ?? ''],
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get subjectType(): FleetComplianceSubjectType {
    return this.form.get('subjectType')?.value as FleetComplianceSubjectType;
  }

  get subjectOptions(): { id: number; label: string }[] {
    if (this.subjectType === 'driver') {
      return this.drivers.map((d) => ({ id: d.id, label: d.fullName }));
    }
    return this.assets
      .filter((a) => typeof a.id === 'number')
      .map((a) => ({ id: Number(a.id), label: `${a.registration} · ${a.makeModel}` }));
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.form.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || control.dirty);
  }

  cancel(): void {
    if (!this.submitting) {
      this.dialogRef.close();
    }
  }

  save(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.getRawValue();
    this.submitting = true;
    this.saveError = '';

    if (this.isEdit && this.recordId != null) {
      const payload: EditFleetCompliancePayload = {
        fileUploadId: v.fileUploadId != null && v.fileUploadId !== '' ? Number(v.fileUploadId) : undefined,
        expiresAt: v.expiresAt ? `${v.expiresAt}T23:59:59` : undefined,
        status: String(v.status ?? 'ACTIVE').trim() || undefined,
        notes: String(v.notes ?? '').trim() || undefined,
      };
      this.fleet
        .updateCompliance(this.recordId, payload)
        .pipe(
          finalize(() => (this.submitting = false)),
          takeUntil(this.destroy$),
        )
        .subscribe({
          next: (row) => this.dialogRef.close(row),
          error: (err: Error) => {
            this.saveError = err.message ?? 'Could not save compliance record.';
          },
        });
      return;
    }

    const payload: CreateFleetCompliancePayload = {
      subjectType: v.subjectType as FleetComplianceSubjectType,
      subjectId: Number(v.subjectId),
      complianceType: v.complianceType as FleetComplianceType,
      fileUploadId: v.fileUploadId != null && v.fileUploadId !== '' ? Number(v.fileUploadId) : undefined,
      expiresAt: v.expiresAt ? `${v.expiresAt}T23:59:59` : undefined,
      notes: String(v.notes ?? '').trim() || undefined,
    };

    this.fleet
      .createCompliance(payload)
      .pipe(
        finalize(() => (this.submitting = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (row) => this.dialogRef.close(row),
        error: (err: Error) => {
          this.saveError = err.message ?? 'Could not save compliance record.';
        },
      });
  }
}
