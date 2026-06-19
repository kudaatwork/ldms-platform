import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import { apiEnvelopeOk, mapBotAdminApiError } from './bot-admin-api.util';

export type BotFaqCategory =
  | 'GENERAL'
  | 'ONBOARDING'
  | 'OPERATIONS'
  | 'BILLING'
  | 'FLEET'
  | 'TRIPS'
  | 'SUPPORT';

export interface BotFaqRow {
  id: number;
  question: string;
  answer: string;
  category: BotFaqCategory | string;
  keywords?: string;
  published: boolean;
  useCount: number;
  createdAt?: string;
  modifiedAt?: string;
}

export interface BotKnowledgeStatus {
  lastLoadedAt?: string;
  documentCount: number;
  characterCount: number;
  faqCount: number;
  sources?: string[];
}

export interface CreateBotFaqPayload {
  question: string;
  answer: string;
  category: string;
  keywords?: string;
  published?: boolean;
}

export interface EditBotFaqPayload {
  question?: string;
  answer?: string;
  category?: string;
  keywords?: string;
  published?: boolean;
}

interface BotFaqApiResponse {
  success?: boolean;
  isSuccess?: boolean;
  statusCode?: number;
  message?: string;
  botFaqDto?: BotFaqRow;
  botFaqDtoList?: BotFaqRow[];
}

interface BotKnowledgeApiResponse {
  success?: boolean;
  isSuccess?: boolean;
  statusCode?: number;
  message?: string;
  knowledgeStatus?: BotKnowledgeStatus;
}

@Injectable({ providedIn: 'root' })
export class BotFaqAdminService {
  private readonly faqBase = ldmsServiceUrl('messaging-inbound', 'bot-faq', undefined, 'backoffice');
  private readonly knowledgeBase = ldmsServiceUrl('messaging-inbound', 'bot-knowledge', undefined, 'backoffice');

  constructor(private readonly http: HttpClient) {}

  listFaqs(): Observable<BotFaqRow[]> {
    return this.http.get<BotFaqApiResponse>(`${this.faqBase}/list`).pipe(
      map((resp) => {
        if (!apiEnvelopeOk(resp)) {
          throw new Error(resp.message ?? 'Could not load FAQs.');
        }
        return resp.botFaqDtoList ?? [];
      }),
      catchError((err) => throwError(() => mapBotAdminApiError(err, 'Could not load FAQs.'))),
    );
  }

  createFaq(payload: CreateBotFaqPayload): Observable<BotFaqRow> {
    return this.http.post<BotFaqApiResponse>(`${this.faqBase}/create`, payload).pipe(
      map((resp) => this.requireFaq(resp, 'Could not create FAQ.')),
      catchError((err) => throwError(() => mapBotAdminApiError(err, 'Could not create FAQ.'))),
    );
  }

  updateFaq(id: number, payload: EditBotFaqPayload): Observable<BotFaqRow> {
    return this.http.put<BotFaqApiResponse>(`${this.faqBase}/update/${id}`, payload).pipe(
      map((resp) => this.requireFaq(resp, 'Could not update FAQ.')),
      catchError((err) => throwError(() => mapBotAdminApiError(err, 'Could not update FAQ.'))),
    );
  }

  deleteFaq(id: number): Observable<void> {
    return this.http.delete<BotFaqApiResponse>(`${this.faqBase}/delete/${id}`).pipe(
      map((resp) => {
        if (!apiEnvelopeOk(resp)) {
          throw new Error(resp.message ?? 'Could not delete FAQ.');
        }
      }),
      catchError((err) => throwError(() => mapBotAdminApiError(err, 'Could not delete FAQ.'))),
    );
  }

  knowledgeStatus(): Observable<BotKnowledgeStatus> {
    return this.http.get<BotKnowledgeApiResponse>(`${this.knowledgeBase}/status`).pipe(
      map((resp) => {
        if (!apiEnvelopeOk(resp) || !resp.knowledgeStatus) {
          throw new Error(resp.message ?? 'Could not load knowledge status.');
        }
        return resp.knowledgeStatus;
      }),
      catchError((err) => throwError(() => mapBotAdminApiError(err, 'Could not load knowledge status.'))),
    );
  }

  reloadKnowledge(): Observable<BotKnowledgeStatus> {
    return this.http.post<BotKnowledgeApiResponse>(`${this.knowledgeBase}/reload`, {}).pipe(
      map((resp) => {
        if (!apiEnvelopeOk(resp) || !resp.knowledgeStatus) {
          throw new Error(resp.message ?? 'Could not reload knowledge.');
        }
        return resp.knowledgeStatus;
      }),
      catchError((err) => throwError(() => mapBotAdminApiError(err, 'Could not reload knowledge.'))),
    );
  }

  private requireFaq(resp: BotFaqApiResponse, fallback: string): BotFaqRow {
    if (!apiEnvelopeOk(resp) || !resp.botFaqDto) {
      throw new Error(resp.message ?? fallback);
    }
    return resp.botFaqDto;
  }
}
