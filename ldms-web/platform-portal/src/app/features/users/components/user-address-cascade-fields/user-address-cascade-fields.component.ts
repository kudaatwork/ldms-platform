import { CommonModule } from '@angular/common';
import {
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, finalize, map, switchMap } from 'rxjs/operators';
import { LocationsService, type LocationSelectOption } from '../../../locations/services/locations.service';

interface SelectOption {
  id: number;
  label: string;
  sublabel?: string;
}

/** Pre-selected location hierarchy (e.g. from GET address by id). */
export interface AddressHierarchySeed {
  countryId?: number;
  provinceId?: number;
  districtId?: number;
  cityId?: number;
  suburbId: number;
  /** Helps match the city select when stored ids are legacy location-node ids. */
  cityName?: string;
  /** Optional free-text hint (e.g. "Mufakose, Harare") when city id was not persisted. */
  addressLine2?: string;
}

/**
 * Country → province → district → city → suburb selects (same cascade as Create user).
 * Parent owns line1 / postal code; this component owns FK cascade and suburb id.
 */
@Component({
  selector: 'app-user-address-cascade-fields',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './user-address-cascade-fields.component.html',
  styleUrl: './user-address-cascade-fields.component.scss',
})
export class UserAddressCascadeFieldsComponent implements OnInit, OnChanges {
  @Input() suburbId = '';
  @Output() suburbIdChange = new EventEmitter<string>();

  /** When set, loads country/province/district/city options to match this suburb. */
  @Input() seedSuburbId: number | null = null;

  /** Preferred seed source — uses denormalized ids from an address row (includes city when suburb lookup does not). */
  @Input() seedAddressHierarchy: AddressHierarchySeed | null = null;

  @Input() disabled = false;

  countryOptions: SelectOption[] = [];
  provinceOptions: SelectOption[] = [];
  districtOptions: SelectOption[] = [];
  cityOptions: SelectOption[] = [];
  suburbOptions: SelectOption[] = [];

  countryId = '';
  provinceId = '';
  districtId = '';
  @Input() cityId = '';
  @Output() cityIdChange = new EventEmitter<string>();

  countriesLoading = false;
  provinceOptionsLoading = false;
  districtOptionsLoading = false;
  cityOptionsLoading = false;
  suburbOptionsLoading = false;

  private seedApplied = false;

  constructor(
    private readonly locationsService: LocationsService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadCountries();
    this.tryApplySeed();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['seedAddressHierarchy']) {
      this.seedApplied = false;
    }
    if (changes['seedAddressHierarchy'] || changes['seedSuburbId']) {
      this.tryApplySeed();
    }
  }

  private tryApplySeed(): void {
    if (this.seedApplied) {
      return;
    }
    const hierarchy = this.seedAddressHierarchy;
    if (hierarchy?.suburbId != null && Number.isFinite(hierarchy.suburbId) && hierarchy.suburbId > 0) {
      this.seedApplied = true;
      this.applySeedHierarchy(hierarchy);
      return;
    }
    const id = this.seedSuburbId;
    if (id != null && Number.isFinite(id) && id > 0) {
      this.seedApplied = true;
      this.applySeedSuburb(id);
    }
  }

  trackBySelectOptionId(_index: number, o: SelectOption): number {
    return o.id;
  }

  selectOptionValue(id: number): string {
    return String(id);
  }

  onCountryChange(value: unknown): void {
    const raw = String(value ?? '').trim();
    this.countryId = raw;
    this.provinceId = '';
    this.districtId = '';
    this.emitCityId('');
    this.provinceOptions = [];
    this.districtOptions = [];
    this.cityOptions = [];
    this.suburbOptions = [];
    this.provinceOptionsLoading = false;
    this.districtOptionsLoading = false;
    this.cityOptionsLoading = false;
    this.suburbOptionsLoading = false;
    this.emitSuburbId('');
    const id = Number(raw);
    if (!raw || !Number.isFinite(id)) {
      return;
    }
    this.provinceOptionsLoading = true;
    this.locationsService
      .fetchProvincesForSelect({ countryId: String(id) })
      .pipe(finalize(() => this.afterOptionsLoaded('provinces')))
      .subscribe({
        next: (opts) => {
          this.provinceOptions = this.mapOptions(opts);
        },
        error: () => {
          this.provinceOptions = [];
        },
      });
  }

  onProvinceChange(value: unknown): void {
    const raw = String(value ?? '').trim();
    this.provinceId = raw;
    this.districtId = '';
    this.emitCityId('');
    this.districtOptions = [];
    this.cityOptions = [];
    this.suburbOptions = [];
    this.districtOptionsLoading = false;
    this.cityOptionsLoading = false;
    this.suburbOptionsLoading = false;
    this.emitSuburbId('');
    const id = Number(raw);
    if (!raw || !Number.isFinite(id)) {
      return;
    }
    this.districtOptionsLoading = true;
    this.locationsService
      .fetchDistrictsForSelect({ provinceId: String(id) })
      .pipe(finalize(() => this.afterOptionsLoaded('districts')))
      .subscribe({
        next: (opts) => {
          this.districtOptions = this.mapOptions(opts);
        },
        error: () => {
          this.districtOptions = [];
        },
      });
  }

  onDistrictChange(value: unknown): void {
    const raw = String(value ?? '').trim();
    this.districtId = raw;
    this.emitCityId('');
    this.cityOptions = [];
    this.suburbOptions = [];
    this.cityOptionsLoading = false;
    this.suburbOptionsLoading = false;
    this.emitSuburbId('');
    const id = Number(raw);
    if (!raw || !Number.isFinite(id)) {
      return;
    }
    this.cityOptionsLoading = true;
    this.locationsService
      .fetchCitiesForSelect({ districtId: String(id) })
      .pipe(finalize(() => this.afterOptionsLoaded('cities')))
      .subscribe({
        next: (opts) => {
          this.cityOptions = this.mapOptions(opts);
        },
        error: () => {
          this.cityOptions = [];
        },
      });
  }

  onCityChange(value: unknown): void {
    const raw = String(value ?? '').trim();
    this.emitCityId(raw);
    this.suburbOptions = [];
    this.suburbOptionsLoading = false;
    this.emitSuburbId('');
    const cityId = Number(raw);
    const districtId = Number(this.districtId);
    if (!raw || !Number.isFinite(cityId) || !Number.isFinite(districtId)) {
      return;
    }
    this.suburbOptionsLoading = true;
    this.locationsService
      .fetchSuburbsForSelect({ districtId: String(districtId), cityId: String(cityId) })
      .pipe(finalize(() => this.afterOptionsLoaded('suburbs')))
      .subscribe({
        next: (opts) => {
          this.suburbOptions = this.mapOptions(opts);
        },
        error: () => {
          this.suburbOptions = [];
        },
      });
  }

  onSuburbChange(value: unknown): void {
    const trimmed = String(value ?? '').trim();
    if (!trimmed) {
      this.emitSuburbId('');
      return;
    }
    const id = Number(trimmed);
    if (!Number.isFinite(id)) {
      return;
    }
    this.emitSuburbId(String(id));
  }

  private loadCountries(): void {
    this.countriesLoading = true;
    this.locationsService
      .fetchCountriesForSelect()
      .pipe(finalize(() => {
        this.countriesLoading = false;
        queueMicrotask(() => this.cdr.detectChanges());
      }))
      .subscribe({
        next: (opts) => {
          this.countryOptions = this.mapOptions(opts);
        },
        error: () => {
          this.countryOptions = [];
        },
      });
  }

  private applySeedHierarchy(hierarchy: AddressHierarchySeed): void {
    this.emitSuburbId(String(hierarchy.suburbId));
    const countryId = this.toNum(hierarchy.countryId);
    const provinceId = this.toNum(hierarchy.provinceId);
    const districtId = this.toNum(hierarchy.districtId);
    const cityId = this.toNum(hierarchy.cityId);
    if (countryId == null) {
      this.applySeedSuburb(hierarchy.suburbId);
      return;
    }
    this.countryId = String(countryId);
    if (provinceId != null) {
      this.provinceId = String(provinceId);
    }
    if (districtId != null) {
      this.districtId = String(districtId);
    }
    this.emitCityId('');
    this.loadSeedOptionLists(countryId, provinceId, districtId, cityId, hierarchy);
  }

  private applySeedSuburb(suburbId: number): void {
    this.locationsService
      .findLocationById('suburb', suburbId)
      .pipe(
        switchMap((s) => {
          if (!s) {
            return of(null);
          }
          const hierarchy: AddressHierarchySeed = {
            suburbId,
            countryId: this.toNum(s['countryId']) ?? undefined,
            provinceId: this.toNum(s['provinceId']) ?? undefined,
            districtId: this.toNum(s['districtId']) ?? undefined,
            cityId: this.toNum(s['cityId']) ?? undefined,
            cityName: String(s['cityName'] ?? '').trim() || undefined,
          };
          this.emitSuburbId(String(suburbId));
          const countryId = this.toNum(hierarchy.countryId);
          const provinceId = this.toNum(hierarchy.provinceId);
          const districtId = this.toNum(hierarchy.districtId);
          const cityId = this.toNum(hierarchy.cityId);
          if (countryId == null) {
            return of(null);
          }
          this.countryId = String(countryId);
          if (provinceId != null) {
            this.provinceId = String(provinceId);
          }
          if (districtId != null) {
            this.districtId = String(districtId);
          }
          this.emitCityId('');
          return this.buildSeedOptionLists(countryId, provinceId, districtId, cityId).pipe(
            map((bundle) => ({ bundle, hierarchy })),
          );
        }),
      )
      .subscribe({
        next: (result) => {
          if (!result?.bundle) {
            return;
          }
          this.finalizeSeedCascade(result.hierarchy, result.bundle);
        },
      });
  }

  private loadSeedOptionLists(
    countryId: number,
    provinceId: number | null,
    districtId: number | null,
    cityId: number | null,
    hierarchy: AddressHierarchySeed,
  ): void {
    this.buildSeedOptionLists(countryId, provinceId, districtId, cityId).subscribe({
      next: (bundle) => {
        if (!bundle) {
          return;
        }
        this.finalizeSeedCascade(hierarchy, bundle);
      },
    });
  }

  private finalizeSeedCascade(
    hierarchy: AddressHierarchySeed,
    bundle: {
      provinces: LocationSelectOption[];
      districts: LocationSelectOption[];
      cities: LocationSelectOption[];
      suburbs: LocationSelectOption[];
    },
  ): void {
    this.provinceOptions = this.mapOptions(bundle.provinces);
    this.districtOptions = this.mapOptions(bundle.districts);
    this.cityOptions = this.mapOptions(bundle.cities);
    this.suburbOptions = this.mapOptions(bundle.suburbs);

    this.prepareCityOptionForHierarchy(hierarchy)
      .pipe(switchMap(() => this.resolveSeedCityId(hierarchy)))
      .pipe(
        switchMap((resolvedCityId) => {
          if (resolvedCityId != null) {
            this.emitCityId(String(resolvedCityId));
          } else {
            this.emitCityId('');
          }
          const districtId = this.toNum(hierarchy.districtId) ?? this.toNum(this.districtId);
          if (districtId == null) {
            return of(null);
          }
          const suburbFilters: Record<string, string> = { districtId: String(districtId) };
          if (resolvedCityId != null) {
            suburbFilters['cityId'] = String(resolvedCityId);
          }
          return this.locationsService.fetchSuburbsForSelect(suburbFilters);
        }),
        switchMap((suburbs) => {
          if (suburbs) {
            this.suburbOptions = this.mapOptions(suburbs);
          }
          return this.ensureSuburbInOptions(hierarchy.suburbId);
        }),
      )
      .subscribe(() => {
        this.emitSuburbId(String(hierarchy.suburbId));
        queueMicrotask(() => this.cdr.detectChanges());
      });
  }

  private resolveSeedCityId(hierarchy: AddressHierarchySeed): Observable<number | null> {
    const options = this.cityOptions;
    const matchedByName = this.matchCityByName(options, hierarchy.cityName);
    if (matchedByName != null) {
      return of(matchedByName);
    }
    const matchedById = this.matchSeedCityIdInOptions(hierarchy.cityId, hierarchy);
    if (matchedById != null) {
      return of(matchedById);
    }

    return this.locationsService.findLocationById('suburb', hierarchy.suburbId).pipe(
      switchMap((suburb) => {
        const suburbCityId = this.toNum(suburb?.['cityId']);
        const suburbCityName = String(suburb?.['cityName'] ?? '').trim() || undefined;
        const fromSuburbName = this.matchCityByName(this.cityOptions, suburbCityName ?? hierarchy.cityName);
        if (fromSuburbName != null) {
          return of(fromSuburbName);
        }
        const fromSuburbId = this.matchSeedCityIdInOptions(suburbCityId, hierarchy);
        if (fromSuburbId != null) {
          return of(fromSuburbId);
        }
        const candidateIds = [hierarchy.cityId, suburbCityId].filter(
          (id): id is number => id != null && Number.isFinite(id) && id > 0,
        );
        const uniqueIds = [...new Set(candidateIds)];
        return this.fetchAndMergeCityOption(uniqueIds, suburbCityName ?? hierarchy.cityName).pipe(
          switchMap((resolved) => {
            if (resolved != null) {
              return of(resolved);
            }
            return this.inferCityFromContext(hierarchy);
          }),
        );
      }),
      catchError(() =>
        this.fetchAndMergeCityOption(
          hierarchy.cityId != null && hierarchy.cityId > 0 ? [hierarchy.cityId] : [],
          hierarchy.cityName,
        ).pipe(
          switchMap((resolved) => {
            if (resolved != null) {
              return of(resolved);
            }
            return this.inferCityFromContext(hierarchy);
          }),
        ),
      ),
    );
  }

  /** Last-resort city resolution when id/name from the API are missing (legacy addresses). */
  private inferCityFromContext(hierarchy: AddressHierarchySeed): Observable<number | null> {
    const local = this.tryMatchCityLocally(hierarchy);
    if (local != null) {
      return of(local);
    }
    return this.searchCitiesForSeedHints(hierarchy).pipe(
      map(() => this.tryMatchCityLocally(hierarchy)),
    );
  }

  private tryMatchCityLocally(hierarchy: AddressHierarchySeed): number | null {
    const options = this.cityOptions;
    const hints = this.collectCityNameHints(hierarchy);
    for (const hint of hints) {
      const exact = this.matchCityByName(options, hint);
      if (exact != null) {
        return exact;
      }
      const partial = this.matchCityByPartialName(options, hint);
      if (partial != null) {
        return partial;
      }
    }

    const byDistrict = this.matchCityByDistrictName(options);
    if (byDistrict != null) {
      return byDistrict;
    }

    const byProvince = this.matchCityByProvinceName(options);
    if (byProvince != null) {
      return byProvince;
    }

    if (options.length === 1) {
      return options[0].id;
    }

    return null;
  }

  private searchCitiesForSeedHints(hierarchy: AddressHierarchySeed): Observable<void> {
    const hints = this.collectCityNameHints(hierarchy);
    if (!hints.length) {
      return of(void 0);
    }
    const districtId = this.toNum(hierarchy.districtId) ?? this.toNum(this.districtId);
    const searches: Observable<LocationSelectOption[]>[] = [];
    for (const hint of hints) {
      if (districtId != null) {
        searches.push(
          this.locationsService
            .fetchCitiesForSelect({ districtId: String(districtId), name: hint })
            .pipe(catchError(() => of([]))),
        );
      }
      searches.push(
        this.locationsService.fetchCitiesForSelect({ name: hint }).pipe(catchError(() => of([]))),
      );
    }
    return forkJoin(searches).pipe(
      map((resultSets) => {
        for (const opts of resultSets) {
          for (const opt of opts) {
            this.mergeCityOption(opt);
          }
        }
      }),
      catchError(() => of(void 0)),
    );
  }

  private collectCityNameHints(hierarchy: AddressHierarchySeed): string[] {
    const hints: string[] = [];
    const push = (raw?: string) => {
      const trimmed = raw?.trim();
      if (trimmed && !hints.some((h) => h.toLowerCase() === trimmed.toLowerCase())) {
        hints.push(trimmed);
      }
    };
    push(hierarchy.cityName);
    push(this.parseCityFromAddressLine2(hierarchy.addressLine2));
    const district = this.districtOptions.find((d) => d.id === Number(this.districtId));
    push(district?.label);
    return hints;
  }

  private parseCityFromAddressLine2(line2?: string): string | undefined {
    const raw = line2?.trim();
    if (!raw) {
      return undefined;
    }
    const commaParts = raw
      .split(',')
      .map((p) => p.trim())
      .filter(Boolean);
    if (commaParts.length >= 2) {
      return commaParts[commaParts.length - 1];
    }
    return undefined;
  }

  private matchCityByPartialName(cityOptions: SelectOption[], cityName?: string): number | null {
    const normalized = cityName?.trim().toLowerCase();
    if (!normalized) {
      return null;
    }
    const found = cityOptions.find((o) => {
      const label = o.label.trim().toLowerCase();
      return label.includes(normalized) || normalized.includes(label);
    });
    return found?.id ?? null;
  }

  private matchCityByProvinceName(cityOptions: SelectOption[]): number | null {
    const province = this.provinceOptions.find((p) => p.id === Number(this.provinceId));
    if (!province) {
      return null;
    }
    const provinceName = province.label.trim().toLowerCase();
    const found = cityOptions.find((o) => o.label.trim().toLowerCase() === provinceName);
    return found?.id ?? null;
  }

  private matchCityByDistrictName(cityOptions: SelectOption[]): number | null {
    const district = this.districtOptions.find((d) => d.id === Number(this.districtId));
    if (!district) {
      return null;
    }
    const districtName = district.label.trim().toLowerCase();
    const exact = cityOptions.find((o) => o.label.trim().toLowerCase() === districtName);
    if (exact != null) {
      return exact.id;
    }
    const partial = cityOptions.find((o) => {
      const label = o.label.trim().toLowerCase();
      return label.includes(districtName) || districtName.includes(label);
    });
    return partial?.id ?? null;
  }

  /** Id match only when the option exists and does not contradict name hints (legacy ids). */
  private matchSeedCityIdInOptions(
    cityId: number | null | undefined,
    hierarchy: AddressHierarchySeed,
  ): number | null {
    if (cityId == null || !Number.isFinite(cityId) || cityId <= 0) {
      return null;
    }
    const option = this.cityOptions.find((o) => o.id === cityId);
    if (!option) {
      return null;
    }
    const hints = this.collectCityNameHints(hierarchy);
    if (!hints.length) {
      return cityId;
    }
    const label = option.label.trim().toLowerCase();
    const agrees = hints.some((hint) => {
      const normalized = hint.toLowerCase();
      return label === normalized || label.includes(normalized) || normalized.includes(label);
    });
    return agrees ? cityId : null;
  }

  private fetchAndMergeCityOption(ids: number[], cityName?: string): Observable<number | null> {
    const byName = this.matchCityByName(this.cityOptions, cityName);
    if (byName != null) {
      return of(byName);
    }
    if (!ids.length) {
      return of(null);
    }
    const [head, ...rest] = ids;
    return this.locationsService.fetchLocationNodeSelectOptionById(head).pipe(
      switchMap((opt) => {
        if (opt) {
          this.mergeCityOption(opt);
          return of(opt.id);
        }
        return rest.length ? this.fetchAndMergeCityOption(rest, cityName) : of(null);
      }),
      catchError(() => (rest.length ? this.fetchAndMergeCityOption(rest, cityName) : of(null))),
    );
  }

  private ensureSuburbInOptions(suburbId: number): Observable<void> {
    if (this.suburbOptions.some((o) => o.id === suburbId)) {
      return of(void 0);
    }
    return this.locationsService.findLocationById('suburb', suburbId).pipe(
      map((row) => {
        if (!row) {
          return;
        }
        const id = Number(row['id'] ?? 0);
        const label = String(row['name'] ?? '').trim();
        if (!Number.isFinite(id) || id <= 0 || !label) {
          return;
        }
        this.suburbOptions = [...this.suburbOptions, { id, label }].sort((a, b) =>
          a.label.localeCompare(b.label),
        );
      }),
      catchError(() => of(void 0)),
    );
  }

  private matchCityByName(cityOptions: SelectOption[], cityName?: string): number | null {
    const normalized = cityName?.trim().toLowerCase();
    if (!normalized) {
      return null;
    }
    const found = cityOptions.find((o) => o.label.trim().toLowerCase() === normalized);
    return found?.id ?? null;
  }

  private mergeCityOption(opt: LocationSelectOption): void {
    if (this.cityOptions.some((o) => o.id === opt.id)) {
      return;
    }
    this.cityOptions = [...this.cityOptions, opt].sort((a, b) => a.label.localeCompare(b.label));
  }

  private buildSeedOptionLists(
    countryId: number,
    provinceId: number | null,
    districtId: number | null,
    cityId: number | null,
  ) {
    const suburbFilters: Record<string, string> = {};
    if (districtId != null) {
      suburbFilters['districtId'] = String(districtId);
    }
    if (cityId != null) {
      suburbFilters['cityId'] = String(cityId);
    }
    return forkJoin({
      provinces: this.locationsService.fetchProvincesForSelect({ countryId: String(countryId) }),
      districts:
        provinceId != null
          ? this.locationsService.fetchDistrictsForSelect({ provinceId: String(provinceId) })
          : of([]),
      cities:
        districtId != null
          ? this.locationsService.fetchCitiesForSelect({ districtId: String(districtId) })
          : of([]),
      suburbs:
        districtId != null
          ? this.locationsService.fetchSuburbsForSelect(suburbFilters)
          : of([]),
    });
  }

  private prepareCityOptionForHierarchy(hierarchy: AddressHierarchySeed): Observable<void> {
    const id = this.toNum(hierarchy.cityId);
    if (id == null) {
      return of(void 0);
    }
    if (this.cityOptions.some((o) => o.id === id)) {
      return of(void 0);
    }
    return this.locationsService.fetchLocationNodeSelectOptionById(id).pipe(
      map((opt) => {
        if (opt) {
          this.mergeCityOption(opt);
        }
      }),
      catchError(() => of(void 0)),
    );
  }

  private emitCityId(value: string): void {
    this.cityId = value;
    this.cityIdChange.emit(value);
  }

  private emitSuburbId(value: string): void {
    this.suburbId = value;
    this.suburbIdChange.emit(value);
  }

  private mapOptions(
    opts: { id: number; label: string; sublabel?: string }[],
  ): SelectOption[] {
    return opts.map((o) => ({ id: o.id, label: o.label, sublabel: o.sublabel }));
  }

  private afterOptionsLoaded(tier: 'provinces' | 'districts' | 'cities' | 'suburbs'): void {
    if (tier === 'provinces') {
      this.provinceOptionsLoading = false;
    }
    if (tier === 'districts') {
      this.districtOptionsLoading = false;
    }
    if (tier === 'cities') {
      this.cityOptionsLoading = false;
    }
    if (tier === 'suburbs') {
      this.suburbOptionsLoading = false;
    }
    queueMicrotask(() => this.cdr.detectChanges());
  }

  private toNum(v: unknown): number | null {
    const n = Number(v);
    return Number.isFinite(n) && n > 0 ? n : null;
  }
}
