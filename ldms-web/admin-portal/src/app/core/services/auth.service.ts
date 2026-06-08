import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, delay, of, throwError } from 'rxjs';
import { catchError, finalize, map, switchMap, tap, timeout } from 'rxjs/operators';
import { CurrentUserService } from './current-user.service';
import { environment } from '../../../environments/environment';
import {
  AuthTokenResponse,
  AuthLoginOutcome,
  extractAccessToken,
  isAuthSuccess,
} from '../models/auth-api.model';
import { ldmsApiUrl } from '../utils/api-url.util';
import {
  decodeJwtPayload,
  isStoredSessionToken,
  normalizeAccessToken,
  normalizeJwtRoles,
} from '../utils/jwt.util';
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
  private static readonly AUTH_HTTP_TIMEOUT_MS = 20_000;
  private static readonly SESSION_REFRESH_TIMEOUT_MS = 8_000;

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
        timeout(AuthService.AUTH_HTTP_TIMEOUT_MS),
        switchMap((res) => this.completeLogin(this.requireAccessToken(res))),
        catchError((err: HttpErrorResponse) =>
          throwError(() => new Error(this.messageFromHttp(err))),
        ),
      );
  }

  /** @param usernameOrEmail LDMS username or registered email address */
  login(usernameOrEmail: string, password: string): Observable<AuthLoginOutcome> {
    const loginId = usernameOrEmail.trim();
    if (environment.useMocks || environment.authUseMocks) {
      return this.loginMock(loginId.toLowerCase(), password).pipe(map(() => ({ kind: 'authenticated' as const })));
    }
    return this.http
      .post<AuthTokenResponse>(`${this.authBase}/request-access-token`, {
        username: loginId,
        password,
      })
      .pipe(
        timeout(AuthService.AUTH_HTTP_TIMEOUT_MS),
        switchMap((res) => {
          if (!isAuthSuccess(res)) {
            const msg =
              res.errorMessages?.filter(Boolean).join(' ') ||
              res.message ||
              'Authentication failed';
            return throwError(() => new Error(msg));
          }
          if (res.requiresTwoFactor === true) {
            const mfaChallengeToken = String(res.mfaChallengeToken ?? '').trim();
            if (!mfaChallengeToken) {
              return throwError(
                () => new Error('Two-factor authentication is required but no challenge token was returned.'),
              );
            }
            return of({
              kind: 'two_factor_required' as const,
              mfaChallengeToken,
              message: res.message,
              twoFactorMethod: String(res.twoFactorMethod ?? 'SMS').trim(),
            });
          }
          return this.completeLogin(this.requireAccessToken(res)).pipe(
            map(() => ({ kind: 'authenticated' as const })),
          );
        }),
        catchError((err: HttpErrorResponse) =>
          throwError(() => new Error(this.messageFromHttp(err))),
        ),
      );
  }

  verifyTwoFactor(mfaChallengeToken: string, otp: string): Observable<void> {
    return this.http
      .post<AuthTokenResponse>(`${this.authBase}/verify-two-factor`, {
        mfaChallengeToken: mfaChallengeToken.trim(),
        otp: otp.trim(),
      })
      .pipe(
        timeout(AuthService.AUTH_HTTP_TIMEOUT_MS),
        switchMap((res) => this.completeLogin(this.requireAccessToken(res)).pipe(map(() => void 0))),
        catchError((err: HttpErrorResponse) =>
          throwError(() => new Error(this.messageFromHttp(err))),
        ),
      );
  }

  /** Restores JWT session and loads the user profile when the shell starts. */
  initializeSession(): Observable<void> {
    const token = this.storage.getToken();
    if (!isStoredSessionToken(token)) {
      if (token) {
        this.storage.clearSession();
      }
      return of(undefined);
    }
    this.persistJwtFallback(token!);
    return this.currentUser.refreshFromApi().pipe(
      timeout(AuthService.SESSION_REFRESH_TIMEOUT_MS),
      catchError(() => of(null)),
      map(() => undefined),
    );
  }

  bootstrapFromStorage(): void {
    this.currentUser.syncFromStorage();
    void this.initializeSession()
      .pipe(
        timeout(AuthService.SESSION_REFRESH_TIMEOUT_MS),
        catchError(() => of(undefined)),
        finalize(() => undefined),
      )
      .subscribe();
  }

  logout(): void {
    this.storage.clearSession();
    this.currentUser.clear();
  }

  private completeLogin(token: string): Observable<void> {
    const normalized = normalizeAccessToken(token) ?? token.trim();
    this.storage.setToken(normalized);
    this.persistJwtFallback(normalized);
    this.currentUser.syncFromStorage();
    this.scheduleSessionRefresh();
    return of(undefined);
  }

  private scheduleSessionRefresh(): void {
    setTimeout(() => {
      void this.currentUser
        .refreshFromApi()
        .pipe(
          timeout(AuthService.SESSION_REFRESH_TIMEOUT_MS),
          catchError(() => of(null)),
        )
        .subscribe();
    }, 0);
  }

  private persistJwtFallback(token: string): void {
    const payload = decodeJwtPayload(token);
    const username = String(payload?.sub ?? '').trim();
    const email = String(payload?.email ?? username).trim();
    const jwtFirst = String(payload?.firstName ?? '').trim();
    const jwtLast = String(payload?.lastName ?? '').trim();
    const firstName = jwtFirst;
    const name = [jwtFirst, jwtLast].filter(Boolean).join(' ').trim() || jwtFirst || username || email;
    const jwtUserId = payload?.userId != null ? Number(payload.userId) : undefined;
    const user: StoredUser = {
      id: jwtUserId != null && Number.isFinite(jwtUserId) && jwtUserId > 0 ? jwtUserId : undefined,
      username,
      name,
      firstName,
      lastName: jwtLast,
      email: email || username,
      roleLabel: 'User',
      roles: normalizeJwtRoles(payload?.roles),
      organizationKycApprover: payload?.organizationKycApprover === true,
      operationalIssueHandler: payload?.operationalIssueHandler === true,
    };
    this.storage.setUser(user);
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
      delay(80),
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
      return (
        'Cannot reach the API gateway. Start ldms-api-gateway on port 8091 (and ldms-authentication on 8083), ' +
        'then sign in again from http://localhost:4200 using npm start in ldms-web/admin-portal.'
      );
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
