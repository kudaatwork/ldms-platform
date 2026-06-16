import { HttpClient } from '@angular/common/http';
import { Injectable, Injector } from '@angular/core';
import { Observable, catchError, finalize, of, shareReplay, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuthTokenResponse,
  extractAccessToken,
  extractRefreshToken,
  isAuthSuccess,
} from '../models/auth-api.model';
import { ldmsApiUrl } from '../utils/api-url.util';
import { decodeJwtPayload, isJwtExpired, isJwtExpiringSoon } from '../utils/jwt.util';
import { StorageService } from './storage.service';
import { SessionExpiryService } from './session-expiry.service';

/**
 * Silently renews access tokens while the user is active.
 * Sign-out is driven by {@link SessionIdleService} (5 minutes idle), not JWT {@code exp}.
 */
@Injectable({ providedIn: 'root' })
export class TokenRefreshService {
  private readonly authBase = ldmsApiUrl('/ldms-authentication/v1/auth');
  private refreshInFlight$: Observable<string> | null = null;

  constructor(
    private readonly http: HttpClient,
    private readonly storage: StorageService,
    private readonly injector: Injector,
  ) {}

  /** Returns a valid access token, refreshing when expired or expiring soon. */
  ensureValidAccessToken(): Observable<string> {
    if (environment.useMocks) {
      const token = this.storage.getToken();
      return token ? of(token) : throwError(() => new Error('Not signed in'));
    }

    const current = this.storage.getToken()?.trim();
    if (current && !isJwtExpired(current) && !isJwtExpiringSoon(current)) {
      return of(current);
    }
    return this.refreshAccessToken().pipe(
      catchError(() => {
        const fallback = this.storage.getToken()?.trim();
        if (fallback && !isJwtExpired(fallback)) {
          return of(fallback);
        }
        return throwError(() => new Error('Session refresh failed'));
      }),
    );
  }

  /** POST /refresh-token — deduped while a refresh is already in flight. */
  refreshAccessToken(): Observable<string> {
    if (environment.useMocks) {
      return throwError(() => new Error('Token refresh is not available in demo mode.'));
    }

    if (this.refreshInFlight$) {
      return this.refreshInFlight$;
    }

    const refreshToken = this.storage.getRefreshToken()?.trim();
    const username = this.resolveSessionUsername();
    if (!refreshToken || !username) {
      return throwError(() => new Error('No refresh session available'));
    }

    this.refreshInFlight$ = this.http
      .post<AuthTokenResponse>(`${this.authBase}/refresh-token`, { username, refreshToken })
      .pipe(
        switchMap((res) => {
          if (!isAuthSuccess(res)) {
            const msg =
              res.errorMessages?.filter(Boolean).join(' ') ||
              res.message ||
              'Could not refresh session';
            return throwError(() => new Error(msg));
          }
          const accessToken = extractAccessToken(res);
          if (!accessToken) {
            return throwError(() => new Error('No access token in refresh response'));
          }
          this.storage.setToken(accessToken);
          const rotatedRefresh = extractRefreshToken(res);
          if (rotatedRefresh) {
            this.storage.setRefreshToken(rotatedRefresh);
          }
          this.persistSessionUsername(accessToken);
          this.injector.get(SessionExpiryService).watchToken(accessToken);
          return of(accessToken);
        }),
        finalize(() => {
          this.refreshInFlight$ = null;
        }),
        shareReplay(1),
      );

    return this.refreshInFlight$;
  }

  /** Refresh during user activity when the access token is near expiry. */
  refreshIfExpiringSoon(): void {
    const token = this.storage.getToken()?.trim();
    if (!token || !isJwtExpiringSoon(token) || !this.storage.getRefreshToken()) {
      return;
    }
    this.refreshAccessToken().subscribe({ error: () => undefined });
  }

  persistSessionCredentials(res: AuthTokenResponse, accessToken: string, loginId?: string): void {
    const refresh = extractRefreshToken(res);
    if (refresh) {
      this.storage.setRefreshToken(refresh);
    } else {
      this.storage.clearRefreshSession();
    }
    this.persistSessionUsername(accessToken, loginId);
  }

  private persistSessionUsername(accessToken: string, loginId?: string): void {
    const username = String(decodeJwtPayload(accessToken)?.sub ?? loginId ?? '').trim();
    if (username) {
      this.storage.setSessionUsername(username);
    }
  }

  private resolveSessionUsername(): string | null {
    const stored = this.storage.getSessionUsername()?.trim();
    if (stored) {
      return stored;
    }
    const token = this.storage.getToken();
    const fromJwt = token ? String(decodeJwtPayload(token)?.sub ?? '').trim() : '';
    return fromJwt || null;
  }
}
