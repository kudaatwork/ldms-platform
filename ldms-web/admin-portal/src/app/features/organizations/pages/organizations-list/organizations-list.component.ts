import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { filterByGlobalAndColumns } from '@shared/utils/table-search.util';
import { DEFAULT_TABLE_PAGE_SIZE } from '@shared/constants/table-pagination';
import {
  LxExportFormat,
  exportClientTableAsCsv,
} from '@shared/utils/lx-export.util';

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
  pageSize = DEFAULT_TABLE_PAGE_SIZE;

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

  constructor(
    private readonly title: Title,
    private readonly snackBar: MatSnackBar,
  ) {}

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

  exportAs(format: LxExportFormat): void {
    const ok = exportClientTableAsCsv(
      format,
      this.filteredRows,
      [
        { header: 'name', value: (r) => r.name },
        { header: 'classification', value: (r) => r.classification },
        { header: 'status', value: (r) => r.statusLabel },
      ],
      'organizations',
      (message) => this.snackBar.open(message, 'Close', { duration: 4500 }),
    );
    if (ok) {
      this.snackBar.open('Exported organizations as CSV.', 'Close', {
        duration: 3500,
        panelClass: ['app-snackbar-success'],
      });
    }
  }
}
