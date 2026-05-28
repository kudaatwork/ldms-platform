import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, forkJoin, map, of, switchMap, throwError } from 'rxjs';
import { mapExportHttpError } from '@shared/utils/lx-export.util';
import { apiBaseUrl, ldmsServiceUrl } from '../../../core/utils/api-url.util';
import { extractPagedResult } from '../../../core/utils/api-paged-response.util';
import type {
  KycApplicationDetail,
  KycApplicationDocument,
  KycApproverAssignment,
  KycDecisionPayload,
  KycQueueRow,
  KycRejectPayload,
  KycStatus,
  OrganizationClassification,
  OrganizationLinkRow,
  OrganizationProfileDetail,
  OrganizationType,
  RegisterOrganizationPayload,
  UpdateOrganizationPayload,
} from '../models/organization.model';
import type { AgentPayload } from '../models/agent.model';
import type { BranchPayload } from '../models/branch.model';
import type { IndustryPayload } from '../models/industry.model';
import type {
  AgentListRow,
  BranchListRow,
  IndustryUsageRow,
  PagedAgents,
  PagedBranches,
  PagedIndustries,
} from '../models/organization-directory.model';
import {
  ORG_CLASSIFICATIONS,
  classificationLabel,
  kycStatusPresentation,
  organizationTypeLabel,
  resolveKycStage,
} from '../models/organization.model';

export interface PagedOrganizations {
  rows: KycQueueRow[];
  totalElements: number;
}

/** Aggregated KYC pipeline counts for shell badges and dashboard KPIs. */
export interface KycQueueSummary {
  totalInQueue: number;
  stage1Count: number;
  stage2Count: number;
  recentApplications: KycQueueRow[];
}

/** Query sent to `find-by-multiple-filters` for organization tables. */
export interface OrganizationTableQuery {
  page: number;
  size: number;
  searchQuery: string;
  columnFilters: {
    name?: string;
    email?: string;
    classificationLabel?: string;
    statusLabel?: string;
  };
  /** Fixed classification for classification-specific views. */
  organizationClassification?: OrganizationClassification | '';
  /** When set, limits to organisations linked to this industry. */
  industryId?: number | '';
  /** When true, limits to default KYC queue statuses unless `kycStatus` is set in filters. */
  kycQueueOnly?: boolean;
  /**
   * When true, only admin-registered orgs and signup orgs that completed KYC (excludes pending KYC pipeline).
   */
  organizationDirectoryOnly?: boolean;
  /** When set, server filters to organisations assigned to this KYC reviewer. */
  kycAssignedToUsername?: string;
}

export interface BranchTableQuery {
  page: number;
  size: number;
  searchQuery: string;
  branchName?: string;
  /** Resolved to {@code organizationId} on the server when possible. */
  organizationName?: string;
  organizationId?: number | '';
}

export interface AgentTableQuery {
  page: number;
  size: number;
  searchQuery: string;
  /** Resolved to {@code organizationId} on the server when possible. */
  organizationName?: string;
  organizationId?: number | '';
  agentKind?: string;
  role?: string;
}

export interface IndustryTableQuery {
  page: number;
  size: number;
  searchQuery: string;
  columnFilters: {
    name?: string;
    industryCode?: string;
  };
}

/** Result of create/update industry (aligned with locations `LocationActionResponse`). */
export interface IndustryActionResponse {
  ok: boolean;
  message?: string;
  row?: IndustryUsageRow;
}

export interface OrgDirectoryActionResponse {
  ok: boolean;
  message?: string;
}

export interface OrgImportActionResponse {
  ok: boolean;
  message?: string;
}

export type OrgDirectoryEntity = 'branches' | 'agents' | 'industries';

const SAMPLE_CSV: Record<OrgDirectoryEntity, { headers: string[]; rows: string[][]; description: string }> = {
  branches: {
    description: 'Organisation ID, branch name, and contact fields. One row per branch.',
    headers: ['ORGANIZATION ID', 'BRANCH NAME', 'BRANCH CODE', 'REGION', 'EMAIL', 'PHONE NUMBER', 'HEAD OFFICE', 'ACTIVE', 'BUSINESS HOURS', 'LOCATION ID'],
    rows: [['1', 'Harare Head Office', 'HO-HRE', 'Harare', 'hq@example.co.zw', '+263771234567', 'true', 'true', 'Mon–Fri 08:00–17:00', '']],
  },
  agents: {
    description: 'Organisation ID, agent kind (INDIVIDUAL or ORGANIZATION), and contact fields.',
    headers: ['ORGANIZATION ID', 'AGENT KIND', 'FIRST NAME', 'LAST NAME', 'EMAIL', 'PHONE NUMBER', 'AGENT TYPE', 'ROLE', 'BRANCH ID', 'ACTIVE'],
    rows: [['1', 'INDIVIDUAL', 'Tendai', 'Moyo', 'tendai@example.co.zw', '+263771111111', 'Clearing', 'Representative', '', 'true']],
  },
  industries: {
    description: 'Industry name and optional sector metadata. Names must be unique.',
    headers: ['NAME', 'INDUSTRY CODE', 'DESCRIPTION', 'REGULATORY BODY NAME', 'REGULATORY BODY CONTACT', 'COMPLIANCE REQUIREMENTS', 'ACTIVE'],
    rows: [['Logistics & freight', 'LOGISTICS', 'Transport and warehousing', '', '', '', 'true']],
  },
};

/** Result returned by CSV import endpoints (branches, agents, industries). */
export interface ImportActionResponse {
  ok: boolean;
  message?: string;
}

export interface BranchActionResponse {
  ok: boolean;
  message?: string;
  row?: BranchListRow;
}

export interface AgentActionResponse {
  ok: boolean;
  message?: string;
  row?: AgentListRow;
}

type SampleCsvTemplate = { headers: string[]; rows: string[][] };

const BRANCH_SAMPLE: SampleCsvTemplate = {
  headers: ['ORGANIZATION_ID', 'BRANCH_NAME', 'BRANCH_CODE', 'REGION', 'EMAIL', 'PHONE_NUMBER', 'HEAD_OFFICE', 'ACTIVE', 'BUSINESS_HOURS'],
  rows: [
    ['1', 'Harare Head Office', 'HRE-001', 'Mashonaland East', 'info@acme.co.zw', '+263771234567', 'true', 'true', 'Mon-Fri 08:00-17:00'],
    ['2', 'Bulawayo Branch', 'BLW-001', 'Matabeleland North', 'byo@acme.co.zw', '+263292345678', 'false', 'true', 'Mon-Sat 08:00-16:00'],
  ],
};

const AGENT_SAMPLE: SampleCsvTemplate = {
  headers: ['ORGANIZATION_ID', 'AGENT_KIND', 'FIRST_NAME', 'LAST_NAME', 'ROLE', 'AGENT_TYPE', 'EMAIL', 'PHONE_NUMBER', 'ACTIVE'],
  rows: [
    ['1', 'INDIVIDUAL', 'John', 'Doe', 'Clearing Agent', 'PRIMARY', 'jdoe@acme.co.zw', '+263771234567', 'true'],
    ['2', 'ORGANIZATION', 'Jane', 'Smith', 'Transport Rep', 'SECONDARY', 'jsmith@logistics.co.zw', '+263292345678', 'true'],
  ],
};

const INDUSTRY_SAMPLE: SampleCsvTemplate = {
  headers: ['NAME', 'INDUSTRY_CODE', 'DESCRIPTION', 'REGULATORY_BODY_NAME', 'REGULATORY_BODY_CONTACT_INFO', 'COMPLIANCE_REQUIREMENTS', 'ACTIVE'],
  rows: [
    ['Logistics & Freight', 'LOGISTICS', 'Road freight and logistics services', 'Ministry of Transport', 'transport@gov.zw', 'Operating licence required', 'true'],
    ['Agriculture', 'AGRIC', 'Crop and livestock production', 'Ministry of Agriculture', 'agric@gov.zw', 'Agro-dealer permit required', 'true'],
  ],
};

@Injectable({ providedIn: 'root' })
export class OrganizationsAdminService {
  /** Same as {@link LocationsService}: {@link apiBaseUrl} → API gateway ({@code http://localhost:8091} in dev). */
  private readonly base = apiBaseUrl();

  constructor(private readonly http: HttpClient) {}

  /** Admin org APIs live on {@code backoffice} (JWT); platform self-service uses {@code frontend}. */
  private url(operation: string): string {
    return ldmsServiceUrl('organization-management', 'organization', operation, 'backoffice');
  }

  /**
   * Server-paged organization list (same transport as locations `queryTablePage`).
   */
  queryTablePage(q: OrganizationTableQuery): Observable<PagedOrganizations> {
    const body = this.buildFilterBody(q);
    return this.http.post<unknown>(this.url('find-by-multiple-filters'), body).pipe(
      map((resp) => this.mapPaged(resp)),
      catchError((err) => throwError(() => this.toOrgListError(err))),
    );
  }

  /** Counts for nav badge, notification bell, and KYC queue hero stats. */
  fetchKycQueueSummary(): Observable<KycQueueSummary> {
    const base: OrganizationTableQuery = {
      page: 0,
      size: 1,
      searchQuery: '',
      columnFilters: {},
      kycQueueOnly: true,
    };
    return forkJoin({
      total: this.queryTablePage(base),
      stage1: this.queryTablePage({
        ...base,
        columnFilters: { statusLabel: 'STAGE_1_REVIEW' },
      }),
      stage2: this.queryTablePage({
        ...base,
        columnFilters: { statusLabel: 'STAGE_2_REVIEW' },
      }),
      recent: this.queryTablePage({ ...base, size: 8 }),
    }).pipe(
      map(({ total, stage1, stage2, recent }) => ({
        totalInQueue: total.totalElements,
        stage1Count: stage1.totalElements,
        stage2Count: stage2.totalElements,
        recentApplications: recent.rows,
      })),
    );
  }

  exportOrganizations(format: 'csv' | 'xlsx' | 'pdf', filters: OrganizationTableQuery): Observable<Blob> {
    const body = this.buildOrganizationExportBody(filters);
    return this.http
      .post(`${this.url('export')}?format=${format}`, body, { responseType: 'blob' })
      .pipe(catchError((err: HttpErrorResponse) => mapExportHttpError(err)));
  }

  /** @deprecated Use {@link queryTablePage} — retained for KYC queue callers migrating to filters. */
  queryKycQueue(
    page = 0,
    size = 50,
    status?: string,
    classification?: string,
    assignedToUsername?: string,
  ): Observable<PagedOrganizations> {
    const columnFilters: OrganizationTableQuery['columnFilters'] = {};
    let kycQueueOnly = false;
    if (!status || status.trim() === '') {
      kycQueueOnly = true;
    } else if (status !== 'ALL') {
      columnFilters.statusLabel = status;
    }
    return this.queryTablePage({
      page,
      size,
      searchQuery: '',
      columnFilters,
      organizationClassification: (classification as OrganizationClassification) || '',
      kycQueueOnly,
      kycAssignedToUsername: assignedToUsername?.trim() || undefined,
    });
  }

  queryAllOrganizations(page = 0, size = 25, q?: Partial<OrganizationTableQuery>): Observable<PagedOrganizations> {
    return this.queryTablePage({
      page,
      size,
      searchQuery: q?.searchQuery ?? '',
      columnFilters: q?.columnFilters ?? {},
      organizationClassification: q?.organizationClassification ?? '',
      industryId: q?.industryId ?? '',
      kycQueueOnly: false,
      organizationDirectoryOnly: q?.organizationDirectoryOnly ?? true,
    });
  }

  /** Server-paged industries table with usage statistics. */
  queryIndustriesPage(q: IndustryTableQuery): Observable<PagedIndustries> {
    const cf = q.columnFilters ?? {};
    const body: Record<string, unknown> = {
      page: q.page,
      size: q.size,
      searchValue: q.searchQuery.trim(),
      name: cf.name?.trim() ?? '',
      industryCode: cf.industryCode?.trim() ?? '',
    };
    return this.http.post<unknown>(this.url('industries/find-by-multiple-filters'), body).pipe(
      map((resp) => this.mapIndustryPaged(resp)),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** Full list for dropdowns (register organisation). Prefer {@link queryIndustriesPage} for tables. */
  queryIndustriesWithUsage(): Observable<IndustryUsageRow[]> {
    return this.http.get<unknown>(this.url('industries')).pipe(
      map((resp) => this.extractIndustryUsage(resp)),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  getIndustry(id: number): Observable<IndustryUsageRow> {
    return this.http.get<unknown>(this.url(`industries/${id}`)).pipe(
      map((resp) => this.extractIndustryDto(resp)),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  createIndustry(payload: IndustryPayload): Observable<IndustryActionResponse> {
    return this.http.post<unknown>(this.url('industries'), payload).pipe(
      map((resp) => this.toIndustryActionResponse(resp, 'Industry created.')),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  updateIndustry(id: number, payload: IndustryPayload): Observable<IndustryActionResponse> {
    return this.http.put<unknown>(this.url(`industries/${id}`), payload).pipe(
      map((resp) => this.toIndustryActionResponse(resp, 'Industry updated.')),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  deleteIndustry(id: number): Observable<void> {
    return this.http.delete<unknown>(this.url(`industries/${id}`)).pipe(
      map(() => void 0),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  queryBranchesPage(q: BranchTableQuery): Observable<PagedBranches> {
    return this.resolveBranchFiltersForRequest(q).pipe(
      switchMap((resolved) => {
        const body: Record<string, unknown> = {
          page: resolved.page,
          size: resolved.size,
          searchValue: resolved.searchQuery?.trim() ?? '',
        };
        if (resolved.branchName?.trim()) {
          body['branchName'] = resolved.branchName.trim();
        }
        if (resolved.organizationId && Number(resolved.organizationId) > 0) {
          body['organizationId'] = Number(resolved.organizationId);
        }
        return this.http.post<unknown>(this.url('branches/find-by-multiple-filters'), body).pipe(
          map((resp) => this.mapBranchPaged(resp)),
        );
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  queryAgentsPage(q: AgentTableQuery): Observable<PagedAgents> {
    return this.resolveAgentFiltersForRequest(q).pipe(
      switchMap((resolved) => {
        const body: Record<string, unknown> = {
          page: resolved.page,
          size: resolved.size,
          searchValue: resolved.searchQuery?.trim() ?? '',
        };
        if (resolved.organizationId && Number(resolved.organizationId) > 0) {
          body['organizationId'] = Number(resolved.organizationId);
        }
        const kind = this.normalizeAgentKindFilter(resolved.agentKind);
        if (kind) {
          body['agentKind'] = kind;
        }
        return this.http.post<unknown>(this.url('agents/find-by-multiple-filters'), body).pipe(
          map((resp) => this.mapAgentPaged(resp)),
        );
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  private resolveBranchFiltersForRequest(q: BranchTableQuery): Observable<BranchTableQuery> {
    const orgName = q.organizationName?.trim();
    const resolveOrgId$ =
      q.organizationId && Number(q.organizationId) > 0
        ? of(Number(q.organizationId))
        : orgName
          ? this.findOrganizationIdByName(orgName)
          : of(null);
    return resolveOrgId$.pipe(
      map((organizationId) => ({
        ...q,
        searchQuery: this.buildDirectorySearchValue(q.searchQuery, orgName && organizationId ? orgName : ''),
        organizationId: organizationId && organizationId > 0 ? organizationId : q.organizationId,
      })),
    );
  }

  private resolveAgentFiltersForRequest(q: AgentTableQuery): Observable<AgentTableQuery> {
    const orgName = q.organizationName?.trim();
    const resolveOrgId$ =
      q.organizationId && Number(q.organizationId) > 0
        ? of(Number(q.organizationId))
        : orgName
          ? this.findOrganizationIdByName(orgName)
          : of(null);
    return resolveOrgId$.pipe(
      map((organizationId) => ({
        ...q,
        searchQuery: this.buildDirectorySearchValue(q.searchQuery, orgName && organizationId ? orgName : ''),
        organizationId: organizationId && organizationId > 0 ? organizationId : q.organizationId,
        agentKind: this.normalizeAgentKindFilter(q.agentKind) || q.agentKind,
      })),
    );
  }

  /** Looks up organisation id by exact or prefix name match (directory list). */
  private findOrganizationIdByName(name: string): Observable<number | null> {
    const trimmed = name.trim();
    if (!trimmed) {
      return of(null);
    }
    return this.http
      .post<unknown>(this.url('find-by-multiple-filters'), {
        page: 0,
        size: 5,
        searchValue: '',
        name: trimmed,
        organizationDirectoryOnly: true,
      })
      .pipe(
        map((resp) => {
          const rows = this.extractOrganizationRows(resp);
          const lower = trimmed.toLowerCase();
          const exact = rows.find((r) => String(r['name'] ?? '').trim().toLowerCase() === lower);
          const pick = exact ?? rows[0];
          const id = Number(pick?.['id'] ?? 0);
          return Number.isFinite(id) && id > 0 ? id : null;
        }),
        catchError(() => of(null)),
      );
  }

  private buildDirectorySearchValue(searchQuery: string, ...omitTerms: string[]): string {
    let value = searchQuery.trim();
    for (const term of omitTerms) {
      const t = term.trim();
      if (!t) {
        continue;
      }
      value = value
        .replace(new RegExp(`\\b${this.escapeRegExp(t)}\\b`, 'gi'), '')
        .replace(/\s+/g, ' ')
        .trim();
    }
    return value;
  }

  private escapeRegExp(value: string): string {
    return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }

  private normalizeAgentKindFilter(raw: string | undefined): string {
    const t = String(raw ?? '').trim().toUpperCase();
    if (t === 'INDIVIDUAL' || t === 'ORGANIZATION') {
      return t;
    }
    return '';
  }

  getOrganization(id: number): Observable<KycApplicationDetail> {
    return this.http.get<unknown>(this.url(String(id))).pipe(
      map((resp) => this.mapDetail(this.extractSingle(resp), id)),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** GET `/{id}` mapped for admin organisation shell (branches/agents/customers/transporters nested lists). */
  getOrganizationProfile(id: number): Observable<OrganizationProfileDetail> {
    return this.http.get<unknown>(this.url(String(id))).pipe(
      map((resp) => this.mapOrganizationProfileDetail(this.extractSingle(resp), id)),
      catchError((err: HttpErrorResponse) => throwError(() => this.toError(err))),
    );
  }

  /**
   * PUT multipart `/{id}` — aligns with backend {@link UpdateOrganizationRequest}.
   */
  provisionContactPersonUser(id: number): Observable<void> {
    return this.http.post<unknown>(this.url(`${id}/provision-contact-person`), {}).pipe(
      map((resp) => {
        this.assertOrganizationMutationSuccess(resp);
      }),
      map(() => void 0),
      catchError((err: HttpErrorResponse) => {
        if (err.status === 404) {
          return throwError(
            () =>
              new Error(
                'Provision contact person endpoint not found (404). Rebuild and restart ldms-organization-management ' +
                  '(port 8087) and ldms-user-management (8086), then ensure the API gateway (8091) is running.',
              ),
          );
        }
        return throwError(() => this.toError(err));
      }),
    );
  }

  updateOrganization(id: number, payload: UpdateOrganizationPayload): Observable<void> {
    const form = new FormData();
    this.appendUpdateOrganizationFields(form, payload);
    return this.http.put<unknown>(this.url(String(id)), form).pipe(
      map((resp) => {
        this.assertOrganizationMutationSuccess(resp);
      }),
      map(() => void 0),
      catchError((err: HttpErrorResponse) => throwError(() => this.toError(err))),
    );
  }

  /** DELETE `/{id}` (soft-delete on server). */
  deleteOrganization(id: number): Observable<void> {
    return this.http.delete<unknown>(this.url(String(id))).pipe(
      map((resp) => {
        this.assertOrganizationMutationSuccess(resp);
      }),
      map(() => void 0),
      catchError((err: HttpErrorResponse) => throwError(() => this.toError(err))),
    );
  }

  register(payload: RegisterOrganizationPayload): Observable<KycQueueRow> {
    const form = new FormData();
    this.appendFormValue(form, 'name', payload.name);
    this.appendFormValue(form, 'email', payload.email);
    this.appendFormValue(form, 'phoneNumber', payload.phoneNumber);
    this.appendFormValue(form, 'organizationClassification', payload.organizationClassification);
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
    this.appendFormValue(form, 'createdViaSignup', payload.createdViaSignup ?? false);
    this.appendFormValue(form, 'taxClearanceCertificateUploadId', payload.taxClearanceCertificateUploadId);
    this.appendFormFile(form, 'taxClearanceCertificateUpload', payload.taxClearanceCertificateUpload);
    return this.http.post<unknown>(this.url('register'), form).pipe(
      map((resp) => this.mapRow(this.extractSingle(resp))),
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

  private appendUpdateOrganizationFields(form: FormData, payload: UpdateOrganizationPayload): void {
    this.appendFormValue(form, 'name', payload.name);
    this.appendFormValue(form, 'email', payload.email);
    this.appendFormValue(form, 'phoneNumber', payload.phoneNumber);
    if (payload.organizationType) {
      form.append('organizationType', payload.organizationType);
    }
    if (payload.industryId != null && Number.isFinite(payload.industryId) && payload.industryId > 0) {
      form.append('industryId', String(Math.trunc(payload.industryId)));
    }
    this.appendFormValue(form, 'registrationNumber', payload.registrationNumber);
    this.appendFormValue(form, 'taxNumber', payload.taxNumber);
    this.appendFormValue(form, 'contactPersonFirstName', payload.contactPersonFirstName);
    this.appendFormValue(form, 'contactPersonLastName', payload.contactPersonLastName);
    this.appendFormValue(form, 'contactPersonEmail', payload.contactPersonEmail);
    this.appendFormValue(form, 'contactPersonPhoneNumber', payload.contactPersonPhoneNumber);
    this.appendFormValue(form, 'websiteUrl', payload.websiteUrl);
    this.appendFormValue(form, 'organizationDescription', payload.organizationDescription);
  }

  private assertOrganizationMutationSuccess(resp: unknown): void {
    const root = this.toObj(this.parseJson(resp));
    if (!root) {
      return;
    }
    if (this.isApiFailureEnvelope(root)) {
      throw new Error(this.readApiFailureMessage(root, 'Organisation request failed'));
    }
  }

  stage1Approve(id: number, payload: KycDecisionPayload): Observable<void> {
    return this.kycPost(id, 'stage1/approve', payload);
  }

  stage1Reject(id: number, payload: KycRejectPayload): Observable<void> {
    return this.kycPost(id, 'stage1/reject', payload);
  }

  stage2Approve(id: number, payload: KycDecisionPayload): Observable<void> {
    return this.kycPost(id, 'stage2/approve', payload);
  }

  stage2Reject(id: number, payload: KycRejectPayload): Observable<void> {
    return this.kycPost(id, 'stage2/reject', payload);
  }

  allowResubmission(id: number, payload: KycDecisionPayload): Observable<void> {
    return this.kycPost(id, 'allow-resubmission', payload);
  }

  private buildOrganizationExportBody(q: OrganizationTableQuery): Record<string, unknown> {
    const body = this.buildFilterBody({ ...q, page: 0, size: 1_000_000 });
    body['page'] = 0;
    body['size'] = 1_000_000;
    return body;
  }

  private buildFilterBody(q: OrganizationTableQuery): Record<string, unknown> {
    const cf = q.columnFilters ?? {};
    const name = cf.name?.trim() ?? '';
    const email = cf.email?.trim() ?? '';
    const classificationLabel = cf.classificationLabel?.trim() ?? '';
    const statusLabel = cf.statusLabel?.trim() ?? '';
    const body: Record<string, unknown> = {
      page: q.page,
      size: q.size,
      searchValue: q.searchQuery.trim(),
    };
    if (name) {
      body['name'] = name;
    }
    if (email) {
      body['email'] = email;
    }

    const fixedClassification = q.organizationClassification?.trim();
    if (fixedClassification) {
      body['organizationClassification'] = fixedClassification;
    } else {
      const resolved = this.resolveClassificationFilter(classificationLabel);
      if (resolved) {
        body['organizationClassification'] = resolved;
      }
    }

    const kycStatus = this.resolveKycStatusFilter(statusLabel);
    if (kycStatus) {
      body['kycStatus'] = kycStatus;
    }
    if (q.kycQueueOnly) {
      body['kycQueueOnly'] = true;
    }
    if (q.industryId) {
      body['industryId'] = q.industryId;
    }
    if (q.kycAssignedToUsername?.trim()) {
      body['kycAssignedToUsername'] = q.kycAssignedToUsername.trim();
    }
    if (q.organizationDirectoryOnly) {
      body['organizationDirectoryOnly'] = true;
    }

    return body;
  }

  private resolveClassificationFilter(input: string): OrganizationClassification | '' {
    if (!input) {
      return '';
    }
    const upper = input.toUpperCase().replace(/\s+/g, '_');
    const bySlug = ORG_CLASSIFICATIONS.find((c) => c.slug === upper);
    if (bySlug) {
      return bySlug.slug;
    }
    const byLabel = ORG_CLASSIFICATIONS.find((c) => c.label.toLowerCase() === input.toLowerCase());
    return byLabel?.slug ?? '';
  }

  private resolveKycStatusFilter(input: string): KycStatus | '' {
    if (!input) {
      return '';
    }
    const upper = input.toUpperCase().replace(/\s+/g, '_');
    const statuses: KycStatus[] = [
      'DRAFT',
      'SUBMITTED',
      'STAGE_1_REVIEW',
      'STAGE_2_REVIEW',
      'APPROVED',
      'REJECTED',
      'RESUBMITTED',
    ];
    if (statuses.includes(upper as KycStatus)) {
      return upper as KycStatus;
    }
    const match = statuses.find((s) => kycStatusPresentation(s).label.toLowerCase() === input.toLowerCase());
    return match ?? '';
  }

  private kycPost(id: number, path: string, body: object): Observable<void> {
    return this.http.post<unknown>(this.url(`${id}/kyc/${path}`), body).pipe(
      map(() => void 0),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  private mapPaged(resp: unknown): PagedOrganizations {
    const root = this.toObj(this.parseJson(resp));
    if (this.isApiFailureEnvelope(root)) {
      throw new Error(this.readApiFailureMessage(root, 'Failed to load organisations'));
    }

    const fallbackDtos = this.extractOrganizationRows(resp);
    const { rows: raw, totalElements } = extractPagedResult(resp, 'organizationDtoPage');
    const sourceRows = raw.length > 0 ? raw : fallbackDtos;
    const rows = sourceRows
      .filter((r): r is Record<string, unknown> => !!this.toObj(r))
      .map((dto) => this.mapRow(dto));

    const total =
      totalElements > 0
        ? totalElements
        : fallbackDtos.length > 0
          ? fallbackDtos.length
          : rows.length;

    return { rows, totalElements: total };
  }

  private isApiFailureEnvelope(root: Record<string, unknown> | null): boolean {
    if (!root) {
      return false;
    }
    const candidates = [root, this.toObj(root['data'])].filter(Boolean) as Record<string, unknown>[];
    for (const body of candidates) {
      if (body['success'] === false || body['isSuccess'] === false) {
        return true;
      }
      const statusCode = Number(body['statusCode']);
      if (Number.isFinite(statusCode) && statusCode >= 400) {
        return true;
      }
    }
    return false;
  }

  private readApiFailureMessage(root: Record<string, unknown> | null, fallback: string): string {
    if (!root) {
      return fallback;
    }
    const candidates = [root, this.toObj(root['data'])].filter(Boolean) as Record<string, unknown>[];
    for (const body of candidates) {
      const messages = body['errorMessages'];
      if (Array.isArray(messages) && messages.length > 0) {
        return messages.map((m) => String(m)).join(' ');
      }
      if (typeof body['message'] === 'string' && body['message'].trim()) {
        return body['message'].trim();
      }
    }
    return fallback;
  }

  /** Fallback when page JSON shape differs from Spring Data defaults. */
  private extractOrganizationRows(resp: unknown): Record<string, unknown>[] {
    const root = this.toObj(this.parseJson(resp));
    if (!root) {
      return [];
    }
    const data = this.toObj(root['data']) ?? root;
    const page = this.toObj(data['organizationDtoPage']) ?? this.toObj(root['organizationDtoPage']);
    if (page && Array.isArray(page['content'])) {
      return (page['content'] as unknown[]).filter((r): r is Record<string, unknown> => !!this.toObj(r));
    }
    const list = data['organizationDtoList'] ?? root['organizationDtoList'];
    if (Array.isArray(list)) {
      return list.filter((r): r is Record<string, unknown> => !!this.toObj(r));
    }
    const one = this.toObj(data['organizationDto']) ?? this.toObj(root['organizationDto']);
    return one ? [one] : [];
  }

  private mapIndustryPaged(resp: unknown): PagedIndustries {
    const { rows: raw, totalElements } = extractPagedResult(resp, 'industryUsageDtoPage');
    const rows = raw
      .filter((r): r is Record<string, unknown> => !!this.toObj(r))
      .map((dto) => this.mapIndustryUsageDto(dto));
    return { rows, totalElements: totalElements > 0 ? totalElements : rows.length };
  }

  private extractIndustryUsage(resp: unknown): IndustryUsageRow[] {
    const root = this.toObj(this.parseJson(resp));
    if (!root) {
      return [];
    }
    const data = this.toObj(root['data']) ?? root;
    const list = data['industryUsageDtoList'];
    if (!Array.isArray(list)) {
      return [];
    }
    return list
      .filter((r): r is Record<string, unknown> => !!this.toObj(r))
      .map((dto) => this.mapIndustryUsageDto(dto));
  }

  private mapIndustryUsageDto(dto: Record<string, unknown>): IndustryUsageRow {
    const active = Boolean(dto['active'] ?? true);
    return {
      id: Number(dto['id'] ?? 0),
      name: String(dto['name'] ?? ''),
      industryCode: String(dto['industryCode'] ?? ''),
      description: String(dto['description'] ?? ''),
      regulatoryBodyName: String(dto['regulatoryBodyName'] ?? ''),
      regulatoryBodyContactInfo: String(dto['regulatoryBodyContactInfo'] ?? ''),
      complianceRequirements: String(dto['complianceRequirements'] ?? ''),
      active,
      statusLabel: active ? 'Active' : 'Inactive',
      statusCss: active ? 'active' : 'inactive',
      organizationCount: Number(dto['organizationCount'] ?? 0),
      verifiedOrganizationCount: Number(dto['verifiedOrganizationCount'] ?? 0),
      linkedOrganizationNames: Array.isArray(dto['linkedOrganizationNames'])
        ? (dto['linkedOrganizationNames'] as unknown[]).map((n) => String(n))
        : [],
    };
  }

  private extractIndustryDto(resp: unknown): IndustryUsageRow {
    const root = this.toObj(this.parseJson(resp));
    if (!root) {
      throw new Error('Empty response');
    }
    const data = this.toObj(root['data']) ?? root;
    const dto = this.toObj(data['industryDto']) ?? data;
    return {
      id: Number(dto['id'] ?? 0),
      name: String(dto['name'] ?? ''),
      industryCode: String(dto['industryCode'] ?? ''),
      description: String(dto['description'] ?? ''),
      regulatoryBodyName: String(dto['regulatoryBodyName'] ?? ''),
      regulatoryBodyContactInfo: String(dto['regulatoryBodyContactInfo'] ?? ''),
      complianceRequirements: String(dto['complianceRequirements'] ?? ''),
      active: Boolean(dto['active'] ?? true),
      statusLabel: Boolean(dto['active'] ?? true) ? 'Active' : 'Inactive',
      statusCss: Boolean(dto['active'] ?? true) ? 'active' : 'inactive',
      organizationCount: 0,
      verifiedOrganizationCount: 0,
      linkedOrganizationNames: [],
    };
  }

  private mapBranchPaged(resp: unknown): PagedBranches {
    const { rows: raw, totalElements } = extractPagedResult(resp, 'branchDtoPage');
    const rows = raw
      .filter((r): r is Record<string, unknown> => !!this.toObj(r))
      .map((dto) => this.mapBranchRow(dto));
    return { rows, totalElements: totalElements > 0 ? totalElements : rows.length };
  }

  private mapAgentPaged(resp: unknown): PagedAgents {
    const { rows: raw, totalElements } = extractPagedResult(resp, 'agentDtoPage');
    const rows = raw
      .filter((r): r is Record<string, unknown> => !!this.toObj(r))
      .map((dto) => this.mapAgentRow(dto));
    return { rows, totalElements: totalElements > 0 ? totalElements : rows.length };
  }

  private mapBranchRow(dto: Record<string, unknown>): BranchListRow {
    return {
      id: Number(dto['id'] ?? 0),
      branchName: String(dto['branchName'] ?? ''),
      branchCode: String(dto['branchCode'] ?? ''),
      organizationId: Number(dto['organizationId'] ?? 0),
      organizationName: String(dto['organizationName'] ?? '—'),
      region: String(dto['region'] ?? '—'),
      email: String(dto['email'] ?? '—'),
      phoneNumber: String(dto['phoneNumber'] ?? '—'),
      headOffice: Boolean(dto['headOffice'] ?? dto['isHeadOffice']),
      active: Boolean(dto['active'] ?? true),
      businessHours: String(dto['businessHours'] ?? ''),
      locationId: dto['locationId'] != null ? Number(dto['locationId']) : null,
    };
  }

  private extractBranchDto(resp: unknown): BranchListRow {
    const root = this.toObj(this.parseJson(resp));
    const data = this.toObj(root?.['data']) ?? root;
    const dto = this.toObj(data?.['branchDto']) ?? this.toObj(data) ?? {};
    return this.mapBranchRow(dto);
  }

  private mapAgentRow(dto: Record<string, unknown>): AgentListRow {
    const first = String(dto['firstName'] ?? '').trim();
    const last = String(dto['lastName'] ?? '').trim();
    const contact = this.toObj(dto['contact']);
    let agentKind = String(dto['agentKind'] ?? '').trim();
    if (!agentKind && dto['isOrganization'] === true) {
      agentKind = 'ORGANIZATION';
    }
    return {
      id: Number(dto['id'] ?? 0),
      firstName: first,
      lastName: last,
      displayName: `${first} ${last}`.trim() || '—',
      organizationId: Number(dto['organizationId'] ?? 0),
      organizationName: String(dto['organizationName'] ?? '—'),
      agentKind: agentKind || '—',
      agentType: String(dto['agentType'] ?? '—'),
      role: String(dto['role'] ?? '—'),
      email: String(dto['email'] ?? contact?.['email'] ?? '—'),
      phoneNumber: String(dto['phoneNumber'] ?? contact?.['phoneNumber'] ?? '—'),
      active: Boolean(dto['active'] ?? true),
    };
  }

  private unwrapNamedPage(resp: unknown, pageKey: string): Record<string, unknown> | null {
    const root = this.toObj(this.parseJson(resp));
    if (!root) {
      return null;
    }
    const data = this.toObj(root['data']) ?? root;
    const p = this.toObj(data[pageKey]) ?? this.toObj(root[pageKey]);
    if (!p) {
      return null;
    }
    if (Array.isArray(p['content'])) {
      return p;
    }
    return this.toObj(p['page']) ?? null;
  }

  private mapRow(dto: Record<string, unknown>): KycQueueRow {
    const id = Number(dto['id'] ?? 0);
    const name = String(dto['name'] ?? '').trim();
    const classification = String(dto['organizationClassification'] ?? 'SUPPLIER');
    const orgTypeRaw = String(dto['organizationType'] ?? 'OTHER').trim() as OrganizationType;
    const orgType: OrganizationType = orgTypeRaw || 'OTHER';
    const kycStatus = String(dto['kycStatus'] ?? 'DRAFT');
    const pres = kycStatusPresentation(kycStatus);
    const submittedAt = dto['submittedAt'] as string | undefined;
    const createdAt = dto['createdAt'] as string | undefined;
    const stage1 = this.mapApproverAssignment(dto, 1);
    const stage2 = this.mapApproverAssignment(dto, 2);
    const createdViaSignup = dto['createdViaSignup'] === true;
    const verified = Boolean(dto['isVerified']);
    return {
      id,
      name,
      applicant: name,
      classification: classification as KycQueueRow['classification'],
      classificationLabel: classificationLabel(classification),
      organizationType: orgType,
      organizationTypeLabel: organizationTypeLabel(orgType),
      kycStatus: kycStatus as KycQueueRow['kycStatus'],
      statusLabel: pres.label,
      statusCss: pres.css,
      email: String(dto['email'] ?? '').trim(),
      phoneNumber: String(dto['phoneNumber'] ?? '').trim(),
      industryName: String(dto['industryName'] ?? '').trim(),
      registrationNumber: String(dto['registrationNumber'] ?? '').trim(),
      sourceLabel: createdViaSignup ? 'Platform signup' : 'Admin portal',
      createdViaSignup,
      entityStatus: String(dto['entityStatus'] ?? 'ACTIVE'),
      submitted: this.formatKycSubmittedDisplay(submittedAt, createdAt),
      submittedLabel: this.formatKycSubmittedDisplay(submittedAt, createdAt),
      createdLabel: this.formatDateTime(createdAt),
      verified,
      verifiedLabel: verified ? 'Verified' : 'Not verified',
      stage1ApproverLabel: stage1?.displayName ?? stage1?.username ?? '—',
      stage2ApproverLabel: stage2?.displayName ?? stage2?.username ?? '—',
    };
  }

  private mapOrganizationProfileDetail(dto: Record<string, unknown>, fallbackId: number): OrganizationProfileDetail {
    const row = this.mapRow(dto);
    const id =
      Number(dto['id'] ?? row.id ?? fallbackId) > 0
        ? Number(dto['id'] ?? row.id ?? fallbackId)
        : fallbackId;
    const orgTypeRaw = String(dto['organizationType'] ?? 'OTHER').trim() as OrganizationType;
    const orgType: OrganizationType = orgTypeRaw || 'OTHER';

    const industryIdRaw = dto['industryId'];
    let industryId: number | null = null;
    if (industryIdRaw !== null && industryIdRaw !== undefined && `${industryIdRaw}`.trim() !== '') {
      const parsed = Number(industryIdRaw);
      if (Number.isFinite(parsed)) {
        industryId = parsed;
      }
    }

    const branchListRaw = dto['branchDtoList'];
    const branches = Array.isArray(branchListRaw)
      ? branchListRaw
          .filter((r): r is Record<string, unknown> => !!this.toObj(r))
          .map((b) => this.mapBranchRow(b))
      : [];

    const agentListRaw = dto['agentDtoList'];
    const agents = Array.isArray(agentListRaw)
      ? agentListRaw
          .filter((r): r is Record<string, unknown> => !!this.toObj(r))
          .map((a) => this.mapAgentRow(a))
      : [];

    const customerListRaw = dto['customerDtoList'];
    const customers = Array.isArray(customerListRaw)
      ? customerListRaw
          .filter((r): r is Record<string, unknown> => !!this.toObj(r))
          .map((c) => this.mapOrganizationLinkRow(c))
      : [];

    const transportListRaw = dto['contractedTransporterDtoList'];
    const transporters = Array.isArray(transportListRaw)
      ? transportListRaw
          .filter((r): r is Record<string, unknown> => !!this.toObj(r))
          .map((t) => this.mapOrganizationLinkRow(t))
      : [];

    return {
      id,
      name: String(dto['name'] ?? row.name ?? '').trim() || row.name,
      email: String(dto['email'] ?? ''),
      phoneNumber: String(dto['phoneNumber'] ?? ''),
      organizationClassification: row.classification,
      classificationLabel: row.classificationLabel,
      organizationType: orgType,
      organizationTypeLabel: organizationTypeLabel(orgType),
      industryId,
      industryName: String(dto['industryName'] ?? ''),
      registrationNumber: String(dto['registrationNumber'] ?? '').trim(),
      taxNumber: String(dto['taxNumber'] ?? '').trim(),
      contactPersonFirstName: String(dto['contactPersonFirstName'] ?? '').trim(),
      contactPersonLastName: String(dto['contactPersonLastName'] ?? '').trim(),
      contactPersonEmail: String(dto['contactPersonEmail'] ?? ''),
      contactPersonPhoneNumber: String(dto['contactPersonPhoneNumber'] ?? ''),
      contactPersonUserId: this.parseOptionalPositiveLong(dto['contactPersonUserId']),
      websiteUrl: String(dto['websiteUrl'] ?? '').trim(),
      organizationDescription: String(dto['organizationDescription'] ?? '').trim(),
      kycStatus: row.kycStatus,
      kycStatusLabel: row.statusLabel,
      kycStatusCss: row.statusCss,
      isVerified: dto['isVerified'] !== undefined && dto['isVerified'] !== null ? Boolean(dto['isVerified']) : undefined,
      entityStatus: dto['entityStatus'] != null ? String(dto['entityStatus']) : undefined,
      branches,
      agents,
      customers,
      transporters,
    };
  }

  private mapOrganizationLinkRow(dto: Record<string, unknown>): OrganizationLinkRow {
    const classification = String(dto['organizationClassification'] ?? '').trim();
    const kyc = String(dto['kycStatus'] ?? '').trim();
    let kycLabel: string | undefined;
    if (kyc) {
      kycLabel = kycStatusPresentation(kyc).label;
    }
    const idNum = Number(dto['id'] ?? 0);
    return {
      id: Number.isFinite(idNum) ? idNum : 0,
      name: String(dto['name'] ?? '').trim(),
      classification: classification || undefined,
      classificationLabel: classification ? classificationLabel(classification) : undefined,
      email: String(dto['email'] ?? '').trim(),
      kycStatus: kyc || undefined,
      kycStatusLabel: kycLabel,
      verified: Boolean(dto['isVerified']),
    };
  }

  private mapDetail(dto: Record<string, unknown>, fallbackId: number): KycApplicationDetail {
    const row = this.mapRow(dto);
    const first = String(dto['contactPersonFirstName'] ?? '').trim();
    const last = String(dto['contactPersonLastName'] ?? '').trim();
    const createdViaSignup = dto['createdViaSignup'] !== false;
    return {
      ...row,
      id: row.id || fallbackId,
      applicant: row.name,
      tradingName: row.name,
      legalForm: (() => {
        const raw = String(dto['organizationType'] ?? '').trim();
        return raw ? organizationTypeLabel(raw) : '—';
      })(),
      registrationNumber: String(dto['registrationNumber'] ?? '—'),
      taxVatNumber: String(dto['taxNumber'] ?? '—'),
      industrySector: String(dto['industryName'] ?? dto['industryId'] ?? '—').trim() || '—',
      primaryContactName: `${first} ${last}`.trim() || '—',
      primaryContactEmail: String(dto['contactPersonEmail'] ?? dto['email'] ?? '—'),
      primaryContactPhone: String(dto['contactPersonPhoneNumber'] ?? dto['phoneNumber'] ?? '—'),
      contactPersonGenderLabel: this.formatGenderLabel(dto['contactPersonGender']),
      contactPersonDateOfBirth: String(dto['contactPersonDateOfBirth'] ?? '—').trim() || '—',
      contactPersonNationalIdNumber: String(dto['contactPersonNationalIdNumber'] ?? '—').trim() || '—',
      contactPersonPassportNumber: String(dto['contactPersonPassportNumber'] ?? '—').trim() || '—',
      registeredAddress: '—',
      principalPlaceOfBusiness: '—',
      bankName: '—',
      bankAccountMasked: '—',
      applicantNotes: String(dto['organizationDescription'] ?? ''),
      documents: this.mapVerificationDocuments(dto),
      kycStage: resolveKycStage(row.kycStatus),
      requiresKycApproval: createdViaSignup,
      stage1Approver: this.mapApproverAssignment(dto, 1),
      stage2Approver: this.mapApproverAssignment(dto, 2),
    };
  }

  private mapApproverAssignment(
    dto: Record<string, unknown>,
    stage: 1 | 2,
  ): KycApproverAssignment | undefined {
    const prefix = stage === 1 ? 'assignedStage1' : 'assignedStage2';
    const userId = Number(dto[`${prefix}ApproverUserId`] ?? 0);
    const username = String(dto[`${prefix}ApproverUsername`] ?? '').trim();
    const displayName = String(dto[`${prefix}ApproverDisplayName`] ?? username).trim();
    if (!Number.isFinite(userId) || userId < 1) {
      return username ? { username, displayName: displayName || username } : undefined;
    }
    return { userId, username: username || undefined, displayName: displayName || username };
  }

  /** Linked verification uploads on the organisation row (ZIMRA tax cert, registration cert, etc.). */
  private mapVerificationDocuments(dto: Record<string, unknown>): KycApplicationDocument[] {
    const taxNumber = String(dto['taxNumber'] ?? '').trim();
    const docs: KycApplicationDocument[] = [];
    const push = (uploadId: unknown, fileName: string, category: string, fileType: string) => {
      const numericId = Number(uploadId ?? 0);
      if (!Number.isFinite(numericId) || numericId < 1) {
        return;
      }
      const displayType =
        fileType.includes('CERTIFICATE') || fileType.includes('CLEARANCE') || fileType.includes('LICENSE')
          ? 'PDF'
          : fileType.replace(/_/g, ' ');
      docs.push({
        uploadId: numericId,
        fileName,
        category,
        fileType: displayType,
        uploadedAt: '—',
      });
    };
    push(
      dto['taxClearanceCertificateUploadId'],
      taxNumber ? `ZIMRA tax clearance (${taxNumber})` : 'ZIMRA tax clearance certificate',
      'Tax certificate',
      'TAX_CLEARANCE_CERTIFICATE',
    );
    push(
      dto['registrationCertificateUploadId'],
      'Company registration certificate',
      'Registration',
      'COMPANY_REGISTRATION_CERTIFICATE',
    );
    push(dto['businessLicenseUploadId'], 'Business licence', 'Licence', 'BUSINESS_LICENSE');
    push(dto['proofOfAddressUploadId'], 'Proof of address', 'Address', 'PROOF_OF_ADDRESS');
    push(
      dto['contactPersonNationalIdUploadId'],
      'Contact person national ID',
      'Identification',
      'NATIONAL_ID',
    );
    push(dto['contactPersonPassportUploadId'], 'Contact person passport', 'Identification', 'PASSPORT');
    push(dto['industrySpecificLicenseUploadId'], 'Industry-specific licence', 'Licence', 'INDUSTRY_SPECIFIC_LICENSE');
    push(dto['logoUploadId'], 'Organisation logo', 'Branding', 'ORGANIZATION_LOGO');
    return docs;
  }

  private extractRows(response: unknown): Record<string, unknown>[] {
    const root = this.toObj(this.parseJson(response));
    if (!root) {
      return [];
    }
    const data = this.toObj(root['data']) ?? root;
    const page = this.unwrapPage(data['organizationDtoPage']);
    if (page && Array.isArray(page['content'])) {
      return (page['content'] as unknown[]).filter((r): r is Record<string, unknown> => !!this.toObj(r));
    }
    const list = data['organizationDtoList'];
    if (Array.isArray(list)) {
      return list.filter((r): r is Record<string, unknown> => !!this.toObj(r));
    }
    const one = this.toObj(data['organizationDto']);
    return one ? [one] : [];
  }

  private extractSingle(response: unknown): Record<string, unknown> {
    const root = this.toObj(this.parseJson(response));
    if (!root) {
      return {};
    }
    const data = this.toObj(root['data']) ?? root;
    return this.toObj(data['organizationDto']) ?? data;
  }

  private unwrapPage(value: unknown): Record<string, unknown> | null {
    const root = this.toObj(this.parseJson(value));
    if (!root) {
      return null;
    }
    const data = this.toObj(root['data']) ?? root;
    const p = this.toObj(data['organizationDtoPage']) ?? this.toObj(root['organizationDtoPage']);
    if (!p) {
      return null;
    }
    if (Array.isArray(p['content'])) {
      return p;
    }
    return this.toObj(p['page']) ?? null;
  }

  private parseOptionalPositiveLong(value: unknown): number | null {
    if (value === null || value === undefined || `${value}`.trim() === '') {
      return null;
    }
    const n = Number(value);
    return Number.isFinite(n) && n > 0 ? Math.trunc(n) : null;
  }

  private parseJson(value: unknown): unknown {
    if (typeof value === 'string') {
      try {
        return JSON.parse(value);
      } catch {
        return value;
      }
    }
    return value;
  }

  private toObj(value: unknown): Record<string, unknown> | null {
    return value && typeof value === 'object' && !Array.isArray(value)
      ? (value as Record<string, unknown>)
      : null;
  }

  private toIndustryActionResponse(response: unknown, fallbackMessage: string): IndustryActionResponse {
    const body = this.toObj(this.parseJson(response));
    if (!body) {
      return { ok: false, message: 'Empty response' };
    }
    const data = this.toObj(body['data']) ?? body;
    const statusCode =
      typeof data['statusCode'] === 'number'
        ? data['statusCode']
        : typeof body['statusCode'] === 'number'
          ? body['statusCode']
          : undefined;
    const successFlag = data['success'] ?? body['success'];
    const isSuccessFlag = data['isSuccess'] ?? body['isSuccess'];
    const failed =
      successFlag === false ||
      isSuccessFlag === false ||
      (statusCode !== undefined && statusCode >= 400);
    const ok = !failed;

    let message =
      (typeof data['message'] === 'string' ? data['message'] : '') ||
      (typeof body['message'] === 'string' ? body['message'] : '') ||
      (typeof data['messageResponse'] === 'string' ? data['messageResponse'] : '') ||
      fallbackMessage;

    const errList = (data['errorMessages'] ?? body['errorMessages']) as unknown;
    if (!ok && Array.isArray(errList) && errList.length > 0) {
      const parts = errList.filter((e): e is string => typeof e === 'string');
      if (parts.length) {
        message = [message, ...parts].filter((s) => s.length > 0).join(' ');
      }
    }

    let row: IndustryUsageRow | undefined;
    if (ok) {
      try {
        row = this.extractIndustryDto(response);
      } catch {
        row = undefined;
      }
    }

    return { ok, message, row };
  }

  // ─── Organisation select helper ──────────────────────────────────────────────

  fetchOrganizationsForSelect(): Observable<Array<{ id: number; name: string }>> {
    return this.queryAllOrganizations(0, 200, { searchQuery: '' }).pipe(
      map(({ rows }) => rows.map((r) => ({ id: r.id, name: r.name }))),
      catchError(() => of([])),
    );
  }

  // ─── Branch CRUD ──────────────────────────────────────────────────────────────

  getBranch(id: number): Observable<BranchListRow> {
    return this.http.get<unknown>(this.url(`branches/${id}`)).pipe(
      map((resp) => {
        const root = this.toObj(this.parseJson(resp));
        const data = this.toObj(root?.['data']) ?? root ?? {};
        const dto = this.toObj(data['branchDto']) ?? data;
        return this.mapBranchRow(dto as Record<string, unknown>);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  createBranch(payload: BranchPayload): Observable<BranchActionResponse> {
    return this.http.post<unknown>(this.url('branches'), payload).pipe(
      map((resp) => this.toBranchActionResponse(resp, 'Branch created.')),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  updateBranch(id: number, payload: BranchPayload): Observable<BranchActionResponse> {
    return this.http.put<unknown>(this.url(`branches/${id}`), payload).pipe(
      map((resp) => this.toBranchActionResponse(resp, 'Branch updated.')),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  deleteBranch(id: number): Observable<void> {
    return this.http.delete<unknown>(this.url(`branches/${id}`)).pipe(
      map(() => void 0),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  exportBranches(format: 'csv' | 'xlsx' | 'pdf', filters?: BranchTableQuery): Observable<Blob> {
    const q = filters ?? { page: 0, size: 0, searchQuery: '' };
    return this.resolveBranchFiltersForRequest(q).pipe(
      switchMap((resolved) => {
        const body = this.buildBranchExportBody(resolved);
        return this.http.post(`${this.url('branches/export')}?format=${format}`, body, { responseType: 'blob' });
      }),
      catchError((err: HttpErrorResponse) => mapExportHttpError(err)),
    );
  }

  importBranchesCsv(file: File): Observable<ImportActionResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<unknown>(this.url('branches/import-csv'), formData).pipe(
      map((resp) => this.toImportActionResponse(resp, 'Branch import completed.')),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  getBranchSampleCsv(): { blob: Blob; filename: string } {
    return this.buildSampleCsv(BRANCH_SAMPLE, 'branches-sample.csv');
  }

  // ─── Agent CRUD ───────────────────────────────────────────────────────────────

  getAgent(id: number): Observable<AgentListRow> {
    return this.http.get<unknown>(this.url(`agents/${id}`)).pipe(
      map((resp) => {
        const root = this.toObj(this.parseJson(resp));
        const data = this.toObj(root?.['data']) ?? root ?? {};
        const dto = this.toObj(data['agentDto']) ?? data;
        return this.mapAgentRow(dto as Record<string, unknown>);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  createAgent(payload: AgentPayload): Observable<AgentActionResponse> {
    return this.http.post<unknown>(this.url('agents'), payload).pipe(
      map((resp) => this.toAgentActionResponse(resp, 'Agent created.')),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  updateAgent(id: number, payload: AgentPayload): Observable<AgentActionResponse> {
    return this.http.put<unknown>(this.url(`agents/${id}`), payload).pipe(
      map((resp) => this.toAgentActionResponse(resp, 'Agent updated.')),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  deleteAgent(id: number): Observable<void> {
    return this.http.delete<unknown>(this.url(`agents/${id}`)).pipe(
      map(() => void 0),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  exportAgents(format: 'csv' | 'xlsx' | 'pdf', filters?: AgentTableQuery): Observable<Blob> {
    const q = filters ?? { page: 0, size: 0, searchQuery: '' };
    return this.resolveAgentFiltersForRequest(q).pipe(
      switchMap((resolved) => {
        const body = this.buildAgentExportBody(resolved);
        return this.http.post(`${this.url('agents/export')}?format=${format}`, body, { responseType: 'blob' });
      }),
      catchError((err: HttpErrorResponse) => mapExportHttpError(err)),
    );
  }

  importAgentsCsv(file: File): Observable<ImportActionResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<unknown>(this.url('agents/import-csv'), formData).pipe(
      map((resp) => this.toImportActionResponse(resp, 'Agent import completed.')),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  getAgentSampleCsv(): { blob: Blob; filename: string } {
    return this.buildSampleCsv(AGENT_SAMPLE, 'agents-sample.csv');
  }

  // ─── Industry export / import ─────────────────────────────────────────────────

  exportIndustries(format: 'csv' | 'xlsx' | 'pdf', filters?: IndustryTableQuery): Observable<Blob> {
    const body = this.buildIndustryExportBody(filters);
    return this.http
      .post(`${this.url('industries/export')}?format=${format}`, body, { responseType: 'blob' })
      .pipe(catchError((err: HttpErrorResponse) => mapExportHttpError(err)));
  }

  importIndustriesCsv(file: File): Observable<ImportActionResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<unknown>(this.url('industries/import-csv'), formData).pipe(
      map((resp) => this.toImportActionResponse(resp, 'Industry import completed.')),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  getIndustrySampleCsv(): { blob: Blob; filename: string } {
    return this.buildSampleCsv(INDUSTRY_SAMPLE, 'industries-sample.csv');
  }

  // ─── Private helpers for new methods ─────────────────────────────────────────

  private toBranchActionResponse(response: unknown, fallbackMessage: string): BranchActionResponse {
    const body = this.toObj(this.parseJson(response));
    if (!body) {
      return { ok: false, message: 'Empty response' };
    }
    const data = this.toObj(body['data']) ?? body;
    const statusCode =
      typeof data['statusCode'] === 'number'
        ? data['statusCode']
        : typeof body['statusCode'] === 'number'
          ? body['statusCode']
          : undefined;
    const failed =
      data['success'] === false ||
      body['success'] === false ||
      (statusCode !== undefined && statusCode >= 400);
    const ok = !failed;
    const message =
      (typeof data['message'] === 'string' ? data['message'] : '') ||
      (typeof body['message'] === 'string' ? body['message'] : '') ||
      fallbackMessage;
    let row: BranchListRow | undefined;
    if (ok) {
      try {
        const dto = this.toObj(data['branchDto']) ?? data;
        row = this.mapBranchRow(dto as Record<string, unknown>);
      } catch {
        row = undefined;
      }
    }
    return { ok, message, row };
  }

  private toAgentActionResponse(response: unknown, fallbackMessage: string): AgentActionResponse {
    const body = this.toObj(this.parseJson(response));
    if (!body) {
      return { ok: false, message: 'Empty response' };
    }
    const data = this.toObj(body['data']) ?? body;
    const statusCode =
      typeof data['statusCode'] === 'number'
        ? data['statusCode']
        : typeof body['statusCode'] === 'number'
          ? body['statusCode']
          : undefined;
    const failed =
      data['success'] === false ||
      body['success'] === false ||
      (statusCode !== undefined && statusCode >= 400);
    const ok = !failed;
    const message =
      (typeof data['message'] === 'string' ? data['message'] : '') ||
      (typeof body['message'] === 'string' ? body['message'] : '') ||
      fallbackMessage;
    let row: AgentListRow | undefined;
    if (ok) {
      try {
        const dto = this.toObj(data['agentDto']) ?? data;
        row = this.mapAgentRow(dto as Record<string, unknown>);
      } catch {
        row = undefined;
      }
    }
    return { ok, message, row };
  }

  private toImportActionResponse(response: unknown, fallbackMessage: string): ImportActionResponse {
    const body = this.toObj(this.parseJson(response));
    if (!body) {
      return { ok: true, message: fallbackMessage };
    }
    const statusCode = typeof body['statusCode'] === 'number' ? body['statusCode'] : undefined;
    const ok =
      body['isSuccess'] !== false &&
      body['success'] !== false &&
      (statusCode === undefined || statusCode < 400);
    let message =
      (typeof body['message'] === 'string' ? body['message'] : '') ||
      (typeof body['messageResponse'] === 'string' ? body['messageResponse'] : '') ||
      fallbackMessage;
    const errList = body['errorMessages'];
    if (!ok && Array.isArray(errList) && errList.length > 0) {
      const parts = (errList as unknown[]).filter((e): e is string => typeof e === 'string');
      if (parts.length) {
        message = [message, ...parts].filter((s) => s.length > 0).join(' ');
      }
    }
    return { ok, message };
  }

  private buildSampleCsv(
    template: SampleCsvTemplate,
    filename: string,
  ): { blob: Blob; filename: string } {
    const lines = [template.headers, ...template.rows].map((row) =>
      row.map(this.escapeCsvCell).join(','),
    );
    const csvText = `${lines.join('\n')}\n`;
    return {
      blob: new Blob([csvText], { type: 'text/csv;charset=utf-8' }),
      filename,
    };
  }

  private buildBranchExportBody(filters?: BranchTableQuery): Record<string, unknown> {
    const q = filters ?? { page: 0, size: 0, searchQuery: '' };
    const body: Record<string, unknown> = {
      page: 0,
      size: 1_000_000,
      searchValue: q.searchQuery?.trim() ?? '',
    };
    if (q.branchName?.trim()) {
      body['branchName'] = q.branchName.trim();
    }
    if (q.organizationId) {
      body['organizationId'] = q.organizationId;
    }
    return body;
  }

  private buildAgentExportBody(filters?: AgentTableQuery): Record<string, unknown> {
    const q = filters ?? { page: 0, size: 0, searchQuery: '' };
    const body: Record<string, unknown> = {
      page: 0,
      size: 1_000_000,
      searchValue: q.searchQuery?.trim() ?? '',
    };
    const kind = this.normalizeAgentKindFilter(q.agentKind);
    if (kind) {
      body['agentKind'] = kind;
    }
    if (q.organizationId) {
      body['organizationId'] = q.organizationId;
    }
    return body;
  }

  private buildIndustryExportBody(filters?: IndustryTableQuery): Record<string, unknown> {
    const q = filters ?? { page: 0, size: 0, searchQuery: '', columnFilters: {} };
    const cf = q.columnFilters ?? {};
    return {
      page: 0,
      size: 1_000_000,
      searchValue: q.searchQuery?.trim() ?? '',
      name: cf.name?.trim() ?? '',
      industryCode: cf.industryCode?.trim() ?? '',
    };
  }

  private readonly escapeCsvCell = (value: string): string => {
    if (value === undefined || value === null) return '';
    const needsQuoting = /[",\r\n]/.test(value);
    const escaped = value.replace(/"/g, '""');
    return needsQuoting ? `"${escaped}"` : escaped;
  };

  /** KYC queue / review: show submit time when known, otherwise registration time (draft signups). */
  private formatKycSubmittedDisplay(submittedAt?: string, createdAt?: string): string {
    if (submittedAt) {
      return this.formatDateTime(submittedAt);
    }
    if (createdAt) {
      return `Registered ${this.formatDateTime(createdAt)}`;
    }
    return '—';
  }

  private formatGenderLabel(value: unknown): string {
    const raw = String(value ?? '').trim();
    if (!raw) {
      return '—';
    }
    return raw
      .toLowerCase()
      .split('_')
      .map((part) => (part ? part.charAt(0).toUpperCase() + part.slice(1) : ''))
      .join(' ');
  }

  private formatDateTime(iso?: string): string {
    if (!iso) {
      return '—';
    }
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) {
      return iso;
    }
    return d.toLocaleString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  private toOrgListError(err: unknown): Error {
    if (err instanceof HttpErrorResponse) {
      if (err.status === 404) {
        return new Error(
          'Organisation service not found (404). Ensure ldms-organization-management is running on port 8087 and the API gateway (8091) is up.',
        );
      }
      if (err.status === 0 || err.status === 500 || err.status === 502 || err.status === 503) {
        return new Error(
          'Organisation service is unavailable. Requests from this portal go to the API gateway (8091) → ldms-organization-management (8087). ' +
            'Start ldms-organization-management in IntelliJ (or your run configuration), confirm it listens on port 8087, then refresh.',
        );
      }
      return this.toError(err);
    }
    return err instanceof Error ? err : new Error('Failed to load organisations');
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
