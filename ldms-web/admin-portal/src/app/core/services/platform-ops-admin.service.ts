import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, catchError, forkJoin, map, of, switchMap, tap, timer } from 'rxjs';
import { OrganizationsAdminService } from '../../features/organizations/services/organizations-admin.service';
import { classificationLabel } from '../../features/organizations/models/organization.model';
import type { OrganizationClassification } from '../../features/organizations/models/organization.model';
import {
  PlatformOpsSummary,
  PlatformRevenueReport,
  PlatformShipmentOps,
  buildPlatformRevenueReport,
  findShipmentById,
  shipmentsForCompany,
} from './platform-ops-mock.data';
import {
  PlatformDashboardAdminService,
  PlatformShipmentDashboardApi,
  PlatformTripDashboardApi,
} from './platform-dashboard-admin.service';

const ACCENTS = ['#6366f1', '#0ea5e9', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6', '#14b8a6', '#f97316'];

const STATUS_LABELS: Record<string, string> = {
  PENDING_ALLOCATION: 'Pending allocation',
  PENDING_FLEET_ALLOCATION: 'Pending fleet',
  ALLOCATED: 'Allocated',
  IN_TRANSIT: 'In transit',
  ARRIVED_PENDING_OTP: 'At destination',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled',
};

@Injectable({ providedIn: 'root' })
export class PlatformOpsAdminService {
  private readonly summarySubject = new BehaviorSubject<PlatformOpsSummary | null>(null);
  readonly summary$ = this.summarySubject.asObservable();

  constructor(
    private readonly organizations: OrganizationsAdminService,
    private readonly platformDashboard: PlatformDashboardAdminService,
  ) {}

  refresh(): Observable<PlatformOpsSummary> {
    return forkJoin({
      orgs: this.organizations.queryAllOrganizations(0, 500, { searchQuery: '', organizationDirectoryOnly: true }),
      shipments: this.platformDashboard.fetchShipmentDashboard().pipe(catchError(() => of(null))),
      trips: this.platformDashboard.fetchTripDashboard().pipe(catchError(() => of(null))),
      billing: this.platformDashboard.fetchBillingDashboard().pipe(catchError(() => of({ pendingInvoicesCents: 0 }))),
    }).pipe(
      map(({ orgs, shipments, trips, billing }) =>
        this.mergeDashboardSnapshot(orgs.rows, shipments, trips, billing.pendingInvoicesCents),
      ),
      tap((summary) => this.summarySubject.next(summary)),
    );
  }

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

  startLiveTick(intervalMs = 2800): Observable<PlatformOpsSummary> {
    return timer(0, intervalMs).pipe(switchMap(() => this.liveSummary()));
  }

  private mergeDashboardSnapshot(
    orgRows: Array<{
      id: number;
      name: string;
      organizationClassification?: OrganizationClassification | string;
      classificationLabel?: string;
    }>,
    shipmentDash: PlatformShipmentDashboardApi | null,
    tripDash: PlatformTripDashboardApi | null,
    pendingInvoicesCents: number,
  ): PlatformOpsSummary {
    const shipments = shipmentDash ?? {
      activeShipments: 0,
      completedThisMonth: 0,
      organizationsWithActivity: 0,
      organizationStats: [],
      shipmentsByStatus: [],
      weeklyVolume: [0, 0, 0, 0, 0, 0, 0],
      liveShipments: [],
    };
    const trips = tripDash ?? {
      activeTrips: 0,
      deliveredTrips: 0,
      onTimePct: 0,
      organizationStats: [],
    };

    const orgById = new Map(
      orgRows.map((org, index) => [
        org.id,
        {
          name: org.name,
          classification: String(org.organizationClassification ?? ''),
          classificationLabel:
            org.classificationLabel ??
            classificationLabel((org.organizationClassification ?? 'SUPPLIER') as OrganizationClassification),
          accent: ACCENTS[index % ACCENTS.length],
        },
      ]),
    );

    const tripByOrg = new Map(
      trips.organizationStats
        .filter((row) => (row.activeTrips ?? 0) > 0)
        .map((row) => [row.organizationId, row.activeTrips ?? 0]),
    );
    const shipmentByOrg = new Map(
      shipments.organizationStats
        .filter((row) => (row.activeShipments ?? 0) > 0 || (row.completedThisMonth ?? 0) > 0)
        .map((row) => [
          row.organizationId,
          {
            activeShipments: row.activeShipments ?? 0,
            completedThisMonth: row.completedThisMonth ?? 0,
            lastActivityAt: row.lastActivityAt ?? '',
          },
        ]),
    );

    const orgIds = new Set<number>([
      ...shipmentByOrg.keys(),
      ...tripByOrg.keys(),
    ]);

    const performingCompanies = [...orgIds]
      .map((organizationId) => {
        const org = orgById.get(organizationId);
        const ship = shipmentByOrg.get(organizationId);
        const activeShipments = ship?.activeShipments ?? 0;
        const completedShipments = ship?.completedThisMonth ?? 0;
        const activeTrips = tripByOrg.get(organizationId) ?? 0;
        const performanceScore = completedShipments * 2 + activeShipments + activeTrips;
        return {
          organizationId,
          organizationName: org?.name ?? `Organisation #${organizationId}`,
          classification: org?.classification ?? '',
          classificationLabel: org?.classificationLabel ?? 'Organisation',
          activeShipments,
          completedShipments,
          activeTrips,
          onTimePct: trips.onTimePct,
          revenueCents: 0,
          walletBalanceCents: 0,
          lastActivityAt: ship?.lastActivityAt ?? '',
          accent: org?.accent ?? ACCENTS[organizationId % ACCENTS.length],
          performanceScore,
        };
      })
      .filter((row) => hasLogisticsActivity(row));

    const companies = performingCompanies
      .sort((a, b) => b.performanceScore - a.performanceScore)
      .slice(0, 6)
      .map(({ performanceScore: _performanceScore, ...row }) => row);

    const liveShipments: PlatformShipmentOps[] = shipments.liveShipments.map((row, index) => {
      const org = row.organizationId != null ? orgById.get(row.organizationId) : undefined;
      const status = row.status ?? 'IN_TRANSIT';
      const hash = (row.organizationId ?? index) * 17 + index * 3;
      return {
        shipmentId: row.id,
        shipmentRef: row.shipmentNumber ?? `SHP-${row.id}`,
        organizationId: row.organizationId ?? 0,
        organizationName: org?.name ?? 'Organisation',
        vehicleReg: row.transportCompanyName ? row.transportCompanyName.slice(0, 12) : `LDMS-${row.id}`,
        driverName: 'Assigned driver',
        origin: row.fromWarehouseName ?? 'Origin',
        destination: row.toWarehouseName ?? 'Destination',
        status: mapShipmentStatus(status),
        statusLabel: STATUS_LABELS[status] ?? status,
        progressPct: status === 'DELIVERED' ? 100 : status === 'IN_TRANSIT' ? 68 : 42,
        eta: '—',
        lat: -17.8 + (hash % 50) * 0.01,
        lng: 31.0 + (hash % 40) * 0.02,
        speedKph: 55 + (hash % 20),
        cargoSummary: row.productName ?? 'Cargo',
        customerName: org?.name ?? 'Customer',
      };
    });

    return {
      totalOrganizations: orgRows.length,
      organizationsWithActivity: performingCompanies.length,
      activeShipments: shipments.activeShipments ?? 0,
      activeTrips: trips.activeTrips ?? 0,
      completedThisMonth: shipments.completedThisMonth ?? 0,
      onTimePct: trips.onTimePct ?? 0,
      platformRevenueCents: 0,
      pendingInvoicesCents,
      shipmentsByStatus: shipments.shipmentsByStatus ?? [],
      weeklyVolume: normalizeWeeklyVolume(shipments.weeklyVolume),
      companies,
      liveShipments,
    };
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
    return { ...summary, liveShipments };
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

function mapShipmentStatus(status: string): PlatformShipmentOps['status'] {
  switch (status) {
    case 'IN_TRANSIT':
      return 'IN_TRANSIT';
    case 'ARRIVED_PENDING_OTP':
      return 'AT_BORDER';
    case 'DELIVERED':
      return 'DELIVERED';
    case 'CANCELLED':
      return 'CANCELLED';
    case 'ALLOCATED':
    case 'PENDING_FLEET_ALLOCATION':
    case 'PENDING_ALLOCATION':
      return 'APPROVED';
    default:
      return 'SUBMITTED';
  }
}

function hasLogisticsActivity(row: {
  activeShipments: number;
  completedShipments: number;
  activeTrips: number;
}): boolean {
  return row.activeShipments > 0 || row.completedShipments > 0 || row.activeTrips > 0;
}

function normalizeWeeklyVolume(values: number[] | undefined): number[] {
  const base = values?.length ? [...values] : [0, 0, 0, 0, 0, 0, 0];
  while (base.length < 7) {
    base.unshift(0);
  }
  return base.slice(-7);
}
