import type { AgentRow } from '../models/org-management.model';
import type { LxExportColumn } from '../../../shared/utils/lx-export.util';
import { downloadBlob } from '../../../shared/utils/lx-export.util';

export const AGENT_EXPORT_COLUMNS: LxExportColumn<AgentRow>[] = [
  { header: 'FIRST_NAME', value: (r) => r.firstName },
  { header: 'LAST_NAME', value: (r) => r.lastName },
  { header: 'AGENT_KIND', value: (r) => r.agentKind },
  { header: 'EMAIL', value: (r) => r.email },
  { header: 'PHONE_NUMBER', value: (r) => r.phoneNumber },
  { header: 'AGENT_TYPE', value: (r) => r.agentType ?? '' },
  { header: 'ROLE', value: (r) => r.role ?? '' },
  { header: 'ASSIGNED_REGION', value: (r) => r.assignedRegion ?? '' },
  { header: 'BRANCH_ID', value: (r) => (r.branchId != null ? String(r.branchId) : '') },
  { header: 'ACTIVE', value: (r) => (r.active ? 'true' : 'false') },
];

export const AGENT_SAMPLE_CSV = `AGENT KIND,FIRST NAME,LAST NAME,EMAIL,PHONE NUMBER,AGENT TYPE,ROLE,BRANCH ID,ACTIVE
INDIVIDUAL,Tendai,Moyo,tendai.moyo@example.co.zw,+263771234567,CLEARING,Border liaison,,true
ORGANIZATION,ClearCo,Representative,ops@clearco.co.zw,+263292345678,CUSTOMS,Clearing partner,,true
`;

export function downloadAgentSampleCsv(): void {
  downloadBlob(new Blob([AGENT_SAMPLE_CSV], { type: 'text/csv;charset=utf-8' }), 'agents-sample.csv');
}
