import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import {
  OrganizationClassification,
  OrganizationType,
  ORG_TYPES,
} from '../../../../core/models/auth.model';
import { ThemeService } from '../../../../core/services/theme.service';
import { OrganizationService } from '../../../../core/services/organization.service';
import {
  dateOfBirthMinimumAgeMessage,
  isDateOfBirthAtLeastMinimumAge,
  maximumDateOfBirthInput,
} from '../../../../core/utils/date-of-birth.util';
import {
  STORAGE_ORG_ID,
  STORAGE_ORG_KYC,
  STORAGE_ORG_NAME,
} from '../../../onboarding/pages/onboarding-status/onboarding-status.component';

export interface ClassificationOption {
  id: OrganizationClassification;
  title: string;
  description: string;
  icon: string;
  features: string[];
}

const GENDER_OPTIONS = ['MALE', 'FEMALE', 'NON_BINARY', 'PREFER_NOT_TO_SAY'] as const;

@Component({
  selector: 'app-signup-wizard',
  templateUrl: './signup-wizard.component.html',
  styleUrls: ['./signup-wizard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class SignupWizardComponent {
  readonly steps = ['Organisation type', 'Organisation', 'Contact', 'Complete'];
  readonly organizationTypes = ORG_TYPES;
  readonly genderOptions = GENDER_OPTIONS;
  currentStep = 0;

  readonly classifications: ClassificationOption[] = [
    {
      id: 'SUPPLIER',
      title: 'Supplier',
      description: 'Supply products or services to other businesses.',
      icon: 'inventory_2',
      features: ['Inventory & orders', 'Shipments', 'Invoices'],
    },
    {
      id: 'CUSTOMER',
      title: 'Customer',
      description: 'Purchase from suppliers and track deliveries.',
      icon: 'storefront',
      features: ['Orders', 'Deliveries', 'Billing'],
    },
    {
      id: 'TRANSPORT_COMPANY',
      title: 'Transport company',
      description: 'Move goods with your fleet and drivers.',
      icon: 'local_shipping',
      features: ['Trips', 'Fleet', 'Documents'],
    },
    {
      id: 'CLEARING_AGENT',
      title: 'Clearing agent',
      description: 'Customs and regulatory clearance.',
      icon: 'fact_check',
      features: ['Clearances', 'Compliance', 'Tracking'],
    },
    {
      id: 'SERVICE_STATION',
      title: 'Service station',
      description: 'Fueling and vehicle services.',
      icon: 'local_gas_station',
      features: ['Visits', 'Fuel', 'Customers'],
    },
    {
      id: 'ROADSIDE_SUPPORT_SERVICE',
      title: 'Roadside support',
      description: 'Breakdown and emergency assistance.',
      icon: 'support_agent',
      features: ['Incidents', 'Dispatch', 'Jobs'],
    },
    {
      id: 'GOVERNMENT_AGENCY',
      title: 'Government agency',
      description: 'Oversight, border, and regulatory workflows.',
      icon: 'account_balance',
      features: ['Monitoring', 'Reviews', 'Reports'],
    },
  ];

  classification: OrganizationClassification | null = null;
  submitting = false;
  complete = false;
  submitError = '';
  registeredOrgId: number | null = null;

  orgForm: FormGroup;
  contactForm: FormGroup;

  nationalIdUploadLabel = '';
  passportUploadLabel = '';
  taxClearanceUploadLabel = '';
  nationalIdUpload: File | null = null;
  passportUpload: File | null = null;
  taxClearanceUpload: File | null = null;
  identificationError = '';
  taxDocumentError = '';

  constructor(
    private readonly fb: FormBuilder,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
    private readonly title: Title,
    private readonly orgService: OrganizationService,
    readonly theme: ThemeService,
  ) {
    this.title.setTitle('Create account | LX Platform');
    this.orgForm = this.fb.group({
      orgName: ['', [Validators.required, Validators.minLength(2)]],
      orgEmail: ['', [Validators.required, Validators.email]],
      organizationType: ['PRIVATE' as OrganizationType, Validators.required],
      phone: ['', Validators.required],
      registrationNumber: [''],
      taxNumber: ['', Validators.required],
    });
    this.contactForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      gender: ['', Validators.required],
      phoneNumber: ['', Validators.required],
      dateOfBirth: ['', Validators.required],
      nationalIdNumber: [''],
      nationalIdExpiryDate: [''],
      passportNumber: [''],
      passportExpiryDate: [''],
      acceptTerms: [false, Validators.requiredTrue],
    });
  }

  get maximumDateOfBirth(): string {
    return maximumDateOfBirthInput();
  }

  get progress(): number {
    return ((this.currentStep + 1) / this.steps.length) * 100;
  }

  selectClassification(c: OrganizationClassification): void {
    this.classification = c;
    this.cdr.markForCheck();
  }

  toggleTheme(): void {
    this.theme.toggle();
    this.cdr.markForCheck();
  }

  canAdvanceFromStep0(): boolean {
    return this.classification != null;
  }

  next(): void {
    if (this.currentStep >= 2) {
      return;
    }
    if (this.currentStep === 0 && !this.canAdvanceFromStep0()) {
      return;
    }
    if (this.currentStep === 1 && !this.validateOrgStep(true)) {
      this.cdr.markForCheck();
      return;
    }
    if (this.currentStep === 2 && !this.validateContactStep(true)) {
      this.cdr.markForCheck();
      return;
    }
    if (this.currentStep < this.steps.length - 1) {
      this.currentStep++;
      this.cdr.markForCheck();
    }
  }

  back(): void {
    if (this.currentStep > 0) {
      this.currentStep--;
      this.cdr.markForCheck();
    }
  }

  onNationalIdUploadSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.nationalIdUpload = file;
    this.nationalIdUploadLabel = file?.name ?? '';
    this.identificationError = '';
    this.cdr.markForCheck();
  }

  onPassportUploadSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.passportUpload = file;
    this.passportUploadLabel = file?.name ?? '';
    this.identificationError = '';
    this.cdr.markForCheck();
  }

  onTaxClearanceUploadSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.taxClearanceUpload = file;
    this.taxClearanceUploadLabel = file?.name ?? '';
    this.taxDocumentError = '';
    this.cdr.markForCheck();
  }

  submit(): void {
    if (!this.classification || !this.validateOrgStep(true) || !this.validateContactStep(true)) {
      this.cdr.markForCheck();
      return;
    }
    this.submitting = true;
    this.submitError = '';
    this.cdr.markForCheck();
    const org = this.orgForm.value;
    const contact = this.contactForm.value;
    const nationalIdNumber = String(contact.nationalIdNumber ?? '').trim();
    const passportNumber = String(contact.passportNumber ?? '').trim();
    const nationalComplete = nationalIdNumber.length > 0 && !!this.nationalIdUpload;
    const passportComplete = passportNumber.length > 0 && !!this.passportUpload;

    const payload = {
      name: String(org.orgName).trim(),
      email: String(org.orgEmail).trim(),
      phoneNumber: String(org.phone).trim(),
      organizationClassification: this.classification,
      organizationType: org.organizationType as OrganizationType,
      contactPersonFirstName: String(contact.firstName).trim(),
      contactPersonLastName: String(contact.lastName).trim(),
      contactPersonEmail: String(contact.email).trim(),
      contactPersonPhoneNumber: String(contact.phoneNumber).trim(),
      contactPersonGender: String(contact.gender).trim(),
      contactPersonDateOfBirth: String(contact.dateOfBirth).trim(),
      contactPersonNationalIdNumber: nationalComplete ? nationalIdNumber : undefined,
      contactPersonNationalIdExpiryDate: nationalComplete
        ? String(contact.nationalIdExpiryDate ?? '').trim() || undefined
        : undefined,
      contactPersonNationalIdUpload: nationalComplete ? this.nationalIdUpload ?? undefined : undefined,
      contactPersonPassportNumber: passportComplete ? passportNumber : undefined,
      contactPersonPassportExpiryDate: passportComplete
        ? String(contact.passportExpiryDate ?? '').trim() || undefined
        : undefined,
      contactPersonPassportUpload: passportComplete ? this.passportUpload ?? undefined : undefined,
      registrationNumber: String(org.registrationNumber ?? '').trim() || undefined,
      taxNumber: String(org.taxNumber ?? '').trim(),
      taxClearanceCertificateUpload: this.taxClearanceUpload ?? undefined,
    };
    this.orgService.register(payload).subscribe({
      next: (summary) => {
        this.submitting = false;
        this.complete = true;
        this.registeredOrgId = summary.id;
        sessionStorage.setItem(STORAGE_ORG_ID, String(summary.id));
        sessionStorage.setItem(STORAGE_ORG_NAME, summary.name);
        sessionStorage.setItem(STORAGE_ORG_KYC, summary.kycStatus);
        this.currentStep = 3;
        this.cdr.markForCheck();
      },
      error: (err: Error) => {
        this.submitting = false;
        this.submitError = err.message ?? 'Registration failed';
        this.cdr.markForCheck();
      },
    });
  }

  goOnboardingStatus(): void {
    const q = this.registeredOrgId ? { orgId: String(this.registeredOrgId) } : {};
    void this.router.navigate(['/onboarding/status'], { queryParams: q });
  }

  goLogin(): void {
    void this.router.navigate(['/auth/login']);
  }

  goWelcome(): void {
    void this.router.navigate(['/welcome']);
  }

  private validateOrgStep(showErrors: boolean): boolean {
    if (this.orgForm.invalid) {
      if (showErrors) {
        this.orgForm.markAllAsTouched();
      }
      return false;
    }
    if (!this.taxClearanceUpload) {
      if (showErrors) {
        this.taxDocumentError = 'Upload your ZIMRA tax clearance certificate (PDF, JPG, or PNG).';
      }
      return false;
    }
    this.taxDocumentError = '';
    return true;
  }

  private validateContactStep(showErrors: boolean): boolean {
    if (this.contactForm.invalid) {
      if (showErrors) {
        this.contactForm.markAllAsTouched();
      }
      return false;
    }
    const dob = String(this.contactForm.get('dateOfBirth')?.value ?? '').trim();
    if (!isDateOfBirthAtLeastMinimumAge(dob)) {
      if (showErrors) {
        this.submitError = dateOfBirthMinimumAgeMessage();
      }
      return false;
    }
    const nationalIdNumber = String(this.contactForm.get('nationalIdNumber')?.value ?? '').trim();
    const passportNumber = String(this.contactForm.get('passportNumber')?.value ?? '').trim();
    const nationalComplete = nationalIdNumber.length > 0 && !!this.nationalIdUpload;
    const passportComplete = passportNumber.length > 0 && !!this.passportUpload;
    if (!nationalComplete && !passportComplete) {
      if (showErrors) {
        this.identificationError =
          'Provide a national ID number with scan, or a passport number with scan (at least one complete set).';
      }
      return false;
    }
    this.identificationError = '';
    this.submitError = '';
    return true;
  }
}
