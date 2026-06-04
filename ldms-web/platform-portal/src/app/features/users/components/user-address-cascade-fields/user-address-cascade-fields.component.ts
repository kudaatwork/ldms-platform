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
import { forkJoin, of } from 'rxjs';
import { finalize, switchMap } from 'rxjs/operators';
import { LocationsService } from '../../../locations/services/locations.service';

interface SelectOption {
  id: number;
  label: string;
  sublabel?: string;
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

  @Input() disabled = false;

  countryOptions: SelectOption[] = [];
  provinceOptions: SelectOption[] = [];
  districtOptions: SelectOption[] = [];
  cityOptions: SelectOption[] = [];
  suburbOptions: SelectOption[] = [];

  countryId = '';
  provinceId = '';
  districtId = '';
  cityId = '';

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
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['seedSuburbId'] && !this.seedApplied) {
      const id = this.seedSuburbId;
      if (id != null && Number.isFinite(id) && id > 0) {
        this.seedApplied = true;
        this.applySeedSuburb(id);
      }
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
    this.cityId = '';
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
    this.cityId = '';
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
    this.cityId = '';
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
    this.cityId = raw;
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

  private applySeedSuburb(suburbId: number): void {
    this.emitSuburbId(String(suburbId));
    this.locationsService
      .findLocationById('suburb', suburbId)
      .pipe(
        switchMap((s) => {
          if (!s) {
            return of(null);
          }
          const countryId = this.toNum(s['countryId']);
          const provinceId = this.toNum(s['provinceId']);
          const districtId = this.toNum(s['districtId']);
          const cityId = this.toNum(s['cityId']);
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
          if (cityId != null) {
            this.cityId = String(cityId);
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
              districtId != null && cityId != null
                ? this.locationsService.fetchSuburbsForSelect({
                    districtId: String(districtId),
                    cityId: String(cityId),
                  })
                : of([]),
          });
        }),
      )
      .subscribe({
        next: (bundle) => {
          if (!bundle) {
            return;
          }
          this.provinceOptions = this.mapOptions(bundle.provinces);
          this.districtOptions = this.mapOptions(bundle.districts);
          this.cityOptions = this.mapOptions(bundle.cities);
          this.suburbOptions = this.mapOptions(bundle.suburbs);
          queueMicrotask(() => this.cdr.detectChanges());
        },
      });
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
