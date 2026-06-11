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

export function modulesForRoles(roles: string[]): Set<LdmsRoleModuleKey> {
  const modules = new Set<LdmsRoleModuleKey>();
  for (const role of normalizeRoleCodes(roles)) {
    modules.add(resolveLdmsRoleModule(role).key);
  }
  return modules;
}

function hasAnyModulePrefix(roles: string[], modulePrefixes: string[]): boolean {
  const mods = modulesForRoles(roles);
  return [...mods].some((m) => modulePrefixes.some((p) => m === p || m.startsWith(p)));
}

type RouteAccessRule = {
  prefix: string;
  public?: boolean;
  anyAssignedRole?: boolean;
  /** Organisation workspace route — navigation allowed when signed in; APIs enforce permissions. */
  orgWorkspace?: boolean;
  modulePrefixes?: string[];
};

/** Longest-prefix wins; keep sorted by descending prefix length. */
const ROUTE_ACCESS_RULES: RouteAccessRule[] = [
  { prefix: '/account', public: true },
  { prefix: '/help', public: true },
  { prefix: '/settings', anyAssignedRole: true },
  { prefix: '/documents', orgWorkspace: true },
  { prefix: '/fleet', orgWorkspace: true },
  { prefix: '/users/roles', orgWorkspace: true },
  { prefix: '/users/groups', orgWorkspace: true },
  { prefix: '/users/types', orgWorkspace: true },
  { prefix: '/users', orgWorkspace: true },
  { prefix: '/activity', orgWorkspace: true },
  { prefix: '/products-inventory', orgWorkspace: true },
  { prefix: '/my-orders', orgWorkspace: true },
  { prefix: '/dashboard', anyAssignedRole: true },
];

function ruleForPath(path: string): RouteAccessRule | undefined {
  const normalized = path.split('?')[0].split('#')[0];
  return ROUTE_ACCESS_RULES.find((rule) => normalized === rule.prefix || normalized.startsWith(`${rule.prefix}/`));
}

/** Whether the signed-in user may navigate to this path based on JWT user-group roles. */
export function canAccessPath(path: string, roles: string[] | undefined | null): boolean {
  const rule = ruleForPath(path.split('?')[0].split('#')[0]);
  if (!rule) {
    return true;
  }
  if (rule.public || rule.anyAssignedRole || rule.orgWorkspace) {
    return true;
  }
  const codes = normalizeRoleCodes(roles);
  if (codes.length === 0) {
    return false;
  }
  if (rule.modulePrefixes?.length) {
    return hasAnyModulePrefix(codes, rule.modulePrefixes);
  }
  return false;
}
