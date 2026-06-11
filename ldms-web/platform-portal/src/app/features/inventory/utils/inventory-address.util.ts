/** Builds a human-readable address from an ldms-locations address DTO. */
export function formatInventoryAddressLabel(dto: Record<string, unknown>): string {
  const formatted = String(dto['formattedAddress'] ?? '').trim();
  if (formatted) {
    return formatted;
  }

  const parts = [
    dto['line1'],
    dto['line2'],
    dto['suburbName'] ?? dto['villageName'] ?? dto['cityName'],
    dto['districtName'],
    dto['provinceName'],
    dto['postalCode'],
    dto['countryName'],
  ]
    .map((part) => String(part ?? '').trim())
    .filter(Boolean);

  return parts.length ? parts.join(', ') : '—';
}
