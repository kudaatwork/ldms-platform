import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import {
  BotConversationSession,
  BotMessage,
  BotSessionStatus,
} from './bot-service-mock.data';

interface BotSessionApiResponse {
  isSuccess?: boolean;
  success?: boolean;
  statusCode?: number;
  message?: string;
  errorMessages?: string[];
  botSessionDto?: BotConversationSession;
  botSessionDtoList?: BotConversationSession[];
}

function apiOk(resp: BotSessionApiResponse): boolean {
  return (
    resp.isSuccess === true ||
    resp.success === true ||
    (resp.statusCode != null && resp.statusCode >= 200 && resp.statusCode < 300)
  );
}

function mapHttpError(err: unknown, fallback: string): Error {
  if (err instanceof HttpErrorResponse) {
    const body = err.error as BotSessionApiResponse | undefined;
    if (err.status === 404) {
      return new Error(
        'Bot service API returned HTTP 404. Start ldms-messaging-bot (8095) and ensure the API gateway routes /ldms-messaging-inbound/**.',
      );
    }
    if (err.status === 0) {
      return new Error('Cannot reach the API gateway. Confirm ldms-api-gateway (8091) and ldms-messaging-bot (8095) are running.');
    }
    if (body?.message) {
      return new Error(body.message);
    }
    return new Error(`${fallback} (HTTP ${err.status}).`);
  }
  if (err instanceof Error && err.message) {
    return err;
  }
  return new Error(fallback);
}

@Injectable({ providedIn: 'root' })
export class BotServiceAdminService {
  private readonly base = ldmsServiceUrl('messaging-inbound', 'bot-session', undefined, 'backoffice');

  constructor(private readonly http: HttpClient) {}

  listSessions(): Observable<BotConversationSession[]> {
    return this.http.get<BotSessionApiResponse>(`${this.base}/list`).pipe(
      map((resp) => {
        if (!apiOk(resp)) {
          throw new Error(resp.message ?? 'Could not load bot conversations.');
        }
        return (resp.botSessionDtoList ?? []).map(normalizeSession);
      }),
      catchError((err) => throwError(() => mapHttpError(err, 'Could not load bot conversations.'))),
    );
  }

  findSession(sessionId: string): Observable<BotConversationSession | null> {
    return this.http
      .get<BotSessionApiResponse>(`${this.base}/find-by-id/${encodeURIComponent(sessionId)}`)
      .pipe(
        map((resp) => {
          if (!apiOk(resp) || !resp.botSessionDto) {
            return null;
          }
          return normalizeSession(resp.botSessionDto);
        }),
        catchError((err) => throwError(() => mapHttpError(err, 'Could not load bot conversation.'))),
      );
  }

  countByStatus(status: BotSessionStatus): Observable<number> {
    return this.listSessions().pipe(map((rows) => rows.filter((r) => r.status === status).length));
  }
}

function normalizeSession(raw: BotConversationSession): BotConversationSession {
  return {
    ...raw,
    status: (raw.status as BotSessionStatus) ?? 'ACTIVE',
    statusLabel: raw.statusLabel ?? raw.status,
    userPhone: raw.userPhone ?? '',
    messages: (raw.messages ?? []).map((m) => normalizeMessage(m)),
  };
}

function normalizeMessage(raw: BotMessage): BotMessage {
  return {
    id: raw.id,
    role: raw.role,
    body: raw.body,
    sentAt: raw.sentAt,
  };
}
