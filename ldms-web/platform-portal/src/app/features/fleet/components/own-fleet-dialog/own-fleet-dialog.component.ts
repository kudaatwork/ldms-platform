import { CommonModule } from '@angular/common';
import { Component, Inject, OnDestroy, OnInit, Optional } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatRadioModule } from '@angular/material/radio';
import { Observable, Subject, finalize, forkJoin, of, switchMap, takeUntil } from 'rxjs';
import {
  CompleteFleetRegistrationPayload,
  CreateFleetDriverPayload,
  CreateFleetVehiclePayload,
  DriverEmploymentType,
  EditFleetVehiclePayload,
  FleetContractScope,
  FleetDriverRow,
  FleetRegistrationDocumentPayload,
  FleetVehicleOwnershipType,
  FleetVehicleRow,
  FleetVehicleStatus,
  FleetVehicleType,
  TransporterPartnerRow,
} from '../../models/fleet.model';
import { FleetPortalService } from '../../services/fleet-portal.service';
import {
  groupFleetRegistrationDocsByCategory,
  resolveFleetRegistrationDocs,
  type FleetRegistrationComplianceCategory,
} from '../../utils/fleet-registration-compliance.config';
import {
  isDateWithinInclusiveBounds,
  resolveTransporterContractDateBounds,
  TransporterContractDateBounds,
} from '../../utils/transporter-contract.util';
import { maximumDateOfBirthInput } from '../../../../core/utils/date-of-birth.util';
import { OrgContextService } from '../../../../core/services/org-context.service';
import { UserListRow, UserProfileBundle, UsersPortalService } from '../../../users/services/users-portal.service';

export type DriverAssignmentSource = 'org_user' | 'fleet_roster' | 'manual';

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

const DRIVER_SOURCES: { value: DriverAssignmentSource; label: string; hint: string }[] = [
  { value: 'org_user', label: 'Organisation user', hint: 'Pick someone from your organisation user roster.' },
  { value: 'fleet_roster', label: 'Fleet driver', hint: 'Pick an existing driver from your fleet roster.' },
  { value: 'manual', label: 'Enter details', hint: 'Capture driver personal and licence details manually.' },
];

const GENDER_OPTIONS = ['MALE', 'FEMALE', 'PREFER_NOT_TO_SAY', 'NON_BINARY'];

/** One document slot state used in step 2. */
export interface DocSlot {
  complianceType: FleetRegistrationDocumentPayload['complianceType'];
  category: FleetRegistrationComplianceCategory;
  label: string;
  hint: string;
  icon: string;
  file: File | null;
  fileName: string;
  expiresAt: string;
  uploading: boolean;
  uploadError: string;
  fileUploadId: number | null;
}

export interface DocSlotGroup {
  category: FleetRegistrationComplianceCategory;
  label: string;
  slots: DocSlot[];
}

@Component({
  selector: 'app-own-fleet-dialog',
  templateUrl: './own-fleet-dialog.component.html',
  styleUrl: './own-fleet-dialog.component.scss',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MatButtonModule, MatDialogModule, MatIconModule, MatRadioModule],
})
export class OwnFleetDialogComponent implements OnInit, OnDestroy {
  readonly vehicleTypes = VEHICLE_TYPES;
  readonly vehicleStatuses = VEHICLE_STATUSES;
  readonly ownershipTypes = OWNERSHIP_TYPES;
  readonly contractScopes = CONTRACT_SCOPES;
  readonly driverSources = DRIVER_SOURCES;
  readonly genderOptions = GENDER_OPTIONS;

  readonly isEdit: boolean;
  readonly isSupplier: boolean;
  readonly isCustomer: boolean;
  readonly canContractFleet: boolean;
  transporterOptions: TransporterPartnerRow[] = [];
  transportersLoading = false;
  transporterSearchQuery = '';
  orgUsers: UserListRow[] = [];
  fleetDrivers: FleetDriverRow[] = [];
  orgUsersLoading = false;
  fleetDriversLoading = false;
  driverSearchQuery = '';
  selectedUserProfile: UserProfileBundle | null = null;
  userProfileLoading = false;
  readonly title: string;
  readonly subtitle: string;

  // ── Wizard state ──────────────────────────────────────────────────────────
  /** Step 1 = vehicle details; step 2 = required papers (create-only). */
  currentStep: 1 | 2 = 1;

  readonly detailsForm: FormGroup;
  submitting = false;
  saveError = '';

  /** Document slots for step 2 — rebuilt from vehicle context when entering papers step. */
  docSlotGroups: DocSlotGroup[] = [];
  private docSlots: DocSlot[] = [];

  /** Today ISO date string — used as `min` attribute on expiry date inputs in step 2. */
  readonly today = new Date().toISOString().slice(0, 10);
  readonly maximumDateOfBirth = maximumDateOfBirthInput();

  /** Tracks the id of the newly-created asset so we can call complete-registration in step 2. */
  private createdAssetId: number | null = null;
  private readonly vehicleId?: number | string;
  private readonly initialDriverName?: string;
  private readonly initialFleetDriverId?: number;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<OwnFleetDialogComponent, FleetVehicleRow | undefined>,
    private readonly fleet: FleetPortalService,
    private readonly usersPortal: UsersPortalService,
    private readonly orgContext: OrgContextService,
    @Optional() @Inject(MAT_DIALOG_DATA) data: OwnFleetDialogData | null,
  ) {
    this.dialogRef.disableClose = true;
    const vehicle = data?.vehicle;
    this.isEdit = !!vehicle;
    this.isSupplier = data?.isSupplier === true;
    this.isCustomer = data?.isCustomer === true;
    this.canContractFleet = data?.canContractFleet === true;
    this.transporterOptions = (data?.transporterOptions ?? []).filter((row) => row.id > 0);
    this.title = vehicle ? 'Edit vehicle' : 'Add vehicle';
    this.subtitle = this.buildSubtitle(vehicle);
    this.vehicleId = vehicle?.id;
    this.initialDriverName =
      vehicle?.driverName && vehicle.driverName !== '—' ? vehicle.driverName.trim() : undefined;
    this.initialFleetDriverId = vehicle?.fleetDriverId;
    const assignDriverInitially = !!this.initialDriverName || !!this.initialFleetDriverId;

    this.detailsForm = this.fb.group({
      ownershipType: [vehicle?.ownershipType ?? 'owned', Validators.required],
      contractedTransporterOrganizationId: [vehicle?.contractedTransporterOrganizationId ?? null],
      contractScope: [(vehicle?.contractScope ?? 'long_term') as FleetContractScope],
      contractStartDate: [vehicle?.contractStartDate ?? ''],
      contractEndDate: [vehicle?.contractEndDate ?? ''],
      jobReference: [''],
      registration: [vehicle?.registration ?? '', [Validators.required, Validators.maxLength(20)]],
      makeModel: [vehicle?.makeModel ?? '', [Validators.required, Validators.maxLength(100)]],
      type: [vehicle?.type ?? 'rig', Validators.required],
      status: [vehicle?.status ?? 'available', Validators.required],
      utilizationPct: [vehicle?.utilizationPct ?? 0, [Validators.min(0), Validators.max(100)]],
      maxSpeedKmh: [vehicle?.maxSpeedKmh ?? null, [Validators.min(20), Validators.max(200)]],
      driverAssignment: this.fb.group({
        enabled: [assignDriverInitially],
        source: ['org_user' as DriverAssignmentSource],
        orgUserId: [null as number | null],
        fleetDriverId: [null as number | null],
        firstName: [''],
        lastName: [''],
        gender: [''],
        phoneNumber: [''],
        dateOfBirth: [''],
        nationalIdNumber: [''],
        nationalIdExpiryDate: [''],
        passportNumber: [''],
        passportExpiryDate: [''],
        licenseNumber: [''],
        licenseClass: [''],
      }),
    });
  }

  get driverAssignmentForm(): FormGroup {
    return this.detailsForm.get('driverAssignment') as FormGroup;
  }

  ngOnInit(): void {
    this.loadTransporterOptions();
    this.loadDriverPicklists();
    this.refreshFleetDriverRoster();
    this.applyOwnershipValidators(this.detailsForm.get('ownershipType')?.value as FleetVehicleOwnershipType);
    this.detailsForm
      .get('ownershipType')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((value: FleetVehicleOwnershipType) => {
        this.applyOwnershipValidators(value);
        this.refreshFleetDriverRoster();
      });
    this.detailsForm
      .get('contractScope')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe(() => this.applyContractScopeValidators());
    this.detailsForm
      .get('contractedTransporterOrganizationId')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe(() => this.onTransporterChanged());
    this.driverAssignmentForm
      .get('enabled')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((enabled: boolean) => {
        if (!enabled) {
          this.clearDriverSelection();
        }
      });
    this.driverAssignmentForm
      .get('source')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe(() => this.onDriverSourceChanged());
    this.driverAssignmentForm
      .get('orgUserId')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((userId: number | null) => this.onOrgUserSelected(userId));
    this.driverAssignmentForm
      .get('fleetDriverId')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((driverId: number | null) => this.onFleetDriverSelected(driverId));
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

  get showLongTermContractDates(): boolean {
    return this.showContractScopePicker && this.detailsForm.get('contractScope')?.value === 'long_term';
  }

  get selectedTransporterPartner(): TransporterPartnerRow | undefined {
    const id = this.selectedTransporterId;
    if (id == null) {
      return undefined;
    }
    return this.transporterOptions.find((row) => row.id === id);
  }

  get transporterContractBounds(): TransporterContractDateBounds | null {
    return resolveTransporterContractDateBounds(this.selectedTransporterPartner);
  }

  get transporterContractRangeHint(): string {
    const bounds = this.transporterContractBounds;
    if (!bounds) {
      return 'Select a contracted transporter to see the allowed contract window.';
    }
    return `Partner contract window: ${bounds.rangeLabel}`;
  }

  get vehicleContractStartMin(): string | null {
    return this.transporterContractBounds?.minDate ?? null;
  }

  get vehicleContractStartMax(): string | null {
    return this.transporterContractBounds?.maxDate ?? null;
  }

  get vehicleContractEndMin(): string {
    const start = String(this.detailsForm.get('contractStartDate')?.value ?? '').trim();
    const partnerMin = this.transporterContractBounds?.minDate ?? '';
    if (start && partnerMin) {
      return start > partnerMin ? start : partnerMin;
    }
    return start || partnerMin || this.today;
  }

  get vehicleContractEndMax(): string | null {
    return this.transporterContractBounds?.maxDate ?? null;
  }

  get assignDriverEnabled(): boolean {
    return this.driverAssignmentForm.get('enabled')?.value === true;
  }

  get driverSource(): DriverAssignmentSource {
    return (this.driverAssignmentForm.get('source')?.value ?? 'org_user') as DriverAssignmentSource;
  }

  get filteredOrgUsers(): UserListRow[] {
    const q = this.driverSearchQuery.trim().toLowerCase();
    const rows = this.orgUsers.filter((row) => row.id > 0);
    if (!q) {
      return rows;
    }
    return rows.filter((row) => {
      const haystack = `${row.name} ${row.username} ${row.email} ${row.phoneNumber} ${row.nationalIdNumber}`.toLowerCase();
      return haystack.includes(q);
    });
  }

  get filteredFleetDrivers(): FleetDriverRow[] {
    const q = this.driverSearchQuery.trim().toLowerCase();
    const rows = this.fleetDrivers.filter((row) => row.id > 0);
    if (!q) {
      return rows;
    }
    return rows.filter((row) => {
      const haystack = [
        row.fullName,
        row.phoneNumber,
        row.licenseNumber,
        row.licenseClass,
        row.employmentLabel,
        row.homeOrganizationName ?? '',
      ]
        .join(' ')
        .toLowerCase();
      return haystack.includes(q);
    });
  }

  driverEmploymentClass(driver: FleetDriverRow): string {
    return driver.employmentType === 'POOL' ? 'own-fleet-driver__badge--pool' : 'own-fleet-driver__badge--employed';
  }

  driverRosterHint(driver: FleetDriverRow): string {
    if (driver.rosterSource === 'transport_partner') {
      return driver.homeOrganizationName ? `Transport partner · ${driver.homeOrganizationName}` : 'Transport partner';
    }
    return driver.employmentLabel;
  }

  get selectedOrgUserId(): number | null {
    const raw = this.driverAssignmentForm.get('orgUserId')?.value;
    if (raw == null || raw === '') {
      return null;
    }
    const parsed = Number(raw);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
  }

  get selectedFleetDriverId(): number | null {
    const raw = this.driverAssignmentForm.get('fleetDriverId')?.value;
    if (raw == null || raw === '') {
      return null;
    }
    const parsed = Number(raw);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
  }

  get showDriverPicker(): boolean {
    return this.assignDriverEnabled && this.driverSource !== 'manual';
  }

  get showManualDriverForm(): boolean {
    return this.assignDriverEnabled && this.driverSource === 'manual';
  }

  get showDriverPersonalDetails(): boolean {
    if (!this.assignDriverEnabled) {
      return false;
    }
    if (this.driverSource === 'manual') {
      return this.hasManualDriverInput();
    }
    return !!this.personalDetailRows.length;
  }

  get personalDetailRows(): { label: string; value: string }[] {
    if (this.driverSource === 'org_user') {
      if (this.selectedUserProfile?.user) {
        return this.buildPersonalDetailRowsFromUser(this.selectedUserProfile.user);
      }
      const user = this.orgUsers.find((row) => row.id === this.selectedOrgUserId);
      if (!user) {
        return [];
      }
      return [
        { label: 'Name', value: this.formatDisplay(user.name) },
        { label: 'Username', value: this.formatDisplay(user.username) },
        { label: 'Email', value: this.formatDisplay(user.email) },
        { label: 'Phone number', value: this.formatDisplay(user.phoneNumber) },
        { label: 'Gender', value: this.formatDisplay(user.gender) },
        { label: 'National ID', value: this.formatDisplay(user.nationalIdNumber) },
        { label: 'Date of birth', value: this.formatDisplay(user.dateOfBirthLabel) },
      ];
    }
    if (this.driverSource === 'fleet_roster') {
      const driver = this.fleetDrivers.find((row) => row.id === this.selectedFleetDriverId);
      if (!driver) {
        return [];
      }
      if (this.selectedUserProfile?.user && driver.userId) {
        const rows = this.buildPersonalDetailRowsFromUser(this.selectedUserProfile.user);
        return [
          ...rows,
          { label: 'License number', value: this.formatDisplay(driver.licenseNumber) },
          { label: 'License class', value: this.formatDisplay(driver.licenseClass) },
        ];
      }
      return [
        { label: 'First name', value: this.formatDisplay(driver.firstName) },
        { label: 'Last name', value: this.formatDisplay(driver.lastName) },
        { label: 'Phone number', value: this.formatDisplay(driver.phoneNumber) },
        { label: 'License number', value: this.formatDisplay(driver.licenseNumber) },
        { label: 'License class', value: this.formatDisplay(driver.licenseClass) },
      ];
    }
    return [];
  }

  selectFleetDriver(driverId: number | null): void {
    this.driverAssignmentForm.get('fleetDriverId')?.setValue(driverId);
    this.driverAssignmentForm.get('fleetDriverId')?.markAsDirty();
    this.driverAssignmentForm.get('fleetDriverId')?.markAsTouched();
  }

  selectOrgUser(userId: number | null): void {
    this.driverAssignmentForm.get('orgUserId')?.setValue(userId);
    this.driverAssignmentForm.get('orgUserId')?.markAsDirty();
    this.driverAssignmentForm.get('orgUserId')?.markAsTouched();
  }

  get filteredTransporterOptions(): TransporterPartnerRow[] {
    const q = this.transporterSearchQuery.trim().toLowerCase();
    const rows = this.transporterOptions.filter((row) => row.id > 0);
    if (!q) {
      return rows;
    }
    return rows.filter((row) => {
      const haystack = `${row.name} ${row.email} ${row.contractRangeLabel ?? ''} ${row.contractStatusLabel ?? ''}`.toLowerCase();
      return haystack.includes(q);
    });
  }

  get selectedTransporterId(): number | null {
    const raw = this.detailsForm.get('contractedTransporterOrganizationId')?.value;
    if (raw == null || raw === '') {
      return null;
    }
    const parsed = Number(raw);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
  }

  get selectedTransporterLabel(): string {
    const id = this.selectedTransporterId;
    if (id == null) {
      return '';
    }
    return this.transporterOptions.find((row) => row.id === id)?.name ?? '';
  }

  selectTransporter(transporterId: number | string | null): void {
    const control = this.detailsForm.get('contractedTransporterOrganizationId');
    if (!control) {
      return;
    }
    if (transporterId == null || transporterId === '') {
      control.setValue(null);
    } else {
      const parsed = typeof transporterId === 'number' ? transporterId : Number(transporterId);
      control.setValue(Number.isFinite(parsed) && parsed > 0 ? parsed : null);
    }
    control.markAsDirty();
    control.markAsTouched();
    control.updateValueAndValidity();
    this.onTransporterChanged();
  }

  get docsComplete(): boolean {
    return this.docSlots.length > 0 && this.docSlots.every((s) => s.fileUploadId != null);
  }

  get requiredDocCount(): number {
    return this.docSlots.length;
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

    const contractDateError = this.validateLongTermContractDates();
    if (contractDateError) {
      this.saveError = contractDateError;
      return;
    }

    const driverError = this.validateDriverAssignment();
    if (driverError) {
      this.saveError = driverError;
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
      contractStartDate:
        this.canContractFleet && ownershipType === 'contracted' && v.contractScope === 'long_term'
          ? String(v.contractStartDate ?? '').trim() || undefined
          : undefined,
      contractEndDate:
        this.canContractFleet && ownershipType === 'contracted' && v.contractScope === 'long_term'
          ? String(v.contractEndDate ?? '').trim() || undefined
          : undefined,
      utilizationPct: Number(v.utilizationPct) || 0,
      maxSpeedKmh: v.maxSpeedKmh != null && v.maxSpeedKmh !== '' ? Number(v.maxSpeedKmh) : undefined,
    };

    this.submitting = true;
    this.saveError = '';

    this.resolveAssignedDriver()
      .pipe(
        switchMap((assignment) => {
          const vehiclePayload = {
            ...payload,
            driverName: assignment.driverName,
            fleetDriverId: assignment.fleetDriverId,
          };
          if (this.isEdit && this.vehicleId != null) {
            return this.fleet.updateFleetVehicle(this.vehicleId, vehiclePayload as EditFleetVehiclePayload);
          }
          return this.fleet.createFleetVehicle(vehiclePayload as CreateFleetVehiclePayload);
        }),
        finalize(() => (this.submitting = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (row: FleetVehicleRow) => {
          if (this.isEdit) {
            this.dialogRef.close(row);
          } else {
            this.createdAssetId = Number(row.id);
            this.initRegistrationDocSlots();
            this.currentStep = 2;
          }
        },
        error: (err: Error) => {
          this.saveError = err.message ?? 'Could not save vehicle.';
        },
      });
  }

  // ── Step 2 — required papers ───────────────────────────────────────────────

  private initRegistrationDocSlots(): void {
    const v = this.detailsForm.getRawValue();
    const ownershipType = (this.canContractFleet ? v.ownershipType : 'owned') as FleetVehicleOwnershipType;
    const definitions = resolveFleetRegistrationDocs({
      ownershipType,
      vehicleType: v.type as FleetVehicleType,
      driverAssigned: this.assignDriverEnabled,
    });
    this.docSlots = definitions.map((definition) => ({
      complianceType: definition.complianceType,
      category: definition.category,
      label: definition.label,
      hint: definition.hint,
      icon: definition.icon,
      file: null,
      fileName: '',
      expiresAt: '',
      uploading: false,
      uploadError: '',
      fileUploadId: null,
    }));
    this.docSlotGroups = groupFleetRegistrationDocsByCategory(definitions).map((group) => ({
      category: group.category,
      label: group.label,
      slots: this.docSlots.filter((slot) => slot.category === group.category),
    }));
  }

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
      .uploadFleetAssetDocument(this.createdAssetId, file, slot.complianceType)
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

    const documents: FleetRegistrationDocumentPayload[] = this.docSlots.map((s) => {
      const entry: FleetRegistrationDocumentPayload = {
        complianceType: s.complianceType,
        fileUploadId: s.fileUploadId!,
      };
      const expiry = s.expiresAt.trim();
      if (expiry) {
        entry.expiresAt = expiry.slice(0, 10);
      }
      return entry;
    });

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

  private loadTransporterOptions(): void {
    if (!this.canContractFleet) {
      return;
    }
    this.transportersLoading = true;
    this.fleet
      .listPartners()
      .pipe(
        finalize(() => (this.transportersLoading = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.transporterOptions = rows.filter((row) => row.id > 0);
          const current = this.selectedTransporterId;
          if (current != null && !this.transporterOptions.some((row) => row.id === current)) {
            this.selectTransporter(null);
          } else {
            this.onTransporterChanged();
          }
        },
        error: () => {
          if (!this.transporterOptions.length) {
            this.transporterOptions = [];
          }
        },
      });
  }

  private buildSubtitle(vehicle?: FleetVehicleRow): string {
    if (vehicle) {
      return `Update registration details for ${vehicle.registration}.`;
    }
    return this.isCustomer
      ? 'Register a vehicle — attach statutory, operating, and driver compliance papers in step 2.'
      : this.isSupplier
        ? 'Register an owned or contracted vehicle — then attach all required compliance papers.'
        : 'Register a vehicle — attach every compliance document required before the asset goes active.';
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
    const startControl = this.detailsForm.get('contractStartDate');
    const endControl = this.detailsForm.get('contractEndDate');
    if (!jobRefControl || !startControl || !endControl) {
      return;
    }
    if (this.canContractFleet && ownershipType === 'contracted' && contractScope === 'job') {
      jobRefControl.setValidators([Validators.required, Validators.maxLength(100)]);
      startControl.clearValidators();
      startControl.setValue('');
      endControl.clearValidators();
      endControl.setValue('');
    } else if (this.canContractFleet && ownershipType === 'contracted' && contractScope === 'long_term') {
      jobRefControl.clearValidators();
      jobRefControl.setValue('');
      startControl.setValidators([Validators.required]);
      const bounds = this.transporterContractBounds;
      endControl.setValidators(bounds?.endRequired ? [Validators.required] : []);
    } else {
      jobRefControl.clearValidators();
      jobRefControl.setValue('');
      startControl.clearValidators();
      startControl.setValue('');
      endControl.clearValidators();
      endControl.setValue('');
    }
    jobRefControl.updateValueAndValidity();
    startControl.updateValueAndValidity();
    endControl.updateValueAndValidity();
  }

  private onTransporterChanged(): void {
    this.applyContractScopeValidators();
    this.applyDefaultContractDatesFromTransporter();
    this.clampContractDatesToTransporterBounds();
    this.refreshFleetDriverRoster();
  }

  private applyDefaultContractDatesFromTransporter(): void {
    if (!this.showLongTermContractDates) {
      return;
    }
    const bounds = this.transporterContractBounds;
    if (!bounds) {
      return;
    }
    const startControl = this.detailsForm.get('contractStartDate');
    const endControl = this.detailsForm.get('contractEndDate');
    if (!startControl || !endControl) {
      return;
    }
    if (!String(startControl.value ?? '').trim()) {
      startControl.setValue(bounds.minDate);
    }
    if (bounds.endRequired && !String(endControl.value ?? '').trim()) {
      endControl.setValue(bounds.maxDate ?? '');
    }
    startControl.updateValueAndValidity();
    endControl.updateValueAndValidity();
  }

  private clampContractDatesToTransporterBounds(): void {
    const bounds = this.transporterContractBounds;
    if (!bounds) {
      return;
    }
    const startControl = this.detailsForm.get('contractStartDate');
    const endControl = this.detailsForm.get('contractEndDate');
    if (!startControl || !endControl) {
      return;
    }
    const start = String(startControl.value ?? '').trim();
    const end = String(endControl.value ?? '').trim();
    if (start && !isDateWithinInclusiveBounds(start, bounds.minDate, bounds.maxDate)) {
      startControl.setValue(bounds.minDate);
    }
    if (end && !isDateWithinInclusiveBounds(end, bounds.minDate, bounds.maxDate)) {
      endControl.setValue(bounds.maxDate ?? bounds.minDate);
    }
    const adjustedStart = String(startControl.value ?? '').trim();
    const adjustedEnd = String(endControl.value ?? '').trim();
    if (adjustedStart && adjustedEnd && adjustedEnd < adjustedStart) {
      endControl.setValue(bounds.maxDate && bounds.maxDate >= adjustedStart ? bounds.maxDate : adjustedStart);
    }
  }

  private validateLongTermContractDates(): string | null {
    if (!this.showLongTermContractDates) {
      return null;
    }
    const start = String(this.detailsForm.get('contractStartDate')?.value ?? '').trim();
    const end = String(this.detailsForm.get('contractEndDate')?.value ?? '').trim();
    const bounds = this.transporterContractBounds;
    if (!bounds) {
      return 'Select a contracted transporter before setting contract dates.';
    }
    if (!start) {
      return 'Contract start date is required for long-term contracted vehicles.';
    }
    if (!isDateWithinInclusiveBounds(start, bounds.minDate, bounds.maxDate)) {
      return `Start date must be within the partner contract (${bounds.rangeLabel}).`;
    }
    if (bounds.endRequired && !end) {
      return 'Contract end date is required because this transporter has a fixed contract end date.';
    }
    if (end && end < start) {
      return 'Contract end date cannot be before the start date.';
    }
    if (end && !isDateWithinInclusiveBounds(end, bounds.minDate, bounds.maxDate)) {
      return `End date must be within the partner contract (${bounds.rangeLabel}).`;
    }
    return null;
  }

  private loadDriverPicklists(): void {
    const orgId = this.orgContext.organizationId;
    if (orgId != null) {
      this.orgUsersLoading = true;
      this.usersPortal
        .queryUsersForOrganization(orgId)
        .pipe(
          finalize(() => (this.orgUsersLoading = false)),
          takeUntil(this.destroy$),
        )
        .subscribe({
          next: ({ rows }) => {
            this.orgUsers = rows.filter((row) => row.id > 0);
            this.prefillDriverAssignmentFromExisting();
          },
          error: () => {
            this.orgUsers = [];
          },
        });
    }
  }

  private refreshFleetDriverRoster(): void {
    const ownershipType = this.detailsForm.get('ownershipType')?.value as FleetVehicleOwnershipType;
    const transporterId = this.selectedTransporterId;
    const orgDrivers$ = this.fleet.listDrivers();
    const partnerDrivers$ =
      this.canContractFleet && ownershipType === 'contracted' && transporterId != null
        ? this.fleet.listTransporterPartnerDrivers(transporterId)
        : of([] as FleetDriverRow[]);

    this.fleetDriversLoading = true;
    forkJoin({ orgDrivers: orgDrivers$, partnerDrivers: partnerDrivers$ })
      .pipe(
        finalize(() => (this.fleetDriversLoading = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: ({ orgDrivers, partnerDrivers }) => {
          const merged = [...orgDrivers, ...partnerDrivers].filter((row) => row.id > 0);
          const seen = new Set<number>();
          this.fleetDrivers = merged.filter((row) => {
            if (seen.has(row.id)) {
              return false;
            }
            seen.add(row.id);
            return true;
          });
          this.prefillDriverAssignmentFromExisting();
        },
        error: () => {
          this.fleetDrivers = [];
        },
      });
  }

  private prefillDriverAssignmentFromExisting(): void {
    if (!this.assignDriverEnabled) {
      return;
    }
    if (this.initialFleetDriverId != null && this.initialFleetDriverId > 0) {
      const rosterMatch = this.fleetDrivers.find((row) => row.id === this.initialFleetDriverId);
      if (rosterMatch) {
        this.driverAssignmentForm.patchValue({
          source: 'fleet_roster',
          fleetDriverId: rosterMatch.id,
        });
        return;
      }
    }
    if (!this.initialDriverName) {
      return;
    }
    const normalized = this.initialDriverName.toLowerCase();
    const rosterMatch = this.fleetDrivers.find((row) => row.fullName.toLowerCase() === normalized);
    if (rosterMatch) {
      this.driverAssignmentForm.patchValue({
        source: 'fleet_roster',
        fleetDriverId: rosterMatch.id,
      });
      return;
    }
    const userMatch = this.orgUsers.find((row) => row.name.toLowerCase() === normalized);
    if (userMatch) {
      this.driverAssignmentForm.patchValue({
        source: 'org_user',
        orgUserId: userMatch.id,
      });
    }
  }

  private onDriverSourceChanged(): void {
    this.driverSearchQuery = '';
    this.selectedUserProfile = null;
    this.driverAssignmentForm.patchValue({
      orgUserId: null,
      fleetDriverId: null,
      firstName: '',
      lastName: '',
      gender: '',
      phoneNumber: '',
      dateOfBirth: '',
      nationalIdNumber: '',
      nationalIdExpiryDate: '',
      passportNumber: '',
      passportExpiryDate: '',
      licenseNumber: '',
      licenseClass: '',
    });
  }

  private clearDriverSelection(): void {
    this.driverSearchQuery = '';
    this.selectedUserProfile = null;
    this.onDriverSourceChanged();
  }

  private onOrgUserSelected(userId: number | null): void {
    if (userId == null || userId <= 0) {
      this.selectedUserProfile = null;
      return;
    }
    this.userProfileLoading = true;
    this.usersPortal
      .getUserProfileBundle(userId)
      .pipe(
        finalize(() => (this.userProfileLoading = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (bundle) => {
          this.selectedUserProfile = bundle;
        },
        error: () => {
          this.selectedUserProfile = null;
        },
      });
  }

  private onFleetDriverSelected(driverId: number | null): void {
    if (driverId == null || driverId <= 0) {
      this.selectedUserProfile = null;
      return;
    }
    const driver = this.fleetDrivers.find((row) => row.id === driverId);
    if (!driver?.userId) {
      this.selectedUserProfile = null;
      return;
    }
    this.onOrgUserSelected(driver.userId);
  }

  private buildPersonalDetailRowsFromUser(user: Record<string, unknown>): { label: string; value: string }[] {
    return [
      { label: 'Username', value: this.formatDisplay(user['username']) },
      { label: 'First name', value: this.formatDisplay(user['firstName']) },
      { label: 'Last name', value: this.formatDisplay(user['lastName']) },
      { label: 'Email', value: this.formatDisplay(user['email']) },
      { label: 'Phone number', value: this.formatDisplay(user['phoneNumber']) },
      { label: 'Gender', value: this.formatDisplay(user['gender']) },
      { label: 'Date of birth', value: this.formatDisplay(user['dateOfBirth']) },
      { label: 'National ID', value: this.formatDisplay(user['nationalIdNumber']) },
      { label: 'National ID expiry', value: this.formatDisplay(user['nationalIdExpiryDate']) },
      { label: 'Passport number', value: this.formatDisplay(user['passportNumber']) },
      { label: 'Passport expiry', value: this.formatDisplay(user['passportExpiryDate']) },
    ];
  }

  private formatDisplay(value: unknown): string {
    const text = String(value ?? '').trim();
    return text || '—';
  }

  private hasManualDriverInput(): boolean {
    const v = this.driverAssignmentForm.getRawValue();
    return [
      v.firstName,
      v.lastName,
      v.gender,
      v.phoneNumber,
      v.dateOfBirth,
      v.nationalIdNumber,
      v.nationalIdExpiryDate,
      v.passportNumber,
      v.passportExpiryDate,
      v.licenseNumber,
      v.licenseClass,
    ].some((field) => String(field ?? '').trim().length > 0);
  }

  private validateDriverAssignment(): string | null {
    if (!this.assignDriverEnabled) {
      return null;
    }
    if (this.driverSource === 'org_user') {
      if (this.selectedOrgUserId == null) {
        return 'Select an organisation user to assign as the driver.';
      }
      return null;
    }
    if (this.driverSource === 'fleet_roster') {
      if (this.selectedFleetDriverId == null) {
        return 'Select a fleet driver to assign to this vehicle.';
      }
      return null;
    }
    if (!this.hasManualDriverInput()) {
      return null;
    }
    const v = this.driverAssignmentForm.getRawValue();
    if (!String(v.firstName ?? '').trim() || !String(v.lastName ?? '').trim()) {
      return 'First and last name are required when entering driver details.';
    }
    if (!String(v.licenseNumber ?? '').trim()) {
      return 'License number is required when registering a new driver on the fleet roster.';
    }
    return null;
  }

  private resolveAssignedDriver(): Observable<{ driverName?: string; fleetDriverId?: number }> {
    if (!this.assignDriverEnabled) {
      return of({});
    }
    if (this.driverSource === 'org_user') {
      const user = this.orgUsers.find((row) => row.id === this.selectedOrgUserId);
      return of({ driverName: user?.name?.trim() || undefined });
    }
    if (this.driverSource === 'fleet_roster') {
      const driver = this.fleetDrivers.find((row) => row.id === this.selectedFleetDriverId);
      return of({
        driverName: driver?.fullName?.trim() || undefined,
        fleetDriverId: driver?.id,
      });
    }
    if (!this.hasManualDriverInput()) {
      return of({});
    }
    const v = this.driverAssignmentForm.getRawValue();
    const payload: CreateFleetDriverPayload = {
      firstName: String(v.firstName).trim(),
      lastName: String(v.lastName).trim(),
      employmentType: 'POOL' as DriverEmploymentType,
      phoneNumber: String(v.phoneNumber ?? '').trim() || undefined,
      licenseNumber: String(v.licenseNumber ?? '').trim(),
      licenseClass: String(v.licenseClass ?? '').trim() || undefined,
    };
    return this.fleet.createDriver(payload).pipe(
      switchMap((driver) =>
        of({
          driverName: driver.fullName.trim() || `${payload.firstName} ${payload.lastName}`.trim(),
          fleetDriverId: driver.id,
        }),
      ),
    );
  }
}
