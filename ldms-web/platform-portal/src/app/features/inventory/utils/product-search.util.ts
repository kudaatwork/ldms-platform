import type { ProductRow } from '../models/inventory.model';

/** Filter products by name, code, category, description, or manufacturer. */
export function filterProductsForSearch(products: ProductRow[], query: string): ProductRow[] {
  const q = query.trim().toLowerCase();
  if (!q) {
    return products;
  }
  const exact = products.filter((p) => {
    const code = p.code?.trim().toLowerCase();
    const barcode = p.barcode?.trim().toLowerCase();
    return code === q || barcode === q;
  });
  if (exact.length === 1) {
    return exact;
  }
  return products.filter((p) => {
    const hay =
      `${p.name} ${p.code} ${p.barcode} ${p.categoryName} ${p.subcategoryName} ${p.description} ${p.manufacturer}`.toLowerCase();
    return hay.includes(q);
  });
}

export function productPickerLabel(p: ProductRow): string {
  const code = p.code?.trim();
  const barcode = p.barcode?.trim();
  if (code) {
    return `${p.name} (${code})`;
  }
  if (barcode) {
    return `${p.name} [${barcode}]`;
  }
  return p.name;
}
