import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { Subject, debounceTime } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { TripTrackingPortalService } from '../../services/trip-tracking-portal.service';
import { AllocateShipmentDialogComponent } from '../../components/allocate-shipment-dialog/allocate-shipment-dialog.component';
import type { AllocateShipmentDialogData, AllocateShipmentDialogResult } from '../../components/allocate-shipment-dialog/allocate-shipment-dialog.component';
import { ViewTripDialogComponent } from '../../components/view-trip-dialog/view-trip-dialog.component';
import type { ViewTripDialogData } from '../../components/view-trip-dialog/view-trip-dialog.component';
import type {
  ShipmentRow,
  ShipmentStatus,
  TripRow,
  TripStatus,
  TripTrackingTab,
  TripWorkspaceMetrics,
} from '../../models/trip-tracking.model';

@Component({
  selector: 'app-trip-tracking-workspace',
  templateUrl: './trip-tracking-workspace.component.html',
  styleUrl: './trip-tracking-workspace.component.scss',
  standalone: false,
})
export class TripTrackingWorkspaceComponent implements OnInit, OnDestroy {
  fetching = true;
  loadError = '';
  activeTab: TripTrackingTab = 'shipments';

  // Shipments
  shipments: ShipmentRow[] = [];
  shipmentsLoading = false;
  shipmentsError = '';
  shipmentSearch = '';
  shipmentStatusFilter: ShipmentStatus | '' = '';

  // Trips
  trips: TripRow[] = [];
  tripsLoading = false;
  tripsError = '';
  tripSearch = '';
  tripStatusFilter: TripStatus | '' = '';

  metrics: TripWorkspaceMetrics = {
    totalShipments: 0,
    allocatedShipments: 0,
    activeTrips: 0,
    deliveredToday: 0,
  };

  readonly shipmentStatusOptions: Array<{ value: ShipmentStatus | ''; label: string }> = [
    { value: '', label: 'All statuses' },
    { value: 'PENDING', label: 'Pending' },
    { value: 'ALLOCATED', label: 'Allocated' },
    { value: 'IN_TRANSIT', label: 'In transit' },
    { value: 'DELIVERED', label: 'Delivered' },
    { value: 'CANCELLED', label: 'Cancelled' },
  ];

  readonly tripStatusOptions: Array<{ value: TripStatus | ''; label: string }> = [
    { value: '', label: 'All statuses' },
    { value: 'PENDING', label: 'Pending' },
    { value: 'IN_PROGRESS', label: 'In progress' },
    { value: 'ARRIVED', label: 'Arrived' },
    { value: 'DELIVERED', label: 'Delivered' },
    { value: 'CANCELLED', label: 'Cancelled' },
  ];

  private readonly reload$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly title: Title,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly tripTracking: TripTrackingPortalService,
    private readonly dialog: MatDialog,
    private readonly notifications: NotificationService,
    private readonly authState: AuthStateService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Shipments & Trips | LX Platform');

    this.route.params.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const tab = params['tab'] as TripTrackingTab | undefined;
      if (tab === 'trips' || tab === 'shipments') {
        this.activeTab = tab;
      }
    });

    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const transferId = params.get('transferId');
      if (transferId) {
        this.activeTab = 'shipments';
        this.loadShipmentByTransfer(Number(transferId));
      }
    });

    this.reload$.pipe(debounceTime(120), takeUntil(this.destroy$)).subscribe(() => this.loadWorkspace());
    this.reload$.next();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get orgId(): number {
    return Number(this.authState.currentUser?.organizationId ?? 0);
  }

  // ── Tab switching ─────────────────────────────────────────────────────────

  setTab(tab: TripTrackingTab): void {
    this.activeTab = tab;
    this.router.navigate(['.'], {
      relativeTo: this.route.parent,
      queryParamsHandling: 'preserve',
    });
    this.router.navigate([`../${tab}`], { relativeTo: this.route });
  }

  isTab(tab: TripTrackingTab): boolean {
    return this.activeTab === tab;
  }

  // ── Data loading ──────────────────────────────────────────────────────────

  private loadWorkspace(): void {
    this.fetching = true;
    this.loadError = '';
    this.loadShipments();
    this.loadTrips();
  }

  private loadShipments(): void {
    this.shipmentsLoading = true;
    this.shipmentsError = '';
    this.tripTracking
      .findShipments({
        organizationId: this.orgId || undefined,
        status: this.shipmentStatusFilter || undefined,
        search: this.shipmentSearch.trim() || undefined,
      })
      .pipe(
        finalize(() => {
          this.shipmentsLoading = false;
          this.fetching = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.shipments = rows;
          this.updateMetrics();
        },
        error: (err: Error) => {
          this.shipmentsError = err.message || 'Failed to load shipments.';
        },
      });
  }

  private loadTrips(): void {
    this.tripsLoading = true;
    this.tripsError = '';
    this.tripTracking
      .findTrips({
        organizationId: this.orgId || undefined,
        status: this.tripStatusFilter || undefined,
        search: this.tripSearch.trim() || undefined,
      })
      .pipe(
        finalize(() => {
          this.tripsLoading = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.trips = rows;
          this.updateMetrics();
        },
        error: (err: Error) => {
          this.tripsError = err.message || 'Failed to load trips.';
        },
      });
  }

  private loadShipmentByTransfer(transferId: number): void {
    this.tripTracking.getShipmentByTransfer(transferId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (shipment) => {
        if (shipment) {
          this.shipments = [shipment, ...this.shipments.filter((s) => s.id !== shipment.id)];
          this.updateMetrics();
        }
      },
    });
  }

  private updateMetrics(): void {
    this.metrics = this.tripTracking.buildMetrics(this.shipments, this.trips);
  }

  // ── Search & filter ───────────────────────────────────────────────────────

  onShipmentSearchChange(): void {
    this.reload$.next();
  }

  onShipmentStatusChange(): void {
    this.reload$.next();
  }

  onTripSearchChange(): void {
    this.reload$.next();
  }

  onTripStatusChange(): void {
    this.reload$.next();
  }

  refresh(): void {
    this.reload$.next();
  }

  // ── Actions ───────────────────────────────────────────────────────────────

  openAllocate(shipment: ShipmentRow): void {
    const dialogRef = this.dialog.open<
      AllocateShipmentDialogComponent,
      AllocateShipmentDialogData,
      AllocateShipmentDialogResult | undefined
    >(AllocateShipmentDialogComponent, {
      data: { shipment },
      width: '540px',
      maxHeight: '90vh',
      panelClass: 'lx-location-dialog-panel',
    });

    dialogRef.afterClosed().pipe(takeUntil(this.destroy$)).subscribe((result) => {
      if (result?.action === 'allocated') {
        this.reload$.next();
      }
    });
  }

  startTrip(shipment: ShipmentRow): void {
    const userId = Number(this.authState.currentUser?.userId ?? 0);
    if (!userId) {
      this.notifications.error('Cannot determine current user.');
      return;
    }
    // Re-use the allocate dialog to pick driver/vehicle, then call start
    const dialogRef = this.dialog.open<
      AllocateShipmentDialogComponent,
      AllocateShipmentDialogData,
      AllocateShipmentDialogResult | undefined
    >(AllocateShipmentDialogComponent, {
      data: { shipment },
      width: '540px',
      maxHeight: '90vh',
      panelClass: 'lx-location-dialog-panel',
    });

    dialogRef.afterClosed().pipe(takeUntil(this.destroy$)).subscribe((result) => {
      if (result?.action === 'allocated') {
        // After allocation succeeded, the shipment is now ALLOCATED.
        // Immediately start a trip from the new state.
        this.reload$.next();
      }
    });
  }

  viewTrip(trip: TripRow): void {
    const dialogRef = this.dialog.open<
      ViewTripDialogComponent,
      ViewTripDialogData,
      undefined
    >(ViewTripDialogComponent, {
      data: { trip },
      width: '640px',
      maxHeight: '90vh',
      panelClass: 'lx-location-dialog-panel',
    });

    dialogRef.afterClosed().pipe(takeUntil(this.destroy$)).subscribe((result) => {
      if (result) {
        this.reload$.next();
      }
    });
  }

  // ── Status helpers ────────────────────────────────────────────────────────

  statusBadgeClass(tone: string): string {
    const map: Record<string, string> = {
      muted: 'trt-status--muted',
      warn: 'trt-status--warn',
      success: 'trt-status--success',
      danger: 'trt-status--danger',
      info: 'trt-status--info',
    };
    return map[tone] ?? 'trt-status--muted';
  }
}
