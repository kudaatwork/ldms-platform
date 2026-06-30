import type { LinkedSupplierOption } from '../models/inventory.model';

export function supplierSearchText(s: LinkedSupplierOption): string {
  return `${s.id} ${s.name} ${s.email ?? ''}`.replace(/\s+/g, ' ').trim().toLowerCase();
}

export function supplierSearchTextWithHints(
  s: LinkedSupplierOption,
  stockHintBySupplierId?: Readonly<Record<number, string>> | null,
): string {
  const stockHint = stockHintBySupplierId?.[s.id]?.trim() ?? '';
  return `${supplierSearchText(s)} ${stockHint}`.replace(/\s+/g, ' ').trim().toLowerCase();
}

export function filterSuppliersForSearch(
  suppliers: LinkedSupplierOption[],
  query: string | number | null | undefined,
  stockHintBySupplierId?: Readonly<Record<number, string>> | null,
): LinkedSupplierOption[] {
  const tokens = String(query ?? '')
    .trim()
    .toLowerCase()
    .split(/\s+/)
    .filter(Boolean);
  if (!tokens.length) {
    return suppliers;
  }
  return suppliers.filter((s) => {
    const hay = supplierSearchTextWithHints(s, stockHintBySupplierId);
    return tokens.every((token) => hay.includes(token));
  });
}

export function supplierPickerLabel(s: LinkedSupplierOption): string {
  const name = s.name?.trim();
  return name || `#${s.id}`;
}
