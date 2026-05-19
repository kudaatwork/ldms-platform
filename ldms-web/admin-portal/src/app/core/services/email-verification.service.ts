import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ldmsApiUrl } from '../utils/api-url.util';

export interface UserManagementResponse {
  statusCode?: number;
  isSuccess?: boolean;
  message?: string;
  errorMessages?: string[];
  /** {@code VERIFIED} or {@code ALREADY_VERIFIED} from verify-email. */
  emailVerificationOutcome?: string;
}

@Injectable({ providedIn: 'root' })
export class EmailVerificationService {
  constructor(private readonly http: HttpClient) {}

  verifyEmail(token: string, email: string): Observable<UserManagementResponse> {
    if (environment.useMocks) {
      return throwError(() => new Error('Email verification is not available in demo mode.'));
    }
    const params = new HttpParams().set('token', token).set('email', email);
    const url = ldmsApiUrl('/ldms-user-management/v1/system/user/verify-email');
    return this.http
      .post<UserManagementResponse>(url, null, { params })
      .pipe(catchError((e) => throwError(() => this.toErr(e))));
  }

  private toErr(err: HttpErrorResponse): Error {
    const body = err.error as { message?: string; errorMessages?: string[] } | undefined;
    const msg = body?.message ?? body?.errorMessages?.[0] ?? err.message ?? 'Verification failed';
    return new Error(msg);
  }
}
