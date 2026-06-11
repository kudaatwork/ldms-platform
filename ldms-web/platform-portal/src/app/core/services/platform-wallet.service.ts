import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, map, tap } from 'rxjs';
import { ldmsServiceUrl } from '../utils/api-url.util';

export type OrganizationBillingMode = 'PREPAID_WALLET' | 'PREMIUM_SUBSCRIPTION';

export interface PlatformWalletSummary {
  organizationId?: number;
  organizationName?: string;
  balanceCents: number;
  currencyCode: string;
  billingMode: OrganizationBillingMode;
  lowBalanceThresholdCents?: number;
  lowBalance?: boolean;
  subscriptionPackageId?: number | null;
  subscriptionPackageName?: string | null;
}

export interface OrganizationBillingSetting {
  id?: number;
  organizationId?: number;
  organizationName?: string;
  billingMode: OrganizationBillingMode;
  subscriptionPackageId?: number | null;
  subscriptionPackageName?: string | null;
  subscriptionStartedAt?: string | null;
  subscriptionRenewsAt?: string | null;
  lowBalanceThresholdCents?: number;
}

export interface SubscriptionPackageRow {
  id: number;
  code: string;
  name: string;
  description?: string;
  monthlyPriceCents: number;
  currencyCode: string;
  sortOrder?: number;
  featured?: boolean;
  active?: boolean;
}

export interface PlatformActionChargeRow {
  id: number;
  actionCode: string;
  displayName: string;
  description?: string;
  chargeCents: number;
  category?: string;
  active?: boolean;
}

export interface WalletDepositRow {
  id: number;
  organizationId?: number;
  amountCents: number;
  currencyCode: string;
  referenceNumber?: string;
  notes?: string;
  status: string;
  createdAt?: string;
}

export interface UsageChargeBreakdownRow {
  actionCode: string;
  actionDisplayName?: string;
  totalChargeCents: number;
  eventCount: number;
}

export interface UsageChargeRecordRow {
  id: number;
  billingMode: string;
  actionCode: string;
  actionDisplayName?: string;
  chargeCents: number;
  deducted?: boolean;
  tripId?: number | null;
  seasonId?: number | null;
  serviceName?: string;
  createdAt?: string;
}

export interface WalletTransactionRow {
  id: number;
  transactionType: string;
  amountCents: number;
  balanceAfterCents: number;
  actionCode?: string;
  description?: string;
  createdAt?: string;
}

export interface UsageChargeReport {
  organizationId?: number;
  billingMode?: string;
  tripId?: number | null;
  seasonId?: number | null;
  periodFrom?: string;
  periodTo?: string;
  totalChargeCents: number;
  deductedChargeCents: number;
  hypotheticalChargeCents: number;
  breakdown?: UsageChargeBreakdownRow[];
  records?: UsageChargeRecordRow[];
  dailyTotalsCents?: number[];
  dailyLabels?: string[];
}

interface PlatformWalletApiResponse {
  isSuccess?: boolean;
  success?: boolean;
  statusCode?: number;
  message?: string;
  platformWalletSummaryDto?: PlatformWalletSummary;
  organizationBillingSettingDto?: OrganizationBillingSetting;
  subscriptionPackageDtoList?: SubscriptionPackageRow[];
  subscriptionPackageDto?: SubscriptionPackageRow;
  platformActionChargeDtoList?: PlatformActionChargeRow[];
  platformActionChargeDto?: PlatformActionChargeRow;
  walletDepositDto?: WalletDepositRow;
  walletDepositDtoList?: WalletDepositRow[];
  usageChargeReportDto?: UsageChargeReport;
  walletTransactionDtoList?: WalletTransactionRow[];
}

@Injectable({ providedIn: 'root' })
export class PlatformWalletService {
  private readonly frontendBase = ldmsServiceUrl('billing-payments', 'platform-wallet');
  private readonly backofficeBase = ldmsServiceUrl('billing-payments', 'platform-wallet', undefined, 'backoffice');

  private readonly summarySubject = new BehaviorSubject<PlatformWalletSummary | null>(null);
  readonly summary$ = this.summarySubject.asObservable();

  constructor(private readonly http: HttpClient) {}

  private base(adminMode: boolean): string {
    return adminMode ? this.backofficeBase : this.frontendBase;
  }

  refreshSummary(): Observable<PlatformWalletSummary> {
    return this.http.get<PlatformWalletApiResponse>(`${this.frontendBase}/summary`).pipe(
      map((res) => res.platformWalletSummaryDto ?? {
        balanceCents: 0,
        currencyCode: 'USD',
        billingMode: 'PREPAID_WALLET' as OrganizationBillingMode,
      }),
      tap((summary) => this.summarySubject.next(summary)),
    );
  }

  getBillingSetting(): Observable<OrganizationBillingSetting> {
    return this.http.get<PlatformWalletApiResponse>(`${this.frontendBase}/billing-setting`).pipe(
      map((res) => res.organizationBillingSettingDto ?? { billingMode: 'PREPAID_WALLET' }),
    );
  }

  saveBillingSetting(payload: {
    billingMode: OrganizationBillingMode;
    subscriptionPackageId?: number | null;
    lowBalanceThresholdCents?: number;
  }): Observable<OrganizationBillingSetting> {
    return this.http.put<PlatformWalletApiResponse>(`${this.frontendBase}/billing-setting`, payload).pipe(
      map((res) => res.organizationBillingSettingDto ?? { billingMode: payload.billingMode }),
      tap(() => this.refreshSummary().subscribe()),
    );
  }

  listSubscriptionPackages(adminMode = false): Observable<SubscriptionPackageRow[]> {
    return this.http.get<PlatformWalletApiResponse>(`${this.base(adminMode)}/subscription-packages`).pipe(
      map((res) => res.subscriptionPackageDtoList ?? []),
    );
  }

  createDeposit(payload: {
    amountCents: number;
    currencyCode?: string;
    referenceNumber?: string;
    notes?: string;
    proofDocumentId?: number;
  }): Observable<WalletDepositRow> {
    return this.http.post<PlatformWalletApiResponse>(`${this.frontendBase}/deposits`, payload).pipe(
      map((res) => res.walletDepositDto ?? { id: 0, amountCents: payload.amountCents, currencyCode: 'USD', status: 'PENDING' }),
    );
  }

  listDeposits(): Observable<WalletDepositRow[]> {
    return this.http.get<PlatformWalletApiResponse>(`${this.frontendBase}/deposits`).pipe(
      map((res) => res.walletDepositDtoList ?? []),
    );
  }

  listTransactions(): Observable<WalletTransactionRow[]> {
    return this.http.get<PlatformWalletApiResponse>(`${this.frontendBase}/transactions`).pipe(
      map((res) => res.walletTransactionDtoList ?? []),
    );
  }

  listActiveActionCharges(): Observable<PlatformActionChargeRow[]> {
    return this.http.get<PlatformWalletApiResponse>(`${this.frontendBase}/action-charges`).pipe(
      map((res) => res.platformActionChargeDtoList ?? []),
    );
  }

  getUsageReport(params: {
    tripId?: number | null;
    seasonId?: number | null;
    from?: string;
    to?: string;
  }): Observable<UsageChargeReport> {
    const query = new URLSearchParams();
    if (params.tripId != null) query.set('tripId', String(params.tripId));
    if (params.seasonId != null) query.set('seasonId', String(params.seasonId));
    if (params.from) query.set('from', params.from);
    if (params.to) query.set('to', params.to);
    const qs = query.toString();
    const url = qs ? `${this.frontendBase}/usage-report?${qs}` : `${this.frontendBase}/usage-report`;
    return this.http.get<PlatformWalletApiResponse>(url).pipe(
      map((res) => res.usageChargeReportDto ?? {
        totalChargeCents: 0,
        deductedChargeCents: 0,
        hypotheticalChargeCents: 0,
        breakdown: [],
        records: [],
        dailyTotalsCents: [],
        dailyLabels: [],
      }),
    );
  }

  listActionCharges(adminMode = true): Observable<PlatformActionChargeRow[]> {
    return this.http.get<PlatformWalletApiResponse>(`${this.base(adminMode)}/action-charges`).pipe(
      map((res) => res.platformActionChargeDtoList ?? []),
    );
  }

  saveActionCharge(payload: PlatformActionChargeRow, adminMode = true): Observable<PlatformActionChargeRow> {
    return this.http.put<PlatformWalletApiResponse>(`${this.base(adminMode)}/action-charges`, payload).pipe(
      map((res) => res.platformActionChargeDto ?? payload),
    );
  }

  saveSubscriptionPackage(payload: SubscriptionPackageRow, adminMode = true): Observable<SubscriptionPackageRow> {
    return this.http.post<PlatformWalletApiResponse>(`${this.base(adminMode)}/subscription-packages`, payload).pipe(
      map((res) => res.subscriptionPackageDto ?? payload),
    );
  }

  listPendingDeposits(): Observable<WalletDepositRow[]> {
    return this.http.get<PlatformWalletApiResponse>(`${this.backofficeBase}/deposits/pending`).pipe(
      map((res) => res.walletDepositDtoList ?? []),
    );
  }

  confirmDeposit(depositId: number): Observable<WalletDepositRow> {
    return this.http.post<PlatformWalletApiResponse>(`${this.backofficeBase}/deposits/${depositId}/confirm`, {}).pipe(
      map((res) => res.walletDepositDto ?? { id: depositId, amountCents: 0, currencyCode: 'USD', status: 'CONFIRMED' }),
    );
  }

  formatCents(cents: number, currencyCode = 'USD'): string {
    const amount = (cents ?? 0) / 100;
    try {
      return new Intl.NumberFormat(undefined, { style: 'currency', currency: currencyCode, minimumFractionDigits: 2 }).format(amount);
    } catch {
      return `${currencyCode} ${amount.toFixed(2)}`;
    }
  }
}
