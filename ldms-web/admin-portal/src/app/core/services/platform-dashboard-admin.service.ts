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
