import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';

export interface PlatformActionChargeRow {
  id?: number;
  actionCode: string;
  displayName: string;
  description?: string;
  chargeCents: number;
  category?: string;
  active?: boolean;
}

export interface SubscriptionPackageRow {
  id?: number;
  code: string;
  name: string;
  description?: string;
  monthlyPriceCents: number;
  currencyCode: string;
  sortOrder?: number;
  featured?: boolean;
  active?: boolean;
}

export interface WalletDepositRow {
  id: number;
  organizationId?: number;
  amountCents: number;
  currencyCode: string;
  referenceNumber?: string;
  status: string;
  createdAt?: string;
}

interface PlatformWalletApiResponse {
  platformActionChargeDtoList?: PlatformActionChargeRow[];
  platformActionChargeDto?: PlatformActionChargeRow;
  subscriptionPackageDtoList?: SubscriptionPackageRow[];
  subscriptionPackageDto?: SubscriptionPackageRow;
  walletDepositDtoList?: WalletDepositRow[];
  walletDepositDto?: WalletDepositRow;
}

@Injectable({ providedIn: 'root' })
export class PlatformWalletAdminService {
  private readonly backofficeBase = ldmsServiceUrl('billing-payments', 'platform-wallet', undefined, 'backoffice');

  constructor(private readonly http: HttpClient) {}

  listActionCharges(): Observable<PlatformActionChargeRow[]> {
    return this.http.get<PlatformWalletApiResponse>(`${this.backofficeBase}/action-charges`).pipe(
      map((res) => res.platformActionChargeDtoList ?? []),
    );
  }

  saveActionCharge(payload: PlatformActionChargeRow): Observable<PlatformActionChargeRow> {
    return this.http.put<PlatformWalletApiResponse>(`${this.backofficeBase}/action-charges`, payload).pipe(
      map((res) => res.platformActionChargeDto ?? payload),
    );
  }

  listSubscriptionPackages(): Observable<SubscriptionPackageRow[]> {
    return this.http.get<PlatformWalletApiResponse>(`${this.backofficeBase}/subscription-packages`).pipe(
      map((res) => res.subscriptionPackageDtoList ?? []),
    );
  }

  saveSubscriptionPackage(payload: SubscriptionPackageRow): Observable<SubscriptionPackageRow> {
    return this.http.post<PlatformWalletApiResponse>(`${this.backofficeBase}/subscription-packages`, payload).pipe(
      map((res) => res.subscriptionPackageDto ?? payload),
    );
  }

  listPendingDeposits(): Observable<WalletDepositRow[]> {
    return this.http.get<PlatformWalletApiResponse>(`${this.backofficeBase}/deposits/pending`).pipe(
      map((res) => res.walletDepositDtoList ?? []),
    );
  }

  confirmDeposit(depositId: number): Observable<void> {
    return this.http.post<unknown>(`${this.backofficeBase}/deposits/${depositId}/confirm`, {}).pipe(map(() => undefined));
  }

  formatCents(cents: number, currencyCode = 'USD'): string {
    const amount = (cents ?? 0) / 100;
    try {
      return new Intl.NumberFormat(undefined, { style: 'currency', currency: currencyCode }).format(amount);
    } catch {
      return `${currencyCode} ${amount.toFixed(2)}`;
    }
  }
}
