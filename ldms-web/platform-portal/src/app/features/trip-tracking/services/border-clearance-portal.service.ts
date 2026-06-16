import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { isApiFailureEnvelope, readApiFailureMessage } from '../../../core/utils/api-paged-response.util';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';

export type BorderClearanceStatus =
  | 'AWAITING_DOCUMENTS'
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'CLEARED'
  | 'REJECTED';

export type BorderClearanceDocumentType =
  | 'CUSTOMS_DECLARATION'
  | 'COMMERCIAL_INVOICE'
  | 'BILL_OF_LADING'
  | 'PERMIT'
  | 'OTHER';

export interface BorderClearanceDocumentRow {
  id: number;
  documentType: string;
  documentTypeLabel: string;
  fileUploadId: number;
  fileName: string;
  description: string;
  createdAtLabel: string;
}

export interface BorderClearanceCaseRow {
  id: number;
  caseNumber: string;
  shipmentId: number;
  inventoryTransferId: number;
  tripId?: number;
  borderName: string;
  status: BorderClearanceStatus;
  statusLabel: string;
  statusTone: 'muted' | 'warn' | 'success' | 'danger' | 'info';
  notes: string;
  clearedAtLabel: string;
  documents: BorderClearanceDocumentRow[];
  canSubmit: boolean;
  canClear: boolean;
  canReject: boolean;
}

@Injectable({ providedIn: 'root' })
export class BorderClearancePortalService {
  private readonly base = ldmsServiceUrl('shipment-management', 'border-clearance', undefined, 'frontend');
  private readonly fileUploadBase = ldmsServiceUrl('file-upload-service', 'file-upload', undefined, 'frontend');

  constructor(private readonly http: HttpClient) {}

  listCases(status?: BorderClearanceStatus): Observable<BorderClearanceCaseRow[]> {
    return this.http
      .post<unknown>(`${this.base}/find-by-multiple-filters`, { status: status ?? undefined })
      .pipe(
        map((resp) =>
          this.extractList(resp, 'borderClearanceCaseDtoList').map((dto) => this.mapCase(dto)),
        ),
        catchError((err) => throwError(() => this.toError(err))),
      );
  }

  findById(id: number): Observable<BorderClearanceCaseRow> {
    return this.http.get<unknown>(`${this.base}/find-by-id/${id}`).pipe(
      map((resp) => this.mapCase(this.extractSingle(resp, 'borderClearanceCaseDto'))),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  uploadDocument(organizationId: number, file: File): Observable<number> {
    const form = new FormData();
    form.append('files', file, file.name);
    form.append(
      'fileUploadRequest',
      JSON.stringify({
        ownerType: 'ORGANIZATION',
        ownerId: organizationId,
        filesMetadata: [{ fileType: 'OTHER' }],
      }),
    );
    return this.http.post<unknown>(`${this.fileUploadBase}/upload`, form).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const envelope = this.unwrap(resp);
        const dto = (envelope['fileUploadDto'] as Record<string, unknown>) ?? envelope;
        const id = Number(dto['id'] ?? dto['fileUploadId'] ?? 0);
        if (!id) {
          throw new Error('Upload did not return a file id.');
        }
        return id;
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  addDocument(payload: {
    caseId: number;
    fileUploadId: number;
    documentType: BorderClearanceDocumentType;
    fileName: string;
    description?: string;
  }): Observable<void> {
    return this.http.post<unknown>(`${this.base}/add-document`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  submit(caseId: number): Observable<void> {
    return this.http.post<unknown>(`${this.base}/submit/${caseId}`, {}).pipe(
      map((resp) => {
        this.assertSuccess(resp);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  clear(caseId: number, borderName?: string, notes?: string): Observable<void> {
    return this.http.post<unknown>(`${this.base}/clear/${caseId}`, { borderName, notes }).pipe(
      map((resp) => {
        this.assertSuccess(resp);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  reject(caseId: number, notes?: string): Observable<void> {
    return this.http.post<unknown>(`${this.base}/reject/${caseId}`, { notes }).pipe(
      map((resp) => {
        this.assertSuccess(resp);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  private mapCase(dto: Record<string, unknown>): BorderClearanceCaseRow {
    const status = String(dto['status'] ?? 'AWAITING_DOCUMENTS') as BorderClearanceStatus;
    const docsRaw = Array.isArray(dto['documents']) ? dto['documents'] : [];
    const documents = docsRaw.map((d) => this.mapDocument(d as Record<string, unknown>));
    return {
      id: Number(dto['id'] ?? 0),
      caseNumber: String(dto['caseNumber'] ?? `BCC-${dto['id']}`),
      shipmentId: Number(dto['shipmentId'] ?? 0),
      inventoryTransferId: Number(dto['inventoryTransferId'] ?? 0),
      tripId: dto['tripId'] != null ? Number(dto['tripId']) : undefined,
      borderName: String(dto['borderName'] ?? ''),
      status,
      statusLabel: this.statusLabel(status),
      statusTone: this.statusTone(status),
      notes: String(dto['notes'] ?? ''),
      clearedAtLabel: this.formatDate(dto['clearedAt']),
      documents,
      canSubmit: status === 'AWAITING_DOCUMENTS' && documents.length > 0,
      canClear: status === 'SUBMITTED' || status === 'UNDER_REVIEW',
      canReject: status === 'SUBMITTED' || status === 'UNDER_REVIEW',
    };
  }

  private mapDocument(dto: Record<string, unknown>): BorderClearanceDocumentRow {
    const type = String(dto['documentType'] ?? 'OTHER');
    return {
      id: Number(dto['id'] ?? 0),
      documentType: type,
      documentTypeLabel: type.replace(/_/g, ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase()),
      fileUploadId: Number(dto['fileUploadId'] ?? 0),
      fileName: String(dto['fileName'] ?? 'Document'),
      description: String(dto['description'] ?? ''),
      createdAtLabel: this.formatDate(dto['createdAt']),
    };
  }

  private statusLabel(status: BorderClearanceStatus): string {
    const map: Record<BorderClearanceStatus, string> = {
      AWAITING_DOCUMENTS: 'Awaiting documents',
      SUBMITTED: 'Submitted — at border',
      UNDER_REVIEW: 'Under review',
      CLEARED: 'Cleared',
      REJECTED: 'Rejected',
    };
    return map[status] ?? status;
  }

  private statusTone(status: BorderClearanceStatus): BorderClearanceCaseRow['statusTone'] {
    switch (status) {
      case 'SUBMITTED':
      case 'UNDER_REVIEW':
        return 'warn';
      case 'CLEARED':
        return 'success';
      case 'REJECTED':
        return 'danger';
      default:
        return 'muted';
    }
  }

  private formatDate(value: unknown): string {
    if (!value) return '—';
    try {
      return new Date(String(value)).toLocaleString('en-GB', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return String(value);
    }
  }

  private assertSuccess(resp: unknown): void {
    if (isApiFailureEnvelope(resp)) {
      throw new Error(readApiFailureMessage(resp, 'Request failed.'));
    }
    const root = this.unwrap(resp);
    if (root['success'] === false) {
      throw new Error(String(root['message'] ?? 'Request failed.'));
    }
  }

  private extractList(resp: unknown, key: string): Array<Record<string, unknown>> {
    this.assertSuccess(resp);
    const root = this.unwrap(resp);
    const list = root[key];
    return Array.isArray(list) ? (list as Array<Record<string, unknown>>) : [];
  }

  private extractSingle(resp: unknown, key: string): Record<string, unknown> {
    this.assertSuccess(resp);
    const root = this.unwrap(resp);
    const dto = root[key];
    return dto && typeof dto === 'object' ? (dto as Record<string, unknown>) : {};
  }

  private unwrap(resp: unknown): Record<string, unknown> {
    if (!resp || typeof resp !== 'object') return {};
    const root = resp as Record<string, unknown>;
    const body = root['body'];
    return body && typeof body === 'object' ? (body as Record<string, unknown>) : root;
  }

  private toError(err: unknown): Error {
    if (err instanceof Error) return err;
    return new Error('Request failed.');
  }
}
