/** Rich seed data for cross-tenant platform operations until backoffice shipment/trip APIs ship. */

export type ShipmentLifecycleStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'APPROVED'
  | 'IN_TRANSIT'
  | 'AT_BORDER'
  | 'DELIVERED'
  | 'CANCELLED';

export interface PlatformCompanyOps {
  organizationId: number;
  organizationName: string;
  classification: string;
  classificationLabel: string;
  activeShipments: number;
  completedShipments: number;
  activeTrips: number;
  onTimePct: number;
  revenueCents: number;
  walletBalanceCents: number;
  lastActivityAt: string;
  accent: string;
}

export interface PlatformShipmentOps {
  shipmentId: number;
  shipmentRef: string;
  organizationId: number;
  organizationName: string;
  vehicleReg: string;
  driverName: string;
  origin: string;
  destination: string;
  status: ShipmentLifecycleStatus;
  statusLabel: string;
  progressPct: number;
  eta: string;
  startedAt?: string;
  lat: number;
  lng: number;
  speedKph: number;
  cargoSummary: string;
  customerName: string;
}

export interface PlatformOpsSummary {
  totalOrganizations: number;
  organizationsWithActivity: number;
  activeShipments: number;
  activeTrips: number;
  completedThisMonth: number;
  onTimePct: number;
  platformRevenueCents: number;
  pendingInvoicesCents: number;
  shipmentsByStatus: Array<{ status: string; label: string; count: number; color: string }>;
  weeklyVolume: number[];
  companies: PlatformCompanyOps[];
  liveShipments: PlatformShipmentOps[];
}

export interface BillingCostLine {
  id: string;
  label: string;
  category: string;
  amountCents: number;
  organizationName: string;
  occurredAt: string;
}

export interface PlatformRevenueReport {
  totalEarnedCents: number;
  subscriptionCents: number;
  actionChargesCents: number;
  walletDepositsCents: number;
  monthLabels: string[];
  earnedSeries: number[];
  costSeries: number[];
  byOrganization: Array<{
    organizationId: number;
    organizationName: string;
    earnedCents: number;
    costsCents: number;
    netCents: number;
    accent: string;
  }>;
  costBreakdown: Array<{ category: string; amountCents: number; color: string }>;
  recentCharges: BillingCostLine[];
}

const ACCENTS = ['#6366f1', '#0ea5e9', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6', '#14b8a6', '#f97316'];

const COMPANY_SEEDS: Omit<PlatformCompanyOps, 'organizationId'>[] = [
  {
    organizationName: 'ZimChem Logistics',
    classification: 'SUPPLIER',
    classificationLabel: 'Supplier',
    activeShipments: 12,
    completedShipments: 148,
    activeTrips: 9,
    onTimePct: 94.2,
    revenueCents: 284_500_00,
    walletBalanceCents: 42_000_00,
    lastActivityAt: '2026-06-17T09:42:00',
    accent: ACCENTS[0],
  },
  {
    organizationName: 'Bulawayo Retail Group',
    classification: 'CUSTOMER',
    classificationLabel: 'Customer',
    activeShipments: 6,
    completedShipments: 89,
    activeTrips: 4,
    onTimePct: 91.8,
    revenueCents: 156_200_00,
    walletBalanceCents: 18_500_00,
    lastActivityAt: '2026-06-17T08:15:00',
    accent: ACCENTS[1],
  },
  {
    organizationName: 'CrossBorder Hauliers',
    classification: 'TRANSPORT_COMPANY',
    classificationLabel: 'Transporter',
    activeShipments: 18,
    completedShipments: 312,
    activeTrips: 14,
    onTimePct: 96.5,
    revenueCents: 512_800_00,
    walletBalanceCents: 95_000_00,
    lastActivityAt: '2026-06-17T10:01:00',
    accent: ACCENTS[2],
  },
  {
    organizationName: 'Mutare Agro Supplies',
    classification: 'SUPPLIER',
    classificationLabel: 'Supplier',
    activeShipments: 4,
    completedShipments: 67,
    activeTrips: 3,
    onTimePct: 88.4,
    revenueCents: 98_400_00,
    walletBalanceCents: 12_300_00,
    lastActivityAt: '2026-06-16T16:30:00',
    accent: ACCENTS[3],
  },
  {
    organizationName: 'Beitbridge Clearing Co',
    classification: 'CLEARING_AGENT',
    classificationLabel: 'Clearing agent',
    activeShipments: 2,
    completedShipments: 41,
    activeTrips: 1,
    onTimePct: 97.1,
    revenueCents: 67_900_00,
    walletBalanceCents: 8_200_00,
    lastActivityAt: '2026-06-17T07:55:00',
    accent: ACCENTS[4],
  },
  {
    organizationName: 'Harare Fuel Network',
    classification: 'SERVICE_STATION',
    classificationLabel: 'Service station',
    activeShipments: 0,
    completedShipments: 23,
    activeTrips: 0,
    onTimePct: 100,
    revenueCents: 34_100_00,
    walletBalanceCents: 5_600_00,
    lastActivityAt: '2026-06-15T11:20:00',
    accent: ACCENTS[5],
  },
];

const ROUTES: Array<{ from: string; to: string; lat: number; lng: number }> = [
  { from: 'Harare', to: 'Bulawayo', lat: -19.45, lng: 29.82 },
  { from: 'Mutare', to: 'Beitbridge', lat: -21.05, lng: 30.45 },
  { from: 'Gweru', to: 'Harare', lat: -18.95, lng: 30.92 },
  { from: 'Masvingo', to: 'Bulawayo', lat: -20.07, lng: 30.83 },
  { from: 'Harare', to: 'Chirundu', lat: -17.35, lng: 30.98 },
  { from: 'Bulawayo', to: 'Victoria Falls', lat: -19.85, lng: 27.45 },
  { from: 'Kwekwe', to: 'Mutare', lat: -18.97, lng: 32.67 },
  { from: 'Harare', to: 'Plumtree', lat: -20.48, lng: 28.22 },
];

const STATUS_META: Record<
  ShipmentLifecycleStatus,
  { label: string; progress: number; color: string }
> = {
  DRAFT: { label: 'Draft', progress: 5, color: '#94a3b8' },
  SUBMITTED: { label: 'Submitted', progress: 15, color: '#60a5fa' },
  APPROVED: { label: 'Approved', progress: 28, color: '#818cf8' },
  IN_TRANSIT: { label: 'In transit', progress: 62, color: '#22c55e' },
  AT_BORDER: { label: 'At border', progress: 78, color: '#f59e0b' },
  DELIVERED: { label: 'Delivered', progress: 100, color: '#10b981' },
  CANCELLED: { label: 'Cancelled', progress: 0, color: '#ef4444' },
};

function buildShipments(companies: PlatformCompanyOps[]): PlatformShipmentOps[] {
  const statuses: ShipmentLifecycleStatus[] = [
    'IN_TRANSIT',
    'IN_TRANSIT',
    'APPROVED',
    'AT_BORDER',
    'IN_TRANSIT',
    'SUBMITTED',
    'IN_TRANSIT',
    'DELIVERED',
  ];
  const rows: PlatformShipmentOps[] = [];
  let id = 1001;
  companies.forEach((co, ci) => {
    const count = Math.max(co.activeShipments, 1);
    for (let i = 0; i < Math.min(count, 3); i++) {
      const route = ROUTES[(ci + i) % ROUTES.length];
      const status = statuses[(ci + i) % statuses.length];
      const meta = STATUS_META[status];
      rows.push({
        shipmentId: id++,
        shipmentRef: `SHP-${2026}${String(id).padStart(4, '0')}`,
        organizationId: co.organizationId,
        organizationName: co.organizationName,
        vehicleReg: `ZW-${1200 + ci * 111 + i}-A`,
        driverName: ['T. Moyo', 'S. Ncube', 'P. Dube', 'L. Chikwanha'][i % 4],
        origin: route.from,
        destination: route.to,
        status,
        statusLabel: meta.label,
        progressPct: meta.progress + (i * 7) % 20,
        eta: `${14 + i}:${(ci * 7) % 60}`.replace(/^(\d+):(\d)$/, '$1:0$2'),
        startedAt: '2026-06-17T06:00:00',
        lat: route.lat + (Math.random() - 0.5) * 0.4,
        lng: route.lng + (Math.random() - 0.5) * 0.4,
        speedKph: 62 + (ci * 3 + i * 5) % 28,
        cargoSummary: ['Fertilizer 24t', 'Maize seed 18t', 'Chemical drums', 'Retail pallets'][i % 4],
        customerName: ['AgriCorp', 'FarmLink', 'RetailMax', 'BorderTrade'][ci % 4],
      });
    }
  });
  return rows;
}

export function buildPlatformOpsSummary(orgNames: Array<{ id: number; name: string }> = []): PlatformOpsSummary {
  const companies: PlatformCompanyOps[] = COMPANY_SEEDS.map((seed, i) => {
    const org = orgNames[i];
    return {
      ...seed,
      organizationId: org?.id ?? 100 + i,
      organizationName: org?.name ?? seed.organizationName,
      accent: ACCENTS[i % ACCENTS.length],
    };
  });

  const liveShipments = buildShipments(companies);
  const activeShipments = companies.reduce((a, c) => a + c.activeShipments, 0);
  const activeTrips = companies.reduce((a, c) => a + c.activeTrips, 0);
  const completedThisMonth = companies.reduce((a, c) => a + Math.floor(c.completedShipments * 0.12), 0);
  const platformRevenueCents = companies.reduce((a, c) => a + c.revenueCents, 0);

  return {
    totalOrganizations: Math.max(orgNames.length, companies.length) + 24,
    organizationsWithActivity: companies.filter((c) => c.activeShipments > 0).length,
    activeShipments,
    activeTrips,
    completedThisMonth,
    onTimePct: 94.6,
    platformRevenueCents,
    pendingInvoicesCents: 84_200_00,
    shipmentsByStatus: [
      { status: 'IN_TRANSIT', label: 'In transit', count: 28, color: '#22c55e' },
      { status: 'APPROVED', label: 'Approved', count: 14, color: '#818cf8' },
      { status: 'AT_BORDER', label: 'At border', count: 6, color: '#f59e0b' },
      { status: 'SUBMITTED', label: 'Submitted', count: 9, color: '#60a5fa' },
      { status: 'DELIVERED', label: 'Delivered (MTD)', count: completedThisMonth, color: '#10b981' },
    ],
    weeklyVolume: [42, 58, 51, 67, 73, 61, 78],
    companies,
    liveShipments,
  };
}

export function buildPlatformRevenueReport(companies: PlatformCompanyOps[]): PlatformRevenueReport {
  const subscriptionCents = Math.round(companies.reduce((a, c) => a + c.revenueCents, 0) * 0.42);
  const actionChargesCents = Math.round(companies.reduce((a, c) => a + c.revenueCents, 0) * 0.38);
  const walletDepositsCents = Math.round(companies.reduce((a, c) => a + c.walletBalanceCents, 0) * 1.2);
  const totalEarnedCents = subscriptionCents + actionChargesCents + walletDepositsCents * 0.15;

  return {
    totalEarnedCents,
    subscriptionCents,
    actionChargesCents,
    walletDepositsCents,
    monthLabels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
    earnedSeries: [82000, 94000, 88000, 112000, 128000, 154000].map((v) => v * 100),
    costSeries: [12000, 14000, 11000, 18000, 16000, 19000].map((v) => v * 100),
    byOrganization: companies.map((c) => {
      const earned = c.revenueCents;
      const costs = Math.round(earned * 0.22);
      return {
        organizationId: c.organizationId,
        organizationName: c.organizationName,
        earnedCents: earned,
        costsCents: costs,
        netCents: earned - costs,
        accent: c.accent,
      };
    }),
    costBreakdown: [
      { category: 'SMS & notifications', amountCents: 18_400_00, color: '#6366f1' },
      { category: 'Document storage', amountCents: 12_800_00, color: '#0ea5e9' },
      { category: 'Trip telemetry', amountCents: 24_600_00, color: '#10b981' },
      { category: 'Payment processing', amountCents: 9_200_00, color: '#f59e0b' },
      { category: 'Support & onboarding', amountCents: 6_500_00, color: '#ec4899' },
    ],
    recentCharges: companies.slice(0, 8).flatMap((c, i) => [
      {
        id: `chg-${c.organizationId}-sub`,
        label: 'Monthly subscription',
        category: 'Subscription',
        amountCents: Math.round(c.revenueCents * 0.35),
        organizationName: c.organizationName,
        occurredAt: `2026-06-${10 + i}T08:00:00`,
      },
      {
        id: `chg-${c.organizationId}-ship`,
        label: 'Shipment dispatch charge',
        category: 'Logistics',
        amountCents: Math.round(c.revenueCents * 0.08),
        organizationName: c.organizationName,
        occurredAt: `2026-06-${12 + i}T14:22:00`,
      },
    ]),
  };
}

export function findShipmentById(
  summary: PlatformOpsSummary,
  shipmentId: number,
): PlatformShipmentOps | undefined {
  return summary.liveShipments.find((s) => s.shipmentId === shipmentId);
}

export function shipmentsForCompany(summary: PlatformOpsSummary, organizationId: number): PlatformShipmentOps[] {
  return summary.liveShipments.filter((s) => s.organizationId === organizationId);
}
