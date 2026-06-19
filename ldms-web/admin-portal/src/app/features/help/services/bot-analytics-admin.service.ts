import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import { apiEnvelopeOk, mapBotAdminApiError } from './bot-admin-api.util';

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

  constructor(private readonly http: HttpClient) {}

  getSummary(days = 30): Observable<BotAnalyticsSummary> {
    return this.http.get<BotAnalyticsApiResponse>(`${this.base}/summary`, { params: { days } }).pipe(
      map((resp) => {
        if (!apiEnvelopeOk(resp) || !resp.analyticsSummary) {
          throw new Error(resp.message ?? 'Could not load bot analytics.');
        }
        return resp.analyticsSummary;
      }),
      catchError((err) => throwError(() => mapBotAdminApiError(err, 'Could not load bot analytics.'))),
    );
  }
}
