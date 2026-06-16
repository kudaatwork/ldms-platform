import { Injectable, Injector } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { AuthStateService } from './auth-state.service';
import { AuthenticatedHistoryService } from './authenticated-history.service';
import { SessionIdleService } from './session-idle.service';
import { StorageService } from './storage.service';
import { TokenRefreshService } from './token-refresh.service';
import { isJwtExpired, jwtExpiresAtMs } from '../utils/jwt.util';

export type SessionExpiryReason = 'expired' | 'unauthorized' | 'inactivity';

/**
 * Signs the user out on idle timeout or when refresh fails.
 * JWT {@code exp} alone does not end the session while the user is active — see {@link TokenRefreshService}.
 */
@Injectable({ providedIn: 'root' })
export class SessionExpiryService {
  private expiryTimerId: ReturnType<typeof setTimeout> | null = null;
  private handling = false;

  constructor(
    private readonly injector: Injector,
    private readonly dialog: MatDialog,
    private readonly storage: StorageService,
    private readonly authState: AuthStateService,
    private readonly sessionIdle: SessionIdleService,
    private readonly authenticatedHistory: AuthenticatedHistoryService,
    private readonly router: Router,
  ) {}

  /** Schedule a silent refresh before access-token expiry (active sessions stay signed in). */
  watchToken(token: string | null | undefined): void {
    this.clearWatch();
    const normalized = token?.trim();
    if (!normalized || normalized.startsWith('mock.')) {
      return;
    }
    if (isJwtExpired(normalized)) {
      this.scheduleSilentRefresh(true);
      return;
    }
    const expiresAt = jwtExpiresAtMs(normalized);
    if (expiresAt == null) {
      return;
    }
    const refreshLeadMs = 60_000;
    const delayMs = expiresAt - Date.now() - refreshLeadMs;
    if (delayMs <= 0) {
      this.scheduleSilentRefresh(false);
      return;
    }
    this.expiryTimerId = setTimeout(() => this.scheduleSilentRefresh(false), delayMs);
  }

  /** Defer until the router is ready (APP_INITIALIZER / guard timing). */
  scheduleHandleSessionExpired(reason: SessionExpiryReason): void {
    queueMicrotask(() => this.handleSessionExpired(reason));
  }

  clearWatch(): void {
    if (this.expiryTimerId) {
      clearTimeout(this.expiryTimerId);
      this.expiryTimerId = null;
    }
  }

  handleSessionExpired(reason: SessionExpiryReason): void {
    this.dialog.closeAll();

    if (this.handling) {
      return;
    }
    const token = this.storage.getToken();
    const signedIn = !!(token && !token.startsWith('mock.')) || !!this.authState.currentUser;
    if (!signedIn) {
      return;
    }
    if (
      token &&
      !token.startsWith('mock.') &&
      !isJwtExpired(token) &&
      (reason === 'expired' || reason === 'unauthorized')
    ) {
      // A newer login or silent refresh renewed the session after this signal was queued.
      return;
    }
    const onLoginRoute = this.router.url.startsWith('/auth/login');

    this.handling = true;
    this.clearWatch();
    this.sessionIdle.deactivate();
    this.authenticatedHistory.disable();
    this.injector.get(AuthService).logout();

    if (onLoginRoute) {
      this.handling = false;
      return;
    }
    if (this.router.url.startsWith('/auth') && reason === 'unauthorized') {
      this.handling = false;
      return;
    }

    const queryParams =
      reason === 'inactivity'
        ? { reason: 'inactivity' }
        : { reason: 'session_expired' };

    void this.router
      .navigate(['/auth/login'], { replaceUrl: true, queryParams })
      .finally(() => {
        this.handling = false;
      });
  }

  private scheduleSilentRefresh(forceLogoutOnFailure: boolean): void {
    const tokenRefresh = this.injector.get(TokenRefreshService);
    if (!this.storage.getRefreshToken()) {
      if (forceLogoutOnFailure) {
        this.scheduleHandleSessionExpired('expired');
      }
      return;
    }
    tokenRefresh.refreshAccessToken().subscribe({
      next: (newToken) => this.watchToken(newToken),
      error: () => {
        if (forceLogoutOnFailure) {
          this.scheduleHandleSessionExpired('expired');
        }
      },
    });
  }
}
