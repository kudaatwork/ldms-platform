import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import {
  extractDtoList,
  isApiFailureEnvelope,
  readApiFailureMessage,
} from '../../../core/utils/api-paged-response.util';

export interface PlatformActionChargeRow {
  id?: number;
  actionCode: string;
  displayName: string;
  description?: string;
  chargeCents: number;
  category?: string;
  billingTier?: string;
  active?: boolean;
}

export interface SubscriptionPackageRow {
  id?: number;
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
}

export interface WalletDepositRow {
  id: number;
  organizationId?: number;
  organizationName?: string;
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
  rejectionReason?: string;
  receiptEmailStatus?: string;
  receiptEmailAddress?: string;
  receiptEmailAt?: string;
  createdAt?: string;
  modifiedAt?: string;
  modifiedBy?: string;
}

interface PlatformWalletApiResponse {
  platformActionChargeDtoList?: PlatformActionChargeRow[];
  platformActionChargeDto?: PlatformActionChargeRow;
  subscriptionPackageDtoList?: SubscriptionPackageRow[];
  subscriptionPackageDto?: SubscriptionPackageRow;
  walletDepositDtoList?: WalletDepositRow[];
  walletDepositDto?: WalletDepositRow;
  receiptHtml?: string;
}

@Injectable({ providedIn: 'root' })
export class PlatformWalletAdminService {
  private readonly backofficeBase = ldmsServiceUrl('billing-payments', 'platform-wallet', undefined, 'backoffice');

  constructor(private readonly http: HttpClient) {}

  listActionCharges(): Observable<PlatformActionChargeRow[]> {
    return this.http.get<PlatformWalletApiResponse>(`${this.backofficeBase}/action-charges`).pipe(
      map((res) => this.parseList<PlatformActionChargeRow>(res, 'platformActionChargeDtoList', 'Failed to load action charges.')),
      catchError((err) => throwError(() => this.toError(err, 'Failed to load action charges.'))),
    );
  }

  getActionCharge(chargeId: number): Observable<PlatformActionChargeRow> {
    return this.http.get<PlatformWalletApiResponse>(`${this.backofficeBase}/action-charges/${chargeId}`).pipe(
      map((res) => {
        this.assertSuccess(res, 'Could not load action charge.');
        return res.platformActionChargeDto ?? ({} as PlatformActionChargeRow);
      }),
      catchError((err) => throwError(() => this.toError(err, 'Could not load action charge.'))),
    );
  }

  saveActionCharge(payload: PlatformActionChargeRow): Observable<PlatformActionChargeRow> {
    const body = {
      ...payload,
      id: payload.id != null && payload.id > 0 ? payload.id : undefined,
      chargeCents: Math.round(Number(payload.chargeCents ?? 0)),
    };
    return this.http.put<PlatformWalletApiResponse>(`${this.backofficeBase}/action-charges`, body).pipe(
      map((res) => {
        this.assertSuccess(res, 'Could not save action charge.');
        const saved = res.platformActionChargeDto;
        if (!saved?.actionCode) {
          throw new Error(
            'Billing service did not return the saved charge. Ensure ldms-billing-payments is running on the latest build, then retry.',
          );
        }
        return saved;
      }),
      catchError((err) => throwError(() => this.toError(err, 'Could not save action charge.'))),
    );
  }

  deleteActionCharge(chargeId: number): Observable<void> {
    return this.http.delete<PlatformWalletApiResponse>(`${this.backofficeBase}/action-charges/${chargeId}`).pipe(
      map((res) => {
        this.assertSuccess(res, 'Could not delete action charge.');
        return undefined;
      }),
      catchError((err) => throwError(() => this.toError(err, 'Could not delete action charge.'))),
    );
  }

  listSubscriptionPackages(): Observable<SubscriptionPackageRow[]> {
    return this.http.get<PlatformWalletApiResponse>(`${this.backofficeBase}/subscription-packages`).pipe(
      map((res) => this.parseList<SubscriptionPackageRow>(res, 'subscriptionPackageDtoList', 'Failed to load subscription packages.')),
      catchError((err) => throwError(() => this.toError(err, 'Failed to load subscription packages.'))),
    );
  }

  saveSubscriptionPackage(payload: SubscriptionPackageRow): Observable<SubscriptionPackageRow> {
    return this.http.post<PlatformWalletApiResponse>(`${this.backofficeBase}/subscription-packages`, payload).pipe(
      map((res) => {
        this.assertSuccess(res, 'Could not save subscription package.');
        return res.subscriptionPackageDto ?? payload;
      }),
      catchError((err) => throwError(() => this.toError(err, 'Could not save subscription package.'))),
    );
  }

  deleteSubscriptionPackage(packageId: number): Observable<void> {
    return this.http.delete<PlatformWalletApiResponse>(`${this.backofficeBase}/subscription-packages/${packageId}`).pipe(
      map((res) => {
        this.assertSuccess(res, 'Could not delete subscription package.');
        return undefined;
      }),
      catchError((err) => throwError(() => this.toError(err, 'Could not delete subscription package.'))),
    );
  }

  listPendingDeposits(): Observable<WalletDepositRow[]> {
    return this.http.get<PlatformWalletApiResponse>(`${this.backofficeBase}/deposits/pending`).pipe(
      map((res) => this.parseList<WalletDepositRow>(res, 'walletDepositDtoList', 'Failed to load pending deposits.')),
      catchError((err) => throwError(() => this.toError(err, 'Failed to load pending deposits.'))),
    );
  }

  listConfirmedDeposits(): Observable<WalletDepositRow[]> {
    return this.http.get<PlatformWalletApiResponse>(`${this.backofficeBase}/deposits/confirmed`).pipe(
      map((res) => this.parseList<WalletDepositRow>(res, 'walletDepositDtoList', 'Failed to load approved deposits.')),
      catchError((err) => throwError(() => this.toError(err, 'Failed to load approved deposits.'))),
    );
  }

  confirmDeposit(depositId: number): Observable<void> {
    return this.http.post<PlatformWalletApiResponse>(`${this.backofficeBase}/deposits/${depositId}/confirm`, {}).pipe(
      map((res) => {
        this.assertSuccess(res, 'Could not confirm deposit.');
        return undefined;
      }),
      catchError((err) => throwError(() => this.toError(err, 'Could not confirm deposit.'))),
    );
  }

  rejectDeposit(depositId: number, rejectionReason: string): Observable<void> {
    return this.http.post<PlatformWalletApiResponse>(`${this.backofficeBase}/deposits/${depositId}/reject`, { id: depositId, rejectionReason }).pipe(
      map((res) => {
        this.assertSuccess(res, 'Could not reject deposit.');
        return undefined;
      }),
      catchError((err) => throwError(() => this.toError(err, 'Could not reject deposit.'))),
    );
  }

  resendDepositReceiptEmail(depositId: number): Observable<void> {
    return this.http.post<PlatformWalletApiResponse>(`${this.backofficeBase}/deposits/${depositId}/receipt/resend`, {}).pipe(
      map((res) => {
        this.assertSuccess(res, 'Could not re-send the receipt email.');
        return undefined;
      }),
      catchError((err) => throwError(() => this.toError(err, 'Could not re-send the receipt email.'))),
    );
  }

  /** Receipt HTML for an approved deposit (rendered inline in the detail dialog). */
  getDepositReceiptHtml(depositId: number): Observable<string> {
    return this.http.get<PlatformWalletApiResponse>(`${this.backofficeBase}/deposits/${depositId}/receipt`).pipe(
      map((res) => {
        this.assertSuccess(res, 'Could not load the receipt.');
        return res.receiptHtml ?? '';
      }),
      catchError((err) => throwError(() => this.toError(err, 'Could not load the receipt.'))),
    );
  }

  /** Downloads the approved-deposit receipt as a PDF blob. */
  downloadDepositReceiptPdf(depositId: number): Observable<Blob> {
    return this.http
      .get(`${this.backofficeBase}/deposits/${depositId}/receipt/pdf`, { responseType: 'blob' })
      .pipe(catchError((err) => throwError(() => this.toError(err, 'Could not download the receipt PDF.'))));
  }

  creditOrganization(payload: {
    organizationId: number;
    organizationName?: string;
    amountCents: number;
    currencyCode?: string;
    notes?: string;
    enablePrepaidBilling?: boolean;
  }): Observable<void> {
    return this.http.post<PlatformWalletApiResponse>(`${this.backofficeBase}/organizations/credit`, payload).pipe(
      map((res) => {
        this.assertSuccess(res, 'Could not credit organisation wallet.');
        return undefined;
      }),
      catchError((err) => throwError(() => this.toError(err, 'Could not credit organisation wallet.'))),
    );
  }

  formatCents(cents: number, currencyCode = 'USD'): string {
    const amount = (cents ?? 0) / 100;
    try {
      return new Intl.NumberFormat(undefined, { style: 'currency', currency: currencyCode }).format(amount);
    } catch {
      return `${currencyCode} ${amount.toFixed(2)}`;
    }
  }

  formatWhen(iso?: string): string {
    if (!iso) {
      return '—';
    }
    try {
      return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(iso));
    } catch {
      return iso;
    }
  }

  private parseList<T>(res: PlatformWalletApiResponse, listKey: keyof PlatformWalletApiResponse, fallback: string): T[] {
    this.assertSuccess(res, fallback);
    const direct = res[listKey];
    if (Array.isArray(direct)) {
      return direct as T[];
    }
    return extractDtoList<T>(res, String(listKey));
  }

  private assertSuccess(res: unknown, fallback: string): void {
    if (isApiFailureEnvelope(res)) {
      throw new Error(readApiFailureMessage(res, fallback));
    }
  }

  private toError(err: unknown, fallback: string): Error {
    if (err instanceof Error && err.message && err.message !== fallback) {
      return err;
    }
    const http = err as { status?: number; statusText?: string; error?: unknown; message?: string };
    const httpBody = http?.error;
    if (httpBody) {
      return new Error(readApiFailureMessage(httpBody, fallback));
    }
    if (http?.status === 0) {
      return new Error('Cannot reach billing-payments. Check that the API gateway and billing service are running.');
    }
    if (http?.status === 401 || http?.status === 403) {
      const denied = httpBody ? readApiFailureMessage(httpBody, '') : '';
      if (denied) {
        return new Error(denied);
      }
      return new Error('Session expired or insufficient permissions. Sign in again and retry.');
    }
    if (http?.status && http.status >= 500) {
      return new Error(`Billing service error (${http.status}). Restart ldms-billing-payments and retry.`);
    }
    if (http?.message && http.message !== 'Http failure response') {
      return new Error(http.message);
    }
    return new Error(fallback);
  }
}
