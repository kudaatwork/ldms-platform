import { Injectable, Injector } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { AuthStateService } from './auth-state.service';
import { AuthenticatedHistoryService } from './authenticated-history.service';
import { SessionIdleService } from './session-idle.service';
import { StorageService } from './storage.service';
import { isJwtExpired, jwtExpiresAtMs } from '../utils/jwt.util';

export type SessionExpiryReason = 'expired' | 'unauthorized' | 'inactivity';

/**
 * Forces a clean sign-out when the JWT expires or the API returns 401,
 * so users are redirected to login instead of silent request failures.
 */
@Injectable({ providedIn: 'root' })
export class SessionExpiryService {
  private expiryTimerId: ReturnType<typeof setTimeout> | null = null;
  private handling = false;

  constructor(
    private readonly injector: Injector,
    private readonly storage: StorageService,
    private readonly authState: AuthStateService,
    private readonly sessionIdle: SessionIdleService,
    private readonly authenticatedHistory: AuthenticatedHistoryService,
    private readonly router: Router,
  ) {}

  /** Schedule logout at JWT expiry and sign out immediately if already expired. */
  watchToken(token: string | null | undefined): void {
    this.clearWatch();
    const normalized = token?.trim();
    if (!normalized || normalized.startsWith('mock.')) {
      return;
    }
    if (isJwtExpired(normalized)) {
      this.scheduleHandleSessionExpired('expired');
      return;
    }
    const expiresAt = jwtExpiresAtMs(normalized);
    if (expiresAt == null) {
      return;
    }
    const delayMs = expiresAt - Date.now();
    this.expiryTimerId = setTimeout(() => this.scheduleHandleSessionExpired('expired'), Math.max(0, delayMs));
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
    if (this.handling) {
      return;
    }
    const token = this.storage.getToken();
    const signedIn = !!(token && !token.startsWith('mock.')) || !!this.authState.currentUser;
    if (!signedIn) {
      return;
    }
    const onLoginRoute = this.router.url.startsWith('/auth/login');

    this.handling = true;
    this.clearWatch();
    this.sessionIdle.deactivate();
    this.authenticatedHistory.disable();
    // Lazy resolve breaks AuthService ↔ SessionExpiryService circular DI.
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
}
