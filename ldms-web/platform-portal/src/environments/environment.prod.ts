export const environment = {
  production: true,
  apiUrl: "https://api.projectlx.co.zw",
  gatewayUrl: "https://api.projectlx.co.zw",
  useMocks: false,
  apiSurface: 'frontend' as 'system' | 'frontend',
  googleOAuthClientId: "",
  platformPortalOrigin: "https://portal.projectlx.co.zw",
  adminPortalOrigin: "https://admin.projectlx.co.zw",
  /** Replace with live store URLs when apps are published. */
  mobileApps: {
    appleAppStoreUrl: "https://apps.apple.com/app/project-lx-ldms",
    googlePlayStoreUrl: "https://play.google.com/store/apps/details?id=co.zw.projectlx.ldms",
  },
} as const;
