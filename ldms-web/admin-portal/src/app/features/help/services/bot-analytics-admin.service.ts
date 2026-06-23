import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, TimeoutError, catchError, map, shareReplay, throwError, timeout } from 'rxjs';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import { apiEnvelopeOk, mapBotAdminApiError } from './bot-admin-api.util';

const API_TIMEOUT_MS = 15_000;

export interface BotAnalyticsSummary {
  totalSessions: number;
  sessionsToday: number;
  sessionsLast7Days: number;
  totalMessages: number;
  userMessages: number;
  botMessages: number;
  averageMessagesPerSession: number;
  averageSatisfactionScore?: number | null;
  ratedSessionCount: number;
  publishedFaqCount: number;
  sessionsByDay: { date: string; count: number }[];
  messagesByDay: { date: string; userMessages: number; botMessages: number }[];
  topTopics: { topic: string; count: number }[];
  channelBreakdown: { channel: string; count: number }[];
}

interface BotAnalyticsApiResponse {
  success?: boolean;
  isSuccess?: boolean;
  statusCode?: number;
  message?: string;
  analyticsSummary?: BotAnalyticsSummary;
}

@Injectable({ providedIn: 'root' })
export class BotAnalyticsAdminService {
  private readonly base = ldmsServiceUrl('messaging-inbound', 'bot-analytics', undefined, 'backoffice');
  private readonly cache = new Map<number, Observable<BotAnalyticsSummary>>();

  constructor(private readonly http: HttpClient) {}

  getSummary(days = 30, forceRefresh = false): Observable<BotAnalyticsSummary> {
    if (!forceRefresh && this.cache.has(days)) {
      return this.cache.get(days)!;
    }

    const request$ = this.http
      .get<BotAnalyticsApiResponse>(`${this.base}/summary`, { params: { days } })
      .pipe(
        timeout(API_TIMEOUT_MS),
        map((resp) => {
          if (!apiEnvelopeOk(resp) || !resp.analyticsSummary) {
            throw new Error(resp.message ?? 'Could not load bot analytics.');
          }
          return normalizeAnalyticsSummary(resp.analyticsSummary);
        }),
        catchError((err) => {
          this.cache.delete(days);
          if (err instanceof TimeoutError) {
            return throwError(
              () =>
                new Error(
                  'Bot analytics timed out. Start ldms-messaging-inbound (8095) and the API gateway (8091), then retry.',
                ),
            );
          }
          return throwError(() => mapBotAdminApiError(err, 'Could not load bot analytics.'));
        }),
        shareReplay({ bufferSize: 1, refCount: false }),
      );

    this.cache.set(days, request$);
    return request$;
  }
}

function normalizeAnalyticsSummary(raw: BotAnalyticsSummary): BotAnalyticsSummary {
  return {
    ...raw,
    averageMessagesPerSession: toNumber(raw.averageMessagesPerSession),
    averageSatisfactionScore:
      raw.averageSatisfactionScore == null ? null : toNumber(raw.averageSatisfactionScore),
    sessionsByDay: raw.sessionsByDay ?? [],
    messagesByDay: raw.messagesByDay ?? [],
    topTopics: raw.topTopics ?? [],
    channelBreakdown: raw.channelBreakdown ?? [],
  };
}

function toNumber(value: number | string | null | undefined): number {
  if (value == null || value === '') {
    return 0;
  }
  const parsed = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}
