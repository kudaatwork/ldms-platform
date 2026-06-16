import type { BranchDetail } from '../../../core/services/organization.service';

export interface AgentRow {
  id: number;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  phoneNumber: string;
  agentKind: 'INDIVIDUAL' | 'ORGANIZATION';
  agentType?: string;
  role?: string;
  branchId?: number;
  assignedRegion?: string;
  active: boolean;
}

export interface CreateAgentPayload {
  agentKind: 'INDIVIDUAL' | 'ORGANIZATION';
  firstName: string;
  lastName: string;
  email?: string;
  phoneNumber?: string;
  agentType?: string;
  role?: string;
  branchId?: number;
  assignedRegion?: string;
  active?: boolean;
}

export interface TransporterOrgRow {
  id: number;
  name: string;
  email: string;
  phoneNumber: string;
  verified: boolean;
  kycStatus: string;
}

export type BranchListScope = 'top-level' | 'sub-level';

export interface BranchTableQuery {
  page: number;
  size: number;
  searchQuery: string;
  branchName?: string;
  region?: string;
  branchLevel?: 'BRANCH' | 'SUB_BRANCH';
  depot?: boolean | '';
  parentBranchId?: number | '';
  active?: boolean | '';
}

export interface PagedBranches {
  rows: BranchDetail[];
  totalElements: number;
}

export interface ImportActionResponse {
  ok: boolean;
  message?: string;
}
