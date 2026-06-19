import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import { apiEnvelopeOk, mapBotAdminApiError } from './bot-admin-api.util';
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

@Injectable({ providedIn: 'root' })
export class BotServiceAdminService {
  private readonly base = ldmsServiceUrl('messaging-inbound', 'bot-session', undefined, 'backoffice');

  constructor(private readonly http: HttpClient) {}

  listSessions(): Observable<BotConversationSession[]> {
    return this.http.get<BotSessionApiResponse>(`${this.base}/list`).pipe(
      map((resp) => {
        if (!apiEnvelopeOk(resp)) {
          throw new Error(resp.message ?? 'Could not load bot conversations.');
        }
        return (resp.botSessionDtoList ?? []).map(normalizeSession);
      }),
      catchError((err) => throwError(() => mapBotAdminApiError(err, 'Could not load bot conversations.'))),
    );
  }

  findSession(sessionId: string): Observable<BotConversationSession | null> {
    return this.http
      .get<BotSessionApiResponse>(`${this.base}/find-by-id/${encodeURIComponent(sessionId)}`)
      .pipe(
        map((resp) => {
          if (!apiEnvelopeOk(resp) || !resp.botSessionDto) {
            return null;
          }
          return normalizeSession(resp.botSessionDto);
        }),
        catchError((err) => throwError(() => mapBotAdminApiError(err, 'Could not load bot conversation.'))),
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
