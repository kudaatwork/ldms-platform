import { ChangeDetectorRef, Component, Inject, OnInit, Optional } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, finalize, of } from 'rxjs';
import { UsersAdminService } from '../../services/users-admin.service';

export type TwoFactorMethodId = 'SMS' | 'AUTHENTICATOR_APP';

export interface TwoFactorSetupDialogData {
  security?: Record<string, unknown> | null;
  user?: Record<string, unknown> | null;
  /** When set, admin manages this user's 2FA via backoffice APIs. */
  targetUserId?: number;
  targetUserLabel?: string;
  adminMode?: boolean;
}

type Step = 'status' | 'choose' | 'authenticator' | 'disable';

@Component({
  selector: 'app-two-factor-setup-dialog',
  templateUrl: './two-factor-setup-dialog.component.html',
  styleUrl: './two-factor-setup-dialog.component.scss',
  standalone: false,
})
export class TwoFactorSetupDialogComponent implements OnInit {
  step: Step = 'status';
  loading = false;
  error = '';

  isEnabled = false;
  currentMethod: TwoFactorMethodId | null = null;
  phoneVerified = false;
  hasPhone = false;

  otp = '';
  setupSecret = '';
  setupOtpAuthUri = '';
  setupQrCodeDataUrl = '';
  private changingMethod = false;
  readonly adminMode: boolean;
  readonly targetUserId: number;
  readonly targetUserLabel: string;

  constructor(
    private readonly dialogRef: MatDialogRef<TwoFactorSetupDialogComponent, boolean>,
    private readonly usersPortal: UsersAdminService,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
    @Optional() @Inject(MAT_DIALOG_DATA) data: TwoFactorSetupDialogData | null,
  ) {
    const safeData = data ?? {};
    this.adminMode = safeData.adminMode === true && Number(safeData.targetUserId ?? 0) > 0;
    this.targetUserId = Number(safeData.targetUserId ?? 0);
    this.targetUserLabel = String(safeData.targetUserLabel ?? '').trim();
    this.applyUserContext(safeData.user);
    this.applySecurityContext(safeData.security);
  }

  ngOnInit(): void {
    const security$ = this.adminMode
      ? this.usersPortal.findUserSecurityByUserId(this.targetUserId)
      : this.usersPortal.getMyUserSecurity();
    security$.subscribe({
      next: (row) => {
        if (row) {
          this.applySecurityContext(row);
        }
        this.step = this.isEnabled ? 'status' : 'choose';
        this.cdr.markForCheck();
      },
      error: () => {
        this.step = this.isEnabled ? 'status' : 'choose';
        this.cdr.markForCheck();
      },
    });
  }

  close(): void {
    this.dialogRef.close(false);
  }

  stepSubtitle(): string {
    const subject = this.adminMode
      ? this.targetUserLabel
        ? `${this.targetUserLabel}'s sign-in verification`
        : 'This user\'s sign-in verification'
      : 'your sign-in verification';
    switch (this.step) {
      case 'status':
        return this.adminMode
          ? `Manage how ${subject} works. Change the method or turn two-step verification off when the user has requested it.`
          : 'Manage how you verify sign-in. You can change the method or turn two-step verification off.';
      case 'choose':
        return this.adminMode
          ? `Choose SMS or an authenticator app for ${subject}.`
          : 'Pick SMS or an authenticator app. You can switch methods later from My Account.';
      case 'authenticator':
        return this.adminMode
          ? 'Show the QR code to the user, then enter the code from their authenticator app to finish.'
          : 'Set up your authenticator app, then confirm with a one-time code.';
      case 'disable':
        if (this.adminMode) {
          return this.changingMethod
            ? 'Turn off the current method so you can assign a new one.'
            : 'Admin override: turn off two-step verification for this user.';
        }
        return this.changingMethod
          ? 'Verify your current method so you can choose a new one.'
          : 'Confirm with a code to turn off two-step verification.';
      default:
        return '';
    }
  }

  methodLabel(method: TwoFactorMethodId | null): string {
    if (method === 'AUTHENTICATOR_APP') {
      return 'Authenticator app';
    }
    if (method === 'SMS') {
      return 'SMS';
    }
    return '—';
  }

  secondaryActionLabel(): string {
    if (this.step === 'status') {
      return 'Close';
    }
    if (this.step === 'choose') {
      return 'Cancel';
    }
    if (this.step === 'disable') {
      return this.isEnabled && !this.changingMethod ? 'Back' : 'Back';
    }
    return 'Back';
  }

  primaryActionLabel(): string | null {
    if (this.step === 'status') {
      return null;
    }
    if (this.step === 'choose') {
      return null;
    }
    if (this.step === 'authenticator') {
      return 'Enable';
    }
    if (this.step === 'disable') {
      return 'Turn off';
    }
    return null;
  }

  primaryActionEnabled(): boolean {
    if (this.step === 'authenticator') {
      return /^\d{6}$/.test(this.otp.trim());
    }
    if (this.step === 'disable') {
      return this.adminMode || /^\d{6}$/.test(this.otp.trim());
    }
    return true;
  }

  onSecondaryAction(): void {
    if (this.step === 'status' || this.step === 'choose') {
      this.close();
      return;
    }
    if (this.step === 'disable') {
      this.error = '';
      this.otp = '';
      this.changingMethod = false;
      this.step = this.isEnabled ? 'status' : 'choose';
      this.cdr.markForCheck();
      return;
    }
    this.backToChoose();
  }

  onPrimaryAction(): void {
    if (this.step === 'authenticator') {
      this.confirmAuthenticator();
      return;
    }
    if (this.step === 'disable') {
      this.confirmDisable();
    }
  }

  changeMethod(): void {
    this.changingMethod = true;
    this.startDisable();
  }

  startDisable(): void {
    this.error = '';
    this.otp = '';
    this.step = 'disable';
    if (!this.adminMode && this.currentMethod === 'SMS') {
      this.requestDisableOtp();
    }
    this.cdr.markForCheck();
  }

  chooseMethod(method: TwoFactorMethodId): void {
    this.error = '';
    if (method === 'SMS') {
      if (!this.hasPhone) {
        this.error = this.adminMode
          ? 'This user needs a phone number on their profile before SMS two-step verification can be enabled.'
          : 'Add a phone number to your profile before enabling SMS two-step verification.';
        return;
      }
      if (!this.phoneVerified) {
        this.error = this.adminMode
          ? 'Verify this user\'s phone number first, then enable SMS two-step verification.'
          : 'Verify your phone number first (you may be prompted after sign-in), then enable SMS two-step verification.';
        return;
      }
      this.enableSms();
      return;
    }
    this.beginAuthenticatorSetup();
  }

  beginAuthenticatorSetup(): void {
    this.loading = true;
    this.error = '';
    const begin$ = this.adminMode
      ? this.usersPortal.adminBeginAuthenticatorSetup(this.targetUserId)
      : this.usersPortal.beginMyAuthenticatorSetup();
    begin$
      .pipe(
        catchError((err) => {
          this.error = this.formatHttpError(err);
          return of(null);
        }),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe((result) => {
        if (!result) {
          if (!this.error) {
            this.error =
              'Could not start authenticator setup. Restart ldms-user-management and ensure database migration V19 has run.';
          }
          return;
        }
        this.setupSecret = result.secret;
        this.setupOtpAuthUri = result.otpAuthUri;
        this.setupQrCodeDataUrl = result.qrCodeDataUrl;
        this.otp = '';
        this.step = 'authenticator';
        this.cdr.markForCheck();
      });
  }

  confirmAuthenticator(): void {
    const code = this.otp.trim();
    if (!/^\d{6}$/.test(code)) {
      this.error = 'Enter the 6-digit code from your authenticator app.';
      return;
    }
    this.loading = true;
    this.error = '';
    const confirm$ = this.adminMode
      ? this.usersPortal.adminConfirmAuthenticatorSetup(this.targetUserId, code)
      : this.usersPortal.confirmMyAuthenticatorSetup(code);
    confirm$
      .pipe(
        catchError((err) => {
          this.error = this.formatHttpError(err);
          return of(false);
        }),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe((ok) => {
        if (!ok) {
          if (!this.error) {
            this.error = 'Invalid or expired code. Try the latest code from your authenticator app.';
          }
          return;
        }
        this.snackBar.open('Authenticator app two-step verification is enabled.', 'Close', {
          duration: 4000,
        });
        this.dialogRef.close(true);
      });
  }

  enableSms(): void {
    this.loading = true;
    this.error = '';
    const enable$ = this.adminMode
      ? this.usersPortal.adminEnableSmsTwoFactor(this.targetUserId)
      : this.usersPortal.enableMySmsTwoFactor();
    enable$
      .pipe(
        catchError((err) => {
          this.error = this.formatHttpError(err);
          return of(false);
        }),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe((ok) => {
        if (!ok) {
          if (!this.error) {
            this.error = 'Could not enable SMS two-step verification.';
          }
          return;
        }
        this.snackBar.open('SMS two-step verification is enabled.', 'Close', { duration: 4000 });
        this.dialogRef.close(true);
      });
  }

  requestDisableOtp(): void {
    if (this.currentMethod !== 'SMS') {
      return;
    }
    this.loading = true;
    this.usersPortal
      .requestMyTwoFactorDisableOtp()
      .pipe(
        catchError((err) => {
          this.error = this.formatHttpError(err);
          return of(false);
        }),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe((ok) => {
        if (ok) {
          this.snackBar.open('Verification code sent to your phone.', 'Close', { duration: 3500 });
        } else if (!this.error) {
          this.error = 'Could not send verification code. Check that SMS delivery is enabled.';
        }
      });
  }

  confirmDisable(): void {
    const code = this.otp.trim();
    if (!this.adminMode && !/^\d{6}$/.test(code)) {
      this.error =
        this.currentMethod === 'AUTHENTICATOR_APP'
          ? 'Enter the 6-digit code from your authenticator app.'
          : 'Enter the 6-digit code sent to your phone.';
      return;
    }
    this.loading = true;
    this.error = '';
    const disable$ = this.adminMode
      ? this.usersPortal.adminDisableTwoFactor(this.targetUserId)
      : this.usersPortal.disableMyTwoFactor(code);
    disable$
      .pipe(
        catchError((err) => {
          this.error = this.formatHttpError(err);
          return of(false);
        }),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe((ok) => {
        if (!ok) {
          if (!this.error) {
            this.error = 'Invalid or expired verification code.';
          }
          return;
        }
        if (this.changingMethod) {
          this.changingMethod = false;
          this.isEnabled = false;
          this.currentMethod = null;
          this.step = 'choose';
          this.otp = '';
          this.snackBar.open('Choose a new verification method.', 'Close', { duration: 3500 });
          this.cdr.markForCheck();
          return;
        }
        this.snackBar.open('Two-step verification has been turned off.', 'Close', { duration: 4000 });
        this.dialogRef.close(true);
      });
  }

  backToChoose(): void {
    this.error = '';
    this.otp = '';
    this.step = 'choose';
    this.cdr.markForCheck();
  }

  turnOffFromStatus(): void {
    this.changingMethod = false;
    this.startDisable();
  }

  private applySecurityContext(security: Record<string, unknown> | null | undefined): void {
    const row = (security ?? {}) as Record<string, unknown>;
    this.isEnabled = row['isTwoFactorEnabled'] === true;
    const method = String(row['twoFactorMethod'] ?? '').trim().toUpperCase();
    this.currentMethod =
      method === 'AUTHENTICATOR_APP' ? 'AUTHENTICATOR_APP' : method === 'SMS' ? 'SMS' : null;
  }

  private applyUserContext(user: Record<string, unknown> | null | undefined): void {
    const row = (user ?? {}) as Record<string, unknown>;
    this.phoneVerified = row['phoneVerified'] === true;
    this.hasPhone = String(row['phoneNumber'] ?? '').trim().length > 0;
  }

  private formatHttpError(err: unknown): string {
    const e = err as { error?: { message?: string; errorMessages?: string[] }; message?: string };
    const msgs = e?.error?.errorMessages;
    if (Array.isArray(msgs) && msgs.length) {
      return msgs.map((m) => String(m)).join(' ');
    }
    if (typeof e?.error?.message === 'string' && e.error.message.trim()) {
      return e.error.message.trim();
    }
    if (typeof e?.message === 'string' && e.message.trim()) {
      return e.message.trim();
    }
    return 'Request failed. Try again.';
  }
}
