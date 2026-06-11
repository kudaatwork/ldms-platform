import { ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, catchError, finalize, forkJoin, of, takeUntil } from 'rxjs';
import {
  CurrencyContextService,
  type OrganizationCurrencyContext,
} from '../../../../core/services/currency-context.service';
import { OrgContextService } from '../../../../core/services/org-context.service';
import { LocationsService } from '../../../locations/services/locations.service';
import type { Country } from '../../../locations/models/location.models';
import {
  BillingSettingsService,
  type CountryCurrencySettingRow,
  type CurrencyRow,
  type ExchangeRateRow,
} from '../../services/billing-settings.service';

type CurrencyTab = 'guide' | 'organization' | 'countries' | 'rates';

interface CountryCurrencyRow {
  countryId: number;
  countryName: string;
  countryIsoAlpha2: string;
  defaultCurrencyCode: string;
  baseCurrencyCode: string;
  saving: boolean;
}

@Component({
  selector: 'app-settings-currency',
  templateUrl: './settings-currency.component.html',
  styleUrl: './settings-currency.component.scss',
  standalone: false,
})
export class SettingsCurrencyComponent implements OnInit, OnDestroy {
  @Input() adminMode = false;

  loading = true;
  savingRate = false;
  savingOrg = false;
  applyingZwPreset = false;
  error = '';

  activeTab: CurrencyTab = 'guide';
  currencies: CurrencyRow[] = [];
  countryRows: CountryCurrencyRow[] = [];
  filteredCountryRows: CountryCurrencyRow[] = [];
  exchangeRates: ExchangeRateRow[] = [];
  countrySearch = '';
  countryPage = 0;
  readonly countryPageSize = 25;

  functionalCurrencyCode = 'USD';
  orgCountryId: number | null = null;
  orgCountryIso: string | null = null;
  countryDefaultCurrency = 'USD';

  newRateFrom = 'USD';
  newRateTo = 'ZWG';
  newRateValue = 26.8;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly billing: BillingSettingsService,
    private readonly currencyContext: CurrencyContextService,
    readonly orgContext: OrgContextService,
    private readonly locations: LocationsService,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.activeTab = this.adminMode ? 'guide' : 'organization';
    this.reload();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get tabs(): Array<{ id: CurrencyTab; label: string; icon: string }> {
    if (this.adminMode) {
      return [
        { id: 'guide', label: 'How it works', icon: 'menu_book' },
        { id: 'countries', label: 'Country defaults', icon: 'public' },
        { id: 'rates', label: 'Exchange rates', icon: 'currency_exchange' },
      ];
    }
    return [
      { id: 'guide', label: 'How it works', icon: 'menu_book' },
      { id: 'organization', label: 'Your organisation', icon: 'corporate_fare' },
      { id: 'rates', label: 'Exchange rates', icon: 'currency_exchange' },
    ];
  }

  selectTab(tab: CurrencyTab): void {
    this.activeTab = tab;
    this.cdr.markForCheck();
  }

  reload(): void {
    this.loading = true;
    this.error = '';

    forkJoin({
      currencies: this.billing.listCurrencies(this.adminMode).pipe(catchError(() => of([] as CurrencyRow[]))),
      settings: this.billing.listCountryCurrencySettings(this.adminMode).pipe(
        catchError(() => of([] as CountryCurrencySettingRow[])),
      ),
      rates: this.billing.listExchangeRates(this.adminMode).pipe(catchError(() => of([] as ExchangeRateRow[]))),
      countries: this.locations.getAllCountries().pipe(catchError(() => of([] as Country[]))),
      orgContext: this.adminMode
        ? of(null as OrganizationCurrencyContext | null)
        : this.billing.getOrganizationCurrencyContext().pipe(
            catchError(() => of({ functionalCurrencyCode: 'USD' } as OrganizationCurrencyContext)),
          ),
    })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
          this.applyCountryFilter();
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: ({ currencies, settings, rates, countries, orgContext }) => {
          this.currencies = currencies;
          this.exchangeRates = rates;
          const settingsByCountry = new Map(settings.map((s) => [s.countryId, s]));
          this.countryRows = countries.map((country) => {
            const saved = settingsByCountry.get(country.id);
            return {
              countryId: country.id,
              countryName: country.name,
              countryIsoAlpha2: country.isoAlpha2Code,
              defaultCurrencyCode: country.currencyCode ?? 'USD',
              baseCurrencyCode: saved?.baseCurrencyCode ?? country.currencyCode ?? 'USD',
              saving: false,
            };
          });

          if (!this.adminMode && orgContext) {
            this.functionalCurrencyCode = orgContext.functionalCurrencyCode ?? 'USD';
            this.countryDefaultCurrency = orgContext.countryDefaultCurrencyCode ?? 'USD';
            this.orgCountryId = orgContext.countryId ?? null;
            this.orgCountryIso = orgContext.countryIsoAlpha2 ?? null;
          }
        },
        error: () => {
          this.error = 'Could not load currency settings.';
        },
      });
  }

  saveOrganizationCurrency(): void {
    this.savingOrg = true;
    this.billing
      .saveOrganizationCurrencySetting({
        functionalCurrencyCode: this.functionalCurrencyCode,
        organizationName: this.orgContext.organizationName,
        countryId: this.orgCountryId ?? undefined,
        countryIsoAlpha2: this.orgCountryIso ?? undefined,
      })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.savingOrg = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          this.currencyContext.refresh();
          this.snackBar.open(
            `Functional currency set to ${this.functionalCurrencyCode}. Inventory prices and purchase documents will use this currency.`,
            'Close',
            { duration: 5000 },
          );
        },
        error: () => {
          this.snackBar.open('Failed to save organisation currency', 'Close', { duration: 4000 });
        },
      });
  }

  saveCountryBaseCurrency(row: CountryCurrencyRow): void {
    row.saving = true;
    this.billing
      .saveCountryCurrencySetting({
        countryId: row.countryId,
        countryName: row.countryName,
        countryIsoAlpha2: row.countryIsoAlpha2,
        baseCurrencyCode: row.baseCurrencyCode,
      }, this.adminMode)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          row.saving = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          this.snackBar.open(`Country default updated: ${row.countryName} → ${row.baseCurrencyCode}`, 'Close', {
            duration: 3500,
          });
        },
        error: () => {
          this.snackBar.open(`Failed to save ${row.countryName}`, 'Close', { duration: 4000 });
        },
      });
  }

  applyZimbabwePreset(): void {
    const zw = this.countryRows.find((r) => r.countryIsoAlpha2 === 'ZW');
    if (!zw) {
      this.snackBar.open('Zimbabwe (ZW) not found in country list', 'Close', { duration: 4000 });
      return;
    }
    this.applyingZwPreset = true;
    zw.baseCurrencyCode = 'USD';
    this.billing
      .saveCountryCurrencySetting({
        countryId: zw.countryId,
        countryName: zw.countryName,
        countryIsoAlpha2: zw.countryIsoAlpha2,
        baseCurrencyCode: 'USD',
      }, true)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.applyingZwPreset = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          const hasUsdZwg = this.exchangeRates.some(
            (r) => r.fromCurrencyCode === 'USD' && r.toCurrencyCode === 'ZWG' && r.current,
          );
          if (!hasUsdZwg) {
            this.billing.createExchangeRate({ fromCurrencyCode: 'USD', toCurrencyCode: 'ZWG', rate: 26.8 }, true).subscribe({
              next: () => this.reload(),
            });
          }
          this.snackBar.open('Zimbabwe preset applied: base USD, 1 USD = 26.8 ZWG', 'Close', { duration: 5000 });
          this.reload();
        },
        error: () => this.snackBar.open('Failed to apply Zimbabwe preset', 'Close', { duration: 4000 }),
      });
  }

  addExchangeRate(): void {
    if (!this.newRateFrom || !this.newRateTo || this.newRateValue <= 0) {
      this.snackBar.open('Enter valid currencies and a positive rate', 'Close', { duration: 3000 });
      return;
    }
    this.savingRate = true;
    this.billing
      .createExchangeRate({
        fromCurrencyCode: this.newRateFrom.toUpperCase(),
        toCurrencyCode: this.newRateTo.toUpperCase(),
        rate: this.newRateValue,
      }, this.adminMode)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.savingRate = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          this.currencyContext.refresh();
          this.snackBar.open(
            `Rate saved: 1 ${this.newRateFrom} = ${this.newRateValue} ${this.newRateTo}. New transactions only.`,
            'Close',
            { duration: 5000 },
          );
          this.reload();
        },
        error: () => this.snackBar.open('Failed to save exchange rate', 'Close', { duration: 4000 }),
      });
  }

  onCountrySearchChange(): void {
    this.countryPage = 0;
    this.applyCountryFilter();
    this.cdr.markForCheck();
  }

  applyCountryFilter(): void {
    const q = this.countrySearch.trim().toLowerCase();
    this.filteredCountryRows = this.countryRows.filter((row) => {
      if (!q) return true;
      return (
        row.countryName.toLowerCase().includes(q)
        || row.countryIsoAlpha2.toLowerCase().includes(q)
        || row.baseCurrencyCode.toLowerCase().includes(q)
      );
    });
  }

  get pagedCountryRows(): CountryCurrencyRow[] {
    const start = this.countryPage * this.countryPageSize;
    return this.filteredCountryRows.slice(start, start + this.countryPageSize);
  }

  get countryPageCount(): number {
    return Math.max(1, Math.ceil(this.filteredCountryRows.length / this.countryPageSize));
  }

  prevCountryPage(): void {
    this.countryPage = Math.max(0, this.countryPage - 1);
    this.cdr.markForCheck();
  }

  nextCountryPage(): void {
    this.countryPage = Math.min(this.countryPageCount - 1, this.countryPage + 1);
    this.cdr.markForCheck();
  }

  currencyOptions(): string[] {
    return this.currencies.map((c) => c.code);
  }

  currencyName(code: string): string {
    return this.currencies.find((c) => c.code === code)?.name ?? code;
  }

  rateLabel(rate: ExchangeRateRow): string {
    return `1 ${rate.fromCurrencyCode} = ${rate.rate} ${rate.toCurrencyCode}`;
  }

  relevantRates(): ExchangeRateRow[] {
    const functional = this.functionalCurrencyCode;
    return this.exchangeRates.filter(
      (r) => r.current && (r.fromCurrencyCode === functional || r.toCurrencyCode === functional),
    );
  }
}
