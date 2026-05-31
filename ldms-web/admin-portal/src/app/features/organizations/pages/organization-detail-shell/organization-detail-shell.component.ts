import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, catchError, finalize, forkJoin, of, switchMap, takeUntil } from 'rxjs';
import type { IndustryUsageRow } from '../../models/organization-directory.model';
import {
  ORG_TYPES,
  classificationLabel,
  type KycApplicationDocument,
  type OrganizationProfileDetail,
  type OrganizationType,
  type UpdateOrganizationPayload,
} from '../../models/organization.model';
import { OrganizationsAdminService } from '../../services/organizations-admin.service';
import { UsersAdminService, type UserListRow } from '../../../users/services/users-admin.service';
import { MatTableDataSource } from '@angular/material/table';
import { MatTabChangeEvent } from '@angular/material/tabs';
import { MatDialog } from '@angular/material/dialog';
import {
  LinkOrganizationDialogComponent,
  type LinkOrganizationKind,
} from '../link-organization-dialog/link-organization-dialog.component';

type OrgShellLoadError = '' | 'invalidId' | 'missingOrg' | 'requestFailed';

@Component({
  selector: 'app-organization-detail-shell',
  templateUrl: './organization-detail-shell.component.html',
  styleUrl: './organization-detail-shell.component.scss',
  standalone: false,
})
export class OrganizationDetailShellComponent implements OnInit, OnDestroy {
  readonly orgTypes = ORG_TYPES;
  readonly classificationLabelFn = classificationLabel;

  loading = false;
  loadError: OrgShellLoadError = '';
  orgIdParam = '';
  private orgNumericId = 0;
  profile: OrganizationProfileDetail | null = null;

  industries: IndustryUsageRow[] = [];
  industriesLoaded = false;

  editMode = false;
  saving = false;
  selectedTabIndex = 0;
  private pendingEditFromQuery = false;
  private pendingContactTabFromQuery = false;

  overviewForm!: FormGroup;

  readonly branchColumns: string[] = ['branchName', 'region', 'contact', 'flags', 'status'];
  readonly agentColumns: string[] = ['agent', 'kind', 'role', 'contact', 'status'];
  readonly linkColumns: string[] = ['name', 'classification', 'email', 'kycStatus', 'verified'];
  readonly orgUserColumns: string[] = ['name', 'email', 'emailVerified', 'status', 'createdAt', 'actions'];

  orgUsersLoading = false;
  orgUsersProvisioning = false;
  orgUsersError = '';
  /** Null until the first users load completes for this organisation. */
  orgUsersCount: number | null = null;
  orgUsersDataSource = new MatTableDataSource<UserListRow>([]);
  resendingVerificationUserId: number | null = null;
  private orgUsersLoadedForId = 0;

  verificationDocuments: KycApplicationDocument[] = [];
  documentsLoading = false;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly title: Title,
    private readonly orgService: OrganizationsAdminService,
    private readonly usersService: UsersAdminService,
    private readonly snackBar: MatSnackBar,
    private readonly fb: FormBuilder,
    private readonly cdr: ChangeDetectorRef,
    private readonly dialog: MatDialog,
  ) {
    this.overviewForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phoneNumber: [''],
      organizationType: ['PRIVATE' as OrganizationType, Validators.required],
      industryId: [null as number | null],
      registrationNumber: [''],
      taxNumber: [''],
      contactPersonFirstName: [''],
      contactPersonLastName: [''],
      contactPersonEmail: ['', Validators.email],
      contactPersonPhoneNumber: [''],
      websiteUrl: [''],
      organizationDescription: [''],
    });
  }

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe((pm) => {
      const raw = pm.get('orgId') ?? '';
      this.orgIdParam = raw;
      const id = OrganizationDetailShellComponent.parsePositiveIntParam(raw);

      const titleChip = raw || 'organisation';
      this.title.setTitle(`Organisation · ${titleChip} | LX Admin`);

      this.orgNumericId = id;

      if (id <= 0) {
        this.applyInvalidRouteId();
        return;
      }

      this.loadProfile();
    });

    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((qm) => {
      this.pendingEditFromQuery = qm.get('edit') === 'true';
      this.pendingContactTabFromQuery = qm.get('tab') === 'contact';
      if (this.pendingEditFromQuery && this.profile && !this.loading) {
        this.toggleEdit(true);
      }
      if (this.pendingContactTabFromQuery && this.profile && !this.loading) {
        this.goToContactPersonTab();
      }
    });

    this.orgService.queryIndustriesWithUsage().subscribe({
      next: (rows) => {
        this.industries = rows;
        this.industriesLoaded = true;
        this.cdr.markForCheck();
      },
      error: () => {
        this.industries = [];
        this.industriesLoaded = true;
        this.snackBar.open('Could not load industries for dropdown.', 'Close', {
          duration: 5000,
          panelClass: ['app-snackbar-error'],
        });
        this.cdr.markForCheck();
      },
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  orgIdForQuery(): number {
    return this.orgNumericId;
  }

  showCustomersTab(): boolean {
    return this.profile?.organizationClassification === 'SUPPLIER';
  }

  canManageSupplierLinks(): boolean {
    return this.profile?.organizationClassification === 'SUPPLIER';
  }

  openLinkDialog(linkKind: LinkOrganizationKind): void {
    const profile = this.profile;
    if (!profile || !this.canManageSupplierLinks()) {
      return;
    }
    let excludeIds: number[] = [];
    if (linkKind === 'CUSTOMER') {
      excludeIds = profile.customers.map((c) => c.id);
    } else if (linkKind === 'TRANSPORT_COMPANY') {
      excludeIds = profile.transporters.map((t) => t.id);
    } else if (linkKind === 'CLEARING_AGENT') {
      excludeIds = profile.clearingAgents.map((c) => c.id);
    }
    const ref = this.dialog.open(LinkOrganizationDialogComponent, {
      width: '640px',
      maxWidth: '96vw',
      panelClass: 'link-org-dialog-panel',
      autoFocus: 'first-tabbable',
      data: {
        supplierId: profile.id,
        supplierName: profile.name,
        linkKind,
        excludeIds,
      },
    });
    ref.afterClosed().subscribe((result) => {
      if (result?.linked) {
        this.loadProfile();
      }
    });
  }

  avatarLetter(): string {
    const n = String(this.profile?.name ?? '').trim();
    if (n) {
      return n.charAt(0).toUpperCase();
    }
    const fallback = String(this.orgIdParam || '').trim().charAt(0);
    return fallback ? fallback.toUpperCase() : '?';
  }

  formatDisplay(value: unknown): string {
    if (value === null || value === undefined || value === '') {
      return '—';
    }
    return String(value);
  }

  kindLabel(kind: string): string {
    if (kind === 'INDIVIDUAL') return 'Individual';
    if (kind === 'ORGANIZATION') return 'Organisation';
    return kind || '—';
  }

  kycChipClass(css: string): string {
    return `ods-kyc-chip ods-kyc-chip--${css || 'neutral'}`;
  }

  verifiedBadgeVisible(): boolean {
    return Boolean(this.profile?.isVerified === true);
  }

  toggleEdit(enabled: boolean): void {
    if (!this.profile || this.loading) {
      return;
    }
    this.editMode = enabled;
    if (!enabled && this.profile) {
      this.patchOverviewForm(this.profile);
    } else if (enabled && this.profile) {
      this.patchOverviewForm(this.profile);
    }
    this.cdr.markForCheck();
  }

  cancelEdit(): void {
    if (this.profile) {
      this.patchOverviewForm(this.profile);
    }
    this.editMode = false;
    this.cdr.markForCheck();
  }

  saveOverview(): void {
    if (!this.profile || !this.orgNumericId) {
      return;
    }
    if (this.overviewForm.invalid) {
      this.overviewForm.markAllAsTouched();
      return;
    }
    const payload = this.buildUpdatePayloadFromForm(this.overviewForm.getRawValue());
    const previousContactEmail = (this.profile.contactPersonEmail ?? '').trim().toLowerCase();
    const nextContactEmail = (payload.contactPersonEmail ?? '').trim().toLowerCase();
    const contactEmailChanged =
      previousContactEmail !== nextContactEmail && !!nextContactEmail;
    this.saving = true;
    const contactUserId = this.profile.contactPersonUserId;
    const syncContact$ =
      contactUserId && contactUserId > 0
        ? this.usersService.syncOrganizationContactPersonUser(contactUserId, {
            firstName: payload.contactPersonFirstName,
            lastName: payload.contactPersonLastName,
            email: payload.contactPersonEmail,
            phoneNumber: payload.contactPersonPhoneNumber,
          })
        : of(null);

    this.orgService
      .updateOrganization(this.profile.id, payload)
      .pipe(
        switchMap(() => syncContact$),
        finalize(() => {
          this.saving = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (syncResp) => {
          let message = 'Organisation saved.';
          if (contactEmailChanged && contactUserId && contactUserId > 0) {
            message += ' A verification email was sent to the updated contact address.';
          }
          if (syncResp != null && this.usersService.isUserMutationFailure(syncResp)) {
            message += ` Contact person user was not updated: ${this.usersService.formatUserMutationError(syncResp, 'sync failed')}`;
          }
          this.snackBar.open(message, 'Close', {
            duration: syncResp != null && this.usersService.isUserMutationFailure(syncResp) ? 7000 : 4500,
            panelClass: [
              syncResp != null && this.usersService.isUserMutationFailure(syncResp)
                ? 'app-snackbar-error'
                : 'app-snackbar-success',
            ],
          });
          this.editMode = false;
          this.loadProfile();
        },
        error: (err: Error) => {
          this.snackBar.open(err.message ?? 'Could not save organisation.', 'Close', {
            duration: 5500,
            panelClass: ['app-snackbar-error'],
          });
        },
      });
  }

  retryLoad(): void {
    if (this.orgNumericId <= 0) return;
    this.loadProfile();
  }

  secondarySubtitle(): string {
    const p = this.profile;
    if (!p) return '';
    const parts = [p.email?.trim(), p.phoneNumber?.trim()].filter(Boolean);
    return parts.join(' · ');
  }

  contactPersonName(): string {
    const p = this.profile;
    if (!p) {
      return '';
    }
    const n = `${p.contactPersonFirstName ?? ''} ${p.contactPersonLastName ?? ''}`.trim();
    return n;
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.overviewForm.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || control.dirty);
  }

  industryOptionLabel(ind: IndustryUsageRow): string {
    return ind.industryCode ? `${ind.name} (${ind.industryCode})` : ind.name;
  }

  contactPersonChannels(): string {
    const p = this.profile;
    if (!p) return '—';
    const bits = [p.contactPersonEmail?.trim(), p.contactPersonPhoneNumber?.trim()].filter(Boolean);
    if (!bits.length) {
      return '—';
    }
    return bits.join(' · ');
  }

  industriesTrackBy(index: number, row: IndustryUsageRow): number {
    return row.id;
  }

  onOrgTabSelected(event: MatTabChangeEvent): void {
    // Tab order: Overview (0), Documents (1), Contact person (2), Users (3), …
    if (event.index === 1) {
      this.onDocumentsTabActivated();
    }
    if (event.index === 3) {
      this.loadOrganizationUsers(true);
    }
  }

  goToContactPersonTab(): void {
    this.selectedTabIndex = 2;
    this.cdr.markForCheck();
  }

  onContactPersonUserUpdated(): void {
    this.loadProfile();
  }

  orgUsersCountLabel(): string {
    if (this.orgUsersCount === null) {
      return '';
    }
    const n = this.orgUsersCount;
    return n === 1 ? '1 user' : `${n} users`;
  }

  onDocumentsTabActivated(): void {
    if (this.verificationDocuments.length || this.documentsLoading) {
      return;
    }
    this.loadVerificationDocuments();
  }

  provisionContactUser(): void {
    if (!this.orgNumericId || this.orgUsersProvisioning) {
      return;
    }
    this.orgUsersProvisioning = true;
    this.orgUsersError = '';
    this.orgService
      .provisionContactPersonUser(this.orgNumericId)
      .pipe(
        finalize(() => {
          this.orgUsersProvisioning = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          this.snackBar.open('Contact person user account created. Verification email queued.', 'Close', {
            duration: 5000,
            panelClass: ['app-snackbar-success'],
          });
          this.loadProfile();
          this.loadOrganizationUsers(true);
        },
        error: (err: Error) => {
          this.orgUsersError = err.message ?? 'Could not provision contact person user.';
          this.snackBar.open(this.orgUsersError, 'Close', {
            duration: 6000,
            panelClass: ['app-snackbar-error'],
          });
        },
      });
  }

  userProfileLink(userId: number): string[] {
    return ['/users', String(userId), 'profile'];
  }

  isOrgUserEmailUnverified(row: UserListRow): boolean {
    return row.emailVerifiedLabel === 'No' && !!row.email?.trim();
  }

  resendOrgUserVerificationEmail(row: UserListRow): void {
    const email = row.email?.trim();
    if (!email || row.emailVerifiedLabel !== 'No') {
      return;
    }
    this.resendingVerificationUserId = row.id;
    this.usersService
      .resendVerificationEmail(email)
      .pipe(
        finalize(() => {
          this.resendingVerificationUserId = null;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (resp) => {
          if (this.usersService.isUserMutationFailure(resp)) {
            this.snackBar.open(
              this.usersService.formatUserMutationError(resp, 'Could not resend verification email.'),
              'Close',
              { duration: 6000, panelClass: ['app-snackbar-error'] },
            );
            return;
          }
          this.snackBar.open(`Verification email sent to ${email}.`, 'Close', {
            duration: 5000,
            panelClass: ['app-snackbar-success'],
          });
        },
        error: (err: unknown) => {
          this.snackBar.open(this.formatResendVerificationHttpError(err), 'Close', {
            duration: 6000,
            panelClass: ['app-snackbar-error'],
          });
        },
      });
  }

  private formatResendVerificationHttpError(err: unknown): string {
    if (!(err instanceof HttpErrorResponse)) {
      return 'Failed to resend verification email.';
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
    return err.status >= 400
      ? `Failed to resend verification email (${err.status}).`
      : 'Failed to resend verification email.';
  }

  private loadOrganizationUsers(force = false): void {
    const orgId = this.orgNumericId;
    if (!orgId || orgId <= 0) {
      return;
    }
    if (!force && this.orgUsersLoadedForId === orgId && this.orgUsersCount !== null) {
      return;
    }
    this.orgUsersLoading = true;
    this.orgUsersError = '';
    const contactPersonUserId = this.profile?.contactPersonUserId ?? null;
    this.usersService
      .queryUsersForOrganization(orgId)
      .pipe(
        finalize(() => {
          this.orgUsersLoading = false;
          this.orgUsersLoadedForId = orgId;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: ({ rows }) => {
          this.applyOrganizationUsersRows(rows, contactPersonUserId);
        },
        error: (err: Error) => {
          this.orgUsersDataSource.data = [];
          this.orgUsersCount = 0;
          this.orgUsersError = err.message ?? 'Could not load organisation users.';
        },
      });
  }

  private applyOrganizationUsersRows(rows: UserListRow[], contactPersonUserId: number | null): void {
    const orgId = this.orgNumericId;
    if (contactPersonUserId && contactPersonUserId > 0 && !rows.some((r) => r.id === contactPersonUserId)) {
      this.usersService.getUserProfileBundle(contactPersonUserId).subscribe({
        next: (bundle) => {
          const user = bundle.user;
          if (!user) {
            this.finishOrganizationUsersRows(rows);
            return;
          }
          const linkedOrgId = Number(user['organizationId'] ?? 0);
          if (linkedOrgId > 0 && linkedOrgId !== orgId) {
            this.finishOrganizationUsersRows(rows);
            return;
          }
          const extra = this.usersService.mapUserRowFromRecord(user);
          this.finishOrganizationUsersRows([extra, ...rows]);
        },
        error: () => this.finishOrganizationUsersRows(rows),
      });
      return;
    }
    this.finishOrganizationUsersRows(rows);
  }

  private finishOrganizationUsersRows(rows: UserListRow[]): void {
    this.orgUsersDataSource.data = rows;
    this.orgUsersCount = rows.length;
    this.cdr.markForCheck();
  }

  private static parsePositiveIntParam(raw: string): number {
    const t = raw.trim();
    if (!/^[1-9]\d*$/.test(t)) {
      return 0;
    }
    const n = Number(t);
    return Number.isSafeInteger(n) && n > 0 ? n : 0;
  }

  private applyInvalidRouteId(): void {
    this.loading = false;
    this.profile = null;
    this.loadError = 'invalidId';
    this.overviewForm.reset();
    this.cdr.markForCheck();
  }

  private loadProfile(): void {
    this.loadError = '';
    const id = this.orgNumericId;
    if (!id || id <= 0) {
      return;
    }
    this.loading = true;
    this.profile = null;
    this.verificationDocuments = [];
    forkJoin({
      profile: this.orgService.getOrganizationProfile(id),
      org: this.orgService.getOrganization(id).pipe(catchError(() => of(null))),
    })
      .pipe(
        finalize(() => {
          this.loading = false;
          const p = this.profile;
          if (p?.name?.trim()) {
            this.title.setTitle(`${p.name} | LX Admin`);
          }
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: ({ profile, org }) => {
          if (!profile.id || !`${profile.name ?? ''}`.trim()) {
            this.profile = null;
            this.loadError = 'missingOrg';
            return;
          }
          this.profile = profile;
          this.verificationDocuments = org?.documents ?? [];
          this.patchOverviewForm(profile);
          this.orgUsersCount = null;
          this.orgUsersLoadedForId = 0;
          this.orgUsersDataSource.data = [];
          this.loadOrganizationUsers(true);
          if (this.pendingEditFromQuery) {
            this.toggleEdit(true);
          }
          if (this.pendingContactTabFromQuery) {
            this.goToContactPersonTab();
          }
        },
        error: () => {
          this.profile = null;
          this.verificationDocuments = [];
          this.loadError = 'requestFailed';
        },
      });
  }

  private loadVerificationDocuments(): void {
    const id = this.orgNumericId;
    if (!id || id <= 0) {
      return;
    }
    this.documentsLoading = true;
    this.orgService
      .getOrganization(id)
      .pipe(
        finalize(() => {
          this.documentsLoading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (detail) => {
          this.verificationDocuments = detail.documents ?? [];
        },
        error: () => {
          this.verificationDocuments = [];
          this.snackBar.open('Could not load verification documents.', 'Close', {
            duration: 5000,
            panelClass: ['app-snackbar-error'],
          });
        },
      });
  }

  private patchOverviewForm(p: OrganizationProfileDetail): void {
    this.overviewForm.patchValue({
      name: p.name,
      email: p.email ?? '',
      phoneNumber: p.phoneNumber ?? '',
      organizationType: p.organizationType ?? 'OTHER',
      industryId:
        p.industryId !== null && p.industryId !== undefined && Number.isFinite(p.industryId)
          ? p.industryId
          : null,
      registrationNumber: p.registrationNumber ?? '',
      taxNumber: p.taxNumber ?? '',
      contactPersonFirstName: p.contactPersonFirstName ?? '',
      contactPersonLastName: p.contactPersonLastName ?? '',
      contactPersonEmail: p.contactPersonEmail ?? '',
      contactPersonPhoneNumber: p.contactPersonPhoneNumber ?? '',
      websiteUrl: p.websiteUrl ?? '',
      organizationDescription: p.organizationDescription ?? '',
    });
    this.cdr.markForCheck();
  }

  private buildUpdatePayloadFromForm(v: Record<string, unknown>): UpdateOrganizationPayload {
    const trim = (x: unknown) => (typeof x === 'string' ? x.trim() : '');
    const parseIndustry = (raw: unknown): number | undefined => {
      const n =
        typeof raw === 'number' && Number.isFinite(raw)
          ? Math.trunc(raw)
          : raw != null && `${raw}` !== ''
            ? Math.trunc(Number(raw))
            : NaN;
      return Number.isFinite(n) && n > 0 ? n : undefined;
    };

    const typeVal = trim(v['organizationType']) as OrganizationType;
    const ot: OrganizationType = typeVal ? (typeVal as OrganizationType) : 'OTHER';

    return {
      name: trim(v['name']) || undefined,
      email: trim(v['email']) || undefined,
      phoneNumber: trim(v['phoneNumber']) || undefined,
      organizationType: ot,
      industryId: parseIndustry(v['industryId']),
      registrationNumber: trim(v['registrationNumber']) || undefined,
      taxNumber: trim(v['taxNumber']) || undefined,
      contactPersonFirstName: trim(v['contactPersonFirstName']) || undefined,
      contactPersonLastName: trim(v['contactPersonLastName']) || undefined,
      contactPersonEmail: trim(v['contactPersonEmail']) || undefined,
      contactPersonPhoneNumber: trim(v['contactPersonPhoneNumber']) || undefined,
      websiteUrl: trim(v['websiteUrl']) || undefined,
      organizationDescription: trim(v['organizationDescription']) || undefined,
    };
  }
}
