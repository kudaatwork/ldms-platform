import type { StatusTone } from '../models/inventory.model';

export type StockLevelStatus = 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK' | 'FULLY_RESERVED';

export const STOCK_STATUS_FILTER_OPTIONS: ReadonlyArray<{ value: '' | StockLevelStatus; label: string }> = [
  { value: '', label: 'All statuses' },
  { value: 'IN_STOCK', label: 'In stock' },
  { value: 'LOW_STOCK', label: 'Low stock' },
  { value: 'OUT_OF_STOCK', label: 'Out of stock' },
  { value: 'FULLY_RESERVED', label: 'Fully reserved' },
];

export function resolveStockLevelStatus(
  quantityOnHand: number,
  reservedQuantity: number,
  reorderPoint: number,
): StockLevelStatus {
  if (quantityOnHand <= 0) {
    return 'OUT_OF_STOCK';
  }
  if (reorderPoint > 0 && quantityOnHand <= reorderPoint) {
    return 'LOW_STOCK';
  }
  if (quantityOnHand - reservedQuantity <= 0) {
    return 'FULLY_RESERVED';
  }
  return 'IN_STOCK';
}

export function stockLevelStatusLabel(status: StockLevelStatus): string {
  const map: Record<StockLevelStatus, string> = {
    IN_STOCK: 'In stock',
    LOW_STOCK: 'Low stock',
    OUT_OF_STOCK: 'Out of stock',
    FULLY_RESERVED: 'Fully reserved',
  };
  return map[status] ?? status;
}

export function stockLevelStatusCssClass(status: StockLevelStatus): string {
  const map: Record<StockLevelStatus, string> = {
    IN_STOCK: 'approved',
    LOW_STOCK: 'pending',
    OUT_OF_STOCK: 'rejected',
    FULLY_RESERVED: 'submitted',
  };
  return map[status] ?? 'inactive';
}

export function stockLevelStatusTone(status: StockLevelStatus): StatusTone {
  const map: Record<StockLevelStatus, StatusTone> = {
    IN_STOCK: 'success',
    LOW_STOCK: 'warn',
    OUT_OF_STOCK: 'danger',
    FULLY_RESERVED: 'warn',
  };
  return map[status] ?? 'muted';
}
