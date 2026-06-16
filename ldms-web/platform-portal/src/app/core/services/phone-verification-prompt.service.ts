import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable, of, switchMap, tap } from 'rxjs';
import {
  PhoneVerificationDialogComponent,
  PhoneVerificationDialogData,
} from '../../shared/components/phone-verification-dialog/phone-verification-dialog.component';
import { ShellNotificationService } from './shell-notification.service';
import { VerificationService } from './verification.service';

const SESSION_DISMISS_KEY = 'lx.phoneVerification.dismissedSession';
const PROMPT_AFTER_SETUP_KEY = 'lx.phoneVerification.promptAfterSetup';

@Injectable({ providedIn: 'root' })
export class PhoneVerificationPromptService {
  constructor(
    private readonly dialog: MatDialog,
    private readonly verification: VerificationService,
    private readonly shellNotifications: ShellNotificationService,
  ) {}

  /** Call after mandatory credential setup so new users verify phone on first workspace entry. */
  markPromptAfterSetup(): void {
    try {
      sessionStorage.setItem(PROMPT_AFTER_SETUP_KEY, '1');
    } catch {
      // best effort
    }
  }

  /** Opens the phone verification dialog when due (every 14 days) or after registration setup. */
  maybePrompt(): Observable<boolean> {
    if (this.isDismissedThisSession()) {
      return of(false);
    }
    const forceAfterSetup = this.consumePromptAfterSetup();
    return this.verification.fetchVerificationFlags().pipe(
      switchMap((flags) => {
        if (!flags?.phoneNumber) {
          return of(false);
        }
        const smsEnabled = flags.smsDeliveryEnabled !== false;
        const shouldPrompt =
          forceAfterSetup || flags.phoneVerificationDue || !flags.phoneVerified;
        if (!shouldPrompt) {
          return of(false);
        }
        const requireVerify = (forceAfterSetup || flags.phoneVerificationDue) && smsEnabled;
        return this.openDialog({
          required: requireVerify,
          smsDeliveryEnabled: smsEnabled,
          title: flags.phoneVerified ? 'Re-verify your phone number' : 'Verify your phone number',
          lead: flags.phoneVerified
            ? 'For your security, confirm your phone number again with the SMS code we send you.'
            : 'Confirm your phone number with the SMS code we send you. Verified phone numbers cannot be changed later.',
        });
      }),
    );
  }

  openDialog(data?: PhoneVerificationDialogData): Observable<boolean> {
    const ref = this.dialog.open(PhoneVerificationDialogComponent, {
      width: '480px',
      maxWidth: '92vw',
      disableClose: data?.required === true && data?.smsDeliveryEnabled !== false,
      data: data ?? {},
    });
    return ref.afterClosed().pipe(
      tap((verified) => {
        if (verified === true) {
          this.shellNotifications.refresh();
          return;
        }
        this.dismissForSession();
      }),
      switchMap((verified) => of(verified === true)),
    );
  }

  dismissForSession(): void {
    try {
      sessionStorage.setItem(SESSION_DISMISS_KEY, '1');
    } catch {
      // best effort
    }
  }

  private isDismissedThisSession(): boolean {
    try {
      return sessionStorage.getItem(SESSION_DISMISS_KEY) === '1';
    } catch {
      return false;
    }
  }

  private consumePromptAfterSetup(): boolean {
    try {
      const v = sessionStorage.getItem(PROMPT_AFTER_SETUP_KEY) === '1';
      if (v) {
        sessionStorage.removeItem(PROMPT_AFTER_SETUP_KEY);
      }
      return v;
    } catch {
      return false;
    }
  }
}
