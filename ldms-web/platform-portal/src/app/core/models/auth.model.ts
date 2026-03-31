export type OrganizationClassification =
  | 'SUPPLIER'
  | 'CUSTOMER'
  | 'TRANSPORT_COMPANY'
  | 'CLEARING_AGENT'
  | 'SERVICE_STATION'
  | 'ROADSIDE_SUPPORT_SERVICE'
  | 'GOVERNMENT_AGENCY';

export interface CurrentUser {
  userId: string;
  orgClassification: OrganizationClassification;
  organizationId: string;
  orgName: string;
  roles: string[];
  email?: string;
}
