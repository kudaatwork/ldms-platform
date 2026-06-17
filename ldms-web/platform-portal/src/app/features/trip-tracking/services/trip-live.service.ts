import { HttpClient, HttpErrorResponse } from '@angular/common/http';
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

  stopDemoSimulation(tripId: number): Observable<TripLiveSnapshot> {
    return this.http.post<unknown>(`${this.tripLiveBase}/demo-simulation/${tripId}/stop`, {}).pipe(
      map((resp) => this.mapTripSnapshot(this.extractSingle(resp, 'liveSnapshot'))),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  pauseDemoSimulation(tripId: number): Observable<TripLiveSnapshot> {
    return this.http.post<unknown>(`${this.tripLiveBase}/demo-simulation/${tripId}/pause`, {}).pipe(
      map((resp) => this.mapTripSnapshot(this.extractSingle(resp, 'liveSnapshot'))),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  resumeDemoSimulation(tripId: number): Observable<TripLiveSnapshot> {
    return this.http.post<unknown>(`${this.tripLiveBase}/demo-simulation/${tripId}/resume`, {}).pipe(
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
    const trail = Array.isArray(dto['trail'])
      ? (dto['trail'] as unknown[]).map((w) => this.mapWaypoint(w as Record<string, unknown>))
      : [];
    return {
      tripId: Number(dto['tripId'] ?? 0),
      tripNumber: String(dto['tripNumber'] ?? ''),
      status: String(dto['status'] ?? ''),
      shipmentId: dto['shipmentId'] != null ? Number(dto['shipmentId']) : undefined,
      shipmentNumber: dto['shipmentNumber'] != null ? String(dto['shipmentNumber']) : undefined,
      productName: dto['productName'] != null ? String(dto['productName']) : undefined,
      productCode: dto['productCode'] != null ? String(dto['productCode']) : undefined,
      quantity: dto['quantity'] != null ? Number(dto['quantity']) : undefined,
      fromWarehouseName: String(dto['fromWarehouseName'] ?? '—'),
      toWarehouseName: String(dto['toWarehouseName'] ?? '—'),
      vehicleRegistration: dto['vehicleRegistration'] != null ? String(dto['vehicleRegistration']) : undefined,
      driverName: dto['driverName'] != null ? String(dto['driverName']) : undefined,
      fleetAssetId: dto['fleetAssetId'] != null ? Number(dto['fleetAssetId']) : undefined,
      latitude: dto['latitude'] != null ? Number(dto['latitude']) : undefined,
      longitude: dto['longitude'] != null ? Number(dto['longitude']) : undefined,
      speedKmh: dto['speedKmh'] != null ? Number(dto['speedKmh']) : 0,
      maxSpeedKmh: dto['maxSpeedKmh'] != null ? Number(dto['maxSpeedKmh']) : undefined,
      speedLimitExceeded: Boolean(dto['speedLimitExceeded']),
      headingDeg: dto['headingDeg'] != null ? Number(dto['headingDeg']) : 0,
      overallProgressPct: dto['overallProgressPct'] != null ? Number(dto['overallProgressPct']) : 0,
      distanceTravelledKm: dto['distanceTravelledKm'] != null ? Number(dto['distanceTravelledKm']) : undefined,
      fuelLevelPct: dto['fuelLevelPct'] != null ? Number(dto['fuelLevelPct']) : undefined,
      fuelRemainingLiters: dto['fuelRemainingLiters'] != null ? Number(dto['fuelRemainingLiters']) : undefined,
      simulationActive: Boolean(dto['simulationActive']),
      simulationPaused: Boolean(dto['simulationPaused']),
      moving: Boolean(dto['moving']),
      onBreak: Boolean(dto['onBreak']),
      routeWaypoints: waypoints,
      trail,
      journeyStartedAt: dto['journeyStartedAt'] != null ? String(dto['journeyStartedAt']) : undefined,
      totalElapsedSeconds: dto['totalElapsedSeconds'] != null ? Number(dto['totalElapsedSeconds']) : undefined,
      transitSeconds: dto['transitSeconds'] != null ? Number(dto['transitSeconds']) : undefined,
      waitingSeconds: dto['waitingSeconds'] != null ? Number(dto['waitingSeconds']) : undefined,
      idleSeconds: dto['idleSeconds'] != null ? Number(dto['idleSeconds']) : undefined,
      journeyPhase: dto['journeyPhase'] != null ? (String(dto['journeyPhase']) as TripLiveSnapshot['journeyPhase']) : undefined,
      estimatedArrivalSeconds:
        dto['estimatedArrivalSeconds'] != null ? Number(dto['estimatedArrivalSeconds']) : undefined,
      currentSegmentIndex: dto['currentSegmentIndex'] != null ? Number(dto['currentSegmentIndex']) : undefined,
      segmentProgressPct: dto['segmentProgressPct'] != null ? Number(dto['segmentProgressPct']) : undefined,
      completedWaypointCount:
        dto['completedWaypointCount'] != null ? Number(dto['completedWaypointCount']) : undefined,
      totalWaypointCount: dto['totalWaypointCount'] != null ? Number(dto['totalWaypointCount']) : undefined,
      lastTimingTickMs: Date.now(),
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
      speedKmh: dto['speedKmh'] != null ? Number(dto['speedKmh']) : undefined,
      recordedAt: dto['recordedAt'] != null ? String(dto['recordedAt']) : undefined,
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
