import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, ReplaySubject, catchError, forkJoin, from, map, mergeMap, of, share, shareReplay, throwError } from 'rxjs';
import { filterByGlobalAndColumns } from '@shared/utils/table-search.util';
import { environment } from '../../../../environments/environment';
import {
  MOCK_ADMIN_LEVELS,
  MOCK_CITIES,
  MOCK_COUNTRIES,
  MOCK_DISTRICTS,
  MOCK_LANGUAGES,
  MOCK_LOCALIZED_NAMES,
  MOCK_PROVINCES,
  MOCK_SUBURBS,
  MOCK_VILLAGES,
} from '../data/locations-mock-data';
import type {
  Address,
  AddressListResponse,
  AdministrativeLevel,
  AdministrativeLevelListResponse,
  Country,
  CountryListResponse,
  District,
  DistrictListResponse,
  ImportSummaryResponse,
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
/** Smaller page for FK dropdowns when filters narrow results (faster JSON + DB work). */
const LOCATION_SELECT_QUERY_SIZE = 1500;

function hasNonEmptySelectFilters(extraColumnFilters: Record<string, string>): boolean {
  return Object.keys(extraColumnFilters).some((k) => {
    const v = extraColumnFilters[k]?.trim();
    return v != null && v.length > 0;
  });
}

/**
 * Resource path segment for each location entity kind under the location-management service.
 * Cities and villages are first-class resources (Country → Province → District → City → Suburb | Village).
 */
const RESOURCE_SEGMENT: Record<LocationEntityKind, string> = {
  country: 'country',
  province: 'province',
  district: 'district',
  address: 'address',
  suburb: 'suburb',
  'admin-level': 'administrative-level',
  city: 'city',
  village: 'village',
  language: 'language',
  'localized-name': 'localized-name',
};

/**
 * Sample CSV templates per entity. OpenCSV uses header names (case-insensitive); order can vary
 * if the header row matches the backend DTO / importer.
 *
 * Backend alignment (`ldms-locations`):
 * - **Address** `POST …/address/import-csv`: `AddressCsvDto` → LINE 1 (required), LINE 2, POSTAL CODE, SUBURB ID.
 *   Importer always creates `SettlementType.SUBURB` rows (`AddressServiceImpl#importAddressFromCsv`).
 *   `POST …/address/export?format=csv` uses a wider read-only layout (ID, settlement labels, audit fields)—do not reuse that file as an import template without removing extra columns.
 * - **City** `POST …/city/import-csv`: `CityCsvDto` → NAME (required), DISTRICT ID (required), optional CODE,
 *   LATITUDE/LONGITUDE (paired), TIMEZONE, POSTAL CODE; coordinates fall back to the parent district geo when omitted.
 *   **Export** CSV headers from the API are:
 *   ID, NAME, CODE, DISTRICT ID, DISTRICT, PROVINCE, COUNTRY, LATITUDE, LONGITUDE, TIMEZONE, POSTAL CODE, CREATED AT, MODIFIED AT, ENTITY STATUS.
 * - **Village** `POST …/village/import-csv`: `VillageCsvDto` → NAME (required), CITY ID and DISTRICT ID (required),
 *   optional CODE, SUBURB ID, LATITUDE/LONGITUDE (paired; fallback to parent city coordinates, then district geo),
 *   TIMEZONE, POSTAL CODE. Sample columns match `CreateVillageRequest`.
 *   **Export** headers: ID, NAME, CODE, CITY ID, CITY, DISTRICT ID, DISTRICT, PROVINCE, COUNTRY, SUBURB ID, LATITUDE, LONGITUDE, TIMEZONE, POSTAL CODE, CREATED AT, MODIFIED AT, ENTITY STATUS.
 * - **Language** `POST …/language/import-csv`: `LanguageCsvDto` → NAME (required), ISO CODE, NATIVE NAME, IS DEFAULT.
 * - **Localized name** `POST …/localized-name/import-csv`: VALUE, LANGUAGE ID, REFERENCE TYPE, REFERENCE ID (see `LocalizedNameCsvDto`).
 * - **Administrative level** `POST …/administrative-level/import-csv`: NAME + LEVEL + one country column
 *   (COUNTRY_ID, ISO_ALPHA_2, ISO_ALPHA_3, COUNTRY_NAME, or COUNTRY as in export); see `AdministrativeLevelServiceImpl#importAdministrativeLevelFromCsv`.
 */
type SampleCsvTemplate = { headers: string[]; rows: string[][]; description: string };
export type LocationActionResponse = { ok: boolean; message?: string; createdId?: number };

const SAMPLE_CSV_TEMPLATES: Partial<Record<LocationEntityKind, SampleCsvTemplate>> = {
  country: {
    headers: [
      'NAME',
      'ISO ALPHA-2 CODE',
      'ISO ALPHA-3 CODE',
      'DIAL CODE',
      'TIMEZONE',
      'CURRENCY CODE',
      'LATITUDE',
      'LONGITUDE',
      'GEOCOORDINATES ID',
    ],
    rows: [
      ['Zimbabwe', 'ZW', 'ZWE', '+263', 'Africa/Harare', 'USD', '-17.8252', '31.0335', ''],
      ['South Africa', 'ZA', 'ZAF', '+27', 'Africa/Johannesburg', 'ZAR', '-25.7479', '28.2293', ''],
    ],
    description:
      'Each row creates one country. NAME, ISO ALPHA-2 CODE, ISO ALPHA-3 CODE, DIAL CODE, TIMEZONE and ' +
      'CURRENCY CODE are required. LATITUDE and LONGITUDE should both be set: the server saves a GeoCoordinates ' +
      'row first and links it to the country (new id). Leave GEOCOORDINATES ID empty on create; use it only when ' +
      're-linking an existing geo row.',
  },
  province: {
    headers: ['NAME', 'CODE', 'COUNTRY ID', 'ADMINISTRATIVE LEVEL ID', 'LATITUDE', 'LONGITUDE', 'GEO COORDINATES ID'],
    rows: [
      ['Mashonaland West', 'MW', '1', '', '-17.5477', '30.1547', ''],
      ['Limpopo', 'LP', '2', '', '-23.9004', '29.4489', ''],
    ],
    description:
      'Each row creates one province under an existing country. NAME and COUNTRY ID are required. ' +
      'Provide LATITUDE and LONGITUDE (recommended): the server persists GeoCoordinates then links the new id to the province. ' +
      'If both are omitted, coordinates fall back to the parent country’s geo when present. ' +
      'ADMINISTRATIVE LEVEL ID is optional; the server resolves or auto-creates a province-tier level when missing.',
  },
  district: {
    headers: [
      'NAME',
      'CODE',
      'PROVINCE ID',
      'ADMINISTRATIVE LEVEL ID',
      'LATITUDE',
      'LONGITUDE',
      'GEO COORDINATES ID',
    ],
    rows: [
      ['Chinhoyi', 'CHY', '1', '2', '-17.3588', '30.2015', ''],
      ['Polokwane', 'POL', '2', '2', '-23.9004', '29.4489', ''],
    ],
    description:
      'Each row creates one district under an existing province. NAME and PROVINCE ID are required. ' +
      'LATITUDE and LONGITUDE (recommended) create GeoCoordinates and link to the district; if omitted, the server ' +
      'copies the parent province’s coordinates when available. CODE and ADMINISTRATIVE LEVEL ID are optional.',
  },
  suburb: {
    headers: [
      'NAME',
      'CODE',
      'POSTAL CODE',
      'DISTRICT ID',
      'ADMINISTRATIVE LEVEL ID',
      'LATITUDE',
      'LONGITUDE',
      'GEO COORDINATES ID',
    ],
    rows: [
      ['Newlands', 'NLD', '0083', '1', '3', '-17.7833', '30.9333', ''],
      ['Sunninghill', 'SNN', '2191', '2', '3', '-26.0123', '28.0123', ''],
    ],
    description:
      'Each row creates one suburb under an existing district. NAME and DISTRICT ID are required. ' +
      'LATITUDE and LONGITUDE (recommended) create GeoCoordinates and link to the suburb; if omitted, the server ' +
      'copies the parent district’s coordinates when available. CODE, POSTAL CODE and ADMINISTRATIVE LEVEL ID are optional.',
  },
  address: {
    headers: ['LINE 1', 'LINE 2', 'POSTAL CODE', 'SUBURB ID', 'LATITUDE', 'LONGITUDE', 'GEO COORDINATES ID'],
    rows: [
      ['12 Main Road', 'Unit 2', '0001', '3', '-17.7833', '30.9333', ''],
      ['45 First Street', '', '0002', '4', '-26.0123', '28.0123', ''],
    ],
    description:
      'Matches `AddressCsvDto` / `POST …/address/import-csv`. LINE 1 and SUBURB ID are required; each row is SUBURB settlement. ' +
      'LATITUDE and LONGITUDE (recommended) create GeoCoordinates and link to the address; if omitted, the server copies ' +
      'the parent suburb’s coordinates when SUBURB ID resolves. LINE 2, POSTAL CODE and GEO COORDINATES ID are optional.',
  },
  'admin-level': {
    headers: ['NAME', 'CODE', 'LEVEL', 'ISO_ALPHA_2', 'DESCRIPTION'],
    rows: [
      ['Province', 'ADM1', '1', 'ZW', 'First-level administrative subdivision (province / state)'],
      ['District', 'ADM2', '2', 'ZW', 'Second-level administrative subdivision (district)'],
    ],
    description:
      'Each row creates one administrative level scoped to a country. NAME and LEVEL are required. ' +
      'Identify the country with one column: COUNTRY_ID, ISO_ALPHA_2, ISO_ALPHA_3, COUNTRY_NAME, or COUNTRY ' +
      '(COUNTRY matches CSV export, which uses the country display name). Sample uses ISO_ALPHA_2. ' +
      'CODE and DESCRIPTION are optional.',
  },
  city: {
    headers: ['NAME', 'CODE', 'DISTRICT ID', 'LATITUDE', 'LONGITUDE', 'TIMEZONE', 'POSTAL CODE'],
    rows: [
      ['Chinhoyi Town', 'CHT', '1', '-17.3588', '30.2015', 'Africa/Harare', ''],
      ['Kariba Town', 'KRT', '2', '-16.5167', '28.8000', 'Africa/Harare', ''],
    ],
    description:
      'Matches `CityCsvDto` / `POST …/city/import-csv`. NAME and DISTRICT ID are required. CODE, TIMEZONE, and POSTAL CODE are optional. ' +
      'LATITUDE and LONGITUDE must both be set or both omitted; if omitted, the importer copies coordinates from the parent district when available. ' +
      'API CSV export adds ID, DISTRICT, PROVINCE, COUNTRY, audit and status columns.',
  },
  village: {
    headers: [
      'NAME',
      'CODE',
      'CITY ID',
      'DISTRICT ID',
      'SUBURB ID',
      'LATITUDE',
      'LONGITUDE',
      'TIMEZONE',
      'POSTAL CODE',
    ],
    rows: [
      ['Rushinga', 'RSG', '1', '1', '', '-17.6080', '31.1500', 'Africa/Harare', ''],
      ['Nyamapanda', 'NYP', '2', '1', '', '-16.2050', '32.4330', 'Africa/Harare', ''],
    ],
    description:
      'Matches `VillageCsvDto` / `POST …/village/import-csv`. NAME, CITY ID, and DISTRICT ID are required. CODE and SUBURB ID are optional. ' +
      'LATITUDE and LONGITUDE must both be set or both omitted; if omitted, coordinates fall back to the parent city, then the district geo row when present. ' +
      'CITY must belong to DISTRICT ID; SUBURB when set must belong to the same district. ' +
      'Export CSVs include CITY, DISTRICT, PROVINCE, COUNTRY names and audit columns.',
  },
  language: {
    headers: ['NAME', 'ISO CODE', 'NATIVE NAME', 'IS DEFAULT'],
    rows: [
      ['English', 'en', 'English', 'true'],
      ['Shona', 'sn', 'chiShona', 'false'],
    ],
    description:
      'Matches `LanguageCsvDto` / `POST …/language/import-csv`. NAME is required. ISO CODE, NATIVE NAME, ' +
      'and IS DEFAULT are optional; IS DEFAULT accepts true/false.',
  },
  'localized-name': {
    headers: ['VALUE', 'LANGUAGE ID', 'REFERENCE TYPE', 'REFERENCE ID'],
    rows: [
      ['Zimbabwe', '1', 'COUNTRY', '1'],
      ['Mashonaland West', '1', 'PROVINCE', '3'],
    ],
    description:
      'Matches `LocalizedNameCsvDto` / `POST …/localized-name/import-csv`. All columns are required. ' +
      'LANGUAGE ID is the numeric language primary key. REFERENCE TYPE is one of: COUNTRY, PROVINCE, DISTRICT, SUBURB. ' +
      'REFERENCE ID is the numeric id of that entity.',
  },
};

@Injectable({
  providedIn: 'root',
})
export class LocationsService {
  private readonly base = environment.apiUrl;
  private countriesSelectCache$?: Observable<LocationSelectOption[]>;
  private districtsSelectCache$?: Observable<LocationSelectOption[]>;
  private suburbsSelectCache$?: Observable<LocationSelectOption[]>;
  /** Cached tagged rows from `GET …/city|village/find-by-list` (merged for wide pickers). */
  private locationNodesCatalogCache$?: Observable<Record<string, unknown>[]>;
  private parentNodesSelectCache$?: Observable<LocationSelectOption[]>;
  private provincesSelectCache$?: Observable<LocationSelectOption[]>;
  private adminLevelsSelectCache$?: Observable<LocationSelectOption[]>;
  private languagesSelectCache$?: Observable<LocationSelectOption[]>;

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

  getCities(): Observable<Record<string, unknown>[]> {
    if (environment.useMocks) {
      return of(MOCK_CITIES as unknown as Record<string, unknown>[]);
    }
    return this.fetchListWithGatewayFallback<Record<string, unknown>, Record<string, unknown>>(
      'city',
      { ...this.defaultFilterBody(), searchValue: '', name: '', code: '' },
      'cityDtoPage',
      'cityDtoList',
    );
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

  getVillages(): Observable<Record<string, unknown>[]> {
    if (environment.useMocks) {
      return of(MOCK_VILLAGES as unknown as Record<string, unknown>[]);
    }
    return this.fetchListWithGatewayFallback<Record<string, unknown>, Record<string, unknown>>(
      'village',
      { ...this.defaultFilterBody(), searchValue: '', name: '', code: '' },
      'villageDtoPage',
      'villageDtoList',
    );
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
    if (this.countriesSelectCache$) {
      return this.countriesSelectCache$;
    }
    this.countriesSelectCache$ = this.queryTablePage('country', {
      page: 0,
      size: LOCATION_SELECT_LIST_CAP,
      searchQuery: '',
      columnFilters: {},
    }).pipe(
      map(({ rows }) => this.mapRowsToSelectOptions('country', rows)),
      // share() with resetOnError so a failed prefetch retries on the next
      // subscribe instead of permanently caching `[]`.
      share({ connector: () => new ReplaySubject<LocationSelectOption[]>(1), resetOnError: true, resetOnRefCountZero: false }),
      catchError(() => of([])),
    );
    return this.countriesSelectCache$;
  }

  fetchProvincesForSelect(extraColumnFilters: Record<string, string> = {}): Observable<LocationSelectOption[]> {
    if (Object.keys(extraColumnFilters).length === 0 && this.provincesSelectCache$) {
      return this.provincesSelectCache$;
    }
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
    const pageSize = hasNonEmptySelectFilters(extraColumnFilters)
      ? LOCATION_SELECT_QUERY_SIZE
      : LOCATION_SELECT_LIST_CAP;
    const loader$ = this.queryTablePage('province', {
      page: 0,
      size: pageSize,
      searchQuery: '',
      columnFilters: { name: '', code: '', ...extraColumnFilters },
    }).pipe(
      map(({ rows }) => this.mapRowsToSelectOptions('province', rows)),
      catchError(() => of([])),
    );
    if (Object.keys(extraColumnFilters).length === 0) {
      this.provincesSelectCache$ = loader$.pipe(shareReplay(1));
      return this.provincesSelectCache$;
    }
    return loader$;
  }

  fetchDistrictsForSelect(extraColumnFilters: Record<string, string> = {}): Observable<LocationSelectOption[]> {
    if (Object.keys(extraColumnFilters).length === 0 && this.districtsSelectCache$) {
      return this.districtsSelectCache$;
    }
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
    if (hasNonEmptySelectFilters(extraColumnFilters)) {
      return this.queryTablePage('district', {
        page: 0,
        size: LOCATION_SELECT_QUERY_SIZE,
        searchQuery: '',
        columnFilters: { name: '', code: '', ...extraColumnFilters },
      }).pipe(
        map(({ rows }) => this.mapRowsToSelectOptions('district', rows)),
        catchError(() => of([])),
      );
    }
    // Full catalog (e.g. Add City / Village): same mechanism as `fetchCountriesForSelect` —
    // one paged `find-by-multiple-filters` request + shared replay, not GET `find-by-list` first.
    if (Object.keys(extraColumnFilters).length === 0) {
      if (!this.districtsSelectCache$) {
        this.districtsSelectCache$ = this.queryTablePage('district', {
          page: 0,
          size: LOCATION_SELECT_LIST_CAP,
          searchQuery: '',
          columnFilters: { name: '', code: '' },
        }).pipe(
          map(({ rows }) => this.mapRowsToSelectOptions('district', rows)),
          share({
            connector: () => new ReplaySubject<LocationSelectOption[]>(1),
            resetOnError: true,
            resetOnRefCountZero: false,
          }),
          catchError(() => of([])),
        );
      }
      return this.districtsSelectCache$;
    }
    return this.fetchAllRowsByList('district').pipe(
      map((rows) => this.filterRowsBySelectColumns('district', rows, extraColumnFilters)),
      map((rows) => this.mapRowsToSelectOptions('district', rows)),
      catchError(() =>
        this.queryTablePage('district', {
          page: 0,
          size: LOCATION_SELECT_LIST_CAP,
          searchQuery: '',
          columnFilters: { name: '', code: '', ...extraColumnFilters },
        }).pipe(
          map(({ rows }) => this.mapRowsToSelectOptions('district', rows)),
          catchError(() => of([])),
        ),
      ),
    );
  }

  fetchSuburbsForSelect(extraColumnFilters: Record<string, string> = {}): Observable<LocationSelectOption[]> {
    if (Object.keys(extraColumnFilters).length === 0 && this.suburbsSelectCache$) {
      return this.suburbsSelectCache$;
    }
    if (environment.useMocks) {
      let rows = [...MOCK_SUBURBS];
      const districtId = extraColumnFilters['districtId']?.trim();
      if (districtId) {
        const n = Number(districtId);
        if (Number.isFinite(n)) {
          rows = rows.filter((s) => Number(s.districtId) === n);
        }
      }
      return of(this.mapRowsToSelectOptions('suburb', rows as unknown[]));
    }
    if (hasNonEmptySelectFilters(extraColumnFilters)) {
      return this.fetchAllRowsByList('suburb').pipe(
        map((rows) => this.filterRowsBySelectColumns('suburb', rows, extraColumnFilters)),
        map((rows) => this.mapRowsToSelectOptions('suburb', rows)),
        catchError(() =>
          this.queryTablePage('suburb', {
            page: 0,
            size: LOCATION_SELECT_QUERY_SIZE,
            searchQuery: '',
            columnFilters: { name: '', code: '', ...extraColumnFilters },
          }).pipe(
            map(({ rows }) => this.mapRowsToSelectOptions('suburb', rows)),
            catchError(() => of([])),
          ),
        ),
      );
    }
    const loader$ = this.fetchAllRowsByList('suburb').pipe(
      map((rows) => this.filterRowsBySelectColumns('suburb', rows, extraColumnFilters)),
      map((rows) => this.mapRowsToSelectOptions('suburb', rows)),
      catchError(() =>
        this.queryTablePage('suburb', {
          page: 0,
          size: LOCATION_SELECT_LIST_CAP,
          searchQuery: '',
          columnFilters: { name: '', code: '', ...extraColumnFilters },
        }).pipe(
          map(({ rows }) => this.mapRowsToSelectOptions('suburb', rows)),
          catchError(() => of([])),
        ),
      ),
    );
    if (Object.keys(extraColumnFilters).length === 0) {
      this.suburbsSelectCache$ = loader$.pipe(shareReplay(1));
      return this.suburbsSelectCache$;
    }
    return loader$;
  }

  fetchAdministrativeLevelsForSelect(): Observable<LocationSelectOption[]> {
    if (this.adminLevelsSelectCache$) {
      return this.adminLevelsSelectCache$;
    }
    if (environment.useMocks) {
      return of(
        MOCK_ADMIN_LEVELS.map((a) => ({
          id: a.id,
          label: `${a.name}${a.level != null ? ` (level ${a.level})` : ''}`,
          sublabel: a.code ?? undefined,
        })),
      );
    }
    this.adminLevelsSelectCache$ = this.queryTablePage('admin-level', {
      page: 0,
      size: LOCATION_SELECT_LIST_CAP,
      searchQuery: '',
      columnFilters: { name: '', code: '', description: '' },
    }).pipe(
      map(({ rows }) => this.mapRowsToSelectOptions('admin-level', rows)),
      catchError(() => of([])),
      shareReplay(1),
    );
    return this.adminLevelsSelectCache$;
  }

  fetchLanguagesForSelect(): Observable<LocationSelectOption[]> {
    if (this.languagesSelectCache$) {
      return this.languagesSelectCache$;
    }
    if (environment.useMocks) {
      return of(
        MOCK_LANGUAGES.map((l) => ({
          id: l.id,
          label: l.name,
          sublabel: l.isoCode ?? undefined,
        })),
      );
    }
    this.languagesSelectCache$ = this.queryTablePage('language', {
      page: 0,
      size: LOCATION_SELECT_LIST_CAP,
      searchQuery: '',
      columnFilters: { name: '', isoCode: '', nativeName: '', isDefault: '', entityStatus: '' },
    }).pipe(
      map(({ rows }) => this.mapRowsToSelectOptions('language', rows)),
      // Never turn upstream errors into [] before share — that would replay an empty list forever.
      // resetOnError: retry after failed prefetch (e.g. before auth). resetOnRefCountZero: backend
      // returns 404 for empty language pages, so an early [] must not stick once languages exist.
      share({
        connector: () => new ReplaySubject<LocationSelectOption[]>(1),
        resetOnError: true,
        resetOnRefCountZero: true,
      }),
      catchError(() => of([])),
    );
    return this.languagesSelectCache$;
  }

  /** Combined city + village options (e.g. address grid filters) from first-class list endpoints. */
  fetchLocationNodesForParentSelect(): Observable<LocationSelectOption[]> {
    if (this.parentNodesSelectCache$) {
      return this.parentNodesSelectCache$;
    }
    if (environment.useMocks) {
      const cityOpts = this.mapRowsToSelectOptions('city', MOCK_CITIES as unknown[]);
      const villageOpts = this.mapRowsToSelectOptions('village', MOCK_VILLAGES as unknown[]);
      this.parentNodesSelectCache$ = of(
        [...cityOpts, ...villageOpts].sort((a, b) => a.label.localeCompare(b.label)),
      ).pipe(shareReplay(1));
      return this.parentNodesSelectCache$;
    }
    this.parentNodesSelectCache$ = this.fetchLocationNodesCatalogRows().pipe(
      map((rows) => {
        const cityRows = rows.filter((r) => r['_catalogKind'] === 'city');
        const villageRows = rows.filter((r) => r['_catalogKind'] === 'village');
        const cityOpts = this.mapRowsToSelectOptions('city', cityRows as unknown[]);
        const villageOpts = this.mapRowsToSelectOptions('village', villageRows as unknown[]);
        return [...cityOpts, ...villageOpts].sort((a, b) => a.label.localeCompare(b.label));
      }),
      catchError(() => of([])),
      shareReplay(1),
    );
    return this.parentNodesSelectCache$;
  }

  /**
   * Cities and villages in `districtId` for optional-parent dropdowns (Add/Edit City, Village).
   * Uses `find-by-list` + client filter; does not page through `find-by-multiple-filters`.
   */
  fetchLocationNodeParentOptionsForDistrict(
    districtId: number,
    _excludeOwnId?: number | null,
  ): Observable<LocationSelectOption[]> {
    return this.fetchCitiesForSelect({ districtId: String(districtId) }).pipe(
      map((opts) => opts.sort((a, b) => a.label.localeCompare(b.label))),
    );
  }

  /** Single node row as a select option (for merging `find-by-id` when the parent is outside the filtered list). */
  fetchLocationNodeSelectOptionById(id: number): Observable<LocationSelectOption | null> {
    if (environment.useMocks) {
      const row = MOCK_CITIES.find((r) => r.id === id);
      const opt = row ? this.mapRowsToSelectOptions('city', [row as unknown])[0] : null;
      return of(opt ?? null);
    }
    return this.findLocationById('city', id).pipe(
      map((row) => (row ? (this.mapRowsToSelectOptions('city', [row])[0] ?? null) : null)),
    );
  }

  /**
   * Warm the *light* shareReplay-backed FK lists the location form dialogs need
   * upfront. Deliberately excludes `fetchLocationNodesForParentSelect` (combined city +
   * village catalog from `find-by-list`) — that list is large and lazy-loaded where needed.
   */
  prefetchAllDialogOptions(): Observable<void> {
    return forkJoin({
      countries: this.fetchCountriesForSelect(),
      provinces: this.fetchProvincesForSelect(),
      districts: this.fetchDistrictsForSelect(),
      suburbs: this.fetchSuburbsForSelect(),
      adminLevels: this.fetchAdministrativeLevelsForSelect(),
      languages: this.fetchLanguagesForSelect(),
    }).pipe(map(() => void 0));
  }

  fetchCitiesForSelect(extraColumnFilters: Record<string, string> = {}): Observable<LocationSelectOption[]> {
    if (environment.useMocks) {
      let rows = [...MOCK_CITIES];
      const districtId = extraColumnFilters['districtId']?.trim();
      if (districtId) {
        const n = Number(districtId);
        if (Number.isFinite(n)) {
          rows = rows.filter((c) => Number(c['districtId']) === n);
        }
      }
      return of(this.mapRowsToSelectOptions('city', rows as unknown[]));
    }
    const pageSize = hasNonEmptySelectFilters(extraColumnFilters)
      ? LOCATION_SELECT_QUERY_SIZE
      : LOCATION_SELECT_LIST_CAP;
    return this.queryTablePage('city', {
      page: 0,
      size: pageSize,
      searchQuery: '',
      columnFilters: { ...extraColumnFilters },
    }).pipe(
      map(({ rows }) => this.mapRowsToSelectOptions('city', rows)),
      catchError(() => of([])),
    );
  }

  fetchVillagesForSelect(extraColumnFilters: Record<string, string> = {}): Observable<LocationSelectOption[]> {
    if (environment.useMocks) {
      let rows = [...MOCK_VILLAGES];
      const districtId = extraColumnFilters['districtId']?.trim();
      if (districtId) {
        const n = Number(districtId);
        if (Number.isFinite(n)) {
          rows = rows.filter((v) => Number(v['districtId']) === n);
        }
      }
      return of(this.mapRowsToSelectOptions('village', rows as unknown[]));
    }
    const pageSize = hasNonEmptySelectFilters(extraColumnFilters)
      ? LOCATION_SELECT_QUERY_SIZE
      : LOCATION_SELECT_LIST_CAP;
    return this.queryTablePage('village', {
      page: 0,
      size: pageSize,
      searchQuery: '',
      columnFilters: { ...extraColumnFilters },
    }).pipe(
      map(({ rows }) => this.mapRowsToSelectOptions('village', rows)),
      catchError(() => of([])),
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
          sublabel: r['code']
            ? String(r['code'])
            : (r['countryName'] != null && String(r['countryName']).trim() !== ''
                ? String(r['countryName'])
                : `Country #${r['countryId'] ?? ''}`),
        }));
      case 'district':
        return list.map((r) => ({
          id: Number(r['id']),
          label: String(r['name'] ?? ''),
          sublabel: r['code']
            ? String(r['code'])
            : (r['provinceName'] != null && String(r['provinceName']).trim() !== ''
                ? String(r['provinceName'])
                : `Province #${r['provinceId'] ?? ''}`),
        }));
      case 'suburb':
        return list.map((r) => ({
          id: Number(r['id']),
          label: String(r['name'] ?? ''),
          sublabel: r['code']
            ? String(r['code'])
            : (r['districtName'] != null && String(r['districtName']).trim() !== ''
                ? String(r['districtName'])
                : `District #${r['districtId'] ?? ''}`),
        }));
      case 'address':
        return list.map((r) => ({
          id: Number(r['id']),
          label: String(r['line1'] ?? ''),
          sublabel: String(r['postalCode'] ?? ''),
        }));
      case 'admin-level':
        return list.map((r) => ({
          id: Number(r['id']),
          label: `${r['name'] ?? ''}${r['level'] != null ? ` (level ${r['level']})` : ''}`,
          sublabel: r['code'] ? String(r['code']) : undefined,
        }));
      case 'city':
        return list.map((r) => ({
          id: Number(r['id']),
          label: String(r['name'] ?? ''),
          sublabel:
            r['districtName'] != null && String(r['districtName']).trim() !== ''
              ? String(r['districtName'])
              : r['districtId'] != null
                ? `District #${r['districtId']}`
                : undefined,
        }));
      case 'village':
        return list.map((r) => ({
          id: Number(r['id']),
          label: String(r['name'] ?? ''),
          sublabel:
            r['cityName'] != null && String(r['cityName']).trim() !== ''
              ? String(r['cityName'])
              : r['cityId'] != null
                ? `City #${r['cityId']}`
                : undefined,
        }));
      case 'language':
        return list.map((r) => ({
          id: Number(r['id']),
          label: String(r['name'] ?? ''),
          sublabel: r['isoCode'] != null && String(r['isoCode']).trim() !== '' ? String(r['isoCode']) : undefined,
        }));
      case 'localized-name':
        return list.map((r) => ({
          id: Number(r['id']),
          label: String(r['value'] ?? ''),
          sublabel:
            r['referenceType'] != null && r['referenceId'] != null
              ? `${String(r['referenceType'])} #${r['referenceId']}`
              : undefined,
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
      .pipe(
        map((response) => {
          const base = this.toActionResponse(response, 'Saved successfully.');
          const createdId = base.ok ? this.extractCreatedEntityId(kind, response) : undefined;
          return { ...base, createdId };
        }),
      );
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
   * Builds a sample CSV for download. **Address** headers match `AddressCsvDto` (including optional geo columns).
   * **City** template matches `CityCsvDto` / `CreateCityRequest`. **Village** template matches `VillageCsvDto` / `CreateVillageRequest`.
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

  private fetchAllRowsByList(kind: Exclude<LocationEntityKind, 'city' | 'village' | 'address'>): Observable<unknown[]> {
    const dtoPageKey = this.dtoPageKeyForTable(kind);
    const dtoListKey = dtoPageKey.replace(/Page$/, 'List');
    return this.http.get<unknown>(this.url(kind, 'find-by-list')).pipe(
      map((r) => this.extractEntityRows<unknown>(r, dtoPageKey, dtoListKey)),
      catchError((err) => this.emptyOnNotFound<unknown>(err)),
    );
  }

  /** Cities + villages from first-class `GET …/city|village/find-by-list` (shared cache). */
  private fetchLocationNodesCatalogRows(): Observable<Record<string, unknown>[]> {
    if (environment.useMocks) {
      const tagged = [
        ...(MOCK_CITIES as unknown as Record<string, unknown>[]).map((r) => ({ ...r, _catalogKind: 'city' as const })),
        ...(MOCK_VILLAGES as unknown as Record<string, unknown>[]).map((r) => ({
          ...r,
          _catalogKind: 'village' as const,
        })),
      ];
      return of(tagged);
    }
    if (!this.locationNodesCatalogCache$) {
      this.locationNodesCatalogCache$ = forkJoin({
        cities: this.http.get<unknown>(this.url('city', 'find-by-list')),
        villages: this.http.get<unknown>(this.url('village', 'find-by-list')),
      }).pipe(
        map(({ cities, villages }) => {
          const cRows = this.extractEntityRows<Record<string, unknown>>(cities, 'cityDtoPage', 'cityDtoList');
          const vRows = this.extractEntityRows<Record<string, unknown>>(villages, 'villageDtoPage', 'villageDtoList');
          return [
            ...cRows.map((r) => ({ ...r, _catalogKind: 'city' as const })),
            ...vRows.map((r) => ({ ...r, _catalogKind: 'village' as const })),
          ];
        }),
        catchError(() => of([])),
        share({
          connector: () => new ReplaySubject<Record<string, unknown>[]>(1),
          resetOnError: true,
          resetOnRefCountZero: false,
        }),
      );
    }
    return this.locationNodesCatalogCache$;
  }

  private filterRowsBySelectColumns(
    kind: 'district' | 'suburb',
    rows: unknown[],
    extraColumnFilters: Record<string, string>,
  ): unknown[] {
    if (!rows.length) {
      return rows;
    }
    const list = rows as Record<string, unknown>[];
    const districtIdFilter = extraColumnFilters['districtId']?.trim();
    const provinceIdFilter = extraColumnFilters['provinceId']?.trim();
    const districtId = districtIdFilter ? Number(districtIdFilter) : null;
    const provinceId = provinceIdFilter ? Number(provinceIdFilter) : null;

    if (kind === 'district' && Number.isFinite(provinceId as number)) {
      return list.filter((r) => Number(r['provinceId']) === provinceId);
    }
    if (kind === 'suburb' && Number.isFinite(districtId as number)) {
      return list.filter((r) => Number(r['districtId']) === districtId);
    }
    return list;
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
    return true;
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
      case 'city':
        return { ...base, searchValue: '', name: '', code: '' };
      case 'village':
        return { ...base, searchValue: '', name: '', code: '' };
      case 'language':
        return { ...base, searchValue: '', name: '', isoCode: '', nativeName: '' };
      case 'localized-name':
        return { ...base, searchValue: '', value: '', referenceType: '' };
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
      case 'address':
        return [];
      case 'suburb':
        return [...MOCK_SUBURBS];
      case 'admin-level':
        return [...MOCK_ADMIN_LEVELS];
      case 'city':
        return [...MOCK_CITIES];
      case 'village':
        return [...MOCK_VILLAGES];
      case 'language':
        return [...MOCK_LANGUAGES];
      case 'localized-name':
        return [...MOCK_LOCALIZED_NAMES];
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
      case 'address':
        return 'addressDtoPage';
      case 'suburb':
        return 'suburbDtoPage';
      case 'admin-level':
        return 'administrativeLevelDtoPage';
      case 'city':
        return 'cityDtoPage';
      case 'village':
        return 'villageDtoPage';
      case 'language':
        return 'languageDtoPage';
      case 'localized-name':
        return 'localizedNameDtoPage';
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
      case 'address': {
        const body: Record<string, unknown> = {
          page,
          size,
          searchValue,
          line1: cf['line1']?.trim() ?? '',
          line2: cf['line2']?.trim() ?? '',
          postalCode: cf['postalCode']?.trim() ?? '',
        };
        const settlementType = cf['settlementType']?.trim().toUpperCase();
        if (settlementType === 'SUBURB' || settlementType === 'VILLAGE') {
          body['settlementType'] = settlementType;
        }
        const settlementId = this.parseOptionalLong(cf['settlementId']);
        if (settlementId != null) body['settlementId'] = settlementId;
        const suburbId = this.parseOptionalLong(cf['suburbId']);
        if (suburbId != null) body['suburbId'] = suburbId;
        const villageId = this.parseOptionalLong(cf['villageId']);
        if (villageId != null) body['villageId'] = villageId;
        const cityId = this.parseOptionalLong(cf['cityId']);
        if (cityId != null) body['cityId'] = cityId;
        const districtId = this.parseOptionalLong(cf['districtId']);
        if (districtId != null) body['districtId'] = districtId;
        const provinceId = this.parseOptionalLong(cf['provinceId']);
        if (provinceId != null) body['provinceId'] = provinceId;
        const countryId = this.parseOptionalLong(cf['countryId']);
        if (countryId != null) body['countryId'] = countryId;
        if (entityStatus != null) body['entityStatus'] = entityStatus;
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
        const countryId = this.parseOptionalLong(cf['countryId']);
        if (countryId != null) {
          body['countryId'] = countryId;
        }
        if (entityStatus != null) {
          body['entityStatus'] = entityStatus;
        }
        return body;
      }
      case 'city': {
        const body: Record<string, unknown> = {
          page,
          size,
          searchValue,
          name: cf['name']?.trim() ?? '',
          code: cf['code']?.trim() ?? '',
        };
        const districtId = this.parseOptionalLong(cf['districtId']);
        if (districtId != null) {
          body['districtId'] = districtId;
        }
        if (entityStatus != null) {
          body['entityStatus'] = entityStatus;
        }
        return body;
      }
      case 'village': {
        const body: Record<string, unknown> = {
          page,
          size,
          searchValue,
          name: cf['name']?.trim() ?? '',
          code: cf['code']?.trim() ?? '',
        };
        const districtId = this.parseOptionalLong(cf['districtId']);
        if (districtId != null) {
          body['districtId'] = districtId;
        }
        const cityId = this.parseOptionalLong(cf['cityId']);
        if (cityId != null) {
          body['cityId'] = cityId;
        }
        if (entityStatus != null) {
          body['entityStatus'] = entityStatus;
        }
        return body;
      }
      case 'language': {
        const body: Record<string, unknown> = {
          page,
          size,
          searchValue,
          name: cf['name']?.trim() ?? '',
          isoCode: cf['isoCode']?.trim() ?? '',
          nativeName: cf['nativeName']?.trim() ?? '',
        };
        const isDef = cf['isDefault']?.trim().toLowerCase();
        if (isDef === 'true' || isDef === 'false') {
          body['isDefault'] = isDef === 'true';
        }
        if (entityStatus != null) {
          body['entityStatus'] = entityStatus;
        }
        return body;
      }
      case 'localized-name': {
        const body: Record<string, unknown> = {
          page,
          size,
          searchValue,
          value: cf['value']?.trim() ?? '',
          referenceType: cf['referenceType']?.trim().toUpperCase() ?? '',
        };
        const languageId = this.parseOptionalLong(cf['languageId']);
        if (languageId != null) {
          body['languageId'] = languageId;
        }
        if (entityStatus != null) {
          body['entityStatus'] = entityStatus;
        }
        return body;
      }
      default:
        return { page, size, searchValue: '' };
    }
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
    const rows = Array.isArray(content) ? content : [];
    let totalElements = this.coerceNonNegativeInt(page['totalElements']);
    if (totalElements == null) {
      const totalPages = this.coerceNonNegativeInt(page['totalPages']);
      const size = this.coerceNonNegativeInt(page['size']);
      if (totalPages != null && size != null && totalPages > 0) {
        totalElements = (totalPages - 1) * size + rows.length;
      } else {
        totalElements = rows.length;
      }
    }
    return { rows, totalElements };
  }

  /** Parses id from create/find response DTO (handles string ids from JSON). */
  private extractCreatedEntityId(kind: LocationEntityKind, response: unknown): number | undefined {
    const body = this.asRecord(response);
    if (!body) {
      return undefined;
    }
    const candidates = [body, this.asRecord(body['data'])].filter(Boolean) as Record<string, unknown>[];
    for (const src of candidates) {
      const key = this.dtoKeyFor(kind);
      const dto = this.asRecord(src[key]);
      const rawId = dto?.['id'];
      const id = this.coerceNonNegativeInt(rawId);
      if (id != null && id > 0) {
        return id;
      }
    }
    return undefined;
  }

  private coerceNonNegativeInt(raw: unknown): number | null {
    if (typeof raw === 'number' && Number.isFinite(raw)) {
      return Math.max(0, Math.floor(raw));
    }
    if (typeof raw === 'string' && raw.trim() !== '') {
      const n = parseInt(raw, 10);
      return Number.isFinite(n) ? Math.max(0, n) : null;
    }
    return null;
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
    const wrapped = this.asRecord(response['data']);
    const src = wrapped ?? response;
    const dto = src[dtoKey];
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
      case 'address':
        return 'addressDto';
      case 'city':
        return 'cityDto';
      case 'village':
        return 'villageDto';
      case 'language':
        return 'languageDto';
      case 'localized-name':
        return 'localizedNameDto';
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
    if (!body) {
      return { ok: false, message: 'Import response was empty.' };
    }
    const base = body.message || 'Import completed.';
    const statusCode = typeof body.statusCode === 'number' ? body.statusCode : undefined;
    const ok = body.isSuccess !== false && (statusCode === undefined || statusCode < 400);
    let message = base;
    if (typeof body.failed === 'number' && body.failed > 0 && body.errorMessages?.length) {
      const preview = body.errorMessages.slice(0, 3).join('; ');
      const more = body.errorMessages.length > 3 ? ` (+${body.errorMessages.length - 3} more)` : '';
      message = `${base} Row errors: ${preview}${more}`;
    }
    return { ok, message };
  }

  private toActionResponse(response: unknown, fallbackMessage: string): LocationActionResponse {
    const body = this.asRecord(response);
    if (!body) {
      return { ok: true, message: fallbackMessage };
    }
    const statusCode = typeof body['statusCode'] === 'number' ? body['statusCode'] : undefined;
    const successFlag = body['success'];
    const isSuccessFlag = body['isSuccess'];
    const failed =
      successFlag === false ||
      isSuccessFlag === false ||
      (statusCode !== undefined && statusCode >= 400);
    const ok = !failed;

    let message =
      (typeof body['message'] === 'string' ? body['message'] : '') ||
      (typeof body['messageResponse'] === 'string' ? body['messageResponse'] : '') ||
      (typeof body['error'] === 'string' ? body['error'] : '') ||
      fallbackMessage;

    const errList = body['errorMessages'];
    if (!ok && Array.isArray(errList) && errList.length > 0) {
      const parts = errList.filter((e): e is string => typeof e === 'string');
      if (parts.length) {
        message = [message, ...parts].filter((s) => s.length > 0).join(' ');
      }
    }

    return { ok, message };
  }
}
