/** Response from {@code POST /ldms-authentication/v1/auth/*}. */
export interface AuthTokenResponse {
  success?: boolean;
  isSuccess?: boolean;
  statusCode?: number;
  message?: string;
  errorMessages?: string[];
  accessToken?: string;
  refreshToken?: string;
  token?: string;
  mustChangeCredentials?: boolean;
}

export function isAuthSuccess(res: AuthTokenResponse): boolean {
  if (res.success === true || res.isSuccess === true) {
    return true;
  }
  return res.statusCode != null && res.statusCode >= 200 && res.statusCode < 300;
}

export function extractAccessToken(res: AuthTokenResponse): string {
  const raw = res as AuthTokenResponse & {
    access_token?: string;
    authDto?: { accessToken?: string; access_token?: string };
    data?: { accessToken?: string; access_token?: string };
  };
  const nested = raw.authDto?.accessToken ?? raw.authDto?.access_token;
  const data = raw.data?.accessToken ?? raw.data?.access_token;
  return (raw.accessToken ?? raw.access_token ?? nested ?? data ?? raw.token ?? '').trim();
}
