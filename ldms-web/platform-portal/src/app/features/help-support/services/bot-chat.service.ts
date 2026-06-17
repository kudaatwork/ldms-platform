import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';

export type BotMessageRole = 'user' | 'bot' | 'system';

export interface BotChatMessage {
  id: string;
  role: BotMessageRole;
  body: string;
  sentAt: string;
}

export interface BotChatSession {
  sessionId: string;
  userDisplayName: string;
  topic: string;
  status: string;
  statusLabel: string;
  lastMessageAt: string;
  messageCount: number;
  satisfactionScore?: number;
  messages?: BotChatMessage[];
}

interface BotSessionApiResponse {
  isSuccess?: boolean;
  success?: boolean;
  statusCode?: number;
  message?: string;
  botSessionDto?: BotChatSession;
  botSessionDtoList?: BotChatSession[];
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
        'LDMS assistant API not found. Start ldms-messaging-bot (8095) and apply gateway routes.',
      );
    }
    if (err.status === 0) {
      return new Error('Cannot reach the API gateway. Check ldms-api-gateway and ldms-messaging-bot are running.');
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
export class BotChatService {
  private readonly base = ldmsServiceUrl('messaging-inbound', 'bot-session');

  constructor(private readonly http: HttpClient) {}

  startSession(topic?: string): Observable<BotChatSession> {
    return this.http.post<BotSessionApiResponse>(`${this.base}/start`, { topic: topic ?? '' }).pipe(
      map((resp) => {
        if (!apiOk(resp) || !resp.botSessionDto) {
          throw new Error(resp.message ?? 'Could not start assistant chat.');
        }
        return resp.botSessionDto;
      }),
      catchError((err) => throwError(() => mapHttpError(err, 'Could not start assistant chat.'))),
    );
  }

  sendMessage(sessionId: string, body: string): Observable<BotChatSession> {
    return this.http
      .post<BotSessionApiResponse>(`${this.base}/send-message`, { sessionId, body })
      .pipe(
        map((resp) => {
          if (!apiOk(resp) || !resp.botSessionDto) {
            throw new Error(resp.message ?? 'Could not send message.');
          }
          return resp.botSessionDto;
        }),
        catchError((err) => throwError(() => mapHttpError(err, 'Could not send message.'))),
      );
  }

  fetchMySessions(): Observable<BotChatSession[]> {
    return this.http.get<BotSessionApiResponse>(`${this.base}/my-sessions`).pipe(
      map((resp) => {
        if (!apiOk(resp)) {
          throw new Error(resp.message ?? 'Could not load assistant chats.');
        }
        return resp.botSessionDtoList ?? [];
      }),
      catchError((err) => throwError(() => mapHttpError(err, 'Could not load assistant chats.'))),
    );
  }

  fetchSession(sessionId: string): Observable<BotChatSession> {
    return this.http
      .get<BotSessionApiResponse>(`${this.base}/find-by-id/${encodeURIComponent(sessionId)}`)
      .pipe(
        map((resp) => {
          if (!apiOk(resp) || !resp.botSessionDto) {
            throw new Error(resp.message ?? 'Could not load chat.');
          }
          return resp.botSessionDto;
        }),
        catchError((err) => throwError(() => mapHttpError(err, 'Could not load chat.'))),
      );
  }

  rateSession(sessionId: string, score: number): Observable<BotChatSession> {
    return this.http
      .post<BotSessionApiResponse>(`${this.base}/rate`, { sessionId, score })
      .pipe(
        map((resp) => {
          if (!apiOk(resp) || !resp.botSessionDto) {
            throw new Error(resp.message ?? 'Could not save feedback.');
          }
          return resp.botSessionDto;
        }),
        catchError((err) => throwError(() => mapHttpError(err, 'Could not save feedback.'))),
      );
  }
}
