import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import type { CurrentUser, OrganizationClassification } from '../models/auth.model';
import type { TradingWorkspaceMode } from '../utils/org-classification.util';
import { effectiveTradingMode } from '../utils/org-classification.util';

const STORAGE_KEY = 'lx_duplex_trading_mode';

/** Active supplier/customer workspace for duplex organisations. */
@Injectable({ providedIn: 'root' })
export class DuplexTradingModeService {
  private readonly modeSubject = new BehaviorSubject<TradingWorkspaceMode | null>(this.readStored());

  readonly activeMode$ = this.modeSubject.asObservable();

  get activeMode(): TradingWorkspaceMode | null {
    return this.modeSubject.value;
  }

  syncFromUser(user: CurrentUser | null | undefined): void {
    if (!user?.duplexMode) {
      this.modeSubject.next(null);
      return;
    }
    const stored = this.readStored();
    const primary = user.orgClassification;
    if (stored && this.isValidMode(stored, primary)) {
      this.modeSubject.next(stored);
      return;
    }
    const fallback = this.defaultMode(primary);
    if (fallback) {
      this.persist(fallback);
      this.modeSubject.next(fallback);
    }
  }

  setMode(mode: TradingWorkspaceMode, user: CurrentUser | null | undefined): void {
    if (!user?.duplexMode || !this.isValidMode(mode, user.orgClassification)) {
      return;
    }
    this.persist(mode);
    this.modeSubject.next(mode);
  }

  effectiveClassification(user: CurrentUser | null | undefined): OrganizationClassification | undefined {
    if (!user?.orgClassification) {
      return undefined;
    }
    return (
      effectiveTradingMode(user.orgClassification, user.duplexMode, this.activeMode) ?? user.orgClassification
    );
  }

  private defaultMode(classification: OrganizationClassification | undefined): TradingWorkspaceMode | null {
    if (classification === 'SUPPLIER' || classification === 'CUSTOMER') {
      return classification;
    }
    return null;
  }

  private isValidMode(
    mode: TradingWorkspaceMode,
    classification: OrganizationClassification | undefined,
  ): boolean {
    return mode === 'SUPPLIER' || mode === 'CUSTOMER';
  }

  private readStored(): TradingWorkspaceMode | null {
    if (typeof sessionStorage === 'undefined') {
      return null;
    }
    const raw = sessionStorage.getItem(STORAGE_KEY);
    return raw === 'SUPPLIER' || raw === 'CUSTOMER' ? raw : null;
  }

  private persist(mode: TradingWorkspaceMode): void {
    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.setItem(STORAGE_KEY, mode);
    }
  }
}
