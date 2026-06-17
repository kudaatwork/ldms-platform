import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { Subject, forkJoin, of } from 'rxjs';
import { catchError, finalize, map, switchMap, takeUntil } from 'rxjs/operators';
import { buildJourneyProgressView } from '../../../trip-tracking/utils/journey-progress.util';
import { buildJourneyTimeView, journeyPhaseIcon } from '../../../trip-tracking/utils/journey-timing.util';
import type { TripLiveSnapshot, TripRow } from '../../../trip-tracking/models/trip-tracking.model';
import { TripLiveService } from '../../../trip-tracking/services/trip-live.service';
import { TripTrackingPortalService } from '../../../trip-tracking/services/trip-tracking-portal.service';

export interface TripJourneyReportRow {
  trip: TripRow;
  snapshot: TripLiveSnapshot | null;
  completionLabel: string;
  progressPct: number;
  totalLabel: string;
  transitLabel: string;
  waitingLabel: string;
  idleLabel: string;
  phaseLabel: string;
  phaseIcon: string;
  currentLegLabel: string;
}

@Component({
  selector: 'app-trip-journey-report-page',
  templateUrl: './trip-journey-report-page.component.html',
  styleUrl: './trip-journey-report-page.component.scss',
  standalone: false,
})
export class TripJourneyReportPageComponent implements OnInit, OnDestroy {
  loading = false;
  error = '';
  rows: TripJourneyReportRow[] = [];
  tableSearch = '';

  readonly displayedColumns = [
    'trip',
    'route',
    'progress',
    'checkpoints',
    'journeyTime',
    'transit',
    'waiting',
    'phase',
    'actions',
  ];

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly tripPortal: TripTrackingPortalService,
    private readonly tripLive: TripLiveService,
    private readonly router: Router,
    private readonly title: Title,
    private readonly cdr: ChangeDetectorRef,
  ) {
    this.title.setTitle('Trip journeys | LX Platform');
  }

  ngOnInit(): void {
    this.loadReport();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get filteredRows(): TripJourneyReportRow[] {
    const q = this.tableSearch.trim().toLowerCase();
    if (!q) {
      return this.rows;
    }
    return this.rows.filter((row) => {
      const haystack = `${row.trip.tripNumber} ${row.trip.route} ${row.trip.driverName} ${row.trip.vehicleRegistration} ${row.phaseLabel}`.toLowerCase();
      return haystack.includes(q);
    });
  }

  get activeTripCount(): number {
    return this.rows.length;
  }

  get avgProgressPct(): number {
    if (!this.rows.length) {
      return 0;
    }
    const sum = this.rows.reduce((acc, row) => acc + row.progressPct, 0);
    return Math.round(sum / this.rows.length);
  }

  get phaseBreakdown(): { label: string; count: number; pct: number; color: string }[] {
    const counts = new Map<string, number>();
    for (const row of this.rows) {
      const key = row.phaseLabel || 'Unknown';
      counts.set(key, (counts.get(key) ?? 0) + 1);
    }
    const total = this.rows.length || 1;
    const palette = ['#6366f1', '#0ea5e9', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];
    return [...counts.entries()]
      .sort((a, b) => b[1] - a[1])
      .map(([label, count], index) => ({
        label,
        count,
        pct: Math.round((count / total) * 100),
        color: palette[index % palette.length],
      }));
  }

  get progressDistribution(): { label: string; count: number; pct: number }[] {
    const buckets = [
      { label: '0–25%', min: 0, max: 25, count: 0 },
      { label: '26–50%', min: 26, max: 50, count: 0 },
      { label: '51–75%', min: 51, max: 75, count: 0 },
      { label: '76–99%', min: 76, max: 99, count: 0 },
      { label: 'Complete', min: 100, max: 100, count: 0 },
    ];
    for (const row of this.rows) {
      const pct = row.progressPct;
      const bucket = buckets.find((b) => pct >= b.min && pct <= b.max);
      if (bucket) {
        bucket.count += 1;
      }
    }
    const total = this.rows.length || 1;
    return buckets.map((b) => ({
      label: b.label,
      count: b.count,
      pct: Math.round((b.count / total) * 100),
    }));
  }

  get phaseDonutGradient(): string {
    if (!this.phaseBreakdown.length) {
      return 'conic-gradient(#e5e7eb 0 100%)';
    }
    let cursor = 0;
    const stops = this.phaseBreakdown.flatMap((item) => {
      const start = cursor;
      cursor += item.pct;
      return [`${item.color} ${start}% ${cursor}%`];
    });
    return `conic-gradient(${stops.join(', ')})`;
  }

  loadReport(): void {
    this.loading = true;
    this.error = '';
    this.tripPortal
      .findTrips({ status: '' })
      .pipe(
        map((trips) =>
          trips.filter((t) =>
            ['IN_TRANSIT', 'IN_PROGRESS', 'ROADSIDE_HOLD', 'AT_BORDER_HOLD', 'ARRIVED', 'DELIVERED'].includes(t.status),
          ),
        ),
        switchMap((trips) => {
          if (!trips.length) {
            return of([] as TripJourneyReportRow[]);
          }
          return forkJoin(
            trips.slice(0, 80).map((trip) =>
              this.tripLive.getLiveSnapshot(trip.id).pipe(
                catchError(() => of(null)),
                map((snapshot) => this.toReportRow(trip, snapshot)),
              ),
            ),
          );
        }),
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (rows) => {
          this.rows = rows.sort((a, b) => b.progressPct - a.progressPct);
        },
        error: () => {
          this.error = 'Could not load trip journey report.';
        },
      });
  }

  openReplay(row: TripJourneyReportRow): void {
    void this.router.navigate(['/shipments/replay', row.trip.id]);
  }

  openHistory(): void {
    void this.router.navigate(['/shipments/history']);
  }

  openLive(row: TripJourneyReportRow): void {
    void this.router.navigate(['/shipments/live', row.trip.id]);
  }

  private toReportRow(trip: TripRow, snapshot: TripLiveSnapshot | null): TripJourneyReportRow {
    const journey = buildJourneyTimeView(snapshot, { startedAtLabel: trip.startedAtLabel });
    const progress = buildJourneyProgressView(snapshot);
    return {
      trip,
      snapshot,
      completionLabel: progress.completionLabel,
      progressPct: snapshot?.overallProgressPct ?? 0,
      totalLabel: journey.totalLabel,
      transitLabel: journey.transitLabel,
      waitingLabel: journey.waitingLabel,
      idleLabel: journey.idleLabel,
      phaseLabel: journey.phaseLabel,
      phaseIcon: journeyPhaseIcon(journey.phase),
      currentLegLabel: progress.currentLegLabel,
    };
  }
}
