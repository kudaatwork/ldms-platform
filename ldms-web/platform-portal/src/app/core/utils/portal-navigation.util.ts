import type { CurrentUser, OrganizationClassification } from '../models/auth.model';
import { NAV_CONFIG } from '../../layout/sidebar/sidebar.config';

/** Default signed-in landing route — dashboard adapts KPIs and layout per classification. */
export function portalHomeRoute(user: Pick<CurrentUser, 'orgClassification'> | null | undefined): string[] {
  if (!user?.orgClassification) {
    return ['/dashboard'];
  }
  const nav = NAV_CONFIG[user.orgClassification];
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
