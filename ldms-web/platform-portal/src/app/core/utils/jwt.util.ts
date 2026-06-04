import type { CurrentUser, OrganizationClassification } from '../models/auth.model';

export interface LdmsJwtPayload {
  sub?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  userId?: number | string;
  organizationId?: number | string;
  orgClassification?: string;
  orgName?: string;
  roles?: string[];
  mustChangeCredentials?: boolean;
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

export function normalizeJwtRoles(raw: unknown): string[] {
  if (Array.isArray(raw)) {
    return raw
      .map((r) => String(r).trim())
      .filter(Boolean)
      .map((r) => (r.startsWith('ROLE_') ? r.slice(5) : r));
  }
  if (typeof raw === 'string' && raw.trim()) {
    return raw
      .split(',')
      .map((r) => r.trim())
      .filter(Boolean)
      .map((r) => (r.startsWith('ROLE_') ? r.slice(5) : r));
  }
  return [];
}

export function currentUserFromJwt(token: string): CurrentUser | null {
  const payload = decodeJwtPayload(token);
  if (!payload) {
    return null;
  }
  const email = String(payload.email ?? payload.sub ?? '').trim();
  const firstName = String(payload.firstName ?? '').trim();
  const lastName = String(payload.lastName ?? '').trim();
  return {
    userId: payload.userId != null ? String(payload.userId) : '',
    organizationId: payload.organizationId != null ? String(payload.organizationId) : '',
    orgClassification:
      (payload.orgClassification as OrganizationClassification | undefined) ?? 'SUPPLIER',
    orgName: String(payload.orgName ?? ''),
    roles: normalizeJwtRoles(payload.roles),
    email: email || undefined,
    firstName: firstName || undefined,
    lastName: lastName || undefined,
    mustChangeCredentials: payload.mustChangeCredentials === true,
  };
}
