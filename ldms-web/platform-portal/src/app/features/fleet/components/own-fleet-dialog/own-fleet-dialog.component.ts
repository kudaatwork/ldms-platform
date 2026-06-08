import { CommonModule } from '@angular/common';
import { Component, Inject, OnDestroy, OnInit, Optional } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { Subject, finalize, takeUntil } from 'rxjs';
import {
  CreateFleetVehiclePayload,
  EditFleetVehiclePayload,
  FleetVehicleOwnershipType,
  FleetVehicleRow,
  FleetVehicleStatus,
  FleetVehicleType,
  TransporterPartnerRow,
} from '../../models/fleet.model';
import { FleetPortalService } from '../../services/fleet-portal.service';

export type OwnFleetDialogData = {
  /** When set the dialog operates in edit mode and pre-fills the form. */
  vehicle?: FleetVehicleRow;
  /** Supplier workspace: contracted vehicles must link to a contracted transporter. */
  isSupplier?: boolean;
  transporterOptions?: TransporterPartnerRow[];
};

const VEHICLE_TYPES: { value: FleetVehicleType; label: string }[] = [
  { value: 'rig', label: 'Rigid truck (rig)' },
  { value: 'van', label: 'Van / light commercial' },
  { value: 'tanker', label: 'Tanker' },
  { value: 'flatbed', label: 'Flatbed / lowbed' },
];

const VEHICLE_STATUSES: { value: FleetVehicleStatus; label: string }[] = [
  { value: 'available', label: 'Ready to dispatch' },
  { value: 'on_road', label: 'On corridor' },
  { value: 'yard', label: 'At yard' },
  { value: 'maintenance', label: 'In workshop' },
];

const OWNERSHIP_TYPES: { value: FleetVehicleOwnershipType; label: string; hint: string }[] = [
  {
    value: 'owned',
    label: 'Owned asset',
    hint: 'Vehicle owned and operated directly by your organisation.',
  },
  {
    value: 'contracted',
    label: 'Contracted asset',
    hint: 'Vehicle operated under a contracted transporter link.',
  },
];

@Component({
  selector: 'app-own-fleet-dialog',
  templateUrl: './own-fleet-dialog.component.html',
  styleUrl: './own-fleet-dialog.component.scss',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatDialogModule, MatIconModule],
})
export class OwnFleetDialogComponent implements OnInit, OnDestroy {
  readonly vehicleTypes = VEHICLE_TYPES;
  readonly vehicleStatuses = VEHICLE_STATUSES;
  readonly ownershipTypes = OWNERSHIP_TYPES;

  readonly isEdit: boolean;
  readonly isSupplier: boolean;
  readonly transporterOptions: TransporterPartnerRow[];
  readonly title: string;
  readonly subtitle: string;

  readonly form: FormGroup;
  submitting = false;
  saveError = '';

  private readonly vehicleId?: number | string;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<OwnFleetDialogComponent, FleetVehicleRow | undefined>,
    private readonly fleet: FleetPortalService,
    @Optional() @Inject(MAT_DIALOG_DATA) data: OwnFleetDialogData | null,
  ) {
    this.dialogRef.disableClose = true;
    const vehicle = data?.vehicle;
    this.isEdit = !!vehicle;
    this.isSupplier = data?.isSupplier === true;
    this.transporterOptions = data?.transporterOptions ?? [];
    this.title = vehicle ? 'Edit vehicle' : 'Add vehicle';
    this.subtitle = vehicle
      ? `Update registration details for ${vehicle.registration}.`
      : this.isSupplier
        ? 'Register an owned asset or tag a vehicle under a contracted transporter.'
        : 'Register a vehicle in your own rolling stock.';

    this.vehicleId = vehicle?.id;

    this.form = this.fb.group({
      ownershipType: [vehicle?.ownershipType ?? 'owned', Validators.required],
      contractedTransporterOrganizationId: [vehicle?.contractedTransporterOrganizationId ?? null],
      registration: [vehicle?.registration ?? '', [Validators.required, Validators.maxLength(20)]],
      makeModel: [vehicle?.makeModel ?? '', [Validators.required, Validators.maxLength(100)]],
      type: [vehicle?.type ?? 'rig', Validators.required],
      status: [vehicle?.status ?? 'available', Validators.required],
      driverName: [vehicle?.driverName && vehicle.driverName !== '—' ? vehicle.driverName : ''],
      utilizationPct: [vehicle?.utilizationPct ?? 0, [Validators.min(0), Validators.max(100)]],
    });
  }

  ngOnInit(): void {
    this.applyOwnershipValidators(this.form.get('ownershipType')?.value as FleetVehicleOwnershipType);
    this.form
      .get('ownershipType')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((value: FleetVehicleOwnershipType) => this.applyOwnershipValidators(value));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get showTransporterPicker(): boolean {
    return this.isSupplier && this.form.get('ownershipType')?.value === 'contracted';
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
    const ownershipType = v.ownershipType as FleetVehicleOwnershipType;
    const payload: CreateFleetVehiclePayload | EditFleetVehiclePayload = {
      registration: String(v.registration).trim().toUpperCase(),
      makeModel: String(v.makeModel).trim(),
      type: v.type as FleetVehicleType,
      status: v.status as FleetVehicleStatus,
      ownershipType: this.isSupplier ? ownershipType : 'owned',
      contractedTransporterOrganizationId:
        this.isSupplier && ownershipType === 'contracted' ? Number(v.contractedTransporterOrganizationId) : undefined,
      driverName: String(v.driverName ?? '').trim() || undefined,
      utilizationPct: Number(v.utilizationPct) || 0,
    };

    this.submitting = true;
    this.saveError = '';

    const request$ =
      this.isEdit && this.vehicleId != null
        ? this.fleet.updateFleetVehicle(this.vehicleId, payload as EditFleetVehiclePayload)
        : this.fleet.createFleetVehicle(payload as CreateFleetVehiclePayload);

    request$
      .pipe(
        finalize(() => (this.submitting = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (row: FleetVehicleRow) => this.dialogRef.close(row),
        error: (err: Error) => {
          this.saveError = err.message ?? 'Could not save vehicle.';
        },
      });
  }

  private applyOwnershipValidators(ownershipType: FleetVehicleOwnershipType): void {
    const transporterControl = this.form.get('contractedTransporterOrganizationId');
    if (!transporterControl) {
      return;
    }
    if (this.isSupplier && ownershipType === 'contracted') {
      transporterControl.setValidators([Validators.required]);
    } else {
      transporterControl.clearValidators();
      transporterControl.setValue(null);
    }
    transporterControl.updateValueAndValidity();
  }
}
