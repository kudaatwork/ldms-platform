import type { WarehouseRow } from '../models/inventory.model';

/** All searchable text for a warehouse (used for filtering and autocomplete matching). */
export function warehouseSearchText(w: WarehouseRow): string {
  return `${w.id} ${w.name} ${w.description} ${w.addressLabel} ${w.warehouseType} ${w.locationId}`
    .replace(/\s+/g, ' ')
    .trim()
    .toLowerCase();
}

/** Filter warehouses by name, description, address, type, id, or location id. */
export function filterWarehousesForSearch(warehouses: WarehouseRow[], query: string): WarehouseRow[] {
  const tokens = query
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
