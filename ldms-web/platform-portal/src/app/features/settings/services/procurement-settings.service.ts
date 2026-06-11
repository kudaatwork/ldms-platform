import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map, switchMap } from 'rxjs';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import { ProcurementApprovalPolicy } from '../../inventory/models/inventory.model';

@Injectable({ providedIn: 'root' })
export class ProcurementSettingsService {
  private readonly settingsBase = ldmsServiceUrl('inventory-management', 'procurement-settings', undefined, 'frontend');

  constructor(private readonly http: HttpClient) {}

  /** GET /procurement-settings/approval-policy — current policy for the caller's org. */
  fetchApprovalPolicy(): Observable<ProcurementApprovalPolicy> {
    return this.http.get<unknown>(`${this.settingsBase}/approval-policy`).pipe(
      map((resp) => this.mapPolicy(resp)),
    );
  }

  /** PUT /procurement-settings/approval-policy — save the org's procurement approval policy. */
  updateApprovalPolicy(defaultRequiredApprovalStages: number): Observable<ProcurementApprovalPolicy> {
    return this.http
      .put<unknown>(`${this.settingsBase}/approval-policy`, { defaultRequiredApprovalStages })
      .pipe(switchMap(() => this.fetchApprovalPolicy()));
  }

  private mapPolicy(resp: unknown): ProcurementApprovalPolicy {
    const root = this.toObj(resp) ?? {};
    const data = this.toObj(root['data']) ?? this.toObj(root['body']) ?? root;
    const dto =
      this.toObj(data['procurementApprovalPolicyDto']) ??
      this.toObj(data['approvalPolicyDto']) ??
      data;
    return {
      defaultRequiredApprovalStages: Number(dto['defaultRequiredApprovalStages'] ?? 1),
      minAllowedStages: Number(dto['minAllowedStages'] ?? 1),
      maxAllowedStages: Number(dto['maxAllowedStages'] ?? 3),
    };
  }

  private toObj(value: unknown): Record<string, unknown> | null {
    return value && typeof value === 'object' && !Array.isArray(value)
      ? (value as Record<string, unknown>)
      : null;
  }
}
