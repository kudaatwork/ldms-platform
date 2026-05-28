export const environment = {
  production: true,
  apiUrl: 'https://api.projectlx.co.zw',
  gatewayUrl: 'https://api.projectlx.co.zw',
  useMocks: false,
  authUseMocks: false,
  /** Platform self-service default; admin services use {@code backoffice} explicitly. */
  apiSurface: 'frontend' as 'system' | 'frontend' | 'backoffice',
  googleAutocompleteEnabled: false,
  googlePlacesApiKey: '',
  googleOAuthClientId: '',
} as const;

export type Environment = typeof environment;
