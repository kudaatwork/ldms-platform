import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PageEvent } from '@angular/material/paginator';
import { DEFAULT_TABLE_PAGE_SIZE } from '@shared/constants/table-pagination';
import {
  LxExportFormat,
  exportClientTableAsCsv,
  exportFormatLabel,
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
import { KycNotificationDismissService } from '../../../../core/services/kyc-notification-dismiss.service';
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
  private lastOpenedApplicationId: number | null = null;

  constructor(
    private readonly title: Title,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    private readonly orgService: OrganizationsAdminService,
    private readonly kycStats: KycQueueStatsService,
    private readonly kycNotificationDismiss: KycNotificationDismissService,
    private readonly storage: StorageService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
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

    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const statusFromRoute = params.get('status')?.trim() ?? '';
      if (statusFromRoute && this.columnFilters.statusLabel !== statusFromRoute) {
        this.columnFilters.statusLabel = statusFromRoute;
        this.applyFilters();
      }

      const applicationId = Number(params.get('applicationId'));
      if (!Number.isFinite(applicationId) || applicationId <= 0) {
        this.lastOpenedApplicationId = null;
        return;
      }
      if (this.lastOpenedApplicationId === applicationId) {
        return;
      }
      this.lastOpenedApplicationId = applicationId;
      this.openApplicationById(applicationId);
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
    return (
      !!this.searchQuery.trim() ||
      !!this.columnFilters.applicant.trim() ||
      !!this.columnFilters.classificationLabel.trim() ||
      !!this.columnFilters.submitted.trim() ||
      !!this.columnFilters.statusLabel.trim()
    );
  }

  openApplication(row: KycQueueRow): void {
    this.openApplicationById(row.id);
  }

  private openApplicationById(applicationId: number): void {
    this.orgService.getOrganization(applicationId).subscribe({
      next: (detail) => {
        this.clearApplicationQueryParam();
        this.openDetailDialog(detail);
      },
      error: (err: Error) => {
        this.clearApplicationQueryParam();
        this.lastOpenedApplicationId = null;
        this.snackBar.open(err.message ?? 'Could not load application', 'Dismiss', { duration: 6000 });
      },
    });
  }

  private clearApplicationQueryParam(): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { applicationId: null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
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
      this.snackBar.open(`Exported KYC applications as ${exportFormatLabel(format)}.`, 'Close', {
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
    const { reviewerUsername, reviewerUserId } = this.resolveReviewerIdentity();
    const payload = { notes, reviewerUsername, reviewerUserId };
    const rejectPayload = { ...payload, rejectionReason: notes };
    const action$ = this.resolveDecisionAction(orgId, result, payload, rejectPayload);
    if (!action$) {
      return;
    }
    action$.subscribe({
      next: () => {
        this.kycNotificationDismiss.dismissOrganization(orgId);
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

  reviewerChips(row: KycQueueRow): { stage: number; label: string }[] {
    const count = row.effectiveKycRequiredApprovalStages ?? 2;
    const chips: { stage: number; label: string }[] = [];
    for (let stage = 1; stage <= count; stage++) {
      const label =
        stage === 1
          ? row.stage1ApproverLabel
          : stage === 2
            ? row.stage2ApproverLabel
            : stage === 3
              ? row.stage3ApproverLabel
              : stage === 4
                ? row.stage4ApproverLabel
                : row.stage5ApproverLabel;
      chips.push({ stage, label: label ?? '—' });
    }
    return chips;
  }

  private resolveDecisionAction(
    orgId: number,
    result: KycApplicationDecisionResult,
    payload: { notes: string; reviewerUsername: string; reviewerUserId?: number },
    rejectPayload: {
      notes: string;
      reviewerUsername: string;
      reviewerUserId?: number;
      rejectionReason: string;
    },
  ) {
    switch (result.action) {
      case 'stage1-approve':
        return this.orgService.stage1Approve(orgId, payload);
      case 'stage1-reject':
        return this.orgService.stage1Reject(orgId, rejectPayload);
      case 'stage2-approve':
        return this.orgService.stage2Approve(orgId, payload);
      case 'stage2-reject':
        return this.orgService.stage2Reject(orgId, rejectPayload);
      case 'stage3-approve':
        return this.orgService.stage3Approve(orgId, payload);
      case 'stage3-reject':
        return this.orgService.stage3Reject(orgId, rejectPayload);
      case 'stage4-approve':
        return this.orgService.stage4Approve(orgId, payload);
      case 'stage4-reject':
        return this.orgService.stage4Reject(orgId, rejectPayload);
      case 'stage5-approve':
        return this.orgService.stage5Approve(orgId, payload);
      case 'stage5-reject':
        return this.orgService.stage5Reject(orgId, rejectPayload);
      case 'allow-resubmission':
        return this.orgService.allowResubmission(orgId, payload);
      default:
        return null;
    }
  }

  private refreshQueueSummary(): void {
    this.kycStats.refresh().pipe(takeUntil(this.destroy$)).subscribe();
  }

  /**
   * KYC assignments store login {@code username}; email-only identity breaks backend approver checks.
   */
  private resolveReviewerIdentity(): { reviewerUsername: string; reviewerUserId?: number } {
    const user = this.storage.getUser();
    const username = user?.username?.trim() ?? '';
    const email = user?.email?.trim() ?? '';
    const reviewerUsername = username || email;
    const id = user?.id;
    const reviewerUserId = id != null && Number.isFinite(id) && id > 0 ? id : undefined;
    return { reviewerUsername, reviewerUserId };
  }

  private expectedStatusAfter(action: KycApplicationDecisionResult['action']): string {
    const match = /^stage(\d+)-(approve|reject)$/.exec(action);
    if (match) {
      const stage = Number(match[1]);
      const verb = match[2];
      if (verb === 'reject') {
        return 'REJECTED';
      }
      return stage >= 5 ? 'APPROVED' : `STAGE_${stage + 1}_REVIEW`;
    }
    switch (action) {
      case 'allow-resubmission':
        return 'DRAFT';
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

  queueStatRejected(): number {
    return this.queueSummary?.rejectedCount ?? 0;
  }
}
