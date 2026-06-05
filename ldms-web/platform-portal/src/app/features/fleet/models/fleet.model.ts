import type { OrganizationType } from '../../../core/models/auth.model';

export type FleetVehicleType = 'rig' | 'van' | 'tanker' | 'flatbed';
export type FleetVehicleStatus = 'on_road' | 'yard' | 'maintenance' | 'available';

export interface FleetVehicleRow {
  id: string;
  registration: string;
  makeModel: string;
  type: FleetVehicleType;
  status: FleetVehicleStatus;
  statusLabel: string;
  utilizationPct: number;
  lastTripLabel: string;
  driverName: string;
  accentHue: number;
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
}

export interface FleetWorkspaceMetrics {
  ownFleetTotal: number;
  onRoad: number;
  available: number;
  avgUtilization: number;
  partnersTotal: number;
  partnersVerified: number;
}

export type FleetWorkspaceTab = 'overview' | 'convoy' | 'partners';

/** Industry row for register-partner dropdown (from GET /organization/industries). */
export interface IndustrySelectOption {
  id: number;
  label: string;
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
