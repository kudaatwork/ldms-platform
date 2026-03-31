/**
 * Optional global substring across all primitive values on the row (any column matches),
 * then AND each non-empty per-column filter (substring on that property only).
 * Values are compared via String(value).toLowerCase().
 */
export function filterByGlobalAndColumns<T extends object>(
  rows: T[],
  globalQuery: string,
  columnFilters: Partial<Record<keyof T & string, string>>,
): T[] {
  let out = [...rows];
  const g = globalQuery.trim().toLowerCase();
  if (g) {
    out = out.filter((row) => {
      const rec = row as Record<string, unknown>;
      return Object.keys(rec).some((k) =>
        String(rec[k] ?? '')
          .toLowerCase()
          .includes(g),
      );
    });
  }
  for (const key of Object.keys(columnFilters) as (keyof T & string)[]) {
    const raw = columnFilters[key];
    const t = String(raw ?? '')
      .trim()
      .toLowerCase();
    if (!t) continue;
    out = out.filter((row) =>
      String((row as Record<string, unknown>)[key] ?? '')
        .toLowerCase()
        .includes(t),
    );
  }
  return out;
}
