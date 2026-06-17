import { HttpClient, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, of, throwError, timeout, TimeoutError } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import { LxExportFormat } from '../../../shared/utils/lx-export.util';

const API_TIMEOUT_MS = 15_000;

export type SupportTicketStatus =
  | 'OPEN'
  | 'IN_PROGRESS'
  | 'WAITING_ON_CUSTOMER'
  | 'RESOLVED'
  | 'CLOSED';

export type DemoRequisitionStatus = 'NEW' | 'CONTACTED' | 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';

export type SupportTicketMessageVisibility = 'PUBLIC' | 'INTERNAL';
export type SupportTicketMessageAuthorRole = 'REQUESTER' | 'HANDLER' | 'SYSTEM';

export interface AdminSupportTicketMessage {
  id: number;
  supportTicketId: number;
  authorUsername: string;
  authorRole: SupportTicketMessageAuthorRole;
  visibility: SupportTicketMessageVisibility;
  body: string;
  createdAt: string;
}

export interface AdminSupportTicket {
  id: number;
  ticketNumber: string;
  subject: string;
  description: string;
  category: string;
  priority: string;
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
  messages?: AdminSupportTicketMessage[];
}

export interface SupportTicketExportFilters {
  status?: SupportTicketStatus;
  search?: string;
}

export interface AdminDemoRequisition {
  id: number;
  requisitionNumber: string;
  fullName: string;
  email: string;
  phone: string;
  address: string;
  demoRequest: string;
  status: DemoRequisitionStatus;
  assignedHandlerUsername?: string;
  adminNotes?: string;
  contactedAt?: string;
  scheduledAt?: string;
  completedAt?: string;
  createdAt: string;
  modifiedAt?: string;
}

export interface DemoRequisitionUpdatePayload {
  adminNotes?: string;
  scheduledAt?: string;
}

interface HelpSupportApiResponse {
  isSuccess?: boolean;
  success?: boolean;
  statusCode?: number;
  message?: string;
  supportTicketDto?: AdminSupportTicket;
  supportTicketDtoList?: AdminSupportTicket[];
  demoRequisitionDto?: AdminDemoRequisition;
  demoRequisitionDtoList?: AdminDemoRequisition[];
}

function mapHelpSupportHttpError(err: unknown, fallback: string): Error {
  if (err instanceof HttpErrorResponse) {
    const body = err.error;
    const serverMessage =
      typeof body === 'object' && body != null && 'message' in body && typeof body.message === 'string'
        ? body.message
        : '';
    if (err.status === 404) {
      return new Error(
        serverMessage ||
          'Help & Support API returned HTTP 404. Restart ldms-user-management (8086) after pulling the latest build so Flyway migration V15 and the ticket detail endpoints are applied, then retry.',
      );
    }
    if (err.status === 403) {
      return new Error('Not authorized to manage support tickets.');
    }
    if (err.status === 409) {
      return new Error(serverMessage || 'That action is not allowed for this ticket.');
    }
    if (err.status === 0) {
      return new Error('Cannot reach the API gateway. Confirm ldms-api-gateway (8091) and ldms-user-management (8086) are running.');
    }
    if (serverMessage) {
      return new Error(serverMessage);
    }
    return new Error(`${fallback} (HTTP ${err.status}).`);
  }
  if (err instanceof Error && err.message) {
    return err;
  }
  return new Error(fallback);
}

function apiOk(resp: HelpSupportApiResponse): boolean {
  return (
    resp.isSuccess === true ||
    resp.success === true ||
    (resp.statusCode != null && resp.statusCode >= 200 && resp.statusCode < 300)
  );
}

@Injectable({ providedIn: 'root' })
export class HelpSupportAdminService {
  private readonly base = ldmsServiceUrl('user-management', 'help-support', undefined, 'backoffice');

  constructor(private readonly http: HttpClient) {}

  fetchAllTickets(): Observable<AdminSupportTicket[]> {
    if (environment.useMocks) {
      return of([]);
    }
    return this.http.get<HelpSupportApiResponse>(`${this.base}/support-ticket/list`).pipe(
      timeout(API_TIMEOUT_MS),
      map((resp) => this.unwrapTicketList(resp)),
      catchError((err) => this.handleError(err, 'Could not load support tickets.')),
    );
  }

  fetchTicketById(id: number): Observable<AdminSupportTicket> {
    return this.http.get<HelpSupportApiResponse>(`${this.base}/support-ticket/find-by-id/${id}`).pipe(
      timeout(API_TIMEOUT_MS),
      map((resp) => this.unwrapTicket(resp)),
      catchError((err) => this.handleError(err, 'Could not load support ticket.')),
    );
  }

  updateStatus(supportTicketId: number, status: SupportTicketStatus): Observable<AdminSupportTicket> {
    return this.http
      .put<HelpSupportApiResponse>(`${this.base}/support-ticket/update-status`, { supportTicketId, status })
      .pipe(
        timeout(API_TIMEOUT_MS),
        map((resp) => this.unwrapTicket(resp)),
        catchError((err) => this.handleError(err, 'Could not update ticket status.')),
      );
  }

  assignToMe(supportTicketId: number): Observable<AdminSupportTicket> {
    return this.http
      .put<HelpSupportApiResponse>(`${this.base}/support-ticket/assign`, { supportTicketId })
      .pipe(
        timeout(API_TIMEOUT_MS),
        map((resp) => this.unwrapTicket(resp)),
        catchError((err) => this.handleError(err, 'Could not assign ticket.')),
      );
  }

  addMessage(
    supportTicketId: number,
    body: string,
    visibility: SupportTicketMessageVisibility = 'PUBLIC',
  ): Observable<AdminSupportTicket> {
    return this.http
      .post<HelpSupportApiResponse>(`${this.base}/support-ticket/add-message`, {
        supportTicketId,
        body,
        visibility,
      })
      .pipe(
        timeout(API_TIMEOUT_MS),
        map((resp) => this.unwrapTicket(resp)),
        catchError((err) => this.handleError(err, 'Could not send message.')),
      );
  }

  exportTickets(format: LxExportFormat, filters: SupportTicketExportFilters = {}): Observable<HttpResponse<Blob>> {
    const apiFormat = format === 'xlsx' ? 'xlsx' : format;
    return this.http
      .post(`${this.base}/support-ticket/export?format=${encodeURIComponent(apiFormat)}`, filters, {
        observe: 'response',
        responseType: 'blob',
      })
      .pipe(
        timeout(60_000),
        catchError((err) => this.handleError(err, 'Could not export support tickets.')),
      );
  }

  fetchAllDemoRequisitions(): Observable<AdminDemoRequisition[]> {
    if (environment.useMocks) {
      return of([]);
    }
    return this.http.get<HelpSupportApiResponse>(`${this.base}/demo-requisition/list`).pipe(
      timeout(API_TIMEOUT_MS),
      map((resp) => this.unwrapDemoRequisitionList(resp)),
      catchError((err) => this.handleError(err, 'Could not load demo requisitions.')),
    );
  }

  fetchDemoRequisitionById(id: number): Observable<AdminDemoRequisition> {
    return this.http.get<HelpSupportApiResponse>(`${this.base}/demo-requisition/find-by-id/${id}`).pipe(
      timeout(API_TIMEOUT_MS),
      map((resp) => this.unwrapDemoRequisition(resp)),
      catchError((err) => this.handleError(err, 'Could not load demo requisition.')),
    );
  }

  updateDemoRequisitionStatus(
    demoRequisitionId: number,
    status: DemoRequisitionStatus,
    payload: DemoRequisitionUpdatePayload = {},
  ): Observable<AdminDemoRequisition> {
    return this.http
      .put<HelpSupportApiResponse>(`${this.base}/demo-requisition/update-status`, {
        demoRequisitionId,
        status,
        adminNotes: payload.adminNotes,
        scheduledAt: payload.scheduledAt,
      })
      .pipe(
        timeout(API_TIMEOUT_MS),
        map((resp) => this.unwrapDemoRequisition(resp)),
        catchError((err) => this.handleError(err, 'Could not update demo requisition.')),
      );
  }

  private unwrapTicketList(resp: HelpSupportApiResponse): AdminSupportTicket[] {
    if (!apiOk(resp)) {
      throw new Error(resp.message ?? 'Could not load support tickets.');
    }
    return Array.isArray(resp.supportTicketDtoList) ? resp.supportTicketDtoList : [];
  }

  private unwrapTicket(resp: HelpSupportApiResponse): AdminSupportTicket {
    if (!apiOk(resp) || !resp.supportTicketDto) {
      throw new Error(resp.message ?? 'Support ticket response was empty.');
    }
    return resp.supportTicketDto;
  }

  private unwrapDemoRequisitionList(resp: HelpSupportApiResponse): AdminDemoRequisition[] {
    if (!apiOk(resp)) {
      throw new Error(resp.message ?? 'Could not load demo requisitions.');
    }
    return Array.isArray(resp.demoRequisitionDtoList) ? resp.demoRequisitionDtoList : [];
  }

  private unwrapDemoRequisition(resp: HelpSupportApiResponse): AdminDemoRequisition {
    if (!apiOk(resp) || !resp.demoRequisitionDto) {
      throw new Error(resp.message ?? 'Demo requisition response was empty.');
    }
    return resp.demoRequisitionDto;
  }

  private handleError(err: unknown, fallback: string): Observable<never> {
    if (err instanceof TimeoutError) {
      return throwError(
        () =>
          new Error(
            'Help & Support request timed out. Confirm ldms-api-gateway (8091) and ldms-user-management (8086) are running.',
          ),
      );
    }
    return throwError(() => mapHelpSupportHttpError(err, fallback));
  }
}
