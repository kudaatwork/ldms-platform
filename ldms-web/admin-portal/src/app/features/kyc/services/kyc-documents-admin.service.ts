import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, from, map, mergeMap, of, switchMap, tap, throwError, toArray } from 'rxjs';
import { filterByGlobalAndColumns } from '@shared/utils/table-search.util';
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
}
