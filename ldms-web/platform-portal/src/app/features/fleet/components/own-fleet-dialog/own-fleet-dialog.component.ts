import { CommonModule } from '@angular/common';
import { Component, Inject, OnDestroy, OnInit, Optional } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { Subject, finalize, takeUntil } from 'rxjs';
import {
  CompleteFleetRegistrationPayload,
  CreateFleetVehiclePayload,
  EditFleetVehiclePayload,
  FleetContractScope,
  FleetRegistrationDocumentPayload,
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
  /** Supplier or customer workspace: controls ownership picker and transporter picker. */
  isSupplier?: boolean;
  /** True when the signed-in org is a CUSTOMER. */
  isCustomer?: boolean;
  /** True when the org can link a contracted transporter (supplier OR customer). */
  canContractFleet?: boolean;
  transporterOptions?: TransporterPartnerRow[];
};

/** Required compliance documents for registration step 2. */
const REQUIRED_DOC_TYPES: { complianceType: FleetRegistrationDocumentPayload['complianceType']; label: string; icon: string }[] = [
  { complianceType: 'INSURANCE', label: 'Insurance certificate', icon: 'policy' },
  { complianceType: 'ROADWORTHINESS', label: 'Roadworthiness certificate', icon: 'verified' },
  { complianceType: 'PERMIT', label: 'Operating permit', icon: 'badge' },
];

const CONTRACT_SCOPES: { value: FleetContractScope; label: string; hint: string }[] = [
  { value: 'long_term', label: 'Long-term contract', hint: 'Ongoing engagement with a contracted transporter.' },
  { value: 'job', label: 'Job-specific', hint: 'Single job or corridor assignment with a job reference.' },
];

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

/** One document slot state used in step 2. */
export interface DocSlot {
  complianceType: FleetRegistrationDocumentPayload['complianceType'];
  label: string;
  icon: string;
  file: File | null;
  fileName: string;
  expiresAt: string;
  uploading: boolean;
  uploadError: string;
  fileUploadId: number | null;
}

@Component({
  selector: 'app-own-fleet-dialog',
  templateUrl: './own-fleet-dialog.component.html',
  styleUrl: './own-fleet-dialog.component.scss',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MatButtonModule, MatDialogModule, MatIconModule],
})
export class OwnFleetDialogComponent implements OnInit, OnDestroy {
  readonly vehicleTypes = VEHICLE_TYPES;
  readonly vehicleStatuses = VEHICLE_STATUSES;
  readonly ownershipTypes = OWNERSHIP_TYPES;
  readonly contractScopes = CONTRACT_SCOPES;
  readonly requiredDocTypes = REQUIRED_DOC_TYPES;

  readonly isEdit: boolean;
  readonly isSupplier: boolean;
  readonly isCustomer: boolean;
  readonly canContractFleet: boolean;
  readonly transporterOptions: TransporterPartnerRow[];
  readonly title: string;
  readonly subtitle: string;

  // ── Wizard state ──────────────────────────────────────────────────────────
  /** Step 1 = vehicle details; step 2 = required papers (create-only). */
  currentStep: 1 | 2 = 1;

  readonly detailsForm: FormGroup;
  submitting = false;
  saveError = '';

  /** Document slots for step 2 — one per required compliance type. */
  readonly docSlots: DocSlot[] = REQUIRED_DOC_TYPES.map((d) => ({
    complianceType: d.complianceType,
    label: d.label,
    icon: d.icon,
    file: null,
    fileName: '',
    expiresAt: '',
    uploading: false,
    uploadError: '',
    fileUploadId: null,
  }));

  /** Today ISO date string — used as `min` attribute on expiry date inputs in step 2. */
  readonly today = new Date().toISOString().slice(0, 10);

  /** Tracks the id of the newly-created asset so we can call complete-registration in step 2. */
  private createdAssetId: number | null = null;
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
    this.isCustomer = data?.isCustomer === true;
    this.canContractFleet = data?.canContractFleet === true;
    this.transporterOptions = data?.transporterOptions ?? [];
    this.title = vehicle ? 'Edit vehicle' : 'Add vehicle';
    this.subtitle = this.buildSubtitle(vehicle);
    this.vehicleId = vehicle?.id;

    this.detailsForm = this.fb.group({
      ownershipType: [vehicle?.ownershipType ?? 'owned', Validators.required],
      contractedTransporterOrganizationId: [vehicle?.contractedTransporterOrganizationId ?? null],
      contractScope: ['long_term' as FleetContractScope],
      jobReference: [''],
      registration: [vehicle?.registration ?? '', [Validators.required, Validators.maxLength(20)]],
      makeModel: [vehicle?.makeModel ?? '', [Validators.required, Validators.maxLength(100)]],
      type: [vehicle?.type ?? 'rig', Validators.required],
      status: [vehicle?.status ?? 'available', Validators.required],
      driverName: [vehicle?.driverName && vehicle.driverName !== '—' ? vehicle.driverName : ''],
      utilizationPct: [vehicle?.utilizationPct ?? 0, [Validators.min(0), Validators.max(100)]],
    });
  }

  ngOnInit(): void {
    this.applyOwnershipValidators(this.detailsForm.get('ownershipType')?.value as FleetVehicleOwnershipType);
    this.detailsForm
      .get('ownershipType')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((value: FleetVehicleOwnershipType) => this.applyOwnershipValidators(value));
    this.detailsForm
      .get('contractScope')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe(() => this.applyContractScopeValidators());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── Getters ────────────────────────────────────────────────────────────────

  get showOwnershipPicker(): boolean {
    return this.canContractFleet;
  }

  get showTransporterPicker(): boolean {
    return this.canContractFleet && this.detailsForm.get('ownershipType')?.value === 'contracted';
  }

  get showContractScopePicker(): boolean {
    return this.showTransporterPicker;
  }

  get showJobReference(): boolean {
    return this.showContractScopePicker && this.detailsForm.get('contractScope')?.value === 'job';
  }

  get docsComplete(): boolean {
    return this.docSlots.every((s) => s.fileUploadId != null && s.expiresAt.trim().length > 0);
  }

  get stepTwoSubmitting(): boolean {
    return this.docSlots.some((s) => s.uploading) || this.submitting;
  }

  get supplierOrCustomerLabel(): string {
    if (this.isCustomer) {
      return 'Register a vehicle in your fleet for deliveries and last-mile transport.';
    }
    if (this.isSupplier) {
      return 'Register an owned asset or tag a vehicle under a contracted transporter.';
    }
    return 'Register a vehicle in your own rolling stock.';
  }

  // ── Template helpers ───────────────────────────────────────────────────────

  hasError(formGroup: FormGroup, controlName: string, errorName: string): boolean {
    const control = formGroup.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || control.dirty);
  }

  cancel(): void {
    if (!this.submitting && !this.stepTwoSubmitting) {
      this.dialogRef.close();
    }
  }

  // ── Step 1 — vehicle details ───────────────────────────────────────────────

  /**
   * In create mode: POSTs the asset, moves to step 2.
   * In edit mode: PUTs the asset and closes (no papers step).
   */
  saveDetails(): void {
    if (this.detailsForm.invalid || this.submitting) {
      this.detailsForm.markAllAsTouched();
      return;
    }

    const v = this.detailsForm.getRawValue();
    const ownershipType = v.ownershipType as FleetVehicleOwnershipType;
    const payload: CreateFleetVehiclePayload | EditFleetVehiclePayload = {
      registration: String(v.registration).trim().toUpperCase(),
      makeModel: String(v.makeModel).trim(),
      type: v.type as FleetVehicleType,
      status: v.status as FleetVehicleStatus,
      ownershipType: this.canContractFleet ? ownershipType : 'owned',
      contractedTransporterOrganizationId:
        this.canContractFleet && ownershipType === 'contracted' ? Number(v.contractedTransporterOrganizationId) : undefined,
      contractScope:
        this.canContractFleet && ownershipType === 'contracted' ? (v.contractScope as FleetContractScope) : undefined,
      jobReference:
        this.canContractFleet && ownershipType === 'contracted' && v.contractScope === 'job'
          ? String(v.jobReference ?? '').trim() || undefined
          : undefined,
      driverName: String(v.driverName ?? '').trim() || undefined,
      utilizationPct: Number(v.utilizationPct) || 0,
    };

    this.submitting = true;
    this.saveError = '';

    if (this.isEdit && this.vehicleId != null) {
      this.fleet
        .updateFleetVehicle(this.vehicleId, payload as EditFleetVehiclePayload)
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
    } else {
      this.fleet
        .createFleetVehicle(payload as CreateFleetVehiclePayload)
        .pipe(
          finalize(() => (this.submitting = false)),
          takeUntil(this.destroy$),
        )
        .subscribe({
          next: (row: FleetVehicleRow) => {
            this.createdAssetId = Number(row.id);
            this.currentStep = 2;
          },
          error: (err: Error) => {
            this.saveError = err.message ?? 'Could not create vehicle.';
          },
        });
    }
  }

  // ── Step 2 — required papers ───────────────────────────────────────────────

  onFileSelected(slot: DocSlot, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    if (!file) {
      return;
    }
    slot.file = file;
    slot.fileName = file.name;
    slot.fileUploadId = null;
    slot.uploadError = '';
    input.value = '';

    if (!this.createdAssetId) {
      return;
    }

    slot.uploading = true;
    this.fleet
      .uploadFleetAssetDocument(this.createdAssetId, file)
      .pipe(
        finalize(() => (slot.uploading = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (id: number) => {
          slot.fileUploadId = id;
        },
        error: (err: Error) => {
          slot.uploadError = err.message ?? 'Upload failed.';
          slot.file = null;
          slot.fileName = '';
        },
      });
  }

  clearFile(slot: DocSlot): void {
    slot.file = null;
    slot.fileName = '';
    slot.fileUploadId = null;
    slot.uploadError = '';
  }

  goToStep(step: 1 | 2): void {
    if (step === 1 && !this.stepTwoSubmitting) {
      this.currentStep = 1;
      this.saveError = '';
    }
  }

  /**
   * Step 2 submit: POSTs complete-registration with all three document ids, then closes.
   */
  completeRegistration(): void {
    if (!this.docsComplete || !this.createdAssetId || this.submitting) {
      return;
    }

    const documents: FleetRegistrationDocumentPayload[] = this.docSlots.map((s) => ({
      complianceType: s.complianceType,
      fileUploadId: s.fileUploadId!,
      expiresAt: s.expiresAt,
    }));

    const completionPayload: CompleteFleetRegistrationPayload = { documents };

    this.submitting = true;
    this.saveError = '';

    this.fleet
      .completeFleetRegistration(this.createdAssetId, completionPayload)
      .pipe(
        finalize(() => (this.submitting = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (row: FleetVehicleRow) => this.dialogRef.close(row),
        error: (err: Error) => {
          this.saveError = err.message ?? 'Could not complete registration.';
        },
      });
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  private buildSubtitle(vehicle?: FleetVehicleRow): string {
    if (vehicle) {
      return `Update registration details for ${vehicle.registration}.`;
    }
    return this.isCustomer
      ? 'Register a vehicle — attach insurance, roadworthiness, and permit in step 2.'
      : this.isSupplier
        ? 'Register an owned asset or tag a contracted vehicle — then attach required papers.'
        : 'Register a vehicle — attach required compliance papers before completing registration.';
  }

  private applyOwnershipValidators(ownershipType: FleetVehicleOwnershipType): void {
    const transporterControl = this.detailsForm.get('contractedTransporterOrganizationId');
    if (!transporterControl) {
      return;
    }
    if (this.canContractFleet && ownershipType === 'contracted') {
      transporterControl.setValidators([Validators.required]);
    } else {
      transporterControl.clearValidators();
      transporterControl.setValue(null);
    }
    transporterControl.updateValueAndValidity();
    this.applyContractScopeValidators();
  }

  private applyContractScopeValidators(): void {
    const ownershipType = this.detailsForm.get('ownershipType')?.value as FleetVehicleOwnershipType;
    const contractScope = this.detailsForm.get('contractScope')?.value as FleetContractScope;
    const jobRefControl = this.detailsForm.get('jobReference');
    if (!jobRefControl) {
      return;
    }
    if (this.canContractFleet && ownershipType === 'contracted' && contractScope === 'job') {
      jobRefControl.setValidators([Validators.required, Validators.maxLength(100)]);
    } else {
      jobRefControl.clearValidators();
      jobRefControl.setValue('');
    }
    jobRefControl.updateValueAndValidity();
  }
}
