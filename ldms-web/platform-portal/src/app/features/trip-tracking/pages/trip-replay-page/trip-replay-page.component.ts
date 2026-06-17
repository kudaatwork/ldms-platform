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
import { Subject, forkJoin } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { NotificationService } from '../../../../core/services/notification.service';
import type { TripDetail, TripLiveSnapshot, TripTimelineEvent } from '../../models/trip-tracking.model';
import { TripLiveService } from '../../services/trip-live.service';
import { TripTrackingPortalService } from '../../services/trip-tracking-portal.service';
import {
  TRIP_REPLAY_SPEED_OPTIONS,
  buildTripReplayFrames,
  formatReplayClock,
  interpolateReplayFrame,
  replayIndexForMs,
  type TripReplayFrame,
  type TripReplaySpeed,
} from '../../utils/trip-replay.util';

@Component({
  selector: 'app-trip-replay-page',
  templateUrl: './trip-replay-page.component.html',
  styleUrl: './trip-replay-page.component.scss',
  standalone: false,
})
export class TripReplayPageComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('mapHost') mapHost?: ElementRef<HTMLDivElement>;

  tripId = 0;
  loading = true;
  loadError = '';
  detail: TripDetail | null = null;
  snapshot: TripLiveSnapshot | null = null;
  events: TripTimelineEvent[] = [];
  frames: TripReplayFrame[] = [];

  playing = false;
  speed: TripReplaySpeed = 1;
  readonly speedOptions = TRIP_REPLAY_SPEED_OPTIONS;
  cursorMs = 0;
  durationMs = 0;

  private map?: L.Map;
  private routeLayer?: L.Polyline;
  private trailLayer?: L.Polyline;
  private truckMarker?: L.Marker;
  private eventMarkers: L.CircleMarker[] = [];
  private animFrame = 0;
  private lastTick = 0;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly tripLive: TripLiveService,
    private readonly tripPortal: TripTrackingPortalService,
    private readonly notifications: NotificationService,
    private readonly title: Title,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.tripId = Number(this.route.snapshot.paramMap.get('tripId') ?? 0);
    if (!this.tripId) {
      this.loadError = 'Invalid trip id.';
      this.loading = false;
      return;
    }
    this.loadReplayData();
  }

  ngAfterViewInit(): void {
    requestAnimationFrame(() => this.ensureMap());
  }

  ngOnDestroy(): void {
    this.playing = false;
    cancelAnimationFrame(this.animFrame);
    this.destroy$.next();
    this.destroy$.complete();
    this.map?.remove();
  }

  @HostListener('window:resize')
  onResize(): void {
    this.map?.invalidateSize();
  }

  get routeLabel(): string {
    if (!this.snapshot) {
      return '—';
    }
    return `${this.snapshot.fromWarehouseName} → ${this.snapshot.toWarehouseName}`;
  }

  get cursorLabel(): string {
    const rel = this.cursorMs - (this.frames[0]?.atMs ?? 0);
    return formatReplayClock(Math.max(0, rel));
  }

  get durationLabel(): string {
    return formatReplayClock(this.durationMs);
  }

  get scrubPct(): number {
    return this.durationMs > 0 ? (this.cursorMs - (this.frames[0]?.atMs ?? 0)) / this.durationMs * 100 : 0;
  }

  get currentFrame(): TripReplayFrame | null {
    return interpolateReplayFrame(this.frames, this.cursorMs);
  }

  get activeEventIndex(): number {
    let idx = -1;
    for (let i = 0; i < this.events.length; i++) {
      const iso = this.events[i].recordedAtIso;
      if (!iso) {
        continue;
      }
      const ms = Date.parse(iso);
      if (!Number.isNaN(ms) && ms <= this.cursorMs) {
        idx = i;
      }
    }
    return idx;
  }

  goBack(): void {
    void this.router.navigate(['/shipments/history']);
  }

  togglePlay(): void {
    if (!this.frames.length) {
      return;
    }
    this.playing = !this.playing;
    if (this.playing) {
      this.lastTick = performance.now();
      this.tick();
    } else {
      cancelAnimationFrame(this.animFrame);
    }
  }

  rewind(): void {
    this.playing = false;
    cancelAnimationFrame(this.animFrame);
    this.seekToStart();
  }

  setSpeed(next: TripReplaySpeed): void {
    this.speed = next;
  }

  onScrubInput(value: string | number): void {
    const pct = Number(value);
    if (!this.frames.length || Number.isNaN(pct)) {
      return;
    }
    const start = this.frames[0].atMs;
    this.cursorMs = start + (this.durationMs * pct) / 100;
    this.applyFrame();
    this.cdr.markForCheck();
  }

  jumpToEvent(event: TripTimelineEvent): void {
    if (!event.recordedAtIso) {
      return;
    }
    const ms = Date.parse(event.recordedAtIso);
    if (Number.isNaN(ms)) {
      return;
    }
    this.cursorMs = ms;
    this.applyFrame();
    this.cdr.markForCheck();
  }

  private loadReplayData(): void {
    this.loading = true;
    forkJoin({
      detail: this.tripPortal.trackTrip(this.tripId),
      snapshot: this.tripLive.getLiveSnapshot(this.tripId),
    })
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: ({ detail, snapshot }) => {
          this.detail = detail;
          this.snapshot = snapshot;
          this.events = detail.timeline ?? [];
          this.frames = buildTripReplayFrames(snapshot, this.events);
          if (!this.frames.length) {
            this.loadError = 'No GPS trail recorded for this trip yet.';
            return;
          }
          const start = this.frames[0].atMs;
          const end = this.frames[this.frames.length - 1].atMs;
          this.durationMs = Math.max(end - start, 1000);
          this.cursorMs = start;
          this.title.setTitle(`${snapshot.tripNumber} replay | LX Platform`);
          this.renderMapLayers();
          this.applyFrame();
        },
        error: (err: Error) => {
          this.loadError = err.message || 'Could not load trip replay.';
        },
      });
  }

  private seekToStart(): void {
    if (!this.frames.length) {
      return;
    }
    this.cursorMs = this.frames[0].atMs;
    this.applyFrame();
    this.cdr.markForCheck();
  }

  private tick = (): void => {
    if (!this.playing || !this.frames.length) {
      return;
    }
    const now = performance.now();
    const delta = now - this.lastTick;
    this.lastTick = now;
    const end = this.frames[this.frames.length - 1].atMs;
    this.cursorMs += delta * this.speed;
    if (this.cursorMs >= end) {
      this.cursorMs = end;
      this.playing = false;
    }
    this.applyFrame();
    this.cdr.markForCheck();
    if (this.playing) {
      this.animFrame = requestAnimationFrame(this.tick);
    }
  };

  private applyFrame(): void {
    const frame = interpolateReplayFrame(this.frames, this.cursorMs);
    if (!frame || !this.map) {
      return;
    }
    this.updateTruck(frame);
    const idx = replayIndexForMs(this.frames, this.cursorMs);
    if (this.trailLayer && this.frames.length > 1) {
      const played = this.frames.slice(0, idx + 1).map((f) => [f.lat, f.lng] as L.LatLngExpression);
      this.trailLayer.setLatLngs(played);
    }
  }

  private ensureMap(): void {
    const host = this.mapHost?.nativeElement;
    if (!host || this.map) {
      return;
    }
    if (host.offsetWidth < 2) {
      requestAnimationFrame(() => this.ensureMap());
      return;
    }
    this.map = L.map(host, { zoomControl: false }).setView([-19, 30], 6);
    L.control.zoom({ position: 'topright' }).addTo(this.map);
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      attribution: '&copy; OpenStreetMap & Carto',
      maxZoom: 19,
    }).addTo(this.map);
    this.renderMapLayers();
    this.applyFrame();
  }

  private renderMapLayers(): void {
    if (!this.map || !this.snapshot) {
      return;
    }
    const routePts = (this.snapshot.routeWaypoints ?? [])
      .filter((p) => p.latitude && p.longitude)
      .map((p) => [p.latitude, p.longitude] as L.LatLngExpression);
    const trailPts = this.frames.map((f) => [f.lat, f.lng] as L.LatLngExpression);

    this.routeLayer?.remove();
    this.trailLayer?.remove();
    this.eventMarkers.forEach((m) => m.remove());
    this.eventMarkers = [];

    if (routePts.length > 1) {
      this.routeLayer = L.polyline(routePts, {
        color: '#64748b',
        weight: 3,
        opacity: 0.55,
        dashArray: '8 10',
      }).addTo(this.map);
    }

    if (trailPts.length > 1) {
      this.trailLayer = L.polyline([trailPts[0]], {
        color: '#38bdf8',
        weight: 5,
        opacity: 0.95,
      }).addTo(this.map);
      L.polyline(trailPts, { color: '#1e293b', weight: 7, opacity: 0.35 }).addTo(this.map);
    }

    for (const ev of this.events) {
      if (ev.latitude == null || ev.longitude == null) {
        continue;
      }
      const marker = L.circleMarker([ev.latitude, ev.longitude], {
        radius: 6,
        color: '#fbbf24',
        fillColor: '#f59e0b',
        fillOpacity: 0.9,
        weight: 2,
      }).bindTooltip(ev.eventTypeLabel, { permanent: false, direction: 'top' });
      marker.addTo(this.map);
      this.eventMarkers.push(marker);
    }

    const bounds = L.latLngBounds(trailPts.length ? trailPts : routePts);
    if (bounds.isValid()) {
      this.map.fitBounds(bounds.pad(0.15));
    }
  }

  private updateTruck(frame: TripReplayFrame): void {
    if (!this.map) {
      return;
    }
    const icon = L.divIcon({
      className: 'trp-truck-marker',
      html: `<div class="trp-truck-marker__inner" style="transform:rotate(${frame.headingDeg}deg)"><span>🚛</span></div>`,
      iconSize: [36, 36],
      iconAnchor: [18, 18],
    });
    if (!this.truckMarker) {
      this.truckMarker = L.marker([frame.lat, frame.lng], { icon, zIndexOffset: 1000 }).addTo(this.map);
    } else {
      this.truckMarker.setLatLng([frame.lat, frame.lng]);
      this.truckMarker.setIcon(icon);
    }
    this.map.panTo([frame.lat, frame.lng], { animate: true, duration: 0.25 });
  }
}
