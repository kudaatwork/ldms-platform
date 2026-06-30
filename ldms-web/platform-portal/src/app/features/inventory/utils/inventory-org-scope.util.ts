import type { OrganizationClassification } from '../../../core/models/auth.model';

/** Rows owned by an organisation (inventory uses supplierId as owning org id). */
export function rowOrganizationId(row: {
  supplierId?: number;
  organizationId?: number;
}): number {
  const ownerId = Number(row.supplierId ?? row.organizationId ?? 0);
  return Number.isFinite(ownerId) && ownerId > 0 ? ownerId : 0;
}

/** Keep only rows belonging to the signed-in organisation (by owning org id). */
export function filterByOrganizationId<
  T extends {
    supplierId?: number;
    organizationId?: number;
    id?: number;
    organizationOwned?: boolean;
  },
>(rows: T[], organizationId: number | null | undefined): T[] {
  const orgId = Number(organizationId ?? 0);
  if (!Number.isFinite(orgId) || orgId <= 0) {
    return [];
  }
  return rows.filter((row) => {
    if (Number(row.id ?? 0) <= 0) {
      return false;
    }
    if (rowOrganizationId(row) === orgId) {
      return true;
    }
    return row.organizationOwned === true;
  });
}

/** Keep rows visible to the signed-in organisation (owned or explicitly shared). */
export function filterByOrganizationScope<
  T extends {
    supplierId?: number;
    organizationId?: number;
    id?: number;
    organizationOwned?: boolean;
  },
>(
  rows: T[],
  organizationId: number | null | undefined,
  _classification: OrganizationClassification | null | undefined,
): T[] {
  return filterByOrganizationId(rows, organizationId);
}

/** Default warehouse location type for the organisation classification. */
export function defaultWarehouseTypeForClassification(
  classification: OrganizationClassification | null | undefined,
): 'SUPPLIER' | 'CUSTOMER' {
  return classification === 'CUSTOMER' ? 'CUSTOMER' : 'SUPPLIER';
}
