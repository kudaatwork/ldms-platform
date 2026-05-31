/** Parsed LDMS JWT payload (access token). */
export interface LdmsJwtPayload {
  sub?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  userId?: number | string;
  organizationId?: number | string;
  organizationKycApprover?: boolean;
  roles?: string[];
  exp?: number;
}

/** True when the stored value is a non-expired JWT (mock tokens excluded). */
export function isStoredSessionToken(token: string | null | undefined): boolean {
  if (!token || token.startsWith('mock-token-')) {
    return false;
  }
  const payload = decodeJwtPayload(token);
  if (!payload) {
    return false;
  }
  if (payload.exp == null) {
    return true;
  }
  return Date.now() < payload.exp * 1000;
}

export function decodeJwtPayload(token: string): LdmsJwtPayload | null {
  const parts = token.split('.');
  if (parts.length < 2) {
    return null;
  }
  try {
    const json = atob(parts[1].replace(/-/g, '+').replace(/_/g, '/'));
    return JSON.parse(json) as LdmsJwtPayload;
  } catch {
    return null;
  }
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
