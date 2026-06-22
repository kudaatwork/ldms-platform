import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { isApiFailureEnvelope, readApiFailureMessage } from '../../../core/utils/api-paged-response.util';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import type {
  CreateFundRequestPayload,
  FuelTelemetryLogRow,
  OperationalFundRequestRow,
} from '../models/fuel-expenses.model';

@Injectable({ providedIn: 'root' })
export class FuelExpensesPortalService {
  private readonly fundRequestBase = ldmsServiceUrl('fuel-expenses', 'operational-fund-request', undefined, 'frontend');
  private readonly telemetryBase = ldmsServiceUrl('fuel-expenses', 'fuel-telemetry-log', undefined, 'frontend');

  constructor(private readonly http: HttpClient) {}

  findFundRequestsByTrip(tripId: number): Observable<OperationalFundRequestRow[]> {
    return this.findFundRequests({ tripId, page: 0, size: 50 });
  }

  findFundRequests(filter: {
    tripId?: number;
    requestType?: 'FUEL_TOP_UP' | 'MECHANIC' | 'FUNDS';
    status?: string;
    page?: number;
    size?: number;
  }): Observable<OperationalFundRequestRow[]> {
    return this.http
      .post<unknown>(`${this.fundRequestBase}/find-by-multiple-filters`, {
        page: filter.page ?? 0,
        size: filter.size ?? 50,
        tripId: filter.tripId,
        requestType: filter.requestType,
        status: filter.status,
      })
      .pipe(
        map((resp) => this.extractList(resp, 'operationalFundRequestDtoList').map((d) => this.mapFundRequest(d))),
        catchError((err) => throwError(() => this.toError(err))),
      );
  }

  createFundRequest(payload: CreateFundRequestPayload): Observable<OperationalFundRequestRow> {
    return this.http.post<unknown>(`${this.fundRequestBase}/create`, payload).pipe(
      map((resp) => this.mapFundRequest(this.extractSingle(resp, 'operationalFundRequestDto'))),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  approveFundRequest(requestId: number, approvedLiters?: number, approvedAmount?: number): Observable<void> {
    return this.http
      .post<unknown>(`${this.fundRequestBase}/approve`, {
        requestId,
        approvedLiters,
        approvedAmount,
      })
      .pipe(
        map((resp) => {
          this.assertSuccess(resp);
        }),
        catchError((err) => throwError(() => this.toError(err))),
      );
  }

  rejectFundRequest(requestId: number, rejectionReason: string): Observable<void> {
    return this.http
      .post<unknown>(`${this.fundRequestBase}/reject`, { requestId, rejectionReason })
      .pipe(
        map((resp) => {
          this.assertSuccess(resp);
        }),
        catchError((err) => throwError(() => this.toError(err))),
      );
  }

  findTelemetryByTrip(tripId: number): Observable<FuelTelemetryLogRow[]> {
    return this.http.post<unknown>(`${this.telemetryBase}/find-by-trip/${tripId}`, {}).pipe(
      map((resp) => this.extractList(resp, 'fuelTelemetryLogDtoList').map((d) => this.mapTelemetry(d))),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /operational-fund-request/complete-roadside/{tripId} — resume trip after fuel/mechanic stop. */
  completeRoadsideStop(tripId: number): Observable<void> {
    return this.http.post<unknown>(`${this.fundRequestBase}/complete-roadside/${tripId}`, {}).pipe(
      map((resp) => {
        this.assertSuccess(resp);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  private mapFundRequest(dto: Record<string, unknown>): OperationalFundRequestRow {
    const type = String(dto['requestType'] ?? 'FUNDS');
    const status = String(dto['status'] ?? 'PENDING');
    return {
      id: Number(dto['id'] ?? 0),
      requestNumber: String(dto['requestNumber'] ?? `FRQ-${dto['id']}`),
      tripId: Number(dto['tripId'] ?? 0),
      requestType: type as OperationalFundRequestRow['requestType'],
      requestTypeLabel:
        type === 'FUEL_TOP_UP' ? 'Fuel top-up' : type === 'MECHANIC' ? 'Mechanic / roadside repair' : 'Funds',
      status: status as OperationalFundRequestRow['status'],
      statusLabel: this.statusLabel(status),
      litersRequested: dto['litersRequested'] != null ? Number(dto['litersRequested']) : undefined,
      amountRequested: dto['amountRequested'] != null ? Number(dto['amountRequested']) : undefined,
      currencyCode: String(dto['currencyCode'] ?? 'USD'),
      driverNotes: String(dto['driverNotes'] ?? ''),
      createdAtLabel: this.formatDate(dto['createdAt']),
      canApprove: status === 'PENDING',
      canReject: status === 'PENDING',
    };
  }

  private mapTelemetry(dto: Record<string, unknown>): FuelTelemetryLogRow {
    const source = String(dto['source'] ?? 'SYSTEM');
    const readingType = String(dto['readingType'] ?? 'LEVEL_SNAPSHOT');
    return {
      id: Number(dto['id'] ?? 0),
      source,
      sourceLabel: this.sourceLabel(source),
      readingType,
      readingTypeLabel: this.readingTypeLabel(readingType),
      fuelLevelPct: dto['fuelLevelPct'] != null ? Number(dto['fuelLevelPct']) : undefined,
      fuelLiters: dto['fuelLiters'] != null ? Number(dto['fuelLiters']) : undefined,
      consumedLiters: dto['consumedLiters'] != null ? Number(dto['consumedLiters']) : undefined,
      distanceDeltaKm: dto['distanceDeltaKm'] != null ? Number(dto['distanceDeltaKm']) : undefined,
      recordedAtLabel: this.formatDate(dto['recordedAt']),
    };
  }

  private statusLabel(status: string): string {
    switch (status) {
      case 'APPROVED':
        return 'Approved';
      case 'REJECTED':
        return 'Rejected';
      case 'CANCELLED':
        return 'Cancelled';
      default:
        return 'Pending';
    }
  }

  private sourceLabel(source: string): string {
    switch (source) {
      case 'DRIVER_APP':
        return 'Driver app';
      case 'TELEMATICS':
        return 'Telematics';
      case 'MANUAL':
        return 'Manual';
      case 'SIMULATED':
        return 'Simulated';
      default:
        return 'System';
    }
  }

  private readingTypeLabel(type: string): string {
    switch (type) {
      case 'CONSUMPTION_DELTA':
        return 'Consumption';
      case 'DISPENSE':
        return 'Dispense';
      case 'TOP_UP':
        return 'Top-up';
      default:
        return 'Level snapshot';
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
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return String(value);
    }
  }

  private assertSuccess(resp: unknown): void {
    if (isApiFailureEnvelope(resp)) {
      throw new Error(readApiFailureMessage(resp, 'Request failed.'));
    }
  }

  private extractSingle(resp: unknown, nestedKey: string): Record<string, unknown> {
    this.assertSuccess(resp);
    const obj = resp && typeof resp === 'object' && !Array.isArray(resp) ? (resp as Record<string, unknown>) : {};
    for (const key of [nestedKey, 'data', 'body', 'payload']) {
      const val = obj[key];
      if (val && typeof val === 'object' && !Array.isArray(val)) {
        return val as Record<string, unknown>;
      }
    }
    return obj;
  }

  private extractList(resp: unknown, listKey: string): Array<Record<string, unknown>> {
    this.assertSuccess(resp);
    const obj = resp && typeof resp === 'object' && !Array.isArray(resp) ? (resp as Record<string, unknown>) : {};
    for (const key of [listKey, 'data', 'body', 'payload']) {
      const val = obj[key];
      if (Array.isArray(val)) {
        return val as Array<Record<string, unknown>>;
      }
      const nested = val && typeof val === 'object' ? (val as Record<string, unknown>) : null;
      if (nested) {
        for (const k2 of ['content', 'list', listKey]) {
          if (Array.isArray(nested[k2])) {
            return nested[k2] as Array<Record<string, unknown>>;
          }
        }
      }
    }
    return [];
  }

  private toError(err: unknown): Error {
    if (err instanceof Error) {
      return err;
    }
    return new Error(String(err));
  }
}
