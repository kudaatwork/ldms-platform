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

export interface PlatformFleetDashboardApi {
  totalFleetAssets: number;
  ownedFleetAssets: number;
  contractedFleetAssets: number;
  totalDrivers: number;
  organizationsWithFleet: number;
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
  walletDepositCount: number;
  monthLabels: string[];
  earnedSeries: number[];
  walletDepositsSeries?: number[];
  usageSeries?: number[];
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

  fetchFleetDashboard(): Observable<PlatformFleetDashboardApi> {
    return this.http
      .get<{
        platformFleetDashboardDto?: PlatformFleetDashboardApi;
      }>(ldmsServiceUrl('fleet-management', 'platform-dashboard', 'fleet', 'backoffice'))
      .pipe(map((body) => body.platformFleetDashboardDto ?? emptyFleetDashboard()));
  }

  fetchRevenueReport(): Observable<PlatformRevenueReportApi> {
    return this.http
      .get<{
        platformRevenueReportDto?: PlatformRevenueReportApi;
      }>(ldmsServiceUrl('billing-payments', 'platform-dashboard', 'revenue', 'backoffice'))
      .pipe(map((body) => body.platformRevenueReportDto ?? emptyRevenueReport()));
  }

  searchShipments(term: string, purchaseOrderIds: number[] = [], limit = 25): Observable<PlatformShipmentDashboardApi> {
    const params: Record<string, string> = {
      term: term.trim(),
      limit: String(limit),
    };
    if (purchaseOrderIds.length) {
      params['purchaseOrderIds'] = purchaseOrderIds.join(',');
    }
    return this.http
      .get<{
        platformShipmentDashboardDto?: PlatformShipmentDashboardApi;
      }>(ldmsServiceUrl('shipment-management', 'platform-dashboard', 'shipments/search', 'backoffice'), {
        params,
      })
      .pipe(map((body) => body.platformShipmentDashboardDto ?? emptyShipmentDashboard()));
  }

  searchPurchaseOrders(term: string, limit = 25): Observable<PlatformPurchaseOrderSearchRow[]> {
    return this.http
      .get<{
        purchaseOrderDtoList?: PlatformPurchaseOrderSearchRow[];
      }>(ldmsServiceUrl('inventory-management', 'platform-dashboard', 'purchase-orders/search', 'backoffice'), {
        params: { term: term.trim(), limit: String(limit) },
      })
      .pipe(map((body) => body.purchaseOrderDtoList ?? []));
  }
}

export interface PlatformPurchaseOrderSearchRow {
  id: number;
  purchaseOrderNumber?: string;
  organizationId?: number;
  status?: string;
  supplierContact?: string;
  buyerContact?: string;
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

function emptyFleetDashboard(): PlatformFleetDashboardApi {
  return {
    totalFleetAssets: 0,
    ownedFleetAssets: 0,
    contractedFleetAssets: 0,
    totalDrivers: 0,
    organizationsWithFleet: 0,
  };
}

function emptyRevenueReport(): PlatformRevenueReportApi {
  return {
    totalEarnedCents: 0,
    subscriptionCents: 0,
    actionChargesCents: 0,
    walletDepositsCents: 0,
    walletDepositCount: 0,
    monthLabels: [],
    earnedSeries: [],
    walletDepositsSeries: [],
    usageSeries: [],
    costSeries: [],
    byOrganization: [],
    costBreakdown: [],
    recentCharges: [],
  };
}
