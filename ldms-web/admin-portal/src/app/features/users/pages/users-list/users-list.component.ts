import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { PageEvent } from '@angular/material/paginator';
import { filterByGlobalAndColumns } from '@shared/utils/table-search.util';

export interface UserRow {
  email: string;
  name: string;
  role: string;
  status: string;
  statusLabel: string;
}

@Component({
  selector: 'app-users-list',
  templateUrl: './users-list.component.html',
  styleUrl: './users-list.component.scss',
  standalone: false,
})
export class UsersListComponent implements OnInit {
  loading = true;

  displayedColumns = ['email', 'name', 'role', 'status', 'actions'];

  dataSource: UserRow[] = [];

  searchQuery = '';
  filterFieldsOpen = false;

  columnFilters = {
    email: '',
    name: '',
    role: '',
    statusLabel: '',
  };

  pageIndex = 0;
  pageSize = 10;

  private readonly mockRows: UserRow[] = [
    {
      email: 'admin@projectlx.co.zw',
      name: 'Platform Admin',
      role: 'Platform Admin',
      status: 'active',
      statusLabel: 'Active',
    },
    {
      email: 'ops.reviewer@projectlx.co.zw',
      name: 'Tariro Moyo',
      role: 'KYC Reviewer',
      status: 'active',
      statusLabel: 'Active',
    },
    {
      email: 'finance@projectlx.co.zw',
      name: 'Brian Ndlovu',
      role: 'Finance',
      status: 'active',
      statusLabel: 'Active',
    },
    {
      email: 'viewer@projectlx.co.zw',
      name: 'Read-only User',
      role: 'Viewer',
      status: 'inactive',
      statusLabel: 'Inactive',
    },
  ];

  constructor(private readonly title: Title) {}

  get filteredRows(): UserRow[] {
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

  get pagedRows(): UserRow[] {
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
    this.title.setTitle('Users | LX Admin');
    this.dataSource = [...this.mockRows];
    this.loading = false;
  }

  stubImport(): void {}

  stubExport(): void {}
}
