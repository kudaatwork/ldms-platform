import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { OrganizationSummary } from '../../../../core/services/organization.service';
import { ThemeService } from '../../../../core/services/theme.service';

export const STORAGE_ORG_ID = 'lx_platform_registered_org_id';
export const STORAGE_ORG_NAME = 'lx_platform_registered_org_name';
export const STORAGE_ORG_KYC = 'lx_platform_registered_org_kyc';

@Component({
  selector: 'app-onboarding-status',
  templateUrl: './onboarding-status.component.html',
  styleUrl: './onboarding-status.component.scss',
  standalone: false,
})
export class OnboardingStatusComponent implements OnInit {
  loading = false;
  errorMessage = '';
  org: OrganizationSummary | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly title: Title,
    readonly theme: ThemeService,
  ) {
    this.title.setTitle('Onboarding status | LX Platform');
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.queryParamMap.get('orgId');
    const storedId = sessionStorage.getItem(STORAGE_ORG_ID);
    const id = Number(idParam ?? storedId ?? 0);
    if (!Number.isFinite(id) || id < 1) {
      this.errorMessage = 'No organisation reference found. Complete signup first.';
      return;
    }
    if (idParam) {
      sessionStorage.setItem(STORAGE_ORG_ID, String(id));
    }
    this.org = {
      id,
      name: sessionStorage.getItem(STORAGE_ORG_NAME) ?? 'Your organisation',
      email: '',
      kycStatus: sessionStorage.getItem(STORAGE_ORG_KYC) ?? 'DRAFT',
      isVerified: false,
      organizationClassification: 'SUPPLIER',
    };
  }

  get steps(): { label: string; state: 'done' | 'active' | 'pending' }[] {
    const status = (this.org?.kycStatus ?? 'DRAFT').toUpperCase();
    const verified = Boolean(this.org?.isVerified);
    const s = (done: boolean, active: boolean): 'done' | 'active' | 'pending' => {
      if (done) return 'done';
      if (active) return 'active';
      return 'pending';
    };
    const submitted = ['SUBMITTED', 'STAGE_1_REVIEW', 'STAGE_2_REVIEW', 'APPROVED', 'REJECTED', 'RESUBMITTED'].includes(status);
    const stage1Done = ['STAGE_2_REVIEW', 'APPROVED'].includes(status);
    const stage2Done = verified || status === 'APPROVED';
    return [
      { label: 'Registration', state: 'done' },
      { label: 'KYC submitted', state: s(submitted, status === 'DRAFT' || status === 'RESUBMITTED') },
      { label: 'Stage 1 approval', state: s(stage1Done, status === 'SUBMITTED' || status === 'STAGE_1_REVIEW' || status === 'RESUBMITTED') },
      { label: 'Stage 2 approval', state: s(stage2Done, status === 'STAGE_2_REVIEW') },
      { label: 'Email verification', state: s(verified, stage2Done && !verified) },
      { label: 'Platform access', state: s(verified, false) },
    ];
  }

  goSignup(): void {
    void this.router.navigate(['/signup']);
  }

  goLogin(): void {
    void this.router.navigate(['/auth/login']);
  }

  goWelcome(): void {
    void this.router.navigate(['/welcome']);
  }
}
