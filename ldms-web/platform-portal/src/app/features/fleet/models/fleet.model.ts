import type { OrganizationType } from '../../../core/models/auth.model';

export type FleetVehicleType = 'rig' | 'van' | 'tanker' | 'flatbed';
export type FleetVehicleStatus = 'on_road' | 'yard' | 'maintenance' | 'available';
export type FleetVehicleOwnershipType = 'owned' | 'contracted';

export interface FleetVehicleRow {
  /** Numeric id from the Fleet Vehicles API; 'preview-...' string for deterministic preview data. */
  id: number | string;
  registration: string;
  makeModel: string;
  type: FleetVehicleType;
  status: FleetVehicleStatus;
  statusLabel: string;
  ownershipType: FleetVehicleOwnershipType;
  ownershipLabel: string;
  contractedTransporterOrganizationId?: number;
  contractedTransporterOrganizationName?: string;
  utilizationPct: number;
  lastTripLabel: string;
  driverName: string;
  accentHue: number;
}

/** Contract scope for assets registered under a contracted transporter link. */
export type FleetContractScope = 'long_term' | 'job';

/** POST /fleet-vehicles — add a new owned vehicle. */
export interface CreateFleetVehiclePayload {
  registration: string;
  makeModel: string;
  type: FleetVehicleType;
  status: FleetVehicleStatus;
  ownershipType?: FleetVehicleOwnershipType;
  contractedTransporterOrganizationId?: number;
  /** Only present when ownershipType === 'contracted'. */
  contractScope?: FleetContractScope;
  /** Only present when contractScope === 'job'. */
  jobReference?: string;
  driverName?: string;
  utilizationPct?: number;
}

/** PUT /fleet-vehicles/{id} — update an owned vehicle. */
export interface EditFleetVehiclePayload {
  registration: string;
  makeModel: string;
  type: FleetVehicleType;
  status: FleetVehicleStatus;
  ownershipType?: FleetVehicleOwnershipType;
  contractedTransporterOrganizationId?: number;
  contractScope?: FleetContractScope;
  jobReference?: string;
  driverName?: string;
  utilizationPct?: number;
}

/** Single document submitted during fleet asset registration (step 2). */
export interface FleetRegistrationDocumentPayload {
  /** Backend compliance type key — must be INSURANCE, ROADWORTHINESS, or PERMIT for registration. */
  complianceType: 'INSURANCE' | 'ROADWORTHINESS' | 'PERMIT';
  /** ID returned by POST /ldms-file-upload-service/v1/frontend/file-upload/upload. */
  fileUploadId: number;
  /** ISO date string (YYYY-MM-DD). */
  expiresAt: string;
}

/** POST /assets/{id}/complete-registration */
export interface CompleteFleetRegistrationPayload {
  documents: FleetRegistrationDocumentPayload[];
}

export type TransporterContractStatus = 'active' | 'expired' | 'upcoming' | 'open_ended';

export interface TransporterPartnerRow {
  id: number;
  name: string;
  email: string;
  phoneNumber: string;
  verified: boolean;
  verifiedLabel: string;
  kycStatusLabel: string;
  initials: string;
  accentHue: number;
  linkedSinceLabel: string;
  contractStartLabel: string;
  contractEndLabel: string;
  contractRangeLabel: string;
  contractStatus: TransporterContractStatus;
  contractStatusLabel: string;
  partnerKind: 'contracted';
  /** Raw ISO date string stored for use in the edit dialog pre-fill. */
  contractStartDate?: string;
  /** Raw ISO date string stored for use in the edit dialog pre-fill. */
  contractEndDate?: string;
}

export interface FleetWorkspaceMetrics {
  ownFleetTotal: number;
  ownedFleetTotal: number;
  contractedFleetTotal: number;
  onRoad: number;
  available: number;
  avgUtilization: number;
  partnersTotal: number;
  partnersVerified: number;
  driversTotal: number;
  expiringComplianceTotal: number;
}

export type FleetWorkspaceTab = 'overview' | 'convoy' | 'partners' | 'drivers' | 'compliance';

export interface FleetDriverRow {
  id: number;
  userId?: number;
  firstName: string;
  lastName: string;
  fullName: string;
  phoneNumber: string;
  licenseNumber: string;
  licenseClass: string;
  initials: string;
  accentHue: number;
}

export interface CreateFleetDriverPayload {
  userId?: number;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  licenseNumber?: string;
  licenseClass?: string;
}

export interface EditFleetDriverPayload extends CreateFleetDriverPayload {}

export type FleetComplianceSubjectType = 'asset' | 'driver';
export type FleetComplianceType =
  | 'insurance'
  | 'license'
  | 'maintenance'
  | 'roadworthiness'
  | 'permit'
  | 'other';

export interface FleetComplianceRow {
  id: number;
  subjectType: FleetComplianceSubjectType;
  subjectTypeLabel: string;
  subjectId: number;
  subjectLabel: string;
  complianceType: FleetComplianceType;
  complianceTypeLabel: string;
  fileUploadId?: number;
  expiresAt?: string;
  expiresLabel: string;
  expiryStatus: 'ok' | 'expiring' | 'expired' | 'none';
  status: string;
  statusLabel: string;
  notes: string;
}

export interface CreateFleetCompliancePayload {
  subjectType: FleetComplianceSubjectType;
  subjectId: number;
  complianceType: FleetComplianceType;
  fileUploadId?: number;
  expiresAt?: string;
  notes?: string;
}

export interface EditFleetCompliancePayload {
  fileUploadId?: number;
  expiresAt?: string;
  status?: string;
  notes?: string;
}

/** Industry row for register-partner dropdown (from GET /organization/industries). */
export interface IndustrySelectOption {
  id: number;
  label: string;
}

/** Full transporter profile pre-filled in the edit dialog (GET /transporters/{id}). */
export interface TransporterEditDetail {
  id: number;
  name: string;
  email: string;
  phoneNumber: string;
  organizationType: OrganizationType;
  industryId?: number;
  contractStartDate: string;
  contractEndDate?: string;
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
  locationId?: number;
  addressLine1?: string;
  addressLine2?: string;
  postalCode?: string;
  suburbId?: number;
}

/** Multipart payload — mirrors register customer; classification fixed to TRANSPORT_COMPANY on server. */
export interface RegisterTransporterPayload {
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
  contractStartDate: string;
  contractEndDate?: string;
}
