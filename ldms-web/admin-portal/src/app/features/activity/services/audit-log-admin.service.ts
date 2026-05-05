import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

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

/**
 * Audit HTTP request log (requests table) — same gateway URL pattern as {@link LocationsService}
 * (`{apiUrl}/ldms-audit-trail/v1/{apiSurface}/...`).
 */
@Injectable({
  providedIn: 'root',
})
export class AuditLogAdminService {
  private readonly base = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  findByMultipleFilters(request: AuditLogMultipleFiltersRequest): Observable<AuditLogResponse> {
    const url = `${this.base}/ldms-audit-trail/v1/${environment.apiSurface}/audit-log/find-by-multiple-filters`;
    return this.http.post<AuditLogResponse>(url, request);
  }

  /** Full row including payloads and curl (backend uses `includeLargePayloads` for this path). */
  findById(id: number): Observable<AuditLogResponse> {
    const url = `${this.base}/ldms-audit-trail/v1/${environment.apiSurface}/audit-log/find-by-id/${id}`;
    return this.http.get<AuditLogResponse>(url);
  }

  /**
   * Same filter body as {@link findByMultipleFilters}; `format` is `csv`, `xlsx`, or `pdf`
   * (gateway maps to audit-trail `/export?format=`).
   */
  exportAuditLogs(request: AuditLogMultipleFiltersRequest, format: 'csv' | 'xlsx' | 'pdf'): Observable<Blob> {
    const url = `${this.base}/ldms-audit-trail/v1/${environment.apiSurface}/audit-log/export`;
    const apiFormat = format === 'xlsx' ? 'xlsx' : format;
    const params = new HttpParams().set('format', apiFormat);
    return this.http.post(url, request, {
      params,
      responseType: 'blob',
    });
  }

  churnOutRequestLogs(): Observable<AuditLogResponse> {
    const url = `${this.base}/ldms-audit-trail/v1/${environment.apiSurface}/audit-log/churn-out`;
    return this.http.post<AuditLogResponse>(url, {});
  }

  getChurnOutHistory(page = 0, size = 20, filters?: ChurnOutHistoryFilters): Observable<AuditLogResponse> {
    const url = `${this.base}/ldms-audit-trail/v1/${environment.apiSurface}/audit-log/churn-history/find-by-multiple-filters`;
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
    const url = `${this.base}/ldms-audit-trail/v1/${environment.apiSurface}/audit-log/churn-history/export`;
    const apiFormat = format === 'xlsx' ? 'xlsx' : format;
    const params = new HttpParams().set('format', apiFormat);
    return this.http.post(url, filters, { params, responseType: 'blob' });
  }
}
