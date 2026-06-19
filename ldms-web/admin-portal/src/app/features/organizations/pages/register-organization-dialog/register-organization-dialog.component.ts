import { CommonModule } from '@angular/common';
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, finalize, takeUntil } from 'rxjs';
import { OrganizationsAdminService } from '../../services/organizations-admin.service';
import {
  dateOfBirthMinimumAgeMessage,
  isDateOfBirthAtLeastMinimumAge,
  maximumDateOfBirthInput,
} from '../../../../core/utils/date-of-birth.util';
import {
  ORG_CLASSIFICATIONS,
  ORG_TYPES,
  type OrganizationClassification,
  type OrganizationType,
  type RegisterOrganizationPayload,
} from '../../models/organization.model';

export interface RegisterOrganizationDialogData {
  /** Pre-select classification when opened from a classification-specific view. */
  organizationClassification?: OrganizationClassification;
}

export interface RegisterOrganizationDialogResult {
  saved: boolean;
}

interface IndustrySelectOption {
  id: number;
  label: string;
}

const GENDER_OPTIONS = ['MALE', 'FEMALE', 'NON_BINARY', 'PREFER_NOT_TO_SAY'] as const;

@Component({
  selector: 'app-register-organization-dialog',
  templateUrl: './register-organization-dialog.component.html',
  styleUrl: './register-organization-dialog.component.scss',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
})
export class RegisterOrganizationDialogComponent implements OnInit, OnDestroy {
  readonly classifications = ORG_CLASSIFICATIONS;
  readonly organizationTypes = ORG_TYPES;
  readonly genderOptions = GENDER_OPTIONS;
  readonly form: FormGroup;
  readonly title = 'Add organisation';
  readonly subtitle =
    'Register on behalf of an applicant. No KYC approval is required — the organisation is verified immediately for admin registrations.';

  submitting = false;
  saveError = '';
  taxClearanceUploadLabel = '';
  taxClearanceCertificateUpload: File | null = null;
  taxUploadMissing = false;
  nationalIdUploadLabel = '';
  passportUploadLabel = '';
  nationalIdUpload: File | null = null;
  passportUpload: File | null = null;
  identificationError = '';
  industriesLoading = false;
  industryOptions: IndustrySelectOption[] = [];

  standaloneMode = false;
  inventoryManagementEnabled = true;
  crossDockingEnabled = false;
  inventoryDataSource: 'INTERNAL' | 'EXTERNAL_API' | 'MANUAL_ACK' = 'INTERNAL';
  counterpartyEngagementMode: 'RECORD_ONLY' | 'PLATFORM_ORG' = 'PLATFORM_ORG';

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly orgService: OrganizationsAdminService,
    private readonly snackBar: MatSnackBar,
    private readonly dialogRef: MatDialogRef<RegisterOrganizationDialogComponent, RegisterOrganizationDialogResult>,
    @Inject(MAT_DIALOG_DATA) readonly data: RegisterOrganizationDialogData,
  ) {
    const defaultClassification =
      data?.organizationClassification && ORG_CLASSIFICATIONS.some((c) => c.slug === data.organizationClassification)
        ? data.organizationClassification
        : ('SUPPLIER' as OrganizationClassification);

    this.form = this.fb.group({
      organizationClassification: [defaultClassification, Validators.required],
      organizationType: ['PRIVATE' as OrganizationType, Validators.required],
      industryId: [null as number | null],
      name: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phoneNumber: ['', Validators.required],
      registrationNumber: [''],
      taxNumber: [''],
      contactPersonFirstName: ['', Validators.required],
      contactPersonLastName: ['', Validators.required],
      contactPersonEmail: ['', [Validators.required, Validators.email]],
      contactPersonPhoneNumber: ['', Validators.required],
      contactPersonGender: ['', Validators.required],
      contactPersonDateOfBirth: ['', Validators.required],
      contactPersonNationalIdNumber: [''],
      contactPersonNationalIdExpiryDate: [''],
      contactPersonPassportNumber: [''],
      contactPersonPassportExpiryDate: [''],
      duplexMode: [false],
    });
  }

  get showDuplexOption(): boolean {
    const c = this.form.get('organizationClassification')?.value as OrganizationClassification | undefined;
    return c === 'SUPPLIER' || c === 'CUSTOMER';
  }

  get showOperationalMode(): boolean {
    const c = this.form.get('organizationClassification')?.value as OrganizationClassification | undefined;
    return c === 'SUPPLIER' || c === 'CUSTOMER';
  }

  get tradingModel(): 'PLATFORM_PARTNERS' | 'STANDALONE' {
    return this.standaloneMode ? 'STANDALONE' : 'PLATFORM_PARTNERS';
  }

  get inventoryModel(): 'FULL_INVENTORY' | 'CROSS_DOCKING' {
    return this.crossDockingEnabled && !this.inventoryManagementEnabled ? 'CROSS_DOCKING' : 'FULL_INVENTORY';
  }

  get platformTab(): 'RECORD_ONLY' | 'PLATFORM_ORG' {
    return this.counterpartyEngagementMode === 'RECORD_ONLY' ? 'RECORD_ONLY' : 'PLATFORM_ORG';
  }

  get orgClassification(): OrganizationClassification {
    return (this.form.get('organizationClassification')?.value as OrganizationClassification) ?? 'SUPPLIER';
  }

  get counterpartyLabel(): string {
    return this.orgClassification === 'SUPPLIER' ? 'customers' : 'suppliers';
  }

  setTradingModel(model: 'PLATFORM_PARTNERS' | 'STANDALONE'): void {
    if (model === 'STANDALONE') {
      this.standaloneMode = true;
      this.counterpartyEngagementMode = 'RECORD_ONLY';
      return;
    }
    this.standaloneMode = false;
  }

  setPlatformTab(tab: 'RECORD_ONLY' | 'PLATFORM_ORG'): void {
    if (this.tradingModel !== 'PLATFORM_PARTNERS') return;
    this.standaloneMode = false;
    this.counterpartyEngagementMode = tab;
  }

  get inventoryMgmtTab(): 'INTERNAL' | 'EXTERNAL_API' {
    return this.inventoryDataSource === 'EXTERNAL_API' ? 'EXTERNAL_API' : 'INTERNAL';
  }

  setInventoryModel(model: 'FULL_INVENTORY' | 'CROSS_DOCKING'): void {
    if (model === 'FULL_INVENTORY') {
      this.inventoryManagementEnabled = true;
      this.crossDockingEnabled = false;
      this.inventoryDataSource = this.inventoryMgmtTab === 'EXTERNAL_API' ? 'EXTERNAL_API' : 'INTERNAL';
      return;
    }
    this.inventoryManagementEnabled = false;
    this.crossDockingEnabled = true;
    if (this.inventoryDataSource === 'INTERNAL') {
      this.inventoryDataSource = 'EXTERNAL_API';
    }
  }

  setInventoryMgmtTab(tab: 'INTERNAL' | 'EXTERNAL_API'): void {
    if (this.inventoryModel !== 'FULL_INVENTORY') return;
    this.inventoryManagementEnabled = true;
    this.crossDockingEnabled = false;
    this.inventoryDataSource = tab;
  }

  setCrossDockFlow(source: 'EXTERNAL_API' | 'MANUAL_ACK'): void {
    if (this.inventoryModel !== 'CROSS_DOCKING') return;
    this.inventoryDataSource = source;
  }

  get maximumDateOfBirth(): string {
    return maximumDateOfBirthInput();
  }

  ngOnInit(): void {
    this.loadIndustries();
    this.form
      .get('taxNumber')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (!this.taxNumberProvided) {
          this.taxUploadMissing = false;
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get taxNumberProvided(): boolean {
    return String(this.form.get('taxNumber')?.value ?? '').trim().length > 0;
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.form.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || control.dirty);
  }

  cancel(): void {
    this.dialogRef.close({ saved: false });
  }

  onTaxClearanceSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.taxClearanceCertificateUpload = file;
    this.taxClearanceUploadLabel = file?.name ?? '';
    this.taxUploadMissing = false;
  }

  onNationalIdUploadSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.nationalIdUpload = file;
    this.nationalIdUploadLabel = file?.name ?? '';
    this.identificationError = '';
  }

  onPassportUploadSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.passportUpload = file;
    this.passportUploadLabel = file?.name ?? '';
    this.identificationError = '';
  }

  save(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }

    const taxNumber = String(this.form.get('taxNumber')?.value ?? '').trim();
    if (taxNumber && !this.taxClearanceCertificateUpload) {
      this.taxUploadMissing = true;
      this.form.markAllAsTouched();
      return;
    }

    const dob = String(this.form.get('contactPersonDateOfBirth')?.value ?? '').trim();
    if (!isDateOfBirthAtLeastMinimumAge(dob)) {
      this.saveError = dateOfBirthMinimumAgeMessage();
      this.form.markAllAsTouched();
      return;
    }
    const nationalIdNumber = String(this.form.get('contactPersonNationalIdNumber')?.value ?? '').trim();
    const passportNumber = String(this.form.get('contactPersonPassportNumber')?.value ?? '').trim();
    const nationalComplete = nationalIdNumber.length > 0 && !!this.nationalIdUpload;
    const passportComplete = passportNumber.length > 0 && !!this.passportUpload;
    if (!nationalComplete && !passportComplete) {
      this.identificationError =
        'Provide a national ID number with scan, or a passport number with scan (at least one complete set).';
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.getRawValue();
    const payload: RegisterOrganizationPayload = {
      name: String(v.name).trim(),
      email: String(v.email).trim(),
      phoneNumber: String(v.phoneNumber).trim(),
      organizationClassification: v.organizationClassification,
      organizationType: v.organizationType,
      industryId: this.toNumberOrNull(v.industryId) ?? undefined,
      contactPersonFirstName: String(v.contactPersonFirstName).trim(),
      contactPersonLastName: String(v.contactPersonLastName).trim(),
      contactPersonEmail: String(v.contactPersonEmail).trim(),
      contactPersonPhoneNumber: String(v.contactPersonPhoneNumber).trim(),
      contactPersonGender: String(v.contactPersonGender).trim(),
      contactPersonDateOfBirth: dob,
      contactPersonNationalIdNumber: nationalComplete ? nationalIdNumber : undefined,
      contactPersonNationalIdExpiryDate: nationalComplete
        ? String(v.contactPersonNationalIdExpiryDate ?? '').trim() || undefined
        : undefined,
      contactPersonNationalIdUpload: nationalComplete ? this.nationalIdUpload ?? undefined : undefined,
      contactPersonPassportNumber: passportComplete ? passportNumber : undefined,
      contactPersonPassportExpiryDate: passportComplete
        ? String(v.contactPersonPassportExpiryDate ?? '').trim() || undefined
        : undefined,
      contactPersonPassportUpload: passportComplete ? this.passportUpload ?? undefined : undefined,
      registrationNumber: String(v.registrationNumber ?? '').trim() || undefined,
      taxNumber: taxNumber || undefined,
      taxClearanceCertificateUpload: this.taxClearanceCertificateUpload ?? undefined,
      createdViaSignup: false,
      duplexMode: this.showDuplexOption && Boolean(v.duplexMode) ? true : undefined,
    };
    if (this.showOperationalMode) {
      payload.standaloneMode = this.standaloneMode;
      payload.inventoryManagementEnabled = this.inventoryManagementEnabled;
      payload.crossDockingEnabled = this.crossDockingEnabled;
      payload.inventoryDataSource = this.inventoryDataSource;
      payload.counterpartyEngagementMode = this.counterpartyEngagementMode;
    }

    this.submitting = true;
    this.saveError = '';
    this.identificationError = '';

    this.orgService
      .register(payload)
      .pipe(
        finalize(() => (this.submitting = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: () => {
          this.snackBar.open(
            'Organisation registered. Confirmation emails were queued to the organisation email and contact person email (if notifications are running).',
            'Close',
            { duration: 5000, panelClass: ['app-snackbar-success'] },
          );
          this.dialogRef.close({ saved: true });
        },
        error: (err: Error) => {
          this.saveError = err.message ?? 'Registration failed';
        },
      });
  }

  private loadIndustries(): void {
    this.industriesLoading = true;
    this.orgService
      .queryIndustriesWithUsage()
      .pipe(
        finalize(() => (this.industriesLoading = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.industryOptions = rows
            .filter((r) => r.active)
            .map((r) => ({
              id: r.id,
              label: r.industryCode ? `${r.name} (${r.industryCode})` : r.name,
            }))
            .sort((a, b) => a.label.localeCompare(b.label));
        },
        error: () => {
          this.industryOptions = [];
        },
      });
  }

  private toNumberOrNull(raw: unknown): number | null {
    const n = Number(raw);
    return Number.isFinite(n) && n > 0 ? n : null;
  }
}
