import { Injectable, Injector, OnDestroy } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { TokenRefreshService } from './token-refresh.service';

/**
 * Inactivity logout: 5 minutes idle, final 2 minutes show a countdown with "Stay logged in".
 * Mirrors LX Admin {@link AppComponent} session timers.
 */
@Injectable({ providedIn: 'root' })
export class SessionIdleService implements OnDestroy {
  /** Total idle time before forced sign-out. */
  readonly idleTimeoutMs = 5 * 60 * 1000;
  /** Warning window length (countdown) before logout. */
  readonly idleWarningMs = 2 * 60 * 1000;

  readonly warningVisible$ = new BehaviorSubject(false);
  readonly secondsRemaining$ = new BehaviorSubject(0);

  private readonly activityEvents: Array<keyof DocumentEventMap> = [
    'mousemove',
    'mousedown',
    'keydown',
    'touchstart',
    'scroll',
    'click',
  ];
  private static readonly ACTIVITY_RESET_COOLDOWN_MS = 1_000;

  private active = false;
  private idleWarningTimerId: ReturnType<typeof setTimeout> | null = null;
  private idleLogoutTimerId: ReturnType<typeof setTimeout> | null = null;
  private idleCountdownTimerId: ReturnType<typeof setInterval> | null = null;
  private warningDeadlineMs = 0;
  private lastActivityResetMs = 0;
  private onTimeout: (() => void) | null = null;
  private readonly onActivityBound = () => this.onUserActivity();

  constructor(private readonly injector: Injector) {}

  ngOnDestroy(): void {
    this.deactivate();
  }

  /** Start watching activity; {@link onTimeout} runs once when the idle budget expires. */
  activate(onTimeout: () => void): void {
    this.onTimeout = onTimeout;
    if (this.active) {
      this.armSessionTimers();
      return;
    }
    this.active = true;
    for (const evt of this.activityEvents) {
      document.addEventListener(evt, this.onActivityBound, { passive: true });
    }
    this.armSessionTimers();
  }

  deactivate(): void {
    this.active = false;
    this.onTimeout = null;
    for (const evt of this.activityEvents) {
      document.removeEventListener(evt, this.onActivityBound);
    }
    this.clearSessionTimers();
    this.warningVisible$.next(false);
    this.secondsRemaining$.next(0);
  }

  /** Extends the idle budget and dismisses the warning (e.g. "Stay logged in"). */
  staySignedIn(): void {
    if (!this.active || this.onTimeout == null) {
      return;
    }
    this.lastActivityResetMs = 0;
    this.armSessionTimers();
  }

  countdownLabel(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }

  private onUserActivity(): void {
    if (!this.active) {
      return;
    }
    const now = Date.now();
    if (now - this.lastActivityResetMs < SessionIdleService.ACTIVITY_RESET_COOLDOWN_MS) {
      return;
    }
    this.lastActivityResetMs = now;
    this.armSessionTimers();
    this.injector.get(TokenRefreshService).refreshIfExpiringSoon();
  }

  private armSessionTimers(): void {
    this.clearSessionTimers();
    this.warningVisible$.next(false);
    this.secondsRemaining$.next(0);
    const warningDelayMs = this.idleTimeoutMs - this.idleWarningMs;
    this.idleWarningTimerId = setTimeout(() => this.showSessionWarning(), warningDelayMs);
    this.idleLogoutTimerId = setTimeout(() => this.handleInactivityTimeout(), this.idleTimeoutMs);
  }

  private showSessionWarning(): void {
    this.warningVisible$.next(true);
    this.warningDeadlineMs = Date.now() + this.idleWarningMs;
    // Keep forced logout aligned with the visible countdown (browser timer throttling can desync).
    if (this.idleLogoutTimerId) {
      clearTimeout(this.idleLogoutTimerId);
    }
    this.idleLogoutTimerId = setTimeout(() => this.handleInactivityTimeout(), this.idleWarningMs);
    this.updateSessionCountdown();
    this.idleCountdownTimerId = setInterval(() => this.updateSessionCountdown(), 1000);
  }

  private updateSessionCountdown(): void {
    const remaining = Math.max(0, Math.ceil((this.warningDeadlineMs - Date.now()) / 1000));
    this.secondsRemaining$.next(remaining);
    if (remaining <= 0) {
      if (this.idleCountdownTimerId) {
        clearInterval(this.idleCountdownTimerId);
        this.idleCountdownTimerId = null;
      }
      // Fallback when background tabs throttle setTimeout but the interval still ticks.
      if (this.idleLogoutTimerId) {
        clearTimeout(this.idleLogoutTimerId);
        this.idleLogoutTimerId = null;
        this.handleInactivityTimeout();
      }
    }
  }

  private handleInactivityTimeout(): void {
    if (!this.active) {
      return;
    }
    this.warningVisible$.next(false);
    this.secondsRemaining$.next(0);
    const onTimeout = this.onTimeout;
    this.deactivate();
    onTimeout?.();
  }

  private clearSessionTimers(): void {
    if (this.idleWarningTimerId) {
      clearTimeout(this.idleWarningTimerId);
      this.idleWarningTimerId = null;
    }
    if (this.idleLogoutTimerId) {
      clearTimeout(this.idleLogoutTimerId);
      this.idleLogoutTimerId = null;
    }
    if (this.idleCountdownTimerId) {
      clearInterval(this.idleCountdownTimerId);
      this.idleCountdownTimerId = null;
    }
  }
}
