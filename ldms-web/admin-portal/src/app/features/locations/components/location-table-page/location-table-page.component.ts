import { Component, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { Title } from '@angular/platform-browser';
import { Observable, Subject, takeUntil } from 'rxjs';
import { map } from 'rxjs/operators';
import { filterByGlobalAndColumns } from '@shared/utils/table-search.util';
import { LocationsService } from '../../services/locations.service';
import type { LocationEntityKind } from '../../models/location.models';
import {
  LocationFormDialogComponent,
  type LocationFormDialogMode,
} from '../location-form-dialog/location-form-dialog.component';

type Row = Record<string, unknown>;

const CONFIG: Record<
  LocationEntityKind,
  {
    pageTitle: string;
    browserTitle: string;
    lead: string;
    columns: string[];
    labels: Record<string, string>;
    formMode: LocationFormDialogMode;
  }
> = {
  country: {
    pageTitle: 'Countries',
    browserTitle: 'Countries | LX Admin',
    lead: 'ISO reference countries in the locations catalog.',
    columns: [
      'name',
      'isoAlpha2Code',
      'isoAlpha3Code',
      'dialCode',
      'timezone',
      'currencyCode',
      'entityStatus',
      'actions',
    ],
    labels: {
      name: 'Name',
      isoAlpha2Code: 'ISO α2',
      isoAlpha3Code: 'ISO α3',
      dialCode: 'Dial code',
      timezone: 'Timezone',
      currencyCode: 'Currency',
      entityStatus: 'Status',
      actions: 'Actions',
    },
    formMode: 'country',
  },
  province: {
    pageTitle: 'Provinces',
    browserTitle: 'Provinces | LX Admin',
    lead: 'Provinces linked to countries (e.g. Mashonaland West, Limpopo).',
    columns: ['name', 'code', 'countryId', 'administrativeLevelId', 'entityStatus', 'actions'],
    labels: {
      name: 'Name',
      code: 'Code',
      countryId: 'Country ID',
      administrativeLevelId: 'Admin level ID',
      entityStatus: 'Status',
      actions: 'Actions',
    },
    formMode: 'province',
  },
  district: {
    pageTitle: 'Districts',
    browserTitle: 'Districts | LX Admin',
    lead: 'Districts within provinces.',
    columns: ['name', 'code', 'provinceId', 'administrativeLevelId', 'entityStatus', 'actions'],
    labels: {
      name: 'Name',
      code: 'Code',
      provinceId: 'Province ID',
      administrativeLevelId: 'Admin level ID',
      entityStatus: 'Status',
      actions: 'Actions',
    },
    formMode: 'district',
  },
  city: {
    pageTitle: 'Cities',
    browserTitle: 'Cities | LX Admin',
    lead: 'City location nodes (LocationType.CITY).',
    columns: [
      'name',
      'code',
      'locationType',
      'parentName',
      'timezone',
      'entityStatus',
      'actions',
    ],
    labels: {
      name: 'Name',
      code: 'Code',
      locationType: 'Type',
      parentName: 'Parent',
      timezone: 'Timezone',
      entityStatus: 'Status',
      actions: 'Actions',
    },
    formMode: 'city',
  },
  suburb: {
    pageTitle: 'Suburbs',
    browserTitle: 'Suburbs | LX Admin',
    lead: 'Suburbs linked to districts.',
    columns: ['name', 'code', 'districtId', 'postalCode', 'entityStatus', 'actions'],
    labels: {
      name: 'Name',
      code: 'Code',
      districtId: 'District ID',
      postalCode: 'Postal code',
      entityStatus: 'Status',
      actions: 'Actions',
    },
    formMode: 'suburb',
  },
  village: {
    pageTitle: 'Villages',
    browserTitle: 'Villages | LX Admin',
    lead: 'Village location nodes (LocationType.VILLAGE).',
    columns: [
      'name',
      'code',
      'locationType',
      'parentName',
      'timezone',
      'entityStatus',
      'actions',
    ],
    labels: {
      name: 'Name',
      code: 'Code',
      locationType: 'Type',
      parentName: 'Parent',
      timezone: 'Timezone',
      entityStatus: 'Status',
      actions: 'Actions',
    },
    formMode: 'village',
  },
  'admin-level': {
    pageTitle: 'Administrative levels',
    browserTitle: 'Administrative levels | LX Admin',
    lead: 'Administrative level definitions (ADM1, ADM2, …).',
    columns: ['name', 'code', 'level', 'description', 'entityStatus', 'actions'],
    labels: {
      name: 'Name',
      code: 'Code',
      level: 'Level',
      description: 'Description',
      entityStatus: 'Status',
      actions: 'Actions',
    },
    formMode: 'admin-level',
  },
};

@Component({
  selector: 'app-location-table-page',
  templateUrl: './location-table-page.component.html',
  styleUrl: './location-table-page.component.scss',
  standalone: false,
})
export class LocationTablePageComponent implements OnInit, OnDestroy {
  @Input({ required: true }) entity!: LocationEntityKind;

  @ViewChild(MatPaginator)
  set paginator(p: MatPaginator) {
    if (p) {
      this.dataSource.paginator = p;
    }
  }

  @ViewChild(MatSort)
  set sort(s: MatSort) {
    if (s) {
      this.dataSource.sort = s;
    }
  }

  loading = true;
  searchQuery = '';
  filterFieldsOpen = false;
  columnFilters: Record<string, string> = {};

  displayedColumns: string[] = [];
  columnLabels: Record<string, string> = {};
  pageTitle = '';
  pageLead = '';

  dataSource = new MatTableDataSource<Row>([]);
  rawRows: Row[] = [];

  private destroy$ = new Subject<void>();

  constructor(
    private readonly locationsService: LocationsService,
    private readonly title: Title,
    private readonly dialog: MatDialog,
  ) {}

  ngOnInit(): void {
    const cfg = CONFIG[this.entity];
    this.pageTitle = cfg.pageTitle;
    this.pageLead = cfg.lead;
    this.displayedColumns = cfg.columns;
    this.columnLabels = cfg.labels;
    this.columnFilters = Object.fromEntries(cfg.columns.filter((c) => c !== 'actions').map((c) => [c, '']));
    this.title.setTitle(cfg.browserTitle);
    this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get filterableKeys(): string[] {
    return this.displayedColumns.filter((c) => c !== 'actions');
  }

  applyFilters(): void {
    const filtered = filterByGlobalAndColumns(
      this.rawRows,
      this.searchQuery,
      this.columnFilters as Record<string, string>,
    );
    this.dataSource.data = filtered;
    if (this.paginator) {
      this.paginator.firstPage();
    }
  }

  displayCell(row: Row, key: string): string {
    const v = row[key];
    if (v === null || v === undefined) return '—';
    if (Array.isArray(v)) return v.join(', ');
    return String(v);
  }

  statusClass(row: Row): string {
    const raw = String(row['entityStatus'] ?? 'active').toLowerCase();
    if (raw === 'active') return 'active';
    if (raw === 'inactive') return 'inactive';
    if (raw === 'deleted') return 'deleted';
    return 'pending';
  }

  statusLabel(row: Row): string {
    const s = row['entityStatus'];
    if (s === 'ACTIVE') return 'Active';
    if (s === 'INACTIVE') return 'Inactive';
    if (s === 'DELETED') return 'Deleted';
    return s != null ? String(s) : '—';
  }

  openAdd(): void {
    const cfg = CONFIG[this.entity];
    this.dialog
      .open(LocationFormDialogComponent, {
        width: '520px',
        data: { mode: cfg.formMode },
      })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.load();
        }
      });
  }

  stubImport(): void {}

  stubExport(): void {}

  private load(): void {
    this.loading = true;
    this.pickLoader()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (rows) => {
          this.rawRows = rows as Row[];
          this.applyFilters();
          this.loading = false;
        },
        error: () => {
          this.rawRows = [];
          this.applyFilters();
          this.loading = false;
        },
      });
  }

  private pickLoader(): Observable<Row[]> {
    switch (this.entity) {
      case 'country':
        return this.locationsService.getCountries().pipe(map((r) => r as unknown as Row[]));
      case 'province':
        return this.locationsService.getProvinces().pipe(map((r) => r as unknown as Row[]));
      case 'district':
        return this.locationsService.getDistricts().pipe(map((r) => r as unknown as Row[]));
      case 'city':
        return this.locationsService.getCities().pipe(map((r) => r as unknown as Row[]));
      case 'suburb':
        return this.locationsService.getSuburbs().pipe(map((r) => r as unknown as Row[]));
      case 'village':
        return this.locationsService.getVillages().pipe(map((r) => r as unknown as Row[]));
      case 'admin-level':
        return this.locationsService.getAdminLevels().pipe(map((r) => r as unknown as Row[]));
    }
  }
}
