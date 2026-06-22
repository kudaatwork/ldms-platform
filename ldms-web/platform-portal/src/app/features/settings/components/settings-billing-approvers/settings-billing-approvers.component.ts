import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, catchError, finalize, forkJoin, of, takeUntil } from 'rxjs';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { DEFAULT_TABLE_PAGE_SIZE } from '@shared/constants/table-pagination';
import { UserListRow, UsersPortalService } from '../../../users/services/users-portal.service';
import { canManageBillingApproverSettings } from '../../utils/org-settings-permissions.util';
import {
  BillingSettingsService,
  type BillingVerificationPolicy,
  type ProcurementPaymentRow,
} from '../../services/billing-settings.service';

interface StageOption {
  value: number;
  label: string;
  hint: string;
}

const STAGE_HINTS: Record<number, string> = {
  1: 'One billing approver verifies payment',
  2: 'Two billing approvers in sequence',
  3: 'Three billing approvers in sequence',
};

@Component({
  selector: 'app-settings-billing-approvers',
  templateUrl: './settings-billing-approvers.component.html',
  styleUrls: [
    './settings-billing-approvers.component.scss',
    '../settings-procurement-approvers/settings-procurement-approvers.component.scss',
  ],
  standalone: false,
})
export class SettingsBillingApproversComponent implements OnInit, OnDestroy {
  loading = true;
  saving = false;
  error = '';

  policy: BillingVerificationPolicy = {
    defaultRequiredVerificationStages: 1,
    minAllowedStages: 1,
    maxAllowedStages: 3,
  };
  draftStages = 1;

  orgUsers: UserListRow[] = [];
  orgUsersLoading = false;

  pendingPayments: ProcurementPaymentRow[] = [];
  pendingPaymentsLoading = false;
  pendingPaymentsError = '';
  paymentVerifyBusyId: number | null = null;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly users: UsersPortalService,
    private readonly billingSettings: BillingSettingsService,
    private readonly authState: AuthStateService,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.reload();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get isBillingApprover(): boolean {
    return this.authState.currentUser?.billingApprover === true;
  }

  get canManageApprovers(): boolean {
    const roles = this.authState.currentUser?.roles ?? [];
    return canManageBillingApproverSettings(roles);
  }

  get stageOptions(): StageOption[] {
    const min = this.policy.minAllowedStages ?? 1;
    const max = Math.min(this.policy.maxAllowedStages ?? 3, 3);
    const options: StageOption[] = [];
    for (let value = min; value <= max; value++) {
      options.push({
        value,
        label: value === 1 ? 'Single approver' : `${value} approvers`,
        hint: STAGE_HINTS[value] ?? `${value} sequential reviewers`,
      });
    }
    return options;
  }

  stageDots(count: number): number[] {
    return Array.from({ length: count }, (_, i) => i + 1);
  }

  selectStages(stages: number): void {
    this.draftStages = stages;
    this.cdr.markForCheck();
  }

  savePolicy(): void {
    if (this.saving || this.draftStages === this.policy.defaultRequiredVerificationStages) {
      return;
    }
    this.saving = true;
    this.billingSettings
      .updateVerificationPolicy(this.draftStages)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.saving = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (updated) => {
          this.policy = updated;
          this.draftStages = updated.defaultRequiredVerificationStages;
          this.snackBar.open('Billing verification policy saved.', 'Close', {
            duration: 4000,
            panelClass: ['app-snackbar-success'],
          });
        },
        error: (err: Error) => {
          this.snackBar.open(err.message || 'Could not save billing verification policy.', 'Close', {
            duration: 6000,
          });
        },
      });
  }

  toggleBillingApprover(row: UserListRow, enabled: boolean): void {
    if (!row.id) {
      return;
    }
    row.billingApprover = enabled;
    this.cdr.markForCheck();
    this.users
      .setBillingApprover(row.id, enabled)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (resp) => {
          if (this.users.isUserMutationFailure(resp)) {
            row.billingApprover = !enabled;
            this.cdr.markForCheck();
            this.snackBar.open(this.users.formatUserMutationError(resp, 'Could not update billing approver.'), 'Close', {
              duration: 6000,
            });
            return;
          }
          row.billingApproverEligibleLabel = enabled ? 'Eligible' : 'Not eligible';
          this.snackBar.open(
            enabled ? `${row.name} is now a billing approver.` : `${row.name} is no longer a billing approver.`,
            'Close',
            { duration: 3500, panelClass: ['app-snackbar-success'] },
          );
          this.cdr.markForCheck();
        },
        error: (e: Error) => {
          row.billingApprover = !enabled;
          this.cdr.markForCheck();
          this.snackBar.open(e.message || 'Could not update billing approver.', 'Close', { duration: 6000 });
        },
      });
  }

  trackOrgUser(_index: number, row: UserListRow): number {
    return row.id;
  }

  paymentStageLabel(row: ProcurementPaymentRow): string {
    const required = row.requiredVerificationStages ?? 1;
    const current = row.currentVerificationStage ?? 0;
    if (required <= 1) {
      return 'Verify payment';
    }
    return `Verify stage ${Math.min(current + 1, required)} of ${required}`;
  }

  loadPendingPayments(showLoading = true): void {
    if (!this.isBillingApprover) {
      this.pendingPayments = [];
      return;
    }
    if (showLoading) {
      this.pendingPaymentsLoading = true;
      this.pendingPaymentsError = '';
    }
    this.billingSettings
      .listPendingProcurementPayments()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.pendingPaymentsLoading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (rows) => {
          this.pendingPayments = rows;
        },
        error: (err: unknown) => {
          this.pendingPaymentsError = err instanceof Error ? err.message : 'Could not load pending payments.';
        },
      });
  }

  verifyPayment(row: ProcurementPaymentRow): void {
    if (!row.id || this.paymentVerifyBusyId != null) {
      return;
    }
    this.paymentVerifyBusyId = row.id;
    this.billingSettings
      .verifyPayment(row.id)
      .pipe(
        finalize(() => {
          this.paymentVerifyBusyId = null;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (message) => {
          this.snackBar.open(message, 'Close', {
            duration: 4500,
            panelClass: ['app-snackbar-success'],
          });
          this.loadPendingPayments(false);
        },
        error: (err: unknown) => {
          const message = err instanceof Error ? err.message : 'Could not verify payment.';
          this.snackBar.open(message, 'Close', { duration: 6000 });
        },
      });
  }

  private reload(): void {
    this.loading = true;
    this.error = '';
    this.orgUsersLoading = true;
    forkJoin({
      policy: this.billingSettings.fetchVerificationPolicy().pipe(
        catchError(() => {
          this.error = 'Could not load billing verification policy. Ensure ldms-billing-payments is running.';
          return of(null);
        }),
      ),
      users: this.users
        .queryUsers({
          page: 0,
          size: DEFAULT_TABLE_PAGE_SIZE,
          searchQuery: '',
          columnFilters: {
            email: '',
            firstName: '',
            lastName: '',
            username: '',
            phoneNumber: '',
            nationalIdNumber: '',
            passportNumber: '',
            statusLabel: '',
          },
        })
        .pipe(catchError(() => of({ rows: [] as UserListRow[], totalElements: 0 }))),
    })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
          this.orgUsersLoading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: ({ policy, users }) => {
          if (policy) {
            this.error = '';
            this.policy = policy;
            this.draftStages = policy.defaultRequiredVerificationStages;
          }
          this.orgUsers = users.rows;
          this.loadPendingPayments();
        },
      });
  }
}
