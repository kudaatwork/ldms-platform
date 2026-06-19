export interface InventoryModuleComponent {
  id: string;
  portalLabel: string;
  icon: string;
  portalRoute: string;
  phase: number;
  phaseLabel: string;
  erpRole: string;
  ldmsRole: string;
  pushEndpoints: string[];
  outboundEvents: string[];
  dependsOn: string[];
  notes: string;
}

export interface IntegrationWorkflow {
  id: string;
  title: string;
  icon: string;
  trigger: string;
  steps: string[];
  components: string[];
}

export interface SyncPhase {
  order: number;
  title: string;
  summary: string;
  componentIds: string[];
}

export const INVENTORY_SYNC_PHASES: SyncPhase[] = [
  {
    order: 1,
    title: 'Foundation',
    summary: 'Register locations and product hierarchy before any stock or order documents.',
    componentIds: ['warehouses', 'categories', 'products'],
  },
  {
    order: 2,
    title: 'Stock ledger',
    summary: 'Load opening balances, then keep on-hand, reserved, and WAC in sync.',
    componentIds: ['stock', 'adjustments'],
  },
  {
    order: 3,
    title: 'Movements',
    summary: 'Reflect physical moves between warehouses and en-route depots.',
    componentIds: ['transfers'],
  },
  {
    order: 4,
    title: 'Commercial documents',
    summary: 'Push approved requisitions, POs, and sales orders so LDMS can fulfill and invoice.',
    componentIds: ['requisitions', 'quotations', 'purchase-orders', 'sales-orders'],
  },
];

export const INVENTORY_MODULE_COMPONENTS: InventoryModuleComponent[] = [
  {
    id: 'warehouses',
    portalLabel: 'Warehouses',
    icon: 'warehouse',
    portalRoute: '/products-inventory/warehouses',
    phase: 1,
    phaseLabel: 'Foundation',
    erpRole: 'Push branch DCs, depots, and en-route stop locations.',
    ldmsRole: 'Stores warehouse locations used by stock, transfers, and GRV receipts.',
    pushEndpoints: ['POST /warehouse-locations'],
    outboundEvents: [],
    dependsOn: [],
    notes: 'Sync warehouses before stock levels or transfers. Use stable location codes from your ERP.',
  },
  {
    id: 'categories',
    portalLabel: 'Product categories',
    icon: 'folder_open',
    portalRoute: '/products-inventory/categories',
    phase: 1,
    phaseLabel: 'Foundation',
    erpRole: 'Push category and sub-category master.',
    ldmsRole: 'Groups products for catalog, reporting, and portal browsing.',
    pushEndpoints: ['POST /product-category/create', 'POST /product-sub-category/create'],
    outboundEvents: [],
    dependsOn: [],
    notes: 'Category codes are idempotency keys. Products reference categoryCode on upsert.',
  },
  {
    id: 'products',
    portalLabel: 'Products',
    icon: 'category',
    portalRoute: '/products-inventory/products',
    phase: 1,
    phaseLabel: 'Foundation',
    erpRole: 'Push SKU master (name, UOM, category).',
    ldmsRole: 'Central product registry for stock, PO lines, SO lines, and shipments.',
    pushEndpoints: ['POST /product/create'],
    outboundEvents: [],
    dependsOn: ['categories'],
    notes: 'SKU is the primary idempotency key across the entire integration.',
  },
  {
    id: 'stock',
    portalLabel: 'Stock levels',
    icon: 'inventory',
    portalRoute: '/products-inventory/stock',
    phase: 2,
    phaseLabel: 'Stock ledger',
    erpRole: 'Push on-hand, reserved qty, and unit cost per warehouse.',
    ldmsRole: 'Runs reservations, WAC, PO fulfillment, and GRV matching on synced stock.',
    pushEndpoints: ['POST /inventory-item/create', 'POST /inventory-item/initial-stock'],
    outboundEvents: ['inventory.stock.updated'],
    dependsOn: ['products', 'warehouses'],
    notes: 'Use incremental sync for day-to-day deltas; initial-stock for go-live cutover.',
  },
  {
    id: 'adjustments',
    portalLabel: 'Stock adjustments',
    icon: 'tune',
    portalRoute: '/products-inventory/stock',
    phase: 2,
    phaseLabel: 'Stock ledger',
    erpRole: 'Push cycle-count and write-off deltas.',
    ldmsRole: 'Posts ledger corrections and audit trail entries.',
    pushEndpoints: ['POST /stock-adjustment/create'],
    outboundEvents: ['inventory.stock.updated'],
    dependsOn: ['stock'],
    notes: 'Alternatively let LDMS originate adjustments via GRV variance and webhook back to ERP.',
  },
  {
    id: 'transfers',
    portalLabel: 'Transfers',
    icon: 'sync_alt',
    portalRoute: '/products-inventory/transfers',
    phase: 3,
    phaseLabel: 'Movements',
    erpRole: 'Push transfer create, dispatch, and receipt events.',
    ldmsRole: 'Tracks in-transit stock, en-route depot stops, and completion GRVs.',
    pushEndpoints: [
      'POST /inventory-transfer/create',
      'POST /inventory-transfer/start-transit',
      'POST /inventory-transfer/complete',
    ],
    outboundEvents: ['inventory.stock.updated', 'inventory.transfer.completed'],
    dependsOn: ['stock', 'warehouses'],
    notes: 'Route stops mirror the portal transfer UI — same en-route warehouse model.',
  },
  {
    id: 'requisitions',
    portalLabel: 'Requisitions',
    icon: 'fact_check',
    portalRoute: '/products-inventory/requisitions',
    phase: 4,
    phaseLabel: 'Commercial documents',
    erpRole: 'Push internal purchase requisitions when approved in ERP.',
    ldmsRole: 'Supplier approval workflow, quotation requests, and PO creation.',
    pushEndpoints: ['POST /purchase-requisition/create'],
    outboundEvents: ['purchase.requisition.approved'],
    dependsOn: ['products', 'stock'],
    notes: 'Optional if LDMS is system of record for requisitions; required when ERP originates demand.',
  },
  {
    id: 'quotations',
    portalLabel: 'Quotations',
    icon: 'description',
    portalRoute: '/products-inventory/quotations',
    phase: 4,
    phaseLabel: 'Commercial documents',
    erpRole: 'Usually owned by LDMS after requisition; ERP may receive quote PDF via webhook.',
    ldmsRole: 'Supplier quotes against requisition lines before PO approval.',
    pushEndpoints: [],
    outboundEvents: ['quotation.submitted', 'quotation.accepted'],
    dependsOn: ['requisitions'],
    notes: 'Typically no inbound API — quotations are created in LDMS portal unless you build a custom bridge.',
  },
  {
    id: 'purchase-orders',
    portalLabel: 'Purchase orders',
    icon: 'shopping_cart',
    portalRoute: '/products-inventory/purchase-orders',
    phase: 4,
    phaseLabel: 'Commercial documents',
    erpRole: 'Push approved PO headers and lines.',
    ldmsRole: 'Reservation, inbound shipment, GRV, and supplier billing.',
    pushEndpoints: ['POST /purchase-order/create'],
    outboundEvents: ['purchase.order.approved', 'grv.created'],
    dependsOn: ['products', 'warehouses', 'stock'],
    notes: 'GRV callback URL on integration setup receives grv.created payloads back to ERP.',
  },
  {
    id: 'sales-orders',
    portalLabel: 'Sales orders',
    icon: 'sell',
    portalRoute: '/products-inventory/sales-orders',
    phase: 4,
    phaseLabel: 'Commercial documents',
    erpRole: 'Push customer orders approved for fulfillment.',
    ldmsRole: 'Dispatch, shipment creation, delivery GRV, and customer billing.',
    pushEndpoints: ['POST /sales-order/create', 'POST /sales-reservation/create'],
    outboundEvents: ['sales.order.dispatched', 'grv.created', 'inventory.stock.updated'],
    dependsOn: ['products', 'warehouses', 'stock'],
    notes: 'Reserve stock via sales-reservation before dispatch when ERP holds soft allocations.',
  },
];

export const INVENTORY_INTEGRATION_WORKFLOWS: IntegrationWorkflow[] = [
  {
    id: 'go-live',
    title: 'Go-live cutover',
    icon: 'rocket_launch',
    trigger: 'Weekend migration from ERP-only to LDMS operational inventory.',
    steps: [
      'Create integration API key on Inventory integration setup.',
      'Push warehouses → categories → products (Phase 1).',
      'Bulk initial-stock per warehouse (Phase 2).',
      'Smoke-test product-create and inventory-item/create in mock console.',
      'Enable scheduled delta sync and outbound stock webhooks.',
    ],
    components: ['warehouses', 'categories', 'products', 'stock'],
  },
  {
    id: 'procurement',
    title: 'Procurement loop',
    icon: 'shopping_cart',
    trigger: 'ERP approves internal demand or supplier PO.',
    steps: [
      'Optional: POST purchase-requisition/create when demand originates in ERP.',
      'LDMS quotation workflow runs in portal (or custom bridge).',
      'POST purchase-order/create with supplier ref and line SKUs.',
      'LDMS reserves stock, receives goods (GRV), posts inventory.stock.updated webhook.',
      'GRV callback fires to ERP with received quantities and variances.',
    ],
    components: ['requisitions', 'quotations', 'purchase-orders', 'stock'],
  },
  {
    id: 'fulfillment',
    title: 'Customer fulfillment',
    icon: 'local_shipping',
    trigger: 'ERP releases a customer sales order for dispatch.',
    steps: [
      'POST sales-order/create with customer ref and line items.',
      'POST sales-reservation/create to hold stock against the order.',
      'LDMS creates shipment and dispatch workflow in portal.',
      'On delivery GRV, grv.created callback and stock webhook update ERP.',
    ],
    components: ['sales-orders', 'stock', 'products'],
  },
  {
    id: 'inter-warehouse',
    title: 'Inter-warehouse transfer',
    icon: 'sync_alt',
    trigger: 'ERP or TMS confirms stock move between locations.',
    steps: [
      'POST inventory-transfer/create with from/to warehouses, optional routeStops (ORIGIN → EN_ROUTE_DEPOT → DESTINATION), and crossBorder.',
      'POST inventory-transfer/start-transit when truck departs.',
      'POST inventory-transfer/complete on receipt (or partial receipt qty).',
      'LDMS adjusts on-hand at both locations; stock webhook notifies ERP.',
    ],
    components: ['transfers', 'warehouses', 'stock'],
  },
  {
    id: 'reconciliation',
    title: 'Stock reconciliation',
    icon: 'fact_check',
    trigger: 'Nightly ERP stock take or cycle count.',
    steps: [
      'Compare ERP on-hand snapshot with LDMS stock levels.',
      'POST stock-adjustment/create for each variance line.',
      'Alternatively POST inventory-item/create with corrected quantityOnHand.',
      'Subscribe to inventory.stock.updated for LDMS-originated corrections (GRV variance).',
    ],
    components: ['stock', 'adjustments'],
  },
];
