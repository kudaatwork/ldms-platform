import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface UserManagementResponse {
  statusCode?: number;
  isSuccess?: boolean;
  message?: string;
  errorMessages?: string[];
}

export interface ResetPasswordPayload {
  token: string;
  email: string;
  newPassword: string;
  confirmPassword: string;
}

@Injectable({ providedIn: 'root' })
export class PasswordRecoveryService {
  constructor(private readonly http: HttpClient) {}

  private base(): string {
    return environment.apiUrl;
  }

  forgotPassword(usernameOrEmail: string): Observable<UserManagementResponse> {
    if (environment.useMocks || environment.authUseMocks) {
      return throwError(() => new Error('Password recovery is not available in demo mode.'));
    }
    const url = `${this.base()}/ldms-user-management/v1/system/user/forgot-password`;
    return this.http
      .post<UserManagementResponse>(url, { usernameOrEmail: usernameOrEmail.trim() })
      .pipe(catchError((e) => throwError(() => this.toErr(e))));
  }

  validateResetToken(token: string, email: string): Observable<UserManagementResponse> {
    if (environment.useMocks || environment.authUseMocks) {
      return throwError(() => new Error('Password recovery is not available in demo mode.'));
    }
    const params = new HttpParams().set('token', token).set('email', email);
    const url = `${this.base()}/ldms-user-management/v1/system/user/validate-reset-token`;
    return this.http.get<UserManagementResponse>(url, { params }).pipe(catchError((e) => throwError(() => this.toErr(e))));
  }

  resetPassword(body: ResetPasswordPayload): Observable<UserManagementResponse> {
    if (environment.useMocks || environment.authUseMocks) {
      return throwError(() => new Error('Password recovery is not available in demo mode.'));
    }
    const url = `${this.base()}/ldms-user-management/v1/system/user-password/reset-password`;
    return this.http.post<UserManagementResponse>(url, body).pipe(catchError((e) => throwError(() => this.toErr(e))));
  }

  private toErr(err: HttpErrorResponse): Error {
    const body = err.error as { message?: string; errorMessages?: string[] } | undefined;
    const msg = body?.message ?? body?.errorMessages?.[0] ?? err.message ?? 'Request failed';
    return new Error(msg);
  }
}
