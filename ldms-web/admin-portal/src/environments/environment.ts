export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  useMocks: false,
  // 'system'   -> hits /api/v1/system/...   (no auth required, dev convenience)
  // 'frontend' -> hits /api/v1/frontend/... (requires JWT auth, prod default)
  apiSurface: 'system' as 'system' | 'frontend',
} as const;

export type Environment = typeof environment;
