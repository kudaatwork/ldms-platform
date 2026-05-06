import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map, of } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { MOCK_NOTIFICATION_LOG, MOCK_NOTIFICATION_TEMPLATES } from '../data/notifications-mock-data';
import type {
  ChannelOptionDto,
  CreateTemplateRequest,
  NotificationChannel,
  NotificationLogFilters,
  NotificationLogExportFormat,
  NotificationLogRow,
  NotificationTemplateRow,
  TemplateCreationMetadataDto,
  TemplateExportFormat,
  TemplateImportSummary,
  TemplateListResponse,
  TemplateMultipleFiltersRequest,
  TemplateResponse,
  UpdateTemplateRequest,
} from '../models/notification-admin.models';

interface AuditLogFilters {
  page: number;
  size: number;
  searchValue: string;
  serviceName: string;
  username: string;
  eventType: string;
  httpStatusCode: number | null;
  from: string | null;
  to: string | null;
  sortBy: string;
  sortDir: string;
}

interface AuditLogDto {
  id?: number;
  action?: string;
  username?: string;
  requestTimestamp?: string;
  responseTimestamp?: string;
  requestPayload?: string | null;
  responsePayload?: string | null;
  httpStatusCode?: number;
}

interface AuditLogResponse {
  auditLogPage?: {
    content?: AuditLogDto[];
  };
}

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
  private readonly templateBase = `${this.base}/ldms-notifications/v1/${environment.apiSurface}/notification-template`;
  private readonly auditBase = `${this.base}/ldms-audit-trail/v1/${environment.apiSurface}/audit-log`;

  /** Mutable copy for mock create/delete flows */
  private mockTemplates: NotificationTemplateRow[] = [...MOCK_NOTIFICATION_TEMPLATES];

  constructor(private readonly http: HttpClient) {}

  getTemplates(): Observable<NotificationTemplateRow[]> {
    if (environment.useMocks) {
      return of([...this.mockTemplates]);
    }
    return this.http
      .get<TemplateListResponse>(`${this.templateBase}/find-all-as-a-list`)
      .pipe(map((r) => (r.templateList ?? []).map((row) => this.normalizeTemplateRow(row))));
  }

  getAddTemplateMetadata(): Observable<TemplateCreationMetadataDto> {
    if (environment.useMocks) {
      return of({
        channelOptions: FALLBACK_CHANNEL_OPTIONS,
      });
    }
    return this.http.get<TemplateResponse>(`${this.templateBase}/add-template-metadata`).pipe(
      map((r) => {
        if (r.addTemplateMetadata?.channelOptions?.length) {
          return r.addTemplateMetadata;
        }
        return { channelOptions: FALLBACK_CHANNEL_OPTIONS };
      }),
    );
  }

  getTemplateById(id: number): Observable<NotificationTemplateRow> {
    if (environment.useMocks) {
      const row = this.mockTemplates.find((t) => t.id === id);
      if (!row) {
        throw new Error('Template not found');
      }
      return of({ ...row });
    }
    return this.http
      .get<TemplateResponse>(`${this.templateBase}/find-by-id/${id}`)
      .pipe(map((r) => this.normalizeTemplateRow(r.template as NotificationTemplateRow)));
  }

  createTemplate(body: CreateTemplateRequest): Observable<NotificationTemplateRow> {
    if (environment.useMocks) {
      const nextId = this.mockTemplates.reduce((m, t) => Math.max(m, t.id), 0) + 1;
      const row: NotificationTemplateRow = {
        id: nextId,
        templateKey: body.templateKey.trim(),
        description: body.description.trim(),
        channels: [...body.channels] as NotificationChannel[],
        emailSubject: body.emailSubject || null,
        emailBodyHtml: body.emailBodyHtml || null,
        smsBody: body.smsBody || null,
        inAppTitle: body.inAppTitle || null,
        inAppBody: body.inAppBody || null,
        whatsappTemplateName: body.whatsappTemplateName || null,
        whatsappBody: body.whatsappBody || null,
        isActive: true,
      };
      this.mockTemplates = [...this.mockTemplates, row];
      return of(row);
    }
    return this.http
      .post<TemplateResponse>(`${this.templateBase}/create`, body)
      .pipe(map((r) => this.normalizeTemplateRow(r.template as NotificationTemplateRow)));
  }

  updateTemplate(body: UpdateTemplateRequest): Observable<NotificationTemplateRow> {
    if (environment.useMocks) {
      const idx = this.mockTemplates.findIndex((t) => t.id === body.id);
      const existing = idx >= 0 ? this.mockTemplates[idx] : this.mockTemplates[0];
      const updated = { ...existing, ...body };
      if (idx >= 0) {
        this.mockTemplates[idx] = updated;
      }
      return of(updated);
    }
    return this.http
      .put<TemplateResponse>(`${this.templateBase}/update`, body)
      .pipe(map((r) => this.normalizeTemplateRow(r.template as NotificationTemplateRow)));
  }

  importTemplatesCsv(file: File): Observable<TemplateImportSummary> {
    if (environment.useMocks) {
      return of({
        statusCode: 200,
        isSuccess: true,
        message: 'Mock import completed.',
        total: 0,
        success: 0,
        failed: 0,
        errors: [],
      });
    }
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<TemplateImportSummary>(`${this.templateBase}/import`, formData);
  }

  deleteTemplate(id: number): Observable<void> {
    if (environment.useMocks) {
      this.mockTemplates = this.mockTemplates.filter((t) => t.id !== id);
      return of(void 0);
    }
    return this.http.delete<void>(`${this.templateBase}/delete-by-id/${id}`);
  }

  exportTemplates(
    format: TemplateExportFormat,
    filters: TemplateMultipleFiltersRequest = { page: 0, size: 10_000 },
  ): Observable<Blob> {
    if (environment.useMocks) {
      return of(this.buildMockExportBlob(format, [...this.mockTemplates]));
    }
    return this.http.post(`${this.templateBase}/export`, filters, {
      params: { format },
      responseType: 'blob',
    });
  }

  getNotificationLog(filters: NotificationLogFilters): Observable<NotificationLogRow[]> {
    if (environment.useMocks) {
      return of(this.applyLogFilters([...MOCK_NOTIFICATION_LOG], filters));
    }

    return this.http
      .post<AuditLogResponse>(`${this.auditBase}/find-by-multiple-filters`, this.buildAuditFilters(filters))
      .pipe(
        map((resp) => (resp.auditLogPage?.content ?? []).map((log) => this.mapAuditLogToNotificationRow(log))),
      );
  }

  exportNotificationLog(
    format: NotificationLogExportFormat,
    filters: NotificationLogFilters,
  ): Observable<Blob> {
    if (environment.useMocks) {
      return of(this.logsToCsvBlob(this.applyLogFilters([...MOCK_NOTIFICATION_LOG], filters)));
    }
    return this.http.post(`${this.auditBase}/export`, this.buildAuditFilters(filters), {
      params: { format },
      responseType: 'blob',
    });
  }

  setTemplateActive(id: number, isActive: boolean): Observable<void> {
    if (environment.useMocks) {
      const idx = this.mockTemplates.findIndex((t) => t.id === id);
      if (idx >= 0) {
        this.mockTemplates[idx] = { ...this.mockTemplates[idx], isActive };
      }
      return of(void 0);
    }
    return this.http.put<void>(`${this.templateBase}/update`, {
      id,
      isActive,
    });
  }

  /** Backend may emit boolean status as `active` or `isActive`; normalize for UI code paths. */
  private normalizeTemplateRow(row: NotificationTemplateRow & { active?: boolean } | null | undefined): NotificationTemplateRow {
    const safe = (row ?? {}) as NotificationTemplateRow & { active?: boolean };
    return {
      ...safe,
      id: Number(safe.id ?? 0),
      templateKey: String(safe.templateKey ?? ''),
      channels: (safe.channels ?? []) as NotificationChannel[],
      isActive: typeof safe.isActive === 'boolean' ? safe.isActive : Boolean(safe.active),
    };
  }

  private mapAuditLogToNotificationRow(log: AuditLogDto): NotificationLogRow {
    const payload = `${log.requestPayload ?? ''} ${log.responsePayload ?? ''}`;
    const templateKey = this.extractValue(payload, /"templateKey"\s*:\s*"([^"]+)"/) ?? 'N/A';
    const channel = this.extractValue(payload, /"channel"\s*:\s*"([^"]+)"/) ?? 'N/A';
    const status = (log.httpStatusCode ?? 500) >= 200 && (log.httpStatusCode ?? 500) < 300 ? 'SENT' : 'FAILED';
    return {
      id: log.id ?? Date.now(),
      recipientDisplay: log.username ?? 'System',
      channel,
      templateKey,
      status,
      sentAt: log.requestTimestamp ?? log.responseTimestamp ?? new Date().toISOString(),
      retryCount: 0,
    };
  }

  private extractValue(input: string, regex: RegExp): string | null {
    const match = regex.exec(input);
    return match?.[1] ?? null;
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

  private buildAuditFilters(filters: NotificationLogFilters): AuditLogFilters {
    return {
      page: 0,
      size: 200,
      searchValue: filters.search?.trim() || '',
      serviceName: 'ldms-notifications',
      username: '',
      eventType: '',
      httpStatusCode: null,
      from: filters.from ? filters.from.toISOString() : null,
      to: filters.to ? filters.to.toISOString() : null,
      sortBy: 'requestTimestamp',
      sortDir: 'desc',
    };
  }

  private logsToCsvBlob(rows: NotificationLogRow[]): Blob {
    const header = ['recipientDisplay', 'channel', 'templateKey', 'status', 'sentAt', 'retryCount'].join(',');
    const lines = rows.map((r) =>
      [
        this.csvEscape(r.recipientDisplay),
        this.csvEscape(r.channel),
        this.csvEscape(r.templateKey),
        this.csvEscape(String(r.status)),
        this.csvEscape(r.sentAt),
        this.csvEscape(String(r.retryCount)),
      ].join(','),
    );
    return new Blob([[header, ...lines].join('\n')], { type: 'text/csv;charset=utf-8' });
  }
}
