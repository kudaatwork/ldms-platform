import { CommonModule } from '@angular/common';
import { Component, Inject, OnDestroy, OnInit, Optional } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Subject, finalize, from, of } from 'rxjs';
import { catchError, concatMap, map, takeUntil } from 'rxjs/operators';
import { normalizeBase64, resolveFilePreview } from '../../../../shared/utils/file-upload-preview';
import type { FleetComplianceRow } from '../../models/fleet.model';
import type { FleetComplianceSubjectBundle } from '../../utils/fleet-compliance-bundle.util';
import { FleetPortalService } from '../../services/fleet-portal.service';

export type FleetComplianceBundleReviewDialogData = {
  bundle: FleetComplianceSubjectBundle;
};

type DocDecision = 'approved' | 'rejected' | null;

@Component({
  selector: 'app-fleet-compliance-bundle-review-dialog',
  templateUrl: './fleet-compliance-bundle-review-dialog.component.html',
  styleUrl: './fleet-compliance-bundle-review-dialog.component.scss',
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
export class FleetComplianceBundleReviewDialogComponent implements OnInit, OnDestroy {
  readonly bundle: FleetComplianceSubjectBundle;
  readonly pendingRecords: FleetComplianceRow[];
  readonly subjectIcon: string;

  selectedId: number | null = null;
  documentLoading = false;
  documentError = '';
  documentFileName = '';
  previewImageUrl: string | null = null;
  previewPdfUrl: SafeResourceUrl | null = null;
  private documentDto: Record<string, unknown> | null = null;

  /** Document ids the reviewer has opened and loaded in the preview pane. */
  readonly viewedDocIds = new Set<number>();
  readonly decisions = new Map<number, DocDecision>();
  readonly rejectReasons = new Map<number, string>();

  individualForm: FormGroup;
  bulkRejectForm: FormGroup;
  showBulkReject = false;
  bulkSubmitting = false;
  individualSubmitting = false;
  actionError = '';
  changed = false;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<
      FleetComplianceBundleReviewDialogComponent,
      { changed: boolean } | undefined
    >,
    private readonly fleet: FleetPortalService,
    private readonly sanitizer: DomSanitizer,
    @Optional() @Inject(MAT_DIALOG_DATA) data: FleetComplianceBundleReviewDialogData | null,
  ) {
    this.dialogRef.disableClose = true;
    this.bundle = data?.bundle ?? {
      subjectType: 'asset',
      subjectId: 0,
      subjectLabel: 'Unknown subject',
      records: [],
      pendingRecords: [],
      pendingCount: 0,
    };
    this.pendingRecords = [...this.bundle.pendingRecords];
    this.subjectIcon = this.bundle.subjectType === 'driver' ? 'badge' : 'local_shipping';

    this.individualForm = this.fb.group({
      rejectReason: ['', [Validators.maxLength(500)]],
    });
    this.bulkRejectForm = this.fb.group({
      rejectReason: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(500)]],
    });
  }

  ngOnInit(): void {
    if (this.pendingRecords.length) {
      this.selectDocument(this.pendingRecords[0]);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get selectedRecord(): FleetComplianceRow | null {
    if (this.selectedId == null) {
      return null;
    }
    return this.pendingRecords.find((r) => r.id === this.selectedId) ?? null;
  }

  get hasDocumentPreview(): boolean {
    return !!this.previewImageUrl || !!this.previewPdfUrl;
  }

  get canOpenPdfInNewTab(): boolean {
    const d = this.documentDto;
    if (!d) {
      return false;
    }
    const b64 = normalizeBase64(String(d['fileContent'] ?? ''));
    if (!b64) {
      return false;
    }
    return resolveFilePreview(d)?.kind === 'pdf';
  }

  get allDocumentsViewed(): boolean {
    return this.pendingRecords.every((r) => this.viewedDocIds.has(r.id));
  }

  get selectedIsViewed(): boolean {
    return this.selectedId != null && this.viewedDocIds.has(this.selectedId);
  }

  get remainingCount(): number {
    return this.pendingRecords.length;
  }

  get viewedPendingCount(): number {
    return this.pendingRecords.filter((r) => this.viewedDocIds.has(r.id)).length;
  }

  get completedCount(): number {
    return this.bundle.pendingCount - this.pendingRecords.length;
  }

  close(): void {
    if (this.bulkSubmitting || this.individualSubmitting) {
      return;
    }
    this.dialogRef.close(this.changed ? { changed: true } : undefined);
  }

  selectDocument(record: FleetComplianceRow): void {
    if (this.selectedId === record.id && !this.documentLoading) {
      return;
    }
    this.selectedId = record.id;
    this.actionError = '';
    this.showBulkReject = false;
    const savedReason = this.rejectReasons.get(record.id);
    this.individualForm.patchValue({ rejectReason: savedReason ?? '' }, { emitEvent: false });

    if (!record.fileUploadId) {
      this.clearPreview();
      this.documentError = 'No document file is attached to this record.';
      this.viewedDocIds.add(record.id);
      return;
    }

    this.loadDocument(record.fileUploadId);
  }

  docStatusClass(record: FleetComplianceRow): string {
    const decision = this.decisions.get(record.id);
    if (decision === 'approved') {
      return 'fcbr-doc--approved';
    }
    if (decision === 'rejected') {
      return 'fcbr-doc--rejected';
    }
    if (this.viewedDocIds.has(record.id)) {
      return 'fcbr-doc--viewed';
    }
    return '';
  }

  docStatusIcon(record: FleetComplianceRow): string {
    const decision = this.decisions.get(record.id);
    if (decision === 'approved') {
      return 'check_circle';
    }
    if (decision === 'rejected') {
      return 'cancel';
    }
    if (this.viewedDocIds.has(record.id)) {
      return 'visibility';
    }
    return 'description';
  }

  approveSelected(): void {
    const record = this.selectedRecord;
    if (!record || !this.selectedIsViewed || this.individualSubmitting) {
      return;
    }
    this.runIndividualDecision(record, 'approve');
  }

  rejectSelected(): void {
    const record = this.selectedRecord;
    if (!record || !this.selectedIsViewed || this.individualSubmitting) {
      return;
    }
    const reason = String(this.individualForm.get('rejectReason')?.value ?? '').trim();
    if (reason.length < 3) {
      this.individualForm.get('rejectReason')?.setErrors({ required: true });
      this.individualForm.get('rejectReason')?.markAsTouched();
      this.actionError = 'Enter a rejection reason (at least 3 characters).';
      return;
    }
    this.rejectReasons.set(record.id, reason);
    this.runIndividualDecision(record, 'reject', reason);
  }

  approveAll(): void {
    if (!this.allDocumentsViewed || this.bulkSubmitting || !this.pendingRecords.length) {
      return;
    }
    this.bulkSubmitting = true;
    this.actionError = '';
    this.showBulkReject = false;
    this.processBulk(this.pendingRecords.map((r) => ({ record: r, mode: 'approve' as const })));
  }

  openBulkReject(): void {
    if (!this.allDocumentsViewed || this.bulkSubmitting) {
      return;
    }
    this.showBulkReject = true;
    this.actionError = '';
  }

  cancelBulkReject(): void {
    this.showBulkReject = false;
    this.bulkRejectForm.reset({ rejectReason: '' });
  }

  rejectAll(): void {
    if (!this.allDocumentsViewed || this.bulkSubmitting || this.bulkRejectForm.invalid) {
      this.bulkRejectForm.markAllAsTouched();
      return;
    }
    const reason = String(this.bulkRejectForm.get('rejectReason')?.value ?? '').trim();
    this.bulkSubmitting = true;
    this.actionError = '';
    this.processBulk(this.pendingRecords.map((r) => ({ record: r, mode: 'reject' as const, reason })));
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
      this.actionError = 'Could not open PDF in a new tab.';
    }
  }

  private runIndividualDecision(record: FleetComplianceRow, mode: 'approve' | 'reject', reason?: string): void {
    this.individualSubmitting = true;
    this.actionError = '';
    const payload =
      mode === 'reject'
        ? {
            status: 'REVOKED',
            expiresAt: record.expiresAt,
            notes: reason,
          }
        : {
            expiresAt: record.expiresAt,
            notes: record.notes || undefined,
          };

    this.fleet
      .updateCompliance(record.id, payload)
      .pipe(
        finalize(() => (this.individualSubmitting = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: () => {
          this.changed = true;
          this.decisions.set(record.id, mode === 'approve' ? 'approved' : 'rejected');
          this.removePendingRecord(record.id);
        },
        error: (err: Error) => {
          this.actionError = err.message ?? 'Could not update compliance record.';
        },
      });
  }

  private processBulk(
    items: { record: FleetComplianceRow; mode: 'approve' | 'reject'; reason?: string }[],
  ): void {
    let failed = 0;
    from(items)
      .pipe(
        concatMap((item) => {
          const payload =
            item.mode === 'reject'
              ? {
                  status: 'REVOKED',
                  expiresAt: item.record.expiresAt,
                  notes: item.reason,
                }
              : {
                  expiresAt: item.record.expiresAt,
                  notes: item.record.notes || undefined,
                };
          return this.fleet.updateCompliance(item.record.id, payload).pipe(
            map(() => {
              this.decisions.set(item.record.id, item.mode === 'approve' ? 'approved' : 'rejected');
              this.removePendingRecord(item.record.id);
              return true;
            }),
            catchError(() => {
              failed += 1;
              return of(false);
            }),
          );
        }),
        finalize(() => {
          this.bulkSubmitting = false;
          this.changed = true;
          if (failed > 0) {
            this.actionError = `${failed} document(s) could not be updated. Review remaining items and try again.`;
          }
          if (!this.pendingRecords.length) {
            this.dialogRef.close({ changed: true });
          }
        }),
        takeUntil(this.destroy$),
      )
      .subscribe();
  }

  private removePendingRecord(id: number): void {
    const idx = this.pendingRecords.findIndex((r) => r.id === id);
    if (idx >= 0) {
      this.pendingRecords.splice(idx, 1);
    }
    if (!this.pendingRecords.length) {
      this.dialogRef.close({ changed: true });
      return;
    }
    if (this.selectedId === id) {
      this.selectDocument(this.pendingRecords[0]);
    }
  }

  private loadDocument(fileUploadId: number): void {
    this.documentLoading = true;
    this.documentError = '';
    this.clearPreview();
    this.fleet
      .getFileUploadById(fileUploadId)
      .pipe(
        finalize(() => (this.documentLoading = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (dto) => {
          if (!dto) {
            this.documentError = 'Could not load this document.';
            return;
          }
          this.documentDto = dto;
          this.documentFileName = String(dto['originalFileName'] ?? dto['storedFileName'] ?? 'Document').trim();
          this.applyPreview(dto);
          if (this.selectedId != null) {
            this.viewedDocIds.add(this.selectedId);
          }
          if (!this.hasDocumentPreview && this.documentFileName) {
            this.documentError = 'Preview unavailable for this file type. Open in a new tab if available.';
          }
        },
        error: () => {
          this.documentError = 'Could not load this document.';
        },
      });
  }

  private clearPreview(): void {
    this.documentDto = null;
    this.documentFileName = '';
    this.previewImageUrl = null;
    this.previewPdfUrl = null;
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
}
