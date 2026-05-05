export const environment = {
  production: false,
  apiUrl: "http://localhost:8091",
  useMocks: true,
  /** Google Identity Services Web client ID; empty hides “Continue with Google”. */
  googleOAuthClientId: "",
  /** Public marketing / self URL (used on landing CTAs). */
  platformPortalOrigin: "http://localhost:4201",
  /** Admin console (no public signup). */
  adminPortalOrigin: "http://localhost:4200",
} as const;
