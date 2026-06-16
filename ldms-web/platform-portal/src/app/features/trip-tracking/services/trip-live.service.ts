import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { isApiFailureEnvelope, readApiFailureMessage } from '../../../core/utils/api-paged-response.util';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import type { FuelLiveSnapshot, TripLiveSnapshot, TripRouteWaypoint } from '../models/trip-tracking.model';

@Injectable({ providedIn: 'root' })
export class TripLiveService {
  private readonly tripLiveBase = ldmsServiceUrl('trip-tracking', 'trip-live', undefined, 'frontend');
  private readonly fuelBase = ldmsServiceUrl('fuel-expenses', 'fuel-session', undefined, 'frontend');

  constructor(private readonly http: HttpClient) {}

  getLiveSnapshot(tripId: number): Observable<TripLiveSnapshot> {
    return this.http.get<unknown>(`${this.tripLiveBase}/snapshot/${tripId}`).pipe(
      map((resp) => this.mapTripSnapshot(this.extractSingle(resp))),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  startDemoSimulation(tripId: number): Observable<TripLiveSnapshot> {
    return this.http.post<unknown>(`${this.tripLiveBase}/demo-simulation/${tripId}/start`, {}).pipe(
      map((resp) => this.mapTripSnapshot(this.extractSingle(resp, 'liveSnapshot'))),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  getFuelLive(tripId: number): Observable<FuelLiveSnapshot> {
    return this.http.get<unknown>(`${this.fuelBase}/live/${tripId}`).pipe(
      map((resp) => this.mapFuelSnapshot(this.extractSingle(resp))),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  private mapTripSnapshot(dto: Record<string, unknown>): TripLiveSnapshot {
    const waypoints = Array.isArray(dto['routeWaypoints'])
      ? (dto['routeWaypoints'] as unknown[]).map((w) => this.mapWaypoint(w as Record<string, unknown>))
      : [];
    return {
      tripId: Number(dto['tripId'] ?? 0),
      tripNumber: String(dto['tripNumber'] ?? ''),
      status: String(dto['status'] ?? ''),
      fromWarehouseName: String(dto['fromWarehouseName'] ?? '—'),
      toWarehouseName: String(dto['toWarehouseName'] ?? '—'),
      latitude: dto['latitude'] != null ? Number(dto['latitude']) : undefined,
      longitude: dto['longitude'] != null ? Number(dto['longitude']) : undefined,
      speedKmh: dto['speedKmh'] != null ? Number(dto['speedKmh']) : 0,
      headingDeg: dto['headingDeg'] != null ? Number(dto['headingDeg']) : 0,
      overallProgressPct: dto['overallProgressPct'] != null ? Number(dto['overallProgressPct']) : 0,
      simulationActive: Boolean(dto['simulationActive']),
      moving: Boolean(dto['moving']),
      routeWaypoints: waypoints,
    };
  }

  private mapFuelSnapshot(dto: Record<string, unknown>): FuelLiveSnapshot {
    return {
      tripId: Number(dto['tripId'] ?? 0),
      fleetDriverId: dto['fleetDriverId'] != null ? Number(dto['fleetDriverId']) : undefined,
      fleetAssetId: dto['fleetAssetId'] != null ? Number(dto['fleetAssetId']) : undefined,
      fuelLevelPct: dto['fuelLevelPct'] != null ? Number(dto['fuelLevelPct']) : 100,
      fuelRemainingLiters: dto['fuelRemainingLiters'] != null ? Number(dto['fuelRemainingLiters']) : 0,
      tankCapacityLiters: dto['tankCapacityLiters'] != null ? Number(dto['tankCapacityLiters']) : 400,
      distanceTravelledKm: dto['distanceTravelledKm'] != null ? Number(dto['distanceTravelledKm']) : 0,
      consumptionRateLPer100Km:
        dto['consumptionRateLPer100Km'] != null
          ? Number(dto['consumptionRateLPer100Km'])
          : dto['consumptionRateLPer100km'] != null
            ? Number(dto['consumptionRateLPer100km'])
            : 35,
      moving: Boolean(dto['moving']),
      status: String(dto['status'] ?? 'ACTIVE'),
    };
  }

  private mapWaypoint(dto: Record<string, unknown>): TripRouteWaypoint {
    return {
      label: String(dto['label'] ?? ''),
      latitude: Number(dto['latitude'] ?? 0),
      longitude: Number(dto['longitude'] ?? 0),
      type: String(dto['type'] ?? 'CHECKPOINT'),
    };
  }

  private extractSingle(resp: unknown, nestedKey = 'liveSnapshot'): Record<string, unknown> {
    if (isApiFailureEnvelope(resp)) {
      throw new Error(readApiFailureMessage(resp, 'Request failed.'));
    }
    const obj = resp && typeof resp === 'object' && !Array.isArray(resp) ? (resp as Record<string, unknown>) : {};
    for (const key of [nestedKey, 'data', 'body', 'payload', 'fuelSessionDto']) {
      const val = obj[key];
      if (val && typeof val === 'object' && !Array.isArray(val)) {
        return val as Record<string, unknown>;
      }
    }
    return obj;
  }

  private toError(err: unknown): Error {
    if (err instanceof Error) {
      return err;
    }
    return new Error(String(err));
  }
}
