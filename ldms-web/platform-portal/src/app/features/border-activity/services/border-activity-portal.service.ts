import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { isApiFailureEnvelope, readApiFailureMessage } from '../../../core/utils/api-paged-response.util';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';

export type BorderActivityStatus =
  | 'AWAITING_DOCUMENTS'
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'CLEARED'
  | 'REJECTED';

export interface BorderActivityDocumentRow {
  id: number;
  documentType: string;
  documentTypeLabel: string;
  fileUploadId: number;
  fileName: string;
  description: string;
  createdAtLabel: string;
}

export interface BorderActivityCaseRow {
  id: number;
  caseNumber: string;
  shipmentId: number;
  inventoryTransferId: number;
  tripId?: number;
  borderName: string;
  status: BorderActivityStatus;
  statusLabel: string;
  statusTone: 'muted' | 'warn' | 'success' | 'danger' | 'info';
  notes: string;
  clearedAtLabel: string;
  submittedAtLabel: string;
  documents: BorderActivityDocumentRow[];
  canClear: boolean;
  canReject: boolean;
  clearingAgentName: string;
  transporterName: string;
  driverName: string;
  vehicleReg: string;
  cargoDescription: string;
}

export interface BorderActivityMetrics {
  total: number;
  awaiting: number;
  underReview: number;
  cleared: number;
  rejected: number;
}

@Injectable({ providedIn: 'root' })
export class BorderActivityPortalService {
  private readonly base = ldmsServiceUrl('shipment-management', 'border-clearance', undefined, 'frontend');

  constructor(private readonly http: HttpClient) {}

  listCases(status?: BorderActivityStatus): Observable<BorderActivityCaseRow[]> {
    return this.http
      .post<unknown>(`${this.base}/find-by-multiple-filters`, { status: status ?? undefined })
      .pipe(
        map((resp) =>
          this.extractList(resp, 'borderClearanceCaseDtoList').map((dto) => this.mapCase(dto)),
        ),
        catchError((err) => throwError(() => this.toError(err))),
      );
  }

  findById(id: number): Observable<BorderActivityCaseRow> {
    return this.http.get<unknown>(`${this.base}/find-by-id/${id}`).pipe(
      map((resp) => this.mapCase(this.extractSingle(resp, 'borderClearanceCaseDto'))),
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

  private mapCase(dto: Record<string, unknown>): BorderActivityCaseRow {
    const status = String(dto['status'] ?? 'AWAITING_DOCUMENTS') as BorderActivityStatus;
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
      submittedAtLabel: this.formatDate(dto['submittedAt']),
      documents,
      canClear: status === 'SUBMITTED' || status === 'UNDER_REVIEW',
      canReject: status === 'SUBMITTED' || status === 'UNDER_REVIEW',
      clearingAgentName: String(dto['clearingAgentName'] ?? '—'),
      transporterName: String(dto['transporterName'] ?? '—'),
      driverName: String(dto['driverName'] ?? '—'),
      vehicleReg: String(dto['vehicleReg'] ?? '—'),
      cargoDescription: String(dto['cargoDescription'] ?? '—'),
    };
  }

  private mapDocument(dto: Record<string, unknown>): BorderActivityDocumentRow {
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

  private statusLabel(status: BorderActivityStatus): string {
    const map: Record<BorderActivityStatus, string> = {
      AWAITING_DOCUMENTS: 'Awaiting documents',
      SUBMITTED: 'Submitted — at border',
      UNDER_REVIEW: 'Under review',
      CLEARED: 'Cleared',
      REJECTED: 'Rejected',
    };
    return map[status] ?? status;
  }

  private statusTone(status: BorderActivityStatus): BorderActivityCaseRow['statusTone'] {
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
    return new Error(String(err ?? 'Request failed.'));
  }
}
