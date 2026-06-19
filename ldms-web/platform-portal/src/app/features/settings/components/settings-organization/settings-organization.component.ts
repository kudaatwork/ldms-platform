import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { Subject, catchError, finalize, of, takeUntil } from 'rxjs';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import {
  OrganizationService,
  type OrganizationSummary,
  type UpdateMyOrganizationPayload,
} from '../../../../core/services/organization.service';
import type { AddressHierarchySeed } from '../../../users/components/user-address-cascade-fields/user-address-cascade-fields.component';
import {
  canEditOrganizationProfile,
  canManageBranches,
  canManageOperationalSettings,
} from '../../utils/org-settings-permissions.util';

@Component({
  selector: 'app-settings-organization',
  templateUrl: './settings-organization.component.html',
  styleUrl: './settings-organization.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class SettingsOrganizationComponent implements OnInit, OnDestroy {
  loading = true;
  saving = false;
  error = '';
  org: OrganizationSummary | null = null;

  profileForm: FormGroup;
  addressLine1 = '';
  addressLine2 = '';
  postalCode = '';
  suburbIdStr = '';
  cityIdStr = '';
  addressSeed: AddressHierarchySeed | null = null;
  seedSuburbId: number | null = null;
  addressError = '';

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly orgService: OrganizationService,
    private readonly authState: AuthStateService,
    private readonly snackBar: MatSnackBar,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
  ) {
    this.profileForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phoneNumber: ['', Validators.required],
      websiteUrl: [''],
      organizationDescription: [''],
      businessHours: [''],
      regionsServed: [''],
      numberOfEmployees: [null as number | null],
      annualRevenueEstimate: [null as number | null],
    });
  }

  ngOnInit(): void {
    if (!this.canEditProfile) {
      this.loading = false;
      this.error = 'You do not have permission to edit organisation settings.';
      this.cdr.markForCheck();
      return;
    }
    this.loadOrganization();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get roles(): string[] {
    return this.authState.currentUser?.roles ?? [];
  }

  get canEditProfile(): boolean {
    return canEditOrganizationProfile(this.roles);
  }

  get canEditBranches(): boolean {
    return canManageBranches(this.roles);
  }

  get canEditOperationalMode(): boolean {
    return canManageOperationalSettings(this.roles);
  }

  get identityLocked(): boolean {
    if (!this.org) {
      return false;
    }
    const ks = (this.org.kycStatus ?? '').toUpperCase();
    if (this.org.createdViaSignup === false) {
      return false;
    }
    return ks !== 'DRAFT' && ks !== 'RESUBMITTED';
  }

  get verificationHint(): string {
    if (!this.org?.isVerified) {
      return 'Organisation email is not verified yet. Open the link sent to your organisation inbox.';
    }
    return '';
  }

  get formattedAddress(): string {
    if (!this.org) {
      return '';
    }
    const parts = [
      this.org.addressLine1,
      this.org.addressLine2,
      this.org.addressCityName,
      this.org.addressDistrictName,
      this.org.addressProvinceName,
      this.org.addressPostalCode,
    ].filter((p) => !!p && String(p).trim());
    return parts.join(', ');
  }

  get inventoryModeLabel(): string {
    if (!this.org) {
      return '—';
    }
    if (this.org.crossDockingEnabled && !this.org.inventoryManagementEnabled) {
      return 'Cross-docking';
    }
    if (this.org.standaloneMode) {
      return 'Standalone logistics';
    }
    return 'Platform full';
  }

  get stockSourceLabel(): string {
    const source = this.org?.inventoryDataSource ?? 'INTERNAL';
    if (source === 'EXTERNAL_API') {
      return 'External API integration';
    }
    if (source === 'MANUAL_ACK') {
      return 'Manual acknowledgement';
    }
    return 'Internal LDMS inventory';
  }

  loadOrganization(): void {
    this.loading = true;
    this.error = '';
    this.orgService
      .getMy()
      .pipe(
        catchError((err: Error) => {
          this.error = err.message ?? 'Could not load organisation profile.';
          return of(null);
        }),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe((org) => {
        if (!org) {
          return;
        }
        this.org = org;
        this.applyOrgToForm(org);
        this.cdr.markForCheck();
      });
  }

  private applyOrgToForm(org: OrganizationSummary): void {
    this.profileForm.patchValue({
      name: org.name,
      email: org.email,
      phoneNumber: org.phoneNumber ?? '',
      websiteUrl: org.websiteUrl ?? '',
      organizationDescription: org.organizationDescription ?? '',
      businessHours: org.businessHours ?? '',
      regionsServed: org.regionsServed ?? '',
      numberOfEmployees: org.numberOfEmployees ?? null,
      annualRevenueEstimate: org.annualRevenueEstimate ?? null,
    });
    if (this.identityLocked) {
      this.profileForm.get('name')?.disable({ emitEvent: false });
      this.profileForm.get('email')?.disable({ emitEvent: false });
    } else {
      this.profileForm.get('name')?.enable({ emitEvent: false });
      this.profileForm.get('email')?.enable({ emitEvent: false });
    }
    this.addressLine1 = org.addressLine1 ?? '';
    this.addressLine2 = org.addressLine2 ?? '';
    this.postalCode = org.addressPostalCode ?? '';
    this.suburbIdStr = org.addressSuburbId ? String(org.addressSuburbId) : '';
    this.cityIdStr = org.addressCityId ? String(org.addressCityId) : '';
    this.seedSuburbId = org.addressSuburbId ?? null;
    this.addressSeed =
      org.addressSuburbId && org.addressSuburbId > 0
        ? {
            suburbId: org.addressSuburbId,
            cityId: org.addressCityId,
            cityName: org.addressCityName,
            addressLine2: org.addressLine2,
          }
        : null;
  }

  saveProfile(): void {
    if (!this.canEditProfile || this.saving) {
      return;
    }
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }
    const raw = this.profileForm.getRawValue();
    const suburbId = Number(this.suburbIdStr);
    const cityId = Number(this.cityIdStr);
    const hasAddressInput =
      !!this.addressLine1.trim() ||
      !!this.addressLine2.trim() ||
      !!this.postalCode.trim() ||
      (Number.isFinite(suburbId) && suburbId > 0);

    if (hasAddressInput) {
      if (!this.addressLine1.trim() || !this.postalCode.trim() || !Number.isFinite(suburbId) || suburbId < 1) {
        this.addressError = 'Address requires line 1, postal code, and suburb.';
        this.cdr.markForCheck();
        return;
      }
    }
    this.addressError = '';

    const payload: UpdateMyOrganizationPayload = {
      name: String(raw.name ?? '').trim() || undefined,
      email: String(raw.email ?? '').trim() || undefined,
      phoneNumber: String(raw.phoneNumber ?? '').trim() || undefined,
      websiteUrl: String(raw.websiteUrl ?? '').trim() || undefined,
      organizationDescription: String(raw.organizationDescription ?? '').trim() || undefined,
      businessHours: String(raw.businessHours ?? '').trim() || undefined,
      regionsServed: String(raw.regionsServed ?? '').trim() || undefined,
      numberOfEmployees:
        raw.numberOfEmployees != null && Number(raw.numberOfEmployees) > 0
          ? Number(raw.numberOfEmployees)
          : undefined,
      annualRevenueEstimate:
        raw.annualRevenueEstimate != null && Number(raw.annualRevenueEstimate) >= 0
          ? Number(raw.annualRevenueEstimate)
          : undefined,
    };
    if (hasAddressInput) {
      payload.addressLine1 = this.addressLine1.trim();
      payload.addressLine2 = this.addressLine2.trim() || undefined;
      payload.postalCode = this.postalCode.trim();
      payload.suburbId = suburbId;
      if (Number.isFinite(cityId) && cityId > 0) {
        payload.cityId = cityId;
      }
    } else if (this.org?.locationId) {
      payload.locationId = this.org.locationId;
    }

    this.saving = true;
    this.error = '';
    this.orgService
      .updateMyOrganization(payload)
      .pipe(
        catchError((err: Error) => {
          this.error = err.message ?? 'Could not save organisation settings.';
          return of(null);
        }),
        finalize(() => {
          this.saving = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe((updated) => {
        if (!updated) {
          return;
        }
        this.org = updated;
        this.applyOrgToForm(updated);
        const user = this.authState.currentUser;
        if (user) {
          this.authState.setCurrentUser({
            ...user,
            orgName: updated.name,
          });
        }
        this.snackBar.open('Organisation settings saved.', 'Dismiss', { duration: 3500 });
      });
  }

  openOperationalSettings(): void {
    void this.router.navigate(['/settings'], { queryParams: { section: 'operational-mode' } });
  }

  openBranches(): void {
    void this.router.navigate(['/organization/branches']);
  }
}
