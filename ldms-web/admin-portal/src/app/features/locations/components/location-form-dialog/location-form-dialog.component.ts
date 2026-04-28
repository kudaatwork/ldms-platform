import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Observable, Subject, catchError, finalize, forkJoin, map, of, takeUntil, tap } from 'rxjs';
import { LocationsService, type LocationSelectOption } from '../../services/locations.service';

export type LocationFormDialogMode =
  | 'country'
  | 'province'
  | 'district'
  | 'suburb'
  | 'admin-level'
  | 'city'
  | 'village';

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
}

@Component({
  selector: 'app-location-form-dialog',
  templateUrl: './location-form-dialog.component.html',
  styleUrl: './location-form-dialog.component.scss',
  standalone: false,
})
export class LocationFormDialogComponent implements OnInit, OnDestroy {
  form: FormGroup;
  /** Mat-select strict equality breaks when API ids are strings vs numbers — normalize here. */
  readonly compareOptionIds = (a: unknown, b: unknown): boolean =>
    (a == null && b == null) || (a != null && b != null && Number(a) === Number(b));
  submitting = false;
  loading = false;
  /** Loading FK lists for mat-select fields. */
  listsLoading = false;
  listLoadError: string | null = null;
  saveError: string | null = null;

  countryOptions: LocationSelectOption[] = [];
  provinceOptions: LocationSelectOption[] = [];
  districtOptions: LocationSelectOption[] = [];
  adminLevelOptions: LocationSelectOption[] = [];
  parentLocationOptions: LocationSelectOption[] = [];

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly locationsService: LocationsService,
    private readonly dialogRef: MatDialogRef<LocationFormDialogComponent, LocationFormDialogResult>,
    @Inject(MAT_DIALOG_DATA) readonly data: LocationFormDialogData,
  ) {
    this.form = this.buildForm(data.mode);
  }

  ngOnInit(): void {
    if (this.needsFkLists()) {
      this.listsLoading = true;
      this.loadListsForMode()
        .pipe(
          finalize(() => {
            this.listsLoading = false;
          }),
          takeUntil(this.destroy$),
        )
        .subscribe({
          next: () => this.afterListsReady(),
          error: () => {
            this.listLoadError = 'Could not load dropdown data. You can still type IDs manually after refresh.';
            this.afterListsReady();
          },
        });
    } else {
      this.afterListsReady();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private needsFkLists(): boolean {
    return this.data.mode !== 'country';
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
          provinces: this.locationsService.fetchProvincesForSelect(),
          adminLevels: this.locationsService.fetchAdministrativeLevelsForSelect(),
        }).pipe(
          tap(({ provinces, adminLevels }) => {
            this.provinceOptions = provinces;
            this.adminLevelOptions = adminLevels;
          }),
          map(() => void 0),
          catchError(() => {
            this.provinceOptions = [];
            this.adminLevelOptions = [];
            return of(void 0);
          }),
        );
      case 'suburb':
        return forkJoin({
          districts: this.locationsService.fetchDistrictsForSelect(),
          adminLevels: this.locationsService.fetchAdministrativeLevelsForSelect(),
        }).pipe(
          tap(({ districts, adminLevels }) => {
            this.districtOptions = districts;
            this.adminLevelOptions = adminLevels;
          }),
          map(() => void 0),
          catchError(() => {
            this.districtOptions = [];
            this.adminLevelOptions = [];
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
        return this.locationsService.fetchLocationNodesForParentSelect().pipe(
          tap((parents) => {
            this.parentLocationOptions = parents;
          }),
          map(() => void 0),
          catchError(() => {
            this.parentLocationOptions = [];
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
        create: 'Add a district under an existing province.',
        edit: 'Update district details. Province selection is required.',
        view: 'Review district details. Editing is disabled in view mode.',
      },
      suburb: {
        create: 'Add a suburb under an existing district.',
        edit: 'Update suburb details. District selection is required.',
        view: 'Review suburb details. Editing is disabled in view mode.',
      },
      'admin-level': {
        create: 'Define a tier in the administrative hierarchy (e.g. province, district, suburb).',
        edit: 'Update administrative level metadata.',
        view: 'Review administrative level details.',
      },
      city: {
        create: 'Add a city. Link it to a parent province or district.',
        edit: 'Update city details and coordinates.',
        view: 'Review city details. Editing is disabled in view mode.',
      },
      village: {
        create: 'Add a village. Link it to a parent district or city.',
        edit: 'Update village details and coordinates.',
        view: 'Review village details. Editing is disabled in view mode.',
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

    if (this.listsLoading) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.saveError = null;

    const payload = this.toSubmitPayload();
    const action$ =
      this.data.action === 'edit' && this.data.id
        ? this.locationsService.updateLocation(this.data.mode, this.data.id, payload)
        : this.locationsService.createLocation(this.data.mode, payload);

    action$
      .pipe(
        finalize(() => (this.submitting = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
      next: (response) => this.dialogRef.close({ saved: true, message: response.message }),
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
      },
    });
  }

  private fetchById(id: number): void {
    this.loading = true;
    this.form.disable();
    this.locationsService
      .findLocationById(this.data.mode, id)
      .pipe(
        finalize(() => {
          this.loading = false;
          if (!this.isReadonly) {
            this.form.enable();
          }
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (entity) => {
          if (entity) {
            this.applyInitialValue(entity);
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
    const patch: Record<string, unknown> = { ...value };
    if (
      (this.data.mode === 'city' || this.data.mode === 'village') &&
      Array.isArray(patch['aliases'])
    ) {
      patch['aliases'] = (patch['aliases'] as string[]).join(', ');
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
          districtId: [null as number | null, Validators.required],
          administrativeLevelId: [null as number | null],
          latitude: [null as number | null],
          longitude: [null as number | null],
        });
      case 'admin-level':
        return this.fb.group({
          name: ['', Validators.required],
          code: [''],
          level: [null as number | null, [Validators.required, Validators.min(1)]],
          countryId: [null as number | null, Validators.required],
          description: [''],
        });
      case 'city':
      case 'village':
        return this.fb.group({
          name: ['', Validators.required],
          code: [''],
          parentId: [null as number | null],
          latitude: [null as number | null],
          longitude: [null as number | null],
          timezone: [''],
          postalCode: [''],
          aliases: [''],
        });
      default:
        return this.fb.group({});
    }
  }

  private toSubmitPayload(): Record<string, unknown> {
    const raw = this.form.getRawValue() as Record<string, unknown>;
    if (this.data.mode === 'city' || this.data.mode === 'village') {
      const aliases = String(raw['aliases'] ?? '')
        .split(',')
        .map((value) => value.trim())
        .filter((value) => value.length > 0);
      return {
        ...raw,
        locationType: this.data.mode === 'city' ? 'CITY' : 'VILLAGE',
        aliases,
      };
    }
    return raw;
  }

}
