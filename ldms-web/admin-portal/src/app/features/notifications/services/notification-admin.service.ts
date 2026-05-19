import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, from, map, of, switchMap, throwError } from 'rxjs';
import { LOCATIONS_TABLE_PAGE_SIZE } from '../../locations/services/locations.service';
import { environment } from '../../../../environments/environment';
import { apiBaseUrl } from '../../../core/utils/api-url.util';
import { MOCK_NOTIFICATION_LOG, MOCK_NOTIFICATION_TEMPLATES } from '../data/notifications-mock-data';
import type {
  ChannelOptionDto,
  CreateTemplateRequest,
  NotificationChannel,
  NotificationLogFilters,
  NotificationLogMultipleFiltersRequest,
  NotificationLogExportFormat,
  NotificationLogListResponse,
  NotificationLogRow,
  NotificationQueueSummary,
  NotificationTemplateRow,
  TemplateCreationMetadataDto,
  TemplateExportFormat,
  TemplateImportSummary,
  TemplateListResponse,
  TemplateMultipleFiltersRequest,
  TemplateResponse,
  UpdateTemplateRequest,
} from '../models/notification-admin.models';

/** Static fallback when metadata API is unavailable (matches ldms-notifications Channel enum + processor copy). */
const FALLBACK_CHANNEL_OPTIONS: ChannelOptionDto[] = [
  { value: 'EMAIL', label: 'EMAIL', description: 'Send as email (subject + HTML body).' },
  { value: 'SMS', label: 'SMS', description: 'Send as SMS (max 320 chars).' },
  { value: 'WHATSAPP', label: 'WHATSAPP', description: 'Send via WhatsApp (Twilio template name required).' },
  { value: 'IN_APP', label: 'IN APP', description: 'Show in-app notification (title + body).' },
  { value: 'SLACK', label: 'SLACK', description: 'Send to Slack via incoming webhook.' },
  { value: 'TEAMS', label: 'TEAMS', description: 'Send to Microsoft Teams via incoming webhook.' },
];

type NotificationResource = 'notification-template' | 'notification-log';

@Injectable({
  providedIn: 'root',
})
export class NotificationAdminService {
  private readonly base = apiBaseUrl();

  /** Mutable copy for mock create/delete flows */
  private mockTemplates: NotificationTemplateRow[] = [...MOCK_NOTIFICATION_TEMPLATES];

  constructor(private readonly http: HttpClient) {}

  getTemplates(): Observable<NotificationTemplateRow[]> {
    if (environment.useMocks) {
      return of([...this.mockTemplates]);
    }
    return this.http
      .get<TemplateListResponse>(this.url('notification-template', 'find-all-as-a-list'))
      .pipe(map((r) => (r.templateList ?? []).map((row) => this.normalizeTemplateRow(row))));
  }

  /**
   * Server-paged templates (same transport pattern as locations `queryTablePage` → POST + `Page` metadata).
   * Backend returns 404 when the page is empty; treat as zero rows.
   */
  getTemplatesPage(filters: TemplateMultipleFiltersRequest): Observable<{
    rows: NotificationTemplateRow[];
    totalElements: number;
  }> {
    const page = filters.page ?? 0;
    const size = filters.size && filters.size > 0 ? filters.size : LOCATIONS_TABLE_PAGE_SIZE;

    if (environment.useMocks) {
      let list = [...this.mockTemplates];
      const q = filters.searchValue?.trim().toLowerCase();
      if (q) {
        list = list.filter(
          (t) =>
            t.templateKey.toLowerCase().includes(q) ||
            (t.description ?? '').toLowerCase().includes(q) ||
            (t.emailSubject ?? '').toLowerCase().includes(q),
        );
      }
      const tk = filters.templateKey?.trim().toLowerCase();
      if (tk) {
        list = list.filter((t) => t.templateKey.toLowerCase().startsWith(tk));
      }
      if (filters.channels?.length) {
        list = list.filter((t) =>
          filters.channels!.every((ch) => t.channels.includes(ch as NotificationChannel)),
        );
      }
      if (filters.isActive != null) {
        list = list.filter((t) => t.isActive === filters.isActive);
      }
      const totalElements = list.length;
      const start = page * size;
      const rows = list.slice(start, start + size).map((row) => this.normalizeTemplateRow(row));
      return of({ rows, totalElements });
    }

    const body: TemplateMultipleFiltersRequest = {
      ...filters,
      page,
      size,
      searchValue: filters.searchValue ?? '',
    };

    return this.http.post<TemplateResponse>(this.url('notification-template', 'find-by-multiple-filters'), body).pipe(
      map((r) => this.mapTemplatePageResponse(r)),
      catchError((err) => {
        if (err instanceof HttpErrorResponse && err.status === 404) {
          return of({ rows: [], totalElements: 0 });
        }
        return throwError(() => err);
      }),
    );
  }

  getAddTemplateMetadata(): Observable<TemplateCreationMetadataDto> {
    if (environment.useMocks) {
      return of({
        channelOptions: FALLBACK_CHANNEL_OPTIONS,
      });
    }
    return this.http.get<TemplateResponse>(this.url('notification-template', 'add-template-metadata')).pipe(
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
      .get<TemplateResponse>(this.url('notification-template', `find-by-id/${id}`))
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
      .post<TemplateResponse>(this.url('notification-template', 'create'), body)
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
      .put<TemplateResponse>(this.url('notification-template', 'update'), body)
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
    return this.http.post<TemplateImportSummary>(this.url('notification-template', 'import'), formData);
  }

  deleteTemplate(id: number): Observable<void> {
    if (environment.useMocks) {
      this.mockTemplates = this.mockTemplates.filter((t) => t.id !== id);
      return of(void 0);
    }
    return this.http.delete<void>(this.url('notification-template', `delete-by-id/${id}`));
  }

  exportTemplates(
    format: TemplateExportFormat,
    filters: TemplateMultipleFiltersRequest = { page: 0, size: 10_000 },
  ): Observable<Blob> {
    if (environment.useMocks) {
      return of(this.buildMockExportBlob(format, [...this.mockTemplates]));
    }
    return this.http
      .post(this.url('notification-template', 'export'), filters, {
        params: { format },
        responseType: 'blob',
      })
      .pipe(switchMap((blob) => this.ensureExportBlob(blob)));
  }

  /**
   * Server-paged activity log (same transport pattern as templates `getTemplatesPage`).
   */
  getNotificationLogPage(filters: NotificationLogFilters): Observable<{
    rows: NotificationLogRow[];
    totalElements: number;
    queueSummary: NotificationQueueSummary | null;
  }> {
    const page = filters.page ?? 0;
    const size = filters.size && filters.size > 0 ? filters.size : LOCATIONS_TABLE_PAGE_SIZE;

    if (environment.useMocks) {
      const filtered = this.applyLogFilters([...MOCK_NOTIFICATION_LOG], filters);
      const totalElements = filtered.length;
      const start = page * size;
      const rows = filtered.slice(start, start + size);
      return of({
        rows,
        totalElements,
        queueSummary: { queueName: 'notifications.queue', messagesReady: 0 },
      });
    }

    const body = this.buildLogFilters({ ...filters, page, size });

    return this.http
      .post<NotificationLogListResponse>(this.url('notification-log', 'find-by-multiple-filters'), body)
      .pipe(
        map((r) => this.mapNotificationLogPageResponse(r)),
        catchError((err) => {
          if (err instanceof HttpErrorResponse && err.status === 404) {
            return of({ rows: [], totalElements: 0, queueSummary: null });
          }
          return throwError(() => err);
        }),
      );
  }

  exportNotificationLog(
    format: NotificationLogExportFormat,
    filters: NotificationLogFilters,
  ): Observable<Blob> {
    if (environment.useMocks) {
      if (format === 'pdf') {
        return throwError(() => new Error('PDF export is not available while mocks are enabled.'));
      }
      return of(this.logsToCsvBlob(this.applyLogFilters([...MOCK_NOTIFICATION_LOG], filters)));
    }
    return this.http
      .post(this.url('notification-log', 'export'), this.buildLogFilters({ ...filters, page: 0, size: 10_000 }), {
        params: { format },
        responseType: 'blob',
      })
      .pipe(switchMap((blob) => this.ensureExportBlob(blob)));
  }

  private ensureExportBlob(blob: Blob): Observable<Blob> {
    const type = (blob.type ?? '').toLowerCase();
    if (
      type.includes('csv') ||
      type.includes('spreadsheet') ||
      type.includes('pdf') ||
      type.includes('octet-stream') ||
      type.includes('ms-excel')
    ) {
      return of(blob);
    }
    return from(blob.text()).pipe(
      switchMap((text) => {
        const trimmed = text.trim();
        if (trimmed.startsWith('{') || /^failed/i.test(trimmed) || trimmed.toLowerCase().includes('export failed')) {
          let message = trimmed.slice(0, 240);
          try {
            const parsed = JSON.parse(trimmed) as { message?: string };
            if (parsed.message?.trim()) {
              message = parsed.message.trim();
            }
          } catch {
            /* use raw text */
          }
          return throwError(() => new Error(message));
        }
        return of(blob);
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
    return this.http.put<void>(this.url('notification-template', 'update'), {
      id,
      isActive,
    });
  }

  /**
   * Build a fully-qualified API URL for the notifications service (same pattern as locations `url()`).
   */
  private url(resource: NotificationResource, operation: string): string {
    return `${this.base}/ldms-notifications/v1/${environment.apiSurface}/${resource}/${operation}`;
  }

  private mapTemplatePageResponse(r: TemplateResponse): {
    rows: NotificationTemplateRow[];
    totalElements: number;
  } {
    // Backend uses JSON `statusCode` (often still HTTP 200); empty catalog is 404 in-body.
    if (r.statusCode === 404) {
      return { rows: [], totalElements: 0 };
    }
    if (r.statusCode != null && r.statusCode >= 400) {
      throw r;
    }
    const p = r.templatePage;
    const content = p?.content ?? [];
    return {
      rows: content.map((row) => this.normalizeTemplateRow(row)),
      totalElements: Number(p?.totalElements ?? content.length),
    };
  }

  private mapNotificationLogPageResponse(r: NotificationLogListResponse): {
    rows: NotificationLogRow[];
    totalElements: number;
    queueSummary: NotificationQueueSummary | null;
  } {
    if (r.statusCode != null && r.statusCode >= 400) {
      throw r;
    }
    const p = r.notificationLogPage;
    const content = p?.content ?? [];
    return {
      rows: content.map((log) => this.mapNotificationLogDto(log)),
      totalElements: Number(p?.totalElements ?? content.length),
      queueSummary: r.queueSummary ?? null,
    };
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

  private mapNotificationLogDto(log: NotificationLogRow): NotificationLogRow {
    return {
      id: Number(log.id ?? 0),
      eventId: log.eventId ?? null,
      recipientId: log.recipientId ?? null,
      recipientDisplay: log.recipientDisplay ?? '—',
      recipientEmail: log.recipientEmail ?? null,
      recipientPhone: log.recipientPhone ?? null,
      channel: log.channel ?? '—',
      templateKey: log.templateKey ?? '—',
      status: log.status ?? 'PENDING',
      provider: log.provider ?? null,
      providerMessageId: log.providerMessageId ?? null,
      errorMessage: log.errorMessage ?? null,
      sentAt: log.createdAt ?? log.sentAt ?? new Date().toISOString(),
      createdAt: log.createdAt ?? null,
      updatedAt: log.updatedAt ?? null,
      retryCount: log.retryCount ?? 0,
    };
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
    const tk = f.templateKey?.trim().toLowerCase();
    if (tk) {
      out = out.filter((r) => r.templateKey.toLowerCase().includes(tk));
    }
    if (f.channel?.trim()) {
      const c = f.channel.trim();
      out = out.filter((r) => r.channel === c);
    }
    if (f.status?.trim()) {
      const s = f.status.trim();
      out = out.filter((r) => r.status === s);
    }
    if (f.recipientId?.trim()) {
      const id = f.recipientId.trim();
      out = out.filter((r) => String(r.recipientId ?? '') === id);
    }
    if (f.provider?.trim()) {
      const p = f.provider.trim().toLowerCase();
      out = out.filter((r) => (r.provider ?? '').toLowerCase().includes(p));
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

  private buildLogFilters(filters: NotificationLogFilters): NotificationLogMultipleFiltersRequest {
    const page = filters.page ?? 0;
    const size = filters.size && filters.size > 0 ? filters.size : LOCATIONS_TABLE_PAGE_SIZE;
    const body: NotificationLogMultipleFiltersRequest = {
      page,
      size,
      searchValue: filters.search?.trim() ?? '',
    };
    if (filters.from) {
      body.from = filters.from.toISOString();
    }
    if (filters.to) {
      const end = new Date(filters.to);
      end.setHours(23, 59, 59, 999);
      body.to = end.toISOString();
    }
    const tk = filters.templateKey?.trim();
    if (tk) {
      body.templateKey = tk;
    }
    const ch = filters.channel?.trim();
    if (ch) {
      body.channel = ch;
    }
    const st = filters.status?.trim();
    if (st) {
      body.status = st;
    }
    const rid = filters.recipientId?.trim();
    if (rid) {
      body.recipientId = rid;
    }
    const prov = filters.provider?.trim();
    if (prov) {
      body.provider = prov;
    }
    return body;
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
