/** Human-readable labels for account/profile fields (My Account, shell chrome). */

export function formatIsoDateForDisplay(value: unknown): string {
  if (value == null || value === '') {
    return '—';
  }
  const d = new Date(String(value));
  if (Number.isNaN(d.getTime())) {
    return '—';
  }
  return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

export function formatIsoDateTimeForDisplay(value: unknown): string {
  if (value == null || value === '') {
    return '—';
  }
  const d = new Date(String(value));
  if (Number.isNaN(d.getTime())) {
    return '—';
  }
  return d.toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function formatGenderLabel(raw: unknown): string {
  const g = String(raw ?? '').trim().toUpperCase();
  if (!g) {
    return '—';
  }
  if (g === 'MALE') {
    return 'Male';
  }
  if (g === 'FEMALE') {
    return 'Female';
  }
  if (g === 'OTHER') {
    return 'Other';
  }
  return g.charAt(0) + g.slice(1).toLowerCase();
}

export function formatEntityStatusLabel(raw: unknown): string {
  const t = String(raw ?? '').trim().toUpperCase();
  if (!t) {
    return '—';
  }
  return t.charAt(0) + t.slice(1).toLowerCase();
}

/** Placeholder i18n strings from the API are not real security questions. */
export function formatSecurityQuestionLabel(raw: unknown): string {
  const text = String(raw ?? '').trim();
  if (!text) {
    return '—';
  }
  if (/please set your/i.test(text) || /not set/i.test(text)) {
    return 'Not set';
  }
  return text;
}

function toRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return null;
  }
  return value as Record<string, unknown>;
}

/** User-facing role label: group name or user type — never JWT permission codes. */
export function resolveUserRoleLabel(user: Record<string, unknown> | null | undefined): string {
  if (!user) {
    return '';
  }
  const group = toRecord(user['userGroupDto']);
  const userType = toRecord(user['userTypeDto']);
  return String(group?.['name'] ?? userType?.['userTypeName'] ?? '').trim();
}

export function shellRoleSummary(
  roleLabel: string | undefined,
  orgClassification: string | undefined,
): string {
  const label = String(roleLabel ?? '').trim();
  if (label) {
    return label;
  }
  const org = String(orgClassification ?? '').trim();
  if (org) {
    return org.replace(/_/g, ' ');
  }
  return 'User';
}
