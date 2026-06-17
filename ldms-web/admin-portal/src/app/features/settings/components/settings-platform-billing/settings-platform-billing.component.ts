import { ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, finalize, forkJoin, takeUntil } from 'rxjs';
import {
  PlatformWalletAdminService,
  type PlatformActionChargeRow,
  type SubscriptionPackageRow,
  type WalletDepositRow,
} from '../../services/platform-wallet-admin.service';
import {
  ACTION_CHARGE_IMPORT_DISCLAIMER,
  ACTION_CHARGE_SAMPLE_CSV,
  SUBSCRIPTION_PACKAGE_IMPORT_DISCLAIMER,
  SUBSCRIPTION_PACKAGE_SAMPLE_CSV,
  downloadTextFile,
  parseActionChargeCsv,
  parseSubscriptionPackageCsv,
} from '../../utils/platform-billing-csv.util';
import {
  PlatformActionChargeFormDialogComponent,
  type PlatformActionChargeFormDialogData,
} from '../platform-action-charge-form-dialog/platform-action-charge-form-dialog.component';
import {
  SubscriptionPackageFormDialogComponent,
  type SubscriptionPackageFormDialogData,
} from '../subscription-package-form-dialog/subscription-package-form-dialog.component';
import { PLATFORM_BILLING_MODULES, moduleLabel } from '../../utils/platform-billing-modules.util';
import {
  exportClientTableAsCsv,
  exportFormatLabel,
  type LxExportColumn,
  type LxExportFormat,
} from '@shared/utils/lx-export.util';

type BillingTab = 'charges' | 'packages' | 'deposits';

@Component({
  selector: 'app-settings-platform-billing',
  templateUrl: './settings-platform-billing.component.html',
  styleUrl: './settings-platform-billing.component.scss',
  standalone: false,
})
export class SettingsPlatformBillingComponent implements OnInit, OnDestroy {
  @ViewChild('chargeCsvInput') chargeCsvInput?: ElementRef<HTMLInputElement>;
  @ViewChild('packageCsvInput') packageCsvInput?: ElementRef<HTMLInputElement>;

  loading = true;
  importing = false;
  exporting = false;
  activeTab: BillingTab = 'charges';

  actionCharges: PlatformActionChargeRow[] = [];
  packages: SubscriptionPackageRow[] = [];
  pendingDeposits: WalletDepositRow[] = [];
  confirmingDepositId: number | null = null;

  searchQuery = '';
  filterFieldsOpen = false;
  showSampleCsvInfo = false;
  chargeFilters = { actionCode: '', displayName: '', category: '', active: '' };
  packageFilters = { code: '', name: '', active: '', featured: '' };
  depositFilters = { organizationId: '', referenceNumber: '' };

  readonly chargeColumns = ['action', 'category', 'charge', 'active', 'actions'];
  readonly packageColumns = ['code', 'name', 'price', 'featured', 'active', 'actions'];
  readonly depositColumns = ['org', 'amount', 'ref', 'createdAt', 'actions'];

  readonly chargeSampleDescription =
    'Columns: actionCode, displayName, description, chargeCents, category, active. Use uppercase action codes referenced by platform services.';
  readonly packageSampleDescription =
    'Columns: code, name, description, monthlyPriceCents, currencyCode, sortOrder, featured, active.';
  readonly chargeImportDisclaimer = ACTION_CHARGE_IMPORT_DISCLAIMER;
  readonly packageImportDisclaimer = SUBSCRIPTION_PACKAGE_IMPORT_DISCLAIMER;
  readonly billingModules = PLATFORM_BILLING_MODULES;
  readonly moduleLabel = moduleLabel;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly walletAdmin: PlatformWalletAdminService,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
    private readonly dialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.reload();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get filteredActionCharges(): PlatformActionChargeRow[] {
    const q = this.searchQuery.trim().toLowerCase();
    return this.actionCharges.filter((row) => {
      if (q && !`${row.actionCode} ${row.displayName} ${row.description ?? ''} ${row.category ?? ''}`.toLowerCase().includes(q)) {
        return false;
      }
      if (this.chargeFilters.actionCode && !row.actionCode.toLowerCase().includes(this.chargeFilters.actionCode.trim().toLowerCase())) {
        return false;
      }
      if (this.chargeFilters.displayName && !row.displayName.toLowerCase().includes(this.chargeFilters.displayName.trim().toLowerCase())) {
        return false;
      }
      if (this.chargeFilters.category && !(row.category ?? '').toLowerCase().includes(this.chargeFilters.category.trim().toLowerCase())) {
        return false;
      }
      if (this.chargeFilters.active) {
        const want = this.chargeFilters.active.trim().toLowerCase();
        const active = row.active !== false ? 'true' : 'false';
        if (want !== active && want !== 'yes' && want !== 'no' && !active.includes(want)) {
          return false;
        }
      }
      return true;
    });
  }

  get filteredPackages(): SubscriptionPackageRow[] {
    const q = this.searchQuery.trim().toLowerCase();
    return this.packages.filter((row) => {
      if (q && !`${row.code} ${row.name} ${row.description ?? ''}`.toLowerCase().includes(q)) {
        return false;
      }
      if (this.packageFilters.code && !row.code.toLowerCase().includes(this.packageFilters.code.trim().toLowerCase())) {
        return false;
      }
      if (this.packageFilters.name && !row.name.toLowerCase().includes(this.packageFilters.name.trim().toLowerCase())) {
        return false;
      }
      if (this.packageFilters.featured) {
        const featured = row.featured === true ? 'true' : 'false';
        if (!featured.includes(this.packageFilters.featured.trim().toLowerCase())) return false;
      }
      if (this.packageFilters.active) {
        const active = row.active !== false ? 'true' : 'false';
        if (!active.includes(this.packageFilters.active.trim().toLowerCase())) return false;
      }
      return true;
    });
  }

  get filteredDeposits(): WalletDepositRow[] {
    const q = this.searchQuery.trim().toLowerCase();
    return this.pendingDeposits.filter((row) => {
      if (q && !`${row.organizationId} ${row.referenceNumber ?? ''} ${row.amountCents}`.toLowerCase().includes(q)) {
        return false;
      }
      if (this.depositFilters.organizationId && !String(row.organizationId ?? '').includes(this.depositFilters.organizationId.trim())) {
        return false;
      }
      if (this.depositFilters.referenceNumber && !(row.referenceNumber ?? '').toLowerCase().includes(this.depositFilters.referenceNumber.trim().toLowerCase())) {
        return false;
      }
      return true;
    });
  }

  get sampleCsvDescription(): string {
    if (this.activeTab === 'packages') return this.packageSampleDescription;
    if (this.activeTab === 'charges') return this.chargeSampleDescription;
    return '';
  }

  get importDisclaimer(): string {
    if (this.activeTab === 'packages') return this.packageImportDisclaimer;
    if (this.activeTab === 'charges') return this.chargeImportDisclaimer;
    return '';
  }

  get supportsImportExport(): boolean {
    return this.activeTab === 'charges' || this.activeTab === 'packages';
  }

  chargesForModule(category: string): PlatformActionChargeRow[] {
    return this.filteredActionCharges.filter((row) => (row.category ?? 'GENERAL') === category);
  }

  switchTab(tab: BillingTab): void {
    this.activeTab = tab;
    this.searchQuery = '';
    this.filterFieldsOpen = false;
    this.showSampleCsvInfo = false;
    this.cdr.markForCheck();
  }

  reload(): void {
    this.loading = true;
    forkJoin({
      charges: this.walletAdmin.listActionCharges(),
      packages: this.walletAdmin.listSubscriptionPackages(),
      deposits: this.walletAdmin.listPendingDeposits(),
    })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe(({ charges, packages, deposits }) => {
        this.actionCharges = charges;
        this.packages = packages;
        this.pendingDeposits = deposits;
      });
  }

  openChargeDialog(mode: 'create' | 'edit' | 'view', row?: PlatformActionChargeRow, defaultCategory?: string): void {
    const ref = this.dialog.open(PlatformActionChargeFormDialogComponent, {
      width: '640px',
      maxWidth: '92vw',
      panelClass: 'lx-location-dialog-panel',
      data: { mode, row, defaultCategory } satisfies PlatformActionChargeFormDialogData,
    });
    ref.afterClosed().subscribe((payload) => {
      if (!payload) return;
      this.walletAdmin.saveActionCharge(payload).subscribe({
        next: () => {
          this.snackBar.open(`Saved ${payload.actionCode}`, 'Close', { duration: 2500 });
          this.reload();
        },
        error: () => this.snackBar.open('Could not save action charge.', 'Close', { duration: 4000 }),
      });
    });
  }

  openPackageDialog(mode: 'create' | 'edit' | 'view', row?: SubscriptionPackageRow): void {
    const ref = this.dialog.open(SubscriptionPackageFormDialogComponent, {
      width: '640px',
      maxWidth: '92vw',
      panelClass: 'lx-location-dialog-panel',
      data: { mode, row } satisfies SubscriptionPackageFormDialogData,
    });
    ref.afterClosed().subscribe((payload) => {
      if (!payload) return;
      this.walletAdmin.saveSubscriptionPackage(payload).subscribe({
        next: () => {
          this.snackBar.open(`Saved ${payload.name}`, 'Close', { duration: 2500 });
          this.reload();
        },
        error: () => this.snackBar.open('Could not save package.', 'Close', { duration: 4000 }),
      });
    });
  }

  confirmDeposit(deposit: WalletDepositRow): void {
    this.confirmingDepositId = deposit.id;
    this.walletAdmin
      .confirmDeposit(deposit.id)
      .pipe(
        finalize(() => {
          this.confirmingDepositId = null;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          this.snackBar.open('Deposit confirmed and wallet credited.', 'Close', { duration: 3500 });
          this.reload();
        },
        error: () => this.snackBar.open('Could not confirm deposit.', 'Close', { duration: 4000 }),
      });
  }

  downloadSampleCsv(): void {
    if (this.activeTab === 'charges') {
      downloadTextFile('platform-action-charges-sample.csv', ACTION_CHARGE_SAMPLE_CSV);
      return;
    }
    if (this.activeTab === 'packages') {
      downloadTextFile('subscription-packages-sample.csv', SUBSCRIPTION_PACKAGE_SAMPLE_CSV);
    }
  }

  exportAs(format: LxExportFormat): void {
    if (this.exporting) {
      return;
    }
    const { rows, columns, filenameBase, title } = this.exportContext();
    if (!rows.length) {
      this.snackBar.open('No rows to export.', 'Close', { duration: 3000 });
      return;
    }
    this.exporting = true;
    const saved = exportClientTableAsCsv(
      format,
      rows,
      columns,
      filenameBase,
      (message) => this.snackBar.open(message, 'Close', { duration: 4500 }),
      { title },
    );
    this.exporting = false;
    this.cdr.markForCheck();
    if (saved) {
      this.snackBar.open(`Exported as ${exportFormatLabel(format)}.`, 'Close', { duration: 2500 });
    }
  }

  private exportContext(): {
    rows: readonly object[];
    columns: readonly LxExportColumn<object>[];
    filenameBase: string;
    title: string;
  } {
    if (this.activeTab === 'charges') {
      return {
        rows: this.filteredActionCharges,
        filenameBase: 'platform-action-charges',
        title: 'Platform action charges',
        columns: [
          { header: 'actionCode', value: (r) => (r as PlatformActionChargeRow).actionCode },
          { header: 'displayName', value: (r) => (r as PlatformActionChargeRow).displayName },
          { header: 'description', value: (r) => (r as PlatformActionChargeRow).description ?? '' },
          { header: 'chargeCents', value: (r) => (r as PlatformActionChargeRow).chargeCents },
          { header: 'category', value: (r) => (r as PlatformActionChargeRow).category ?? '' },
          { header: 'active', value: (r) => (r as PlatformActionChargeRow).active !== false },
        ],
      };
    }
    if (this.activeTab === 'packages') {
      return {
        rows: this.filteredPackages,
        filenameBase: 'subscription-packages',
        title: 'Subscription packages',
        columns: [
          { header: 'code', value: (r) => (r as SubscriptionPackageRow).code },
          { header: 'name', value: (r) => (r as SubscriptionPackageRow).name },
          { header: 'description', value: (r) => (r as SubscriptionPackageRow).description ?? '' },
          { header: 'monthlyPriceCents', value: (r) => (r as SubscriptionPackageRow).monthlyPriceCents },
          { header: 'currencyCode', value: (r) => (r as SubscriptionPackageRow).currencyCode ?? 'USD' },
          { header: 'sortOrder', value: (r) => (r as SubscriptionPackageRow).sortOrder ?? '' },
          { header: 'featured', value: (r) => (r as SubscriptionPackageRow).featured === true },
          { header: 'active', value: (r) => (r as SubscriptionPackageRow).active !== false },
        ],
      };
    }
    return {
      rows: this.filteredDeposits,
      filenameBase: 'pending-wallet-deposits',
      title: 'Pending wallet deposits',
      columns: [
        { header: 'organizationId', value: (r) => (r as WalletDepositRow).organizationId ?? '' },
        { header: 'amountCents', value: (r) => (r as WalletDepositRow).amountCents },
        { header: 'currencyCode', value: (r) => (r as WalletDepositRow).currencyCode ?? 'USD' },
        { header: 'referenceNumber', value: (r) => (r as WalletDepositRow).referenceNumber ?? '' },
        { header: 'createdAt', value: (r) => (r as WalletDepositRow).createdAt ?? '' },
      ],
    };
  }

  triggerImport(): void {
    if (this.activeTab === 'charges') {
      this.chargeCsvInput?.nativeElement.click();
      return;
    }
    if (this.activeTab === 'packages') {
      this.packageCsvInput?.nativeElement.click();
    }
  }

  onChargeImportSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    this.importChargesFromFile(file);
  }

  onPackageImportSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    this.importPackagesFromFile(file);
  }

  private importChargesFromFile(file: File): void {
    this.importing = true;
    file
      .text()
      .then((text) => {
        const rows = parseActionChargeCsv(text).filter((r) => r.actionCode && r.displayName);
        if (!rows.length) throw new Error('No valid rows found in CSV.');
        let index = 0;
        let done = 0;
        let failed = 0;
        const next = (): void => {
          if (index >= rows.length) {
            this.importing = false;
            this.reload();
            this.snackBar.open(`Import finished: ${done} saved, ${failed} failed.`, 'Close', { duration: 5000 });
            this.cdr.markForCheck();
            return;
          }
          const row = rows[index++];
          this.walletAdmin.saveActionCharge(row).subscribe({
            next: () => {
              done++;
              next();
            },
            error: () => {
              failed++;
              next();
            },
          });
        };
        next();
      })
      .catch((err: unknown) => {
        this.importing = false;
        this.snackBar.open(err instanceof Error ? err.message : 'Import failed.', 'Close', { duration: 5000 });
        this.cdr.markForCheck();
      });
  }

  private importPackagesFromFile(file: File): void {
    this.importing = true;
    file
      .text()
      .then((text) => {
        const rows = parseSubscriptionPackageCsv(text).filter((r) => r.code && r.name);
        if (!rows.length) throw new Error('No valid rows found in CSV.');
        let index = 0;
        let done = 0;
        let failed = 0;
        const next = (): void => {
          if (index >= rows.length) {
            this.importing = false;
            this.reload();
            this.snackBar.open(`Import finished: ${done} saved, ${failed} failed.`, 'Close', { duration: 5000 });
            this.cdr.markForCheck();
            return;
          }
          const row = rows[index++];
          this.walletAdmin.saveSubscriptionPackage(row).subscribe({
            next: () => {
              done++;
              next();
            },
            error: () => {
              failed++;
              next();
            },
          });
        };
        next();
      })
      .catch((err: unknown) => {
        this.importing = false;
        this.snackBar.open(err instanceof Error ? err.message : 'Import failed.', 'Close', { duration: 5000 });
        this.cdr.markForCheck();
      });
  }

  formatMoney(cents: number, currency = 'USD'): string {
    return this.walletAdmin.formatCents(cents, currency);
  }

  yesNo(value: boolean | undefined): string {
    return value !== false ? 'Yes' : 'No';
  }
}
