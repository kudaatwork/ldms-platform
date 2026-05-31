export type OrganizationClassification =
  | 'SUPPLIER'
  | 'CUSTOMER'
  | 'TRANSPORT_COMPANY'
  | 'CLEARING_AGENT'
  | 'SERVICE_STATION'
  | 'ROADSIDE_SUPPORT_SERVICE'
  | 'GOVERNMENT_AGENCY';

/** Legal entity type — aligns with backend {@code OrganizationType}. */
export type OrganizationType =
  | 'PRIVATE'
  | 'GOVERNMENT'
  | 'NGO'
  | 'NON_PROFIT'
  | 'PUBLIC'
  | 'COOPERATIVE'
  | 'OTHER';

export interface OrgTypeOption {
  slug: OrganizationType;
  label: string;
}

export const ORG_TYPES: readonly OrgTypeOption[] = [
  { slug: 'PRIVATE', label: 'Private company' },
  { slug: 'GOVERNMENT', label: 'Government' },
  { slug: 'NGO', label: 'NGO' },
  { slug: 'NON_PROFIT', label: 'Non-profit' },
  { slug: 'PUBLIC', label: 'Public entity' },
  { slug: 'COOPERATIVE', label: 'Cooperative' },
  { slug: 'OTHER', label: 'Other' },
] as const;

export interface CurrentUser {
  userId: string;
  orgClassification: OrganizationClassification;
  organizationId: string;
  orgName: string;
  roles: string[];
  email?: string;
  firstName?: string;
  lastName?: string;
  displayName?: string;
  welcomeMessage?: string;
  mustChangeCredentials?: boolean;
}
