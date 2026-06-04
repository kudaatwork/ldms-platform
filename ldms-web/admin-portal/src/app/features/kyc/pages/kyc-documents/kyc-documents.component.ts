import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { Title } from '@angular/platform-browser';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { PageEvent } from '@angular/material/paginator';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, debounceTime, merge, of, takeUntil } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';
import { DeleteConfirmDialogComponent } from '@shared/components/delete-confirm-dialog/delete-confirm-dialog.component';
import { VAULT_PAGE_SIZE, VAULT_PAGE_SIZE_OPTIONS } from '@shared/constants/table-pagination';
import { LxExportFormat, exportClientTableAsCsv } from '@shared/utils/lx-export.util';
import { FileUploadAdminService } from '../../../../core/services/file-upload-admin.service';
import {
  AdminStagedDocument,
  DocumentFilterId,
  DocumentSortId,
  DocumentSourceChannel,
  KycDocumentsAdminService,
} from '../../services/kyc-documents-admin.service';

type ViewMode = 'grid' | 'list' | 'focus';
type DetailTab = 'preview' | 'metadata' | 'verification';

@Component({
  selector: 'app-kyc-documents',
  templateUrl: './kyc-documents.component.html',
  styleUrl: './kyc-documents.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class KycDocumentsComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly pageReload$ = new Subject<void>();
  private readonly searchReload$ = new Subject<void>();
  private readonly docsApi = inject(KycDocumentsAdminService);
  private readonly fileUpload = inject(FileUploadAdminService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly title = inject(Title);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

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
  readonly deletingId = signal<number | null>(null);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(VAULT_PAGE_SIZE);
  readonly totalElements = signal(0);
  readonly filterCounts = signal<Partial<Record<DocumentFilterId, number>>>({});
  readonly filterCountsLoading = signal(false);

  readonly pageSizeOptions = VAULT_PAGE_SIZE_OPTIONS;

  documents: AdminStagedDocument[] = [];
  selected: AdminStagedDocument | null = null;
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
    this.title.setTitle('Compliance Documents | LX Admin');
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

  get displayDocuments(): AdminStagedDocument[] {
    return this.sortDocuments(this.documents);
  }

  get stats() {
    const page = this.documents;
    return {
      total: this.filterCounts()['ALL'] ?? this.totalElements(),
      org: page.filter((d) => d.sourceScope === 'ORGANIZATION').length,
      user: page.filter((d) => d.sourceScope === 'USER').length,
      verified: page.filter((d) => d.autoVerified).length,
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

  selectDocument(doc: AdminStagedDocument, openInspector = true): void {
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
      .fetchVaultDocumentDetail(doc.id)
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

  downloadDocument(doc: AdminStagedDocument | null): void {
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
    this.snackBar.open('Upload ID copied.', 'Close', { duration: 2500 });
  }

  deleteDocument(doc: AdminStagedDocument): void {
    const ref = this.dialog.open(DeleteConfirmDialogComponent, {
      width: '420px',
      data: {
        title: 'Delete document',
        message: `Soft-delete "${doc.fileName}" (upload #${doc.id})? This removes it from active file-upload records.`,
      },
    });
    ref.afterClosed().subscribe((confirmed) => {
      if (!confirmed) {
        return;
      }
      this.deletingId.set(doc.id);
      this.fileUpload.deleteById(doc.id).subscribe({
        next: (ok) => {
          this.deletingId.set(null);
          if (!ok) {
            this.snackBar.open('Delete failed.', 'Close', {
              duration: 5000,
              panelClass: ['app-snackbar-error'],
            });
            return;
          }
          this.totalElements.update((n) => Math.max(0, n - 1));
          this.documents = this.documents.filter((d) => d.id !== doc.id);
          if (this.selectedId() === doc.id) {
            this.selected = null;
            this.selectedId.set(null);
            this.inspectorOpen.set(false);
          }
          this.snackBar.open('Document deleted.', 'Close', {
            duration: 3500,
            panelClass: ['app-snackbar-success'],
          });
          this.refreshFilterCounts();
          this.pageReload$.next();
          this.cdr.markForCheck();
        },
        error: () => {
          this.deletingId.set(null);
          this.snackBar.open('Delete failed.', 'Close', {
            duration: 5000,
            panelClass: ['app-snackbar-error'],
          });
        },
      });
    });
  }

  exportAs(format: LxExportFormat): void {
    const rows = this.displayDocuments;
    const ok = exportClientTableAsCsv(
      format,
      rows,
      [
        { header: 'uploadId', value: (r) => r.id },
        { header: 'fileName', value: (r) => r.fileName },
        { header: 'organization', value: (r) => r.organizationName },
        { header: 'category', value: (r) => r.category },
        { header: 'sourceChannel', value: (r) => r.sourceChannel },
        { header: 'kycStatus', value: (r) => r.kycStatusLabel },
        { header: 'status', value: (r) => r.statusLabel },
        { header: 'uploadedAt', value: (r) => r.createdAt },
      ],
      'compliance-documents-page',
      (message) => this.snackBar.open(message, 'Close', { duration: 4500 }),
    );
    if (ok) {
      this.snackBar.open('Exported current page as CSV.', 'Close', {
        duration: 3500,
        panelClass: ['app-snackbar-success'],
      });
    }
  }

  formatSourceChannel(channel: DocumentSourceChannel): string {
    return channel.replace(/_/g, ' ');
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

  fileKindIcon(doc: AdminStagedDocument): string {
    if (doc.hasPdfPreview) {
      return 'picture_as_pdf';
    }
    if (doc.previewImageUrl || doc.contentType.startsWith('image/')) {
      return 'image';
    }
    return 'description';
  }

  fileKindClass(doc: AdminStagedDocument): string {
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

  sourceTone(scope: AdminStagedDocument['sourceScope']): string {
    return scope === 'ORGANIZATION' ? 'org' : 'user';
  }

  trackDoc(_: number, doc: AdminStagedDocument): number {
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

  private sortDocuments(rows: AdminStagedDocument[]): AdminStagedDocument[] {
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
