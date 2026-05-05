/** One row in the Requests log (HTTP audit) grid — mirrors fields shown in the table. */
export interface RequestLogRow {
  id: number;
  action: string;
  eventType: string;
  username: string;
  serviceName: string;
  method: string;
  statusCode: number | null;
  requestUrl: string;
  clientIpAddress: string;
  traceId: string;
  responseTimeMs: number;
  time: string;
  exceptionMessage: string;
  /** Raw request timestamp from API (when present), for detail view fallback */
  requestTimestamp?: string;
  responseTimestamp?: string;
  requestHeaders?: string | null;
}
