import { CommonModule } from '@angular/common';
import {
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterModule } from '@angular/router';
import { Subject, finalize, takeUntil } from 'rxjs';
import { UserEditAddressDialogComponent } from '../../../features/users/components/user-edit-address-dialog/user-edit-address-dialog.component';
import { UserEditProfileDialogComponent } from '../../../features/users/components/user-edit-profile-dialog/user-edit-profile-dialog.component';
import {
  UsersAdminService,
  type UserProfileBundle,
} from '../../../features/users/services/users-admin.service';
import { UsersModule } from '../../../features/users/users.module';

@Component({
  selector: 'app-organization-contact-person-panel',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatDialogModule,
    UsersModule,
  ],
  templateUrl: './organization-contact-person-panel.component.html',
  styleUrl: './organization-contact-person-panel.component.scss',
})
export class OrganizationContactPersonPanelComponent implements OnChanges, OnDestroy {
  @Input() userId: number | null = null;
  @Input() organizationId = 0;

  @Output() userProfileUpdated = new EventEmitter<void>();

  loading = false;
  loadError = '';
  bundle: UserProfileBundle = {
    user: null,
    account: null,
    security: null,
    address: null,
    password: null,
  };

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly usersService: UsersAdminService,
    private readonly dialog: MatDialog,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['userId']) {
      this.reload();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get hasLinkedUser(): boolean {
    return this.userId != null && this.userId > 0;
  }

  reload(): void {
    const id = this.userId;
    if (!id || id <= 0) {
      this.bundle = { user: null, account: null, security: null, address: null, password: null };
      this.loadError = '';
      this.loading = false;
      this.cdr.markForCheck();
      return;
    }
    this.loading = true;
    this.loadError = '';
    this.usersService
      .getUserProfileBundle(id)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (bundle) => {
          if (!bundle.user) {
            this.bundle = bundle;
            this.loadError = 'missingUser';
            return;
          }
          this.bundle = bundle;
          this.loadError = '';
        },
        error: () => {
          this.bundle = { user: null, account: null, security: null, address: null, password: null };
          this.loadError = 'requestFailed';
        },
      });
  }

  userProfileRoute(): (string | number)[] {
    const id = this.userId;
    return id && id > 0 ? ['/users', id, 'profile'] : ['/users'];
  }

  userAddressRoute(): (string | number)[] {
    const id = this.userId;
    return id && id > 0 ? ['/users', id, 'addresses'] : ['/users'];
  }

  headingName(): string {
    const u = this.bundle.user;
    if (!u) {
      return '';
    }
    const full = `${String(u['firstName'] ?? '').trim()} ${String(u['lastName'] ?? '').trim()}`.trim();
    return full || String(u['username'] ?? '').trim();
  }

  formatDisplay(v: unknown): string {
    if (v === null || v === undefined || v === '') {
      return '—';
    }
    return String(v);
  }

  nestedUser(key: string): Record<string, unknown> | null {
    const v = this.bundle.user?.[key];
    if (v !== null && typeof v === 'object' && !Array.isArray(v)) {
      return v as Record<string, unknown>;
    }
    return null;
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
          this.onSaved();
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
          this.onSaved();
        }
      });
  }

  private onSaved(): void {
    this.reload();
    this.userProfileUpdated.emit();
  }
}
