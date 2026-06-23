import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, catchError, finalize, forkJoin, of, takeUntil } from 'rxjs';
import { OrganizationService } from '../../../../core/services/organization.service';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { PlatformWalletService, type SubscriptionPackageRow } from '../../../../core/services/platform-wallet.service';
import type { OperationalModeSelection } from '../../../../shared/components/operational-mode-picker/operational-mode-picker.component';
import { DeleteConfirmDialogComponent } from '../../../../shared/components/delete-confirm-dialog/delete-confirm-dialog.component';
import { isFuelConsumptionPackageAvailable } from '../../../../shared/utils/fuel-consumption.util';

export type InventoryDataSource = 'INTERNAL' | 'EXTERNAL_API' | 'MANUAL_ACK';
export type OperationalScenario = 'platform-full' | 'standalone-inventory' | 'cross-docking';

@Component({
  selector: 'app-settings-operational-mode',
  templateUrl: './settings-operational-mode.component.html',
  styleUrl: './settings-operational-mode.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class SettingsOperationalModeComponent implements OnInit, OnDestroy {
  loading = true;
  saving = false;
  error = '';

  standaloneMode = false;
  inventoryManagementEnabled = true;
  crossDockingEnabled = false;
  inventoryDataSource: InventoryDataSource = 'INTERNAL';
  counterpartyEngagementMode: 'RECORD_ONLY' | 'PLATFORM_ORG' = 'PLATFORM_ORG';
  fuelConsumptionEnabled = false;
  billingMode: 'PREPAID_WALLET' | 'PREMIUM_SUBSCRIPTION' = 'PREPAID_WALLET';
  subscriptionPackageId: number | null = null;
  packages: SubscriptionPackageRow[] = [];
  /** Last value loaded from the server — used to detect turn-off and show confirmation. */
  private persistedFuelConsumptionEnabled = false;

  /** Whether the current billing package allows opting in to fuel consumption. */
  get fuelConsumptionPackageAvailable(): boolean {
    if (this.billingMode !== 'PREMIUM_SUBSCRIPTION' || this.subscriptionPackageId == null) {
      return true;
    }
    const pkg = this.packages.find((row) => row.id === this.subscriptionPackageId);
    return isFuelConsumptionPackageAvailable(pkg?.fuelConsumptionAvailable);
  }

  /** The computed scenario based on current flags. */
  get activeScenario(): OperationalScenario {
    if (this.crossDockingEnabled && !this.inventoryManagementEnabled) {
      return 'cross-docking';
    }
    if (this.standaloneMode && this.inventoryManagementEnabled) {
      return 'standalone-inventory';
    }
    return 'platform-full';
  }

  /** When true, cross-docking cannot be enabled (inventory management is the permanent choice). */
  get inventoryLocked(): boolean {
    return this.inventoryManagementEnabled;
  }

  /** Warning shown when user tries to enable cross-docking while inventory management is on. */
  get crossDockConflict(): boolean {
    return this.crossDockingEnabled && this.inventoryManagementEnabled;
  }

  /** Warn that switching from INTERNAL to external is irreversible. */
  get switchFromInternalWarning(): boolean {
    return this.inventoryDataSource !== 'INTERNAL';
  }

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly orgService: OrganizationService,
    private readonly wallet: PlatformWalletService,
    private readonly authState: AuthStateService,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadFromCurrentUser();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadFromCurrentUser(): void {
    this.loading = true;
    forkJoin({
      org: this.orgService.getMy().pipe(catchError(() => of(null))),
      setting: this.wallet.getBillingSetting().pipe(catchError(() => of(null))),
      packages: this.wallet.listSubscriptionPackages().pipe(catchError(() => of([]))),
    })
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe(({ org, setting, packages }) => {
        this.packages = packages;
        if (setting) {
          this.billingMode = setting.billingMode ?? 'PREPAID_WALLET';
          this.subscriptionPackageId = setting.subscriptionPackageId ?? null;
        }
        if (org) {
          this.standaloneMode = org.standaloneMode ?? false;
          this.inventoryManagementEnabled = org.inventoryManagementEnabled ?? true;
          this.crossDockingEnabled = org.crossDockingEnabled ?? false;
          this.inventoryDataSource = org.inventoryDataSource ?? 'INTERNAL';
          this.counterpartyEngagementMode = org.counterpartyEngagementMode ?? 'PLATFORM_ORG';
          this.fuelConsumptionEnabled = org.fuelConsumptionEnabled ?? false;
          this.persistedFuelConsumptionEnabled = this.fuelConsumptionEnabled;
          if (!this.fuelConsumptionPackageAvailable) {
            this.fuelConsumptionEnabled = false;
            this.persistedFuelConsumptionEnabled = false;
          }
        }
        this.cdr.markForCheck();
      });
  }

  get orgClassification() {
    return this.authState.currentUser?.orgClassification ?? null;
  }

  onTradingModeChange(selection: OperationalModeSelection): void {
    this.standaloneMode = selection.standaloneMode;
    this.counterpartyEngagementMode = selection.counterpartyEngagementMode;
    this.cdr.markForCheck();
  }

  onInventoryModeChange(selection: OperationalModeSelection): void {
    this.inventoryManagementEnabled = selection.inventoryManagementEnabled;
    this.crossDockingEnabled = selection.crossDockingEnabled;
    this.inventoryDataSource = selection.inventoryDataSource;
    this.cdr.markForCheck();
  }

  onFuelConsumptionToggle(enabled: boolean): void {
    if (enabled) {
      this.fuelConsumptionEnabled = true;
      this.cdr.markForCheck();
      return;
    }

    if (!this.persistedFuelConsumptionEnabled && !this.fuelConsumptionEnabled) {
      this.fuelConsumptionEnabled = false;
      this.cdr.markForCheck();
      return;
    }

    this.dialog
      .open(DeleteConfirmDialogComponent, {
        width: '460px',
        maxWidth: '92vw',
        data: {
          title: 'Turn off fuel consumption?',
          message:
            'Live fuel telemetry, trip fuel tracking, and fuel-related wallet charges will be disabled for this organisation. You can turn it back on later from this screen.',
          confirmLabel: 'Turn off',
          confirmDanger: false,
        },
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed) => {
        if (!confirmed) {
          this.fuelConsumptionEnabled = this.persistedFuelConsumptionEnabled;
          this.cdr.markForCheck();
          return;
        }
        this.fuelConsumptionEnabled = false;
        this.persistOperationalSettings('Fuel consumption has been turned off.');
      });
  }

  save(): void {
    this.persistOperationalSettings('Operational settings saved.');
  }

  private persistOperationalSettings(successMessage: string): void {
    if (this.crossDockConflict) {
      this.snackBar.open(
        'Disable inventory management before enabling cross-docking.',
        'Close',
        { duration: 4000 },
      );
      return;
    }

    if (this.fuelConsumptionEnabled && !this.fuelConsumptionPackageAvailable) {
      this.snackBar.open(
        'Fuel consumption is not included on Supplier Pro. Choose Fleet Manager Premium, or use prepaid wallet billing.',
        'Close',
        { duration: 5000 },
      );
      return;
    }

    this.saving = true;
    this.error = '';

    this.orgService
      .saveOperationalSettings({
        standaloneMode: this.standaloneMode,
        inventoryManagementEnabled: this.inventoryManagementEnabled,
        crossDockingEnabled: this.crossDockingEnabled,
        inventoryDataSource: this.inventoryDataSource,
        counterpartyEngagementMode: this.counterpartyEngagementMode,
        fuelConsumptionEnabled: this.fuelConsumptionEnabled,
      })
      .pipe(
        catchError((err: Error) => {
          this.error = err.message ?? 'Could not save operational settings.';
          return of(null);
        }),
        finalize(() => {
          this.saving = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe((org) => {
        if (org) {
          this.applySavedOrganization(org);
          this.snackBar.open(successMessage, 'Close', { duration: 3500 });
        } else {
          this.fuelConsumptionEnabled = this.persistedFuelConsumptionEnabled;
        }
        this.cdr.markForCheck();
      });
  }

  private applySavedOrganization(org: {
    standaloneMode?: boolean;
    inventoryManagementEnabled?: boolean;
    crossDockingEnabled?: boolean;
    inventoryDataSource?: InventoryDataSource;
    counterpartyEngagementMode?: 'RECORD_ONLY' | 'PLATFORM_ORG';
    fuelConsumptionEnabled?: boolean;
  }): void {
    this.standaloneMode = org.standaloneMode ?? this.standaloneMode;
    this.inventoryManagementEnabled = org.inventoryManagementEnabled ?? this.inventoryManagementEnabled;
    this.crossDockingEnabled = org.crossDockingEnabled ?? this.crossDockingEnabled;
    this.inventoryDataSource = org.inventoryDataSource ?? this.inventoryDataSource;
    this.counterpartyEngagementMode = org.counterpartyEngagementMode ?? this.counterpartyEngagementMode;
    this.fuelConsumptionEnabled = org.fuelConsumptionEnabled ?? false;
    this.persistedFuelConsumptionEnabled = this.fuelConsumptionEnabled;

    const user = this.authState.currentUser;
    if (user) {
      this.authState.setCurrentUser({
        ...user,
        standaloneMode: org.standaloneMode,
        inventoryManagementEnabled: org.inventoryManagementEnabled,
        crossDockingEnabled: org.crossDockingEnabled,
        inventoryDataSource: org.inventoryDataSource,
        counterpartyEngagementMode: org.counterpartyEngagementMode,
        fuelConsumptionEnabled: org.fuelConsumptionEnabled,
      });
    }
  }
}
