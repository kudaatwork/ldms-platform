/** Stable module key for grouping LDMS permission codes in the admin UI. */
export type LdmsRoleModuleKey =
  | 'platform'
  | 'user-management.users'
  | 'user-management.accounts'
  | 'user-management.addresses'
  | 'user-management.groups'
  | 'user-management.roles'
  | 'user-management.security'
  | 'user-management.preferences'
  | 'user-management.types'
  | 'user-management.password'
  | 'organization-management'
  | 'audit-trail'
  | 'notifications'
  | 'locations.address'
  | 'locations.administrative-level'
  | 'locations.city'
  | 'locations.country'
  | 'locations.district'
  | 'locations.geo-coordinates'
  | 'locations.language'
  | 'locations.localized-name'
  | 'locations.location-node'
  | 'locations.province'
  | 'locations.suburb'
  | 'locations.village'
  | 'other';

export interface LdmsRoleModuleSection {
  key: LdmsRoleModuleKey;
  label: string;
  icon: string;
  sortOrder: number;
}

const MODULE_META: Record<LdmsRoleModuleKey, Omit<LdmsRoleModuleSection, 'key'>> = {
  platform: { label: 'Platform', icon: 'shield', sortOrder: 0 },
  'user-management.users': { label: 'User management — Users', icon: 'person', sortOrder: 10 },
  'user-management.accounts': { label: 'User management — Accounts', icon: 'account_circle', sortOrder: 11 },
  'user-management.addresses': { label: 'User management — Addresses', icon: 'home', sortOrder: 12 },
  'user-management.groups': { label: 'User management — Groups', icon: 'groups', sortOrder: 13 },
  'user-management.roles': { label: 'User management — Roles', icon: 'admin_panel_settings', sortOrder: 14 },
  'user-management.security': { label: 'User management — Security', icon: 'security', sortOrder: 15 },
  'user-management.preferences': { label: 'User management — Preferences', icon: 'tune', sortOrder: 16 },
  'user-management.types': { label: 'User management — Types', icon: 'category', sortOrder: 17 },
  'user-management.password': {
    label: 'User management — Password & verification',
    icon: 'vpn_key',
    sortOrder: 18,
  },
  'organization-management': { label: 'Organization management', icon: 'business', sortOrder: 20 },
  'audit-trail': { label: 'Audit trail', icon: 'history', sortOrder: 30 },
  notifications: { label: 'Notifications', icon: 'notifications', sortOrder: 40 },
  'locations.address': { label: 'Locations — Addresses', icon: 'place', sortOrder: 50 },
  'locations.administrative-level': {
    label: 'Locations — Administrative levels',
    icon: 'account_tree',
    sortOrder: 51,
  },
  'locations.city': { label: 'Locations — Cities', icon: 'location_city', sortOrder: 52 },
  'locations.country': { label: 'Locations — Countries', icon: 'public', sortOrder: 53 },
  'locations.district': { label: 'Locations — Districts', icon: 'map', sortOrder: 54 },
  'locations.geo-coordinates': { label: 'Locations — Geo coordinates', icon: 'my_location', sortOrder: 55 },
  'locations.language': { label: 'Locations — Languages', icon: 'translate', sortOrder: 56 },
  'locations.localized-name': { label: 'Locations — Localized names', icon: 'language', sortOrder: 57 },
  'locations.location-node': { label: 'Locations — Location nodes', icon: 'hub', sortOrder: 58 },
  'locations.province': { label: 'Locations — Provinces', icon: 'terrain', sortOrder: 59 },
  'locations.suburb': { label: 'Locations — Suburbs', icon: 'holiday_village', sortOrder: 60 },
  'locations.village': { label: 'Locations — Villages', icon: 'cottage', sortOrder: 61 },
  other: { label: 'Other', icon: 'extension', sortOrder: 99 },
};

const PLATFORM = new Set(['ADMIN', 'KYC_STAGE1', 'KYC_STAGE2', 'KYC_STAGE3', 'KYC_STAGE4', 'KYC_STAGE5', 'READ_ONLY']);
const ORGANIZATION = new Set([
  'SUBMIT_KYC',
  'VIEW_MY_ORGAN',
  'UPDATE_MY_ORGAN',
  'MANAGE_BRANCHES',
  'LIST_CUSTOMERS',
  'REGISTER_CUSTOMER',
  'LINK_TRANSPORTER',
]);
const PASSWORD = new Set([
  'CHANGE_USER_PASSWORD',
  'RESET_USER_PASSWORD',
  'FORGOT_PASSWORD',
  'VALIDATE_RESET_TOKEN',
  'VERIFY_USER_EMAIL',
  'RESEND_VERIFICATION_LINK',
]);

function contains(role: string, fragment: string): boolean {
  return role.includes(fragment);
}

function entity(role: string, singular: string, plural: string): boolean {
  return contains(role, `_${singular}`) || contains(role, plural);
}

/** Resolves the module for a permission code (mirrors backend {@code LdmsRoleModuleResolver}). */
export function resolveLdmsRoleModule(role: string): LdmsRoleModuleSection {
  const normalized = (role ?? '').trim().toUpperCase();
  let key: LdmsRoleModuleKey = 'other';

  if (PLATFORM.has(normalized)) {
    key = 'platform';
  } else if (ORGANIZATION.has(normalized)) {
    key = 'organization-management';
  } else if (contains(normalized, 'AUDIT_LOG')) {
    key = 'audit-trail';
  } else if (contains(normalized, 'TEMPLATE')) {
    key = 'notifications';
  } else if (PASSWORD.has(normalized)) {
    key = 'user-management.password';
  } else if (contains(normalized, 'USER_ACCOUNT')) {
    key = 'user-management.accounts';
  } else if (contains(normalized, 'USER_ADDRESS')) {
    key = 'user-management.addresses';
  } else if (
    contains(normalized, 'USER_GROUP') ||
    normalized === 'ASSIGN_USER_ROLES_TO_USER_GROUP' ||
    normalized === 'REMOVE_USER_ROLES_FROM_USER_GROUP'
  ) {
    key = 'user-management.groups';
  } else if (
    contains(normalized, 'USER_ROLE') &&
    normalized !== 'ASSIGN_USER_ROLES_TO_USER_GROUP' &&
    normalized !== 'REMOVE_USER_ROLES_FROM_USER_GROUP'
  ) {
    key = 'user-management.roles';
  } else if (contains(normalized, 'USER_SECURIT')) {
    key = 'user-management.security';
  } else if (contains(normalized, 'USER_PREFERENCES')) {
    key = 'user-management.preferences';
  } else if (contains(normalized, 'USER_TYPE')) {
    key = 'user-management.types';
  } else if (
    contains(normalized, 'USER') &&
    !contains(normalized, 'USER_ACCOUNT') &&
    !contains(normalized, 'USER_ADDRESS') &&
    !contains(normalized, 'USER_GROUP') &&
    !contains(normalized, 'USER_ROLE') &&
    !contains(normalized, 'USER_SECURIT') &&
    !contains(normalized, 'USER_PREFERENCES') &&
    !contains(normalized, 'USER_TYPE')
  ) {
    key = 'user-management.users';
  } else if (contains(normalized, 'ADDRESS') && !contains(normalized, 'USER_ADDRESS')) {
    key = 'locations.address';
  } else if (contains(normalized, 'ADMINISTRATIVE_LEVEL')) {
    key = 'locations.administrative-level';
  } else if (entity(normalized, 'CITY', 'CITIES')) {
    key = 'locations.city';
  } else if (entity(normalized, 'COUNTRY', 'COUNTRIES')) {
    key = 'locations.country';
  } else if (entity(normalized, 'DISTRICT', 'DISTRICTS')) {
    key = 'locations.district';
  } else if (contains(normalized, 'GEO_COORDINATES')) {
    key = 'locations.geo-coordinates';
  } else if (entity(normalized, 'LANGUAGE', 'LANGUAGES')) {
    key = 'locations.language';
  } else if (contains(normalized, 'LOCALIZED_NAME')) {
    key = 'locations.localized-name';
  } else if (contains(normalized, 'LOCATION_NODE')) {
    key = 'locations.location-node';
  } else if (entity(normalized, 'PROVINCE', 'PROVINCES')) {
    key = 'locations.province';
  } else if (entity(normalized, 'SUBURB', 'SUBURBS')) {
    key = 'locations.suburb';
  } else if (entity(normalized, 'VILLAGE', 'VILLAGES')) {
    key = 'locations.village';
  }

  const meta = MODULE_META[key];
  return { key, ...meta };
}

export interface LdmsRoleModuleGroup<T> {
  section: LdmsRoleModuleSection;
  roles: T[];
}

/** Groups rows by module; preserves module sort order, roles sorted by code within each section. */
export function groupRolesByModule<T extends { role: string }>(
  rows: T[],
  moduleKeyOf: (row: T) => LdmsRoleModuleKey = (row) => resolveLdmsRoleModule(row.role).key,
): LdmsRoleModuleGroup<T>[] {
  const buckets = new Map<LdmsRoleModuleKey, T[]>();
  for (const row of rows) {
    const key = moduleKeyOf(row);
    const list = buckets.get(key) ?? [];
    list.push(row);
    buckets.set(key, list);
  }
  return [...buckets.entries()]
    .map(([key, roles]) => ({
      section: { key, ...MODULE_META[key] },
      roles: [...roles].sort((a, b) => a.role.localeCompare(b.role, undefined, { sensitivity: 'base' })),
    }))
    .sort((a, b) => a.section.sortOrder - b.section.sortOrder || a.section.label.localeCompare(b.section.label));
}

/** Reads module metadata from API when present, otherwise derives from role code. */
export function moduleSectionFromApi(
  role: string,
  moduleKey?: string | null,
  moduleLabel?: string | null,
): LdmsRoleModuleSection {
  const derived = resolveLdmsRoleModule(role);
  if (!moduleKey?.trim()) {
    return derived;
  }
  const key = moduleKey.trim() as LdmsRoleModuleKey;
  const meta = MODULE_META[key];
  if (!meta) {
    return derived;
  }
  return {
    key,
    label: moduleLabel?.trim() || meta.label,
    icon: meta.icon,
    sortOrder: meta.sortOrder,
  };
}
