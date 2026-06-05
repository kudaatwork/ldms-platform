import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';

export type HelpArticleCategory =
  | 'GETTING_STARTED'
  | 'OPERATIONS'
  | 'ACCOUNT'
  | 'BILLING'
  | 'PLATFORM';

export type SupportTicketCategory =
  | 'GENERAL'
  | 'TECHNICAL'
  | 'BILLING'
  | 'ACCESS'
  | 'SECURITY'
  | 'OPERATIONS';

export type SupportTicketPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';

export type SupportTicketStatus =
  | 'OPEN'
  | 'IN_PROGRESS'
  | 'WAITING_ON_CUSTOMER'
  | 'RESOLVED'
  | 'CLOSED';

export interface HelpArticle {
  id: number;
  slug: string;
  title: string;
  summary: string;
  bodyMarkdown: string;
  category: HelpArticleCategory;
  sortOrder: number;
}

export interface SupportTicketMessage {
  id: number;
  supportTicketId: number;
  authorUsername: string;
  authorRole: 'REQUESTER' | 'HANDLER' | 'SYSTEM';
  visibility: 'PUBLIC' | 'INTERNAL';
  body: string;
  createdAt: string;
}

export interface SupportTicket {
  id: number;
  ticketNumber: string;
  subject: string;
  description: string;
  category: SupportTicketCategory;
  priority: SupportTicketPriority;
  status: SupportTicketStatus;
  requesterUsername: string;
  requesterEmail: string;
  organizationId?: number;
  organizationName?: string;
  assignedHandlerUserId?: number;
  assignedHandlerUsername?: string;
  resolvedAt?: string;
  closedAt?: string;
  createdAt: string;
  modifiedAt?: string;
  messages?: SupportTicketMessage[];
}

export interface PlatformStatusSummary {
  checkedAt: string;
  overallStatus: 'OPERATIONAL' | 'DEGRADED' | 'OUTAGE';
  headline: string;
  detail: string;
  totalServices: number;
  upCount: number;
  downCount: number;
}

export interface CreateSupportTicketPayload {
  subject: string;
  description: string;
  category: SupportTicketCategory;
  priority?: SupportTicketPriority;
}

interface HelpSupportApiResponse {
  isSuccess?: boolean;
  success?: boolean;
  statusCode?: number;
  message?: string;
  errorMessages?: string[];
  helpArticleDto?: HelpArticle;
  helpArticleDtoList?: HelpArticle[];
  supportTicketDto?: SupportTicket;
  supportTicketDtoList?: SupportTicket[];
  platformStatusDto?: PlatformStatusSummary;
}

function apiOk(resp: HelpSupportApiResponse): boolean {
  return (
    resp.isSuccess === true ||
    resp.success === true ||
    (resp.statusCode != null && resp.statusCode >= 200 && resp.statusCode < 300)
  );
}

function mapHttpError(err: unknown, fallback: string): Error {
  if (err instanceof HttpErrorResponse) {
    const body = err.error as HelpSupportApiResponse | undefined;
    if (err.status === 404) {
      return new Error(
        'Help & Support API returned HTTP 404. Restart ldms-user-management (8086) so the new endpoints and Flyway migration V6 are applied, then retry.',
      );
    }
    if (err.status === 401 || err.status === 403) {
      return new Error('Your session cannot access Help & Support. Sign out and sign in again.');
    }
    if (err.status === 0) {
      return new Error('Cannot reach the API gateway. Confirm ldms-api-gateway (8091) and ldms-user-management (8086) are running.');
    }
    if (body?.message) {
      return new Error(body.message);
    }
    if (body?.errorMessages?.length) {
      return new Error(body.errorMessages.join(' '));
    }
    return new Error(`${fallback} (HTTP ${err.status}).`);
  }
  if (err instanceof Error && err.message) {
    return err;
  }
  return new Error(fallback);
}

@Injectable({ providedIn: 'root' })
export class HelpSupportService {
  private readonly base = ldmsServiceUrl('user-management', 'help-support');

  constructor(private readonly http: HttpClient) {}

  fetchArticles(category?: HelpArticleCategory): Observable<HelpArticle[]> {
    const url = category ? `${this.base}/articles?category=${category}` : `${this.base}/articles`;
    return this.http.get<HelpSupportApiResponse>(url).pipe(
      map((resp) => {
        if (!apiOk(resp)) {
          throw new Error(resp.message ?? 'Could not load help articles.');
        }
        return resp.helpArticleDtoList ?? [];
      }),
      catchError((err) => throwError(() => mapHttpError(err, 'Could not load help articles.'))),
    );
  }

  fetchArticle(slug: string): Observable<HelpArticle> {
    return this.http.get<HelpSupportApiResponse>(`${this.base}/articles/${encodeURIComponent(slug)}`).pipe(
      map((resp) => {
        if (!apiOk(resp) || !resp.helpArticleDto) {
          throw new Error(resp.message ?? 'Article not found.');
        }
        return resp.helpArticleDto;
      }),
      catchError((err) => throwError(() => mapHttpError(err, 'Article not found.'))),
    );
  }

  createTicket(payload: CreateSupportTicketPayload): Observable<SupportTicket> {
    return this.http.post<HelpSupportApiResponse>(`${this.base}/support-ticket/create`, payload).pipe(
      map((resp) => {
        if (!apiOk(resp) || !resp.supportTicketDto) {
          throw new Error(resp.message ?? 'Could not create support ticket.');
        }
        return resp.supportTicketDto;
      }),
      catchError((err) => throwError(() => mapHttpError(err, 'Could not create support ticket.'))),
    );
  }

  fetchMyTickets(): Observable<SupportTicket[]> {
    return this.http.get<HelpSupportApiResponse>(`${this.base}/support-ticket/my-tickets`).pipe(
      map((resp) => {
        if (!apiOk(resp)) {
          throw new Error(resp.message ?? 'Could not load your tickets.');
        }
        return resp.supportTicketDtoList ?? [];
      }),
      catchError((err) => throwError(() => mapHttpError(err, 'Could not load your tickets.'))),
    );
  }

  fetchMyTicketById(id: number): Observable<SupportTicket> {
    return this.http.get<HelpSupportApiResponse>(`${this.base}/support-ticket/find-by-id/${id}`).pipe(
      map((resp) => {
        if (!apiOk(resp) || !resp.supportTicketDto) {
          throw new Error(resp.message ?? 'Could not load ticket.');
        }
        return resp.supportTicketDto;
      }),
      catchError((err) => throwError(() => mapHttpError(err, 'Could not load ticket.'))),
    );
  }

  addTicketMessage(supportTicketId: number, body: string): Observable<SupportTicket> {
    return this.http
      .post<HelpSupportApiResponse>(`${this.base}/support-ticket/add-message`, { supportTicketId, body })
      .pipe(
        map((resp) => {
          if (!apiOk(resp) || !resp.supportTicketDto) {
            throw new Error(resp.message ?? 'Could not send message.');
          }
          return resp.supportTicketDto;
        }),
        catchError((err) => throwError(() => mapHttpError(err, 'Could not send message.'))),
      );
  }

  fetchPlatformStatus(): Observable<PlatformStatusSummary> {
    return this.http.get<HelpSupportApiResponse>(`${this.base}/platform-status`).pipe(
      map((resp) => {
        if (!apiOk(resp) || !resp.platformStatusDto) {
          throw new Error(resp.message ?? 'Could not load platform status.');
        }
        return resp.platformStatusDto;
      }),
      catchError((err) => throwError(() => mapHttpError(err, 'Could not load platform status.'))),
    );
  }
}
