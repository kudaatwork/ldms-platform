import type {
  ProductCategoryRow,
  ProductRow,
  ProductSubCategoryRow,
  PurchaseOrderRow,
  PurchaseRequisitionRow,
  SalesOrderRow,
  StockRow,
  SupplierQuoteLineRow,
  SupplierQuoteRow,
  TransferRow,
  WarehouseRow,
} from '../models/inventory.model';

export type SortDirection = 'asc' | 'desc';

export interface InventoryTableSortState<K extends string> {
  column: K | null;
  direction: SortDirection;
}

function str(value: unknown): string {
  return String(value ?? '').trim().toLowerCase();
}

function num(value: unknown): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function compareText(left: unknown, right: unknown): number {
  return str(left).localeCompare(str(right));
}

function compareNumber(left: unknown, right: unknown): number {
  return num(left) - num(right);
}

export const PRODUCT_SORT_COMPARATORS = {
  name: (a: ProductRow, b: ProductRow) => compareText(a.name, b.name),
  code: (a: ProductRow, b: ProductRow) => compareText(a.code, b.code),
  categoryName: (a: ProductRow, b: ProductRow) => compareText(a.categoryName, b.categoryName),
  subcategoryName: (a: ProductRow, b: ProductRow) => compareText(a.subcategoryName, b.subcategoryName),
  unitPrice: (a: ProductRow, b: ProductRow) => compareNumber(a.unitPrice, b.unitPrice),
  unitOfMeasure: (a: ProductRow, b: ProductRow) => compareText(a.unitOfMeasure, b.unitOfMeasure),
  entityStatus: (a: ProductRow, b: ProductRow) => compareText(a.entityStatus, b.entityStatus),
  createdAtLabel: (a: ProductRow, b: ProductRow) => compareText(a.createdAtLabel, b.createdAtLabel),
} as const;

export type ProductSortColumn = keyof typeof PRODUCT_SORT_COMPARATORS;

export const CATEGORY_SORT_COMPARATORS = {
  name: (a: ProductCategoryRow, b: ProductCategoryRow) => compareText(a.name, b.name),
  description: (a: ProductCategoryRow, b: ProductCategoryRow) => compareText(a.description, b.description),
  entityStatus: (a: ProductCategoryRow, b: ProductCategoryRow) => compareText(a.entityStatus, b.entityStatus),
  createdAtLabel: (a: ProductCategoryRow, b: ProductCategoryRow) => compareText(a.createdAtLabel, b.createdAtLabel),
} as const;

export type CategorySortColumn = keyof typeof CATEGORY_SORT_COMPARATORS;

export const SUBCATEGORY_SORT_COMPARATORS = {
  name: (a: ProductSubCategoryRow, b: ProductSubCategoryRow) => compareText(a.name, b.name),
  categoryName: (a: ProductSubCategoryRow, b: ProductSubCategoryRow) => compareText(a.categoryName, b.categoryName),
  description: (a: ProductSubCategoryRow, b: ProductSubCategoryRow) => compareText(a.description, b.description),
  entityStatus: (a: ProductSubCategoryRow, b: ProductSubCategoryRow) => compareText(a.entityStatus, b.entityStatus),
  createdAtLabel: (a: ProductSubCategoryRow, b: ProductSubCategoryRow) => compareText(a.createdAtLabel, b.createdAtLabel),
} as const;

export type SubcategorySortColumn = keyof typeof SUBCATEGORY_SORT_COMPARATORS;

export const WAREHOUSE_SORT_COMPARATORS = {
  name: (a: WarehouseRow, b: WarehouseRow) => compareText(a.name, b.name),
  description: (a: WarehouseRow, b: WarehouseRow) => compareText(a.description, b.description),
  warehouseType: (a: WarehouseRow, b: WarehouseRow) => compareText(a.warehouseType, b.warehouseType),
  branchLabel: (a: WarehouseRow, b: WarehouseRow) => compareText(a.branchLabel, b.branchLabel),
  addressLabel: (a: WarehouseRow, b: WarehouseRow) => compareText(a.addressLabel, b.addressLabel),
  entityStatus: (a: WarehouseRow, b: WarehouseRow) => compareText(a.entityStatus, b.entityStatus),
  createdAtLabel: (a: WarehouseRow, b: WarehouseRow) => compareText(a.createdAtLabel, b.createdAtLabel),
} as const;

export type WarehouseSortColumn = keyof typeof WAREHOUSE_SORT_COMPARATORS;

export const STOCK_SORT_COMPARATORS = {
  productName: (a: StockRow, b: StockRow) => compareText(a.productName, b.productName),
  productCode: (a: StockRow, b: StockRow) => compareText(a.productCode, b.productCode),
  productBarcode: (a: StockRow, b: StockRow) => compareText(a.productBarcode, b.productBarcode),
  warehouseName: (a: StockRow, b: StockRow) => compareText(a.warehouseName, b.warehouseName),
  quantityOnHand: (a: StockRow, b: StockRow) => compareNumber(a.quantityOnHand, b.quantityOnHand),
  reservedQuantity: (a: StockRow, b: StockRow) => compareNumber(a.reservedQuantity, b.reservedQuantity),
  availableQuantity: (a: StockRow, b: StockRow) => compareNumber(a.availableQuantity, b.availableQuantity),
  unitOfMeasure: (a: StockRow, b: StockRow) => compareText(a.unitOfMeasure, b.unitOfMeasure),
  reorderPoint: (a: StockRow, b: StockRow) => compareNumber(a.reorderPoint, b.reorderPoint),
  statusLabel: (a: StockRow, b: StockRow) => compareText(a.statusLabel, b.statusLabel),
} as const;

export type StockSortColumn = keyof typeof STOCK_SORT_COMPARATORS;

export const TRANSFER_SORT_COMPARATORS = {
  transferNumber: (a: TransferRow, b: TransferRow) => compareText(a.transferNumber, b.transferNumber),
  productName: (a: TransferRow, b: TransferRow) => compareText(a.productName, b.productName),
  quantity: (a: TransferRow, b: TransferRow) => compareNumber(a.quantity, b.quantity),
  fromWarehouse: (a: TransferRow, b: TransferRow) => compareText(a.fromWarehouse, b.fromWarehouse),
  toWarehouse: (a: TransferRow, b: TransferRow) => compareText(a.toWarehouse, b.toWarehouse),
  statusLabel: (a: TransferRow, b: TransferRow) => compareText(a.statusLabel, b.statusLabel),
  createdAtLabel: (a: TransferRow, b: TransferRow) => compareText(a.createdAtLabel, b.createdAtLabel),
} as const;

export type TransferSortColumn = keyof typeof TRANSFER_SORT_COMPARATORS;

export const REQUISITION_SORT_COMPARATORS = {
  requisitionNumber: (a: PurchaseRequisitionRow, b: PurchaseRequisitionRow) =>
    compareText(a.requisitionNumber, b.requisitionNumber),
  purpose: (a: PurchaseRequisitionRow, b: PurchaseRequisitionRow) => compareText(a.purpose, b.purpose),
  priorityLabel: (a: PurchaseRequisitionRow, b: PurchaseRequisitionRow) => compareText(a.priorityLabel, b.priorityLabel),
  lineCount: (a: PurchaseRequisitionRow, b: PurchaseRequisitionRow) => compareNumber(a.lineCount, b.lineCount),
  totalAmount: (a: PurchaseRequisitionRow, b: PurchaseRequisitionRow) => compareNumber(a.totalAmount, b.totalAmount),
  requiredByDateLabel: (a: PurchaseRequisitionRow, b: PurchaseRequisitionRow) =>
    compareText(a.requiredByDateLabel, b.requiredByDateLabel),
  statusLabel: (a: PurchaseRequisitionRow, b: PurchaseRequisitionRow) => compareText(a.statusLabel, b.statusLabel),
} as const;

export type RequisitionSortColumn = keyof typeof REQUISITION_SORT_COMPARATORS;

export const QUOTE_SORT_COMPARATORS = {
  quoteNumber: (a: SupplierQuoteRow, b: SupplierQuoteRow) => compareText(a.quoteNumber, b.quoteNumber),
  requisitionNumber: (a: SupplierQuoteRow, b: SupplierQuoteRow) => compareText(a.requisitionNumber, b.requisitionNumber),
  statusLabel: (a: SupplierQuoteRow, b: SupplierQuoteRow) => compareText(a.statusLabel, b.statusLabel),
  quoteSourceLabel: (a: SupplierQuoteRow, b: SupplierQuoteRow) => compareText(a.quoteSourceLabel, b.quoteSourceLabel),
  totalAmount: (a: SupplierQuoteRow, b: SupplierQuoteRow) => compareNumber(a.totalAmount, b.totalAmount),
  validityUntilLabel: (a: SupplierQuoteRow, b: SupplierQuoteRow) => compareText(a.validityUntilLabel, b.validityUntilLabel),
  submittedAtLabel: (a: SupplierQuoteRow, b: SupplierQuoteRow) => compareText(a.submittedAtLabel, b.submittedAtLabel),
} as const;

export type QuoteSortColumn = keyof typeof QUOTE_SORT_COMPARATORS;

export const SALES_ORDER_SORT_COMPARATORS = {
  salesOrderNumber: (a: SalesOrderRow, b: SalesOrderRow) => compareText(a.salesOrderNumber, b.salesOrderNumber),
  purchaseOrderNumber: (a: SalesOrderRow, b: SalesOrderRow) => compareText(a.purchaseOrderNumber, b.purchaseOrderNumber),
  statusLabel: (a: SalesOrderRow, b: SalesOrderRow) => compareText(a.statusLabel, b.statusLabel),
  stageProgressLabel: (a: SalesOrderRow, b: SalesOrderRow) => compareText(a.stageProgressLabel, b.stageProgressLabel),
  totalAmount: (a: SalesOrderRow, b: SalesOrderRow) => compareNumber(a.totalAmount, b.totalAmount),
  expectedDeliveryLabel: (a: SalesOrderRow, b: SalesOrderRow) => compareText(a.expectedDeliveryLabel, b.expectedDeliveryLabel),
  createdAtLabel: (a: SalesOrderRow, b: SalesOrderRow) => compareText(a.createdAtLabel, b.createdAtLabel),
} as const;

export type SalesOrderSortColumn = keyof typeof SALES_ORDER_SORT_COMPARATORS;

export const PURCHASE_ORDER_SORT_COMPARATORS = {
  orderNumber: (a: PurchaseOrderRow, b: PurchaseOrderRow) => compareText(a.orderNumber, b.orderNumber),
  customerName: (a: PurchaseOrderRow, b: PurchaseOrderRow) => compareText(a.customerName, b.customerName),
  statusLabel: (a: PurchaseOrderRow, b: PurchaseOrderRow) => compareText(a.statusLabel, b.statusLabel),
  totalAmount: (a: PurchaseOrderRow, b: PurchaseOrderRow) => compareNumber(a.totalAmount, b.totalAmount),
  expectedDeliveryLabel: (a: PurchaseOrderRow, b: PurchaseOrderRow) =>
    compareText(a.expectedDeliveryLabel, b.expectedDeliveryLabel),
  createdAtLabel: (a: PurchaseOrderRow, b: PurchaseOrderRow) => compareText(a.createdAtLabel, b.createdAtLabel),
} as const;

export type PurchaseOrderSortColumn = keyof typeof PURCHASE_ORDER_SORT_COMPARATORS;

export const QUOTE_LINE_SORT_COMPARATORS = {
  lineNumber: (a: SupplierQuoteLineRow, b: SupplierQuoteLineRow) => compareNumber(a.lineNumber, b.lineNumber),
  quotedQuantity: (a: SupplierQuoteLineRow, b: SupplierQuoteLineRow) => compareNumber(a.quotedQuantity, b.quotedQuantity),
  unitPrice: (a: SupplierQuoteLineRow, b: SupplierQuoteLineRow) => compareNumber(a.unitPrice, b.unitPrice),
  lineTotal: (a: SupplierQuoteLineRow, b: SupplierQuoteLineRow) => compareNumber(a.lineTotal, b.lineTotal),
  leadTimeDays: (a: SupplierQuoteLineRow, b: SupplierQuoteLineRow) => compareNumber(a.leadTimeDays ?? -1, b.leadTimeDays ?? -1),
} as const;

export type QuoteLineSortColumn = keyof typeof QUOTE_LINE_SORT_COMPARATORS;

export function toggleInventoryTableSort<K extends string>(
  state: InventoryTableSortState<K>,
  column: K,
): InventoryTableSortState<K> {
  if (state.column === column) {
    return { column, direction: state.direction === 'asc' ? 'desc' : 'asc' };
  }
  return { column, direction: 'asc' };
}

export function sortInventoryRows<T, K extends string>(
  rows: readonly T[],
  state: InventoryTableSortState<K>,
  comparators: Record<K, (left: T, right: T) => number>,
): T[] {
  const { column, direction } = state;
  if (!column) {
    return [...rows];
  }
  const compare = comparators[column];
  if (!compare) {
    return [...rows];
  }
  const sorted = [...rows].sort(compare);
  return direction === 'desc' ? sorted.reverse() : sorted;
}

export function inventoryTableSortIcon<K extends string>(
  column: K,
  state: InventoryTableSortState<K>,
): string {
  if (state.column !== column) {
    return 'unfold_more';
  }
  return state.direction === 'asc' ? 'arrow_upward' : 'arrow_downward';
}

export class InventoryTableSortController<K extends string, T> {
  private column: K | null;
  private direction: SortDirection;

  constructor(
    private readonly comparators: Record<K, (left: T, right: T) => number>,
    initialColumn: K | null = null,
    initialDirection: SortDirection = 'asc',
  ) {
    this.column = initialColumn;
    this.direction = initialDirection;
  }

  get state(): InventoryTableSortState<K> {
    return { column: this.column, direction: this.direction };
  }

  toggle(column: K): void {
    const next = toggleInventoryTableSort(this.state, column);
    this.column = next.column;
    this.direction = next.direction;
  }

  sort(rows: readonly T[]): T[] {
    return sortInventoryRows(rows, this.state, this.comparators);
  }

  icon(column: K): string {
    return inventoryTableSortIcon(column, this.state);
  }

  isActive(column: K): boolean {
    return this.column === column;
  }
}

type RowFromComparators<C> = C extends Record<string, (left: infer T, right: infer T) => number> ? T : never;

/** Infers sortable columns from the comparators map (not from initialColumn). */
export function createInventoryTableSortController<
  C extends Record<string, (left: any, right: any) => number>,
>(
  comparators: C,
  initialColumn: (keyof C & string) | null = null,
  initialDirection: SortDirection = 'asc',
): InventoryTableSortController<keyof C & string, RowFromComparators<C>> {
  type K = keyof C & string;
  type T = RowFromComparators<C>;
  return new InventoryTableSortController(
    comparators as Record<K, (left: T, right: T) => number>,
    initialColumn as K | null,
    initialDirection,
  );
}
