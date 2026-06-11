import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

export type SettingsSection = 'group-roles' | 'kyc-approvers' | 'currency' | 'procurement-approvers' | 'billing';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class SettingsComponent implements OnInit {
  activeSection: SettingsSection = 'group-roles';

  readonly sections: Array<{ id: SettingsSection; label: string; icon: string; hint: string }> = [
    {
      id: 'group-roles',
      label: 'Group roles',
      icon: 'groups',
      hint: 'Assign LDMS permission roles to each user group',
    },
    {
      id: 'kyc-approvers',
      label: 'KYC approvers',
      icon: 'verified_user',
      hint: 'Approval stages and per-company overrides',
    },
    {
      id: 'currency',
      label: 'Currency & rates',
      icon: 'currency_exchange',
      hint: 'Country base currencies and exchange rates',
    },
    {
      id: 'procurement-approvers',
      label: 'Procurement approvals',
      icon: 'approval',
      hint: 'Approval stages for requisitions — each stage may deduct wallet usage',
    },
    {
      id: 'billing',
      label: 'Platform billing',
      icon: 'account_balance_wallet',
      hint: 'Wallet balance, usage deductions, and top-ups',
    },
  ];

  constructor(
    private readonly title: Title,
    private readonly cdr: ChangeDetectorRef,
    private readonly route: ActivatedRoute,
  ) {
    this.title.setTitle('Settings | LX Platform');
  }

  ngOnInit(): void {
    this.applySectionFromRoute();
    this.route.queryParamMap.subscribe((params) => {
      const section = params.get('section');
      if (section === 'billing' || section === 'group-roles' || section === 'kyc-approvers'
          || section === 'currency' || section === 'procurement-approvers') {
        this.activeSection = section;
        this.cdr.markForCheck();
      }
    });
  }

  private applySectionFromRoute(): void {
    const section = this.route.snapshot.queryParamMap.get('section');
    if (section === 'billing' || section === 'group-roles' || section === 'kyc-approvers'
        || section === 'currency' || section === 'procurement-approvers') {
      this.activeSection = section;
    }
  }

  selectSection(section: SettingsSection): void {
    this.activeSection = section;
    this.cdr.markForCheck();
  }
}
