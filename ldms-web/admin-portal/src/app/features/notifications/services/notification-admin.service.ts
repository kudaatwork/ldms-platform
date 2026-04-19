import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map, of } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { MOCK_NOTIFICATION_LOG, MOCK_NOTIFICATION_TEMPLATES } from '../data/notifications-mock-data';
import type {
  ChannelOptionDto,
  CreateTemplateRequest,
  NotificationChannel,
  NotificationLogFilters,
  NotificationLogRow,
  NotificationTemplateRow,
  TemplateCreationMetadataDto,
  TemplateExportFormat,
  TemplateListResponse,
  TemplateMultipleFiltersRequest,
  TemplateResponse,
} from '../models/notification-admin.models';

/** Unknown API shape for GET /api/v1/system/notifications/log — map defensively */
interface NotificationLogApiEnvelope {
  logDtoList?: NotificationLogRow[];
  notificationLogDtoList?: NotificationLogRow[];
  data?: NotificationLogRow[];
}

const FRONTEND_TEMPLATE_BASE = '/api/v1/frontend/notification-template';

/** Static fallback when metadata API is unavailable (matches ldms-notifications Channel enum + processor copy). */
const FALLBACK_CHANNEL_OPTIONS: ChannelOptionDto[] = [
  { value: 'EMAIL', label: 'EMAIL', description: 'Send as email (subject + HTML body).' },
  { value: 'SMS', label: 'SMS', description: 'Send as SMS (max 320 chars).' },
  { value: 'WHATSAPP', label: 'WHATSAPP', description: 'Send via WhatsApp (Twilio template name required).' },
  { value: 'IN_APP', label: 'IN APP', description: 'Show in-app notification (title + body).' },
  { value: 'SLACK', label: 'SLACK', description: 'Send to Slack via incoming webhook.' },
  { value: 'TEAMS', label: 'TEAMS', description: 'Send to Microsoft Teams via incoming webhook.' },
];

@Injectable({
  providedIn: 'root',
})
export class NotificationAdminService {
  private readonly base = environment.apiUrl;

  /** Mutable copy for mock create/delete flows */
  private mockTemplates: NotificationTemplateRow[] = [...MOCK_NOTIFICATION_TEMPLATES];

  constructor(private readonly http: HttpClient) {}

  getTemplates(): Observable<NotificationTemplateRow[]> {
    if (environment.useMocks) {
      return of([...this.mockTemplates]);
    }
    return this.http
      .get<TemplateListResponse>(`${this.base}/api/v1/system/notification-template/find-all-as-a-list`)
      .pipe(map((r) => r.templateList ?? []));
  }

  getAddTemplateMetadata(): Observable<TemplateCreationMetadataDto> {
    if (environment.useMocks) {
      return of({
        channelOptions: FALLBACK_CHANNEL_OPTIONS,
      });
    }
    return this.http
      .get<TemplateResponse>(`${this.base}${FRONTEND_TEMPLATE_BASE}/add-template-metadata`)
      .pipe(
        map((r) => {
          if (r.addTemplateMetadata?.channelOptions?.length) {
            return r.addTemplateMetadata;
          }
          return { channelOptions: FALLBACK_CHANNEL_OPTIONS };
        }),
      );
  }

  createTemplate(body: CreateTemplateRequest): Observable<NotificationTemplateRow> {
    if (environment.useMocks) {
      const nextId = this.mockTemplates.reduce((m, t) => Math.max(m, t.id), 0) + 1;
      const row: NotificationTemplateRow = {
        id: nextId,
        templateKey: body.templateKey.trim(),
        description: body.description.trim(),
        channels: [...body.channels] as NotificationChannel[],
        emailSubject: body.emailSubject ?? null,
        isActive: true,
      };
      this.mockTemplates = [...this.mockTemplates, row];
      return of(row);
    }
    return this.http
      .post<TemplateResponse>(`${this.base}${FRONTEND_TEMPLATE_BASE}/create`, body)
      .pipe(map((r) => r.template as NotificationTemplateRow));
  }

  exportTemplates(
    format: TemplateExportFormat,
    filters: TemplateMultipleFiltersRequest = { page: 0, size: 10_000 },
  ): Observable<Blob> {
    if (environment.useMocks) {
      return of(this.buildMockExportBlob(format, [...this.mockTemplates]));
    }
    return this.http.post(`${this.base}${FRONTEND_TEMPLATE_BASE}/export`, filters, {
      params: { format },
      responseType: 'blob',
    });
  }

  getNotificationLog(filters: NotificationLogFilters): Observable<NotificationLogRow[]> {
    if (environment.useMocks) {
      return of(this.applyLogFilters([...MOCK_NOTIFICATION_LOG], filters));
    }
    let params = new HttpParams();
    if (filters.search?.trim()) {
      params = params.set('search', filters.search.trim());
    }
    if (filters.from) {
      params = params.set('from', filters.from.toISOString());
    }
    if (filters.to) {
      params = params.set('to', filters.to.toISOString());
    }
    return this.http
      .get<NotificationLogApiEnvelope>(`${this.base}/api/v1/system/notifications/log`, { params })
      .pipe(
        map((body) => {
          const raw = body.logDtoList ?? body.notificationLogDtoList ?? body.data ?? [];
          return raw;
        }),
      );
  }

  setTemplateActive(id: number, isActive: boolean): Observable<void> {
    if (environment.useMocks) {
      const idx = this.mockTemplates.findIndex((t) => t.id === id);
      if (idx >= 0) {
        this.mockTemplates[idx] = { ...this.mockTemplates[idx], isActive };
      }
      return of(void 0);
    }
    return this.http.put<void>(`${this.base}/api/v1/system/notification-template/update`, {
      id,
      isActive,
    });
  }

  private buildMockExportBlob(_format: TemplateExportFormat, rows: NotificationTemplateRow[]): Blob {
    // Mock: CSV bytes only; live API returns real XLSX/PDF from the gateway.
    return new Blob([this.templatesToCsv(rows)], { type: 'text/csv;charset=utf-8' });
  }

  private templatesToCsv(rows: NotificationTemplateRow[]): string {
    const header = ['templateKey', 'description', 'channels', 'emailSubject', 'isActive'].join(',');
    const lines = rows.map((r) =>
      [
        this.csvEscape(r.templateKey),
        this.csvEscape(r.description ?? ''),
        this.csvEscape(r.channels.join(';')),
        this.csvEscape(r.emailSubject ?? ''),
        r.isActive ? 'true' : 'false',
      ].join(','),
    );
    return [header, ...lines].join('\n');
  }

  private csvEscape(v: string): string {
    const s = String(v ?? '');
    if (/[",\n]/.test(s)) {
      return `"${s.replace(/"/g, '""')}"`;
    }
    return s;
  }

  private applyLogFilters(rows: NotificationLogRow[], f: NotificationLogFilters): NotificationLogRow[] {
    let out = rows;
    const q = f.search?.trim().toLowerCase();
    if (q) {
      out = out.filter(
        (r) =>
          r.recipientDisplay.toLowerCase().includes(q) || r.templateKey.toLowerCase().includes(q),
      );
    }
    if (f.from) {
      const t = f.from.getTime();
      out = out.filter((r) => new Date(r.sentAt).getTime() >= t);
    }
    if (f.to) {
      const t = f.to.getTime();
      out = out.filter((r) => new Date(r.sentAt).getTime() <= t);
    }
    return out;
  }
}
