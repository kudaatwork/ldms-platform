import { CurrentUser, OrganizationClassification } from '../../../core/models/auth.model';

export type KpiCardTheme = 'ocean' | 'sunset' | 'forest' | 'violet' | 'ember' | 'slate' | 'mint' | 'rose';

export interface KpiCard {
  label: string;
  value: string;
  icon: string;
  trend: string;
  up: boolean;
  spark: number[];
  theme: KpiCardTheme;
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
    {
      label: 'Pending POs',
      value: '12',
      icon: 'shopping_cart',
      trend: '+3 this week',
      up: true,
      spark: [42, 58, 48, 72, 55, 68, 62, 78],
      theme: 'ocean',
    },
    {
      label: 'Active shipments',
      value: '27',
      icon: 'local_shipping',
      trend: '+8.4%',
      up: true,
      spark: [35, 52, 44, 68, 58, 82, 70, 92],
      theme: 'forest',
    },
    {
      label: 'Low stock alerts',
      value: '6',
      icon: 'inventory_2',
      trend: '−2 resolved',
      up: true,
      spark: [88, 72, 80, 55, 65, 48, 52, 38],
      theme: 'ember',
    },
    {
      label: 'Pending invoices',
      value: '9',
      icon: 'receipt_long',
      trend: '$24K due',
      up: false,
      spark: [50, 62, 58, 70, 64, 75, 68, 72],
      theme: 'violet',
    },
  ],
  CUSTOMER: [
    {
      label: 'Orders in transit',
      value: '18',
      icon: 'local_shipping',
      trend: '+2 today',
      up: true,
      spark: [40, 55, 48, 62, 58, 74, 68, 80],
      theme: 'ocean',
    },
    {
      label: 'Pending deliveries',
      value: '5',
      icon: 'pending_actions',
      trend: '3 due today',
      up: false,
      spark: [70, 58, 62, 50, 55, 42, 48, 40],
      theme: 'sunset',
    },
    {
      label: 'Unpaid invoices',
      value: '4',
      icon: 'payments',
      trend: '$8.2K',
      up: false,
      spark: [45, 52, 48, 55, 50, 58, 52, 48],
      theme: 'violet',
    },
    {
      label: 'Next delivery ETA',
      value: '2h 40m',
      icon: 'schedule',
      trend: 'On track',
      up: true,
      spark: [60, 62, 65, 68, 70, 72, 75, 78],
      theme: 'mint',
    },
  ],
  TRANSPORT_COMPANY: [
    {
      label: 'Trucks available',
      value: '44',
      icon: 'airport_shuttle',
      trend: '71% utilisation',
      up: true,
      spark: [55, 60, 58, 65, 62, 70, 68, 72],
      theme: 'ocean',
    },
    {
      label: 'Active trips',
      value: '31',
      icon: 'route',
      trend: '+5 vs yesterday',
      up: true,
      spark: [38, 48, 42, 58, 52, 68, 62, 75],
      theme: 'forest',
    },
    {
      label: 'Drivers on duty',
      value: '52',
      icon: 'groups',
      trend: 'Full roster',
      up: true,
      spark: [72, 74, 76, 78, 80, 82, 84, 86],
      theme: 'mint',
    },
    {
      label: 'Docs expiring',
      value: '3',
      icon: 'warning_amber',
      trend: 'Within 14 days',
      up: false,
      spark: [30, 35, 42, 38, 45, 40, 48, 44],
      theme: 'ember',
    },
  ],
  CLEARING_AGENT: [
    {
      label: 'Shipments at border',
      value: '15',
      icon: 'flag',
      trend: '+4 inbound',
      up: true,
      spark: [48, 55, 52, 60, 58, 65, 62, 70],
      theme: 'ocean',
    },
    {
      label: 'Clearances in progress',
      value: '11',
      icon: 'fact_check',
      trend: 'Avg 2.1h',
      up: true,
      spark: [55, 58, 62, 60, 65, 68, 64, 72],
      theme: 'violet',
    },
    {
      label: 'Completed today',
      value: '7',
      icon: 'task_alt',
      trend: '+28%',
      up: true,
      spark: [35, 42, 48, 55, 62, 68, 75, 82],
      theme: 'forest',
    },
  ],
  SERVICE_STATION: [
    {
      label: 'Trucks visited today',
      value: '63',
      icon: 'local_gas_station',
      trend: '+12 vs avg',
      up: true,
      spark: [45, 52, 58, 62, 68, 72, 78, 85],
      theme: 'sunset',
    },
    {
      label: 'Fuel dispensed',
      value: '12,480 L',
      icon: 'water_drop',
      trend: '+6.2%',
      up: true,
      spark: [50, 55, 58, 62, 65, 70, 74, 80],
      theme: 'ocean',
    },
    {
      label: 'Revenue today',
      value: '$18,450',
      icon: 'attach_money',
      trend: 'On target',
      up: true,
      spark: [58, 62, 60, 65, 68, 72, 70, 76],
      theme: 'forest',
    },
  ],
  ROADSIDE_SUPPORT_SERVICE: [
    {
      label: 'Incidents today',
      value: '14',
      icon: 'report',
      trend: 'Peak hours',
      up: false,
      spark: [40, 55, 72, 85, 78, 65, 52, 48],
      theme: 'ember',
    },
    {
      label: 'Active calls',
      value: '5',
      icon: 'support_agent',
      trend: '2 urgent',
      up: false,
      spark: [60, 65, 70, 68, 72, 75, 70, 68],
      theme: 'violet',
    },
    {
      label: 'Avg response time',
      value: '18 min',
      icon: 'timer',
      trend: '−3 min',
      up: true,
      spark: [85, 78, 72, 68, 62, 58, 52, 48],
      theme: 'mint',
    },
  ],
  GOVERNMENT_AGENCY: [
    {
      label: 'Trucks at border',
      value: '92',
      icon: 'local_shipping',
      trend: 'Live count',
      up: true,
      spark: [62, 68, 72, 75, 78, 82, 85, 88],
      theme: 'ocean',
    },
    {
      label: 'Clearances pending',
      value: '21',
      icon: 'hourglass_top',
      trend: '6 overdue',
      up: false,
      spark: [55, 58, 62, 65, 68, 72, 70, 74],
      theme: 'sunset',
    },
    {
      label: 'Compliance alerts',
      value: '8',
      icon: 'gpp_maybe',
      trend: '2 critical',
      up: false,
      spark: [42, 48, 52, 58, 55, 62, 58, 65],
      theme: 'ember',
    },
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
