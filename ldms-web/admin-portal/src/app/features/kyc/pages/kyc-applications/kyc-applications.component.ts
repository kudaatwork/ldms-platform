import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PageEvent } from '@angular/material/paginator';
import { DEFAULT_TABLE_PAGE_SIZE } from '@shared/constants/table-pagination';
import {
  LxExportFormat,
  exportClientTableAsCsv,
} from '@shared/utils/lx-export.util';
import type {
  KycApplicationDecisionResult,
  KycApplicationDetail,
  KycQueueRow,
} from '../../../organizations/models/organization.model';
import { kycStatusPresentation } from '../../../organizations/models/organization.model';
import {
  OrganizationsAdminService,
  type OrganizationTableQuery,
} from '../../../organizations/services/organizations-admin.service';
import { KycQueueStatsService } from '../../../../core/services/kyc-queue-stats.service';
import { StorageService } from '../../../../core/services/storage.service';
import type { KycQueueSummary } from '../../../organizations/services/organizations-admin.service';
import { KycApplicationDetailDialogComponent } from '../kyc-application-detail-dialog/kyc-application-detail-dialog.component';
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
  selector: 'app-kyc-applications',
  templateUrl: './kyc-applications.component.html',
  styleUrl: './kyc-applications.component.scss',
  standalone: false,
})
export class KycApplicationsComponent implements OnInit, OnDestroy {
  readonly pageLead =
    'Platform signup organisations — includes new registrations in Draft (not yet submitted), the two-stage review pipeline, and rejected applications. Admin-registered organisations appear in the Organisations directory instead.';

  fetching = true;
  loadError = '';

  displayedColumns = ['applicant', 'classification', 'submitted', 'status', 'reviewers', 'actions'];

  dataSource: KycQueueRow[] = [];
  totalRecords = 0;
  queueSummary: KycQueueSummary | null = null;

  searchQuery = '';
  filterFieldsOpen = false;

  columnFilters = {
    applicant: '',
    classificationLabel: '',
    submitted: '',
    statusLabel: '',
  };

  pageIndex = 0;
  pageSize = DEFAULT_TABLE_PAGE_SIZE;

  private readonly filterReload$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();
  private latestLoadToken = 0;
  private lastFilterSignature = '';

  constructor(
    private readonly title: Title,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    private readonly orgService: OrganizationsAdminService,
    private readonly kycStats: KycQueueStatsService,
    private readonly storage: StorageService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('KYC Applications | LX Admin');
    this.lastFilterSignature = this.currentFilterSignature();

    merge(of(undefined as void), this.filterReload$.pipe(debounceTime(150)))
      .pipe(
        switchMap(() => this.runTableQuery({ background: false })),
        takeUntil(this.destroy$),
      )
      .subscribe();

    this.kycStats.summary$.pipe(takeUntil(this.destroy$)).subscribe((summary) => {
      this.queueSummary = summary;
      this.cdr.markForCheck();
    });
    this.refreshQueueSummary();
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
      !!this.searchQuery.trim() ||
      !!this.columnFilters.applicant.trim() ||
      !!this.columnFilters.classificationLabel.trim() ||
      !!this.columnFilters.submitted.trim() ||
      !!this.columnFilters.statusLabel.trim()
    );
  }

  openApplication(row: KycQueueRow): void {
    this.orgService.getOrganization(row.id).subscribe({
      next: (detail) => this.openDetailDialog(detail),
      error: (err: Error) => {
        this.snackBar.open(err.message ?? 'Could not load application', 'Dismiss', { duration: 6000 });
      },
    });
  }

  stubImport(): void {
    this.snackBar.open('CSV import for KYC applications is not available yet.', 'Close', { duration: 4000 });
  }

  exportAs(format: LxExportFormat): void {
    const ok = exportClientTableAsCsv(
      format,
      this.dataSource,
      [
        { header: 'applicant', value: (r) => r.applicant },
        { header: 'classification', value: (r) => r.classificationLabel },
        { header: 'submitted', value: (r) => r.submitted },
        { header: 'status', value: (r) => r.statusLabel },
        { header: 'stage1', value: (r) => r.stage1ApproverLabel ?? '—' },
        { header: 'stage2', value: (r) => r.stage2ApproverLabel ?? '—' },
      ],
      'kyc-applications',
      (message) => this.snackBar.open(message, 'Close', { duration: 4500 }),
    );
    if (ok) {
      this.snackBar.open('Exported KYC applications as CSV.', 'Close', {
        duration: 3500,
        panelClass: ['app-snackbar-success'],
      });
    }
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
      columnFilters: {
        name: this.columnFilters.applicant,
        classificationLabel: this.columnFilters.classificationLabel,
        statusLabel: this.columnFilters.statusLabel,
      },
      kycQueueOnly: true,
    };
    return this.orgService.queryTablePage(query).pipe(
      tap(({ rows, totalElements }) => this.applyLoadedPage(loadToken, rows, totalElements)),
      catchError((err: Error) => {
        if (loadToken !== this.latestLoadToken) {
          return EMPTY;
        }
        this.loadError = err.message ?? 'Failed to load KYC queue';
        this.dataSource = [];
        this.totalRecords = 0;
        this.snackBar.open(this.loadError, 'Dismiss', { duration: 8000 });
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

  private openDetailDialog(detail: KycApplicationDetail): void {
    this.dialog
      .open(KycApplicationDetailDialogComponent, {
        width: 'min(1080px, 98vw)',
        maxWidth: '98vw',
        maxHeight: '94vh',
        panelClass: 'kyc-review-dialog-panel',
        autoFocus: 'first-tabbable',
        data: { detail },
      })
      .afterClosed()
      .subscribe((result: KycApplicationDecisionResult | undefined) => {
        if (!result) {
          return;
        }
        this.applyDecision(detail.id, result);
      });
  }

  private applyDecision(orgId: number, result: KycApplicationDecisionResult): void {
    const notes = result.reason;
    const reviewerUsername = this.resolveReviewerUsername();
    const payload = { notes, reviewerUsername };
    const rejectPayload = { ...payload, rejectionReason: notes };
    let action$;
    switch (result.action) {
      case 'stage1-approve':
        action$ = this.orgService.stage1Approve(orgId, payload);
        break;
      case 'stage1-reject':
        action$ = this.orgService.stage1Reject(orgId, rejectPayload);
        break;
      case 'stage2-approve':
        action$ = this.orgService.stage2Approve(orgId, payload);
        break;
      case 'stage2-reject':
        action$ = this.orgService.stage2Reject(orgId, rejectPayload);
        break;
      case 'allow-resubmission':
        action$ = this.orgService.allowResubmission(orgId, payload);
        break;
      default:
        return;
    }
    action$.subscribe({
      next: () => {
        const pres = kycStatusPresentation(this.expectedStatusAfter(result.action));
        this.snackBar.open(`Application updated — ${pres.label}.`, 'Dismiss', {
          duration: 6000,
          panelClass: ['app-snackbar-success'],
        });
        this.refresh();
        this.refreshQueueSummary();
      },
      error: (err: Error) => {
        this.snackBar.open(err.message ?? 'Action failed', 'Dismiss', { duration: 8000 });
      },
    });
  }

  private refreshQueueSummary(): void {
    this.kycStats.refresh().pipe(takeUntil(this.destroy$)).subscribe();
  }

  private resolveReviewerUsername(): string {
    const email = this.storage.getUser()?.email?.trim();
    const username = this.storage.getUser()?.username?.trim();
    return email || username || '';
  }

  private expectedStatusAfter(action: KycApplicationDecisionResult['action']): string {
    switch (action) {
      case 'stage1-approve':
        return 'STAGE_2_REVIEW';
      case 'stage1-reject':
      case 'stage2-reject':
        return 'REJECTED';
      case 'stage2-approve':
        return 'APPROVED';
      case 'allow-resubmission':
        return 'RESUBMITTED';
      default:
        return '';
    }
  }

  private currentFilterSignature(): string {
    return JSON.stringify({ q: this.searchQuery.trim(), filters: this.columnFilters });
  }

  queueStatPending(): number {
    return this.queueSummary?.totalInQueue ?? this.totalRecords;
  }

  queueStatStage1(): number {
    return this.queueSummary?.stage1Count ?? 0;
  }

  queueStatStage2(): number {
    return this.queueSummary?.stage2Count ?? 0;
  }
}
