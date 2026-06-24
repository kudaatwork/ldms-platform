import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { Subject, takeUntil } from 'rxjs';

export type SettingsSection = 'group-roles' | 'kyc-approvers' | 'currency' | 'platform-billing' | 'organization-documents';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class SettingsComponent implements OnInit, OnDestroy {
  activeSection: SettingsSection = 'group-roles';

  private readonly destroy$ = new Subject<void>();

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
      hint: 'Action charges, packages, wallet deposits, and approval history',
    },
    {
      id: 'organization-documents',
      label: 'Documents',
      icon: 'folder_open',
      hint: 'View organisation verification documents read-only',
    },
  ];

  constructor(
    private readonly title: Title,
    private readonly cdr: ChangeDetectorRef,
    private readonly route: ActivatedRoute,
  ) {
    this.title.setTitle('Settings | LX Admin');
  }

  ngOnInit(): void {
    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((params: ParamMap) => {
      const section = params.get('section');
      if (this.isSettingsSection(section)) {
        this.activeSection = section;
        this.cdr.markForCheck();
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private isSettingsSection(value: string | null): value is SettingsSection {
    return value === 'group-roles'
      || value === 'kyc-approvers'
      || value === 'currency'
      || value === 'platform-billing'
      || value === 'organization-documents';
  }

  selectSection(section: SettingsSection): void {
    this.activeSection = section;
    this.cdr.markForCheck();
  }
}
