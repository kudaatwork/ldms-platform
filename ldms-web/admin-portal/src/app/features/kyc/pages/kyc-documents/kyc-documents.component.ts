import { Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { PageEvent } from '@angular/material/paginator';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, takeUntil, timeout } from 'rxjs';
import { filterByGlobalAndColumns } from '@shared/utils/table-search.util';
import { DEFAULT_TABLE_PAGE_SIZE } from '@shared/constants/table-pagination';
import {
  LxExportFormat,
  exportClientTableAsCsv,
} from '@shared/utils/lx-export.util';
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

  loading = true;
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

  dataSource: KycDocumentTableRow[] = [];

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

  constructor(
    private readonly title: Title,
    private readonly snackBar: MatSnackBar,
    private readonly dialog: MatDialog,
    private readonly catalog: KycDocumentsAdminService,
    private readonly fileUpload: FileUploadAdminService,
  ) {}

  get filteredRows(): KycDocumentTableRow[] {
    return filterByGlobalAndColumns(this.dataSource, this.searchQuery, this.columnFilters);
  }

  get clampedPageIndex(): number {
    const total = this.filteredRows.length;
    if (total === 0) {
      return 0;
    }
    const max = Math.max(0, Math.ceil(total / this.pageSize) - 1);
    return Math.min(this.pageIndex, max);
  }

  get pagedRows(): KycDocumentTableRow[] {
    const all = this.filteredRows;
    const start = this.clampedPageIndex * this.pageSize;
    return all.slice(start, start + this.pageSize);
  }

  resetPaging(): void {
    this.pageIndex = 0;
  }

  onPage(e: PageEvent): void {
    this.pageIndex = e.pageIndex;
    this.pageSize = e.pageSize;
  }

  ngOnInit(): void {
    this.title.setTitle('Compliance Documents | LX Admin');
    this.reload();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  reload(): void {
    this.loading = true;
    this.loadError = '';
    this.catalog
      .loadCatalog()
      .pipe(timeout(45_000), takeUntil(this.destroy$))
      .subscribe({
        next: (rows) => {
          this.dataSource = rows;
          this.loading = false;
          this.loadError = '';
        },
        error: () => {
          this.loading = false;
          this.dataSource = [];
          this.loadError =
            'Could not load the document catalogue in time. Check the API gateway and organisation-management service, then retry.';
        },
      });
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
          this.dataSource = this.dataSource.filter((r) => r.uploadId !== row.uploadId);
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
      this.filteredRows,
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
}
