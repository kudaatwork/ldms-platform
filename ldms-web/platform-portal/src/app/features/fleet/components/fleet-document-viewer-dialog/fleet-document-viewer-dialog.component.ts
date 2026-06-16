import { CommonModule } from '@angular/common';
import { Component, Inject, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { normalizeBase64, resolveFilePreview } from '../../../../shared/utils/file-upload-preview';
import { FleetPortalService } from '../../services/fleet-portal.service';

export interface FleetDocumentViewerDialogData {
  fileUploadId: number;
  title?: string;
}

@Component({
  selector: 'app-fleet-document-viewer-dialog',
  templateUrl: './fleet-document-viewer-dialog.component.html',
  styleUrl: './fleet-document-viewer-dialog.component.scss',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatDialogModule, MatIconModule, MatProgressSpinnerModule],
})
export class FleetDocumentViewerDialogComponent implements OnInit {
  loading = true;
  error = '';
  fileName = '';
  previewImageUrl: string | null = null;
  previewPdfUrl: SafeResourceUrl | null = null;
  private doc: Record<string, unknown> | null = null;

  constructor(
    private readonly fleet: FleetPortalService,
    private readonly sanitizer: DomSanitizer,
    private readonly dialogRef: MatDialogRef<FleetDocumentViewerDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public readonly data: FleetDocumentViewerDialogData,
  ) {}

  ngOnInit(): void {
    const id = Number(this.data?.fileUploadId ?? 0);
    if (!Number.isFinite(id) || id < 1) {
      this.loading = false;
      this.error = 'Invalid document reference.';
      return;
    }
    this.fleet.getFileUploadById(id).subscribe({
      next: (dto) => {
        this.loading = false;
        if (!dto) {
          this.error = 'Could not load this document.';
          return;
        }
        this.doc = dto;
        this.fileName = String(dto['originalFileName'] ?? dto['storedFileName'] ?? 'Document').trim();
        this.applyPreview(dto);
        if (!this.previewImageUrl && !this.previewPdfUrl) {
          this.error = 'Preview unavailable for this file type.';
        }
      },
      error: () => {
        this.loading = false;
        this.error = 'Could not load this document.';
      },
    });
  }

  get hasPreview(): boolean {
    return !!this.previewImageUrl || !!this.previewPdfUrl;
  }

  get canOpenPdfInNewTab(): boolean {
    const d = this.doc;
    if (!d) {
      return false;
    }
    const b64 = normalizeBase64(String(d['fileContent'] ?? ''));
    if (!b64) {
      return false;
    }
    const preview = resolveFilePreview(d);
    return preview?.kind === 'pdf';
  }

  close(): void {
    this.dialogRef.close();
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
      this.error = 'Could not open PDF in a new tab.';
    }
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
