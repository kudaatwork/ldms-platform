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
}

export interface OrganizationSummary {
  id: number;
  name: string;
  email: string;
  kycStatus: string;
  isVerified: boolean;
  organizationClassification?: OrganizationClassification;
  duplexMode?: boolean;
}

/** Public onboarding tracker (limited fields from GET onboarding-status). */
export interface OnboardingStatus {
  id: number;
  name: string;
  kycStatus: string;
  verified: boolean;
  requiredApprovalStages: number;
  lastRejectionReason?: string;
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
      map((resp) => this.mapSummary(this.extractDto(resp))),
      catchError((err) => throwError(() => this.toError(err))),
    );
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

  private mapSummary(dto: Record<string, unknown>): OrganizationSummary {
    const organizationClassification = this.readOrganizationClassification(dto);
    return {
      id: Number(dto['id'] ?? 0),
      name: String(dto['name'] ?? ''),
      email: String(dto['email'] ?? ''),
      kycStatus: String(dto['kycStatus'] ?? 'DRAFT'),
      isVerified: Boolean(dto['isVerified']),
      organizationClassification,
      duplexMode: Boolean(dto['duplexMode']),
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

  private toError(err: HttpErrorResponse): Error {
    const body = err.error as { message?: string; errorMessages?: string[] } | undefined;
    const msgs = body?.errorMessages;
    if (Array.isArray(msgs) && msgs.length) {
      return new Error(msgs.join(' '));
    }
    return new Error(body?.message ?? err.message ?? 'Request failed');
  }
}
