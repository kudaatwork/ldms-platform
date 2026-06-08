import type { FleetVehicleRow, FleetVehicleStatus, FleetVehicleType } from '../models/fleet.model';

const MAKES = [
  'Volvo FH16',
  'Scania R-Series',
  'Mercedes Actros',
  'MAN TGX',
  'Isuzu FTR',
  'Hino 700',
  'DAF XF',
  'Freightliner Cascadia',
] as const;

const REG_PREFIXES = ['ADX', 'AEX', 'AFG', 'AG', 'AEZ', 'AFL'] as const;

const DRIVERS = [
  'T. Moyo',
  'S. Ndlovu',
  'K. Chikwanha',
  'R. Dube',
  'P. Sibanda',
  'L. Mutasa',
] as const;

const TYPES: FleetVehicleType[] = ['rig', 'rig', 'tanker', 'flatbed', 'van', 'rig'];

const STATUS_WEIGHTS: FleetVehicleStatus[] = [
  'on_road',
  'on_road',
  'on_road',
  'available',
  'yard',
  'maintenance',
];

function hash(seed: string): number {
  let h = 0;
  for (let i = 0; i < seed.length; i += 1) {
    h = seed.charCodeAt(i) + ((h << 5) - h);
  }
  return Math.abs(h);
}

function pick<T>(arr: readonly T[], seed: number, offset: number): T {
  return arr[(seed + offset) % arr.length];
}

function statusLabel(status: FleetVehicleStatus): string {
  const map: Record<FleetVehicleStatus, string> = {
    on_road: 'On corridor',
    yard: 'At yard',
    maintenance: 'In workshop',
    available: 'Ready to dispatch',
  };
  return map[status];
}

function daysAgoLabel(seed: number): string {
  const days = (seed % 5) + 1;
  return days === 1 ? 'Yesterday' : `${days} days ago`;
}

/** Deterministic preview fleet until ldms-fleet-management is live. */
export function buildPreviewFleet(organizationId: number, organizationName: string): FleetVehicleRow[] {
  const base = hash(`${organizationId}:${organizationName}`);
  const count = 6 + (base % 5);
  const rows: FleetVehicleRow[] = [];

  for (let i = 0; i < count; i += 1) {
    const s = base + i * 17;
    const status = pick(STATUS_WEIGHTS, s, 3);
    const type = pick(TYPES, s, 1);
    const util =
      status === 'maintenance' ? 12 + (s % 18) : status === 'available' ? 8 + (s % 25) : 58 + (s % 38);
    const reg = `${pick(REG_PREFIXES, s, 0)} ${1000 + ((s * 37) % 8999)}`;
    rows.push({
      id: `preview-${organizationId}-${i}`,
      registration: reg,
      makeModel: pick(MAKES, s, 2),
      type,
      status,
      statusLabel: statusLabel(status),
      ownershipType: i % 4 === 0 ? 'contracted' : 'owned',
      ownershipLabel: i % 4 === 0 ? 'Contracted asset' : 'Own asset',
      utilizationPct: Math.min(98, util),
      lastTripLabel: daysAgoLabel(s),
      driverName: pick(DRIVERS, s, 4),
      accentHue: 168 + (s % 42),
    });
  }

  return rows;
}
