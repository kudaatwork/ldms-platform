import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, delay, of, throwError } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { CurrentUserService } from './current-user.service';
import { environment } from '../../../environments/environment';
import {
  AuthTokenResponse,
  extractAccessToken,
  isAuthSuccess,
} from '../models/auth-api.model';
import { ldmsApiUrl } from '../utils/api-url.util';
import { decodeJwtPayload, normalizeJwtRoles } from '../utils/jwt.util';
import { StorageService, StoredUser } from './storage.service';

export interface MockCredential {
  label: string;
  email: string;
  pass: string;
}

/** Demo users — only when {@code authUseMocks} is true. */
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
  /** {@code /ldms-authentication/v1/auth} (dev proxy → gateway :8091). */
  private readonly authBase = ldmsApiUrl('/ldms-authentication/v1/auth');

  constructor(
    private readonly http: HttpClient,
    private readonly storage: StorageService,
    private readonly currentUser: CurrentUserService,
  ) {}

  loginWithGoogleIdToken(idToken: string): Observable<void> {
    if (environment.useMocks || environment.authUseMocks) {
      return throwError(() => new Error('Google sign-in is not available in demo mode.'));
    }
    return this.http
      .post<AuthTokenResponse>(`${this.authBase}/google-id-token`, { idToken })
      .pipe(
        switchMap((res) => this.completeLogin(this.requireAccessToken(res))),
        catchError((err: HttpErrorResponse) =>
          throwError(() => new Error(this.messageFromHttp(err))),
        ),
      );
  }

  /** @param usernameOrEmail LDMS username or registered email address */
  login(usernameOrEmail: string, password: string): Observable<void> {
    const loginId = usernameOrEmail.trim();
    if (environment.useMocks || environment.authUseMocks) {
      return this.loginMock(loginId.toLowerCase(), password);
    }
    return this.http
      .post<AuthTokenResponse>(`${this.authBase}/request-access-token`, {
        username: loginId,
        password,
      })
      .pipe(
        switchMap((res) => this.completeLogin(this.requireAccessToken(res))),
        catchError((err: HttpErrorResponse) =>
          throwError(() => new Error(this.messageFromHttp(err))),
        ),
      );
  }

  /** Restores JWT session and loads the user profile when the shell starts. */
  initializeSession(): Observable<void> {
    const token = this.storage.getToken();
    if (!token || token.startsWith('mock-token-')) {
      return of(undefined);
    }
    this.persistJwtFallback(token);
    return this.currentUser.refreshFromApi().pipe(map(() => undefined));
  }

  bootstrapFromStorage(): void {
    void this.initializeSession().subscribe();
  }

  logout(): void {
    this.storage.clearSession();
    this.currentUser.clear();
  }

  private completeLogin(token: string): Observable<void> {
    this.storage.setToken(token);
    this.persistJwtFallback(token);
    return this.currentUser.refreshFromApi().pipe(map(() => undefined));
  }

  private persistJwtFallback(token: string): void {
    const payload = decodeJwtPayload(token);
    const username = String(payload?.sub ?? '').trim();
    const email = String(payload?.email ?? username).trim();
    const jwtFirst = String(payload?.firstName ?? '').trim();
    const jwtLast = String(payload?.lastName ?? '').trim();
    const firstName = jwtFirst;
    const name = [jwtFirst, jwtLast].filter(Boolean).join(' ').trim() || jwtFirst || username || email;
    const user: StoredUser = {
      username,
      name,
      firstName,
      lastName: jwtLast,
      email: email || username,
      roleLabel: 'User',
      roles: normalizeJwtRoles(payload?.roles),
      organizationKycApprover: payload?.organizationKycApprover === true,
    };
    this.storage.setUser(user);
    this.currentUser.syncFromStorage();
  }

  private requireAccessToken(res: AuthTokenResponse): string {
    if (!isAuthSuccess(res)) {
      const msg =
        res.errorMessages?.filter(Boolean).join(' ') ||
        res.message ||
        'Authentication failed';
      throw new Error(msg);
    }
    const token = extractAccessToken(res);
    if (!token) {
      throw new Error('No access token in response');
    }
    return token;
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
        const user: StoredUser = {
          name: row.label,
          firstName: row.label.split(/\s+/)[0] ?? row.label,
          lastName: row.label.split(/\s+/).slice(1).join(' '),
          email: row.email,
          roleLabel: row.label,
          roles: rolesForEmail(row.email),
        };
        this.storage.setUser(user);
        this.currentUser.syncFromStorage();
      }),
    );
  }

  private messageFromHttp(err: HttpErrorResponse): string {
    const body = err.error as AuthTokenResponse | undefined;
    if (body?.errorMessages?.length) {
      return body.errorMessages.join(' ');
    }
    if (body?.message) {
      return body.message;
    }
    if (err.status === 0) {
      return 'Cannot reach the API gateway. Start ldms-api-gateway on port 8091.';
    }
    if (err.status === 503) {
      return (
        body?.message ??
        'Authentication service is not running. Start ldms-authentication on port 8083 (and user-management on 8086), then retry.'
      );
    }
    if (err.status === 502 || err.status === 504) {
      return 'Gateway could not reach the authentication service. Ensure ldms-authentication is running on port 8083.';
    }
    return err.message ?? 'Login failed';
  }
}
