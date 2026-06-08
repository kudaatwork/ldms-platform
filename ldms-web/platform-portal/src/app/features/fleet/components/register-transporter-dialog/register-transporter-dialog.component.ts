import { Component, Inject, OnDestroy, OnInit, Optional } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Subject, finalize, takeUntil } from 'rxjs';
import { ORG_TYPES, type OrganizationType } from '../../../../core/models/auth.model';
import {
  dateOfBirthMinimumAgeMessage,
  isDateOfBirthAtLeastMinimumAge,
  maximumDateOfBirthInput,
} from '../../../../core/utils/date-of-birth.util';
import { CustomersPortalService } from '../../../customers/services/customers-portal.service';
import {
  IndustrySelectOption,
  RegisterTransporterPayload,
  TransporterEditDetail,
  TransporterPartnerRow,
} from '../../models/fleet.model';
import { FleetPortalService } from '../../services/fleet-portal.service';

const GENDER_OPTIONS = ['MALE', 'FEMALE', 'NON_BINARY', 'PREFER_NOT_TO_SAY'] as const;

export type RegisterTransporterDialogData = {
  /** When set the dialog operates in edit mode and pre-fills from GET /transporters/{id}. */
  partnerId?: number;
};

@Component({
  selector: 'app-register-transporter-dialog',
  templateUrl: './register-transporter-dialog.component.html',
  styleUrl: './register-transporter-dialog.component.scss',
  standalone: false,
})
export class RegisterTransporterDialogComponent implements OnInit, OnDestroy {
  readonly organizationTypes = ORG_TYPES;
  readonly genderOptions = GENDER_OPTIONS;

  readonly isEdit: boolean;
  readonly title: string;
  readonly subtitle: string;

  readonly form: FormGroup;
  submitting = false;
  loadingPartner = false;
  loadPartnerError = '';
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
  industriesLoadError = '';
  industryOptions: IndustrySelectOption[] = [];
  addressLine1 = '';
  addressLine2 = '';
  postalCode = '';
  suburbIdStr = '';
  addressError = '';

  private readonly partnerId?: number;
  private existingTaxUploadId?: number;
  private existingNationalIdUploadId?: number;
  private existingPassportUploadId?: number;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<RegisterTransporterDialogComponent, TransporterPartnerRow | undefined>,
    private readonly fleet: FleetPortalService,
    private readonly customers: CustomersPortalService,
    @Optional() @Inject(MAT_DIALOG_DATA) data: RegisterTransporterDialogData | null,
  ) {
    this.dialogRef.disableClose = true;
    this.partnerId = data?.partnerId;
    this.isEdit = !!this.partnerId;
    this.title = this.isEdit ? 'Edit transport partner' : 'Contract transporter';
    this.subtitle = this.isEdit
      ? 'Update the contracted transport partner profile and contract terms.'
      : 'Register a third-party transport company under contract — separate from your own fleet. Capture contract dates, then complete the same organisation profile fields as Add customer.';

    const today = new Date().toISOString().slice(0, 10);
    this.form = this.fb.group({
      contractStartDate: [today, Validators.required],
      contractEndDate: [''],
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
    });
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

    if (this.isEdit && this.partnerId) {
      this.loadPartnerForEdit(this.partnerId);
    }
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
    if (!this.submitting) {
      this.dialogRef.close();
    }
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

  // ============================================================
  // STEP 1: Validate form and identification docs
  // ============================================================
  save(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }

    const taxNumber = String(this.form.get('taxNumber')?.value ?? '').trim();
    if (taxNumber && !this.taxClearanceCertificateUpload && !this.existingTaxUploadId) {
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
    const nationalComplete =
      nationalIdNumber.length > 0 && (!!this.nationalIdUpload || !!this.existingNationalIdUploadId);
    const passportComplete =
      passportNumber.length > 0 && (!!this.passportUpload || !!this.existingPassportUploadId);
    if (!nationalComplete && !passportComplete) {
      this.identificationError =
        'Provide a national ID number with scan, or a passport number with scan (at least one complete set).';
      this.form.markAllAsTouched();
      return;
    }

    this.addressError = '';
    const suburbId = Number(this.suburbIdStr.trim());
    const hasAnyAddressInput =
      this.addressLine1.trim().length > 0 ||
      this.addressLine2.trim().length > 0 ||
      this.postalCode.trim().length > 0 ||
      (Number.isFinite(suburbId) && suburbId > 0);
    if (hasAnyAddressInput) {
      if (!this.addressLine1.trim() || !this.postalCode.trim() || !Number.isFinite(suburbId) || suburbId <= 0) {
        this.addressError =
          'Address requires line 1, postal code, and a selected suburb when any address field is set.';
        return;
      }
    }

    const v = this.form.getRawValue();
    const contractStartDate = String(v.contractStartDate ?? '').trim();
    const contractEndDate = String(v.contractEndDate ?? '').trim();
    if (contractEndDate && contractEndDate < contractStartDate) {
      this.saveError = 'Contract end date cannot be before the start date.';
      return;
    }

    // ============================================================
    // STEP 2: Build payload
    // ============================================================
    const payload: RegisterTransporterPayload = {
      contractStartDate,
      contractEndDate: contractEndDate || undefined,
      name: String(v.name).trim(),
      email: String(v.email).trim(),
      phoneNumber: String(v.phoneNumber).trim(),
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
      contactPersonNationalIdUploadId: nationalComplete ? this.existingNationalIdUploadId : undefined,
      contactPersonPassportNumber: passportComplete ? passportNumber : undefined,
      contactPersonPassportExpiryDate: passportComplete
        ? String(v.contactPersonPassportExpiryDate ?? '').trim() || undefined
        : undefined,
      contactPersonPassportUpload: passportComplete ? this.passportUpload ?? undefined : undefined,
      contactPersonPassportUploadId: passportComplete ? this.existingPassportUploadId : undefined,
      registrationNumber: String(v.registrationNumber ?? '').trim() || undefined,
      taxNumber: taxNumber || undefined,
      taxClearanceCertificateUpload: this.taxClearanceCertificateUpload ?? undefined,
      taxClearanceCertificateUploadId: taxNumber ? this.existingTaxUploadId : undefined,
      addressLine1: hasAnyAddressInput ? this.addressLine1.trim() : undefined,
      addressLine2: hasAnyAddressInput ? this.addressLine2.trim() || undefined : undefined,
      postalCode: hasAnyAddressInput ? this.postalCode.trim() : undefined,
      suburbId: hasAnyAddressInput ? suburbId : undefined,
    };

    this.submitting = true;
    this.saveError = '';
    this.identificationError = '';

    // ============================================================
    // STEP 3: Call update (edit) or register (create)
    // ============================================================
    const request$ =
      this.isEdit && this.partnerId
        ? this.fleet.updateTransporter(this.partnerId, payload)
        : this.fleet.registerTransporter(payload);

    request$
      .pipe(
        finalize(() => (this.submitting = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (row: TransporterPartnerRow) => this.dialogRef.close(row),
        error: (err: Error) => {
          this.saveError = err.message ?? 'Could not save transport partner.';
        },
      });
  }

  // ============================================================
  // Pre-fill form for edit mode
  // ============================================================
  private loadPartnerForEdit(partnerId: number): void {
    this.loadingPartner = true;
    this.loadPartnerError = '';
    this.fleet
      .getTransporterEditDetail(partnerId)
      .pipe(
        finalize(() => (this.loadingPartner = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (detail: TransporterEditDetail) => {
          this.form.patchValue({
            contractStartDate: detail.contractStartDate ?? '',
            contractEndDate: detail.contractEndDate ?? '',
            organizationType: detail.organizationType ?? 'PRIVATE',
            industryId: detail.industryId ?? null,
            name: detail.name,
            email: detail.email,
            phoneNumber: detail.phoneNumber,
            registrationNumber: detail.registrationNumber ?? '',
            taxNumber: detail.taxNumber ?? '',
            contactPersonFirstName: detail.contactPersonFirstName,
            contactPersonLastName: detail.contactPersonLastName,
            contactPersonEmail: detail.contactPersonEmail,
            contactPersonPhoneNumber: detail.contactPersonPhoneNumber,
            contactPersonGender: detail.contactPersonGender,
            contactPersonDateOfBirth: detail.contactPersonDateOfBirth,
            contactPersonNationalIdNumber: detail.contactPersonNationalIdNumber ?? '',
            contactPersonPassportNumber: detail.contactPersonPassportNumber ?? '',
          });

          this.existingTaxUploadId = detail.taxClearanceCertificateUploadId;
          this.existingNationalIdUploadId = detail.contactPersonNationalIdUploadId;
          this.existingPassportUploadId = detail.contactPersonPassportUploadId;

          if (detail.taxClearanceCertificateUploadId) {
            this.taxClearanceUploadLabel = '(existing file on record)';
          }
          if (detail.contactPersonNationalIdUploadId) {
            this.nationalIdUploadLabel = '(existing file on record)';
          }
          if (detail.contactPersonPassportUploadId) {
            this.passportUploadLabel = '(existing file on record)';
          }

          this.addressLine1 = detail.addressLine1 ?? '';
          this.addressLine2 = detail.addressLine2 ?? '';
          this.postalCode = detail.postalCode ?? '';
          this.suburbIdStr = detail.suburbId ? String(detail.suburbId) : '';
        },
        error: (err: Error) => {
          this.loadPartnerError = err.message ?? 'Could not load transport partner details for editing.';
        },
      });
  }

  private loadIndustries(): void {
    this.industriesLoading = true;
    this.industriesLoadError = '';
    this.customers
      .listPlatformIndustries()
      .pipe(
        finalize(() => (this.industriesLoading = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.industryOptions = rows;
        },
        error: (err: Error) => {
          this.industryOptions = [];
          this.industriesLoadError =
            err.message ?? 'Could not load industries. Ensure you are signed in and organization-management is running.';
        },
      });
  }

  private toNumberOrNull(raw: unknown): number | null {
    const n = Number(raw);
    return Number.isFinite(n) && n > 0 ? n : null;
  }
}
