import { Injectable, OnDestroy } from '@angular/core';
import { Subject, interval, switchMap, takeUntil, catchError, of, startWith } from 'rxjs';
import { PlatformWalletService } from './platform-wallet.service';
import { ShellNotificationService } from './shell-notification.service';

@Injectable({ providedIn: 'root' })
export class PlatformWalletAlertMonitorService implements OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private started = false;

  constructor(
    private readonly wallet: PlatformWalletService,
    private readonly shellNotifications: ShellNotificationService,
  ) {}

  start(): void {
    if (this.started) {
      return;
    }
    this.started = true;
    this.pollWalletAlerts();
    interval(60_000)
      .pipe(
        startWith(0),
        takeUntil(this.destroy$),
        switchMap(() => this.loadWalletPayload()),
      )
      .subscribe((payload) => {
        if (payload) {
          this.shellNotifications.syncWalletAlerts(payload.summary, payload.report);
        }
      });
  }

  private pollWalletAlerts(): void {
    this.loadWalletPayload().subscribe((payload) => {
      if (payload) {
        this.shellNotifications.syncWalletAlerts(payload.summary, payload.report);
      }
    });
  }

  private loadWalletPayload() {
    return this.wallet.refreshSummary().pipe(
      switchMap((summary) => {
        const today = new Date().toISOString().slice(0, 10);
        return this.wallet.getUsageReport({ from: today, to: today }).pipe(
          catchError(() => of(null)),
          switchMap((report) => of({ summary, report })),
        );
      }),
      catchError(() => of(null)),
    );
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
