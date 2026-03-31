import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { PageEvent } from '@angular/material/paginator';
import { filterByGlobalAndColumns } from '@shared/utils/table-search.util';

export interface ActivityRow {
  time: string;
  actor: string;
  action: string;
  resource: string;
}

/** Inbound/outbound HTTP calls: integrations, webhooks, and internal services. */
export interface RequestLogRow {
  time: string;
  method: string;
  path: string;
  integration: string;
  statusCode: number;
  durationMs: number;
}

@Component({
  selector: 'app-activity-page',
  templateUrl: './activity-page.component.html',
  styleUrl: './activity-page.component.scss',
  standalone: false,
})
export class ActivityPageComponent implements OnInit {
  loading = true;

  displayedColumns = ['time', 'actor', 'action', 'resource', 'actions'];

  dataSource: ActivityRow[] = [];

  searchQuery = '';
  filterFieldsOpen = false;

  activityColumnFilters = {
    time: '',
    actor: '',
    action: '',
    resource: '',
  };

  activityPageIndex = 0;
  activityPageSize = 10;

  /* ── Requests log ─────────────────────────────────────────── */
  requestDisplayedColumns = [
    'time',
    'method',
    'path',
    'integration',
    'statusCode',
    'durationMs',
    'actions',
  ];

  requestDataSource: RequestLogRow[] = [];

  requestSearchQuery = '';
  requestFilterFieldsOpen = false;

  requestColumnFilters = {
    time: '',
    method: '',
    path: '',
    integration: '',
    statusCode: '',
    durationMs: '',
  };

  requestPageIndex = 0;
  requestPageSize = 10;

  private readonly mockRows: ActivityRow[] = [
    {
      time: '28 Mar, 09:42',
      actor: 'ops.reviewer@projectlx.co.zw',
      action: 'Approved KYC application',
      resource: 'Nexus Logistics (Pvt) Ltd',
    },
    {
      time: '28 Mar, 09:18',
      actor: 'admin@projectlx.co.zw',
      action: 'Updated organization',
      resource: 'Chirundu Cold Chain Co.',
    },
    {
      time: '27 Mar, 16:05',
      actor: 'ops.reviewer@projectlx.co.zw',
      action: 'Requested document resubmit',
      resource: 'Masvingo Grain Brokers',
    },
    {
      time: '27 Mar, 11:22',
      actor: 'admin@projectlx.co.zw',
      action: 'Created user',
      resource: 'finance@projectlx.co.zw',
    },
    {
      time: '26 Mar, 14:50',
      actor: 'system',
      action: 'Scheduled health check',
      resource: 'API Gateway',
    },
    {
      time: '26 Mar, 10:03',
      actor: 'admin@projectlx.co.zw',
      action: 'Exported audit trail',
      resource: 'CSV · Q1 scope',
    },
    {
      time: '25 Mar, 17:41',
      actor: 'ops.reviewer@projectlx.co.zw',
      action: 'Rejected KYC application',
      resource: 'Mutare Fresh Produce',
    },
    {
      time: '25 Mar, 08:55',
      actor: 'finance@projectlx.co.zw',
      action: 'Downloaded invoice pack',
      resource: 'INV-2026-0142',
    },
    {
      time: '24 Mar, 15:20',
      actor: 'system',
      action: 'Password rotation reminder',
      resource: 'viewer@projectlx.co.zw',
    },
    {
      time: '24 Mar, 09:00',
      actor: 'admin@projectlx.co.zw',
      action: 'Disabled user account',
      resource: 'legacy.vendor@example.com',
    },
    {
      time: '23 Mar, 13:12',
      actor: 'ops.reviewer@projectlx.co.zw',
      action: 'Commented on application',
      resource: 'Great North Hauliers',
    },
  ];

  private readonly mockRequestRows: RequestLogRow[] = [
    {
      time: '28 Mar, 09:44:02',
      method: 'GET',
      path: '/api/v1/frontend/organizations',
      integration: 'Partner TMS · API key',
      statusCode: 200,
      durationMs: 38,
    },
    {
      time: '28 Mar, 09:43:51',
      method: 'POST',
      path: '/api/v1/webhooks/payments/stripe',
      integration: 'Stripe webhook',
      statusCode: 204,
      durationMs: 124,
    },
    {
      time: '28 Mar, 09:42:18',
      method: 'PUT',
      path: '/api/v1/system/users/1042',
      integration: 'Admin portal',
      statusCode: 200,
      durationMs: 56,
    },
    {
      time: '28 Mar, 09:41:05',
      method: 'POST',
      path: '/api/v1/frontend/kyc/applications',
      integration: 'Mobile app · OAuth',
      statusCode: 201,
      durationMs: 210,
    },
    {
      time: '28 Mar, 09:40:33',
      method: 'GET',
      path: '/api/v1/frontend/trips?status=ACTIVE',
      integration: 'Internal scheduler',
      statusCode: 200,
      durationMs: 92,
    },
    {
      time: '28 Mar, 09:39:12',
      method: 'DELETE',
      path: '/api/v1/system/cache/region/harare',
      integration: 'Ops automation',
      statusCode: 204,
      durationMs: 18,
    },
    {
      time: '28 Mar, 09:38:44',
      method: 'PATCH',
      path: '/api/v1/frontend/documents/d-8821',
      integration: 'Document service',
      statusCode: 200,
      durationMs: 44,
    },
    {
      time: '28 Mar, 09:37:01',
      method: 'POST',
      path: '/api/v1/integrations/fleetyu/events',
      integration: 'FleetYu connector',
      statusCode: 502,
      durationMs: 30008,
    },
    {
      time: '28 Mar, 09:36:22',
      method: 'GET',
      path: '/api/v1/frontend/invoices/INV-2026-0142',
      integration: 'Finance export job',
      statusCode: 404,
      durationMs: 12,
    },
    {
      time: '28 Mar, 09:35:00',
      method: 'HEAD',
      path: '/api/v1/system/health',
      integration: 'Load balancer probe',
      statusCode: 200,
      durationMs: 3,
    },
    {
      time: '28 Mar, 09:34:11',
      method: 'POST',
      path: '/api/v1/events/rabbit/publish',
      integration: 'Notification worker',
      statusCode: 200,
      durationMs: 8,
    },
  ];

  constructor(private readonly title: Title) {}

  get filteredRows(): ActivityRow[] {
    return filterByGlobalAndColumns(
      this.dataSource,
      this.searchQuery,
      this.activityColumnFilters,
    );
  }

  get activityClampedPageIndex(): number {
    const total = this.filteredRows.length;
    if (total === 0) return 0;
    const max = Math.max(0, Math.ceil(total / this.activityPageSize) - 1);
    return Math.min(this.activityPageIndex, max);
  }

  get pagedActivityRows(): ActivityRow[] {
    const all = this.filteredRows;
    const start = this.activityClampedPageIndex * this.activityPageSize;
    return all.slice(start, start + this.activityPageSize);
  }

  resetActivityPaging(): void {
    this.activityPageIndex = 0;
  }

  onActivityPage(e: PageEvent): void {
    this.activityPageIndex = e.pageIndex;
    this.activityPageSize = e.pageSize;
  }

  get filteredRequestRows(): RequestLogRow[] {
    return filterByGlobalAndColumns(
      this.requestDataSource,
      this.requestSearchQuery,
      this.requestColumnFilters as Partial<Record<keyof RequestLogRow, string>>,
    );
  }

  get requestClampedPageIndex(): number {
    const total = this.filteredRequestRows.length;
    if (total === 0) return 0;
    const max = Math.max(0, Math.ceil(total / this.requestPageSize) - 1);
    return Math.min(this.requestPageIndex, max);
  }

  get pagedRequestRows(): RequestLogRow[] {
    const all = this.filteredRequestRows;
    const start = this.requestClampedPageIndex * this.requestPageSize;
    return all.slice(start, start + this.requestPageSize);
  }

  resetRequestPaging(): void {
    this.requestPageIndex = 0;
  }

  onRequestPage(e: PageEvent): void {
    this.requestPageIndex = e.pageIndex;
    this.requestPageSize = e.pageSize;
  }

  requestStatusClass(code: number): string {
    if (code >= 500) {
      return 'rejected';
    }
    if (code >= 400) {
      return 'pending';
    }
    if (code >= 200 && code < 300) {
      return 'approved';
    }
    return 'submitted';
  }

  ngOnInit(): void {
    this.title.setTitle('Audit & activity | LX Admin');
    this.dataSource = [...this.mockRows];
    this.requestDataSource = [...this.mockRequestRows];
    this.loading = false;
  }

  stubImport(): void {}

  stubExport(): void {}

  stubRequestImport(): void {}

  stubRequestExport(): void {}
}
