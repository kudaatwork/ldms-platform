/** Payload for create/update industry API calls. */
export interface IndustryPayload {
  name: string;
  industryCode?: string;
  description?: string;
  regulatoryBodyName?: string;
  regulatoryBodyContactInfo?: string;
  complianceRequirements?: string;
  active?: boolean;
}
