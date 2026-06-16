import { CommonModule } from '@angular/common';
import { Component, Inject, OnDestroy, OnInit, Optional } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Subject, finalize, takeUntil } from 'rxjs';
import { normalizeBase64, resolveFilePreview } from '../../../../shared/utils/file-upload-preview';
import {
  FleetDocumentViewerDialogComponent,
  type FleetDocumentViewerDialogData,
} from '../fleet-document-viewer-dialog/fleet-document-viewer-dialog.component';
import type {
  CreateFleetCompliancePayload,
  EditFleetCompliancePayload,
  FleetComplianceRow,
  FleetComplianceSubjectType,
  FleetComplianceType,
  FleetDriverRow,
  FleetRegistrationDocumentPayload,
  FleetVehicleRow,
} from '../../models/fleet.model';
import { FleetPortalService, isCompliancePendingReview } from '../../services/fleet-portal.service';

export type FleetComplianceDialogData = {
  record?: FleetComplianceRow;
  assets: FleetVehicleRow[];
  drivers: FleetDriverRow[];
};

const COMPLIANCE_TYPES: { value: FleetComplianceType; label: string }[] = [
  { value: 'vehicle_registration', label: 'Vehicle registration book' },
  { value: 'road_license', label: 'Road licence disc' },
  { value: 'roadworthiness', label: 'Certificate of fitness' },
  { value: 'insurance', label: 'Commercial insurance' },
  { value: 'goods_operator_licence', label: "Goods operator's licence" },
  { value: 'permit', label: 'Vehicle operator disc' },
  { value: 'hazardous_substances_permit', label: 'Hazardous substances permit' },
  { value: 'fire_safety_clearance', label: 'Fire safety clearance' },
  { value: 'lease_hire_agreement', label: 'Lease / hire agreement' },
  { value: 'license', label: "Driver's licence" },
  { value: 'defensive_driving_certificate', label: 'Defensive driving certificate' },
  { value: 'driver_medical_certificate', label: 'Driver medical certificate' },
  { value: 'maintenance', label: 'Maintenance record' },
  { value: 'other', label: 'Other' },
];

const SUBJECT_TYPES: { value: FleetComplianceSubjectType; label: string }[] = [
  { value: 'asset', label: 'Fleet asset' },
  { value: 'driver', label: 'Driver' },
];

@Component({
  selector: 'app-fleet-compliance-dialog',
  templateUrl: './fleet-compliance-dialog.component.html',
  styleUrl: './fleet-compliance-dialog.component.scss',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
})
export class FleetComplianceDialogComponent implements OnInit, OnDestroy {
  readonly complianceTypes = COMPLIANCE_TYPES;
  readonly subjectTypes = SUBJECT_TYPES;
  readonly isEdit: boolean;
  readonly title: string;
  readonly subtitle: string;
  readonly assets: FleetVehicleRow[];
  readonly drivers: FleetDriverRow[];
  readonly form: FormGroup;
  submitting = false;
  saveError = '';

  documentLoading = false;
  documentError = '';
  documentFileName = '';
  previewImageUrl: string | null = null;
  previewPdfUrl: SafeResourceUrl | null = null;
  documentUploading = false;
  pendingDocumentName = '';
  private documentDto: Record<string, unknown> | null = null;

  private readonly recordId?: number;
  private readonly existingRecord?: FleetComplianceRow;
  readonly attachedFileUploadId?: number;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<FleetComplianceDialogComponent, FleetComplianceRow | undefined>,
    private readonly dialog: MatDialog,
    private readonly fleet: FleetPortalService,
    private readonly sanitizer: DomSanitizer,
    @Optional() @Inject(MAT_DIALOG_DATA) data: FleetComplianceDialogData | null,
  ) {
    this.dialogRef.disableClose = true;
    const record = data?.record;
    this.existingRecord = record;
    this.isEdit = !!record;
    this.assets = data?.assets ?? [];
    this.drivers = data?.drivers ?? [];
    this.title = record ? 'Edit compliance record' : 'Add compliance record';
    this.subtitle = record
      ? 'Update expiry, status, or notes for this compliance item.'
      : 'Track insurance, licenses, maintenance, and other compliance documents.';
    this.recordId = record?.id;
    this.attachedFileUploadId = record?.fileUploadId;

    const expiresAt = record?.expiresAt ? record.expiresAt.slice(0, 10) : '';

    this.form = this.fb.group({
      subjectType: [{ value: record?.subjectType ?? 'asset', disabled: this.isEdit }, Validators.required],
      subjectId: [{ value: record?.subjectId ?? null, disabled: this.isEdit }, Validators.required],
      complianceType: [record?.complianceType ?? 'insurance', Validators.required],
      fileUploadId: [record?.fileUploadId ?? null],
      expiresAt: [expiresAt],
      status: [record?.status ?? 'PENDING'],
      notes: [record?.notes ?? ''],
    });
  }

  ngOnInit(): void {
    if (this.attachedFileUploadId) {
      this.loadAttachedDocument(this.attachedFileUploadId);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get subjectType(): FleetComplianceSubjectType {
    return this.form.get('subjectType')?.value as FleetComplianceSubjectType;
  }

  get hasDocumentPreview(): boolean {
    return !!this.previewImageUrl || !!this.previewPdfUrl;
  }

  get canOpenPdfInNewTab(): boolean {
    if (!this.documentDto) {
      return false;
    }
    const b64 = normalizeBase64(String(this.documentDto['fileContent'] ?? ''));
    if (!b64) {
      return false;
    }
    const preview = resolveFilePreview(this.documentDto);
    return preview?.kind === 'pdf';
  }

  get canOpenFullView(): boolean {
    return !!this.attachedFileUploadId && (!!this.hasDocumentPreview || !!this.documentFileName);
  }

  get isPendingReview(): boolean {
    return this.isEdit && isCompliancePendingReview(this.existingRecord?.status ?? this.form.get('status')?.value);
  }

  get subjectOptions(): { id: number; label: string }[] {
    if (this.subjectType === 'driver') {
      return this.drivers.map((d) => ({ id: d.id, label: d.fullName }));
    }
    return this.assets
      .filter((a) => typeof a.id === 'number')
      .map((a) => ({ id: Number(a.id), label: `${a.registration} · ${a.makeModel}` }));
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.form.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || control.dirty);
  }

  cancel(): void {
    if (!this.submitting) {
      this.dialogRef.close();
    }
  }

  approveReview(): void {
    if (!this.isEdit || this.recordId == null || this.submitting) {
      return;
    }
    const v = this.form.getRawValue();
    this.submitting = true;
    this.saveError = '';
    this.fleet
      .updateCompliance(this.recordId, {
        expiresAt: v.expiresAt ? `${v.expiresAt}T23:59:59` : undefined,
        notes: String(v.notes ?? '').trim() || undefined,
      })
      .pipe(
        finalize(() => (this.submitting = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (row) => this.dialogRef.close(row),
        error: (err: Error) => {
          this.saveError = err.message ?? 'Could not approve compliance record.';
        },
      });
  }

  rejectReview(): void {
    if (!this.isEdit || this.recordId == null || this.submitting) {
      return;
    }
    const v = this.form.getRawValue();
    this.submitting = true;
    this.saveError = '';
    this.fleet
      .updateCompliance(this.recordId, {
        status: 'REVOKED',
        expiresAt: v.expiresAt ? `${v.expiresAt}T23:59:59` : undefined,
        notes: String(v.notes ?? '').trim() || undefined,
      })
      .pipe(
        finalize(() => (this.submitting = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (row) => this.dialogRef.close(row),
        error: (err: Error) => {
          this.saveError = err.message ?? 'Could not reject compliance record.';
        },
      });
  }

  onAddDocumentSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    if (!file) {
      return;
    }

    const v = this.form.getRawValue();
    const subjectId = Number(v.subjectId);
    if (!Number.isFinite(subjectId) || subjectId < 1) {
      this.saveError = 'Select a subject before uploading a document.';
      return;
    }

    this.saveError = '';
    this.documentUploading = true;
    this.pendingDocumentName = file.name;

    const subjectType = v.subjectType as FleetComplianceSubjectType;
    const complianceType = v.complianceType as FleetComplianceType;
    const upload$ =
      subjectType === 'driver'
        ? this.fleet.uploadFleetDriverDocument(subjectId, file, this.driverDocumentFileType(complianceType))
        : this.fleet.uploadFleetAssetDocument(
            subjectId,
            file,
            this.assetDocumentComplianceType(complianceType),
          );

    upload$
      .pipe(
        finalize(() => (this.documentUploading = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (id) => {
          this.form.patchValue({ fileUploadId: id });
        },
        error: (err: Error) => {
          this.pendingDocumentName = '';
          this.saveError = err.message ?? 'Could not upload document.';
        },
      });
  }

  openPdfInNewTab(): void {
    const d = this.documentDto;
    if (!d) {
      return;
    }
    const b64 = normalizeBase64(String(d['fileContent'] ?? ''));
    if (!b64) {
      return;
    }
    try {
      const binary = atob(b64);
      const bytes = new Uint8Array(binary.length);
      for (let i = 0; i < binary.length; i++) {
        bytes[i] = binary.charCodeAt(i);
      }
      const blob = new Blob([bytes], { type: 'application/pdf' });
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank', 'noopener,noreferrer');
      setTimeout(() => URL.revokeObjectURL(url), 120_000);
    } catch {
      this.saveError = 'Could not open PDF in a new tab.';
    }
  }

  openFullDocumentView(): void {
    if (!this.attachedFileUploadId) {
      return;
    }
    this.dialog.open(FleetDocumentViewerDialogComponent, {
      width: '960px',
      maxWidth: '96vw',
      maxHeight: '96vh',
      panelClass: 'lx-dialog-panel--wide',
      data: {
        fileUploadId: this.attachedFileUploadId,
        title: this.documentFileName || 'Compliance document',
      } satisfies FleetDocumentViewerDialogData,
    });
  }

  save(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.getRawValue();
    this.submitting = true;
    this.saveError = '';

    if (this.isEdit && this.recordId != null) {
      const payload: EditFleetCompliancePayload = {
        expiresAt: v.expiresAt ? `${v.expiresAt}T23:59:59` : undefined,
        status: String(v.status ?? 'ACTIVE').trim() || undefined,
        notes: String(v.notes ?? '').trim() || undefined,
      };
      this.fleet
        .updateCompliance(this.recordId, payload)
        .pipe(
          finalize(() => (this.submitting = false)),
          takeUntil(this.destroy$),
        )
        .subscribe({
          next: (row) => this.dialogRef.close(row),
          error: (err: Error) => {
            this.saveError = err.message ?? 'Could not save compliance record.';
          },
        });
      return;
    }

    const payload: CreateFleetCompliancePayload = {
      subjectType: v.subjectType as FleetComplianceSubjectType,
      subjectId: Number(v.subjectId),
      complianceType: v.complianceType as FleetComplianceType,
      fileUploadId: v.fileUploadId != null && v.fileUploadId !== '' ? Number(v.fileUploadId) : undefined,
      expiresAt: v.expiresAt ? `${v.expiresAt}T23:59:59` : undefined,
      notes: String(v.notes ?? '').trim() || undefined,
    };

    this.fleet
      .createCompliance(payload)
      .pipe(
        finalize(() => (this.submitting = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (row) => this.dialogRef.close(row),
        error: (err: Error) => {
          this.saveError = err.message ?? 'Could not save compliance record.';
        },
      });
  }

  private loadAttachedDocument(id: number): void {
    this.documentLoading = true;
    this.documentError = '';
    this.fleet
      .getFileUploadById(id)
      .pipe(
        finalize(() => (this.documentLoading = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (dto) => {
          if (!dto) {
            this.documentError = 'Could not load the attached document.';
            return;
          }
          this.documentDto = dto;
          this.documentFileName = String(dto['originalFileName'] ?? dto['storedFileName'] ?? 'Document').trim();
          this.applyPreview(dto);
          if (!this.hasDocumentPreview && this.documentFileName) {
            this.documentError = 'Preview unavailable for this file type. Try full view or open in a new tab.';
          }
        },
        error: () => {
          this.documentError = 'Could not load the attached document.';
        },
      });
  }

  private applyPreview(dto: Record<string, unknown>): void {
    this.previewImageUrl = null;
    this.previewPdfUrl = null;
    const hit = resolveFilePreview(dto, { maxBase64Chars: 4_000_000 });
    if (!hit) {
      return;
    }
    if (hit.kind === 'image') {
      this.previewImageUrl = hit.dataUrl;
      return;
    }
    if (hit.kind === 'pdf') {
      this.previewPdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(hit.dataUrl);
    }
  }

  private assetDocumentComplianceType(
    type: FleetComplianceType,
  ): FleetRegistrationDocumentPayload['complianceType'] {
    const map: Partial<Record<FleetComplianceType, FleetRegistrationDocumentPayload['complianceType']>> = {
      vehicle_registration: 'VEHICLE_REGISTRATION',
      road_license: 'ROAD_LICENSE',
      roadworthiness: 'ROADWORTHINESS',
      insurance: 'INSURANCE',
      goods_operator_licence: 'GOODS_OPERATOR_LICENCE',
      permit: 'PERMIT',
      hazardous_substances_permit: 'HAZARDOUS_SUBSTANCES_PERMIT',
      fire_safety_clearance: 'FIRE_SAFETY_CLEARANCE',
      lease_hire_agreement: 'LEASE_HIRE_AGREEMENT',
      license: 'LICENSE',
      defensive_driving_certificate: 'DEFENSIVE_DRIVING_CERTIFICATE',
      driver_medical_certificate: 'DRIVER_MEDICAL_CERTIFICATE',
    };
    return map[type] ?? 'INSURANCE';
  }

  private driverDocumentFileType(
    type: FleetComplianceType,
  ): 'NATIONAL_ID' | 'PASSPORT' | 'DRIVER_LICENCE' {
    if (type === 'license') {
      return 'DRIVER_LICENCE';
    }
    return 'DRIVER_LICENCE';
  }
}
