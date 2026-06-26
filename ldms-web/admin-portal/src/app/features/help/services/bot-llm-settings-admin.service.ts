import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import { apiEnvelopeOk, mapBotAdminApiError } from './bot-admin-api.util';

export interface BotLlmModelOption {
  providerId: string;
  modelId: string;
  label: string;
}

export interface BotLlmSettings {
  configuredProvider: string;
  activeProvider: string;
  activeModel: string;
  geminiConfigured: boolean;
  anthropicConfigured: boolean;
  runtimeProvider?: string | null;
  runtimeGeminiModel?: string | null;
  runtimeAnthropicModel?: string | null;
  modelCatalog: BotLlmModelOption[];
}

interface BotLlmSettingsApiResponse {
  isSuccess?: boolean;
  success?: boolean;
  statusCode?: number;
  message?: string;
  botLlmSettingsDto?: BotLlmSettings;
}

export interface UpdateBotLlmRuntimePayload {
  provider?: string | null;
  geminiModel?: string | null;
  anthropicModel?: string | null;
}

@Injectable({ providedIn: 'root' })
export class BotLlmSettingsAdminService {
  private readonly base = ldmsServiceUrl('messaging-inbound', 'bot-llm-settings', undefined, 'backoffice');

  constructor(private readonly http: HttpClient) {}

  currentSettings(): Observable<BotLlmSettings> {
    return this.http.get<BotLlmSettingsApiResponse>(`${this.base}/current`).pipe(
      map((resp) => {
        if (!apiEnvelopeOk(resp) || !resp.botLlmSettingsDto) {
          throw new Error(resp.message ?? 'Could not load bot LLM settings.');
        }
        return resp.botLlmSettingsDto;
      }),
      catchError((err) => throwError(() => mapBotAdminApiError(err, 'Could not load bot LLM settings.'))),
    );
  }

  updateRuntime(payload: UpdateBotLlmRuntimePayload): Observable<BotLlmSettings> {
    return this.http.put<BotLlmSettingsApiResponse>(`${this.base}/runtime`, payload).pipe(
      map((resp) => {
        if (!apiEnvelopeOk(resp) || !resp.botLlmSettingsDto) {
          throw new Error(resp.message ?? 'Could not update bot LLM settings.');
        }
        return resp.botLlmSettingsDto;
      }),
      catchError((err) => throwError(() => mapBotAdminApiError(err, 'Could not update bot LLM settings.'))),
    );
  }
}
