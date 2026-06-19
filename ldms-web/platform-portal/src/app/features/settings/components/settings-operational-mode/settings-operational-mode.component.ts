import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, catchError, finalize, of, takeUntil } from 'rxjs';
import { OrganizationService } from '../../../../core/services/organization.service';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import type { OperationalModeSelection } from '../../../../shared/components/operational-mode-picker/operational-mode-picker.component';

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
    private readonly authState: AuthStateService,
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
    this.orgService
      .getMy()
      .pipe(
        catchError(() => of(null)),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe((org) => {
        if (org) {
          this.standaloneMode = org.standaloneMode ?? false;
          this.inventoryManagementEnabled = org.inventoryManagementEnabled ?? true;
          this.crossDockingEnabled = org.crossDockingEnabled ?? false;
          this.inventoryDataSource = org.inventoryDataSource ?? 'INTERNAL';
          this.counterpartyEngagementMode = org.counterpartyEngagementMode ?? 'PLATFORM_ORG';
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

  save(): void {
    if (this.crossDockConflict) {
      this.snackBar.open(
        'Disable inventory management before enabling cross-docking.',
        'Close',
        { duration: 4000 },
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
          const user = this.authState.currentUser;
          if (user) {
            this.authState.setCurrentUser({
              ...user,
              standaloneMode: org.standaloneMode,
              inventoryManagementEnabled: org.inventoryManagementEnabled,
              crossDockingEnabled: org.crossDockingEnabled,
              inventoryDataSource: org.inventoryDataSource,
              counterpartyEngagementMode: org.counterpartyEngagementMode,
            });
          }
          this.snackBar.open('Operational settings saved.', 'Close', { duration: 3500 });
        }
        this.cdr.markForCheck();
      });
  }
}
