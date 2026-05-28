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

/** Strip {@code ROLE_} prefix for UI role checks. */
export function normalizeJwtRoles(raw: unknown): string[] {
  if (!Array.isArray(raw)) {
    return [];
  }
  return raw
    .map((r) => String(r).trim())
    .filter(Boolean)
    .map((r) => (r.startsWith('ROLE_') ? r.slice(5) : r));
}
