import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, catchError, map, of } from 'rxjs';
import {
  fuelAlertTone,
} from '../constants/fuel-alert.constants';
import { VerificationService, VerificationUserFlags } from './verification.service';

import type { PlatformWalletSummary, UsageChargeReport } from './platform-wallet.service';

export type ShellNotificationAction =
  | 'verify-phone'
  | 'verify-email'
  | 'fuel-alert'
  | 'wallet-low'
  | 'wallet-frozen'
  | 'sms-exhausted'
  | 'usage-report';

export interface ShellNotification {
  id: string;
  title: string;
  body: string;
  time: string;
  action?: ShellNotificationAction;
  urgent?: boolean;
  tripId?: number;
  tone?: 'warn' | 'critical';
}

export interface FuelAlertSyncPayload {
  tripId: number;
  tripNumber: string;
  vehicleLabel?: string;
  fuelLevelPct: number;
  litersRemaining: number;
}

const DISMISSED_KEY = 'lx.shellNotifications.dismissed';
const OPERATIONAL_KEY = 'lx.shellNotifications.operational';

@Injectable({ providedIn: 'root' })
export class ShellNotificationService {
  private readonly notificationsSubject = new BehaviorSubject<ShellNotification[]>([]);
  readonly notifications$ = this.notificationsSubject.asObservable();
  private lastVerificationItems: ShellNotification[] = [];
  private lastWalletItems: ShellNotification[] = [];

  constructor(private readonly verification: VerificationService) {}

  refresh(): void {
    this.loadNotifications().subscribe((items) => this.notificationsSubject.next(items));
  }

  loadNotifications(): Observable<ShellNotification[]> {
    return this.verification.fetchVerificationFlags().pipe(
      map((flags) => {
        this.lastVerificationItems = this.buildFromFlags(flags);
        return this.mergeAll();
      }),
      catchError(() => of(this.mergeAll())),
    );
  }

  /** Push or update a fuel-low alert on the notification bell. */
  syncFuelAlert(payload: FuelAlertSyncPayload): void {
    const id = this.fuelAlertId(payload.tripId);
    const tone = fuelAlertTone(payload.fuelLevelPct);
    const dismissed = this.readDismissed();

    if (tone === 'ok') {
      this.removeOperational(id);
      dismissed.delete(id);
      this.writeDismissed(dismissed);
      this.publishMerged();
      return;
    }

    if (dismissed.has(id)) {
      return;
    }

    const critical = tone === 'critical';
    const pctLabel = Math.round(payload.fuelLevelPct);
    const litersLabel = Math.round(payload.litersRemaining);
    const context = [payload.tripNumber, payload.vehicleLabel].filter(Boolean).join(' · ');

    this.upsertOperational({
      id,
      title: critical ? 'Critical fuel level' : 'Fuel running low',
      body: critical
        ? `${context}: only ${pctLabel}% (${litersLabel} L) left — request an urgent top-up.`
        : `${context}: ${pctLabel}% (${litersLabel} L) remaining — plan a fuel top-up soon.`,
      time: 'Live tracking',
      action: 'fuel-alert',
      tripId: payload.tripId,
      urgent: critical,
      tone: critical ? 'critical' : 'warn',
    });
    this.publishMerged();
  }

  dismiss(id: string): void {
    const dismissed = this.readDismissed();
    dismissed.add(id);
    this.writeDismissed(dismissed);
    this.removeOperational(id);
    this.publishMerged();
  }

  clearAll(): void {
    const ids = this.notificationsSubject.value.map((n) => n.id);
    const dismissed = this.readDismissed();
    for (const id of ids) {
      dismissed.add(id);
    }
    this.writeDismissed(dismissed);
    this.writeOperational([]);
    this.publishMerged();
  }

  private fuelAlertId(tripId: number): string {
    return `fuel-alert-${tripId}`;
  }

  /** Push wallet low-balance / frozen / recent usage alerts onto the bell. */
  syncWalletAlerts(summary: PlatformWalletSummary | null, report: UsageChargeReport | null): void {
    const dismissed = this.readDismissed();
    const items: ShellNotification[] = [];

    if (summary?.walletFrozen) {
      const id = 'wallet-frozen';
      if (!dismissed.has(id)) {
        items.push({
          id,
          title: 'Wallet empty — platform paused',
          body: 'Top up your prepaid wallet in Settings → Billing to restore shipments, fleet, and orders.',
          time: 'Billing',
          action: 'wallet-frozen',
          urgent: true,
          tone: 'critical',
        });
      }
    } else if (summary?.smsQuotaExhausted && summary.billingMode === 'PREMIUM_SUBSCRIPTION') {
      const id = 'sms-quota-exhausted';
      if (!dismissed.has(id)) {
        const included = summary.smsIncludedMonthly ?? 0;
        const used = summary.smsUsedThisPeriod ?? included;
        items.push({
          id,
          title: 'SMS quota exhausted',
          body: `You have used all ${included} included SMS this period (${used} sent). Top up your wallet in Settings → Billing to resume SMS notifications.`,
          time: 'Billing',
          action: 'sms-exhausted',
          urgent: true,
          tone: 'warn',
        });
      }
    } else if (summary?.lowBalance) {
      const id = 'wallet-low';
      if (!dismissed.has(id)) {
        const balance = this.formatUsd(summary.balanceCents ?? 0);
        items.push({
          id,
          title: 'Prepaid wallet running low',
          body: `Balance is ${balance}. Top up soon to avoid interrupted corridor operations.`,
          time: 'Billing',
          action: 'wallet-low',
          urgent: true,
          tone: 'warn',
        });
      }
    }

    const recentCount = report?.records?.length ?? 0;
    const deductedToday = report?.deductedChargeCents ?? 0;
    if (recentCount > 0) {
      const id = 'usage-report-today';
      if (!dismissed.has(id)) {
        items.push({
          id,
          title: 'Platform usage today',
          body: `${recentCount} charge${recentCount === 1 ? '' : 's'} recorded (${this.formatUsd(deductedToday)} deducted). Open your usage report for details.`,
          time: 'Analytics',
          action: 'usage-report',
        });
      }
    }

    this.lastWalletItems = items;
    this.publishMerged();
  }

  private formatUsd(cents: number): string {
    return new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD' }).format((cents ?? 0) / 100);
  }

  private publishMerged(): void {
    this.notificationsSubject.next(this.mergeAll());
  }

  private mergeAll(): ShellNotification[] {
    const operational = this.readOperationalFiltered();
    return this.sortNotifications([
      ...operational,
      ...this.lastWalletItems,
      ...this.lastVerificationItems,
    ]);
  }

  private sortNotifications(items: ShellNotification[]): ShellNotification[] {
    return [...items].sort((a, b) => {
      const au = a.urgent ? 1 : 0;
      const bu = b.urgent ? 1 : 0;
      if (au !== bu) {
        return bu - au;
      }
      const ac = a.tone === 'critical' ? 1 : 0;
      const bc = b.tone === 'critical' ? 1 : 0;
      return bc - ac;
    });
  }

  private readOperationalFiltered(): ShellNotification[] {
    const dismissed = this.readDismissed();
    return this.readOperational().filter((n) => !dismissed.has(n.id));
  }

  private readOperational(): ShellNotification[] {
    try {
      const raw = sessionStorage.getItem(OPERATIONAL_KEY);
      if (!raw) {
        return [];
      }
      const parsed = JSON.parse(raw) as unknown;
      if (!Array.isArray(parsed)) {
        return [];
      }
      return parsed.filter((n): n is ShellNotification => Boolean(n && typeof n === 'object' && 'id' in n));
    } catch {
      return [];
    }
  }

  private writeOperational(items: ShellNotification[]): void {
    try {
      sessionStorage.setItem(OPERATIONAL_KEY, JSON.stringify(items));
    } catch {
      // best effort
    }
  }

  private upsertOperational(notification: ShellNotification): void {
    const items = this.readOperational().filter((n) => n.id !== notification.id);
    items.push(notification);
    this.writeOperational(items);
  }

  private removeOperational(id: string): void {
    const items = this.readOperational().filter((n) => n.id !== id);
    this.writeOperational(items);
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
