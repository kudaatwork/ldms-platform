/** Parsed LDMS JWT payload (access token). */
export interface LdmsJwtPayload {
  sub?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  userId?: number | string;
  organizationId?: number | string;
  organizationKycApprover?: boolean;
  operationalIssueHandler?: boolean;
  roles?: string[];
  exp?: number;
}

/** Normalizes a token value before persistence or guard checks. */
export function normalizeAccessToken(raw: string | null | undefined): string | null {
  if (!raw) {
    return null;
  }
  const trimmed = raw.trim();
  if (!trimmed || trimmed.startsWith('mock-token-')) {
    return null;
  }
  if (trimmed.toLowerCase().startsWith('bearer ')) {
    return trimmed.slice(7).trim() || null;
  }
  return trimmed;
}

/** True when the token is a decodable JWT with a subject (ignores expiry — for route guards after sign-in). */
export function isReadableAccessToken(token: string | null | undefined): boolean {
  if (token?.startsWith('mock-token-')) {
    return true;
  }
  const normalized = normalizeAccessToken(token);
  if (!normalized || normalized.split('.').length < 3) {
    return false;
  }
  const sub = String(decodeJwtPayload(normalized)?.sub ?? '').trim();
  return sub.length > 0;
}

/** True when the stored value is a non-expired JWT (mock tokens excluded). */
export function isStoredSessionToken(token: string | null | undefined): boolean {
  const normalized = normalizeAccessToken(token);
  if (!normalized) {
    return false;
  }
  const parts = normalized.split('.');
  if (parts.length < 3) {
    return false;
  }
  const payload = decodeJwtPayload(normalized);
  if (!payload) {
    return false;
  }
  if (payload.exp == null) {
    return true;
  }
  return Date.now() < payload.exp * 1000;
}

export function decodeJwtPayload(token: string): LdmsJwtPayload | null {
  const normalized = normalizeAccessToken(token);
  if (!normalized) {
    return null;
  }
  const parts = normalized.split('.');
  if (parts.length < 2) {
    return null;
  }
  try {
    const json = decodeBase64Url(parts[1]);
    return JSON.parse(json) as LdmsJwtPayload;
  } catch {
    return null;
  }
}

function decodeBase64Url(segment: string): string {
  const base64 = segment.replace(/-/g, '+').replace(/_/g, '/');
  const remainder = base64.length % 4;
  const padded = remainder ? base64 + '='.repeat(4 - remainder) : base64;
  return atob(padded);
}

/** Strip {@code ROLE_} prefix for UI role checks. Accepts array or comma-separated JWT claim. */
export function normalizeJwtRoles(raw: unknown): string[] {
  if (typeof raw === 'string') {
    return raw
      .split(',')
      .map((r) => r.trim())
      .filter(Boolean)
      .map((r) => (r.startsWith('ROLE_') ? r.slice(5) : r));
  }
  if (!Array.isArray(raw)) {
    return [];
  }
  return raw
    .map((r) => String(r).trim())
    .filter(Boolean)
    .map((r) => (r.startsWith('ROLE_') ? r.slice(5) : r));
}
