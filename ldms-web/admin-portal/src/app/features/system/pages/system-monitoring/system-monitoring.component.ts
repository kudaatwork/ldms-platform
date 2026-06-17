import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { PageEvent } from '@angular/material/paginator';
import { filterByGlobalAndColumns } from '@shared/utils/table-search.util';
import { DEFAULT_TABLE_PAGE_SIZE } from '@shared/constants/table-pagination';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  LxExportFormat,
  exportClientTableAsCsv,
  exportFormatLabel,
} from '@shared/utils/lx-export.util';

export interface MonitoringRow {
  metric: string;
  value: string;
  status: string;
  statusLabel: string;
}

@Component({
  selector: 'app-system-monitoring',
  templateUrl: './system-monitoring.component.html',
  styleUrl: './system-monitoring.component.scss',
  standalone: false,
})
export class SystemMonitoringComponent implements OnInit {
  loading = true;

  displayedColumns = ['metric', 'value', 'status', 'actions'];

  dataSource: MonitoringRow[] = [];

  searchQuery = '';
  filterFieldsOpen = false;

  columnFilters = {
    metric: '',
    value: '',
    statusLabel: '',
  };

  pageIndex = 0;
  pageSize = DEFAULT_TABLE_PAGE_SIZE;

  private readonly mockRows: MonitoringRow[] = [
    {
      metric: 'API latency (p95)',
      value: '118 ms',
      status: 'active',
      statusLabel: 'Healthy',
    },
    {
      metric: 'Auth service',
      value: '99.98% · 24h',
      status: 'active',
      statusLabel: 'Healthy',
    },
    {
      metric: 'Job queue depth',
      value: '42 jobs',
      status: 'pending',
      statusLabel: 'Elevated',
    },
    {
      metric: 'Error rate (5m)',
      value: '0.02%',
      status: 'active',
      statusLabel: 'Healthy',
    },
    {
      metric: 'DB connections',
      value: '38 / 100',
      status: 'active',
      statusLabel: 'Healthy',
    },
  ];

  constructor(
    private readonly title: Title,
    private readonly snackBar: MatSnackBar,
  ) {}

  get filteredRows(): MonitoringRow[] {
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

  get pagedRows(): MonitoringRow[] {
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
    this.title.setTitle('Monitoring | LX Admin');
    this.dataSource = [...this.mockRows];
    this.loading = false;
  }

  stubImport(): void {}

  exportAs(format: LxExportFormat): void {
    const ok = exportClientTableAsCsv(
      format,
      this.filteredRows,
      [
        { header: 'metric', value: (r) => r.metric },
        { header: 'value', value: (r) => r.value },
        { header: 'status', value: (r) => r.statusLabel },
      ],
      'system-monitoring',
      (message) => this.snackBar.open(message, 'Close', { duration: 4500 }),
    );
    if (ok) {
      this.snackBar.open(`Exported monitoring metrics as ${exportFormatLabel(format)}.`, 'Close', {
        duration: 3500,
        panelClass: ['app-snackbar-success'],
      });
    }
  }
}
