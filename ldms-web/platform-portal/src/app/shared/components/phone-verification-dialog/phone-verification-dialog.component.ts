import { Component, Inject, Optional } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { VerificationService } from '../../../core/services/verification.service';
import { VerificationFlowError } from '../../../core/services/verification-flow.error';

export interface PhoneVerificationDialogData {
  /** When true, user must verify before continuing (no dismiss). */
  required?: boolean;
  title?: string;
  lead?: string;
}

@Component({
  selector: 'app-phone-verification-dialog',
  templateUrl: './phone-verification-dialog.component.html',
  styleUrl: './phone-verification-dialog.component.scss',
  standalone: false,
})
export class PhoneVerificationDialogComponent {
  otp = '';
  sending = false;
  confirming = false;
  error = '';
  codeSent = false;
  readonly title: string;
  readonly lead: string;
  readonly required: boolean;

  constructor(
    private readonly dialogRef: MatDialogRef<PhoneVerificationDialogComponent, boolean>,
    private readonly verification: VerificationService,
    private readonly snackBar: MatSnackBar,
    @Optional() @Inject(MAT_DIALOG_DATA) data: PhoneVerificationDialogData | null,
  ) {
    this.required = data?.required === true;
    this.title = data?.title?.trim() || 'Verify your phone number';
    this.lead =
      data?.lead?.trim() ||
      'We sent a 6-digit code by SMS. Enter it below to confirm your phone number and keep your account secure.';
  }

  sendCode(): void {
    if (this.sending) {
      return;
    }
    this.error = '';
    this.sending = true;
    this.verification.requestPhoneVerification().subscribe({
      next: () => {
        this.sending = false;
        this.codeSent = true;
      },
      error: (err: unknown) => {
        this.sending = false;
        this.handleRequestError(err);
      },
    });
  }

  confirm(): void {
    const code = this.otp.trim();
    if (!/^\d{6}$/.test(code)) {
      this.error = 'Enter the 6-digit code from your SMS.';
      return;
    }
    if (this.confirming) {
      return;
    }
    this.error = '';
    this.confirming = true;
    this.verification.confirmPhoneVerification(code).subscribe({
      next: () => {
        this.confirming = false;
        this.dialogRef.close(true);
      },
      error: (err: unknown) => {
        this.confirming = false;
        if (err instanceof VerificationFlowError && err.dismissDialog) {
          this.handleRequestError(err);
          return;
        }
        this.error = err instanceof Error ? err.message : 'Verification failed.';
      },
    });
  }

  dismiss(): void {
    if (this.required) {
      return;
    }
    this.dialogRef.close(false);
  }

  private handleRequestError(err: unknown): void {
    const flowErr = err instanceof VerificationFlowError ? err : null;
    const message =
      err instanceof Error ? err.message : 'Could not send verification code.';
    if (flowErr?.dismissDialog) {
      this.snackBar.open(message, 'Close', {
        duration: 7000,
        panelClass: ['app-snackbar-info'],
      });
      this.dialogRef.close(false);
      return;
    }
    this.error = message;
  }
}
