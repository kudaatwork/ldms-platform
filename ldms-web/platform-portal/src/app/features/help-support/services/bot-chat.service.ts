import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, map, throwError } from 'rxjs';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import { stripLegacyBotNag } from '../utils/strip-legacy-bot-nag.util';

export type BotMessageRole = 'user' | 'bot' | 'system';
export type BotAssistantMode = 'ASSISTANT' | 'AGENT';

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
  assistantMode?: BotAssistantMode;
  assistantModeLabel?: string;
  lastMessageAt: string;
  messageCount: number;
  satisfactionScore?: number;
  messages?: BotChatMessage[];
}

export interface BotPricing {
  assistantMessageCents: number;
  agentMessageCents: number;
  sessionStartCents: number;
  supportTicketOpenCents: number;
  liveChatMessageCents: number;
  currencyCode: string;
}

interface BotSessionApiResponse {
  isSuccess?: boolean;
  success?: boolean;
  statusCode?: number;
  message?: string;
  botSessionDto?: BotChatSession;
  botSessionDtoList?: BotChatSession[];
  botPricingDto?: BotPricing;
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

function normalizeSession(session: BotChatSession): BotChatSession {
  if (!session.messages?.length) {
    return session;
  }
  return {
    ...session,
    messages: session.messages.map((m) =>
      m.role === 'bot' ? { ...m, body: stripLegacyBotNag(m.body) } : m,
    ),
  };
}

function normalizePricing(raw: BotPricing | undefined): BotPricing {
  return {
    assistantMessageCents: Number(raw?.assistantMessageCents ?? 0),
    agentMessageCents: Number(raw?.agentMessageCents ?? 0),
    sessionStartCents: Number(raw?.sessionStartCents ?? 0),
    supportTicketOpenCents: Number(raw?.supportTicketOpenCents ?? 0),
    liveChatMessageCents: Number(raw?.liveChatMessageCents ?? 0),
    currencyCode: String(raw?.currencyCode ?? 'USD').trim() || 'USD',
  };
}

@Injectable({ providedIn: 'root' })
export class BotChatService {
  private readonly base = ldmsServiceUrl('messaging-inbound', 'bot-session');

  constructor(private readonly http: HttpClient) {}

  fetchPricing(): Observable<BotPricing> {
    return this.http.get<BotSessionApiResponse>(`${this.base}/pricing`).pipe(
      map((resp) => {
        if (!apiOk(resp) || !resp.botPricingDto) {
          throw new Error(resp.message ?? 'Could not load Lexi pricing.');
        }
        return normalizePricing(resp.botPricingDto);
      }),
      catchError((err) => throwError(() => mapHttpError(err, 'Could not load Lexi pricing.'))),
    );
  }

  startSession(topic?: string, assistantMode?: BotAssistantMode): Observable<{ session: BotChatSession; pricing?: BotPricing }> {
    return this.http
      .post<BotSessionApiResponse>(`${this.base}/start`, {
        topic: topic ?? '',
        assistantMode: assistantMode ?? 'ASSISTANT',
      })
      .pipe(
        map((resp) => {
          if (!apiOk(resp) || !resp.botSessionDto) {
            throw new Error(resp.message ?? 'Could not start assistant chat.');
          }
          return {
            session: normalizeSession(resp.botSessionDto),
            pricing: resp.botPricingDto ? normalizePricing(resp.botPricingDto) : undefined,
          };
        }),
        catchError((err) => throwError(() => mapHttpError(err, 'Could not start assistant chat.'))),
      );
  }

  setAssistantMode(sessionId: string, assistantMode: BotAssistantMode): Observable<BotChatSession> {
    return this.http
      .post<BotSessionApiResponse>(`${this.base}/assistant-mode`, { sessionId, assistantMode })
      .pipe(
        map((resp) => {
          if (!apiOk(resp) || !resp.botSessionDto) {
            throw new Error(resp.message ?? 'Could not switch assistant mode.');
          }
          return normalizeSession(resp.botSessionDto);
        }),
        catchError((err) => throwError(() => mapHttpError(err, 'Could not switch assistant mode.'))),
      );
  }

  sendMessage(
    sessionId: string,
    body: string,
    assistantMode?: BotAssistantMode,
  ): Observable<BotChatSession> {
    return this.http
      .post<BotSessionApiResponse>(`${this.base}/send-message`, {
        sessionId,
        body,
        ...(assistantMode ? { assistantMode } : {}),
      })
      .pipe(
        map((resp) => {
          if (!apiOk(resp) || !resp.botSessionDto) {
            throw new Error(resp.message ?? 'Could not send message.');
          }
          return normalizeSession(resp.botSessionDto);
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
        return (resp.botSessionDtoList ?? []).map(normalizeSession);
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
          return normalizeSession(resp.botSessionDto);
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
          return normalizeSession(resp.botSessionDto);
        }),
        catchError((err) => throwError(() => mapHttpError(err, 'Could not save feedback.'))),
      );
  }
}
