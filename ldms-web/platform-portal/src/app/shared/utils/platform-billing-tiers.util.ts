/** Customer-facing prepaid wallet tiers (USD cents). See docs/PROJECT-LX-PLATFORM-PRICING-GUIDE.md */
export const PLATFORM_BILLING_TIERS = [
  {
    tier: 'INCLUDED',
    label: 'Included',
    cents: 0,
    icon: 'check_circle',
    summary: 'Admin, documents, and status updates — bundled in subscription',
    examples: 'Uploads, PO approvals, fleet registration, email & push',
  },
  {
    tier: 'MILESTONE',
    label: 'Milestone',
    cents: 1000,
    icon: 'local_shipping',
    summary: 'High-value corridor transactions ($5–$25 per event)',
    examples: 'Trip booking $10, dispatch $8, GRV $5, clearing match $25',
  },
  {
    tier: 'TRACKING',
    label: 'Premium GPS',
    cents: 150,
    icon: 'share_location',
    summary: 'Hardware GPS integration — per trip per calendar day',
    examples: 'Live map, GPS trail, high-frequency device pings',
  },
  {
    tier: 'TELEMETRY',
    label: 'Fuel telemetry',
    cents: 175,
    icon: 'local_gas_station',
    summary: 'Fuel sensor hardware — per vehicle per calendar day',
    examples: 'Fuel level telemetry, consumption analytics',
  },
  {
    tier: 'MESSAGING',
    label: 'SMS / WhatsApp',
    cents: 10,
    icon: 'sms',
    summary: 'After included monthly SMS quota in your plan',
    examples: 'Border alerts, delivery OTPs, WhatsApp stop commands',
  },
] as const;

export type PlatformBillingTierCode = (typeof PLATFORM_BILLING_TIERS)[number]['tier'];

export function billingTierLabel(tier?: string | null): string {
  const match = PLATFORM_BILLING_TIERS.find((entry) => entry.tier === tier);
  if (match) {
    return match.label;
  }
  const legacy: Record<string, string> = {
    LIGHT: 'Included',
    STANDARD: 'Included',
    HEAVY: 'Milestone',
  };
  return legacy[tier ?? ''] ?? tier ?? 'General';
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
