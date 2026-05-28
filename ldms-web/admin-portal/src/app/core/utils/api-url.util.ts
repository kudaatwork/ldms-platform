import { environment } from '../../../environments/environment';

/**
 * API base URL. Empty string = relative {@code /ldms-*} paths; dev-server proxy forwards to {@code :8091} (no CORS).
 * Set {@code LDMS_API_GATEWAY_URL} to {@code http://localhost:8091} only for non-browser tooling.
 */
export function apiBaseUrl(): string {
  return (environment.gatewayUrl ?? environment.apiUrl).replace(/\/$/, '');
}

/** Absolute or same-origin URL for an LDMS path segment (must start with `/ldms-`). */
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

/** True when the request targets the API gateway (relative {@code /ldms-*} or absolute gateway URL). */
export function isLdmsApiRequest(url: string): boolean {
  if (url.startsWith('/ldms-') || url.startsWith('/api')) {
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
