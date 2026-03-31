import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { PageEvent } from '@angular/material/paginator';
import { filterByGlobalAndColumns } from '@shared/utils/table-search.util';

export interface RoleRow {
  role: string;
  permissions: number;
  status: string;
  statusLabel: string;
}

@Component({
  selector: 'app-users-roles',
  templateUrl: './users-roles.component.html',
  styleUrl: './users-roles.component.scss',
  standalone: false,
})
export class UsersRolesComponent implements OnInit {
  loading = true;

  displayedColumns = ['role', 'permissions', 'status', 'actions'];

  dataSource: RoleRow[] = [];

  searchQuery = '';
  filterFieldsOpen = false;

  columnFilters = {
    role: '',
    permissions: '',
    statusLabel: '',
  };

  pageIndex = 0;
  pageSize = 10;

  private readonly mockRows: RoleRow[] = [
    {
      role: 'Platform Admin',
      permissions: 64,
      status: 'active',
      statusLabel: 'Active',
    },
    {
      role: 'KYC Reviewer',
      permissions: 18,
      status: 'active',
      statusLabel: 'Active',
    },
    {
      role: 'Org Manager',
      permissions: 24,
      status: 'active',
      statusLabel: 'Active',
    },
    {
      role: 'Viewer',
      permissions: 6,
      status: 'active',
      statusLabel: 'Active',
    },
  ];

  constructor(private readonly title: Title) {}

  get filteredRows(): RoleRow[] {
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

  get pagedRows(): RoleRow[] {
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
    this.title.setTitle('Roles | LX Admin');
    this.dataSource = [...this.mockRows];
    this.loading = false;
  }

  stubImport(): void {}

  stubExport(): void {}
}
