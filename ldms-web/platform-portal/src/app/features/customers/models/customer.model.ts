import type { OrganizationType } from '../../../core/models/auth.model';

export type CustomerKycStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'RESUBMISSION_REQUIRED'
  | string;

export type CustomerViewMode = 'atlas' | 'ledger';

export type CustomerStatusFilter = 'ALL' | 'VERIFIED' | 'PENDING_KYC' | 'DRAFT';

export interface CustomerListRow {
  id: number;
  name: string;
  email: string;
  phoneNumber: string;
  kycStatus: CustomerKycStatus;
  kycStatusLabel: string;
  kycTone: 'success' | 'warn' | 'muted' | 'danger';
  verified: boolean;
  verifiedLabel: string;
  initials: string;
  accentHue: number;
  createdAtLabel: string;
}

/** Industry row for register-customer dropdown (from GET /organization/industries). */
export interface IndustrySelectOption {
  id: number;
  label: string;
}

/** Full customer profile for edit dialog (GET /customers/{id}). */
export interface CustomerEditDetail {
  id: number;
  name: string;
  email: string;
  phoneNumber: string;
  organizationType: OrganizationType;
  industryId?: number;
  contactPersonFirstName: string;
  contactPersonLastName: string;
  contactPersonEmail: string;
  contactPersonPhoneNumber: string;
  contactPersonGender: string;
  contactPersonDateOfBirth: string;
  contactPersonNationalIdNumber?: string;
  contactPersonPassportNumber?: string;
  registrationNumber?: string;
  taxNumber?: string;
  taxClearanceCertificateUploadId?: number;
  contactPersonNationalIdUploadId?: number;
  contactPersonPassportUploadId?: number;
}

/** Multipart payload — mirrors admin Add organisation (classification fixed to CUSTOMER on server). */
export interface RegisterCustomerPayload {
  name: string;
  email: string;
  phoneNumber: string;
  organizationType: OrganizationType;
  industryId?: number;
  contactPersonFirstName: string;
  contactPersonLastName: string;
  contactPersonEmail: string;
  contactPersonPhoneNumber: string;
  contactPersonGender: string;
  contactPersonDateOfBirth: string;
  contactPersonNationalIdNumber?: string;
  contactPersonNationalIdExpiryDate?: string;
  contactPersonNationalIdUpload?: File;
  contactPersonNationalIdUploadId?: number;
  contactPersonPassportNumber?: string;
  contactPersonPassportExpiryDate?: string;
  contactPersonPassportUpload?: File;
  contactPersonPassportUploadId?: number;
  registrationNumber?: string;
  taxNumber?: string;
  taxClearanceCertificateUpload?: File;
  taxClearanceCertificateUploadId?: number;
  addressLine1?: string;
  addressLine2?: string;
  postalCode?: string;
  suburbId?: number;
}

export interface CustomerPageMetrics {
  total: number;
  verified: number;
  pendingKyc: number;
  draft: number;
}
