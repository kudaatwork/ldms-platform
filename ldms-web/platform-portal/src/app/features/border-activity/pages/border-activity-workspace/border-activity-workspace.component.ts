import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { finalize } from 'rxjs/operators';
import { NotificationService } from '../../../../core/services/notification.service';
import {
  BorderActivityCaseRow,
  BorderActivityPortalService,
  BorderActivityStatus,
} from '../../services/border-activity-portal.service';

export interface BorderActivityMetrics {
  total: number;
  awaiting: number;
  underReview: number;
  cleared: number;
  rejected: number;
}

@Component({
  selector: 'app-border-activity-workspace',
  templateUrl: './border-activity-workspace.component.html',
  styleUrl: './border-activity-workspace.component.scss',
  standalone: false,
})
export class BorderActivityWorkspaceComponent implements OnInit {
  loading = true;
  loadError = '';
  cases: BorderActivityCaseRow[] = [];
  statusFilter: BorderActivityStatus | '' = '';
  selectedCase: BorderActivityCaseRow | null = null;
  acting = false;
  actionNotes = '';
  showClearDialog = false;
  showRejectDialog = false;

  metrics: BorderActivityMetrics = { total: 0, awaiting: 0, underReview: 0, cleared: 0, rejected: 0 };

  readonly statusOptions: Array<{ value: BorderActivityStatus | ''; label: string; icon: string }> = [
    { value: '', label: 'All cases', icon: 'folder_open' },
    { value: 'AWAITING_DOCUMENTS', label: 'Awaiting docs', icon: 'upload_file' },
    { value: 'SUBMITTED', label: 'At border', icon: 'flag' },
    { value: 'UNDER_REVIEW', label: 'Under review', icon: 'hourglass_top' },
    { value: 'CLEARED', label: 'Cleared', icon: 'verified' },
    { value: 'REJECTED', label: 'Rejected', icon: 'block' },
  ];

  readonly workflowSteps: Array<{ key: BorderActivityStatus; label: string; icon: string }> = [
    { key: 'AWAITING_DOCUMENTS', label: 'Documents', icon: 'upload_file' },
    { key: 'SUBMITTED', label: 'At border', icon: 'flag' },
    { key: 'UNDER_REVIEW', label: 'Under review', icon: 'hourglass_top' },
    { key: 'CLEARED', label: 'Cleared', icon: 'check_circle' },
  ];

  constructor(
    private readonly title: Title,
    private readonly borderActivity: BorderActivityPortalService,
    private readonly notifications: NotificationService,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Border Activity | LX Platform');
    this.reload();
  }

  reload(): void {
    this.loading = true;
    this.loadError = '';
    this.borderActivity
      .listCases(this.statusFilter || undefined)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (rows) => {
          this.cases = rows;
          this.updateMetrics(rows);
          if (this.selectedCase) {
            this.selectedCase = rows.find((r) => r.id === this.selectedCase?.id) ?? null;
          } else if (rows.length) {
            this.selectCase(rows[0]);
          }
        },
        error: (err: Error) => (this.loadError = err.message ?? 'Could not load border activity cases.'),
      });
  }

  setStatusFilter(value: BorderActivityStatus | ''): void {
    this.statusFilter = value;
    this.reload();
  }

  selectCase(row: BorderActivityCaseRow): void {
    this.selectedCase = row;
    this.actionNotes = '';
    this.showClearDialog = false;
    this.showRejectDialog = false;
  }

  trackByCaseId(_index: number, row: BorderActivityCaseRow): number {
    return row.id;
  }

  statusClass(tone: BorderActivityCaseRow['statusTone']): string {
    const map: Record<string, string> = {
      muted: 'lx-org-meta__badge--default',
      warn: 'lx-org-meta__badge--warning',
      success: 'lx-org-meta__badge--success',
      danger: 'lx-org-meta__badge--danger',
      info: 'lx-org-meta__badge--info',
    };
    return `lx-org-meta__badge ${map[tone] ?? map['muted']}`;
  }

  statusIcon(status: BorderActivityStatus): string {
    const map: Record<BorderActivityStatus, string> = {
      AWAITING_DOCUMENTS: 'upload_file',
      SUBMITTED: 'flag',
      UNDER_REVIEW: 'hourglass_top',
      CLEARED: 'verified',
      REJECTED: 'block',
    };
    return map[status] ?? 'help';
  }

  documentIcon(type: string): string {
    const map: Record<string, string> = {
      CUSTOMS_DECLARATION: 'gavel',
      COMMERCIAL_INVOICE: 'receipt_long',
      BILL_OF_LADING: 'local_shipping',
      PERMIT: 'verified_user',
      OTHER: 'attach_file',
    };
    return map[type] ?? 'description';
  }

  isWorkflowComplete(stepIndex: number, status: BorderActivityStatus): boolean {
    const order = ['AWAITING_DOCUMENTS', 'SUBMITTED', 'UNDER_REVIEW', 'CLEARED'];
    const currentIndex = order.indexOf(status);
    if (currentIndex === -1) return false;
    return stepIndex <= currentIndex;
  }

  isWorkflowCurrent(stepIndex: number, status: BorderActivityStatus): boolean {
    const order = ['AWAITING_DOCUMENTS', 'SUBMITTED', 'UNDER_REVIEW', 'CLEARED'];
    const currentIndex = order.indexOf(status);
    return stepIndex === currentIndex;
  }

  openClearDialog(): void {
    this.actionNotes = '';
    this.showClearDialog = true;
    this.showRejectDialog = false;
  }

  openRejectDialog(): void {
    this.actionNotes = '';
    this.showRejectDialog = true;
    this.showClearDialog = false;
  }

  cancelAction(): void {
    this.showClearDialog = false;
    this.showRejectDialog = false;
    this.actionNotes = '';
  }

  confirmClear(): void {
    if (!this.selectedCase) return;
    this.acting = true;
    this.borderActivity
      .clear(this.selectedCase.id, this.selectedCase.borderName, this.actionNotes)
      .pipe(finalize(() => (this.acting = false)))
      .subscribe({
        next: () => {
          this.notifications.success('Case cleared successfully');
          this.showClearDialog = false;
          this.actionNotes = '';
          this.reload();
        },
        error: (err: Error) => this.notifications.error(err.message ?? 'Failed to clear case'),
      });
  }

  confirmReject(): void {
    if (!this.selectedCase) return;
    this.acting = true;
    this.borderActivity
      .reject(this.selectedCase.id, this.actionNotes)
      .pipe(finalize(() => (this.acting = false)))
      .subscribe({
        next: () => {
          this.notifications.success('Case rejected');
          this.showRejectDialog = false;
          this.actionNotes = '';
          this.reload();
        },
        error: (err: Error) => this.notifications.error(err.message ?? 'Failed to reject case'),
      });
  }

  private updateMetrics(rows: BorderActivityCaseRow[]): void {
    this.metrics = {
      total: rows.length,
      awaiting: rows.filter((r) => r.status === 'AWAITING_DOCUMENTS').length,
      underReview: rows.filter((r) => r.status === 'UNDER_REVIEW').length,
      cleared: rows.filter((r) => r.status === 'CLEARED').length,
      rejected: rows.filter((r) => r.status === 'REJECTED').length,
    };
  }
}
