import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import {
  canEditOrganizationProfile,
  canManageBillingSettings,
  canManageGroupRoles,
  canManageKycApproverSettings,
  canManageOperationalSettings,
  canManageProcurementSettings,
} from '../../utils/org-settings-permissions.util';

export type SettingsSection =
  | 'organization'
  | 'group-roles'
  | 'kyc-approvers'
  | 'currency'
  | 'procurement-approvers'
  | 'billing'
  | 'operational-mode';

interface SettingsSectionDef {
  id: SettingsSection;
  label: string;
  icon: string;
  hint: string;
}

type SettingsSectionCandidate = SettingsSectionDef & { visible: boolean };

const ALL_SECTION_IDS: SettingsSection[] = [
  'organization',
  'group-roles',
  'kyc-approvers',
  'currency',
  'procurement-approvers',
  'billing',
  'operational-mode',
];

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class SettingsComponent implements OnInit, OnDestroy {
  activeSection: SettingsSection = 'organization';
  sections: SettingsSectionDef[] = [];

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly title: Title,
    private readonly cdr: ChangeDetectorRef,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly authState: AuthStateService,
  ) {
    this.title.setTitle('Settings | LX Platform');
  }

  ngOnInit(): void {
    this.refreshSections();
    this.syncActiveSection();
    this.authState.currentUser$.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.refreshSections();
      this.syncActiveSection();
      this.cdr.markForCheck();
    });
    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const section = params.get('section');
      if (section && this.isValidSection(section) && this.sections.some((s) => s.id === section)) {
        this.activeSection = section as SettingsSection;
      } else {
        this.syncActiveSection();
      }
      this.cdr.markForCheck();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  selectSection(section: SettingsSection): void {
    this.activeSection = section;
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { section },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
    this.cdr.markForCheck();
  }

  /** Ensures the active tab always maps to a visible section (avoids empty panel). */
  get panelSection(): SettingsSection | null {
    if (!this.sections.length) {
      return null;
    }
    return this.sections.some((s) => s.id === this.activeSection)
      ? this.activeSection
      : this.sections[0].id;
  }

  private syncActiveSection(): void {
    const section = this.route.snapshot.queryParamMap.get('section');
    if (section && this.isValidSection(section) && this.sections.some((s) => s.id === section)) {
      this.activeSection = section as SettingsSection;
      return;
    }
    if (this.sections.length) {
      this.activeSection = this.sections[0].id;
    }
  }

  private refreshSections(): void {
    const roles = this.authState.currentUser?.roles ?? [];
    const classification = this.authState.currentUser?.orgClassification;
    const candidates: SettingsSectionCandidate[] = [
      {
        id: 'organization',
        label: 'Organisation',
        icon: 'business',
        hint: 'Company profile, email, address, and stock options',
        visible: canEditOrganizationProfile(roles),
      },
      {
        id: 'operational-mode',
        label: 'Operational mode',
        icon: 'tune',
        hint: 'Platform full, standalone logistics, or cross-docking',
        visible: canManageOperationalSettings(roles),
      },
      {
        id: 'group-roles',
        label: 'Group roles',
        icon: 'groups',
        hint: 'Assign LDMS permission roles to each user group',
        visible: canManageGroupRoles(roles),
      },
      {
        id: 'kyc-approvers',
        label: 'KYC approvers',
        icon: 'verified_user',
        hint: 'Approval stages and per-company overrides',
        visible: canManageKycApproverSettings(classification) && canManageGroupRoles(roles),
      },
      {
        id: 'currency',
        label: 'Currency & rates',
        icon: 'currency_exchange',
        hint: 'Country base currencies and exchange rates',
        visible: canManageGroupRoles(roles),
      },
      {
        id: 'procurement-approvers',
        label: 'Procurement approvals',
        icon: 'approval',
        hint: 'Approval stages for requisitions',
        visible: canManageProcurementSettings(roles) && classification === 'SUPPLIER',
      },
      {
        id: 'billing',
        label: 'Platform billing',
        icon: 'account_balance_wallet',
        hint: 'Wallet balance, usage deductions, and top-ups',
        visible: canManageBillingSettings(classification) && canEditOrganizationProfile(roles),
      },
    ];
    this.sections = candidates
      .filter((section) => section.visible)
      .map(({ id, label, icon, hint }) => ({ id, label, icon, hint }));
  }

  private isValidSection(section: string): section is SettingsSection {
    return (ALL_SECTION_IDS as string[]).includes(section);
  }
}
