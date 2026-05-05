import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { PageEvent } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { Title } from '@angular/platform-browser';
import { finalize } from 'rxjs';
import {
  AuditLogAdminService,
  AuditLogChurnHistoryDto,
  ChurnOutHistoryFilters,
} from '../../services/audit-log-admin.service';

@Component({
  selector: 'app-churnout-history-page',
  templateUrl: './churnout-history-page.component.html',
  styleUrl: './churnout-history-page.component.scss',
  standalone: false,
})
export class ChurnoutHistoryPageComponent implements OnInit {
  loading = false;
  totalElements = 0;
  pageIndex = 0;
  pageSize = 20;
  displayedColumns = ['triggeredAt', 'triggerType', 'triggeredBy', 'deletedLogCount', 'status', 'batchReference'];
  dataSource = new MatTableDataSource<AuditLogChurnHistoryDto>([]);
  filters: ChurnOutHistoryFilters = {
    triggerType: '',
    status: '',
    triggeredBy: '',
    batchReference: '',
    from: '',
    to: '',
  };
  readonly triggerTypeOptions = ['MANUAL', 'SCHEDULED', 'SYSTEM'];
  readonly statusOptions = ['RUNNING', 'SUCCESS', 'FAILED'];
  exporting = false;

  constructor(
    private readonly title: Title,
    private readonly auditLogAdmin: AuditLogAdminService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Churnout history | LX Admin');
    this.load();
  }

  onPage(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.load();
  }

  applyFilters(): void {
    this.pageIndex = 0;
    this.load();
  }

  resetFilters(): void {
    this.filters = {
      triggerType: '',
      status: '',
      triggeredBy: '',
      batchReference: '',
      from: '',
      to: '',
    };
    this.pageIndex = 0;
    this.load();
  }

  export(format: 'csv' | 'xlsx' | 'pdf'): void {
    const requestFilters = this.buildRequestFilters();
    this.exporting = true;
    this.auditLogAdmin
      .exportChurnOutHistory(
        {
          ...requestFilters,
          page: 0,
          size: 5000,
          searchValue: '',
        },
        format,
      )
      .pipe(
        finalize(() => {
          this.exporting = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe((blob) => {
        const url = window.URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = `audit-log-churn-history.${format === 'xlsx' ? 'xlsx' : format}`;
        anchor.click();
        window.URL.revokeObjectURL(url);
      });
  }

  private load(): void {
    this.loading = true;
    const requestFilters = this.buildRequestFilters();
    this.auditLogAdmin
      .getChurnOutHistory(this.pageIndex, this.pageSize, requestFilters)
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe((response) => {
        const page = response.churnHistoryPage;
        this.dataSource.data = page?.content ?? [];
        this.totalElements = page?.totalElements ?? this.dataSource.data.length;
        this.cdr.markForCheck();
      });
  }

  private buildRequestFilters(): ChurnOutHistoryFilters {
    return {
      triggerType: this.filters.triggerType?.trim() || undefined,
      status: this.filters.status?.trim() || undefined,
      triggeredBy: this.filters.triggeredBy?.trim() || undefined,
      batchReference: this.filters.batchReference?.trim() || undefined,
      from: this.filters.from ? this.toLocalDateTime(this.filters.from) : undefined,
      to: this.filters.to ? this.toLocalDateTime(this.filters.to) : undefined,
    };
  }

  private toLocalDateTime(value: string): string {
    // Backend expects LocalDateTime (no timezone suffix).
    return value.length === 16 ? `${value}:00` : value;
  }
}
