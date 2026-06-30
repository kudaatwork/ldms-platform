import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, map, of, throwError } from 'rxjs';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import {
  isApiFailureEnvelope,
  readApiFailureMessage,
} from '../../../core/utils/api-paged-response.util';
import {
  DriverProfileDto,
  DriverProfileEditRequest,
  DriverTripRow,
  DriverWorkspaceMetrics,
  DeliveryWorkflowPhase,
  ReceiverContactDto,
  TripChatState,
  TripMessageDto,
} from '../models/driver-portal.model';

@Injectable({ providedIn: 'root' })
export class DriverPortalService {
  private readonly fleetBase = ldmsServiceUrl('fleet-management', 'fleet', undefined, 'frontend');
  private readonly tripBase = ldmsServiceUrl('trip-tracking', 'trip', undefined, 'frontend');

  constructor(private readonly http: HttpClient) {}

  // ── Driver profile ────────────────────────────────────────────────────────

  getMyDriverProfile(): Observable<DriverProfileDto> {
    return this.http.get<unknown>(`${this.fleetBase}/drivers/me`).pipe(
      map((res: unknown) => this.mapDriverProfile(res as Record<string, unknown>)),
      catchError((err) =>
        throwError(
          () => new Error(err?.error?.message ?? 'Failed to load driver profile'),
        ),
      ),
    );
  }

  updateMyDriverProfile(payload: DriverProfileEditRequest): Observable<DriverProfileDto> {
    return this.http.put<unknown>(`${this.fleetBase}/drivers/me`, payload).pipe(
      map((res: unknown) => {
        if (isApiFailureEnvelope(res)) {
          throw new Error(readApiFailureMessage(res, 'Failed to update profile'));
        }
        return this.mapDriverProfile(res as Record<string, unknown>);
      }),
      catchError((err) => throwError(() => this.toApiError(err, 'Failed to update profile'))),
    );
  }

  // ── Trip chat (driver ⇄ receiver) ──────────────────────────────────────────

  getTripChat(tripId: number): Observable<TripChatState> {
    return this.http.get<unknown>(`${this.tripBase}/my-trips/${tripId}/messages`).pipe(
      map((res) => this.mapChatState(res)),
      catchError((err) => throwError(() => this.toApiError(err, 'Failed to load messages'))),
    );
  }

  sendTripMessage(tripId: number, body: string): Observable<TripChatState> {
    return this.http
      .post<unknown>(`${this.tripBase}/my-trips/${tripId}/messages`, { body })
      .pipe(
        map((res) => this.mapChatState(res)),
        catchError((err) => throwError(() => this.toApiError(err, 'Failed to send message'))),
      );
  }

  getReceiverContact(tripId: number): Observable<ReceiverContactDto> {
    return this.http
      .get<unknown>(`${this.tripBase}/my-trips/${tripId}/receiver-contact`)
      .pipe(
        map((res) => {
          if (isApiFailureEnvelope(res)) {
            throw new Error(readApiFailureMessage(res, 'Failed to load receiver'));
          }
          const raw = res as Record<string, unknown>;
          return this.mapReceiverContact(raw['receiverContact'] as Record<string, unknown>);
        }),
        catchError((err) => throwError(() => this.toApiError(err, 'Failed to load receiver'))),
      );
  }

  // ── Driver trips ──────────────────────────────────────────────────────────

  getMyTrips(): Observable<DriverTripRow[]> {
    return this.http.get<unknown>(`${this.tripBase}/my-trips`).pipe(
      map((res: unknown) => {
        if (isApiFailureEnvelope(res)) {
          throw new Error(readApiFailureMessage(res, 'Failed to load trips'));
        }
        const raw = res as Record<string, unknown>;
        const list: unknown[] = Array.isArray(raw['driverTripSummaryDtoList'])
          ? raw['driverTripSummaryDtoList']
          : Array.isArray(raw['driverTripSummaryList'])
            ? raw['driverTripSummaryList']
            : Array.isArray(raw['data'])
              ? raw['data']
              : Array.isArray(res)
                ? res
                : [];
        return list.map((item) => this.mapTripRow(item as Record<string, unknown>));
      }),
      catchError((err) =>
        throwError(() => new Error(err?.error?.message ?? 'Failed to load trips')),
      ),
    );
  }

  getMyTripById(tripId: number): Observable<DriverTripRow> {
    return this.http.get<unknown>(`${this.tripBase}/my-trips/${tripId}`).pipe(
      map((raw: unknown) => {
        if (isApiFailureEnvelope(raw)) {
          throw new Error(readApiFailureMessage(raw, 'Failed to load trip'));
        }
        const res = raw as Record<string, unknown>;
        const nested = res['driverTripSummaryDto'] as Record<string, unknown> | undefined;
        return this.mapTripRow((nested ?? res) as Record<string, unknown>);
      }),
      catchError((err) => throwError(() => this.toApiError(err, 'Failed to load trip'))),
    );
  }

  getWorkspaceMetrics(): Observable<DriverWorkspaceMetrics> {
    return this.http.get<unknown>(`${this.tripBase}/my-trips/metrics`).pipe(
      map((res: unknown) => {
        const raw = res as any;
        // Backend may return metrics nested under driverWorkspaceMetrics or at root
        const m = raw?.driverTripMetricsDto ?? raw?.driverWorkspaceMetrics ?? raw ?? {};
        return {
          activeTrips: Number(m.activeTrips ?? m.active_trips ?? 0),
          completedToday: Number(m.completedToday ?? m.completed_today ?? 0),
          pendingDeliveries: Number(m.pendingDeliveries ?? m.pending_deliveries ?? 0),
        } satisfies DriverWorkspaceMetrics;
      }),
      catchError(() => of({ activeTrips: 0, completedToday: 0, pendingDeliveries: 0 })),
    );
  }

  // ── Internal mapping ──────────────────────────────────────────────────────

  private mapDriverProfile(raw: Record<string, unknown>): DriverProfileDto {
    const dto = (raw['fleetDriverDto'] ?? raw) as Record<string, unknown>;
    const first = String(dto['firstName'] ?? '');
    const last = String(dto['lastName'] ?? '');
    return {
      id: Number(dto['id'] ?? 0),
      userId: Number(dto['userId'] ?? 0),
      firstName: first,
      lastName: last,
      fullName: `${first} ${last}`.trim() || 'Driver',
      licenseNumber: String(dto['licenseNumber'] ?? ''),
      licenseClass: String(dto['licenseClass'] ?? ''),
      phoneNumber: String(dto['phoneNumber'] ?? ''),
      organizationName: String(
        dto['organizationName'] ?? dto['homeOrganizationName'] ?? '',
      ),
      vehicleRegistration: dto['vehicleRegistration']
        ? String(dto['vehicleRegistration'])
        : undefined,
      vehicleType: dto['vehicleType'] ? String(dto['vehicleType']) : undefined,
      employmentType: dto['employmentType'] ? String(dto['employmentType']) : undefined,
      nationalIdNumber: dto['nationalIdNumber'] ? String(dto['nationalIdNumber']) : undefined,
      addressLine1: dto['addressLine1'] ? String(dto['addressLine1']) : undefined,
      addressLine2: dto['addressLine2'] ? String(dto['addressLine2']) : undefined,
      addressCity: dto['addressCity'] ? String(dto['addressCity']) : undefined,
      addressProvince: dto['addressProvince'] ? String(dto['addressProvince']) : undefined,
      addressPostalCode: dto['addressPostalCode'] ? String(dto['addressPostalCode']) : undefined,
      addressCountry: dto['addressCountry'] ? String(dto['addressCountry']) : undefined,
    };
  }

  private mapChatState(res: unknown): TripChatState {
    if (isApiFailureEnvelope(res)) {
      throw new Error(readApiFailureMessage(res, 'Failed to load messages'));
    }
    const raw = (res ?? {}) as Record<string, unknown>;
    const list = Array.isArray(raw['messages']) ? (raw['messages'] as unknown[]) : [];
    return {
      messages: list.map((m) => this.mapMessage(m as Record<string, unknown>)),
      receiverContact: raw['receiverContact']
        ? this.mapReceiverContact(raw['receiverContact'] as Record<string, unknown>)
        : undefined,
      myRole: String(raw['myRole'] ?? ''),
      currentUserId: Number(raw['currentUserId'] ?? 0) || undefined,
    };
  }

  private mapMessage(raw: Record<string, unknown>): TripMessageDto {
    return {
      id: Number(raw['id'] ?? 0),
      senderUserId: Number(raw['senderUserId'] ?? 0),
      senderRole: String(raw['senderRole'] ?? ''),
      senderName: String(raw['senderName'] ?? '').trim() || 'Unknown',
      body: String(raw['body'] ?? ''),
      createdAtLabel: String(raw['createdAtLabel'] ?? ''),
      mine: raw['mine'] === true,
      read: raw['read'] === true,
    };
  }

  private mapReceiverContact(raw: Record<string, unknown> | undefined): ReceiverContactDto {
    const dto = (raw ?? {}) as Record<string, unknown>;
    return {
      userId: Number(dto['userId'] ?? 0) || undefined,
      name: dto['name'] ? String(dto['name']) : undefined,
      phoneNumber: dto['phoneNumber'] ? String(dto['phoneNumber']) : undefined,
      email: dto['email'] ? String(dto['email']) : undefined,
      destinationName: dto['destinationName'] ? String(dto['destinationName']) : undefined,
      reachable: dto['reachable'] === true,
    };
  }

  private mapTripRow(raw: Record<string, unknown>): DriverTripRow {
    const status = String(raw['status'] ?? raw['tripStatus'] ?? '');
    return {
      id: Number(raw['id'] ?? raw['tripId'] ?? 0),
      tripNumber: String(raw['tripNumber'] ?? ''),
      shipmentNumber: String(raw['shipmentNumber'] ?? ''),
      route: String(raw['route'] ?? raw['routeLabel'] ?? ''),
      cargoLabel: String(raw['cargoLabel'] ?? raw['cargo'] ?? ''),
      productName: String(raw['productName'] ?? ''),
      quantity: Number(raw['quantity'] ?? 0),
      unitOfMeasure: String(raw['unitOfMeasure'] ?? raw['uom'] ?? ''),
      vehicleRegistration: String(raw['vehicleRegistration'] ?? raw['vehicleReg'] ?? ''),
      status,
      statusLabel: String(raw['statusLabel'] ?? status),
      statusTone: (raw['statusTone'] as DriverTripRow['statusTone']) ?? 'muted',
      startedAtLabel: String(raw['startedAtLabel'] ?? raw['startedAt'] ?? ''),
      estimatedArrivalLabel: raw['estimatedArrivalLabel']
        ? String(raw['estimatedArrivalLabel'])
        : undefined,
      canTriggerArrival: raw['canTriggerArrival'] === true,
      canStartDeliveryWorkflow:
        raw['canStartDeliveryWorkflow'] === true || status === 'ARRIVED',
      canLiveTrack: raw['canLiveTrack'] === true,
      deliveryWorkflowPhase: raw['deliveryWorkflowPhase'] as DeliveryWorkflowPhase | undefined,
      driverName: String(raw['driverName'] ?? '').trim() || undefined,
      driverPhone: String(raw['driverPhone'] ?? '').trim() || undefined,
      fleetDriverId: Number(raw['fleetDriverId'] ?? 0) > 0 ? Number(raw['fleetDriverId']) : undefined,
      driverUserId: Number(raw['driverUserId'] ?? 0) > 0 ? Number(raw['driverUserId']) : undefined,
      inventoryTransferId: Number(raw['inventoryTransferId'] ?? 0) > 0 ? Number(raw['inventoryTransferId']) : undefined,
    };
  }

  private toApiError(err: unknown, fallback: string): Error {
    if (err instanceof Error && !(err instanceof HttpErrorResponse)) {
      return err;
    }
    if (err instanceof HttpErrorResponse) {
      const body = err.error;
      if (typeof body === 'string' && body.trim()) {
        return new Error(body.trim());
      }
      const fromEnvelope = readApiFailureMessage(body, '');
      if (fromEnvelope) {
        return new Error(fromEnvelope);
      }
      const gatewayError =
        typeof body === 'object' && body && typeof (body as Record<string, unknown>)['error'] === 'string'
          ? String((body as Record<string, unknown>)['error'])
          : '';
      return new Error(gatewayError || err.message || fallback);
    }
    return new Error(fallback);
  }
}
