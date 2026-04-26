import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, of, throwError, timeout } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  MOCK_ADMIN_LEVELS,
  MOCK_CITIES,
  MOCK_COUNTRIES,
  MOCK_DISTRICTS,
  MOCK_PROVINCES,
  MOCK_SUBURBS,
  MOCK_VILLAGES,
} from '../data/locations-mock-data';
import type {
  AdministrativeLevel,
  AdministrativeLevelListResponse,
  Country,
  CountryListResponse,
  District,
  DistrictListResponse,
  LocationNode,
  LocationNodeListResponse,
  LocationEntityKind,
  Province,
  ProvinceListResponse,
  Suburb,
  SuburbListResponse,
} from '../models/location.models';

const DEFAULT_PAGE_SIZE = 500;
/** Hard ceiling per list call so the UI never blocks waiting for a stalled service. */
const LIST_TIMEOUT_MS = 8000;

/**
 * Resource path segment for each location entity kind under the location-management service.
 * `city` and `village` both live under the `location-node` resource, which is only exposed
 * via the frontend surface today (no system resource exists yet).
 */
const RESOURCE_SEGMENT: Record<LocationEntityKind, string> = {
  country: 'country',
  province: 'province',
  district: 'district',
  suburb: 'suburb',
  'admin-level': 'administrative-level',
  city: 'location-node',
  village: 'location-node',
};

/** Kinds for which only the `frontend` surface exists, regardless of `environment.apiSurface`. */
const FRONTEND_ONLY: ReadonlySet<LocationEntityKind> = new Set(['city', 'village']);

@Injectable({
  providedIn: 'root',
})
export class LocationsService {
  private readonly base = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  getCountries(): Observable<Country[]> {
    if (environment.useMocks) {
      return of([...MOCK_COUNTRIES]);
    }
    return this.http
      .post<CountryListResponse>(this.url('country', 'find-by-multiple-filters'), this.defaultFilterBody())
      .pipe(
        timeout(LIST_TIMEOUT_MS),
        map((r) => r.countryDtoPage?.content ?? r.countryDtoList ?? []),
        catchError((err) => this.emptyOnNotFound<Country>(err)),
      );
  }

  getProvinces(): Observable<Province[]> {
    if (environment.useMocks) {
      return of([...MOCK_PROVINCES]);
    }
    return this.http
      .post<ProvinceListResponse>(this.url('province', 'find-by-multiple-filters'), this.defaultFilterBody())
      .pipe(
        timeout(LIST_TIMEOUT_MS),
        map((r) => r.provinceDtoPage?.content ?? r.provinceDtoList ?? []),
        catchError((err) => this.emptyOnNotFound<Province>(err)),
      );
  }

  getDistricts(): Observable<District[]> {
    if (environment.useMocks) {
      return of([...MOCK_DISTRICTS]);
    }
    return this.http
      .post<DistrictListResponse>(this.url('district', 'find-by-multiple-filters'), this.defaultFilterBody())
      .pipe(
        timeout(LIST_TIMEOUT_MS),
        map((r) => r.districtDtoPage?.content ?? r.districtDtoList ?? []),
        catchError((err) => this.emptyOnNotFound<District>(err)),
      );
  }

  getCities(): Observable<LocationNode[]> {
    return this.getLocationNodesByType('CITY');
  }

  getSuburbs(): Observable<Suburb[]> {
    if (environment.useMocks) {
      return of([...MOCK_SUBURBS]);
    }
    return this.http
      .post<SuburbListResponse>(this.url('suburb', 'find-by-multiple-filters'), this.defaultFilterBody())
      .pipe(
        timeout(LIST_TIMEOUT_MS),
        map((r) => r.suburbDtoPage?.content ?? r.suburbDtoList ?? []),
        catchError((err) => this.emptyOnNotFound<Suburb>(err)),
      );
  }

  getVillages(): Observable<LocationNode[]> {
    return this.getLocationNodesByType('VILLAGE');
  }

  getAdminLevels(): Observable<AdministrativeLevel[]> {
    if (environment.useMocks) {
      return of([...MOCK_ADMIN_LEVELS]);
    }
    return this.http
      .post<AdministrativeLevelListResponse>(
        this.url('admin-level', 'find-by-multiple-filters'),
        this.defaultFilterBody(),
      )
      .pipe(
        timeout(LIST_TIMEOUT_MS),
        map((r) => r.administrativeLevelDtoPage?.content ?? r.administrativeLevelDtoList ?? []),
        catchError((err) => this.emptyOnNotFound<AdministrativeLevel>(err)),
      );
  }

  createLocation(kind: LocationEntityKind, payload: Record<string, unknown>): Observable<boolean> {
    if (environment.useMocks) {
      return of(true);
    }
    return this.http
      .post(this.url(kind, 'create'), this.cleanPayload(payload))
      .pipe(map(() => true));
  }

  updateLocation(
    kind: LocationEntityKind,
    id: number,
    payload: Record<string, unknown>,
  ): Observable<boolean> {
    if (environment.useMocks) {
      return of(true);
    }
    const body = this.cleanPayload({ ...payload, id });
    return this.http.put(this.url(kind, 'update'), body).pipe(map(() => true));
  }

  findLocationById(kind: LocationEntityKind, id: number): Observable<Record<string, unknown> | null> {
    if (environment.useMocks) {
      return of(null);
    }
    return this.http.get<Record<string, unknown>>(`${this.url(kind, 'find-by-id')}/${id}`).pipe(
      map((response) => this.extractEntity(kind, response)),
      catchError((err: HttpErrorResponse) => {
        if (err.status === 404) return of(null);
        return throwError(() => err);
      }),
    );
  }

  deleteLocation(kind: LocationEntityKind, id: number): Observable<boolean> {
    if (environment.useMocks) {
      return of(true);
    }
    return this.http.delete(`${this.url(kind, 'delete-by-id')}/${id}`).pipe(map(() => true));
  }

  exportLocation(kind: LocationEntityKind, format: 'csv' | 'xlsx' | 'pdf'): Observable<Blob> {
    if (environment.useMocks) {
      return throwError(() => new Error('Export is not available while mocks are enabled.'));
    }
    if (!this.supportsImportExport(kind)) {
      return throwError(() => new Error('Export is not supported for this location type yet.'));
    }
    return this.http.post(`${this.url(kind, 'export')}?format=${format}`, this.defaultFilterBody(), {
      responseType: 'blob',
    });
  }

  importLocationCsv(kind: LocationEntityKind, file: File): Observable<boolean> {
    if (environment.useMocks) {
      return throwError(() => new Error('Import is not available while mocks are enabled.'));
    }
    if (!this.supportsImportExport(kind)) {
      return throwError(() => new Error('CSV import is not supported for this location type yet.'));
    }
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post(this.url(kind, 'import-csv'), formData).pipe(map(() => true));
  }

  private getLocationNodesByType(locationType: 'CITY' | 'VILLAGE'): Observable<LocationNode[]> {
    if (environment.useMocks) {
      return of([...(locationType === 'CITY' ? MOCK_CITIES : MOCK_VILLAGES)]);
    }
    const kind: LocationEntityKind = locationType === 'CITY' ? 'city' : 'village';
    return this.http
      .post<LocationNodeListResponse>(this.url(kind, 'find-by-multiple-filters'), {
        locationType,
        page: 0,
        size: DEFAULT_PAGE_SIZE,
      })
      .pipe(
        timeout(LIST_TIMEOUT_MS),
        map((r) => r.locationNodeDtoPage?.content ?? r.locationNodeDtoList ?? []),
        catchError((err) => this.emptyOnNotFound<LocationNode>(err)),
      );
  }

  /**
   * Build a fully-qualified API URL for the given entity + operation, picking the right
   * surface (`/api/v1/system/...` vs `/api/v1/frontend/...`) per environment configuration.
   * Entity kinds in {@link FRONTEND_ONLY} always use the frontend surface.
   */
  private url(kind: LocationEntityKind, operation: string): string {
    const surface =
      FRONTEND_ONLY.has(kind) ? 'frontend' : environment.apiSurface;
    return `${this.base}/api/v1/${surface}/${RESOURCE_SEGMENT[kind]}/${operation}`;
  }

  /** location-node has no export/import endpoints in either surface today. */
  private supportsImportExport(kind: LocationEntityKind): boolean {
    return !FRONTEND_ONLY.has(kind);
  }

  private defaultFilterBody(): Record<string, unknown> {
    return { page: 0, size: DEFAULT_PAGE_SIZE };
  }

  private emptyOnNotFound<T>(error: unknown): Observable<T[]> {
    if (error instanceof HttpErrorResponse && error.status === 404) {
      return of([]);
    }
    return throwError(() => error);
  }

  private extractEntity(
    kind: LocationEntityKind,
    response: Record<string, unknown>,
  ): Record<string, unknown> | null {
    const dtoKey = this.dtoKeyFor(kind);
    const dto = response[dtoKey];
    return (dto as Record<string, unknown> | undefined) ?? null;
  }

  private dtoKeyFor(kind: LocationEntityKind): string {
    switch (kind) {
      case 'country':
        return 'countryDto';
      case 'province':
        return 'provinceDto';
      case 'district':
        return 'districtDto';
      case 'suburb':
        return 'suburbDto';
      case 'admin-level':
        return 'administrativeLevelDto';
      case 'city':
      case 'village':
        return 'locationNodeDto';
    }
  }

  private cleanPayload(payload: Record<string, unknown>): Record<string, unknown> {
    return Object.fromEntries(
      Object.entries(payload).filter(([, value]) => {
        if (value === null || value === undefined) return false;
        if (typeof value === 'string') return value.trim().length > 0;
        if (Array.isArray(value)) return value.length > 0;
        return true;
      }),
    );
  }
}
