import { AfterViewInit, ChangeDetectorRef, Component, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableDataSource } from '@angular/material/table';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, catchError, finalize, forkJoin, Observable, of, switchMap, takeUntil } from 'rxjs';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import {
  PlatformWalletService,
  type OrganizationBillingMode,
  type PlatformActionChargeRow,
  type PlatformWalletSummary,
  type SubscriptionPackageRow,
  type SubscriptionQuotaMeter,
  type WalletDepositPaymentMethod,
  type WalletDepositRow,
  type WalletTransactionRow,
} from '../../../../core/services/platform-wallet.service';
import { DEFAULT_TABLE_PAGE_SIZE_OPTIONS } from '../../../../shared/constants/table-pagination';
import {
  exportClientTableAsCsv,
  exportFormatLabel,
  type LxExportColumn,
  type LxExportFormat,
} from '../../../../shared/utils/lx-export.util';
import { PLATFORM_BILLING_MODULES, type PlatformBillingModuleMeta } from '../../utils/platform-billing-modules.util';
import { packageFeaturePoints } from '../../../../shared/utils/subscription-package-description.util';
import {
  isFuelConsumptionBillingAction,
  isFuelConsumptionFeatureEnabled,
} from '../../../../shared/utils/fuel-consumption.util';

interface PaymentMethodOption {
  id: WalletDepositPaymentMethod;
  label: string;
  hint: string;
  icon: string;
  online?: boolean;
  comingSoon?: boolean;
}

interface BillingHistoryRow {
  key: string;
  id?: string;
  type?: string;
  sortAt: string;
  typeLabel: string;
  description: string;
  amountCents: number;
  currencyCode?: string;
  status?: string;
  emailSent?: boolean;
  balanceAfterCents?: number;
  receiptNumber?: string;
  transactionId?: number;
  isDebit: boolean;
  canDownloadReceipt: boolean;
}

@Component({
  selector: 'app-settings-billing',
  templateUrl: './settings-billing.component.html',
  styleUrl: './settings-billing.component.scss',
  standalone: false,
})
export class SettingsBillingComponent implements OnInit, OnDestroy, AfterViewInit {
  @Input() adminMode = false;

  loading = true;
  saving = false;
  submittingDeposit = false;
  uploadingProof = false;
  error = '';
  showFrozenBanner = false;

  billingMode: OrganizationBillingMode = 'PREPAID_WALLET';
  subscriptionPackageId: number | null = null;
  lowBalanceThresholdCents = 500;
  packages: SubscriptionPackageRow[] = [];
  actionCharges: PlatformActionChargeRow[] = [];
  deposits: WalletDepositRow[] = [];
  transactions: WalletTransactionRow[] = [];
  allBillingHistory: BillingHistoryRow[] = [];
  historyDataSource = new MatTableDataSource<BillingHistoryRow>([]);
  historySearchQuery = '';
  historyFilterFieldsOpen = false;
  historyExporting = false;
  historyFilters = { type: '', status: '', description: '', dateFrom: '', dateTo: '' };
  readonly historyPageSizeOptions = DEFAULT_TABLE_PAGE_SIZE_OPTIONS;
  walletSummary: PlatformWalletSummary | null = null;
  readonly billingModules = PLATFORM_BILLING_MODULES;
  readonly billingHistoryColumns = ['when', 'type', 'description', 'amount', 'status', 'email', 'actions', 'balance'];
  depositAmount = '';
  depositReference = '';
  depositNotes = '';
  depositPaymentMethod: WalletDepositPaymentMethod = 'BANK_TRANSFER';
  depositProofFile: File | null = null;

  submittingSubscription = false;
  subscriptionReference = '';
  subscriptionProofFile: File | null = null;

  readonly paymentMethods: PaymentMethodOption[] = [
    { id: 'BANK_TRANSFER', label: 'Bank transfer', hint: 'EFT or RTGS to LX — attach proof of payment', icon: 'account_balance' },
    { id: 'CASH', label: 'Cash deposit', hint: 'Paid in person — reference the receipt number', icon: 'payments' },
    { id: 'ECOCASH', label: 'EcoCash', hint: 'Zimbabwe mobile money', icon: 'smartphone', online: true, comingSoon: true },
    { id: 'PAYNOW', label: 'PayNow', hint: 'Zimbabwe mobile & bank rails', icon: 'account_balance_wallet', online: true, comingSoon: true },
    { id: 'PAYPAL', label: 'PayPal', hint: 'International checkout', icon: 'account_balance_wallet', online: true, comingSoon: true },
    { id: 'MASTERCARD', label: 'Mastercard', hint: 'Card payments via LX', icon: 'credit_card', online: true, comingSoon: true },
  ];

  private readonly destroy$ = new Subject<void>();

  @ViewChild('historyPaginator')
  set historyPaginatorRef(paginator: MatPaginator | undefined) {
    if (paginator) {
      this.historyDataSource.paginator = paginator;
    }
  }

  constructor(
    private readonly wallet: PlatformWalletService,
    private readonly authState: AuthStateService,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      this.showFrozenBanner = params.get('frozen') === '1';
      this.cdr.markForCheck();
    });
    this.reload();
  }

  ngAfterViewInit(): void {
    this.applyHistoryFilters();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get chargeCategories(): string[] {
    return this.filteredChargeCategories;
  }

  get walletFrozen(): boolean {
    return this.wallet.isWalletFrozen(this.walletSummary);
  }

  get quotaMeters(): SubscriptionQuotaMeter[] {
    return this.walletSummary?.subscriptionQuotas ?? [];
  }

  get hasExhaustedQuota(): boolean {
    return this.quotaMeters.some((meter) => meter.exhausted);
  }

  meterUsagePct(meter: SubscriptionQuotaMeter): number {
    const included = meter.includedMonthly ?? 0;
    if (included <= 0) {
      return 0;
    }
    return Math.min(100, Math.round(((meter.usedThisPeriod ?? 0) / included) * 100));
  }

  trackMeter(_: number, meter: SubscriptionQuotaMeter): string {
    return meter.code;
  }

  get selectedPaymentMethod(): PaymentMethodOption {
    return this.paymentMethods.find((m) => m.id === this.depositPaymentMethod) ?? this.paymentMethods[0];
  }

  get canSubmitDeposit(): boolean {
    if (this.selectedPaymentMethod.comingSoon) {
      return false;
    }
    const amountCents = Math.round(parseFloat(this.depositAmount || '0') * 100);
    if (!amountCents || amountCents < 1) {
      return false;
    }
    if (this.depositPaymentMethod === 'BANK_TRANSFER' && !this.depositProofFile && !this.depositReference.trim()) {
      return false;
    }
    return true;
  }

  chargesForModule(category: string): PlatformActionChargeRow[] {
    return this.visibleActionCharges.filter((row) => (row.category ?? 'GENERAL') === category);
  }

  get visibleActionCharges(): PlatformActionChargeRow[] {
    const fuelEnabled = isFuelConsumptionFeatureEnabled(this.authState.currentUser?.fuelConsumptionEnabled);
    if (fuelEnabled) {
      return this.actionCharges;
    }
    return this.actionCharges.filter((row) => !isFuelConsumptionBillingAction(row.actionCode));
  }

  chargesForCategory(category: string): PlatformActionChargeRow[] {
    return this.chargesForModule(category);
  }

  get filteredActionCharges(): PlatformActionChargeRow[] {
    return this.visibleActionCharges;
  }

  get filteredChargeCategories(): string[] {
    const categories = new Set(this.visibleActionCharges.map((row) => row.category ?? 'GENERAL'));
    return [...categories].sort();
  }

  get filteredBillingHistory(): BillingHistoryRow[] {
    const q = this.historySearchQuery.trim().toLowerCase();
    return this.allBillingHistory.filter((row) => {
      if (q) {
        const haystack = `${row.typeLabel} ${row.description} ${row.status ?? 'Posted'} ${row.receiptNumber ?? ''} ${row.amountCents}`.toLowerCase();
        if (!haystack.includes(q)) {
          return false;
        }
      }
      if (this.historyFilters.type && row.typeLabel !== this.historyFilters.type) {
        return false;
      }
      if (this.historyFilters.status) {
        const want = this.historyFilters.status.trim();
        if (want === 'Posted') {
          if (row.status) {
            return false;
          }
        } else if ((row.status ?? '').toUpperCase() !== want.toUpperCase()) {
          return false;
        }
      }
      if (this.historyFilters.description) {
        const needle = this.historyFilters.description.trim().toLowerCase();
        if (!row.description.toLowerCase().includes(needle)) {
          return false;
        }
      }
      if (this.historyFilters.dateFrom) {
        const fromMs = Date.parse(`${this.historyFilters.dateFrom}T00:00:00`);
        const rowMs = Date.parse(row.sortAt);
        if (Number.isFinite(fromMs) && Number.isFinite(rowMs) && rowMs < fromMs) {
          return false;
        }
      }
      if (this.historyFilters.dateTo) {
        const toMs = Date.parse(`${this.historyFilters.dateTo}T23:59:59.999`);
        const rowMs = Date.parse(row.sortAt);
        if (Number.isFinite(toMs) && Number.isFinite(rowMs) && rowMs > toMs) {
          return false;
        }
      }
      return true;
    });
  }

  get historyTypeOptions(): string[] {
    return [...new Set(this.allBillingHistory.map((row) => row.typeLabel))].sort();
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
      transactions: this.adminMode
        ? of([])
        : this.wallet.listTransactions().pipe(
            catchError((err: unknown) => {
              const message = err instanceof Error ? err.message : 'Could not load transactions.';
              this.snackBar.open(message, 'Close', { duration: 6000 });
              return of([]);
            }),
          ),
      deposits: this.adminMode
        ? of([])
        : this.wallet.listDeposits().pipe(
            catchError((err: unknown) => {
              const message = err instanceof Error ? err.message : 'Could not load deposits.';
              this.snackBar.open(message, 'Close', { duration: 6000 });
              return of([]);
            }),
          ),
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
        this.deposits = deposits;
        this.transactions = transactions;
        this.allBillingHistory = this.buildBillingHistory(deposits, transactions);
        this.applyHistoryFilters();
        if (this.wallet.isWalletFrozen(summary)) {
          this.showFrozenBanner = true;
        }
      });
  }

  refreshHistory(): void {
    this.reload();
  }

  resetHistoryPaging(): void {
    this.applyHistoryFilters();
    this.historyDataSource.paginator?.firstPage();
  }

  applyHistoryFilters(): void {
    this.historyDataSource.data = this.filteredBillingHistory;
    this.cdr.markForCheck();
  }

  exportHistoryAs(format: LxExportFormat): void {
    if (this.historyExporting) {
      return;
    }
    const rows = this.filteredBillingHistory;
    if (!rows.length) {
      this.snackBar.open('No billing history rows match your filters.', 'Close', { duration: 3500 });
      return;
    }
    this.historyExporting = true;
    const columns: LxExportColumn<BillingHistoryRow>[] = [
      { header: 'Date', value: (row) => this.formatWhen(row.sortAt) },
      { header: 'Type', value: (row) => row.typeLabel },
      { header: 'Description', value: (row) => row.description },
      {
        header: 'Amount',
        value: (row) => `${row.isDebit ? '-' : '+'}${this.formatMoney(this.absCents(row.amountCents), row.currencyCode)}`,
      },
      { header: 'Status', value: (row) => row.status ?? 'Posted' },
      {
        header: 'Balance after',
        value: (row) => (row.balanceAfterCents != null ? this.formatMoney(row.balanceAfterCents, row.currencyCode) : ''),
      },
      { header: 'Receipt', value: (row) => row.receiptNumber ?? '' },
    ];
    const saved = exportClientTableAsCsv(
      format,
      rows,
      columns,
      'billing-history',
      (message) => this.snackBar.open(message, 'Close', { duration: 4500 }),
      { title: 'Billing history' },
    );
    if (saved) {
      this.snackBar.open(`Exported billing history as ${exportFormatLabel(format)}.`, 'Close', { duration: 3500 });
    }
    this.historyExporting = false;
    this.cdr.markForCheck();
  }

  save(): void {
    // Activating a subscription is gated behind a paid, LX-approved payment — it cannot be
    // self-applied here. Switching back to pay-as-you-go is allowed.
    if (this.billingMode === 'PREMIUM_SUBSCRIPTION' && !this.subscriptionActive) {
      this.snackBar.open(
        'Submit a subscription payment below — LX activates your package after confirming payment.',
        'Close',
        { duration: 5500 },
      );
      return;
    }
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

  selectPaymentMethod(method: WalletDepositPaymentMethod): void {
    const option = this.paymentMethods.find((m) => m.id === method);
    if (option?.comingSoon) {
      this.snackBar.open(`${option.label} checkout is coming soon. Use bank transfer or cash for now.`, 'Close', {
        duration: 5000,
      });
      return;
    }
    this.depositPaymentMethod = method;
  }

  onDepositProofSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.depositProofFile = input.files?.[0] ?? null;
    this.cdr.markForCheck();
  }

  // ── Subscription activation (LX-approved, paid with proof) ────────────────

  get selectedPackage(): SubscriptionPackageRow | null {
    return this.packages.find((p) => p.id === this.subscriptionPackageId) ?? null;
  }

  /** The org's pending subscription activation awaiting LX approval, if any. */
  get pendingSubscription(): WalletDepositRow | null {
    return (
      this.deposits.find(
        (d) => (d.purpose ?? '').toUpperCase() === 'SUBSCRIPTION' && (d.status ?? '').toUpperCase() === 'PENDING',
      ) ?? null
    );
  }

  get subscriptionActive(): boolean {
    return this.walletSummary?.billingMode === 'PREMIUM_SUBSCRIPTION' && !!this.walletSummary?.subscriptionPackageName;
  }

  get subscriptionEndsAt(): string | null {
    return this.walletSummary?.subscriptionRenewsAt ?? null;
  }

  get canSubmitSubscription(): boolean {
    if (!this.selectedPackage || this.selectedPaymentMethod.comingSoon) {
      return false;
    }
    if (this.depositPaymentMethod === 'BANK_TRANSFER' && !this.subscriptionProofFile && !this.subscriptionReference.trim()) {
      return false;
    }
    return true;
  }

  onSubscriptionProofSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.subscriptionProofFile = input.files?.[0] ?? null;
    this.cdr.markForCheck();
  }

  submitSubscriptionActivation(): void {
    const pkg = this.selectedPackage;
    if (!pkg) {
      this.snackBar.open('Select a package to subscribe to.', 'Close', { duration: 3500 });
      return;
    }
    if (this.selectedPaymentMethod.comingSoon) {
      this.snackBar.open('This payment channel is not live yet.', 'Close', { duration: 4000 });
      return;
    }
    if (this.depositPaymentMethod === 'BANK_TRANSFER' && !this.subscriptionProofFile && !this.subscriptionReference.trim()) {
      this.snackBar.open('Attach proof of payment or enter a bank reference.', 'Close', { duration: 4500 });
      return;
    }

    const orgIdRaw = this.authState.currentUser?.organizationId ?? this.walletSummary?.organizationId;
    const orgId = typeof orgIdRaw === 'string' ? Number(orgIdRaw) : orgIdRaw;
    if (!orgId || orgId < 1) {
      this.snackBar.open('Could not resolve your organisation.', 'Close', { duration: 4500 });
      return;
    }

    this.submittingSubscription = true;
    const proof$: Observable<number | undefined> = this.subscriptionProofFile
      ? this.wallet.uploadPaymentProof(orgId, this.subscriptionProofFile)
      : of(undefined);

    proof$
      .pipe(
        switchMap((proofDocumentId) => {
          if (this.subscriptionProofFile && !proofDocumentId) {
            throw new Error('Proof of payment upload did not return a document id.');
          }
          const { gatewayProvider, paymentMethod } = this.mapPaymentMethod(this.depositPaymentMethod);
          return this.wallet.createDeposit({
            amountCents: pkg.monthlyPriceCents,
            currencyCode: pkg.currencyCode,
            referenceNumber: this.subscriptionReference || undefined,
            proofDocumentId,
            gatewayProvider,
            paymentMethod,
            purpose: 'SUBSCRIPTION',
            subscriptionPackageId: pkg.id,
          });
        }),
        finalize(() => {
          this.submittingSubscription = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          this.subscriptionReference = '';
          this.subscriptionProofFile = null;
          this.snackBar.open(
            'Subscription payment submitted — LX will activate your package after confirming payment.',
            'Close',
            { duration: 6000 },
          );
          this.reload();
        },
        error: (err: unknown) => {
          const message = err instanceof Error ? err.message : 'Could not submit subscription payment.';
          this.snackBar.open(message, 'Close', { duration: 6000 });
        },
      });
  }

  submitDeposit(): void {
    const amountCents = Math.round(parseFloat(this.depositAmount || '0') * 100);
    if (!amountCents || amountCents < 1) {
      this.snackBar.open('Enter a valid deposit amount.', 'Close', { duration: 3500 });
      return;
    }
    if (this.selectedPaymentMethod.comingSoon) {
      this.snackBar.open('This payment channel is not live yet.', 'Close', { duration: 4000 });
      return;
    }
    if (this.depositPaymentMethod === 'BANK_TRANSFER' && !this.depositProofFile && !this.depositReference.trim()) {
      this.snackBar.open('Attach proof of payment or enter a bank reference.', 'Close', { duration: 4500 });
      return;
    }

    const orgIdRaw = this.authState.currentUser?.organizationId ?? this.walletSummary?.organizationId;
    const orgId = typeof orgIdRaw === 'string' ? Number(orgIdRaw) : orgIdRaw;
    if (!orgId || orgId < 1) {
      this.snackBar.open('Could not resolve your organisation.', 'Close', { duration: 4500 });
      return;
    }

    this.submittingDeposit = true;
    const proof$: Observable<number | undefined> = this.depositProofFile
      ? this.wallet.uploadPaymentProof(orgId, this.depositProofFile)
      : of(undefined);

    proof$
      .pipe(
        switchMap((proofDocumentId) => {
          if (this.depositProofFile && !proofDocumentId) {
            throw new Error('Proof of payment upload did not return a document id.');
          }
          const { gatewayProvider, paymentMethod } = this.mapPaymentMethod(this.depositPaymentMethod);
          return this.wallet.createDeposit({
            amountCents,
            referenceNumber: this.depositReference || undefined,
            notes: this.depositNotes || undefined,
            proofDocumentId,
            gatewayProvider,
            paymentMethod,
          });
        }),
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
          this.depositProofFile = null;
          this.depositPaymentMethod = 'BANK_TRANSFER';
          this.snackBar.open(
            'Deposit submitted — LX will credit your wallet after confirming payment.',
            'Close',
            { duration: 5500 },
          );
          this.reload();
        },
        error: (err: unknown) => {
          const message = err instanceof Error ? err.message : 'Could not submit deposit.';
          this.snackBar.open(message, 'Close', { duration: 6000 });
        },
      });
  }

  downloadReceipt(row: BillingHistoryRow): void {
    if (!row.transactionId) {
      return;
    }
    this.wallet.downloadReceipt(row.transactionId, row.receiptNumber);
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

  statusClass(status?: string): string {
    const normalized = (status ?? '').toUpperCase();
    if (normalized === 'PENDING') {
      return 'settings-billing__status settings-billing__status--pending';
    }
    if (normalized === 'CONFIRMED' || normalized === 'COMPLETED') {
      return 'settings-billing__status settings-billing__status--confirmed';
    }
    if (normalized === 'REJECTED') {
      return 'settings-billing__status settings-billing__status--rejected';
    }
    return 'settings-billing__status';
  }

  openUsageReport(): void {
    void this.router.navigate(['/analytics/platform-usage']);
  }

  formatMoney(cents: number, currencyCode?: string): string {
    return this.wallet.formatCents(cents, currencyCode ?? this.walletSummary?.currencyCode ?? 'USD');
  }

  packagePrice(pkg: SubscriptionPackageRow): string {
    return this.wallet.formatCents(pkg.monthlyPriceCents, pkg.currencyCode);
  }

  packageFeatures(description?: string | null): string[] {
    return packageFeaturePoints(description);
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

  canDownloadReceipt(row: WalletTransactionRow): boolean {
    return row.transactionType === 'DEPOSIT' || row.transactionType === 'ADJUSTMENT';
  }

  absCents(cents: number): number {
    return Math.abs(cents ?? 0);
  }

  private buildBillingHistory(deposits: WalletDepositRow[], transactions: WalletTransactionRow[]): BillingHistoryRow[] {
    const depositRows: BillingHistoryRow[] = deposits.map((deposit) => ({
      key: `deposit-${deposit.id}`,
      sortAt: deposit.createdAt ?? '',
      typeLabel: 'Wallet top-up',
      description: deposit.referenceNumber?.trim() || deposit.notes?.trim() || deposit.paymentMethod?.replace(/_/g, ' ') || 'Deposit',
      amountCents: deposit.amountCents,
      currencyCode: deposit.currencyCode,
      status: deposit.status,
      isDebit: false,
      canDownloadReceipt: false,
    }));

    const transactionRows: BillingHistoryRow[] = transactions.map((tx) => ({
      key: `tx-${tx.id}`,
      sortAt: tx.createdAt ?? '',
      typeLabel: this.transactionTypeLabel(tx),
      description: this.transactionLabel(tx),
      amountCents: tx.amountCents,
      currencyCode: this.walletSummary?.currencyCode,
      balanceAfterCents: tx.balanceAfterCents,
      receiptNumber: tx.receiptNumber,
      transactionId: tx.id,
      isDebit: this.isDebit(tx),
      canDownloadReceipt: this.canDownloadReceipt(tx),
    }));

    return [...depositRows, ...transactionRows].sort((left, right) => this.compareWhen(right.sortAt, left.sortAt));
  }

  private compareWhen(left: string, right: string): number {
    const leftMs = Date.parse(left);
    const rightMs = Date.parse(right);
    if (Number.isFinite(leftMs) && Number.isFinite(rightMs)) {
      return leftMs - rightMs;
    }
    return left.localeCompare(right);
  }

  private transactionTypeLabel(row: WalletTransactionRow): string {
    switch (row.transactionType) {
      case 'DEPOSIT':
        return 'Credit';
      case 'CHARGE':
        return 'Usage charge';
      case 'ADJUSTMENT':
        return 'Adjustment';
      default:
        return row.transactionType.replace(/_/g, ' ');
    }
  }

  private mapPaymentMethod(method: WalletDepositPaymentMethod): { gatewayProvider: string; paymentMethod: string } {
    switch (method) {
      case 'CASH':
        return { gatewayProvider: 'MANUAL', paymentMethod: 'CASH' };
      case 'ECOCASH':
        return { gatewayProvider: 'ECOCASH', paymentMethod: 'ECOCASH' };
      case 'PAYNOW':
        return { gatewayProvider: 'PAYNOW', paymentMethod: 'PAYNOW' };
      case 'PAYPAL':
        return { gatewayProvider: 'PAYPAL', paymentMethod: 'PAYPAL' };
      case 'MASTERCARD':
        return { gatewayProvider: 'MASTERCARD', paymentMethod: 'CARD' };
      default:
        return { gatewayProvider: 'MANUAL', paymentMethod: 'BANK_TRANSFER' };
    }
  }
}
