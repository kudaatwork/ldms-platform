import { Component, Inject, OnDestroy, OnInit, Optional } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Subject, debounceTime, distinctUntilChanged, finalize, takeUntil } from 'rxjs';
import { ORG_TYPES, type OrganizationType } from '../../../../core/models/auth.model';
import {
  dateOfBirthMinimumAgeMessage,
  isDateOfBirthAtLeastMinimumAge,
  maximumDateOfBirthInput,
} from '../../../../core/utils/date-of-birth.util';
import type { AddressHierarchySeed } from '../../../users/components/user-address-cascade-fields/user-address-cascade-fields.component';
import {
  CustomerEditDetail,
  CustomerListRow,
  IndustrySelectOption,
  RegisterCustomerPayload,
} from '../../models/customer.model';
import {
  CustomerRegistrationConflictError,
  CustomerRegistrationEmailCheck,
  CustomersPortalService,
} from '../../services/customers-portal.service';
import {
  DuplexModeOfferDialogComponent,
  type DuplexModeOfferDialogData,
  type DuplexModeOfferDialogResult,
} from '../duplex-mode-offer-dialog/duplex-mode-offer-dialog.component';

const GENDER_OPTIONS = ['MALE', 'FEMALE', 'NON_BINARY', 'PREFER_NOT_TO_SAY'] as const;

const WIZARD_STEPS = [
  'Organization Info',
  'Address & Location',
  'Contact Person',
  'Business Details',
  'Documents',
  'Social & Submit',
] as const;

export type RegisterCustomerDialogData = {
  customerId?: number;
};

@Component({
  selector: 'app-register-customer-dialog',
  templateUrl: './register-customer-dialog.component.html',
  styleUrl: './register-customer-dialog.component.scss',
  standalone: false,
})
export class RegisterCustomerDialogComponent implements OnInit, OnDestroy {
  readonly organizationTypes = ORG_TYPES;
  readonly genderOptions = GENDER_OPTIONS;
  readonly wizardSteps = WIZARD_STEPS;
  currentStep = 0;
  readonly isEdit: boolean;
  readonly title: string;
  readonly subtitle: string;

  readonly form: FormGroup;
  submitting = false;
  loadingCustomer = false;
  saveError = '';
  private readonly customerId?: number;
  private existingTaxUploadId?: number;
  private existingNationalIdUploadId?: number;
  private existingPassportUploadId?: number;
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
  cityIdStr = '';
  seedSuburbId: number | null = null;
  addressSeed: AddressHierarchySeed | null = null;
  addressError = '';
  emailCheckLoading = false;
  emailCheckMessage = '';
  pendingLinkCheck: CustomerRegistrationEmailCheck | null = null;

  private existingLocationId?: number;
  private loadedAddress: {
    line1: string;
    line2: string;
    postalCode: string;
    suburbId: number;
  } | null = null;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<RegisterCustomerDialogComponent, CustomerListRow | undefined>,
    private readonly customers: CustomersPortalService,
    private readonly dialog: MatDialog,
    @Optional() @Inject(MAT_DIALOG_DATA) data: RegisterCustomerDialogData | null,
  ) {
    this.customerId = data?.customerId;
    this.isEdit = this.customerId != null && this.customerId > 0;
    this.title = this.isEdit ? 'Edit customer' : 'Add Customer Organization';
    this.subtitle = this.isEdit
      ? 'Update the buyer profile linked to your supplier workspace. Existing verification documents are kept unless you upload replacements.'
      : 'Complete registration form with all required business information';
    this.dialogRef.disableClose = true;
    this.form = this.fb.group({
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

  get wizardProgress(): number {
    return ((this.currentStep + 1) / this.wizardSteps.length) * 100;
  }

  get showWizard(): boolean {
    return !this.isEdit && !this.loadingCustomer;
  }

  showStep(stepIndex: number): boolean {
    return this.isEdit || this.currentStep === stepIndex;
  }

  goBack(): void {
    if (this.currentStep > 0) {
      this.currentStep -= 1;
      this.saveError = '';
      this.addressError = '';
      this.identificationError = '';
    }
  }

  goNext(): void {
    if (!this.validateStep(this.currentStep)) {
      return;
    }
    this.saveError = '';
    if (this.currentStep < this.wizardSteps.length - 1) {
      this.currentStep += 1;
    }
  }

  private validateStep(step: number): boolean {
    switch (step) {
      case 0:
        return this.touchControlsValid(['organizationType', 'name', 'email', 'phoneNumber']);
      case 1:
        return this.validateAddressStep();
      case 2:
        return this.touchControlsValid([
          'contactPersonFirstName',
          'contactPersonLastName',
          'contactPersonEmail',
          'contactPersonPhoneNumber',
          'contactPersonGender',
          'contactPersonDateOfBirth',
        ]);
      case 3:
        return true;
      case 4:
        return true;
      default:
        return true;
    }
  }

  private touchControlsValid(controlNames: string[]): boolean {
    controlNames.forEach((name) => this.form.get(name)?.markAsTouched());
    return controlNames.every((name) => this.form.get(name)?.valid);
  }

  private validateAddressStep(): boolean {
    this.addressError = '';
    const suburbId = Number(this.suburbIdStr.trim());
    const hasAnyAddressInput =
      this.addressLine1.trim().length > 0 ||
      this.addressLine2.trim().length > 0 ||
      this.postalCode.trim().length > 0 ||
      (Number.isFinite(suburbId) && suburbId > 0);
    if (!hasAnyAddressInput) {
      return true;
    }
    if (!this.addressLine1.trim() || !this.postalCode.trim() || !Number.isFinite(suburbId) || suburbId <= 0) {
      this.addressError =
        'Address requires line 1, postal code, and a selected suburb when any address field is set.';
      return false;
    }
    return true;
  }

  ngOnInit(): void {
    this.loadIndustries();
    if (this.isEdit && this.customerId) {
      this.loadCustomer(this.customerId);
    }
    this.form
      .get('taxNumber')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (!this.taxNumberProvided) {
          this.taxUploadMissing = false;
        }
      });
    if (!this.isEdit) {
      this.form
        .get('email')
        ?.valueChanges.pipe(debounceTime(450), distinctUntilChanged(), takeUntil(this.destroy$))
        .subscribe((raw) => this.checkRegistrationEmail(String(raw ?? '').trim()));
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

  save(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }

    const taxNumber = String(this.form.get('taxNumber')?.value ?? '').trim();
    const hasTaxDocument = !!this.taxClearanceCertificateUpload || !!this.existingTaxUploadId;
    if (taxNumber && !hasTaxDocument) {
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
    const cityId = Number(this.cityIdStr.trim());
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
    const payload: RegisterCustomerPayload = {
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
      contactPersonNationalIdUploadId: this.existingNationalIdUploadId,
      contactPersonPassportNumber: passportComplete ? passportNumber : undefined,
      contactPersonPassportExpiryDate: passportComplete
        ? String(v.contactPersonPassportExpiryDate ?? '').trim() || undefined
        : undefined,
      contactPersonPassportUpload: passportComplete ? this.passportUpload ?? undefined : undefined,
      contactPersonPassportUploadId: this.existingPassportUploadId,
      registrationNumber: String(v.registrationNumber ?? '').trim() || undefined,
      taxNumber: taxNumber || undefined,
      taxClearanceCertificateUpload: this.taxClearanceCertificateUpload ?? undefined,
      taxClearanceCertificateUploadId: this.existingTaxUploadId,
      addressLine1: hasAnyAddressInput ? this.addressLine1.trim() : undefined,
      addressLine2: hasAnyAddressInput ? this.addressLine2.trim() || undefined : undefined,
      postalCode: hasAnyAddressInput ? this.postalCode.trim() : undefined,
      suburbId: hasAnyAddressInput ? suburbId : undefined,
      cityId: hasAnyAddressInput && Number.isFinite(cityId) && cityId > 0 ? cityId : undefined,
      locationId: hasAnyAddressInput && this.shouldReuseExistingLocation(suburbId) ? this.existingLocationId : undefined,
    };

    this.submitting = true;
    this.saveError = '';
    this.identificationError = '';

    const request$ =
      this.isEdit && this.customerId
        ? this.customers.updateCustomer(this.customerId, payload)
        : this.customers.registerCustomer(payload);

    request$
      .pipe(
        finalize(() => (this.submitting = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (row) => this.dialogRef.close(row),
        error: (err: Error) => {
          if (err instanceof CustomerRegistrationConflictError) {
            this.openLinkOfferDialog(err.conflict);
            return;
          }
          this.saveError =
            err.message ?? (this.isEdit ? 'Could not update customer.' : 'Could not register customer.');
        },
      });
  }

  private checkRegistrationEmail(email: string): void {
    this.pendingLinkCheck = null;
    this.emailCheckMessage = '';
    if (!email || this.form.get('email')?.invalid) {
      return;
    }
    this.emailCheckLoading = true;
    this.customers
      .checkCustomerRegistrationEmail(email)
      .pipe(
        finalize(() => (this.emailCheckLoading = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (check) => {
          this.pendingLinkCheck =
            check.status === 'DUPLEX_OFFERED' || check.status === 'LINKABLE_CUSTOMER' ? check : null;
          if (check.status === 'AVAILABLE') {
            this.emailCheckMessage = '';
            return;
          }
          if (check.status === 'DUPLEX_OFFERED' || check.status === 'LINKABLE_CUSTOMER') {
            this.emailCheckMessage = check.message;
            return;
          }
          this.emailCheckMessage = check.message;
        },
        error: () => {
          this.emailCheckMessage = '';
        },
      });
  }

  offerLinkExisting(): void {
    if (!this.pendingLinkCheck?.existingOrganizationId) {
      return;
    }
    this.openLinkOfferDialog({
      ...this.pendingLinkCheck,
      existingOrganizationId: this.pendingLinkCheck.existingOrganizationId,
    });
  }

  private openLinkOfferDialog(
    check: CustomerRegistrationEmailCheck & { existingOrganizationId: number },
  ): void {
    const data: DuplexModeOfferDialogData = {
      organizationName: check.existingOrganizationName ?? 'Existing organisation',
      organizationEmail: check.existingOrganizationEmail,
      linkMode: check.status === 'LINKABLE_CUSTOMER' ? 'LINKABLE_CUSTOMER' : 'DUPLEX_OFFERED',
    };
    this.dialog
      .open<DuplexModeOfferDialogComponent, DuplexModeOfferDialogData, DuplexModeOfferDialogResult>(
        DuplexModeOfferDialogComponent,
        { data, width: '560px', maxWidth: '95vw' },
      )
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((result) => {
        if (!result || result.action !== 'link') {
          return;
        }
        this.submitting = true;
        this.saveError = '';
        this.customers
          .linkExistingOrganizationAsCustomer(check.existingOrganizationId, result.enableDuplexMode)
          .pipe(
            finalize(() => (this.submitting = false)),
            takeUntil(this.destroy$),
          )
          .subscribe({
            next: (row) => this.dialogRef.close(row),
            error: (err: Error) => {
              this.saveError = err.message ?? 'Could not link existing organisation.';
            },
          });
      });
  }

  private loadCustomer(customerId: number): void {
    this.loadingCustomer = true;
    this.saveError = '';
    this.customers
      .getCustomer(customerId)
      .pipe(
        finalize(() => (this.loadingCustomer = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (detail) => {
          this.existingLocationId = detail.locationId;
          this.existingTaxUploadId = detail.taxClearanceCertificateUploadId;
          this.existingNationalIdUploadId = detail.contactPersonNationalIdUploadId;
          this.existingPassportUploadId = detail.contactPersonPassportUploadId;
          if (this.existingTaxUploadId) {
            this.taxClearanceUploadLabel = 'Existing document on file';
          }
          if (this.existingNationalIdUploadId) {
            this.nationalIdUploadLabel = 'Existing document on file';
          }
          if (this.existingPassportUploadId) {
            this.passportUploadLabel = 'Existing document on file';
          }
          this.form.patchValue({
            organizationType: detail.organizationType,
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
          this.applyLoadedAddress(detail);
        },
        error: (err: Error) => {
          this.saveError = err.message ?? 'Could not load customer profile.';
        },
      });
  }

  private applyLoadedAddress(detail: CustomerEditDetail): void {
    this.loadedAddress = null;
    this.seedSuburbId = null;
    this.addressSeed = null;

    const line1 = String(detail.addressLine1 ?? '').trim();
    const line2 = String(detail.addressLine2 ?? '').trim();
    const postalCode = String(detail.postalCode ?? '').trim();
    const suburbNum = detail.suburbId ?? 0;

    if (!line1 && !line2 && !postalCode && !(suburbNum > 0)) {
      return;
    }

    this.addressLine1 = line1;
    this.addressLine2 = line2;
    this.postalCode = postalCode;

    if (suburbNum > 0) {
      this.seedSuburbId = suburbNum;
      this.suburbIdStr = String(suburbNum);
      this.cityIdStr = '';
      this.addressSeed = {
        countryId: detail.countryId,
        provinceId: detail.provinceId,
        districtId: detail.districtId,
        cityId: detail.cityId,
        cityName: detail.cityName ?? this.parseCityHintFromLine2(line2),
        addressLine2: line2 || undefined,
        suburbId: suburbNum,
      };
      this.loadedAddress = {
        line1,
        line2,
        postalCode,
        suburbId: suburbNum,
      };
    }
  }

  private parseCityHintFromLine2(line2: string): string | undefined {
    const raw = line2.trim();
    if (!raw) {
      return undefined;
    }
    const parts = raw
      .split(',')
      .map((p) => p.trim())
      .filter(Boolean);
    if (parts.length >= 2) {
      return parts[parts.length - 1];
    }
    return undefined;
  }

  private shouldReuseExistingLocation(suburbId: number): boolean {
    if (!this.isEdit || !this.existingLocationId || !this.loadedAddress) {
      return false;
    }
    return (
      this.addressLine1.trim() === this.loadedAddress.line1 &&
      this.addressLine2.trim() === this.loadedAddress.line2 &&
      this.postalCode.trim() === this.loadedAddress.postalCode &&
      suburbId === this.loadedAddress.suburbId
    );
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
