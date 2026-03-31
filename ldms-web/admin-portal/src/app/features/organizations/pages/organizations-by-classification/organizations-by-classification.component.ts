import { Component, OnInit, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { map } from 'rxjs/operators';
import { PageEvent } from '@angular/material/paginator';
import { ORG_CLASSIFICATIONS } from '../../../../shared/models/org-classifications';
import { filterByGlobalAndColumns } from '@shared/utils/table-search.util';

export interface OrgByClassRow {
  name: string;
  status: string;
  statusLabel: string;
}

@Component({
  selector: 'app-organizations-by-classification',
  templateUrl: './organizations-by-classification.component.html',
  styleUrl: './organizations-by-classification.component.scss',
  standalone: false,
})
export class OrganizationsByClassificationComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly title = inject(Title);

  loading = true;

  readonly slug$ = this.route.paramMap.pipe(map((p) => p.get('slug') ?? ''));

  displayedColumns = ['name', 'status', 'actions'];

  dataSource: OrgByClassRow[] = [];

  searchQuery = '';
  filterFieldsOpen = false;

  columnFilters = {
    name: '',
    statusLabel: '',
  };

  pageIndex = 0;
  pageSize = 10;

  private readonly mockRows: OrgByClassRow[] = [
    {
      name: 'Nexus Logistics (Pvt) Ltd',
      status: 'active',
      statusLabel: 'Active',
    },
    {
      name: 'Great North Hauliers',
      status: 'pending',
      statusLabel: 'Onboarding',
    },
    {
      name: 'Beitbridge Transit Services',
      status: 'active',
      statusLabel: 'Active',
    },
    {
      name: 'Mutare Linehaul Co-op',
      status: 'inactive',
      statusLabel: 'Inactive',
    },
    {
      name: 'Harare Metro Couriers',
      status: 'active',
      statusLabel: 'Active',
    },
    {
      name: 'Bulawayo Bulk Haulage',
      status: 'pending',
      statusLabel: 'Onboarding',
    },
  ];

  get filteredRows(): OrgByClassRow[] {
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

  get pagedRows(): OrgByClassRow[] {
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
    this.slug$.subscribe((slug) => {
      const label = this.labelFor(slug);
      this.title.setTitle(`${label} · Organizations | LX Admin`);
    });
    this.dataSource = [...this.mockRows];
    this.loading = false;
  }

  labelFor(slug: string): string {
    return ORG_CLASSIFICATIONS.find((c) => c.slug === slug)?.label ?? slug;
  }

  stubImport(): void {}

  stubExport(): void {}
}
