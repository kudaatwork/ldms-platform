/** Matches {@code PurchaseRequisitionStatus} on ldms-inventory-management. */
export type RequisitionStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'APPROVED'
  | 'PARTIALLY_FULFILLED'
  | 'FULFILLED'
  | 'CLOSED'
  | 'CANCELLED'
  | 'REJECTED'
  | 'EXPIRED'
  | 'PUBLISHED_TO_SUPPLIER'
  | 'SUPPLIER_CONFIRMED'
  | 'CUSTOMER_ACKNOWLEDGED';

/** Matches {@code SalesOrderStatus} on ldms-inventory-management. */
export type SalesOrderStatus =
  | 'AWAITING_RECEIPT'
  | 'PENDING'
  | 'CONFIRMED'
  | 'PENDING_APPROVAL'
  | 'APPROVED'
  | 'PARTIALLY_SHIPPED'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'FULFILLED'
  | 'CANCELLED';

/** Matches {@code PurchaseOrderStatus} on ldms-inventory-management. */
export type PurchaseOrderStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'APPROVED'
  | 'PARTIALLY_RECEIVED'
  | 'RECEIVED'
  | 'CANCELLED'
  | 'REJECTED'
  | 'PENDING_CUSTOMER_APPROVAL'
  | 'CUSTOMER_APPROVED'
  | 'PENDING_SUPPLIER_APPROVAL';

/** Matches {@code TransferStatus} on ldms-inventory-management. */
export type TransferStatus =
  | 'REQUESTED'
  | 'APPROVED'
  | 'IN_TRANSIT'
  | 'COMPLETED'
  | 'REJECTED'
  | 'CANCELLED';

export const TRANSFER_STATUS_FILTER_OPTIONS: ReadonlyArray<{ value: '' | TransferStatus; label: string }> = [
  { value: '', label: 'All statuses' },
  { value: 'REQUESTED', label: 'Requested' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'IN_TRANSIT', label: 'In transit' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'CANCELLED', label: 'Cancelled' },
];

export type StatusTone = 'muted' | 'warn' | 'success' | 'danger';

/** View row returned from product list API. */
export interface ProductRow {
  id: number;
  name: string;
  code: string;
  barcode: string;
  description: string;
  unitPrice: number;
  unitPriceLabel: string;
  categoryId: number;
  categoryName: string;
  subcategoryId: number;
  subcategoryName: string;
  unitOfMeasure: string;
  unitOfMeasureLabel: string;
  manufacturer: string;
  imageId: number | null;
  expiresAtLabel: string;
  supplierId: number;
  entityStatus: string;
  createdAtLabel: string;
  updatedAtLabel: string;
  /** Raw API / database timestamp values for detail view. */
  createdAtIso: string;
  updatedAtIso: string;
  expiresAtIso: string;
  initials: string;
  accentHue: number;
}

/** View row for warehouse location list. */
export interface WarehouseRow {
  id: number;
  name: string;
  description: string;
  warehouseType: string;
  locationId: string;
  supplierId: number;
  branchId?: number;
  branchLabel?: string;
  organizationOwned?: boolean;
  sharedAccess?: boolean;
  callerAccessLevel?: 'READ' | 'FULFILL';
  addressLabel: string;
  entityStatus: string;
  createdAtLabel: string;
}

export type WarehouseLocationType = 'SUPPLIER' | 'CUSTOMER' | 'TRANSIT';

export interface WarehouseAccessGrant {
  id?: number;
  warehouseLocationId: number;
  grantedOrganizationId: number;
  accessLevel: 'READ' | 'FULFILL';
}

export interface WarehouseImportSummary {
  success: boolean;
  message: string;
  total: number;
  imported: number;
  failed: number;
  errors: string[];
}

export const WAREHOUSE_LOCATION_TYPE_OPTIONS: ReadonlyArray<{ value: WarehouseLocationType; label: string }> = [
  { value: 'SUPPLIER', label: 'Supplier warehouse' },
  { value: 'CUSTOMER', label: 'Customer warehouse' },
];

export type StockLevelStatus = 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK' | 'FULLY_RESERVED';

/** View row for current stock per product per warehouse. */
export interface StockRow {
  id: number;
  productId: number;
  productName: string;
  productCode: string;
  productBarcode: string;
  warehouseLocationId: number;
  warehouseName: string;
  quantityOnHand: number;
  reservedQuantity: number;
  availableQuantity: number;
  unitOfMeasure: string;
  unitCost: number;
  unitCostLabel: string;
  reorderPoint: number;
  status: StockLevelStatus;
  statusLabel: string;
  statusTone: StatusTone;
  isLowStock: boolean;
}

/** View row for purchase requisition list. */
export interface PurchaseRequisitionRow {
  id: number;
  requisitionNumber: string;
  status: RequisitionStatus;
  statusLabel: string;
  statusTone: StatusTone;
  stageProgressLabel: string;
  purpose: string;
  priority: string;
  priorityLabel: string;
  requestedBy: string;
  totalAmount: number;
  totalAmountLabel: string;
  requiredByDateLabel: string;
  lineCount: number;
  createdAtLabel: string;
  submittedAtLabel: string;
  supplierQuoteId?: number;
  currentApprovalStage?: number;
  requiredApprovalStages?: number;
}

export type SupplierQuoteStatus = 'DRAFT' | 'SUBMITTED' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED';

/** View row for supplier quotation list. */
export interface SupplierQuoteRow {
  id: number;
  quoteNumber: string;
  purchaseRequisitionId: number;
  requisitionNumber: string;
  status: SupplierQuoteStatus;
  statusLabel: string;
  quoteSource: QuoteCaptureSource;
  quoteSourceLabel: string;
  currency: string;
  subtotal: number;
  taxAmount: number;
  totalAmount: number;
  totalAmountLabel: string;
  paymentTerm: string;
  deliveryTerms: string;
  validityUntilLabel: string;
  submittedAtLabel: string;
  lineCount: number;
  externalDocumentId?: number;
  notes?: string;
}

export interface SupplierQuoteLineRow {
  lineNumber: number;
  productId?: number;
  quotedQuantity: number;
  unitPrice: number;
  lineTotal: number;
  unitPriceLabel: string;
  lineTotalLabel: string;
  leadTimeDays?: number;
  notes?: string;
}

export interface SupplierQuoteDetail extends SupplierQuoteRow {
  lines: SupplierQuoteLineRow[];
}

/** View row for sales order list. */
export interface SalesOrderRow {
  id: number;
  salesOrderNumber: string;
  purchaseOrderId: number;
  purchaseOrderNumber: string;
  customerId: number;
  supplierOrganizationId: number;
  status: SalesOrderStatus;
  statusLabel: string;
  statusTone: StatusTone;
  stageProgressLabel: string;
  totalAmount: number;
  totalAmountLabel: string;
  orderDateLabel: string;
  expectedDeliveryLabel: string;
  createdAtLabel: string;
  currentApprovalStage?: number;
  requiredApprovalStages?: number;
}

/** View row for purchase order list. */
export interface PurchaseOrderRow {
  id: number;
  orderNumber: string;
  supplierName: string;
  customerName: string;
  status: PurchaseOrderStatus;
  statusLabel: string;
  statusTone: StatusTone;
  stageProgressLabel: string;
  totalAmount: number;
  totalAmountLabel: string;
  expectedDeliveryLabel: string;
  createdAtLabel: string;
  currentCustomerApprovalStage?: number;
  currentSupplierApprovalStage?: number;
  requiredApprovalStages?: number;
  customerApprovalComplete?: boolean;
  supplierApprovalComplete?: boolean;
}

/** View row for inventory transfer list. */
export interface TransferRow {
  id: number;
  transferNumber: string;
  productId: number;
  productName: string;
  productCode: string;
  unitOfMeasure: string;
  quantity: number;
  unitCost: number;
  unitCostLabel: string;
  reference: string;
  rejectionReason: string;
  rejectedAtLabel: string;
  fromLocationId: number;
  toLocationId: number;
  fromWarehouse: string;
  toWarehouse: string;
  status: TransferStatus;
  statusLabel: string;
  statusTone: StatusTone;
  createdByUserId: number;
  requestedBy: string;
  createdAtLabel: string;
  updatedAtLabel: string;
  canApprove?: boolean;
  canReject?: boolean;
  canStartTransit?: boolean;
  canComplete?: boolean;
  canCancel?: boolean;
  crossBorder?: boolean;
}

/** PO line for goods receipt dialog. */
export interface PurchaseOrderLineRow {
  id: number;
  purchaseOrderId: number;
  productId: number;
  productName: string;
  orderedQuantity: number;
  receivedQuantity: number;
  remainingQuantity: number;
  unitOfMeasure: string;
}

/** View row for product category list / management. */
export interface ProductCategoryRow {
  id: number;
  name: string;
  description: string;
  entityStatus: string;
  createdAtLabel: string;
}

/** View row for product subcategory list / management. */
export interface ProductSubCategoryRow {
  id: number;
  categoryId: number;
  categoryName: string;
  name: string;
  description: string;
  entityStatus: string;
  createdAtLabel: string;
}

/** Subcategory option for product create dropdown. */
export interface ProductSubCategoryOption {
  id: number;
  categoryId: number;
  name: string;
}

/** Category option for product create/edit dropdown. */
export interface ProductCategoryOption {
  id: number;
  name: string;
  code: string;
}

/** Matches {@code UnitOfMeasure} on ldms-inventory-management. */
export interface ProductUnitOfMeasureOption {
  value: string;
  label: string;
}

export const PRODUCT_UNIT_OF_MEASURE_OPTIONS: ProductUnitOfMeasureOption[] = [
  { value: 'EACH', label: 'Each / unit' },
  { value: 'BOX', label: 'Box' },
  { value: 'PACK', label: 'Pack' },
  { value: 'KILOGRAM', label: 'Kilogram (kg)' },
  { value: 'GRAM', label: 'Gram (g)' },
  { value: 'LITER', label: 'Litre (L)' },
  { value: 'MILLILITER', label: 'Millilitre (mL)' },
  { value: 'METER', label: 'Metre (m)' },
  { value: 'CENTIMETER', label: 'Centimetre (cm)' },
  { value: 'CYLINDER', label: 'Cylinder (gas bottle)' },
  { value: 'CUBIC_METER', label: 'Cubic metre (m³ — bulk gas)' },
  { value: 'SERVICE', label: 'Service' },
];

export function productUnitOfMeasureLabel(value: string | null | undefined): string {
  const normalized = String(value ?? '').trim().toUpperCase();
  if (!normalized) {
    return '—';
  }
  return PRODUCT_UNIT_OF_MEASURE_OPTIONS.find((option) => option.value === normalized)?.label ?? normalized;
}

/** POST /product/create — matches {@code CreateProductRequest} on inventory-management. */
export interface CreateProductPayload {
  name: string;
  productCode: string;
  barcode?: string;
  description?: string;
  price: number;
  productCategoryId: number;
  productSubCategoryId?: number;
  unitOfMeasure: string;
  supplierId: number;
}

/** PUT /product/update — matches {@code EditProductRequest} on inventory-management. */
export interface EditProductPayload {
  productId: number;
  name: string;
  productCode: string;
  barcode?: string;
  description?: string;
  price: number;
  categoryId: number;
  subcategoryId?: number;
  unitOfMeasure: string;
  supplierId: number;
}

/** POST /product-category/create */
export interface CreateProductCategoryPayload {
  name: string;
  description: string;
}

/** PUT /product-category/update */
export interface EditProductCategoryPayload {
  productCategoryId: number;
  name: string;
  description: string;
}

/** POST /product-sub-category/create */
export interface CreateProductSubCategoryPayload {
  categoryId: number;
  name: string;
  description: string;
}

/** PUT /product-sub-category/update */
export interface EditProductSubCategoryPayload {
  productSubCategoryId: number;
  categoryId: number;
  name: string;
  description: string;
}

/** POST /warehouse-locations */
export interface CreateWarehouseLocationPayload {
  name: string;
  description: string;
  line1: string;
  line2?: string;
  postalCode?: string;
  suburbId: number;
  geoCoordinatesId?: number;
  supplierId: number;
  branchId: number;
  warehouseType: WarehouseLocationType;
}

/** PUT /warehouse-locations/{id} — matches {@code EditWarehouseLocationRequest}. */
export interface EditWarehouseLocationPayload {
  warehouseLocationId: number;
  name: string;
  description: string;
  locationId: string;
  supplierId: number;
  branchId?: number;
  warehouseType: WarehouseLocationType;
}

export type PriorityLevel = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';

export type FulfillmentMethod =
  | 'PURCHASE'
  | 'FROM_STOCK'
  | 'TRANSFER'
  | 'DEFERRED'
  | 'NOT_REQUIRED';

export const PRIORITY_LEVEL_OPTIONS: ReadonlyArray<{ value: PriorityLevel; label: string }> = [
  { value: 'LOW', label: 'Low' },
  { value: 'NORMAL', label: 'Normal' },
  { value: 'HIGH', label: 'High' },
  { value: 'URGENT', label: 'Urgent' },
];

export const FULFILLMENT_METHOD_OPTIONS: ReadonlyArray<{ value: FulfillmentMethod; label: string }> = [
  { value: 'PURCHASE', label: 'Purchase from supplier' },
  { value: 'FROM_STOCK', label: 'From existing stock' },
  { value: 'TRANSFER', label: 'Internal transfer' },
  { value: 'DEFERRED', label: 'Deferred procurement' },
  { value: 'NOT_REQUIRED', label: 'Not required' },
];

/** Line on POST /purchase-requisition/create — matches PurchaseRequisitionLineRequest. */
export interface CreateRequisitionLinePayload {
  productId: number;
  productDescription?: string;
  unitOfMeasure: string;
  requestedQuantity: number;
  estimatedUnitPrice?: number;
  fulfillmentMethod?: FulfillmentMethod;
  specifications?: string;
  preferredBrand?: string;
  isSubstituteAcceptable?: boolean;
}

/** POST /purchase-requisition/create — matches CreatePurchaseRequisitionRequest. */
export interface CreateRequisitionPayload {
  organizationId: number;
  departmentId: number;
  requestedByUserId: number;
  createdByUserId: number;
  purpose: string;
  justification?: string;
  priority?: PriorityLevel;
  requisitionDate?: string;
  requiredByDate?: string;
  expiryDate?: string;
  defaultFulfillmentMethod?: FulfillmentMethod;
  targetWarehouseId?: number;
  preferredSupplierId?: number;
  currency?: string;
  budgetAvailable?: boolean;
  budgetCode?: string;
  costCenter?: string;
  projectCode?: string;
  notes?: string;
  lines: CreateRequisitionLinePayload[];
}

export interface DepartmentOption {
  id: number;
  name: string;
}

/** POST /inventory-transfer/create — matches CreateInventoryTransferRequest. */
export interface CreateTransferPayload {
  transferNumber: string;
  productId: number;
  fromLocationId: number;
  toLocationId: number;
  quantity: number;
  unitCost?: number;
  status?: TransferStatus;
  reference?: string;
  crossBorder?: boolean;
  createdByUserId: number;
}

/** POST /inventory-item/initial-stock — matches CreateInitialStockRequest. */
export interface CreateInitialStockPayload {
  productId: number;
  warehouseLocationId: number;
  supplierId: number;
  quantity: number;
  unitCost: number;
  minStockLevel?: number;
  reorderQuantity?: number;
  notes?: string;
  createdByUserId: number;
}

/** POST /stock-adjustment/create — positive quantity replenishment. */
export interface ReplenishStockPayload {
  inventoryItemId: number;
  quantity: number;
  unitCost: number;
  reason?: string;
  adjustedByUserId: number;
}

/** POST /purchase-requisition/approve */
export interface ApproveRequisitionPayload {
  id: number;
  approvedByUserId: number;
  approvalNotes?: string;
}

/** POST /purchase-requisition/reject */
export interface RejectRequisitionPayload {
  id: number;
  rejectedByUserId: number;
  rejectionReason: string;
}

/** POST /purchase-orders/{id}/receive — matches ReceiveGoodsRequest. */
export interface ReceiveGoodsPayload {
  purchaseOrderId: number;
  warehouseLocationId: number;
  receivedByUserId: number;
  notes?: string;
  idempotencyKey: string;
  receivedItems: Array<{
    purchaseOrderLineId: number;
    quantityReceived: number;
    reason?: string;
  }>;
}

/** Hero statistics bar for inventory workspace. */
export interface InventoryWorkspaceMetrics {
  totalProducts: number;
  totalCategories: number;
  totalSubCategories: number;
  totalWarehouses: number;
  lowStockItems: number;
  pendingTransfers: number;
  pendingRequisitions: number;
  pendingOrders: number;
}

export type CatalogWorkspaceView = 'categories' | 'subcategories';

export type InventoryWorkspaceTab =
  | 'warehouses'
  | 'categories'
  | 'products'
  | 'stock'
  | 'transfers'
  | 'requisitions'
  | 'quotations'
  | 'purchase-orders'
  | 'sales-orders';
export type OrdersWorkspaceTab =
  | 'requisitions'
  | 'quotations'
  | 'purchase-orders'
  | 'sales-orders'
  | 'deliveries';

/** POST /procurement-workflow/pr/approve-stage */
export interface ApprovePrStagePayload {
  purchaseRequisitionId: number;
  approvedByUserId: number;
  approvalNotes?: string;
}

export type QuoteCaptureSource = 'SYSTEM_GENERATED' | 'EXTERNAL_UPLOAD';

/** POST /procurement-workflow/pr/submit-quote — supplier quote with metadata (both capture modes). */
export interface SubmitQuotePayload {
  purchaseRequisitionId: number;
  supplierOrganizationId: number;
  submittedByUserId: number;
  quoteSource: QuoteCaptureSource;
  currency: string;
  paymentTerm: string;
  deliveryTerms: string;
  validityUntil: string;
  taxAmount?: number;
  notes?: string;
  externalDocumentId?: number;
  lines: Array<{
    purchaseRequisitionLineId: number;
    productId?: number;
    quotedQuantity: number;
    unitPrice: number;
  }>;
}

/** POST /procurement-workflow/pr/acknowledge-quote — customer acknowledges supplier quote */
export interface AcknowledgeQuotePayload {
  purchaseRequisitionId: number;
  acknowledgedByUserId: number;
}

/** POST /procurement-workflow/po/approve-customer-stage */
export interface ApprovePoCustomerStagePayload {
  purchaseOrderId: number;
  approvedByUserId: number;
  approvalNotes?: string;
}

/** POST /procurement-workflow/po/approve-supplier-stage */
export interface ApprovePoSupplierStagePayload {
  purchaseOrderId: number;
  approvedByUserId: number;
  approvalNotes?: string;
}

/** POST /procurement-workflow/so/approve-stage */
export interface ApproveSoStagePayload {
  salesOrderId: number;
  approvedByUserId: number;
  approvalNotes?: string;
  fulfillmentWarehouseId?: number;
}

/** Procurement approval policy — matches {@code ProcurementApprovalPolicyDto}. */
export interface ProcurementApprovalPolicy {
  defaultRequiredApprovalStages: number;
  minAllowedStages: number;
  maxAllowedStages: number;
}

export const SUPPLIER_QUOTE_STATUS_FILTER_OPTIONS: ReadonlyArray<{
  value: '' | SupplierQuoteStatus;
  label: string;
}> = [
  { value: '', label: 'All statuses' },
  { value: 'SUBMITTED', label: 'Submitted' },
  { value: 'ACCEPTED', label: 'Accepted' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'EXPIRED', label: 'Expired' },
  { value: 'DRAFT', label: 'Draft' },
];

export const QUOTE_SOURCE_FILTER_OPTIONS: ReadonlyArray<{
  value: '' | QuoteCaptureSource;
  label: string;
}> = [
  { value: '', label: 'All sources' },
  { value: 'SYSTEM_GENERATED', label: 'Built in LX' },
  { value: 'EXTERNAL_UPLOAD', label: 'Uploaded document' },
];

export const SUPPLIER_REQUISITION_STATUS_FILTER_OPTIONS: ReadonlyArray<{
  value: '' | RequisitionStatus;
  label: string;
}> = [
  { value: '', label: 'All statuses' },
  { value: 'PUBLISHED_TO_SUPPLIER', label: 'Awaiting quote' },
  { value: 'SUPPLIER_CONFIRMED', label: 'Quote submitted' },
  { value: 'CUSTOMER_ACKNOWLEDGED', label: 'Customer acknowledged' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'CANCELLED', label: 'Cancelled' },
];
