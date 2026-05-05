import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, map, Observable, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CurrentUser } from '../models/auth.model';
import { AuthStateService } from './auth-state.service';
import { StorageService } from './storage.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(
    private readonly http: HttpClient,
    private readonly storage: StorageService,
    private readonly authState: AuthStateService,
  ) {}

  loginWithGoogleIdToken(idToken: string): Observable<void> {
    if (environment.useMocks) {
      return throwError(() => new Error('Google sign-in is not available in demo mode.'));
    }
    return this.http
      .post<{ accessToken?: string; token?: string }>(
        `${environment.apiUrl}/ldms-authentication/v1/auth/google-id-token`,
        { idToken },
      )
      .pipe(
        map((res) => {
          const token = res.accessToken ?? res.token ?? '';
          if (!token) {
            throw new Error('No token in response');
          }
          return token;
        }),
        tap((token) => {
          this.storage.setToken(token);
          this.authState.setCurrentUser(this.decodeToken(token));
        }),
        map(() => void 0),
        catchError((err: HttpErrorResponse) =>
          throwError(() => new Error(this.messageFromHttp(err))),
        ),
      );
  }

  login(email: string, password: string): Observable<void> {
    return this.http
      .post<{ token: string; accessToken?: string }>(`${environment.apiUrl}/api/v1/frontend/auth/login`, {
        email,
        password,
      })
      .pipe(
        map((res) => {
          const token = res.accessToken ?? res.token;
          if (!token) {
            throw new Error('No token in response');
          }
          return token;
        }),
        tap((token) => this.storage.setToken(token)),
        tap((token) => this.authState.setCurrentUser(this.decodeToken(token))),
        map(() => void 0),
      );
  }

  private messageFromHttp(err: HttpErrorResponse): string {
    const body = err.error as { message?: string } | undefined;
    return body?.message ?? err.message ?? 'Google sign-in failed';
  }

  bootstrapFromStorage(): void {
    const token = this.storage.getToken();
    this.authState.setCurrentUser(token ? this.decodeToken(token) : null);
  }

  logout(): void {
    this.storage.clearSession();
    this.authState.setCurrentUser(null);
  }

  private decodeToken(token: string): CurrentUser {
    const chunks = token.split('.');
    if (chunks.length < 2) {
      throw new Error('Invalid JWT token');
    }
    const payload = JSON.parse(atob(chunks[1] ?? ''));
    return {
      orgClassification: payload.orgClassification,
      organizationId: String(payload.organizationId),
      orgName: payload.orgName,
      userId: String(payload.userId),
      roles: Array.isArray(payload.roles) ? payload.roles : [],
      email: payload.email,
    };
  }
}
