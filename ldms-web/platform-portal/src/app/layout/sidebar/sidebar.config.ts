import { OrganizationClassification } from '../../core/models/auth.model';

export interface NavChild {
  label: string;
  route: string;
  /** Material Symbols name */
  icon: string;
  queryParams?: Record<string, string>;
  /** Shown only when inventory data source is EXTERNAL_API (ERP/WMS sync). */
  integrationOnly?: boolean;
}

export interface NavItem {
  label: string;
  route: string;
  /** Material Symbols name */
  icon: string;
  queryParams?: Record<string, string>;
  children?: NavChild[];
}

/** Branches, agents, and contracted transporter organisations. */
export const ORGANIZATION_MANAGEMENT_NAV_ITEM: NavItem = {
  label: 'Organization management',
  route: '/organization',
  icon: 'corporate_fare',
  children: [
    { label: 'Branches', icon: 'account_tree', route: '/organization/branches' },
    { label: 'Sub-branches & depots', icon: 'fork_right', route: '/organization/sub-branches' },
    { label: 'Agents', icon: 'support_agent', route: '/organization/agents' },
    { label: 'Transporters', icon: 'local_shipping', route: '/organization/transporters' },
  ],
};

const ORGANIZATION_MANAGEMENT_NAV_TRANSPORT_ONLY: NavItem = {
  ...ORGANIZATION_MANAGEMENT_NAV_ITEM,
  children: ORGANIZATION_MANAGEMENT_NAV_ITEM.children?.filter(
    (child) => child.route !== '/organization/transporters',
  ),
};

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

/** Analytics hub with child routes — mirrors inventory submenu pattern. */
export const ANALYTICS_NAV_ITEM: NavItem = {
  label: 'Analytics',
  route: '/analytics',
  icon: 'insights',
  children: [
    { label: 'Platform usage & changes', icon: 'bar_chart', route: '/analytics/platform-usage' },
    { label: 'Trips', icon: 'route', route: '/analytics/trips' },
  ],
};

/** Replaces flat reports nav item with expandable Analytics submenu. */
export function withAnalyticsNav(items: NavItem[]): NavItem[] {
  const reportsIndex = items.findIndex((item) => item.route === '/reports' || item.route === '/analytics');
  if (reportsIndex === -1) {
    return items;
  }
  const existing = items[reportsIndex];
  if (existing.route === ANALYTICS_NAV_ITEM.route && existing.children?.length) {
    return items;
  }
  return [...items.slice(0, reportsIndex), ANALYTICS_NAV_ITEM, ...items.slice(reportsIndex + 1)];
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
    { label: 'Inventory integration API', icon: 'api', route: '/products-inventory/integration-api', queryParams: { mode: 'inventory' } },
    { label: 'Inventory integration setup', icon: 'key', route: '/products-inventory/integration-setup', integrationOnly: true },
  ],
};

/** Fleet workspace tabs — mirrors in-page navigation. */
export const FLEET_NAV_ITEM: NavItem = {
  label: 'Fleet & Transporters',
  route: '/fleet',
  icon: 'local_shipping',
  children: [
    { label: 'Overview', icon: 'dashboard', route: '/fleet/overview' },
    { label: 'Own fleet', icon: 'garage', route: '/fleet/convoy' },
    { label: 'Contracted transporters', icon: 'handshake', route: '/fleet/partners' },
    { label: 'Drivers', icon: 'badge', route: '/fleet/drivers' },
    { label: 'Compliance', icon: 'verified', route: '/fleet/compliance' },
    { label: 'Device installation', icon: 'sensors', route: '/fleet/tracking' },
    { label: 'Tracking integration API', icon: 'api', route: '/fleet/tracking-api' },
    { label: 'Tracking integration setup', icon: 'key', route: '/fleet/tracking-setup' },
  ],
};

/** Shipment management workspace — shipments, trips, and border clearance. */
export const SHIPMENT_MANAGEMENT_NAV_ITEM: NavItem = {
  label: 'Shipment management',
  route: '/shipments',
  icon: 'assignment',
  children: [
    { label: 'Shipments', icon: 'inventory_2', route: '/shipments/shipments' },
    { label: 'Trips', icon: 'route', route: '/shipments/trips' },
    { label: 'Trip history', icon: 'history', route: '/shipments/history' },
    { label: 'Border clearance', icon: 'fact_check', route: '/shipments/clearances' },
  ],
};

/** @deprecated Use SHIPMENT_MANAGEMENT_NAV_ITEM */
export const SHIPMENTS_MANAGEMENT_NAV_ITEM = SHIPMENT_MANAGEMENT_NAV_ITEM;

const SHIPMENT_MANAGEMENT_NAV_CUSTOMER: NavItem = {
  ...SHIPMENT_MANAGEMENT_NAV_ITEM,
  children: [
    { label: 'Track shipments', icon: 'track_changes', route: '/shipments/shipments' },
    { label: 'Border clearance', icon: 'fact_check', route: '/shipments/clearances' },
  ],
};

const SHIPMENT_MANAGEMENT_NAV_CLEARING_AGENT: NavItem = {
  ...SHIPMENT_MANAGEMENT_NAV_ITEM,
  children: [
    { label: 'Border clearance', icon: 'fact_check', route: '/shipments/clearances' },
    { label: 'Shipments', icon: 'inventory_2', route: '/shipments/shipments' },
  ],
};

/** @deprecated Use SHIPMENT_MANAGEMENT_NAV_CUSTOMER */
export const SHIPMENTS_MANAGEMENT_NAV_CUSTOMER = SHIPMENT_MANAGEMENT_NAV_CUSTOMER;

/** @deprecated Use SHIPMENT_MANAGEMENT_NAV_CLEARING_AGENT */
export const SHIPMENTS_MANAGEMENT_NAV_CLEARING_AGENT = SHIPMENT_MANAGEMENT_NAV_CLEARING_AGENT;

/** Replaces flat shipment nav items with expandable submenu when not already present. */
export function withShipmentsNav(items: NavItem[]): NavItem[] {
  const shipmentsIndex = items.findIndex(
    (item) =>
      item.route === '/shipments' ||
      item.route === '/track-shipments' ||
      item.route === '/trips' ||
      item.route === '/active-clearances',
  );
  if (shipmentsIndex === -1) {
    return items;
  }

  const existing = items[shipmentsIndex];
  if (existing.route === SHIPMENT_MANAGEMENT_NAV_ITEM.route && existing.children?.length) {
    return items.map((item) =>
      item.route === SHIPMENT_MANAGEMENT_NAV_ITEM.route
        ? {
            ...item,
            label: SHIPMENT_MANAGEMENT_NAV_ITEM.label,
            icon: SHIPMENT_MANAGEMENT_NAV_ITEM.icon,
            children: item.children?.length ? [...item.children] : [...(SHIPMENT_MANAGEMENT_NAV_ITEM.children ?? [])],
          }
        : item,
    );
  }

  const defaultChildren =
    existing.route === '/active-clearances'
      ? SHIPMENT_MANAGEMENT_NAV_CLEARING_AGENT.children
      : existing.route === '/track-shipments'
        ? SHIPMENT_MANAGEMENT_NAV_CUSTOMER.children
        : SHIPMENT_MANAGEMENT_NAV_ITEM.children;

  const shipmentsItem: NavItem = {
    ...SHIPMENT_MANAGEMENT_NAV_ITEM,
    children: [...(defaultChildren ?? [])],
  };
  return [...items.slice(0, shipmentsIndex), shipmentsItem, ...items.slice(shipmentsIndex + 1)];
}

/** Replaces flat fleet nav item with expandable submenu when not already present. */
export function withFleetNav(items: NavItem[]): NavItem[] {
  if (items.some((item) => item.route === FLEET_NAV_ITEM.route && item.children?.length)) {
    return items.map((item) =>
      item.route === FLEET_NAV_ITEM.route && !item.children?.length ? FLEET_NAV_ITEM : item,
    );
  }
  const fleetIndex = items.findIndex((item) => item.route === '/fleet');
  if (fleetIndex === -1) {
    return items;
  }
  const existing = items[fleetIndex];
  const fleetItem: NavItem = {
    ...FLEET_NAV_ITEM,
    label: existing.label,
    children: [...(FLEET_NAV_ITEM.children ?? [])],
  };
  return [...items.slice(0, fleetIndex), fleetItem, ...items.slice(fleetIndex + 1)];
}

/** CRM trading partners — for standalone mode suppliers/customers. */
export const TRADING_PARTNERS_NAV_ITEM: NavItem = {
  label: 'Trading partners',
  route: '/trading-partners',
  icon: 'handshake',
};

/** Cross-docking logistics nav — shown when crossDockingEnabled && !inventoryManagementEnabled. */
export const CROSS_DOCKING_NAV_ITEM: NavItem = {
  label: 'Cross-dock logistics',
  route: '/products-inventory',
  icon: 'swap_horiz',
  children: [
    { label: 'Cross-dock integration API', icon: 'api', route: '/products-inventory/integration-api', queryParams: { mode: 'crossdock' } },
    { label: 'Cross-dock integration setup', icon: 'key', route: '/products-inventory/integration-setup', queryParams: { mode: 'crossdock' } },
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

/**
 * Applies operational mode filtering to nav items:
 * - Hides internal inventory nav when crossDocking is on and inventoryManagement is off.
 * - Injects CROSS_DOCKING_NAV_ITEM after Fleet when cross-docking is enabled.
 * - Injects TRADING_PARTNERS_NAV_ITEM and hides Customers when standalone mode is on.
 */
export function withOperationalModeNav(
  items: NavItem[],
  crossDockingEnabled: boolean,
  inventoryManagementEnabled: boolean,
  standaloneMode = false,
  counterpartyEngagementMode: 'RECORD_ONLY' | 'PLATFORM_ORG' = 'PLATFORM_ORG',
  inventoryDataSource: 'INTERNAL' | 'EXTERNAL_API' | 'MANUAL_ACK' = 'INTERNAL',
): NavItem[] {
  let result = [...items];
  const showErpIntegration = inventoryDataSource === 'EXTERNAL_API';

  const applyIntegrationFilter = (item: NavItem): NavItem => {
    if (!item.children?.length) {
      return item;
    }
    return {
      ...item,
      children: item.children.filter((child) => !child.integrationOnly || showErpIntegration),
    };
  };

  if (crossDockingEnabled && !inventoryManagementEnabled) {
    // Remove the full inventory nav item.
    result = result.filter((item) => item.route !== INVENTORY_NAV_ITEM.route);

    // Inject cross-dock nav after Fleet or at end if not already present.
    const alreadyPresent = result.some((item) => item.route === CROSS_DOCKING_NAV_ITEM.route);
    if (!alreadyPresent) {
      const fleetIndex = result.findIndex((item) => item.route === '/fleet');
      const insertAt = fleetIndex !== -1 ? fleetIndex + 1 : result.length;
      result = [
        ...result.slice(0, insertAt),
        applyIntegrationFilter({ ...CROSS_DOCKING_NAV_ITEM, children: [...(CROSS_DOCKING_NAV_ITEM.children ?? [])] }),
        ...result.slice(insertAt),
      ];
    }
  }

  const useTradingPartners = standaloneMode || counterpartyEngagementMode === 'RECORD_ONLY';

  if (useTradingPartners) {
    result = result.filter((item) => item.route !== '/customers');
    if (!result.some((item) => item.route === TRADING_PARTNERS_NAV_ITEM.route)) {
      const fleetIndex = result.findIndex((item) => item.route === '/fleet');
      const insertAt = fleetIndex !== -1 ? fleetIndex + 1 : result.length;
      result = [
        ...result.slice(0, insertAt),
        { ...TRADING_PARTNERS_NAV_ITEM },
        ...result.slice(insertAt),
      ];
    }
  } else {
    result = result.filter((item) => item.route !== TRADING_PARTNERS_NAV_ITEM.route);
  }

  return result.map((item) =>
    item.route === INVENTORY_NAV_ITEM.route || item.route === CROSS_DOCKING_NAV_ITEM.route
      ? applyIntegrationFilter(item)
      : item,
  );
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

/** Inserts organization management immediately after Dashboard when not already present. */
export function withOrganizationManagementNav(
  items: NavItem[],
  classification?: OrganizationClassification,
): NavItem[] {
  if (items.some((item) => item.route === ORGANIZATION_MANAGEMENT_NAV_ITEM.route)) {
    return items;
  }
  const navItem =
    classification === 'TRANSPORT_COMPANY' || classification === 'CLEARING_AGENT'
      ? ORGANIZATION_MANAGEMENT_NAV_TRANSPORT_ONLY
      : ORGANIZATION_MANAGEMENT_NAV_ITEM;
  const dashboardIndex = items.findIndex((item) => item.route === '/dashboard');
  if (dashboardIndex === -1) {
    return [navItem, ...items];
  }
  const insertAt = dashboardIndex + 1;
  return [...items.slice(0, insertAt), navItem, ...items.slice(insertAt)];
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
    {
      ...SHIPMENT_MANAGEMENT_NAV_ITEM,
      children: [...(SHIPMENT_MANAGEMENT_NAV_ITEM.children ?? [])],
    },
    { ...FLEET_NAV_ITEM, children: [...(FLEET_NAV_ITEM.children ?? [])] },
    { label: 'Customers', route: '/customers', icon: 'groups' },
    { label: 'Documents', route: '/documents', icon: 'folder_open' },
    { label: 'Billing', route: '/settings', icon: 'receipt_long', queryParams: { section: 'billing' } },
    { ...ANALYTICS_NAV_ITEM, children: [...(ANALYTICS_NAV_ITEM.children ?? [])] },
  ],
  CUSTOMER: [
    { label: 'Dashboard', route: '/dashboard', icon: 'dashboard' },
    {
      ...MY_ORDERS_NAV_ITEM,
      children: [...(MY_ORDERS_NAV_ITEM.children ?? [])],
    },
    {
      ...SHIPMENT_MANAGEMENT_NAV_CUSTOMER,
      children: [...(SHIPMENT_MANAGEMENT_NAV_CUSTOMER.children ?? [])],
    },
    { ...FLEET_NAV_ITEM, label: 'Fleet & Transport', children: [...(FLEET_NAV_ITEM.children ?? [])] },
    { label: 'Invoices', route: '/invoices', icon: 'request_quote' },
    { label: 'Documents', route: '/documents', icon: 'folder_open' },
    { label: 'Billing', route: '/settings', icon: 'receipt_long', queryParams: { section: 'billing' } },
    { ...ANALYTICS_NAV_ITEM, children: [...(ANALYTICS_NAV_ITEM.children ?? [])] },
  ],
  TRANSPORT_COMPANY: [
    { label: 'Dashboard', route: '/dashboard', icon: 'dashboard' },
    { ...FLEET_NAV_ITEM, children: [...(FLEET_NAV_ITEM.children ?? [])] },
    {
      ...SHIPMENT_MANAGEMENT_NAV_ITEM,
      children: [...(SHIPMENT_MANAGEMENT_NAV_ITEM.children ?? [])],
    },
    { label: 'Documents', route: '/documents', icon: 'folder_open' },
    { label: 'Billing', route: '/settings', icon: 'receipt_long', queryParams: { section: 'billing' } },
    { ...ANALYTICS_NAV_ITEM, children: [...(ANALYTICS_NAV_ITEM.children ?? [])] },
  ],
  CLEARING_AGENT: [
    { label: 'Dashboard', route: '/dashboard', icon: 'dashboard' },
    {
      ...SHIPMENT_MANAGEMENT_NAV_CLEARING_AGENT,
      children: [...(SHIPMENT_MANAGEMENT_NAV_CLEARING_AGENT.children ?? [])],
    },
    { label: 'Documents', route: '/documents', icon: 'folder_open' },
    { label: 'Billing', route: '/settings', icon: 'receipt_long', queryParams: { section: 'billing' } },
    { ...ANALYTICS_NAV_ITEM, children: [...(ANALYTICS_NAV_ITEM.children ?? [])] },
  ],
  SERVICE_STATION: [
    { label: 'Dashboard', route: '/dashboard', icon: 'dashboard' },
    { label: 'Truck Visits', route: '/truck-visits', icon: 'local_gas_station' },
    { label: 'Fuel Log', route: '/fuel-log', icon: 'oil_barrel' },
    { label: 'Documents', route: '/documents', icon: 'folder_open' },
    { label: 'Billing', route: '/settings', icon: 'receipt_long', queryParams: { section: 'billing' } },
    { ...ANALYTICS_NAV_ITEM, children: [...(ANALYTICS_NAV_ITEM.children ?? [])] },
  ],
  ROADSIDE_SUPPORT_SERVICE: [
    { label: 'Dashboard', route: '/dashboard', icon: 'dashboard' },
    { label: 'Incidents', route: '/incidents', icon: 'car_crash' },
    { label: 'Service Log', route: '/service-log', icon: 'build' },
    { label: 'Documents', route: '/documents', icon: 'folder_open' },
    { label: 'Billing', route: '/settings', icon: 'receipt_long', queryParams: { section: 'billing' } },
    { ...ANALYTICS_NAV_ITEM, children: [...(ANALYTICS_NAV_ITEM.children ?? [])] },
  ],
  GOVERNMENT_AGENCY: [
    { label: 'Dashboard', route: '/dashboard', icon: 'dashboard' },
    { label: 'Border Activity', route: '/border-activity', icon: 'border_outer' },
    { label: 'Documents', route: '/documents', icon: 'folder_open' },
    { label: 'Billing', route: '/settings', icon: 'receipt_long', queryParams: { section: 'billing' } },
    { ...ANALYTICS_NAV_ITEM, children: [...(ANALYTICS_NAV_ITEM.children ?? [])] },
  ],
};
