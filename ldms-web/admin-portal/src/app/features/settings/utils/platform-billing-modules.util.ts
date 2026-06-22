export interface PlatformBillingModuleMeta {
  category: string;
  label: string;
  description: string;
  icon: string;
}

/** Modules whose per-action charges admins configure for prepaid wallet deductions. */
export const PLATFORM_BILLING_MODULES: PlatformBillingModuleMeta[] = [
  {
    category: 'TRIPS',
    label: 'Trips & tracking',
    description: 'Trip creation, GPS pings, and completion',
    icon: 'local_shipping',
  },
  {
    category: 'NOTIFICATIONS',
    label: 'Notifications',
    description: 'Email, SMS, and push messages',
    icon: 'notifications',
  },
  {
    category: 'SUPPORT',
    label: 'Help & support',
    description: 'LDMS Assistant bot messages vs live agent ticket chat',
    icon: 'support_agent',
  },
  {
    category: 'LOGISTICS',
    label: 'Shipments',
    description: 'Shipment status updates',
    icon: 'inventory_2',
  },
  {
    category: 'ORDERS',
    label: 'Orders & procurement',
    description: 'Orders and procurement approval stages',
    icon: 'shopping_cart',
  },
  {
    category: 'DOCUMENTS',
    label: 'Documents',
    description: 'Uploads and sharing',
    icon: 'folder',
  },
  {
    category: 'BILLING',
    label: 'Billing documents',
    description: 'Invoice generation',
    icon: 'receipt_long',
  },
  {
    category: 'IOT',
    label: 'GPS & IoT',
    description: 'Telemetry and location pings',
    icon: 'gps_fixed',
  },
  {
    category: 'PLATFORM',
    label: 'Platform audit',
    description: 'Audit trail writes',
    icon: 'policy',
  },
  {
    category: 'PROCUREMENT',
    label: 'Procurement',
    description: 'Procurement-specific actions',
    icon: 'approval',
  },
  {
    category: 'GENERAL',
    label: 'General',
    description: 'Other platform actions',
    icon: 'apps',
  },
];

export function moduleLabel(category: string): string {
  if (category === 'PROCUREMENT') {
    return 'Orders & procurement';
  }
  const match = PLATFORM_BILLING_MODULES.find((m) => m.category === category);
  if (match) {
    return match.label;
  }
  return category.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
}
