import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, map, throwError } from 'rxjs';
import { ldmsServiceUrl } from '../../core/utils/api-url.util';
import type { BotChatSession } from '../../features/help-support/services/bot-chat.service';
import { stripLegacyBotNag } from '../../features/help-support/utils/strip-legacy-bot-nag.util';
import type { LexxiChatBrand } from './lexxi-chat-launcher.service';
import { GUEST_LIVE_CHAT_TOPIC, LEXXI_DEFAULT_TOPIC } from '../constants/lexxi-bot.constants';

const GUEST_LEXXI_SESSION_KEY = 'lx-guest-lexxi-session-id';
const GUEST_LIVE_CHAT_SESSION_KEY = 'lx-guest-live-chat-session-id';
const LEGACY_GUEST_SESSION_KEY = 'lx-lexi-guest-session-id';

interface BotSessionApiResponse {
  isSuccess?: boolean;
  success?: boolean;
  statusCode?: number;
  message?: string;
  botSessionDto?: BotChatSession;
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
    if (body?.message) {
      return new Error(body.message);
    }
    if (err.status === 0) {
      return new Error('Cannot reach the API gateway. Check your connection and try again.');
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

function storageKeyForBrand(brand: LexxiChatBrand): string {
  return brand === 'live-chat' ? GUEST_LIVE_CHAT_SESSION_KEY : GUEST_LEXXI_SESSION_KEY;
}

function topicForBrand(brand: LexxiChatBrand): string {
  return brand === 'live-chat' ? GUEST_LIVE_CHAT_TOPIC : LEXXI_DEFAULT_TOPIC;
}

function openingAgentMessage(session: BotChatSession): string {
  const msg = session.messages?.find((m) => m.role === 'bot' || m.role === 'system');
  return msg?.body ?? '';
}

/** Detect Lexxi AI intro copy that must not appear in live chat sessions. */
function isLexxiIntroMessage(body: string): boolean {
  const text = body.toLowerCase();
  return (
    /i'?m \*\*lexx?i\*\*/.test(text) ||
    text.includes("project lx's ldms guide") ||
    text.includes('ldms guide, and i') ||
    text.includes('glad you stopped by')
  );
}

export function isGuestSessionValidForBrand(session: BotChatSession, brand: LexxiChatBrand): boolean {
  const topic = (session.topic ?? '').trim().toLowerCase();
  const liveChatTopic = GUEST_LIVE_CHAT_TOPIC.toLowerCase();

  if (brand === 'live-chat') {
    if (topic !== liveChatTopic) {
      return false;
    }
    return !isLexxiIntroMessage(openingAgentMessage(session));
  }

  return topic !== liveChatTopic;
}

@Injectable({ providedIn: 'root' })
export class LexiGuestChatService {
  private readonly base = ldmsServiceUrl('messaging-inbound', 'bot-session/guest');

  constructor(private readonly http: HttpClient) {}

  readStoredSessionId(brand: LexxiChatBrand): string | null {
    try {
      const key = storageKeyForBrand(brand);
      let raw = localStorage.getItem(key)?.trim();
      if (!raw && brand === 'lexxi') {
        raw = localStorage.getItem(LEGACY_GUEST_SESSION_KEY)?.trim() ?? '';
      }
      return raw || null;
    } catch {
      return null;
    }
  }

  storeSessionId(brand: LexxiChatBrand, sessionId: string): void {
    try {
      localStorage.setItem(storageKeyForBrand(brand), sessionId);
      if (brand === 'lexxi') {
        localStorage.removeItem(LEGACY_GUEST_SESSION_KEY);
      }
    } catch {
      // ignore quota / private mode
    }
  }

  clearStoredSessionId(brand: LexxiChatBrand): void {
    try {
      localStorage.removeItem(storageKeyForBrand(brand));
      if (brand === 'lexxi') {
        localStorage.removeItem(LEGACY_GUEST_SESSION_KEY);
      }
    } catch {
      // ignore
    }
  }

  startSession(brand: LexxiChatBrand): Observable<BotChatSession> {
    return this.http
      .post<BotSessionApiResponse>(`${this.base}/start`, {
        topic: topicForBrand(brand),
        assistantMode: 'ASSISTANT',
      })
      .pipe(
        map((resp) => {
          if (!apiOk(resp) || !resp.botSessionDto) {
            throw new Error(resp.message ?? 'Could not start chat.');
          }
          const session = normalizeSession(resp.botSessionDto);
          if (!isGuestSessionValidForBrand(session, brand)) {
            throw new Error(
              brand === 'live-chat'
                ? 'Live chat returned an invalid session. Try again in a moment.'
                : 'Lexxi chat returned an invalid session. Try again in a moment.',
            );
          }
          this.storeSessionId(brand, session.sessionId);
          return session;
        }),
        catchError((err) =>
          throwError(() =>
            mapHttpError(err, brand === 'live-chat' ? 'Could not start live chat.' : 'Could not start Lexxi chat.'),
          ),
        ),
      );
  }

  fetchSession(brand: LexxiChatBrand, sessionId: string): Observable<BotChatSession> {
    return this.http
      .get<BotSessionApiResponse>(`${this.base}/find-by-id/${encodeURIComponent(sessionId)}`)
      .pipe(
        map((resp) => {
          if (!apiOk(resp) || !resp.botSessionDto) {
            throw new Error(resp.message ?? 'Could not load chat.');
          }
          const session = normalizeSession(resp.botSessionDto);
          this.storeSessionId(brand, session.sessionId);
          return session;
        }),
        catchError((err) =>
          throwError(() =>
            mapHttpError(err, brand === 'live-chat' ? 'Could not load live chat.' : 'Could not load Lexxi chat.'),
          ),
        ),
      );
  }

  sendMessage(brand: LexxiChatBrand, sessionId: string, body: string): Observable<BotChatSession> {
    return this.http
      .post<BotSessionApiResponse>(`${this.base}/send-message`, { sessionId, body })
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
}
