import { OrganizationClassification } from '../../core/models/auth.model';

export interface NavItem {
  label: string;
  route: string;
}

export const NAV_CONFIG: Record<OrganizationClassification, NavItem[]> = {
  SUPPLIER: [
    { label: 'Dashboard', route: '/dashboard' },
    { label: 'Products/Inventory', route: '/products-inventory' },
    { label: 'Purchase Orders', route: '/purchase-orders' },
    { label: 'Shipments', route: '/shipments' },
    { label: 'Fleet', route: '/fleet' },
    { label: 'Customers', route: '/customers' },
    { label: 'Documents', route: '/documents' },
    { label: 'Billing', route: '/billing' },
    { label: 'Reports', route: '/reports' },
  ],
  CUSTOMER: [
    { label: 'Dashboard', route: '/dashboard' },
    { label: 'My Orders', route: '/my-orders' },
    { label: 'Track Shipments', route: '/track-shipments' },
    { label: 'Deliveries', route: '/deliveries' },
    { label: 'Invoices', route: '/invoices' },
    { label: 'Documents', route: '/documents' },
    { label: 'Reports', route: '/reports' },
  ],
  TRANSPORT_COMPANY: [
    { label: 'Dashboard', route: '/dashboard' },
    { label: 'Fleet', route: '/fleet' },
    { label: 'Drivers', route: '/drivers' },
    { label: 'Trips', route: '/trips' },
    { label: 'Documents', route: '/documents' },
    { label: 'Billing', route: '/billing' },
    { label: 'Reports', route: '/reports' },
  ],
  CLEARING_AGENT: [
    { label: 'Dashboard', route: '/dashboard' },
    { label: 'Active Clearances', route: '/active-clearances' },
    { label: 'Documents', route: '/documents' },
    { label: 'Billing', route: '/billing' },
    { label: 'Reports', route: '/reports' },
  ],
  SERVICE_STATION: [
    { label: 'Dashboard', route: '/dashboard' },
    { label: 'Truck Visits', route: '/truck-visits' },
    { label: 'Fuel Log', route: '/fuel-log' },
    { label: 'Documents', route: '/documents' },
    { label: 'Reports', route: '/reports' },
  ],
  ROADSIDE_SUPPORT_SERVICE: [
    { label: 'Dashboard', route: '/dashboard' },
    { label: 'Incidents', route: '/incidents' },
    { label: 'Service Log', route: '/service-log' },
    { label: 'Documents', route: '/documents' },
    { label: 'Reports', route: '/reports' },
  ],
  GOVERNMENT_AGENCY: [
    { label: 'Dashboard', route: '/dashboard' },
    { label: 'Border Activity', route: '/border-activity' },
    { label: 'Documents', route: '/documents' },
    { label: 'Reports', route: '/reports' },
  ],
};
