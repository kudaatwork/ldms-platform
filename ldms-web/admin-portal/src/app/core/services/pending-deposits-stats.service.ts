import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import {
  PlatformWalletAdminService,
  type WalletDepositRow,
} from '../../features/settings/services/platform-wallet-admin.service';

/** Shared pending wallet deposit counts for the topbar notification bell. */
@Injectable({ providedIn: 'root' })
export class PendingDepositsStatsService {
  private readonly depositsSubject = new BehaviorSubject<WalletDepositRow[]>([]);
  readonly deposits$ = this.depositsSubject.asObservable();

  constructor(private readonly walletAdmin: PlatformWalletAdminService) {}

  get snapshot(): WalletDepositRow[] {
    return this.depositsSubject.value;
  }

  setSnapshot(rows: WalletDepositRow[]): void {
    this.depositsSubject.next(rows);
  }

  refresh(): Observable<WalletDepositRow[]> {
    return this.walletAdmin.listPendingDeposits().pipe(
      tap((rows) => this.depositsSubject.next(rows)),
      catchError(() => {
        this.depositsSubject.next([]);
        return of([]);
      }),
    );
  }
}
