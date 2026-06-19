import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { OrganizationClassification } from '../../../core/models/auth.model';

export type InventoryDataSource = 'INTERNAL' | 'EXTERNAL_API' | 'MANUAL_ACK';
export type TradingModel = 'PLATFORM_PARTNERS' | 'STANDALONE';
export type InventoryModel = 'FULL_INVENTORY' | 'CROSS_DOCKING';
export type CounterpartyEngagementMode = 'RECORD_ONLY' | 'PLATFORM_ORG';
export type PlatformEngagementTab = 'RECORD_ONLY' | 'PLATFORM_ORG';
export type InventoryMgmtTab = 'INTERNAL' | 'EXTERNAL_API';

export interface OperationalModeSelection {
  standaloneMode: boolean;
  inventoryManagementEnabled: boolean;
  crossDockingEnabled: boolean;
  inventoryDataSource: InventoryDataSource;
  counterpartyEngagementMode: CounterpartyEngagementMode;
}

@Component({
  selector: 'app-operational-mode-picker',
  templateUrl: './operational-mode-picker.component.html',
  styleUrl: './operational-mode-picker.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
  imports: [CommonModule, MatIconModule, RouterLink],
})
export class OperationalModePickerComponent {
  @Input() classification: OrganizationClassification | null = null;
  @Input() inventoryLocked = false;
  @Input() standaloneMode = false;
  @Input() inventoryManagementEnabled = true;
  @Input() crossDockingEnabled = false;
  @Input() inventoryDataSource: InventoryDataSource = 'INTERNAL';
  @Input() counterpartyEngagementMode: CounterpartyEngagementMode = 'PLATFORM_ORG';
  @Input() disabled = false;
  @Input() showTradingSection = true;
  @Input() showInventorySection = true;
  /** Use stacked (single-column) panels in narrow forms e.g. signup. */
  @Input() panelLayout: 'grid' | 'stacked' = 'grid';

  @Output() selectionChange = new EventEmitter<OperationalModeSelection>();

  get showTradingChoice(): boolean {
    return this.classification === 'SUPPLIER' || this.classification === 'CUSTOMER';
  }

  get tradingModel(): TradingModel {
    return this.standaloneMode ? 'STANDALONE' : 'PLATFORM_PARTNERS';
  }

  get inventoryModel(): InventoryModel {
    return this.crossDockingEnabled && !this.inventoryManagementEnabled ? 'CROSS_DOCKING' : 'FULL_INVENTORY';
  }

  get inventoryMgmtTab(): InventoryMgmtTab {
    return this.inventoryDataSource === 'EXTERNAL_API' ? 'EXTERNAL_API' : 'INTERNAL';
  }

  get platformTab(): PlatformEngagementTab {
    return this.counterpartyEngagementMode === 'RECORD_ONLY' ? 'RECORD_ONLY' : 'PLATFORM_ORG';
  }

  get counterpartyLabel(): string {
    return this.classification === 'SUPPLIER' ? 'customers' : 'suppliers';
  }

  get counterpartyLabelSingular(): string {
    return this.classification === 'SUPPLIER' ? 'customer' : 'supplier';
  }

  get fullInventoryApiLink(): string {
    return '/products-inventory/integration-api';
  }

  get crossDockApiLink(): string {
    return '/products-inventory/integration-api';
  }

  setTradingModel(model: TradingModel): void {
    if (this.disabled) return;
    if (model === 'STANDALONE') {
      this.emit({ standaloneMode: true, counterpartyEngagementMode: 'RECORD_ONLY' });
      return;
    }
    this.emit({
      standaloneMode: false,
      counterpartyEngagementMode:
        this.counterpartyEngagementMode === 'RECORD_ONLY' ? 'RECORD_ONLY' : 'PLATFORM_ORG',
    });
  }

  setPlatformTab(tab: PlatformEngagementTab): void {
    if (this.disabled || this.tradingModel !== 'PLATFORM_PARTNERS') return;
    this.emit({ standaloneMode: false, counterpartyEngagementMode: tab });
  }

  setInventoryModel(model: InventoryModel): void {
    if (this.disabled) return;
    if (model === 'FULL_INVENTORY') {
      this.emit({
        inventoryManagementEnabled: true,
        crossDockingEnabled: false,
        inventoryDataSource: this.inventoryMgmtTab === 'EXTERNAL_API' ? 'EXTERNAL_API' : 'INTERNAL',
      });
      return;
    }
    if (!this.inventoryLocked) {
      const source =
        this.inventoryDataSource === 'MANUAL_ACK' || this.inventoryDataSource === 'INTERNAL'
          ? 'EXTERNAL_API'
          : this.inventoryDataSource;
      this.emit({
        inventoryManagementEnabled: false,
        crossDockingEnabled: true,
        inventoryDataSource: source,
      });
    }
  }

  setInventoryMgmtTab(tab: InventoryMgmtTab): void {
    if (this.disabled || this.inventoryModel !== 'FULL_INVENTORY') return;
    this.emit({
      inventoryManagementEnabled: true,
      crossDockingEnabled: false,
      inventoryDataSource: tab,
    });
  }

  setCrossDockFlow(source: 'EXTERNAL_API' | 'MANUAL_ACK'): void {
    if (this.disabled || this.inventoryModel !== 'CROSS_DOCKING') return;
    this.emit({ inventoryDataSource: source });
  }

  private emit(partial: Partial<OperationalModeSelection>): void {
    this.selectionChange.emit({
      standaloneMode: partial.standaloneMode ?? this.standaloneMode,
      inventoryManagementEnabled: partial.inventoryManagementEnabled ?? this.inventoryManagementEnabled,
      crossDockingEnabled: partial.crossDockingEnabled ?? this.crossDockingEnabled,
      inventoryDataSource: partial.inventoryDataSource ?? this.inventoryDataSource,
      counterpartyEngagementMode: partial.counterpartyEngagementMode ?? this.counterpartyEngagementMode,
    });
  }
}
