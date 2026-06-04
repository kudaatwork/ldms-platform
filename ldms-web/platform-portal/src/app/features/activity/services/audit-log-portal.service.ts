import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, of, shareReplay, switchMap, throwError } from 'rxjs';
import {
  extractPagedResult,
  isApiFailureEnvelope,
  readApiFailureMessage,
} from '../../../core/utils/api-paged-response.util';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import { OrgContextService } from '../../../core/services/org-context.service';
import { UsersPortalService } from '../../users/services/users-portal.service';

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
  action?: string;
  requestUrl?: string;
  httpMethod?: string;
  traceId?: string;
  clientPlatform?: string;
  actionsIn?: string[];
  excludeActions?: string[];
  /** Restrict results to these login usernames (organisation workspace scope). */
  usernamesIn?: string[];
}

export const LOGIN_AUDIT_ACTIONS = ['USER_AUTHENTICATION', 'USER_AUTHENTICATION_GOOGLE'] as const;

export const USER_ACTIVITY_EXCLUDED_ACTIONS = [
  ...LOGIN_AUDIT_ACTIONS,
  'REFRESH_TOKEN',
  'HTTP_REQUEST',
] as const;

export interface AuditLogDto {
  id?: number;
  action?: string;
  eventType?: string;
  username?: string;
  requestTimestamp?: string;
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
  clientPlatform?: string;
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
}

/**
 * Organisation workspace audit log API — uses {@code backoffice} like the admin portal.
 * The {@code frontend} surface requires {@code SEARCH_AUDIT_LOGS} in the JWT; org {@code Administrator}
 * groups often exceed the compact roles claim limit, which produced 403 on login & activity.
 */
/** Sentinel username that never matches — used when a filter points outside the org. */
const NO_MATCH_USERNAME = '__ldms_org_scope_no_match__';

@Injectable({
  providedIn: 'root',
})
export class AuditLogPortalService {
  private cachedOrgId: number | null = null;
  private cachedOrgUsernames: string[] | null = null;
  private orgUsernamesLoad$: Observable<string[]> | null = null;

  constructor(
    private readonly http: HttpClient,
    private readonly orgContext: OrgContextService,
    private readonly usersPortal: UsersPortalService,
  ) {}

  private url(operation: string): string {
    return ldmsServiceUrl('audit-trail', 'audit-log', operation, 'backoffice');
  }

  queryLoginEventsPage(
    request: AuditLogMultipleFiltersRequest,
  ): Observable<{ rows: AuditLogDto[]; totalElements: number }> {
    return this.scopeRequestToOrganization(request).pipe(
      switchMap((scoped) => {
        if (scoped.emptyResult) {
          return of({ rows: [], totalElements: 0 });
        }
        const payload: AuditLogMultipleFiltersRequest = {
          ...scoped.request,
          eventType: 'SERVICE_METHOD',
          serviceName: scoped.request.serviceName?.trim() ?? '',
          actionsIn:
            scoped.request.actionsIn && scoped.request.actionsIn.length > 0
              ? scoped.request.actionsIn
              : [...LOGIN_AUDIT_ACTIONS],
        };
        return this.queryAuditLogPage(payload);
      }),
    );
  }

  queryUserActivityPage(
    request: AuditLogMultipleFiltersRequest,
    _username: string,
  ): Observable<{ rows: AuditLogDto[]; totalElements: number }> {
    return this.scopeRequestToOrganization(request).pipe(
      switchMap((scoped) => {
        if (scoped.emptyResult) {
          return of({ rows: [], totalElements: 0 });
        }
        const payload: AuditLogMultipleFiltersRequest = {
          ...scoped.request,
          eventType: scoped.request.eventType?.trim() ?? '',
          excludeActions: [...USER_ACTIVITY_EXCLUDED_ACTIONS],
        };
        return this.queryAuditLogPage(payload);
      }),
    );
  }

  findById(id: number): Observable<AuditLogResponse> {
    return this.http.get<AuditLogResponse>(this.url(`find-by-id/${id}`));
  }

  exportAuditLogs(request: AuditLogMultipleFiltersRequest, format: 'csv' | 'xlsx' | 'pdf'): Observable<Blob> {
    return this.scopeRequestToOrganization(request).pipe(
      switchMap((scoped) => {
        if (scoped.emptyResult) {
          return throwError(() => new Error('No users in your organisation match the current filters.'));
        }
        const url = this.url('export');
        const apiFormat = format === 'xlsx' ? 'xlsx' : format;
        const params = new HttpParams().set('format', apiFormat);
        return this.http.post(url, scoped.request, {
          params,
          responseType: 'blob',
        });
      }),
    );
  }

  /** Resolves login usernames for the signed-in user's organisation. */
  resolveOrganizationUsernames(): Observable<string[]> {
    return this.loadOrganizationUsernames();
  }

  /** Warms the organisation username cache (call when entering Login & activity). */
  prefetchOrganizationUsernames(): Observable<string[]> {
    return this.loadOrganizationUsernames();
  }

  isUsernameInOrganization(username: string, orgUsernames: string[]): boolean {
    const normalized = username.trim().toLowerCase();
    if (!normalized) {
      return false;
    }
    return orgUsernames.some((u) => u.toLowerCase() === normalized);
  }

  private scopeRequestToOrganization(
    request: AuditLogMultipleFiltersRequest,
  ): Observable<{ request: AuditLogMultipleFiltersRequest; emptyResult: boolean }> {
    const orgId = this.orgContext.organizationId;
    if (orgId == null) {
      return of({ request, emptyResult: true });
    }

    const explicit = request.username?.trim();
    if (explicit) {
      return this.loadOrganizationUsernames().pipe(
        map((usernames) => {
          if (usernames.length > 0 && !this.isUsernameInOrganization(explicit, usernames)) {
            return {
              request: { ...request, username: explicit, usernamesIn: [NO_MATCH_USERNAME] },
              emptyResult: false,
            };
          }
          return {
            request: { ...request, username: explicit, usernamesIn: [explicit] },
            emptyResult: false,
          };
        }),
        catchError(() =>
          of({
            request: { ...request, username: explicit, usernamesIn: [explicit] },
            emptyResult: false,
          }),
        ),
      );
    }

    return this.loadOrganizationUsernames().pipe(
      map((usernames) => {
        if (usernames.length === 0) {
          return { request, emptyResult: true };
        }
        return {
          request: { ...request, usernamesIn: usernames },
          emptyResult: false,
        };
      }),
    );
  }

  private loadOrganizationUsernames(): Observable<string[]> {
    const orgId = this.orgContext.organizationId;
    if (orgId == null) {
      return of([]);
    }
    if (this.cachedOrgId === orgId && this.cachedOrgUsernames != null) {
      return of(this.cachedOrgUsernames);
    }
    if (this.cachedOrgId !== orgId) {
      this.orgUsernamesLoad$ = null;
      this.cachedOrgUsernames = null;
    }
    if (!this.orgUsernamesLoad$) {
      this.orgUsernamesLoad$ = this.usersPortal.queryOrganizationUsernames(orgId).pipe(
        map((usernames) => {
          this.cachedOrgId = orgId;
          this.cachedOrgUsernames = usernames;
          return usernames;
        }),
        catchError(() => {
          return this.usersPortal.queryUsersForOrganization(orgId).pipe(
            map(({ rows }) => {
              const fallback = [
                ...new Set(
                  rows.map((r) => r.username?.trim()).filter((u): u is string => !!u),
                ),
              ];
              this.cachedOrgId = orgId;
              this.cachedOrgUsernames = fallback;
              return fallback;
            }),
            catchError(() => of([])),
          );
        }),
        shareReplay(1),
      );
    }
    return this.orgUsernamesLoad$;
  }

  private queryAuditLogPage(request: AuditLogMultipleFiltersRequest): Observable<{
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

  private mapRequestLogPage(resp: AuditLogResponse): { rows: AuditLogDto[]; totalElements: number } {
    if (isApiFailureEnvelope(resp)) {
      throw new Error(readApiFailureMessage(resp, 'Failed to load audit logs.'));
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
      if (err.status === 403) {
        return new Error('You do not have permission to view audit logs.');
      }
      if (err.status === 0) {
        return new Error(
          'Request failed before the server responded. Check that the API gateway and audit-trail service are running.',
        );
      }
      return new Error(err.message || 'Failed to load audit logs.');
    }
    return new Error('Failed to load audit logs.');
  }
}
