import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { Subject, catchError, finalize, of, switchMap, takeUntil, timer } from 'rxjs';
import type { LxMapLatLng, LxMapMarker, LxMapWaypoint } from '@shared/components/lx-leaflet-map/lx-leaflet-map.model';
import type { ShipmentLiveView, TripLiveSnapshot } from '../../models/shipment-live.model';
import { ShipmentLiveAdminService } from '../../services/shipment-live-admin.service';
import {
  buildJourneyTimeView,
  fuelTone,
  statusLabel,
  type JourneyTimeView,
} from '../../utils/journey-timing.util';

@Component({
  selector: 'app-shipment-live-page',
  templateUrl: './shipment-live-page.component.html',
  styleUrl: './shipment-live-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ShipmentLivePageComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();

  loading = true;
  loadError = '';
  organizationId = 0;
  shipmentId = 0;
  view: ShipmentLiveView | null = null;
  journeyView: JourneyTimeView = buildJourneyTimeView(null);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly shipmentLive: ShipmentLiveAdminService,
    private readonly cdr: ChangeDetectorRef,
    private readonly title: Title,
  ) {}

  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        takeUntil(this.destroy$),
        switchMap((params) => {
          this.organizationId = Number(params.get('orgId'));
          this.shipmentId = Number(params.get('shipmentId'));
          this.loading = true;
          this.loadError = '';
          return this.shipmentLive.fetchShipmentLive(this.organizationId, this.shipmentId).pipe(
            catchError((err: Error) => {
              this.loadError = err.message || 'Unable to load live corridor data.';
              return of(null);
            }),
            finalize(() => {
              this.loading = false;
              this.cdr.markForCheck();
            }),
          );
        }),
      )
      .subscribe((row) => {
        this.applyView(row);
      });

    timer(2500, 2500)
      .pipe(
        takeUntil(this.destroy$),
        switchMap(() =>
          this.shipmentLive.fetchShipmentLive(this.organizationId, this.shipmentId).pipe(
            catchError(() => of(null)),
          ),
        ),
      )
      .subscribe((row) => {
        if (row) {
          this.applyView(row);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get snap(): TripLiveSnapshot | null {
    return this.view?.snapshot ?? null;
  }

  get shipmentRef(): string {
    return this.snap?.shipmentNumber || this.snap?.tripNumber || 'Shipment';
  }

  get routeLabel(): string {
    const s = this.snap;
    if (!s) {
      return '—';
    }
    return `${s.fromWarehouseName} → ${s.toWarehouseName}`;
  }

  get cargoLabel(): string {
    const s = this.snap;
    if (!s) {
      return '—';
    }
    const parts = [s.productName, s.quantity != null ? `${s.quantity}` : ''].filter(Boolean);
    return parts.length ? parts.join(' · ') : '—';
  }

  get liveMapMarkers(): LxMapMarker[] {
    const s = this.snap;
    if (!s || s.latitude == null || s.longitude == null) {
      return [];
    }
    return [
      {
        id: s.tripId,
        lat: s.latitude,
        lng: s.longitude,
        label: s.vehicleRegistration || s.tripNumber,
        headingDeg: s.headingDeg,
        tone: s.onBreak ? 'warning' : s.moving ? 'primary' : 'secondary',
      },
    ];
  }

  get routePoints(): LxMapLatLng[] {
    return (this.snap?.routeWaypoints ?? [])
      .filter((w) => Number.isFinite(w.latitude) && Number.isFinite(w.longitude))
      .map((w) => ({ lat: w.latitude, lng: w.longitude }));
  }

  get trailPoints(): LxMapLatLng[] {
    const trail = this.snap?.trail ?? [];
    return trail
      .filter((w) => Number.isFinite(w.latitude) && Number.isFinite(w.longitude))
      .map((w) => ({ lat: w.latitude, lng: w.longitude }));
  }

  get mapWaypoints(): LxMapWaypoint[] {
    return (this.snap?.routeWaypoints ?? [])
      .filter((w) => Number.isFinite(w.latitude) && Number.isFinite(w.longitude))
      .map((w) => ({
        lat: w.latitude,
        lng: w.longitude,
        label: w.label,
        type: w.type,
      }));
  }

  get fuelLevelLabel(): string {
    const pct = this.snap?.fuelLevelPct;
    return pct != null ? `${Math.round(pct)}%` : '—';
  }

  get fuelLitersLabel(): string {
    const liters = this.snap?.fuelRemainingLiters;
    return liters != null ? `${liters.toFixed(1)} L` : '—';
  }

  get distanceLabel(): string {
    const km = this.snap?.distanceTravelledKm;
    return km != null ? `${km.toFixed(1)} km` : '—';
  }

  get speedLabel(): string {
    const speed = this.snap?.speedKmh ?? 0;
    return `${Math.round(speed)}`;
  }

  get progressLabel(): string {
    return `${Math.round(this.snap?.overallProgressPct ?? 0)}`;
  }

  get statusText(): string {
    return statusLabel(this.snap?.status ?? '');
  }

  get fuelStatus(): 'ok' | 'warn' | 'critical' | 'unknown' {
    return fuelTone(this.snap?.fuelLevelPct);
  }

  get telematicsLabel(): string {
    const s = this.snap;
    if (!s) {
      return 'No signal';
    }
    if (s.simulationActive) {
      return s.simulationPaused || s.onBreak ? 'Simulation halted' : 'IoT simulation';
    }
    if (s.latitude != null && s.longitude != null && s.moving) {
      return 'Live telematics';
    }
    return 'No telematics signal';
  }

  private applyView(row: ShipmentLiveView | null): void {
    this.view = row;
    if (row) {
      this.journeyView = buildJourneyTimeView(row.snapshot);
      const ref = row.snapshot.shipmentNumber || row.snapshot.tripNumber;
      this.title.setTitle(`${ref} live | LX Admin`);
      this.loadError = '';
    } else if (!this.loading) {
      this.title.setTitle('Shipment live | LX Admin');
    }
    this.cdr.markForCheck();
  }
}
