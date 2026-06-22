import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { Router } from '@angular/router';
import * as L from 'leaflet';
import { Subject, catchError, forkJoin, interval, map, of, startWith, switchMap, takeUntil, timeout } from 'rxjs';
import type { TripLiveMapTrack, TripLiveSnapshot } from '../../models/trip-tracking.model';
import { TripLiveService } from '../../services/trip-live.service';
import type { LiveMapStyle } from '../../pages/live-trip-tracking/live-trip-tracking.component';

const TRACK_COLORS = ['#38bdf8', '#34d399', '#fbbf24', '#a78bfa', '#fb7185', '#2dd4bf', '#60a5fa', '#f472b6'];

@Component({
  selector: 'app-trip-live-map-panel',
  templateUrl: './trip-live-map-panel.component.html',
  styleUrl: './trip-live-map-panel.component.scss',
  standalone: false,
})
export class TripLiveMapPanelComponent implements OnChanges, AfterViewInit, OnDestroy {
  @ViewChild('mapHost') mapHost?: ElementRef<HTMLDivElement>;

  /** Single-trip mode (legacy). Ignored when {@link tracks} is non-empty. */
  @Input() tripId: number | null = null;
  @Input() shipmentNumber = '';
  @Input() statusLabel = '';
  @Input() routeLabel = '';
  /** Organisation fleet — all active live loads on one map. */
  @Input() tracks: TripLiveMapTrack[] = [];
  @Input() highlightTripId: number | null = null;
  @Input() compact = true;

  loading = false;
  liveLoadFailed = false;
  tripSnapshot: TripLiveSnapshot | null = null;
  mapStyle: LiveMapStyle = 'live';
  followTruck = false;
  mapScrollEnabled = false;

  private map?: L.Map;
  private baseTileLayer?: L.TileLayer;
  private routeLayer?: L.Polyline;
  private trailLayer?: L.Polyline;
  private truckMarker?: L.Marker;
  private waypointMarkers: L.Marker[] = [];
  private readonly trailPoints: L.LatLngExpression[] = [];
  private readonly trackSnapshots = new Map<number, TripLiveSnapshot>();
  private readonly routeLayers = new Map<number, L.Polyline>();
  private readonly truckMarkers = new Map<number, L.Marker>();
  private readonly destroy$ = new Subject<void>();
  private readonly pollStop$ = new Subject<void>();
  private displayLat = 0;
  private displayLng = 0;
  private mapInitAttempts = 0;
  private routeFitted = false;
  private fleetBoundsFitted = false;
  private waypointSignature = '';
  private readonly recenterZoom = 15;
  private mapResizeObserver?: ResizeObserver;
  private viewReady = false;
  private lastTrackSignature = '';
  /** User-selected load on the fleet map (does not navigate away). */
  private fleetFocusTripId: number | null = null;

  constructor(
    private readonly tripLive: TripLiveService,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  get isFleetMode(): boolean {
    return this.tracks.length > 0;
  }

  get hasLiveTargets(): boolean {
    return this.isFleetMode || (this.tripId != null && this.tripId > 0);
  }

  get activeSnapshot(): TripLiveSnapshot | null {
    if (this.isFleetMode) {
      const focusId = this.focusTripId;
      return focusId ? this.trackSnapshots.get(focusId) ?? null : null;
    }
    return this.tripSnapshot;
  }

  get focusTripId(): number | null {
    if (this.isFleetMode) {
      if (this.fleetFocusTripId && this.tracks.some((t) => t.tripId === this.fleetFocusTripId)) {
        return this.fleetFocusTripId;
      }
      if (this.highlightTripId && this.tracks.some((t) => t.tripId === this.highlightTripId)) {
        return this.highlightTripId;
      }
      return this.tracks[0]?.tripId ?? null;
    }
    return this.tripId;
  }

  get fleetSummaryLabel(): string {
    const loaded = this.trackSnapshots.size;
    const total = this.tracks.length;
    if (!total) {
      return 'No active live loads';
    }
    if (loaded === 0) {
      return `Syncing ${total} load${total === 1 ? '' : 's'}…`;
    }
    return `${loaded} of ${total} load${total === 1 ? '' : 's'} on map`;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['tracks'] || changes['tripId']) {
      const signature = this.trackSignature();
      if (signature === this.lastTrackSignature) {
        if (changes['highlightTripId'] && this.isFleetMode && this.map) {
          this.renderFleet(false);
          this.cdr.markForCheck();
        }
        return;
      }
      this.lastTrackSignature = signature;
      this.fleetFocusTripId = null;
      this.pollStop$.next();
      this.resetMapState();
      if (this.isFleetMode) {
        this.startFleetPolling();
      } else if (this.tripId && this.tripId > 0) {
        this.startPolling();
      } else {
        this.tripSnapshot = null;
        this.loading = false;
        this.liveLoadFailed = false;
      }
      this.cdr.markForCheck();
    }

    if (changes['highlightTripId'] && !changes['tracks'] && !changes['tripId'] && this.isFleetMode && this.map) {
      this.renderFleet(false);
      this.cdr.markForCheck();
    }
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.ensureMap();
  }

  ngOnDestroy(): void {
    this.pollStop$.next();
    this.destroy$.next();
    this.destroy$.complete();
    this.mapResizeObserver?.disconnect();
    this.map?.remove();
    this.map = undefined;
  }

  trackColor(index: number): string {
    return TRACK_COLORS[index % TRACK_COLORS.length];
  }

  isHighlightedTrack(tripId: number): boolean {
    return this.focusTripId === tripId;
  }

  setMapStyle(style: LiveMapStyle): void {
    this.mapStyle = style;
    this.applyMapStyle();
  }

  toggleFollowTruck(): void {
    this.followTruck = !this.followTruck;
    if (this.followTruck) {
      this.recenterOnTruck();
    }
  }

  recenterOnTruck(): void {
    if (!this.map) {
      return;
    }
    if (this.isFleetMode) {
      this.fitFleetBounds(true);
      return;
    }
    if (this.displayLat && this.displayLng) {
      this.map.setView([this.displayLat, this.displayLng], this.recenterZoom, { animate: true });
      return;
    }
    if (this.routeLayer) {
      try {
        this.map.fitBounds(this.routeLayer.getBounds(), { padding: [40, 40], maxZoom: 11 });
      } catch {
        /* ignore */
      }
    }
  }

  mapZoomIn(): void {
    this.map?.zoomIn();
  }

  mapZoomOut(): void {
    this.map?.zoomOut();
  }

  enableMapScroll(): void {
    if (!this.map) {
      return;
    }
    this.map.scrollWheelZoom.enable();
    this.mapScrollEnabled = true;
  }

  get resolvedRouteLabel(): string {
    if (this.isFleetMode) {
      const focus = this.tracks.find((t) => t.tripId === this.focusTripId);
      return focus?.routeLabel ?? '';
    }
    if (this.routeLabel) {
      return this.routeLabel;
    }
    if (!this.tripSnapshot) {
      return '';
    }
    return `${this.tripSnapshot.fromWarehouseName ?? 'Origin'} → ${this.tripSnapshot.toWarehouseName ?? 'Destination'}`;
  }

  selectTrack(tripId: number): void {
    if (!this.isFleetMode) {
      this.openFullLiveView(tripId);
      return;
    }
    this.fleetFocusTripId = tripId;
    if (this.map) {
      this.renderFleet(false);
    }
    this.cdr.markForCheck();
  }

  openFullLiveView(tripId?: number): void {
    if (this.isFleetMode) {
      void this.router.navigate(['/shipments/trips']);
      return;
    }
    const target = tripId ?? this.focusTripId;
    if (!target) {
      return;
    }
    void this.router.navigate(['/shipments/live', target]);
  }

  private trackSignature(): string {
    if (this.isFleetMode) {
      return this.tracks.map((t) => t.tripId).sort((a, b) => a - b).join(',');
    }
    return String(this.tripId ?? '');
  }

  private startPolling(): void {
    this.pollStop$.next();
    if (!this.tripId) {
      return;
    }
    this.loading = true;
    this.liveLoadFailed = false;
    this.ensureMap();

    interval(8000)
      .pipe(
        startWith(0),
        switchMap(() =>
          this.tripLive.getLiveSnapshot(this.tripId!).pipe(
            timeout(8000),
            catchError(() => of(null)),
          ),
        ),
        takeUntil(this.pollStop$),
        takeUntil(this.destroy$),
      )
      .subscribe((snap) => {
        if (snap) {
          this.tripSnapshot = snap;
          this.liveLoadFailed = false;
          this.ensureMap();
          this.renderRoute(snap);
          if (snap.latitude != null && snap.longitude != null) {
            this.displayLat = snap.latitude;
            this.displayLng = snap.longitude;
            this.updateSingleMarker(snap.latitude, snap.longitude, snap.headingDeg, TRACK_COLORS[0]);
          }
        } else if (!this.tripSnapshot) {
          this.liveLoadFailed = true;
        }
        this.loading = false;
        this.scheduleMapResizeAfterOverlay();
        this.cdr.markForCheck();
      });
  }

  private startFleetPolling(): void {
    this.pollStop$.next();
    const ids = this.tracks.map((t) => t.tripId).filter((id) => id > 0);
    if (!ids.length) {
      this.loading = false;
      return;
    }
    this.loading = true;
    this.liveLoadFailed = false;
    this.ensureMap();

    interval(8000)
      .pipe(
        startWith(0),
        switchMap(() =>
          forkJoin(
            ids.map((id) =>
              this.tripLive.getLiveSnapshot(id).pipe(
                timeout(8000),
                catchError(() => of(null)),
                map((snap) => ({ id, snap })),
              ),
            ),
          ),
        ),
        takeUntil(this.pollStop$),
        takeUntil(this.destroy$),
      )
      .subscribe((results) => {
        let loaded = 0;
        for (const { id, snap } of results) {
          if (snap) {
            this.trackSnapshots.set(id, snap);
            loaded++;
          }
        }
        if (loaded === 0 && this.trackSnapshots.size === 0) {
          this.liveLoadFailed = true;
        } else {
          this.liveLoadFailed = false;
        }
        this.ensureMap();
        this.renderFleet(!this.fleetBoundsFitted);
        this.loading = false;
        this.scheduleMapResizeAfterOverlay();
        this.cdr.markForCheck();
      });
  }

  private resetMapState(): void {
    this.tripSnapshot = null;
    this.liveLoadFailed = false;
    this.trailPoints.length = 0;
    this.routeFitted = false;
    this.fleetBoundsFitted = false;
    this.waypointSignature = '';
    this.waypointMarkers.forEach((m) => m.remove());
    this.waypointMarkers = [];
    this.routeLayer?.remove();
    this.routeLayer = undefined;
    this.trailLayer?.remove();
    this.trailLayer = undefined;
    this.truckMarker?.remove();
    this.truckMarker = undefined;
    this.routeLayers.forEach((layer) => layer.remove());
    this.routeLayers.clear();
    this.truckMarkers.forEach((marker) => marker.remove());
    this.truckMarkers.clear();
    this.trackSnapshots.clear();
    this.displayLat = 0;
    this.displayLng = 0;
  }

  private ensureMap(): void {
    if (!this.viewReady) {
      return;
    }
    const host = this.mapHost?.nativeElement;
    if (!host) {
      return;
    }
    if (this.map) {
      requestAnimationFrame(() => this.refreshMapSize());
      return;
    }
    if (host.offsetWidth < 2 || host.offsetHeight < 2) {
      if (this.mapInitAttempts++ < 24) {
        requestAnimationFrame(() => this.ensureMap());
      }
      return;
    }
    this.initMap(host);
    if (!this.isFleetMode && this.tripSnapshot) {
      this.renderRoute(this.tripSnapshot);
    }
    if (this.isFleetMode && this.trackSnapshots.size) {
      this.renderFleet(!this.fleetBoundsFitted);
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
    host.addEventListener('click', () => this.enableMapScroll());
    this.map.on('mouseout', () => {
      this.map?.scrollWheelZoom.disable();
      this.mapScrollEnabled = false;
    });

    const panel = host.closest('.lt-map-panel');
    if (panel && typeof ResizeObserver !== 'undefined') {
      this.mapResizeObserver?.disconnect();
      this.mapResizeObserver = new ResizeObserver(() => this.refreshMapSize());
      this.mapResizeObserver.observe(panel);
    }

    requestAnimationFrame(() => this.refreshMapSize());
  }

  private refreshMapSize(): void {
    if (!this.map) {
      return;
    }
    this.map.invalidateSize({ animate: false });
    if (this.isFleetMode) {
      this.renderFleet(false);
    } else if (this.tripSnapshot) {
      this.renderRoute(this.tripSnapshot);
    }
    // Dashboard embeds the map after layout paint — second pass avoids soft tiles.
    window.setTimeout(() => {
      if (!this.map) {
        return;
      }
      this.map.invalidateSize({ animate: false });
    }, 120);
  }

  /** Re-measure once the loading blur overlay is removed from the DOM. */
  private scheduleMapResizeAfterOverlay(): void {
    requestAnimationFrame(() => this.refreshMapSize());
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
    const retina = typeof window !== 'undefined' && window.devicePixelRatio > 1;
    const retinaOptions: L.TileLayerOptions = { detectRetina: true };
    switch (style) {
      case 'standard':
        return {
          url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
          options: { attribution: '&copy; OpenStreetMap contributors', maxZoom: 19, ...retinaOptions },
        };
      case 'satellite':
        return {
          url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
          options: { attribution: '&copy; Esri', maxZoom: 19, ...retinaOptions },
        };
      default:
        return {
          url: retina
            ? 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}@2x.png'
            : 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
          options: {
            attribution: '&copy; OpenStreetMap &copy; CARTO',
            subdomains: 'abcd',
            maxZoom: 19,
            ...retinaOptions,
          },
        };
    }
  }

  private renderFleet(fitBounds: boolean): void {
    if (!this.map) {
      return;
    }

    const activeIds = new Set(this.tracks.map((t) => t.tripId));

    for (const [tripId, layer] of this.routeLayers.entries()) {
      if (!activeIds.has(tripId)) {
        layer.remove();
        this.routeLayers.delete(tripId);
      }
    }
    for (const [tripId, marker] of this.truckMarkers.entries()) {
      if (!activeIds.has(tripId)) {
        marker.remove();
        this.truckMarkers.delete(tripId);
      }
    }

    this.tracks.forEach((track, index) => {
      const snap = this.trackSnapshots.get(track.tripId);
      if (!snap?.routeWaypoints?.length) {
        return;
      }
      const color = this.trackColor(index);
      const highlighted = this.isHighlightedTrack(track.tripId);
      const latlngs: L.LatLngExpression[] = snap.routeWaypoints.map((w) => [w.latitude, w.longitude]);
      let routeLayer = this.routeLayers.get(track.tripId);
      if (!routeLayer) {
        routeLayer = L.polyline(latlngs, {
          color,
          weight: highlighted ? 5 : 3,
          opacity: highlighted ? 0.72 : 0.38,
          dashArray: highlighted ? undefined : '8 12',
        }).addTo(this.map!);
        this.routeLayers.set(track.tripId, routeLayer);
      } else {
        routeLayer.setStyle({
          color,
          weight: highlighted ? 5 : 3,
          opacity: highlighted ? 0.72 : 0.38,
          dashArray: highlighted ? undefined : '8 12',
        });
        routeLayer.setLatLngs(latlngs);
      }

      if (snap.latitude != null && snap.longitude != null) {
        this.updateFleetMarker(track.tripId, snap.latitude, snap.longitude, snap.headingDeg, color, highlighted);
      }
    });

    if (fitBounds) {
      this.fitFleetBounds(false);
    } else if (this.followTruck && this.focusTripId) {
      const focus = this.trackSnapshots.get(this.focusTripId);
      if (focus?.latitude != null && focus?.longitude != null) {
        this.map.panTo([focus.latitude, focus.longitude], { animate: false });
      }
    }
  }

  private fitFleetBounds(animate: boolean): void {
    if (!this.map) {
      return;
    }
    const group = L.featureGroup([
      ...Array.from(this.routeLayers.values()),
      ...Array.from(this.truckMarkers.values()),
    ]);
    if (!group.getLayers().length) {
      return;
    }
    try {
      this.map.fitBounds(group.getBounds(), { padding: [48, 48], maxZoom: 11, animate });
      this.fleetBoundsFitted = true;
    } catch {
      /* ignore */
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

    const activeTrail =
      serverTrail.length > 1
        ? serverTrail.map((p) => [p.latitude, p.longitude] as L.LatLngExpression)
        : this.trailPoints;
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
        this.map.fitBounds(this.routeLayer.getBounds(), { padding: [40, 40], maxZoom: 10 });
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

  private updateSingleMarker(lat: number, lng: number, headingDeg: number, color: string): void {
    if (!this.map) {
      return;
    }
    const icon = this.truckIcon(headingDeg, color, false);
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

  private updateFleetMarker(
    tripId: number,
    lat: number,
    lng: number,
    headingDeg: number,
    color: string,
    highlighted: boolean,
  ): void {
    if (!this.map) {
      return;
    }
    const icon = this.truckIcon(headingDeg, color, highlighted);
    let marker = this.truckMarkers.get(tripId);
    if (!marker) {
      marker = L.marker([lat, lng], {
        icon,
        zIndexOffset: highlighted ? 1200 : 1000,
      })
        .bindTooltip(this.tracks.find((t) => t.tripId === tripId)?.shipmentNumber ?? `Trip #${tripId}`, {
          direction: 'top',
          offset: [0, -12],
        })
        .addTo(this.map);
      this.truckMarkers.set(tripId, marker);
    } else {
      marker.setLatLng([lat, lng]);
      marker.setIcon(icon);
      marker.setZIndexOffset(highlighted ? 1200 : 1000);
    }
  }

  private truckIcon(headingDeg: number, color: string, highlighted: boolean): L.DivIcon {
    return L.divIcon({
      className: `lt-map-truck${highlighted ? ' lt-map-truck--focus' : ''}`,
      html: `<div class="lt-map-truck__body" style="transform: rotate(${headingDeg}deg)">
        <span class="lt-map-truck__cab" style="background:${color}"></span>
        <span class="lt-map-truck__trailer"></span>
        <span class="lt-map-truck__pulse" style="border-color:${color}88"></span>
      </div>`,
      iconSize: [36, 36],
      iconAnchor: [18, 18],
    });
  }
}
