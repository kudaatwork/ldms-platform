import { CommonModule } from '@angular/common';
import { Component, Inject, OnDestroy, Optional } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { Subject, finalize, takeUntil } from 'rxjs';
import type { CreateFleetDriverPayload, EditFleetDriverPayload, FleetDriverRow } from '../../models/fleet.model';
import { FleetPortalService } from '../../services/fleet-portal.service';

export type FleetDriverDialogData = {
  driver?: FleetDriverRow;
};

@Component({
  selector: 'app-fleet-driver-dialog',
  templateUrl: './fleet-driver-dialog.component.html',
  styleUrl: './fleet-driver-dialog.component.scss',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatDialogModule, MatIconModule],
})
export class FleetDriverDialogComponent implements OnDestroy {
  readonly isEdit: boolean;
  readonly title: string;
  readonly subtitle: string;
  readonly form: FormGroup;
  submitting = false;
  saveError = '';

  private readonly driverId?: number;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<FleetDriverDialogComponent, FleetDriverRow | undefined>,
    private readonly fleet: FleetPortalService,
    @Optional() @Inject(MAT_DIALOG_DATA) data: FleetDriverDialogData | null,
  ) {
    this.dialogRef.disableClose = true;
    const driver = data?.driver;
    this.isEdit = !!driver;
    this.title = driver ? 'Edit driver' : 'Add driver';
    this.subtitle = driver
      ? `Update profile details for ${driver.fullName}.`
      : 'Register a driver in your organisation fleet roster.';
    this.driverId = driver?.id;

    this.form = this.fb.group({
      firstName: [driver?.firstName ?? '', [Validators.required, Validators.maxLength(80)]],
      lastName: [driver?.lastName ?? '', [Validators.required, Validators.maxLength(80)]],
      phoneNumber: [driver?.phoneNumber && driver.phoneNumber !== '—' ? driver.phoneNumber : ''],
      licenseNumber: [driver?.licenseNumber && driver.licenseNumber !== '—' ? driver.licenseNumber : ''],
      licenseClass: [driver?.licenseClass && driver.licenseClass !== '—' ? driver.licenseClass : ''],
      userId: [driver?.userId ?? null],
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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
    const payload: CreateFleetDriverPayload | EditFleetDriverPayload = {
      firstName: String(v.firstName).trim(),
      lastName: String(v.lastName).trim(),
      phoneNumber: String(v.phoneNumber ?? '').trim() || undefined,
      licenseNumber: String(v.licenseNumber ?? '').trim() || undefined,
      licenseClass: String(v.licenseClass ?? '').trim() || undefined,
      userId: v.userId != null && v.userId !== '' ? Number(v.userId) : undefined,
    };

    this.submitting = true;
    this.saveError = '';

    const request$ =
      this.isEdit && this.driverId != null
        ? this.fleet.updateDriver(this.driverId, payload)
        : this.fleet.createDriver(payload);

    request$
      .pipe(
        finalize(() => (this.submitting = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (row) => this.dialogRef.close(row),
        error: (err: Error) => {
          this.saveError = err.message ?? 'Could not save driver.';
        },
      });
  }
}
