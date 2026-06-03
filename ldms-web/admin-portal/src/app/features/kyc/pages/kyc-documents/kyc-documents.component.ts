import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { PageEvent } from '@angular/material/paginator';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { EMPTY, Observable, Subject, merge, of } from 'rxjs';
import { catchError, debounceTime, finalize, switchMap, takeUntil, tap } from 'rxjs/operators';
import {
  LxExportFormat,
  exportClientTableAsCsv,
} from '@shared/utils/lx-export.util';
import { DEFAULT_TABLE_PAGE_SIZE } from '@shared/constants/table-pagination';
import { DeleteConfirmDialogComponent } from '@shared/components/delete-confirm-dialog/delete-confirm-dialog.component';
import {
  UserDocumentDetailDialogComponent,
  type UserDocumentDetailDialogData,
} from '../../../users/components/user-document-detail-dialog/user-document-detail-dialog.component';
import {
  KycDocumentsAdminService,
  type KycDocumentTableRow,
} from '../../services/kyc-documents-admin.service';
import { FileUploadAdminService } from '../../../../core/services/file-upload-admin.service';

@Component({
  selector: 'app-kyc-documents',
  templateUrl: './kyc-documents.component.html',
  styleUrl: './kyc-documents.component.scss',
  standalone: false,
})
export class KycDocumentsComponent implements OnInit, OnDestroy {
  readonly pageLead =
    'Every file registered in LDMS — organisation compliance packs, profile uploads, and registry entries. Open View to load the full file and metadata from file-upload.';

  fetching = false;
  loadError = '';
  deletingId: number | null = null;

  displayedColumns = [
    'fileName',
    'organizationName',
    'type',
    'kycStatusLabel',
    'status',
    'uploadedAt',
    'actions',
  ];

  rows: KycDocumentTableRow[] = [];
  totalRecords = 0;

  searchQuery = '';
  filterFieldsOpen = false;

  columnFilters = {
    fileName: '',
    organizationName: '',
    type: '',
    statusLabel: '',
    kycStatusLabel: '',
  };

  pageIndex = 0;
  pageSize = DEFAULT_TABLE_PAGE_SIZE;

  private readonly destroy$ = new Subject<void>();
  private readonly filterReload$ = new Subject<void>();
  private latestLoadToken = 0;
  private lastFilterSignature = '';

  constructor(
    private readonly title: Title,
    private readonly snackBar: MatSnackBar,
    private readonly dialog: MatDialog,
    private readonly catalog: KycDocumentsAdminService,
    private readonly fileUpload: FileUploadAdminService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  get hasActiveFilters(): boolean {
    if (this.searchQuery.trim()) {
      return true;
    }
    return Object.values(this.columnFilters).some((v) => String(v ?? '').trim().length > 0);
  }

  ngOnInit(): void {
    this.title.setTitle('Compliance Documents | LX Admin');
    this.lastFilterSignature = this.currentFilterSignature();
    merge(of(undefined), this.filterReload$.pipe(debounceTime(150)))
      .pipe(
        switchMap(() => {
          this.pageIndex = 0;
          return this.runTableQuery({ background: false });
        }),
        takeUntil(this.destroy$),
      )
      .subscribe();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  applyFilters(): void {
    const nextSignature = this.currentFilterSignature();
    if (nextSignature === this.lastFilterSignature) {
      return;
    }
    this.lastFilterSignature = nextSignature;
    this.filterReload$.next();
  }

  onFiltersChanged(): void {
    this.applyFilters();
  }

  onPage(e: PageEvent): void {
    if (e.pageIndex === this.pageIndex && e.pageSize === this.pageSize) {
      return;
    }
    this.pageIndex = e.pageIndex;
    this.pageSize = e.pageSize;
    this.runTableQuery({ background: false }).pipe(takeUntil(this.destroy$)).subscribe();
  }

  reload(): void {
    this.filterReload$.next();
  }

  viewDocument(row: KycDocumentTableRow): void {
    this.dialog.open(UserDocumentDetailDialogComponent, {
      width: 'min(920px, 96vw)',
      maxHeight: '92vh',
      data: { id: row.uploadId } satisfies UserDocumentDetailDialogData,
    });
  }

  deleteDocument(row: KycDocumentTableRow): void {
    const ref = this.dialog.open(DeleteConfirmDialogComponent, {
      width: '420px',
      data: {
        title: 'Delete document',
        message: `Soft-delete "${row.fileName}" (upload #${row.uploadId})? This removes it from active file-upload records.`,
      },
    });
    ref.afterClosed().subscribe((confirmed) => {
      if (!confirmed) {
        return;
      }
      this.deletingId = row.uploadId;
      this.fileUpload.deleteById(row.uploadId).subscribe({
        next: (ok) => {
          this.deletingId = null;
          if (!ok) {
            this.snackBar.open('Delete failed. The file-upload service may have rejected the request.', 'Close', {
              duration: 5000,
              panelClass: ['app-snackbar-error'],
            });
            return;
          }
          this.filterReload$.next();
          this.snackBar.open('Document deleted.', 'Close', {
            duration: 3500,
            panelClass: ['app-snackbar-success'],
          });
        },
        error: () => {
          this.deletingId = null;
          this.snackBar.open('Delete failed.', 'Close', {
            duration: 5000,
            panelClass: ['app-snackbar-error'],
          });
        },
      });
    });
  }

  stubImport(): void {
    this.snackBar.open('Bulk import is not available for compliance documents. Upload via organisation registration or user profile.', 'Close', {
      duration: 4500,
    });
  }

  uploadHint(): void {
    this.snackBar.open(
      'New files are uploaded during organisation registration, compliance workflows, or user profile updates — stored in file-upload and listed here automatically.',
      'Close',
      { duration: 5500 },
    );
  }

  exportAs(format: LxExportFormat): void {
    const ok = exportClientTableAsCsv(
      format,
      this.rows,
      [
        { header: 'uploadId', value: (r) => r.uploadId },
        { header: 'fileName', value: (r) => r.fileName },
        { header: 'organization', value: (r) => r.organizationName },
        { header: 'type', value: (r) => r.type },
        { header: 'kycStatus', value: (r) => r.kycStatusLabel },
        { header: 'status', value: (r) => r.statusLabel },
        { header: 'uploadedAt', value: (r) => r.uploadedAt },
      ],
      'compliance-documents',
      (message) => this.snackBar.open(message, 'Close', { duration: 4500 }),
    );
    if (ok) {
      this.snackBar.open('Exported compliance documents as CSV.', 'Close', {
        duration: 3500,
        panelClass: ['app-snackbar-success'],
      });
    }
  }

  private runTableQuery(opts?: { background?: boolean }): Observable<KycDocumentTableRow[]> {
    const loadToken = ++this.latestLoadToken;
    const background = opts?.background === true;
    if (!background || this.totalRecords === 0) {
      this.fetching = true;
      this.loadError = '';
    }
    return this.catalog
      .queryTablePage({
        page: this.pageIndex,
        size: this.pageSize,
        searchQuery: this.searchQuery,
        columnFilters: this.columnFilters,
      })
      .pipe(
        tap(({ rows, totalElements }) => this.applyLoadedPage(loadToken, rows, totalElements)),
        catchError((err) => {
          if (loadToken !== this.latestLoadToken) {
            return EMPTY;
          }
          this.rows = [];
          this.totalRecords = 0;
          this.loadError =
            err instanceof Error
              ? err.message
              : 'Could not load compliance documents. Check the API gateway and file-upload service, then retry.';
          return EMPTY;
        }),
        finalize(() => {
          if (loadToken === this.latestLoadToken) {
            this.fetching = false;
            this.cdr.markForCheck();
          }
        }),
        switchMap(({ rows }) => of(rows)),
      );
  }

  private applyLoadedPage(loadToken: number, rows: KycDocumentTableRow[], totalElements: number): void {
    if (loadToken !== this.latestLoadToken) {
      return;
    }
    if (rows.length === 0 && totalElements > 0 && this.pageIndex > 0) {
      this.pageIndex = 0;
      this.runTableQuery({ background: true }).pipe(takeUntil(this.destroy$)).subscribe();
      return;
    }
    this.totalRecords = totalElements;
    this.rows = rows;
  }

  private currentFilterSignature(): string {
    return JSON.stringify({
      q: this.searchQuery.trim(),
      filters: this.columnFilters,
    });
  }
}
