import { Injectable, OnDestroy } from '@angular/core';
import { Subject, Subscription, forkJoin, interval, of, switchMap } from 'rxjs';
import { catchError, map, startWith } from 'rxjs/operators';
import { TripLiveService } from '../../features/trip-tracking/services/trip-live.service';
import { ShellNotificationService } from './shell-notification.service';

const WATCHED_KEY = 'lx.fuelAlert.watchedTrips';
const POLL_MS = 15_000;

export interface WatchedTripFuelMeta {
  tripNumber: string;
  vehicleLabel?: string;
}

@Injectable({ providedIn: 'root' })
export class FuelAlertMonitorService implements OnDestroy {
  private pollSub?: Subscription;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly tripLive: TripLiveService,
    private readonly shellNotifications: ShellNotificationService,
  ) {}

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.pollSub?.unsubscribe();
  }

  watchTrip(tripId: number, meta: WatchedTripFuelMeta): void {
    if (!Number.isFinite(tripId) || tripId <= 0) {
      return;
    }
    const watched = this.readWatched();
    watched[String(tripId)] = meta;
    this.writeWatched(watched);
    this.ensurePolling();
  }

  unwatchTrip(tripId: number): void {
    if (!Number.isFinite(tripId) || tripId <= 0) {
      return;
    }
    const watched = this.readWatched();
    delete watched[String(tripId)];
    this.writeWatched(watched);
    if (!Object.keys(watched).length) {
      this.pollSub?.unsubscribe();
      this.pollSub = undefined;
    }
  }

  resumeWatching(): void {
    if (Object.keys(this.readWatched()).length) {
      this.ensurePolling();
    }
  }

  private ensurePolling(): void {
    if (this.pollSub) {
      return;
    }
    this.pollSub = interval(POLL_MS)
      .pipe(
        startWith(0),
        switchMap(() => {
          const watched = this.readWatched();
          const entries = Object.entries(watched);
          if (!entries.length) {
            return of([]);
          }
          return forkJoin(
            entries.map(([id, meta]) =>
              this.tripLive.getFuelLive(Number(id)).pipe(
                catchError(() => of(null)),
                map((fuel) => ({ tripId: Number(id), meta, fuel })),
              ),
            ),
          );
        }),
      )
      .subscribe((results) => {
        for (const row of results) {
          if (!row.fuel) {
            continue;
          }
          this.shellNotifications.syncFuelAlert({
            tripId: row.tripId,
            tripNumber: row.meta.tripNumber,
            vehicleLabel: row.meta.vehicleLabel,
            fuelLevelPct: row.fuel.fuelLevelPct,
            litersRemaining: row.fuel.fuelRemainingLiters,
          });
        }
      });
  }

  private readWatched(): Record<string, WatchedTripFuelMeta> {
    try {
      const raw = sessionStorage.getItem(WATCHED_KEY);
      if (!raw) {
        return {};
      }
      const parsed = JSON.parse(raw) as unknown;
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
        return {};
      }
      return parsed as Record<string, WatchedTripFuelMeta>;
    } catch {
      return {};
    }
  }

  private writeWatched(watched: Record<string, WatchedTripFuelMeta>): void {
    try {
      sessionStorage.setItem(WATCHED_KEY, JSON.stringify(watched));
    } catch {
      // best effort
    }
  }
}
