import { CommonModule } from '@angular/common';
import { Component, Inject, OnDestroy, OnInit, Optional } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatRadioModule } from '@angular/material/radio';
import { forkJoin, Observable, of, Subject, finalize, switchMap, takeUntil, throwError } from 'rxjs';
import { OrgContextService } from '../../../../core/services/org-context.service';
import { LocationsService } from '../../../locations/services/locations.service';
import {
  AddressHierarchySeed,
  UserAddressCascadeFieldsComponent,
} from '../../../users/components/user-address-cascade-fields/user-address-cascade-fields.component';
import {
  UserListRow,
  UserProfileBundle,
  UsersPortalService,
} from '../../../users/services/users-portal.service';
import type {
  CreateFleetDriverPayload,
  DriverEmploymentType,
  EditFleetDriverPayload,
  FleetDriverRow,
} from '../../models/fleet.model';
import { FleetPortalService } from '../../services/fleet-portal.service';

export type FleetDriverDialogData = {
  driver?: FleetDriverRow;
  /** When true, scroll focus to the legacy platform-login panel (edit mode). */
  focusPlatformAccess?: boolean;
};

type DriverSource = 'org_user' | 'manual';

interface DriverDocSlot {
  file: File | null;
  fileName: string;
  uploadId: number | null;
  profileUploadId: number | null;
  uploading: boolean;
}

const DRIVER_SOURCES: { value: DriverSource; label: string; hint: string }[] = [
  {
    value: 'org_user',
    label: 'Organisation user',
    hint: 'Recommended — pull name, ID, passport, and address from user management.',
  },
  {
    value: 'manual',
    label: 'Enter manually',
    hint: 'For drivers not yet on the platform user roster.',
  },
];

const EMPLOYMENT_TYPES: { value: DriverEmploymentType; label: string; hint: string }[] = [
  {
    value: 'EMPLOYED',
    label: 'Employed driver',
    hint: 'Staff driver employed directly by your organisation.',
  },
  {
    value: 'POOL',
    label: 'Driver pool',
    hint: 'Hired into your pool — can be allocated to any vehicle when needed.',
  },
];

@Component({
  selector: 'app-fleet-driver-dialog',
  templateUrl: './fleet-driver-dialog.component.html',
  styleUrl: './fleet-driver-dialog.component.scss',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatRadioModule,
    UserAddressCascadeFieldsComponent,
  ],
})
export class FleetDriverDialogComponent implements OnInit, OnDestroy {
  readonly driverSources = DRIVER_SOURCES;
  readonly employmentTypes = EMPLOYMENT_TYPES;

  readonly isEdit: boolean;
  readonly title: string;
  readonly subtitle: string;
  readonly form: FormGroup;

  orgUsers: UserListRow[] = [];
  orgUsersLoading = false;
  userProfileLoading = false;
  userSearchQuery = '';
  selectedUserProfile: UserProfileBundle | null = null;
  linkedUserIds = new Set<number>();

  nationalIdDoc: DriverDocSlot = this.emptyDocSlot();
  passportDoc: DriverDocSlot = this.emptyDocSlot();
  licenseDoc: DriverDocSlot = this.emptyDocSlot();

  submitting = false;
  saveError = '';
  addressError = '';

  /** Linked platform user id — updated after legacy provisioning. */
  platformUserId: number | null = null;
  legacyPlatformEmail = '';
  reissuePlatformEmail = '';
  showReissuePanel = false;
  provisioningAccess = false;
  provisionError = '';
  provisionSuccess = '';
  private provisionedDriver: FleetDriverRow | null = null;

  suburbIdStr = '';
  cityIdStr = '';
  seedSuburbId: number | null = null;
  addressSeed: AddressHierarchySeed | null = null;

  private readonly driverId?: number;
  private readonly existingDriver?: FleetDriverRow;
  private readonly focusPlatformAccess: boolean;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<FleetDriverDialogComponent, FleetDriverRow | undefined>,
    private readonly fleet: FleetPortalService,
    private readonly usersPortal: UsersPortalService,
    private readonly orgContext: OrgContextService,
    private readonly locationsService: LocationsService,
    @Optional() @Inject(MAT_DIALOG_DATA) data: FleetDriverDialogData | null,
  ) {
    this.dialogRef.disableClose = true;
    this.focusPlatformAccess = data?.focusPlatformAccess === true;
    const driver = data?.driver;
    this.existingDriver = driver;
    this.isEdit = !!driver;
    this.platformUserId = driver?.userId ?? null;
    this.title = driver ? 'Edit driver' : 'Add driver';
    this.subtitle = driver
      ? `Update driver profile, identity documents, and licence for ${driver.fullName}.`
      : 'Link a platform user or enter details manually. Provide national ID or passport (at least one), plus a licence upload.';
    this.driverId = driver?.id;

    const initialSource: DriverSource = driver?.userId ? 'org_user' : driver ? 'manual' : 'org_user';

    this.form = this.fb.group({
      source: [initialSource],
      employmentType: [(driver?.employmentType ?? 'EMPLOYED') as DriverEmploymentType],
      orgUserId: [driver?.userId ?? null],
      userId: [driver?.userId ?? null],
      firstName: [driver?.firstName ?? '', [Validators.required, Validators.maxLength(80)]],
      lastName: [driver?.lastName ?? '', [Validators.required, Validators.maxLength(80)]],
      email: ['', [Validators.email, Validators.maxLength(150)]],
      provisionPlatformAccess: [false],
      phoneNumber: [driver?.phoneNumber && driver.phoneNumber !== '—' ? driver.phoneNumber : ''],
      nationalIdNumber: [driver?.nationalIdNumber ?? ''],
      nationalIdExpiryDate: [driver?.nationalIdExpiryDate ?? ''],
      passportNumber: [driver?.passportNumber ?? ''],
      passportExpiryDate: [driver?.passportExpiryDate ?? ''],
      licenseNumber: [
        driver?.licenseNumber && driver.licenseNumber !== '—' ? driver.licenseNumber : '',
        [Validators.required, Validators.maxLength(100)],
      ],
      licenseClass: [driver?.licenseClass && driver.licenseClass !== '—' ? driver.licenseClass : ''],
      addressLine1: [driver?.addressLine1 ?? '', [Validators.required, Validators.maxLength(200)]],
      addressLine2: [driver?.addressLine2 ?? ''],
      addressPostalCode: [driver?.addressPostalCode ?? '', [Validators.required, Validators.maxLength(30)]],
    });

    if (driver?.nationalIdUploadId) {
      this.nationalIdDoc.uploadId = driver.nationalIdUploadId;
      this.nationalIdDoc.fileName = 'National ID on file';
    }
    if (driver?.passportUploadId) {
      this.passportDoc.uploadId = driver.passportUploadId;
      this.passportDoc.fileName = 'Passport on file';
    }
    if (driver?.licenseUploadId) {
      this.licenseDoc.uploadId = driver.licenseUploadId;
      this.licenseDoc.fileName = 'Licence on file';
    }
  }

  ngOnInit(): void {
    this.loadLinkedUserIds();
    this.loadOrgUsers();

    this.form
      .get('source')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((source: DriverSource) => {
        this.onSourceChanged();
        if (source === 'org_user' && !this.isEdit) {
          this.form.patchValue({ employmentType: 'EMPLOYED' });
        }
      });

    this.form
      .get('orgUserId')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((userId: number | null) => this.onOrgUserSelected(userId));

    const existingUserId = this.form.get('userId')?.value as number | null;
    if (existingUserId) {
      this.onOrgUserSelected(existingUserId);
    } else if (this.isEdit && this.existingDriver?.userId) {
      this.loadAddressSeedFromUserProfile(this.existingDriver.userId);
    }
    if (this.focusPlatformAccess && this.showLegacyProvisionPanel) {
      this.scrollToLegacyPanel();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get driverSource(): DriverSource {
    return (this.form.get('source')?.value ?? 'org_user') as DriverSource;
  }

  get selectedOrgUserId(): number | null {
    const raw = this.form.get('orgUserId')?.value;
    return raw != null && raw !== '' ? Number(raw) : null;
  }

  get showOrgUserPicker(): boolean {
    return this.driverSource === 'org_user';
  }

  get showManualProfileFields(): boolean {
    return this.driverSource === 'manual';
  }

  get filteredOrgUsers(): UserListRow[] {
    const q = this.userSearchQuery.trim().toLowerCase();
    const currentUserId = this.form.get('userId')?.value as number | null;
    let rows = this.orgUsers.filter((row) => row.id > 0);
    rows = rows.filter((row) => !this.linkedUserIds.has(row.id) || row.id === currentUserId);
    if (!q) {
      return rows;
    }
    return rows.filter((row) => {
      const haystack =
        `${row.name} ${row.username} ${row.email} ${row.phoneNumber} ${row.nationalIdNumber}`.toLowerCase();
      return haystack.includes(q);
    });
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.form.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || control.dirty);
  }

  selectOrgUser(userId: number | null): void {
    this.form.patchValue({ orgUserId: userId, userId });
    this.form.get('orgUserId')?.markAsTouched();
  }

  onNationalIdFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.nationalIdDoc = {
      ...this.nationalIdDoc,
      file,
      fileName: file?.name ?? '',
      uploadId: null,
      profileUploadId: null,
    };
  }

  onPassportFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.passportDoc = {
      ...this.passportDoc,
      file,
      fileName: file?.name ?? '',
      uploadId: null,
      profileUploadId: null,
    };
  }

  onLicenseFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.licenseDoc = {
      ...this.licenseDoc,
      file,
      fileName: file?.name ?? '',
      uploadId: null,
    };
  }

  clearNationalIdDoc(): void {
    this.nationalIdDoc = this.emptyDocSlot();
  }

  clearPassportDoc(): void {
    this.passportDoc = this.emptyDocSlot();
  }

  clearLicenseDoc(): void {
    this.licenseDoc = this.emptyDocSlot();
  }

  cancel(): void {
    if (!this.submitting && !this.provisioningAccess) {
      this.dialogRef.close(this.provisionedDriver ?? undefined);
    }
  }

  enableDriverLogin(): void {
    if (!this.driverId) {
      return;
    }
    const email = this.legacyPlatformEmail.trim();
    if (!email) {
      this.provisionError = 'Enter the driver email address where login credentials should be sent.';
      return;
    }
    this.provisionError = '';
    this.provisionSuccess = '';
    this.provisioningAccess = true;
    this.fleet
      .provisionDriverPlatformAccess(this.driverId, { email })
      .pipe(
        finalize(() => (this.provisioningAccess = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (result) => {
          this.platformUserId = result.driver.userId ?? null;
          this.provisionedDriver = result.driver;
          this.provisionSuccess =
            result.message ||
            'Platform login enabled. Temporary credentials were emailed to the driver.';
          if (result.driver.userId) {
            this.form.patchValue({ userId: result.driver.userId });
          }
        },
        error: (err: Error) => {
          this.provisionError = err.message ?? 'Could not enable driver platform login.';
        },
      });
  }

  resendDriverCredentials(): void {
    if (!this.driverId) {
      return;
    }
    const email = this.reissuePlatformEmail.trim();
    if (!email) {
      this.provisionError = 'Enter the email address where new credentials should be sent.';
      return;
    }
    this.provisionError = '';
    this.provisionSuccess = '';
    this.provisioningAccess = true;
    this.fleet
      .provisionDriverPlatformAccess(this.driverId, { email, reissueCredentials: true })
      .pipe(
        finalize(() => (this.provisioningAccess = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (result) => {
          this.provisionedDriver = result.driver;
          this.provisionSuccess =
            result.message || 'New temporary credentials were emailed to the driver.';
          this.showReissuePanel = false;
        },
        error: (err: Error) => {
          this.provisionError = err.message ?? 'Could not re-send driver credentials.';
        },
      });
  }

  get showProvisionAccess(): boolean {
    return this.driverSource === 'manual' && !this.isEdit;
  }

  get hasPlatformLogin(): boolean {
    return this.platformUserId != null && this.platformUserId > 0;
  }

  get showLegacyProvisionPanel(): boolean {
    return this.isEdit && !this.hasPlatformLogin;
  }

  get showReissueCredentialsPanel(): boolean {
    return this.isEdit && this.hasPlatformLogin;
  }

  get provisionPlatformAccess(): boolean {
    return !!this.form.get('provisionPlatformAccess')?.value;
  }

  onProvisionAccessToggle(): void {
    const emailCtrl = this.form.get('email');
    if (!emailCtrl) return;
    if (this.provisionPlatformAccess) {
      emailCtrl.setValidators([Validators.required, Validators.email, Validators.maxLength(150)]);
    } else {
      emailCtrl.setValidators([Validators.email, Validators.maxLength(150)]);
      emailCtrl.setValue('');
    }
    emailCtrl.updateValueAndValidity();
  }

  save(): void {
    this.saveError = '';
    this.addressError = '';

    if (this.driverSource === 'org_user' && !this.selectedOrgUserId) {
      this.saveError = 'Select an organisation user to link this driver profile.';
      this.form.get('orgUserId')?.markAsTouched();
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const identityError = this.validateIdentityRules();
    if (identityError) {
      this.saveError = identityError;
      return;
    }

    const v = this.form.getRawValue();
    const suburbId = Number(this.suburbIdStr.trim());
    if (!Number.isFinite(suburbId) || suburbId <= 0) {
      this.addressError = 'Select country through suburb using the location dropdowns.';
      return;
    }
    if (!String(v.addressLine1 ?? '').trim() || !String(v.addressPostalCode ?? '').trim()) {
      this.addressError = 'Address line 1 and postal code are required.';
      return;
    }

    this.submitting = true;

    this.resolveAddressFromSuburb(suburbId)
      .pipe(
        switchMap((resolvedAddress) => {
          const provisionAccess = this.showProvisionAccess && !!v.provisionPlatformAccess;
          const basePayload: CreateFleetDriverPayload = {
            firstName: String(v.firstName).trim(),
            lastName: String(v.lastName).trim(),
            employmentType: (v.employmentType as DriverEmploymentType) ?? 'EMPLOYED',
            email: provisionAccess ? String(v.email ?? '').trim() || undefined : undefined,
            provisionPlatformAccess: provisionAccess || undefined,
            phoneNumber: String(v.phoneNumber ?? '').trim() || undefined,
            nationalIdNumber: String(v.nationalIdNumber ?? '').trim() || undefined,
            nationalIdExpiryDate: String(v.nationalIdExpiryDate ?? '').trim().slice(0, 10) || undefined,
            passportNumber: String(v.passportNumber ?? '').trim() || undefined,
            passportExpiryDate: String(v.passportExpiryDate ?? '').trim().slice(0, 10) || undefined,
            licenseNumber: String(v.licenseNumber ?? '').trim(),
            licenseClass: String(v.licenseClass ?? '').trim() || undefined,
            addressLine1: String(v.addressLine1 ?? '').trim(),
            addressLine2: String(v.addressLine2 ?? '').trim() || undefined,
            addressPostalCode: String(v.addressPostalCode ?? '').trim(),
            addressCity: resolvedAddress.addressCity,
            addressProvince: resolvedAddress.addressProvince,
            addressCountry: resolvedAddress.addressCountry,
            userId:
              this.driverSource === 'org_user' && v.userId != null && v.userId !== ''
                ? Number(v.userId)
                : undefined,
          };

          const persist$ =
            this.isEdit && this.driverId != null
              ? of(this.driverId)
              : this.fleet.createDriver(basePayload).pipe(switchMap((row) => of(row.id)));

          return persist$.pipe(
            switchMap((driverId) =>
              this.uploadDocuments(driverId).pipe(
                switchMap((uploadIds) => {
                  const payload: EditFleetDriverPayload = {
                    ...basePayload,
                    ...uploadIds,
                  };
                  return this.fleet.updateDriver(driverId, payload);
                }),
              ),
            ),
          );
        }),
        finalize(() => (this.submitting = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (row: FleetDriverRow) => this.dialogRef.close(row),
        error: (err: Error) => {
          this.saveError = err.message ?? 'Could not save driver.';
        },
      });
  }

  private scrollToLegacyPanel(): void {
    queueMicrotask(() => {
      document.getElementById('flt-driver-legacy-login')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  }

  private validateIdentityRules(): string | null {
    const v = this.form.getRawValue();
    const hasNationalId = !!String(v.nationalIdNumber ?? '').trim();
    const hasPassport = !!String(v.passportNumber ?? '').trim();
    if (!hasNationalId && !hasPassport) {
      return 'Provide a national ID number or a passport number (at least one is required).';
    }

    const hasNationalDoc =
      !!this.nationalIdDoc.file ||
      !!this.nationalIdDoc.uploadId ||
      !!this.nationalIdDoc.profileUploadId;
    const hasPassportDoc =
      !!this.passportDoc.file || !!this.passportDoc.uploadId || !!this.passportDoc.profileUploadId;
    if (!hasNationalDoc && !hasPassportDoc) {
      return 'Upload a national ID scan or a passport scan (at least one is required).';
    }

    const hasLicenseDoc = !!this.licenseDoc.file || !!this.licenseDoc.uploadId;
    if (!hasLicenseDoc) {
      return "Upload the driver's licence document.";
    }

    return null;
  }

  private uploadDocuments(driverId: number) {
    const uploads: Record<string, ReturnType<FleetPortalService['uploadFleetDriverDocument']>> = {};

    if (this.nationalIdDoc.file) {
      uploads['nationalIdUploadId'] = this.fleet.uploadFleetDriverDocument(
        driverId,
        this.nationalIdDoc.file,
        'NATIONAL_ID',
      );
    }
    if (this.passportDoc.file) {
      uploads['passportUploadId'] = this.fleet.uploadFleetDriverDocument(
        driverId,
        this.passportDoc.file,
        'PASSPORT',
      );
    }
    if (this.licenseDoc.file) {
      uploads['licenseUploadId'] = this.fleet.uploadFleetDriverDocument(
        driverId,
        this.licenseDoc.file,
        'DRIVER_LICENCE',
      );
    }

    const keys = Object.keys(uploads);
    if (!keys.length) {
      return of({
        nationalIdUploadId: this.resolveExistingUploadId(this.nationalIdDoc),
        passportUploadId: this.resolveExistingUploadId(this.passportDoc),
        licenseUploadId: this.licenseDoc.uploadId ?? undefined,
      });
    }

    return forkJoin(uploads).pipe(
      switchMap((result) =>
        of({
          nationalIdUploadId:
            result['nationalIdUploadId'] ?? this.resolveExistingUploadId(this.nationalIdDoc),
          passportUploadId:
            result['passportUploadId'] ?? this.resolveExistingUploadId(this.passportDoc),
          licenseUploadId: result['licenseUploadId'] ?? this.licenseDoc.uploadId ?? undefined,
        }),
      ),
    );
  }

  private resolveExistingUploadId(slot: DriverDocSlot): number | undefined {
    return slot.uploadId ?? slot.profileUploadId ?? undefined;
  }

  private emptyDocSlot(): DriverDocSlot {
    return {
      file: null,
      fileName: '',
      uploadId: null,
      profileUploadId: null,
      uploading: false,
    };
  }

  private loadLinkedUserIds(): void {
    this.fleet
      .listDrivers()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (rows) => {
          this.linkedUserIds = new Set(
            rows
              .filter((row) => row.userId && row.id !== this.driverId)
              .map((row) => Number(row.userId)),
          );
        },
        error: () => {
          this.linkedUserIds = new Set();
        },
      });
  }

  private loadOrgUsers(): void {
    const orgId = this.orgContext.organizationId;
    if (orgId == null) {
      this.orgUsers = [];
      return;
    }
    this.orgUsersLoading = true;
    this.usersPortal
      .queryUsersForOrganization(orgId)
      .pipe(
        finalize(() => (this.orgUsersLoading = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: ({ rows }) => {
          this.orgUsers = rows.filter((row) => row.id > 0);
        },
        error: () => {
          this.orgUsers = [];
        },
      });
  }

  private onSourceChanged(): void {
    this.userSearchQuery = '';
    this.selectedUserProfile = null;
    if (this.driverSource === 'manual') {
      this.form.patchValue({ orgUserId: null, userId: null });
      this.nationalIdDoc.profileUploadId = null;
      this.passportDoc.profileUploadId = null;
      this.clearAddressSeed();
    }
  }

  private onOrgUserSelected(userId: number | null): void {
    if (userId == null || userId <= 0) {
      this.selectedUserProfile = null;
      return;
    }

    const listUser = this.orgUsers.find((row) => row.id === userId);
    if (listUser) {
      const parts = listUser.name.trim().split(/\s+/);
      this.form.patchValue({
        userId,
        orgUserId: userId,
        firstName: parts[0] ?? '',
        lastName: parts.slice(1).join(' ') || listUser.name,
        phoneNumber: listUser.phoneNumber !== '—' ? listUser.phoneNumber : '',
        nationalIdNumber: listUser.nationalIdNumber !== '—' ? listUser.nationalIdNumber : '',
      });
    } else {
      this.form.patchValue({ userId, orgUserId: userId });
    }

    this.userProfileLoading = true;
    this.usersPortal
      .getUserProfileBundle(userId)
      .pipe(
        finalize(() => (this.userProfileLoading = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (bundle) => {
          this.selectedUserProfile = bundle;
          const user = bundle.user;
          const address = bundle.address;
          if (user) {
            this.form.patchValue({
              firstName: String(user['firstName'] ?? this.form.get('firstName')?.value ?? '').trim(),
              lastName: String(user['lastName'] ?? this.form.get('lastName')?.value ?? '').trim(),
              phoneNumber: String(user['phoneNumber'] ?? '').trim(),
              nationalIdNumber: String(user['nationalIdNumber'] ?? '').trim(),
              nationalIdExpiryDate: this.dateInputValue(user['nationalIdExpiryDate']),
              passportNumber: String(user['passportNumber'] ?? '').trim(),
              passportExpiryDate: this.dateInputValue(user['passportExpiryDate']),
              userId: Number(user['id'] ?? userId),
              orgUserId: userId,
            });

            const nationalUploadId = Number(user['nationalIdUploadId'] ?? 0);
            if (Number.isFinite(nationalUploadId) && nationalUploadId > 0) {
              this.nationalIdDoc.profileUploadId = nationalUploadId;
              this.nationalIdDoc.fileName = 'National ID from user profile';
            }

            const passportUploadId = Number(user['passportUploadId'] ?? 0);
            if (Number.isFinite(passportUploadId) && passportUploadId > 0) {
              this.passportDoc.profileUploadId = passportUploadId;
              this.passportDoc.fileName = 'Passport from user profile';
            }
          }
          if (address) {
            this.applyAddressFromRecord(address);
          } else {
            this.clearAddressSeed();
          }
        },
        error: () => {
          this.selectedUserProfile = null;
        },
      });
  }

  private loadAddressSeedFromUserProfile(userId: number): void {
    this.usersPortal
      .getUserProfileBundle(userId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (bundle) => {
          if (bundle.address) {
            this.applyAddressFromRecord(bundle.address);
          }
        },
      });
  }

  private applyAddressFromRecord(address: Record<string, unknown>): void {
    this.form.patchValue({
      addressLine1: String(address['line1'] ?? '').trim(),
      addressLine2: String(address['line2'] ?? '').trim(),
      addressPostalCode: String(address['postalCode'] ?? '').trim(),
    });

    const suburbNum = Number(address['suburbId'] ?? 0);
    if (address['suburbId'] != null && Number.isFinite(suburbNum) && suburbNum > 0) {
      this.seedSuburbId = suburbNum;
      this.suburbIdStr = String(suburbNum);
      this.cityIdStr = '';
      this.addressSeed = {
        countryId: this.toPositiveId(address['countryId']),
        provinceId: this.toPositiveId(address['provinceId']),
        districtId: this.toPositiveId(address['districtId']),
        cityId: this.toPositiveId(address['cityId']),
        suburbId: suburbNum,
        cityName:
          String(address['city'] ?? address['cityName'] ?? '').trim() ||
          undefined,
        addressLine2: String(address['line2'] ?? '').trim() || undefined,
      };
      return;
    }

    this.clearAddressSeed();
  }

  private clearAddressSeed(): void {
    this.seedSuburbId = null;
    this.suburbIdStr = '';
    this.cityIdStr = '';
    this.addressSeed = null;
  }

  private resolveAddressFromSuburb(suburbId: number): Observable<{
    addressCity: string;
    addressProvince?: string;
    addressCountry?: string;
  }> {
    return this.locationsService.findLocationById('suburb', suburbId).pipe(
      switchMap((suburb) => {
        if (!suburb) {
          return throwError(() => new Error('Could not resolve the selected suburb.'));
        }
        const city = String(suburb['cityName'] ?? suburb['name'] ?? '').trim();
        if (!city) {
          return throwError(() => new Error('Selected suburb is missing city information.'));
        }
        return of({
          addressCity: city,
          addressProvince: String(suburb['provinceName'] ?? '').trim() || undefined,
          addressCountry: String(suburb['countryName'] ?? '').trim() || undefined,
        });
      }),
    );
  }

  private toPositiveId(value: unknown): number | undefined {
    const n = Number(value);
    return Number.isFinite(n) && n > 0 ? n : undefined;
  }

  private dateInputValue(raw: unknown): string {
    const text = String(raw ?? '').trim();
    return text ? text.slice(0, 10) : '';
  }
}
