import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { Title } from '@angular/platform-browser';

export type SettingsSection = 'group-roles' | 'kyc-approvers' | 'currency' | 'platform-billing';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class SettingsComponent {
  activeSection: SettingsSection = 'group-roles';

  readonly sections: Array<{ id: SettingsSection; label: string; icon: string; hint: string }> = [
    {
      id: 'group-roles',
      label: 'Group roles',
      icon: 'groups',
      hint: 'Map permission roles to user groups',
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
      id: 'platform-billing',
      label: 'Platform billing',
      icon: 'account_balance_wallet',
      hint: 'Action charges, packages, and wallet deposits',
    },
  ];

  constructor(
    private readonly title: Title,
    private readonly cdr: ChangeDetectorRef,
  ) {
    this.title.setTitle('Settings | LX Admin');
  }

  selectSection(section: SettingsSection): void {
    this.activeSection = section;
    this.cdr.markForCheck();
  }
}
