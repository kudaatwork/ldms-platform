import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import * as L from 'leaflet';
import { Subject, interval, switchMap, takeUntil, catchError, of, startWith } from 'rxjs';
import { NotificationService } from '../../../../core/services/notification.service';
import type { FuelLiveSnapshot, TripLiveSnapshot } from '../../models/trip-tracking.model';
import type { FuelTelemetryLogRow, OperationalFundRequestRow } from '../../models/fuel-expenses.model';
import { FuelExpensesPortalService } from '../../services/fuel-expenses-portal.service';
import { TripLiveService } from '../../services/trip-live.service';

@Component({
  selector: 'app-live-trip-tracking',
  templateUrl: './live-trip-tracking.component.html',
  styleUrl: './live-trip-tracking.component.scss',
  standalone: false,
})
export class LiveTripTrackingComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('mapHost', { static: true }) mapHost!: ElementRef<HTMLDivElement>;

  tripId = 0;
  loading = true;
  loadError = '';
  tripSnapshot: TripLiveSnapshot | null = null;
  fuelSnapshot: FuelLiveSnapshot | null = null;
  simulationStarting = false;

  fundRequests: OperationalFundRequestRow[] = [];
  telemetryLogs: FuelTelemetryLogRow[] = [];
  requestBusy = false;
  showRequestForm = false;
  requestType: 'FUEL_TOP_UP' | 'FUNDS' | 'MECHANIC' = 'FUEL_TOP_UP';
  requestLiters = 80;
  requestAmount = 150;
  requestNotes = '';

  private map?: L.Map;
  private routeLayer?: L.Polyline;
  private trailLayer?: L.Polyline;
  private truckMarker?: L.Marker;
  private waypointMarkers: L.CircleMarker[] = [];
  private readonly trailPoints: L.LatLngExpression[] = [];
  private readonly destroy$ = new Subject<void>();
  private displayLat = 0;
  private displayLng = 0;
  private animFrame = 0;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly title: Title,
    private readonly tripLive: TripLiveService,
    private readonly fuelExpenses: FuelExpensesPortalService,
    private readonly notifications: NotificationService,
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
    this.startPolling();
    this.loadOperationalData();
  }

  ngAfterViewInit(): void {
    this.initMap();
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.animFrame);
    this.destroy$.next();
    this.destroy$.complete();
    this.map?.remove();
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
    const pct = this.fuelSnapshot?.fuelLevelPct ?? 100;
    if (pct <= 15) {
      return 'critical';
    }
    if (pct <= 35) {
      return 'warn';
    }
    return 'ok';
  }

  get fuelArcOffset(): number {
    const pct = Math.max(0, Math.min(100, this.fuelSnapshot?.fuelLevelPct ?? 0));
    const circumference = 2 * Math.PI * 54;
    return circumference * (1 - pct / 100);
  }

  goBack(): void {
    void this.router.navigate(['/shipments/trips']);
  }

  startSimulation(): void {
    this.simulationStarting = true;
    this.tripLive.startDemoSimulation(this.tripId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (snap) => {
        this.tripSnapshot = snap;
        this.simulationStarting = false;
        this.notifications.success('Corridor simulation started — truck is moving.');
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
          this.fuelSnapshot = fuel;
          this.cdr.markForCheck();
        }
      });

    interval(5000)
      .pipe(startWith(0), takeUntil(this.destroy$))
      .subscribe(() => this.loadOperationalData());
  }

  private applyTripSnapshot(snap: TripLiveSnapshot): void {
    const prev = this.tripSnapshot;
    this.tripSnapshot = snap;
    this.title.setTitle(`Live · ${snap.tripNumber} | LX Platform`);

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
      return;
    }

    this.animateTo(targetLat, targetLng, snap.headingDeg);
    this.renderRoute(snap);
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

  private initMap(): void {
    if (this.map) {
      return;
    }
    this.map = L.map(this.mapHost.nativeElement, {
      zoomControl: false,
      attributionControl: true,
    }).setView([-19.0, 30.0], 7);

    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      attribution: '&copy; OpenStreetMap &copy; CARTO',
      subdomains: 'abcd',
      maxZoom: 19,
    }).addTo(this.map);

    L.control.zoom({ position: 'bottomright' }).addTo(this.map);
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

    this.waypointMarkers.forEach((m) => m.remove());
    this.waypointMarkers = snap.routeWaypoints.map((w) => {
      const isEndpoint = w.type === 'ORIGIN' || w.type === 'DESTINATION';
      return L.circleMarker([w.latitude, w.longitude], {
        radius: isEndpoint ? 7 : 5,
        color: isEndpoint ? '#a78bfa' : '#64748b',
        fillColor: isEndpoint ? '#c4b5fd' : '#94a3b8',
        fillOpacity: 0.9,
        weight: 2,
      })
        .bindTooltip(w.label, { permanent: false, direction: 'top' })
        .addTo(this.map!);
    });

    if (snap.latitude != null && snap.longitude != null) {
      this.trailPoints.push([snap.latitude, snap.longitude]);
      if (this.trailPoints.length > 80) {
        this.trailPoints.shift();
      }
      if (!this.trailLayer) {
        this.trailLayer = L.polyline(this.trailPoints, {
          color: '#34d399',
          weight: 3,
          opacity: 0.85,
        }).addTo(this.map);
      } else {
        this.trailLayer.setLatLngs(this.trailPoints);
      }
    }

    this.map.fitBounds(this.routeLayer.getBounds(), { padding: [48, 48], maxZoom: 10 });
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
  }
}
