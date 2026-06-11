import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Observable, Subject, finalize, of, switchMap, takeUntil } from 'rxjs';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { NotificationService } from '../../../../core/services/notification.service';
import type { PurchaseRequisitionRow } from '../../models/inventory.model';
import { InventoryPortalService, type RequisitionQuoteLineDetail } from '../../services/inventory-portal.service';

export type QuoteCaptureMode = 'SYSTEM_GENERATED' | 'EXTERNAL_UPLOAD';

export type SubmitSupplierQuoteDialogData = {
  requisition: PurchaseRequisitionRow;
  submittedByUserId: number;
};

@Component({
  selector: 'app-submit-supplier-quote-dialog',
  templateUrl: './submit-supplier-quote-dialog.component.html',
  styleUrl: './submit-supplier-quote-dialog.component.scss',
  standalone: false,
})
export class SubmitSupplierQuoteDialogComponent implements OnInit, OnDestroy {
  readonly captureModes: { id: QuoteCaptureMode; title: string; description: string }[] = [
    {
      id: 'SYSTEM_GENERATED',
      title: 'Build in LX',
      description: 'Enter commercial terms and line pricing in the platform. LX generates the quote record.',
    },
    {
      id: 'EXTERNAL_UPLOAD',
      title: 'Upload existing quote',
      description: 'Attach a PDF or image from your ERP or email. The same metadata is still required and stored.',
    },
  ];

  captureMode: QuoteCaptureMode = 'SYSTEM_GENERATED';
  readonly form: FormGroup;
  lines: RequisitionQuoteLineDetail[] = [];
  linePrices: Record<number, string> = {};
  loading = true;
  submitting = false;
  loadError = '';
  uploadLabel = '';
  uploadedDocumentId?: number;
  externalQuoteFile: File | null = null;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly inventory: InventoryPortalService,
    private readonly authState: AuthStateService,
    private readonly notifications: NotificationService,
    private readonly dialogRef: MatDialogRef<SubmitSupplierQuoteDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) readonly data: SubmitSupplierQuoteDialogData,
  ) {
    this.form = this.fb.group({
      currency: ['USD', Validators.required],
      paymentTerm: ['NET_30', Validators.required],
      deliveryTerms: ['', Validators.required],
      validityUntil: ['', Validators.required],
      taxAmount: ['0'],
      notes: [''],
    });
    this.dialogRef.disableClose = true;
  }

  ngOnInit(): void {
    this.inventory
      .getRequisitionQuoteContext(this.data.requisition.id)
      .pipe(
        finalize(() => (this.loading = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (ctx) => {
          this.lines = ctx.lines;
          for (const line of ctx.lines) {
            this.linePrices[line.id] = String(line.estimatedUnitPrice ?? '');
          }
          if (ctx.currency) {
            this.form.patchValue({ currency: ctx.currency });
          }
        },
        error: (err: Error) => {
          this.loadError = err.message ?? 'Could not load requisition lines.';
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setCaptureMode(mode: QuoteCaptureMode): void {
    this.captureMode = mode;
  }

  onQuoteFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    this.externalQuoteFile = file;
    this.uploadLabel = file?.name ?? '';
    this.uploadedDocumentId = undefined;
  }

  cancel(): void {
    if (!this.submitting) {
      this.dialogRef.close(false);
    }
  }

  submit(): void {
    if (this.form.invalid || this.submitting || !this.lines.length) {
      this.form.markAllAsTouched();
      return;
    }
    const supplierOrganizationId = Number(this.authState.currentUser?.organizationId ?? 0);
    if (!supplierOrganizationId) {
      this.notifications.error('Supplier organisation context is missing.');
      return;
    }
    const quoteLines = this.lines
      .map((line) => {
        const unitPrice = Number(String(this.linePrices[line.id] ?? '').trim());
        return {
          purchaseRequisitionLineId: line.id,
          productId: line.productId,
          quotedQuantity: line.requestedQuantity,
          unitPrice,
        };
      })
      .filter((line) => Number.isFinite(line.unitPrice) && line.unitPrice > 0);
    if (!quoteLines.length) {
      this.notifications.error('Enter a unit price for at least one line.');
      return;
    }
    if (this.captureMode === 'EXTERNAL_UPLOAD' && !this.externalQuoteFile) {
      this.notifications.error('Choose the quote document to upload.');
      return;
    }

    const v = this.form.getRawValue();
    const basePayload = {
      purchaseRequisitionId: this.data.requisition.id,
      supplierOrganizationId,
      submittedByUserId: this.data.submittedByUserId,
      quoteSource: this.captureMode,
      currency: String(v.currency).trim(),
      paymentTerm: String(v.paymentTerm).trim(),
      deliveryTerms: String(v.deliveryTerms).trim(),
      validityUntil: String(v.validityUntil).trim(),
      taxAmount: Number(String(v.taxAmount ?? '0').trim()) || 0,
      notes: String(v.notes ?? '').trim() || undefined,
      lines: quoteLines,
    };

    this.submitting = true;
    const upload$: Observable<number | undefined> =
      this.captureMode === 'EXTERNAL_UPLOAD' && this.externalQuoteFile
        ? this.inventory.uploadOrganizationDocument(supplierOrganizationId, this.externalQuoteFile, 'OTHER')
        : of<number | undefined>(undefined);

    upload$
      .pipe(
        switchMap((documentId: number | undefined) =>
          this.inventory.submitQuote({
            ...basePayload,
            externalDocumentId: documentId,
          }),
        ),
        finalize(() => (this.submitting = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: () => {
          this.notifications.success(`Quote submitted for ${this.data.requisition.requisitionNumber}.`);
          this.dialogRef.close(true);
        },
        error: (err: Error) => this.notifications.error(err.message ?? 'Could not submit quote.'),
      });
  }
}
