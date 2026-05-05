export const environment = {
  production: false,
  /**
   * Dev: `/api` — same-origin via `ng serve` proxy (`proxy.conf.json` → gateway :8091), avoids browser CORS.
   * Prod: absolute gateway URL in `environment.prod.ts`.
   */
  apiUrl: '/api',
  useMocks: false,
  // Temporary dev bypass for authentication integration.
  // Keeps login local/mock while locations continue using real backend endpoints.
  authUseMocks: true,
  // 'system'   -> hits /<service>/v1/system/...   (no auth required, dev convenience)
  // 'frontend' -> hits /<service>/v1/frontend/... (requires JWT auth, prod default)
  apiSurface: 'system' as 'system' | 'frontend',
  googleAutocompleteEnabled: false,
  googlePlacesApiKey: '',
  /** Web client ID from Google Cloud Console; empty hides “Continue with Google” on sign-in. */
  googleOAuthClientId: '',
} as const;

export type Environment = typeof environment;
