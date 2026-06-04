import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';

function mapHelpSupportHttpError(err: unknown, fallback: string): Error {
  if (err instanceof HttpErrorResponse) {
    const body = err.error;
    const serverMessage =
      typeof body === 'object' && body != null && 'message' in body && typeof body.message === 'string'
        ? body.message
        : '';
    if (err.status === 404) {
      return new Error(
        'Help & Support API returned HTTP 404. Restart ldms-user-management (8086) so the new endpoints and Flyway migration V6 are applied, then retry.',
      );
    }
    if (err.status === 403) {
      return new Error('Not authorized to view support tickets. Sign in with an ADMIN or READ_ONLY account.');
    }
    if (err.status === 401) {
      return new Error(
        'Help & Support API returned HTTP 401. Restart ldms-user-management (8086) after pulling the latest build — backoffice ticket listing must not require a Bearer token.',
      );
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

export interface AdminSupportTicket {
  id: number;
  ticketNumber: string;
  subject: string;
  description: string;
  category: string;
  priority: string;
  status: string;
  requesterUsername: string;
  requesterEmail: string;
  organizationId?: number;
  organizationName?: string;
  assignedHandlerUserId?: number;
  assignedHandlerUsername?: string;
  createdAt: string;
}

interface HelpSupportApiResponse {
  isSuccess?: boolean;
  success?: boolean;
  statusCode?: number;
  message?: string;
  supportTicketDtoList?: AdminSupportTicket[];
}

@Injectable({ providedIn: 'root' })
export class HelpSupportAdminService {
  private readonly base = ldmsServiceUrl('user-management', 'help-support', undefined, 'backoffice');

  constructor(private readonly http: HttpClient) {}

  fetchAllTickets(): Observable<AdminSupportTicket[]> {
    return this.http.get<HelpSupportApiResponse>(`${this.base}/support-ticket/list`).pipe(
      map((resp) => {
        const ok =
          resp.isSuccess === true ||
          resp.success === true ||
          (resp.statusCode != null && resp.statusCode >= 200 && resp.statusCode < 300);
        if (!ok) {
          throw new Error(resp.message ?? 'Could not load support tickets.');
        }
        return resp.supportTicketDtoList ?? [];
      }),
      catchError((err) => throwError(() => mapHelpSupportHttpError(err, 'Could not load support tickets.'))),
    );
  }
}
