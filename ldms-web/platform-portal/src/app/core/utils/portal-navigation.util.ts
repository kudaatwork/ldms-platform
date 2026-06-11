import type { CurrentUser, OrganizationClassification } from '../models/auth.model';
import { NAV_CONFIG } from '../../layout/sidebar/sidebar.config';
import { effectiveTradingMode, type TradingWorkspaceMode } from './org-classification.util';

/** Default signed-in landing route — dashboard adapts KPIs and layout per classification. */
export function portalHomeRoute(
  user: Pick<CurrentUser, 'orgClassification' | 'duplexMode'> | null | undefined,
  activeTradingMode?: TradingWorkspaceMode | null,
): string[] {
  const classification = effectiveTradingMode(
    user?.orgClassification,
    user?.duplexMode,
    activeTradingMode ?? null,
  );
  if (!classification) {
    return ['/dashboard'];
  }
  const nav = NAV_CONFIG[classification];
  return nav?.[0]?.route ? [nav[0].route] : ['/dashboard'];
}

export function formatClassificationLabel(raw: OrganizationClassification | string | null | undefined): string {
  if (!raw) {
    return '';
  }
  return raw
    .split('_')
    .map((w) => w.charAt(0) + w.slice(1).toLowerCase())
    .join(' ');
}
