import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ldmsApiUrl, ldmsServiceUrl } from '../../../core/utils/api-url.util';
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

  // ── Payment methods ───────────────────────────────────────────────────────

  /**
   * POST /billing/payments — submit a payment with optional proof-of-payment file.
   * Maps to {@code ldms-billing-payments/v1/frontend/billing/payments}.
   */
  submitPayment(
    payload: {
      purchaseOrderId: number;
      amount: number;
      currency: string;
      paymentMethod: string;
      referenceNumber: string;
      notes?: string;
      proofSource: 'SYSTEM_GENERATED' | 'EXTERNAL_UPLOAD';
      proofDocumentId?: number;
    },
  ): Observable<{ id: number; status: string; message: string }> {
    return this.http.post<Record<string, unknown>>(`${this.frontendBase}/payments/procurement`, payload).pipe(
      map((res) => {
        const payment = (res['paymentDto'] as Record<string, unknown> | undefined) ?? res;
        return {
          id: Number(payment['id'] ?? res['id'] ?? 0),
          status: String(payment['status'] ?? res['status'] ?? 'PENDING'),
          message: String(res['message'] ?? 'Payment submitted.'),
        };
      }),
    );
  }

  /** Upload payment proof document for procurement payments. */
  uploadPaymentProof(organizationId: number, file: File): Observable<number> {
    const form = new FormData();
    form.append('files', file, file.name);
    form.append(
      'fileUploadRequest',
      JSON.stringify({
        ownerType: 'ORGANIZATION',
        ownerId: organizationId,
        filesMetadata: [{ fileType: 'OTHER' }],
      }),
    );
    return this.http.post<Record<string, unknown>>(`${ldmsApiUrl('/ldms-file-upload-service/v1/frontend/file-upload')}/upload`, form).pipe(
      map((res) => {
        const dto = (res['fileUploadDto'] as Record<string, unknown> | undefined) ?? res;
        const id = Number(dto['id'] ?? 0);
        if (!id) {
          throw new Error('File upload service did not return a file id.');
        }
        return id;
      }),
    );
  }

  /** POST /billing/payments/{id}/verify — verify/confirm a submitted payment. */
  verifyPayment(paymentId: number): Observable<void> {
    return this.http
      .post<unknown>(`${this.frontendBase}/payments/${paymentId}/verify`, {})
      .pipe(map(() => undefined));
  }
}
