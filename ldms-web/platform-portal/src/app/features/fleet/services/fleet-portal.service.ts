import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import {
  isApiFailureEnvelope,
  readApiFailureMessage,
} from '../../../core/utils/api-paged-response.util';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import type { OrganizationPartnerMetadata } from '../../../shared/models/organization-metadata.model';
import {
  extractOrganizationDto,
  mapOrganizationPartnerMetadata,
} from '../../../shared/utils/map-organization-metadata.util';
import { RegisterTransporterPayload, TransporterPartnerRow } from '../models/fleet.model';
import { presentTransporterContract } from '../utils/transporter-contract.util';

@Injectable({ providedIn: 'root' })
export class FleetPortalService {
  private readonly base = ldmsServiceUrl('organization-management', 'organization', undefined, 'frontend');

  constructor(private readonly http: HttpClient) {}

  listPartners(): Observable<TransporterPartnerRow[]> {
    return this.http.get<unknown>(`${this.base}/transporters`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.extractOrganizationRows(resp).map((dto) => this.mapPartnerRow(dto));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** GET /transporters/{id} — full linked metadata for spotlight panels. */
  getPartnerMetadata(partnerId: number): Observable<OrganizationPartnerMetadata> {
    return this.http.get<unknown>(`${this.base}/transporters/${partnerId}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return mapOrganizationPartnerMetadata(extractOrganizationDto(resp));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  searchTransportCandidates(search: string): Observable<TransporterPartnerRow[]> {
    const q = search.trim();
    const url = q
      ? `${this.base}/transporters/candidates?search=${encodeURIComponent(q)}`
      : `${this.base}/transporters/candidates`;
    return this.http.get<unknown>(url).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.extractOrganizationRows(resp).map((dto) => this.mapPartnerRow(dto));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /transporters/register (multipart) — transport org + supplier link. */
  registerTransporter(payload: RegisterTransporterPayload): Observable<TransporterPartnerRow> {
    return this.http.post<unknown>(`${this.base}/transporters/register`, this.buildRegisterFormData(payload)).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingleOrganization(resp);
        if (!dto['id']) {
          throw new Error('Partner was created but the response did not include an organisation id.');
        }
        return this.mapPartnerRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  linkTransporter(transporterOrganizationId: number): Observable<void> {
    return this.http
      .post<unknown>(`${this.base}/transporters/link`, { transporterOrganizationId })
      .pipe(
        map((resp) => {
          this.assertSuccess(resp);
          return void 0;
        }),
        catchError((err) => throwError(() => this.toError(err))),
      );
  }

  private buildRegisterFormData(payload: RegisterTransporterPayload): FormData {
    const form = new FormData();
    this.appendFormValue(form, 'name', payload.name);
    this.appendFormValue(form, 'email', payload.email);
    this.appendFormValue(form, 'phoneNumber', payload.phoneNumber);
    this.appendFormValue(form, 'organizationClassification', 'TRANSPORT_COMPANY');
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
    this.appendFormFile(form, 'contactPersonNationalIdUpload', payload.contactPersonNationalIdUpload);
    this.appendFormFile(form, 'contactPersonPassportUpload', payload.contactPersonPassportUpload);
    this.appendFormValue(form, 'registrationNumber', payload.registrationNumber);
    this.appendFormValue(form, 'taxNumber', payload.taxNumber);
    this.appendFormValue(form, 'createdViaSignup', false);
    this.appendFormFile(form, 'taxClearanceCertificateUpload', payload.taxClearanceCertificateUpload);
    this.appendFormValue(form, 'addressLine1', payload.addressLine1);
    this.appendFormValue(form, 'addressLine2', payload.addressLine2);
    this.appendFormValue(form, 'postalCode', payload.postalCode);
    this.appendFormValue(form, 'suburbId', payload.suburbId);
    this.appendFormValue(form, 'contractStartDate', payload.contractStartDate);
    this.appendFormValue(form, 'contractEndDate', payload.contractEndDate);
    return form;
  }

  private appendFormValue(form: FormData, key: string, value: unknown): void {
    if (value == null || value === '') {
      return;
    }
    form.append(key, String(value));
  }

  private appendFormFile(form: FormData, key: string, file: File | undefined): void {
    if (file) {
      form.append(key, file, file.name);
    }
  }

  private extractSingleOrganization(response: unknown): Record<string, unknown> {
    const envelope = this.unwrapEnvelope(response);
    const one = this.toObj(envelope['organizationDto']);
    if (one) {
      return one;
    }
    const list = envelope['organizationDtoList'];
    if (Array.isArray(list) && list.length) {
      const first = this.toObj(list[0]);
      if (first) {
        return first;
      }
    }
    return envelope;
  }

  private mapPartnerRow(dto: Record<string, unknown>): TransporterPartnerRow {
    const name = String(dto['name'] ?? '').trim() || 'Unnamed partner';
    const verified = Boolean(dto['isVerified'] ?? dto['verified']);
    const kyc = String(dto['kycStatus'] ?? 'APPROVED');
    const contract = presentTransporterContract(dto);
    return {
      id: Number(dto['id'] ?? 0),
      name,
      email: String(dto['email'] ?? '').trim(),
      phoneNumber: String(dto['phoneNumber'] ?? '').trim(),
      verified,
      verifiedLabel: verified ? 'Verified' : 'Pending trust',
      kycStatusLabel: this.kycLabel(kyc),
      initials: this.initialsFromName(name),
      accentHue: 32 + (hash(name) % 55),
      linkedSinceLabel: contract.linkedSinceLabel,
      contractStartLabel: contract.startLabel,
      contractEndLabel: contract.endLabel,
      contractRangeLabel: contract.rangeLabel,
      contractStatus: contract.status,
      contractStatusLabel: contract.statusLabel,
      partnerKind: 'contracted',
    };
  }

  private kycLabel(status: string): string {
    const key = status.toUpperCase();
    const map: Record<string, string> = {
      APPROVED: 'Approved',
      DRAFT: 'Draft',
      SUBMITTED: 'Submitted',
      UNDER_REVIEW: 'Under review',
    };
    return map[key] ?? key.split('_').join(' ');
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
      if (err.status === 403) {
        return new Error('You do not have permission to manage transport partners.');
      }
      if (err.status === 0) {
        return new Error('Cannot reach the API gateway. Start ldms-api-gateway on port 8091.');
      }
      const serverMessage = parsed?.['message'];
      if (typeof serverMessage === 'string' && serverMessage.trim()) {
        return new Error(serverMessage.trim());
      }
      if (err.status === 500) {
        return new Error(
          'Transport partners API failed (HTTP 500). Rebuild and restart ldms-organization-management, then reload this page.',
        );
      }
      return new Error(err.message ?? 'Request failed');
    }
    return err;
  }
}

function hash(seed: string): number {
  let h = 0;
  for (let i = 0; i < seed.length; i += 1) {
    h = seed.charCodeAt(i) + ((h << 5) - h);
  }
  return Math.abs(h);
}
