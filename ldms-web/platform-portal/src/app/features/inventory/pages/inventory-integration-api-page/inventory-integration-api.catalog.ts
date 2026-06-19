export interface InvApiField {
  name: string;
  type: string;
  required: boolean;
  description: string;
  example: string;
}

export interface InventoryEndpointGroup {
  id: string;
  label: string;
  icon: string;
}

export interface InventoryIntegrationEndpoint {
  id: string;
  groupId: string;
  method: string;
  pathSuffix: string;
  title: string;
  summary: string;
  fields: InvApiField[];
  successExample: string;
}

export const INVENTORY_API_BASE = '/ldms-inventory-management/v1/system';

export const INVENTORY_ENDPOINT_GROUPS: InventoryEndpointGroup[] = [
  { id: 'master', label: 'Master data', icon: 'category' },
  { id: 'stock', label: 'Stock ledger', icon: 'inventory' },
  { id: 'movements', label: 'Transfers', icon: 'sync_alt' },
  { id: 'procurement', label: 'Procurement', icon: 'shopping_cart' },
  { id: 'fulfillment', label: 'Fulfillment', icon: 'sell' },
];

const AUTH_FIELD: InvApiField = {
  name: 'apiKey',
  type: 'string',
  required: true,
  description: 'Integration API key from Inventory integration setup. No JWT required.',
  example: 'inv-live-a1b2c3…',
};

export const INVENTORY_INTEGRATION_ENDPOINTS: InventoryIntegrationEndpoint[] = [
  {
    id: 'product-category-create',
    groupId: 'master',
    method: 'POST',
    pathSuffix: '/product-category/create',
    title: 'Upsert product category',
    summary: 'Create or update a category your ERP uses for product grouping.',
    fields: [
      AUTH_FIELD,
      { name: 'name', type: 'string', required: true, description: 'Category display name.', example: 'Building materials' },
      { name: 'code', type: 'string', required: true, description: 'Stable ERP code — used as idempotency key.', example: 'BUILD' },
    ],
    successExample: `{ "statusCode": 200, "success": true, "message": "Category saved", "categoryId": 14 }`,
  },
  {
    id: 'product-sub-category-create',
    groupId: 'master',
    method: 'POST',
    pathSuffix: '/product-sub-category/create',
    title: 'Upsert product sub-category',
    summary: 'Child category under a parent category code.',
    fields: [
      AUTH_FIELD,
      { name: 'name', type: 'string', required: true, description: 'Sub-category name.', example: 'Cement' },
      { name: 'code', type: 'string', required: true, description: 'ERP sub-category code.', example: 'CEMENT' },
      { name: 'categoryCode', type: 'string', required: true, description: 'Parent category code.', example: 'BUILD' },
    ],
    successExample: `{ "statusCode": 200, "success": true, "message": "Sub-category saved", "subCategoryId": 88 }`,
  },
  {
    id: 'product-create',
    groupId: 'master',
    method: 'POST',
    pathSuffix: '/product/create',
    title: 'Upsert product master',
    summary: 'Push SKU, name, category, and unit of measure. LDMS upserts on SKU match.',
    fields: [
      AUTH_FIELD,
      { name: 'sku', type: 'string', required: true, description: 'Your ERP SKU — primary idempotency key.', example: 'PRD-001' },
      { name: 'name', type: 'string', required: true, description: 'Product display name.', example: 'Portland Cement 50kg' },
      { name: 'categoryCode', type: 'string', required: false, description: 'Linked category code.', example: 'BUILD' },
      { name: 'unitOfMeasure', type: 'string', required: true, description: 'Base UOM for stock and orders.', example: 'BAG' },
    ],
    successExample: `{ "statusCode": 200, "success": true, "message": "Product master upserted", "productId": 1204 }`,
  },
  {
    id: 'warehouse-create',
    groupId: 'master',
    method: 'POST',
    pathSuffix: '/warehouse-locations',
    title: 'Register warehouse / depot',
    summary: 'Sync branch warehouses and en-route depot stops used in transfers.',
    fields: [
      AUTH_FIELD,
      { name: 'name', type: 'string', required: true, description: 'Warehouse or depot name.', example: 'Harare Central DC' },
      { name: 'code', type: 'string', required: true, description: 'ERP location code.', example: 'WH-HRE-01' },
      { name: 'locationType', type: 'string', required: false, description: 'WAREHOUSE, DEPOT, or EN_ROUTE.', example: 'WAREHOUSE' },
    ],
    successExample: `{ "statusCode": 200, "success": true, "message": "Warehouse location saved", "warehouseLocationId": 3 }`,
  },
  {
    id: 'inventory-item-create',
    groupId: 'stock',
    method: 'POST',
    pathSuffix: '/inventory-item/create',
    title: 'Sync stock level',
    summary: 'Set on-hand, reserved quantity, and weighted average cost per warehouse.',
    fields: [
      AUTH_FIELD,
      { name: 'productSku', type: 'string', required: true, description: 'Product SKU to update.', example: 'PRD-001' },
      { name: 'warehouseLocationId', type: 'integer', required: true, description: 'LDMS warehouse location ID.', example: '1' },
      { name: 'quantityOnHand', type: 'decimal', required: true, description: 'Available quantity in base UOM.', example: '500' },
      { name: 'reservedQuantity', type: 'decimal', required: false, description: 'Quantity reserved for orders.', example: '20' },
      { name: 'unitCost', type: 'decimal', required: false, description: 'WAC unit cost (4 dp).', example: '12.50' },
    ],
    successExample: `{ "statusCode": 200, "success": true, "message": "Stock level synchronised", "inventoryItemId": 8842 }`,
  },
  {
    id: 'initial-stock',
    groupId: 'stock',
    method: 'POST',
    pathSuffix: '/inventory-item/initial-stock',
    title: 'Bulk initial stock load',
    summary: 'Seed opening balances when onboarding a warehouse from ERP.',
    fields: [
      AUTH_FIELD,
      { name: 'warehouseLocationId', type: 'integer', required: true, description: 'Target warehouse.', example: '1' },
      { name: 'lines', type: 'array', required: true, description: 'SKU + quantity + optional unit cost rows.', example: '[{ "sku": "PRD-001", "quantity": 500 }]' },
    ],
    successExample: `{ "statusCode": 200, "success": true, "message": "Initial stock applied", "linesProcessed": 42 }`,
  },
  {
    id: 'stock-adjustment-create',
    groupId: 'stock',
    method: 'POST',
    pathSuffix: '/stock-adjustment/create',
    title: 'Post stock adjustment',
    summary: 'Push cycle-count or write-off deltas from your WMS.',
    fields: [
      AUTH_FIELD,
      { name: 'warehouseLocationId', type: 'integer', required: true, description: 'Warehouse where adjustment applies.', example: '1' },
      { name: 'productSku', type: 'string', required: true, description: 'Adjusted SKU.', example: 'PRD-001' },
      { name: 'quantityDelta', type: 'decimal', required: true, description: 'Signed quantity change.', example: '-2' },
      { name: 'reason', type: 'string', required: false, description: 'Adjustment reason code.', example: 'CYCLE_COUNT' },
    ],
    successExample: `{ "statusCode": 200, "success": true, "message": "Stock adjustment posted", "adjustmentId": 501 }`,
  },
  {
    id: 'transfer-create',
    groupId: 'movements',
    method: 'POST',
    pathSuffix: '/inventory-transfer/create',
    title: 'Create transfer',
    summary: 'Warehouse-to-warehouse move with optional en-route depot stops.',
    fields: [
      AUTH_FIELD,
      { name: 'productId', type: 'integer', required: true, description: 'LDMS product ID.', example: '42' },
      { name: 'fromLocationId', type: 'integer', required: true, description: 'Source warehouse ID.', example: '1' },
      { name: 'toLocationId', type: 'integer', required: true, description: 'Destination warehouse ID.', example: '3' },
      { name: 'quantity', type: 'decimal', required: true, description: 'Quantity in base UOM.', example: '100' },
      { name: 'createdByUserId', type: 'integer', required: true, description: 'User ID initiating the transfer.', example: '7' },
      { name: 'crossBorder', type: 'boolean', required: false, description: 'When true, border clearance is required after approval.', example: 'true' },
      {
        name: 'routeStops',
        type: 'array',
        required: false,
        description: 'Ordered stops: ORIGIN, EN_ROUTE_DEPOT(s), DESTINATION — each with stopSequence, stopType, warehouseLocationId.',
        example: '[{"stopSequence":0,"stopType":"ORIGIN","warehouseLocationId":1},{"stopSequence":1,"stopType":"EN_ROUTE_DEPOT","warehouseLocationId":2},{"stopSequence":2,"stopType":"DESTINATION","warehouseLocationId":3}]',
      },
    ],
    successExample: `{ "statusCode": 200, "success": true, "message": "Transfer created", "transferId": 310 }`,
  },
  {
    id: 'transfer-start',
    groupId: 'movements',
    method: 'POST',
    pathSuffix: '/inventory-transfer/start-transit',
    title: 'Start transfer transit',
    summary: 'Mark goods as in-transit when your TMS dispatches the truck.',
    fields: [
      AUTH_FIELD,
      { name: 'transferId', type: 'integer', required: true, description: 'LDMS transfer ID.', example: '310' },
      { name: 'dispatchedAt', type: 'datetime', required: false, description: 'ISO-8601 dispatch time.', example: '2026-06-17T08:00:00' },
    ],
    successExample: `{ "statusCode": 200, "success": true, "message": "Transfer in transit" }`,
  },
  {
    id: 'transfer-complete',
    groupId: 'movements',
    method: 'POST',
    pathSuffix: '/inventory-transfer/complete',
    title: 'Complete transfer',
    summary: 'Confirm receipt at destination warehouse.',
    fields: [
      AUTH_FIELD,
      { name: 'transferId', type: 'integer', required: true, description: 'LDMS transfer ID.', example: '310' },
      { name: 'receivedQuantity', type: 'decimal', required: false, description: 'Actual received qty if partial.', example: '98' },
    ],
    successExample: `{ "statusCode": 200, "success": true, "message": "Transfer completed" }`,
  },
  {
    id: 'requisition-create',
    groupId: 'procurement',
    method: 'POST',
    pathSuffix: '/purchase-requisition/create',
    title: 'Create purchase requisition',
    summary: 'Push internal requisitions from ERP for supplier approval in LDMS.',
    fields: [
      AUTH_FIELD,
      { name: 'requisitionRef', type: 'string', required: true, description: 'ERP requisition number.', example: 'REQ-2026-0142' },
      { name: 'lines', type: 'array', required: true, description: 'SKU, quantity, required date per line.', example: '[{ "sku": "PRD-001", "quantity": 200 }]' },
    ],
    successExample: `{ "statusCode": 200, "success": true, "message": "Requisition created", "requisitionId": 77 }`,
  },
  {
    id: 'purchase-order-create',
    groupId: 'procurement',
    method: 'POST',
    pathSuffix: '/purchase-order/create',
    title: 'Create purchase order',
    summary: 'Sync approved PO headers and lines from your ERP.',
    fields: [
      AUTH_FIELD,
      { name: 'purchaseOrderRef', type: 'string', required: true, description: 'ERP PO number — idempotency key.', example: 'PO-8842' },
      { name: 'supplierRef', type: 'string', required: true, description: 'Supplier trading partner ref.', example: 'SUP-001' },
      { name: 'lines', type: 'array', required: true, description: 'SKU, quantity, unit price per line.', example: '[{ "sku": "PRD-001", "quantity": 200, "unitPrice": 11.80 }]' },
    ],
    successExample: `{ "statusCode": 200, "success": true, "message": "Purchase order created", "purchaseOrderId": 442 }`,
  },
  {
    id: 'sales-order-create',
    groupId: 'fulfillment',
    method: 'POST',
    pathSuffix: '/sales-order/create',
    title: 'Create sales order',
    summary: 'Push customer orders for LDMS fulfillment and dispatch.',
    fields: [
      AUTH_FIELD,
      { name: 'salesOrderRef', type: 'string', required: true, description: 'ERP sales order number.', example: 'SO-2026-0091' },
      { name: 'customerRef', type: 'string', required: true, description: 'Customer trading partner ref.', example: 'CUST-001' },
      { name: 'lines', type: 'array', required: true, description: 'SKU and ordered quantity per line.', example: '[{ "sku": "PRD-001", "quantity": 50 }]' },
    ],
    successExample: `{ "statusCode": 200, "success": true, "message": "Sales order created", "salesOrderId": 991 }`,
  },
  {
    id: 'sales-reservation-create',
    groupId: 'fulfillment',
    method: 'POST',
    pathSuffix: '/sales-reservation/create',
    title: 'Reserve stock for order',
    summary: 'Hold on-hand quantity against a sales order line.',
    fields: [
      AUTH_FIELD,
      { name: 'salesOrderId', type: 'integer', required: true, description: 'LDMS sales order ID.', example: '991' },
      { name: 'productSku', type: 'string', required: true, description: 'SKU to reserve.', example: 'PRD-001' },
      { name: 'quantity', type: 'decimal', required: true, description: 'Reserved quantity.', example: '50' },
    ],
    successExample: `{ "statusCode": 200, "success": true, "message": "Stock reserved", "reservationId": 120 }`,
  },
];

export const INVENTORY_SYNC_RESPONSE_CODES = [
  { code: 200, label: 'OK', detail: 'Record upserted or stock synchronised successfully.' },
  { code: 400, label: 'Bad request', detail: 'Missing required fields, invalid warehouse, or unknown SKU.' },
  { code: 401, label: 'Unauthorized', detail: 'Integration API key not found, suspended, or revoked.' },
  { code: 409, label: 'Conflict', detail: 'Duplicate external reference — same idempotency key already processed.' },
  { code: 503, label: 'Unavailable', detail: 'Inventory service temporarily unreachable.' },
];
