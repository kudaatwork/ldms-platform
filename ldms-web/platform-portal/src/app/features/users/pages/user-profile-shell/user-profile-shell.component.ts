import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DomSanitizer, SafeResourceUrl, Title } from '@angular/platform-browser';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subscription, combineLatest, forkJoin, of } from 'rxjs';
import { catchError, finalize, map, startWith, switchMap } from 'rxjs/operators';
import { UserDocumentDetailDialogComponent } from '../../components/user-document-detail-dialog/user-document-detail-dialog.component';
import { UserEditAccountDialogComponent } from '../../components/user-edit-account-dialog/user-edit-account-dialog.component';
import { UserEditAddressDialogComponent } from '../../components/user-edit-address-dialog/user-edit-address-dialog.component';
import { UserEditProfileDialogComponent } from '../../components/user-edit-profile-dialog/user-edit-profile-dialog.component';
import { UserEditSecurityDialogComponent } from '../../components/user-edit-security-dialog/user-edit-security-dialog.component';
import { UserAssignUserGroupDialogComponent } from '../../components/user-assign-user-group-dialog/user-assign-user-group-dialog.component';
import { UsersPortalService, UserFileUploadSummary, UserProfileBundle } from '../../services/users-portal.service';
import { isLdmsPasswordValid, LDMS_PASSWORD_INVALID_MESSAGE } from '@core/utils/ldms-password.util';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { StorageService } from '../../../../core/services/storage.service';
import { decodeJwtPayload } from '../../../../core/utils/jwt.util';
import { PhoneVerificationPromptService } from '../../../../core/services/phone-verification-prompt.service';
import { ShellNotificationService } from '../../../../core/services/shell-notification.service';

type UserSection = 'profile' | 'account' | 'preferences' | 'security-policies' | 'addresses' | 'password';
type ProfileLoadErrorKey = '' | 'invalidId' | 'missingUser' | 'requestFailed';

/** Roles listed before the user expands or searches. */
const ROLES_PREVIEW_LIMIT = 8;

@Component({
  selector: 'app-user-profile-shell',
  templateUrl: './user-profile-shell.component.html',
  styleUrl: './user-profile-shell.component.scss',
  standalone: false,
})
export class UserProfileShellComponent implements OnInit, OnDestroy {
  loading = true;
  loadError: ProfileLoadErrorKey = '';
  userId = '';
  userIdNumber = 0;
  section: UserSection = 'profile';
  bundle: UserProfileBundle = {
    user: null,
    account: null,
    security: null,
    address: null,
    password: null,
  };
  /** User-owned file uploads (national ID, passport, etc.). */
  userDocuments: UserFileUploadSummary[] = [];

  /** Cached sanitized PDF data URLs (keyed by upload id) for mini previews. */
  private readonly pdfSafeUrlByUploadId = new Map<number, SafeResourceUrl>();

  newPassword = '';
  confirmPassword = '';
  passwordSaving = false;
  passwordError = '';
  resendingVerificationEmail = false;
  private pendingVerifyAction: 'phone' | 'email' | null = null;

  readonly rolesPreviewLimit = ROLES_PREVIEW_LIMIT;
  roleSearch = '';
  rolesExpanded = false;

  readonly documentColumns: string[] = ['preview', 'originalFileName', 'fileType', 'fileSizeInBytes', 'createdAt', 'entityStatus'];

  readonly nav: ReadonlyArray<{ key: UserSection; icon: string }> = [
    { key: 'profile', icon: 'person' },
    { key: 'account', icon: 'manage_accounts' },
    { key: 'preferences', icon: 'tune' },
    { key: 'security-policies', icon: 'gpp_good' },
    { key: 'addresses', icon: 'home_pin' },
    { key: 'password', icon: 'lock_reset' },
  ];

  private routeSub?: Subscription;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly title: Title,
    private readonly sanitizer: DomSanitizer,
    private readonly usersService: UsersPortalService,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
    private readonly authState: AuthStateService,
    private readonly storage: StorageService,
    private readonly phoneVerificationPrompt: PhoneVerificationPromptService,
    private readonly shellNotifications: ShellNotificationService,
  ) {}

  ngOnInit(): void {
    // Do not include `route.url` in combineLatest: in some lazy-route setups it never emits an
    // initial value, which blocks combineLatest and leaves this screen stuck on “Loading…”.
    // Section is resolved from `route.data` (see users-routing) and `snapshot.url` as fallback.
    this.routeSub = combineLatest([
      this.route.paramMap.pipe(startWith(this.route.snapshot.paramMap)),
      this.route.data.pipe(startWith(this.route.snapshot.data)),
      this.route.queryParamMap.pipe(startWith(this.route.snapshot.queryParamMap)),
    ]).subscribe(([, , queryParams]) => {
      const verify = queryParams.get('verify');
      this.pendingVerifyAction = verify === 'phone' || verify === 'email' ? verify : null;

      const previousUserId = this.userId;
      this.applyRouteSnapshot();
      const userChanged = this.userId !== previousUserId || previousUserId === '';
      if (userChanged || !this.bundle.user) {
        this.resetRolesPanel();
        this.loadBundle();
      } else {
        this.maybeRunVerifyAction();
        this.cdr.markForCheck();
      }
    });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
    this.pdfSafeUrlByUploadId.clear();
  }

  routeFor(key: string): string[] {
    return ['/users', this.userId, key];
  }

  headingName(): string | null {
    const u = this.bundle.user;
    if (!u) {
      return null;
    }
    const first = String(u['firstName'] ?? '').trim();
    const last = String(u['lastName'] ?? '').trim();
    const full = `${first} ${last}`.trim();
    if (full) {
      return full;
    }
    const un = String(u['username'] ?? '').trim();
    return un || null;
  }

  headingSubtitle(): string {
    const u = this.bundle.user;
    if (!u) {
      return '';
    }
    const parts = [String(u['username'] ?? '').trim(), String(u['email'] ?? '').trim()].filter(Boolean);
    return parts.join(' · ');
  }

  avatarLetter(): string {
    const n = this.headingName();
    if (n) {
      return n.charAt(0).toUpperCase();
    }
    const u = this.bundle.user;
    const fromUser = String(u?.['username'] ?? '').trim().charAt(0);
    if (fromUser) {
      return fromUser.toUpperCase();
    }
    return '#';
  }

  nestedUser(key: string): Record<string, unknown> | null {
    return this.asRecord(this.bundle.user?.[key]);
  }

  /** Router state so the group-roles banner matches this nested group (name/description). */
  groupRolesNavState(ug: Record<string, unknown>): { lxGroupId: number; lxGroupName: string; lxGroupDescription: string } {
    const id = Number(ug['id']);
    return {
      lxGroupId: Number.isFinite(id) && id > 0 ? id : 0,
      lxGroupName: String(ug['name'] ?? ''),
      lxGroupDescription: String(ug['description'] ?? ''),
    };
  }

  /** Security row from merged bundle or nested `userSecurityDto` on the user. */
  securityPolicyRecord(): Record<string, unknown> | null {
    return this.bundle.security ?? this.nestedUser('userSecurityDto');
  }

  formatDisplay(v: unknown): string {
    if (v === null || v === undefined || v === '') {
      return '—';
    }
    return String(v);
  }

  statusToneClass(v: unknown): string {
    const t = String(v ?? '').trim().toUpperCase();
    if (t === 'ACTIVE') return 'up-status--active';
    if (t === 'INACTIVE') return 'up-status--inactive';
    if (t === 'DELETED') return 'up-status--deleted';
    if (t === 'ARCHIVED') return 'up-status--archived';
    return 'up-status--neutral';
  }

  secretConfigured(v: unknown): boolean {
    return v != null && String(v).trim() !== '';
  }

  /** Whether a two-factor secret is stored (value never shown on this screen). */
  twoFactorSecretStored(s: Record<string, unknown> | null): boolean {
    if (!s) {
      return false;
    }
    return this.secretConfigured(s['twoFactorAuthSecret']);
  }

  asRecord(v: unknown): Record<string, unknown> | null {
    if (v !== null && typeof v === 'object' && !Array.isArray(v)) {
      return v as Record<string, unknown>;
    }
    return null;
  }

  canVerifyEmail(): boolean {
    const u = this.bundle.user;
    if (!u) {
      return false;
    }
    return this.usersService.needsEmailVerification(u['emailVerified']) && String(u['email'] ?? '').trim().length > 0;
  }

  canVerifyPhone(): boolean {
    const u = this.bundle.user;
    if (!u) {
      return false;
    }
    return this.isSelfProfile() && this.usersService.needsPhoneVerification(u['phoneVerified'], u['phoneNumber']);
  }

  canResendVerificationEmail(): boolean {
    const u = this.bundle.user;
    if (!u) {
      return false;
    }
    return this.usersService.canResendVerificationEmail(u['emailVerified'], u['createdAt']);
  }

  verifyPhone(): void {
    const u = this.bundle.user;
    if (!u || !this.canVerifyPhone()) {
      return;
    }
    const phone = String(u['phoneNumber'] ?? '').trim();
    this.phoneVerificationPrompt
      .openDialog({
        title: 'Verify your phone number',
        lead: `Send an SMS code to ${phone}, then enter the code below to verify your phone number.`,
      })
      .subscribe((verified) => {
        if (verified) {
          this.loadBundle();
          this.shellNotifications.refresh();
        }
      });
  }

  resendVerificationEmail(): void {
    const u = this.bundle.user;
    const email = String(u?.['email'] ?? '').trim();
    if (!email || !this.canResendVerificationEmail()) {
      return;
    }
    this.resendingVerificationEmail = true;
    this.usersService
      .resendVerificationEmail(email)
      .pipe(
        finalize(() => {
          this.resendingVerificationEmail = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (resp) => {
          if (this.usersService.isUserMutationFailure(resp)) {
            this.snackBar.open(
              this.usersService.formatUserMutationError(resp, 'Could not resend verification email.'),
              'Close',
              { duration: 5000, panelClass: ['app-snackbar-error'] },
            );
            return;
          }
          this.snackBar.open(`Verification email sent to ${email}.`, 'Close', {
            duration: 5000,
            panelClass: ['app-snackbar-success'],
          });
          this.shellNotifications.refresh();
        },
        error: () => {
          this.snackBar.open('Failed to resend verification email.', 'Close', {
            duration: 5000,
            panelClass: ['app-snackbar-error'],
          });
        },
      });
  }

  openEditProfile(): void {
    const u = this.bundle.user;
    if (!u) {
      return;
    }
    this.dialog
      .open(UserEditProfileDialogComponent, {
        width: '640px',
        maxWidth: '95vw',
        autoFocus: 'first-tabbable',
        panelClass: 'lx-location-dialog-panel',
        data: { user: u },
      })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.loadBundle();
        }
      });
  }

  openEditAddress(): void {
    const user = this.bundle.user;
    if (!user) {
      return;
    }
    const existing = this.usersService.resolveAddressRecord(user, this.bundle.address);
    const address = this.usersService.addressDraftForEdit(user, this.bundle.address);
    this.dialog
      .open(UserEditAddressDialogComponent, {
        width: '640px',
        maxWidth: '95vw',
        autoFocus: 'first-tabbable',
        panelClass: 'lx-location-dialog-panel',
        data: { address, user, createMode: !existing },
      })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.loadBundle();
        }
      });
  }

  openEditAccount(): void {
    const ac = this.bundle.account;
    if (!ac || !Number.isFinite(this.userIdNumber) || this.userIdNumber <= 0) {
      return;
    }
    this.dialog
      .open(UserEditAccountDialogComponent, {
        width: '480px',
        maxWidth: '95vw',
        panelClass: 'lx-location-dialog-panel',
        data: { account: ac, userId: this.userIdNumber },
      })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.loadBundle();
        }
      });
  }

  openEditSecurity(emphasis: 'recovery' | 'full' = 'full'): void {
    if (!Number.isFinite(this.userIdNumber) || this.userIdNumber <= 0) {
      this.snackBar.open('Could not resolve this user id.', 'Close', {
        duration: 5000,
        panelClass: ['app-snackbar-error'],
      });
      return;
    }
    const selfService = this.isSelfProfile();
    const s = this.securityPolicyRecord() ?? {};
    this.dialog
      .open(UserEditSecurityDialogComponent, {
        width: '640px',
        maxWidth: '95vw',
        autoFocus: 'first-tabbable',
        panelClass: 'lx-location-dialog-panel',
        data: { security: s, userId: this.userIdNumber, emphasis, selfService },
      })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.loadBundle();
        }
      });
  }

  /** Signed-in user viewing their own profile (uses {@code /user-security/me} when editing security). */
  private isSelfProfile(): boolean {
    const jwt = decodeJwtPayload(this.storage.getToken() ?? '');
    const fromJwt = Number(jwt?.userId ?? 0);
    if (Number.isFinite(fromJwt) && fromJwt > 0 && fromJwt === this.userIdNumber) {
      return true;
    }
    const authId = Number(this.authState.currentUser?.userId ?? 0);
    return Number.isFinite(authId) && authId > 0 && authId === this.userIdNumber;
  }

  openAssignUserGroupDialog(): void {
    if (!Number.isFinite(this.userIdNumber) || this.userIdNumber <= 0) {
      return;
    }
    const ug = this.nestedUser('userGroupDto');
    const currentGroupIdRaw = ug?.['id'];
    const currentGroupId =
      typeof currentGroupIdRaw === 'number' && Number.isFinite(currentGroupIdRaw) && currentGroupIdRaw > 0
        ? currentGroupIdRaw
        : Number(currentGroupIdRaw);
    this.dialog
      .open(UserAssignUserGroupDialogComponent, {
        width: '560px',
        maxWidth: '95vw',
        autoFocus: 'first-tabbable',
        panelClass: 'lx-location-dialog-panel',
        data: {
          userId: this.userIdNumber,
          currentGroupId: Number.isFinite(currentGroupId) && currentGroupId > 0 ? currentGroupId : null,
        },
      })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.loadBundle();
        }
      });
  }

  openDocumentDetail(row: UserFileUploadSummary): void {
    if (!row?.id) {
      return;
    }
    this.dialog.open(UserDocumentDetailDialogComponent, {
      width: '720px',
      maxWidth: '95vw',
      panelClass: 'lx-location-dialog-panel',
      data: { id: row.id },
    });
  }

  /** Linked national ID / passport rows resolved after `find-by-id` enrichment (thumbnails). */
  linkedUploadGallery(): { label: string; row: UserFileUploadSummary | null }[] {
    return this.linkedUploadRefs().map((ref) => ({
      label: ref.label,
      row: this.userDocuments.find((d) => d.id === ref.id) ?? null,
    }));
  }

  /** Sanitized PDF data URL for mini iframe previews (cached per upload id). */
  safePdfPreview(row: UserFileUploadSummary | null | undefined): SafeResourceUrl | null {
    const id = Number(row?.id ?? 0);
    const raw = row?.previewPdfDataUrl;
    if (!Number.isFinite(id) || id <= 0 || !raw) {
      return null;
    }
    let hit = this.pdfSafeUrlByUploadId.get(id);
    if (!hit) {
      hit = this.sanitizer.bypassSecurityTrustResourceUrl(raw);
      this.pdfSafeUrlByUploadId.set(id, hit);
    }
    return hit;
  }

  submitPasswordChange(): void {
    this.passwordError = '';
    if (!Number.isFinite(this.userIdNumber) || this.userIdNumber <= 0) {
      return;
    }
    const p = this.newPassword.trim();
    const c = this.confirmPassword.trim();
    if (!isLdmsPasswordValid(p)) {
      this.passwordError = LDMS_PASSWORD_INVALID_MESSAGE;
      return;
    }
    if (p !== c) {
      this.passwordError = 'Password and confirmation do not match.';
      return;
    }
    this.passwordSaving = true;
    this.usersService.changeUserPasswordForUser(this.userIdNumber, p).subscribe({
      next: () => {
        this.passwordSaving = false;
        this.newPassword = '';
        this.confirmPassword = '';
        this.snackBar.open('Password updated.', 'Close', { duration: 4000 });
        this.loadBundle();
      },
      error: (err: unknown) => {
        this.passwordSaving = false;
        this.passwordError = this.formatPasswordHttpError(err);
        this.cdr.markForCheck();
      },
    });
  }

  private formatPasswordHttpError(err: unknown): string {
    const e = err as { error?: { message?: string; errorMessages?: string[] } };
    const msgs = e?.error?.errorMessages;
    if (Array.isArray(msgs) && msgs.length) {
      return msgs.map((m) => String(m)).join(' ');
    }
    if (typeof e?.error?.message === 'string' && e.error.message.trim()) {
      return e.error.message.trim();
    }
    return 'Could not update password. Try again or check logs.';
  }

  formatBytes(n: number | undefined): string {
    if (n == null || !Number.isFinite(n) || n < 0) {
      return '—';
    }
    if (n < 1024) {
      return `${Math.round(n)} B`;
    }
    const kb = n / 1024;
    if (kb < 1024) {
      return `${kb.toFixed(1)} KB`;
    }
    return `${(kb / 1024).toFixed(2)} MB`;
  }

  /** National ID / passport upload ids stored on the user row (used to resolve file-upload rows). */
  linkedUploadRefs(): { id: number; label: string }[] {
    const u = this.bundle.user;
    if (!u) {
      return [];
    }
    const out: { id: number; label: string }[] = [];
    const add = (key: string, label: string) => {
      const n = Number(u[key] ?? 0);
      if (Number.isFinite(n) && n > 0) {
        out.push({ id: n, label });
      }
    };
    add('nationalIdUploadId', 'National ID');
    add('passportUploadId', 'Passport');
    return out;
  }

  roleChips(): string[] {
    const g = this.nestedUser('userGroupDto');
    const raw = g?.['userRoleDtoSet'] ?? g?.['userRoleDtos'];
    if (!Array.isArray(raw)) {
      return [];
    }
    const names = raw
      .map((r) => {
        if (r && typeof r === 'object' && 'role' in (r as object)) {
          return String((r as { role?: unknown }).role ?? '').trim();
        }
        return '';
      })
      .filter(Boolean);
    return [...names].sort((a, b) => a.localeCompare(b, undefined, { sensitivity: 'base' }));
  }

  filteredRoleChips(): string[] {
    const q = this.roleSearch.trim().toLowerCase();
    const all = this.roleChips();
    if (!q) {
      return all;
    }
    return all.filter((role) => role.toLowerCase().includes(q));
  }

  displayedRoleChips(): string[] {
    const filtered = this.filteredRoleChips();
    const searching = this.roleSearch.trim().length > 0;
    if (this.rolesExpanded || searching || filtered.length <= ROLES_PREVIEW_LIMIT) {
      return filtered;
    }
    return filtered.slice(0, ROLES_PREVIEW_LIMIT);
  }

  showRoleSearch(): boolean {
    return this.roleChips().length > 4;
  }

  canExpandRoles(): boolean {
    if (this.roleSearch.trim()) {
      return false;
    }
    return this.filteredRoleChips().length > ROLES_PREVIEW_LIMIT && !this.rolesExpanded;
  }

  canCollapseRoles(): boolean {
    return this.rolesExpanded && this.filteredRoleChips().length > ROLES_PREVIEW_LIMIT;
  }

  onRoleSearchChange(): void {
    if (this.filteredRoleChips().length <= ROLES_PREVIEW_LIMIT) {
      this.rolesExpanded = false;
    }
    this.cdr.markForCheck();
  }

  toggleRolesExpanded(): void {
    this.rolesExpanded = !this.rolesExpanded;
    this.cdr.markForCheck();
  }

  trackRoleChip(_index: number, role: string): string {
    return role;
  }

  retryLoad(): void {
    if (!Number.isFinite(this.userIdNumber) || this.userIdNumber <= 0) {
      return;
    }
    this.loadBundle();
  }

  private applyRouteSnapshot(): void {
    this.userId = this.findParamInTree('userId') ?? '';
    this.userIdNumber = Number(this.userId);
    this.section = this.resolveSection();
    const sectionTitle = this.browserSectionTitle(this.section);
    const who =
      this.headingName() ?? `User ${this.userId}`;
    const dot = ' · ';
    const suffix = ' | LX Platform';
    this.title.setTitle(`${sectionTitle}${dot}${who}${suffix}`);
  }

  private browserSectionTitle(s: UserSection): string {
    switch (s) {
      case 'profile':
        return 'Profile';
      case 'account':
        return 'Account';
      case 'preferences':
        return 'Preferences';
      case 'security-policies':
        return 'Security';
      case 'addresses':
        return 'Addresses';
      case 'password':
        return 'Password';
      default:
        return 'User';
    }
  }

  private findParamInTree(param: string): string | null {
    let r: ActivatedRoute | null = this.route;
    while (r) {
      const v = r.snapshot.paramMap.get(param);
      if (v) {
        return v;
      }
      r = r.parent;
    }
    return null;
  }

  private resolveSection(): UserSection {
    const fromData = this.route.snapshot.data['section'] as UserSection | undefined;
    if (fromData && this.isUserSection(fromData)) {
      return fromData;
    }
    const paths = this.collectUrlPaths(this.route);
    for (const p of paths) {
      if (this.isUserSection(p)) {
        return p;
      }
    }
    return 'profile';
  }

  private collectUrlPaths(route: ActivatedRoute): string[] {
    const out: string[] = [];
    let r: ActivatedRoute | null = route;
    while (r) {
      for (const seg of r.snapshot.url) {
        if (seg.path) {
          out.push(seg.path);
        }
      }
      r = r.parent;
    }
    return out;
  }

  private isUserSection(v: string): v is UserSection {
    return (
      v === 'profile' ||
      v === 'account' ||
      v === 'preferences' ||
      v === 'security-policies' ||
      v === 'addresses' ||
      v === 'password'
    );
  }

  private resetRolesPanel(): void {
    this.roleSearch = '';
    this.rolesExpanded = false;
  }

  private loadBundle(): void {
    this.loadError = '';
    if (!Number.isFinite(this.userIdNumber) || this.userIdNumber <= 0) {
      this.loading = false;
      this.loadError = 'invalidId';
      this.bundle = { user: null, account: null, security: null, address: null, password: null };
      this.userDocuments = [];
      this.cdr.markForCheck();
      return;
    }

    this.loading = true;
    this.pdfSafeUrlByUploadId.clear();
    const profile$ = this.isSelfProfile()
      ? this.usersService.getMyAccountProfileBundle().pipe(
          switchMap((selfBundle) =>
            selfBundle.user
              ? this.usersService.enrichUserProfileBundle(selfBundle, this.userIdNumber)
              : this.usersService.getUserProfileBundle(this.userIdNumber),
          ),
        )
      : this.usersService.getUserProfileBundle(this.userIdNumber);

    profile$
      .pipe(
        switchMap((bundle) => {
          const needAccount = !bundle.account;
          const needSecurity = !bundle.security;
          return forkJoin({
            account: needAccount ? this.usersService.findUserAccountByUserId(this.userIdNumber) : of(null),
            security: needSecurity ? this.usersService.findUserSecurityByUserId(this.userIdNumber) : of(null),
          }).pipe(
            switchMap(({ account, security }) => {
              const merged: UserProfileBundle = {
                ...bundle,
                account: bundle.account ?? account,
                security: bundle.security ?? security,
              };
              return this.usersService.listUserFileUploadsForProfile(this.userIdNumber, merged).pipe(
                map((uploads) => ({ bundle: merged, uploads })),
                catchError(() => of({ bundle: merged, uploads: [] as UserFileUploadSummary[] })),
              );
            }),
          );
        }),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: ({ bundle, uploads }) => {
          this.bundle = bundle;
          this.userDocuments = uploads;
          this.resetRolesPanel();
          if (!bundle.user) {
            this.loadError = 'missingUser';
          }
          this.maybeRunVerifyAction();
        },
        error: () => {
          this.bundle = { user: null, account: null, security: null, address: null, password: null };
          this.userDocuments = [];
          this.loadError = 'requestFailed';
        },
      });
  }

  private maybeRunVerifyAction(): void {
    if (!this.pendingVerifyAction || this.loading || !this.bundle.user || !this.isSelfProfile()) {
      return;
    }

    const action = this.pendingVerifyAction;
    this.pendingVerifyAction = null;
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { verify: null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });

    if (action === 'phone') {
      if (this.canVerifyPhone()) {
        this.verifyPhone();
      }
      return;
    }

    if (this.canResendVerificationEmail()) {
      this.resendVerificationEmail();
      return;
    }
    if (this.canVerifyEmail()) {
      this.snackBar.open('Check your inbox for the verification link we sent when you registered.', 'Close', {
        duration: 6000,
      });
    }
  }
}
