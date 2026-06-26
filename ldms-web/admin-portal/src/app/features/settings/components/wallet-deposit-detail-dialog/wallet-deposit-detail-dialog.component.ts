import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DomSanitizer, SafeHtml, SafeResourceUrl } from '@angular/platform-browser';
import type { WalletDepositRow } from '../../services/platform-wallet-admin.service';
import { PlatformWalletAdminService } from '../../services/platform-wallet-admin.service';
import { FileUploadAdminService } from '../../../../core/services/file-upload-admin.service';
import { normalizeBase64, resolveFilePreview } from '../../../../shared/utils/file-upload-preview';

export interface WalletDepositDetailDialogData {
  deposit: WalletDepositRow;
}

export type WalletDepositDetailResult =
  | { action: 'confirm' }
  | { action: 'reject'; reason: string };

@Component({
  selector: 'app-wallet-deposit-detail-dialog',
  templateUrl: './wallet-deposit-detail-dialog.component.html',
  styleUrl: './wallet-deposit-detail-dialog.component.scss',
  standalone: false,
})
export class WalletDepositDetailDialogComponent implements OnInit {
  proofLoading = false;
  proofError = '';
  proofDoc: Record<string, unknown> | null = null;
  previewImageUrl: string | null = null;
  previewPdfUrl: SafeResourceUrl | null = null;

  rejecting = false;
  rejectReason = '';
  rejectError = '';
  readonly minReasonLength = 10;

  receiptLoading = false;
  receiptError = '';
  receiptHtml: SafeHtml | null = null;
  receiptDownloading = false;

  constructor(
    private readonly walletAdmin: PlatformWalletAdminService,
    private readonly fileUpload: FileUploadAdminService,
    private readonly sanitizer: DomSanitizer,
    private readonly dialogRef: MatDialogRef<WalletDepositDetailDialogComponent, WalletDepositDetailResult | undefined>,
    @Inject(MAT_DIALOG_DATA) readonly data: WalletDepositDetailDialogData,
  ) {}

  ngOnInit(): void {
    this.loadReceipt();
    const proofId = Number(this.deposit.proofDocumentId ?? 0);
    if (!Number.isFinite(proofId) || proofId <= 0) {
      return;
    }
    this.proofLoading = true;
    this.fileUpload.getById(proofId).subscribe({
      next: (doc) => {
        this.proofLoading = false;
        if (!doc) {
          this.proofError = 'Could not load the proof of payment. Confirm the file-upload service is running.';
          return;
        }
        this.proofDoc = doc;
        this.applyPreview(doc);
      },
      error: () => {
        this.proofLoading = false;
        this.proofError = 'Could not load the proof of payment document.';
      },
    });
  }

  get deposit(): WalletDepositRow {
    return this.data.deposit;
  }

  /** Approve / reject controls only when the deposit is still pending. */
  get actionable(): boolean {
    return (this.deposit.status ?? '').toUpperCase() === 'PENDING';
  }

  get hasProof(): boolean {
    return Number(this.deposit.proofDocumentId ?? 0) > 0;
  }

  get canOpenPdfInNewTab(): boolean {
    const d = this.proofDoc;
    if (!d) {
      return false;
    }
    const b64 = normalizeBase64(String(d['fileContent'] ?? ''));
    if (!b64) {
      return false;
    }
    const preview = resolveFilePreview(d);
    const ct = String(d['contentType'] ?? '').toLowerCase();
    const name = String(d['originalFileName'] ?? '').toLowerCase();
    return preview?.kind === 'pdf' || ct.includes('pdf') || name.endsWith('.pdf');
  }

  openPdfInNewTab(): void {
    const d = this.proofDoc;
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
      this.proofError = 'Could not decode the PDF content for viewing.';
    }
  }

  approve(): void {
    this.dialogRef.close({ action: 'confirm' });
  }

  startReject(): void {
    this.rejecting = true;
    this.rejectError = '';
  }

  cancelReject(): void {
    this.rejecting = false;
    this.rejectReason = '';
    this.rejectError = '';
  }

  submitReject(): void {
    const reason = this.rejectReason.trim();
    if (reason.length < this.minReasonLength) {
      this.rejectError = `Enter at least ${this.minReasonLength} characters explaining why this deposit is being rejected.`;
      return;
    }
    this.dialogRef.close({ action: 'reject', reason });
  }

  /** A receipt only exists once the deposit has been confirmed/credited. */
  get hasReceipt(): boolean {
    return (this.deposit.status ?? '').toUpperCase() === 'CONFIRMED';
  }

  get emailStatus(): string {
    return (this.deposit.receiptEmailStatus ?? '').toUpperCase();
  }

  get emailStatusLabel(): string {
    switch (this.emailStatus) {
      case 'SENT':
        return 'Sent';
      case 'NO_EMAIL':
        return 'Not sent — no email on file';
      case 'FAILED':
        return 'Failed to send';
      default:
        return 'Not sent';
    }
  }

  /** CSS modifier reusing the existing lx-status palette (success/neutral/danger). */
  get emailStatusTone(): string {
    switch (this.emailStatus) {
      case 'SENT':
        return 'confirmed';
      case 'FAILED':
        return 'rejected';
      default:
        return 'pending';
    }
  }

  private loadReceipt(): void {
    if (!this.hasReceipt) {
      return;
    }
    this.receiptLoading = true;
    this.receiptError = '';
    this.walletAdmin.getDepositReceiptHtml(this.deposit.id).subscribe({
      next: (html) => {
        this.receiptLoading = false;
        this.receiptHtml = html ? this.sanitizer.bypassSecurityTrustHtml(html) : null;
        if (!html) {
          this.receiptError = 'No receipt was returned for this deposit.';
        }
      },
      error: (err: Error) => {
        this.receiptLoading = false;
        this.receiptError = err.message || 'Could not load the receipt.';
      },
    });
  }

  downloadReceiptPdf(): void {
    if (this.receiptDownloading) {
      return;
    }
    this.receiptDownloading = true;
    this.walletAdmin.downloadDepositReceiptPdf(this.deposit.id).subscribe({
      next: (blob) => {
        this.receiptDownloading = false;
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `wallet-receipt-deposit-${this.deposit.id}.pdf`;
        link.click();
        setTimeout(() => URL.revokeObjectURL(url), 120_000);
      },
      error: (err: Error) => {
        this.receiptDownloading = false;
        this.receiptError = err.message || 'Could not download the receipt PDF.';
      },
    });
  }

  formatMoney(cents?: number, currency?: string): string {
    return this.walletAdmin.formatCents(cents ?? 0, currency ?? 'USD');
  }

  formatWhen(iso?: string): string {
    return this.walletAdmin.formatWhen(iso);
  }

  close(): void {
    this.dialogRef.close();
  }

  private applyPreview(d: Record<string, unknown>): void {
    this.previewImageUrl = null;
    this.previewPdfUrl = null;
    const hit = resolveFilePreview(d);
    if (!hit) {
      return;
    }
    if (hit.kind === 'image') {
      this.previewImageUrl = hit.dataUrl;
    } else if (hit.kind === 'pdf') {
      this.previewPdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(hit.dataUrl);
    }
  }
}
