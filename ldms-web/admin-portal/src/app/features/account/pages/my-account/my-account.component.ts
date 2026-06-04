import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, finalize, switchMap } from 'rxjs/operators';
import { Observable, of } from 'rxjs';
import { decodeJwtPayload } from '../../../../core/utils/jwt.util';
import { UsersAdminService, UserProfileBundle } from '../../../users/services/users-admin.service';
import { StorageService } from '../../../../core/services/storage.service';
import { CurrentUserService } from '../../../../core/services/current-user.service';
import { UserEditProfileDialogComponent } from '../../../users/components/user-edit-profile-dialog/user-edit-profile-dialog.component';
import { UserEditAccountDialogComponent } from '../../../users/components/user-edit-account-dialog/user-edit-account-dialog.component';
import { UserEditAddressDialogComponent } from '../../../users/components/user-edit-address-dialog/user-edit-address-dialog.component';
import { UserEditSecurityDialogComponent } from '../../../users/components/user-edit-security-dialog/user-edit-security-dialog.component';

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
    autoFocus: 'first-tabbable',
    panelClass: 'lx-location-dialog-panel',
  };

  constructor(
    private readonly title: Title,
    private readonly usersService: UsersAdminService,
    private readonly storage: StorageService,
    private readonly currentUser: CurrentUserService,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('My Account | LX Admin');
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
    return this.currentUser.snapshot?.role ?? 'User';
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

  formatBoolean(v: unknown): string {
    if (v === true) {
      return 'Yes';
    }
    if (v === false) {
      return 'No';
    }
    return this.formatDisplay(v);
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

  openEditProfile(): void {
    if (!this.bundle.user) {
      return;
    }
    this.dialog
      .open(UserEditProfileDialogComponent, {
        ...this.editDialogConfig,
        data: { user: this.bundle.user, scope: 'profile-only' },
      })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.loadMyAccount();
          this.currentUser.refreshFromApi().subscribe();
        }
      });
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
          this.dialog
            .open(UserEditAccountDialogComponent, {
              ...this.editDialogConfig,
              data: { account, userId: this.userId, selfService: true },
            })
            .afterClosed()
            .subscribe((saved) => saved && this.loadMyAccount());
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
    this.dialog
      .open(UserEditAddressDialogComponent, {
        ...this.editDialogConfig,
        data: {
          address,
          user,
          createMode: !existing,
        },
      })
      .afterClosed()
      .subscribe((saved) => saved && this.loadMyAccount());
  }

  openEditSecurity(): void {
    if (!Number.isFinite(this.userId) || this.userId <= 0) {
      return;
    }
    this.dialog
      .open(UserEditSecurityDialogComponent, {
        ...this.editDialogConfig,
        data: {
          security: this.bundle.security ?? {},
          userId: this.userId,
          emphasis: 'full',
        },
      })
      .afterClosed()
      .subscribe((saved) => saved && this.loadMyAccount());
  }

  private loadMyAccount(): void {
    const token = this.storage.getToken() ?? '';
    if (token.startsWith('mock-token-')) {
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

  /** Prefer backoffice find-by-id (no role gate); fall back to /me and username lookup. */
  private resolveMyProfileBundle(): Observable<UserProfileBundle> {
    const stored = this.storage.getUser();
    const jwt = decodeJwtPayload(this.storage.getToken() ?? '');
    const storedId = Number(stored?.id ?? jwt?.userId ?? 0);
    const username = String(stored?.username ?? jwt?.sub ?? '').trim();

    if (Number.isFinite(storedId) && storedId > 0) {
      this.userId = storedId;
      return this.usersService
        .getUserProfileBundle(storedId)
        .pipe(switchMap((bundle) => this.enrichIfFound(bundle, storedId)));
    }

    return this.usersService.getMyAccountProfileBundle().pipe(
      switchMap((bundle) => this.continueProfileResolution(bundle, username)),
      catchError(() =>
        username
          ? this.usersService
              .getUserProfileBundleByUsername(username)
              .pipe(switchMap((bundle) => this.enrichIfFound(bundle, Number(bundle.user?.['id'] ?? 0))))
          : of(this.emptyBundle()),
      ),
    );
  }

  private continueProfileResolution(
    bundle: UserProfileBundle,
    username: string,
  ): Observable<UserProfileBundle> {
    const resolvedId = Number(bundle.user?.['id'] ?? 0);
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
