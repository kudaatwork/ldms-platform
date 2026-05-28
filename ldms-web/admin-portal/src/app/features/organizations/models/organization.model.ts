import type { AgentListRow, BranchListRow } from './organization-directory.model';

/** Aligns with backend {@code OrganizationClassification}. */
export type OrganizationClassification =
  | 'SUPPLIER'
  | 'CUSTOMER'
  | 'TRANSPORT_COMPANY'
  | 'CLEARING_AGENT'
  | 'SERVICE_STATION'
  | 'ROADSIDE_SUPPORT_SERVICE'
  | 'GOVERNMENT_AGENCY';

/** Legal entity type — aligns with backend {@code OrganizationType}. */
export type OrganizationType =
  | 'PRIVATE'
  | 'GOVERNMENT'
  | 'NGO'
  | 'NON_PROFIT'
  | 'PUBLIC'
  | 'COOPERATIVE'
  | 'OTHER';

export type KycStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'STAGE_1_REVIEW'
  | 'STAGE_2_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'RESUBMITTED';

export interface OrganizationListRow {
  id: number;
  name: string;
  classification: OrganizationClassification;
  classificationLabel: string;
  organizationType: OrganizationType;
  organizationTypeLabel: string;
  kycStatus: KycStatus;
  statusLabel: string;
  statusCss: string;
  email: string;
  phoneNumber: string;
  industryName: string;
  registrationNumber: string;
  /** Admin portal registration vs platform signup. */
  sourceLabel: string;
  createdViaSignup: boolean;
  entityStatus: string;
  submittedLabel: string;
  createdLabel: string;
  verified: boolean;
  verifiedLabel: string;
}

export interface KycQueueRow extends OrganizationListRow {
  applicant: string;
  submitted: string;
  stage1ApproverLabel?: string;
  stage2ApproverLabel?: string;
}

export interface KycApplicationDocument {
  /** File-upload service primary key. */
  uploadId: number;
  fileName: string;
  category: string;
  /** Human hint: PDF, Image, or file-type label. */
  fileType: string;
  uploadedAt: string;
}

export interface KycApproverAssignment {
  userId?: number;
  username?: string;
  displayName?: string;
}

export interface KycApplicationDetail extends KycQueueRow {
  tradingName: string;
  legalForm: string;
  registrationNumber: string;
  taxVatNumber: string;
  industrySector: string;
  primaryContactName: string;
  primaryContactEmail: string;
  primaryContactPhone: string;
  contactPersonGenderLabel: string;
  contactPersonDateOfBirth: string;
  contactPersonNationalIdNumber: string;
  contactPersonPassportNumber: string;
  registeredAddress: string;
  principalPlaceOfBusiness: string;
  bankName: string;
  bankAccountMasked: string;
  applicantNotes: string;
  documents: KycApplicationDocument[];
  kycStage: 'stage1' | 'stage2' | 'none';
  stage1Approver?: KycApproverAssignment;
  stage2Approver?: KycApproverAssignment;
  requiresKycApproval: boolean;
}

/** Linked organisation summary (supplier customers, contracted transporters, etc.). */
export interface OrganizationLinkRow {
  id: number;
  name: string;
  classification?: string;
  classificationLabel?: string;
  email?: string;
  kycStatus?: string;
  kycStatusLabel?: string;
  verified?: boolean;
}

/**
 * Organisation profile for admin detail shell (GET by id includes nested directory lists).
 */
export interface OrganizationProfileDetail {
  id: number;
  name: string;
  email: string;
  phoneNumber: string;
  organizationClassification: OrganizationClassification;
  classificationLabel: string;
  organizationType: OrganizationType;
  organizationTypeLabel: string;
  industryId?: number | null;
  industryName?: string;
  registrationNumber?: string;
  taxNumber?: string;
  contactPersonFirstName: string;
  contactPersonLastName: string;
  contactPersonEmail: string;
  contactPersonPhoneNumber?: string;
  /** User-management id for the provisioned contact person portal account. */
  contactPersonUserId?: number | null;
  websiteUrl?: string;
  organizationDescription?: string;
  kycStatus: KycStatus;
  kycStatusLabel: string;
  kycStatusCss: string;
  isVerified?: boolean | null;
  entityStatus?: string | null;
  branches: BranchListRow[];
  agents: AgentListRow[];
  customers: OrganizationLinkRow[];
  transporters: OrganizationLinkRow[];
}

/**
 * Payload for multipart PUT `/organization/{id}` (mirrors backend {@link UpdateOrganizationRequest} subset).
 */
export interface UpdateOrganizationPayload {
  name?: string;
  email?: string;
  phoneNumber?: string;
  organizationType?: OrganizationType;
  industryId?: number;
  registrationNumber?: string;
  taxNumber?: string;
  contactPersonFirstName?: string;
  contactPersonLastName?: string;
  contactPersonEmail?: string;
  contactPersonPhoneNumber?: string;
  websiteUrl?: string;
  organizationDescription?: string;
}

export interface RegisterOrganizationPayload {
  name: string;
  email: string;
  phoneNumber: string;
  organizationClassification: OrganizationClassification;
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
  contactPersonPassportNumber?: string;
  contactPersonPassportExpiryDate?: string;
  contactPersonPassportUpload?: File;
  registrationNumber?: string;
  taxNumber?: string;
  createdViaSignup?: boolean;
  /** ZIMRA / tax clearance certificate (multipart — same pattern as user national ID upload). */
  taxClearanceCertificateUpload?: File;
  taxClearanceCertificateUploadId?: number;
}

export interface KycDecisionPayload {
  reviewerUsername?: string;
  notes?: string;
}

export interface KycRejectPayload extends KycDecisionPayload {
  rejectionReason: string;
}

export type KycDecisionAction =
  | 'stage1-approve'
  | 'stage1-reject'
  | 'stage2-approve'
  | 'stage2-reject'
  | 'allow-resubmission';

export interface KycApplicationDecisionResult {
  action: KycDecisionAction;
  reason: string;
}

export interface OrgClassificationNav {
  slug: OrganizationClassification;
  label: string;
}

export const ORG_CLASSIFICATIONS: readonly OrgClassificationNav[] = [
  { slug: 'SUPPLIER', label: 'Supplier' },
  { slug: 'CUSTOMER', label: 'Customer' },
  { slug: 'TRANSPORT_COMPANY', label: 'Transport company' },
  { slug: 'CLEARING_AGENT', label: 'Clearing agent' },
  { slug: 'SERVICE_STATION', label: 'Service station' },
  { slug: 'ROADSIDE_SUPPORT_SERVICE', label: 'Roadside support' },
  { slug: 'GOVERNMENT_AGENCY', label: 'Government agency' },
] as const;

export interface OrgTypeOption {
  slug: OrganizationType;
  label: string;
}

export const ORG_TYPES: readonly OrgTypeOption[] = [
  { slug: 'PRIVATE', label: 'Private company' },
  { slug: 'GOVERNMENT', label: 'Government' },
  { slug: 'NGO', label: 'NGO' },
  { slug: 'NON_PROFIT', label: 'Non-profit' },
  { slug: 'PUBLIC', label: 'Public entity' },
  { slug: 'COOPERATIVE', label: 'Cooperative' },
  { slug: 'OTHER', label: 'Other' },
] as const;

export function organizationTypeLabel(t: OrganizationType | string): string {
  return ORG_TYPES.find((x) => x.slug === t)?.label ?? String(t).replace(/_/g, ' ');
}

export function classificationLabel(c: OrganizationClassification | string): string {
  return ORG_CLASSIFICATIONS.find((x) => x.slug === c)?.label ?? String(c).replace(/_/g, ' ');
}

export function kycStatusPresentation(status: KycStatus | string): { label: string; css: string } {
  const s = String(status).toUpperCase();
  switch (s) {
    case 'DRAFT':
      return { label: 'Draft', css: 'pending' };
    case 'SUBMITTED':
      return { label: 'Submitted', css: 'submitted' };
    case 'STAGE_1_REVIEW':
      return { label: 'Stage 1 review', css: 'stage1' };
    case 'STAGE_2_REVIEW':
      return { label: 'Stage 2 review', css: 'stage2' };
    case 'APPROVED':
      return { label: 'Approved', css: 'approved' };
    case 'REJECTED':
      return { label: 'Rejected', css: 'rejected' };
    case 'RESUBMITTED':
      return { label: 'Resubmitted', css: 'resubmitted' };
    default:
      return { label: s, css: 'pending' };
  }
}

export function resolveKycStage(status: KycStatus | string): 'stage1' | 'stage2' | 'none' {
  const s = String(status).toUpperCase();
  /** Platform signups start in DRAFT but appear in the KYC queue for stage-1 review. */
  if (s === 'DRAFT' || s === 'SUBMITTED' || s === 'STAGE_1_REVIEW' || s === 'RESUBMITTED') {
    return 'stage1';
  }
  if (s === 'STAGE_2_REVIEW') {
    return 'stage2';
  }
  return 'none';
}
