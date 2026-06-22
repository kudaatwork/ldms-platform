import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { Observable, catchError, map, of } from 'rxjs';
import { PlatformWalletService } from '../services/platform-wallet.service';

/** Blocks operational modules when prepaid wallet balance is zero. */
@Injectable({ providedIn: 'root' })
export class WalletAccessGuard implements CanActivate {
  constructor(
    private readonly wallet: PlatformWalletService,
    private readonly router: Router,
  ) {}

  canActivate(): Observable<boolean | UrlTree> {
    return this.wallet.refreshSummary().pipe(
      map((summary) => {
        if (summary.billingMode !== 'PREPAID_WALLET') {
          return true;
        }
        if (summary.walletFrozen || summary.platformAccessAllowed === false) {
          return this.router.createUrlTree(['/settings'], {
            queryParams: { section: 'billing', frozen: '1' },
          });
        }
        return true;
      }),
      catchError(() => of(true)),
    );
  }
}
