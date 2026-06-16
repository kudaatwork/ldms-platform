import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
} from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, switchMap, throwError } from 'rxjs';
import { AuthStateService } from '../services/auth-state.service';
import { SessionExpiryService } from '../services/session-expiry.service';
import { StorageService } from '../services/storage.service';
import { TokenRefreshService } from '../services/token-refresh.service';
import { isLdmsApiRequest } from '../utils/api-url.util';
import { isPublicLdmsApiRequest } from '../utils/public-api.util';
import { isJwtExpired, isJwtExpiringSoon } from '../utils/jwt.util';

@Injectable()
export class SessionExpiredInterceptor implements HttpInterceptor {
  constructor(
    private readonly storage: StorageService,
    private readonly authState: AuthStateService,
    private readonly sessionExpiry: SessionExpiryService,
    private readonly tokenRefresh: TokenRefreshService,
  ) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(req).pipe(
      catchError((err: unknown) => {
        if (!(err instanceof HttpErrorResponse) || err.status !== 401) {
          return throwError(() => err);
        }
        if (!this.shouldAttemptRecovery(req.url)) {
          return throwError(() => err);
        }
        return this.tokenRefresh.refreshAccessToken().pipe(
          switchMap((token) =>
            next.handle(
              req.clone({
                setHeaders: { Authorization: `Bearer ${token}` },
              }),
            ),
          ),
          catchError((refreshErr) => {
            this.maybeForceLogout(req.url);
            return throwError(() => refreshErr);
          }),
        );
      }),
    );
  }

  private shouldAttemptRecovery(url: string): boolean {
    if (!isLdmsApiRequest(url) || isPublicLdmsApiRequest(url)) {
      return false;
    }
    const token = this.storage.getToken();
    const signedIn = !!(token && !token.startsWith('mock.')) || !!this.authState.currentUser;
    if (!signedIn || !this.storage.getRefreshToken()) {
      return false;
    }
    if (!token) {
      return true;
    }
    return isJwtExpired(token) || isJwtExpiringSoon(token);
  }

  private maybeForceLogout(url: string): void {
    if (!isLdmsApiRequest(url) || isPublicLdmsApiRequest(url)) {
      return;
    }
    const token = this.storage.getToken();
    const signedIn = !!(token && !token.startsWith('mock.')) || !!this.authState.currentUser;
    if (!signedIn) {
      return;
    }
    const tokenExpired = !token || isJwtExpired(token);
    if (!tokenExpired && this.storage.getRefreshToken()) {
      // Access token still valid — 401 is an authorization issue, not session expiry.
      return;
    }
    this.sessionExpiry.scheduleHandleSessionExpired('unauthorized');
  }
}
