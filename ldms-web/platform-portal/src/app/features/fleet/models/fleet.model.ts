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
  contractScope?: FleetContractScope;
  contractStartDate?: string;
  contractEndDate?: string;
  utilizationPct: number;
  maxSpeedKmh?: number;
  lastTripLabel: string;
  driverName: string;
  fleetDriverId?: number;
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
  /** Only present when contractScope === 'long_term' and ownershipType === 'contracted'. */
  contractStartDate?: string;
  contractEndDate?: string;
  driverName?: string;
  fleetDriverId?: number;
  utilizationPct?: number;
  maxSpeedKmh?: number;
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
  contractStartDate?: string;
  contractEndDate?: string;
  driverName?: string;
  fleetDriverId?: number | null;
  utilizationPct?: number;
  maxSpeedKmh?: number;
}

/** Single document submitted during fleet asset registration (step 2). */
export type FleetRegistrationComplianceType =
  | 'VEHICLE_REGISTRATION'
  | 'ROAD_LICENSE'
  | 'ROADWORTHINESS'
  | 'INSURANCE'
  | 'GOODS_OPERATOR_LICENCE'
  | 'PERMIT'
  | 'HAZARDOUS_SUBSTANCES_PERMIT'
  | 'FIRE_SAFETY_CLEARANCE'
  | 'LEASE_HIRE_AGREEMENT'
  | 'LICENSE'
  | 'DEFENSIVE_DRIVING_CERTIFICATE'
  | 'DRIVER_MEDICAL_CERTIFICATE';

export interface FleetRegistrationDocumentPayload {
  /** Backend compliance type key — see {@link FleetRegistrationComplianceType}. */
  complianceType: FleetRegistrationComplianceType;
  /** ID returned by POST /ldms-file-upload-service/v1/frontend/file-upload/upload. */
  fileUploadId: number;
  /** ISO date string (YYYY-MM-DD) when the document expires; omit when not applicable. */
  expiresAt?: string;
}

/** POST /assets/{id}/complete-registration */
export interface CompleteFleetRegistrationPayload {
  documents: FleetRegistrationDocumentPayload[];
}

export type TransporterContractStatus = 'active' | 'expired' | 'upcoming' | 'open_ended';

export type TransporterLinkStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED';

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
  /** Offer lifecycle: ACCEPTED links are active; PENDING are offers awaiting the transporter's response. */
  linkStatus: TransporterLinkStatus;
  pending: boolean;
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

export type FleetWorkspaceTab = 'overview' | 'convoy' | 'partners' | 'drivers' | 'compliance' | 'tracking';

export type DriverEmploymentType = 'EMPLOYED' | 'POOL';
export type DriverRosterSource = 'organization' | 'transport_partner';

export interface FleetDriverRow {
  id: number;
  userId?: number;
  employmentType: DriverEmploymentType;
  employmentLabel: string;
  rosterSource: DriverRosterSource;
  homeOrganizationName?: string;
  firstName: string;
  lastName: string;
  fullName: string;
  phoneNumber: string;
  licenseNumber: string;
  licenseClass: string;
  nationalIdNumber?: string;
  nationalIdExpiryDate?: string;
  nationalIdUploadId?: number;
  passportNumber?: string;
  passportExpiryDate?: string;
  passportUploadId?: number;
  licenseUploadId?: number;
  addressLine1?: string;
  addressLine2?: string;
  addressCity?: string;
  addressProvince?: string;
  addressPostalCode?: string;
  addressCountry?: string;
  initials: string;
  accentHue: number;
}

export interface CreateFleetDriverPayload {
  userId?: number;
  employmentType?: DriverEmploymentType;
  firstName: string;
  lastName: string;
  /** Required when provisionPlatformAccess is true to receive temporary credentials. */
  email?: string;
  phoneNumber?: string;
  licenseNumber?: string;
  licenseClass?: string;
  nationalIdNumber?: string;
  nationalIdExpiryDate?: string;
  nationalIdUploadId?: number;
  passportNumber?: string;
  passportExpiryDate?: string;
  passportUploadId?: number;
  licenseUploadId?: number;
  addressLine1?: string;
  addressLine2?: string;
  addressCity?: string;
  addressProvince?: string;
  addressPostalCode?: string;
  addressCountry?: string;
  /**
   * When true the backend creates a platform user account and emails temporary credentials.
   * The transporter does NOT set a password — the driver receives it by email.
   */
  provisionPlatformAccess?: boolean;
}

export interface EditFleetDriverPayload extends CreateFleetDriverPayload {}

/** POST /fleet/drivers/{id}/provision-platform-access — enable login for legacy drivers. */
export interface ProvisionDriverPlatformAccessPayload {
  email: string;
  reissueCredentials?: boolean;
}

export interface ProvisionDriverPlatformAccessResult {
  driver: FleetDriverRow;
  message: string;
}

// ── Driver marketplace (freelance driver search) ──────────────────────────────

export type MarketplaceDriverAvailability = 'AVAILABLE' | 'BUSY' | 'INACTIVE';

export interface MarketplaceDriverRow {
  id: number;
  firstName: string;
  lastName: string;
  fullName: string;
  phoneNumber: string;
  licenseNumber: string;
  licenseClass: string;
  availability: MarketplaceDriverAvailability;
  availabilityLabel: string;
  initials: string;
  accentHue: number;
}

// ── Driver signup requests (for transporter review) ───────────────────────────

export type DriverSignupRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface DriverSignupRequestRow {
  id: number;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  phoneNumber: string;
  nationalIdNumber: string;
  licenseNumber: string;
  licenseClass: string;
  freelance: boolean;
  status: DriverSignupRequestStatus;
  statusLabel: string;
  createdAt: string;
  createdAtLabel: string;
  initials: string;
  accentHue: number;
  nationalIdFrontUploadId?: number;
  nationalIdBackUploadId?: number;
  licenseFrontUploadId?: number;
  licenseBackUploadId?: number;
}

export type FleetComplianceSubjectType = 'asset' | 'driver';
export type FleetComplianceType =
  | 'insurance'
  | 'license'
  | 'maintenance'
  | 'roadworthiness'
  | 'permit'
  | 'vehicle_registration'
  | 'road_license'
  | 'goods_operator_licence'
  | 'hazardous_substances_permit'
  | 'fire_safety_clearance'
  | 'lease_hire_agreement'
  | 'defensive_driving_certificate'
  | 'driver_medical_certificate'
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

// ── Tracking devices ──────────────────────────────────────────────────────────

export type TrackingDeviceType = 'MOBILE_PHONE' | 'OBD_TELEMATICS' | 'DEDICATED_GPS' | 'FUEL_SENSOR' | 'COMBO_UNIT';
export type TrackingIntegrationProvider =
  | 'LDMS_MOBILE'
  | 'GENERIC_MQTT'
  | 'TRACCAR'
  | 'GEOTAB'
  | 'CALAMP'
  | 'WIALON'
  | 'CUSTOM_HTTP';
export type TrackingInstallStatus = 'ACTIVE' | 'SUSPENDED' | 'PENDING';

/** Row returned by GET /fleet/tracking-devices and after install/edit. */
export interface FleetTrackingDeviceRow {
  id: number;
  deviceLabel: string;
  deviceType: TrackingDeviceType;
  deviceTypeLabel: string;
  installStatus: TrackingInstallStatus;
  installStatusLabel: string;
  integrationProvider: TrackingIntegrationProvider;
  integrationProviderLabel: string;
  fleetAssetId?: number;
  fleetDriverId?: number;
  linkedUserId?: number;
  deviceSerial?: string;
  externalDeviceId?: string;
  ingestKey?: string;
  tracksGps: boolean;
  tracksFuel: boolean;
  mqttTopic?: string;
  vehicleRegistration?: string;
  vehicleMakeModel?: string;
  installedAt?: string;
  lastTelemetryAt?: string;
  notes?: string;
}

/** POST /fleet/tracking-devices — install a new tracking device. */
export interface InstallFleetTrackingDevicePayload {
  deviceLabel: string;
  deviceType: TrackingDeviceType;
  integrationProvider: TrackingIntegrationProvider;
  fleetAssetId?: number;
  fleetDriverId?: number;
  deviceSerial?: string;
  externalDeviceId?: string;
  tracksGps: boolean;
  tracksFuel: boolean;
  notes?: string;
}

/** PUT /fleet/tracking-devices/{id} — update an installed tracking device. */
export interface EditFleetTrackingDevicePayload {
  deviceLabel?: string;
  deviceType?: TrackingDeviceType;
  integrationProvider?: TrackingIntegrationProvider;
  fleetAssetId?: number;
  fleetDriverId?: number;
  deviceSerial?: string;
  externalDeviceId?: string;
  tracksGps?: boolean;
  tracksFuel?: boolean;
  notes?: string;
}

/** Row returned by tracking-integration-credential API (integrator ingest keys). */
export interface FleetTrackingIntegrationCredentialRow {
  id: number;
  organizationId: number;
  credentialLabel: string;
  ingestKey?: string;
  integrationProvider: TrackingIntegrationProvider;
  integrationProviderLabel: string;
  status: TrackingInstallStatus;
  statusLabel: string;
  fleetAssetId?: number;
  vehicleRegistration?: string;
  vehicleMakeModel?: string;
  externalDeviceId?: string;
  mqttTopic?: string;
  lastTelemetryAt?: string;
  createdAt?: string;
}

/** POST /tracking-integration-credential/create */
export interface CreateFleetTrackingIntegrationCredentialPayload {
  organizationId: number;
  credentialLabel: string;
  integrationProvider: TrackingIntegrationProvider;
  fleetAssetId: number;
  externalDeviceId?: string;
  notes?: string;
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
