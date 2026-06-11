import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { map, Observable } from 'rxjs';
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
    return this.authService.initializeSession().pipe(
      map(() => {
        this.duplexMode.syncFromUser(this.authState.currentUser);
        return isSupplierOrganization(
          this.authState.currentUser?.orgClassification,
          this.authState.currentUser?.duplexMode,
          this.duplexMode.activeMode,
        )
          ? true
          : this.router.createUrlTree(['/my-orders']);
      }),
    );
  }
}
