import { Component, Inject, OnInit, Optional } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { VerificationService } from '../../../core/services/verification.service';
import { VerificationFlowError } from '../../../core/services/verification-flow.error';

export interface PhoneVerificationDialogData {
  /** When true, user must verify before continuing (no dismiss). */
  required?: boolean;
  title?: string;
  lead?: string;
  smsDeliveryEnabled?: boolean;
}

@Component({
  selector: 'app-phone-verification-dialog',
  templateUrl: './phone-verification-dialog.component.html',
  styleUrl: './phone-verification-dialog.component.scss',
  standalone: false,
})
export class PhoneVerificationDialogComponent implements OnInit {
  otp = '';
  sending = false;
  confirming = false;
  loading = true;
  error = '';
  codeSent = false;
  smsDeliveryEnabled = true;
  smsDisabledRevealed = false;
  phoneNumber = '';
  readonly title: string;
  readonly required: boolean;
  private readonly initialLead: string;
  lead: string;

  constructor(
    private readonly dialogRef: MatDialogRef<PhoneVerificationDialogComponent, boolean>,
    private readonly verification: VerificationService,
    private readonly snackBar: MatSnackBar,
    @Optional() @Inject(MAT_DIALOG_DATA) data: PhoneVerificationDialogData | null,
  ) {
    this.required = data?.required === true;
    this.title = data?.title?.trim() || 'Verify your phone number';
    this.initialLead =
      data?.lead?.trim() ||
      'We sent a 6-digit code by SMS. Enter it below to confirm your phone number and keep your account secure.';
    this.lead = this.initialLead;
    if (data?.smsDeliveryEnabled === false) {
      this.smsDeliveryEnabled = false;
    }
  }

  ngOnInit(): void {
    if (!this.loading) {
      return;
    }
    this.verification.fetchVerificationFlags().subscribe({
      next: (flags) => {
        this.loading = false;
        if (!flags) {
          return;
        }
        this.phoneNumber = flags.phoneNumber;
        this.smsDeliveryEnabled = flags.smsDeliveryEnabled;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  get canSendSms(): boolean {
    return !this.smsDisabledRevealed && !this.loading;
  }

  get canDismiss(): boolean {
    return !this.required || this.smsDisabledRevealed;
  }

  sendCode(): void {
    if (!this.canSendSms || this.sending) {
      return;
    }
    this.error = '';
    this.sending = true;
    if (!this.smsDeliveryEnabled) {
      this.sending = false;
      this.revealSmsDisabled();
      return;
    }
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
    if (!this.canSendSms) {
      return;
    }
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
    if (!this.canDismiss) {
      return;
    }
    this.dialogRef.close(false);
  }

  private revealSmsDisabled(): void {
    this.smsDisabledRevealed = true;
    this.applySmsDisabledState();
  }

  private applySmsDisabledState(): void {
    this.lead = this.phoneNumber
      ? `SMS delivery is turned off, so we cannot send a code to ${this.phoneNumber} right now. You can continue using the portal and verify later when SMS is available.`
      : 'SMS delivery is turned off in this environment, so verification codes cannot be sent. You can continue using the portal and verify later when SMS is available.';
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
    if (message.toLowerCase().includes('sms')) {
      this.smsDeliveryEnabled = false;
      this.revealSmsDisabled();
    }
    this.error = message;
  }
}
