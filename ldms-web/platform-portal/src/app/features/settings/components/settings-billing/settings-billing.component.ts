import { ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { Subject, catchError, finalize, forkJoin, of, takeUntil } from 'rxjs';
import {
  PlatformWalletService,
  type OrganizationBillingMode,
  type PlatformActionChargeRow,
  type PlatformWalletSummary,
  type SubscriptionPackageRow,
  type WalletTransactionRow,
} from '../../../../core/services/platform-wallet.service';
import { PLATFORM_BILLING_MODULES, type PlatformBillingModuleMeta } from '../../utils/platform-billing-modules.util';

@Component({
  selector: 'app-settings-billing',
  templateUrl: './settings-billing.component.html',
  styleUrl: './settings-billing.component.scss',
  standalone: false,
})
export class SettingsBillingComponent implements OnInit, OnDestroy {
  @Input() adminMode = false;

  loading = true;
  saving = false;
  submittingDeposit = false;
  error = '';

  billingMode: OrganizationBillingMode = 'PREPAID_WALLET';
  subscriptionPackageId: number | null = null;
  lowBalanceThresholdCents = 500;
  packages: SubscriptionPackageRow[] = [];
  actionCharges: PlatformActionChargeRow[] = [];
  transactions: WalletTransactionRow[] = [];
  walletSummary: PlatformWalletSummary | null = null;
  readonly billingModules = PLATFORM_BILLING_MODULES;
  depositAmount = '';
  depositReference = '';
  depositNotes = '';
  recentDeposits: Array<{ id: number; amountCents: number; status: string; createdAt?: string }> = [];

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly wallet: PlatformWalletService,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.reload();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get chargeCategories(): string[] {
    return this.filteredChargeCategories;
  }

  chargesForModule(category: string): PlatformActionChargeRow[] {
    return this.actionCharges.filter((row) => (row.category ?? 'GENERAL') === category);
  }

  chargesForCategory(category: string): PlatformActionChargeRow[] {
    return this.chargesForModule(category);
  }

  get filteredActionCharges(): PlatformActionChargeRow[] {
    return this.actionCharges;
  }

  get filteredChargeCategories(): string[] {
    const categories = new Set(this.actionCharges.map((row) => row.category ?? 'GENERAL'));
    return [...categories].sort();
  }

  reload(): void {
    this.loading = true;
    this.error = '';
    forkJoin({
      setting: this.wallet.getBillingSetting().pipe(
        catchError(() =>
          of({
            billingMode: 'PREPAID_WALLET' as OrganizationBillingMode,
            subscriptionPackageId: null,
            lowBalanceThresholdCents: 500,
          }),
        ),
      ),
      summary: this.wallet.refreshSummary().pipe(
        catchError(() =>
          of({
            balanceCents: 0,
            currencyCode: 'USD',
            billingMode: 'PREPAID_WALLET' as OrganizationBillingMode,
          }),
        ),
      ),
      packages: this.wallet.listSubscriptionPackages(false).pipe(catchError(() => of([]))),
      actionCharges: this.wallet.listActiveActionCharges().pipe(catchError(() => of([]))),
      transactions: this.adminMode ? of([]) : this.wallet.listTransactions().pipe(catchError(() => of([]))),
      deposits: this.adminMode ? of([]) : this.wallet.listDeposits().pipe(catchError(() => of([]))),
    })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe(({ setting, summary, packages, actionCharges, transactions, deposits }) => {
        this.billingMode = setting.billingMode ?? 'PREPAID_WALLET';
        this.subscriptionPackageId = setting.subscriptionPackageId ?? null;
        this.lowBalanceThresholdCents = setting.lowBalanceThresholdCents ?? 500;
        this.walletSummary = summary;
        this.packages = packages;
        this.actionCharges = actionCharges;
        this.transactions = transactions.slice(0, 12);
        this.recentDeposits = deposits.slice(0, 8);
      });
  }

  save(): void {
    this.saving = true;
    this.wallet
      .saveBillingSetting({
        billingMode: this.billingMode,
        subscriptionPackageId: this.billingMode === 'PREMIUM_SUBSCRIPTION' ? this.subscriptionPackageId ?? undefined : null,
        lowBalanceThresholdCents: this.lowBalanceThresholdCents,
      })
      .pipe(
        finalize(() => {
          this.saving = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: () => this.snackBar.open('Billing settings saved.', 'Close', { duration: 3500 }),
        error: () => this.snackBar.open('Could not save billing settings.', 'Close', { duration: 4500 }),
      });
  }

  submitDeposit(): void {
    const amountCents = Math.round(parseFloat(this.depositAmount || '0') * 100);
    if (!amountCents || amountCents < 1) {
      this.snackBar.open('Enter a valid deposit amount.', 'Close', { duration: 3500 });
      return;
    }
    this.submittingDeposit = true;
    this.wallet
      .createDeposit({
        amountCents,
        referenceNumber: this.depositReference || undefined,
        notes: this.depositNotes || undefined,
      })
      .pipe(
        finalize(() => {
          this.submittingDeposit = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          this.depositAmount = '';
          this.depositReference = '';
          this.depositNotes = '';
          this.snackBar.open('Deposit submitted — we will credit your wallet after confirmation.', 'Close', { duration: 5000 });
          this.reload();
        },
        error: () => this.snackBar.open('Could not submit deposit.', 'Close', { duration: 4500 }),
      });
  }

  openUsageReport(): void {
    void this.router.navigate(['/reports/usage-charges']);
  }

  formatMoney(cents: number, currencyCode?: string): string {
    return this.wallet.formatCents(cents, currencyCode ?? this.walletSummary?.currencyCode ?? 'USD');
  }

  packagePrice(pkg: SubscriptionPackageRow): string {
    return this.wallet.formatCents(pkg.monthlyPriceCents, pkg.currencyCode);
  }

  categoryLabel(category: string): string {
    const mod = this.billingModules.find((m: PlatformBillingModuleMeta) => m.category === category);
    return mod?.label ?? category.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
  }

  transactionLabel(row: WalletTransactionRow): string {
    return row.description?.trim() || row.actionCode?.replace(/_/g, ' ') || row.transactionType;
  }

  isDebit(row: WalletTransactionRow): boolean {
    return row.transactionType === 'CHARGE' || row.amountCents < 0;
  }

  absCents(cents: number): number {
    return Math.abs(cents ?? 0);
  }
}
