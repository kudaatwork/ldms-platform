import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs';
import type { OrganizationProfileDetail } from '../../models/organization.model';
import { OrganizationsAdminService } from '../../services/organizations-admin.service';

export type InventoryDataSource = 'INTERNAL' | 'EXTERNAL_API' | 'MANUAL_ACK';
export type InventoryModel = 'FULL_INVENTORY' | 'CROSS_DOCKING';

@Component({
  selector: 'app-org-admin-operational-mode',
  templateUrl: './org-admin-operational-mode.component.html',
  styleUrl: './org-admin-operational-mode.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class OrgAdminOperationalModeComponent implements OnChanges {
  @Input({ required: true }) profile!: OrganizationProfileDetail;

  saving = false;
  standaloneMode = false;
  inventoryManagementEnabled = true;
  crossDockingEnabled = false;
  inventoryDataSource: InventoryDataSource = 'INTERNAL';

  counterpartyEngagementMode = 'PLATFORM_ORG' as 'RECORD_ONLY' | 'PLATFORM_ORG';

  constructor(
    private readonly orgService: OrganizationsAdminService,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnChanges(): void {
    this.loadFromProfile();
  }

  get inventoryModel(): InventoryModel {
    return this.crossDockingEnabled && !this.inventoryManagementEnabled ? 'CROSS_DOCKING' : 'FULL_INVENTORY';
  }

  get inventoryMgmtTab(): 'INTERNAL' | 'EXTERNAL_API' {
    return this.inventoryDataSource === 'EXTERNAL_API' ? 'EXTERNAL_API' : 'INTERNAL';
  }

  get activeModeLabel(): string {
    if (this.crossDockingEnabled && !this.inventoryManagementEnabled) {
      return 'Cross-docking';
    }
    if (this.standaloneMode && this.inventoryManagementEnabled) {
      return 'Standalone + inventory';
    }
    return 'Platform full';
  }

  get inventoryLocked(): boolean {
    return this.inventoryManagementEnabled;
  }

  setInventoryModel(model: InventoryModel): void {
    if (model === 'FULL_INVENTORY') {
      this.inventoryManagementEnabled = true;
      this.crossDockingEnabled = false;
      this.inventoryDataSource = this.inventoryMgmtTab === 'EXTERNAL_API' ? 'EXTERNAL_API' : 'INTERNAL';
      this.cdr.markForCheck();
      return;
    }
    if (this.inventoryLocked) {
      return;
    }
    this.inventoryManagementEnabled = false;
    this.crossDockingEnabled = true;
    if (this.inventoryDataSource === 'INTERNAL') {
      this.inventoryDataSource = 'EXTERNAL_API';
    }
    this.cdr.markForCheck();
  }

  setInventoryMgmtTab(tab: 'INTERNAL' | 'EXTERNAL_API'): void {
    if (this.inventoryModel !== 'FULL_INVENTORY') {
      return;
    }
    this.inventoryManagementEnabled = true;
    this.crossDockingEnabled = false;
    this.inventoryDataSource = tab;
    this.cdr.markForCheck();
  }

  setCrossDockFlow(source: 'EXTERNAL_API' | 'MANUAL_ACK'): void {
    if (this.inventoryModel !== 'CROSS_DOCKING') {
      return;
    }
    this.inventoryDataSource = source;
    this.cdr.markForCheck();
  }

  save(): void {
    if (this.crossDockingEnabled && this.inventoryManagementEnabled) {
      this.snackBar.open('Cross-docking and full inventory cannot both be enabled.', 'Dismiss', { duration: 5000 });
      return;
    }
    this.saving = true;
    this.orgService
      .saveOperationalSettings(this.profile.id, {
        standaloneMode: this.standaloneMode,
        inventoryManagementEnabled: this.inventoryManagementEnabled,
        crossDockingEnabled: this.crossDockingEnabled,
        inventoryDataSource: this.inventoryDataSource,
        counterpartyEngagementMode: this.counterpartyEngagementMode,
      })
      .pipe(
        finalize(() => {
          this.saving = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (updated) => {
          Object.assign(this.profile, updated);
          this.loadFromProfile();
          this.snackBar.open('Operational mode updated.', 'Dismiss', { duration: 3000 });
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.snackBar.open(err.message ?? 'Could not save operational mode.', 'Dismiss', { duration: 5000 });
        },
      });
  }

  private loadFromProfile(): void {
    if (!this.profile) {
      return;
    }
    this.standaloneMode = this.profile.standaloneMode ?? false;
    this.inventoryManagementEnabled = this.profile.inventoryManagementEnabled ?? true;
    this.crossDockingEnabled = this.profile.crossDockingEnabled ?? false;
    this.inventoryDataSource = this.profile.inventoryDataSource ?? 'INTERNAL';
    this.counterpartyEngagementMode = this.profile.counterpartyEngagementMode ?? 'PLATFORM_ORG';
  }
}
