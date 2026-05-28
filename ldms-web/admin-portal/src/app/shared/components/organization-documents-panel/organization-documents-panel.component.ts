import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { FileUploadAdminService } from '../../../core/services/file-upload-admin.service';
import type { KycApplicationDocument } from '../../../features/organizations/models/organization.model';
import { normalizeBase64, resolveFilePreview } from '../../utils/file-upload-preview';

@Component({
  selector: 'app-organization-documents-panel',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './organization-documents-panel.component.html',
  styleUrl: './organization-documents-panel.component.scss',
})
export class OrganizationDocumentsPanelComponent implements OnChanges, OnDestroy {
  @Input() documents: KycApplicationDocument[] = [];
  @Input() compact = false;

  selected: KycApplicationDocument | null = null;
  loading = false;
  loadError = '';
  previewImageUrl: string | null = null;
  previewPdfUrl: SafeResourceUrl | null = null;
  meta: Record<string, unknown> | null = null;

  private loadSeq = 0;
  private previewBlobUrl: string | null = null;

  constructor(
    private readonly fileUpload: FileUploadAdminService,
    private readonly sanitizer: DomSanitizer,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['documents']) {
      return;
    }
    if (!this.documents.length) {
      this.selected = null;
      this.resetPreviewState();
      this.cdr.markForCheck();
      return;
    }
    const stillListed =
      this.selected != null && this.documents.some((d) => d.uploadId === this.selected?.uploadId);
    if (!stillListed) {
      this.select(this.documents[0]);
    }
  }

  ngOnDestroy(): void {
    this.revokePreviewBlob();
  }

  select(doc: KycApplicationDocument): void {
    this.selected = doc;
    const seq = ++this.loadSeq;
    this.loading = true;
    this.loadError = '';
    this.previewImageUrl = null;
    this.previewPdfUrl = null;
    this.revokePreviewBlob();

    this.fileUpload.getById(doc.uploadId).subscribe({
      next: (row) => {
        if (seq !== this.loadSeq) {
          return;
        }
        this.loading = false;
        if (!row) {
          this.meta = null;
          this.loadError =
            'Could not load this file. Confirm the file-upload service is running and the upload id exists.';
          this.cdr.markForCheck();
          return;
        }
        this.meta = row;
        this.applyPreview(row, doc);
        if (!this.previewImageUrl && !this.previewPdfUrl) {
          const hasContent = !!normalizeBase64(String(row['fileContent'] ?? ''));
          this.loadError = hasContent
            ? 'Preview could not be rendered for this file type. Try Open PDF.'
            : 'File metadata loaded but content is unavailable from storage. Try Open PDF or check file-upload / RustFS.';
        } else {
          this.loadError = '';
        }
        this.cdr.markForCheck();
      },
      error: () => {
        if (seq !== this.loadSeq) {
          return;
        }
        this.loading = false;
        this.meta = null;
        this.loadError = 'Could not load document preview.';
        this.cdr.markForCheck();
      },
    });
  }

  isPdf(): boolean {
    const d = this.meta;
    if (!d) {
      return this.selected?.fileType === 'PDF';
    }
    const ct = String(d['contentType'] ?? '').toLowerCase();
    const name = String(d['originalFileName'] ?? this.selected?.fileName ?? '').toLowerCase();
    const ft = String(d['fileType'] ?? '').toUpperCase();
    return (
      ct.includes('pdf') ||
      name.endsWith('.pdf') ||
      this.selected?.fileType === 'PDF' ||
      ft.includes('CERTIFICATE') ||
      ft.includes('CLEARANCE')
    );
  }

  openPdfInNewTab(): void {
    const d = this.meta;
    if (!d) {
      return;
    }
    const b64 = normalizeBase64(String(d['fileContent'] ?? ''));
    if (!b64) {
      this.loadError = 'PDF bytes are not available to open.';
      this.cdr.markForCheck();
      return;
    }
    try {
      const url = this.createBlobUrl(b64, 'application/pdf');
      window.open(url, '_blank', 'noopener,noreferrer');
      setTimeout(() => URL.revokeObjectURL(url), 120_000);
    } catch {
      this.loadError = 'Could not open PDF in a new tab.';
      this.cdr.markForCheck();
    }
  }

  docIcon(doc: KycApplicationDocument): string {
    return doc.fileType === 'PDF' ? 'picture_as_pdf' : 'image';
  }

  private applyPreview(row: Record<string, unknown>, doc: KycApplicationDocument): void {
    const enriched: Record<string, unknown> = {
      ...row,
      originalFileName: row['originalFileName'] ?? doc.fileName,
    };
    if (doc.fileType === 'PDF' && !String(enriched['contentType'] ?? '').trim()) {
      enriched['contentType'] = 'application/pdf';
    }

    const hit = resolveFilePreview(enriched, { maxBase64Chars: 4_000_000 });
    if (hit?.kind === 'image') {
      this.previewImageUrl = hit.dataUrl;
      return;
    }

    const b64 = normalizeBase64(String(enriched['fileContent'] ?? ''));
    if (!b64) {
      return;
    }

    const ct = String(enriched['contentType'] ?? '').toLowerCase();
    const name = String(enriched['originalFileName'] ?? '').toLowerCase();
    const pdfLike =
      hit?.kind === 'pdf' ||
      ct.includes('pdf') ||
      name.endsWith('.pdf') ||
      doc.fileType === 'PDF';

    if (pdfLike) {
      try {
        const blobUrl = this.createBlobUrl(b64, 'application/pdf');
        this.previewBlobUrl = blobUrl;
        this.previewPdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(blobUrl);
        return;
      } catch {
        if (hit?.kind === 'pdf') {
          this.previewPdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(hit.dataUrl);
        }
      }
    }
  }

  private createBlobUrl(b64: string, mime: string): string {
    const binary = atob(b64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i);
    }
    return URL.createObjectURL(new Blob([bytes], { type: mime }));
  }

  private revokePreviewBlob(): void {
    if (this.previewBlobUrl) {
      URL.revokeObjectURL(this.previewBlobUrl);
      this.previewBlobUrl = null;
    }
  }

  private resetPreviewState(): void {
    this.loading = false;
    this.loadError = '';
    this.previewImageUrl = null;
    this.previewPdfUrl = null;
    this.meta = null;
    this.revokePreviewBlob();
  }
}
