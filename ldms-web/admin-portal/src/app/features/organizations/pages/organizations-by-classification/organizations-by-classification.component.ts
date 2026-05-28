import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { map } from 'rxjs/operators';
import { PageEvent } from '@angular/material/paginator';
import { classificationLabel } from '../../../../shared/models/org-classifications';
import { DEFAULT_TABLE_PAGE_SIZE } from '@shared/constants/table-pagination';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { DeleteConfirmDialogComponent } from '@shared/components/delete-confirm-dialog/delete-confirm-dialog.component';
import {
  LxExportFormat,
  downloadBlob,
  exportFilename,
} from '@shared/utils/lx-export.util';
import {
  OrganizationsAdminService,
  type OrganizationTableQuery,
} from '../../services/organizations-admin.service';
import type { OrganizationClassification, KycQueueRow } from '../../models/organization.model';
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
  selector: 'app-organizations-by-classification',
  templateUrl: './organizations-by-classification.component.html',
  styleUrl: './organizations-by-classification.component.scss',
  standalone: false,
})
export class OrganizationsByClassificationComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly title = inject(Title);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly orgService = inject(OrganizationsAdminService);
  private readonly cdr = inject(ChangeDetectorRef);

  fetching = true;
  exporting = false;
  loadError = '';

  readonly slug$ = this.route.paramMap.pipe(map((p) => p.get('slug') ?? ''));

  displayedColumns = [
    'name',
    'email',
    'phone',
    'type',
    'industry',
    'source',
    'status',
    'verified',
    'actions',
  ];

  dataSource: KycQueueRow[] = [];
  totalRecords = 0;

  searchQuery = '';
  filterFieldsOpen = false;

  columnFilters = {
    name: '',
    statusLabel: '',
  };

  pageIndex = 0;
  pageSize = DEFAULT_TABLE_PAGE_SIZE;

  classificationSlug: OrganizationClassification | '' = '';

  private readonly filterReload$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();
  private latestLoadToken = 0;
  private lastFilterSignature = '';

  get pageTitle(): string {
    return this.classificationSlug
      ? classificationLabel(this.classificationSlug)
      : 'Organisations';
  }

  get pageLead(): string {
    if (!this.classificationSlug) {
      return 'Organisations filtered by classification.';
    }
    return `Organisations classified as ${this.pageTitle}. Admin registrations appear here immediately; platform signups appear after KYC approval.`;
  }

  ngOnInit(): void {
    merge(of(undefined as void), this.filterReload$.pipe(debounceTime(150)))
      .pipe(
        switchMap(() => this.runTableQuery({ background: false })),
        takeUntil(this.destroy$),
      )
      .subscribe();

    this.slug$.pipe(takeUntil(this.destroy$)).subscribe((slug) => {
      this.classificationSlug = (slug as OrganizationClassification) || '';
      this.title.setTitle(`${this.pageTitle} | LX Admin`);
      this.pageIndex = 0;
      this.lastFilterSignature = this.currentFilterSignature();
      this.filterReload$.next();
    });
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
    return !!this.searchQuery.trim() || !!this.columnFilters.name.trim() || !!this.columnFilters.statusLabel.trim();
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

  exportAs(format: LxExportFormat): void {
    if (!this.classificationSlug || this.exporting) {
      return;
    }
    this.exporting = true;
    const query: OrganizationTableQuery = {
      page: this.pageIndex,
      size: this.pageSize,
      searchQuery: this.searchQuery,
      columnFilters: { ...this.columnFilters },
      organizationClassification: this.classificationSlug,
      kycQueueOnly: false,
      organizationDirectoryOnly: true,
    };
    this.orgService
      .exportOrganizations(format, query)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.exporting = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (blob) => {
          downloadBlob(blob, exportFilename(`organizations-${this.classificationSlug}`, format));
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
    if (!this.classificationSlug) {
      this.dataSource = [];
      this.totalRecords = 0;
      this.fetching = false;
      this.cdr.markForCheck();
      return of({ rows: [], totalElements: 0 });
    }

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
      organizationClassification: this.classificationSlug,
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
    return JSON.stringify({ q: this.searchQuery.trim(), filters: this.columnFilters });
  }
}
