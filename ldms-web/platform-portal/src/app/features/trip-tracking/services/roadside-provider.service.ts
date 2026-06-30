import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map, catchError, of } from 'rxjs';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';

export type RoadsideProviderType = 'FUEL_STATION' | 'MECHANIC' | 'ROADSIDE_SUPPORT';

export interface RoadsideProviderRow {
  id: number;
  providerType: RoadsideProviderType | string;
  providerTypeLabel: string;
  name: string;
  description?: string;
  phone?: string;
  servicesOffered?: string;
  latitude: number;
  longitude: number;
  addressLabel?: string;
  open24Hours: boolean;
  verified: boolean;
  distanceKm?: number;
}

interface RoadsideProviderApiResponse {
  success?: boolean;
  isSuccess?: boolean;
  statusCode?: number;
  message?: string;
  roadsideProviderDtoList?: RoadsideProviderRow[];
}

@Injectable({ providedIn: 'root' })
export class RoadsideProviderService {
  private readonly base = ldmsServiceUrl('fuel-expenses', 'roadside-provider', undefined, 'frontend');

  constructor(private readonly http: HttpClient) {}

  listAll(): Observable<RoadsideProviderRow[]> {
    return this.http.get<RoadsideProviderApiResponse>(`${this.base}/list`).pipe(
      map((res) => {
        if (!(res.success === true || res.isSuccess === true)) {
          throw new Error(res.message ?? 'Could not load roadside providers.');
        }
        return res.roadsideProviderDtoList ?? [];
      }),
      catchError((err: HttpErrorResponse | Error) => {
        const status = (err as HttpErrorResponse).status;
        if (status === 503 || status === 502 || status === 504) {
          // Service temporarily unavailable — return empty list so callers degrade gracefully
          return of([] as RoadsideProviderRow[]);
        }
        throw err;
      }),
    );
  }

  listNearby(lat: number, lng: number, radiusKm = 150): Observable<RoadsideProviderRow[]> {
    return this.http
      .get<RoadsideProviderApiResponse>(`${this.base}/nearby`, {
        params: { lat, lng, radiusKm },
      })
      .pipe(
        map((res) => {
          if (!(res.success === true || res.isSuccess === true)) {
            throw new Error(res.message ?? 'Could not load nearby roadside providers.');
          }
          return res.roadsideProviderDtoList ?? [];
        }),
        catchError((err: HttpErrorResponse | Error) => {
          const status = (err as HttpErrorResponse).status;
          if (status === 503 || status === 502 || status === 504) {
            return of([] as RoadsideProviderRow[]);
          }
          throw err;
        }),
      );
  }
}
