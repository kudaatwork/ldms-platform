import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';
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
  saving = false;
  userId = 0;
  bundle: UserProfileBundle = {
    user: null,
    account: null,
    security: null,
    address: null,
    password: null,
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
    this.resolveCurrentUserIdAndLoad();
  }

  headingName(): string {
    const user = this.bundle.user;
    if (!user) {
      return 'My Account';
    }
    const first = String(user['firstName'] ?? '').trim();
    const last = String(user['lastName'] ?? '').trim();
    const full = `${first} ${last}`.trim();
    return full || String(user['username'] ?? 'User').trim();
  }

  initials(): string {
    const user = this.bundle.user;
    const first = String(user?.['firstName'] ?? '').trim();
    const last = String(user?.['lastName'] ?? '').trim();
    const f = first.charAt(0);
    const l = last.charAt(0);
    if (f && l) {
      return `${f}${l}`.toUpperCase();
    }
    if (f) {
      return f.toUpperCase();
    }
    return 'U';
  }

  formatDisplay(v: unknown): string {
    if (v === null || v === undefined || String(v).trim() === '') {
      return '—';
    }
    return String(v);
  }

  refresh(): void {
    this.loadBundle();
  }

  openEditProfile(): void {
    if (!this.bundle.user) {
      return;
    }
    this.dialog
      .open(UserEditProfileDialogComponent, {
        width: '680px',
        maxWidth: '96vw',
        panelClass: 'lx-location-dialog-panel',
        data: { user: this.bundle.user },
      })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.loadBundle();
          this.currentUser.refreshFromApi().subscribe();
        }
      });
  }

  openEditAccount(): void {
    if (!this.bundle.account) {
      return;
    }
    this.dialog
      .open(UserEditAccountDialogComponent, {
        width: '520px',
        maxWidth: '95vw',
        panelClass: 'lx-location-dialog-panel',
        data: { account: this.bundle.account, userId: this.userId },
      })
      .afterClosed()
      .subscribe((saved) => saved && this.loadBundle());
  }

  openEditAddress(): void {
    if (!this.bundle.address) {
      return;
    }
    this.dialog
      .open(UserEditAddressDialogComponent, {
        width: '680px',
        maxWidth: '96vw',
        panelClass: 'lx-location-dialog-panel',
        data: { address: this.bundle.address },
      })
      .afterClosed()
      .subscribe((saved) => saved && this.loadBundle());
  }

  openEditSecurity(): void {
    this.dialog
      .open(UserEditSecurityDialogComponent, {
        width: '620px',
        maxWidth: '95vw',
        panelClass: 'lx-location-dialog-panel',
        data: {
          security: this.bundle.security ?? {},
          userId: this.userId,
          emphasis: 'full',
        },
      })
      .afterClosed()
      .subscribe((saved) => saved && this.loadBundle());
  }

  private resolveCurrentUserIdAndLoad(): void {
    const storedId = Number(this.storage.getUser()?.id ?? 0);
    if (Number.isFinite(storedId) && storedId > 0) {
      this.userId = storedId;
      this.loadBundle();
      return;
    }
    this.currentUser
      .refreshFromApi()
      .pipe(
        finalize(() => {
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          const refreshedId = Number(this.storage.getUser()?.id ?? 0);
          if (Number.isFinite(refreshedId) && refreshedId > 0) {
            this.userId = refreshedId;
            this.loadBundle();
            return;
          }
          this.loading = false;
          this.loadError = 'Could not resolve your profile ID from session.';
        },
        error: () => {
          this.loading = false;
          this.loadError = 'Failed to load your session profile.';
        },
      });
  }

  private loadBundle(): void {
    if (!Number.isFinite(this.userId) || this.userId <= 0) {
      this.loading = false;
      this.loadError = 'Invalid account profile.';
      return;
    }
    this.loading = true;
    this.loadError = '';
    this.usersService
      .getUserProfileBundle(this.userId)
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (bundle) => {
          this.bundle = bundle;
          if (!bundle.user) {
            this.loadError = 'Your profile could not be loaded.';
          }
        },
        error: () => {
          this.loadError = 'Failed to load your profile details.';
          this.snackBar.open(this.loadError, 'Close', { duration: 5000, panelClass: ['app-snackbar-error'] });
        },
      });
  }
}
