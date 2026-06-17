import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';

export interface SubmitDemoRequisitionPayload {
  fullName: string;
  email: string;
  phone: string;
  address: string;
  demoRequest: string;
}

interface DemoRequisitionApiResponse {
  success?: boolean;
  isSuccess?: boolean;
  statusCode?: number;
  message?: string;
}

@Injectable({ providedIn: 'root' })
export class DemoRequisitionService {
  private readonly base = ldmsServiceUrl('user-management', 'help-support', undefined, 'frontend');

  constructor(private readonly http: HttpClient) {}

  submit(payload: SubmitDemoRequisitionPayload): Observable<string> {
    return this.http.post<DemoRequisitionApiResponse>(`${this.base}/demo-requisition/submit`, payload).pipe(
      map((resp) => {
        const ok =
          resp.success === true ||
          resp.isSuccess === true ||
          (resp.statusCode != null && resp.statusCode >= 200 && resp.statusCode < 300);
        if (!ok) {
          throw new Error(resp.message ?? 'Could not submit your demo request.');
        }
        return resp.message ?? 'Your demo request has been recorded.';
      }),
      catchError((err: unknown) => throwError(() => this.toError(err))),
    );
  }

  private toError(err: unknown): Error {
    if (err instanceof HttpErrorResponse) {
      const body = err.error;
      const serverMessage =
        typeof body === 'object' && body != null && 'message' in body && typeof body.message === 'string'
          ? body.message
          : '';
      if (err.status === 0) {
        return new Error('Cannot reach the server. Check your connection and try again.');
      }
      if (serverMessage) {
        return new Error(serverMessage);
      }
      return new Error(`Could not submit your demo request (HTTP ${err.status}).`);
    }
    if (err instanceof Error && err.message) {
      return err;
    }
    return new Error('Could not submit your demo request.');
  }
}
