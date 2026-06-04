import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { Title } from '@angular/platform-browser';

export type SettingsSection = 'group-roles' | 'kyc-approvers';

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
      hint: 'Assign LDMS permission roles to each user group',
    },
    {
      id: 'kyc-approvers',
      label: 'KYC approvers',
      icon: 'verified_user',
      hint: 'Approval stages and per-company overrides',
    },
  ];

  constructor(
    private readonly title: Title,
    private readonly cdr: ChangeDetectorRef,
  ) {
    this.title.setTitle('Settings | LX Platform');
  }

  selectSection(section: SettingsSection): void {
    this.activeSection = section;
    this.cdr.markForCheck();
  }
}
