/**
 * Local dev: empty = same-origin {@code /ldms-*} on the dev server; {@code proxy.conf.json} forwards to
 * the API gateway ({@code :8091}) with no browser CORS. Do not set {@code http://localhost:8091} here —
 * that triggers cross-origin calls from {@code :4200} and CORS failures when gateway headers are stripped.
 */
export const LDMS_API_GATEWAY_URL = '';

export const environment = {
  production: false,
  apiUrl: LDMS_API_GATEWAY_URL,
  gatewayUrl: LDMS_API_GATEWAY_URL,
  useMocks: false,
  authUseMocks: false,
  /**
   * Default LDMS surface for platform self-service. Admin portal modules use {@code backoffice}
   * explicitly in their admin services (organizations, users, locations, notifications, audit).
   */
  apiSurface: 'frontend' as 'system' | 'frontend' | 'backoffice',
  googleAutocompleteEnabled: false,
  googlePlacesApiKey: '',
  googleOAuthClientId: '',
} as const;

export type Environment = typeof environment;
