import { HttpErrorResponse } from '@angular/common/http';

interface ApiEnvelope {
  message?: string;
  error?: string;
  path?: string;
  status?: number;
}

/**
 * Maps messaging-bot HTTP failures into actionable admin messages.
 */
export function mapBotAdminApiError(err: unknown, fallback: string): Error {
  if (err instanceof Error && !(err instanceof HttpErrorResponse)) {
    return err;
  }
  if (!(err instanceof HttpErrorResponse)) {
    return new Error(fallback);
  }

  const body = (err.error ?? {}) as ApiEnvelope;
  const path = body.path ?? err.url ?? '';

  if (err.status === 0) {
    return new Error(
      'Cannot reach the API gateway. Start ldms-api-gateway (8091) and ldms-messaging-inbound (8095), then refresh.',
    );
  }

  if (err.status === 401 || err.status === 403) {
    return new Error(body.message ?? body.error ?? 'Your session expired. Sign in again and retry.');
  }

  if (err.status === 404) {
    if (String(path).includes('bot-analytics') || String(path).includes('bot-faq')) {
      return new Error(
        'Bot analytics / FAQ API not found on ldms-messaging-inbound (8095). Restart that service after pulling latest code, then refresh.',
      );
    }
    return new Error(
      body.message ??
        body.error ??
        'Bot service API returned HTTP 404. Start ldms-messaging-inbound (8095) and confirm the gateway routes /ldms-messaging-inbound/**.',
    );
  }

  if (err.status === 503) {
    return new Error(body.message ?? 'Messaging bot service is unavailable. Start ldms-messaging-inbound on port 8095.');
  }

  return new Error(body.message ?? body.error ?? `${fallback} (HTTP ${err.status}).`);
}

export function apiEnvelopeOk(resp: {
  success?: boolean;
  isSuccess?: boolean;
  statusCode?: number;
}): boolean {
  return (
    resp.success === true ||
    resp.isSuccess === true ||
    (resp.statusCode != null && resp.statusCode >= 200 && resp.statusCode < 300)
  );
}
