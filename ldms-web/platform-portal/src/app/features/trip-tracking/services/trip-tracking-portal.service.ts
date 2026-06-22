import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { isApiFailureEnvelope, readApiFailureMessage, readInBodyStatusCode } from '../../../core/utils/api-paged-response.util';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import {
  AssignTransportCompanyPayload,
  AllocateShipmentPayload,
  RecordTripEventPayload,
  ShipmentFilterPayload,
  ShipmentRow,
  ShipmentStatus,
  StartTripPayload,
  TriggerArrivalPayload,
  TripDetail,
  TripEventType,
  TripFilterPayload,
  TripRow,
  TripStatus,
  TripTimelineEvent,
  TripWorkspaceMetrics,
  VerifyDeliveryOtpPayload,
} from '../models/trip-tracking.model';

/** Frontend portal service for shipment and trip-tracking APIs. */
@Injectable({ providedIn: 'root' })
export class TripTrackingPortalService {
  private readonly shipmentBase = ldmsServiceUrl('shipment-management', 'shipment', undefined, 'frontend');
  private readonly tripBase = ldmsServiceUrl('trip-tracking', 'trip', undefined, 'frontend');

  constructor(private readonly http: HttpClient) {}

  // ── Shipments ────────────────────────────────────────────────────────────

  /** POST /shipment/find-by-multiple-filters */
  findShipments(filters: ShipmentFilterPayload): Observable<ShipmentRow[]> {
    const payload: Record<string, unknown> = {};
    if (filters.organizationId) {
      payload['organizationId'] = filters.organizationId;
    }
    if (filters.inventoryTransferId) {
      payload['inventoryTransferId'] = filters.inventoryTransferId;
    }
    if (filters.search?.trim()) {
      payload['search'] = filters.search.trim();
    }
    const apiStatus = this.toApiShipmentStatus(filters.status);
    if (apiStatus) {
      payload['status'] = apiStatus;
    }
    return this.http.post<unknown>(`${this.shipmentBase}/find-by-multiple-filters`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.extractListOrEmpty(resp, 'shipmentDtoList').map((dto) => this.mapShipmentRow(dto));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** GET /shipment/find-by-id/{id} */
  getShipment(id: number): Observable<ShipmentRow> {
    return this.http.get<unknown>(`${this.shipmentBase}/find-by-id/${id}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapShipmentRow(this.extractSingle(resp, 'shipmentDto'));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** GET /shipment/by-transfer/{transferId} */
  getShipmentByTransfer(transferId: number): Observable<ShipmentRow | null> {
    return this.http.get<unknown>(`${this.shipmentBase}/by-transfer/${transferId}`).pipe(
      map((resp) => {
        if (isApiFailureEnvelope(resp)) {
          return null;
        }
        const dto = this.tryExtractSingle(resp, 'shipmentDto');
        return dto ? this.mapShipmentRow(dto) : null;
      }),
      catchError(() => [null]),
    );
  }

  /** POST /shipment/assign-transport-company */
  assignTransportCompany(payload: AssignTransportCompanyPayload): Observable<ShipmentRow> {
    return this.http.post<unknown>(`${this.shipmentBase}/assign-transport-company`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapShipmentRow(this.extractSingle(resp, 'shipmentDto'));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /shipment/allocate */
  allocateShipment(payload: AllocateShipmentPayload): Observable<ShipmentRow> {
    return this.http.post<unknown>(`${this.shipmentBase}/allocate`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapShipmentRow(this.extractSingle(resp, 'shipmentDto'));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  // ── Trips ────────────────────────────────────────────────────────────────

  /** POST /trip/find-by-multiple-filters */
  findTrips(filters: TripFilterPayload): Observable<TripRow[]> {
    const payload: Record<string, unknown> = { page: 0, size: 100 };
    if (filters.organizationId) {
      payload['organizationId'] = filters.organizationId;
    }
    if (filters.search?.trim()) {
      payload['searchTerm'] = filters.search.trim();
    }
    if (filters.activeOnly) {
      payload['activeOnly'] = true;
    } else {
      const apiStatus = this.toApiTripStatus(filters.status);
      if (apiStatus) {
        payload['status'] = apiStatus;
      }
    }
    return this.http.post<unknown>(`${this.tripBase}/find-by-multiple-filters`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.extractListOrEmpty(resp, 'tripDtoList').map((dto) => this.mapTripRow(dto));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** GET /trip/find-by-id/{id} */
  getTrip(id: number): Observable<TripDetail> {
    return this.http.get<unknown>(`${this.tripBase}/find-by-id/${id}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapTripDetail(this.extractSingle(resp, 'tripDto'));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** GET /trip/track/{id} — timeline view */
  trackTrip(id: number): Observable<TripDetail> {
    return this.http.get<unknown>(`${this.tripBase}/track/${id}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapTripDetail(this.extractSingle(resp, 'tripDto'));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /trip/start */
  startTrip(payload: StartTripPayload): Observable<TripRow> {
    return this.http.post<unknown>(`${this.tripBase}/start`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapTripRow(this.extractSingle(resp, 'tripDto'));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /trip/trigger-arrival */
  triggerArrival(payload: TriggerArrivalPayload): Observable<TripRow> {
    return this.http.post<unknown>(`${this.tripBase}/trigger-arrival`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapTripRow(this.extractSingle(resp, 'tripDto'));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /trip/verify-delivery-otp */
  verifyDeliveryOtp(payload: VerifyDeliveryOtpPayload): Observable<TripRow> {
    return this.http.post<unknown>(`${this.tripBase}/verify-delivery-otp`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapTripRow(this.extractSingle(resp, 'tripDto'));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /trip/record-event */
  recordEvent(payload: RecordTripEventPayload): Observable<void> {
    return this.http.post<unknown>(`${this.tripBase}/record-event`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  // ── Metrics helper ───────────────────────────────────────────────────────

  buildMetrics(shipments: ShipmentRow[], trips: TripRow[]): TripWorkspaceMetrics {
    return {
      totalShipments: shipments.length,
      allocatedShipments: shipments.filter((s) => s.status !== 'PENDING').length,
      activeTrips: trips.filter((t) => this.isActiveTrip(t.status)).length,
      deliveredToday: trips.filter((t) => t.status === 'DELIVERED').length,
    };
  }

  // ── Private mapping ──────────────────────────────────────────────────────

  private mapShipmentRow(dto: Record<string, unknown>): ShipmentRow {
    const status = this.mapShipmentStatus(String(dto['status'] ?? 'PENDING'));
    const fleetDriverId = dto['fleetDriverId'] ? Number(dto['fleetDriverId']) : undefined;
    const fleetAssetId = dto['fleetAssetId'] ? Number(dto['fleetAssetId']) : undefined;
    const transferId = dto['inventoryTransferId'] ? Number(dto['inventoryTransferId']) : undefined;
    return {
      id: Number(dto['id'] ?? 0),
      shipmentNumber: String(dto['shipmentNumber'] ?? dto['shipmentNo'] ?? `SHP-${dto['id']}`),
      organizationId: dto['organizationId'] ? Number(dto['organizationId']) : undefined,
      transferReference: transferId
        ? `TRF-${transferId}`
        : String(dto['transferReference'] ?? dto['reference'] ?? '—'),
      inventoryTransferId: transferId,
      fromWarehouse: String(dto['fromWarehouseName'] ?? dto['fromWarehouse'] ?? dto['originWarehouse'] ?? '—'),
      toWarehouse: String(dto['toWarehouseName'] ?? dto['toWarehouse'] ?? dto['destinationWarehouse'] ?? '—'),
      productName: String(dto['productName'] ?? dto['product'] ?? '—'),
      quantity: Number(dto['quantity'] ?? 0),
      unitOfMeasure: String(dto['unitOfMeasure'] ?? dto['uom'] ?? ''),
      status,
      statusLabel: this.shipmentStatusLabel(status),
      statusTone: this.shipmentStatusTone(status),
      fleetDriverId,
      fleetAssetId,
      transportCompanyOrganizationId: dto['transportCompanyOrganizationId']
        ? Number(dto['transportCompanyOrganizationId'])
        : undefined,
      transportCompanyName: String(dto['transportCompanyName'] ?? ''),
      driverName: String(
        dto['driverName'] ?? (fleetDriverId ? `Driver #${fleetDriverId}` : '—'),
      ),
      vehicleRegistration: String(
        dto['vehicleRegistration'] ?? (fleetAssetId ? `Vehicle #${fleetAssetId}` : '—'),
      ),
      createdAtLabel: this.formatDate(dto['createdAt']),
      canAssignTransport: status === 'PENDING',
      canAllocate: status === 'PENDING_FLEET',
      canStartTrip: status === 'ALLOCATED' && !!fleetDriverId && !!fleetAssetId,
      crossBorder: dto['crossBorder'] === true,
      tripId: dto['tripId'] ? Number(dto['tripId']) : undefined,
    };
  }

  mapShipmentRowForViewer(
    row: ShipmentRow,
    viewerOrganizationId: number,
    orgClassification: string,
    canAllocateFleet: boolean,
  ): ShipmentRow {
    const assignedToViewer =
      !!row.transportCompanyOrganizationId && row.transportCompanyOrganizationId === viewerOrganizationId;
    const ownedByViewer =
      !!row.organizationId
      && row.organizationId === viewerOrganizationId
      && (orgClassification === 'SUPPLIER' || orgClassification === 'CUSTOMER');
    const transporterViewer = orgClassification === 'TRANSPORT_COMPANY';
    const mayActOnShipment = ownedByViewer || (transporterViewer && assignedToViewer);
    return {
      ...row,
      canAssignTransport: row.status === 'PENDING' && ownedByViewer,
      canAllocate: row.status === 'PENDING_FLEET' && mayActOnShipment && canAllocateFleet,
      canStartTrip:
        row.status === 'ALLOCATED'
        && !!row.fleetDriverId
        && !!row.fleetAssetId
        && (ownedByViewer || assignedToViewer),
    };
  }

  private mapTripRow(dto: Record<string, unknown>): TripRow {
    const status = this.mapTripStatus(String(dto['status'] ?? 'PENDING'));
    const recentEvents = Array.isArray(dto['recentEvents']) ? dto['recentEvents'] : [];
    const lastEvent = recentEvents.length
      ? (recentEvents[0] as Record<string, unknown>)
      : this.toObj(dto['lastEvent']);
    return {
      id: Number(dto['id'] ?? 0),
      tripNumber: String(dto['tripNumber'] ?? dto['tripNo'] ?? `TRP-${dto['id']}`),
      shipmentId: Number(dto['shipmentId'] ?? 0),
      shipmentNumber: String(dto['shipmentNumber'] ?? (dto['shipmentId'] ? `SHP-${dto['shipmentId']}` : '—')),
      route: this.buildRoute(dto),
      productName: String(dto['productName'] ?? '—'),
      productCode: dto['productCode'] != null ? String(dto['productCode']) : undefined,
      quantity: dto['quantity'] != null ? Number(dto['quantity']) : undefined,
      cargoLabel: this.buildCargoLabel(dto),
      driverName: String(dto['driverName'] ?? (dto['fleetDriverId'] ? `Driver #${dto['fleetDriverId']}` : '—')),
      vehicleRegistration: String(
        dto['vehicleRegistration'] ?? (dto['fleetAssetId'] ? `Vehicle #${dto['fleetAssetId']}` : '—'),
      ),
      status,
      statusLabel: this.tripStatusLabel(status),
      statusTone: this.tripStatusTone(status),
      lastEventLabel: lastEvent ? this.eventTypeLabel(String(lastEvent['eventType'] ?? '')) : '—',
      lastEventAt: lastEvent ? this.formatDate(lastEvent['eventTime'] ?? lastEvent['recordedAt']) : '—',
      startedAtLabel: this.formatDate(dto['startedAt']),
      canTriggerArrival:
        status === 'IN_PROGRESS'
        || status === 'IN_TRANSIT'
        || status === 'AT_BORDER_HOLD'
        || status === 'ROADSIDE_HOLD'
        || status === 'RETURN_IN_TRANSIT',
      canVerifyOtp:
        status === 'ARRIVED' || status === 'OTP_PENDING' || status === 'COUNT_COMPLETE',
      canLiveTrack: this.isActiveTrip(status),
    };
  }

  private mapTripDetail(dto: Record<string, unknown>): TripDetail {
    const status = this.mapTripStatus(String(dto['status'] ?? 'PENDING'));
    const events = Array.isArray(dto['recentEvents'])
      ? dto['recentEvents'].map((e: unknown) => this.mapTimelineEvent(e as Record<string, unknown>))
      : Array.isArray(dto['events'])
        ? dto['events'].map((e: unknown) => this.mapTimelineEvent(e as Record<string, unknown>))
        : Array.isArray(dto['timeline'])
          ? (dto['timeline'] as unknown[]).map((e) => this.mapTimelineEvent(e as Record<string, unknown>))
          : [];
    return {
      id: Number(dto['id'] ?? 0),
      tripNumber: String(dto['tripNumber'] ?? dto['tripNo'] ?? `TRP-${dto['id']}`),
      shipmentId: Number(dto['shipmentId'] ?? 0),
      shipmentNumber: String(dto['shipmentNumber'] ?? (dto['shipmentId'] ? `SHP-${dto['shipmentId']}` : '—')),
      driverName: String(dto['driverName'] ?? (dto['fleetDriverId'] ? `Driver #${dto['fleetDriverId']}` : '—')),
      vehicleRegistration: String(
        dto['vehicleRegistration'] ?? (dto['fleetAssetId'] ? `Vehicle #${dto['fleetAssetId']}` : '—'),
      ),
      status,
      statusLabel: this.tripStatusLabel(status),
      statusTone: this.tripStatusTone(status),
      route: this.buildRoute(dto),
      startedAtLabel: this.formatDate(dto['startedAt']),
      arrivedAtLabel: dto['arrivedAt'] ? this.formatDate(dto['arrivedAt']) : undefined,
      deliveredAtLabel: dto['completedAt']
        ? this.formatDate(dto['completedAt'])
        : dto['deliveredAt']
          ? this.formatDate(dto['deliveredAt'])
          : undefined,
      timeline: events,
      canTriggerArrival:
        status === 'IN_PROGRESS'
        || status === 'IN_TRANSIT'
        || status === 'AT_BORDER_HOLD'
        || status === 'ROADSIDE_HOLD'
        || status === 'RETURN_IN_TRANSIT',
      canVerifyOtp:
        status === 'ARRIVED' || status === 'OTP_PENDING' || status === 'COUNT_COMPLETE',
      canLiveTrack: this.isActiveTrip(status),
    };
  }

  private mapTimelineEvent(dto: Record<string, unknown>): TripTimelineEvent {
    const eventTypeRaw = String(dto['eventType'] ?? 'NOTE');
    const eventType = eventTypeRaw as TripEventType;
    return {
      id: Number(dto['id'] ?? Math.random()),
      eventType,
      eventTypeLabel: this.eventTypeLabel(eventTypeRaw),
      latitude: dto['latitude'] ? Number(dto['latitude']) : undefined,
      longitude: dto['longitude'] ? Number(dto['longitude']) : undefined,
      notes: String(dto['notes'] ?? ''),
      recordedBy: String(dto['recordedBy'] ?? dto['createdBy'] ?? '—'),
      recordedAtLabel: this.formatDate(dto['eventTime'] ?? dto['recordedAt'] ?? dto['createdAt']),
      recordedAtIso: String(dto['eventTime'] ?? dto['recordedAt'] ?? dto['createdAt'] ?? ''),
    };
  }

  private buildRoute(dto: Record<string, unknown>): string {
    const from = String(
      dto['fromWarehouseName'] ?? dto['fromWarehouse'] ?? dto['originWarehouse'] ?? dto['origin'] ?? '',
    );
    const to = String(
      dto['toWarehouseName'] ?? dto['toWarehouse'] ?? dto['destinationWarehouse'] ?? dto['destination'] ?? '',
    );
    if (from && to) {
      return `${from} → ${to}`;
    }
    return from || to || '—';
  }

  private buildCargoLabel(dto: Record<string, unknown>): string {
    const product = String(dto['productName'] ?? '').trim();
    const code = dto['productCode'] != null ? String(dto['productCode']).trim() : '';
    const qty = dto['quantity'] != null ? Number(dto['quantity']) : null;
    if (!product && (qty == null || Number.isNaN(qty))) {
      return '—';
    }
    const parts: string[] = [];
    if (product) {
      parts.push(product);
    }
    if (code) {
      parts.push(`(${code})`);
    }
    if (qty != null && !Number.isNaN(qty)) {
      parts.push(`× ${qty.toLocaleString()}`);
    }
    return parts.join(' ') || '—';
  }

  private mapShipmentStatus(raw: string): ShipmentStatus {
    switch (raw.toUpperCase()) {
      case 'PENDING_ALLOCATION':
        return 'PENDING';
      case 'PENDING_FLEET_ALLOCATION':
        return 'PENDING_FLEET';
      case 'ARRIVED_PENDING_OTP':
        return 'IN_TRANSIT';
      default:
        return (raw as ShipmentStatus) || 'PENDING';
    }
  }

  /** Maps portal display status to backend enum name for filter requests. */
  private toApiShipmentStatus(status: ShipmentStatus | '' | undefined): string | undefined {
    if (!status) {
      return undefined;
    }
    switch (status) {
      case 'PENDING':
        return 'PENDING_ALLOCATION';
      case 'PENDING_FLEET':
        return 'PENDING_FLEET_ALLOCATION';
      default:
        return status;
    }
  }

  /** Maps portal display status to backend enum name for filter requests. */
  private toApiTripStatus(status: TripStatus | '' | undefined): string | undefined {
    if (!status) {
      return undefined;
    }
    switch (status) {
      case 'PENDING':
        return 'SCHEDULED';
      case 'IN_PROGRESS':
        return 'IN_TRANSIT';
      case 'ARRIVED':
        return 'ARRIVED';
      default:
        return status;
    }
  }

  /** Trip is still on the road or awaiting delivery confirmation. */
  isActiveTrip(status: TripStatus): boolean {
    return (
      status === 'PENDING'
      || status === 'IN_PROGRESS'
      || status === 'IN_TRANSIT'
      || status === 'AT_BORDER_HOLD'
      || status === 'ROADSIDE_HOLD'
      || status === 'ARRIVED'
      || status === 'COUNTING_STOCK'
      || status === 'COUNT_COMPLETE'
      || status === 'OTP_PENDING'
      || status === 'RETURN_IN_TRANSIT'
    );
  }

  private mapTripStatus(raw: string): TripStatus {
    switch (raw.toUpperCase()) {
      case 'SCHEDULED':
        return 'PENDING';
      case 'IN_TRANSIT':
        return 'IN_PROGRESS';
      case 'OTP_PENDING':
      case 'COUNTING_STOCK':
      case 'COUNT_COMPLETE':
      case 'ARRIVED':
        return 'ARRIVED';
      case 'AT_BORDER_HOLD':
        return 'AT_BORDER_HOLD';
      case 'ROADSIDE_HOLD':
        return 'ROADSIDE_HOLD';
      case 'RETURN_IN_TRANSIT':
        return 'RETURN_IN_TRANSIT';
      case 'RETURNED':
        return 'RETURNED';
      default:
        return (raw as TripStatus) || 'PENDING';
    }
  }

  private shipmentStatusLabel(status: ShipmentStatus): string {
    const map: Record<ShipmentStatus, string> = {
      PENDING: 'Pending dispatch',
      PENDING_FLEET: 'Awaiting fleet',
      ALLOCATED: 'Allocated',
      IN_TRANSIT: 'In transit',
      DELIVERED: 'Delivered',
      CANCELLED: 'Cancelled',
    };
    return map[status] ?? status;
  }

  private shipmentStatusTone(status: ShipmentStatus): ShipmentRow['statusTone'] {
    switch (status) {
      case 'PENDING_FLEET':
        return 'warn';
      case 'ALLOCATED':
        return 'info';
      case 'IN_TRANSIT':
        return 'warn';
      case 'DELIVERED':
        return 'success';
      case 'CANCELLED':
        return 'danger';
      default:
        return 'muted';
    }
  }

  private tripStatusLabel(status: TripStatus): string {
    const map: Record<string, string> = {
      PENDING: 'Pending',
      IN_PROGRESS: 'In progress',
      IN_TRANSIT: 'In transit',
      AT_BORDER_HOLD: 'At border — awaiting clearance',
      ROADSIDE_HOLD: 'Roadside stop',
      ARRIVED: 'Arrived',
      COUNTING_STOCK: 'Counting stock',
      COUNT_COMPLETE: 'Count complete',
      OTP_PENDING: 'Awaiting delivery OTP',
      RETURN_IN_TRANSIT: 'Return in transit',
      RETURNED: 'Returned',
      DELIVERED: 'Delivered',
      CANCELLED: 'Cancelled',
    };
    return map[status] ?? status;
  }

  private tripStatusTone(status: TripStatus): TripRow['statusTone'] {
    switch (status) {
      case 'IN_PROGRESS':
      case 'IN_TRANSIT':
        return 'warn';
      case 'AT_BORDER_HOLD':
      case 'ROADSIDE_HOLD':
        return 'danger';
      case 'ARRIVED':
        return 'info';
      case 'DELIVERED':
        return 'success';
      case 'CANCELLED':
        return 'danger';
      default:
        return 'muted';
    }
  }

  private eventTypeLabel(type: string): string {
    switch (type.toUpperCase()) {
      case 'DEPARTED':
      case 'DEPARTURE':
        return 'Departed';
      case 'ARRIVED_AT_BORDER':
        return 'Stopped at border';
      case 'BORDER_CLEARED':
        return 'Border cleared — proceeding';
      case 'ROADSIDE_FUEL_STOP':
        return 'Roadside fuel stop';
      case 'ROADSIDE_MECHANIC_STOP':
        return 'Roadside mechanic stop';
      case 'ROADSIDE_RESUMED':
        return 'Resumed after roadside stop';
      case 'DRIVER_BREAK':
        return 'Driver break / vehicle halted';
      case 'DRIVER_RESUMED':
        return 'Driver resumed movement';
      case 'CHECKPOINT':
        return 'Checkpoint';
      case 'BREAK':
        return 'Break';
      case 'DELAY':
        return 'Delay';
      case 'ARRIVED':
      case 'ARRIVAL':
        return 'Arrived';
      case 'OTP_SENT':
        return 'OTP sent';
      case 'OTP_VERIFIED':
        return 'OTP verified';
      case 'DELIVERED':
        return 'Delivered';
      case 'INCIDENT':
        return 'Incident';
      case 'NOTE':
      case 'OTHER':
        return 'Note';
      default:
        return type.replace(/_/g, ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase());
    }
  }

  private formatDate(value: unknown): string {
    if (!value) {
      return '—';
    }
    try {
      return new Date(String(value)).toLocaleString('en-GB', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return String(value);
    }
  }

  // ── Envelope helpers ─────────────────────────────────────────────────────

  private assertSuccess(resp: unknown): void {
    if (isApiFailureEnvelope(resp)) {
      throw new Error(readApiFailureMessage(resp, 'Request failed.'));
    }
  }

  private extractListOrEmpty(response: unknown, listKey: string): Array<Record<string, unknown>> {
    if (isApiFailureEnvelope(response)) {
      if (readInBodyStatusCode(response) === 404) {
        return [];
      }
      throw new Error(readApiFailureMessage(response, 'Request failed.'));
    }
    return this.extractList(response, listKey);
  }

  private unwrapEnvelope(response: unknown): Record<string, unknown> {
    const root = this.toObj(response);
    if (!root) {
      return {};
    }
    return this.toObj(root['data']) ?? this.toObj(root['body']) ?? this.toObj(root['payload']) ?? root;
  }

  private extractList(response: unknown, listKey: string): Array<Record<string, unknown>> {
    const envelope = this.unwrapEnvelope(response);
    const list = envelope[listKey];
    if (Array.isArray(list)) {
      return list.filter((r): r is Record<string, unknown> => !!this.toObj(r));
    }
    return [];
  }

  private extractSingle(response: unknown, dtoKey: string): Record<string, unknown> {
    const envelope = this.unwrapEnvelope(response);
    return this.toObj(envelope[dtoKey]) ?? envelope;
  }

  private tryExtractSingle(response: unknown, dtoKey: string): Record<string, unknown> | null {
    try {
      if (isApiFailureEnvelope(response)) {
        return null;
      }
      return this.extractSingle(response, dtoKey);
    } catch {
      return null;
    }
  }

  private toObj(value: unknown): Record<string, unknown> | null {
    return value && typeof value === 'object' && !Array.isArray(value)
      ? (value as Record<string, unknown>)
      : null;
  }

  private toError(err: unknown): Error {
    if (err instanceof HttpErrorResponse) {
      if (err.status === 401 && (err.url ?? '').includes('shipment-management')) {
        return new Error(
          'Shipment service rejected your sign-in token. Restart ldms-shipment-management (port 8015) so its JWT secret matches authentication, then refresh this page.',
        );
      }
      const msg = readApiFailureMessage(err.error, err.message);
      return new Error(msg || 'Network error.');
    }
    if (err instanceof Error) {
      return err;
    }
    return new Error(String(err));
  }
}
