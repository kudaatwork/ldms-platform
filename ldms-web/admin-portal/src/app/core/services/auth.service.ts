import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, delay, of, throwError } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { StorageService, StoredUser } from './storage.service';

export interface MockCredential {
  label: string;
  email: string;
  pass: string;
}

/** Demo users — must match login UI quick-fill buttons when `useMocks` is true. */
export const MOCK_DEMO_CREDENTIALS: readonly MockCredential[] = [
  { label: 'Platform Admin', email: 'admin@projectlx.co.zw', pass: 'Admin1234!' },
  { label: 'Stage 1 Reviewer', email: 'reviewer1@projectlx.co.zw', pass: 'Review1234!' },
  { label: 'Stage 2 Reviewer', email: 'reviewer2@projectlx.co.zw', pass: 'Review1234!' },
  { label: 'Read Only', email: 'readonly@projectlx.co.zw', pass: 'Read1234!' },
];

function rolesForEmail(email: string): string[] {
  const e = email.toLowerCase();
  if (e === 'admin@projectlx.co.zw') return ['ADMIN'];
  if (e.includes('reviewer1')) return ['KYC_STAGE1'];
  if (e.includes('reviewer2')) return ['KYC_STAGE2'];
  if (e.includes('readonly')) return ['READ_ONLY'];
  return ['USER'];
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(
    private readonly http: HttpClient,
    private readonly storage: StorageService,
  ) {}

  login(email: string, password: string): Observable<void> {
    const normalized = email.trim().toLowerCase();
    if (environment.useMocks) {
      return this.loginMock(normalized, password);
    }
    return this.http
      .post<{ accessToken?: string; token?: string }>(
        `${environment.apiUrl}/api/v1/frontend/auth/login`,
        { email: normalized, password },
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
          const user: StoredUser = {
            name: normalized.split('@')[0] ?? 'User',
            email: normalized,
            roles: [],
          };
          this.storage.setUser(user);
        }),
        map(() => undefined),
        catchError((err: HttpErrorResponse) =>
          throwError(() => new Error(this.messageFromHttp(err))),
        ),
      );
  }

  private loginMock(email: string, password: string): Observable<void> {
    const row = MOCK_DEMO_CREDENTIALS.find(
      (c) => c.email.toLowerCase() === email && c.pass === password,
    );
    if (!row) {
      return throwError(() => new Error('Invalid credentials'));
    }
    return of(undefined).pipe(
      delay(350),
      tap(() => {
        this.storage.setToken(`mock-token-${Date.now()}`);
        this.storage.setUser({
          name: row.label,
          email: row.email,
          roles: rolesForEmail(row.email),
        });
      }),
    );
  }

  private messageFromHttp(err: HttpErrorResponse): string {
    const body = err.error as { message?: string } | undefined;
    return body?.message ?? err.message ?? 'Login failed';
  }
}
