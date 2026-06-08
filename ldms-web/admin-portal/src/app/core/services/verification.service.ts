import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { ldmsServiceUrl } from '../utils/api-url.util';
import { SMS_DELIVERY_DISABLED_CODE, VerificationFlowError } from './verification-flow.error';

export interface VerificationUserFlags {
  emailVerified: boolean;
  phoneVerified: boolean;
  phoneVerificationDue: boolean;
  phoneNumber: string;
}

@Injectable({ providedIn: 'root' })
export class VerificationService {
  private readonly userBase = ldmsServiceUrl('user-management', 'user', undefined, 'backoffice');

  constructor(private readonly http: HttpClient) {}

  fetchVerificationFlags(): Observable<VerificationUserFlags | null> {
    return this.http.get<unknown>(`${this.userBase}/me`).pipe(
      map((resp) => this.extractFlags(resp)),
      catchError(() => throwError(() => new VerificationFlowError(
        'Could not load your profile for phone verification.',
        false,
      ))),
    );
  }

  requestPhoneVerification(userId?: number): Observable<void> {
    const url = userId && userId > 0
      ? `${this.userBase}/${userId}/request-phone-verification`
      : `${this.userBase}/request-phone-verification`;
    return this.http.post<unknown>(url, {}).pipe(
      map((resp) => {
        this.ensureSuccess(resp);
      }),
      catchError((err: unknown) => throwError(() => this.mapRequestError(err))),
    );
  }

  confirmPhoneVerification(otp: string, userId?: number): Observable<void> {
    const url = userId && userId > 0
      ? `${this.userBase}/${userId}/confirm-phone-verification`
      : `${this.userBase}/confirm-phone-verification`;
    return this.http
      .post<unknown>(url, { otp: otp.trim() })
      .pipe(
        map((resp) => {
          this.ensureSuccess(resp);
        }),
        catchError((err: unknown) => throwError(() => this.mapRequestError(err, false))),
      );
  }

  requestStepUpVerification(): Observable<void> {
    return this.http.post<unknown>(`${this.userBase}/request-step-up-verification`, {}).pipe(
      map((resp) => {
        this.ensureSuccess(resp);
      }),
      catchError((err: unknown) => throwError(() => this.mapRequestError(err))),
    );
  }

  confirmStepUpVerification(otp: string): Observable<boolean> {
    return this.http
      .post<unknown>(`${this.userBase}/confirm-step-up-verification`, { otp: otp.trim() })
      .pipe(
        map((resp) => {
          this.ensureSuccess(resp);
          return true;
        }),
        catchError((err: unknown) => throwError(() => this.mapRequestError(err, false))),
      );
  }

  private ensureSuccess(response: unknown): void {
    if (!this.isFailure(response)) {
      return;
    }
    throw this.failureError(response);
  }

  private mapRequestError(err: unknown, dismissOnUnavailable = true): VerificationFlowError | Error {
    if (err instanceof VerificationFlowError) {
      return err;
    }
    if (err instanceof HttpErrorResponse) {
      if (err.status === 404) {
        return new VerificationFlowError(
          dismissOnUnavailable
            ? 'Phone verification is not available on this server yet. Restart ldms-user-management and try again later.'
            : 'Phone verification is not available on this server yet.',
          dismissOnUnavailable,
        );
      }
      if (err.status === 503 && this.isSmsDisabledBody(err.error)) {
        return new VerificationFlowError(
          this.messageFrom(err.error) ??
            'SMS verification is currently disabled. You can continue without verifying your phone number for now.',
          true,
        );
      }
      if (err.status === 0) {
        return new VerificationFlowError(
          'Cannot reach the API gateway. Start ldms-api-gateway on port 8091.',
          dismissOnUnavailable,
        );
      }
      const bodyFailure = this.failureError(err.error);
      if (bodyFailure instanceof VerificationFlowError) {
        return bodyFailure;
      }
      const msg =
        this.messageFrom(err.error) ??
        (typeof err.error === 'string' ? err.error : null) ??
        err.message ??
        'Could not send verification code.';
      return new VerificationFlowError(msg, dismissOnUnavailable && this.isSmsDisabledBody(err.error));
    }
    if (err instanceof Error) {
      return err;
    }
    return new VerificationFlowError('Could not send verification code.', dismissOnUnavailable);
  }

  private failureError(response: unknown): VerificationFlowError | Error {
    const message =
      this.messageFrom(response) ??
      (this.isSmsDisabledBody(response)
        ? 'SMS verification is currently disabled. You can continue without verifying your phone number for now.'
        : 'Could not complete phone verification.');
    return new VerificationFlowError(message, this.isSmsDisabledBody(response) || this.isUnavailableBody(response));
  }

  private isSmsDisabledBody(response: unknown): boolean {
    const root = this.toRecord(response);
    if (!root) {
      return false;
    }
    const errors = root['errorMessages'];
    if (Array.isArray(errors) && errors.map(String).includes(SMS_DELIVERY_DISABLED_CODE)) {
      return true;
    }
    const statusCode = Number(root['statusCode'] ?? 0);
    return statusCode === 503 && String(root['message'] ?? '').toLowerCase().includes('sms');
  }

  private isUnavailableBody(response: unknown): boolean {
    const root = this.toRecord(response);
    if (!root) {
      return false;
    }
    const statusCode = Number(root['statusCode'] ?? 0);
    return statusCode === 404;
  }

  private extractFlags(response: unknown): VerificationUserFlags | null {
    const user = this.extractUserDto(response);
    if (!user) {
      return null;
    }
    return {
      emailVerified: user['emailVerified'] === true,
      phoneVerified: user['phoneVerified'] === true,
      phoneVerificationDue: user['phoneVerificationDue'] === true,
      phoneNumber: String(user['phoneNumber'] ?? '').trim(),
    };
  }

  private extractUserDto(response: unknown): Record<string, unknown> | null {
    const root = this.toRecord(response);
    if (!root) {
      return null;
    }
    const data = this.toRecord(root['data']) ?? root;
    const dto = this.toRecord(data['userDto']) ?? this.toRecord(data['user']);
    return dto ?? data;
  }

  private isFailure(response: unknown): boolean {
    const root = this.toRecord(response);
    if (!root) {
      return false;
    }
    if (root['success'] === false || root['isSuccess'] === false) {
      return true;
    }
    const code = Number(root['statusCode'] ?? 0);
    return code >= 400;
  }

  private messageFrom(response: unknown): string | null {
    const root = this.toRecord(response);
    if (!root) {
      return null;
    }
    const errors = root['errorMessages'];
    if (Array.isArray(errors) && errors.length) {
      const human = errors.map(String).filter((e) => e !== SMS_DELIVERY_DISABLED_CODE);
      if (human.length) {
        return human.join(' ');
      }
    }
    const msg = root['message'];
    return typeof msg === 'string' && msg.trim() ? msg.trim() : null;
  }

  private toRecord(value: unknown): Record<string, unknown> | null {
    return value !== null && typeof value === 'object' && !Array.isArray(value)
      ? (value as Record<string, unknown>)
      : null;
  }
}
