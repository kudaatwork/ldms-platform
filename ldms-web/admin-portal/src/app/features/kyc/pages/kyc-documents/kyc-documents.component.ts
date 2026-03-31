import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { PageEvent } from '@angular/material/paginator';
import { filterByGlobalAndColumns } from '@shared/utils/table-search.util';

export interface KycDocumentRow {
  fileName: string;
  type: string;
  status: string;
  statusLabel: string;
}

@Component({
  selector: 'app-kyc-documents',
  templateUrl: './kyc-documents.component.html',
  styleUrl: './kyc-documents.component.scss',
  standalone: false,
})
export class KycDocumentsComponent implements OnInit {
  loading = true;

  displayedColumns = ['fileName', 'type', 'status', 'actions'];

  dataSource: KycDocumentRow[] = [];

  searchQuery = '';
  filterFieldsOpen = false;

  columnFilters = {
    fileName: '',
    type: '',
    statusLabel: '',
  };

  pageIndex = 0;
  pageSize = 10;

  private readonly mockRows: KycDocumentRow[] = [
    {
      fileName: 'company_registration.pdf',
      type: 'Registration',
      status: 'approved',
      statusLabel: 'Verified',
    },
    {
      fileName: 'director_id_scan.jpg',
      type: 'Identity',
      status: 'approved',
      statusLabel: 'Verified',
    },
    {
      fileName: 'tax_clearance_2026.pdf',
      type: 'Tax',
      status: 'submitted',
      statusLabel: 'In review',
    },
    {
      fileName: 'fleet_insurance.pdf',
      type: 'Insurance',
      status: 'pending',
      statusLabel: 'Awaiting upload',
    },
    {
      fileName: 'bank_confirmation_letter.pdf',
      type: 'Banking',
      status: 'rejected',
      statusLabel: 'Resubmit',
    },
    {
      fileName: 'operating_licence_zw.pdf',
      type: 'Licence',
      status: 'approved',
      statusLabel: 'Verified',
    },
    {
      fileName: 'beneficial_owners.xlsx',
      type: 'Ownership',
      status: 'submitted',
      statusLabel: 'In review',
    },
    {
      fileName: 'warehouse_lease.pdf',
      type: 'Property',
      status: 'pending',
      statusLabel: 'Awaiting upload',
    },
    {
      fileName: 'safety_policy.docx',
      type: 'Compliance',
      status: 'stage1',
      statusLabel: 'Stage 1',
    },
    {
      fileName: 'vat_certificate.pdf',
      type: 'Tax',
      status: 'approved',
      statusLabel: 'Verified',
    },
  ];

  constructor(private readonly title: Title) {}

  get filteredRows(): KycDocumentRow[] {
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

  get pagedRows(): KycDocumentRow[] {
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
    this.title.setTitle('KYC Documents | LX Admin');
    this.dataSource = [...this.mockRows];
    this.loading = false;
  }

  stubImport(): void {}

  stubExport(): void {}
}
