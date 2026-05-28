export interface IndustryUsageRow {
  id: number;
  name: string;
  industryCode: string;
  description: string;
  regulatoryBodyName: string;
  regulatoryBodyContactInfo: string;
  complianceRequirements: string;
  active: boolean;
  /** Display chip (aligned with organisations KYC table). */
  statusLabel: string;
  statusCss: string;
  organizationCount: number;
  verifiedOrganizationCount: number;
  linkedOrganizationNames: string[];
}

export interface BranchListRow {
  id: number;
  branchName: string;
  branchCode: string;
  organizationId: number;
  organizationName: string;
  region: string;
  email: string;
  phoneNumber: string;
  headOffice: boolean;
  active: boolean;
  businessHours?: string;
  locationId?: number | null;
}

export interface AgentListRow {
  id: number;
  firstName?: string;
  lastName?: string;
  displayName: string;
  organizationId: number;
  organizationName: string;
  agentKind: string;
  agentType: string;
  role: string;
  email: string;
  phoneNumber: string;
  active: boolean;
  branchId?: number | null;
  locationId?: number | null;
  assignedRegion?: string;
}

export interface PagedBranches {
  rows: BranchListRow[];
  totalElements: number;
}

export interface PagedAgents {
  rows: AgentListRow[];
  totalElements: number;
}

export interface PagedIndustries {
  rows: IndustryUsageRow[];
  totalElements: number;
}
