import type { NotificationChannel, NotificationTemplateRow } from '../models/notification-admin.models';

export interface ChannelDeliveryMeta {
  icon: string;
  accent: string;
  softBg: string;
  border: string;
}

const CHANNEL_META: Partial<Record<NotificationChannel, ChannelDeliveryMeta>> = {
  EMAIL: { icon: 'mail', accent: '#1d4ed8', softBg: '#eff6ff', border: '#bfdbfe' },
  SMS: { icon: 'sms', accent: '#0f766e', softBg: '#ecfdf5', border: '#99f6e4' },
  WHATSAPP: { icon: 'chat', accent: '#15803d', softBg: '#f0fdf4', border: '#bbf7d0' },
  IN_APP: { icon: 'notifications', accent: '#7c3aed', softBg: '#f5f3ff', border: '#ddd6fe' },
  SLACK: { icon: 'tag', accent: '#b45309', softBg: '#fffbeb', border: '#fde68a' },
  TEAMS: { icon: 'groups', accent: '#4338ca', softBg: '#eef2ff', border: '#c7d2fe' },
};

const DEFAULT_META: ChannelDeliveryMeta = {
  icon: 'send',
  accent: '#475569',
  softBg: '#f8fafc',
  border: '#e2e8f0',
};

export function channelDeliveryMeta(channel: NotificationChannel): ChannelDeliveryMeta {
  return CHANNEL_META[channel] ?? DEFAULT_META;
}

/** Normalise API/JSON keys (e.g. email → EMAIL) and boolean values. */
export function coerceChannelDeliveryMap(
  existing: Record<string, boolean> | null | undefined,
): Record<string, boolean> {
  if (!existing) {
    return {};
  }
  const out: Record<string, boolean> = {};
  for (const [key, value] of Object.entries(existing)) {
    const normalized = key.trim().toUpperCase();
    if (normalized) {
      out[normalized] = value === true;
    }
  }
  return out;
}

export function isOrganizationNotificationTemplateKey(templateKey: string | null | undefined): boolean {
  const key = (templateKey ?? '').trim().toUpperCase();
  return key.startsWith('ORG_') || key.startsWith('ORGANIZATION_');
}

export function isChannelDeliveryActive(
  channel: NotificationChannel,
  flags: Record<string, boolean> | null | undefined,
): boolean {
  const map = coerceChannelDeliveryMap(flags);
  if (Object.keys(map).length === 0) {
    return true;
  }
  if (map[channel] === undefined) {
    return true;
  }
  return map[channel] === true;
}

export function defaultOrganizationChannelDeliveryFlags(): Record<string, boolean> {
  return { EMAIL: true, SMS: false, WHATSAPP: false };
}

/** Merge API flags with defaults for each listed channel. */
export function normalizeChannelDeliveryFlags(
  channels: NotificationChannel[],
  existing: Record<string, boolean> | null | undefined,
  templateKey: string,
): Record<string, boolean> {
  const flags = coerceChannelDeliveryMap(existing);
  const isOrg = isOrganizationNotificationTemplateKey(templateKey);
  for (const ch of channels) {
    if (flags[ch] === undefined) {
      flags[ch] = isOrg ? ch === 'EMAIL' : true;
    }
  }
  return flags;
}

export function hasPerChannelDeliveryConfig(
  templateKey: string,
  flags: Record<string, boolean> | null | undefined,
): boolean {
  return (
    isOrganizationNotificationTemplateKey(templateKey) ||
    Object.keys(coerceChannelDeliveryMap(flags)).length > 0
  );
}

export function supportsChannelDeliveryToggles(row: NotificationTemplateRow): boolean {
  return hasPerChannelDeliveryConfig(row.templateKey, row.channelDeliveryEnabled);
}

export interface ChannelDeliveryToggleRow {
  channel: NotificationChannel;
  label: string;
  active: boolean;
  meta: ChannelDeliveryMeta;
}

export function channelDeliveryToggleRows(row: NotificationTemplateRow): ChannelDeliveryToggleRow[] {
  const channels = (row.channels ?? []) as NotificationChannel[];
  const flags = normalizeChannelDeliveryFlags(channels, row.channelDeliveryEnabled, row.templateKey);
  return buildChannelDeliveryToggleRows(channels, flags);
}

export function buildChannelDeliveryToggleRows(
  channels: NotificationChannel[],
  flags: Record<string, boolean>,
): ChannelDeliveryToggleRow[] {
  return channels.map((channel) => ({
    channel,
    label: channelLabel(channel),
    active: flags[channel] === true,
    meta: channelDeliveryMeta(channel),
  }));
}

export function channelLabel(channel: NotificationChannel): string {
  switch (channel) {
    case 'EMAIL':
      return 'Email';
    case 'SMS':
      return 'SMS';
    case 'WHATSAPP':
      return 'WhatsApp';
    case 'IN_APP':
      return 'In-app';
    case 'SLACK':
      return 'Slack';
    case 'TEAMS':
      return 'Teams';
  }
}

export function formatChannelsWithDelivery(row: NotificationTemplateRow): string {
  return channelDeliveryToggleRows(row)
    .map((ch) => (ch.active ? ch.label : `${ch.label} (off)`))
    .join(', ');
}

export function rowChannelDeliveryActive(
  row: NotificationTemplateRow,
  channel: NotificationChannel,
): boolean {
  const channels = (row.channels ?? []) as NotificationChannel[];
  const flags = normalizeChannelDeliveryFlags(channels, row.channelDeliveryEnabled, row.templateKey);
  return flags[channel] === true;
}
