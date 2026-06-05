import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { PageEvent } from '@angular/material/paginator';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Subject, debounceTime, merge, of, takeUntil } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';
import { VAULT_PAGE_SIZE, VAULT_PAGE_SIZE_OPTIONS } from '@shared/constants/table-pagination';
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
  private readonly pageReload$ = new Subject<void>();
  private readonly searchReload$ = new Subject<void>();
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
  readonly pageIndex = signal(0);
  readonly pageSize = signal(VAULT_PAGE_SIZE);
  readonly totalElements = signal(0);
  readonly filterCounts = signal<Partial<Record<DocumentFilterId, number>>>({});
  readonly filterCountsLoading = signal(false);

  readonly pageSizeOptions = VAULT_PAGE_SIZE_OPTIONS;

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
    this.refreshFilterCounts();
    merge(of(undefined), this.pageReload$, this.searchReload$.pipe(debounceTime(350)))
      .pipe(
        switchMap(() => this.loadPage()),
        takeUntil(this.destroy$),
      )
      .subscribe();
    this.searchReload$
      .pipe(debounceTime(350), takeUntil(this.destroy$))
      .subscribe(() => this.refreshFilterCounts());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get orgName(): string {
    return this.authState.currentUser?.orgName ?? 'Your organisation';
  }

  get heroLead(): string {
    return `The complete document library for ${this.orgName} — onboarding packs, compliance certificates, profile IDs, branding assets, and every upload registered in LDMS, with full metadata and instant preview.`;
  }

  get displayDocuments(): StagedDocument[] {
    return this.sortDocuments(this.documents);
  }

  get stats() {
    const page = this.documents;
    return {
      total: this.filterCounts()['ALL'] ?? this.totalElements(),
      org: page.filter((d) => d.sourceScope === 'ORGANIZATION').length,
      user: page.filter((d) => d.sourceScope === 'USER').length,
      verified: page.filter((d) => d.autoVerified).length,
      withPreview: page.filter((d) => d.hasPreview).length,
    };
  }

  get pageRangeLabel(): string {
    const total = this.totalElements();
    if (total < 1) {
      return 'No documents';
    }
    const start = this.pageIndex() * this.pageSize() + 1;
    const end = Math.min(total, (this.pageIndex() + 1) * this.pageSize());
    return `${start.toLocaleString()}–${end.toLocaleString()} of ${total.toLocaleString()}`;
  }

  onSearchInput(value: string): void {
    this.search.set(value);
    if (this.pageIndex() !== 0) {
      this.pageIndex.set(0);
    }
    this.searchReload$.next();
  }

  reload(): void {
    this.refreshFilterCounts();
    this.pageReload$.next();
  }

  filterCount(id: DocumentFilterId): string {
    if (this.filterCountsLoading()) {
      return '…';
    }
    const count = this.filterCounts()[id];
    return count != null ? count.toLocaleString() : '—';
  }

  filterCssClass(id: DocumentFilterId): string {
    return `vault-filter--${id.toLowerCase()}`;
  }

  setFilter(id: DocumentFilterId): void {
    this.filter.set(id);
    this.pageIndex.set(0);
    this.pageReload$.next();
  }

  onSortChange(value: DocumentSortId): void {
    this.sortBy.set(value);
    this.cdr.markForCheck();
  }

  onPage(event: PageEvent): void {
    if (event.pageIndex === this.pageIndex() && event.pageSize === this.pageSize()) {
      return;
    }
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.pageReload$.next();
  }

  setViewMode(mode: ViewMode): void {
    this.viewMode.set(mode);
    if (mode === 'focus' && this.displayDocuments.length && !this.selected) {
      this.selectDocument(this.displayDocuments[0], false);
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

  private refreshFilterCounts(): void {
    this.filterCountsLoading.set(true);
    this.docsApi
      .loadFilterCounts(this.search())
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (counts) => {
          this.filterCounts.set(counts);
          this.filterCountsLoading.set(false);
          this.cdr.markForCheck();
        },
        error: () => {
          this.filterCountsLoading.set(false);
          this.cdr.markForCheck();
        },
      });
  }

  private loadPage() {
    this.loading.set(true);
    this.loadError.set('');
    return this.docsApi
      .queryVaultPage({
        page: this.pageIndex(),
        size: this.pageSize(),
        searchQuery: this.search(),
        categoryFilter: this.filter(),
      })
      .pipe(
        tap((page) => {
          this.documents = page.documents;
          this.totalElements.set(page.totalElements);
          this.loading.set(false);
          if (page.documents.length && !this.selected) {
            this.selectDocument(page.documents[0], false);
          } else if (this.selected && !page.documents.some((d) => d.id === this.selected?.id)) {
            this.selected = page.documents[0] ?? null;
            this.selectedId.set(this.selected?.id ?? null);
          }
          this.cdr.markForCheck();
        }),
        catchError((err: Error) => {
          this.loadError.set(err.message);
          this.documents = [];
          this.totalElements.set(0);
          this.loading.set(false);
          this.cdr.markForCheck();
          return of(null);
        }),
      );
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
}
