import { environment } from '../../../environments/environment';

/** Trimmed {@link environment.apiUrl} (API gateway base, no trailing slash). */
export function apiBaseUrl(): string {
  return environment.apiUrl.replace(/\/$/, '');
}

/** Absolute URL for an LDMS path segment (must start with `/ldms-` or `/api/`). */
export function ldmsApiUrl(path: string): string {
  const normalized = path.startsWith('/') ? path : `/${path}`;
  return `${apiBaseUrl()}${normalized}`;
}

/**
 * `{gateway}/ldms-{service}/v1/{surface}/{resource}[/{operation}]`
 * e.g. `ldmsServiceUrl('audit-trail', 'audit-log', 'find-by-multiple-filters')`.
 */
export function ldmsServiceUrl(
  service: string,
  resource: string,
  operation?: string,
  surface: 'system' | 'frontend' | 'backoffice' = environment.apiSurface,
): string {
  const prefix = service.startsWith('ldms-') ? service : `ldms-${service}`;
  let path = `/${prefix}/v1/${surface}/${resource}`;
  if (operation) {
    path += `/${operation}`;
  }
  return ldmsApiUrl(path);
}

/** True when the request targets the API gateway (or legacy `/api` dev proxy prefix). */
export function isLdmsApiRequest(url: string): boolean {
  if (url.startsWith('/api')) {
    return true;
  }
  const base = apiBaseUrl();
  if (!base) {
    return false;
  }
  if (url.startsWith(base)) {
    return true;
  }
  try {
    const requestOrigin = new URL(url, typeof window !== 'undefined' ? window.location.origin : 'http://localhost').origin;
    const gatewayOrigin = new URL(base, typeof window !== 'undefined' ? window.location.origin : 'http://localhost').origin;
    return requestOrigin === gatewayOrigin;
  } catch {
    return false;
  }
}
