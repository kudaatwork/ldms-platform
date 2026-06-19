import type { OrganizationClassification } from '../../../core/models/auth.model';

const ADMIN_ROLES = new Set(['ORGANIZATION_ADMINISTRATOR', 'ADMIN']);

export function hasOrgRole(roles: readonly string[], required: string): boolean {
  const normalized = new Set(roles.map((r) => r.trim().toUpperCase()));
  if ([...normalized].some((r) => ADMIN_ROLES.has(r))) {
    return true;
  }
  return normalized.has(required.trim().toUpperCase());
}

export function hasAnyOrgRole(roles: readonly string[], required: readonly string[]): boolean {
  return required.some((role) => hasOrgRole(roles, role));
}

export function canEditOrganizationProfile(roles: readonly string[]): boolean {
  return hasOrgRole(roles, 'UPDATE_MY_ORGAN');
}

export function canManageBranches(roles: readonly string[]): boolean {
  return hasOrgRole(roles, 'MANAGE_BRANCHES');
}

export function canManageGroupRoles(roles: readonly string[]): boolean {
  return hasAnyOrgRole(roles, ['ASSIGN_USER_ROLES_TO_USER_GROUP', 'CREATE_USER_GROUP', 'UPDATE_USER_GROUP']);
}

export function canManageOperationalSettings(roles: readonly string[]): boolean {
  return hasOrgRole(roles, 'UPDATE_MY_ORGAN');
}

export function canManageProcurementSettings(roles: readonly string[]): boolean {
  return hasOrgRole(roles, 'UPDATE_MY_ORGAN');
}

export function canManageBillingSettings(classification?: OrganizationClassification): boolean {
  return (
    classification === 'SUPPLIER' ||
    classification === 'TRANSPORT_COMPANY' ||
    classification === 'CLEARING_AGENT'
  );
}

export function canManageKycApproverSettings(classification?: OrganizationClassification): boolean {
  return classification === 'SUPPLIER' || classification === 'CUSTOMER';
}
