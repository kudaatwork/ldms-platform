import { Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import {
  OnboardingStatus,
  OrganizationService,
} from '../../../../core/services/organization.service';
import { ThemeService } from '../../../../core/services/theme.service';
import { Subject, interval, startWith, switchMap, takeUntil } from 'rxjs';

export const STORAGE_ORG_ID = 'lx_platform_registered_org_id';
export const STORAGE_ORG_NAME = 'lx_platform_registered_org_name';
export const STORAGE_ORG_KYC = 'lx_platform_registered_org_kyc';
export const STORAGE_ORG_CLASSIFICATION = 'lx_platform_registered_org_classification';

export type StepState = 'done' | 'active' | 'pending' | 'rejected';

export interface OnboardingStep {
  id: string;
  label: string;
  hint: string;
  state: StepState;
  icon: string;
}

@Component({
  selector: 'app-onboarding-status',
  templateUrl: './onboarding-status.component.html',
  styleUrl: './onboarding-status.component.scss',
  standalone: false,
})
export class OnboardingStatusComponent implements OnInit, OnDestroy {
  loading = true;
  loadError = '';
  orgId = 0;
  status: OnboardingStatus | null = null;
  lastRefreshed = new Date();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly orgService: OrganizationService,
    private readonly title: Title,
    readonly theme: ThemeService,
  ) {
    this.title.setTitle('Onboarding progress | LX Platform');
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.queryParamMap.get('orgId');
    const storedId = sessionStorage.getItem(STORAGE_ORG_ID);
    const id = Number(idParam ?? storedId ?? 0);
    if (!Number.isFinite(id) || id < 1) {
      this.loading = false;
      this.loadError = 'No organisation reference found. Complete signup first.';
      return;
    }
    this.orgId = id;
    sessionStorage.setItem(STORAGE_ORG_ID, String(id));

    interval(12_000)
      .pipe(
        startWith(0),
        switchMap(() => this.orgService.getOnboardingStatus(this.orgId)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (s) => this.applyStatus(s),
        error: (err: Error) => {
          this.loading = false;
          this.loadError = err.message ?? 'Could not load onboarding status.';
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get steps(): OnboardingStep[] {
    const s = this.status;
    if (!s) {
      return [];
    }
    const status = (s.kycStatus ?? 'DRAFT').toUpperCase();
    const verified = s.verified;
    const totalStages = Math.max(1, s.requiredApprovalStages ?? 2);
    const rejected = status === 'REJECTED';
    const submitted = [
      'SUBMITTED',
      'STAGE_1_REVIEW',
      'STAGE_2_REVIEW',
      'STAGE_3_REVIEW',
      'STAGE_4_REVIEW',
      'STAGE_5_REVIEW',
      'APPROVED',
      'REJECTED',
      'RESUBMITTED',
    ].includes(status);

    const currentReviewStage = this.reviewStageFromStatus(status);
    const stageDone = (n: number): boolean => {
      if (verified || status === 'APPROVED') {
        return true;
      }
      if (rejected) {
        return currentReviewStage > n;
      }
      return currentReviewStage > n;
    };
    const stageActive = (n: number): boolean => {
      if (rejected) {
        return false;
      }
      if (verified || status === 'APPROVED') {
        return false;
      }
      return currentReviewStage === n;
    };

    const pick = (done: boolean, active: boolean, isRejected = false): StepState => {
      if (isRejected) {
        return 'rejected';
      }
      if (done) {
        return 'done';
      }
      if (active) {
        return 'active';
      }
      return 'pending';
    };

    const rows: OnboardingStep[] = [
      {
        id: 'registration',
        label: 'Registration received',
        hint: 'Your organisation profile is on file.',
        state: 'done',
        icon: 'how_to_reg',
      },
      {
        id: 'submitted',
        label: 'Application in review',
        hint: submitted ? 'Our team is reviewing your submission.' : 'Submit KYC when your profile is ready.',
        state: pick(submitted, !submitted && (status === 'DRAFT' || status === 'RESUBMITTED'), false),
        icon: 'assignment_turned_in',
      },
    ];

    for (let n = 1; n <= totalStages; n++) {
      const done = stageDone(n);
      const active = stageActive(n);
      rows.push({
        id: `stage-${n}`,
        label: totalStages === 1 ? 'Compliance approval' : `Approval stage ${n}`,
        hint: done
          ? 'This review stage is complete.'
          : active
            ? 'An assigned reviewer is evaluating this stage now.'
            : 'Waiting for prior stages to complete.',
        state: pick(done, active, rejected && active),
        icon: 'verified_user',
      });
    }

    rows.push(
      {
        id: 'verified',
        label: 'Organisation verified',
        hint: verified ? 'All approval stages are complete.' : 'Issued after final KYC approval.',
        state: pick(verified, status === 'APPROVED' && !verified, false),
        icon: 'workspace_premium',
      },
      {
        id: 'access',
        label: 'Platform access',
        hint: verified ? 'Sign in with your contact person account.' : 'Enabled after verification and email confirmation.',
        state: pick(verified, false, false),
        icon: 'rocket_launch',
      },
    );

    return rows;
  }

  get progressPercent(): number {
    const list = this.steps;
    if (!list.length) {
      return 0;
    }
    const done = list.filter((s) => s.state === 'done').length;
    const active = list.some((s) => s.state === 'active') ? 0.5 : 0;
    return Math.round(((done + active) / list.length) * 100);
  }

  get statusHeadline(): string {
    const status = (this.status?.kycStatus ?? 'DRAFT').toUpperCase();
    if (this.status?.verified) {
      return 'You are verified';
    }
    if (status === 'REJECTED') {
      return 'Application needs attention';
    }
    if (status === 'DRAFT' || status === 'RESUBMITTED') {
      return 'Complete your application';
    }
    if (status === 'APPROVED') {
      return 'Almost there';
    }
    return 'Review in progress';
  }

  get statusBadgeClass(): string {
    const status = (this.status?.kycStatus ?? 'DRAFT').toUpperCase();
    if (this.status?.verified) {
      return 'ob-badge--success';
    }
    if (status === 'REJECTED') {
      return 'ob-badge--danger';
    }
    if (status === 'DRAFT' || status === 'RESUBMITTED') {
      return 'ob-badge--muted';
    }
    return 'ob-badge--progress';
  }

  get kycStatusLabel(): string {
    const raw = (this.status?.kycStatus ?? 'DRAFT').toUpperCase();
    const map: Record<string, string> = {
      DRAFT: 'Draft',
      SUBMITTED: 'Submitted',
      STAGE_1_REVIEW: 'Stage 1 review',
      STAGE_2_REVIEW: 'Stage 2 review',
      STAGE_3_REVIEW: 'Stage 3 review',
      STAGE_4_REVIEW: 'Stage 4 review',
      STAGE_5_REVIEW: 'Stage 5 review',
      APPROVED: 'Approved',
      REJECTED: 'Rejected',
      RESUBMITTED: 'Resubmission open',
    };
    return map[raw] ?? raw;
  }

  refreshNow(): void {
    this.loading = true;
    this.orgService.getOnboardingStatus(this.orgId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (s) => this.applyStatus(s),
      error: (err: Error) => {
        this.loading = false;
        this.loadError = err.message ?? 'Refresh failed.';
      },
    });
  }

  goSignup(): void {
    void this.router.navigate(['/signup']);
  }

  goLogin(): void {
    void this.router.navigate(['/auth/login'], { queryParams: { registered: '1' } });
  }

  goWelcome(): void {
    void this.router.navigate(['/welcome']);
  }

  toggleTheme(): void {
    this.theme.toggle();
  }

  private applyStatus(s: OnboardingStatus): void {
    this.status = s;
    this.loading = false;
    this.loadError = '';
    this.lastRefreshed = new Date();
    sessionStorage.setItem(STORAGE_ORG_NAME, s.name);
    sessionStorage.setItem(STORAGE_ORG_KYC, s.kycStatus);
    const existing = sessionStorage.getItem(STORAGE_ORG_CLASSIFICATION);
    if (!existing) {
      sessionStorage.setItem(STORAGE_ORG_CLASSIFICATION, 'SUPPLIER');
    }
  }

  private reviewStageFromStatus(status: string): number {
    if (status === 'SUBMITTED' || status === 'RESUBMITTED') {
      return 1;
    }
    const m = /^STAGE_(\d)_REVIEW$/.exec(status);
    if (m) {
      return Number(m[1]);
    }
    if (status === 'APPROVED') {
      return 99;
    }
    return 0;
  }
}
