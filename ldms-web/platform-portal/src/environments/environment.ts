/**
 * Local dev: empty = same-origin {@code /ldms-*} proxied to API gateway ({@code :8091}), no CORS.
 */
export const LDMS_API_GATEWAY_URL = '';

export const environment = {
  production: false,
  apiUrl: LDMS_API_GATEWAY_URL,
  gatewayUrl: LDMS_API_GATEWAY_URL,
  useMocks: false,
  apiSurface: 'frontend' as 'system' | 'frontend',
  googleOAuthClientId: "",
  platformPortalOrigin: "http://localhost:4201",
  adminPortalOrigin: "http://localhost:4200",
} as const;
