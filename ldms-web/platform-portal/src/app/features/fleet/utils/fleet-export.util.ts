import type { FleetDriverRow, FleetVehicleRow, FleetVehicleStatus, FleetVehicleType, TransporterPartnerRow } from '../models/fleet.model';
import type { LxExportColumn } from '../../../shared/utils/lx-export.util';
import { downloadBlob } from '../../../shared/utils/lx-export.util';

export const FLEET_VEHICLE_EXPORT_COLUMNS: LxExportColumn<FleetVehicleRow>[] = [
  { header: 'REGISTRATION', value: (r) => r.registration },
  { header: 'MAKE_MODEL', value: (r) => r.makeModel },
  { header: 'ASSET_TYPE', value: (r) => r.type },
  { header: 'STATUS', value: (r) => r.status },
  { header: 'OWNERSHIP', value: (r) => r.ownershipType },
  { header: 'TRANSPORTER', value: (r) => r.contractedTransporterOrganizationName ?? '' },
  { header: 'DRIVER', value: (r) => r.driverName },
  { header: 'UTILIZATION_PCT', value: (r) => r.utilizationPct },
  { header: 'LAST_MOVEMENT', value: (r) => r.lastTripLabel },
];

export const TRANSPORTER_EXPORT_COLUMNS: LxExportColumn<TransporterPartnerRow>[] = [
  { header: 'ORGANISATION_NAME', value: (r) => r.name },
  { header: 'EMAIL', value: (r) => r.email },
  { header: 'PHONE', value: (r) => r.phoneNumber },
  { header: 'VERIFIED', value: (r) => (r.verified ? 'true' : 'false') },
  { header: 'KYC_STATUS', value: (r) => r.kycStatusLabel },
  { header: 'CONTRACT_STATUS', value: (r) => r.contractStatus },
  { header: 'CONTRACT_START', value: (r) => r.contractStartDate ?? r.contractStartLabel },
  { header: 'CONTRACT_END', value: (r) => r.contractEndDate ?? r.contractEndLabel },
  { header: 'LINKED_SINCE', value: (r) => r.linkedSinceLabel },
];

export const DRIVER_EXPORT_COLUMNS: LxExportColumn<FleetDriverRow>[] = [
  { header: 'FIRST_NAME', value: (r) => r.firstName },
  { header: 'LAST_NAME', value: (r) => r.lastName },
  { header: 'PHONE', value: (r) => r.phoneNumber },
  { header: 'LICENSE_NUMBER', value: (r) => r.licenseNumber },
  { header: 'LICENSE_CLASS', value: (r) => r.licenseClass },
  { header: 'NATIONAL_ID', value: (r) => r.nationalIdNumber ?? '' },
  { header: 'PASSPORT', value: (r) => r.passportNumber ?? '' },
  { header: 'ADDRESS_CITY', value: (r) => r.addressCity ?? '' },
  { header: 'ADDRESS_PROVINCE', value: (r) => r.addressProvince ?? '' },
  { header: 'ADDRESS_COUNTRY', value: (r) => r.addressCountry ?? '' },
  { header: 'USER_LINKED', value: (r) => (r.userId ? 'true' : 'false') },
];

export const FLEET_VEHICLE_SAMPLE_CSV = `REGISTRATION,MAKE_MODEL,ASSET_TYPE,STATUS,OWNERSHIP,DRIVER_NAME,UTILIZATION_PCT
AHC5564,Volvo FH16,rig,available,owned,,0
HRE1234,Isuzu NQR,van,yard,owned,Tinashe Moyo,45
`;

export const TRANSPORTER_SAMPLE_CSV = `ORGANISATION_NAME,EMAIL,PHONE,CONTRACT_START,CONTRACT_END,CONTACT_FIRST_NAME,CONTACT_LAST_NAME,CONTACT_EMAIL,CONTACT_PHONE
Example Hauliers Pvt Ltd,ops@examplehauliers.co.zw,+263771234567,2026-01-01,2027-12-31,Tendai,Moyo,tendai.moyo@examplehauliers.co.zw,+263779876543
`;

export const DRIVER_SAMPLE_CSV = `FIRST_NAME,LAST_NAME,PHONE,LICENSE_NUMBER,LICENSE_CLASS
Themba,Gorimbo,+263776336778,AA66772930,2
`;

export interface FleetVehicleImportRow {
  registration: string;
  makeModel: string;
  type: FleetVehicleType;
  status: FleetVehicleStatus;
  ownershipType: 'owned' | 'contracted';
  driverName?: string;
  utilizationPct?: number;
}

export interface FleetDriverImportRow {
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  licenseNumber?: string;
  licenseClass?: string;
}

export function downloadFleetSampleCsv(kind: 'vehicles' | 'transporters' | 'drivers'): void {
  const content =
    kind === 'vehicles' ? FLEET_VEHICLE_SAMPLE_CSV : kind === 'drivers' ? DRIVER_SAMPLE_CSV : TRANSPORTER_SAMPLE_CSV;
  const filename =
    kind === 'vehicles' ? 'fleet-vehicles-sample.csv' : kind === 'drivers' ? 'fleet-drivers-sample.csv' : 'transporters-sample.csv';
  downloadBlob(new Blob([content], { type: 'text/csv;charset=utf-8' }), filename);
}

/** Minimal RFC4180-style CSV parser (single header row). */
export function parseCsv(text: string): string[][] {
  const rows: string[][] = [];
  let row: string[] = [];
  let cell = '';
  let inQuotes = false;

  for (let i = 0; i < text.length; i++) {
    const ch = text[i];
    const next = text[i + 1];
    if (inQuotes) {
      if (ch === '"' && next === '"') {
        cell += '"';
        i++;
      } else if (ch === '"') {
        inQuotes = false;
      } else {
        cell += ch;
      }
      continue;
    }
    if (ch === '"') {
      inQuotes = true;
    } else if (ch === ',') {
      row.push(cell);
      cell = '';
    } else if (ch === '\n') {
      row.push(cell);
      cell = '';
      if (row.some((c) => c.trim().length > 0)) {
        rows.push(row);
      }
      row = [];
    } else if (ch !== '\r') {
      cell += ch;
    }
  }
  row.push(cell);
  if (row.some((c) => c.trim().length > 0)) {
    rows.push(row);
  }
  return rows;
}

function normalizeVehicleType(raw: string): FleetVehicleType | null {
  const value = raw.trim().toLowerCase();
  if (value === 'rig' || value === 'van' || value === 'tanker' || value === 'flatbed') {
    return value;
  }
  return null;
}

function normalizeVehicleStatus(raw: string): FleetVehicleStatus | null {
  const value = raw.trim().toLowerCase().replace(/\s+/g, '_');
  if (value === 'on_road' || value === 'on corridor') {
    return 'on_road';
  }
  if (value === 'available' || value === 'ready' || value === 'ready_to_dispatch') {
    return 'available';
  }
  if (value === 'yard' || value === 'at_yard') {
    return 'yard';
  }
  if (value === 'maintenance' || value === 'workshop' || value === 'in_workshop') {
    return 'maintenance';
  }
  return null;
}

export function parseFleetVehicleImportCsv(text: string): FleetVehicleImportRow[] {
  const table = parseCsv(text.trim());
  if (!table.length) {
    return [];
  }
  const header = table[0].map((h) => h.trim().toUpperCase());
  const idx = (name: string) => header.indexOf(name);

  const regIdx = idx('REGISTRATION');
  const modelIdx = idx('MAKE_MODEL');
  const typeIdx = idx('ASSET_TYPE');
  const statusIdx = idx('STATUS');
  const ownershipIdx = idx('OWNERSHIP');
  const driverIdx = idx('DRIVER_NAME');
  const utilIdx = idx('UTILIZATION_PCT');

  if (regIdx < 0 || modelIdx < 0 || typeIdx < 0) {
    throw new Error('CSV must include REGISTRATION, MAKE_MODEL, and ASSET_TYPE columns.');
  }

  const rows: FleetVehicleImportRow[] = [];
  for (let line = 1; line < table.length; line++) {
    const cells = table[line];
    const registration = String(cells[regIdx] ?? '').trim();
    const makeModel = String(cells[modelIdx] ?? '').trim();
    if (!registration && !makeModel) {
      continue;
    }
    if (!registration || !makeModel) {
      throw new Error(`Row ${line + 1}: registration and make/model are required.`);
    }
    const type = normalizeVehicleType(String(cells[typeIdx] ?? ''));
    if (!type) {
      throw new Error(`Row ${line + 1}: ASSET_TYPE must be rig, van, tanker, or flatbed.`);
    }
    const statusRaw = statusIdx >= 0 ? String(cells[statusIdx] ?? '') : 'available';
    const status = normalizeVehicleStatus(statusRaw) ?? 'available';
    const ownershipRaw = ownershipIdx >= 0 ? String(cells[ownershipIdx] ?? 'owned').trim().toLowerCase() : 'owned';
    const ownershipType = ownershipRaw === 'contracted' ? 'contracted' : 'owned';
    if (ownershipType === 'contracted') {
      throw new Error(
        `Row ${line + 1}: contracted vehicles cannot be bulk-imported — use Add vehicle and pick a transporter.`,
      );
    }
    const driverName = driverIdx >= 0 ? String(cells[driverIdx] ?? '').trim() : '';
    const utilRaw = utilIdx >= 0 ? Number(String(cells[utilIdx] ?? '').trim()) : NaN;
    rows.push({
      registration,
      makeModel,
      type,
      status,
      ownershipType,
      driverName: driverName || undefined,
      utilizationPct: Number.isFinite(utilRaw) ? Math.min(100, Math.max(0, utilRaw)) : undefined,
    });
  }
  return rows;
}

export function parseFleetDriverImportCsv(text: string): FleetDriverImportRow[] {
  const table = parseCsv(text.trim());
  if (!table.length) {
    return [];
  }
  const header = table[0].map((h) => h.trim().toUpperCase());
  const idx = (name: string) => header.indexOf(name);

  const firstIdx = idx('FIRST_NAME');
  const lastIdx = idx('LAST_NAME');
  const phoneIdx = idx('PHONE');
  const licenseIdx = idx('LICENSE_NUMBER');
  const classIdx = idx('LICENSE_CLASS');

  if (firstIdx < 0 || lastIdx < 0) {
    throw new Error('CSV must include FIRST_NAME and LAST_NAME columns.');
  }

  const rows: FleetDriverImportRow[] = [];
  for (let line = 1; line < table.length; line++) {
    const cells = table[line];
    const firstName = String(cells[firstIdx] ?? '').trim();
    const lastName = String(cells[lastIdx] ?? '').trim();
    if (!firstName && !lastName) {
      continue;
    }
    if (!firstName || !lastName) {
      throw new Error(`Row ${line + 1}: first and last name are required.`);
    }
    rows.push({
      firstName,
      lastName,
      phoneNumber: phoneIdx >= 0 ? String(cells[phoneIdx] ?? '').trim() || undefined : undefined,
      licenseNumber: licenseIdx >= 0 ? String(cells[licenseIdx] ?? '').trim() || undefined : undefined,
      licenseClass: classIdx >= 0 ? String(cells[classIdx] ?? '').trim() || undefined : undefined,
    });
  }
  return rows;
}
