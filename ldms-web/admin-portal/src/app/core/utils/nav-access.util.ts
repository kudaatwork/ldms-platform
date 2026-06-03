import { LdmsRoleModuleKey, resolveLdmsRoleModule } from '@shared/utils/ldms-role-module.util';

export function normalizeRoleCodes(roles: string[] | undefined | null): string[] {
  if (!roles?.length) {
    return [];
  }
  return roles
    .map((r) => String(r).trim().toUpperCase())
    .filter(Boolean)
    .map((r) => (r.startsWith('ROLE_') ? r.slice(5) : r));
}

/** Full admin portal access (all menus). */
export function hasPlatformWideAccess(roles: string[]): boolean {
  const set = new Set(normalizeRoleCodes(roles));
  return set.has('ADMIN') || set.has('READ_ONLY');
}

export function modulesForRoles(roles: string[]): Set<LdmsRoleModuleKey> {
  const modules = new Set<LdmsRoleModuleKey>();
  for (const role of normalizeRoleCodes(roles)) {
    modules.add(resolveLdmsRoleModule(role).key);
  }
  return modules;
}

function hasAnyModulePrefix(roles: string[], modulePrefixes: string[]): boolean {
  if (hasPlatformWideAccess(roles)) {
    return true;
  }
  const mods = modulesForRoles(roles);
  return [...mods].some((m) => modulePrefixes.some((p) => m === p || m.startsWith(p)));
}

function hasAnyRoleCode(roles: string[], required: string[]): boolean {
  if (hasPlatformWideAccess(roles)) {
    return true;
  }
  const set = new Set(normalizeRoleCodes(roles));
  return required.some((r) => set.has(r.toUpperCase()));
}

type RouteAccessRule = {
  prefix: string;
  /** Authenticated users always allowed (account, help). */
  public?: boolean;
  /** Any assigned group role. */
  anyAssignedRole?: boolean;
  /** ADMIN only (settings). */
  adminOnly?: boolean;
  /** ADMIN or READ_ONLY (system health). */
  platformOnly?: boolean;
  /** User must hold a role mapped to one of these module keys/prefixes. */
  modulePrefixes?: string[];
  /** Exact role codes (any). */
  roleCodes?: string[];
};

/** Longest-prefix wins; keep sorted by descending prefix length. */
const ROUTE_ACCESS_RULES: RouteAccessRule[] = [
  { prefix: '/account', public: true },
  { prefix: '/help', public: true },
  { prefix: '/settings', adminOnly: true },
  { prefix: '/system', platformOnly: true },
  { prefix: '/users/roles', modulePrefixes: ['user-management.roles'] },
  { prefix: '/users/groups', modulePrefixes: ['user-management.groups'] },
  { prefix: '/users/types', modulePrefixes: ['user-management.types'] },
  { prefix: '/users', modulePrefixes: ['user-management.'] },
  { prefix: '/activity', modulePrefixes: ['audit-trail'] },
  { prefix: '/notifications', modulePrefixes: ['notifications'] },
  { prefix: '/locations', modulePrefixes: ['locations.'] },
  { prefix: '/organizations', modulePrefixes: ['organization-management'] },
  { prefix: '/kyc', modulePrefixes: ['platform', 'organization-management'] },
  { prefix: '/dashboard', anyAssignedRole: true },
];

function ruleForPath(path: string): RouteAccessRule | undefined {
  const normalized = path.split('?')[0].split('#')[0];
  return ROUTE_ACCESS_RULES.find((rule) => normalized === rule.prefix || normalized.startsWith(`${rule.prefix}/`));
}

/**
 * Whether the signed-in user may navigate to this path based on user-group roles only.
 */
export function canAccessPath(path: string, roles: string[] | undefined | null): boolean {
  const normalizedPath = path.split('?')[0].split('#')[0];
  const rule = ruleForPath(normalizedPath);
  if (!rule) {
    return hasPlatformWideAccess(roles ?? []) || normalizeRoleCodes(roles).length > 0;
  }
  if (rule.public) {
    return true;
  }
  const codes = normalizeRoleCodes(roles);
  if (codes.length === 0) {
    return false;
  }
  if (rule.adminOnly) {
    return codes.includes('ADMIN');
  }
  if (rule.platformOnly) {
    return hasPlatformWideAccess(codes);
  }
  if (rule.anyAssignedRole) {
    return true;
  }
  if (rule.roleCodes?.length) {
    return hasAnyRoleCode(codes, rule.roleCodes);
  }
  if (rule.modulePrefixes?.length) {
    return hasAnyModulePrefix(codes, rule.modulePrefixes);
  }
  return false;
}

