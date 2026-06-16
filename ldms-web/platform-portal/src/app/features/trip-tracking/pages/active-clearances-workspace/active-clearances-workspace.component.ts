import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { finalize } from 'rxjs/operators';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { NotificationService } from '../../../../core/services/notification.service';
import {
  BorderClearanceCaseRow,
  BorderClearanceDocumentType,
  BorderClearancePortalService,
  BorderClearanceStatus,
} from '../../services/border-clearance-portal.service';

export interface ClearanceMetrics {
  total: number;
  awaiting: number;
  atBorder: number;
  cleared: number;
}

@Component({
  selector: 'app-active-clearances-workspace',
  templateUrl: './active-clearances-workspace.component.html',
  styleUrl: './active-clearances-workspace.component.scss',
  standalone: false,
})
export class ActiveClearancesWorkspaceComponent implements OnInit {
  loading = true;
  loadError = '';
  cases: BorderClearanceCaseRow[] = [];
  statusFilter: BorderClearanceStatus | '' = '';
  selectedCase: BorderClearanceCaseRow | null = null;
  uploading = false;
  acting = false;
  documentType: BorderClearanceDocumentType = 'CUSTOMS_DECLARATION';
  borderName = '';

  metrics: ClearanceMetrics = { total: 0, awaiting: 0, atBorder: 0, cleared: 0 };

  readonly statusOptions: Array<{ value: BorderClearanceStatus | ''; label: string; icon: string }> = [
    { value: '', label: 'All cases', icon: 'folder_open' },
    { value: 'AWAITING_DOCUMENTS', label: 'Awaiting documents', icon: 'upload_file' },
    { value: 'SUBMITTED', label: 'At border', icon: 'flag' },
    { value: 'CLEARED', label: 'Cleared', icon: 'verified' },
    { value: 'REJECTED', label: 'Rejected', icon: 'block' },
  ];

  readonly documentTypes: Array<{ value: BorderClearanceDocumentType; label: string; icon: string }> = [
    { value: 'CUSTOMS_DECLARATION', label: 'Customs declaration', icon: 'gavel' },
    { value: 'COMMERCIAL_INVOICE', label: 'Commercial invoice', icon: 'receipt_long' },
    { value: 'BILL_OF_LADING', label: 'Bill of lading', icon: 'local_shipping' },
    { value: 'PERMIT', label: 'Permit', icon: 'verified_user' },
    { value: 'OTHER', label: 'Other', icon: 'attach_file' },
  ];

  readonly workflowSteps: Array<{ key: BorderClearanceStatus; label: string; icon: string }> = [
    { key: 'AWAITING_DOCUMENTS', label: 'Documents', icon: 'upload_file' },
    { key: 'SUBMITTED', label: 'At border', icon: 'flag' },
    { key: 'CLEARED', label: 'Cleared', icon: 'check_circle' },
  ];

  constructor(
    private readonly title: Title,
    private readonly borderClearance: BorderClearancePortalService,
    private readonly authState: AuthStateService,
    private readonly notifications: NotificationService,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Border clearance | LX Platform');
    this.reload();
  }

  reload(): void {
    this.loading = true;
    this.loadError = '';
    this.borderClearance
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
        error: (err: Error) => (this.loadError = err.message ?? 'Could not load clearance cases.'),
      });
  }

  setStatusFilter(value: BorderClearanceStatus | ''): void {
    if (this.statusFilter === value) {
      return;
    }
    this.statusFilter = value;
    this.selectedCase = null;
    this.reload();
  }

  selectCase(row: BorderClearanceCaseRow): void {
    this.selectedCase = row;
    this.borderName = row.borderName;
  }

  trackByCaseId(_index: number, row: BorderClearanceCaseRow): number {
    return row.id;
  }

  statusClass(tone: string): string {
    return `bcc-badge bcc-badge--${tone}`;
  }

  statusIcon(status: BorderClearanceStatus): string {
    switch (status) {
      case 'AWAITING_DOCUMENTS':
        return 'upload_file';
      case 'SUBMITTED':
      case 'UNDER_REVIEW':
        return 'flag';
      case 'CLEARED':
        return 'verified';
      case 'REJECTED':
        return 'block';
      default:
        return 'help_outline';
    }
  }

  documentIcon(type: string): string {
    const match = this.documentTypes.find((t) => t.value === type);
    return match?.icon ?? 'description';
  }

  workflowIndex(status: BorderClearanceStatus): number {
    if (status === 'REJECTED') {
      return -1;
    }
    if (status === 'UNDER_REVIEW') {
      return 1;
    }
    const idx = this.workflowSteps.findIndex((s) => s.key === status);
    return idx >= 0 ? idx : 0;
  }

  isWorkflowComplete(stepIndex: number, status: BorderClearanceStatus): boolean {
    const current = this.workflowIndex(status);
    if (status === 'REJECTED') {
      return false;
    }
    return current > stepIndex;
  }

  isWorkflowCurrent(stepIndex: number, status: BorderClearanceStatus): boolean {
    return this.workflowIndex(status) === stepIndex;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    const caseRow = this.selectedCase;
    const orgId = Number(this.authState.currentUser?.organizationId ?? 0);
    if (!file || !caseRow || !orgId) {
      return;
    }
    this.uploading = true;
    this.borderClearance
      .uploadDocument(orgId, file)
      .pipe(
        finalize(() => {
          this.uploading = false;
          input.value = '';
        }),
      )
      .subscribe({
        next: (fileUploadId) => {
          this.borderClearance
            .addDocument({
              caseId: caseRow.id,
              fileUploadId,
              documentType: this.documentType,
              fileName: file.name,
            })
            .subscribe({
              next: () => {
                this.notifications.success('Document uploaded.');
                this.reload();
              },
              error: (err: Error) => this.notifications.error(err.message),
            });
        },
        error: (err: Error) => this.notifications.error(err.message),
      });
  }

  submitCase(): void {
    if (!this.selectedCase) return;
    this.acting = true;
    this.borderClearance
      .submit(this.selectedCase.id)
      .pipe(finalize(() => (this.acting = false)))
      .subscribe({
        next: () => {
          this.notifications.success('Submitted for border review. Trip tracking will show a border stop.');
          this.reload();
        },
        error: (err: Error) => this.notifications.error(err.message),
      });
  }

  clearCase(): void {
    if (!this.selectedCase) return;
    this.acting = true;
    this.borderClearance
      .clear(this.selectedCase.id, this.borderName.trim() || undefined)
      .pipe(finalize(() => (this.acting = false)))
      .subscribe({
        next: () => {
          this.notifications.success('Border cleared — truck may proceed.');
          this.reload();
        },
        error: (err: Error) => this.notifications.error(err.message),
      });
  }

  rejectCase(): void {
    if (!this.selectedCase) return;
    this.acting = true;
    this.borderClearance
      .reject(this.selectedCase.id)
      .pipe(finalize(() => (this.acting = false)))
      .subscribe({
        next: () => {
          this.notifications.success('Clearance rejected.');
          this.reload();
        },
        error: (err: Error) => this.notifications.error(err.message),
      });
  }

  private updateMetrics(rows: BorderClearanceCaseRow[]): void {
    this.metrics = {
      total: rows.length,
      awaiting: rows.filter((r) => r.status === 'AWAITING_DOCUMENTS').length,
      atBorder: rows.filter((r) => r.status === 'SUBMITTED' || r.status === 'UNDER_REVIEW').length,
      cleared: rows.filter((r) => r.status === 'CLEARED').length,
    };
  }
}
