import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import type { RequestLogRow } from '../../models/request-log-row.model';
import { AuditLogAdminService, type AuditLogDto } from '../../services/audit-log-admin.service';

export interface AuditLogRequestDetailDialogData {
  row: RequestLogRow;
}

/** Flattened strings for read-only detail view (all fields visible). */
export interface AuditLogDetailView {
  id: string;
  action: string;
  eventType: string;
  username: string;
  serviceName: string;
  httpMethod: string;
  httpStatusCode: string;
  requestUrl: string;
  clientIpAddress: string;
  traceId: string;
  /** Display-ready, e.g. `42 ms` or `—` */
  responseTime: string;
  requestTimestamp: string;
  responseTimestamp: string;
  exceptionMessage: string;
  requestHeaders: string;
  requestPayload: string;
  responsePayload: string;
  curlCommand: string;
}

@Component({
  selector: 'app-audit-log-request-detail-dialog',
  templateUrl: './audit-log-request-detail-dialog.component.html',
  styleUrl: './audit-log-request-detail-dialog.component.scss',
  standalone: false,
})
export class AuditLogRequestDetailDialogComponent implements OnInit {
  loading = true;
  infoBanner: string | null = null;
  detail: AuditLogDetailView | null = null;

  constructor(
    private readonly dialogRef: MatDialogRef<AuditLogRequestDetailDialogComponent, void>,
    private readonly auditLogAdmin: AuditLogAdminService,
    @Inject(MAT_DIALOG_DATA) readonly data: AuditLogRequestDetailDialogData,
  ) {}

  ngOnInit(): void {
    if (environment.useMocks) {
      this.detail = this.buildFromRow(this.data.row);
      this.loading = false;
      return;
    }
    this.auditLogAdmin
      .findById(this.data.row.id)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (resp) => {
          const dto = resp.auditLog;
          if (dto && resp.success !== false) {
            this.detail = this.buildFromDto(dto);
          } else {
            this.detail = this.buildFromRow(this.data.row);
            this.infoBanner = 'Full log payload was not returned; showing fields from the list row.';
          }
        },
        error: () => {
          this.detail = this.buildFromRow(this.data.row);
          this.infoBanner = 'Could not load the full log from the server; showing fields from the list row only.';
        },
      });
  }

  close(): void {
    this.dialogRef.close();
  }

  private buildFromDto(d: AuditLogDto): AuditLogDetailView {
    const ev = d.eventType;
    const eventLabel = typeof ev === 'string' ? ev : ev != null ? String(ev) : '—';
    return {
      id: String(d.id ?? '—'),
      action: this.dash(d.action),
      eventType: this.dash(eventLabel),
      username: this.dash(d.username),
      serviceName: this.dash(d.serviceName),
      httpMethod: this.dash(d.httpMethod),
      httpStatusCode: d.httpStatusCode != null ? String(d.httpStatusCode) : '—',
      requestUrl: this.dash(d.requestUrl),
      clientIpAddress: this.dash(d.clientIpAddress),
      traceId: this.dash(d.traceId),
      responseTime: d.responseTimeMs != null ? `${d.responseTimeMs} ms` : '—',
      requestTimestamp: this.formatApiDateTime(d.requestTimestamp as unknown),
      responseTimestamp: this.formatApiDateTime(d.responseTimestamp as unknown),
      exceptionMessage: this.dash(d.exceptionMessage),
      requestHeaders: this.dash(d.requestHeaders),
      requestPayload: this.dash(d.requestPayload),
      responsePayload: this.dash(d.responsePayload),
      curlCommand: this.dash(d.curlCommand),
    };
  }

  private buildFromRow(r: RequestLogRow): AuditLogDetailView {
    return {
      id: String(r.id),
      action: this.dash(r.action),
      eventType: this.dash(r.eventType),
      username: this.dash(r.username),
      serviceName: this.dash(r.serviceName),
      httpMethod: this.dash(r.method),
      httpStatusCode: r.statusCode != null ? String(r.statusCode) : '—',
      requestUrl: this.dash(r.requestUrl),
      clientIpAddress: this.dash(r.clientIpAddress),
      traceId: this.dash(r.traceId),
      responseTime: r.responseTimeMs != null && r.responseTimeMs !== undefined ? `${r.responseTimeMs} ms` : '—',
      requestTimestamp: this.formatApiDateTime(r.requestTimestamp) || this.dash(r.time),
      responseTimestamp: this.formatApiDateTime(r.responseTimestamp),
      exceptionMessage: this.dash(r.exceptionMessage),
      requestHeaders: this.dash(r.requestHeaders),
      requestPayload: '—',
      responsePayload: '—',
      curlCommand: '—',
    };
  }

  private dash(value: string | null | undefined): string {
    const t = (value ?? '').trim();
    return t.length > 0 ? t : '—';
  }

  private formatApiDateTime(value: unknown): string {
    if (value == null || value === '') {
      return '—';
    }
    if (typeof value === 'string') {
      const d = new Date(value);
      return Number.isNaN(d.getTime()) ? value : d.toLocaleString();
    }
    if (Array.isArray(value)) {
      return value.join(', ');
    }
    return String(value);
  }
}
