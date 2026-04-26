export const environment = {
  production: true,
  apiUrl: 'https://api.projectlx.co.zw',
  useMocks: false,
  // 'system'   -> hits /api/v1/system/...   (no auth required, dev convenience)
  // 'frontend' -> hits /api/v1/frontend/... (requires JWT auth, prod default)
  apiSurface: 'frontend' as 'system' | 'frontend',
} as const;

export type Environment = typeof environment;
