import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { PageEvent } from '@angular/material/paginator';
import { filterByGlobalAndColumns } from '@shared/utils/table-search.util';

export interface OrganizationRow {
  name: string;
  classification: string;
  status: string;
  statusLabel: string;
}

@Component({
  selector: 'app-organizations-list',
  templateUrl: './organizations-list.component.html',
  styleUrl: './organizations-list.component.scss',
  standalone: false,
})
export class OrganizationsListComponent implements OnInit {
  loading = true;

  displayedColumns = ['name', 'classification', 'status', 'actions'];

  dataSource: OrganizationRow[] = [];

  searchQuery = '';
  filterFieldsOpen = false;

  columnFilters = {
    name: '',
    classification: '',
    statusLabel: '',
  };

  pageIndex = 0;
  pageSize = 10;

  private readonly mockRows: OrganizationRow[] = [
    {
      name: 'Nexus Logistics (Pvt) Ltd',
      classification: 'Carrier',
      status: 'active',
      statusLabel: 'Active',
    },
    {
      name: 'Chirundu Cold Chain Co.',
      classification: 'Shipper',
      status: 'active',
      statusLabel: 'Active',
    },
    {
      name: 'Great North Hauliers',
      classification: 'Carrier',
      status: 'pending',
      statusLabel: 'Pending review',
    },
    {
      name: 'Project LX Demo Warehouse',
      classification: 'Warehouse',
      status: 'active',
      statusLabel: 'Active',
    },
    {
      name: 'Beitbridge Transit Services',
      classification: 'Broker',
      status: 'inactive',
      statusLabel: 'Inactive',
    },
  ];

  constructor(private readonly title: Title) {}

  get filteredRows(): OrganizationRow[] {
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

  get pagedRows(): OrganizationRow[] {
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
    this.title.setTitle('Organizations | LX Admin');
    this.dataSource = [...this.mockRows];
    this.loading = false;
  }

  stubImport(): void {}

  stubExport(): void {}
}
