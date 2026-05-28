/** Payload for create/update agent API calls. */
export interface AgentPayload {
  organizationId: number;
  agentKind: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  phoneNumber?: string;
  agentType?: string;
  role?: string;
  branchId?: number;
  locationId?: number;
  assignedRegion?: string;
  active?: boolean;
}
