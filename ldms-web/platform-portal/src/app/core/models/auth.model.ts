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
  /** Resolved from GET /organization/my when absent from JWT. */
  orgClassification?: OrganizationClassification;
  /** When true, org may use both supplier and customer workspaces (primary classification unchanged). */
  duplexMode?: boolean;
  organizationId: string;
  orgName: string;
  roles: string[];
  /** User group or user type name for display — not permission codes. */
  roleLabel?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  displayName?: string;
  welcomeMessage?: string;
  mustChangeCredentials?: boolean;
  /** Organisation user flagged to approve procurement workflow stages. */
  procurementApprover?: boolean;
  /** Organisation user flagged to allocate fleet to shipments. */
  shipmentFleetAllocator?: boolean;
  /** Organisation workspace administrator (Administrator group). */
  organizationWorkspaceAdministrator?: boolean;
}
