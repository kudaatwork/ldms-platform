import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs';
import { LocationsService } from '../../services/locations.service';

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

@Component({
  selector: 'app-location-form-dialog',
  templateUrl: './location-form-dialog.component.html',
  styleUrl: './location-form-dialog.component.scss',
  standalone: false,
})
export class LocationFormDialogComponent implements OnInit {
  form: FormGroup;
  submitting = false;
  loading = false;
  saveError: string | null = null;

  constructor(
    private readonly fb: FormBuilder,
    private readonly locationsService: LocationsService,
    private readonly dialogRef: MatDialogRef<LocationFormDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) readonly data: LocationFormDialogData,
  ) {
    this.form = this.buildForm(data.mode);
  }

  ngOnInit(): void {
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
      country: 'country',
      province: 'province',
      district: 'district',
      suburb: 'suburb',
      'admin-level': 'administrative level',
      city: 'city (location node)',
      village: 'village (location node)',
    };
    return `${verb} ${noun[this.data.mode]}`;
  }

  get isReadonly(): boolean {
    return this.data.action === 'view';
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  save(): void {
    if (this.isReadonly) {
      this.dialogRef.close(false);
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

    action$.pipe(finalize(() => (this.submitting = false))).subscribe({
      next: () => this.dialogRef.close(true),
      error: (err) => {
        this.saveError =
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
      .pipe(finalize(() => {
        this.loading = false;
        if (!this.isReadonly) {
          this.form.enable();
        }
      }))
      .subscribe({
        next: (entity) => {
          if (entity) {
            this.applyInitialValue(entity);
          }
        },
        error: (err) => {
          this.saveError =
            err?.error?.message || err?.message || 'Failed to load record details.';
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
          currencyCode: [''],
          geoCoordinatesId: [null as number | null],
        });
      case 'province':
        return this.fb.group({
          name: ['', Validators.required],
          code: [''],
          countryId: [null as number | null, Validators.required],
          administrativeLevelId: [null as number | null],
          geoCoordinatesId: [null as number | null],
        });
      case 'district':
        return this.fb.group({
          name: ['', Validators.required],
          code: [''],
          provinceId: [null as number | null, Validators.required],
          administrativeLevelId: [null as number | null],
          geoCoordinatesId: [null as number | null],
        });
      case 'suburb':
        return this.fb.group({
          name: ['', Validators.required],
          code: [''],
          postalCode: [''],
          districtId: [null as number | null, Validators.required],
          geoCoordinatesId: [null as number | null],
          administrativeLevelId: [null as number | null],
        });
      case 'admin-level':
        return this.fb.group({
          name: ['', Validators.required],
          code: [''],
          level: [null as number | null, Validators.required],
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
