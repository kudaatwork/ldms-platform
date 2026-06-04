import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, forkJoin, map, of, switchMap, tap, throwError } from 'rxjs';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { extractPagedResult } from '../../../core/utils/api-paged-response.util';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import {
  collectUploadIdRefsFromJsonTree,
  humanizeUploadFieldKey,
} from '../../../shared/utils/collect-upload-id-refs.util';
import {
  extractFileUploadDtoFromResponse,
  extractFileUploadDtoList,
  extractOrganizationDtoFromResponse,
  extractUserDtoFromResponse,
} from '../../../shared/utils/file-upload-dto-extract.util';
import { resolveFilePreview } from '../../../shared/utils/file-upload-preview';

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

export interface VaultDocumentsQuery {
  page: number;
  size: number;
  searchQuery: string;
  categoryFilter: DocumentFilterId;
}

export interface VaultDocumentsPage {
  documents: StagedDocument[];
  totalElements: number;
}

export interface StagedDocument {
  id: number;
  fileName: string;
  displayTitle: string;
  category: string;
  sourceLabel: string;
  sourceScope: DocumentSourceScope;
  sourceChannel: DocumentSourceChannel;
  ownerType: 'USER' | 'ORGANIZATION';
  ownerId: number;
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

interface LinkedDocumentRef {
  uploadId: number;
  displayTitle: string;
  category: string;
  sourceLabel: string;
  sourceScope: DocumentSourceScope;
  sourceChannel: DocumentSourceChannel;
  ownerType: 'USER' | 'ORGANIZATION';
  ownerId: number;
  linkedFieldKey?: string;
}

@Injectable({ providedIn: 'root' })
export class DocumentsService {
  private readonly fileUploadBase = ldmsServiceUrl('file-upload-service', 'file-upload');
  private readonly userBase = ldmsServiceUrl('user-management', 'user');
  private readonly orgBase = ldmsServiceUrl('organization-management', 'organization');

  private linkedRefs: LinkedDocumentRef[] = [];
  private userOwnerId = 0;
  private orgOwnerId = 0;
  private vaultContextLoaded = false;

  constructor(
    private readonly http: HttpClient,
    private readonly authState: AuthStateService,
  ) {}

  /** Server-paged vault catalogue scoped to the signed-in user and organisation. */
  queryVaultPage(q: VaultDocumentsQuery): Observable<VaultDocumentsPage> {
    return this.ensureVaultContext().pipe(
      switchMap(() => {
        if (this.userOwnerId < 1 && this.orgOwnerId < 1) {
          return of({ documents: [], totalElements: 0 });
        }
        const body: Record<string, unknown> = {
          page: q.page,
          size: q.size,
          searchValue: q.searchQuery.trim(),
          fileType: this.fileTypeHintForCategory(q.categoryFilter),
        };
        if (this.userOwnerId > 0) {
          body['userOwnerIds'] = [this.userOwnerId];
        }
        if (this.orgOwnerId > 0) {
          body['organizationOwnerIds'] = [this.orgOwnerId];
        }
        return this.http.post<unknown>(`${this.fileUploadBase}/find-by-multiple-filters`, body).pipe(
          map((resp) => {
            const { rows, totalElements } = extractPagedResult(resp, 'fileUploadDtoPage');
            const registryDocs = rows.map((row) => {
              const dto = row as Record<string, unknown>;
              return this.toStagedDocument(
                dto,
                {
                  uploadId: Number(dto['id']),
                  displayTitle: this.fileNameFromDto(dto),
                  category: this.fileTypeLabel(dto['fileType']),
                  sourceLabel: this.buildRegistrySourceLabel(dto),
                  sourceScope: String(dto['ownerType'] ?? '').includes('ORG') ? 'ORGANIZATION' : 'USER',
                  sourceChannel: 'FILE_UPLOAD_REGISTRY',
                  ownerType: String(dto['ownerType'] ?? '').includes('ORG') ? 'ORGANIZATION' : 'USER',
                  ownerId: Number(dto['ownerId'] ?? 0),
                },
                { withPreview: false },
              );
            });
            let documents = this.mergeDocuments(this.linkedRefs, registryDocs);
            if (q.categoryFilter !== 'ALL') {
              documents = documents.filter((d) => this.matchesCategoryFilter(d, q.categoryFilter));
            }
            return { documents, totalElements };
          }),
          catchError((err) => {
            if (err instanceof HttpErrorResponse && err.status === 404) {
              return this.queryVaultPageViaOwner(q);
            }
            return throwError(() => this.mapError(err, 'Could not load documents.'));
          }),
        );
      }),
    );
  }

  /** Total documents per vault category (respects current search). */
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

  /** @deprecated Use {@link queryVaultPage}. */
  loadStagingDocuments(): Observable<StagedDocument[]> {
    return this.queryVaultPage({ page: 0, size: 100, searchQuery: '', categoryFilter: 'ALL' }).pipe(
      map(({ documents }) => documents),
    );
  }

  fetchDocumentDetail(id: number): Observable<StagedDocument | null> {
    return this.fetchById(id).pipe(
      map((dto) => {
        if (!dto) {
          return null;
        }
        return this.toStagedDocument(
          dto,
          {
            uploadId: Number(dto['id']),
            displayTitle: this.fileNameFromDto(dto),
            category: this.fileTypeLabel(dto['fileType']),
            sourceLabel: this.buildRegistrySourceLabel(dto),
            sourceScope: String(dto['ownerType'] ?? '').includes('ORG') ? 'ORGANIZATION' : 'USER',
            sourceChannel: 'FILE_UPLOAD_REGISTRY',
            ownerType: String(dto['ownerType'] ?? '').includes('ORG') ? 'ORGANIZATION' : 'USER',
            ownerId: Number(dto['ownerId'] ?? 0),
          },
          { withPreview: true },
        );
      }),
      catchError(() => of(null)),
    );
  }

  private listByOwner(ownerType: 'USER' | 'ORGANIZATION', ownerId: number): Observable<StagedDocument[]> {
    return this.http
      .get<unknown>(`${this.fileUploadBase}/find-by-owner`, {
        params: { ownerType, ownerId: String(ownerId) },
      })
      .pipe(
        map((resp) =>
          extractFileUploadDtoList(resp).map((dto) =>
            this.toStagedDocument(
              dto,
              {
                uploadId: Number(dto['id']),
                displayTitle: this.fileNameFromDto(dto),
                category: this.fileTypeLabel(dto['fileType']),
                sourceLabel: this.buildRegistrySourceLabel(dto),
                sourceScope: ownerType === 'ORGANIZATION' ? 'ORGANIZATION' : 'USER',
                sourceChannel: 'FILE_UPLOAD_REGISTRY',
                ownerType,
                ownerId,
              },
              { withPreview: false },
            ),
          ),
        ),
        catchError(() => of([])),
      );
  }

  private fetchById(id: number): Observable<Record<string, unknown> | null> {
    if (!Number.isFinite(id) || id < 1) {
      return of(null);
    }
    return this.http.get<unknown>(`${this.fileUploadBase}/find-by-id/${id}`).pipe(
      map((resp) => extractFileUploadDtoFromResponse(resp)),
      catchError(() => of(null)),
    );
  }

  private mergeDocuments(linked: LinkedDocumentRef[], registry: StagedDocument[]): StagedDocument[] {
    const byId = new Map<number, StagedDocument>();

    for (const doc of registry) {
      byId.set(doc.id, doc);
    }

    for (const ref of linked) {
      const existing = byId.get(ref.uploadId);
      if (existing) {
        byId.set(ref.uploadId, {
          ...existing,
          displayTitle: ref.displayTitle,
          category: ref.category,
          sourceLabel: ref.sourceLabel,
          sourceScope: ref.sourceScope,
          sourceChannel: ref.sourceChannel,
          ownerType: ref.ownerType,
          ownerId: ref.ownerId,
          linkedFieldKey: ref.linkedFieldKey,
        });
      }
    }

    return [...byId.values()].sort((a, b) => {
      const ta = Date.parse(a.createdAt) || 0;
      const tb = Date.parse(b.createdAt) || 0;
      return tb - ta;
    });
  }

  private buildLinkedRefsFromTree(
    root: Record<string, unknown> | null,
    ownerId: number,
    scope: DocumentSourceScope,
  ): LinkedDocumentRef[] {
    if (!root || ownerId < 1) {
      return [];
    }

    const scopeLabel = scope === 'ORGANIZATION' ? 'Organisation' : 'My profile';
    return collectUploadIdRefsFromJsonTree(root).map(({ uploadId, fieldKey }) => {
      const category = this.inferCategoryFromFieldKey(fieldKey);
      return {
        uploadId,
        displayTitle: this.enrichDisplayTitle(fieldKey, scope),
        category,
        sourceLabel: `${scopeLabel} · ${category}`,
        sourceScope: scope,
        sourceChannel: this.inferSourceChannel(fieldKey, scope),
        ownerType: scope,
        ownerId,
        linkedFieldKey: fieldKey,
      };
    });
  }

  private enrichDisplayTitle(fieldKey: string, scope: DocumentSourceScope): string {
    const base = humanizeUploadFieldKey(fieldKey);
    const key = fieldKey.toLowerCase();
    if (key.includes('taxclearance')) {
      return 'ZIMRA tax clearance certificate';
    }
    if (key.includes('registrationcertificate')) {
      return 'Company registration certificate';
    }
    if (key.includes('proofofaddress')) {
      return 'Proof of address';
    }
    if (key.includes('businesslicense')) {
      return 'Business licence';
    }
    if (key.includes('industryspecific')) {
      return 'Industry-specific licence';
    }
    if (key.includes('logo')) {
      return 'Organisation logo';
    }
    if (key.includes('nationalid')) {
      return scope === 'ORGANIZATION' ? 'Contact person national ID' : 'National ID document';
    }
    if (key.includes('passport')) {
      return scope === 'ORGANIZATION' ? 'Contact person passport' : 'Passport document';
    }
    return base;
  }

  private inferCategoryFromFieldKey(fieldKey: string): string {
    const key = fieldKey.toLowerCase();
    if (key.includes('logo')) {
      return 'Branding';
    }
    if (key.includes('tax') || key.includes('certificate')) {
      return 'Tax certificate';
    }
    if (key.includes('registration')) {
      return 'Registration';
    }
    if (key.includes('license') || key.includes('licence')) {
      return 'Licence';
    }
    if (key.includes('address')) {
      return 'Address';
    }
    if (key.includes('nationalid') || key.includes('passport')) {
      return 'Identification';
    }
    return 'Document';
  }

  private inferSourceChannel(fieldKey: string, scope: DocumentSourceScope): DocumentSourceChannel {
    const key = fieldKey.toLowerCase();
    if (key.includes('logo')) {
      return 'ORGANIZATION_BRANDING';
    }
    if (key.includes('nationalid') || key.includes('passport')) {
      return scope === 'USER' ? 'USER_PROFILE' : 'KYC_ONBOARDING';
    }
    if (
      key.includes('tax') ||
      key.includes('businesslicense') ||
      key.includes('industryspecific')
    ) {
      return 'ORGANIZATION_COMPLIANCE';
    }
    if (
      key.includes('registration') ||
      key.includes('proofofaddress') ||
      key.includes('contactperson') ||
      key.includes('kyc')
    ) {
      return 'KYC_ONBOARDING';
    }
    return 'FILE_UPLOAD_REGISTRY';
  }

  private toStagedDocument(dto: Record<string, unknown>, ref: LinkedDocumentRef, options?: { withPreview?: boolean }): StagedDocument {
    const preview =
      options?.withPreview === true ? resolveFilePreview(dto, { maxBase64Chars: 900_000 }) : null;
    const fileName = this.fileNameFromDto(dto);
    const contentType = String(dto['contentType'] ?? '').trim();
    const nameLower = fileName.toLowerCase();
    const ctLower = contentType.toLowerCase();
    const hasPdfPreview = preview?.kind === 'pdf' || ctLower.includes('pdf') || nameLower.endsWith('.pdf');
    const hasPreview = preview != null;

    return {
      id: Number(dto['id']),
      fileName,
      displayTitle: ref.displayTitle || fileName,
      category: ref.category || this.fileTypeLabel(dto['fileType']),
      sourceLabel: ref.sourceLabel,
      sourceScope: ref.sourceScope,
      sourceChannel: ref.sourceChannel,
      ownerType: ref.ownerType,
      ownerId: ref.ownerId,
      fileType: this.fileTypeLabel(dto['fileType']),
      contentType,
      fileSizeInBytes: dto['fileSizeInBytes'] != null ? Number(dto['fileSizeInBytes']) : undefined,
      createdAt: dto['createdAt'] != null ? String(dto['createdAt']) : '',
      entityStatus: dto['entityStatus'] != null ? String(dto['entityStatus']) : 'ACTIVE',
      storageProvider: dto['storageProvider'] != null ? String(dto['storageProvider']) : undefined,
      storedFileName: dto['storedFileName'] != null ? String(dto['storedFileName']) : undefined,
      fileUrl: dto['fileUrl'] != null ? String(dto['fileUrl']) : undefined,
      fileHash: dto['fileHash'] != null ? String(dto['fileHash']) : undefined,
      autoVerified: dto['autoVerified'] === true,
      autoVerificationNotes:
        dto['autoVerificationNotes'] != null ? String(dto['autoVerificationNotes']) : undefined,
      autoVerifiedAt: dto['autoVerifiedAt'] != null ? String(dto['autoVerifiedAt']) : undefined,
      autoVerificationMethod:
        dto['autoVerificationMethod'] != null ? String(dto['autoVerificationMethod']) : undefined,
      autoVerificationSource:
        dto['autoVerificationSource'] != null ? String(dto['autoVerificationSource']) : undefined,
      expiresAt: dto['expiresAt'] != null ? String(dto['expiresAt']) : undefined,
      createdBy: dto['createdBy'] != null ? String(dto['createdBy']) : undefined,
      modifiedAt:
        dto['modifiedAt'] != null
          ? String(dto['modifiedAt'])
          : dto['updatedAt'] != null
            ? String(dto['updatedAt'])
            : undefined,
      modifiedBy: dto['modifiedBy'] != null ? String(dto['modifiedBy']) : undefined,
      linkedFieldKey: ref.linkedFieldKey,
      previewImageUrl: preview?.kind === 'image' ? preview.dataUrl : null,
      previewPdfDataUrl: preview?.kind === 'pdf' ? preview.dataUrl : null,
      hasPdfPreview,
      hasPreview,
    };
  }

  private buildRegistrySourceLabel(dto: Record<string, unknown>): string {
    const ownerType = String(dto['ownerType'] ?? 'UNKNOWN');
    const ownerId = Number(dto['ownerId'] ?? 0);
    const scope = ownerType.includes('ORG') ? 'Organisation' : 'User';
    return `${scope} registry · Upload #${ownerId || '—'}`;
  }

  private fileNameFromDto(dto: Record<string, unknown>): string {
    const original = String(dto['originalFileName'] ?? '').trim();
    const stored = String(dto['storedFileName'] ?? '').trim();
    return original || stored || `Document #${dto['id'] ?? ''}`;
  }

  private fileTypeLabel(value: unknown): string {
    if (value == null) {
      return 'Document';
    }
    if (typeof value === 'object' && value !== null && 'name' in (value as object)) {
      return String((value as { name?: unknown }).name ?? '').replace(/_/g, ' ');
    }
    return String(value).replace(/_/g, ' ');
  }

  private ensureVaultContext(): Observable<void> {
    if (this.vaultContextLoaded) {
      return of(undefined);
    }
    return forkJoin({
      userResp: this.http.get<unknown>(`${this.userBase}/me`).pipe(catchError(() => of(null))),
      orgResp: this.http.get<unknown>(`${this.orgBase}/my`).pipe(catchError(() => of(null))),
    }).pipe(
      tap(({ userResp, orgResp }) => {
        const user = extractUserDtoFromResponse(userResp);
        const org = extractOrganizationDtoFromResponse(orgResp);
        this.userOwnerId = Number(user?.['id'] ?? this.authState.currentUser?.userId ?? 0);
        this.orgOwnerId = Number(org?.['id'] ?? this.authState.currentUser?.organizationId ?? 0);
        this.linkedRefs = [
          ...this.buildLinkedRefsFromTree(org, this.orgOwnerId, 'ORGANIZATION'),
          ...this.buildLinkedRefsFromTree(user, this.userOwnerId, 'USER'),
        ];
        this.vaultContextLoaded = true;
      }),
      map(() => undefined),
    );
  }

  /** Legacy fallback when the paginated filter endpoint is not deployed yet. */
  private queryVaultPageViaOwner(q: VaultDocumentsQuery): Observable<VaultDocumentsPage> {
    const ownerLoads = [
      this.userOwnerId > 0 ? this.listByOwner('USER', this.userOwnerId) : of([] as StagedDocument[]),
      this.orgOwnerId > 0 ? this.listByOwner('ORGANIZATION', this.orgOwnerId) : of([] as StagedDocument[]),
    ];
    return forkJoin(ownerLoads).pipe(
      map(([userDocs, orgDocs]) => {
        let documents = this.mergeDocuments(this.linkedRefs, [...userDocs, ...orgDocs]);
        const search = q.searchQuery.trim().toLowerCase();
        if (search) {
          documents = documents.filter(
            (d) =>
              d.displayTitle.toLowerCase().includes(search) ||
              d.fileName.toLowerCase().includes(search) ||
              d.category.toLowerCase().includes(search) ||
              String(d.id).includes(search),
          );
        }
        if (q.categoryFilter !== 'ALL') {
          documents = documents.filter((d) => this.matchesCategoryFilter(d, q.categoryFilter));
        }
        const start = q.page * q.size;
        return {
          totalElements: documents.length,
          documents: documents.slice(start, start + q.size),
        };
      }),
    );
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

  matchesCategoryFilter(doc: StagedDocument, filter: DocumentFilterId): boolean {
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

  private mapError(err: unknown, fallback: string): Error {
    if (err instanceof HttpErrorResponse) {
      if (err.status === 404) {
        return new Error(
          'Documents API returned HTTP 404. Confirm ldms-file-upload-service and ldms-organization-management are running.',
        );
      }
      if (err.status === 401 || err.status === 403) {
        return new Error('Your session cannot load documents. Sign out and sign in again.');
      }
      if (err.status === 0) {
        return new Error('Cannot reach the API gateway. Confirm ldms-api-gateway (8091) is running.');
      }
      const body = err.error as { message?: string } | undefined;
      if (body?.message) {
        return new Error(body.message);
      }
    }
    if (err instanceof Error && err.message) {
      return err;
    }
    return new Error(fallback);
  }
}
