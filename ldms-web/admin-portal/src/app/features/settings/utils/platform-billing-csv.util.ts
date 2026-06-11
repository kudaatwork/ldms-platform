import type { PlatformActionChargeRow, SubscriptionPackageRow } from '../services/platform-wallet-admin.service';

export const ACTION_CHARGE_CSV_HEADERS = [
  'actionCode',
  'displayName',
  'description',
  'chargeCents',
  'category',
  'active',
] as const;

export const SUBSCRIPTION_PACKAGE_CSV_HEADERS = [
  'code',
  'name',
  'description',
  'monthlyPriceCents',
  'currencyCode',
  'sortOrder',
  'featured',
  'active',
] as const;

export const ACTION_CHARGE_SAMPLE_CSV = [
  ACTION_CHARGE_CSV_HEADERS.join(','),
  'TRIP_TRACK,Trip tracking ping,GPS / status tracking update for a trip,3,TRIPS,true',
  'PROCUREMENT_PR_APPROVE,Purchase requisition approval,Each internal approval stage on a PR,5,ORDERS,true',
].join('\n');

export const SUBSCRIPTION_PACKAGE_SAMPLE_CSV = [
  SUBSCRIPTION_PACKAGE_CSV_HEADERS.join(','),
  'STARTER,Starter,Essential platform access,9900,USD,10,false,true',
  'GROWTH,Growth,Higher volume corridors,24900,USD,20,true,true',
].join('\n');

export const ACTION_CHARGE_IMPORT_DISCLAIMER =
  'CSV import only. Keep column headers unchanged. Existing action codes are updated; new codes are created when valid.';
export const SUBSCRIPTION_PACKAGE_IMPORT_DISCLAIMER =
  'CSV import only. Keep column headers unchanged. Package code must be unique; existing packages are updated.';

export function downloadTextFile(filename: string, contents: string): void {
  const blob = new Blob([contents], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.rel = 'noopener';
  anchor.style.display = 'none';
  document.body.appendChild(anchor);
  anchor.click();
  requestAnimationFrame(() => {
    anchor.remove();
    URL.revokeObjectURL(url);
  });
}

function parseCsvLine(line: string): string[] {
  const cells: string[] = [];
  let current = '';
  let inQuotes = false;
  for (let i = 0; i < line.length; i++) {
    const ch = line[i];
    if (ch === '"') {
      if (inQuotes && line[i + 1] === '"') {
        current += '"';
        i++;
      } else {
        inQuotes = !inQuotes;
      }
      continue;
    }
    if (ch === ',' && !inQuotes) {
      cells.push(current.trim());
      current = '';
      continue;
    }
    current += ch;
  }
  cells.push(current.trim());
  return cells;
}

function parseCsv(text: string): string[][] {
  return text
    .replace(/^\uFEFF/, '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0)
    .map(parseCsvLine);
}

function boolCell(raw: string | undefined): boolean {
  const v = String(raw ?? '').trim().toLowerCase();
  return v === 'true' || v === '1' || v === 'yes';
}

export function parseActionChargeCsv(text: string): PlatformActionChargeRow[] {
  const rows = parseCsv(text);
  if (rows.length < 2) {
    throw new Error('CSV must include a header row and at least one data row.');
  }
  const header = rows[0].map((h) => h.trim());
  const idx = (name: string): number => header.indexOf(name);
  const required = ['actionCode', 'displayName', 'chargeCents'];
  for (const col of required) {
    if (idx(col) < 0) {
      throw new Error(`Missing required column: ${col}`);
    }
  }
  return rows.slice(1).map((cells) => ({
    actionCode: cells[idx('actionCode')]?.trim().toUpperCase() ?? '',
    displayName: cells[idx('displayName')]?.trim() ?? '',
    description: cells[idx('description')]?.trim() || undefined,
    chargeCents: Math.max(0, Number.parseInt(cells[idx('chargeCents')] ?? '0', 10) || 0),
    category: cells[idx('category')]?.trim().toUpperCase() || 'GENERAL',
    active: idx('active') >= 0 ? boolCell(cells[idx('active')]) : true,
  }));
}

export function parseSubscriptionPackageCsv(text: string): SubscriptionPackageRow[] {
  const rows = parseCsv(text);
  if (rows.length < 2) {
    throw new Error('CSV must include a header row and at least one data row.');
  }
  const header = rows[0].map((h) => h.trim());
  const idx = (name: string): number => header.indexOf(name);
  const required = ['code', 'name', 'monthlyPriceCents', 'currencyCode'];
  for (const col of required) {
    if (idx(col) < 0) {
      throw new Error(`Missing required column: ${col}`);
    }
  }
  return rows.slice(1).map((cells) => ({
    code: cells[idx('code')]?.trim().toUpperCase() ?? '',
    name: cells[idx('name')]?.trim() ?? '',
    description: cells[idx('description')]?.trim() || undefined,
    monthlyPriceCents: Math.max(0, Number.parseInt(cells[idx('monthlyPriceCents')] ?? '0', 10) || 0),
    currencyCode: (cells[idx('currencyCode')]?.trim() || 'USD').toUpperCase(),
    sortOrder: idx('sortOrder') >= 0 ? Number.parseInt(cells[idx('sortOrder')] ?? '0', 10) || 0 : 0,
    featured: idx('featured') >= 0 ? boolCell(cells[idx('featured')]) : false,
    active: idx('active') >= 0 ? boolCell(cells[idx('active')]) : true,
  }));
}

export function actionChargesToCsv(rows: PlatformActionChargeRow[]): string {
  const lines = [ACTION_CHARGE_CSV_HEADERS.join(',')];
  for (const row of rows) {
    lines.push(
      [
        row.actionCode,
        `"${(row.displayName ?? '').replace(/"/g, '""')}"`,
        `"${(row.description ?? '').replace(/"/g, '""')}"`,
        row.chargeCents ?? 0,
        row.category ?? 'GENERAL',
        row.active !== false,
      ].join(','),
    );
  }
  return lines.join('\n');
}

export function subscriptionPackagesToCsv(rows: SubscriptionPackageRow[]): string {
  const lines = [SUBSCRIPTION_PACKAGE_CSV_HEADERS.join(',')];
  for (const row of rows) {
    lines.push(
      [
        row.code,
        `"${(row.name ?? '').replace(/"/g, '""')}"`,
        `"${(row.description ?? '').replace(/"/g, '""')}"`,
        row.monthlyPriceCents ?? 0,
        row.currencyCode ?? 'USD',
        row.sortOrder ?? 0,
        row.featured === true,
        row.active !== false,
      ].join(','),
    );
  }
  return lines.join('\n');
}
