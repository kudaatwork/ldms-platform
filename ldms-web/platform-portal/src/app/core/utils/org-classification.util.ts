import type { CurrentUser, OrganizationClassification } from '../models/auth.model';

export type TradingWorkspaceMode = 'SUPPLIER' | 'CUSTOMER';

/** Workspace hat in use for duplex orgs; otherwise the primary classification. */
export function effectiveTradingMode(
  classification: OrganizationClassification | null | undefined,
  duplexMode: boolean | null | undefined,
  activeMode: TradingWorkspaceMode | null | undefined,
): OrganizationClassification | null {
  if (!classification) {
    return null;
  }
  if (!duplexMode || (classification !== 'SUPPLIER' && classification !== 'CUSTOMER')) {
    return classification;
  }
  return activeMode ?? classification;
}

/** Catalogue management (add product, inventory workspace) is supplier-only. */
export function isSupplierOrganization(
  classification: OrganizationClassification | null | undefined,
  duplexMode?: boolean | null,
  activeMode?: TradingWorkspaceMode | null,
): boolean {
  return effectiveTradingMode(classification, duplexMode, activeMode) === 'SUPPLIER';
}

/** Customer orgs order stock via My Orders — they do not maintain supplier catalogues. */
export function isCustomerOrganization(
  classification: OrganizationClassification | null | undefined,
  duplexMode?: boolean | null,
  activeMode?: TradingWorkspaceMode | null,
): boolean {
  return effectiveTradingMode(classification, duplexMode, activeMode) === 'CUSTOMER';
}

export function isDuplexTradingOrg(user: Pick<CurrentUser, 'duplexMode' | 'orgClassification'> | null | undefined): boolean {
  if (!user?.duplexMode) {
    return false;
  }
  const c = user.orgClassification;
  return c === 'SUPPLIER' || c === 'CUSTOMER';
}
