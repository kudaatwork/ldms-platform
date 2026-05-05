import { CurrentUser, OrganizationClassification } from '../../../core/models/auth.model';

export interface KpiCard {
  label: string;
  value: string;
}

export type SupplierShipmentStatus = 'PREPARED' | 'IN_TRANSIT' | 'COMPLETED' | 'FAILED';

export interface SupplierShipmentCard {
  id: string;
  shipmentNo: string;
  status: SupplierShipmentStatus;
  departureLabel: string;
  arrivalLabel: string;
  departureDate: string;
  arrivalDate: string;
  category: string;
  driver: string;
  /** Short hint shown on the map panel */
  routeHint: string;
}

export const SUPPLIER_SHIPMENT_MOCKS: SupplierShipmentCard[] = [
  {
    id: 's1',
    shipmentNo: 'LX-2017287528',
    status: 'IN_TRANSIT',
    departureLabel: 'Harare DC',
    arrivalLabel: 'Bulawayo Hub',
    departureDate: '30 Apr',
    arrivalDate: '2 May',
    category: 'Food & beverages',
    driver: 'T. Moyo',
    routeHint: 'A6 · 120 km to next waypoint',
  },
  {
    id: 's2',
    shipmentNo: 'LX-2017289102',
    status: 'PREPARED',
    departureLabel: 'Supplier DC',
    arrivalLabel: 'Retail chain',
    departureDate: '2 May',
    arrivalDate: '3 May',
    category: 'Packaged goods',
    driver: 'Unassigned',
    routeHint: 'Ready for dispatch',
  },
  {
    id: 's3',
    shipmentNo: 'LX-2017286640',
    status: 'COMPLETED',
    departureLabel: 'Mutare',
    arrivalLabel: 'Chipinge',
    departureDate: '28 Apr',
    arrivalDate: '29 Apr',
    category: 'Agricultural',
    driver: 'R. Ndlovu',
    routeHint: 'Delivered · POD signed',
  },
  {
    id: 's4',
    shipmentNo: 'LX-2017288201',
    status: 'FAILED',
    departureLabel: 'Harare',
    arrivalLabel: 'Chirundu',
    departureDate: '1 May',
    arrivalDate: '—',
    category: 'Cold chain',
    driver: 'J. Sibanda',
    routeHint: 'Border delay · contact clearing',
  },
  {
    id: 's5',
    shipmentNo: 'LX-2017289011',
    status: 'IN_TRANSIT',
    departureLabel: 'Beitbridge',
    arrivalLabel: 'Johannesburg',
    departureDate: '2 May',
    arrivalDate: '3 May',
    category: 'Textiles',
    driver: 'P. van der Merwe',
    routeHint: 'N1 · customs cleared',
  },
  {
    id: 's6',
    shipmentNo: 'LX-2017287555',
    status: 'PREPARED',
    departureLabel: 'Warehouse 3',
    arrivalLabel: 'Government depot',
    departureDate: '3 May',
    arrivalDate: '4 May',
    category: 'Medical supplies',
    driver: 'Unassigned',
    routeHint: 'Awaiting vehicle allocation',
  },
];

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
