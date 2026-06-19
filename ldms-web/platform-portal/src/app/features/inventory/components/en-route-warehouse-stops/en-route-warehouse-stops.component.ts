import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { WarehouseRow } from '../../models/inventory.model';

@Component({
  selector: 'app-en-route-warehouse-stops',
  templateUrl: './en-route-warehouse-stops.component.html',
  styleUrl: './en-route-warehouse-stops.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class EnRouteWarehouseStopsComponent {
  @Input() warehouses: WarehouseRow[] = [];
  @Input() fromLocationId = 0;
  @Input() toLocationId = 0;
  @Input() stopIds: number[] = [];
  @Input() disabled = false;
  @Input() label = 'En-route stops';
  @Input() hint = 'Add intermediate warehouses the shipment passes through before the final destination.';

  @Output() stopIdsChange = new EventEmitter<number[]>();

  get fromLabel(): string {
    return this.warehouses.find((w) => w.id === this.fromLocationId)?.name ?? 'Origin';
  }

  get toLabel(): string {
    return this.warehouses.find((w) => w.id === this.toLocationId)?.name ?? 'Destination';
  }

  optionsForRow(rowIndex: number): WarehouseRow[] {
    const used = new Set(
      this.stopIds.filter((id, i) => i !== rowIndex && id > 0),
    );
    return this.warehouses.filter(
      (w) =>
        w.id !== this.fromLocationId &&
        w.id !== this.toLocationId &&
        !used.has(w.id),
    );
  }

  get intermediateWarehouseCount(): number {
    return this.optionsForRow(-1).length;
  }

  get addStopHint(): string {
    if (this.fromLocationId <= 0 || this.toLocationId <= 0) {
      return 'Select source and destination warehouses first.';
    }
    if (!this.canAddStop()) {
      return 'Add another warehouse location (besides source and destination) under Warehouses to use en-route stops.';
    }
    return 'Add intermediate warehouses the truck passes through before delivery.';
  }

  canAddStop(): boolean {
    return this.fromLocationId > 0 && this.toLocationId > 0 && this.optionsForRow(-1).length > 0;
  }

  addStop(): void {
    if (this.disabled || !this.canAddStop()) return;
    const next = this.optionsForRow(-1)[0]?.id;
    if (!next) return;
    this.stopIdsChange.emit([...this.stopIds, next]);
  }

  removeStop(index: number): void {
    if (this.disabled) return;
    this.stopIdsChange.emit(this.stopIds.filter((_, i) => i !== index));
  }

  onStopChange(index: number, warehouseId: number): void {
    const updated = [...this.stopIds];
    updated[index] = warehouseId;
    this.stopIdsChange.emit(updated);
  }
}
