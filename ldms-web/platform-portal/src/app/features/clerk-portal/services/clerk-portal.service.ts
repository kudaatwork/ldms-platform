import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, map, throwError } from 'rxjs';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import {
  isApiFailureEnvelope,
  readApiFailureMessage,
} from '../../../core/utils/api-paged-response.util';
import {
  ChatContactDto,
  ClerkProfileDto,
  ClerkWorkspaceMetrics,
  IncomingDeliveryRow,
  StockReceiveRequest,
  TripChatState,
  TripMessageDto,
} from '../models/clerk-portal.model';

@Injectable({ providedIn: 'root' })
export class ClerkPortalService {
  private readonly tripBase = ldmsServiceUrl('trip-tracking', 'trip', undefined, 'frontend');
  private readonly inventoryBase = ldmsServiceUrl('inventory-management', 'inventory', undefined, 'frontend');

  constructor(private readonly http: HttpClient) {}

  // ── Clerk profile ────────────────────────────────────────────────────────

  getMyClerkProfile(): Observable<ClerkProfileDto> {
    return this.http.get<unknown>(`${this.tripBase}/clerks/me`).pipe(
      map((res: unknown) => this.mapClerkProfile(res as Record<string, unknown>)),
      catchError((err) =>
        throwError(() => new Error(err?.error?.message ?? 'Failed to load clerk profile')),
      ),
    );
  }

  // ── Incoming deliveries ──────────────────────────────────────────────────

  getIncomingDeliveries(): Observable<IncomingDeliveryRow[]> {
    return this.http.get<unknown>(`${this.tripBase}/clerks/incoming-deliveries`).pipe(
      map((res) => this.mapDeliveryList(res)),
      catchError((err) => throwError(() => this.toApiError(err, 'Failed to load deliveries'))),
    );
  }

  getDeliveryDetail(tripId: number): Observable<IncomingDeliveryRow> {
    return this.http.get<unknown>(`${this.tripBase}/clerks/incoming-deliveries/${tripId}`).pipe(
      map((res) => {
        if (isApiFailureEnvelope(res)) {
          throw new Error(readApiFailureMessage(res, 'Failed to load delivery detail'));
        }
        const raw = (res ?? {}) as Record<string, unknown>;
        const data = (raw['data'] ?? raw) as Record<string, unknown>;
        return this.mapDeliveryRow(data);
      }),
      catchError((err) => throwError(() => this.toApiError(err, 'Failed to load delivery detail'))),
    );
  }

  // ── Stock receive ────────────────────────────────────────────────────────

  receiveStock(request: StockReceiveRequest): Observable<void> {
    return this.http
      .post<unknown>(`${this.inventoryBase}/goods-receipts/clerk-receive`, request)
      .pipe(
        map((res) => {
          if (isApiFailureEnvelope(res)) {
            throw new Error(readApiFailureMessage(res, 'Failed to receive stock'));
          }
        }),
        catchError((err) => throwError(() => this.toApiError(err, 'Failed to receive stock'))),
      );
  }

  // ── Trip chat (clerk ⇄ driver) ───────────────────────────────────────────

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

  // ── Mappers ──────────────────────────────────────────────────────────────

  private mapChatState(res: unknown): TripChatState {
    if (isApiFailureEnvelope(res)) {
      throw new Error(readApiFailureMessage(res, 'Failed to load messages'));
    }
    const raw = (res ?? {}) as Record<string, unknown>;
    const list = Array.isArray(raw['messages']) ? (raw['messages'] as unknown[]) : [];
    return {
      messages: list.map((m) => this.mapMessage(m as Record<string, unknown>)),
      contact: raw['receiverContact']
        ? this.mapChatContact(raw['receiverContact'] as Record<string, unknown>)
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

  private mapChatContact(raw: Record<string, unknown> | undefined): ChatContactDto {
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

  private mapClerkProfile(raw: Record<string, unknown>): ClerkProfileDto {
    return {
      id: Number(raw['id'] ?? 0),
      firstName: String(raw['firstName'] ?? ''),
      lastName: String(raw['lastName'] ?? ''),
      email: String(raw['email'] ?? ''),
      phoneNumber: raw['phoneNumber'] ? String(raw['phoneNumber']) : undefined,
      organizationName: String(raw['organizationName'] ?? ''),
      branchName: String(raw['branchName'] ?? ''),
      branchId: Number(raw['branchId'] ?? 0),
    };
  }

  private mapDeliveryList(res: unknown): IncomingDeliveryRow[] {
    const raw = res as Record<string, unknown>;
    const items = (raw['data'] ?? raw['items'] ?? raw) as unknown[];
    if (!Array.isArray(items)) return [];
    return items.map((item) => this.mapDeliveryRow(item as Record<string, unknown>));
  }

  private mapDeliveryRow(raw: Record<string, unknown>): IncomingDeliveryRow {
    return {
      tripId: Number(raw['tripId'] ?? raw['id'] ?? 0),
      tripNumber: String(raw['tripNumber'] ?? ''),
      status: String(raw['status'] ?? ''),
      productName: String(raw['productName'] ?? ''),
      quantity: Number(raw['quantity'] ?? 0),
      driverName: String(raw['driverName'] ?? ''),
      vehicleReg: raw['vehicleReg'] ? String(raw['vehicleReg']) : undefined,
      originBranch: String(raw['originBranch'] ?? ''),
      eta: raw['eta'] ? String(raw['eta']) : undefined,
      arrivedAt: raw['arrivedAt'] ? String(raw['arrivedAt']) : undefined,
      inventoryTransferId: raw['inventoryTransferId'] ? Number(raw['inventoryTransferId']) : undefined,
    };
  }

  private toApiError(err: HttpErrorResponse, fallback: string): Error {
    return new Error(err?.error?.message ?? fallback);
  }
}
