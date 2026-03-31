import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { PageEvent } from '@angular/material/paginator';
import { filterByGlobalAndColumns } from '@shared/utils/table-search.util';

export interface HealthServiceRow {
  service: string;
  detail: string;
  status: string;
  statusLabel: string;
}

@Component({
  selector: 'app-system-health',
  templateUrl: './system-health.component.html',
  styleUrl: './system-health.component.scss',
  standalone: false,
})
export class SystemHealthComponent implements OnInit {
  loading = true;

  displayedColumns = ['service', 'detail', 'status', 'actions'];

  dataSource: HealthServiceRow[] = [];

  searchQuery = '';
  filterFieldsOpen = false;

  columnFilters = {
    service: '',
    detail: '',
    statusLabel: '',
  };

  pageIndex = 0;
  pageSize = 10;

  private readonly mockRows: HealthServiceRow[] = [
    {
      service: 'Admin API',
      detail: 'REST · last probe 12s ago',
      status: 'active',
      statusLabel: 'Operational',
    },
    {
      service: 'Authentication',
      detail: 'Token issuance · SSO hooks',
      status: 'active',
      statusLabel: 'Operational',
    },
    {
      service: 'Document store',
      detail: 'Upload pipeline · virus scan',
      status: 'active',
      statusLabel: 'Operational',
    },
    {
      service: 'Background jobs',
      detail: 'Queue workers · retries OK',
      status: 'pending',
      statusLabel: 'Degraded',
    },
    {
      service: 'Notifications',
      detail: 'Email · in-app',
      status: 'active',
      statusLabel: 'Operational',
    },
    {
      service: 'Primary database',
      detail: 'MySQL 8 · replication lag 0.4s',
      status: 'active',
      statusLabel: 'Operational',
    },
    {
      service: 'Cache layer',
      detail: 'Redis cluster · memory 62%',
      status: 'active',
      statusLabel: 'Operational',
    },
    {
      service: 'Message bus',
      detail: 'RabbitMQ · 1 node restarting',
      status: 'pending',
      statusLabel: 'Degraded',
    },
    {
      service: 'Object storage',
      detail: 'S3-compatible · multipart uploads',
      status: 'active',
      statusLabel: 'Operational',
    },
    {
      service: 'Search index',
      detail: 'OpenSearch · index lag 2m',
      status: 'active',
      statusLabel: 'Operational',
    },
  ];

  constructor(private readonly title: Title) {}

  get filteredRows(): HealthServiceRow[] {
    return filterByGlobalAndColumns(
      this.dataSource,
      this.searchQuery,
      this.columnFilters,
    );
  }

  get clampedPageIndex(): number {
    const total = this.filteredRows.length;
    if (total === 0) return 0;
    const max = Math.max(0, Math.ceil(total / this.pageSize) - 1);
    return Math.min(this.pageIndex, max);
  }

  get pagedRows(): HealthServiceRow[] {
    const all = this.filteredRows;
    const start = this.clampedPageIndex * this.pageSize;
    return all.slice(start, start + this.pageSize);
  }

  resetPaging(): void {
    this.pageIndex = 0;
  }

  onPage(e: PageEvent): void {
    this.pageIndex = e.pageIndex;
    this.pageSize = e.pageSize;
  }

  ngOnInit(): void {
    this.title.setTitle('System Health | LX Admin');
    this.dataSource = [...this.mockRows];
    this.loading = false;
  }

  stubImport(): void {}

  stubExport(): void {}
}
