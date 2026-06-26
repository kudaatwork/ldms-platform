import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, catchError, map, of, tap, throwError } from 'rxjs';
import { isApiFailureEnvelope, readApiFailureMessage, extractDtoList } from '../utils/api-paged-response.util';
import { extractFileUploadDtoFromResponse } from '../utils/file-upload-dto-extract.util';
import { downloadBlob } from '../../shared/utils/lx-export.util';
import { ldmsApiUrl, ldmsServiceUrl } from '../utils/api-url.util';

export type OrganizationBillingMode = 'PREPAID_WALLET' | 'PREMIUM_SUBSCRIPTION';

export type WalletDepositPaymentMethod =
  | 'BANK_TRANSFER'
  | 'CASH'
  | 'ECOCASH'
  | 'PAYNOW'
  | 'PAYPAL'
  | 'MASTERCARD';

export interface PlatformWalletSummary {
  organizationId?: number;
  organizationName?: string;
  balanceCents: number;
  currencyCode: string;
  billingMode: OrganizationBillingMode;
  lowBalanceThresholdCents?: number;
  lowBalance?: boolean;
  walletFrozen?: boolean;
  platformAccessAllowed?: boolean;
  subscriptionPackageId?: number | null;
  subscriptionPackageName?: string | null;
  subscriptionStartedAt?: string | null;
  subscriptionRenewsAt?: string | null;
  smsIncludedMonthly?: number;
  smsUsedThisPeriod?: number;
  smsRemainingThisPeriod?: number;
  smsQuotaExhausted?: boolean;
  subscriptionQuotas?: SubscriptionQuotaMeter[];
}

export interface SubscriptionQuotaMeter {
  code: string;
  label: string;
  includedMonthly: number;
  usedThisPeriod: number;
  remainingThisPeriod: number;
  exhausted: boolean;
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
  includedHeavyCredits?: number;
  includedStandardCredits?: number;
  includedLightCredits?: number;
  includedTrackingDayCredits?: number;
  sortOrder?: number;
  featured?: boolean;
  active?: boolean;
  fuelConsumptionAvailable?: boolean;
}

export interface PlatformActionChargeRow {
  id: number;
  actionCode: string;
  displayName: string;
  description?: string;
  chargeCents: number;
  category?: string;
  billingTier?: string;
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
  purpose?: string;
  subscriptionPackageId?: number;
  subscriptionPackageName?: string;
  proofDocumentId?: number;
  gatewayProvider?: string;
  paymentMethod?: string;
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
  receiptNumber?: string;
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
  walletTransactionDto?: WalletTransactionRow;
  walletTransactionDtoList?: WalletTransactionRow[];
  receiptHtml?: string;
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

  uploadPaymentProof(organizationId: number, file: File): Observable<number> {
    const form = new FormData();
    form.append('files', file, file.name);
    form.append(
      'fileUploadRequest',
      JSON.stringify({
        ownerType: 'ORGANIZATION',
        ownerId: organizationId,
        filesMetadata: [{ fileType: 'OTHER' }],
      }),
    );
    return this.http.post<unknown>(`${ldmsApiUrl('/ldms-file-upload-service/v1/frontend/file-upload')}/upload`, form).pipe(
      map((res) => {
        if (isApiFailureEnvelope(res)) {
          throw new Error(readApiFailureMessage(res, 'Could not upload proof of payment.'));
        }
        const dto = extractFileUploadDtoFromResponse(res);
        const id = Number(dto?.['id'] ?? 0);
        if (!id) {
          throw new Error('File upload service did not return a file id.');
        }
        return id;
      }),
      catchError((err) => throwError(() => this.toDepositError(err, 'Could not upload proof of payment.'))),
    );
  }

  createDeposit(payload: {
    amountCents: number;
    currencyCode?: string;
    referenceNumber?: string;
    notes?: string;
    proofDocumentId?: number;
    gatewayProvider?: string;
    paymentMethod?: string;
    purpose?: 'WALLET_TOPUP' | 'SUBSCRIPTION';
    subscriptionPackageId?: number;
  }): Observable<WalletDepositRow> {
    return this.http.post<PlatformWalletApiResponse>(`${this.frontendBase}/deposits`, payload).pipe(
      map((res) => {
        if (isApiFailureEnvelope(res)) {
          throw new Error(readApiFailureMessage(res, 'Could not submit deposit.'));
        }
        const row = res.walletDepositDto;
        if (!row?.id) {
          throw new Error(readApiFailureMessage(res, 'Deposit was not created — check billing-payments is running.'));
        }
        return row;
      }),
      catchError((err) => throwError(() => this.toDepositError(err, 'Could not submit deposit.'))),
    );
  }

  listDeposits(): Observable<WalletDepositRow[]> {
    return this.http.get<PlatformWalletApiResponse>(`${this.frontendBase}/deposits`).pipe(
      map((res) => {
        if (isApiFailureEnvelope(res)) {
          throw new Error(readApiFailureMessage(res, 'Could not load deposits.'));
        }
        return res.walletDepositDtoList ?? [];
      }),
      catchError((err: unknown) => throwError(() => (err instanceof Error ? err : new Error('Could not load deposits.')))),
    );
  }

  listTransactions(): Observable<WalletTransactionRow[]> {
    return this.http.get<PlatformWalletApiResponse>(`${this.frontendBase}/transactions`).pipe(
      map((res) => {
        if (isApiFailureEnvelope(res)) {
          throw new Error(readApiFailureMessage(res, 'Could not load transactions.'));
        }
        return res.walletTransactionDtoList ?? [];
      }),
      catchError((err: unknown) => throwError(() => (err instanceof Error ? err : new Error('Could not load transactions.')))),
    );
  }

  getTransactionReceipt(transactionId: number): Observable<{ receiptHtml: string; transaction: WalletTransactionRow }> {
    return this.http.get<PlatformWalletApiResponse>(`${this.frontendBase}/transactions/${transactionId}/receipt`).pipe(
      map((res) => ({
        receiptHtml: res.receiptHtml ?? '',
        transaction: res.walletTransactionDto ?? { id: transactionId, transactionType: 'DEPOSIT', amountCents: 0, balanceAfterCents: 0 },
      })),
    );
  }

  downloadReceipt(transactionId: number, receiptNumber?: string): void {
    this.http
      .get(`${this.frontendBase}/transactions/${transactionId}/receipt/pdf`, {
        responseType: 'blob',
      })
      .pipe(
        catchError((err) => throwError(() => this.toDepositError(err, 'Could not download receipt PDF.'))),
      )
      .subscribe({
        next: (blob) => {
          const filename = `${receiptNumber ?? `receipt-${transactionId}`}.pdf`;
          downloadBlob(blob, filename);
        },
      });
  }

  isWalletFrozen(summary: PlatformWalletSummary | null | undefined): boolean {
    if (!summary || summary.billingMode !== 'PREPAID_WALLET') {
      return false;
    }
    return summary.walletFrozen === true || summary.platformAccessAllowed === false;
  }

  listActiveActionCharges(): Observable<PlatformActionChargeRow[]> {
    return this.http.get<PlatformWalletApiResponse>(`${this.frontendBase}/action-charges`).pipe(
      map((res) => {
        if (isApiFailureEnvelope(res)) {
          throw new Error(readApiFailureMessage(res, 'Could not load action charges.'));
        }
        return extractDtoList<PlatformActionChargeRow>(res, 'platformActionChargeDtoList');
      }),
      catchError((err: unknown) => throwError(() => (err instanceof Error ? err : new Error('Could not load action charges.')))),
    );
  }

  /** Public marketing catalog — no auth required (system surface; gateway permits /v1/system/**). */
  getPublicPricingCatalog(): Observable<{
    packages: SubscriptionPackageRow[];
    actionCharges: PlatformActionChargeRow[];
  }> {
    const url = ldmsServiceUrl('billing-payments', 'platform-wallet', 'pricing-catalog', 'system');
    return this.http.get<PlatformWalletApiResponse>(url).pipe(
      map((res) => {
        if (isApiFailureEnvelope(res)) {
          throw new Error(readApiFailureMessage(res, 'Could not load pricing catalog.'));
        }
        const packages = extractDtoList<SubscriptionPackageRow>(res, 'subscriptionPackageDtoList').filter(
          (row) => row.active !== false,
        );
        const actionCharges = extractDtoList<PlatformActionChargeRow>(res, 'platformActionChargeDtoList').filter(
          (row) => row.active !== false,
        );
        return { packages, actionCharges };
      }),
      catchError((err: unknown) => {
        console.warn('[PlatformWalletService] Public pricing catalog request failed', err);
        return throwError(() => err);
      }),
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
      map((res) => {
        if (isApiFailureEnvelope(res)) {
          throw new Error(readApiFailureMessage(res, 'Could not load action charges.'));
        }
        return extractDtoList<PlatformActionChargeRow>(res, 'platformActionChargeDtoList');
      }),
      catchError((err: unknown) => throwError(() => (err instanceof Error ? err : new Error('Could not load action charges.')))),
    );
  }

  saveActionCharge(payload: PlatformActionChargeRow, adminMode = true): Observable<PlatformActionChargeRow> {
    const body = {
      ...payload,
      id: payload.id ?? undefined,
      chargeCents: Number(payload.chargeCents ?? 0),
    };
    return this.http.put<PlatformWalletApiResponse>(`${this.base(adminMode)}/action-charges`, body).pipe(
      map((res) => {
        if (isApiFailureEnvelope(res)) {
          throw new Error(readApiFailureMessage(res, 'Could not save action charge.'));
        }
        return res.platformActionChargeDto ?? { ...payload, ...body };
      }),
      catchError((err: unknown) => throwError(() => (err instanceof Error ? err : new Error('Could not save action charge.')))),
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

  rejectDeposit(depositId: number): Observable<WalletDepositRow> {
    return this.http.post<PlatformWalletApiResponse>(`${this.backofficeBase}/deposits/${depositId}/reject`, {}).pipe(
      map((res) => res.walletDepositDto ?? { id: depositId, amountCents: 0, currencyCode: 'USD', status: 'REJECTED' }),
    );
  }

  creditOrganization(payload: {
    organizationId: number;
    organizationName?: string;
    amountCents: number;
    currencyCode?: string;
    notes?: string;
    enablePrepaidBilling?: boolean;
  }): Observable<PlatformWalletSummary> {
    return this.http.post<PlatformWalletApiResponse>(`${this.backofficeBase}/organizations/credit`, payload).pipe(
      map((res) => res.platformWalletSummaryDto ?? { balanceCents: payload.amountCents, currencyCode: 'USD', billingMode: 'PREPAID_WALLET' }),
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

  private toDepositError(err: unknown, fallback: string): Error {
    if (err instanceof Error && err.message && err.message !== fallback) {
      return err;
    }
    if (err instanceof HttpErrorResponse) {
      const body = err.error;
      if (body && typeof body === 'object') {
        const message = readApiFailureMessage(body, fallback);
        if (message !== fallback) {
          return new Error(message);
        }
      }
      if (err.status === 503) {
        return new Error('Billing service is unavailable. Start ldms-billing-payments and try again.');
      }
      if (err.status === 401 || err.status === 403) {
        return new Error('Session expired or access denied. Sign in again and retry.');
      }
    }
    return new Error(fallback);
  }
}
