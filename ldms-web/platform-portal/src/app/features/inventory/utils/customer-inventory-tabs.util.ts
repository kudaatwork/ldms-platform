/** Catalog tabs rendered by InventoryWorkspaceComponent on the customer route (/my-orders). */
export const CUSTOMER_CATALOG_TABS = ['warehouses', 'categories', 'products', 'stock', 'transfers'] as const;

/** Procurement tabs rendered by OrdersWorkspaceComponent on the customer route (/my-orders). */
export const CUSTOMER_ORDER_TABS = [
  'requisitions',
  'quotations',
  'purchase-orders',
  'sales-orders',
  'deliveries',
] as const;

export type CustomerCatalogTab = (typeof CUSTOMER_CATALOG_TABS)[number];
export type CustomerOrderTab = (typeof CUSTOMER_ORDER_TABS)[number];

export function isCustomerCatalogTab(tab: string | null | undefined): tab is CustomerCatalogTab {
  return !!tab && (CUSTOMER_CATALOG_TABS as readonly string[]).includes(tab);
}

export function isCustomerOrderTab(tab: string | null | undefined): tab is CustomerOrderTab {
  return !!tab && (CUSTOMER_ORDER_TABS as readonly string[]).includes(tab);
}
