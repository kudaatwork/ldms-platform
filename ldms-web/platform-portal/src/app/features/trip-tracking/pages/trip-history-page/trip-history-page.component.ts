import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { debounceTime, finalize, takeUntil } from 'rxjs/operators';
import { TripTrackingPortalService } from '../../services/trip-tracking-portal.service';
import type { TripRow, TripStatus } from '../../models/trip-tracking.model';

@Component({
  selector: 'app-trip-history-page',
  templateUrl: './trip-history-page.component.html',
  styleUrl: './trip-history-page.component.scss',
  standalone: false,
})
export class TripHistoryPageComponent implements OnInit, OnDestroy {
  loading = false;
  error = '';
  trips: TripRow[] = [];
  search = '';
  statusFilter: TripStatus | '' = '';

  readonly statusOptions: Array<{ value: TripStatus | ''; label: string }> = [
    { value: '', label: 'All completed' },
    { value: 'DELIVERED', label: 'Delivered' },
    { value: 'ARRIVED', label: 'Arrived' },
    { value: 'CANCELLED', label: 'Cancelled' },
  ];

  private readonly reload$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly tripPortal: TripTrackingPortalService,
    private readonly router: Router,
    private readonly title: Title,
    private readonly cdr: ChangeDetectorRef,
  ) {
    this.title.setTitle('Trip history | LX Platform');
  }

  ngOnInit(): void {
    this.reload$.pipe(debounceTime(200), takeUntil(this.destroy$)).subscribe(() => this.fetchTrips());
    this.reload$.next();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get filteredTrips(): TripRow[] {
    const q = this.search.trim().toLowerCase();
    return this.trips.filter((t) => {
      if (this.statusFilter && t.status !== this.statusFilter) {
        return false;
      }
      if (!q) {
        return true;
      }
      const hay = `${t.tripNumber} ${t.shipmentNumber} ${t.route} ${t.driverName} ${t.vehicleRegistration} ${t.cargoLabel} ${t.statusLabel}`.toLowerCase();
      return hay.includes(q);
    });
  }

  onSearchChange(): void {
    this.reload$.next();
  }

  onStatusChange(): void {
    this.cdr.markForCheck();
  }

  refresh(): void {
    this.reload$.next();
  }

  openReplay(trip: TripRow): void {
    void this.router.navigate(['/shipments/replay', trip.id]);
  }

  openLive(trip: TripRow): void {
    void this.router.navigate(['/shipments/live', trip.id]);
  }

  private fetchTrips(): void {
    this.loading = true;
    this.error = '';
    this.tripPortal
      .findTrips({ status: '' })
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.trips = rows
            .filter((t) => ['DELIVERED', 'ARRIVED', 'CANCELLED', 'IN_TRANSIT', 'IN_PROGRESS', 'ROADSIDE_HOLD', 'AT_BORDER_HOLD'].includes(t.status))
            .sort((a, b) => (b.startedAtLabel || '').localeCompare(a.startedAtLabel || ''));
        },
        error: () => {
          this.error = 'Could not load trip history.';
        },
      });
  }
}
