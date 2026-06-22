/** Fused customer-facing prepaid wallet tiers (USD cents). */
export const PLATFORM_BILLING_TIERS = [
  {
    tier: 'LIGHT',
    label: 'Light',
    cents: 5,
    icon: 'tune',
    summary: 'Workflow steps, documents, stock admin, email & push',
    examples: 'PO approvals, compliance uploads, stock reserve',
  },
  {
    tier: 'STANDARD',
    label: 'Standard',
    cents: 15,
    icon: 'swap_horiz',
    summary: 'Orders, fleet ops, and corridor activity steps',
    examples: 'New order, driver assign, fuel fund request',
  },
  {
    tier: 'HEAVY',
    label: 'Heavy',
    cents: 45,
    icon: 'local_shipping',
    summary: 'Deliveries, proof of receipt, invoices, exports',
    examples: 'Trip complete, GRV, shipment dispatch, invoice',
  },
  {
    tier: 'TRACKING',
    label: 'Live tracking',
    cents: 20,
    icon: 'share_location',
    summary: 'Per trip per calendar day (not per GPS ping)',
    examples: 'Live map, GPS trail, ops tracking session',
  },
  {
    tier: 'MESSAGING',
    label: 'SMS / WhatsApp',
    cents: 7,
    icon: 'sms',
    summary: 'Outbound SMS and WhatsApp bot commands',
    examples: 'Driver alerts, WhatsApp stop commands',
  },
] as const;

export type PlatformBillingTierCode = (typeof PLATFORM_BILLING_TIERS)[number]['tier'];

export function billingTierLabel(tier?: string | null): string {
  const match = PLATFORM_BILLING_TIERS.find((entry) => entry.tier === tier);
  return match?.label ?? tier ?? 'General';
}

export function chargesForTier<T extends { billingTier?: string | null }>(
  charges: T[],
  tier: PlatformBillingTierCode,
): T[] {
  return charges.filter((charge) => (charge.billingTier ?? '').toUpperCase() === tier);
}

export function tierRateCents(tier: PlatformBillingTierCode): number {
  return PLATFORM_BILLING_TIERS.find((entry) => entry.tier === tier)?.cents ?? 0;
}
