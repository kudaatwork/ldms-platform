import type { WarehouseRow } from '../models/inventory.model';

/** All searchable text for a warehouse (used for filtering and autocomplete matching). */
export function warehouseSearchText(w: WarehouseRow): string {
  return `${w.id} ${w.name} ${w.description} ${w.addressLabel} ${w.warehouseType} ${w.locationId}`
    .replace(/\s+/g, ' ')
    .trim()
    .toLowerCase();
}

/** Filter warehouses by name, description, address, type, id, or location id. */
export function filterWarehousesForSearch(warehouses: WarehouseRow[], query: string | number | null | undefined): WarehouseRow[] {
  const tokens = String(query ?? '')
    .trim()
    .toLowerCase()
    .split(/\s+/)
    .filter(Boolean);
  if (!tokens.length) {
    return warehouses;
  }
  return warehouses.filter((w) => {
    const hay = warehouseSearchText(w);
    return tokens.every((token) => hay.includes(token));
  });
}

export function warehousePickerLabel(w: WarehouseRow): string {
  const name = w.name?.trim();
  return name || `#${w.id}`;
}

export function warehouseTypeLabel(type: string | undefined): string {
  const normalized = (type ?? '').trim().toUpperCase();
  if (normalized === 'CUSTOMER') {
    return 'Customer warehouse';
  }
  if (normalized === 'TRANSIT') {
    return 'Transit warehouse';
  }
  if (normalized === 'SUPPLIER') {
    return 'Supplier warehouse';
  }
  return type?.trim() || 'Warehouse';
}

/** Secondary line for warehouse autocomplete options. */
export function warehouseOptionMeta(w: WarehouseRow): string {
  const parts: string[] = [];
  const type = warehouseTypeLabel(w.warehouseType);
  if (type) {
    parts.push(type);
  }
  if (w.branchLabel?.trim()) {
    parts.push(w.branchLabel.trim());
  }
  if (w.addressLabel?.trim()) {
    parts.push(w.addressLabel.trim());
  }
  return parts.join(' · ');
}
