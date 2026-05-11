import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { PageEvent } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { IdLabelOption, UsersAdminService, UserListRow } from '../../services/users-admin.service';
import { Subject, debounceTime, forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { LocationsService } from '../../../locations/services/locations.service';
import { MatDialog } from '@angular/material/dialog';
import { AssignRolesDialogComponent } from '../../components/assign-roles-dialog/assign-roles-dialog.component';
import { NotificationService } from '../../../../core/services/notification.service';

interface SelectOption {
  id: number;
  label: string;
  sublabel?: string;
}

interface UserTypeOption extends SelectOption {
  description: string;
}

@Component({
  selector: 'app-users-list',
  templateUrl: './users-list.component.html',
  styleUrl: './users-list.component.scss',
  standalone: false,
})
export class UsersListComponent implements OnInit {
  private static readonly MINIMUM_USER_AGE = 16;
  fetching = false;

  displayedColumns = [
    'name',
    'username',
    'email',
    'phoneNumber',
    'gender',
    'nationalId',
    'dateOfBirth',
    'accountType',
    'role',
    'emailVerified',
    'status',
    'createdAt',
    'updatedAt',
    'actions',
  ];

  /** Material table + async loads: use dataSource so rows render reliably after HTTP. */
  readonly userTable = new MatTableDataSource<UserListRow>([]);

  searchQuery = '';
  filterFieldsOpen = false;
  showSampleCsvInfo = false;
  readonly sampleCsvDescription =
    'Use this template to prepare user imports. Keep the column headers unchanged and provide one user per row.';
  readonly importCsvDisclaimer =
    'CSV import only. Keep required fields populated (username, email, first/last name, gender, date of birth, phone, password, userTypeName).';

  columnFilters = {
    email: '',
    firstName: '',
    lastName: '',
    username: '',
    phoneNumber: '',
    nationalIdNumber: '',
    passportNumber: '',
    statusLabel: '',
  };

  pageIndex = 0;
  pageSize = 10;
  totalRecords = 0;
  private readonly reload$ = new Subject<void>();
  private latestLoadToken = 0;
  showCreateModal = false;
  creating = false;
  createError = '';
  createStep = 0;
  optionsLoading = false;
  createModel = {
    organizationId: '',
    branchId: '',
    username: '',
    email: '',
    firstName: '',
    lastName: '',
    gender: '',
    dateOfBirth: '',
    phoneNumber: '',
    password: '',
    confirmPassword: '',
    nationalIdNumber: '',
    nationalIdExpiryDate: '',
    nationalIdUpload: null as File | null,
    passportNumber: '',
    passportExpiryDate: '',
    passportUpload: null as File | null,
    userTypeName: '',
    userTypeDescription: '',
    addressLine1: '',
    addressLine2: '',
    postalCode: '',
    suburbId: '',
    geoCoordinatesId: '',
    geoLatitude: '',
    geoLongitude: '',
    preferredLanguage: '',
    timezone: '',
    securityQuestion1: '',
    securityAnswer1: '',
    securityQuestion2: '',
    securityAnswer2: '',
    twoFactorAuthSecret: '',
    isTwoFactorEnabled: false,
  };
  genderOptions = ['MALE', 'FEMALE', 'OTHER'];
  languageOptions = ['English', 'Shona', 'Ndebele'];
  timezoneOptions = ['Africa/Harare', 'UTC'];
  userTypeOptions: UserTypeOption[] = [];
  countryOptions: SelectOption[] = [];
  provinceOptions: SelectOption[] = [];
  districtOptions: SelectOption[] = [];
  cityOptions: SelectOption[] = [];
  suburbOptions: SelectOption[] = [];
  organizationOptions: IdLabelOption[] = [];
  branchOptions: IdLabelOption[] = [];
  /** Wizard-only address cascade; suburb id is sent as `userAddressDetails.suburbId` on create. */
  addressCountryId = '';
  addressProvinceId = '';
  addressDistrictId = '';
  addressCityId = '';
  /** Prevents opening native selects while FK rows are still in flight (empty first paint). */
  provinceOptionsLoading = false;
  districtOptionsLoading = false;
  cityOptionsLoading = false;
  suburbOptionsLoading = false;
  organizationsLoading = false;
  branchesLoading = false;

  constructor(
    private readonly title: Title,
    private readonly usersService: UsersAdminService,
    private readonly locationsService: LocationsService,
    private readonly dialog: MatDialog,
    private readonly cdr: ChangeDetectorRef,
    private readonly notifications: NotificationService,
  ) {}

  trackBySelectOptionId(_index: number, o: SelectOption): number {
    return o.id;
  }

  /** String `<option value>` so `ngModel` stays aligned with the DOM (avoids native select mismatch). */
  selectOptionValue(id: number): string {
    return String(id);
  }

  resetPaging(): void {
    this.pageIndex = 0;
    this.reload$.next();
  }

  onPage(e: PageEvent): void {
    this.pageIndex = e.pageIndex;
    this.pageSize = e.pageSize;
    this.reload$.next();
  }

  ngOnInit(): void {
    this.title.setTitle('Users | LX Admin');
    this.reload$.pipe(debounceTime(150)).subscribe(() => this.loadUsers());
    this.reload$.next();
  }

  userLink(
    row: UserListRow,
    section: 'profile' | 'account' | 'preferences' | 'security-policies' | 'addresses' | 'password',
  ): string[] {
    return ['/users', String(row.id), section];
  }

  openAssignRolesToGroupDialog(row: UserListRow): void {
    if (!row.userGroupId) return;
    const groupLabel = row.role !== '—' ? row.role : undefined;
    this.dialog.open(AssignRolesDialogComponent, {
      width: '560px',
      maxWidth: '94vw',
      data: { userGroupId: row.userGroupId, groupLabel },
    });
  }

  refresh(): void {
    this.reload$.next();
  }

  stubImport(): void {}

  stubExport(): void {}

  downloadSampleCsv(): void {
    const rows = [
      'username,email,firstName,lastName,gender,dateOfBirth,phoneNumber,password,userTypeName,nationalIdNumber,passportNumber,statusLabel',
      'tmoyo,tmoyo@example.com,Tinashe,Moyo,MALE,1992-04-17,+263771111111,Temp@123,Driver,63-123456-A-12,DN123456,ACTIVE',
      'rnyoni,rnyoni@example.com,Rudo,Nyoni,FEMALE,1996-11-02,+263772222222,Temp@123,Dispatcher,12-987654-B-34,DN654321,ACTIVE',
    ].join('\n');
    this.downloadCsv('users-sample.csv', rows);
  }

  openCreateModal(): void {
    this.resetCreateWizardForm();
    this.showCreateModal = true;
    this.loadCreateOptions();
  }

  closeCreateModal(): void {
    if (this.creating) return;
    this.showCreateModal = false;
  }

  submitCreate(): void {
    const required = [
      this.createModel.username,
      this.createModel.email,
      this.createModel.firstName,
      this.createModel.lastName,
      this.createModel.gender,
      this.createModel.dateOfBirth,
      this.createModel.phoneNumber,
      this.createModel.password,
      this.createModel.userTypeName,
      this.createModel.userTypeDescription,
    ];
    if (required.some((v) => !v.trim())) {
      this.createError = 'Fill all required user fields.';
      return;
    }
    if (this.createModel.password !== this.createModel.confirmPassword) {
      this.createError = 'Password and confirm password must match.';
      return;
    }
    const nid = this.createModel.nationalIdNumber.trim();
    const ppt = this.createModel.passportNumber.trim();
    if (!nid && !ppt) {
      this.createError = 'Provide a national ID or a passport number (backend requires identification).';
      return;
    }
    if (!this.createModel.preferredLanguage.trim() || !this.createModel.timezone.trim()) {
      this.createError = 'Select preferred language and timezone.';
      return;
    }

    this.creating = true;
    this.createError = '';
    this.usersService
      .createUser({
        organizationId: this.parseLong(this.createModel.organizationId),
        branchId: this.parseLong(this.createModel.branchId),
        username: this.createModel.username.trim(),
        email: this.createModel.email.trim(),
        firstName: this.createModel.firstName.trim(),
        lastName: this.createModel.lastName.trim(),
        gender: this.createModel.gender.trim(),
        dateOfBirth: this.createModel.dateOfBirth.trim(),
        phoneNumber: this.createModel.phoneNumber.trim(),
        password: this.createModel.password,
        nationalIdNumber: nid || undefined,
        nationalIdExpiryDate: this.createModel.nationalIdExpiryDate.trim() || undefined,
        nationalIdUpload: this.createModel.nationalIdUpload ?? undefined,
        passportNumber: ppt || undefined,
        passportExpiryDate: this.createModel.passportExpiryDate.trim() || undefined,
        passportUpload: this.createModel.passportUpload ?? undefined,
        userTypeName: this.createModel.userTypeName.trim(),
        userTypeDescription: this.createModel.userTypeDescription.trim(),
        addressLine1: this.createModel.addressLine1.trim(),
        addressLine2: this.createModel.addressLine2.trim(),
        postalCode: this.createModel.postalCode.trim(),
        suburbId: this.parseLong(this.createModel.suburbId),
        geoCoordinatesId: this.parseLong(this.createModel.geoCoordinatesId),
        geoLatitude: this.parseOptionalNumber(this.createModel.geoLatitude),
        geoLongitude: this.parseOptionalNumber(this.createModel.geoLongitude),
        preferredLanguage: this.createModel.preferredLanguage.trim(),
        timezone: this.createModel.timezone.trim(),
        securityQuestion1: this.createModel.securityQuestion1.trim(),
        securityAnswer1: this.createModel.securityAnswer1.trim(),
        securityQuestion2: this.createModel.securityQuestion2.trim(),
        securityAnswer2: this.createModel.securityAnswer2.trim(),
        twoFactorAuthSecret: this.createModel.twoFactorAuthSecret.trim(),
        isTwoFactorEnabled: this.createModel.isTwoFactorEnabled,
      })
      .pipe(
        finalize(() => {
          this.creating = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (resp: unknown) => {
          if (this.isUserCreateFailure(resp)) {
            const msg = this.formatUserApiError(resp);
            this.createError = msg;
            this.notifications.error(msg);
            return;
          }
          this.notifications.success('User created successfully.');
          this.showCreateModal = false;
          this.resetPaging();
        },
        error: (err: unknown) => {
          const msg = this.formatCreateUserHttpError(err);
          this.createError = msg;
          this.notifications.error(msg);
        },
      });
  }

  nextCreateStep(): void {
    if (!this.validateStep(this.createStep, true)) return;
    this.createStep = Math.min(3, this.createStep + 1);
  }

  prevCreateStep(): void {
    this.createError = '';
    this.createStep = Math.max(0, this.createStep - 1);
  }

  goToCreateStep(step: number): void {
    if (step < 0 || step > 3) return;
    if (!this.canAccessStep(step)) {
      this.createError = 'Complete the current stage before moving forward.';
      return;
    }
    this.createStep = step;
    this.createError = '';
  }

  canAccessStep(step: number): boolean {
    for (let i = 0; i < step; i += 1) {
      if (!this.isStepComplete(i)) return false;
    }
    return true;
  }

  isStepComplete(step: number): boolean {
    return this.validateStep(step, false);
  }

  onUserTypeChange(userTypeIdRaw: string): void {
    const id = Number(userTypeIdRaw);
    const selected = this.userTypeOptions.find((u) => u.id === id);
    if (!selected) return;
    this.createModel.userTypeName = selected.label;
    this.createModel.userTypeDescription = selected.description;
  }

  onSuburbChange(suburbIdRaw: unknown): void {
    const trimmed = String(suburbIdRaw ?? '').trim();
    if (!trimmed) {
      this.createModel.suburbId = '';
      return;
    }
    const id = Number(trimmed);
    if (!Number.isFinite(id)) return;
    this.createModel.suburbId = String(id);
  }

  onNationalIdUploadSelected(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.[0] ?? null;
    this.createModel.nationalIdUpload = file;
  }

  onPassportUploadSelected(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.[0] ?? null;
    this.createModel.passportUpload = file;
  }

  onOrganizationChange(value: unknown): void {
    const raw = String(value ?? '').trim();
    this.createModel.organizationId = raw;
    this.createModel.branchId = '';
    this.branchOptions = [];
    this.branchesLoading = false;
    const organizationId = Number(raw);
    if (!raw || !Number.isFinite(organizationId) || organizationId < 1) return;
    this.branchesLoading = true;
    this.usersService.queryBranchesForOrganization(organizationId).pipe(
      finalize(() => {
        this.branchesLoading = false;
        queueMicrotask(() => this.cdr.detectChanges());
      }),
    ).subscribe({
      next: (options) => {
        this.branchOptions = options;
      },
      error: () => {
        this.branchOptions = [];
      },
    });
  }

  onAddressCountryChange(value: unknown): void {
    const raw = String(value ?? '').trim();
    this.addressCountryId = raw;
    this.addressProvinceId = '';
    this.addressDistrictId = '';
    this.addressCityId = '';
    this.provinceOptions = [];
    this.districtOptions = [];
    this.cityOptions = [];
    this.suburbOptions = [];
    this.provinceOptionsLoading = false;
    this.districtOptionsLoading = false;
    this.cityOptionsLoading = false;
    this.suburbOptionsLoading = false;
    this.clearSuburbSelection();
    const id = Number(raw);
    if (!raw || !Number.isFinite(id)) return;
    this.provinceOptionsLoading = true;
    this.locationsService
      .fetchProvincesForSelect({ countryId: String(id) })
      .pipe(finalize(() => this.afterAddressOptionsLoaded('provinces')))
      .subscribe({
        next: (opts) => {
          this.provinceOptions = opts.map((o) => ({ id: o.id, label: o.label, sublabel: o.sublabel }));
        },
        error: () => {
          this.provinceOptions = [];
        },
      });
  }

  onAddressProvinceChange(value: unknown): void {
    const raw = String(value ?? '').trim();
    this.addressProvinceId = raw;
    this.addressDistrictId = '';
    this.addressCityId = '';
    this.districtOptions = [];
    this.cityOptions = [];
    this.suburbOptions = [];
    this.districtOptionsLoading = false;
    this.cityOptionsLoading = false;
    this.suburbOptionsLoading = false;
    this.clearSuburbSelection();
    const id = Number(raw);
    if (!raw || !Number.isFinite(id)) return;
    this.districtOptionsLoading = true;
    this.locationsService
      .fetchDistrictsForSelect({ provinceId: String(id) })
      .pipe(finalize(() => this.afterAddressOptionsLoaded('districts')))
      .subscribe({
        next: (opts) => {
          this.districtOptions = opts.map((o) => ({ id: o.id, label: o.label, sublabel: o.sublabel }));
        },
        error: () => {
          this.districtOptions = [];
        },
      });
  }

  onAddressDistrictChange(value: unknown): void {
    const raw = String(value ?? '').trim();
    this.addressDistrictId = raw;
    this.addressCityId = '';
    this.cityOptions = [];
    this.suburbOptions = [];
    this.cityOptionsLoading = false;
    this.suburbOptionsLoading = false;
    this.clearSuburbSelection();
    const id = Number(raw);
    if (!raw || !Number.isFinite(id)) return;
    this.cityOptionsLoading = true;
    this.locationsService
      .fetchCitiesForSelect({ districtId: String(id) })
      .pipe(finalize(() => this.afterAddressOptionsLoaded('cities')))
      .subscribe({
        next: (opts) => {
          this.cityOptions = opts.map((o) => ({ id: o.id, label: o.label, sublabel: o.sublabel }));
        },
        error: () => {
          this.cityOptions = [];
        },
      });
  }

  onAddressCityChange(value: unknown): void {
    const raw = String(value ?? '').trim();
    this.addressCityId = raw;
    this.suburbOptions = [];
    this.suburbOptionsLoading = false;
    this.clearSuburbSelection();
    const cityId = Number(raw);
    const districtId = Number(this.addressDistrictId);
    if (!raw || !Number.isFinite(cityId) || !Number.isFinite(districtId)) return;
    this.suburbOptionsLoading = true;
    this.locationsService
      .fetchSuburbsForSelect({ districtId: String(districtId), cityId: String(cityId) })
      .pipe(finalize(() => this.afterAddressOptionsLoaded('suburbs')))
      .subscribe({
        next: (opts) => {
          this.suburbOptions = opts.map((o) => ({ id: o.id, label: o.label, sublabel: o.sublabel }));
        },
        error: () => {
          this.suburbOptions = [];
        },
      });
  }

  /** Native `<select>` + async `*ngFor` options often need a tick to repaint correctly. */
  private afterAddressOptionsLoaded(
    tier: 'provinces' | 'districts' | 'cities' | 'suburbs',
  ): void {
    if (tier === 'provinces') this.provinceOptionsLoading = false;
    if (tier === 'districts') this.districtOptionsLoading = false;
    if (tier === 'cities') this.cityOptionsLoading = false;
    if (tier === 'suburbs') this.suburbOptionsLoading = false;
    queueMicrotask(() => this.cdr.detectChanges());
  }

  private loadUsers(): void {
    const loadToken = ++this.latestLoadToken;
    this.fetching = true;
    this.usersService
      .queryUsers({
        page: this.pageIndex,
        size: this.pageSize,
        searchQuery: this.searchQuery,
        columnFilters: this.columnFilters,
      })
      .pipe(
        finalize(() => {
          if (loadToken === this.latestLoadToken) {
            this.fetching = false;
            this.cdr.detectChanges();
          }
        }),
      )
      .subscribe({
        next: ({ rows, totalElements }) => {
          if (loadToken !== this.latestLoadToken) return;
          if (rows.length === 0 && totalElements > 0 && this.pageIndex > 0) {
            this.pageIndex = 0;
            this.loadUsers();
            return;
          }
          // New array ref so MatTableDataSource always emits; avoids stale empty state after HTTP.
          this.userTable.data = rows.slice();
          this.totalRecords = totalElements;
          this.cdr.detectChanges();
        },
        error: () => {
          if (loadToken !== this.latestLoadToken) return;
          this.userTable.data = [];
          this.totalRecords = 0;
        },
      });
  }

  private parseOptionalNumber(raw: string): number | undefined {
    const trimmed = raw.trim();
    if (!trimmed) return undefined;
    const n = Number(trimmed);
    return Number.isFinite(n) ? n : undefined;
  }

  private parseLong(raw: string): number | undefined {
    const trimmed = raw.trim();
    if (!trimmed) return undefined;
    const n = Number(trimmed);
    return Number.isFinite(n) ? n : undefined;
  }

  private downloadCsv(filename: string, contents: string): void {
    const blob = new Blob([contents], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }

  private validateStep(step: number, showError: boolean): boolean {
    if (step === 0) {
      const basicRequired = [
        this.createModel.username,
        this.createModel.email,
        this.createModel.firstName,
        this.createModel.lastName,
        this.createModel.gender,
        this.createModel.dateOfBirth,
        this.createModel.phoneNumber,
        this.createModel.password,
        this.createModel.confirmPassword,
      ];
      const isValid = basicRequired.every((v) => v.trim().length > 0);
      if (!isValid && showError) this.createError = 'Complete all required basic information fields.';
      if (!isValid) return false;

      const passwordsMatch = this.createModel.password === this.createModel.confirmPassword;
      if (!passwordsMatch && showError) {
        this.createError = 'Password and confirm password must match before continuing.';
      }
      if (!passwordsMatch) return false;

      const dateIsValid = this.isDateOfBirthAllowed(this.createModel.dateOfBirth);
      if (!dateIsValid && showError) {
        this.createError = `Date of birth must be at least ${UsersListComponent.MINIMUM_USER_AGE} years ago.`;
      }
      if (dateIsValid && showError) this.createError = '';
      return dateIsValid;
    }

    if (step === 1) {
      const typeOk = this.createModel.userTypeName.trim().length > 0;
      const nid = this.createModel.nationalIdNumber.trim();
      const ppt = this.createModel.passportNumber.trim();
      const idOk = nid.length > 0 || ppt.length > 0;
      const addrOk =
        this.createModel.addressLine1.trim().length > 0 &&
        this.createModel.postalCode.trim().length > 0 &&
        !!this.parseLong(this.createModel.suburbId);
      const isValid = typeOk && idOk && addrOk;
      if (!typeOk && showError) this.createError = 'Select a user type.';
      else if (!idOk && showError) {
        this.createError = 'Enter a national ID or passport number.';
      } else if (!addrOk && showError) {
        this.createError = 'Enter address line 1, postal code, and suburb (required to create the address).';
      }
      if (isValid && showError) this.createError = '';
      return isValid;
    }

    if (step === 2) {
      const isValid = this.createModel.preferredLanguage.trim().length > 0 && this.createModel.timezone.trim().length > 0;
      if (!isValid && showError) this.createError = 'Select preferred language and timezone before continuing.';
      if (isValid && showError) this.createError = '';
      return isValid;
    }

    if (step === 3) {
      if (showError) this.createError = '';
      return true;
    }

    return true;
  }

  private isUserCreateFailure(resp: unknown): boolean {
    if (resp === null || typeof resp !== 'object') {
      return false;
    }
    const r = resp as Record<string, unknown>;
    if (r['success'] === false || r['isSuccess'] === false) {
      return true;
    }
    const statusCode = r['statusCode'];
    return typeof statusCode === 'number' && statusCode >= 400;
  }

  private formatUserApiError(resp: unknown): string {
    if (resp !== null && typeof resp === 'object') {
      const r = resp as Record<string, unknown>;
      const messages = r['errorMessages'];
      if (Array.isArray(messages) && messages.length > 0) {
        return messages.map((m) => String(m)).join(' ');
      }
      if (typeof r['message'] === 'string' && r['message'].trim()) {
        return r['message'].trim();
      }
    }
    return 'Could not create user. Check the form and try again.';
  }

  private formatCreateUserHttpError(err: unknown): string {
    if (!(err instanceof HttpErrorResponse)) {
      return 'Failed to create user.';
    }
    const body = err.error;
    if (body !== null && typeof body === 'object') {
      const rec = body as Record<string, unknown>;
      const messages = rec['errorMessages'];
      if (Array.isArray(messages) && messages.length > 0) {
        return messages.map((m) => String(m)).join(' ');
      }
      if (typeof rec['message'] === 'string' && rec['message'].trim()) {
        return rec['message'].trim();
      }
    }
    if (typeof body === 'string' && body.trim()) {
      return body.trim().slice(0, 300);
    }
    return err.status >= 400
      ? `Failed to create user (${err.status}).`
      : 'Failed to create user.';
  }

  get maximumDateOfBirth(): string {
    const maxDate = new Date();
    maxDate.setFullYear(maxDate.getFullYear() - UsersListComponent.MINIMUM_USER_AGE);
    return this.toDateInputValue(maxDate);
  }

  private isDateOfBirthAllowed(rawDate: string): boolean {
    const trimmed = rawDate.trim();
    if (!trimmed) return false;
    const picked = new Date(trimmed);
    if (Number.isNaN(picked.getTime())) return false;
    return trimmed <= this.maximumDateOfBirth;
  }

  private toDateInputValue(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private clearSuburbSelection(): void {
    this.createModel.suburbId = '';
  }

  private resetCreateWizardForm(): void {
    this.createError = '';
    this.createStep = 0;
    this.createModel = {
      organizationId: '',
      branchId: '',
      username: '',
      email: '',
      firstName: '',
      lastName: '',
      gender: '',
      dateOfBirth: '',
      phoneNumber: '',
      password: '',
      confirmPassword: '',
      nationalIdNumber: '',
      nationalIdExpiryDate: '',
      nationalIdUpload: null,
      passportNumber: '',
      passportExpiryDate: '',
      passportUpload: null,
      userTypeName: '',
      userTypeDescription: '',
      addressLine1: '',
      addressLine2: '',
      postalCode: '',
      suburbId: '',
      geoCoordinatesId: '',
      geoLatitude: '',
      geoLongitude: '',
      preferredLanguage: '',
      timezone: '',
      securityQuestion1: '',
      securityAnswer1: '',
      securityQuestion2: '',
      securityAnswer2: '',
      twoFactorAuthSecret: '',
      isTwoFactorEnabled: false,
    };
    this.addressCountryId = '';
    this.addressProvinceId = '';
    this.addressDistrictId = '';
    this.addressCityId = '';
    this.countryOptions = [];
    this.organizationOptions = [];
    this.branchOptions = [];
    this.provinceOptions = [];
    this.districtOptions = [];
    this.cityOptions = [];
    this.suburbOptions = [];
  }

  private loadCreateOptions(): void {
    this.optionsLoading = true;
    this.organizationsLoading = true;
    forkJoin({
      userTypes: this.usersService.queryUserTypes({
        page: 0,
        size: 200,
        searchQuery: '',
        columnFilters: { userTypeName: '', description: '' },
      }),
      countries: this.locationsService.fetchCountriesForSelect(),
      organizations: this.usersService.queryOrganizationsForSelect(),
    }).subscribe({
      next: ({ userTypes, countries, organizations }) => {
        this.userTypeOptions = userTypes.rows.map((r) => ({
          id: Number(r['id'] ?? 0),
          label: String(r['userTypeName'] ?? ''),
          description: String(r['description'] ?? ''),
        }));
        this.countryOptions = countries.map((o) => ({ id: o.id, label: o.label, sublabel: o.sublabel }));
        this.organizationOptions = organizations;
        this.optionsLoading = false;
        this.organizationsLoading = false;
        queueMicrotask(() => this.cdr.detectChanges());
      },
      error: () => {
        this.userTypeOptions = [];
        this.countryOptions = [];
        this.organizationOptions = [];
        this.optionsLoading = false;
        this.organizationsLoading = false;
      },
    });
  }
}
