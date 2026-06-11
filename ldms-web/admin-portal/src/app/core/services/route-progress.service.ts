import { ApplicationRef, Injectable, NgZone, OnDestroy, signal } from '@angular/core';
import {
  NavigationCancel,
  NavigationEnd,
  NavigationError,
  NavigationStart,
  Router,
} from '@angular/router';
import { filter, first, Subscription } from 'rxjs';

/** Global top-of-screen route / bootstrap progress (NProgress-style trickle). */
@Injectable({ providedIn: 'root' })
export class RouteProgressService implements OnDestroy {
  readonly visible = signal(false);
  readonly progress = signal(0);
  readonly finishing = signal(false);

  private readonly activeKeys = new Set<number>();
  private bootReleased = false;
  private startedAt = 0;
  private trickleTimer: ReturnType<typeof setInterval> | null = null;
  private finishTimer: ReturnType<typeof setTimeout> | null = null;
  private hideTimer: ReturnType<typeof setTimeout> | null = null;
  private readonly routerSub: Subscription;

  private static readonly BOOT_KEY = -1;
  private static readonly MIN_VISIBLE_MS = 380;
  private static readonly HIDE_DELAY_MS = 520;

  constructor(
    private readonly router: Router,
    private readonly appRef: ApplicationRef,
    private readonly zone: NgZone,
  ) {
    this.engage(RouteProgressService.BOOT_KEY);

    this.routerSub = this.router.events.subscribe((event) => {
      if (event instanceof NavigationStart) {
        this.engage(event.id);
      } else if (
        event instanceof NavigationEnd ||
        event instanceof NavigationCancel ||
        event instanceof NavigationError
      ) {
        this.release(event.id);
      }
    });

    this.appRef.isStable.pipe(filter(Boolean), first()).subscribe(() => {
      this.zone.runOutsideAngular(() => {
        const releaseBoot = () => {
          this.zone.run(() => {
            if (!this.bootReleased) {
              this.bootReleased = true;
              this.release(RouteProgressService.BOOT_KEY);
            }
          });
        };
        if (document.readyState === 'complete') {
          setTimeout(releaseBoot, 220);
        } else {
          window.addEventListener('load', () => setTimeout(releaseBoot, 220), { once: true });
        }
      });
    });
  }

  ngOnDestroy(): void {
    this.routerSub.unsubscribe();
    this.clearTimers();
  }

  private engage(key: number): void {
    const wasIdle = this.activeKeys.size === 0;
    this.activeKeys.add(key);
    if (wasIdle) {
      this.startBar();
    }
  }

  private release(key: number): void {
    this.activeKeys.delete(key);
    if (this.activeKeys.size === 0) {
      this.scheduleFinish();
    }
  }

  private startBar(): void {
    this.clearTimers();
    this.finishing.set(false);
    this.visible.set(true);
    this.progress.set(0);
    this.startedAt = Date.now();

    requestAnimationFrame(() => {
      this.progress.set(10);
      this.startTrickle();
    });
  }

  private scheduleFinish(): void {
    const elapsed = Date.now() - this.startedAt;
    const wait = Math.max(0, RouteProgressService.MIN_VISIBLE_MS - elapsed);
    this.finishTimer = setTimeout(() => this.completeBar(), wait);
  }

  private completeBar(): void {
    this.stopTrickle();
    this.finishing.set(true);
    this.progress.set(100);

    this.hideTimer = setTimeout(() => {
      this.visible.set(false);
      this.finishing.set(false);
      this.progress.set(0);
    }, RouteProgressService.HIDE_DELAY_MS);
  }

  private startTrickle(): void {
    this.stopTrickle();
    this.zone.runOutsideAngular(() => {
      this.trickleTimer = setInterval(() => {
        this.zone.run(() => {
          const current = this.progress();
          if (current >= 94 || !this.visible()) {
            return;
          }
          const increment =
            current < 35 ? 4 + Math.random() * 8 : current < 70 ? 1.5 + Math.random() * 4 : 0.4 + Math.random() * 1.6;
          this.progress.set(Math.min(94, current + increment));
        });
      }, 280);
    });
  }

  private stopTrickle(): void {
    if (this.trickleTimer) {
      clearInterval(this.trickleTimer);
      this.trickleTimer = null;
    }
  }

  private clearTimers(): void {
    this.stopTrickle();
    if (this.finishTimer) {
      clearTimeout(this.finishTimer);
      this.finishTimer = null;
    }
    if (this.hideTimer) {
      clearTimeout(this.hideTimer);
      this.hideTimer = null;
    }
  }
}
