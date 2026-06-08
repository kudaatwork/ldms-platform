const STORAGE_KEY_PREFIX = 'lx.platform.userTypes.unlocked.';

/** Whether the organisation has expanded beyond the first-login bootstrap user type list. */
export function isUserTypeCatalogUnlocked(orgId: number | null | undefined): boolean {
  if (orgId == null || orgId <= 0) {
    return true;
  }
  try {
    return localStorage.getItem(`${STORAGE_KEY_PREFIX}${orgId}`) === 'true';
  } catch {
    return false;
  }
}

/** Call after the workspace admin creates their first custom user type. */
export function unlockUserTypeCatalog(orgId: number | null | undefined): void {
  if (orgId == null || orgId <= 0) {
    return;
  }
  try {
    localStorage.setItem(`${STORAGE_KEY_PREFIX}${orgId}`, 'true');
  } catch {
    // best effort only
  }
}

/** First-login bootstrap: only {@code System Administrator} until unlocked. */
export function shouldBootstrapUserTypes(orgId: number | null | undefined): boolean {
  return !isUserTypeCatalogUnlocked(orgId);
}
