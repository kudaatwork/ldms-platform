import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, forkJoin, from, map, mergeMap, of, throwError } from 'rxjs';
import { filterByGlobalAndColumns } from '@shared/utils/table-search.util';
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
  ImportSummaryResponse,
  LocationNode,
  LocationNodeListResponse,
  LocationEntityKind,
  Province,
  ProvinceListResponse,
  Suburb,
  SuburbListResponse,
} from '../models/location.models';

/** Default page size for location admin tables and list helpers. */
export const LOCATIONS_TABLE_PAGE_SIZE = 20;

const DEFAULT_PAGE_SIZE = LOCATIONS_TABLE_PAGE_SIZE;

/** Query sent to `find-by-multiple-filters` for a single table page. */
export interface LocationTableQuery {
  page: number;
  size: number;
  searchQuery: string;
  columnFilters: Record<string, string>;
}

/** Option row for FK dropdowns (forms / filters). */
export interface LocationSelectOption {
  id: number;
  label: string;
  sublabel?: string;
}

const LOCATION_SELECT_LIST_CAP = 5000;
/** Hard ceiling per list call so the UI never blocks waiting for a stalled service. */

/**
 * Resource path segment for each location entity kind under the location-management service.
 * `city` and `village` both live under the `location-node` resource.
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

/**
 * Sample CSV templates per entity. Headers and column order match the backend importers
 * in `ldms-locations` (see `*ServiceImpl.import*FromCsv` and `CountryCsvDto`). Each template
 * ships with two example rows so users can see exactly what valid data looks like.
 *
 * Notes:
 * - Optional ID columns can be left blank (e.g. `GEOCOORDINATES ID`).
 * - The backend matches columns by header name, so column order in real uploads is flexible
 *   as long as the header row is present and exactly matches the names below.
 * - Cities and villages currently have no server-side CSV importer. Their templates are
 *   provided as a schema reference so admins can prepare data ahead of upload support.
 */
type SampleCsvTemplate = { headers: string[]; rows: string[][]; description: string };
export type LocationActionResponse = { ok: boolean; message?: string };

const SAMPLE_CSV_TEMPLATES: Partial<Record<LocationEntityKind, SampleCsvTemplate>> = {
  country: {
    headers: [
      'NAME',
      'ISO ALPHA-2 CODE',
      'ISO ALPHA-3 CODE',
      'DIAL CODE',
      'TIMEZONE',
      'CURRENCY CODE',
    ],
    rows: [
      ['Zimbabwe', 'ZW', 'ZWE', '+263', 'Africa/Harare', 'USD'],
      ['South Africa', 'ZA', 'ZAF', '+27', 'Africa/Johannesburg', 'ZAR'],
    ],
    description:
      'Each row creates one country. All columns are required: NAME, ISO ALPHA-2 CODE, ' +
      'ISO ALPHA-3 CODE, DIAL CODE, TIMEZONE and CURRENCY CODE. Geo coordinates are ' +
      'generated automatically by the server, so they are not part of this template.',
  },
  province: {
    headers: ['NAME', 'CODE', 'COUNTRY ID', 'ADMINISTRATIVE LEVEL ID'],
    rows: [
      ['Mashonaland West', 'MW', '1', '1'],
      ['Limpopo', 'LP', '2', '1'],
    ],
    description:
      'Each row creates one province under an existing country. NAME and COUNTRY ID are ' +
      'required. CODE and ADMINISTRATIVE LEVEL ID are optional. Use the numeric ID of the ' +
      'parent country exactly as shown in the Countries list. Geo coordinates are generated ' +
      'automatically by the server.',
  },
  district: {
    headers: ['NAME', 'CODE', 'PROVINCE ID', 'ADMINISTRATIVE LEVEL ID'],
    rows: [
      ['Chinhoyi', 'CHY', '1', '2'],
      ['Polokwane', 'POL', '2', '2'],
    ],
    description:
      'Each row creates one district under an existing province. NAME and PROVINCE ID are ' +
      'required. CODE and ADMINISTRATIVE LEVEL ID are optional. Use the numeric ID of the ' +
      'parent province exactly as shown in the Provinces list. Geo coordinates are generated ' +
      'automatically by the server.',
  },
  suburb: {
    headers: ['NAME', 'CODE', 'POSTAL CODE', 'DISTRICT ID', 'ADMINISTRATIVE LEVEL ID'],
    rows: [
      ['Newlands', 'NLD', '0083', '1', '3'],
      ['Sunninghill', 'SNN', '2191', '2', '3'],
    ],
    description:
      'Each row creates one suburb under an existing district. NAME and DISTRICT ID are ' +
      'required. CODE, POSTAL CODE and ADMINISTRATIVE LEVEL ID are optional. Use the numeric ' +
      'ID of the parent district as shown in the Districts list. Geo coordinates are ' +
      'generated automatically by the server.',
  },
  'admin-level': {
    headers: ['NAME', 'CODE', 'LEVEL', 'DESCRIPTION'],
    rows: [
      ['Province', 'ADM1', '1', 'First-level administrative subdivision (province / state)'],
      ['District', 'ADM2', '2', 'Second-level administrative subdivision (district)'],
    ],
    description:
      'Each row creates one administrative level. NAME and LEVEL are required. LEVEL is the ' +
      'numeric depth in the hierarchy (1 = top, e.g. province; 2 = district; 3 = suburb). ' +
      'CODE and DESCRIPTION are optional.',
  },
  city: {
    headers: [
      'NAME',
      'CODE',
      'PARENT ID',
      'LATITUDE',
      'LONGITUDE',
      'TIMEZONE',
      'POSTAL CODE',
      'ALIASES',
    ],
    rows: [
      ['Harare', 'HRE', '1', '-17.8252', '31.0335', 'Africa/Harare', '00263', 'Salisbury;Sunshine City'],
      ['Bulawayo', 'BYO', '2', '-20.1500', '28.5833', 'Africa/Harare', '00264', 'City of Kings'],
    ],
    description:
      'Each row defines one city. NAME is required. PARENT ID is the numeric ID of the parent ' +
      'province or district as shown in those tables; leave blank for top-level cities. ' +
      'LATITUDE / LONGITUDE use decimal degrees. ALIASES is a semicolon-separated list of ' +
      'alternate names. Note: server-side CSV upload for cities is not yet available — use ' +
      'this template to prepare data or as a schema reference.',
  },
  village: {
    headers: [
      'NAME',
      'CODE',
      'PARENT ID',
      'LATITUDE',
      'LONGITUDE',
      'TIMEZONE',
      'POSTAL CODE',
      'ALIASES',
    ],
    rows: [
      ['Domboshava', 'DBV', '7', '-17.6080', '31.1500', 'Africa/Harare', '', 'Domboshawa'],
      ['Murewa', 'MRW', '8', '-17.6500', '31.7833', 'Africa/Harare', '', 'Murehwa'],
    ],
    description:
      'Each row defines one village. NAME is required. PARENT ID is the numeric ID of the ' +
      'parent district (or city) as shown in those tables. LATITUDE / LONGITUDE use decimal ' +
      'degrees. ALIASES is a semicolon-separated list of alternate names. Note: server-side ' +
      'CSV upload for villages is not yet available — use this template to prepare data or as ' +
      'a schema reference.',
  },
};

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
    return this.fetchListWithGatewayFallback<CountryListResponse, Country>(
      'country',
      this.countryFilterBody(),
      'countryDtoPage',
      'countryDtoList',
    );
  }

  getProvinces(): Observable<Province[]> {
    if (environment.useMocks) {
      return of([...MOCK_PROVINCES]);
    }
    return this.fetchListWithGatewayFallback<ProvinceListResponse, Province>(
      'province',
      this.provinceFilterBody(),
      'provinceDtoPage',
      'provinceDtoList',
    );
  }

  getDistricts(): Observable<District[]> {
    if (environment.useMocks) {
      return of([...MOCK_DISTRICTS]);
    }
    return this.fetchListWithGatewayFallback<DistrictListResponse, District>(
      'district',
      this.districtFilterBody(),
      'districtDtoPage',
      'districtDtoList',
    );
  }

  getCities(): Observable<LocationNode[]> {
    return this.getLocationNodesByType('CITY');
  }

  getSuburbs(): Observable<Suburb[]> {
    if (environment.useMocks) {
      return of([...MOCK_SUBURBS]);
    }
    return this.fetchListWithGatewayFallback<SuburbListResponse, Suburb>(
      'suburb',
      this.suburbFilterBody(),
      'suburbDtoPage',
      'suburbDtoList',
    );
  }

  getVillages(): Observable<LocationNode[]> {
    return this.getLocationNodesByType('VILLAGE');
  }

  getAdminLevels(): Observable<AdministrativeLevel[]> {
    if (environment.useMocks) {
      return of([...MOCK_ADMIN_LEVELS]);
    }
    return this.fetchListWithGatewayFallback<AdministrativeLevelListResponse, AdministrativeLevel>(
      'admin-level',
      this.administrativeLevelFilterBody(),
      'administrativeLevelDtoPage',
      'administrativeLevelDtoList',
    );
  }

  /**
   * Loads one page of rows using the backend `find-by-multiple-filters` contract
   * (search value + column fields + pagination).
   */
  queryTablePage(
    kind: LocationEntityKind,
    q: LocationTableQuery,
  ): Observable<{ rows: unknown[]; totalElements: number }> {
    if (environment.useMocks) {
      return of(this.mockTablePage(kind, q));
    }
    const body = this.buildTableFilterBody(kind, q);
    const pageKey = this.dtoPageKeyForTable(kind);
    return this.http.post<unknown>(this.url(kind, 'find-by-multiple-filters'), body).pipe(
      map((r) => {
        const { rows, totalElements } = this.extractPagedResult(r, pageKey);
        return { rows, totalElements };
      }),
      catchError((err) => {
        if (err instanceof HttpErrorResponse && err.status === 404) {
          return of({ rows: [], totalElements: 0 });
        }
        return throwError(() => err);
      }),
    );
  }

  fetchCountriesForSelect(): Observable<LocationSelectOption[]> {
    if (environment.useMocks) {
      return of(
        MOCK_COUNTRIES.map((c) => ({
          id: c.id,
          label: `${c.name} (${c.isoAlpha2Code})`,
        })),
      );
    }
    return this.queryTablePage('country', {
      page: 0,
      size: LOCATION_SELECT_LIST_CAP,
      searchQuery: '',
      columnFilters: {},
    }).pipe(
      map(({ rows }) => this.mapRowsToSelectOptions('country', rows)),
      catchError(() => of([])),
    );
  }

  fetchProvincesForSelect(extraColumnFilters: Record<string, string> = {}): Observable<LocationSelectOption[]> {
    if (environment.useMocks) {
      let rows = [...MOCK_PROVINCES];
      const cid = extraColumnFilters['countryId']?.trim();
      if (cid) {
        const n = Number(cid);
        if (Number.isFinite(n)) {
          rows = rows.filter((p) => Number(p.countryId) === n);
        }
      }
      return of(this.mapRowsToSelectOptions('province', rows as unknown[]));
    }
    return this.queryTablePage('province', {
      page: 0,
      size: LOCATION_SELECT_LIST_CAP,
      searchQuery: '',
      columnFilters: { name: '', code: '', ...extraColumnFilters },
    }).pipe(
      map(({ rows }) => this.mapRowsToSelectOptions('province', rows)),
      catchError(() => of([])),
    );
  }

  fetchDistrictsForSelect(extraColumnFilters: Record<string, string> = {}): Observable<LocationSelectOption[]> {
    if (environment.useMocks) {
      let rows = [...MOCK_DISTRICTS];
      const pid = extraColumnFilters['provinceId']?.trim();
      if (pid) {
        const n = Number(pid);
        if (Number.isFinite(n)) {
          rows = rows.filter((d) => Number(d.provinceId) === n);
        }
      }
      return of(this.mapRowsToSelectOptions('district', rows as unknown[]));
    }
    return this.queryTablePage('district', {
      page: 0,
      size: LOCATION_SELECT_LIST_CAP,
      searchQuery: '',
      columnFilters: { name: '', code: '', ...extraColumnFilters },
    }).pipe(
      map(({ rows }) => this.mapRowsToSelectOptions('district', rows)),
      catchError(() => of([])),
    );
  }

  fetchAdministrativeLevelsForSelect(): Observable<LocationSelectOption[]> {
    if (environment.useMocks) {
      return of(
        MOCK_ADMIN_LEVELS.map((a) => ({
          id: a.id,
          label: `${a.name}${a.level != null ? ` (level ${a.level})` : ''}`,
          sublabel: a.code ?? undefined,
        })),
      );
    }
    return this.queryTablePage('admin-level', {
      page: 0,
      size: LOCATION_SELECT_LIST_CAP,
      searchQuery: '',
      columnFilters: { name: '', code: '', description: '' },
    }).pipe(
      map(({ rows }) => this.mapRowsToSelectOptions('admin-level', rows)),
      catchError(() => of([])),
    );
  }

  /** Parent picker for location nodes: cities and villages (API parent is another node id). */
  fetchLocationNodesForParentSelect(): Observable<LocationSelectOption[]> {
    if (environment.useMocks) {
      const mapNode = (rows: typeof MOCK_CITIES): LocationSelectOption[] =>
        rows.map((r) => ({
          id: r.id,
          label: String(r.name),
          sublabel: [r.locationType, r.parentName ? `under ${r.parentName}` : ''].filter(Boolean).join(' · '),
        }));
      return of([...mapNode(MOCK_CITIES), ...mapNode(MOCK_VILLAGES)].sort((a, b) =>
        a.label.localeCompare(b.label),
      ));
    }
    return forkJoin({
      cities: this.queryTablePage('city', {
        page: 0,
        size: LOCATION_SELECT_LIST_CAP,
        searchQuery: '',
        columnFilters: {},
      }).pipe(
        map((p) => this.mapRowsToSelectOptions('city', p.rows)),
        catchError(() => of([])),
      ),
      villages: this.queryTablePage('village', {
        page: 0,
        size: LOCATION_SELECT_LIST_CAP,
        searchQuery: '',
        columnFilters: {},
      }).pipe(
        map((p) => this.mapRowsToSelectOptions('village', p.rows)),
        catchError(() => of([])),
      ),
    }).pipe(
      map(({ cities, villages }) =>
        [...cities, ...villages].sort((a, b) => a.label.localeCompare(b.label)),
      ),
    );
  }

  private mapRowsToSelectOptions(kind: LocationEntityKind, rows: unknown[]): LocationSelectOption[] {
    const list = rows as Record<string, unknown>[];
    switch (kind) {
      case 'country':
        return list.map((r) => ({
          id: Number(r['id']),
          label: `${r['name'] ?? ''} (${r['isoAlpha2Code'] ?? ''})`.trim(),
        }));
      case 'province':
        return list.map((r) => ({
          id: Number(r['id']),
          label: String(r['name'] ?? ''),
          sublabel: r['code'] ? String(r['code']) : `Country #${r['countryId'] ?? ''}`,
        }));
      case 'district':
        return list.map((r) => ({
          id: Number(r['id']),
          label: String(r['name'] ?? ''),
          sublabel: r['code'] ? String(r['code']) : `Province #${r['provinceId'] ?? ''}`,
        }));
      case 'suburb':
        return list.map((r) => ({
          id: Number(r['id']),
          label: String(r['name'] ?? ''),
          sublabel: r['code'] ? String(r['code']) : `District #${r['districtId'] ?? ''}`,
        }));
      case 'admin-level':
        return list.map((r) => ({
          id: Number(r['id']),
          label: `${r['name'] ?? ''}${r['level'] != null ? ` (level ${r['level']})` : ''}`,
          sublabel: r['code'] ? String(r['code']) : undefined,
        }));
      case 'city':
      case 'village':
        return list.map((r) => ({
          id: Number(r['id']),
          label: String(r['name'] ?? ''),
          sublabel: [String(r['locationType'] ?? ''), r['parentName'] ? `under ${r['parentName']}` : '']
            .filter(Boolean)
            .join(' · '),
        }));
      default:
        return [];
    }
  }

  /** True when any non-deleted province references `countryId` (paged probe, size 1). */
  hasLinkedProvincesForCountry(countryId: number): Observable<boolean> {
    if (environment.useMocks) {
      return this.getProvinces().pipe(
        map((provinces) => provinces.some((p) => Number(p.countryId) === countryId)),
      );
    }
    const body: Record<string, unknown> = {
      page: 0,
      size: 1,
      searchValue: '',
      name: '',
      code: '',
      countryId,
    };
    return this.http.post<unknown>(this.url('province', 'find-by-multiple-filters'), body).pipe(
      map((r) => this.extractPagedResult(r, 'provinceDtoPage').totalElements > 0),
      catchError((err) => {
        if (err instanceof HttpErrorResponse && err.status === 404) {
          return of(false);
        }
        return throwError(() => err);
      }),
    );
  }

  createLocation(
    kind: LocationEntityKind,
    payload: Record<string, unknown>,
  ): Observable<LocationActionResponse> {
    if (environment.useMocks) {
      return of({ ok: true, message: 'Saved successfully.' });
    }
    return this.http
      .post(this.url(kind, 'create'), this.cleanPayload(payload))
      .pipe(map((response) => this.toActionResponse(response, 'Saved successfully.')));
  }

  updateLocation(
    kind: LocationEntityKind,
    id: number,
    payload: Record<string, unknown>,
  ): Observable<LocationActionResponse> {
    if (environment.useMocks) {
      return of({ ok: true, message: 'Updated successfully.' });
    }
    const body = this.cleanPayload({ ...payload, id });
    return this.http
      .put(this.url(kind, 'update'), body)
      .pipe(map((response) => this.toActionResponse(response, 'Updated successfully.')));
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

  findGeoCoordinatesById(
    id: number,
  ): Observable<{ latitude?: number | null; longitude?: number | null } | null> {
    if (environment.useMocks) {
      return of(null);
    }
    const geoUrl = `${this.base}/ldms-locations/v1/${environment.apiSurface}/geo-coordinates/find-by-id/${id}`;
    return this.http.get<Record<string, unknown>>(geoUrl).pipe(
      map((response) => this.extractGeoCoordinatesEntity(response)),
      catchError((err: HttpErrorResponse) => {
        if (err.status === 404) return of(null);
        return throwError(() => err);
      }),
    );
  }

  deleteLocation(kind: LocationEntityKind, id: number): Observable<LocationActionResponse> {
    if (environment.useMocks) {
      return of({ ok: true, message: 'Deleted successfully.' });
    }
    return this.http
      .delete(`${this.url(kind, 'delete-by-id')}/${id}`)
      .pipe(map((response) => this.toActionResponse(response, 'Deleted successfully.')));
  }

  exportLocation(kind: LocationEntityKind, format: 'csv' | 'xlsx' | 'pdf'): Observable<Blob> {
    if (environment.useMocks) {
      return throwError(() => new Error('Export is not available while mocks are enabled.'));
    }
    if (!this.supportsImportExport(kind)) {
      return throwError(() => new Error('Export is not supported for this location type yet.'));
    }
    return this.http
      .post(`${this.url(kind, 'export')}?format=${format}`, this.exportFilterBody(kind), {
        responseType: 'blob',
      })
      .pipe(
        catchError((err: HttpErrorResponse) => {
          const blob: Blob | null = err.error instanceof Blob ? err.error : null;
          if (blob) {
            return from(blob.text()).pipe(
              mergeMap((text) => {
                const message = text || `Export failed with status ${err.status}.`;
                return throwError(() => new Error(message));
              }),
            );
          }
          return throwError(() => err);
        }),
      );
  }

  importLocationCsv(kind: LocationEntityKind, file: File): Observable<LocationActionResponse> {
    if (environment.useMocks) {
      return throwError(() => new Error('Import is not available while mocks are enabled.'));
    }
    if (!this.supportsImportExport(kind)) {
      return throwError(() => new Error('CSV import is not supported for this location type yet.'));
    }
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post(this.url(kind, 'import-csv'), formData)
      .pipe(map((response) => this.toImportResponse(response)));
  }

  /**
   * Returns a short human-readable description of what the sample CSV contains for `kind`,
   * or `null` if no sample is available. Used in the UI next to the "Sample CSV" button so
   * users know what to expect before downloading.
   */
  getSampleCsvDescription(kind: LocationEntityKind): string | null {
    return SAMPLE_CSV_TEMPLATES[kind]?.description ?? null;
  }

  /**
   * Builds a backend-aligned sample CSV for the selected location entity. The output contains:
   *   - a header row matching the importer in `ldms-locations` exactly, and
   *   - two example data rows so users can see what valid input looks like.
   *
   * Returns `null` when no template is defined for the entity. Cities and villages emit a
   * schema-only template (server-side CSV import is not yet wired for those types).
   */
  getSampleCsvTemplate(kind: LocationEntityKind): { blob: Blob; filename: string } | null {
    const template = SAMPLE_CSV_TEMPLATES[kind];
    if (!template) {
      return null;
    }
    const lines = [template.headers, ...template.rows].map((row) =>
      row.map(this.escapeCsvCell).join(','),
    );
    const csvText = `${lines.join('\n')}\n`;
    const filename = `${RESOURCE_SEGMENT[kind]}-sample.csv`;
    return {
      blob: new Blob([csvText], { type: 'text/csv;charset=utf-8' }),
      filename,
    };
  }

  /** RFC 4180 — quote any cell containing `,`, `"`, CR or LF, and double up quotes inside. */
  private escapeCsvCell = (value: string): string => {
    if (value === undefined || value === null) return '';
    const needsQuoting = /[",\r\n]/.test(value);
    const escaped = value.replace(/"/g, '""');
    return needsQuoting ? `"${escaped}"` : escaped;
  };

  private getLocationNodesByType(locationType: 'CITY' | 'VILLAGE'): Observable<LocationNode[]> {
    if (environment.useMocks) {
      return of([...(locationType === 'CITY' ? MOCK_CITIES : MOCK_VILLAGES)]);
    }
    const kind: LocationEntityKind = locationType === 'CITY' ? 'city' : 'village';
    return this.fetchListWithGatewayFallback<LocationNodeListResponse, LocationNode>(
      kind,
      {
        searchValue: '',
        locationType,
        parentId: null,
        page: 0,
        size: DEFAULT_PAGE_SIZE,
      },
      'locationNodeDtoPage',
      'locationNodeDtoList',
    );
  }

  private fetchListWithGatewayFallback<TResponse, TEntity>(
    kind: LocationEntityKind,
    body: Record<string, unknown>,
    dtoPageKey: string,
    dtoListKey: string,
  ): Observable<TEntity[]> {
    return this.http.post<TResponse>(this.url(kind, 'find-by-multiple-filters'), body).pipe(
      map((r) => this.extractEntityRows<TEntity>(r, dtoPageKey, dtoListKey)),
      catchError((err) => this.emptyOnNotFound<TEntity>(err)),
    );
  }

  private extractEntityRows<T>(response: unknown, dtoPageKey: string, dtoListKey: string): T[] {
    // If the response is already an array, use it directly.
    if (Array.isArray(response) && response.length > 0) {
      return response as T[];
    }

    const obj = this.toObj(response);
    if (!obj) return [];

    // Check top-level and data-wrapped variants in priority order.
    const candidates = [obj, this.toObj(obj['data'])];
    for (const src of candidates) {
      if (!src) continue;

      // 1. Page DTO: { <dtoPageKey>: { content: [...] } }
      const page = this.toObj(src[dtoPageKey]);
      if (page) {
        const content = page['content'];
        if (Array.isArray(content) && content.length > 0) return content as T[];
      }

      // 2. List DTO: { <dtoListKey>: [...] }
      const list = src[dtoListKey];
      if (Array.isArray(list) && list.length > 0) return list as T[];

      // 3. Case-insensitive search for any *DtoPage or *DtoList key at this level.
      for (const [key, val] of Object.entries(src)) {
        const lower = key.toLowerCase();
        if (lower.endsWith('dtopage')) {
          const p = this.toObj(val);
          if (p) {
            const c = p['content'];
            if (Array.isArray(c) && c.length > 0) return c as T[];
          }
        }
        if (lower.endsWith('dtolist') && Array.isArray(val) && val.length > 0) {
          return val as T[];
        }
      }
    }

    return [];
  }

  private toObj(value: unknown): Record<string, unknown> | null {
    if (value === null || value === undefined) return null;
    if (Array.isArray(value)) return null;
    if (typeof value === 'object') return value as Record<string, unknown>;
    if (typeof value === 'string') {
      const t = value.trim();
      if (t.startsWith('{') || t.startsWith('[')) {
        try { return JSON.parse(t) as Record<string, unknown>; } catch { /* fall through */ }
      }
    }
    return null;
  }

  private asRecord(value: unknown): Record<string, unknown> | null {
    return value !== null && typeof value === 'object' && !Array.isArray(value)
      ? (value as Record<string, unknown>)
      : null;
  }


  /**
   * Build a fully-qualified API URL for the given entity + operation. The location-management
   * service exposes its routes under `/ldms-locations/v1/{system|frontend}/<resource>/<op>`.
   * The surface is picked per environment configuration.
   */
  private url(kind: LocationEntityKind, operation: string): string {
    return `${this.base}/ldms-locations/v1/${environment.apiSurface}/${RESOURCE_SEGMENT[kind]}/${operation}`;
  }

  /** location-node has no export/import endpoints today. */
  private supportsImportExport(kind: LocationEntityKind): boolean {
    return kind !== 'city' && kind !== 'village';
  }

  private defaultFilterBody(): Record<string, unknown> {
    return { page: 0, size: DEFAULT_PAGE_SIZE };
  }

  private exportFilterBody(kind: LocationEntityKind): Record<string, unknown> {
    const base: Record<string, unknown> = { page: 0, size: 2147483647 };
    switch (kind) {
      case 'country':
        return { ...base, searchValue: '', name: '', isoAlpha2Code: '', isoAlpha3Code: '', dialCode: '', timezone: '', currencyCode: '' };
      case 'province':
        return { ...base, searchValue: '', name: '', code: '' };
      case 'district':
        return { ...base, searchValue: '', name: '', code: '' };
      case 'suburb':
        return { ...base, searchValue: '', name: '', code: '', postalCode: '' };
      case 'admin-level':
        return { ...base, searchValue: '', name: '', code: '', level: null, description: '' };
      default:
        return { ...base, searchValue: '' };
    }
  }

  private countryFilterBody(): Record<string, unknown> {
    return {
      ...this.defaultFilterBody(),
      searchValue: '',
      name: '',
      isoAlpha2Code: '',
      isoAlpha3Code: '',
      dialCode: '',
      timezone: '',
      currencyCode: '',
    };
  }

  private provinceFilterBody(): Record<string, unknown> {
    return {
      ...this.defaultFilterBody(),
      searchValue: '',
      name: '',
      code: '',
    };
  }

  private districtFilterBody(): Record<string, unknown> {
    return {
      ...this.defaultFilterBody(),
      searchValue: '',
      name: '',
      code: '',
    };
  }

  private suburbFilterBody(): Record<string, unknown> {
    return {
      ...this.defaultFilterBody(),
      searchValue: '',
      name: '',
      code: '',
      postalCode: '',
    };
  }

  private administrativeLevelFilterBody(): Record<string, unknown> {
    return {
      ...this.defaultFilterBody(),
      searchValue: '',
      name: '',
      code: '',
      level: null,
      description: '',
    };
  }

  private mockTablePage(
    kind: LocationEntityKind,
    q: LocationTableQuery,
  ): { rows: unknown[]; totalElements: number } {
    const all = this.getMockRowsForKind(kind) as Record<string, unknown>[];
    const filtered = filterByGlobalAndColumns(all, q.searchQuery, q.columnFilters);
    const totalElements = filtered.length;
    const start = q.page * q.size;
    const rows = filtered.slice(start, start + q.size);
    return { rows, totalElements };
  }

  private getMockRowsForKind(kind: LocationEntityKind): unknown[] {
    switch (kind) {
      case 'country':
        return [...MOCK_COUNTRIES];
      case 'province':
        return [...MOCK_PROVINCES];
      case 'district':
        return [...MOCK_DISTRICTS];
      case 'suburb':
        return [...MOCK_SUBURBS];
      case 'admin-level':
        return [...MOCK_ADMIN_LEVELS];
      case 'city':
        return [...MOCK_CITIES];
      case 'village':
        return [...MOCK_VILLAGES];
      default:
        return [];
    }
  }

  private dtoPageKeyForTable(kind: LocationEntityKind): string {
    switch (kind) {
      case 'country':
        return 'countryDtoPage';
      case 'province':
        return 'provinceDtoPage';
      case 'district':
        return 'districtDtoPage';
      case 'suburb':
        return 'suburbDtoPage';
      case 'admin-level':
        return 'administrativeLevelDtoPage';
      case 'city':
      case 'village':
        return 'locationNodeDtoPage';
      default:
        return 'countryDtoPage';
    }
  }

  private buildTableFilterBody(kind: LocationEntityKind, q: LocationTableQuery): Record<string, unknown> {
    const page = q.page;
    const size = q.size;
    const searchValue = q.searchQuery.trim();
    const cf = q.columnFilters;
    const entityStatus = this.parseEntityStatusFilter(cf['entityStatus']);

    switch (kind) {
      case 'country':
        return {
          page,
          size,
          searchValue,
          name: cf['name']?.trim() ?? '',
          isoAlpha2Code: cf['isoAlpha2Code']?.trim() ?? '',
          isoAlpha3Code: cf['isoAlpha3Code']?.trim() ?? '',
          dialCode: cf['dialCode']?.trim() ?? '',
          timezone: cf['timezone']?.trim() ?? '',
          currencyCode: cf['currencyCode']?.trim() ?? '',
          ...(entityStatus != null ? { entityStatus } : {}),
        };
      case 'province': {
        const body: Record<string, unknown> = {
          page,
          size,
          searchValue,
          name: cf['name']?.trim() ?? '',
          code: cf['code']?.trim() ?? '',
        };
        const countryId = this.parseOptionalLong(cf['countryId']);
        if (countryId != null) {
          body['countryId'] = countryId;
        }
        const adminLvl = this.parseOptionalLong(cf['administrativeLevelId']);
        if (adminLvl != null) {
          body['administrativeLevelId'] = adminLvl;
        }
        if (entityStatus != null) {
          body['entityStatus'] = entityStatus;
        }
        return body;
      }
      case 'district': {
        const body: Record<string, unknown> = {
          page,
          size,
          searchValue,
          name: cf['name']?.trim() ?? '',
          code: cf['code']?.trim() ?? '',
        };
        const provinceId = this.parseOptionalLong(cf['provinceId']);
        if (provinceId != null) {
          body['provinceId'] = provinceId;
        }
        const adminLvl = this.parseOptionalLong(cf['administrativeLevelId']);
        if (adminLvl != null) {
          body['administrativeLevelId'] = adminLvl;
        }
        if (entityStatus != null) {
          body['entityStatus'] = entityStatus;
        }
        return body;
      }
      case 'suburb': {
        const body: Record<string, unknown> = {
          page,
          size,
          searchValue,
          name: cf['name']?.trim() ?? '',
          code: cf['code']?.trim() ?? '',
          postalCode: cf['postalCode']?.trim() ?? '',
        };
        const districtId = this.parseOptionalLong(cf['districtId']);
        if (districtId != null) {
          body['districtId'] = districtId;
        }
        const adminLvl = this.parseOptionalLong(cf['administrativeLevelId']);
        if (adminLvl != null) {
          body['administrativeLevelId'] = adminLvl;
        }
        if (entityStatus != null) {
          body['entityStatus'] = entityStatus;
        }
        return body;
      }
      case 'admin-level': {
        const body: Record<string, unknown> = {
          page,
          size,
          searchValue,
          name: cf['name']?.trim() ?? '',
          code: cf['code']?.trim() ?? '',
          description: cf['description']?.trim() ?? '',
        };
        const level = this.parseOptionalInt(cf['level']);
        if (level != null) {
          body['level'] = level;
        }
        if (entityStatus != null) {
          body['entityStatus'] = entityStatus;
        }
        return body;
      }
      case 'city':
        return this.buildLocationNodeFilterBody(page, size, searchValue, cf, 'CITY', entityStatus);
      case 'village':
        return this.buildLocationNodeFilterBody(page, size, searchValue, cf, 'VILLAGE', entityStatus);
      default:
        return { page, size, searchValue: '' };
    }
  }

  private buildLocationNodeFilterBody(
    page: number,
    size: number,
    searchValue: string,
    cf: Record<string, string>,
    locationType: 'CITY' | 'VILLAGE',
    entityStatus: string | undefined,
  ): Record<string, unknown> {
    const body: Record<string, unknown> = {
      page,
      size,
      searchValue,
      locationType,
      name: cf['name']?.trim() ?? '',
      code: cf['code']?.trim() ?? '',
      timezone: cf['timezone']?.trim() ?? '',
      parentName: cf['parentName']?.trim() ?? '',
    };
    if (entityStatus != null) {
      body['entityStatus'] = entityStatus;
    }
    return body;
  }

  private parseEntityStatusFilter(raw: string | undefined): string | undefined {
    const t = raw?.trim().toUpperCase();
    if (t === 'ACTIVE' || t === 'INACTIVE' || t === 'DELETED') {
      return t;
    }
    return undefined;
  }

  private parseOptionalLong(raw: string | undefined): number | null {
    const t = raw?.trim();
    if (!t) {
      return null;
    }
    const n = Number(t);
    return Number.isFinite(n) ? n : null;
  }

  private parseOptionalInt(raw: string | undefined): number | null {
    const t = raw?.trim();
    if (!t) {
      return null;
    }
    const n = parseInt(t, 10);
    return Number.isFinite(n) ? n : null;
  }

  private extractPagedResult(response: unknown, dtoPageKey: string): { rows: unknown[]; totalElements: number } {
    const empty = { rows: [] as unknown[], totalElements: 0 };
    const obj = this.toObj(response);
    if (!obj) {
      return empty;
    }
    const candidates = [obj, this.toObj(obj['data'])].filter(Boolean) as Record<string, unknown>[];
    for (const src of candidates) {
      const direct = this.readSpringPage(src[dtoPageKey]);
      if (direct) {
        return direct;
      }
      // Fallback for endpoints that may return *DtoList instead of *DtoPage.
      const listKey = dtoPageKey.replace(/Page$/, 'List');
      const list = src[listKey];
      if (Array.isArray(list)) {
        return { rows: list, totalElements: list.length };
      }
      for (const [key, val] of Object.entries(src)) {
        if (key.toLowerCase().endsWith('dtopage')) {
          const p = this.readSpringPage(val);
          if (p) {
            return p;
          }
        }
        if (key.toLowerCase().endsWith('dtolist') && Array.isArray(val)) {
          return { rows: val, totalElements: val.length };
        }
      }
    }
    return empty;
  }

  private readSpringPage(val: unknown): { rows: unknown[]; totalElements: number } | null {
    const page = this.toObj(val);
    if (!page) {
      return null;
    }
    const content = page['content'];
    const totalRaw = page['totalElements'];
    const rows = Array.isArray(content) ? content : [];
    const totalElements = typeof totalRaw === 'number' ? totalRaw : rows.length;
    return { rows, totalElements };
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

  private extractGeoCoordinatesEntity(
    response: Record<string, unknown>,
  ): { latitude?: number | null; longitude?: number | null } | null {
    const wrapped = this.asRecord(response['data']);
    const dto =
      (this.asRecord(response['geoCoordinatesDto']) ??
        this.asRecord(wrapped?.['geoCoordinatesDto']) ??
        null);
    if (!dto) {
      return null;
    }
    return {
      latitude:
        typeof dto['latitude'] === 'number' || dto['latitude'] === null
          ? (dto['latitude'] as number | null)
          : undefined,
      longitude:
        typeof dto['longitude'] === 'number' || dto['longitude'] === null
          ? (dto['longitude'] as number | null)
          : undefined,
    };
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

  private toImportResponse(response: unknown): LocationActionResponse {
    const body = response as ImportSummaryResponse | null;
    const base = body?.message || 'Import completed.';
    if (body && typeof body.failed === 'number' && body.failed > 0 && body.errorMessages?.length) {
      const preview = body.errorMessages.slice(0, 3).join('; ');
      const more = body.errorMessages.length > 3 ? ` (+${body.errorMessages.length - 3} more)` : '';
      return { ok: true, message: `${base} Row errors: ${preview}${more}` };
    }
    return { ok: true, message: base };
  }

  private toActionResponse(response: unknown, fallbackMessage: string): LocationActionResponse {
    const body = response as { messageResponse?: string; message?: string; error?: string } | null;
    return {
      ok: true,
      message: body?.messageResponse || body?.message || fallbackMessage,
    };
  }
}
