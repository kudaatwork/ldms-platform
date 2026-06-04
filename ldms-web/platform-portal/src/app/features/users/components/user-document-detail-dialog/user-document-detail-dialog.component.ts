import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { normalizeBase64, resolveFilePreview } from '../../../../shared/utils/file-upload-preview';
import { FileUploadPortalService } from '../../../../core/services/file-upload-portal.service';

export interface UserDocumentDetailDialogData {
  id: number;
}

@Component({
  selector: 'app-user-document-detail-dialog',
  templateUrl: './user-document-detail-dialog.component.html',
  styleUrl: './user-document-detail-dialog.component.scss',
  standalone: false,
})
export class UserDocumentDetailDialogComponent implements OnInit {
  loading = true;
  error = '';
  doc: Record<string, unknown> | null = null;
  /** Data URL for image preview when `fileContent` is base64. */
  previewImageUrl: string | null = null;
  /** Sanitized data URL for PDF preview in an iframe. */
  previewPdfUrl: SafeResourceUrl | null = null;

  constructor(
    private readonly fileUpload: FileUploadPortalService,
    private readonly sanitizer: DomSanitizer,
    @Inject(MAT_DIALOG_DATA) public readonly data: UserDocumentDetailDialogData,
  ) {}

  ngOnInit(): void {
    const id = Number(this.data?.id ?? 0);
    if (!Number.isFinite(id) || id <= 0) {
      this.loading = false;
      this.error = 'Invalid document id.';
      return;
    }
    this.fileUpload.getById(id).subscribe({
      next: (d) => {
        this.loading = false;
        if (!d) {
          this.error =
            'Could not load this document. Confirm the API gateway routes to the file-upload service and that the upload id exists.';
          return;
        }
        this.doc = d;
        this.applyPreview(d);
      },
      error: () => {
        this.error = 'Could not load document details.';
        this.loading = false;
      },
    });
  }

  /** True when inline PDF bytes are present so we can open a Blob URL (works when iframe blocks data URLs). */
  get canOpenPdfInNewTab(): boolean {
    const d = this.doc;
    if (!d) {
      return false;
    }
    const b64 = normalizeBase64(String(d['fileContent'] ?? ''));
    if (!b64) {
      return false;
    }
    const ct = String(d['contentType'] ?? '').toLowerCase();
    const name = String(d['originalFileName'] ?? '').toLowerCase();
    const preview = resolveFilePreview(d);
    return preview?.kind === 'pdf' || ct.includes('pdf') || name.endsWith('.pdf');
  }

  openPdfInNewTab(): void {
    const d = this.doc;
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
      this.error = 'Could not decode PDF content for viewing.';
    }
  }

  str(key: string): string {
    const v = this.doc?.[key];
    if (v == null || v === '') {
      return '';
    }
    if (typeof v === 'object' && !Array.isArray(v) && v !== null && 'name' in (v as object)) {
      return String((v as { name?: unknown }).name ?? '').trim();
    }
    return String(v).trim();
  }

  private applyPreview(d: Record<string, unknown> | null): void {
    this.previewImageUrl = null;
    this.previewPdfUrl = null;
    if (!d) {
      return;
    }
    const hit = resolveFilePreview(d);
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

  formatBytes(n: unknown): string {
    const v = Number(n);
    if (!Number.isFinite(v) || v < 0) {
      return '—';
    }
    if (v < 1024) {
      return `${Math.round(v)} B`;
    }
    const kb = v / 1024;
    if (kb < 1024) {
      return `${kb.toFixed(1)} KB`;
    }
    const mb = kb / 1024;
    return `${mb.toFixed(2)} MB`;
  }
}
