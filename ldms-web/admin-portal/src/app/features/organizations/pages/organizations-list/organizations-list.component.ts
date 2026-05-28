import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { PageEvent } from '@angular/material/paginator';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DeleteConfirmDialogComponent } from '@shared/components/delete-confirm-dialog/delete-confirm-dialog.component';
import { DEFAULT_TABLE_PAGE_SIZE } from '@shared/constants/table-pagination';
import {
  LxExportFormat,
  downloadBlob,
  exportFilename,
} from '@shared/utils/lx-export.util';
import {
  OrganizationsAdminService,
  type OrganizationTableQuery,
} from '../../services/organizations-admin.service';
import type { KycQueueRow } from '../../models/organization.model';
import {
  RegisterOrganizationDialogComponent,
  type RegisterOrganizationDialogResult,
} from '../register-organization-dialog/register-organization-dialog.component';
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

@Component({
  selector: 'app-organizations-list',
  templateUrl: './organizations-list.component.html',
  styleUrl: './organizations-list.component.scss',
  standalone: false,
})
export class OrganizationsListComponent implements OnInit, OnDestroy {
  readonly pageLead =
    'Approved organisations directory. Admin registrations appear here immediately; platform signups appear after KYC approval (see KYC Applications until then).';

  fetching = true;
  exporting = false;
  loadError = '';

  displayedColumns = [
    'name',
    'email',
    'phone',
    'classification',
    'type',
    'industry',
    'registration',
    'source',
    'status',
    'verified',
    'registered',
    'actions',
  ];

  dataSource: KycQueueRow[] = [];
  totalRecords = 0;

  searchQuery = '';
  filterFieldsOpen = false;

  columnFilters = {
    name: '',
    email: '',
    classificationLabel: '',
    statusLabel: '',
  };

  pageIndex = 0;
  pageSize = DEFAULT_TABLE_PAGE_SIZE;

  industryFilterId: number | null = null;
  industryFilterLabel = '';

  private readonly filterReload$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();
  private latestLoadToken = 0;
  private lastFilterSignature = '';

  constructor(
    private readonly title: Title,
    private readonly snackBar: MatSnackBar,
    private readonly dialog: MatDialog,
    private readonly orgService: OrganizationsAdminService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Organizations | LX Admin');
    this.lastFilterSignature = this.currentFilterSignature();

    merge(of(undefined as void), this.filterReload$.pipe(debounceTime(150)))
      .pipe(
        switchMap(() => this.runTableQuery({ background: false })),
        takeUntil(this.destroy$),
      )
      .subscribe();

    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const raw = params.get('industryId');
      const id = raw ? Number(raw) : NaN;
      this.industryFilterId = Number.isFinite(id) && id > 0 ? id : null;
      this.industryFilterLabel = params.get('industry') ?? '';
      this.pageIndex = 0;
      this.filterReload$.next();
    });
  }

  clearIndustryFilter(): void {
    void this.router.navigate(['/organizations'], { queryParams: {} });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  applyFilters(): void {
    const next = this.currentFilterSignature();
    if (next === this.lastFilterSignature) {
      return;
    }
    this.lastFilterSignature = next;
    this.pageIndex = 0;
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

  refresh(): void {
    this.runTableQuery({ background: false }).pipe(takeUntil(this.destroy$)).subscribe();
  }

  hasActiveFilters(): boolean {
    return (
      this.industryFilterId != null ||
      !!this.searchQuery.trim() ||
      !!this.columnFilters.name.trim() ||
      !!this.columnFilters.email.trim() ||
      !!this.columnFilters.classificationLabel.trim() ||
      !!this.columnFilters.statusLabel.trim()
    );
  }

  openRegister(): void {
    this.dialog
      .open(RegisterOrganizationDialogComponent, {
        width: '720px',
        maxWidth: '95vw',
        disableClose: true,
        panelClass: 'lx-location-dialog-panel',
        autoFocus: 'first-tabbable',
      })
      .afterClosed()
      .subscribe((result: RegisterOrganizationDialogResult | undefined) => {
        if (result?.saved) {
          this.refresh();
        }
      });
  }

  viewOrganization(row: KycQueueRow): void {
    void this.router.navigate(['/organizations', row.id]);
  }

  editOrganization(row: KycQueueRow): void {
    void this.router.navigate(['/organizations', row.id], { queryParams: { edit: 'true' } });
  }

  confirmDelete(row: KycQueueRow): void {
    this.dialog
      .open(DeleteConfirmDialogComponent, {
        width: '420px',
        maxWidth: '92vw',
        data: { entityLabel: `organisation "${row.name}"` },
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed: boolean) => {
        if (!confirmed) {
          return;
        }
        this.orgService
          .deleteOrganization(row.id)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              this.snackBar.open('Organisation deleted.', 'Close', {
                duration: 4500,
                panelClass: ['app-snackbar-success'],
              });
              this.refresh();
            },
            error: (err: Error) => {
              this.snackBar.open(err.message ?? 'Delete failed.', 'Close', {
                duration: 5500,
                panelClass: ['app-snackbar-error'],
              });
            },
          });
      });
  }

  stubImport(): void {
    this.snackBar.open('CSV import for organizations is not available yet.', 'Close', { duration: 4000 });
  }

  exportAs(format: LxExportFormat): void {
    if (this.exporting) {
      return;
    }
    this.exporting = true;
    const query: OrganizationTableQuery = {
      page: this.pageIndex,
      size: this.pageSize,
      searchQuery: this.searchQuery,
      columnFilters: { ...this.columnFilters },
      industryId: this.industryFilterId ?? '',
      kycQueueOnly: false,
      organizationDirectoryOnly: true,
    };
    this.orgService
      .exportOrganizations(format, query)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.exporting = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (blob) => {
          downloadBlob(blob, exportFilename('organizations', format));
          this.snackBar.open(`Exported organisations as ${format.toUpperCase()}.`, 'Close', {
            duration: 3500,
            panelClass: ['app-snackbar-success'],
          });
        },
        error: (err: Error) => {
          this.snackBar.open(err.message ?? 'Export failed.', 'Close', {
            duration: 5500,
            panelClass: ['app-snackbar-error'],
          });
        },
      });
  }

  private runTableQuery(opts?: { background?: boolean }): Observable<{ rows: KycQueueRow[]; totalElements: number }> {
    const loadToken = ++this.latestLoadToken;
    if (!opts?.background || this.totalRecords === 0) {
      this.fetching = true;
    }
    this.loadError = '';
    const query: OrganizationTableQuery = {
      page: this.pageIndex,
      size: this.pageSize,
      searchQuery: this.searchQuery,
      columnFilters: { ...this.columnFilters },
      industryId: this.industryFilterId ?? '',
      kycQueueOnly: false,
      organizationDirectoryOnly: true,
    };
    return this.orgService.queryTablePage(query).pipe(
      tap(({ rows, totalElements }) => this.applyLoadedPage(loadToken, rows, totalElements)),
      catchError((err: Error) => {
        if (loadToken !== this.latestLoadToken) {
          return EMPTY;
        }
        this.loadError = err.message ?? 'Failed to load organisations';
        this.dataSource = [];
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

  private applyLoadedPage(loadToken: number, rows: KycQueueRow[], totalElements: number): void {
    if (loadToken !== this.latestLoadToken) {
      return;
    }
    if (rows.length === 0 && totalElements > 0 && this.pageIndex > 0) {
      this.pageIndex = 0;
      this.runTableQuery({ background: true }).pipe(takeUntil(this.destroy$)).subscribe();
      return;
    }
    this.totalRecords = totalElements > 0 ? totalElements : rows.length;
    this.dataSource = rows;
    this.cdr.markForCheck();
  }

  private currentFilterSignature(): string {
    return JSON.stringify({
      q: this.searchQuery.trim(),
      filters: this.columnFilters,
      industryId: this.industryFilterId,
    });
  }
}
