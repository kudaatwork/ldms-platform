import { CurrentUser, OrganizationClassification } from '../../../core/models/auth.model';

export interface KpiCard {
  label: string;
  value: string;
}

export const PLATFORM_KPI_CONFIG: Record<OrganizationClassification, KpiCard[]> = {
  SUPPLIER: [
    { label: 'pending POs', value: '12' },
    { label: 'active shipments', value: '27' },
    { label: 'low stock alerts', value: '6' },
    { label: 'pending invoices', value: '9' },
  ],
  CUSTOMER: [
    { label: 'orders in transit', value: '18' },
    { label: 'pending deliveries', value: '5' },
    { label: 'unpaid invoices', value: '4' },
    { label: 'next delivery ETA', value: '2h 40m' },
  ],
  TRANSPORT_COMPANY: [
    { label: 'trucks available', value: '44' },
    { label: 'active trips', value: '31' },
    { label: 'drivers on duty', value: '52' },
    { label: 'docs expiring', value: '3' },
  ],
  CLEARING_AGENT: [
    { label: 'shipments at my border', value: '15' },
    { label: 'clearances in progress', value: '11' },
    { label: 'completed today', value: '7' },
  ],
  SERVICE_STATION: [
    { label: 'trucks visited today', value: '63' },
    { label: 'fuel dispensed', value: '12,480 L' },
    { label: 'revenue today', value: '$18,450' },
  ],
  ROADSIDE_SUPPORT_SERVICE: [
    { label: 'incidents today', value: '14' },
    { label: 'active calls', value: '5' },
    { label: 'avg response time', value: '18 min' },
  ],
  GOVERNMENT_AGENCY: [
    { label: 'trucks at border', value: '92' },
    { label: 'clearances pending', value: '21' },
    { label: 'compliance alerts', value: '8' },
  ],
};

export const MOCK_USERS: Array<{ email: string; password: string; user: CurrentUser }> = [
  {
    email: 'supplier@projectlx.co.zw',
    password: 'Password123!',
    user: {
      userId: 'u-1001',
      organizationId: 'org-01',
      orgName: 'Supplier One',
      orgClassification: 'SUPPLIER',
      roles: ['SUPPLIER_ADMIN'],
      email: 'supplier@projectlx.co.zw',
    },
  },
  {
    email: 'customer@projectlx.co.zw',
    password: 'Password123!',
    user: {
      userId: 'u-1002',
      organizationId: 'org-02',
      orgName: 'Customer One',
      orgClassification: 'CUSTOMER',
      roles: ['CUSTOMER_ADMIN'],
      email: 'customer@projectlx.co.zw',
    },
  },
  {
    email: 'transport@projectlx.co.zw',
    password: 'Password123!',
    user: {
      userId: 'u-1003',
      organizationId: 'org-03',
      orgName: 'Transport Co',
      orgClassification: 'TRANSPORT_COMPANY',
      roles: ['TRANSPORT_MANAGER'],
      email: 'transport@projectlx.co.zw',
    },
  },
  {
    email: 'clearing@projectlx.co.zw',
    password: 'Password123!',
    user: {
      userId: 'u-1004',
      organizationId: 'org-04',
      orgName: 'Clearing Agent',
      orgClassification: 'CLEARING_AGENT',
      roles: ['CLEARANCE_OFFICER'],
      email: 'clearing@projectlx.co.zw',
    },
  },
  {
    email: 'station@projectlx.co.zw',
    password: 'Password123!',
    user: {
      userId: 'u-1005',
      organizationId: 'org-05',
      orgName: 'Service Station',
      orgClassification: 'SERVICE_STATION',
      roles: ['STATION_MANAGER'],
      email: 'station@projectlx.co.zw',
    },
  },
  {
    email: 'roadside@projectlx.co.zw',
    password: 'Password123!',
    user: {
      userId: 'u-1006',
      organizationId: 'org-06',
      orgName: 'Roadside Assist',
      orgClassification: 'ROADSIDE_SUPPORT_SERVICE',
      roles: ['SUPPORT_AGENT'],
      email: 'roadside@projectlx.co.zw',
    },
  },
  {
    email: 'gov@projectlx.co.zw',
    password: 'Password123!',
    user: {
      userId: 'u-1007',
      organizationId: 'org-07',
      orgName: 'Government Agency',
      orgClassification: 'GOVERNMENT_AGENCY',
      roles: ['BORDER_OFFICER'],
      email: 'gov@projectlx.co.zw',
    },
  },
];
