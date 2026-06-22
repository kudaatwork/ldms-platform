import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ldmsServiceUrl } from '../utils/api-url.util';

export interface PlatformShipmentDashboardApi {
  activeShipments: number;
  completedThisMonth: number;
  organizationsWithActivity: number;
  organizationStats: Array<{
    organizationId: number;
    activeShipments: number;
    completedThisMonth: number;
    lastActivityAt?: string;
  }>;
  shipmentsByStatus: Array<{
    status: string;
    label: string;
    count: number;
    color: string;
  }>;
  weeklyVolume: number[];
  liveShipments: Array<{
    id: number;
    shipmentNumber?: string;
    organizationId?: number;
    fromWarehouseName?: string;
    toWarehouseName?: string;
    transportCompanyName?: string;
    status?: string;
    productName?: string;
    modifiedAt?: string;
  }>;
}

export interface PlatformTripDashboardApi {
  activeTrips: number;
  deliveredTrips: number;
  onTimePct: number;
  organizationStats: Array<{
    organizationId: number;
    activeTrips: number;
  }>;
}

export interface PlatformBillingDashboardApi {
  pendingInvoicesCents: number;
}

export interface PlatformRevenueUsageBreakdownRow {
  actionCode: string;
  actionDisplayName?: string;
  totalChargeCents: number;
  eventCount: number;
}

export interface PlatformRevenueOrgRow {
  organizationId: number;
  organizationName: string;
  billingMode?: string;
  earnedCents: number;
  costsCents: number;
  netCents: number;
  walletBalanceCents?: number;
  depositCents?: number;
  actionChargeCents?: number;
  subscriptionUsageCents?: number;
  totalUsageCents?: number;
  usageEventCount?: number;
  accent: string;
  usageBreakdown?: PlatformRevenueUsageBreakdownRow[];
}

export interface PlatformRevenueChargeLine {
  id: string;
  label: string;
  category: string;
  amountCents: number;
  organizationId?: number;
  organizationName: string;
  occurredAt: string;
  deducted?: boolean;
}

export interface PlatformRevenueCategoryRow {
  category: string;
  amountCents: number;
  color: string;
}

export interface PlatformRevenueReportApi {
  totalEarnedCents: number;
  subscriptionCents: number;
  actionChargesCents: number;
  walletDepositsCents: number;
  monthLabels: string[];
  earnedSeries: number[];
  costSeries: number[];
  byOrganization: PlatformRevenueOrgRow[];
  costBreakdown: PlatformRevenueCategoryRow[];
  recentCharges: PlatformRevenueChargeLine[];
}

@Injectable({ providedIn: 'root' })
export class PlatformDashboardAdminService {
  constructor(private readonly http: HttpClient) {}

  fetchShipmentDashboard(): Observable<PlatformShipmentDashboardApi> {
    return this.http
      .get<{
        platformShipmentDashboardDto?: PlatformShipmentDashboardApi;
      }>(ldmsServiceUrl('shipment-management', 'platform-dashboard', 'shipments', 'backoffice'))
      .pipe(map((body) => body.platformShipmentDashboardDto ?? emptyShipmentDashboard()));
  }

  fetchTripDashboard(): Observable<PlatformTripDashboardApi> {
    return this.http
      .get<{
        platformTripDashboardDto?: PlatformTripDashboardApi;
      }>(ldmsServiceUrl('trip-tracking', 'platform-dashboard', 'trips', 'backoffice'))
      .pipe(map((body) => body.platformTripDashboardDto ?? emptyTripDashboard()));
  }

  fetchBillingDashboard(): Observable<PlatformBillingDashboardApi> {
    return this.http
      .get<{
        platformBillingDashboardDto?: PlatformBillingDashboardApi;
      }>(ldmsServiceUrl('billing-payments', 'platform-dashboard', 'invoices', 'backoffice'))
      .pipe(map((body) => body.platformBillingDashboardDto ?? { pendingInvoicesCents: 0 }));
  }

  fetchRevenueReport(): Observable<PlatformRevenueReportApi> {
    return this.http
      .get<{
        platformRevenueReportDto?: PlatformRevenueReportApi;
      }>(ldmsServiceUrl('billing-payments', 'platform-dashboard', 'revenue', 'backoffice'))
      .pipe(map((body) => body.platformRevenueReportDto ?? emptyRevenueReport()));
  }
}

function emptyShipmentDashboard(): PlatformShipmentDashboardApi {
  return {
    activeShipments: 0,
    completedThisMonth: 0,
    organizationsWithActivity: 0,
    organizationStats: [],
    shipmentsByStatus: [],
    weeklyVolume: [0, 0, 0, 0, 0, 0, 0],
    liveShipments: [],
  };
}

function emptyTripDashboard(): PlatformTripDashboardApi {
  return {
    activeTrips: 0,
    deliveredTrips: 0,
    onTimePct: 0,
    organizationStats: [],
  };
}

function emptyRevenueReport(): PlatformRevenueReportApi {
  return {
    totalEarnedCents: 0,
    subscriptionCents: 0,
    actionChargesCents: 0,
    walletDepositsCents: 0,
    monthLabels: [],
    earnedSeries: [],
    costSeries: [],
    byOrganization: [],
    costBreakdown: [],
    recentCharges: [],
  };
}
