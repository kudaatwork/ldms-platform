/**
 * LDMS endpoints that do not require a Bearer token on the client.
 * Keep aligned with {@code SharedJwtSecurityConfig} and {@code JwtAuthenticationFilter#isPublicPath}.
 */
export function isPublicLdmsApiRequest(url: string): boolean {
  if (url.includes('/v1/system/') || url.includes('/v1/auth/')) {
    return true;
  }
  if (url.includes('/v1/frontend/organization/register')) {
    return true;
  }
  if (url.includes('/organization/onboarding-status')) {
    return true;
  }
  return false;
}
