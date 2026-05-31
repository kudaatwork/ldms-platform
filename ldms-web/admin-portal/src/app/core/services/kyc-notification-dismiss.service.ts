import { Injectable } from '@angular/core';

const STORAGE_KEY = 'ldms_admin_dismissed_kyc_notifications';

/**
 * Persists dismissed KYC shell notifications so queue refreshes do not resurrect them
 * after the reviewer has acted or explicitly cleared the item.
 */
@Injectable({ providedIn: 'root' })
export class KycNotificationDismissService {
  dismiss(id: string): void {
    const key = id?.trim();
    if (!key) {
      return;
    }
    const set = this.readSet();
    set.add(key);
    this.writeSet(set);
  }

  dismissOrganization(orgId: number): void {
    if (!Number.isFinite(orgId) || orgId <= 0) {
      return;
    }
    this.dismiss(`kyc-${orgId}`);
  }

  isDismissed(id: string): boolean {
    const key = id?.trim();
    if (!key) {
      return false;
    }
    return this.readSet().has(key);
  }

  filterById<T extends { id: string }>(items: T[]): T[] {
    const dismissed = this.readSet();
    return items.filter((item) => !dismissed.has(item.id));
  }

  private readSet(): Set<string> {
    if (typeof sessionStorage === 'undefined') {
      return new Set();
    }
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return new Set();
    }
    try {
      const parsed = JSON.parse(raw) as unknown;
      if (!Array.isArray(parsed)) {
        return new Set();
      }
      return new Set(parsed.filter((v): v is string => typeof v === 'string' && v.length > 0));
    } catch {
      return new Set();
    }
  }

  private writeSet(set: Set<string>): void {
    if (typeof sessionStorage === 'undefined') {
      return;
    }
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify([...set]));
  }
}
