import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PageEvent } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { Title } from '@angular/platform-browser';
import {
  EMPTY,
  Observable,
  Subject,
  catchError,
  debounceTime,
  finalize,
  merge,
  of,
  switchMap,
  takeUntil,
  tap,
} from 'rxjs';
import { DEFAULT_TABLE_PAGE_SIZE } from '@shared/constants/table-pagination';
import { downloadBlob, exportFilename } from '@shared/utils/lx-export.util';
import { DeleteConfirmDialogComponent } from '@shared/components/delete-confirm-dialog/delete-confirm-dialog.component';
import { OrganizationsAdminService } from '../../services/organizations-admin.service';
import type { IndustryUsageRow } from '../../models/organization-directory.model';
import {
  IndustryFormDialogComponent,
  type IndustryFormDialogData,
  type IndustryFormDialogResult,
} from '../industry-form-dialog/industry-form-dialog.component';
import { IndustryLinkedOrganizationsDialogComponent } from '../industry-linked-organizations-dialog/industry-linked-organizations-dialog.component';

@Component({
  selector: 'app-industries-list',
  templateUrl: './industries-list.component.html',
  styleUrl: './industries-list.component.scss',
  standalone: false,
})
export class IndustriesListComponent implements OnInit, OnDestroy {
  readonly pageLead =
    'Industry sectors and how they are adopted across registered organisations. Usage counts update from live organisation records.';

  fetching = false;
  actionInProgress = false;
  exporting = false;
  loadError = '';
  searchQuery = '';
  filterFieldsOpen = false;
  columnFilters = { name: '', industryCode: '' };

  showSampleCsvInfo = false;
  readonly sampleCsvDescription =
    'Industry imports use NAME, industry code, description, regulatory body fields, and ACTIVE. Leave ID empty when creating new rows.';
  readonly importCsvTooltip = 'Import accepts CSV only (UTF-8). NAME is required on each row.';
  readonly importCsvDisclaimer =
    'CSV import only. NAME is required. Leave ID column empty on create.';

  displayedColumns = ['name', 'industryCode', 'usage', 'linked', 'status', 'actions'];
  dataSource = new MatTableDataSource<IndustryUsageRow>([]);

  totalRecords = 0;
  pageIndex = 0;
  pageSize = DEFAULT_TABLE_PAGE_SIZE;

  private readonly destroy$ = new Subject<void>();
  private readonly filterReload$ = new Subject<void>();
  private latestLoadToken = 0;
  private lastFilterSignature = '';

  constructor(
    private readonly title: Title,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    private readonly orgService: OrganizationsAdminService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Industries | LX Admin');
    this.lastFilterSignature = this.currentFilterSignature();
    merge(of(undefined as void), this.filterReload$.pipe(debounceTime(150)))
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

  onPage(e: PageEvent): void {
    if (e.pageIndex === this.pageIndex && e.pageSize === this.pageSize) {
      return;
    }
    this.pageIndex = e.pageIndex;
    this.pageSize = e.pageSize;
    this.runTableQuery({ background: false }).pipe(takeUntil(this.destroy$)).subscribe();
  }

  hasActiveFilters(): boolean {
    return (
      !!this.searchQuery.trim() ||
      !!this.columnFilters.name.trim() ||
      !!this.columnFilters.industryCode.trim()
    );
  }

  viewOrganizations(row: IndustryUsageRow): void {
    if (!row.id) {
      return;
    }
    this.dialog.open(IndustryLinkedOrganizationsDialogComponent, {
      width: '720px',
      maxWidth: '95vw',
      panelClass: 'lx-location-dialog-panel',
      data: { industryId: row.id, industryName: row.name },
    });
  }

  openCreate(): void {
    this.openFormDialog({ action: 'create' });
  }

  openEdit(row: IndustryUsageRow): void {
    this.openFormDialog({ action: 'edit', row });
  }

  confirmDelete(row: IndustryUsageRow): void {
    if (row.organizationCount > 0) {
      this.snackBar.open('Remove linked organisations before deleting this industry.', 'Close', {
        duration: 4000,
      });
      return;
    }
    this.dialog.open(DeleteConfirmDialogComponent, {
      width: '420px',
      data: {
        entityLabel: `industry “${row.name}”`,
        onConfirm: () => this.deleteIndustry(row),
      },
    });
  }

  refresh(): void {
    this.runTableQuery({ background: false }).pipe(takeUntil(this.destroy$)).subscribe();
  }

  viewRow(row: IndustryUsageRow): void {
    this.openFormDialog({ action: 'view', row });
  }

  // ─── Import / Export / Sample CSV ────────────────────────────────────────────

  importCsv(input: HTMLInputElement): void {
    input.click();
  }

  onImportFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.actionInProgress = true;
    this.orgService
      .importIndustriesCsv(file)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          if (response.ok) {
            this.showSuccess(response.message ?? 'Import completed successfully.');
          } else {
            this.showError(response.message ?? 'Import failed.');
          }
          input.value = '';
          this.actionInProgress = false;
          setTimeout(
            () => this.runTableQuery({ background: true }).pipe(takeUntil(this.destroy$)).subscribe(),
            250,
          );
        },
        error: (err: Error) => {
          this.showError(err.message ?? 'Import failed.');
          input.value = '';
          this.actionInProgress = false;
          setTimeout(
            () => this.runTableQuery({ background: true }).pipe(takeUntil(this.destroy$)).subscribe(),
            250,
          );
        },
      });
  }

  downloadSampleCsv(): void {
    const template = this.orgService.getIndustrySampleCsv();
    this.download(template.blob, template.filename);
    this.showSuccess('Sample CSV downloaded.');
  }

  exportAs(format: 'csv' | 'xlsx' | 'pdf'): void {
    this.exporting = true;
    this.orgService
      .exportIndustries(format, {
        page: this.pageIndex,
        size: this.pageSize,
        searchQuery: this.searchQuery,
        columnFilters: this.columnFilters,
      })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.exporting = false)),
      )
      .subscribe({
        next: (blob) => {
          downloadBlob(blob, exportFilename('industries', format));
          this.showSuccess(`Exported industries as ${format.toUpperCase()}.`);
        },
        error: (err: Error) => {
          this.showError(err.message ?? 'Export failed.');
        },
      });
  }

  private download(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 60_000);
  }

  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Close', { duration: 4000, panelClass: ['app-snackbar-success'] });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Close', { duration: 5000, panelClass: ['app-snackbar-error'] });
  }

  private openFormDialog(data: IndustryFormDialogData): void {
    const ref = this.dialog.open(IndustryFormDialogComponent, {
      width: '640px',
      maxWidth: '95vw',
      panelClass: 'lx-location-dialog-panel',
      autoFocus: 'first-tabbable',
      disableClose: true,
      data,
    });
    ref.afterClosed().subscribe((result: IndustryFormDialogResult | undefined) => {
      if (result?.saved) {
        this.runTableQuery({ background: false }).pipe(takeUntil(this.destroy$)).subscribe();
      }
    });
  }

  private deleteIndustry(row: IndustryUsageRow): void {
    this.orgService
      .deleteIndustry(row.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.snackBar.open('Industry deleted.', 'Close', { duration: 3000 });
          this.runTableQuery({ background: false }).pipe(takeUntil(this.destroy$)).subscribe();
        },
        error: (err: Error) => {
          this.snackBar.open(err.message ?? 'Delete failed', 'Close', { duration: 5000 });
        },
      });
  }

  /**
   * Server-paged load (same pattern as locations countries table).
   */
  private runTableQuery(opts?: { background?: boolean }): Observable<{ rows: IndustryUsageRow[]; totalElements: number }> {
    const loadToken = ++this.latestLoadToken;
    const background = opts?.background === true;
    if (!background || this.totalRecords === 0) {
      this.fetching = true;
    }
    this.loadError = '';
    return this.orgService
      .queryIndustriesPage({
        page: this.pageIndex,
        size: this.pageSize,
        searchQuery: this.searchQuery,
        columnFilters: this.columnFilters,
      })
      .pipe(
        tap(({ rows, totalElements }) => this.applyLoadedPage(loadToken, rows, totalElements)),
        catchError((err: Error) => {
          if (loadToken !== this.latestLoadToken) {
            return EMPTY;
          }
          this.loadError = err.message ?? 'Failed to load industries';
          this.dataSource.data = [];
          this.totalRecords = 0;
          return EMPTY;
        }),
        finalize(() => {
          if (loadToken === this.latestLoadToken) {
            this.fetching = false;
            this.cdr.markForCheck();
          }
        }),
      );
  }

  private applyLoadedPage(loadToken: number, rows: IndustryUsageRow[], totalElements: number): void {
    if (loadToken !== this.latestLoadToken) {
      return;
    }
    if (rows.length === 0 && totalElements > 0 && this.pageIndex > 0) {
      this.pageIndex = 0;
      this.runTableQuery({ background: true }).pipe(takeUntil(this.destroy$)).subscribe();
      return;
    }
    this.totalRecords = totalElements > 0 ? totalElements : rows.length;
    this.dataSource.data = rows;
    this.cdr.markForCheck();
  }

  private currentFilterSignature(): string {
    return JSON.stringify({
      q: this.searchQuery.trim(),
      filters: this.columnFilters,
    });
  }
}
