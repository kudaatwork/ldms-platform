import type { FleetComplianceRow, FleetComplianceSubjectType } from '../models/fleet.model';
import { isCompliancePendingReview } from '../services/fleet-portal.service';

export interface FleetComplianceSubjectBundle {
  subjectType: FleetComplianceSubjectType;
  subjectId: number;
  subjectLabel: string;
  records: FleetComplianceRow[];
  pendingRecords: FleetComplianceRow[];
  pendingCount: number;
}

export function complianceSubjectKey(subjectType: FleetComplianceSubjectType, subjectId: number): string {
  return `${subjectType}:${subjectId}`;
}

export function complianceSubjectKeyForRow(row: FleetComplianceRow): string {
  return complianceSubjectKey(row.subjectType, row.subjectId);
}

/** Groups compliance rows by subject; pending bundles are those with at least one PENDING record. */
export function groupComplianceBySubject(rows: FleetComplianceRow[]): FleetComplianceSubjectBundle[] {
  const map = new Map<string, FleetComplianceSubjectBundle>();

  for (const row of rows) {
    const key = complianceSubjectKeyForRow(row);
    let bundle = map.get(key);
    if (!bundle) {
      bundle = {
        subjectType: row.subjectType,
        subjectId: row.subjectId,
        subjectLabel: row.subjectLabel,
        records: [],
        pendingRecords: [],
        pendingCount: 0,
      };
      map.set(key, bundle);
    }
    bundle.records.push(row);
    if (isCompliancePendingReview(row.status)) {
      bundle.pendingRecords.push(row);
      bundle.pendingCount += 1;
    }
    if (row.subjectLabel && row.subjectLabel !== `#${row.subjectId}`) {
      bundle.subjectLabel = row.subjectLabel;
    }
  }

  return [...map.values()].sort((a, b) => {
    if (b.pendingCount !== a.pendingCount) {
      return b.pendingCount - a.pendingCount;
    }
    return a.subjectLabel.localeCompare(b.subjectLabel);
  });
}

export function pendingComplianceBundles(rows: FleetComplianceRow[]): FleetComplianceSubjectBundle[] {
  return groupComplianceBySubject(rows).filter((bundle) => bundle.pendingCount > 0);
}

export function findComplianceBundleForRow(
  rows: FleetComplianceRow[],
  row: FleetComplianceRow,
): FleetComplianceSubjectBundle | null {
  const key = complianceSubjectKeyForRow(row);
  return groupComplianceBySubject(rows).find((b) => complianceSubjectKey(b.subjectType, b.subjectId) === key) ?? null;
}
