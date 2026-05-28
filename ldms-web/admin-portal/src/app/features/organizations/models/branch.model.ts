/** Payload for create/update branch API calls. */
export interface BranchPayload {
  organizationId: number;
  branchName: string;
  branchCode?: string;
  locationId?: number;
  phoneNumber?: string;
  email?: string;
  headOffice?: boolean;
  region?: string;
  businessHours?: string;
  active?: boolean;
}
