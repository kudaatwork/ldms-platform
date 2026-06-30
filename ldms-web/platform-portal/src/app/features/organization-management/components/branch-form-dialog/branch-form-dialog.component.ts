import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, Inject, OnDestroy, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs';
import * as L from 'leaflet';
import { BranchDetail } from '../../../../core/services/organization.service';
import { OrgManagementPortalService } from '../../services/org-management-portal.service';

/** Centre of Zimbabwe — default map view when a branch has no coordinates yet. */
const DEFAULT_CENTER: L.LatLngExpression = [-19.0154, 29.1549];

export type BranchFormDialogAction = 'create' | 'edit' | 'view';
export type BranchFormDialogMode = 'top-level' | 'sub-level';

export interface BranchFormDialogData {
  action: BranchFormDialogAction;
  mode: BranchFormDialogMode;
  row?: BranchDetail | null;
  parentBranches?: BranchDetail[];
}

export interface BranchFormDialogResult {
  saved: boolean;
}

@Component({
  selector: 'app-branch-form-dialog',
  templateUrl: './branch-form-dialog.component.html',
  styleUrl: './branch-form-dialog.component.scss',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
  ],
})
export class BranchFormDialogComponent implements AfterViewInit, OnDestroy {
  @ViewChild('mapHost') mapHost?: ElementRef<HTMLDivElement>;

  form: FormGroup;
  submitting = false;
  saveError: string | null = null;

  private map?: L.Map;
  private marker?: L.CircleMarker;

  constructor(
    private readonly fb: FormBuilder,
    private readonly orgMgmt: OrgManagementPortalService,
    private readonly snackBar: MatSnackBar,
    private readonly dialogRef: MatDialogRef<BranchFormDialogComponent, BranchFormDialogResult>,
    @Inject(MAT_DIALOG_DATA) readonly data: BranchFormDialogData,
  ) {
    const row = data.row;
    this.form = this.fb.group({
      branchName: [row?.branchName ?? '', [Validators.required, Validators.maxLength(200)]],
      branchCode: [row?.branchCode ?? '', Validators.maxLength(50)],
      region: [row?.region ?? '', Validators.maxLength(100)],
      email: [row?.email ?? '', [Validators.email, Validators.maxLength(255)]],
      phoneNumber: [row?.phoneNumber ?? '', Validators.maxLength(50)],
      businessHours: [row?.businessHours ?? '', Validators.maxLength(200)],
      headOffice: [row?.headOffice ?? false],
      active: [row?.active ?? true],
      parentBranchId: [row?.parentBranchId ?? null],
      depot: [row?.depot ?? false],
      latitude: [row?.latitude ?? null],
      longitude: [row?.longitude ?? null],
    });

    if (data.mode === 'sub-level' && data.action === 'create') {
      this.form.get('parentBranchId')?.setValidators([Validators.required]);
    }
    if (data.action === 'view') {
      this.form.disable();
    }
  }

  ngAfterViewInit(): void {
    // Defer so the dialog has finished its open animation and the container has size.
    setTimeout(() => this.initMap(), 250);
  }

  ngOnDestroy(): void {
    this.map?.remove();
    this.map = undefined;
  }

  get hasCoordinates(): boolean {
    return this.form.get('latitude')?.value != null && this.form.get('longitude')?.value != null;
  }

  /** Re-centre the marker from the manually typed lat/long inputs. */
  applyTypedCoordinates(): void {
    const lat = Number(this.form.get('latitude')?.value);
    const lng = Number(this.form.get('longitude')?.value);
    if (Number.isFinite(lat) && Number.isFinite(lng) && this.map) {
      this.setPoint(lat, lng, true);
    }
  }

  clearCoordinates(): void {
    if (this.data.action === 'view') {
      return;
    }
    this.form.patchValue({ latitude: null, longitude: null });
    if (this.marker) {
      this.marker.remove();
      this.marker = undefined;
    }
  }

  private initMap(): void {
    if (!this.mapHost?.nativeElement || this.map) {
      return;
    }
    const lat = Number(this.form.get('latitude')?.value);
    const lng = Number(this.form.get('longitude')?.value);
    const hasPoint = Number.isFinite(lat) && Number.isFinite(lng);

    this.map = L.map(this.mapHost.nativeElement, {
      center: hasPoint ? [lat, lng] : DEFAULT_CENTER,
      zoom: hasPoint ? 13 : 6,
      attributionControl: false,
    });
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(this.map);

    if (hasPoint) {
      this.renderMarker(lat, lng);
    }

    if (this.data.action !== 'view') {
      this.map.on('click', (e: L.LeafletMouseEvent) => {
        this.setPoint(e.latlng.lat, e.latlng.lng, false);
      });
    }

    // Maps inside dialogs often need a size recalculation once visible.
    setTimeout(() => this.map?.invalidateSize(), 50);
  }

  private setPoint(lat: number, lng: number, recenter: boolean): void {
    const rounded = (n: number) => Math.round(n * 1_000_000) / 1_000_000;
    const rlat = rounded(lat);
    const rlng = rounded(lng);
    this.form.patchValue({ latitude: rlat, longitude: rlng });
    this.renderMarker(rlat, rlng);
    if (recenter) {
      this.map?.setView([rlat, rlng], Math.max(this.map.getZoom(), 13));
    }
  }

  private renderMarker(lat: number, lng: number): void {
    if (!this.map) {
      return;
    }
    if (this.marker) {
      this.marker.setLatLng([lat, lng]);
    } else {
      this.marker = L.circleMarker([lat, lng], {
        radius: 9,
        color: '#2563eb',
        fillColor: '#3b82f6',
        fillOpacity: 0.9,
        weight: 3,
      }).addTo(this.map);
    }
  }

  get isEdit(): boolean {
    return this.data.action === 'edit';
  }

  get isView(): boolean {
    return this.data.action === 'view';
  }

  get isSubLevel(): boolean {
    return this.data.mode === 'sub-level';
  }

  get title(): string {
    if (this.isView) {
      return this.isSubLevel ? 'View sub-branch' : 'View branch';
    }
    if (this.isEdit) {
      return this.isSubLevel ? 'Edit sub-branch' : 'Edit branch';
    }
    return this.isSubLevel ? 'Add sub-branch or depot' : 'Add branch';
  }

  get subtitle(): string {
    if (this.isView) {
      return 'Branch details — read only.';
    }
    if (this.isEdit) {
      return 'Update the branch details below.';
    }
    return this.isSubLevel
      ? 'Register a sub-branch or depot under an existing top-level branch.'
      : 'Register a new top-level branch for your organisation.';
  }

  get primaryActionLabel(): string {
    return this.isEdit ? 'Update' : 'Save';
  }

  get primaryActionLoadingLabel(): string {
    return this.isEdit ? 'Updating…' : 'Saving…';
  }

  get parentBranchOptions(): BranchDetail[] {
    return this.data.parentBranches ?? [];
  }

  hasError(control: string, error: string): boolean {
    const c = this.form.get(control);
    return !!(c && (c.touched || c.dirty) && c.hasError(error));
  }

  cancel(): void {
    this.dialogRef.close({ saved: false });
  }

  save(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const payload = this.toPayload();
    this.submitting = true;
    this.saveError = null;

    const request$ =
      this.isEdit && this.data.row?.id
        ? this.orgMgmt.updateBranch(this.data.row.id, payload)
        : this.orgMgmt.addBranch(payload);

    request$.pipe(finalize(() => (this.submitting = false))).subscribe({
      next: () => {
        this.snackBar.open(
          this.isEdit ? 'Branch updated.' : 'Branch created.',
          'Close',
          { duration: 3000 },
        );
        this.dialogRef.close({ saved: true });
      },
      error: (err: Error) => {
        this.saveError = err.message ?? 'Failed to save this branch. Please check inputs and try again.';
        this.snackBar.open(this.saveError, 'Close', { duration: 5000 });
      },
    });
  }

  private toPayload() {
    const v = this.form.getRawValue();
    return {
      branchName: String(v.branchName ?? '').trim(),
      branchCode: this.optionalString(v.branchCode),
      region: this.optionalString(v.region),
      email: this.optionalString(v.email),
      phoneNumber: this.optionalString(v.phoneNumber),
      businessHours: this.optionalString(v.businessHours),
      headOffice: Boolean(v.headOffice),
      active: Boolean(v.active),
      parentBranchId: this.isSubLevel && v.parentBranchId ? Number(v.parentBranchId) : undefined,
      depot: this.isSubLevel ? Boolean(v.depot) : false,
      latitude: this.optionalNumber(v.latitude),
      longitude: this.optionalNumber(v.longitude),
    };
  }

  private optionalNumber(value: unknown): number | undefined {
    if (value === null || value === undefined || value === '') {
      return undefined;
    }
    const n = Number(value);
    return Number.isFinite(n) ? n : undefined;
  }

  private optionalString(value: unknown): string | undefined {
    const s = String(value ?? '').trim();
    return s.length > 0 ? s : undefined;
  }
}
