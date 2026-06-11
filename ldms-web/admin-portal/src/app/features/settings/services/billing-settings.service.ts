import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import type { OrganizationCurrencyContext } from '../../../core/services/currency-context.service';

export interface CurrencyRow {
  id: number;
  code: string;
  name: string;
  symbol?: string | null;
  decimalPlaces?: number;
}

export interface CountryCurrencySettingRow {
  id?: number;
  countryId: number;
  countryName: string;
  countryIsoAlpha2: string;
  baseCurrencyCode: string;
}

export interface OrganizationCurrencySettingRow {
  id?: number;
  organizationId?: number;
  organizationName?: string;
  countryId?: number;
  countryIsoAlpha2?: string;
  functionalCurrencyCode: string;
  countryDefaultCurrencyCode?: string;
}

export interface ExchangeRateRow {
  id: number;
  fromCurrencyCode: string;
  toCurrencyCode: string;
  rate: number;
  effectiveFrom?: string;
  effectiveTo?: string | null;
  current?: boolean;
}

interface CurrencyApiResponse {
  isSuccess?: boolean;
  success?: boolean;
  statusCode?: number;
  message?: string;
  currencyDtoList?: CurrencyRow[];
  countryCurrencySettingDtoList?: CountryCurrencySettingRow[];
  countryCurrencySettingDto?: CountryCurrencySettingRow;
  organizationCurrencySettingDto?: OrganizationCurrencySettingRow;
  organizationCurrencyContextDto?: OrganizationCurrencyContext;
  exchangeRateDtoList?: ExchangeRateRow[];
  exchangeRateDto?: ExchangeRateRow;
  activeExchangeRates?: ExchangeRateRow[];
}

@Injectable({ providedIn: 'root' })
export class BillingSettingsService {
  private readonly frontendBase = ldmsServiceUrl('billing-payments', 'billing');
  private readonly backofficeBase = ldmsServiceUrl('billing-payments', 'billing', undefined, 'backoffice');

  constructor(private readonly http: HttpClient) {}

  private base(adminMode: boolean): string {
    return adminMode ? this.backofficeBase : this.frontendBase;
  }

  listCurrencies(adminMode = false): Observable<CurrencyRow[]> {
    return this.http.get<CurrencyApiResponse>(`${this.base(adminMode)}/currencies`).pipe(
      map((res) => res.currencyDtoList ?? []),
    );
  }

  listCountryCurrencySettings(adminMode = false): Observable<CountryCurrencySettingRow[]> {
    return this.http.get<CurrencyApiResponse>(`${this.base(adminMode)}/country-currency-settings`).pipe(
      map((res) => res.countryCurrencySettingDtoList ?? []),
    );
  }

  saveCountryCurrencySetting(payload: CountryCurrencySettingRow, adminMode = false): Observable<CountryCurrencySettingRow> {
    return this.http.put<CurrencyApiResponse>(`${this.base(adminMode)}/country-currency-settings`, payload).pipe(
      map((res) => res.countryCurrencySettingDto ?? payload),
    );
  }

  getOrganizationCurrencyContext(): Observable<OrganizationCurrencyContext> {
    return this.http.get<CurrencyApiResponse>(`${this.frontendBase}/currency-context`).pipe(
      map((res) => res.organizationCurrencyContextDto ?? { functionalCurrencyCode: 'USD' }),
    );
  }

  saveOrganizationCurrencySetting(payload: {
    functionalCurrencyCode: string;
    organizationName?: string;
    countryId?: number;
    countryIsoAlpha2?: string;
  }): Observable<OrganizationCurrencyContext> {
    return this.http.put<CurrencyApiResponse>(`${this.frontendBase}/organization-currency-setting`, payload).pipe(
      map((res) => res.organizationCurrencyContextDto ?? { functionalCurrencyCode: payload.functionalCurrencyCode }),
    );
  }

  listExchangeRates(adminMode = false): Observable<ExchangeRateRow[]> {
    return this.http.get<CurrencyApiResponse>(`${this.base(adminMode)}/exchange-rates`).pipe(
      map((res) => res.exchangeRateDtoList ?? []),
    );
  }

  createExchangeRate(
    payload: { fromCurrencyCode: string; toCurrencyCode: string; rate: number },
    adminMode = false,
  ): Observable<ExchangeRateRow> {
    return this.http.post<CurrencyApiResponse>(`${this.base(adminMode)}/exchange-rates`, payload).pipe(
      map((res) => res.exchangeRateDto ?? {
        id: 0,
        fromCurrencyCode: payload.fromCurrencyCode,
        toCurrencyCode: payload.toCurrencyCode,
        rate: payload.rate,
      }),
    );
  }
}
