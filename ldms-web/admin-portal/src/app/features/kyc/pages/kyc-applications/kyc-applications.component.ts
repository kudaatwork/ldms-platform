import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PageEvent } from '@angular/material/paginator';
import { filterByGlobalAndColumns } from '@shared/utils/table-search.util';
import { MOCK_KYC_APPLICATION_ROWS, getKycApplicationDetail } from '../../data/kyc-applications.mock-data';
import type {
  KycApplicationDecisionResult,
  KycApplicationDetail,
  KycApplicationRow,
} from '../../models/kyc-application.model';
import { KycApplicationDeleteDialogComponent } from '../kyc-application-delete-dialog/kyc-application-delete-dialog.component';
import { KycApplicationDetailDialogComponent } from '../kyc-application-detail-dialog/kyc-application-detail-dialog.component';
import {
  KycApplicationEditDialogComponent,
  type KycApplicationEditResult,
} from '../kyc-application-edit-dialog/kyc-application-edit-dialog.component';

@Component({
  selector: 'app-kyc-applications',
  templateUrl: './kyc-applications.component.html',
  styleUrl: './kyc-applications.component.scss',
  standalone: false,
})
export class KycApplicationsComponent implements OnInit {
  loading = true;

  displayedColumns = ['applicant', 'submitted', 'status', 'actions'];

  dataSource: KycApplicationRow[] = [];

  searchQuery = '';
  filterFieldsOpen = false;

  columnFilters = {
    applicant: '',
    submitted: '',
    statusLabel: '',
  };

  pageIndex = 0;
  pageSize = 10;

  constructor(
    private readonly title: Title,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
  ) {}

  get filteredRows(): KycApplicationRow[] {
    return filterByGlobalAndColumns(
      this.dataSource,
      this.searchQuery,
      this.columnFilters,
    );
  }

  get clampedPageIndex(): number {
    const total = this.filteredRows.length;
    if (total === 0) {
      return 0;
    }
    const max = Math.max(0, Math.ceil(total / this.pageSize) - 1);
    return Math.min(this.pageIndex, max);
  }

  get pagedRows(): KycApplicationRow[] {
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
    this.title.setTitle('KYC Applications | LX Admin');
    this.dataSource = MOCK_KYC_APPLICATION_ROWS.map((r) => ({ ...r }));
    this.loading = false;
  }

  openApplication(row: KycApplicationRow): void {
    const base = getKycApplicationDetail(row.id);
    if (!base) {
      return;
    }
    const detail: KycApplicationDetail = {
      ...base,
      applicant: row.applicant,
      submitted: row.submitted,
      status: row.status,
      statusLabel: row.statusLabel,
    };
    this.dialog
      .open(KycApplicationDetailDialogComponent, {
        width: 'min(760px, 96vw)',
        maxHeight: '92vh',
        autoFocus: 'first-tabbable',
        data: { detail },
      })
      .afterClosed()
      .subscribe((result: KycApplicationDecisionResult | undefined) => {
        if (!result) {
          return;
        }
        const idx = this.dataSource.findIndex((r) => r.id === row.id);
        if (idx < 0) {
          return;
        }
        const next = [...this.dataSource];
        if (result.decision === 'approve') {
          next[idx] = { ...next[idx], status: 'approved', statusLabel: 'Approved' };
        } else {
          next[idx] = { ...next[idx], status: 'rejected', statusLabel: 'Rejected' };
        }
        this.dataSource = next;
        const msg =
          result.decision === 'approve'
            ? 'Application accepted and marked approved.'
            : 'Application rejected.';
        this.snackBar.open(msg, 'Dismiss', { duration: 6000 });
      });
  }

  editApplication(row: KycApplicationRow): void {
    this.dialog
      .open(KycApplicationEditDialogComponent, {
        width: 'min(480px, 92vw)',
        autoFocus: 'first-tabbable',
        data: { row },
      })
      .afterClosed()
      .subscribe((result: KycApplicationEditResult | undefined) => {
        if (!result) {
          return;
        }
        const idx = this.dataSource.findIndex((r) => r.id === row.id);
        if (idx < 0) {
          return;
        }
        const next = [...this.dataSource];
        next[idx] = {
          ...next[idx],
          applicant: result.applicant,
          status: result.status,
          statusLabel: result.statusLabel,
        };
        this.dataSource = next;
        this.snackBar.open('Application updated.', 'Dismiss', { duration: 4000 });
      });
  }

  deleteApplication(row: KycApplicationRow): void {
    this.dialog
      .open(KycApplicationDeleteDialogComponent, {
        width: 'min(420px, 90vw)',
        autoFocus: 'first-tabbable',
        data: { applicant: row.applicant, id: row.id },
      })
      .afterClosed()
      .subscribe((confirmed: boolean | undefined) => {
        if (confirmed !== true) {
          return;
        }
        this.dataSource = this.dataSource.filter((r) => r.id !== row.id);
        this.snackBar.open('Application removed from queue.', 'Dismiss', { duration: 5000 });
      });
  }

  stubImport(): void {
    /* Wire file picker / API */
  }

  stubExport(): void {
    /* Wire export API */
  }
}
