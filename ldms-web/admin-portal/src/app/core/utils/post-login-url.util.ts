/** Resolves a safe in-app path after sign-in (never returns `/` or auth routes). */
export function resolveAdminPostLoginUrl(returnUrl: string | null | undefined): string {
  const raw = (returnUrl ?? '').trim();
  if (!raw || raw === '/' || raw === '/auth' || raw.startsWith('/auth/')) {
    return '/dashboard';
  }
  if (!raw.startsWith('/')) {
    return '/dashboard';
  }
  const path = raw.split('?')[0].split('#')[0];
  if (!path || path === '/' || path.startsWith('/auth')) {
    return '/dashboard';
  }
  return path;
}
