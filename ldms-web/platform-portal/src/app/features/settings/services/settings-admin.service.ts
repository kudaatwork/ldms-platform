import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map, switchMap } from 'rxjs';
import { extractPagedResult } from '../../../core/utils/api-paged-response.util';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';

export interface KycApprovalPolicy {
  defaultRequiredApprovalStages: number;
  minAllowedStages: number;
  maxAllowedStages: number;
}

/** Organisation row for KYC approver settings (subset of admin KYC queue list). */
export interface KycSettingsOrganizationRow {
  id: number;
  name: string;
  applicant: string;
  classificationLabel: string;
  kycRequiredApprovalStages: number | null;
  effectiveKycRequiredApprovalStages: number | null;
}

export interface PagedKycSettingsOrganizations {
  rows: KycSettingsOrganizationRow[];
  totalElements: number;
}

@Injectable({ providedIn: 'root' })
export class SettingsAdminService {
  private readonly orgBase = ldmsServiceUrl('organization-management', 'organization', undefined, 'backoffice');

  constructor(private readonly http: HttpClient) {}

  fetchKycApprovalPolicy(): Observable<KycApprovalPolicy> {
    return this.http.get<unknown>(`${this.orgBase}/kyc/policy`).pipe(
      map((resp) => {
        const body = this.unwrap(resp);
        const dto = (body['kycApprovalPolicyDto'] ??
          body['kyc_approval_policy_dto'] ??
          body['data'] ??
          body) as Record<string, unknown>;
        const nested = this.toObj(dto['kycApprovalPolicyDto'] ?? dto['kyc_approval_policy_dto']);
        const source = nested ?? dto;
        return {
          defaultRequiredApprovalStages: Number(
            source['defaultRequiredApprovalStages'] ?? source['default_required_approval_stages'] ?? 2,
          ),
          minAllowedStages: Number(source['minAllowedStages'] ?? source['min_allowed_stages'] ?? 1),
          maxAllowedStages: Number(source['maxAllowedStages'] ?? source['max_allowed_stages'] ?? 2),
        };
      }),
    );
  }

  updateKycApprovalPolicy(defaultRequiredApprovalStages: number): Observable<KycApprovalPolicy> {
    return this.http.put<unknown>(`${this.orgBase}/kyc/policy`, { defaultRequiredApprovalStages }).pipe(
      switchMap(() => this.fetchKycApprovalPolicy()),
    );
  }

  updateOrganizationKycStages(organizationId: number, kycRequiredApprovalStages: number | null): Observable<void> {
    return this.http
      .put<unknown>(`${this.orgBase}/${organizationId}/kyc/required-stages`, { kycRequiredApprovalStages })
      .pipe(map(() => undefined));
  }

  /** KYC pipeline organisations for per-org stage overrides on Settings → KYC approvers. */
  queryKycOrganizationsForSettings(page: number, size: number): Observable<PagedKycSettingsOrganizations> {
    const body = {
      page,
      size,
      searchValue: '',
      kycQueueOnly: true,
    };
    return this.http.post<unknown>(`${this.orgBase}/find-by-multiple-filters`, body).pipe(
      map((resp) => {
        const pageResult = extractPagedResult(resp, 'organizationDtoPage');
        const rows = pageResult.rows
          .map((raw) => this.mapKycSettingsOrganizationRow(raw))
          .filter((row): row is KycSettingsOrganizationRow => row.id > 0);
        return { rows, totalElements: pageResult.totalElements };
      }),
    );
  }

  private mapKycSettingsOrganizationRow(raw: unknown): KycSettingsOrganizationRow {
    const dto =
      raw && typeof raw === 'object' && !Array.isArray(raw) ? (raw as Record<string, unknown>) : {};
    const id = Number(dto['id'] ?? 0);
    const name = String(dto['name'] ?? '').trim();
    const classification = String(dto['organizationClassification'] ?? 'SUPPLIER');
    return {
      id,
      name,
      applicant: name,
      classificationLabel: this.formatClassificationLabel(classification),
      kycRequiredApprovalStages: this.readOptionalInt(dto, 'kycRequiredApprovalStages', 'kyc_required_approval_stages'),
      effectiveKycRequiredApprovalStages: this.readOptionalInt(
        dto,
        'effectiveKycRequiredApprovalStages',
        'effective_kyc_required_approval_stages',
      ),
    };
  }

  private formatClassificationLabel(slug: string): string {
    return slug
      .split('_')
      .map((part) => part.charAt(0) + part.slice(1).toLowerCase())
      .join(' ');
  }

  private readOptionalInt(dto: Record<string, unknown>, ...keys: string[]): number | null {
    for (const key of keys) {
      const v = dto[key];
      if (v === null || v === undefined || v === '') {
        continue;
      }
      const n = Number(v);
      if (Number.isFinite(n)) {
        return n;
      }
    }
    return null;
  }

  private unwrap(resp: unknown): Record<string, unknown> {
    if (resp && typeof resp === 'object' && !Array.isArray(resp)) {
      return resp as Record<string, unknown>;
    }
    return {};
  }

  private toObj(value: unknown): Record<string, unknown> | null {
    return value && typeof value === 'object' && !Array.isArray(value) ? (value as Record<string, unknown>) : null;
  }
}
