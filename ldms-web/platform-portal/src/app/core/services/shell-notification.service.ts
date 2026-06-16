import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, catchError, map, of } from 'rxjs';
import { VerificationService, VerificationUserFlags } from './verification.service';

export type ShellNotificationAction = 'verify-phone' | 'verify-email';

export interface ShellNotification {
  id: string;
  title: string;
  body: string;
  time: string;
  action?: ShellNotificationAction;
  urgent?: boolean;
}

const DISMISSED_KEY = 'lx.shellNotifications.dismissed';

@Injectable({ providedIn: 'root' })
export class ShellNotificationService {
  private readonly notificationsSubject = new BehaviorSubject<ShellNotification[]>([]);
  readonly notifications$ = this.notificationsSubject.asObservable();

  constructor(private readonly verification: VerificationService) {}

  refresh(): void {
    this.loadNotifications().subscribe((items) => this.notificationsSubject.next(items));
  }

  loadNotifications(): Observable<ShellNotification[]> {
    return this.verification.fetchVerificationFlags().pipe(
      map((flags) => this.buildFromFlags(flags)),
      catchError(() => of([])),
    );
  }

  dismiss(id: string): void {
    const dismissed = this.readDismissed();
    dismissed.add(id);
    this.writeDismissed(dismissed);
    this.notificationsSubject.next(
      this.notificationsSubject.value.filter((n) => n.id !== id),
    );
  }

  clearAll(): void {
    const ids = this.notificationsSubject.value.map((n) => n.id);
    const dismissed = this.readDismissed();
    for (const id of ids) {
      dismissed.add(id);
    }
    this.writeDismissed(dismissed);
    this.notificationsSubject.next([]);
  }

  private buildFromFlags(flags: VerificationUserFlags | null): ShellNotification[] {
    if (!flags) {
      return [];
    }
    const dismissed = this.readDismissed();
    const items: ShellNotification[] = [];

    if (!flags.phoneVerified && flags.phoneNumber) {
      const id = 'verify-phone';
      if (!dismissed.has(id)) {
        const masked = this.maskPhone(flags.phoneNumber);
        items.push({
          id,
          title: flags.phoneVerificationDue ? 'Phone verification overdue' : 'Verify your phone number',
          body: flags.phoneVerificationDue
            ? `Confirm ${masked} to keep full access to your account.`
            : `Confirm ${masked} with the SMS code we send you.`,
          time: 'Action required',
          action: 'verify-phone',
          urgent: flags.phoneVerificationDue,
        });
      }
    }

    if (!flags.emailVerified) {
      const id = 'verify-email';
      if (!dismissed.has(id)) {
        const emailHint = flags.email ? ` (${this.maskEmail(flags.email)})` : '';
        items.push({
          id,
          title: 'Verify your email address',
          body: `Check your inbox for the verification link${emailHint}, or resend it from your profile.`,
          time: 'Action required',
          action: 'verify-email',
        });
      }
    }

    return items;
  }

  private readDismissed(): Set<string> {
    try {
      const raw = sessionStorage.getItem(DISMISSED_KEY);
      if (!raw) {
        return new Set();
      }
      const parsed = JSON.parse(raw) as unknown;
      if (!Array.isArray(parsed)) {
        return new Set();
      }
      return new Set(parsed.map(String));
    } catch {
      return new Set();
    }
  }

  private writeDismissed(dismissed: Set<string>): void {
    try {
      sessionStorage.setItem(DISMISSED_KEY, JSON.stringify([...dismissed]));
    } catch {
      // best effort
    }
  }

  private maskPhone(phone: string): string {
    const trimmed = phone.trim();
    if (trimmed.length <= 4) {
      return trimmed;
    }
    return `••• ${trimmed.slice(-4)}`;
  }

  private maskEmail(email: string): string {
    const trimmed = email.trim();
    const at = trimmed.indexOf('@');
    if (at <= 1) {
      return trimmed;
    }
    const local = trimmed.slice(0, at);
    const domain = trimmed.slice(at);
    const visible = local.slice(0, Math.min(2, local.length));
    return `${visible}•••${domain}`;
  }
}
