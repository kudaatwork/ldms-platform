import { OrganizationClassification } from '../../core/models/auth.model';

export interface NavChild {
  label: string;
  route: string;
  /** Material Symbols name */
  icon: string;
}

export interface NavItem {
  label: string;
  route: string;
  /** Material Symbols name */
  icon: string;
  queryParams?: Record<string, string>;
  children?: NavChild[];
}

/** Organisation-scoped user management (contact person + future org users). */
export const USERS_NAV_ITEM: NavItem = {
  label: 'User management',
  route: '/users',
  icon: 'people_outline',
  children: [
    { label: 'All users', icon: 'person_search', route: '/users' },
    { label: 'User groups', icon: 'groups', route: '/users/groups' },
    { label: 'User roles', icon: 'verified_user', route: '/users/roles' },
    { label: 'User types', icon: 'category', route: '/users/types' },
  ],
};

/** Organisation audit log (login & user activity only). */
export const AUDIT_LOG_NAV_ITEM: NavItem = {
  label: 'Audit Log',
  route: '/activity',
  icon: 'receipt_long',
  children: [{ label: 'Login & activity', icon: 'history', route: '/activity/activity-logs' }],
};

/** Inserts audit log after user management when not already present. */
export function withAuditLogNav(items: NavItem[]): NavItem[] {
  if (items.some((item) => item.route === AUDIT_LOG_NAV_ITEM.route)) {
    return items;
  }
  const usersIndex = items.findIndex((item) => item.route === USERS_NAV_ITEM.route);
  if (usersIndex === -1) {
    return [...items, AUDIT_LOG_NAV_ITEM];
  }
  return [
    ...items.slice(0, usersIndex + 1),
    AUDIT_LOG_NAV_ITEM,
    ...items.slice(usersIndex + 1),
  ];
}

/** Supplier inventory management with child routes for each workspace tab. */
export const INVENTORY_NAV_ITEM: NavItem = {
  label: 'Inventory management',
  route: '/products-inventory',
  icon: 'inventory_2',
  children: [
    { label: 'Warehouses', icon: 'warehouse', route: '/products-inventory/warehouses' },
    { label: 'Product categories', icon: 'folder_open', route: '/products-inventory/categories' },
    { label: 'Products', icon: 'category', route: '/products-inventory/products' },
    { label: 'Stock levels', icon: 'inventory', route: '/products-inventory/stock' },
    { label: 'Transfers', icon: 'sync_alt', route: '/products-inventory/transfers' },
    { label: 'Requisitions', icon: 'fact_check', route: '/products-inventory/requisitions' },
    { label: 'Quotations', icon: 'description', route: '/products-inventory/quotations' },
    { label: 'Purchase orders', icon: 'shopping_cart', route: '/products-inventory/purchase-orders' },
    { label: 'Sales orders', icon: 'sell', route: '/products-inventory/sales-orders' },
  ],
};

/** Customer order management with child routes for each workspace tab. */
export const MY_ORDERS_NAV_ITEM: NavItem = {
  label: 'My Orders',
  route: '/my-orders',
  icon: 'receipt_long',
  children: [
    { label: 'Requisitions', icon: 'request_quote', route: '/my-orders/requisitions' },
    { label: 'Quotations', icon: 'description', route: '/my-orders/quotations' },
    { label: 'Purchase orders', icon: 'shopping_cart', route: '/my-orders/purchase-orders' },
    { label: 'Sales orders', icon: 'sell', route: '/my-orders/sales-orders' },
    { label: 'Deliveries', icon: 'local_shipping', route: '/my-orders/deliveries' },
  ],
};

/** Replaces flat inventory nav item with expandable submenu when not already present. */
export function withInventoryNav(items: NavItem[]): NavItem[] {
  if (items.some((item) => item.route === INVENTORY_NAV_ITEM.route)) {
    return items.map((item) =>
      item.route === '/products-inventory' && !item.children?.length ? INVENTORY_NAV_ITEM : item,
    );
  }
  const inventoryIndex = items.findIndex((item) => item.route === '/products-inventory');
  if (inventoryIndex === -1) {
    return items;
  }
  return [...items.slice(0, inventoryIndex), INVENTORY_NAV_ITEM, ...items.slice(inventoryIndex + 1)];
}

/** Replaces flat My Orders nav item with expandable submenu when not already present. */
export function withMyOrdersNav(items: NavItem[]): NavItem[] {
  if (items.some((item) => item.route === MY_ORDERS_NAV_ITEM.route && item.children?.length)) {
    return items;
  }
  const ordersIndex = items.findIndex((item) => item.route === '/my-orders');
  if (ordersIndex === -1) {
    return items;
  }
  return [...items.slice(0, ordersIndex), MY_ORDERS_NAV_ITEM, ...items.slice(ordersIndex + 1)];
}

/** Inserts org-scoped user management immediately after Documents when not already present. */
export function withUsersNavAfterDocuments(items: NavItem[]): NavItem[] {
  if (items.some((item) => item.route === USERS_NAV_ITEM.route)) {
    return items;
  }
  const documentsIndex = items.findIndex((item) => item.route === '/documents');
  if (documentsIndex === -1) {
    return [...items, USERS_NAV_ITEM];
  }
  return [
    ...items.slice(0, documentsIndex + 1),
    USERS_NAV_ITEM,
    ...items.slice(documentsIndex + 1),
  ];
}

export const NAV_CONFIG: Record<OrganizationClassification, NavItem[]> = {
  SUPPLIER: [
    { label: 'Dashboard', route: '/dashboard', icon: 'dashboard' },
    {
      ...INVENTORY_NAV_ITEM,
      children: [...(INVENTORY_NAV_ITEM.children ?? [])],
    },
    { label: 'Shipments', route: '/shipments', icon: 'local_shipping' },
    { label: 'Fleet & Transporters', route: '/fleet', icon: 'local_shipping' },
    { label: 'Customers', route: '/customers', icon: 'groups' },
    { label: 'Documents', route: '/documents', icon: 'folder_open' },
    { label: 'Billing', route: '/settings', icon: 'receipt_long', queryParams: { section: 'billing' } },
    { label: 'Reports', route: '/reports', icon: 'analytics' },
  ],
  CUSTOMER: [
    { label: 'Dashboard', route: '/dashboard', icon: 'dashboard' },
    {
      ...MY_ORDERS_NAV_ITEM,
      children: [...(MY_ORDERS_NAV_ITEM.children ?? [])],
    },
    { label: 'Track Shipments', route: '/track-shipments', icon: 'track_changes' },
    { label: 'Fleet & Transport', route: '/fleet', icon: 'local_shipping' },
    { label: 'Invoices', route: '/invoices', icon: 'request_quote' },
    { label: 'Documents', route: '/documents', icon: 'folder_open' },
    { label: 'Reports', route: '/reports', icon: 'analytics' },
  ],
  TRANSPORT_COMPANY: [
    { label: 'Dashboard', route: '/dashboard', icon: 'dashboard' },
    { label: 'Fleet & Transporters', route: '/fleet', icon: 'local_shipping' },
    { label: 'Drivers', route: '/fleet/drivers', icon: 'badge' },
    { label: 'Trips', route: '/trips', icon: 'route' },
    { label: 'Documents', route: '/documents', icon: 'folder_open' },
    { label: 'Billing', route: '/settings', icon: 'receipt_long', queryParams: { section: 'billing' } },
    { label: 'Reports', route: '/reports', icon: 'analytics' },
  ],
  CLEARING_AGENT: [
    { label: 'Dashboard', route: '/dashboard', icon: 'dashboard' },
    { label: 'Active Clearances', route: '/active-clearances', icon: 'fact_check' },
    { label: 'Documents', route: '/documents', icon: 'folder_open' },
    { label: 'Billing', route: '/settings', icon: 'receipt_long', queryParams: { section: 'billing' } },
    { label: 'Reports', route: '/reports', icon: 'analytics' },
  ],
  SERVICE_STATION: [
    { label: 'Dashboard', route: '/dashboard', icon: 'dashboard' },
    { label: 'Truck Visits', route: '/truck-visits', icon: 'local_gas_station' },
    { label: 'Fuel Log', route: '/fuel-log', icon: 'oil_barrel' },
    { label: 'Documents', route: '/documents', icon: 'folder_open' },
    { label: 'Reports', route: '/reports', icon: 'analytics' },
  ],
  ROADSIDE_SUPPORT_SERVICE: [
    { label: 'Dashboard', route: '/dashboard', icon: 'dashboard' },
    { label: 'Incidents', route: '/incidents', icon: 'car_crash' },
    { label: 'Service Log', route: '/service-log', icon: 'build' },
    { label: 'Documents', route: '/documents', icon: 'folder_open' },
    { label: 'Reports', route: '/reports', icon: 'analytics' },
  ],
  GOVERNMENT_AGENCY: [
    { label: 'Dashboard', route: '/dashboard', icon: 'dashboard' },
    { label: 'Border Activity', route: '/border-activity', icon: 'border_outer' },
    { label: 'Documents', route: '/documents', icon: 'folder_open' },
    { label: 'Reports', route: '/reports', icon: 'analytics' },
  ],
};
