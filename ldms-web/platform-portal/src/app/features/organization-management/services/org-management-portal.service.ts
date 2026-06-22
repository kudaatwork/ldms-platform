import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { BranchDetail, BranchFormPayload, OrganizationService } from '../../../core/services/organization.service';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import { FleetPortalService } from '../../fleet/services/fleet-portal.service';
import { TransporterPartnerRow } from '../../fleet/models/fleet.model';
import { AgentRow, BranchTableQuery, CreateAgentPayload, ImportActionResponse, PagedBranches } from '../models/org-management.model';
import { extractPagedResult } from '../../../core/utils/api-paged-response.util';
import { LxExportFormat, mapExportHttpError } from '../../../shared/utils/lx-export.util';

@Injectable({ providedIn: 'root' })
export class OrgManagementPortalService {
  private readonly orgBase = ldmsServiceUrl('organization-management', 'organization', undefined, 'frontend');

  constructor(
    private readonly http: HttpClient,
    private readonly organizationService: OrganizationService,
    private readonly fleetPortal: FleetPortalService,
  ) {}

  listBranches(): Observable<BranchDetail[]> {
    return this.organizationService.listBranches();
  }

  addBranch(payload: BranchFormPayload): Observable<BranchDetail[]> {
    return this.organizationService.addBranch(payload);
  }

  getBranch(branchId: number): Observable<BranchDetail> {
    return this.organizationService.getBranch(branchId);
  }

  updateBranch(branchId: number, payload: BranchFormPayload): Observable<BranchDetail[]> {
    return this.organizationService.updateBranch(branchId, payload);
  }

  deleteBranch(branchId: number): Observable<BranchDetail[]> {
    return this.organizationService.deleteBranch(branchId);
  }

  queryBranchesPage(q: BranchTableQuery): Observable<PagedBranches> {
    const body: Record<string, unknown> = {
      page: q.page,
      size: q.size,
      searchValue: q.searchQuery?.trim() ?? '',
    };
    if (q.branchName?.trim()) {
      body['branchName'] = q.branchName.trim();
    }
    if (q.region?.trim()) {
      body['region'] = q.region.trim();
    }
    if (q.branchLevel) {
      body['branchLevel'] = q.branchLevel;
    }
    if (q.depot === true || q.depot === false) {
      body['depot'] = q.depot;
    }
    if (q.parentBranchId && Number(q.parentBranchId) > 0) {
      body['parentBranchId'] = Number(q.parentBranchId);
    }
    if (q.active === true || q.active === false) {
      body['active'] = q.active;
    }
    return this.http.post<unknown>(`${this.orgBase}/branches/find-by-multiple-filters`, body).pipe(
      map((resp) => this.mapBranchPaged(resp)),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  exportBranches(format: LxExportFormat, filters: BranchTableQuery): Observable<Blob> {
    const body = this.buildBranchExportBody(filters);
    return this.http
      .post(`${this.orgBase}/branches/export?format=${format}`, body, { responseType: 'blob' })
      .pipe(catchError((err: HttpErrorResponse) => mapExportHttpError(err)));
  }

  importBranchesCsv(file: File): Observable<ImportActionResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<unknown>(`${this.orgBase}/branches/import-csv`, formData).pipe(
      map((resp) => this.toImportActionResponse(resp)),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  getBranchSampleCsv(scope: 'top-level' | 'sub-level'): { blob: Blob; filename: string } {
    if (scope === 'sub-level') {
      return this.buildSampleCsv(
        {
          headers: [
            'BRANCH NAME',
            'BRANCH CODE',
            'REGION',
            'EMAIL',
            'PHONE NUMBER',
            'PARENT BRANCH ID',
            'DEPOT',
            'ACTIVE',
            'BUSINESS HOURS',
          ],
          rows: [
            ['Eastgate Depot', 'EG-DEP', 'Harare', 'eg@example.co.zw', '+263771234567', '1', 'true', 'true', 'Mon-Sat 07:00-18:00'],
            ['Mutare Sub-branch', 'MUT-001', 'Manicaland', 'mutare@example.co.zw', '+263292345678', '1', 'false', 'true', 'Mon-Fri 08:00-17:00'],
          ],
        },
        'sub-branches-sample.csv',
      );
    }
    return this.buildSampleCsv(
      {
        headers: [
          'BRANCH NAME',
          'BRANCH CODE',
          'REGION',
          'EMAIL',
          'PHONE NUMBER',
          'HEAD OFFICE',
          'ACTIVE',
          'BUSINESS HOURS',
        ],
        rows: [
          ['Harare Head Office', 'HRE-001', 'Mashonaland East', 'info@example.co.zw', '+263771234567', 'true', 'true', 'Mon-Fri 08:00-17:00'],
          ['Bulawayo Branch', 'BLW-001', 'Matabeleland North', 'byo@example.co.zw', '+263292345678', 'false', 'true', 'Mon-Sat 08:00-16:00'],
        ],
      },
      'branches-sample.csv',
    );
  }

  listAgents(): Observable<AgentRow[]> {
    return this.http.get<unknown>(`${this.orgBase}/agents`).pipe(
      map((resp) => this.mapAgents(resp)),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  createAgent(payload: CreateAgentPayload): Observable<AgentRow> {
    return this.http.post<unknown>(`${this.orgBase}/agents`, payload).pipe(
      map((resp) => this.extractSingleAgent(resp)),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  updateAgent(agentId: number, payload: Partial<CreateAgentPayload>): Observable<AgentRow> {
    return this.http.put<unknown>(`${this.orgBase}/agents/${agentId}`, payload).pipe(
      map((resp) => this.extractSingleAgent(resp)),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  deleteAgent(agentId: number): Observable<void> {
    return this.http.delete<unknown>(`${this.orgBase}/agents/${agentId}`).pipe(
      map(() => void 0),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  importAgentsCsv(file: File): Observable<ImportActionResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<unknown>(`${this.orgBase}/agents/import-csv`, formData).pipe(
      map((resp) => this.toImportActionResponse(resp)),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  listTransporters(): Observable<TransporterPartnerRow[]> {
    return this.fleetPortal.listPartners();
  }

  private mapAgents(response: unknown): AgentRow[] {
    const root = this.toObj(response);
    const data = this.toObj(root?.['data']) ?? root;
    const orgDto = this.toObj(data?.['organizationDto']) ?? data;
    const list = Array.isArray(orgDto?.['agentDtoList']) ? orgDto['agentDtoList'] : [];
    return list
      .map((item) => this.toObj(item))
      .filter((item): item is Record<string, unknown> => !!item)
      .map((dto) => this.mapAgentRow(dto))
      .filter((row) => row.id > 0);
  }

  private extractSingleAgent(response: unknown): AgentRow {
    const root = this.toObj(response);
    const data = this.toObj(root?.['data']) ?? root;
    const dto = this.toObj(data?.['agentDto']) ?? data;
    return this.mapAgentRow(dto ?? {});
  }

  private mapAgentRow(dto: Record<string, unknown>): AgentRow {
    const firstName = String(dto['firstName'] ?? '').trim();
    const lastName = String(dto['lastName'] ?? '').trim();
    const kindRaw = String(dto['agentKind'] ?? 'INDIVIDUAL').trim().toUpperCase();
    const contact = this.toObj(dto['contact']);
    return {
      id: Number(dto['id'] ?? 0),
      firstName,
      lastName,
      fullName: [firstName, lastName].filter(Boolean).join(' ') || '—',
      email: String(dto['email'] ?? contact?.['email'] ?? '').trim(),
      phoneNumber: String(dto['phoneNumber'] ?? contact?.['phoneNumber'] ?? '').trim(),
      agentKind: kindRaw === 'ORGANIZATION' ? 'ORGANIZATION' : 'INDIVIDUAL',
      agentType: String(dto['agentType'] ?? '').trim() || undefined,
      role: String(dto['role'] ?? '').trim() || undefined,
      branchId: dto['branchId'] != null ? Number(dto['branchId']) : undefined,
      assignedRegion: String(dto['assignedRegion'] ?? '').trim() || undefined,
      active: dto['active'] !== false,
    };
  }

  private mapBranchPaged(response: unknown): PagedBranches {
    const { rows: raw, totalElements } = extractPagedResult(response, 'branchDtoPage');
    const rows = raw
      .map((item) => this.toObj(item))
      .filter((item): item is Record<string, unknown> => !!item)
      .map((dto) => this.mapBranchRow(dto))
      .filter((row) => row.id > 0);
    return { rows, totalElements: totalElements > 0 ? totalElements : rows.length };
  }

  private mapBranchRow(dto: Record<string, unknown>): BranchDetail {
    const levelRaw = String(dto['branchLevel'] ?? 'BRANCH').toUpperCase();
    return {
      id: Number(dto['id'] ?? 0),
      branchName: String(dto['branchName'] ?? '').trim(),
      branchCode: String(dto['branchCode'] ?? '').trim() || undefined,
      branchLevel: levelRaw === 'SUB_BRANCH' ? 'SUB_BRANCH' : 'BRANCH',
      parentBranchId: dto['parentBranchId'] != null ? Number(dto['parentBranchId']) : undefined,
      parentBranchName: String(dto['parentBranchName'] ?? '').trim() || undefined,
      depot: Boolean(dto['depot']),
      headOffice: Boolean(dto['headOffice'] ?? dto['isHeadOffice']),
      region: this.readOptionalString(dto, 'region'),
      email: this.readOptionalString(dto, 'email'),
      phoneNumber: this.readOptionalString(dto, 'phoneNumber'),
      businessHours: this.readOptionalString(dto, 'businessHours', 'business_hours'),
      active: dto['active'] !== false,
    };
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

  private buildBranchExportBody(filters: BranchTableQuery): Record<string, unknown> {
    return {
      page: 0,
      size: 0,
      searchValue: filters.searchQuery?.trim() ?? '',
      branchName: filters.branchName?.trim() || undefined,
      region: filters.region?.trim() || undefined,
      branchLevel: filters.branchLevel,
      depot: filters.depot === true || filters.depot === false ? filters.depot : undefined,
      parentBranchId: filters.parentBranchId && Number(filters.parentBranchId) > 0 ? Number(filters.parentBranchId) : undefined,
      active: filters.active === true || filters.active === false ? filters.active : undefined,
    };
  }

  private toImportActionResponse(response: unknown): ImportActionResponse {
    const root = this.toObj(response);
    const data = this.toObj(root?.['data']) ?? root;
    const success = data?.['success'] !== false && data?.['statusCode'] !== 400 && data?.['statusCode'] !== 500;
    const message = String(data?.['message'] ?? root?.['message'] ?? '').trim() || undefined;
    return { ok: success, message };
  }

  private buildSampleCsv(template: { headers: string[]; rows: string[][] }, filename: string): { blob: Blob; filename: string } {
    const escape = (value: string): string => {
      const v = value.replace(/"/g, '""');
      return /[",\n\r]/.test(v) ? `"${v}"` : v;
    };
    const lines = [template.headers.map(escape).join(',')];
    for (const row of template.rows) {
      lines.push(row.map((cell) => escape(String(cell ?? ''))).join(','));
    }
    return { blob: new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8' }), filename };
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
