import { ChangeDetectorRef, Component, Inject, OnDestroy, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {
  EMPTY,
  Observable,
  Subject,
  catchError,
  finalize,
  forkJoin,
  map,
  mergeMap,
  of,
  takeUntil,
  tap,
} from 'rxjs';
import { LocationsService, type LocationSelectOption } from '../../services/locations.service';
import type { LocationEntityKind } from '../../models/location.models';
import {
  GoogleAddressAutocompleteService,
  type GoogleAddressSuggestion,
} from '../../services/google-address-autocomplete.service';

export type LocationFormDialogMode =
  | 'country'
  | 'province'
  | 'district'
  | 'suburb'
  | 'admin-level'
  | 'city'
  | 'village'
  | 'address'
  | 'language'
  | 'localized-name';

export type LocationFormDialogAction = 'create' | 'edit' | 'view';

export interface LocationFormDialogData {
  mode: LocationFormDialogMode;
  action: LocationFormDialogAction;
  id?: number | null;
  initialValue?: Record<string, unknown> | null;
}

export interface LocationFormDialogResult {
  saved: boolean;
  message?: string;
  /** True when a new row was created; the grid can jump to the last page (new id sorts last). */
  created?: boolean;
  /** Server id of the created row when `created` is true (used to retry load if totals are briefly stale). */
  createdId?: number;
}

@Component({
  selector: 'app-location-form-dialog',
  templateUrl: './location-form-dialog.component.html',
  styleUrl: './location-form-dialog.component.scss',
  standalone: false,
})
export class LocationFormDialogComponent implements OnInit, OnDestroy {
  form: FormGroup;
  submitting = false;
  loading = false;
  listLoadError: string | null = null;
  saveError: string | null = null;

  /** Form-control names whose values are numeric foreign keys; coerced before patching. */
  private static readonly ID_FIELDS = [
    'countryId',
    'provinceId',
    'districtId',
    'cityId',
    'suburbId',
    'administrativeLevelId',
    'settlementId',
    'villageLocationNodeId',
    'languageId',
  ];

  countryOptions: LocationSelectOption[] = [];
  provinceOptions: LocationSelectOption[] = [];
  districtOptions: LocationSelectOption[] = [];
  cityOptions: LocationSelectOption[] = [];
  suburbOptions: LocationSelectOption[] = [];
  adminLevelOptions: LocationSelectOption[] = [];
  /** Cities in the selected district (village form: required parent city). */
  villageCityOptions: LocationSelectOption[] = [];
  villageOptions: LocationSelectOption[] = [];
  /** Languages (localized-name form). */
  languageOptions: LocationSelectOption[] = [];
  googleSuggestions: GoogleAddressSuggestion[] = [];
  googleLookupRunning = false;

  /** Village: city + suburb lists loading after district is chosen. */
  villageListsLoading = false;
  /** Address: child dropdown lists loading (cascade + settlement refresh). */
  addressProvincesLoading = false;
  addressDistrictsLoading = false;
  addressCitySettlementLoading = false;
  addressSettlementRefreshing = false;

  private villageDependentFetchGen = 0;
  private addressCascadeGen = 0;
  /** Settlement-only refetch (must not cancel address cascade / hydrate). */
  private addressSettlementGen = 0;
  /** Cancels stale province/district/suburb loads when localized-name cascade inputs change. */
  private localizedNameCascadeGen = 0;

  private readonly destroy$ = new Subject<void>();
  private readonly googleAddressAutocompleteService = inject(GoogleAddressAutocompleteService);

  constructor(
    private readonly fb: FormBuilder,
    private readonly locationsService: LocationsService,
    private readonly cdr: ChangeDetectorRef,
    private readonly dialogRef: MatDialogRef<LocationFormDialogComponent, LocationFormDialogResult>,
    @Inject(MAT_DIALOG_DATA) readonly data: LocationFormDialogData,
  ) {
    this.form = this.buildForm(data.mode);
  }

  ngOnInit(): void {
    // Load root FK options first, then run edit/view hydration. If we hydrate (e.g. fetch provinces)
    // before `loadListsForMode` completes, its `tap` can reset `provinceOptions` / `districtOptions`
    // to [] and wipe the cascade — parent province/country then look blank in view/edit.
    if (this.needsFkLists()) {
      this.loadListsForMode()
        .pipe(
          finalize(() => this.cdr.markForCheck()),
          takeUntil(this.destroy$),
        )
        .subscribe({
          next: () => this.afterListsReady(),
          error: () => {
            this.listLoadError = 'Could not load some dropdown data. Try reopening this dialog.';
            this.afterListsReady();
          },
        });
    } else {
      this.afterListsReady();
    }

    if (this.data.mode === 'localized-name') {
      this.syncLocalizedNameReferenceValidators();
      this.form
        .get('referenceType')
        ?.valueChanges.pipe(takeUntil(this.destroy$))
        .subscribe(() => this.onLocalizedNameReferenceTypeChange());
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private needsFkLists(): boolean {
    return this.data.mode !== 'country' && this.data.mode !== 'language';
  }

  private loadListsForMode(): Observable<void> {
    switch (this.data.mode) {
      case 'province':
        return forkJoin({
          countries: this.locationsService.fetchCountriesForSelect(),
          adminLevels: this.locationsService.fetchAdministrativeLevelsForSelect(),
        }).pipe(
          tap(({ countries, adminLevels }) => {
            this.countryOptions = countries;
            this.adminLevelOptions = adminLevels;
          }),
          map(() => void 0),
          catchError(() => {
            this.countryOptions = [];
            this.adminLevelOptions = [];
            return of(void 0);
          }),
        );
      case 'district':
        return forkJoin({
          countries: this.locationsService.fetchCountriesForSelect(),
          adminLevels: this.locationsService.fetchAdministrativeLevelsForSelect(),
        }).pipe(
          tap(({ countries, adminLevels }) => {
            this.countryOptions = countries;
            this.adminLevelOptions = adminLevels;
            this.provinceOptions = [];
          }),
          map(() => void 0),
          catchError(() => {
            this.countryOptions = [];
            this.adminLevelOptions = [];
            this.provinceOptions = [];
            return of(void 0);
          }),
        );
      case 'suburb':
        return forkJoin({
          countries: this.locationsService.fetchCountriesForSelect(),
          adminLevels: this.locationsService.fetchAdministrativeLevelsForSelect(),
        }).pipe(
          tap(({ countries, adminLevels }) => {
            this.countryOptions = countries;
            this.adminLevelOptions = adminLevels;
            this.provinceOptions = [];
            this.districtOptions = [];
          }),
          map(() => void 0),
          catchError(() => {
            this.countryOptions = [];
            this.adminLevelOptions = [];
            this.provinceOptions = [];
            this.districtOptions = [];
            return of(void 0);
          }),
        );
      case 'admin-level':
        return this.locationsService.fetchCountriesForSelect().pipe(
          tap((countries) => {
            this.countryOptions = countries;
          }),
          map(() => void 0),
          catchError(() => {
            this.countryOptions = [];
            return of(void 0);
          }),
        );
      case 'city':
      case 'village':
        return this.locationsService.fetchCountriesForSelect().pipe(
          tap((countries) => {
            this.countryOptions = countries;
            this.provinceOptions = [];
            this.districtOptions = [];
          }),
          map(() => void 0),
          catchError(() => {
            this.countryOptions = [];
            this.provinceOptions = [];
            this.districtOptions = [];
            return of(void 0);
          }),
        );
      case 'address':
        return this.locationsService.fetchCountriesForSelect().pipe(
          tap((countries) => {
            this.countryOptions = countries;
          }),
          map(() => void 0),
          catchError(() => {
            this.countryOptions = [];
            return of(void 0);
          }),
        );
      case 'localized-name':
        return forkJoin({
          languages: this.locationsService.fetchLanguagesForSelect(),
          countries: this.locationsService.fetchCountriesForSelect(),
        }).pipe(
          tap(({ languages, countries }) => {
            this.languageOptions = languages;
            this.countryOptions = countries;
            this.provinceOptions = [];
            this.districtOptions = [];
            this.suburbOptions = [];
          }),
          map(() => void 0),
          catchError(() => {
            this.languageOptions = [];
            this.countryOptions = [];
            this.provinceOptions = [];
            this.districtOptions = [];
            this.suburbOptions = [];
            return of(void 0);
          }),
        );
      default:
        return of(void 0);
    }
  }

  private afterListsReady(): void {
    if (this.data.action === 'view' || this.data.action === 'edit') {
      if (this.data.initialValue) {
        this.applyInitialValue(this.data.initialValue);
        if (this.data.mode === 'address') {
          this.hydrateAddressDropdownsFromForm();
        } else if (this.data.mode === 'district') {
          this.hydrateDistrictProvinceListFromCountry();
        } else if (this.data.mode === 'suburb') {
          this.hydrateSuburbCascadeFromFormKeys();
        } else if (this.data.mode === 'city' || this.data.mode === 'village') {
          this.hydrateCityVillageFromDistrictId()
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
              this.reloadVillageDependentLists();
              this.cdr.markForCheck();
            });
        }
      } else if (this.data.id) {
        this.fetchById(this.data.id);
      }
      if (this.data.action === 'view') {
        this.form.disable();
      }
    }
  }

  get title(): string {
    const verb =
      this.data.action === 'create' ? 'Add' : this.data.action === 'edit' ? 'Edit' : 'View';
    const noun: Record<LocationFormDialogMode, string> = {
      country: 'Country',
      province: 'Province',
      district: 'District',
      suburb: 'Suburb',
      'admin-level': 'Administrative Level',
      city: 'City',
      village: 'Village',
      address: 'Address',
      language: 'Language',
      'localized-name': 'Localized name',
    };
    return `${verb} ${noun[this.data.mode]}`;
  }

  get subtitle(): string {
    const subtitles: Record<LocationFormDialogMode, { create: string; edit: string; view: string }> = {
      country: {
        create: 'Add a sovereign country with its ISO codes, dial code and timezone.',
        edit: 'Update country details. ISO codes and dial code are required.',
        view: 'Review country details. Editing is disabled in view mode.',
      },
      province: {
        create: 'Add a province under an existing country.',
        edit: 'Update province details. Country selection is required.',
        view: 'Review province details. Editing is disabled in view mode.',
      },
      district: {
        create: 'Choose country and province, then enter the district name and optional details.',
        edit: 'Update district details. Country and province are required.',
        view: 'Review district details. Editing is disabled in view mode.',
      },
      suburb: {
        create: 'Choose country, province, and district, then enter the suburb name.',
        edit: 'Update suburb details. Hierarchy selection is required.',
        view: 'Review suburb details. Editing is disabled in view mode.',
      },
      'admin-level': {
        create: 'Define a tier in the administrative hierarchy (e.g. province, district, suburb).',
        edit: 'Update administrative level metadata.',
        view: 'Review administrative level details.',
      },
      city: {
        create: 'Pick country → province → district, then add name, code, and optional geo details.',
        edit: 'Update city details and coordinates.',
        view: 'Review city details. Editing is disabled in view mode.',
      },
      village: {
        create: 'Pick country → province → district, then the city this village belongs to; suburb is optional.',
        edit: 'Update village details and coordinates.',
        view: 'Review village details. Editing is disabled in view mode.',
      },
      address: {
        create: 'Pick country → province → district so settlement lists stay small and fast. Optional city narrows context.',
        edit: 'Update address details and settlement mapping.',
        view: 'Review full address hierarchy linkage.',
      },
      language: {
        create: 'Add a language with optional ISO code, native name, and default flag.',
        edit: 'Update language metadata.',
        view: 'Review language details.',
      },
      'localized-name': {
        create:
          'Pick reference type and location (country → province → district → suburb when needed), language, and translation.',
        edit: 'Update the translation or its target reference.',
        view: 'Review localized name details.',
      },
    };
    return subtitles[this.data.mode][this.data.action];
  }

  get headerVariant(): 'create' | 'edit' | 'view' {
    return this.data.action;
  }

  get primaryActionLabel(): string {
    if (this.data.action === 'edit') return 'Update';
    return 'Save';
  }

  get primaryActionLoadingLabel(): string {
    if (this.data.action === 'edit') return 'Updating...';
    return 'Saving...';
  }

  get isReadonly(): boolean {
    return this.data.action === 'view';
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.form.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || control.dirty);
  }

  cancel(): void {
    this.dialogRef.close({ saved: false });
  }

  save(): void {
    if (this.isReadonly) {
      this.dialogRef.close({ saved: false });
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.cdr.markForCheck();
      return;
    }

    this.submitting = true;
    this.saveError = null;

    const payload = this.toSubmitPayload();
    const kind = this.data.mode as LocationEntityKind;
    const action$ =
      this.data.action === 'edit' && this.data.id
        ? this.locationsService.updateLocation(kind, this.data.id, payload)
        : this.locationsService.createLocation(kind, payload);

    action$
      .pipe(
        finalize(() => {
          this.submitting = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
      next: (response) => {
        if (!response.ok) {
          this.saveError =
            response.message || 'The server rejected this save. Check required fields and try again.';
          this.cdr.markForCheck();
          return;
        }
        this.dialogRef.close({
          saved: true,
          message: response.message,
          created: this.data.action === 'create',
          createdId: response.createdId,
        });
      },
      error: (err) => {
        this.saveError =
          (err?.status === 0
            ? 'Request failed before the server response reached the browser. Please retry.'
            : undefined) ||
          err?.error?.messageResponse ||
          err?.error?.message ||
          err?.error?.error ||
          err?.message ||
          'Failed to save this location. Please check inputs and try again.';
        this.cdr.markForCheck();
      },
    });
  }

  private fetchById(id: number): void {
    this.loading = true;
    this.form.disable();
    this.locationsService
      .findLocationById(this.data.mode as LocationEntityKind, id)
      .pipe(
        finalize(() => {
          this.loading = false;
          if (!this.isReadonly) {
            this.form.enable();
          }
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (entity) => {
          if (entity) {
            this.applyInitialValue(entity);
            if (this.data.mode === 'address') {
              this.hydrateAddressDropdownsFromForm();
            } else if (this.data.mode === 'district') {
              this.hydrateDistrictProvinceListFromCountry();
            } else if (this.data.mode === 'suburb') {
              this.hydrateSuburbCascadeFromFormKeys();
            } else if (this.data.mode === 'city' || this.data.mode === 'village') {
              this.hydrateCityVillageFromDistrictId()
                .pipe(takeUntil(this.destroy$))
                .subscribe(() => {
                  this.reloadVillageDependentLists();
                  this.cdr.markForCheck();
                });
            }
          }
        },
        error: (err) => {
          this.saveError =
            (err?.status === 0
              ? 'Request failed before the server response reached the browser. Please retry.'
              : undefined) ||
            err?.error?.messageResponse || err?.error?.message || err?.message || 'Failed to load record details.';
        },
      });
  }

  private applyInitialValue(value: Record<string, unknown>): void {
    if (this.data.mode === 'localized-name') {
      this.applyLocalizedNameInitialValue(value);
      return;
    }
    const patch: Record<string, unknown> = { ...value };
    if (this.data.mode === 'address') {
      const settlementType = String(patch['settlementType'] ?? 'SUBURB').toUpperCase();
      patch['settlementType'] = settlementType === 'VILLAGE' ? 'VILLAGE' : 'SUBURB';
      patch['settlementId'] = patch['settlementId'] ?? (settlementType === 'VILLAGE' ? patch['villageId'] : patch['suburbId']);
      patch['googleSearchText'] = patch['formattedAddress'] ?? '';
    }
    // Native <select [ngValue]> uses === to match the form value to an option's id.
    // API payloads sometimes return ids as strings, so coerce to numbers up-front.
    for (const field of LocationFormDialogComponent.ID_FIELDS) {
      if (field in patch) {
        patch[field] = this.toNumberOrNull(patch[field]);
      }
    }
    this.form.patchValue(patch, { emitEvent: false });
  }

  private buildForm(mode: LocationFormDialogMode): FormGroup {
    switch (mode) {
      case 'country':
        return this.fb.group({
          name: ['', Validators.required],
          isoAlpha2Code: ['', [Validators.required, Validators.maxLength(2)]],
          isoAlpha3Code: ['', [Validators.required, Validators.maxLength(3)]],
          dialCode: ['', Validators.required],
          timezone: ['', Validators.required],
          currencyCode: ['', [Validators.required, Validators.maxLength(3)]],
          latitude: [null as number | null],
          longitude: [null as number | null],
        });
      case 'province':
        return this.fb.group({
          name: ['', Validators.required],
          code: [''],
          countryId: [null as number | null, Validators.required],
          administrativeLevelId: [null as number | null],
          latitude: [null as number | null],
          longitude: [null as number | null],
        });
      case 'district':
        return this.fb.group({
          name: ['', Validators.required],
          code: [''],
          countryId: [null as number | null, Validators.required],
          provinceId: [null as number | null, Validators.required],
          administrativeLevelId: [null as number | null],
          latitude: [null as number | null],
          longitude: [null as number | null],
        });
      case 'suburb':
        return this.fb.group({
          name: ['', Validators.required],
          code: [''],
          postalCode: [''],
          countryId: [null as number | null, Validators.required],
          provinceId: [null as number | null, Validators.required],
          districtId: [null as number | null, Validators.required],
          administrativeLevelId: [null as number | null],
          latitude: [null as number | null],
          longitude: [null as number | null],
        });
      case 'admin-level':
        return this.fb.group({
          name: ['', Validators.required],
          code: [''],
          level: [null as number | null, [Validators.required, Validators.min(0)]],
          countryId: [null as number | null, Validators.required],
          description: [''],
        });
      case 'city':
        return this.fb.group({
          name: ['', Validators.required],
          code: [''],
          countryId: [null as number | null, Validators.required],
          provinceId: [null as number | null, Validators.required],
          districtId: [null as number | null, Validators.required],
          latitude: [null as number | null],
          longitude: [null as number | null],
          timezone: [''],
          postalCode: [''],
        });
      case 'village':
        return this.fb.group({
          name: ['', Validators.required],
          code: [''],
          countryId: [null as number | null, Validators.required],
          provinceId: [null as number | null, Validators.required],
          cityId: [null as number | null, Validators.required],
          districtId: [null as number | null, Validators.required],
          suburbId: [null as number | null],
          latitude: [null as number | null],
          longitude: [null as number | null],
          timezone: [''],
          postalCode: [''],
        });
      case 'address':
        return this.fb.group({
          line1: ['', Validators.required],
          line2: [''],
          postalCode: [''],
          countryId: [null as number | null],
          provinceId: [null as number | null],
          districtId: [null as number | null],
          cityId: [null as number | null],
          settlementType: ['SUBURB', Validators.required],
          settlementId: [null as number | null, Validators.required],
          suburbId: [null as number | null],
          villageLocationNodeId: [null as number | null],
          externalSource: [''],
          externalPlaceId: [''],
          formattedAddress: [''],
          googleSearchText: [''],
        });
      case 'language':
        return this.fb.group({
          name: ['', Validators.required],
          isoCode: [''],
          nativeName: [''],
          isDefault: [false],
        });
      case 'localized-name':
        return this.fb.group({
          value: ['', Validators.required],
          languageId: [null as number | null, Validators.required],
          referenceType: ['COUNTRY', Validators.required],
          countryId: [null as number | null],
          provinceId: [null as number | null],
          districtId: [null as number | null],
          suburbId: [null as number | null],
        });
      default:
        return this.fb.group({});
    }
  }

  private toSubmitPayload(): Record<string, unknown> {
    const raw = this.form.getRawValue() as Record<string, unknown>;
    if (this.data.mode === 'district') {
      return {
        name: raw['name'],
        code: raw['code'],
        provinceId: raw['provinceId'],
        administrativeLevelId: raw['administrativeLevelId'],
        latitude: raw['latitude'],
        longitude: raw['longitude'],
      };
    }
    if (this.data.mode === 'suburb') {
      return {
        name: raw['name'],
        code: raw['code'],
        postalCode: raw['postalCode'],
        districtId: raw['districtId'],
        administrativeLevelId: raw['administrativeLevelId'],
        latitude: raw['latitude'],
        longitude: raw['longitude'],
      };
    }
    if (this.data.mode === 'city') {
      return {
        name: raw['name'],
        code: raw['code'],
        districtId: raw['districtId'],
        latitude: raw['latitude'],
        longitude: raw['longitude'],
        timezone: raw['timezone'],
        postalCode: raw['postalCode'],
      };
    }
    if (this.data.mode === 'village') {
      return {
        name: raw['name'],
        code: raw['code'],
        cityId: raw['cityId'],
        districtId: raw['districtId'],
        suburbId: raw['suburbId'],
        latitude: raw['latitude'],
        longitude: raw['longitude'],
        timezone: raw['timezone'],
        postalCode: raw['postalCode'],
      };
    }
    if (this.data.mode === 'address') {
      const settlementType = String(raw['settlementType'] ?? 'SUBURB').toUpperCase();
      const settlementId = this.toNumberOrNull(raw['settlementId']);
      return {
        line1: raw['line1'],
        line2: raw['line2'],
        postalCode: raw['postalCode'],
        cityId: this.toNumberOrNull(raw['cityId']),
        settlementType,
        settlementId,
        suburbId: settlementType === 'SUBURB' ? settlementId : null,
        villageLocationNodeId: settlementType === 'VILLAGE' ? settlementId : null,
        externalSource: raw['externalSource'],
        externalPlaceId: raw['externalPlaceId'],
        formattedAddress: raw['formattedAddress'],
      };
    }
    if (this.data.mode === 'language') {
      return {
        name: raw['name'],
        isoCode: raw['isoCode'],
        nativeName: raw['nativeName'],
        isDefault: raw['isDefault'] === true,
      };
    }
    if (this.data.mode === 'localized-name') {
      const rt = String(raw['referenceType'] ?? '').toUpperCase();
      let referenceId: number | null = null;
      if (rt === 'COUNTRY') {
        referenceId = this.toNumberOrNull(raw['countryId']);
      } else if (rt === 'PROVINCE') {
        referenceId = this.toNumberOrNull(raw['provinceId']);
      } else if (rt === 'DISTRICT') {
        referenceId = this.toNumberOrNull(raw['districtId']);
      } else if (rt === 'SUBURB') {
        referenceId = this.toNumberOrNull(raw['suburbId']);
      }
      return {
        value: raw['value'],
        languageId: raw['languageId'],
        referenceType: rt,
        referenceId,
      };
    }
    return raw;
  }

  searchGoogleSuggestions(): void {
    if (this.data.mode !== 'address') {
      return;
    }
    const searchText = String(this.form.get('googleSearchText')?.value ?? '').trim();
    if (!searchText) {
      this.googleSuggestions = [];
      return;
    }
    this.googleLookupRunning = true;
    this.googleAddressAutocompleteService
      .search(searchText)
      .pipe(
        finalize(() => (this.googleLookupRunning = false)),
        takeUntil(this.destroy$),
      )
      .subscribe((results) => {
        this.googleSuggestions = results;
      });
  }

  onGoogleSuggestionPicked(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const index = Number(select.value);
    const suggestion = this.googleSuggestions[index];
    if (suggestion) {
      this.applyGoogleSuggestion(suggestion);
    }
    select.value = '';
  }

  applyGoogleSuggestion(suggestion: GoogleAddressSuggestion): void {
    if (this.data.mode !== 'address') {
      return;
    }
    this.form.patchValue(
      {
        line1: suggestion.line1 ?? this.form.get('line1')?.value,
        postalCode: suggestion.postalCode ?? this.form.get('postalCode')?.value,
        externalSource: 'GOOGLE_PLACES',
        externalPlaceId: suggestion.placeId,
        formattedAddress: suggestion.formattedAddress ?? suggestion.description,
        googleSearchText: suggestion.description,
      },
      { emitEvent: false },
    );
  }

  onSettlementTypeChange(): void {
    if (this.data.mode !== 'address') {
      return;
    }
    this.form.patchValue({ settlementId: null, suburbId: null, villageLocationNodeId: null }, { emitEvent: false });
    this.reloadAddressSettlementOptions();
  }

  /** Address: country chosen (not “Any”). */
  hasAddressCountry(): boolean {
    return this.toNumberOrNull(this.form.get('countryId')?.value) != null;
  }

  /** Address: province chosen (not “Any”). */
  hasAddressProvince(): boolean {
    return this.toNumberOrNull(this.form.get('provinceId')?.value) != null;
  }

  /** Address: district chosen (not “Any”) — required for settlement lists. */
  hasAddressDistrict(): boolean {
    return this.toNumberOrNull(this.form.get('districtId')?.value) != null;
  }

  settlementOptionsForAddress(): LocationSelectOption[] {
    return String(this.form.get('settlementType')?.value ?? 'SUBURB').toUpperCase() === 'VILLAGE'
      ? this.villageOptions
      : this.suburbOptions;
  }

  onAddressCountryChange(): void {
    const gen = ++this.addressCascadeGen;
    const countryId = this.toNumberOrNull(this.form.get('countryId')?.value);
    this.form.patchValue({ provinceId: null, districtId: null, cityId: null, settlementId: null }, { emitEvent: false });
    this.provinceOptions = [];
    this.districtOptions = [];
    this.cityOptions = [];
    this.suburbOptions = [];
    this.villageOptions = [];
    this.addressProvincesLoading = false;
    this.addressDistrictsLoading = false;
    this.addressCitySettlementLoading = false;
    if (countryId == null) {
      return;
    }
    this.addressProvincesLoading = true;
    this.locationsService
      .fetchProvincesForSelect({ countryId: String(countryId) })
      .pipe(
        finalize(() => {
          if (gen === this.addressCascadeGen) {
            this.addressProvincesLoading = false;
          }
        }),
        takeUntil(this.destroy$),
      )
      .subscribe((options) => {
        if (gen !== this.addressCascadeGen) {
          return;
        }
        this.provinceOptions = options;
      });
  }

  onAddressProvinceChange(): void {
    const gen = ++this.addressCascadeGen;
    const provinceId = this.toNumberOrNull(this.form.get('provinceId')?.value);
    this.form.patchValue({ districtId: null, cityId: null, settlementId: null }, { emitEvent: false });
    this.districtOptions = [];
    this.cityOptions = [];
    this.suburbOptions = [];
    this.villageOptions = [];
    this.addressDistrictsLoading = false;
    this.addressCitySettlementLoading = false;
    if (provinceId == null) {
      return;
    }
    this.addressDistrictsLoading = true;
    this.locationsService
      .fetchDistrictsForSelect({ provinceId: String(provinceId) })
      .pipe(
        finalize(() => {
          if (gen === this.addressCascadeGen) {
            this.addressDistrictsLoading = false;
          }
        }),
        takeUntil(this.destroy$),
      )
      .subscribe((options) => {
        if (gen !== this.addressCascadeGen) {
          return;
        }
        this.districtOptions = options;
      });
  }

  onAddressDistrictChange(): void {
    const gen = ++this.addressCascadeGen;
    const districtId = this.toNumberOrNull(this.form.get('districtId')?.value);
    this.form.patchValue({ cityId: null, settlementId: null }, { emitEvent: false });
    this.cityOptions = [];
    this.suburbOptions = [];
    this.villageOptions = [];
    if (districtId == null) {
      return;
    }
    this.addressCitySettlementLoading = true;
    forkJoin({
      cities: this.locationsService.fetchCitiesForSelect({ districtId: String(districtId) }),
      suburbs: this.locationsService.fetchSuburbsForSelect({ districtId: String(districtId) }),
      villages: this.locationsService.fetchVillagesForSelect({ districtId: String(districtId) }),
    })
      .pipe(
        finalize(() => {
          if (gen === this.addressCascadeGen) {
            this.addressCitySettlementLoading = false;
          }
        }),
        takeUntil(this.destroy$),
      )
      .subscribe(({ cities, suburbs, villages }) => {
        if (gen !== this.addressCascadeGen) {
          return;
        }
        this.cityOptions = cities;
        this.suburbOptions = suburbs;
        this.villageOptions = villages;
        this.cdr.markForCheck();
      });
  }

  /** Resolve country/province from the selected district (edit / view from grid row). */
  private hydrateCityVillageFromDistrictId(): Observable<void> {
    const districtId = this.toNumberOrNull(this.form.get('districtId')?.value);
    if (districtId == null) {
      return of(void 0);
    }
    return this.locationsService.findLocationById('district', districtId).pipe(
      mergeMap((d) => {
        if (!d) {
          return of(void 0);
        }
        const countryId = this.toNumberOrNull(d['countryId']);
        const provinceId = this.toNumberOrNull(d['provinceId']);
        this.form.patchValue({ countryId, provinceId }, { emitEvent: false });
        return forkJoin({
          provinces:
            countryId != null
              ? this.locationsService.fetchProvincesForSelect({ countryId: String(countryId) })
              : of([] as LocationSelectOption[]),
          districts:
            provinceId != null
              ? this.locationsService.fetchDistrictsForSelect({ provinceId: String(provinceId) })
              : of([] as LocationSelectOption[]),
        }).pipe(
          tap(({ provinces, districts }) => {
            this.provinceOptions = provinces;
            this.districtOptions = districts;
          }),
          map(() => void 0),
        );
      }),
      catchError(() => of(void 0)),
    );
  }

  private hydrateDistrictProvinceListFromCountry(): void {
    const countryId = this.toNumberOrNull(this.form.get('countryId')?.value);
    const provinceId = this.toNumberOrNull(this.form.get('provinceId')?.value);
    if (countryId != null) {
      this.fetchProvincesForDistrictForm(countryId);
      return;
    }
    if (provinceId != null) {
      this.locationsService
        .findLocationById('province', provinceId)
        .pipe(takeUntil(this.destroy$))
        .subscribe((p) => {
          const cid = this.toNumberOrNull(p?.['countryId']);
          if (cid != null) {
            this.form.patchValue({ countryId: cid }, { emitEvent: false });
            this.fetchProvincesForDistrictForm(cid);
          }
        });
    }
  }

  private fetchProvincesForDistrictForm(countryId: number): void {
    this.locationsService
      .fetchProvincesForSelect({ countryId: String(countryId) })
      .pipe(takeUntil(this.destroy$))
      .subscribe((rows) => {
        this.provinceOptions = rows;
        this.cdr.markForCheck();
      });
  }

  private hydrateSuburbCascadeFromFormKeys(): void {
    const countryId = this.toNumberOrNull(this.form.get('countryId')?.value);
    const provinceId = this.toNumberOrNull(this.form.get('provinceId')?.value);
    const districtId = this.toNumberOrNull(this.form.get('districtId')?.value);
    if (countryId == null && provinceId == null && districtId != null) {
      this.locationsService
        .findLocationById('district', districtId)
        .pipe(
          takeUntil(this.destroy$),
          mergeMap((d) => {
            if (!d) {
              return of(void 0);
            }
            const cid = this.toNumberOrNull(d['countryId']);
            const pid = this.toNumberOrNull(d['provinceId']);
            this.form.patchValue({ countryId: cid, provinceId: pid }, { emitEvent: false });
            if (cid == null) {
              return of(void 0);
            }
            return forkJoin({
              provinces: this.locationsService.fetchProvincesForSelect({ countryId: String(cid) }),
              districts:
                pid != null
                  ? this.locationsService.fetchDistrictsForSelect({ provinceId: String(pid) })
                  : of([] as LocationSelectOption[]),
            }).pipe(
              tap(({ provinces, districts }) => {
                this.provinceOptions = provinces;
                this.districtOptions = districts;
              }),
              map(() => void 0),
            );
          }),
        )
        .subscribe(() => this.cdr.markForCheck());
      return;
    }
    if (countryId != null) {
      this.locationsService
        .fetchProvincesForSelect({ countryId: String(countryId) })
        .pipe(takeUntil(this.destroy$))
        .subscribe((rows) => {
          this.provinceOptions = rows;
          this.cdr.markForCheck();
        });
    }
    if (provinceId != null) {
      this.locationsService
        .fetchDistrictsForSelect({ provinceId: String(provinceId) })
        .pipe(takeUntil(this.destroy$))
        .subscribe((rows) => {
          this.districtOptions = rows;
          this.cdr.markForCheck();
        });
    }
  }

  onDistrictCountryChange(): void {
    if (this.data.mode !== 'district') {
      return;
    }
    const countryId = this.toNumberOrNull(this.form.get('countryId')?.value);
    this.form.patchValue({ provinceId: null }, { emitEvent: false });
    this.provinceOptions = [];
    if (countryId == null) {
      this.cdr.markForCheck();
      return;
    }
    this.locationsService
      .fetchProvincesForSelect({ countryId: String(countryId) })
      .pipe(takeUntil(this.destroy$))
      .subscribe((rows) => {
        this.provinceOptions = rows;
        this.cdr.markForCheck();
      });
  }

  onSuburbCountryChange(): void {
    if (this.data.mode !== 'suburb') {
      return;
    }
    const countryId = this.toNumberOrNull(this.form.get('countryId')?.value);
    this.form.patchValue({ provinceId: null, districtId: null }, { emitEvent: false });
    this.provinceOptions = [];
    this.districtOptions = [];
    if (countryId == null) {
      this.cdr.markForCheck();
      return;
    }
    this.locationsService
      .fetchProvincesForSelect({ countryId: String(countryId) })
      .pipe(takeUntil(this.destroy$))
      .subscribe((rows) => {
        this.provinceOptions = rows;
        this.cdr.markForCheck();
      });
  }

  onSuburbProvinceChange(): void {
    if (this.data.mode !== 'suburb') {
      return;
    }
    const provinceId = this.toNumberOrNull(this.form.get('provinceId')?.value);
    this.form.patchValue({ districtId: null }, { emitEvent: false });
    this.districtOptions = [];
    if (provinceId == null) {
      this.cdr.markForCheck();
      return;
    }
    this.locationsService
      .fetchDistrictsForSelect({ provinceId: String(provinceId) })
      .pipe(takeUntil(this.destroy$))
      .subscribe((rows) => {
        this.districtOptions = rows;
        this.cdr.markForCheck();
      });
  }

  onCityVillageCountryChange(): void {
    if (this.data.mode !== 'city' && this.data.mode !== 'village') {
      return;
    }
    const countryId = this.toNumberOrNull(this.form.get('countryId')?.value);
    this.form.patchValue({ provinceId: null, districtId: null }, { emitEvent: false });
    this.provinceOptions = [];
    this.districtOptions = [];
    this.villageCityOptions = [];
    this.suburbOptions = [];
    if (countryId == null) {
      this.cdr.markForCheck();
      return;
    }
    this.locationsService
      .fetchProvincesForSelect({ countryId: String(countryId) })
      .pipe(takeUntil(this.destroy$))
      .subscribe((rows) => {
        this.provinceOptions = rows;
        this.cdr.markForCheck();
      });
  }

  onCityVillageProvinceChange(): void {
    if (this.data.mode !== 'city' && this.data.mode !== 'village') {
      return;
    }
    const provinceId = this.toNumberOrNull(this.form.get('provinceId')?.value);
    this.form.patchValue({ districtId: null }, { emitEvent: false });
    this.districtOptions = [];
    this.villageCityOptions = [];
    this.suburbOptions = [];
    if (provinceId == null) {
      this.cdr.markForCheck();
      this.reloadVillageDependentLists();
      return;
    }
    this.locationsService
      .fetchDistrictsForSelect({ provinceId: String(provinceId) })
      .pipe(takeUntil(this.destroy$))
      .subscribe((rows) => {
        this.districtOptions = rows;
        this.cdr.markForCheck();
        this.reloadVillageDependentLists();
      });
  }

  onCityVillageDistrictChange(): void {
    if (this.data.mode !== 'city' && this.data.mode !== 'village') {
      return;
    }
    if (this.data.mode === 'village') {
      this.form.patchValue({ cityId: null, suburbId: null }, { emitEvent: false });
    }
    this.reloadVillageDependentLists();
    this.cdr.markForCheck();
  }

  private reloadVillageDependentLists(): void {
    if (this.data.mode !== 'city' && this.data.mode !== 'village') {
      return;
    }
    const gen = ++this.villageDependentFetchGen;
    const districtId = this.toNumberOrNull(this.form.get('districtId')?.value);
    this.villageCityOptions = [];
    if (this.data.mode === 'village') {
      this.suburbOptions = [];
    }
    if (districtId == null) {
      this.villageListsLoading = false;
      this.cdr.markForCheck();
      return;
    }
    if (this.data.mode === 'city') {
      this.villageCityOptions = [];
      this.villageListsLoading = false;
      this.cdr.markForCheck();
      return;
    }
    this.villageListsLoading = true;
    forkJoin({
      parents: this.locationsService.fetchLocationNodeParentOptionsForDistrict(
        districtId,
        this.data.action === 'edit' ? this.data.id ?? null : null,
      ),
      suburbs: this.locationsService.fetchSuburbsForSelect({ districtId: String(districtId) }),
    })
      .pipe(
        finalize(() => {
          if (gen === this.villageDependentFetchGen) {
            this.villageListsLoading = false;
          }
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe(({ parents, suburbs }) => {
        if (gen !== this.villageDependentFetchGen) {
          return;
        }
        this.villageCityOptions = parents;
        this.suburbOptions = suburbs;
        this.ensureVillageCityOptionFromFindById();
        this.cdr.markForCheck();
      });
  }

  /**
   * When editing, the stored city may sit outside the district-filtered list; merge one option
   * from `find-by-id` so the native select can display the label.
   */
  private ensureVillageCityOptionFromFindById(): void {
    if (this.data.mode !== 'village') {
      return;
    }
    const cid = this.toNumberOrNull(this.form.get('cityId')?.value);
    if (cid == null || this.villageCityOptions.some((o) => o.id === cid)) {
      return;
    }
    this.locationsService
      .fetchLocationNodeSelectOptionById(cid)
      .pipe(takeUntil(this.destroy$))
      .subscribe((opt) => {
        if (!opt) {
          return;
        }
        this.villageCityOptions = [...this.villageCityOptions, opt].sort((a, b) =>
          a.label.localeCompare(b.label),
        );
        this.cdr.markForCheck();
      });
  }

  private reloadAddressSettlementOptions(): void {
    if (this.data.mode !== 'address') {
      return;
    }
    const gen = ++this.addressSettlementGen;
    const districtId = this.toNumberOrNull(this.form.get('districtId')?.value);
    const filter: Record<string, string> = districtId != null ? { districtId: String(districtId) } : {};
    this.addressSettlementRefreshing = true;
    forkJoin({
      suburbs: this.locationsService.fetchSuburbsForSelect(filter),
      villages: this.locationsService.fetchVillagesForSelect(filter),
    })
      .pipe(
        finalize(() => {
          if (gen === this.addressSettlementGen) {
            this.addressSettlementRefreshing = false;
          }
        }),
        takeUntil(this.destroy$),
      )
      .subscribe(({ suburbs, villages }) => {
        if (gen !== this.addressSettlementGen) {
          return;
        }
        this.suburbOptions = suburbs;
        this.villageOptions = villages;
      });
  }

  /**
   * After patching edit/view address values, load dependent dropdown options (province → district → city → settlement).
   */
  private hydrateAddressDropdownsFromForm(): void {
    const countryId = this.toNumberOrNull(this.form.get('countryId')?.value);
    const provinceId = this.toNumberOrNull(this.form.get('provinceId')?.value);
    const districtId = this.toNumberOrNull(this.form.get('districtId')?.value);
    if (countryId == null) {
      return;
    }
    const gen = ++this.addressCascadeGen;
    this.addressProvincesLoading = true;
    this.locationsService
      .fetchProvincesForSelect({ countryId: String(countryId) })
      .pipe(
        takeUntil(this.destroy$),
        mergeMap((provinces) => {
          if (gen !== this.addressCascadeGen) {
            return EMPTY;
          }
          this.provinceOptions = provinces;
          this.addressProvincesLoading = false;
          if (provinceId == null) {
            return EMPTY;
          }
          this.addressDistrictsLoading = true;
          return this.locationsService.fetchDistrictsForSelect({ provinceId: String(provinceId) });
        }),
        mergeMap((districts) => {
          if (gen !== this.addressCascadeGen) {
            return EMPTY;
          }
          this.districtOptions = districts;
          this.addressDistrictsLoading = false;
          if (districtId == null) {
            return EMPTY;
          }
          this.addressCitySettlementLoading = true;
          return forkJoin({
            cities: this.locationsService.fetchCitiesForSelect({ districtId: String(districtId) }),
            suburbs: this.locationsService.fetchSuburbsForSelect({ districtId: String(districtId) }),
            villages: this.locationsService.fetchVillagesForSelect({ districtId: String(districtId) }),
          });
        }),
        finalize(() => {
          if (gen === this.addressCascadeGen) {
            this.addressProvincesLoading = false;
            this.addressDistrictsLoading = false;
            this.addressCitySettlementLoading = false;
          }
        }),
      )
      .subscribe((bundle) => {
        if (gen !== this.addressCascadeGen || !bundle) {
          return;
        }
        const { cities, suburbs, villages } = bundle as {
          cities: LocationSelectOption[];
          suburbs: LocationSelectOption[];
          villages: LocationSelectOption[];
        };
        this.cityOptions = cities;
        this.suburbOptions = suburbs;
        this.villageOptions = villages;
      });
  }

  private toNumberOrNull(raw: unknown): number | null {
    const n = Number(raw);
    return Number.isFinite(n) && n > 0 ? n : null;
  }

  /** Province / district / suburb pickers for localized-name (by reference type). */
  get localizedNameShowsProvinceField(): boolean {
    if (this.data.mode !== 'localized-name') {
      return false;
    }
    const rt = String(this.form.get('referenceType')?.value ?? '').toUpperCase();
    return rt === 'PROVINCE' || rt === 'DISTRICT' || rt === 'SUBURB';
  }

  get localizedNameShowsDistrictField(): boolean {
    if (this.data.mode !== 'localized-name') {
      return false;
    }
    const rt = String(this.form.get('referenceType')?.value ?? '').toUpperCase();
    return rt === 'DISTRICT' || rt === 'SUBURB';
  }

  get localizedNameShowsSuburbField(): boolean {
    if (this.data.mode !== 'localized-name') {
      return false;
    }
    return String(this.form.get('referenceType')?.value ?? '').toUpperCase() === 'SUBURB';
  }

  private applyLocalizedNameInitialValue(value: Record<string, unknown>): void {
    const patch: Record<string, unknown> = { ...value };
    const refTypeRaw = String(patch['referenceType'] ?? 'COUNTRY').toUpperCase();
    const refId = this.toNumberOrNull(patch['referenceId']);
    delete patch['referenceId'];
    patch['referenceType'] = refTypeRaw;
    patch['countryId'] = null;
    patch['provinceId'] = null;
    patch['districtId'] = null;
    patch['suburbId'] = null;
    for (const field of LocationFormDialogComponent.ID_FIELDS) {
      if (field in patch) {
        patch[field] = this.toNumberOrNull(patch[field]);
      }
    }
    this.form.patchValue(patch, { emitEvent: false });
    this.syncLocalizedNameReferenceValidators();
    if (refId != null) {
      this.hydrateLocalizedNameCascadeFromReference(refTypeRaw, refId);
    }
  }

  private hydrateLocalizedNameCascadeFromReference(referenceType: string, referenceId: number): void {
    const gen = ++this.localizedNameCascadeGen;
    const rt = referenceType.toUpperCase();
    const finish = (): void => {
      this.syncLocalizedNameReferenceValidators();
      this.cdr.markForCheck();
    };

    if (rt === 'COUNTRY') {
      this.form.patchValue(
        { countryId: referenceId, provinceId: null, districtId: null, suburbId: null },
        { emitEvent: false },
      );
      finish();
      return;
    }

    if (rt === 'PROVINCE') {
      this.locationsService
        .findLocationById('province', referenceId)
        .pipe(takeUntil(this.destroy$))
        .subscribe((p) => {
          if (gen !== this.localizedNameCascadeGen) {
            return;
          }
          if (!p) {
            finish();
            return;
          }
          const countryId = this.toNumberOrNull(p['countryId']);
          this.form.patchValue(
            { countryId, provinceId: referenceId, districtId: null, suburbId: null },
            { emitEvent: false },
          );
          if (countryId != null) {
            this.locationsService
              .fetchProvincesForSelect({ countryId: String(countryId) })
              .pipe(takeUntil(this.destroy$))
              .subscribe((rows) => {
                if (gen !== this.localizedNameCascadeGen) {
                  return;
                }
                this.provinceOptions = rows;
                finish();
              });
          } else {
            finish();
          }
        });
      return;
    }

    if (rt === 'DISTRICT') {
      this.locationsService
        .findLocationById('district', referenceId)
        .pipe(
          mergeMap((d) => {
            if (gen !== this.localizedNameCascadeGen || !d) {
              return of({
                provinces: [] as LocationSelectOption[],
                districts: [] as LocationSelectOption[],
              });
            }
            const countryId = this.toNumberOrNull(d['countryId']);
            const provinceId = this.toNumberOrNull(d['provinceId']);
            this.form.patchValue(
              { countryId, provinceId, districtId: referenceId, suburbId: null },
              { emitEvent: false },
            );
            if (countryId == null) {
              return of({
                provinces: [] as LocationSelectOption[],
                districts: [] as LocationSelectOption[],
              });
            }
            return forkJoin({
              provinces: this.locationsService.fetchProvincesForSelect({ countryId: String(countryId) }),
              districts:
                provinceId != null
                  ? this.locationsService.fetchDistrictsForSelect({ provinceId: String(provinceId) })
                  : of([] as LocationSelectOption[]),
            });
          }),
          takeUntil(this.destroy$),
        )
        .subscribe((bundle) => {
          if (gen !== this.localizedNameCascadeGen) {
            return;
          }
          this.provinceOptions = bundle.provinces;
          this.districtOptions = bundle.districts;
          finish();
        });
      return;
    }

    if (rt === 'SUBURB') {
      this.locationsService
        .findLocationById('suburb', referenceId)
        .pipe(
          mergeMap((s) => {
            if (gen !== this.localizedNameCascadeGen || !s) {
              return of({
                provinces: [] as LocationSelectOption[],
                districts: [] as LocationSelectOption[],
                suburbs: [] as LocationSelectOption[],
              });
            }
            const countryId = this.toNumberOrNull(s['countryId']);
            const provinceId = this.toNumberOrNull(s['provinceId']);
            const districtId = this.toNumberOrNull(s['districtId']);
            this.form.patchValue(
              { countryId, provinceId, districtId, suburbId: referenceId },
              { emitEvent: false },
            );
            if (countryId == null) {
              return of({
                provinces: [] as LocationSelectOption[],
                districts: [] as LocationSelectOption[],
                suburbs: [] as LocationSelectOption[],
              });
            }
            return forkJoin({
              provinces: this.locationsService.fetchProvincesForSelect({ countryId: String(countryId) }),
              districts:
                provinceId != null
                  ? this.locationsService.fetchDistrictsForSelect({ provinceId: String(provinceId) })
                  : of([] as LocationSelectOption[]),
              suburbs:
                districtId != null
                  ? this.locationsService.fetchSuburbsForSelect({ districtId: String(districtId) })
                  : of([] as LocationSelectOption[]),
            });
          }),
          takeUntil(this.destroy$),
        )
        .subscribe((bundle) => {
          if (gen !== this.localizedNameCascadeGen) {
            return;
          }
          this.provinceOptions = bundle.provinces;
          this.districtOptions = bundle.districts;
          this.suburbOptions = bundle.suburbs;
          finish();
        });
      return;
    }

    finish();
  }

  private syncLocalizedNameReferenceValidators(): void {
    if (this.data.mode !== 'localized-name') {
      return;
    }
    const rt = String(this.form.get('referenceType')?.value ?? 'COUNTRY').toUpperCase();
    const cCountry = this.form.get('countryId');
    const cProvince = this.form.get('provinceId');
    const cDistrict = this.form.get('districtId');
    const cSuburb = this.form.get('suburbId');
    for (const c of [cCountry, cProvince, cDistrict, cSuburb]) {
      c?.clearValidators();
    }
    cCountry?.addValidators(Validators.required);
    if (rt === 'PROVINCE' || rt === 'DISTRICT' || rt === 'SUBURB') {
      cProvince?.addValidators(Validators.required);
    }
    if (rt === 'DISTRICT' || rt === 'SUBURB') {
      cDistrict?.addValidators(Validators.required);
    }
    if (rt === 'SUBURB') {
      cSuburb?.addValidators(Validators.required);
    }
    for (const c of [cCountry, cProvince, cDistrict, cSuburb]) {
      c?.updateValueAndValidity({ emitEvent: false });
    }
  }

  onLocalizedNameReferenceTypeChange(): void {
    if (this.data.mode !== 'localized-name') {
      return;
    }
    ++this.localizedNameCascadeGen;
    const rt = String(this.form.get('referenceType')?.value ?? 'COUNTRY').toUpperCase();
    if (rt === 'COUNTRY') {
      this.form.patchValue({ provinceId: null, districtId: null, suburbId: null }, { emitEvent: false });
      this.provinceOptions = [];
      this.districtOptions = [];
      this.suburbOptions = [];
    } else if (rt === 'PROVINCE') {
      this.form.patchValue({ provinceId: null, districtId: null, suburbId: null }, { emitEvent: false });
      this.districtOptions = [];
      this.suburbOptions = [];
      this.loadLocalizedNameProvinceOptions();
    } else if (rt === 'DISTRICT') {
      this.form.patchValue({ districtId: null, suburbId: null }, { emitEvent: false });
      this.suburbOptions = [];
      this.loadLocalizedNameDistrictOptions();
    } else if (rt === 'SUBURB') {
      this.form.patchValue({ suburbId: null }, { emitEvent: false });
      this.loadLocalizedNameSuburbOptions();
    }
    this.syncLocalizedNameReferenceValidators();
    this.cdr.markForCheck();
  }

  onLocalizedNameCountryChange(): void {
    if (this.data.mode !== 'localized-name') {
      return;
    }
    const gen = ++this.localizedNameCascadeGen;
    const countryId = this.toNumberOrNull(this.form.get('countryId')?.value);
    const rt = String(this.form.get('referenceType')?.value ?? 'COUNTRY').toUpperCase();
    this.form.patchValue({ provinceId: null, districtId: null, suburbId: null }, { emitEvent: false });
    this.provinceOptions = [];
    this.districtOptions = [];
    this.suburbOptions = [];
    if (countryId == null || rt === 'COUNTRY') {
      this.cdr.markForCheck();
      return;
    }
    this.locationsService
      .fetchProvincesForSelect({ countryId: String(countryId) })
      .pipe(takeUntil(this.destroy$))
      .subscribe((rows) => {
        if (gen !== this.localizedNameCascadeGen) {
          return;
        }
        this.provinceOptions = rows;
        this.cdr.markForCheck();
      });
  }

  onLocalizedNameProvinceChange(): void {
    if (this.data.mode !== 'localized-name') {
      return;
    }
    const gen = ++this.localizedNameCascadeGen;
    const provinceId = this.toNumberOrNull(this.form.get('provinceId')?.value);
    const rt = String(this.form.get('referenceType')?.value ?? 'COUNTRY').toUpperCase();
    this.form.patchValue({ districtId: null, suburbId: null }, { emitEvent: false });
    this.districtOptions = [];
    this.suburbOptions = [];
    if (provinceId == null || rt === 'PROVINCE') {
      this.cdr.markForCheck();
      return;
    }
    this.locationsService
      .fetchDistrictsForSelect({ provinceId: String(provinceId) })
      .pipe(takeUntil(this.destroy$))
      .subscribe((rows) => {
        if (gen !== this.localizedNameCascadeGen) {
          return;
        }
        this.districtOptions = rows;
        this.cdr.markForCheck();
      });
  }

  onLocalizedNameDistrictChange(): void {
    if (this.data.mode !== 'localized-name') {
      return;
    }
    const gen = ++this.localizedNameCascadeGen;
    const districtId = this.toNumberOrNull(this.form.get('districtId')?.value);
    const rt = String(this.form.get('referenceType')?.value ?? 'COUNTRY').toUpperCase();
    this.form.patchValue({ suburbId: null }, { emitEvent: false });
    this.suburbOptions = [];
    if (districtId == null || rt !== 'SUBURB') {
      this.cdr.markForCheck();
      return;
    }
    this.locationsService
      .fetchSuburbsForSelect({ districtId: String(districtId) })
      .pipe(takeUntil(this.destroy$))
      .subscribe((rows) => {
        if (gen !== this.localizedNameCascadeGen) {
          return;
        }
        this.suburbOptions = rows;
        this.cdr.markForCheck();
      });
  }

  private loadLocalizedNameProvinceOptions(): void {
    const countryId = this.toNumberOrNull(this.form.get('countryId')?.value);
    if (countryId == null) {
      return;
    }
    const gen = ++this.localizedNameCascadeGen;
    this.locationsService
      .fetchProvincesForSelect({ countryId: String(countryId) })
      .pipe(takeUntil(this.destroy$))
      .subscribe((rows) => {
        if (gen !== this.localizedNameCascadeGen) {
          return;
        }
        this.provinceOptions = rows;
        this.cdr.markForCheck();
      });
  }

  private loadLocalizedNameDistrictOptions(): void {
    const provinceId = this.toNumberOrNull(this.form.get('provinceId')?.value);
    if (provinceId == null) {
      return;
    }
    const gen = ++this.localizedNameCascadeGen;
    this.locationsService
      .fetchDistrictsForSelect({ provinceId: String(provinceId) })
      .pipe(takeUntil(this.destroy$))
      .subscribe((rows) => {
        if (gen !== this.localizedNameCascadeGen) {
          return;
        }
        this.districtOptions = rows;
        this.cdr.markForCheck();
      });
  }

  private loadLocalizedNameSuburbOptions(): void {
    const districtId = this.toNumberOrNull(this.form.get('districtId')?.value);
    if (districtId == null) {
      return;
    }
    const gen = ++this.localizedNameCascadeGen;
    this.locationsService
      .fetchSuburbsForSelect({ districtId: String(districtId) })
      .pipe(takeUntil(this.destroy$))
      .subscribe((rows) => {
        if (gen !== this.localizedNameCascadeGen) {
          return;
        }
        this.suburbOptions = rows;
        this.cdr.markForCheck();
      });
  }

}
