import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, catchError, finalize, of, takeUntil } from 'rxjs';
import { DEFAULT_TABLE_PAGE_SIZE } from '@shared/constants/table-pagination';
import type { KycApplicationDocument } from '../../../organizations/models/organization.model';
import { OrganizationsAdminService } from '../../../organizations/services/organizations-admin.service';

interface OrgDocRow {
  id: number;
  name: string;
  classificationLabel: string;
  documentCount: number;
}

@Component({
  selector: 'app-settings-organization-documents',
  templateUrl: './settings-organization-documents.component.html',
  styleUrl: './settings-organization-documents.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class SettingsOrganizationDocumentsComponent implements OnInit, OnDestroy {
  loading = true;
  orgRows: OrgDocRow[] = [];
  filteredOrgRows: OrgDocRow[] = [];
  orgSearch = '';
  error = '';

  selectedOrgId: number | null = null;
  selectedOrgName = '';
  selectedOrgKycStatus = '';
  documentsLoading = false;
  documents: KycApplicationDocument[] = [];
  documentsError = '';

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly orgs: OrganizationsAdminService,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadOrganizations();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get hasSelection(): boolean {
    return this.selectedOrgId !== null;
  }

  onOrgSearch(value: string): void {
    this.orgSearch = value;
    this.applyOrgFilter();
    this.cdr.markForCheck();
  }

  selectOrg(row: OrgDocRow): void {
    if (this.selectedOrgId === row.id) {
      return;
    }
    this.selectedOrgId = row.id;
    this.selectedOrgName = row.name;
    this.selectedOrgKycStatus = '';
    this.documents = [];
    this.documentsError = '';
    this.loadDocuments(row.id);
    this.cdr.markForCheck();
  }

  trackOrg(_index: number, row: OrgDocRow): number {
    return row.id;
  }

  private loadOrganizations(): void {
    this.loading = true;
    this.error = '';
    this.orgs
      .queryTablePage({
        page: 0,
        size: DEFAULT_TABLE_PAGE_SIZE,
        searchQuery: '',
        columnFilters: {},
        kycQueueOnly: false,
        organizationDirectoryOnly: true,
      })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (result) => {
          this.orgRows = result.rows.map((row) => ({
            id: row.id,
            name: row.name,
            classificationLabel: row.classificationLabel,
            documentCount: 0,
          }));
          this.applyOrgFilter();
        },
        error: () => {
          this.error = 'Could not load organisations. Ensure ldms-organization-management is running.';
          this.orgRows = [];
          this.filteredOrgRows = [];
        },
      });
  }

  private applyOrgFilter(): void {
    const q = this.orgSearch.trim().toLowerCase();
    if (!q) {
      this.filteredOrgRows = this.orgRows;
      return;
    }
    this.filteredOrgRows = this.orgRows.filter(
      (row) =>
        row.name.toLowerCase().includes(q) ||
        row.classificationLabel.toLowerCase().includes(q),
    );
  }

  private loadDocuments(orgId: number): void {
    this.documentsLoading = true;
    this.orgs
      .getOrganization(orgId)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.documentsLoading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (detail) => {
          this.documents = detail.documents ?? [];
          this.documentsError = '';
          this.selectedOrgKycStatus = detail.kycStatus ?? '';
          // Update document count on the row
          const row = this.orgRows.find((r) => r.id === orgId);
          if (row) {
            row.documentCount = this.documents.length;
          }
        },
        error: () => {
          this.documents = [];
          this.documentsError = 'Could not load documents for this organisation.';
          this.snackBar.open('Could not load organisation documents.', 'Close', {
            duration: 5000,
            panelClass: ['app-snackbar-error'],
          });
        },
      });
  }
}
