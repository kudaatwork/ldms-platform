import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import * as L from 'leaflet';
import { Subject, interval, switchMap, takeUntil, catchError, of, startWith, take } from 'rxjs';
import { NotificationService } from '../../../../core/services/notification.service';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { DriverPortalService } from '../../../driver-portal/services/driver-portal.service';
import { fuelAlertTone } from '../../../../core/constants/fuel-alert.constants';
import { FuelAlertMonitorService } from '../../../../core/services/fuel-alert-monitor.service';
import { ShellNotificationService } from '../../../../core/services/shell-notification.service';
import type { FuelLiveSnapshot, TripLiveSnapshot, TripTimelineEvent } from '../../models/trip-tracking.model';
import {
  buildJourneyProgressView,
  type JourneyProgressView,
} from '../../utils/journey-progress.util';
import {
  buildJourneyTimeView,
  journeyPhaseIcon,
  type JourneyTimeView,
} from '../../utils/journey-timing.util';
import type { FuelTelemetryLogRow, OperationalFundRequestRow } from '../../models/fuel-expenses.model';
import { FuelExpensesPortalService } from '../../services/fuel-expenses-portal.service';
import { RoadsideProviderService, type RoadsideProviderRow } from '../../services/roadside-provider.service';
import { TripLiveService } from '../../services/trip-live.service';
import { TripTrackingPortalService } from '../../services/trip-tracking-portal.service';

export type LiveMapStyle = 'standard' | 'satellite' | 'live';

@Component({
  selector: 'app-live-trip-tracking',
  templateUrl: './live-trip-tracking.component.html',
  styleUrl: './live-trip-tracking.component.scss',
  standalone: false,
})
export class LiveTripTrackingComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('mapHost') mapHost?: ElementRef<HTMLDivElement>;

  tripId = 0;
  loading = true;
  loadError = '';
  tripSnapshot: TripLiveSnapshot | null = null;
  fuelSnapshot: FuelLiveSnapshot | null = null;
  simulationStarting = false;
  simulationControlBusy = false;
  mapStyle: LiveMapStyle = 'live';
  followTruck = false;
  showHistory = true;
  mapScrollEnabled = false;

  fundRequests: OperationalFundRequestRow[] = [];
  telemetryLogs: FuelTelemetryLogRow[] = [];
  timelineEvents: TripTimelineEvent[] = [];
  tripStartedAtLabel = '';
  private clockTickMs = Date.now();
  requestBusy = false;
  showRequestForm = false;
  requestType: 'FUEL_TOP_UP' | 'FUNDS' | 'MECHANIC' = 'FUEL_TOP_UP';
  requestLiters = 80;
  requestAmount = 150;
  requestNotes = '';
  roadsideProviders: RoadsideProviderRow[] = [];
  roadsideProvidersLoading = false;

  private map?: L.Map;
  private baseTileLayer?: L.TileLayer;
  private routeLayer?: L.Polyline;
  private trailLayer?: L.Polyline;
  private truckMarker?: L.Marker;
  private waypointMarkers: L.Marker[] = [];
  private stopMarkers: L.Marker[] = [];
  private providerMarkers: L.Marker[] = [];
  private providersLoadedForTrip = false;
  private readonly trailPoints: L.LatLngExpression[] = [];
  private readonly destroy$ = new Subject<void>();
  private displayLat = 0;
  private displayLng = 0;
  private animFrame = 0;
  private mapInitAttempts = 0;
  private routeFitted = false;
  private waypointSignature = '';
  private readonly recenterZoom = 15;
  private mapResizeObserver?: ResizeObserver;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly title: Title,
    private readonly tripLive: TripLiveService,
    private readonly tripPortal: TripTrackingPortalService,
    private readonly fuelExpenses: FuelExpensesPortalService,
    private readonly roadsideProvidersApi: RoadsideProviderService,
    private readonly notifications: NotificationService,
    private readonly authState: AuthStateService,
    private readonly driverPortal: DriverPortalService,
    private readonly shellNotifications: ShellNotificationService,
    private readonly fuelAlertMonitor: FuelAlertMonitorService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.tripId = Number(this.route.snapshot.paramMap.get('tripId') ?? 0);
    if (!this.tripId) {
      this.loadError = 'Invalid trip.';
      this.loading = false;
      return;
    }
    this.title.setTitle(`Live track · TRP | LX Platform`);
    this.fuelAlertMonitor.watchTrip(this.tripId, {
      tripNumber: `Trip #${this.tripId}`,
    });
    this.startPolling();
    this.loadOperationalData();
    this.loadTimeline();
    interval(1000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.clockTickMs = Date.now();
        this.cdr.markForCheck();
      });
  }

  ngAfterViewInit(): void {
    this.scheduleMapInit();
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.animFrame);
    this.mapResizeObserver?.disconnect();
    this.destroy$.next();
    this.destroy$.complete();
    this.waypointMarkers.forEach((m) => m.remove());
    this.waypointMarkers = [];
    this.waypointSignature = '';
    this.map?.remove();
    this.map = undefined;
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    this.refreshMapSize();
  }

  get routeLabel(): string {
    if (!this.tripSnapshot) {
      return '—';
    }
    return `${this.tripSnapshot.fromWarehouseName} → ${this.tripSnapshot.toWarehouseName}`;
  }

  get isRoadsideHold(): boolean {
    return String(this.tripSnapshot?.status ?? '').toUpperCase() === 'ROADSIDE_HOLD';
  }

  get vehicleLabel(): string {
    return this.tripSnapshot?.vehicleRegistration || 'Vehicle pending';
  }

  get driverLabel(): string {
    return this.tripSnapshot?.driverName || 'Driver pending';
  }

  get cargoLabel(): string {
    if (!this.tripSnapshot) {
      return '—';
    }
    const product = (this.tripSnapshot.productName ?? '').trim();
    const code = (this.tripSnapshot.productCode ?? '').trim();
    const qty = this.tripSnapshot.quantity;
    if (!product && (qty == null || Number.isNaN(qty))) {
      return 'No cargo linked';
    }
    const parts: string[] = [];
    if (product) {
      parts.push(product);
    }
    if (code) {
      parts.push(`(${code})`);
    }
    if (qty != null && !Number.isNaN(qty)) {
      parts.push(`× ${qty.toLocaleString()}`);
    }
    return parts.join(' ');
  }

  get shipmentRefLabel(): string {
    return this.tripSnapshot?.shipmentNumber || (this.tripSnapshot?.shipmentId ? `SHP-${this.tripSnapshot.shipmentId}` : '—');
  }

  get distanceTravelledKm(): number {
    return this.tripSnapshot?.distanceTravelledKm ?? this.fuelSnapshot?.distanceTravelledKm ?? 0;
  }

  get maxSpeedKmh(): number | undefined {
    return this.tripSnapshot?.maxSpeedKmh;
  }

  get speedGaugePct(): number {
    const max = this.maxSpeedKmh;
    if (!max || max <= 0) {
      return Math.min(100, (this.tripSnapshot?.speedKmh ?? 0) / 1.2);
    }
    return Math.min(100, ((this.tripSnapshot?.speedKmh ?? 0) / max) * 100);
  }

  get meaningfulStops(): TripTimelineEvent[] {
    const hidden = new Set(['CHECKPOINT', 'NOTE', 'OTP_SENT', 'OTP_VERIFIED']);
    return this.timelineEvents.filter((e) => !hidden.has(String(e.eventType).toUpperCase()));
  }

  get trailPointCount(): number {
    return this.tripSnapshot?.trail?.length ?? this.trailPoints.length;
  }

  get journeyView(): JourneyTimeView {
    return buildJourneyTimeView(this.tripSnapshot, {
      nowMs: this.clockTickMs,
      startedAtLabel: this.tripStartedAtLabel || undefined,
      timeline: this.timelineEvents,
    });
  }

  get journeyPhaseIcon(): string {
    return journeyPhaseIcon(this.journeyView.phase);
  }

  get journeyProgress(): JourneyProgressView {
    return buildJourneyProgressView(this.tripSnapshot);
  }

  setMapStyle(style: LiveMapStyle): void {
    this.mapStyle = style;
    this.applyMapStyle();
  }

  toggleFollowTruck(): void {
    this.followTruck = !this.followTruck;
    if (this.followTruck) {
      this.recenterOnTruck(false);
    }
  }

  recenterOnTruck(animate = true): void {
    if (!this.map || !this.displayLat || !this.displayLng) {
      return;
    }
    const heading = this.tripSnapshot?.headingDeg ?? 0;
    const ahead = this.pointAhead(this.displayLat, this.displayLng, heading, 650);
    const bounds = L.latLngBounds([this.displayLat, this.displayLng], ahead);
    this.map.fitBounds(bounds, {
      padding: [72, 72],
      maxZoom: this.recenterZoom,
      animate,
      duration: animate ? 0.75 : 0,
    });
  }

  mapZoomIn(): void {
    this.map?.zoomIn();
  }

  mapZoomOut(): void {
    this.map?.zoomOut();
  }

  enableMapScroll(): void {
    if (!this.map || this.mapScrollEnabled) {
      return;
    }
    this.map.scrollWheelZoom.enable();
    this.mapScrollEnabled = true;
  }

  pauseVehicle(): void {
    if (!this.tripId || this.simulationControlBusy) return;
    this.simulationControlBusy = true;
    this.tripLive.pauseDemoSimulation(this.tripId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (snap) => {
        this.applyTripSnapshot(snap);
        this.simulationControlBusy = false;
        this.loadTimeline();
        this.notifications.success('Vehicle halted on corridor.');
        this.cdr.markForCheck();
      },
      error: (err: Error) => {
        this.simulationControlBusy = false;
        this.notifications.error(err.message);
        this.cdr.markForCheck();
      },
    });
  }

  resumeVehicle(): void {
    if (!this.tripId || this.simulationControlBusy) return;
    this.simulationControlBusy = true;
    this.tripLive.resumeDemoSimulation(this.tripId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (snap) => {
        this.applyTripSnapshot(snap);
        this.simulationControlBusy = false;
        this.loadTimeline();
        this.notifications.success('Vehicle resumed movement.');
        this.cdr.markForCheck();
      },
      error: (err: Error) => {
        this.simulationControlBusy = false;
        this.notifications.error(err.message);
        this.cdr.markForCheck();
      },
    });
  }

  stopSimulation(): void {
    if (!this.tripId || this.simulationControlBusy) return;
    this.simulationControlBusy = true;
    this.tripLive.stopDemoSimulation(this.tripId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (snap) => {
        this.applyTripSnapshot(snap);
        this.simulationControlBusy = false;
        this.notifications.success('IoT simulation stopped.');
        this.cdr.markForCheck();
      },
      error: (err: Error) => {
        this.simulationControlBusy = false;
        this.notifications.error(err.message);
        this.cdr.markForCheck();
      },
    });
  }

  stopIcon(type: string): string {
    switch (String(type).toUpperCase()) {
      case 'ROADSIDE_FUEL_STOP':
        return 'local_gas_station';
      case 'ROADSIDE_MECHANIC_STOP':
        return 'build';
      case 'DRIVER_BREAK':
        return 'free_breakfast';
      case 'DRIVER_RESUMED':
        return 'play_circle';
      case 'ARRIVED_AT_BORDER':
        return 'flag';
      case 'BORDER_CLEARED':
        return 'verified';
      case 'ARRIVED':
        return 'place';
      default:
        return 'location_on';
    }
  }

  completeRoadsideStop(): void {
    if (!this.tripId || this.requestBusy) return;
    this.requestBusy = true;
    this.fuelExpenses
      .completeRoadsideStop(this.tripId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.requestBusy = false;
          this.notifications.success('Roadside stop completed — trip resumed.');
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.requestBusy = false;
          this.notifications.error(err.message);
          this.cdr.markForCheck();
        },
      });
  }

  get fuelTone(): 'ok' | 'warn' | 'critical' {
    return fuelAlertTone(this.fuelSnapshot?.fuelLevelPct);
  }

  get fuelAlertMessage(): string {
    if (this.fuelTone === 'critical') {
      return 'Critical fuel — request an urgent top-up';
    }
    if (this.fuelTone === 'warn') {
      return 'Fuel running low — plan a top-up soon';
    }
    return '';
  }

  get fuelArcOffset(): number {
    const pct = Math.max(0, Math.min(100, this.fuelSnapshot?.fuelLevelPct ?? 0));
    const circumference = 2 * Math.PI * 54;
    return circumference * (1 - pct / 100);
  }

  get progressLabel(): string {
    return `${Math.round(this.tripSnapshot?.overallProgressPct ?? 0)}`;
  }

  get showArrivalPrompt(): boolean {
    const s = this.tripSnapshot;
    return !!(
      s?.awaitingArrivalConfirmation
      || ((s?.overallProgressPct ?? 0) >= 97 && s?.status === 'IN_TRANSIT')
    );
  }

  get showDeliveryBanner(): boolean {
    const status = String(this.tripSnapshot?.status ?? '').toUpperCase();
    return ['ARRIVED', 'COUNTING_STOCK', 'COUNT_COMPLETE', 'OTP_PENDING', 'DELIVERED'].includes(status);
  }

  get showReturnBanner(): boolean {
    return this.tripSnapshot?.returnJourneyActive === true
      || String(this.tripSnapshot?.status ?? '').toUpperCase() === 'RETURN_IN_TRANSIT';
  }

  openDeliveryWorkflow(): void {
    const fromDispatcher = !this.router.url.startsWith('/driver');
    void this.router.navigate(['/driver/trip', this.tripId], {
      queryParams: fromDispatcher ? { workflow: '1', returnTo: 'trips' } : { workflow: '1' },
    });
  }

  confirmArrivalFromLive(): void {
    const userId = Number(this.authState.currentUser?.userId ?? 0);
    if (!userId) {
      this.notifications.error('Sign in to confirm arrival.');
      return;
    }
    this.tripPortal
      .triggerArrival({ tripId: this.tripId, driverUserId: userId })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.notifications.success('Arrival confirmed.');
          this.driverPortal
            .getMyTripById(this.tripId)
            .pipe(take(1), takeUntil(this.destroy$))
            .subscribe({
              next: () => this.openDeliveryWorkflow(),
              error: () => {
                this.notifications.success(
                  'Arrival recorded. Continue delivery steps from this live map or your dispatcher workspace.',
                );
                this.cdr.markForCheck();
              },
            });
        },
        error: (err: Error) => this.notifications.error(err.message || 'Could not confirm arrival.'),
      });
  }

  goBack(): void {
    if (this.router.url.startsWith('/driver')) {
      void this.router.navigate(['/driver/workspace']);
      return;
    }
    void this.router.navigate(['/shipments/trips']);
  }

  /** Navigate to the driver trip workflow with arrival suggested flag. */
  confirmArrivalFromMap(): void {
    if (!this.tripId) {
      return;
    }
    const fromDispatcher = !this.router.url.startsWith('/driver');
    void this.router.navigate(['/driver', 'trip', this.tripId], {
      queryParams: fromDispatcher
        ? { workflow: '1', arrivalSuggested: '1', returnTo: 'trips' }
        : { workflow: '1', arrivalSuggested: '1' },
    });
  }

  startSimulation(): void {
    this.simulationStarting = true;
    this.routeFitted = false;
    this.trailPoints.length = 0;
    this.tripLive.startDemoSimulation(this.tripId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (snap) => {
        this.applyTripSnapshot(snap);
        this.simulationStarting = false;
        this.notifications.success('Corridor simulation started — truck is moving.');
        this.tripLive.getFuelLive(this.tripId).pipe(takeUntil(this.destroy$), catchError(() => of(null))).subscribe((fuel) => {
          if (fuel) {
            this.applyFuelSnapshot(fuel);
          }
          this.cdr.markForCheck();
        });
        this.recenterOnTruck();
        this.cdr.markForCheck();
      },
      error: (err: Error) => {
        this.simulationStarting = false;
        this.notifications.error(err.message || 'Could not start simulation.');
        this.cdr.markForCheck();
      },
    });
  }

  toggleRequestForm(): void {
    this.showRequestForm = !this.showRequestForm;
  }

  submitFundRequest(): void {
    const fleetDriverId = this.fuelSnapshot?.fleetDriverId;
    if (!fleetDriverId) {
      this.notifications.error('Driver context is not available yet — wait for the fuel session to load.');
      return;
    }
    this.requestBusy = true;
    const lat = this.tripSnapshot?.latitude;
    const lng = this.tripSnapshot?.longitude;
    this.fuelExpenses
      .createFundRequest({
        tripId: this.tripId,
        fleetDriverId,
        fleetAssetId: this.fuelSnapshot?.fleetAssetId,
        requestType: this.requestType,
        litersRequested: this.requestType === 'FUEL_TOP_UP' ? this.requestLiters : undefined,
        amountRequested: this.requestType === 'FUNDS' ? this.requestAmount : undefined,
        currencyCode: 'USD',
        latitude: lat,
        longitude: lng,
        driverNotes: this.requestNotes.trim() || undefined,
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.requestBusy = false;
          this.showRequestForm = false;
          this.requestNotes = '';
          this.notifications.success('Driver request submitted.');
          this.loadOperationalData();
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.requestBusy = false;
          this.notifications.error(err.message);
          this.cdr.markForCheck();
        },
      });
  }

  approveRequest(row: OperationalFundRequestRow): void {
    this.requestBusy = true;
    this.fuelExpenses
      .approveFundRequest(row.id, row.litersRequested, row.amountRequested)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.requestBusy = false;
          this.notifications.success(`${row.requestNumber} approved.`);
          this.loadOperationalData();
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.requestBusy = false;
          this.notifications.error(err.message);
          this.cdr.markForCheck();
        },
      });
  }

  rejectRequest(row: OperationalFundRequestRow): void {
    this.requestBusy = true;
    this.fuelExpenses
      .rejectFundRequest(row.id, 'Rejected from live tracking console')
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.requestBusy = false;
          this.notifications.success(`${row.requestNumber} rejected.`);
          this.loadOperationalData();
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.requestBusy = false;
          this.notifications.error(err.message);
          this.cdr.markForCheck();
        },
      });
  }

  private loadTimeline(): void {
    this.tripPortal
      .trackTrip(this.tripId)
      .pipe(takeUntil(this.destroy$), catchError(() => of(null)))
      .subscribe((detail) => {
        if (detail?.timeline) {
          this.timelineEvents = detail.timeline;
          this.tripStartedAtLabel = detail.startedAtLabel;
          this.renderStopMarkers();
          this.cdr.markForCheck();
        }
      });
  }

  private loadOperationalData(): void {
    this.fuelExpenses
      .findFundRequestsByTrip(this.tripId)
      .pipe(takeUntil(this.destroy$), catchError(() => of([])))
      .subscribe((rows) => {
        this.fundRequests = rows;
        this.cdr.markForCheck();
      });

    this.fuelExpenses
      .findTelemetryByTrip(this.tripId)
      .pipe(takeUntil(this.destroy$), catchError(() => of([])))
      .subscribe((rows) => {
        this.telemetryLogs = rows.slice(0, 8);
        this.cdr.markForCheck();
      });
  }

  private startPolling(): void {
    interval(2000)
      .pipe(
        startWith(0),
        switchMap(() =>
          this.tripLive.getLiveSnapshot(this.tripId).pipe(
            catchError((err: Error) => {
              if (!this.tripSnapshot) {
                this.loadError = err.message;
              }
              return of(null);
            }),
          ),
        ),
        takeUntil(this.destroy$),
      )
      .subscribe((snap) => {
        if (snap) {
          this.applyTripSnapshot(snap);
          this.loading = false;
          this.loadError = '';
          requestAnimationFrame(() => this.refreshMapSize());
        }
        this.cdr.markForCheck();
      });

    interval(2000)
      .pipe(
        startWith(0),
        switchMap(() =>
          this.tripLive.getFuelLive(this.tripId).pipe(catchError(() => of(null))),
        ),
        takeUntil(this.destroy$),
      )
      .subscribe((fuel) => {
        if (fuel) {
          this.applyFuelSnapshot(this.mergeFuelWithTripSnapshot(fuel));
        }
        this.cdr.markForCheck();
      });

    interval(5000)
      .pipe(startWith(0), takeUntil(this.destroy$))
      .subscribe(() => {
        this.loadOperationalData();
        this.loadTimeline();
      });
  }

  private applyTripSnapshot(snap: TripLiveSnapshot): void {
    const prev = this.tripSnapshot;
    this.tripSnapshot = { ...snap, lastTimingTickMs: Date.now() };
    this.title.setTitle(`Live · ${snap.tripNumber} | LX Platform`);
    this.fuelAlertMonitor.watchTrip(this.tripId, {
      tripNumber: snap.tripNumber,
      vehicleLabel: snap.vehicleRegistration || undefined,
    });
    if (snap.fuelLevelPct != null || snap.fuelRemainingLiters != null || (snap.distanceTravelledKm ?? 0) > 0) {
      this.applyFuelSnapshot(this.deriveFuelFromTripSnapshot(snap));
    }
    this.ensureMap();

    if (!this.map || snap.latitude == null || snap.longitude == null) {
      this.renderRoute(snap);
      return;
    }

    const targetLat = snap.latitude;
    const targetLng = snap.longitude;
    if (!prev?.latitude || !prev?.longitude) {
      this.displayLat = targetLat;
      this.displayLng = targetLng;
      this.renderRoute(snap);
      this.updateMarker(targetLat, targetLng, snap.headingDeg);
      if (!this.providersLoadedForTrip) {
        this.loadRoadsideProviders(targetLat, targetLng);
      }
      return;
    }

    this.animateTo(targetLat, targetLng, snap.headingDeg);
    this.renderRoute(snap);
    if (snap.latitude != null && snap.longitude != null && !this.providersLoadedForTrip) {
      this.loadRoadsideProviders(snap.latitude, snap.longitude);
    }
  }

  planStopAtProvider(provider: RoadsideProviderRow): void {
    this.requestType = provider.providerType === 'MECHANIC' ? 'MECHANIC' : 'FUEL_TOP_UP';
    this.showRequestForm = true;
    this.requestNotes = `Planned stop: ${provider.name}${provider.addressLabel ? ` (${provider.addressLabel})` : ''}`;
    this.notifications.success(
      `Prepare a ${(provider.providerTypeLabel ?? 'fuel stop').toLowerCase()} request for ${provider.name}.`,
    );
    this.cdr.markForCheck();
  }

  providerGlyph(type: string): string {
    switch (String(type).toUpperCase()) {
      case 'MECHANIC':
        return '🔧';
      case 'ROADSIDE_SUPPORT':
        return '🛟';
      default:
        return '⛽';
    }
  }

  private loadRoadsideProviders(lat: number, lng: number): void {
    this.roadsideProvidersLoading = true;
    this.roadsideProvidersApi
      .listNearby(lat, lng, 180)
      .pipe(takeUntil(this.destroy$), catchError(() => of([])))
      .subscribe((rows) => {
        this.roadsideProviders = rows.slice(0, 12);
        this.roadsideProvidersLoading = false;
        this.providersLoadedForTrip = true;
        this.renderProviderMarkers();
        this.cdr.markForCheck();
      });
  }

  private renderProviderMarkers(): void {
    if (!this.map) {
      return;
    }
    this.providerMarkers.forEach((m) => m.remove());
    this.providerMarkers = this.roadsideProviders.map((p) =>
      L.marker([p.latitude, p.longitude], {
        icon: this.providerIconMarker(p.providerType),
        zIndexOffset: 300,
      })
        .bindTooltip(
          `<strong>${p.name}</strong><br>${p.providerTypeLabel}${p.distanceKm != null ? `<br>${p.distanceKm} km away` : ''}`,
          { direction: 'top', sticky: true },
        )
        .addTo(this.map!),
    );
  }

  private providerIconMarker(type: string): L.DivIcon {
    const tone =
      String(type).toUpperCase() === 'MECHANIC'
        ? 'mechanic'
        : String(type).toUpperCase() === 'ROADSIDE_SUPPORT'
          ? 'support'
          : 'fuel';
    return L.divIcon({
      className: `lt-map-provider lt-map-provider--${tone}`,
      html: `<span class="lt-map-provider__glyph">${this.providerGlyph(type)}</span>`,
      iconSize: [34, 34],
      iconAnchor: [17, 17],
    });
  }

  private applyFuelSnapshot(fuel: FuelLiveSnapshot): void {
    this.fuelSnapshot = fuel;
    this.shellNotifications.syncFuelAlert({
      tripId: this.tripId,
      tripNumber: this.tripSnapshot?.tripNumber ?? `Trip #${this.tripId}`,
      vehicleLabel: this.tripSnapshot?.vehicleRegistration || this.vehicleLabel || undefined,
      fuelLevelPct: fuel.fuelLevelPct,
      litersRemaining: fuel.fuelRemainingLiters,
    });
  }

  private deriveFuelFromTripSnapshot(snap: TripLiveSnapshot): FuelLiveSnapshot {
    const distanceKm = snap.distanceTravelledKm ?? 0;
    const fuelRemainingLiters =
      snap.fuelRemainingLiters ?? Math.max(0, 400 - distanceKm * 0.35);
    const fuelLevelPct =
      snap.fuelLevelPct ?? Math.max(0, (fuelRemainingLiters / 400) * 100);
    return {
      tripId: snap.tripId,
      fleetDriverId: this.fuelSnapshot?.fleetDriverId,
      fleetAssetId: snap.fleetAssetId ?? this.fuelSnapshot?.fleetAssetId,
      fuelLevelPct,
      fuelRemainingLiters,
      tankCapacityLiters: 400,
      distanceTravelledKm: distanceKm,
      consumptionRateLPer100Km: 35,
      moving: snap.moving,
      status: 'ACTIVE',
    };
  }

  private mergeFuelWithTripSnapshot(fuel: FuelLiveSnapshot): FuelLiveSnapshot {
    const snap = this.tripSnapshot;
    if (!snap) {
      return fuel;
    }
    const derived = this.deriveFuelFromTripSnapshot(snap);
    const derivedHasProgress =
      (snap.distanceTravelledKm ?? 0) > 0
      || snap.fuelLevelPct != null
      || snap.fuelRemainingLiters != null;
    if (!derivedHasProgress) {
      return fuel;
    }
    const useDerived =
      fuel.fuelLevelPct >= 99.95 && derived.fuelLevelPct < fuel.fuelLevelPct;
    if (useDerived || derived.fuelLevelPct < fuel.fuelLevelPct) {
      return {
        ...fuel,
        fuelLevelPct: derived.fuelLevelPct,
        fuelRemainingLiters: derived.fuelRemainingLiters,
        distanceTravelledKm: derived.distanceTravelledKm ?? fuel.distanceTravelledKm,
        moving: snap.moving || fuel.moving,
      };
    }
    return fuel;
  }

  get fuelDisplayFormat(): string {
    if (this.tripSnapshot?.moving || this.tripSnapshot?.simulationActive) {
      return '1.1-2';
    }
    return '1.0-0';
  }

  get fuelLitersDisplayFormat(): string {
    if (this.tripSnapshot?.moving || this.tripSnapshot?.simulationActive) {
      return '1.1-1';
    }
    return '1.0-0';
  }

  private animateTo(targetLat: number, targetLng: number, heading: number): void {
    const fromLat = this.displayLat;
    const fromLng = this.displayLng;
    const start = performance.now();
    const duration = 1800;

    const step = (now: number) => {
      const t = Math.min(1, (now - start) / duration);
      const eased = 1 - Math.pow(1 - t, 3);
      this.displayLat = fromLat + (targetLat - fromLat) * eased;
      this.displayLng = fromLng + (targetLng - fromLng) * eased;
      this.updateMarker(this.displayLat, this.displayLng, heading);
      if (t < 1) {
        this.animFrame = requestAnimationFrame(step);
      }
    };
    cancelAnimationFrame(this.animFrame);
    this.animFrame = requestAnimationFrame(step);
  }

  private scheduleMapInit(): void {
    requestAnimationFrame(() => this.ensureMap());
  }

  private ensureMap(): void {
    if (this.map) {
      return;
    }

    const host = this.mapHost?.nativeElement;
    if (!host) {
      if (this.mapInitAttempts++ < 24) {
        requestAnimationFrame(() => this.ensureMap());
      }
      return;
    }

    if (host.offsetWidth < 2 || host.offsetHeight < 2) {
      if (this.mapInitAttempts++ < 24) {
        requestAnimationFrame(() => this.ensureMap());
      }
      return;
    }

    this.initMap(host);
    if (this.tripSnapshot) {
      this.renderRoute(this.tripSnapshot);
      if (this.tripSnapshot.latitude != null && this.tripSnapshot.longitude != null) {
        this.displayLat = this.tripSnapshot.latitude;
        this.displayLng = this.tripSnapshot.longitude;
        this.updateMarker(this.displayLat, this.displayLng, this.tripSnapshot.headingDeg);
      }
    }
  }

  private initMap(host: HTMLElement): void {
    if (this.map) {
      return;
    }

    this.map = L.map(host, {
      zoomControl: false,
      attributionControl: true,
      scrollWheelZoom: false,
      wheelDebounceTime: 120,
      wheelPxPerZoomLevel: 90,
    }).setView([-19.0, 30.0], 7);

    this.applyMapStyle();
    this.bindMapInteractionGuards(host);
    this.observeMapPanelResize(host);

    requestAnimationFrame(() => {
      this.refreshMapSize();
    });
  }

  private observeMapPanelResize(host: HTMLElement): void {
    const panel = host.closest('.lt-map-panel');
    if (!panel || typeof ResizeObserver === 'undefined') {
      return;
    }
    this.mapResizeObserver?.disconnect();
    this.mapResizeObserver = new ResizeObserver(() => this.refreshMapSize());
    this.mapResizeObserver.observe(panel);
  }

  private refreshMapSize(): void {
    if (!this.map) {
      return;
    }
    this.map.invalidateSize({ animate: false });
    if (this.tripSnapshot) {
      this.renderRoute(this.tripSnapshot);
      if (this.displayLat && this.displayLng) {
        this.updateMarker(this.displayLat, this.displayLng, this.tripSnapshot.headingDeg);
      }
    }
  }

  private applyMapStyle(): void {
    if (!this.map) {
      return;
    }
    if (this.baseTileLayer) {
      this.map.removeLayer(this.baseTileLayer);
    }
    const config = this.tileLayerForStyle(this.mapStyle);
    this.baseTileLayer = L.tileLayer(config.url, config.options).addTo(this.map);
  }

  private tileLayerForStyle(style: LiveMapStyle): { url: string; options: L.TileLayerOptions } {
    switch (style) {
      case 'standard':
        return {
          url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
          options: { attribution: '&copy; OpenStreetMap contributors', maxZoom: 19 },
        };
      case 'satellite':
        return {
          url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
          options: { attribution: '&copy; Esri', maxZoom: 19 },
        };
      default:
        return {
          url: 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
          options: {
            attribution: '&copy; OpenStreetMap &copy; CARTO',
            subdomains: 'abcd',
            maxZoom: 19,
          },
        };
    }
  }

  private renderRoute(snap: TripLiveSnapshot): void {
    if (!this.map || !snap.routeWaypoints?.length) {
      return;
    }
    const latlngs: L.LatLngExpression[] = snap.routeWaypoints.map((w) => [w.latitude, w.longitude]);

    if (!this.routeLayer) {
      this.routeLayer = L.polyline(latlngs, {
        color: '#38bdf8',
        weight: 4,
        opacity: 0.45,
        dashArray: '8 12',
      }).addTo(this.map);
    } else {
      this.routeLayer.setLatLngs(latlngs);
    }

    this.syncWaypointMarkers(snap);

    const serverTrail = snap.trail?.filter((p) => p.latitude && p.longitude) ?? [];

    if (snap.latitude != null && snap.longitude != null && !serverTrail.length) {
      this.trailPoints.push([snap.latitude, snap.longitude]);
      if (this.trailPoints.length > 500) {
        this.trailPoints.shift();
      }
    }

    const activeTrail = serverTrail.length > 1 ? serverTrail.map((p) => [p.latitude, p.longitude] as L.LatLngExpression) : this.trailPoints;
    if (activeTrail.length > 1) {
      if (!this.trailLayer) {
        this.trailLayer = L.polyline(activeTrail, {
          color: '#34d399',
          weight: 4,
          opacity: 0.9,
        }).addTo(this.map);
      } else {
        this.trailLayer.setLatLngs(activeTrail);
      }
    }

    if (!this.routeFitted) {
      try {
        this.map.fitBounds(this.routeLayer.getBounds(), { padding: [48, 48], maxZoom: 10 });
        this.routeFitted = true;
      } catch {
        this.map.setView(latlngs[0], 8);
      }
    }

    if (this.followTruck && snap.latitude != null && snap.longitude != null) {
      this.map.panTo([snap.latitude, snap.longitude], { animate: false });
    }
  }

  private syncWaypointMarkers(snap: TripLiveSnapshot): void {
    if (!this.map || !snap.routeWaypoints?.length) {
      return;
    }
    const signature = snap.routeWaypoints
      .map((w) => `${w.latitude},${w.longitude},${w.type},${w.label}`)
      .join('|');
    if (signature === this.waypointSignature) {
      return;
    }
    this.waypointSignature = signature;
    this.waypointMarkers.forEach((m) => m.remove());
    this.waypointMarkers = snap.routeWaypoints.map((w) =>
      L.marker([w.latitude, w.longitude], { icon: this.waypointIcon(w), zIndexOffset: 200 })
        .bindTooltip(w.label, {
          permanent: false,
          direction: 'top',
          sticky: false,
          interactive: false,
        })
        .addTo(this.map!),
    );
  }

  private bindMapInteractionGuards(host: HTMLElement): void {
    host.addEventListener('click', () => this.enableMapScroll());
    this.map?.on('mouseout', () => {
      this.map?.scrollWheelZoom.disable();
      this.mapScrollEnabled = false;
    });
  }

  private pointAhead(lat: number, lng: number, headingDeg: number, meters: number): L.LatLng {
    const earthRadius = 6378137;
    const bearing = (headingDeg * Math.PI) / 180;
    const latRad = (lat * Math.PI) / 180;
    const lngRad = (lng * Math.PI) / 180;
    const dist = meters / earthRadius;
    const lat2 = Math.asin(
      Math.sin(latRad) * Math.cos(dist) + Math.cos(latRad) * Math.sin(dist) * Math.cos(bearing),
    );
    const lng2 =
      lngRad +
      Math.atan2(
        Math.sin(bearing) * Math.sin(dist) * Math.cos(latRad),
        Math.cos(dist) - Math.sin(latRad) * Math.sin(lat2),
      );
    return L.latLng((lat2 * 180) / Math.PI, (lng2 * 180) / Math.PI);
  }

  private renderStopMarkers(): void {
    if (!this.map) {
      return;
    }
    this.stopMarkers.forEach((m) => m.remove());
    this.stopMarkers = this.meaningfulStops
      .filter((e) => e.latitude != null && e.longitude != null)
      .map((e) =>
        L.marker([e.latitude!, e.longitude!], {
          icon: this.stopIconMarker(String(e.eventType)),
          zIndexOffset: 400,
        })
          .bindTooltip(`${e.eventTypeLabel}<br><small>${e.recordedAtLabel}</small>`, {
            direction: 'top',
            sticky: false,
            interactive: false,
          })
          .addTo(this.map!),
      );
  }

  private waypointIcon(wp: { type: string; label: string }): L.DivIcon {
    const type = String(wp.type).toUpperCase();
    let glyph = '●';
    let tone = 'checkpoint';
    if (type === 'ORIGIN') {
      glyph = '⌂';
      tone = 'origin';
    } else if (type === 'DESTINATION') {
      glyph = '⚑';
      tone = 'destination';
    } else if (type.includes('TOLL')) {
      glyph = '₿';
      tone = 'toll';
    }
    return L.divIcon({
      className: `lt-map-pin lt-map-pin--${tone}`,
      html: `<span class="lt-map-pin__glyph">${glyph}</span>`,
      iconSize: [28, 28],
      iconAnchor: [14, 14],
    });
  }

  private stopIconMarker(eventType: string): L.DivIcon {
    const glyph = this.stopGlyph(eventType);
    return L.divIcon({
      className: 'lt-map-stop',
      html: `<span class="lt-map-stop__glyph">${glyph}</span>`,
      iconSize: [30, 30],
      iconAnchor: [15, 15],
    });
  }

  private stopGlyph(type: string): string {
    switch (String(type).toUpperCase()) {
      case 'ROADSIDE_FUEL_STOP':
        return '⛽';
      case 'ROADSIDE_MECHANIC_STOP':
        return '🔧';
      case 'DRIVER_BREAK':
        return '☕';
      case 'DRIVER_RESUMED':
        return '▶';
      case 'ARRIVED_AT_BORDER':
        return '🛃';
      case 'BORDER_CLEARED':
        return '✓';
      case 'ARRIVED':
        return '🏁';
      default:
        return '📍';
    }
  }

  private updateMarker(lat: number, lng: number, headingDeg: number): void {
    if (!this.map) {
      return;
    }
    const icon = L.divIcon({
      className: 'lt-map-truck',
      html: `<div class="lt-map-truck__body" style="transform: rotate(${headingDeg}deg)">
        <span class="lt-map-truck__cab"></span>
        <span class="lt-map-truck__trailer"></span>
        <span class="lt-map-truck__pulse"></span>
      </div>`,
      iconSize: [36, 36],
      iconAnchor: [18, 18],
    });

    if (!this.truckMarker) {
      this.truckMarker = L.marker([lat, lng], { icon, zIndexOffset: 1000 }).addTo(this.map);
    } else {
      this.truckMarker.setLatLng([lat, lng]);
      this.truckMarker.setIcon(icon);
    }
    if (this.followTruck) {
      this.map.panTo([lat, lng], { animate: false });
    }
  }
}
