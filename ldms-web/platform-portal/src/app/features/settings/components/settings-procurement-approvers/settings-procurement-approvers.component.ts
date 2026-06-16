import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, catchError, finalize, forkJoin, of, takeUntil } from 'rxjs';
import { DEFAULT_TABLE_PAGE_SIZE } from '@shared/constants/table-pagination';
import { ProcurementApprovalPolicy } from '../../../inventory/models/inventory.model';
import { UserListRow, UsersPortalService } from '../../../users/services/users-portal.service';
import { ProcurementSettingsService } from '../../services/procurement-settings.service';

interface StageOption {
  value: number;
  label: string;
  hint: string;
}

const STAGE_HINTS: Record<number, string> = {
  1: 'One approval completes procurement',
  2: 'Two approvers in sequence',
  3: 'Three approvers in sequence',
};

@Component({
  selector: 'app-settings-procurement-approvers',
  templateUrl: './settings-procurement-approvers.component.html',
  styleUrl: './settings-procurement-approvers.component.scss',
  standalone: false,
})
export class SettingsProcurementApproversComponent implements OnInit, OnDestroy {
  loading = true;
  saving = false;
  error = '';

  policy: ProcurementApprovalPolicy = { defaultRequiredApprovalStages: 1, minAllowedStages: 1, maxAllowedStages: 3 };
  draftStages = 1;
  orgUsers: UserListRow[] = [];
  orgUsersLoading = false;
  orgUsersError = '';

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly procurementSettings: ProcurementSettingsService,
    private readonly users: UsersPortalService,
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

  toggleProcurementApprover(row: UserListRow, enabled: boolean): void {
    if (!row.id) {
      return;
    }
    row.procurementApprover = enabled;
    this.cdr.markForCheck();
    this.users
      .setProcurementApprover(row.id, enabled)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (resp) => {
          if (this.users.isUserMutationFailure(resp)) {
            row.procurementApprover = !enabled;
            this.cdr.markForCheck();
            this.snackBar.open(this.users.formatUserMutationError(resp, 'Could not update procurement approver.'), 'Close', {
              duration: 6000,
            });
            return;
          }
          row.procurementApproverEligibleLabel = enabled ? 'Eligible' : 'Not eligible';
          this.snackBar.open(
            enabled ? `${row.name} is now a procurement approver.` : `${row.name} is no longer a procurement approver.`,
            'Close',
            { duration: 3500, panelClass: ['app-snackbar-success'] },
          );
          this.cdr.markForCheck();
        },
        error: () => {
          row.procurementApprover = !enabled;
          this.cdr.markForCheck();
          this.snackBar.open('Could not update procurement approver.', 'Close', { duration: 6000 });
        },
      });
  }

  trackOrgUser(_index: number, row: UserListRow): number {
    return row.id;
  }

  savePolicy(): void {
    if (this.saving || this.draftStages === this.policy.defaultRequiredApprovalStages) {
      return;
    }
    this.saving = true;
    this.procurementSettings
      .updateApprovalPolicy(this.draftStages)
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
          this.draftStages = updated.defaultRequiredApprovalStages;
          this.snackBar.open('Procurement approval policy saved.', 'Close', {
            duration: 4000,
            panelClass: ['app-snackbar-success'],
          });
        },
        error: () => {
          this.snackBar.open('Could not save procurement approval policy.', 'Close', { duration: 6000 });
        },
      });
  }

  private reload(): void {
    this.loading = true;
    this.error = '';
    this.orgUsersLoading = true;
    this.orgUsersError = '';
    forkJoin({
      policy: this.procurementSettings.fetchApprovalPolicy().pipe(
        catchError(() => {
          this.error = 'Could not load procurement approval policy. Ensure ldms-inventory-management is running.';
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
            this.draftStages = policy.defaultRequiredApprovalStages;
          }
          this.orgUsers = users.rows;
        },
      });
  }
}
