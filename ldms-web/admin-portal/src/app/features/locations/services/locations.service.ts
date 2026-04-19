import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map, of } from 'rxjs';
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
  Province,
  ProvinceListResponse,
  Suburb,
  SuburbListResponse,
} from '../models/location.models';

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
      .get<CountryListResponse>(`${this.base}/api/v1/frontend/country/find-by-list`)
      .pipe(map((r) => r.countryDtoList ?? []));
  }

  getProvinces(): Observable<Province[]> {
    if (environment.useMocks) {
      return of([...MOCK_PROVINCES]);
    }
    return this.http
      .get<ProvinceListResponse>(`${this.base}/api/v1/frontend/province/find-by-list`)
      .pipe(map((r) => r.provinceDtoList ?? []));
  }

  getDistricts(): Observable<District[]> {
    if (environment.useMocks) {
      return of([...MOCK_DISTRICTS]);
    }
    return this.http
      .get<DistrictListResponse>(`${this.base}/api/v1/frontend/district/find-by-list`)
      .pipe(map((r) => r.districtDtoList ?? []));
  }

  getCities(): Observable<LocationNode[]> {
    return this.getLocationNodesByType('CITY');
  }

  getSuburbs(): Observable<Suburb[]> {
    if (environment.useMocks) {
      return of([...MOCK_SUBURBS]);
    }
    return this.http
      .get<SuburbListResponse>(`${this.base}/api/v1/frontend/suburb/find-by-list`)
      .pipe(map((r) => r.suburbDtoList ?? []));
  }

  getVillages(): Observable<LocationNode[]> {
    return this.getLocationNodesByType('VILLAGE');
  }

  getAdminLevels(): Observable<AdministrativeLevel[]> {
    if (environment.useMocks) {
      return of([...MOCK_ADMIN_LEVELS]);
    }
    return this.http
      .get<AdministrativeLevelListResponse>(`${this.base}/api/v1/frontend/administrative-level/find-by-list`)
      .pipe(map((r) => r.administrativeLevelDtoList ?? []));
  }

  private getLocationNodesByType(locationType: 'CITY' | 'VILLAGE'): Observable<LocationNode[]> {
    if (environment.useMocks) {
      return of([...(locationType === 'CITY' ? MOCK_CITIES : MOCK_VILLAGES)]);
    }
    return this.http
      .post<LocationNodeListResponse>(`${this.base}/api/v1/frontend/location-node/find-by-multiple-filters`, {
        locationType,
        page: 0,
        size: 500,
      })
      .pipe(
        map((r) => {
          if (r.locationNodeDtoPage?.content?.length) {
            return r.locationNodeDtoPage.content;
          }
          return r.locationNodeDtoList ?? [];
        }),
      );
  }
}
