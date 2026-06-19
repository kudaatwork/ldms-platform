import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { map, Observable, of } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { AuthStateService } from '../services/auth-state.service';
import { DuplexTradingModeService } from '../services/duplex-trading-mode.service';
import { isSupplierOrganization } from '../utils/org-classification.util';

/** Inventory catalogue management is supplier-only; customers order via My Orders. */
@Injectable({ providedIn: 'root' })
export class SupplierClassificationGuard implements CanActivate {
  constructor(
    private readonly authState: AuthStateService,
    private readonly authService: AuthService,
    private readonly duplexMode: DuplexTradingModeService,
    private readonly router: Router,
  ) {}

  canActivate(): Observable<boolean | UrlTree> {
    const immediate = this.evaluate();
    if (immediate !== null) {
      return of(immediate);
    }
    return this.authService.initializeSession().pipe(map(() => this.evaluate() ?? this.router.createUrlTree(['/my-orders'])));
  }

  private evaluate(): boolean | UrlTree | null {
    const user = this.authState.currentUser;
    if (!user?.orgClassification) {
      return null;
    }
    this.duplexMode.syncFromUser(user);
    return isSupplierOrganization(user.orgClassification, user.duplexMode, this.duplexMode.activeMode)
      ? true
      : this.router.createUrlTree(['/my-orders']);
  }
}
