import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import {
  isApiFailureEnvelope,
  readApiFailureMessage,
} from '../../../core/utils/api-paged-response.util';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import type { OrganizationType } from '../../../core/models/auth.model';
import type { OrganizationPartnerMetadata } from '../../../shared/models/organization-metadata.model';
import {
  extractOrganizationDto,
  mapOrganizationPartnerMetadata,
} from '../../../shared/utils/map-organization-metadata.util';
import {
  CustomerEditDetail,
  CustomerKycStatus,
  CustomerListRow,
  IndustrySelectOption,
  RegisterCustomerPayload,
} from '../models/customer.model';

/** LDMS organization-management frontend customer APIs for supplier workspaces. */
@Injectable({ providedIn: 'root' })
export class CustomersPortalService {
  private readonly base = ldmsServiceUrl('organization-management', 'organization', undefined, 'frontend');

  constructor(private readonly http: HttpClient) {}

  /** GET /customers — linked buyer organisations for the signed-in supplier. */
  listCustomers(): Observable<CustomerListRow[]> {
    return this.http.get<unknown>(`${this.base}/customers`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.extractOrganizationRows(resp).map((dto) => this.mapCustomerRow(dto));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /**
   * GET /industries — platform-wide catalogue (configured in admin portal, shared by all organisations).
   */
  listPlatformIndustries(): Observable<IndustrySelectOption[]> {
    return this.http.get<unknown>(`${this.base}/industries`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.extractIndustryOptions(resp);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** GET /customers/{id} — linked customer profile for edit. */
  getCustomer(customerId: number): Observable<CustomerEditDetail> {
    return this.http.get<unknown>(`${this.base}/customers/${customerId}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingleOrganization(resp);
        return this.mapCustomerEditDetail(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** GET /customers/{id} — full linked metadata for spotlight panels. */
  getCustomerMetadata(customerId: number): Observable<OrganizationPartnerMetadata> {
    return this.http.get<unknown>(`${this.base}/customers/${customerId}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return mapOrganizationPartnerMetadata(extractOrganizationDto(resp));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /customers/register (multipart) — full customer org + supplier link. */
  registerCustomer(payload: RegisterCustomerPayload): Observable<CustomerListRow> {
    return this.http.post<unknown>(`${this.base}/customers/register`, this.buildRegisterFormData(payload)).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingleOrganization(resp);
        if (!dto['id']) {
          throw new Error('Customer was created but the response did not include an organisation id.');
        }
        return this.mapCustomerRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** PUT /customers/{id} (multipart) — update linked customer. */
  updateCustomer(customerId: number, payload: RegisterCustomerPayload): Observable<CustomerListRow> {
    return this.http.put<unknown>(`${this.base}/customers/${customerId}`, this.buildRegisterFormData(payload)).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingleOrganization(resp);
        if (!dto['id']) {
          throw new Error('Customer was updated but the response did not include an organisation id.');
        }
        return this.mapCustomerRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** DELETE /customers/{id} — unlink from supplier (soft-delete when no other links). */
  deleteCustomer(customerId: number): Observable<string> {
    return this.http.delete<unknown>(`${this.base}/customers/${customerId}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const parsed = this.toObj(resp);
        const message = parsed?.['message'];
        return typeof message === 'string' && message.trim() ? message.trim() : 'Customer removed.';
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /customers/{id}/retry-onboarding — resend contact credentials and org verification emails. */
  retryOnboardingEmails(customerId: number): Observable<string> {
    return this.http.post<unknown>(`${this.base}/customers/${customerId}/retry-onboarding`, {}).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const parsed = this.toObj(resp);
        const message = parsed?.['message'];
        return typeof message === 'string' && message.trim()
          ? message.trim()
          : 'Onboarding emails were queued.';
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  private buildRegisterFormData(payload: RegisterCustomerPayload): FormData {
    const form = new FormData();
    this.appendFormValue(form, 'name', payload.name);
    this.appendFormValue(form, 'email', payload.email);
    this.appendFormValue(form, 'phoneNumber', payload.phoneNumber);
    this.appendFormValue(form, 'organizationClassification', 'CUSTOMER');
    this.appendFormValue(form, 'organizationType', payload.organizationType);
    this.appendFormValue(form, 'industryId', payload.industryId);
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
    this.appendFormValue(form, 'contactPersonNationalIdUploadId', payload.contactPersonNationalIdUploadId);
    this.appendFormFile(form, 'contactPersonNationalIdUpload', payload.contactPersonNationalIdUpload);
    this.appendFormValue(form, 'contactPersonPassportUploadId', payload.contactPersonPassportUploadId);
    this.appendFormFile(form, 'contactPersonPassportUpload', payload.contactPersonPassportUpload);
    this.appendFormValue(form, 'registrationNumber', payload.registrationNumber);
    this.appendFormValue(form, 'taxNumber', payload.taxNumber);
    this.appendFormValue(form, 'createdViaSignup', false);
    this.appendFormValue(form, 'taxClearanceCertificateUploadId', payload.taxClearanceCertificateUploadId);
    this.appendFormFile(form, 'taxClearanceCertificateUpload', payload.taxClearanceCertificateUpload);
    this.appendFormValue(form, 'addressLine1', payload.addressLine1);
    this.appendFormValue(form, 'addressLine2', payload.addressLine2);
    this.appendFormValue(form, 'postalCode', payload.postalCode);
    this.appendFormValue(form, 'suburbId', payload.suburbId);
    this.appendFormValue(form, 'cityId', payload.cityId);
    this.appendFormValue(form, 'locationId', payload.locationId);
    return form;
  }

  private mapCustomerEditDetail(dto: Record<string, unknown>): CustomerEditDetail {
    const id = Number(dto['id'] ?? 0);
    if (!id) {
      throw new Error('Customer profile is missing an organisation id.');
    }
    const orgType = String(dto['organizationType'] ?? 'PRIVATE').toUpperCase() as OrganizationType;
    return {
      id,
      name: String(dto['name'] ?? '').trim(),
      email: String(dto['email'] ?? '').trim(),
      phoneNumber: String(dto['phoneNumber'] ?? '').trim(),
      organizationType: orgType,
      industryId: this.toPositiveId(dto['industryId']),
      contactPersonFirstName: String(dto['contactPersonFirstName'] ?? '').trim(),
      contactPersonLastName: String(dto['contactPersonLastName'] ?? '').trim(),
      contactPersonEmail: String(dto['contactPersonEmail'] ?? '').trim(),
      contactPersonPhoneNumber: String(dto['contactPersonPhoneNumber'] ?? '').trim(),
      contactPersonGender: String(dto['contactPersonGender'] ?? '').trim(),
      contactPersonDateOfBirth: String(dto['contactPersonDateOfBirth'] ?? '').trim(),
      contactPersonNationalIdNumber: String(dto['contactPersonNationalIdNumber'] ?? '').trim() || undefined,
      contactPersonPassportNumber: String(dto['contactPersonPassportNumber'] ?? '').trim() || undefined,
      registrationNumber: String(dto['registrationNumber'] ?? '').trim() || undefined,
      taxNumber: String(dto['taxNumber'] ?? '').trim() || undefined,
      taxClearanceCertificateUploadId: this.toPositiveId(dto['taxClearanceCertificateUploadId']),
      contactPersonNationalIdUploadId: this.toPositiveId(dto['contactPersonNationalIdUploadId']),
      contactPersonPassportUploadId: this.toPositiveId(dto['contactPersonPassportUploadId']),
      locationId: this.toPositiveId(dto['locationId']),
      addressLine1: String(dto['addressLine1'] ?? '').trim() || undefined,
      addressLine2: String(dto['addressLine2'] ?? '').trim() || undefined,
      postalCode: String(dto['addressPostalCode'] ?? '').trim() || undefined,
      suburbId: this.toPositiveId(dto['addressSuburbId']),
      cityId: this.toPositiveId(dto['addressCityId']),
      cityName: String(dto['addressCityName'] ?? '').trim() || undefined,
      districtId: this.toPositiveId(dto['addressDistrictId']),
      provinceId: this.toPositiveId(dto['addressProvinceId']),
      countryId: this.toPositiveId(dto['addressCountryId']),
    };
  }

  private toPositiveId(value: unknown): number | undefined {
    const n = Number(value);
    return Number.isFinite(n) && n > 0 ? n : undefined;
  }

  private mapCustomerRow(dto: Record<string, unknown>): CustomerListRow {
    const name = String(dto['name'] ?? '').trim() || 'Unnamed customer';
    const kycStatus = String(dto['kycStatus'] ?? 'DRAFT') as CustomerKycStatus;
    const pres = this.kycPresentation(kycStatus);
    const verified = Boolean(dto['isVerified'] ?? dto['verified']);
    return {
      id: Number(dto['id'] ?? 0),
      name,
      email: String(dto['email'] ?? '').trim(),
      phoneNumber: String(dto['phoneNumber'] ?? '').trim(),
      kycStatus,
      kycStatusLabel: pres.label,
      kycTone: pres.tone,
      verified,
      verifiedLabel: verified ? 'Verified' : 'Unverified',
      initials: this.initialsFromName(name),
      accentHue: this.hueFromString(name),
      createdAtLabel: this.formatDate(dto['createdAt']),
    };
  }

  private kycPresentation(status: CustomerKycStatus): {
    label: string;
    tone: CustomerListRow['kycTone'];
  } {
    const key = String(status ?? 'DRAFT').toUpperCase();
    const map: Record<string, { label: string; tone: CustomerListRow['kycTone'] }> = {
      DRAFT: { label: 'Draft', tone: 'muted' },
      SUBMITTED: { label: 'Submitted', tone: 'warn' },
      UNDER_REVIEW: { label: 'Under review', tone: 'warn' },
      APPROVED: { label: 'Approved', tone: 'success' },
      REJECTED: { label: 'Rejected', tone: 'danger' },
      RESUBMISSION_REQUIRED: { label: 'Resubmission', tone: 'warn' },
    };
    return (
      map[key] ?? {
        label: key
          .split('_')
          .map((p) => p.charAt(0) + p.slice(1).toLowerCase())
          .join(' '),
        tone: 'muted',
      }
    );
  }

  private initialsFromName(name: string): string {
    const parts = name.split(/\s+/).filter(Boolean);
    if (!parts.length) {
      return '?';
    }
    if (parts.length === 1) {
      return parts[0].slice(0, 2).toUpperCase();
    }
    return `${parts[0][0] ?? ''}${parts[parts.length - 1][0] ?? ''}`.toUpperCase();
  }

  /** Blue–indigo range so atlas avatars match the customers page chrome. */
  private hueFromString(seed: string): number {
    let hash = 0;
    for (let i = 0; i < seed.length; i += 1) {
      hash = seed.charCodeAt(i) + ((hash << 5) - hash);
    }
    return 205 + (Math.abs(hash) % 45);
  }

  private formatDate(value: unknown): string {
    if (value == null || value === '') {
      return '—';
    }
    const d = new Date(String(value));
    if (Number.isNaN(d.getTime())) {
      return '—';
    }
    return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
  }

  private assertSuccess(response: unknown): void {
    if (isApiFailureEnvelope(response)) {
      throw new Error(readApiFailureMessage(response, 'Request failed'));
    }
  }

  private unwrapEnvelope(response: unknown): Record<string, unknown> {
    const root = this.toObj(response);
    if (!root) {
      return {};
    }
    return this.toObj(root['data']) ?? this.toObj(root['body']) ?? this.toObj(root['payload']) ?? root;
  }

  private extractOrganizationRows(response: unknown): Record<string, unknown>[] {
    const envelope = this.unwrapEnvelope(response);
    const list = envelope['organizationDtoList'];
    if (Array.isArray(list)) {
      return list.filter((r): r is Record<string, unknown> => !!this.toObj(r));
    }
    const one = this.toObj(envelope['organizationDto']);
    return one ? [one] : [];
  }

  private extractSingleOrganization(response: unknown): Record<string, unknown> {
    const rows = this.extractOrganizationRows(response);
    return rows[0] ?? {};
  }

  private extractIndustryOptions(response: unknown): IndustrySelectOption[] {
    const root = this.toObj(response);
    if (!root) {
      return [];
    }
    const envelope = this.toObj(root['data']) ?? this.toObj(root['body']) ?? root;
    const list = envelope['industryUsageDtoList'] ?? root['industryUsageDtoList'];
    if (!Array.isArray(list)) {
      return [];
    }
    return list
      .filter((r): r is Record<string, unknown> => !!this.toObj(r))
      .filter((dto) => Boolean(dto['active'] ?? true))
      .map((dto) => {
        const name = String(dto['name'] ?? '').trim();
        const code = String(dto['industryCode'] ?? '').trim();
        return {
          id: Number(dto['id'] ?? 0),
          label: code ? `${name} (${code})` : name,
        };
      })
      .filter((o) => o.id > 0 && o.label.length > 0)
      .sort((a, b) => a.label.localeCompare(b.label));
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

  private toObj(value: unknown): Record<string, unknown> | null {
    return value && typeof value === 'object' && !Array.isArray(value)
      ? (value as Record<string, unknown>)
      : null;
  }

  private toError(err: HttpErrorResponse | Error): Error {
    if (err instanceof HttpErrorResponse) {
      const body = err.error;
      if (isApiFailureEnvelope(body)) {
        return new Error(readApiFailureMessage(body, 'Request failed'));
      }
      const parsed = this.toObj(body);
      const msgs = parsed?.['errorMessages'];
      if (Array.isArray(msgs) && msgs.length) {
        return new Error(msgs.map((m) => String(m)).join(' '));
      }
      const message = parsed?.['message'];
      if (typeof message === 'string' && message.trim()) {
        return new Error(message.trim());
      }
      if (err.status === 403) {
        return new Error(
          'You do not have permission to manage customers. Ensure your role includes REGISTER_CUSTOMER.',
        );
      }
      if (err.status === 401) {
        return new Error('Not signed in. Log in again to continue.');
      }
      if (err.status === 0) {
        return new Error('Cannot reach the API gateway. Start ldms-api-gateway on port 8091.');
      }
      return new Error(err.message ?? 'Request failed');
    }
    return err;
  }
}
