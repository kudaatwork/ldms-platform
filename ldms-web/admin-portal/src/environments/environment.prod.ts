export const environment = {
  production: true,
  apiUrl: 'https://api.projectlx.co.zw',
  useMocks: false,
} as const;

export type Environment = typeof environment;
