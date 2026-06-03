import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map, switchMap } from 'rxjs';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';

export interface KycApprovalPolicy {
  defaultRequiredApprovalStages: number;
  minAllowedStages: number;
  maxAllowedStages: number;
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
