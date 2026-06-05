import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError, timeout, TimeoutError } from 'rxjs';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';

const SNAPSHOT_TIMEOUT_MS = 15_000;

export type PlatformOverallStatus = 'OPERATIONAL' | 'DEGRADED' | 'OUTAGE';

export interface PlatformHealthSummary {
  totalServices: number;
  upCount: number;
  downCount: number;
  unknownCount: number;
}

export interface ServiceHealthSnapshot {
  serviceId: string;
  displayName: string;
  eurekaServiceName?: string;
  host: string;
  port: number;
  managementPortUsed: boolean;
  status: string;
  latencyMs: number;
  message?: string;
  components: Record<string, string>;
}

export interface InfrastructureHealth {
  component: string;
  status: string;
  servicesReporting: number;
  servicesUp: number;
  servicesDown: number;
}

export interface EurekaHealthSummary {
  available: boolean;
  registeredServiceCount: number;
  instanceCount: number;
  services: { serviceName: string; instanceCount: number }[];
}

export interface PlatformHealthSnapshot {
  checkedAt: string;
  overallStatus: PlatformOverallStatus;
  summary: PlatformHealthSummary;
  services: ServiceHealthSnapshot[];
  infrastructure: InfrastructureHealth[];
  eureka?: EurekaHealthSummary;
}

interface PlatformHealthApiResponse {
  isSuccess?: boolean;
  success?: boolean;
  statusCode?: number;
  message?: string;
  checkedAt?: string;
  overallStatus?: PlatformOverallStatus;
  summary?: PlatformHealthSummary;
  services?: ServiceHealthSnapshot[];
  infrastructure?: InfrastructureHealth[];
  eureka?: EurekaHealthSummary;
}

/** Maps gateway / service HTTP failures into operator-friendly copy for the health console. */
export function mapPlatformHealthHttpError(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    const body = err.error;
    const path =
      typeof body === 'object' && body != null && 'path' in body && typeof body.path === 'string'
        ? body.path
        : err.url ?? '';

    if (err.status === 404) {
      return (
        `Platform health API returned HTTP 404 (Not Found)${path ? ` for ${path}` : ''}. ` +
        'Restart ldms-api-gateway (8091) so the snapshot endpoint is registered, then retry.'
      );
    }
    if (err.status === 401 || err.status === 403) {
      return 'Not authorized to read platform health. Sign in with an ADMIN or READ_ONLY account.';
    }
    if (err.status === 0) {
      return 'Cannot reach the API gateway. Confirm ldms-api-gateway (8091) and ldms-user-management (8086) are running.';
    }
    const serverMessage =
      typeof body === 'object' && body != null && 'message' in body && typeof body.message === 'string'
        ? body.message
        : typeof body === 'object' && body != null && 'error' in body && typeof body.error === 'string'
          ? body.error
          : '';
    if (serverMessage) {
      return `Platform health request failed (HTTP ${err.status}): ${serverMessage}`;
    }
    return err.message || `Platform health request failed (HTTP ${err.status}).`;
  }
  if (err instanceof Error && err.message) {
    return err.message;
  }
  return 'Failed to load platform health snapshot.';
}

@Injectable({ providedIn: 'root' })
export class PlatformHealthAdminService {
  constructor(private readonly http: HttpClient) {}

  fetchSnapshot(): Observable<PlatformHealthSnapshot> {
    const url = ldmsServiceUrl('api-gateway', 'platform-health', 'snapshot', 'backoffice');
    return this.http.get<PlatformHealthApiResponse>(url).pipe(
      timeout(SNAPSHOT_TIMEOUT_MS),
      map((resp) => this.mapSnapshot(resp)),
      catchError((err) => {
        if (err instanceof TimeoutError) {
          return throwError(
            () =>
              new Error(
                'Platform health probe timed out. Some services may be slow to respond — retry, or confirm ldms-api-gateway (8091) is running.',
              ),
          );
        }
        return throwError(() => new Error(mapPlatformHealthHttpError(err)));
      }),
    );
  }

  private mapSnapshot(resp: PlatformHealthApiResponse): PlatformHealthSnapshot {
    const ok =
      resp.isSuccess === true ||
      resp.success === true ||
      (resp.statusCode != null && resp.statusCode >= 200 && resp.statusCode < 300);
    if (!ok && !resp.services?.length) {
      throw new Error(resp.message ?? 'Platform health snapshot was not successful');
    }
    return {
      checkedAt: resp.checkedAt ?? new Date().toISOString(),
      overallStatus: resp.overallStatus ?? 'DEGRADED',
      summary: resp.summary ?? { totalServices: 0, upCount: 0, downCount: 0, unknownCount: 0 },
      services: (resp.services ?? []).map((s) => ({
        ...s,
        components: s.components ?? {},
      })),
      infrastructure: resp.infrastructure ?? [],
      eureka: resp.eureka,
    };
  }
}
