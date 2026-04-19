import { OrganizationClassification } from '../../core/models/auth.model';

export interface NavItem {
  label: string;
  route: string;
  /** Material Symbols name */
  icon: string;
}

export const NAV_CONFIG: Record<OrganizationClassification, NavItem[]> = {
  SUPPLIER: [
    { label: 'Dashboard', route: '/dashboard', icon: 'dashboard' },
    { label: 'Products/Inventory', route: '/products-inventory', icon: 'inventory_2' },
    { label: 'Purchase Orders', route: '/purchase-orders', icon: 'shopping_cart' },
    { label: 'Shipments', route: '/shipments', icon: 'local_shipping' },
    { label: 'Fleet', route: '/fleet', icon: 'directions_car' },
    { label: 'Customers', route: '/customers', icon: 'groups' },
    { label: 'Documents', route: '/documents', icon: 'folder_open' },
    { label: 'Billing', route: '/billing', icon: 'receipt_long' },
    { label: 'Reports', route: '/reports', icon: 'analytics' },
  ],
  CUSTOMER: [
    { label: 'Dashboard', route: '/dashboard', icon: 'dashboard' },
    { label: 'My Orders', route: '/my-orders', icon: 'receipt_long' },
    { label: 'Track Shipments', route: '/track-shipments', icon: 'track_changes' },
    { label: 'Deliveries', route: '/deliveries', icon: 'delivery_dining' },
    { label: 'Invoices', route: '/invoices', icon: 'request_quote' },
    { label: 'Documents', route: '/documents', icon: 'folder_open' },
    { label: 'Reports', route: '/reports', icon: 'analytics' },
  ],
  TRANSPORT_COMPANY: [
    { label: 'Dashboard', route: '/dashboard', icon: 'dashboard' },
    { label: 'Fleet', route: '/fleet', icon: 'local_shipping' },
    { label: 'Drivers', route: '/drivers', icon: 'badge' },
    { label: 'Trips', route: '/trips', icon: 'route' },
    { label: 'Documents', route: '/documents', icon: 'folder_open' },
    { label: 'Billing', route: '/billing', icon: 'receipt_long' },
    { label: 'Reports', route: '/reports', icon: 'analytics' },
  ],
  CLEARING_AGENT: [
    { label: 'Dashboard', route: '/dashboard', icon: 'dashboard' },
    { label: 'Active Clearances', route: '/active-clearances', icon: 'fact_check' },
    { label: 'Documents', route: '/documents', icon: 'folder_open' },
    { label: 'Billing', route: '/billing', icon: 'receipt_long' },
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
