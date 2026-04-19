import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export type LocationFormDialogMode =
  | 'country'
  | 'province'
  | 'district'
  | 'suburb'
  | 'admin-level'
  | 'city'
  | 'village';

@Component({
  selector: 'app-location-form-dialog',
  templateUrl: './location-form-dialog.component.html',
  styleUrl: './location-form-dialog.component.scss',
  standalone: false,
})
export class LocationFormDialogComponent {
  form: FormGroup;
  submitting = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<LocationFormDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) readonly data: { mode: LocationFormDialogMode },
  ) {
    this.form = this.buildForm(data.mode);
  }

  get title(): string {
    const t: Record<LocationFormDialogMode, string> = {
      country: 'Add country',
      province: 'Add province',
      district: 'Add district',
      suburb: 'Add suburb',
      'admin-level': 'Add administrative level',
      city: 'Add city (location node)',
      village: 'Add village (location node)',
    };
    return t[this.data.mode];
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    // Mock-only: no HTTP yet; parent reloads list from service (unchanged) — dialog still confirms flow.
    queueMicrotask(() => {
      this.submitting = false;
      this.dialogRef.close(true);
    });
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
}
