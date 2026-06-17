import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import * as L from 'leaflet';
import type { LxMapLatLng, LxMapMarker, LxMapStyle, LxMapWaypoint } from './lx-leaflet-map.model';

@Component({
  selector: 'app-lx-leaflet-map',
  templateUrl: './lx-leaflet-map.component.html',
  styleUrl: './lx-leaflet-map.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class LxLeafletMapComponent implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild('mapHost') mapHost?: ElementRef<HTMLDivElement>;

  @Input() markers: LxMapMarker[] = [];
  @Input() routePoints: LxMapLatLng[] = [];
  @Input() trailPoints: LxMapLatLng[] = [];
  @Input() waypoints: LxMapWaypoint[] = [];
  @Input() loading = false;
  @Input() showControls = true;
  @Input() minHeight = '320px';
  @Input() emptyMessage = 'No live positions on the corridor yet.';
  @Input() initialMapStyle: LxMapStyle = 'standard';

  mapStyle: LxMapStyle = 'standard';
  mapScrollEnabled = false;

  private map?: L.Map;
  private baseTileLayer?: L.TileLayer;
  private routeLayer?: L.Polyline;
  private trailLayer?: L.Polyline;
  private markerLayer = new Map<string | number, L.Marker>();
  private waypointMarkers: L.Marker[] = [];
  private waypointSignature = '';
  private mapInitAttempts = 0;
  private boundsFitted = false;
  private mapResizeObserver?: ResizeObserver;

  ngAfterViewInit(): void {
    this.mapStyle = this.initialMapStyle;
    this.scheduleMapInit();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.map) {
      return;
    }
    if (changes['markers'] || changes['routePoints'] || changes['trailPoints'] || changes['waypoints']) {
      this.syncMapLayers();
    }
    if (changes['initialMapStyle'] && !changes['initialMapStyle'].firstChange) {
      this.mapStyle = this.initialMapStyle;
      this.applyMapStyle();
    }
  }

  ngOnDestroy(): void {
    this.mapResizeObserver?.disconnect();
    this.clearWaypointMarkers();
    this.markerLayer.forEach((marker) => marker.remove());
    this.markerLayer.clear();
    this.routeLayer?.remove();
    this.trailLayer?.remove();
    this.map?.remove();
    this.map = undefined;
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    this.refreshMapSize();
  }

  setMapStyle(style: LxMapStyle): void {
    this.mapStyle = style;
    this.applyMapStyle();
  }

  mapZoomIn(): void {
    this.map?.zoomIn();
  }

  mapZoomOut(): void {
    this.map?.zoomOut();
  }

  recenter(): void {
    this.fitToContent(true);
  }

  enableMapScroll(): void {
    if (!this.map || this.mapScrollEnabled) {
      return;
    }
    this.map.scrollWheelZoom.enable();
    this.mapScrollEnabled = true;
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
    this.syncMapLayers();
  }

  private initMap(host: HTMLElement): void {
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

    const panel = host.closest('.lx-map-panel');
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
    if (this.hasMapContent()) {
      this.fitToContent(false);
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

  private tileLayerForStyle(style: LxMapStyle): { url: string; options: L.TileLayerOptions } {
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

  private syncMapLayers(): void {
    if (!this.map) {
      return;
    }

    if (!this.hasMapContent()) {
      this.boundsFitted = false;
    }

    this.syncMarkers();
    this.syncRoute();
    this.syncTrail();
    this.syncWaypointMarkers();
    this.fitToContent(!this.boundsFitted);
  }

  private syncMarkers(): void {
    if (!this.map) {
      return;
    }

    const nextIds = new Set(this.markers.map((marker) => marker.id));
    for (const [id, marker] of this.markerLayer.entries()) {
      if (!nextIds.has(id)) {
        marker.remove();
        this.markerLayer.delete(id);
      }
    }

    for (const row of this.markers) {
      const icon = this.truckIcon(row);
      const existing = this.markerLayer.get(row.id);
      if (existing) {
        existing.setLatLng([row.lat, row.lng]);
        existing.setIcon(icon);
        if (row.label) {
          existing.setTooltipContent(row.label);
        }
        continue;
      }

      const marker = L.marker([row.lat, row.lng], { icon, zIndexOffset: 1000 })
        .bindTooltip(row.label ?? 'Vehicle', {
          permanent: false,
          direction: 'top',
          sticky: false,
          interactive: false,
        })
        .addTo(this.map);
      this.markerLayer.set(row.id, marker);
    }
  }

  private syncRoute(): void {
    if (!this.map) {
      return;
    }

    if (this.routePoints.length < 2) {
      this.routeLayer?.remove();
      this.routeLayer = undefined;
      return;
    }

    const latlngs: L.LatLngExpression[] = this.routePoints.map((p) => [p.lat, p.lng]);
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
  }

  private syncTrail(): void {
    if (!this.map) {
      return;
    }

    if (this.trailPoints.length < 2) {
      this.trailLayer?.remove();
      this.trailLayer = undefined;
      return;
    }

    const latlngs: L.LatLngExpression[] = this.trailPoints.map((p) => [p.lat, p.lng]);
    if (!this.trailLayer) {
      this.trailLayer = L.polyline(latlngs, {
        color: '#34d399',
        weight: 4,
        opacity: 0.9,
      }).addTo(this.map);
    } else {
      this.trailLayer.setLatLngs(latlngs);
    }
  }

  private syncWaypointMarkers(): void {
    if (!this.map || !this.waypoints.length) {
      this.clearWaypointMarkers();
      return;
    }

    const signature = this.waypoints
      .map((w) => `${w.lat},${w.lng},${w.type ?? ''},${w.label}`)
      .join('|');
    if (signature === this.waypointSignature) {
      return;
    }

    this.waypointSignature = signature;
    this.clearWaypointMarkers();
    this.waypointMarkers = this.waypoints.map((w) =>
      L.marker([w.lat, w.lng], { icon: this.waypointIcon(w), zIndexOffset: 200 })
        .bindTooltip(w.label, {
          permanent: false,
          direction: 'top',
          sticky: false,
          interactive: false,
        })
        .addTo(this.map!),
    );
  }

  private clearWaypointMarkers(): void {
    this.waypointMarkers.forEach((m) => m.remove());
    this.waypointMarkers = [];
    this.waypointSignature = '';
  }

  private hasMapContent(): boolean {
    return this.markers.length > 0 || this.routePoints.length > 1 || this.trailPoints.length > 1;
  }

  private fitToContent(animate: boolean): void {
    if (!this.map) {
      return;
    }

    const points: L.LatLngExpression[] = [
      ...this.markers.map((m) => [m.lat, m.lng] as L.LatLngExpression),
      ...this.routePoints.map((p) => [p.lat, p.lng] as L.LatLngExpression),
      ...this.trailPoints.map((p) => [p.lat, p.lng] as L.LatLngExpression),
    ];

    if (!points.length) {
      return;
    }

    if (points.length === 1) {
      const only = points[0] as [number, number];
      this.map.setView(only, 12, { animate });
      this.boundsFitted = true;
      return;
    }

    const bounds = L.latLngBounds(points);
    this.map.fitBounds(bounds, {
      padding: [48, 48],
      maxZoom: 11,
      animate,
      duration: animate ? 0.75 : 0,
    });
    this.boundsFitted = true;
  }

  private truckIcon(marker: LxMapMarker): L.DivIcon {
    const heading = marker.headingDeg ?? 0;
    const tone = marker.tone ?? 'primary';
    return L.divIcon({
      className: `lx-map-truck lx-map-truck--${tone}`,
      html: `<div class="lx-map-truck__body" style="transform: rotate(${heading}deg)">
        <span class="lx-map-truck__cab"></span>
        <span class="lx-map-truck__trailer"></span>
        <span class="lx-map-truck__pulse"></span>
      </div>`,
      iconSize: [36, 36],
      iconAnchor: [18, 18],
    });
  }

  private waypointIcon(waypoint: LxMapWaypoint): L.DivIcon {
    const type = (waypoint.type ?? 'CHECKPOINT').toUpperCase();
    const tone =
      type === 'ORIGIN' || type === 'PICKUP'
        ? 'origin'
        : type === 'DESTINATION' || type === 'DELIVERY'
          ? 'destination'
          : type === 'BORDER'
            ? 'border'
            : 'checkpoint';
    return L.divIcon({
      className: `lx-map-waypoint lx-map-waypoint--${tone}`,
      html: `<span class="lx-map-waypoint__dot"></span>`,
      iconSize: [14, 14],
      iconAnchor: [7, 7],
    });
  }
}
