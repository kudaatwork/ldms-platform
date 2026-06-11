import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, catchError, map, of, tap } from 'rxjs';
import { ldmsServiceUrl } from '../utils/api-url.util';

export interface OrganizationCurrencyContext {
  organizationId?: number;
  organizationName?: string;
  functionalCurrencyCode: string;
  functionalCurrencyName?: string;
  functionalCurrencySymbol?: string;
  functionalCurrencyDecimalPlaces?: number;
  countryDefaultCurrencyCode?: string;
  countryId?: number;
  countryIsoAlpha2?: string;
  activeExchangeRates?: Array<{
    fromCurrencyCode: string;
    toCurrencyCode: string;
    rate: number;
    current?: boolean;
  }>;
}

interface CurrencyContextApiResponse {
  organizationCurrencyContextDto?: OrganizationCurrencyContext;
}

const DEFAULT_CONTEXT: OrganizationCurrencyContext = {
  functionalCurrencyCode: 'USD',
  functionalCurrencySymbol: '$',
  functionalCurrencyName: 'US Dollar',
};

@Injectable({ providedIn: 'root' })
export class CurrencyContextService {
  private readonly base = ldmsServiceUrl('billing-payments', 'billing');
  private readonly contextSubject = new BehaviorSubject<OrganizationCurrencyContext>(DEFAULT_CONTEXT);

  readonly context$ = this.contextSubject.asObservable();

  constructor(private readonly http: HttpClient) {}

  get snapshot(): OrganizationCurrencyContext {
    return this.contextSubject.value;
  }

  get functionalCurrencyCode(): string {
    return this.snapshot.functionalCurrencyCode || 'USD';
  }

  load(): Observable<OrganizationCurrencyContext> {
    return this.http.get<CurrencyContextApiResponse>(`${this.base}/currency-context`).pipe(
      map((res) => res.organizationCurrencyContextDto ?? DEFAULT_CONTEXT),
      tap((ctx) => this.contextSubject.next({ ...DEFAULT_CONTEXT, ...ctx })),
      catchError(() => {
        this.contextSubject.next(DEFAULT_CONTEXT);
        return of(DEFAULT_CONTEXT);
      }),
    );
  }

  refresh(): void {
    this.load().subscribe();
  }

  formatAmount(value: number, currencyCode?: string): string {
    if (!Number.isFinite(value)) {
      return '—';
    }
    const code = (currencyCode ?? this.functionalCurrencyCode).toUpperCase();
    try {
      return new Intl.NumberFormat(undefined, {
        style: 'currency',
        currency: code,
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }).format(value);
    } catch {
      return `${code} ${value.toFixed(2)}`;
    }
  }

  /** Human-readable rate e.g. "1 USD = 26.80 ZWG" */
  describeRate(from: string, to: string, rate: number): string {
    return `1 ${from} = ${rate.toLocaleString(undefined, { maximumFractionDigits: 4 })} ${to}`;
  }
}
