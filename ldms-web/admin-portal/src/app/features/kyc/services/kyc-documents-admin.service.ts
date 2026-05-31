import { Injectable } from '@angular/core';
import { Observable, catchError, forkJoin, map, of } from 'rxjs';
import { FileUploadAdminService, FileUploadSummary } from '../../../core/services/file-upload-admin.service';
import { OrganizationsAdminService } from '../../organizations/services/organizations-admin.service';
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
 * Admin compliance document catalogue — every row in file-upload (metadata only),
 * enriched with organisation names and linked-field labels where available.
 */
@Injectable({ providedIn: 'root' })
export class KycDocumentsAdminService {
  private readonly maxDocuments = 2000;
  private readonly maxOrganizations = 250;
  private readonly orgPageSize = 100;

  constructor(
    private readonly fileUpload: FileUploadAdminService,
    private readonly organizations: OrganizationsAdminService,
  ) {}

  loadCatalog(): Observable<KycDocumentTableRow[]> {
    return forkJoin({
      uploads: this.fileUpload.loadAllMetadata(this.maxDocuments),
      orgDtos: this.organizations.fetchOrganizationDtoPages(this.maxOrganizations, this.orgPageSize),
    }).pipe(
      map(({ uploads, orgDtos }) => this.rowsFromUploadsAndOrganizations(uploads, orgDtos)),
      catchError(() => of([])),
    );
  }

  private rowsFromUploadsAndOrganizations(
    uploads: FileUploadSummary[],
    orgDtos: Record<string, unknown>[],
  ): KycDocumentTableRow[] {
    const orgById = new Map<number, OrgContext>();
    const linkedByUploadId = new Map<number, LinkedDocRef>();

    for (const dto of orgDtos) {
      const orgId = Number(dto['id'] ?? 0);
      if (!Number.isFinite(orgId) || orgId < 1) {
        continue;
      }
      const orgName = String(dto['name'] ?? '').trim() || `Organisation #${orgId}`;
      const kycRaw = String(dto['kycStatus'] ?? '').trim();
      const kycStatusLabel = kycRaw ? kycStatusPresentation(kycRaw as never).label : '—';
      orgById.set(orgId, { organizationId: orgId, organizationName: orgName, kycStatusLabel });

      for (const doc of this.organizations.verificationDocumentsFromOrganizationDto(dto)) {
        linkedByUploadId.set(doc.uploadId, {
          ...doc,
          organizationId: orgId,
          organizationName: orgName,
          kycStatusLabel,
        });
      }
    }

    const byUploadId = new Map<number, KycDocumentTableRow>();

    for (const upload of uploads) {
      byUploadId.set(upload.id, this.uploadToRow(upload, orgById, linkedByUploadId));
    }

    for (const ref of linkedByUploadId.values()) {
      if (!byUploadId.has(ref.uploadId)) {
        byUploadId.set(ref.uploadId, this.linkedRefToRow(ref));
      }
    }

    return [...byUploadId.values()].sort((a, b) => {
      const ta = Date.parse(a.uploadedAt) || 0;
      const tb = Date.parse(b.uploadedAt) || 0;
      return tb - ta;
    });
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

  private linkedRefToRow(ref: LinkedDocRef): KycDocumentTableRow {
    const pres = this.entityStatusPresentation('ACTIVE');
    return {
      uploadId: ref.uploadId,
      fileName: ref.fileName,
      type: ref.category,
      fileTypeLabel: ref.fileType,
      status: pres.css,
      statusLabel: pres.label,
      organizationId: ref.organizationId,
      organizationName: ref.organizationName,
      kycStatusLabel: ref.kycStatusLabel,
      uploadedAt: ref.uploadedAt !== '—' ? ref.uploadedAt : '',
      entityStatus: 'ACTIVE',
      ownerType: 'ORGANIZATION',
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
