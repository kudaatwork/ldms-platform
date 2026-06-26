import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, map, throwError } from 'rxjs';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';

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

function apiOk(resp: BotLlmSettingsApiResponse): boolean {
  return (
    resp.isSuccess === true ||
    resp.success === true ||
    (resp.statusCode != null && resp.statusCode >= 200 && resp.statusCode < 300)
  );
}

function mapHttpError(err: unknown, fallback: string): Error {
  if (err instanceof HttpErrorResponse) {
    const body = err.error as BotLlmSettingsApiResponse | undefined;
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
export class BotLlmSettingsService {
  private readonly base = ldmsServiceUrl('messaging-inbound', 'bot-llm-settings');

  constructor(private readonly http: HttpClient) {}

  currentSettings(): Observable<BotLlmSettings> {
    return this.http.get<BotLlmSettingsApiResponse>(`${this.base}/current`).pipe(
      map((resp) => {
        if (!apiOk(resp) || !resp.botLlmSettingsDto) {
          throw new Error(resp.message ?? 'Could not load assistant model settings.');
        }
        return resp.botLlmSettingsDto;
      }),
      catchError((err) => throwError(() => mapHttpError(err, 'Could not load assistant model settings.'))),
    );
  }

  updateRuntime(payload: UpdateBotLlmRuntimePayload): Observable<BotLlmSettings> {
    return this.http.put<BotLlmSettingsApiResponse>(`${this.base}/runtime`, payload).pipe(
      map((resp) => {
        if (!apiOk(resp) || !resp.botLlmSettingsDto) {
          throw new Error(resp.message ?? 'Could not update assistant model.');
        }
        return resp.botLlmSettingsDto;
      }),
      catchError((err) => throwError(() => mapHttpError(err, 'Could not update assistant model.'))),
    );
  }
}
