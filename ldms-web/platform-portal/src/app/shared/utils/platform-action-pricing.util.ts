import type { PlatformActionChargeRow } from '../../core/services/platform-wallet.service';

/** Well-known platform action codes used in Help & Support and Lexi. */
export const HELP_SUPPORT_ACTION_CODES = {
  ASSISTANT_MESSAGE: 'HELP_BOT_MESSAGE',
  AGENT_MESSAGE: 'HELP_BOT_AGENT_MESSAGE',
  SESSION_START: 'BOT_SESSION_START',
  TICKET_OPEN: 'HELP_SUPPORT_TICKET_OPEN',
  LIVE_CHAT_MESSAGE: 'HELP_LIVE_CHAT_MESSAGE',
} as const;

export type HelpSupportActionCode =
  (typeof HELP_SUPPORT_ACTION_CODES)[keyof typeof HELP_SUPPORT_ACTION_CODES];

export function actionChargeMap(charges: PlatformActionChargeRow[]): Map<string, PlatformActionChargeRow> {
  const map = new Map<string, PlatformActionChargeRow>();
  for (const row of charges) {
    const code = String(row.actionCode ?? '').trim().toUpperCase();
    if (code) {
      map.set(code, row);
    }
  }
  return map;
}

export function chargeCentsForAction(
  charges: PlatformActionChargeRow[] | Map<string, PlatformActionChargeRow>,
  actionCode: string,
): number {
  const map = charges instanceof Map ? charges : actionChargeMap(charges);
  return map.get(actionCode.trim().toUpperCase())?.chargeCents ?? 0;
}

export function formatActionPrice(
  charges: PlatformActionChargeRow[] | Map<string, PlatformActionChargeRow>,
  actionCode: string,
  formatCents: (cents: number, currencyCode?: string) => string,
  options?: { perMessage?: boolean; freeLabel?: string },
): string {
  const map = charges instanceof Map ? charges : actionChargeMap(charges);
  const row = map.get(actionCode.trim().toUpperCase());
  const cents = row?.chargeCents ?? 0;
  if (cents <= 0) {
    return options?.freeLabel ?? 'Included';
  }
  const formatted = formatCents(cents, 'USD');
  return options?.perMessage ? `${formatted} / msg` : formatted;
}

/** Build milestone tier example text from live catalog (e.g. pricing page). */
export function milestoneExamplesFromCharges(charges: PlatformActionChargeRow[]): string {
  const map = actionChargeMap(charges);
  const picks: { code: string; fallback: string }[] = [
    { code: 'TRIP_CREATE', fallback: 'Trip booking' },
    { code: 'SHIPMENT_DISPATCH', fallback: 'Dispatch' },
    { code: 'INVENTORY_GRV_CREATE', fallback: 'GRV' },
    { code: 'CLEARING_AGENT_MATCH', fallback: 'Clearing match' },
  ];
  const parts: string[] = [];
  for (const pick of picks) {
    const row = map.get(pick.code);
    if (!row) {
      continue;
    }
    const label = row.displayName?.trim() || pick.fallback;
    if (row.chargeCents <= 0) {
      parts.push(`${label} included`);
    } else {
      parts.push(`${label}`);
    }
  }
  return parts.length ? parts.join(', ') : 'Trip booking, dispatch, GRV, clearing match';
}

export interface PrepaidDemoEvent {
  icon: string;
  label: string;
  cents: number;
  credit: boolean;
}

/** Prepaid wallet animation on the public pricing page — uses live catalog when available. */
export function buildPrepaidDemoEvents(charges: PlatformActionChargeRow[]): PrepaidDemoEvent[] {
  const map = actionChargeMap(charges);
  const pick = (code: string, label: string, icon: string, credit = false): PrepaidDemoEvent => ({
    icon,
    label,
    cents: map.get(code)?.chargeCents ?? 0,
    credit,
  });
  return [
    { icon: 'account_balance_wallet', label: 'Wallet top-up', cents: 50000, credit: true },
    pick('TRIP_CREATE', 'Trip booking (milestone)', 'local_shipping'),
    pick('TRIP_TRACK', 'Premium GPS day', 'share_location'),
    pick('NOTIFICATION_SMS', 'SMS alert sent', 'sms'),
    pick('DOCUMENT_UPLOAD', 'Document upload (included)', 'upload_file'),
  ];
}
