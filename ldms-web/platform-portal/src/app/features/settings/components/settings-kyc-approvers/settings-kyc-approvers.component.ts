import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, catchError, finalize, forkJoin, of, takeUntil } from 'rxjs';
import { DEFAULT_TABLE_PAGE_SIZE } from '@shared/constants/table-pagination';
import { UsersPortalService } from '../../../users/services/users-portal.service';
import {
  SettingsAdminService,
  type KycApprovalPolicy,
  type KycSettingsOrganizationRow,
  type PagedKycSettingsOrganizations,
} from '../../services/settings-admin.service';

interface OrgStageRow {
  id: number;
  name: string;
  classificationLabel: string;
  overrideStages: number | null;
  effectiveStages: number;
  saving: boolean;
}

interface ApproverGroupRow {
  id: number;
  name: string;
  stages: number[];
}

interface StageOption {
  value: number;
  label: string;
  hint: string;
}

const STAGE_HINTS: Record<number, string> = {
  1: 'One review completes KYC',
  2: 'Two reviewers in sequence',
  3: 'Three reviewers in sequence',
  4: 'Four reviewers in sequence',
  5: 'Five reviewers in sequence',
};

const KYC_STAGE_ROLE_CODES = ['KYC_STAGE1', 'KYC_STAGE2', 'KYC_STAGE3', 'KYC_STAGE4', 'KYC_STAGE5'] as const;

@Component({
  selector: 'app-settings-kyc-approvers',
  templateUrl: './settings-kyc-approvers.component.html',
  styleUrl: './settings-kyc-approvers.component.scss',
  standalone: false,
})
export class SettingsKycApproversComponent implements OnInit, OnDestroy {
  loading = true;
  savingDefault = false;
  policy: KycApprovalPolicy = { defaultRequiredApprovalStages: 2, minAllowedStages: 1, maxAllowedStages: 5 };
  draftDefaultStages = 2;
  orgRows: OrgStageRow[] = [];
  approverGroups: ApproverGroupRow[] = [];
  orgSearch = '';
  error = '';

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly settings: SettingsAdminService,
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
    const max = this.policy.maxAllowedStages ?? 5;
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

  get orgStageOverrideOptions(): number[] {
    const min = this.policy.minAllowedStages ?? 1;
    const max = this.policy.maxAllowedStages ?? 5;
    return Array.from({ length: max - min + 1 }, (_, i) => min + i);
  }

  get filteredOrgRows(): OrgStageRow[] {
    const q = this.orgSearch.trim().toLowerCase();
    if (!q) {
      return this.orgRows;
    }
    return this.orgRows.filter(
      (row) => row.name.toLowerCase().includes(q) || row.classificationLabel.toLowerCase().includes(q),
    );
  }

  selectDefaultStages(stages: number): void {
    this.draftDefaultStages = stages;
    this.cdr.markForCheck();
  }

  saveDefaultPolicy(): void {
    if (this.savingDefault || this.draftDefaultStages === this.policy.defaultRequiredApprovalStages) {
      return;
    }
    this.savingDefault = true;
    this.settings
      .updateKycApprovalPolicy(this.draftDefaultStages)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.savingDefault = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (policy) => {
          this.policy = policy;
          this.draftDefaultStages = policy.defaultRequiredApprovalStages;
          this.reloadOrganizations();
          this.snackBar.open('Platform KYC approval policy saved.', 'Close', {
            duration: 4000,
            panelClass: ['app-snackbar-success'],
          });
        },
        error: () => {
          this.snackBar.open('Could not save platform KYC policy.', 'Close', { duration: 6000 });
        },
      });
  }

  onOrgStagesChange(row: OrgStageRow, value: string): void {
    const parsed = value === 'inherit' ? null : Number(value);
    const max = this.policy.maxAllowedStages ?? 5;
    const min = this.policy.minAllowedStages ?? 1;
    if (parsed !== null && (!Number.isFinite(parsed) || parsed < min || parsed > max)) {
      return;
    }
    row.saving = true;
    this.cdr.markForCheck();
    this.settings
      .updateOrganizationKycStages(row.id, parsed)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          row.saving = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          row.overrideStages = parsed;
          row.effectiveStages = parsed ?? this.policy.defaultRequiredApprovalStages;
          this.snackBar.open(`Updated approval stages for ${row.name}.`, 'Close', {
            duration: 3500,
            panelClass: ['app-snackbar-success'],
          });
        },
        error: () => {
          this.snackBar.open(`Could not update ${row.name}.`, 'Close', { duration: 6000 });
        },
      });
  }

  orgStageLabel(row: OrgStageRow): string {
    if (row.overrideStages != null) {
      return `${row.overrideStages} approver${row.overrideStages === 1 ? '' : 's'} (override)`;
    }
    return `${row.effectiveStages} approver${row.effectiveStages === 1 ? '' : 's'} (platform default)`;
  }

  trackOrg(_index: number, row: OrgStageRow): number {
    return row.id;
  }

  stageDots(count: number): number[] {
    return Array.from({ length: count }, (_, i) => i + 1);
  }

  private reload(): void {
    this.loading = true;
    this.error = '';
    forkJoin({
      policy: this.settings.fetchKycApprovalPolicy().pipe(
        catchError(() => {
          this.error = 'Could not load platform KYC policy. Ensure ldms-organization-management is running.';
          return of(null);
        }),
      ),
      orgs: this.settings
        .queryKycOrganizationsForSettings(0, DEFAULT_TABLE_PAGE_SIZE)
        .pipe(catchError(() => of({ rows: [], totalElements: 0 } satisfies PagedKycSettingsOrganizations))),
      groups: this.users
        .queryUserGroups({
          page: 0,
          size: 200,
          searchQuery: '',
          columnFilters: { name: '', description: '' },
        })
        .pipe(catchError(() => of({ rows: [], totalElements: 0 }))),
    })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: ({ policy, orgs, groups }) => {
          if (!policy) {
            return;
          }
          this.error = '';
          this.policy = policy;
          this.draftDefaultStages = policy.defaultRequiredApprovalStages;
          this.orgRows = orgs.rows.map((row: KycSettingsOrganizationRow) => this.toOrgStageRow(row, policy));
          this.approverGroups = this.buildApproverGroups(groups.rows);
        },
        error: () => {
          if (!this.error) {
            this.error = 'Could not load KYC approver settings.';
          }
        },
      });
  }

  private reloadOrganizations(): void {
    this.settings
      .queryKycOrganizationsForSettings(0, DEFAULT_TABLE_PAGE_SIZE)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (page: PagedKycSettingsOrganizations) => {
          this.orgRows = page.rows.map((row) => this.toOrgStageRow(row, this.policy));
          this.cdr.markForCheck();
        },
      });
  }

  private toOrgStageRow(row: KycSettingsOrganizationRow, policy: KycApprovalPolicy): OrgStageRow {
    return {
      id: row.id,
      name: row.applicant || row.name,
      classificationLabel: row.classificationLabel,
      overrideStages: row.kycRequiredApprovalStages ?? null,
      effectiveStages:
        row.effectiveKycRequiredApprovalStages ??
        row.kycRequiredApprovalStages ??
        policy.defaultRequiredApprovalStages,
      saving: false,
    };
  }

  private buildApproverGroups(groupRows: Record<string, unknown>[]): ApproverGroupRow[] {
    return groupRows
      .map((group) => {
        const assigned = this.extractRoleCodes(group);
        const stages = KYC_STAGE_ROLE_CODES.flatMap((code, index) =>
          assigned.has(code) ? [index + 1] : [],
        );
        return {
          id: Number(group['id'] ?? 0),
          name: String(group['name'] ?? '—'),
          stages,
        };
      })
      .filter((g) => g.stages.length > 0)
      .sort((a, b) => a.name.localeCompare(b.name));
  }

  private extractRoleCodes(group: Record<string, unknown>): Set<string> {
    const raw =
      (group['userRoleDtoSet'] ?? group['user_role_dto_set'] ?? group['userRoleDtoList'] ?? group['user_role_dto_list']) as unknown;
    const codes = new Set<string>();
    if (!Array.isArray(raw)) {
      return codes;
    }
    for (const item of raw) {
      if (item && typeof item === 'object' && !Array.isArray(item)) {
        const role = String((item as Record<string, unknown>)['role'] ?? '').toUpperCase();
        if (role) {
          codes.add(role);
        }
      }
    }
    return codes;
  }
}
