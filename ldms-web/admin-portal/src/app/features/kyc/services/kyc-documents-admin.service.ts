import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, forkJoin, from, map, mergeMap, of, switchMap, tap, throwError, toArray } from 'rxjs';
import { filterByGlobalAndColumns } from '@shared/utils/table-search.util';
import { resolveFilePreview } from '@shared/utils/file-upload-preview';
import { extractPagedResult } from '../../../core/utils/api-paged-response.util';
import { ldmsApiUrl } from '../../../core/utils/api-url.util';
import {
  FileUploadAdminService,
  FileUploadSummary,
} from '../../../core/services/file-upload-admin.service';
import {
  OrganizationsAdminService,
  type OrganizationTableQuery,
} from '../../organizations/services/organizations-admin.service';
import type { KycApplicationDocument } from '../../organizations/models/organization.model';
import { kycStatusPresentation } from '../../organizations/models/organization.model';

export type DocumentSourceScope = 'ORGANIZATION' | 'USER';
export type DocumentSourceChannel =
  | 'KYC_ONBOARDING'
  | 'ORGANIZATION_COMPLIANCE'
  | 'USER_PROFILE'
  | 'ORGANIZATION_BRANDING'
  | 'FILE_UPLOAD_REGISTRY';

export type DocumentFilterId =
  | 'ALL'
  | 'KYC'
  | 'COMPLIANCE'
  | 'PROFILE'
  | 'BRANDING'
  | 'OTHER';

export type DocumentSortId = 'newest' | 'oldest' | 'name' | 'size';

export interface AdminStagedDocument {
  id: number;
  fileName: string;
  displayTitle: string;
  category: string;
  sourceLabel: string;
  sourceScope: DocumentSourceScope;
  sourceChannel: DocumentSourceChannel;
  ownerType: 'USER' | 'ORGANIZATION';
  ownerId: number;
  organizationId: number;
  organizationName: string;
  kycStatusLabel: string;
  statusLabel: string;
  fileType: string;
  contentType: string;
  fileSizeInBytes?: number;
  createdAt: string;
  entityStatus: string;
  storageProvider?: string;
  storedFileName?: string;
  fileUrl?: string;
  fileHash?: string;
  autoVerified?: boolean;
  autoVerificationNotes?: string;
  autoVerifiedAt?: string;
  autoVerificationMethod?: string;
  autoVerificationSource?: string;
  expiresAt?: string;
  createdBy?: string;
  modifiedAt?: string;
  modifiedBy?: string;
  linkedFieldKey?: string;
  previewImageUrl?: string | null;
  previewPdfDataUrl?: string | null;
  hasPdfPreview: boolean;
  hasPreview: boolean;
}

export interface KycDocumentTableRow {
  uploadId: number;
  fileName: string;
  type: string;
  fileTypeLabel: string;
  status: string;
  statusLabel: string;
  organizationId: number;
  organizationName: string;
  kycStatusLabel: string;
  uploadedAt: string;
  entityStatus: string;
  ownerType: 'ORGANIZATION' | 'USER';
}

export interface KycDocumentsTablePage {
  rows: KycDocumentTableRow[];
  totalElements: number;
}

export interface VaultDocumentsQuery {
  page: number;
  size: number;
  searchQuery: string;
  categoryFilter: DocumentFilterId;
}

export interface VaultDocumentsPage {
  documents: AdminStagedDocument[];
  totalElements: number;
}

/** Query sent to file-upload `find-by-multiple-filters` (same pattern as locations / organisations). */
export interface DocumentsTableQuery {
  page: number;
  size: number;
  searchQuery: string;
  columnFilters: {
    fileName?: string;
    organizationName?: string;
    type?: string;
    statusLabel?: string;
    kycStatusLabel?: string;
  };
}

interface OrgContext {
  organizationId: number;
  organizationName: string;
  kycStatusLabel: string;
}

interface LinkedDocRef extends KycApplicationDocument {
  organizationId: number;
  organizationName: string;
  kycStatusLabel: string;
}

/**
 * Admin compliance document catalogue — file-upload metadata enriched with organisation context.
 */
@Injectable({ providedIn: 'root' })
export class KycDocumentsAdminService {
  private readonly base = ldmsApiUrl('/ldms-file-upload-service/v1/backoffice/file-upload');
  private readonly filteredCatalogCap = 500;
  private readonly orgOwnerIdCap = 500;
  private readonly orgResolveConcurrency = 4;

  /** Session cache — avoids repeat org GETs when paging through uploads. */
  private readonly orgContextById = new Map<number, OrgContext>();
  /** KYC verification doc metadata keyed by upload id (category labels, linked org). */
  private readonly linkedByUploadId = new Map<number, LinkedDocRef>();

  constructor(
    private readonly http: HttpClient,
    private readonly fileUpload: FileUploadAdminService,
    private readonly organizations: OrganizationsAdminService,
  ) {}

  /** Total active uploads (for dashboard badges). */
  countTotal(): Observable<number> {
    return this.queryTablePage({ page: 0, size: 1, searchQuery: '', columnFilters: {} }).pipe(
      map(({ totalElements }) => totalElements),
      catchError(() => of(0)),
    );
  }

  /**
   * Server-paged document list (same transport as locations `queryTablePage` / countries table).
   * Falls back to {@code GET find-all} when the filter endpoint is not deployed yet (HTTP 404).
   */
  queryTablePage(q: DocumentsTableQuery): Observable<KycDocumentsTablePage> {
    return this.resolveOrganizationOwnerIds(q).pipe(
      switchMap((organizationOwnerIds) => {
        if (organizationOwnerIds !== null && organizationOwnerIds.length === 0) {
          return of({ rows: [], totalElements: 0 });
        }
        const body = this.buildFilterBody(q, organizationOwnerIds);
        return this.http.post<unknown>(`${this.base}/find-by-multiple-filters`, body).pipe(
          switchMap((resp) => this.mapUploadResponse(resp)),
          catchError((err) => {
            if (err instanceof HttpErrorResponse && err.status === 404) {
              return this.queryTablePageViaFindAll(q, organizationOwnerIds);
            }
            return throwError(() => this.toTableError(err));
          }),
        );
      }),
    );
  }

  /** @deprecated Use {@link queryTablePage}. */
  loadTablePage(page: number, size: number): Observable<KycDocumentsTablePage> {
    return this.queryTablePage({ page, size, searchQuery: '', columnFilters: {} });
  }

  /** Server-paged vault catalogue (metadata only until detail fetch). */
  queryVaultPage(q: VaultDocumentsQuery): Observable<VaultDocumentsPage> {
    const fileTypeHint = this.fileTypeHintForCategory(q.categoryFilter);
    return this.queryTablePage({
      page: q.page,
      size: q.size,
      searchQuery: q.searchQuery,
      columnFilters: { type: fileTypeHint },
    }).pipe(
      map(({ rows, totalElements }) => {
        let documents = rows.map((row) => this.stagedDocumentFromTableRow(row));
        if (q.categoryFilter !== 'ALL') {
          documents = documents.filter((d) => this.matchesCategoryFilter(d, q.categoryFilter));
        }
        return { documents, totalElements };
      }),
      catchError((err) => throwError(() => this.toTableError(err))),
    );
  }

  /** Total documents per vault category (respects current search; one lightweight count query each). */
  loadFilterCounts(searchQuery: string): Observable<Record<DocumentFilterId, number>> {
    const filters: DocumentFilterId[] = ['ALL', 'KYC', 'COMPLIANCE', 'PROFILE', 'BRANDING', 'OTHER'];
    return forkJoin(
      filters.map((categoryFilter) =>
        this.queryVaultPage({ page: 0, size: 1, searchQuery, categoryFilter }).pipe(
          map(({ totalElements }) => ({ categoryFilter, count: totalElements })),
          catchError(() => of({ categoryFilter, count: 0 })),
        ),
      ),
    ).pipe(
      map((entries) => {
        const counts = {} as Record<DocumentFilterId, number>;
        for (const entry of entries) {
          counts[entry.categoryFilter] = entry.count;
        }
        return counts;
      }),
    );
  }

  fetchVaultDocumentDetail(id: number): Observable<AdminStagedDocument | null> {
    return this.fileUpload.getById(id).pipe(
      switchMap((dto) => {
        if (!dto) {
          return of(null);
        }
        const summary = this.fileUpload.dtoToSummary(dto);
        if (!summary) {
          return of(null);
        }
        return this.enrichUploads([summary]).pipe(
          map((rows) => {
            const row = rows[0];
            if (!row) {
              return null;
            }
            return this.rowToStagedDocument(row, summary, dto, { withPreview: true });
          }),
        );
      }),
      catchError(() => of(null)),
    );
  }

  private buildFilterBody(
    q: DocumentsTableQuery,
    organizationOwnerIds: number[] | null,
  ): Record<string, unknown> {
    const cf = q.columnFilters ?? {};
    const body: Record<string, unknown> = {
      page: q.page,
      size: q.size,
      searchValue: q.searchQuery.trim(),
      originalFileName: cf.fileName?.trim() ?? '',
      fileType: cf.type?.trim() ?? '',
      entityStatus: this.resolveEntityStatusFilter(cf.statusLabel?.trim() ?? ''),
    };
    if (organizationOwnerIds != null) {
      body['organizationOwnerIds'] = organizationOwnerIds;
    }
    return body;
  }

  private resolveEntityStatusFilter(input: string): string {
    if (!input) {
      return '';
    }
    const lower = input.toLowerCase();
    if (lower === 'active') {
      return 'ACTIVE';
    }
    if (lower === 'deleted') {
      return 'DELETED';
    }
    if (lower === 'inactive') {
      return 'INACTIVE';
    }
    if (lower === 'suspended') {
      return 'SUSPENDED';
    }
    return input.toUpperCase().replace(/\s+/g, '_');
  }

  private resolveOrganizationOwnerIds(q: DocumentsTableQuery): Observable<number[] | null> {
    const orgName = q.columnFilters.organizationName?.trim() ?? '';
    const kycLabel = q.columnFilters.kycStatusLabel?.trim() ?? '';
    if (!orgName && !kycLabel) {
      return of(null);
    }
    const orgQuery: OrganizationTableQuery = {
      page: 0,
      size: this.orgOwnerIdCap,
      searchQuery: orgName,
      columnFilters: {},
    };
    if (kycLabel) {
      orgQuery.columnFilters.statusLabel = kycLabel;
    }
    return this.organizations.queryTablePage(orgQuery).pipe(
      map(({ rows }) => rows.map((r) => r.id).filter((id) => Number.isFinite(id) && id > 0)),
      catchError(() => of([])),
    );
  }

  /** Legacy path when file-upload service has not been restarted with the filter endpoint. */
  private queryTablePageViaFindAll(
    q: DocumentsTableQuery,
    organizationOwnerIds: number[] | null,
  ): Observable<KycDocumentsTablePage> {
    const hasClientFilters = this.hasClientSideFilters(q, organizationOwnerIds);
    if (!hasClientFilters) {
      return this.fileUpload.findAllMetadataPage(q.page, q.size).pipe(
        switchMap(({ rows, totalElements }) => this.enrichUploadsToPage(rows, totalElements)),
        catchError(() => of({ rows: [], totalElements: 0 })),
      );
    }
    return this.fileUpload.loadAllMetadata(this.filteredCatalogCap).pipe(
      switchMap((uploads) => {
        let scoped = uploads;
        if (organizationOwnerIds != null) {
          scoped = scoped.filter(
            (u) =>
              this.normalizeOwnerType(u.ownerType) === 'ORGANIZATION' &&
              organizationOwnerIds.includes(Number(u.ownerId ?? 0)),
          );
        }
        return this.enrichUploads(scoped).pipe(
          map((allRows) => {
            const filtered = filterByGlobalAndColumns(allRows, q.searchQuery, {
              fileName: q.columnFilters.fileName ?? '',
              organizationName: q.columnFilters.organizationName ?? '',
              type: q.columnFilters.type ?? '',
              statusLabel: q.columnFilters.statusLabel ?? '',
              kycStatusLabel: q.columnFilters.kycStatusLabel ?? '',
            });
            const start = q.page * q.size;
            return {
              totalElements: filtered.length,
              rows: filtered.slice(start, start + q.size),
            };
          }),
        );
      }),
      catchError(() => of({ rows: [], totalElements: 0 })),
    );
  }

  private hasClientSideFilters(q: DocumentsTableQuery, organizationOwnerIds: number[] | null): boolean {
    if (organizationOwnerIds != null) {
      return true;
    }
    if (q.searchQuery.trim()) {
      return true;
    }
    return Object.values(q.columnFilters).some((v) => String(v ?? '').trim().length > 0);
  }

  private mapUploadResponse(resp: unknown): Observable<KycDocumentsTablePage> {
    const { rows: uploads, totalElements } = extractPagedResult(resp, 'fileUploadDtoPage');
    const summaries = uploads
      .map((row) => this.fileUpload.dtoToSummary(row as Record<string, unknown>))
      .filter((r): r is FileUploadSummary => r != null);
    return this.enrichUploadsToPage(summaries, totalElements);
  }

  private enrichUploadsToPage(
    uploads: FileUploadSummary[],
    totalElements: number,
  ): Observable<KycDocumentsTablePage> {
    return this.enrichUploads(uploads).pipe(
      map((rows) => ({ rows, totalElements })),
    );
  }

  private enrichUploads(uploads: FileUploadSummary[]): Observable<KycDocumentTableRow[]> {
    const orgIds = this.orgIdsFromUploads(uploads);
    return this.resolveOrgContexts(orgIds).pipe(
      map(() => uploads.map((upload) => this.uploadToRow(upload, this.orgContextById, this.linkedByUploadId))),
    );
  }

  private toTableError(err: unknown): Error {
    if (err instanceof Error) {
      return err;
    }
    if (err instanceof HttpErrorResponse) {
      const body = err.error as { message?: string } | undefined;
      if (body?.message?.trim()) {
        return new Error(body.message.trim());
      }
      if (err.status === 401) {
        return new Error('Not signed in. Log in again to continue.');
      }
      if (err.status === 0) {
        return new Error(
          'Request failed before the server responded. Check that the API gateway and file-upload service are running.',
        );
      }
      return new Error(err.message || 'Failed to load compliance documents.');
    }
    return new Error('Failed to load compliance documents.');
  }

  private orgIdsFromUploads(uploads: FileUploadSummary[]): number[] {
    const ids = new Set<number>();
    for (const upload of uploads) {
      if (this.normalizeOwnerType(upload.ownerType) !== 'ORGANIZATION') {
        continue;
      }
      const ownerId = Number(upload.ownerId ?? 0);
      if (ownerId > 0) {
        ids.add(ownerId);
      }
    }
    return [...ids];
  }

  private resolveOrgContexts(orgIds: number[]): Observable<Map<number, OrgContext>> {
    const missing = orgIds.filter((id) => id > 0 && !this.orgContextById.has(id));
    if (missing.length === 0) {
      return of(this.orgContextById);
    }
    return from(missing).pipe(
      mergeMap(
        (id) =>
          this.organizations.getOrganization(id).pipe(
            map((detail) => {
              const kycRaw = String(detail.kycStatus ?? '').trim();
              const orgName = detail.name?.trim() || `Organisation #${id}`;
              const kycStatusLabel = kycRaw ? kycStatusPresentation(kycRaw as never).label : '—';
              const ctx: OrgContext = {
                organizationId: id,
                organizationName: orgName,
                kycStatusLabel,
              };
              for (const doc of detail.documents ?? []) {
                this.linkedByUploadId.set(doc.uploadId, {
                  ...doc,
                  organizationId: id,
                  organizationName: orgName,
                  kycStatusLabel,
                });
              }
              return { id, ctx };
            }),
            catchError(() => of(null)),
          ),
        this.orgResolveConcurrency,
      ),
      toArray(),
      tap((results) => {
        for (const entry of results) {
          if (entry) {
            this.orgContextById.set(entry.id, entry.ctx);
          }
        }
      }),
      map(() => this.orgContextById),
    );
  }

  private uploadToRow(
    upload: FileUploadSummary,
    orgById: Map<number, OrgContext>,
    linkedByUploadId: Map<number, LinkedDocRef>,
  ): KycDocumentTableRow {
    const linked = linkedByUploadId.get(upload.id);
    const ownerType = this.normalizeOwnerType(upload.ownerType);
    const ownerId = Number(upload.ownerId ?? 0);
    const org =
      ownerType === 'ORGANIZATION' && ownerId > 0 ? orgById.get(ownerId) : linked ? orgById.get(linked.organizationId) : undefined;

    const pres = this.entityStatusPresentation(upload.entityStatus);
    return {
      uploadId: upload.id,
      fileName: linked?.fileName || upload.originalFileName,
      type: linked?.category || this.categoryFromFileType(upload.fileType),
      fileTypeLabel: upload.fileType || linked?.fileType || '—',
      status: pres.css,
      statusLabel: pres.label,
      organizationId: org?.organizationId ?? (ownerType === 'ORGANIZATION' ? ownerId : linked?.organizationId ?? 0),
      organizationName:
        org?.organizationName ??
        linked?.organizationName ??
        (ownerType === 'ORGANIZATION' && ownerId > 0 ? `Organisation #${ownerId}` : ownerType === 'USER' && ownerId > 0 ? `User #${ownerId}` : '—'),
      kycStatusLabel: org?.kycStatusLabel ?? linked?.kycStatusLabel ?? '—',
      uploadedAt: upload.createdAt || linked?.uploadedAt || '',
      entityStatus: upload.entityStatus || 'ACTIVE',
      ownerType,
    };
  }

  private normalizeOwnerType(value: string | undefined): 'ORGANIZATION' | 'USER' {
    const raw = String(value ?? '').toUpperCase();
    return raw.includes('ORG') ? 'ORGANIZATION' : 'USER';
  }

  private categoryFromFileType(fileType: string): string {
    const ft = String(fileType ?? '').trim();
    if (!ft) {
      return 'Document';
    }
    return ft.replace(/_/g, ' ');
  }

  private entityStatusPresentation(entityStatus: string): { css: string; label: string } {
    const s = String(entityStatus ?? 'ACTIVE').toUpperCase();
    if (s === 'DELETED') {
      return { css: 'inactive', label: 'Deleted' };
    }
    if (s === 'INACTIVE' || s === 'SUSPENDED') {
      return { css: 'inactive', label: s === 'SUSPENDED' ? 'Suspended' : 'Inactive' };
    }
    return { css: 'active', label: 'Active' };
  }

  private rowToStagedDocument(
    row: KycDocumentTableRow,
    upload: FileUploadSummary,
    dto?: Record<string, unknown>,
    options?: { withPreview?: boolean },
  ): AdminStagedDocument {
    const sourceScope: DocumentSourceScope = row.ownerType === 'ORGANIZATION' ? 'ORGANIZATION' : 'USER';
    const sourceChannel = this.inferSourceChannel(row.type, row.fileTypeLabel, sourceScope);
    const displayTitle = this.enrichDisplayTitle(row.fileName, row.type);
    const preview =
      options?.withPreview === true && dto
        ? resolveFilePreview(dto, { maxBase64Chars: 900_000 })
        : null;
    const contentType = upload.contentType || String(dto?.['contentType'] ?? '').trim();
    const nameLower = row.fileName.toLowerCase();
    const ctLower = contentType.toLowerCase();
    const hasPdfPreview = preview?.kind === 'pdf' || ctLower.includes('pdf') || nameLower.endsWith('.pdf');
    const hasPreview = preview != null;

    return {
      id: row.uploadId,
      fileName: row.fileName,
      displayTitle,
      category: row.type,
      sourceLabel: `${row.organizationName} · ${row.type}`,
      sourceScope,
      sourceChannel,
      ownerType: row.ownerType,
      ownerId: Number(upload.ownerId ?? 0),
      organizationId: row.organizationId,
      organizationName: row.organizationName,
      kycStatusLabel: row.kycStatusLabel,
      statusLabel: row.statusLabel,
      fileType: row.fileTypeLabel,
      contentType,
      fileSizeInBytes: upload.fileSizeInBytes,
      createdAt: row.uploadedAt || upload.createdAt || '',
      entityStatus: row.entityStatus,
      storageProvider: dto?.['storageProvider'] != null ? String(dto['storageProvider']) : undefined,
      storedFileName: dto?.['storedFileName'] != null ? String(dto['storedFileName']) : undefined,
      fileUrl: dto?.['fileUrl'] != null ? String(dto['fileUrl']) : undefined,
      fileHash: dto?.['fileHash'] != null ? String(dto['fileHash']) : undefined,
      autoVerified: dto?.['autoVerified'] === true,
      autoVerificationNotes:
        dto?.['autoVerificationNotes'] != null ? String(dto['autoVerificationNotes']) : undefined,
      autoVerifiedAt: dto?.['autoVerifiedAt'] != null ? String(dto['autoVerifiedAt']) : undefined,
      autoVerificationMethod:
        dto?.['autoVerificationMethod'] != null ? String(dto['autoVerificationMethod']) : undefined,
      autoVerificationSource:
        dto?.['autoVerificationSource'] != null ? String(dto['autoVerificationSource']) : undefined,
      expiresAt: dto?.['expiresAt'] != null ? String(dto['expiresAt']) : undefined,
      createdBy: dto?.['createdBy'] != null ? String(dto['createdBy']) : undefined,
      modifiedAt:
        dto?.['modifiedAt'] != null
          ? String(dto['modifiedAt'])
          : dto?.['updatedAt'] != null
            ? String(dto['updatedAt'])
            : undefined,
      modifiedBy: dto?.['modifiedBy'] != null ? String(dto['modifiedBy']) : undefined,
      previewImageUrl: preview?.kind === 'image' ? preview.dataUrl : null,
      previewPdfDataUrl: preview?.kind === 'pdf' ? preview.dataUrl : null,
      hasPdfPreview,
      hasPreview,
    };
  }

  private enrichDisplayTitle(fileName: string, category: string): string {
    const cat = category.toLowerCase();
    if (cat.includes('tax')) {
      return 'ZIMRA tax clearance certificate';
    }
    if (cat.includes('registration')) {
      return 'Company registration certificate';
    }
    if (cat.includes('address')) {
      return 'Proof of address';
    }
    if (cat.includes('licence') || cat.includes('license')) {
      return 'Business licence';
    }
    if (cat.includes('brand')) {
      return 'Organisation logo';
    }
    if (cat.includes('identification')) {
      return 'Identification document';
    }
    return fileName;
  }

  private inferSourceChannel(
    category: string,
    fileType: string,
    scope: DocumentSourceScope,
  ): DocumentSourceChannel {
    const cat = category.toLowerCase();
    const ft = fileType.toLowerCase();
    if (cat.includes('brand') || ft.includes('logo')) {
      return 'ORGANIZATION_BRANDING';
    }
    if (cat.includes('identification') || ft.includes('national') || ft.includes('passport')) {
      return scope === 'USER' ? 'USER_PROFILE' : 'KYC_ONBOARDING';
    }
    if (
      cat.includes('tax') ||
      cat.includes('licence') ||
      cat.includes('license') ||
      ft.includes('clearance') ||
      ft.includes('certificate')
    ) {
      return 'ORGANIZATION_COMPLIANCE';
    }
    if (cat.includes('registration') || cat.includes('address') || cat.includes('kyc')) {
      return 'KYC_ONBOARDING';
    }
    return 'FILE_UPLOAD_REGISTRY';
  }

  private stagedDocumentFromTableRow(row: KycDocumentTableRow): AdminStagedDocument {
    const upload: FileUploadSummary = {
      id: row.uploadId,
      originalFileName: row.fileName,
      fileType: row.fileTypeLabel,
      contentType: '',
      createdAt: row.uploadedAt,
      entityStatus: row.entityStatus,
      ownerType: row.ownerType,
      ownerId: row.ownerType === 'ORGANIZATION' ? row.organizationId : undefined,
    };
    return this.rowToStagedDocument(row, upload);
  }

  private fileTypeHintForCategory(filter: DocumentFilterId): string {
    switch (filter) {
      case 'COMPLIANCE':
        return 'TAX';
      case 'BRANDING':
        return 'LOGO';
      case 'KYC':
        return 'REGISTRATION';
      case 'PROFILE':
        return 'NATIONAL';
      default:
        return '';
    }
  }

  matchesCategoryFilter(doc: AdminStagedDocument, filter: DocumentFilterId): boolean {
    switch (filter) {
      case 'KYC':
        return doc.sourceChannel === 'KYC_ONBOARDING';
      case 'COMPLIANCE':
        return doc.sourceChannel === 'ORGANIZATION_COMPLIANCE';
      case 'PROFILE':
        return doc.sourceChannel === 'USER_PROFILE';
      case 'BRANDING':
        return doc.sourceChannel === 'ORGANIZATION_BRANDING';
      case 'OTHER':
        return doc.sourceChannel === 'FILE_UPLOAD_REGISTRY';
      default:
        return true;
    }
  }
}
