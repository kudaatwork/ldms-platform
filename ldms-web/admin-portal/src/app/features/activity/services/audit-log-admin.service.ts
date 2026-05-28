import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, of, throwError } from 'rxjs';
import {
  extractPagedResult,
  isApiFailureEnvelope,
  readApiFailureMessage,
} from '../../../core/utils/api-paged-response.util';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';

/** POST body for `find-by-multiple-filters` (ldms-audit-trail). */
export interface AuditLogMultipleFiltersRequest {
  page: number;
  size: number;
  searchValue: string;
  serviceName: string;
  username: string;
  eventType: string;
  httpStatusCode: number | null;
  from: string | null;
  to: string | null;
  sortBy: string;
  sortDir: string;
  /** Substring match (server-side); optional. */
  action?: string;
  requestUrl?: string;
  httpMethod?: string;
  traceId?: string;
}

export interface AuditLogDto {
  id?: number;
  action?: string;
  eventType?: string;
  username?: string;
  requestTimestamp?: string;
  /** ISO-8601 when serialized from the API */
  responseTimestamp?: string;
  httpMethod?: string;
  requestUrl?: string;
  serviceName?: string;
  clientIpAddress?: string;
  traceId?: string;
  exceptionMessage?: string;
  httpStatusCode?: number;
  responseTimeMs?: number;
  requestHeaders?: string | null;
  requestPayload?: string | null;
  responsePayload?: string | null;
  curlCommand?: string | null;
}

export interface AuditLogResponse {
  statusCode?: number;
  success?: boolean;
  message?: string;
  auditLog?: AuditLogDto;
  auditLogPage?: {
    content?: AuditLogDto[];
    totalElements?: number;
  };
  churnOut?: AuditLogChurnOutDto;
  churnLaunch?: AuditLogChurnLaunchDto;
  churnHistoryPage?: {
    content?: AuditLogChurnHistoryDto[];
    totalElements?: number;
  };
}

/** Immediate response when Spring Batch churn job is accepted (runs asynchronously). */
export interface AuditLogChurnLaunchDto {
  jobExecutionId: number;
  batchReference: string;
  acceptedAt: string;
  triggerType: string;
  triggeredBy: string;
  message?: string;
}

export interface AuditLogChurnOutDto {
  historyId: number;
  triggerType: string;
  triggeredBy: string;
  triggeredAt: string;
  deletedLogCount: number;
  oldestRequestTimestamp?: string | null;
  newestRequestTimestamp?: string | null;
  batchReference: string;
}

export interface AuditLogChurnHistoryDto {
  id: number;
  batchReference: string;
  triggerType: string;
  triggeredBy: string;
  triggeredAt: string;
  deletedLogCount: number;
  oldestRequestTimestamp?: string | null;
  newestRequestTimestamp?: string | null;
  churnStatus?: string;
  jobExecutionId?: number | null;
  failureReason?: string | null;
  completedAt?: string | null;
}

export interface ChurnOutHistoryFilters {
  page?: number;
  size?: number;
  searchValue?: string;
  triggerType?: string;
  status?: string;
  triggeredBy?: string;
  batchReference?: string;
  from?: string;
  to?: string;
}

/** Audit HTTP request log — admin portal uses {@code backoffice} (JWT only), like organizations. */
@Injectable({
  providedIn: 'root',
})
export class AuditLogAdminService {
  constructor(private readonly http: HttpClient) {}

  /** Admin portal: {@code backoffice} (JWT only). {@code frontend} enforces audit {@code @PreAuthorize} roles. */
  private url(operation: string): string {
    return ldmsServiceUrl('audit-trail', 'audit-log', operation, 'backoffice');
  }

  /**
   * Server-paged request log (same transport as organizations {@code queryTablePage}).
   */
  queryRequestLogPage(request: AuditLogMultipleFiltersRequest): Observable<{
    rows: AuditLogDto[];
    totalElements: number;
  }> {
    return this.http.post<AuditLogResponse>(this.url('find-by-multiple-filters'), request).pipe(
      map((resp) => this.mapRequestLogPage(resp)),
      catchError((err) => {
        if (err instanceof HttpErrorResponse && err.status === 404) {
          return of({ rows: [], totalElements: 0 });
        }
        return throwError(() => this.toRequestLogError(err));
      }),
    );
  }

  findByMultipleFilters(request: AuditLogMultipleFiltersRequest): Observable<AuditLogResponse> {
    return this.http.post<AuditLogResponse>(this.url('find-by-multiple-filters'), request);
  }

  /** Full row including payloads and curl (backend uses `includeLargePayloads` for this path). */
  findById(id: number): Observable<AuditLogResponse> {
    return this.http.get<AuditLogResponse>(this.url(`find-by-id/${id}`));
  }

  /**
   * Same filter body as {@link findByMultipleFilters}; `format` is `csv`, `xlsx`, or `pdf`
   * (gateway maps to audit-trail `/export?format=`).
   */
  exportAuditLogs(request: AuditLogMultipleFiltersRequest, format: 'csv' | 'xlsx' | 'pdf'): Observable<Blob> {
    const url = this.url('export');
    const apiFormat = format === 'xlsx' ? 'xlsx' : format;
    const params = new HttpParams().set('format', apiFormat);
    return this.http.post(url, request, {
      params,
      responseType: 'blob',
    });
  }

  churnOutRequestLogs(): Observable<AuditLogResponse> {
    return this.http.post<AuditLogResponse>(this.url('churn-out'), {});
  }

  getChurnOutHistory(page = 0, size = 20, filters?: ChurnOutHistoryFilters): Observable<AuditLogResponse> {
    const url = this.url('churn-history/find-by-multiple-filters');
    const body: ChurnOutHistoryFilters = {
      page,
      size,
      searchValue: '',
      triggerType: filters?.triggerType || undefined,
      status: filters?.status || undefined,
      triggeredBy: filters?.triggeredBy || undefined,
      batchReference: filters?.batchReference || undefined,
      from: filters?.from || undefined,
      to: filters?.to || undefined,
    };
    return this.http.post<AuditLogResponse>(url, body);
  }

  exportChurnOutHistory(filters: ChurnOutHistoryFilters, format: 'csv' | 'xlsx' | 'pdf'): Observable<Blob> {
    const url = this.url('churn-history/export');
    const apiFormat = format === 'xlsx' ? 'xlsx' : format;
    const params = new HttpParams().set('format', apiFormat);
    return this.http.post(url, filters, { params, responseType: 'blob' });
  }

  private mapRequestLogPage(resp: AuditLogResponse): { rows: AuditLogDto[]; totalElements: number } {
    if (isApiFailureEnvelope(resp)) {
      throw new Error(readApiFailureMessage(resp, 'Failed to load request logs.'));
    }
    const { rows, totalElements } = extractPagedResult(resp, 'auditLogPage');
    return { rows: rows as AuditLogDto[], totalElements };
  }

  private toRequestLogError(err: unknown): Error {
    if (err instanceof Error) {
      return err;
    }
    if (err instanceof HttpErrorResponse) {
      const body = err.error as AuditLogResponse | undefined;
      if (body?.message?.trim()) {
        return new Error(body.message.trim());
      }
      if (err.status === 401) {
        return new Error('Not signed in. Log in again to continue.');
      }
      if (err.status === 0) {
        return new Error('Request failed before the server responded. Check that the API gateway and audit-trail service are running.');
      }
      return new Error(err.message || 'Failed to load request logs.');
    }
    return new Error('Failed to load request logs.');
  }
}
