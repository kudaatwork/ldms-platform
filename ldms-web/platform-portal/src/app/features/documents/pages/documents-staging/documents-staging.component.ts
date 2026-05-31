import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Subject, takeUntil } from 'rxjs';
import {
  DocumentFilterId,
  DocumentSortId,
  DocumentSourceChannel,
  DocumentsService,
  StagedDocument,
} from '../../services/documents.service';
import { AuthStateService } from '../../../../core/services/auth-state.service';

type ViewMode = 'grid' | 'list' | 'focus';
type DetailTab = 'preview' | 'metadata' | 'verification';

@Component({
  selector: 'app-documents-staging',
  templateUrl: './documents-staging.component.html',
  styleUrls: ['./documents-staging.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class DocumentsStagingComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly docsApi = inject(DocumentsService);
  private readonly authState = inject(AuthStateService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly loading = signal(true);
  readonly loadError = signal('');
  readonly search = signal('');
  readonly filter = signal<DocumentFilterId>('ALL');
  readonly sortBy = signal<DocumentSortId>('newest');
  readonly viewMode = signal<ViewMode>('grid');
  readonly detailTab = signal<DetailTab>('preview');
  readonly selectedId = signal<number | null>(null);
  readonly detailLoading = signal(false);
  readonly inspectorOpen = signal(false);

  documents: StagedDocument[] = [];
  selected: StagedDocument | null = null;
  selectedPdfUrl: SafeResourceUrl | null = null;

  readonly filters: { id: DocumentFilterId; label: string; icon: string }[] = [
    { id: 'ALL', label: 'All', icon: 'layers' },
    { id: 'KYC', label: 'KYC', icon: 'verified_user' },
    { id: 'COMPLIANCE', label: 'Compliance', icon: 'gavel' },
    { id: 'PROFILE', label: 'Profile', icon: 'person' },
    { id: 'BRANDING', label: 'Branding', icon: 'palette' },
    { id: 'OTHER', label: 'Registry', icon: 'cloud_upload' },
  ];

  readonly viewModes: { id: ViewMode; label: string; icon: string }[] = [
    { id: 'grid', label: 'Gallery', icon: 'grid_view' },
    { id: 'list', label: 'List', icon: 'view_list' },
    { id: 'focus', label: 'Focus', icon: 'splitscreen' },
  ];

  ngOnInit(): void {
    this.reload();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get orgName(): string {
    return this.authState.currentUser?.orgName ?? 'Your organisation';
  }

  get filteredDocuments(): StagedDocument[] {
    const q = this.search().trim().toLowerCase();
    let rows = this.documents;
    const f = this.filter();
    if (f !== 'ALL') {
      rows = rows.filter((d) => this.matchesFilter(d, f));
    }
    if (q) {
      rows = rows.filter(
        (d) =>
          d.displayTitle.toLowerCase().includes(q) ||
          d.fileName.toLowerCase().includes(q) ||
          d.sourceLabel.toLowerCase().includes(q) ||
          d.category.toLowerCase().includes(q) ||
          d.fileType.toLowerCase().includes(q) ||
          String(d.id).includes(q),
      );
    }
    return this.sortDocuments(rows);
  }

  get stats() {
    const all = this.documents;
    return {
      total: all.length,
      org: all.filter((d) => d.sourceScope === 'ORGANIZATION').length,
      user: all.filter((d) => d.sourceScope === 'USER').length,
      verified: all.filter((d) => d.autoVerified).length,
      withPreview: all.filter((d) => d.hasPreview).length,
    };
  }

  filterCount(id: DocumentFilterId): number {
    if (id === 'ALL') {
      return this.documents.length;
    }
    return this.documents.filter((d) => this.matchesFilter(d, id)).length;
  }

  reload(): void {
    this.loading.set(true);
    this.loadError.set('');
    this.docsApi
      .loadStagingDocuments()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (docs) => {
          this.documents = docs;
          this.loading.set(false);
          if (docs.length && !this.selected) {
            this.selectDocument(docs[0], false);
          }
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.loadError.set(err.message);
          this.loading.set(false);
          this.cdr.markForCheck();
        },
      });
  }

  setFilter(id: DocumentFilterId): void {
    this.filter.set(id);
    const rows = this.filteredDocuments;
    if (rows.length && !rows.some((d) => d.id === this.selectedId())) {
      this.selectDocument(rows[0], false);
    }
  }

  setViewMode(mode: ViewMode): void {
    this.viewMode.set(mode);
    if (mode === 'focus' && this.filteredDocuments.length && !this.selected) {
      this.selectDocument(this.filteredDocuments[0], false);
    }
  }

  setDetailTab(tab: DetailTab): void {
    this.detailTab.set(tab);
  }

  selectDocument(doc: StagedDocument, openInspector = true): void {
    this.selectedId.set(doc.id);
    this.selected = doc;
    this.detailTab.set('preview');
    this.selectedPdfUrl = doc.previewPdfDataUrl
      ? this.sanitizer.bypassSecurityTrustResourceUrl(doc.previewPdfDataUrl)
      : null;

    if (openInspector) {
      this.inspectorOpen.set(true);
    }

    if (doc.previewImageUrl || doc.previewPdfDataUrl) {
      this.cdr.markForCheck();
      return;
    }

    this.detailLoading.set(true);
    this.docsApi
      .fetchDocumentDetail(doc.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (detail) => {
          if (detail) {
            const idx = this.documents.findIndex((d) => d.id === detail.id);
            if (idx >= 0) {
              this.documents[idx] = { ...this.documents[idx], ...detail };
            }
            this.selected = detail;
            this.selectedPdfUrl = detail.previewPdfDataUrl
              ? this.sanitizer.bypassSecurityTrustResourceUrl(detail.previewPdfDataUrl)
              : null;
          }
          this.detailLoading.set(false);
          this.cdr.markForCheck();
        },
        error: () => {
          this.detailLoading.set(false);
          this.cdr.markForCheck();
        },
      });
  }

  closeInspector(): void {
    this.inspectorOpen.set(false);
  }

  openPdfInNewTab(): void {
    if (!this.selected?.previewPdfDataUrl) {
      return;
    }
    window.open(this.selected.previewPdfDataUrl, '_blank', 'noopener,noreferrer');
  }

  downloadDocument(doc: StagedDocument | null): void {
    if (!doc) {
      return;
    }
    const url = doc.previewPdfDataUrl || doc.previewImageUrl;
    if (!url) {
      return;
    }
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = doc.fileName || `document-${doc.id}`;
    anchor.click();
  }

  copyUploadId(id: number): void {
    void navigator.clipboard?.writeText(String(id));
  }

  formatBytes(bytes?: number): string {
    if (bytes == null || !Number.isFinite(bytes) || bytes < 1) {
      return '—';
    }
    if (bytes < 1024) {
      return `${bytes} B`;
    }
    if (bytes < 1024 * 1024) {
      return `${(bytes / 1024).toFixed(1)} KB`;
    }
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  fileKindIcon(doc: StagedDocument): string {
    if (doc.hasPdfPreview) {
      return 'picture_as_pdf';
    }
    if (doc.previewImageUrl || doc.contentType.startsWith('image/')) {
      return 'image';
    }
    return 'description';
  }

  fileKindClass(doc: StagedDocument): string {
    if (doc.hasPdfPreview) {
      return 'pdf';
    }
    if (doc.previewImageUrl || doc.contentType.startsWith('image/')) {
      return 'image';
    }
    return 'file';
  }

  sourceIcon(channel: DocumentSourceChannel): string {
    switch (channel) {
      case 'KYC_ONBOARDING':
        return 'verified_user';
      case 'ORGANIZATION_COMPLIANCE':
        return 'gavel';
      case 'USER_PROFILE':
        return 'person';
      case 'ORGANIZATION_BRANDING':
        return 'palette';
      default:
        return 'cloud_upload';
    }
  }

  sourceTone(scope: StagedDocument['sourceScope']): string {
    return scope === 'ORGANIZATION' ? 'org' : 'user';
  }

  trackDoc(_: number, doc: StagedDocument): number {
    return doc.id;
  }

  private sortDocuments(rows: StagedDocument[]): StagedDocument[] {
    const sorted = [...rows];
    switch (this.sortBy()) {
      case 'oldest':
        return sorted.sort((a, b) => (Date.parse(a.createdAt) || 0) - (Date.parse(b.createdAt) || 0));
      case 'name':
        return sorted.sort((a, b) => a.displayTitle.localeCompare(b.displayTitle));
      case 'size':
        return sorted.sort((a, b) => (b.fileSizeInBytes ?? 0) - (a.fileSizeInBytes ?? 0));
      default:
        return sorted.sort((a, b) => (Date.parse(b.createdAt) || 0) - (Date.parse(a.createdAt) || 0));
    }
  }

  private matchesFilter(doc: StagedDocument, filter: DocumentFilterId): boolean {
    switch (filter) {
      case 'KYC':
        return doc.sourceChannel === 'KYC_ONBOARDING';
      case 'COMPLIANCE':
        return doc.sourceChannel === 'ORGANIZATION_COMPLIANCE';
      case 'PROFILE':
        return doc.sourceChannel === 'USER_PROFILE';
      case 'BRANDING':
        return doc.sourceChannel === 'ORGANIZATION_BRANDING';
      case 'OTHER':
        return doc.sourceChannel === 'FILE_UPLOAD_REGISTRY';
      default:
        return true;
    }
  }
}
