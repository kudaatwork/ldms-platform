import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Observable, Subject, catchError, finalize, forkJoin, of, takeUntil } from 'rxjs';
import { LocationsService, type LocationSelectOption } from '../../services/locations.service';

type ExplorerPhase = 'countries' | 'provinces' | 'districts' | 'settlements';

@Component({
  selector: 'app-location-explorer',
  templateUrl: './location-explorer.component.html',
  styleUrl: './location-explorer.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LocationExplorerComponent implements OnInit, OnDestroy {
  phase: ExplorerPhase = 'countries';
  loading = false;
  loadError: string | null = null;
  /** Quick filter for country / province / district steps */
  filterText = '';
  /** Filter for suburbs & villages columns */
  settlementFilter = '';

  countries: LocationSelectOption[] = [];
  provinces: LocationSelectOption[] = [];
  districts: LocationSelectOption[] = [];
  suburbs: LocationSelectOption[] = [];
  villages: LocationSelectOption[] = [];

  selectedCountry: LocationSelectOption | null = null;
  selectedProvince: LocationSelectOption | null = null;
  selectedDistrict: LocationSelectOption | null = null;

  private readonly destroy$ = new Subject<void>();
  /** Ignores superseded HTTP responses when the user drills down quickly. */
  private loadGeneration = 0;

  constructor(
    private readonly locationsService: LocationsService,
    private readonly title: Title,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Hierarchy explorer | LX Admin');
    this.reloadCountries();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get phaseTitle(): string {
    switch (this.phase) {
      case 'countries':
        return 'Countries';
      case 'provinces':
        return 'Provinces';
      case 'districts':
        return 'Districts';
      case 'settlements':
        return 'Suburbs & villages';
      default:
        return '';
    }
  }

  get phaseHint(): string {
    switch (this.phase) {
      case 'countries':
        return 'Pick a country to see its provinces.';
      case 'provinces':
        return 'Pick a province to see districts.';
      case 'districts':
        return 'Pick a district to see suburbs and villages.';
      case 'settlements':
        return 'Read-only view of suburbs and villages in this district.';
      default:
        return '';
    }
  }

  filteredCountries(): LocationSelectOption[] {
    return this.filterOptions(this.countries);
  }

  filteredProvinces(): LocationSelectOption[] {
    return this.filterOptions(this.provinces);
  }

  filteredDistricts(): LocationSelectOption[] {
    return this.filterOptions(this.districts);
  }

  filteredSuburbs(): LocationSelectOption[] {
    return this.filterOptionsWithQuery(this.suburbs, this.settlementFilter);
  }

  filteredVillages(): LocationSelectOption[] {
    return this.filterOptionsWithQuery(this.villages, this.settlementFilter);
  }

  onFilterChange(): void {
    this.cdr.markForCheck();
  }

  private filterOptions(opts: LocationSelectOption[]): LocationSelectOption[] {
    return this.filterOptionsWithQuery(opts, this.filterText);
  }

  private filterOptionsWithQuery(opts: LocationSelectOption[], raw: string): LocationSelectOption[] {
    const q = raw.trim().toLowerCase();
    if (!q) {
      return opts;
    }
    return opts.filter((o) => {
      const a = `${o.label} ${o.sublabel ?? ''}`.toLowerCase();
      return a.includes(q);
    });
  }

  /**
   * @param empty returned on HTTP error body (subscriber still receives it)
   * @param apply called only when no newer load was started since this call
   */
  private loadOrError<T>(source$: Observable<T>, empty: T, errorLabel: string, apply: (payload: T) => void): void {
    const gen = ++this.loadGeneration;
    this.loading = true;
    this.loadError = null;
    this.cdr.markForCheck();

    source$
      .pipe(
        takeUntil(this.destroy$),
        catchError(() => {
          if (gen === this.loadGeneration) {
            this.loadError = errorLabel;
          }
          return of(empty);
        }),
        finalize(() => {
          if (gen === this.loadGeneration) {
            this.loading = false;
          }
          this.cdr.markForCheck();
        }),
      )
      .subscribe((payload) => {
        if (gen !== this.loadGeneration) {
          return;
        }
        apply(payload);
      });
  }

  reloadCountries(): void {
    this.loadOrError(
      this.locationsService.fetchCountriesForSelect(),
      [] as LocationSelectOption[],
      'Could not load countries.',
      (rows) => {
        this.countries = rows;
      },
    );
  }

  selectCountry(c: LocationSelectOption): void {
    this.filterText = '';
    this.settlementFilter = '';
    this.selectedCountry = c;
    this.selectedProvince = null;
    this.selectedDistrict = null;
    this.suburbs = [];
    this.villages = [];
    this.phase = 'provinces';
    this.loadOrError(
      this.locationsService.fetchProvincesForSelect({ countryId: String(c.id) }),
      [] as LocationSelectOption[],
      'Could not load provinces for this country.',
      (rows) => {
        this.provinces = rows;
      },
    );
  }

  selectProvince(p: LocationSelectOption): void {
    this.filterText = '';
    this.settlementFilter = '';
    this.selectedProvince = p;
    this.selectedDistrict = null;
    this.suburbs = [];
    this.villages = [];
    this.phase = 'districts';
    this.loadOrError(
      this.locationsService.fetchDistrictsForSelect({ provinceId: String(p.id) }),
      [] as LocationSelectOption[],
      'Could not load districts for this province.',
      (rows) => {
        this.districts = rows;
      },
    );
  }

  selectDistrict(d: LocationSelectOption): void {
    this.filterText = '';
    this.settlementFilter = '';
    this.selectedDistrict = d;
    this.phase = 'settlements';
    this.loadOrError(
      forkJoin({
        suburbs: this.locationsService.fetchSuburbsForSelect({ districtId: String(d.id) }),
        villages: this.locationsService.fetchVillagesForSelect({ districtId: String(d.id) }),
      }),
      { suburbs: [] as LocationSelectOption[], villages: [] as LocationSelectOption[] },
      'Could not load suburbs and villages for this district.',
      ({ suburbs, villages }) => {
        this.suburbs = suburbs;
        this.villages = villages;
      },
    );
  }

  /** Breadcrumb: back to country → provinces list */
  goToProvinces(): void {
    if (!this.selectedCountry) {
      return;
    }
    this.filterText = '';
    this.settlementFilter = '';
    this.selectedProvince = null;
    this.selectedDistrict = null;
    this.suburbs = [];
    this.villages = [];
    this.phase = 'provinces';
    this.loadOrError(
      this.locationsService.fetchProvincesForSelect({ countryId: String(this.selectedCountry.id) }),
      [] as LocationSelectOption[],
      'Could not load provinces for this country.',
      (rows) => {
        this.provinces = rows;
      },
    );
  }

  /** Breadcrumb: back to province → districts list */
  goToDistricts(): void {
    if (!this.selectedProvince) {
      return;
    }
    this.filterText = '';
    this.settlementFilter = '';
    this.selectedDistrict = null;
    this.suburbs = [];
    this.villages = [];
    this.phase = 'districts';
    this.loadOrError(
      this.locationsService.fetchDistrictsForSelect({ provinceId: String(this.selectedProvince.id) }),
      [] as LocationSelectOption[],
      'Could not load districts for this province.',
      (rows) => {
        this.districts = rows;
      },
    );
  }

  /** Breadcrumb: back to district → reload settlements */
  goToSettlements(): void {
    if (!this.selectedDistrict) {
      return;
    }
    this.selectDistrict(this.selectedDistrict);
  }

  resetAll(): void {
    this.filterText = '';
    this.settlementFilter = '';
    this.selectedCountry = null;
    this.selectedProvince = null;
    this.selectedDistrict = null;
    this.provinces = [];
    this.districts = [];
    this.suburbs = [];
    this.villages = [];
    this.phase = 'countries';
    this.reloadCountries();
  }
}
