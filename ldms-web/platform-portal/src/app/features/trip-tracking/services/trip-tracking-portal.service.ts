import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { isApiFailureEnvelope, readApiFailureMessage } from '../../../core/utils/api-paged-response.util';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import {
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
    return this.http.post<unknown>(`${this.shipmentBase}/find-by-multiple-filters`, filters).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.extractList(resp).map((dto) => this.mapShipmentRow(dto));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** GET /shipment/find-by-id/{id} */
  getShipment(id: number): Observable<ShipmentRow> {
    return this.http.get<unknown>(`${this.shipmentBase}/find-by-id/${id}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapShipmentRow(this.extractSingle(resp));
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
        const dto = this.tryExtractSingle(resp);
        return dto ? this.mapShipmentRow(dto) : null;
      }),
      catchError(() => [null]),
    );
  }

  /** POST /shipment/allocate */
  allocateShipment(payload: AllocateShipmentPayload): Observable<ShipmentRow> {
    return this.http.post<unknown>(`${this.shipmentBase}/allocate`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapShipmentRow(this.extractSingle(resp));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  // ── Trips ────────────────────────────────────────────────────────────────

  /** POST /trip/find-by-multiple-filters */
  findTrips(filters: TripFilterPayload): Observable<TripRow[]> {
    return this.http.post<unknown>(`${this.tripBase}/find-by-multiple-filters`, filters).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.extractList(resp).map((dto) => this.mapTripRow(dto));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** GET /trip/find-by-id/{id} */
  getTrip(id: number): Observable<TripDetail> {
    return this.http.get<unknown>(`${this.tripBase}/find-by-id/${id}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapTripDetail(this.extractSingle(resp));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** GET /trip/track/{id} — timeline view */
  trackTrip(id: number): Observable<TripDetail> {
    return this.http.get<unknown>(`${this.tripBase}/track/${id}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapTripDetail(this.extractSingle(resp));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /trip/start */
  startTrip(payload: StartTripPayload): Observable<TripRow> {
    return this.http.post<unknown>(`${this.tripBase}/start`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapTripRow(this.extractSingle(resp));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /trip/trigger-arrival */
  triggerArrival(payload: TriggerArrivalPayload): Observable<TripRow> {
    return this.http.post<unknown>(`${this.tripBase}/trigger-arrival`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapTripRow(this.extractSingle(resp));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /trip/verify-delivery-otp */
  verifyDeliveryOtp(payload: VerifyDeliveryOtpPayload): Observable<TripRow> {
    return this.http.post<unknown>(`${this.tripBase}/verify-delivery-otp`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapTripRow(this.extractSingle(resp));
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
      activeTrips: trips.filter((t) => t.status === 'IN_PROGRESS' || t.status === 'ARRIVED').length,
      deliveredToday: trips.filter((t) => t.status === 'DELIVERED').length,
    };
  }

  // ── Private mapping ──────────────────────────────────────────────────────

  private mapShipmentRow(dto: Record<string, unknown>): ShipmentRow {
    const status = (String(dto['status'] ?? 'PENDING')) as ShipmentStatus;
    return {
      id: Number(dto['id'] ?? 0),
      shipmentNumber: String(dto['shipmentNumber'] ?? dto['shipmentNo'] ?? `SHP-${dto['id']}`),
      transferReference: String(dto['transferReference'] ?? dto['reference'] ?? '—'),
      inventoryTransferId: dto['inventoryTransferId'] ? Number(dto['inventoryTransferId']) : undefined,
      fromWarehouse: String(dto['fromWarehouse'] ?? dto['originWarehouse'] ?? '—'),
      toWarehouse: String(dto['toWarehouse'] ?? dto['destinationWarehouse'] ?? '—'),
      productName: String(dto['productName'] ?? dto['product'] ?? '—'),
      quantity: Number(dto['quantity'] ?? 0),
      unitOfMeasure: String(dto['unitOfMeasure'] ?? dto['uom'] ?? ''),
      status,
      statusLabel: this.shipmentStatusLabel(status),
      statusTone: this.shipmentStatusTone(status),
      driverName: String(dto['driverName'] ?? dto['driver'] ?? '—'),
      vehicleRegistration: String(dto['vehicleRegistration'] ?? dto['registration'] ?? '—'),
      createdAtLabel: this.formatDate(dto['createdAt']),
      canAllocate: status === 'PENDING',
      canStartTrip: status === 'ALLOCATED',
    };
  }

  private mapTripRow(dto: Record<string, unknown>): TripRow {
    const status = (String(dto['status'] ?? 'PENDING')) as TripStatus;
    const lastEvent = this.toObj(dto['lastEvent']);
    return {
      id: Number(dto['id'] ?? 0),
      tripNumber: String(dto['tripNumber'] ?? dto['tripNo'] ?? `TRP-${dto['id']}`),
      shipmentId: Number(dto['shipmentId'] ?? 0),
      shipmentNumber: String(dto['shipmentNumber'] ?? '—'),
      route: this.buildRoute(dto),
      driverName: String(dto['driverName'] ?? dto['driver'] ?? '—'),
      vehicleRegistration: String(dto['vehicleRegistration'] ?? dto['registration'] ?? '—'),
      status,
      statusLabel: this.tripStatusLabel(status),
      statusTone: this.tripStatusTone(status),
      lastEventLabel: lastEvent ? String(lastEvent['eventType'] ?? '') : '—',
      lastEventAt: lastEvent ? this.formatDate(lastEvent['recordedAt']) : '—',
      startedAtLabel: this.formatDate(dto['startedAt']),
      canTriggerArrival: status === 'IN_PROGRESS',
      canVerifyOtp: status === 'ARRIVED',
    };
  }

  private mapTripDetail(dto: Record<string, unknown>): TripDetail {
    const status = (String(dto['status'] ?? 'PENDING')) as TripStatus;
    const events = Array.isArray(dto['events'])
      ? dto['events'].map((e: unknown) => this.mapTimelineEvent(e as Record<string, unknown>))
      : Array.isArray(dto['timeline'])
        ? (dto['timeline'] as unknown[]).map((e) => this.mapTimelineEvent(e as Record<string, unknown>))
        : [];
    return {
      id: Number(dto['id'] ?? 0),
      tripNumber: String(dto['tripNumber'] ?? dto['tripNo'] ?? `TRP-${dto['id']}`),
      shipmentId: Number(dto['shipmentId'] ?? 0),
      shipmentNumber: String(dto['shipmentNumber'] ?? '—'),
      driverName: String(dto['driverName'] ?? '—'),
      vehicleRegistration: String(dto['vehicleRegistration'] ?? '—'),
      status,
      statusLabel: this.tripStatusLabel(status),
      statusTone: this.tripStatusTone(status),
      route: this.buildRoute(dto),
      startedAtLabel: this.formatDate(dto['startedAt']),
      arrivedAtLabel: dto['arrivedAt'] ? this.formatDate(dto['arrivedAt']) : undefined,
      deliveredAtLabel: dto['deliveredAt'] ? this.formatDate(dto['deliveredAt']) : undefined,
      timeline: events,
      canTriggerArrival: status === 'IN_PROGRESS',
      canVerifyOtp: status === 'ARRIVED',
    };
  }

  private mapTimelineEvent(dto: Record<string, unknown>): TripTimelineEvent {
    const eventType = (String(dto['eventType'] ?? 'OTHER')) as TripEventType;
    return {
      id: Number(dto['id'] ?? Math.random()),
      eventType,
      eventTypeLabel: this.eventTypeLabel(eventType),
      latitude: dto['latitude'] ? Number(dto['latitude']) : undefined,
      longitude: dto['longitude'] ? Number(dto['longitude']) : undefined,
      notes: String(dto['notes'] ?? ''),
      recordedBy: String(dto['recordedBy'] ?? dto['createdBy'] ?? '—'),
      recordedAtLabel: this.formatDate(dto['recordedAt'] ?? dto['createdAt']),
    };
  }

  private buildRoute(dto: Record<string, unknown>): string {
    const from = String(dto['fromWarehouse'] ?? dto['originWarehouse'] ?? dto['origin'] ?? '');
    const to = String(dto['toWarehouse'] ?? dto['destinationWarehouse'] ?? dto['destination'] ?? '');
    if (from && to) {
      return `${from} → ${to}`;
    }
    return from || to || '—';
  }

  private shipmentStatusLabel(status: ShipmentStatus): string {
    const map: Record<ShipmentStatus, string> = {
      PENDING: 'Pending',
      ALLOCATED: 'Allocated',
      IN_TRANSIT: 'In transit',
      DELIVERED: 'Delivered',
      CANCELLED: 'Cancelled',
    };
    return map[status] ?? status;
  }

  private shipmentStatusTone(status: ShipmentStatus): ShipmentRow['statusTone'] {
    switch (status) {
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
    const map: Record<TripStatus, string> = {
      PENDING: 'Pending',
      IN_PROGRESS: 'In progress',
      ARRIVED: 'Arrived',
      DELIVERED: 'Delivered',
      CANCELLED: 'Cancelled',
    };
    return map[status] ?? status;
  }

  private tripStatusTone(status: TripStatus): TripRow['statusTone'] {
    switch (status) {
      case 'IN_PROGRESS':
        return 'warn';
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

  private eventTypeLabel(type: TripEventType): string {
    const map: Record<TripEventType, string> = {
      DEPARTURE: 'Departed',
      CHECKPOINT: 'Checkpoint',
      BREAK: 'Break',
      DELAY: 'Delay',
      ARRIVAL: 'Arrived',
      DELIVERED: 'Delivered',
      INCIDENT: 'Incident',
      OTHER: 'Event',
    };
    return map[type] ?? type;
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

  private extractList(resp: unknown): Array<Record<string, unknown>> {
    const obj = this.toObj(resp);
    if (!obj) {
      return [];
    }
    for (const key of ['data', 'body', 'payload', 'content']) {
      const val = obj[key];
      if (Array.isArray(val)) {
        return val as Array<Record<string, unknown>>;
      }
      const nested = this.toObj(val);
      if (nested) {
        for (const k2 of ['content', 'data', 'list']) {
          if (Array.isArray(nested[k2])) {
            return nested[k2] as Array<Record<string, unknown>>;
          }
        }
      }
    }
    if (Array.isArray(resp)) {
      return resp as Array<Record<string, unknown>>;
    }
    return [];
  }

  private extractSingle(resp: unknown): Record<string, unknown> {
    const obj = this.toObj(resp);
    if (!obj) {
      throw new Error('Empty response from server.');
    }
    for (const key of ['data', 'body', 'payload']) {
      const val = obj[key];
      if (val && typeof val === 'object' && !Array.isArray(val)) {
        return val as Record<string, unknown>;
      }
    }
    return obj;
  }

  private tryExtractSingle(resp: unknown): Record<string, unknown> | null {
    try {
      return this.extractSingle(resp);
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
      const msg = readApiFailureMessage(err.error, err.message);
      return new Error(msg || 'Network error.');
    }
    if (err instanceof Error) {
      return err;
    }
    return new Error(String(err));
  }
}
