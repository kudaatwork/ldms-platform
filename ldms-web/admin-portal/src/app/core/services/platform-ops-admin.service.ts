import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, catchError, map, of, switchMap, tap, timer } from 'rxjs';
import { OrganizationsAdminService } from '../../features/organizations/services/organizations-admin.service';
import {
  PlatformOpsSummary,
  PlatformRevenueReport,
  PlatformShipmentOps,
  buildPlatformOpsSummary,
  buildPlatformRevenueReport,
  findShipmentById,
  shipmentsForCompany,
} from './platform-ops-mock.data';

@Injectable({ providedIn: 'root' })
export class PlatformOpsAdminService {
  private readonly summarySubject = new BehaviorSubject<PlatformOpsSummary | null>(null);
  readonly summary$ = this.summarySubject.asObservable();

  constructor(private readonly organizations: OrganizationsAdminService) {}

  refresh(): Observable<PlatformOpsSummary> {
    return this.organizations.fetchOrganizationsForSelect().pipe(
      map((orgs) => buildPlatformOpsSummary(orgs.slice(0, 8))),
      catchError(() => of(buildPlatformOpsSummary())),
      tap((summary) => this.summarySubject.next(summary)),
    );
  }

  /** Poll-friendly snapshot for dashboard live widgets. */
  liveSummary(): Observable<PlatformOpsSummary> {
    const current = this.summarySubject.value;
    if (current) {
      return of(this.nudgeLivePositions(current));
    }
    return this.refresh().pipe(map((s) => this.nudgeLivePositions(s)));
  }

  getSummarySnapshot(): PlatformOpsSummary | null {
    return this.summarySubject.value;
  }

  fetchCompanyShipments(organizationId: number): Observable<PlatformShipmentOps[]> {
    return this.ensureSummary().pipe(map((s) => shipmentsForCompany(s, organizationId)));
  }

  fetchShipmentLive(shipmentId: number): Observable<PlatformShipmentOps | null> {
    return this.ensureSummary().pipe(
      map((s) => {
        const row = findShipmentById(s, shipmentId);
        return row ? this.nudgeShipment(row) : null;
      }),
    );
  }

  fetchRevenueReport(): Observable<PlatformRevenueReport> {
    return this.ensureSummary().pipe(map((s) => buildPlatformRevenueReport(s.companies)));
  }

  /** Starts a lightweight live tick for map pins and progress bars. */
  startLiveTick(intervalMs = 2800): Observable<PlatformOpsSummary> {
    return timer(0, intervalMs).pipe(
      switchMap(() => this.liveSummary()),
    );
  }

  private ensureSummary(): Observable<PlatformOpsSummary> {
    const snap = this.summarySubject.value;
    if (snap) {
      return of(snap);
    }
    return this.refresh();
  }

  private nudgeLivePositions(summary: PlatformOpsSummary): PlatformOpsSummary {
    const tick = Date.now() % 1000;
    const liveShipments = summary.liveShipments.map((s, i) => this.nudgeShipment(s, tick + i * 17));
    return { ...summary, liveShipments, onTimePct: summary.onTimePct + (tick % 3 === 0 ? 0.05 : 0) };
  }

  private nudgeShipment(row: PlatformShipmentOps, seed = Date.now()): PlatformShipmentOps {
    if (row.status !== 'IN_TRANSIT' && row.status !== 'AT_BORDER') {
      return row;
    }
    const delta = ((seed % 7) - 3) * 0.008;
    const progress = Math.min(98, Math.max(row.progressPct, row.progressPct + (seed % 2)));
    return {
      ...row,
      lat: row.lat + delta,
      lng: row.lng + delta * 0.6,
      progressPct: progress,
      speedKph: row.speedKph + ((seed % 5) - 2),
    };
  }
}
