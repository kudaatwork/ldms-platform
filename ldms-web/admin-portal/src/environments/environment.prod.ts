export const environment = {
  production: true,
  apiUrl: 'https://api.projectlx.co.zw',
  useMocks: false,
  authUseMocks: false,
  // 'system'   -> hits /<service>/v1/system/...   (no auth required, dev convenience)
  // 'frontend' -> hits /<service>/v1/frontend/... (requires JWT auth, prod default)
  apiSurface: 'frontend' as 'system' | 'frontend',
  googleAutocompleteEnabled: false,
  googlePlacesApiKey: '',
  googleOAuthClientId: '',
} as const;

export type Environment = typeof environment;
