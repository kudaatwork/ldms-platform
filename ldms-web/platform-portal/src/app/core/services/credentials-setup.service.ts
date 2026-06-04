import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ldmsServiceUrl } from '../utils/api-url.util';

export interface CompleteCredentialsSetupPayload {
  newUsername: string;
  newPassword: string;
  confirmPassword: string;
}

interface UserEnvelope {
  success?: boolean;
  isSuccess?: boolean;
  statusCode?: number;
  message?: string;
  errorMessages?: string[];
}

interface UsernameAvailabilityEnvelope extends UserEnvelope {
  available?: boolean;
}

@Injectable({ providedIn: 'root' })
export class CredentialsSetupService {
  private readonly passwordBase = ldmsServiceUrl('user-management', 'user-password');
  private readonly userBase = ldmsServiceUrl('user-management', 'user');

  constructor(private readonly http: HttpClient) {}

  /** Live uniqueness check while the user types a new username (credential setup). */
  checkUsernameAvailability(username: string): Observable<boolean> {
    const trimmed = username.trim();
    if (!trimmed) {
      return of(false);
    }
    return this.http
      .get<UsernameAvailabilityEnvelope>(
        `${this.userBase}/username-availability/${encodeURIComponent(trimmed)}`,
      )
      .pipe(
        map((res) => res.available === true),
        catchError(() => of(false)),
      );
  }

  completeSetup(payload: CompleteCredentialsSetupPayload): Observable<void> {
    return this.http
      .put<UserEnvelope>(`${this.userBase}/complete-credentials-setup`, payload)
      .pipe(
        map((res) => {
          if (res.success === false || res.isSuccess === false) {
            throw new Error(this.messageFromEnvelope(res));
          }
          if (res.statusCode != null && (res.statusCode < 200 || res.statusCode >= 300)) {
            throw new Error(this.messageFromEnvelope(res));
          }
        }),
        catchError((err: HttpErrorResponse) =>
          throwError(() => new Error(this.messageFromHttp(err))),
        ),
      );
  }

  private messageFromEnvelope(body: UserEnvelope | undefined): string {
    if (body?.errorMessages?.length) {
      return body.errorMessages.join(' ');
    }
    return body?.message ?? 'Could not update credentials';
  }

  private messageFromHttp(err: HttpErrorResponse): string {
    const body = err.error as UserEnvelope | undefined;
    if (body?.errorMessages?.length) {
      return body.errorMessages.join(' ');
    }
    if (body?.message) {
      return body.message;
    }
    return err.message ?? 'Could not update credentials';
  }
}
