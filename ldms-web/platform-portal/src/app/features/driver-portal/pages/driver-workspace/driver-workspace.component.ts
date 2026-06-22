import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { Router } from '@angular/router';
import { Subject, interval, takeUntil, switchMap, startWith } from 'rxjs';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { DriverPortalService } from '../../services/driver-portal.service';
import { DriverProfileDto, DriverTripRow, DriverWorkspaceMetrics } from '../../models/driver-portal.model';

@Component({
  selector: 'app-driver-workspace',
  templateUrl: './driver-workspace.component.html',
  styleUrls: ['./driver-workspace.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class DriverWorkspaceComponent implements OnInit, OnDestroy {
  driverProfile: DriverProfileDto | null = null;
  trips: DriverTripRow[] = [];
  metrics: DriverWorkspaceMetrics = { activeTrips: 0, completedToday: 0, pendingDeliveries: 0 };

  loadingProfile = true;
  loadingTrips = true;
  profileError = '';
  tripsError = '';
  refreshing = false;
  lastRefreshed: Date | null = null;

  private readonly POLL_INTERVAL_MS = 30_000;
  private readonly destroy$ = new Subject<void>();
  private readonly manualRefresh$ = new Subject<void>();

  constructor(
    private readonly driverService: DriverPortalService,
    private readonly authState: AuthStateService,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadProfile();
    this.startPolling();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── Profile ───────────────────────────────────────────────────────────────

  private loadProfile(): void {
    this.loadingProfile = true;
    this.driverService.getMyDriverProfile().pipe(takeUntil(this.destroy$)).subscribe({
      next: (p) => {
        this.driverProfile = p;
        this.loadingProfile = false;
        this.cdr.markForCheck();
      },
      error: (e: Error) => {
        this.profileError = e.message;
        this.loadingProfile = false;
        this.cdr.markForCheck();
      },
    });
  }

  // ── Trips polling ─────────────────────────────────────────────────────────

  private startPolling(): void {
    this.manualRefresh$
      .pipe(
        startWith(null),
        switchMap(() => interval(this.POLL_INTERVAL_MS).pipe(startWith(0))),
        takeUntil(this.destroy$),
      )
      .subscribe(() => {
        this.fetchTrips();
        this.fetchMetrics();
      });
  }

  private fetchTrips(): void {
    this.loadingTrips = this.trips.length === 0;
    this.driverService.getMyTrips().pipe(takeUntil(this.destroy$)).subscribe({
      next: (t) => {
        this.trips = t;
        this.loadingTrips = false;
        this.refreshing = false;
        this.lastRefreshed = new Date();
        this.tripsError = '';
        this.cdr.markForCheck();
      },
      error: (e: Error) => {
        this.tripsError = e.message;
        this.loadingTrips = false;
        this.refreshing = false;
        this.cdr.markForCheck();
      },
    });
  }

  private fetchMetrics(): void {
    this.driverService.getWorkspaceMetrics().pipe(takeUntil(this.destroy$)).subscribe({
      next: (m) => {
        this.metrics = m;
        this.cdr.markForCheck();
      },
    });
  }

  // ── Actions ───────────────────────────────────────────────────────────────

  refresh(): void {
    this.refreshing = true;
    this.cdr.markForCheck();
    this.manualRefresh$.next();
  }

  openTripDetail(trip: DriverTripRow): void {
    void this.router.navigate(['/driver', 'trip', trip.id]);
  }

  openLiveTracking(trip: DriverTripRow): void {
    void this.router.navigate(['/driver', 'live', trip.id]);
  }

  openDeliveryWorkflow(trip: DriverTripRow): void {
    void this.router.navigate(['/driver', 'trip', trip.id], {
      queryParams: { workflow: '1' },
    });
  }

  // ── Template helpers ──────────────────────────────────────────────────────

  get currentUserName(): string {
    return (
      this.driverProfile?.fullName ||
      this.authState.currentUser?.displayName ||
      'Driver'
    );
  }

  get companyName(): string {
    return this.driverProfile?.organizationName || this.authState.currentUser?.orgName || '';
  }

  get vehicleLabel(): string {
    if (this.driverProfile?.vehicleRegistration) {
      return `${this.driverProfile.vehicleType ?? 'Vehicle'} · ${this.driverProfile.vehicleRegistration}`;
    }
    return 'No vehicle assigned';
  }

  get activeTrips(): DriverTripRow[] {
    return this.trips.filter(
      (t) => !['DELIVERED', 'CANCELLED'].includes(t.status),
    );
  }

  get completedTrips(): DriverTripRow[] {
    return this.trips.filter((t) => t.status === 'DELIVERED');
  }

  statusIcon(tone: DriverTripRow['statusTone']): string {
    const map: Record<string, string> = {
      success: 'check_circle',
      danger: 'error',
      warn: 'warning',
      info: 'info',
      muted: 'radio_button_unchecked',
    };
    return map[tone] ?? 'radio_button_unchecked';
  }
}
