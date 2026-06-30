import { Injectable, OnDestroy } from '@angular/core';
import { Subject, Subscription, interval, switchMap, catchError, of, startWith } from 'rxjs';
import { PlatformInboxService, PlatformInboxNotification } from './platform-inbox.service';
import { ShellNotificationService } from './shell-notification.service';
import { NotificationService } from './notification.service';

const POLL_MS = 30_000;
const SEEN_KEY = 'lx.platformInbox.seenIds';

@Injectable({ providedIn: 'root' })
export class PlatformInboxMonitorService implements OnDestroy {
  private pollSub?: Subscription;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly inbox: PlatformInboxService,
    private readonly shellNotifications: ShellNotificationService,
    private readonly toast: NotificationService,
  ) {}

  start(): void {
    if (this.pollSub) {
      return;
    }
    this.pollSub = interval(POLL_MS)
      .pipe(
        startWith(0),
        switchMap(() => this.inbox.fetchInbox().pipe(catchError(() => of([])))),
      )
      .subscribe((items) => this.sync(items));
  }

  stop(): void {
    this.pollSub?.unsubscribe();
    this.pollSub = undefined;
  }

  ngOnDestroy(): void {
    this.stop();
    this.destroy$.next();
    this.destroy$.complete();
  }

  private sync(items: PlatformInboxNotification[]): void {
    this.shellNotifications.syncPlatformInbox(items);
    const seen = this.readSeen();
    const fresh = items.filter((item) => item.id > 0 && !seen.has(item.id));
    if (!fresh.length) {
      return;
    }
    for (const item of fresh) {
      seen.add(item.id);
    }
    this.writeSeen(seen);
    if (fresh.length === 1) {
      const item = fresh[0];
      this.toast.show(`${item.title}: ${item.body}`, 'View', {
        duration: 7000,
        panelClass: ['app-snackbar-success'],
      });
      return;
    }
    this.toast.show(`${fresh.length} new notifications — open the bell to review.`, 'OK', {
      duration: 6000,
      panelClass: ['app-snackbar-success'],
    });
  }

  private readSeen(): Set<number> {
    try {
      const raw = sessionStorage.getItem(SEEN_KEY);
      if (!raw) {
        return new Set();
      }
      const parsed = JSON.parse(raw) as unknown;
      if (!Array.isArray(parsed)) {
        return new Set();
      }
      return new Set(parsed.map(Number).filter((n) => Number.isFinite(n) && n > 0));
    } catch {
      return new Set();
    }
  }

  private writeSeen(seen: Set<number>): void {
    try {
      sessionStorage.setItem(SEEN_KEY, JSON.stringify([...seen].slice(-200)));
    } catch {
      // best effort
    }
  }
}
