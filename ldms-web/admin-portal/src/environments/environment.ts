export const environment = {
  production: false,
  apiUrl: '/api',
  useMocks: false,
  // Temporary dev bypass for authentication integration.
  // Keeps login local/mock while locations continue using real backend endpoints.
  authUseMocks: true,
  // 'system'   -> hits /<service>/v1/system/...   (no auth required, dev convenience)
  // 'frontend' -> hits /<service>/v1/frontend/... (requires JWT auth, prod default)
  apiSurface: 'system' as 'system' | 'frontend',
} as const;

export type Environment = typeof environment;
