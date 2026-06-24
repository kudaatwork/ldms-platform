import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { OrganizationClassification, OrganizationType } from '../models/auth.model';
import { ldmsApiUrl, ldmsServiceUrl } from '../utils/api-url.util';

export interface RegisterOrganizationPayload {
  name: string;
  email: string;
  phoneNumber: string;
  organizationClassification: OrganizationClassification;
  organizationType: OrganizationType;
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
  taxClearanceCertificateUpload?: File;
  createdViaSignup?: boolean;
  duplexMode?: boolean;
  standaloneMode?: boolean;
  inventoryManagementEnabled?: boolean;
  crossDockingEnabled?: boolean;
  inventoryDataSource?: 'INTERNAL' | 'EXTERNAL_API' | 'MANUAL_ACK';
  counterpartyEngagementMode?: 'RECORD_ONLY' | 'PLATFORM_ORG';
}

export interface OrganizationSummary {
  id: number;
  name: string;
  email: string;
  phoneNumber?: string;
  locationId?: number;
  websiteUrl?: string;
  organizationDescription?: string;
  numberOfEmployees?: number;
  annualRevenueEstimate?: number;
  regionsServed?: string;
  businessHours?: string;
  registrationNumber?: string;
  taxNumber?: string;
  addressLine1?: string;
  addressLine2?: string;
  addressPostalCode?: string;
  addressSuburbId?: number;
  addressCityId?: number;
  addressCityName?: string;
  addressDistrictName?: string;
  addressProvinceName?: string;
  createdViaSignup?: boolean;
  headOfficeBranch?: BranchDetail;
  kycStatus: string;
  isVerified: boolean;
  organizationClassification?: OrganizationClassification;
  duplexMode?: boolean;
  standaloneMode?: boolean;
  inventoryManagementEnabled?: boolean;
  crossDockingEnabled?: boolean;
  inventoryDataSource?: 'INTERNAL' | 'EXTERNAL_API' | 'MANUAL_ACK';
  counterpartyEngagementMode?: 'RECORD_ONLY' | 'PLATFORM_ORG';
  fuelConsumptionEnabled?: boolean;
  branches?: BranchAllocationOption[];

  // === KYC / contact person fields (read-only after approval) ===
  contactPersonFirstName?: string;
  contactPersonLastName?: string;
  contactPersonEmail?: string;
  contactPersonPhoneNumber?: string;
  contactPersonPosition?: string;
  contactPersonGender?: string;
  contactPersonNationalIdNumber?: string;
  contactPersonNationalIdUploadId?: number;
  contactPersonPassportNumber?: string;
  contactPersonPassportUploadId?: number;
  contactPersonDateOfBirth?: string;
  contactPersonUserId?: number;
  representativeNationalIdNumber?: string;
  representativePassportNumber?: string;

  // === KYC document upload ids ===
  registrationCertificateUploadId?: number;
  taxClearanceCertificateUploadId?: number;
  businessLicenseUploadId?: number;
  proofOfAddressUploadId?: number;
  industrySpecificLicenseUploadId?: number;
  logoUploadId?: number;

  // === KYC audit trail ===
  submittedAt?: string;
  currentResubmissionCycle?: number;
  lastRejectionReason?: string;
  resubmissionCount?: number;
}

export interface BranchAllocationOption {
  id: number;
  label: string;
  level: 'BRANCH' | 'SUB_BRANCH';
  depot: boolean;
  parentBranchId?: number;
  parentBranchName?: string;
}

export interface BranchDetail {
  id: number;
  branchName: string;
  branchCode?: string;
  branchLevel: 'BRANCH' | 'SUB_BRANCH';
  parentBranchId?: number;
  parentBranchName?: string;
  depot: boolean;
  headOffice: boolean;
  region?: string;
  email?: string;
  phoneNumber?: string;
  businessHours?: string;
  active: boolean;
}

export interface BranchFormPayload {
  branchName: string;
  branchCode?: string;
  region?: string;
  email?: string;
  phoneNumber?: string;
  businessHours?: string;
  headOffice?: boolean;
  active?: boolean;
  parentBranchId?: number;
  depot?: boolean;
}

/** @deprecated Use BranchFormPayload */
export type AddBranchPayload = BranchFormPayload;

/** Public onboarding tracker (limited fields from GET onboarding-status). */
export interface OnboardingStatus {
  id: number;
  name: string;
  kycStatus: string;
  verified: boolean;
  requiredApprovalStages: number;
  lastRejectionReason?: string;
}

export interface OperationalSettingsPayload {
  standaloneMode: boolean;
  inventoryManagementEnabled: boolean;
  crossDockingEnabled: boolean;
  inventoryDataSource: 'INTERNAL' | 'EXTERNAL_API' | 'MANUAL_ACK';
  counterpartyEngagementMode?: 'RECORD_ONLY' | 'PLATFORM_ORG';
  fuelConsumptionEnabled?: boolean;
}

export interface UpdateMyOrganizationPayload {
  name?: string;
  email?: string;
  phoneNumber?: string;
  locationId?: number;
  websiteUrl?: string;
  organizationDescription?: string;
  businessHours?: string;
  numberOfEmployees?: number;
  annualRevenueEstimate?: number;
  regionsServed?: string;
  registrationNumber?: string;
  taxNumber?: string;
  addressLine1?: string;
  addressLine2?: string;
  postalCode?: string;
  suburbId?: number;
  cityId?: number;
}

export interface TradingPartner {
  id: number;
  name: string;
  email?: string;
  phoneNumber?: string;
  role: 'CUSTOMER' | 'SUPPLIER' | 'TRANSPORTER' | 'OTHER';
  notes?: string;
  entityStatus: string;
  createdAt?: string;
}

export interface TradingPartnerPayload {
  name: string;
  email?: string;
  phoneNumber?: string;
  role: 'CUSTOMER' | 'SUPPLIER' | 'TRANSPORTER' | 'OTHER';
  notes?: string;
}

@Injectable({ providedIn: 'root' })
export class OrganizationService {
  private readonly base = ldmsServiceUrl('organization-management', 'organization');

  constructor(private readonly http: HttpClient) {}

  /**
   * Public tracker — uses system surface (permitAll) so applicants without a JWT are not blocked.
   */
  getOnboardingStatus(organizationId: number): Observable<OnboardingStatus> {
    const url = ldmsServiceUrl(
      'organization-management',
      'organization',
      `onboarding-status/${organizationId}`,
      'system',
    );
    return this.http
      .get<unknown>(url)
      .pipe(
        map((resp) => this.mapOnboardingStatus(resp)),
        catchError((err) => throwError(() => this.toError(err))),
      );
  }

  register(payload: RegisterOrganizationPayload): Observable<OrganizationSummary> {
    const form = new FormData();
    this.appendFormValue(form, 'name', payload.name);
    this.appendFormValue(form, 'email', payload.email);
    this.appendFormValue(form, 'phoneNumber', payload.phoneNumber);
    this.appendFormValue(form, 'organizationClassification', payload.organizationClassification);
    this.appendFormValue(form, 'organizationType', payload.organizationType);
    this.appendFormValue(form, 'contactPersonFirstName', payload.contactPersonFirstName);
    this.appendFormValue(form, 'contactPersonLastName', payload.contactPersonLastName);
    this.appendFormValue(form, 'contactPersonEmail', payload.contactPersonEmail);
    this.appendFormValue(form, 'contactPersonPhoneNumber', payload.contactPersonPhoneNumber);
    this.appendFormValue(form, 'contactPersonGender', payload.contactPersonGender);
    this.appendFormValue(form, 'contactPersonDateOfBirth', payload.contactPersonDateOfBirth);
    this.appendFormValue(form, 'contactPersonNationalIdNumber', payload.contactPersonNationalIdNumber);
    this.appendFormValue(form, 'contactPersonNationalIdExpiryDate', payload.contactPersonNationalIdExpiryDate);
    this.appendFormValue(form, 'contactPersonPassportNumber', payload.contactPersonPassportNumber);
    this.appendFormValue(form, 'contactPersonPassportExpiryDate', payload.contactPersonPassportExpiryDate);
    this.appendFormValue(form, 'registrationNumber', payload.registrationNumber);
    this.appendFormValue(form, 'taxNumber', payload.taxNumber);
    this.appendFormValue(form, 'createdViaSignup', true);
    if (payload.duplexMode) {
      this.appendFormValue(form, 'duplexMode', true);
    }
    if (payload.standaloneMode != null) {
      this.appendFormValue(form, 'standaloneMode', payload.standaloneMode);
    }
    if (payload.inventoryManagementEnabled != null) {
      this.appendFormValue(form, 'inventoryManagementEnabled', payload.inventoryManagementEnabled);
    }
    if (payload.crossDockingEnabled != null) {
      this.appendFormValue(form, 'crossDockingEnabled', payload.crossDockingEnabled);
    }
    if (payload.inventoryDataSource) {
      this.appendFormValue(form, 'inventoryDataSource', payload.inventoryDataSource);
    }
    if (payload.counterpartyEngagementMode) {
      this.appendFormValue(form, 'counterpartyEngagementMode', payload.counterpartyEngagementMode);
    }
    this.appendFormFile(form, 'contactPersonNationalIdUpload', payload.contactPersonNationalIdUpload);
    this.appendFormFile(form, 'contactPersonPassportUpload', payload.contactPersonPassportUpload);
    this.appendFormFile(form, 'taxClearanceCertificateUpload', payload.taxClearanceCertificateUpload);
    return this.http.post<unknown>(`${this.base}/register`, form).pipe(
      map((resp) => this.mapSummary(this.extractDto(resp))),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  getMy(): Observable<OrganizationSummary> {
    return this.http.get<unknown>(`${this.base}/my`).pipe(
      map((resp) => this.mapSummaryWithBranches(this.extractDto(resp), resp)),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  updateMyOrganization(payload: UpdateMyOrganizationPayload): Observable<OrganizationSummary> {
    return this.http.put<unknown>(`${this.base}/my/update`, payload).pipe(
      map((resp) => this.mapSummaryWithBranches(this.extractDto(resp), resp)),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  listBranchesForAllocation(): Observable<BranchAllocationOption[]> {
    return this.getMy().pipe(map((org) => org.branches ?? []));
  }

  listBranches(): Observable<BranchDetail[]> {
    return this.http.get<unknown>(`${this.base}/branches`).pipe(
      map((resp) => this.mapBranchDetails(resp)),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  addBranch(payload: BranchFormPayload): Observable<BranchDetail[]> {
    return this.http.post<unknown>(`${this.base}/branch/add`, this.toBranchRequestBody(payload)).pipe(
      map((resp) => this.mapBranchDetails(resp)),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  getBranch(branchId: number): Observable<BranchDetail> {
    return this.http.get<unknown>(`${this.base}/branches/${branchId}`).pipe(
      map((resp) => this.mapSingleBranch(resp)),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  updateBranch(branchId: number, payload: BranchFormPayload): Observable<BranchDetail[]> {
    return this.http.put<unknown>(`${this.base}/branches/${branchId}`, this.toBranchRequestBody(payload)).pipe(
      map((resp) => this.mapBranchDetails(resp)),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  deleteBranch(branchId: number): Observable<BranchDetail[]> {
    return this.http.delete<unknown>(`${this.base}/branches/${branchId}`).pipe(
      map((resp) => this.mapBranchDetails(resp)),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  private toBranchRequestBody(payload: BranchFormPayload): Record<string, unknown> {
    return {
      branchName: payload.branchName.trim(),
      branchCode: payload.branchCode?.trim() || undefined,
      region: payload.region?.trim() || undefined,
      email: payload.email?.trim() || undefined,
      phoneNumber: payload.phoneNumber?.trim() || undefined,
      businessHours: payload.businessHours?.trim() || undefined,
      headOffice: payload.headOffice ?? false,
      active: payload.active ?? true,
      parentBranchId: payload.parentBranchId,
      depot: payload.depot ?? false,
    };
  }

  submitKyc(): Observable<OrganizationSummary> {
    return this.http.post<unknown>(`${this.base}/submit-kyc`, {}).pipe(
      map((resp) => this.mapSummary(this.extractDto(resp))),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  resendVerificationEmail(email: string): Observable<void> {
    const url = ldmsApiUrl('/ldms-user-management/v1/system/user/resend-verification-link');
    return this.http
      .post<unknown>(url, null, { params: { email } })
      .pipe(
        map(() => void 0),
        catchError((err) => throwError(() => this.toError(err))),
      );
  }

  private appendFormValue(form: FormData, key: string, value: string | number | boolean | undefined): void {
    if (value === undefined || value === null) {
      return;
    }
    const s = String(value).trim();
    if (s.length > 0) {
      form.append(key, s);
    }
  }

  private appendFormFile(form: FormData, key: string, file: File | undefined): void {
    if (file) {
      form.append(key, file, file.name);
    }
  }

  private mapOnboardingStatus(response: unknown): OnboardingStatus {
    const root = this.toObj(response);
    const data = this.toObj(root?.['data']) ?? root;
    const dto =
      this.toObj(data?.['onboardingStatusDto']) ??
      this.toObj(data?.['organizationDto']) ??
      data ??
      {};
    return {
      id: Number(dto['id'] ?? 0),
      name: String(dto['name'] ?? ''),
      kycStatus: String(dto['kycStatus'] ?? 'DRAFT'),
      verified: Boolean(dto['verified'] ?? dto['isVerified']),
      requiredApprovalStages: Math.max(1, Number(dto['requiredApprovalStages'] ?? 2)),
      lastRejectionReason: String(dto['lastRejectionReason'] ?? '').trim() || undefined,
    };
  }

  private mapSummaryWithBranches(dto: Record<string, unknown>, rawResponse: unknown): OrganizationSummary {
    const summary = this.mapSummary(dto);
    const root = this.toObj(rawResponse);
    const data = this.toObj(root?.['data']) ?? root;
    const orgDto = this.toObj(data?.['organizationDto']) ?? dto;
    const branchList = Array.isArray(orgDto['branchDtoList']) ? orgDto['branchDtoList'] : [];
    const branchDetails = branchList
      .map((b) => this.toObj(b))
      .filter((b): b is Record<string, unknown> => !!b)
      .map((b) => this.mapBranchDtoRecord(b))
      .filter((b) => b.id > 0);
    summary.headOfficeBranch = branchDetails.find((b) => b.headOffice);
    summary.branches = branchDetails.map((b) => ({
      id: b.id,
      label: this.formatBranchLabel({
        branchName: b.branchName,
        branchLevel: b.branchLevel,
        depot: b.depot,
      }),
      level: b.branchLevel,
      depot: b.depot,
      parentBranchId: b.parentBranchId,
      parentBranchName: b.parentBranchName,
    } satisfies BranchAllocationOption));
    return summary;
  }

  private formatBranchLabel(b: {
    branchName?: unknown;
    branchLevel?: unknown;
    depot?: unknown;
  }): string {
    const name = String(b.branchName ?? '').trim();
    const level = this.parseBranchLevel(b.branchLevel);
    const depot = Boolean(b.depot);
    if (level === 'SUB_BRANCH') {
      return depot ? `${name} (Depot)` : `${name} (Sub-branch)`;
    }
    return `${name} (Branch)`;
  }

  private parseBranchLevel(raw: unknown): 'BRANCH' | 'SUB_BRANCH' {
    return String(raw ?? 'BRANCH').toUpperCase() === 'SUB_BRANCH' ? 'SUB_BRANCH' : 'BRANCH';
  }

  private mapBranchDetails(response: unknown): BranchDetail[] {
    const root = this.toObj(response);
    const data = this.toObj(root?.['data']) ?? root;
    const orgDto = this.toObj(data?.['organizationDto']) ?? data;
    const branchList = Array.isArray(orgDto?.['branchDtoList']) ? orgDto['branchDtoList'] : [];
    return branchList
      .map((b) => this.toObj(b))
      .filter((b): b is Record<string, unknown> => !!b)
      .map((b) => ({
        id: Number(b['id'] ?? 0),
        branchName: String(b['branchName'] ?? '').trim(),
        branchCode: String(b['branchCode'] ?? '').trim() || undefined,
        branchLevel: this.parseBranchLevel(b['branchLevel']),
        parentBranchId: b['parentBranchId'] != null ? Number(b['parentBranchId']) : undefined,
        parentBranchName: String(b['parentBranchName'] ?? '').trim() || undefined,
        depot: Boolean(b['depot']),
        headOffice: Boolean(b['isHeadOffice'] ?? b['headOffice']),
        region: String(b['region'] ?? '').trim() || undefined,
        email: String(b['email'] ?? '').trim() || undefined,
        phoneNumber: String(b['phoneNumber'] ?? '').trim() || undefined,
        businessHours: String(b['businessHours'] ?? '').trim() || undefined,
        active: b['active'] !== false,
      } satisfies BranchDetail))
      .filter((b) => b.id > 0);
  }

  private mapSingleBranch(response: unknown): BranchDetail {
    const root = this.toObj(response);
    const data = this.toObj(root?.['data']) ?? root;
    const dto = this.toObj(data?.['branchDto']) ?? data;
    const mapped = this.mapBranchDtoRecord(dto ?? {});
    if (mapped.id <= 0) {
      throw new Error('Branch not found.');
    }
    return mapped;
  }

  private mapBranchDtoRecord(b: Record<string, unknown>): BranchDetail {
    return {
      id: Number(b['id'] ?? 0),
      branchName: String(b['branchName'] ?? '').trim(),
      branchCode: String(b['branchCode'] ?? '').trim() || undefined,
      branchLevel: this.parseBranchLevel(b['branchLevel']),
      parentBranchId: b['parentBranchId'] != null ? Number(b['parentBranchId']) : undefined,
      parentBranchName: String(b['parentBranchName'] ?? '').trim() || undefined,
      depot: Boolean(b['depot']),
      headOffice: Boolean(b['isHeadOffice'] ?? b['headOffice']),
      region: this.readOptionalString(b, 'region'),
      email: this.readOptionalString(b, 'email'),
      phoneNumber: this.readOptionalString(b, 'phoneNumber'),
      businessHours: this.readOptionalString(b, 'businessHours', 'business_hours'),
      active: b['active'] !== false,
    };
  }

  private mapSummary(dto: Record<string, unknown>): OrganizationSummary {
    const organizationClassification = this.readOrganizationClassification(dto);
    return {
      id: Number(dto['id'] ?? 0),
      name: String(dto['name'] ?? ''),
      email: String(dto['email'] ?? ''),
      phoneNumber: String(dto['phoneNumber'] ?? '').trim() || undefined,
      locationId: dto['locationId'] != null ? Number(dto['locationId']) : undefined,
      websiteUrl: String(dto['websiteUrl'] ?? '').trim() || undefined,
      organizationDescription: String(dto['organizationDescription'] ?? '').trim() || undefined,
      numberOfEmployees: dto['numberOfEmployees'] != null ? Number(dto['numberOfEmployees']) : undefined,
      annualRevenueEstimate:
        dto['annualRevenueEstimate'] != null ? Number(dto['annualRevenueEstimate']) : undefined,
      regionsServed: String(dto['regionsServed'] ?? '').trim() || undefined,
      businessHours: String(dto['businessHours'] ?? '').trim() || undefined,
      registrationNumber: String(dto['registrationNumber'] ?? '').trim() || undefined,
      taxNumber: String(dto['taxNumber'] ?? '').trim() || undefined,
      addressLine1: String(dto['addressLine1'] ?? '').trim() || undefined,
      addressLine2: String(dto['addressLine2'] ?? '').trim() || undefined,
      addressPostalCode: String(dto['addressPostalCode'] ?? '').trim() || undefined,
      addressSuburbId: dto['addressSuburbId'] != null ? Number(dto['addressSuburbId']) : undefined,
      addressCityId: dto['addressCityId'] != null ? Number(dto['addressCityId']) : undefined,
      addressCityName: String(dto['addressCityName'] ?? '').trim() || undefined,
      addressDistrictName: String(dto['addressDistrictName'] ?? '').trim() || undefined,
      addressProvinceName: String(dto['addressProvinceName'] ?? '').trim() || undefined,
      createdViaSignup: dto['createdViaSignup'] != null ? Boolean(dto['createdViaSignup']) : undefined,
      kycStatus: String(dto['kycStatus'] ?? 'DRAFT'),
      isVerified: Boolean(dto['isVerified']),
      organizationClassification,
      duplexMode: Boolean(dto['duplexMode']),
      standaloneMode: Boolean(dto['standaloneMode']),
      inventoryManagementEnabled: Boolean(dto['inventoryManagementEnabled']),
      crossDockingEnabled: Boolean(dto['crossDockingEnabled']),
      inventoryDataSource: this.readInventoryDataSource(dto),
      counterpartyEngagementMode: this.readCounterpartyEngagementMode(dto),
      fuelConsumptionEnabled: Boolean(dto['fuelConsumptionEnabled']),

      // KYC contact person
      contactPersonFirstName: this.readOptionalString(dto, 'contactPersonFirstName'),
      contactPersonLastName: this.readOptionalString(dto, 'contactPersonLastName'),
      contactPersonEmail: this.readOptionalString(dto, 'contactPersonEmail'),
      contactPersonPhoneNumber: this.readOptionalString(dto, 'contactPersonPhoneNumber'),
      contactPersonPosition: this.readOptionalString(dto, 'contactPersonPosition'),
      contactPersonGender: this.readOptionalString(dto, 'contactPersonGender'),
      contactPersonNationalIdNumber: this.readOptionalString(dto, 'contactPersonNationalIdNumber'),
      contactPersonNationalIdUploadId: dto['contactPersonNationalIdUploadId'] != null ? Number(dto['contactPersonNationalIdUploadId']) : undefined,
      contactPersonPassportNumber: this.readOptionalString(dto, 'contactPersonPassportNumber'),
      contactPersonPassportUploadId: dto['contactPersonPassportUploadId'] != null ? Number(dto['contactPersonPassportUploadId']) : undefined,
      contactPersonDateOfBirth: this.readOptionalString(dto, 'contactPersonDateOfBirth'),
      contactPersonUserId: dto['contactPersonUserId'] != null ? Number(dto['contactPersonUserId']) : undefined,
      representativeNationalIdNumber: this.readOptionalString(dto, 'representativeNationalIdNumber'),
      representativePassportNumber: this.readOptionalString(dto, 'representativePassportNumber'),

      // KYC document uploads
      registrationCertificateUploadId: dto['registrationCertificateUploadId'] != null ? Number(dto['registrationCertificateUploadId']) : undefined,
      taxClearanceCertificateUploadId: dto['taxClearanceCertificateUploadId'] != null ? Number(dto['taxClearanceCertificateUploadId']) : undefined,
      businessLicenseUploadId: dto['businessLicenseUploadId'] != null ? Number(dto['businessLicenseUploadId']) : undefined,
      proofOfAddressUploadId: dto['proofOfAddressUploadId'] != null ? Number(dto['proofOfAddressUploadId']) : undefined,
      industrySpecificLicenseUploadId: dto['industrySpecificLicenseUploadId'] != null ? Number(dto['industrySpecificLicenseUploadId']) : undefined,
      logoUploadId: dto['logoUploadId'] != null ? Number(dto['logoUploadId']) : undefined,

      // KYC audit trail
      submittedAt: this.readOptionalString(dto, 'submittedAt'),
      currentResubmissionCycle: dto['currentResubmissionCycle'] != null ? Number(dto['currentResubmissionCycle']) : undefined,
      lastRejectionReason: this.readOptionalString(dto, 'lastRejectionReason'),
      resubmissionCount: dto['resubmissionCount'] != null ? Number(dto['resubmissionCount']) : undefined,
    };
  }

  private readOrganizationClassification(
    dto: Record<string, unknown>,
  ): OrganizationClassification | undefined {
    const raw = dto['organizationClassification'];
    if (typeof raw === 'string' && raw.trim()) {
      return raw.trim().toUpperCase() as OrganizationClassification;
    }
    if (raw && typeof raw === 'object' && !Array.isArray(raw)) {
      const nested = String((raw as Record<string, unknown>)['name'] ?? '').trim();
      if (nested) {
        return nested.toUpperCase() as OrganizationClassification;
      }
    }
    return undefined;
  }

  private readInventoryDataSource(dto: Record<string, unknown>): 'INTERNAL' | 'EXTERNAL_API' | 'MANUAL_ACK' | undefined {
    const raw = String(dto['inventoryDataSource'] ?? '').trim().toUpperCase();
    if (raw === 'EXTERNAL_API' || raw === 'MANUAL_ACK' || raw === 'INTERNAL') {
      return raw as 'INTERNAL' | 'EXTERNAL_API' | 'MANUAL_ACK';
    }
    return undefined;
  }

  private readCounterpartyEngagementMode(
    dto: Record<string, unknown>,
  ): 'RECORD_ONLY' | 'PLATFORM_ORG' | undefined {
    const raw = String(dto['counterpartyEngagementMode'] ?? '').trim().toUpperCase();
    if (raw === 'RECORD_ONLY' || raw === 'PLATFORM_ORG') {
      return raw;
    }
    return undefined;
  }

  saveOperationalSettings(payload: OperationalSettingsPayload): Observable<OrganizationSummary> {
    return this.http.put<unknown>(`${this.base}/operational-settings`, payload).pipe(
      map((resp) => this.mapSummary(this.extractDto(resp))),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  listTradingPartners(): Observable<TradingPartner[]> {
    const url = ldmsServiceUrl('organization-management', 'organization', 'trading-partners');
    return this.http.get<unknown>(url).pipe(
      map((resp) => this.mapTradingPartnerList(resp)),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  createTradingPartner(payload: TradingPartnerPayload): Observable<TradingPartner> {
    const url = ldmsServiceUrl('organization-management', 'organization', 'trading-partners');
    return this.http.post<unknown>(url, payload).pipe(
      map((resp) => this.mapTradingPartner(this.extractTradingPartnerDto(resp))),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  updateTradingPartner(id: number, payload: TradingPartnerPayload): Observable<TradingPartner> {
    const url = ldmsServiceUrl('organization-management', 'organization', `trading-partners/${id}`);
    return this.http.put<unknown>(url, payload).pipe(
      map((resp) => this.mapTradingPartner(this.extractTradingPartnerDto(resp))),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  deleteTradingPartner(id: number): Observable<void> {
    const url = ldmsServiceUrl('organization-management', 'organization', `trading-partners/${id}`);
    return this.http.delete<unknown>(url).pipe(
      map(() => void 0),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  private mapTradingPartnerList(response: unknown): TradingPartner[] {
    const root = this.toObj(response);
    const data = this.toObj(root?.['data']) ?? root;
    const list = Array.isArray(data?.['tradingPartnerDtoList'])
      ? data['tradingPartnerDtoList']
      : Array.isArray(data?.['list'])
        ? data['list']
        : Array.isArray(data)
          ? data
          : [];
    return (list as unknown[])
      .map((item) => this.toObj(item))
      .filter((item): item is Record<string, unknown> => !!item)
      .map((item) => this.mapTradingPartner(item));
  }

  private extractTradingPartnerDto(response: unknown): Record<string, unknown> {
    const root = this.toObj(response);
    const data = this.toObj(root?.['data']) ?? root;
    return this.toObj(data?.['tradingPartnerDto']) ?? data ?? {};
  }

  private mapTradingPartner(dto: Record<string, unknown>): TradingPartner {
    return {
      id: Number(dto['id'] ?? 0),
      name: String(dto['name'] ?? '').trim(),
      email: this.readOptionalString(dto, 'email'),
      phoneNumber: this.readOptionalString(dto, 'phoneNumber'),
      role: (String(dto['role'] ?? 'OTHER').trim().toUpperCase() as TradingPartner['role']) || 'OTHER',
      notes: this.readOptionalString(dto, 'notes'),
      entityStatus: String(dto['entityStatus'] ?? 'ACTIVE'),
      createdAt: this.readOptionalString(dto, 'createdAt'),
    };
  }

  private extractDto(response: unknown): Record<string, unknown> {
    const root = this.toObj(response);
    if (!root) {
      return {};
    }
    const data = this.toObj(root['data']) ?? root;
    return this.toObj(data['organizationDto']) ?? data;
  }

  private toObj(value: unknown): Record<string, unknown> | null {
    return value && typeof value === 'object' && !Array.isArray(value)
      ? (value as Record<string, unknown>)
      : null;
  }

  private readOptionalString(dto: Record<string, unknown>, ...keys: string[]): string | undefined {
    for (const key of keys) {
      const value = String(dto[key] ?? '').trim();
      if (value) {
        return value;
      }
    }
    return undefined;
  }

  private toError(err: HttpErrorResponse): Error {
    const body = err.error as { message?: string; errorMessages?: string[] } | undefined;
    const msgs = body?.errorMessages;
    if (Array.isArray(msgs) && msgs.length) {
      return new Error(msgs.join(' '));
    }
    return new Error(body?.message ?? err.message ?? 'Request failed');
  }
}
