import { ChangeDetectorRef, Component, NgZone, OnInit, Type } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { MatDialog, MatDialogConfig, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, finalize, switchMap } from 'rxjs/operators';
import { Observable, of } from 'rxjs';
import { decodeJwtPayload } from '../../../../core/utils/jwt.util';
import { UsersPortalService, UserProfileBundle } from '../../../users/services/users-portal.service';
import { StorageService } from '../../../../core/services/storage.service';
import { ShellUserService } from '../../../../core/services/shell-user.service';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { UserEditProfileDialogComponent } from '../../../users/components/user-edit-profile-dialog/user-edit-profile-dialog.component';
import { UserEditAccountDialogComponent } from '../../../users/components/user-edit-account-dialog/user-edit-account-dialog.component';
import { UserEditAddressDialogComponent } from '../../../users/components/user-edit-address-dialog/user-edit-address-dialog.component';
import { UserEditSecurityDialogComponent } from '../../../users/components/user-edit-security-dialog/user-edit-security-dialog.component';
import { TwoFactorSetupDialogComponent } from '../../../users/components/two-factor-setup-dialog/two-factor-setup-dialog.component';
import {
  formatEntityStatusLabel,
  formatGenderLabel,
  formatIsoDateForDisplay,
  formatIsoDateTimeForDisplay,
  formatSecurityQuestionLabel,
  resolveUserRoleLabel,
  shellRoleSummary,
} from '../../../../core/utils/field-display.util';

@Component({
  selector: 'app-my-account',
  templateUrl: './my-account.component.html',
  styleUrl: './my-account.component.scss',
  standalone: false,
})
export class MyAccountComponent implements OnInit {
  loading = true;
  loadError = '';
  resolvingSection = '';
  userId = 0;
  bundle: UserProfileBundle = {
    user: null,
    account: null,
    security: null,
    address: null,
    password: null,
  };

  private readonly editDialogConfig: MatDialogConfig = {
    width: '720px',
    maxWidth: '96vw',
    maxHeight: '90vh',
    autoFocus: false,
    hasBackdrop: true,
    panelClass: 'lx-location-dialog-panel',
    backdropClass: 'cdk-overlay-dark-backdrop',
  };

  constructor(
    private readonly title: Title,
    private readonly usersService: UsersPortalService,
    private readonly storage: StorageService,
    private readonly currentUser: ShellUserService,
    private readonly authState: AuthStateService,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
    private readonly ngZone: NgZone,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('My Account | LX Platform');
    this.loadMyAccount();
  }

  headingName(): string {
    const user = this.bundle.user;
    if (!user) {
      const shell = this.currentUser.snapshot;
      if (shell?.displayName) {
        return shell.displayName;
      }
      return 'My Account';
    }
    const first = String(user['firstName'] ?? '').trim();
    const last = String(user['lastName'] ?? '').trim();
    const full = `${first} ${last}`.trim();
    return full || String(user['username'] ?? 'User').trim();
  }

  heroEmail(): string {
    const fromUser = this.bundle.user?.['email'];
    if (fromUser != null && String(fromUser).trim()) {
      return String(fromUser);
    }
    return this.currentUser.snapshot?.email ?? '';
  }

  heroRole(): string {
    const fromProfile = resolveUserRoleLabel(this.bundle.user ?? undefined);
    if (fromProfile) {
      return fromProfile;
    }
    const shell = this.currentUser.snapshot?.role;
    if (shell && !shell.includes(',')) {
      return shell;
    }
    return shellRoleSummary(
      this.authState.currentUser?.roleLabel,
      this.authState.currentUser?.orgClassification,
    );
  }

  heroOrgName(): string {
    return this.authState.currentUser?.orgName?.trim() ?? '';
  }

  initials(): string {
    const user = this.bundle.user;
    const first = String(user?.['firstName'] ?? this.currentUser.snapshot?.firstName ?? '').trim();
    const last = String(user?.['lastName'] ?? this.currentUser.snapshot?.lastName ?? '').trim();
    const f = first.charAt(0);
    const l = last.charAt(0);
    if (f && l) {
      return `${f}${l}`.toUpperCase();
    }
    if (f) {
      return f.toUpperCase();
    }
    return this.currentUser.snapshot?.initials ?? 'U';
  }

  formatDisplay(v: unknown): string {
    if (v === null || v === undefined || String(v).trim() === '') {
      return '—';
    }
    return String(v);
  }

  formatDate(v: unknown): string {
    return formatIsoDateForDisplay(v);
  }

  formatDateTime(v: unknown): string {
    return formatIsoDateTimeForDisplay(v);
  }

  formatGender(v: unknown): string {
    return formatGenderLabel(v);
  }

  formatStatus(v: unknown): string {
    return formatEntityStatusLabel(v);
  }

  formatSecurityQuestion(v: unknown): string {
    return formatSecurityQuestionLabel(v);
  }

  accountStatusLabel(): string {
    const ac = this.bundle.account;
    if (!ac) {
      return '—';
    }
    return formatEntityStatusLabel(ac['entityStatus'] ?? ac['accountStatus']);
  }

  formatBoolean(v: unknown): string {
    if (v === true) {
      return 'Yes';
    }
    if (v === false) {
      return 'No';
    }
    return this.formatDisplay(v);
  }

  twoFactorMethodLabel(): string {
    if (this.bundle.security?.['isTwoFactorEnabled'] !== true) {
      return '—';
    }
    const method = String(this.bundle.security?.['twoFactorMethod'] ?? '').trim().toUpperCase();
    if (method === 'AUTHENTICATOR_APP') {
      return 'Authenticator app';
    }
    if (method === 'SMS') {
      return 'SMS';
    }
    return 'SMS';
  }

  hasAccount(): boolean {
    return Number(this.bundle.account?.['id'] ?? 0) > 0;
  }

  hasAddress(): boolean {
    return Number(this.bundle.address?.['id'] ?? 0) > 0;
  }

  refresh(): void {
    this.loadMyAccount();
  }

  /** Link to the full profile workspace (same as users list “View profile”). */
  profileWorkspaceLink(): string[] | null {
    if (!Number.isFinite(this.userId) || this.userId <= 0) {
      return null;
    }
    return ['/users', String(this.userId), 'profile'];
  }

  openEditProfile(): void {
    if (!this.bundle.user) {
      return;
    }
    this.openEditDialog(
      UserEditProfileDialogComponent,
      { user: this.bundle.user, scope: 'profile-only' as const },
      (saved) => {
        if (!saved) {
          return;
        }
        this.loadMyAccount();
        this.currentUser.refreshFromApi().subscribe({
          next: () => this.cdr.markForCheck(),
        });
      },
    );
  }

  openEditAccount(): void {
    if (!Number.isFinite(this.userId) || this.userId <= 0) {
      this.snackBar.open('Could not resolve your user id.', 'Close', { duration: 4000 });
      return;
    }
    this.resolvingSection = 'account';
    this.usersService
      .resolveAccountRecord(this.bundle.user, this.userId)
      .pipe(
        finalize(() => {
          this.resolvingSection = '';
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (account) => {
          if (!account || Number(account['id'] ?? 0) <= 0) {
            this.snackBar.open('No account record is linked to your profile yet.', 'Close', {
              duration: 5000,
              panelClass: ['app-snackbar-error'],
            });
            return;
          }
          this.bundle = { ...this.bundle, account };
          this.openEditDialog(
            UserEditAccountDialogComponent,
            { account, userId: this.userId, selfService: true },
            (saved) => saved && this.loadMyAccount(),
          );
        },
        error: () => {
          this.snackBar.open('Could not load account details to edit.', 'Close', {
            duration: 5000,
            panelClass: ['app-snackbar-error'],
          });
        },
      });
  }

  openEditAddress(): void {
    const user = this.bundle.user;
    if (!user) {
      return;
    }
    const existing = this.usersService.resolveAddressRecord(user, this.bundle.address);
    const address = this.usersService.addressDraftForEdit(user, this.bundle.address);
    this.openEditDialog(
      UserEditAddressDialogComponent,
      {
        address,
        user,
        createMode: !existing,
      },
      (saved) => saved && this.loadMyAccount(),
    );
  }

  openEditSecurity(): void {
    const userId = this.resolveEditableUserId();
    const security = this.resolveSecurityRecordForEdit();
    this.openEditDialog(
      UserEditSecurityDialogComponent,
      {
        security,
        userId: userId > 0 ? userId : 0,
        emphasis: 'recovery' as const,
        selfService: true,
      },
      (saved) => saved && this.loadMyAccount(),
    );
  }

  openManageTwoFactor(): void {
    this.openEditDialog(
      TwoFactorSetupDialogComponent,
      {
        security: this.resolveSecurityRecordForEdit(),
        user: this.bundle.user,
      },
      (saved) => saved && this.loadMyAccount(),
    );
  }

  /**
   * Opens a user edit dialog on the Angular zone and surfaces constructor failures
   * (e.g. null dialog data) instead of failing silently.
   */
  private openEditDialog<T, R = boolean>(
    component: Type<T>,
    data: unknown,
    onClosed?: (saved: R | undefined) => void,
  ): void {
    this.ngZone.run(() => {
      let ref: MatDialogRef<T, R>;
      try {
        ref = this.dialog.open(component, {
          ...this.editDialogConfig,
          data,
        });
      } catch (err) {
        console.error('[MyAccount] Failed to open edit dialog', component.name, err);
        this.snackBar.open('Could not open the editor. Refresh the page and try again.', 'Close', {
          duration: 5000,
          panelClass: ['app-snackbar-error'],
        });
        return;
      }
      ref.afterClosed().subscribe((saved) => {
        if (onClosed) {
          onClosed(saved);
        }
      });
    });
  }

  private resolveSecurityRecordForEdit(): Record<string, unknown> {
    const direct = this.bundle.security;
    if (direct && typeof direct === 'object' && !Array.isArray(direct)) {
      return direct;
    }
    const user = this.bundle.user;
    if (user && typeof user === 'object') {
      for (const key of ['userSecurityDto', 'userSecurity']) {
        const nested = user[key];
        if (nested && typeof nested === 'object' && !Array.isArray(nested)) {
          return nested as Record<string, unknown>;
        }
      }
    }
    return {};
  }

  private resolveEditableUserId(): number {
    if (Number.isFinite(this.userId) && this.userId > 0) {
      return this.userId;
    }
    const fromUser = Number(this.bundle.user?.['id'] ?? 0);
    if (Number.isFinite(fromUser) && fromUser > 0) {
      return fromUser;
    }
    const fromAuth = Number(this.authState.currentUser?.userId ?? 0);
    if (Number.isFinite(fromAuth) && fromAuth > 0) {
      return fromAuth;
    }
    const jwt = decodeJwtPayload(this.storage.getToken() ?? '');
    const fromJwt = Number(jwt?.userId ?? 0);
    return Number.isFinite(fromJwt) && fromJwt > 0 ? fromJwt : 0;
  }

  private loadMyAccount(): void {
    const token = this.storage.getToken() ?? '';
    if (token.startsWith('mock-token-') || token.startsWith('mock.')) {
      this.loading = false;
      this.loadError =
        'My Account needs a real signed-in session. Sign in with your LDMS credentials (not demo mode) to view and edit your profile.';
      this.cdr.markForCheck();
      return;
    }

    this.loading = true;
    this.loadError = '';

    this.currentUser
      .refreshFromApi()
      .pipe(
        switchMap(() => this.resolveMyProfileBundle()),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (bundle) => {
          this.bundle = bundle;
          if (!bundle.user) {
            this.loadError = 'Your profile could not be loaded. Try refreshing or signing in again.';
          }
        },
        error: () => {
          this.loadError = 'Failed to load your profile details.';
          this.snackBar.open(this.loadError, 'Close', { duration: 5000, panelClass: ['app-snackbar-error'] });
        },
      });
  }

  /** Signed-in user's own profile — uses {@code GET /user/me} (no admin lookup role required). */
  private resolveMyProfileBundle(): Observable<UserProfileBundle> {
    const jwt = decodeJwtPayload(this.storage.getToken() ?? '');
    const username = String(jwt?.sub ?? '').trim();

    return this.usersService.getMyAccountProfileBundle().pipe(
      switchMap((bundle) => this.continueProfileResolution(bundle, username)),
      catchError(() =>
        username
          ? this.usersService
              .getUserProfileBundleByUsername(username)
              .pipe(switchMap((fallback) => this.enrichIfFound(fallback, Number(fallback.user?.['id'] ?? 0))))
          : of(this.emptyBundle()),
      ),
    );
  }

  private continueProfileResolution(
    bundle: UserProfileBundle,
    username: string,
  ): Observable<UserProfileBundle> {
    const jwt = decodeJwtPayload(this.storage.getToken() ?? '');
    const resolvedId = Number(bundle.user?.['id'] ?? jwt?.userId ?? 0);
    if (bundle.user && Number.isFinite(resolvedId) && resolvedId > 0) {
      this.userId = resolvedId;
      return this.usersService.enrichUserProfileBundle(bundle, resolvedId);
    }
    if (username) {
      return this.usersService
        .getUserProfileBundleByUsername(username)
        .pipe(switchMap((fallback) => this.enrichIfFound(fallback, Number(fallback.user?.['id'] ?? 0))));
    }
    return of(bundle);
  }

  private enrichIfFound(bundle: UserProfileBundle, userId: number): Observable<UserProfileBundle> {
    if (Number.isFinite(userId) && userId > 0) {
      this.userId = userId;
    }
    if (!bundle.user) {
      return of(bundle);
    }
    return this.usersService.enrichUserProfileBundle(bundle, this.userId);
  }

  private emptyBundle(): UserProfileBundle {
    return {
      user: null,
      account: null,
      security: null,
      address: null,
      password: null,
    };
  }
}
