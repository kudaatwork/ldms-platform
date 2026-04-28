import { ChangeDetectorRef, Component, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { Title } from '@angular/platform-browser';
import {
  EMPTY,
  Observable,
  Subject,
  catchError,
  debounceTime,
  finalize,
  merge,
  of,
  switchMap,
  takeUntil,
  tap,
} from 'rxjs';
import { map } from 'rxjs/operators';
import {
  LocationsService,
  LOCATIONS_TABLE_PAGE_SIZE,
  type LocationSelectOption,
} from '../../services/locations.service';
import type { LocationEntityKind } from '../../models/location.models';
import {
  LocationFormDialogComponent,
  type LocationFormDialogAction,
  type LocationFormDialogData,
  type LocationFormDialogResult,
  type LocationFormDialogMode,
} from '../location-form-dialog/location-form-dialog.component';
import { DeleteConfirmDialogComponent } from '../delete-confirm-dialog/delete-confirm-dialog.component';

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
      countryId: 'Country',
      administrativeLevelId: 'Admin level',
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
      provinceId: 'Province',
      administrativeLevelId: 'Admin level',
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
      districtId: 'District',
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

  @ViewChild(MatSort)
  set sort(s: MatSort) {
    if (s) {
      this.dataSource.sort = s;
    }
  }

  fetching = false;
  /** Import / delete / dependency checks — blocks mutating actions only. */
  actionInProgress = false;
  /** Export runs separately so the table and Refresh stay usable during long downloads. */
  exporting = false;
  sampleCsvDescription: string | null = null;
  /** When true, the sample CSV explanatory paragraph is rendered below the controls header.
   *  Toggled by the small "i" button so the page stays uncluttered until users ask for help. */
  showSampleCsvInfo = false;
  /** Tooltip and copy for CSV import (format + leave GEOCOORDINATES ID blank). */
  readonly importCsvTooltip =
    'Import accepts CSV files only (UTF-8). Do not populate GEOCOORDINATES ID — the server creates or links coordinates automatically.';
  readonly importCsvDisclaimer =
    'CSV import only. Leave GEOCOORDINATES ID empty in your file; the service assigns coordinates.';
  searchQuery = '';
  filterFieldsOpen = false;
  columnFilters: Record<string, string> = {};

  /** Server-driven pagination */
  pageIndex = 0;
  pageSize = LOCATIONS_TABLE_PAGE_SIZE;
  totalRecords = 0;

  displayedColumns: string[] = [];
  columnLabels: Record<string, string> = {};
  pageTitle = '';
  pageLead = '';

  dataSource = new MatTableDataSource<Row>([]);
  rawRows: Row[] = [];

  /** Options for FK column filters (province / district / suburb grids). */
  filterFkOptions: Record<string, LocationSelectOption[]> = {};

  private destroy$ = new Subject<void>();
  private readonly filterReload$ = new Subject<void>();
  /** Monotonic token to ignore stale in-flight table responses. */
  private latestLoadToken = 0;
  /** Prevent duplicate reloads when form controls emit initial default values. */
  private lastFilterSignature = '';

  constructor(
    private readonly locationsService: LocationsService,
    private readonly title: Title,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const cfg = CONFIG[this.entity];
    this.pageTitle = cfg.pageTitle;
    this.pageLead = cfg.lead;
    this.displayedColumns = cfg.columns;
    this.columnLabels = cfg.labels;
    this.columnFilters = Object.fromEntries(cfg.columns.filter((c) => c !== 'actions').map((c) => [c, '']));
    this.lastFilterSignature = this.currentFilterSignature();
    this.title.setTitle(cfg.browserTitle);
    this.sampleCsvDescription = this.locationsService.getSampleCsvDescription(this.entity);
    this.loadFilterFkOptions();
    merge(
      of(undefined as void),
      this.filterReload$.pipe(debounceTime(150)),
    )
      .pipe(
        switchMap(() => {
          this.pageIndex = 0;
          return this.runTableQuery({ background: false });
        }),
        takeUntil(this.destroy$),
      )
      .subscribe();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get filterableKeys(): string[] {
    return this.displayedColumns.filter((c) => c !== 'actions');
  }

  isFkFilterColumn(key: string): boolean {
    const keys: Partial<Record<LocationEntityKind, string[]>> = {
      province: ['countryId', 'administrativeLevelId'],
      district: ['provinceId', 'administrativeLevelId'],
      suburb: ['districtId'],
    };
    return keys[this.entity]?.includes(key) ?? false;
  }

  fkFilterOptionsFor(key: string): LocationSelectOption[] {
    return this.filterFkOptions[key] ?? [];
  }

  private loadFilterFkOptions(): void {
    switch (this.entity) {
      case 'province':
        this.locationsService
          .fetchCountriesForSelect()
          .pipe(takeUntil(this.destroy$))
          .subscribe((o) => {
            this.filterFkOptions['countryId'] = o;
            this.cdr.markForCheck();
          });
        this.locationsService
          .fetchAdministrativeLevelsForSelect()
          .pipe(takeUntil(this.destroy$))
          .subscribe((o) => {
            this.filterFkOptions['administrativeLevelId'] = o;
            this.cdr.markForCheck();
          });
        break;
      case 'district':
        this.locationsService
          .fetchProvincesForSelect()
          .pipe(takeUntil(this.destroy$))
          .subscribe((o) => {
            this.filterFkOptions['provinceId'] = o;
            this.cdr.markForCheck();
          });
        this.locationsService
          .fetchAdministrativeLevelsForSelect()
          .pipe(takeUntil(this.destroy$))
          .subscribe((o) => {
            this.filterFkOptions['administrativeLevelId'] = o;
            this.cdr.markForCheck();
          });
        break;
      case 'suburb':
        this.locationsService
          .fetchDistrictsForSelect()
          .pipe(takeUntil(this.destroy$))
          .subscribe((o) => {
            this.filterFkOptions['districtId'] = o;
            this.cdr.markForCheck();
          });
        break;
      default:
        break;
    }
  }

  get supportsImportExport(): boolean {
    return this.entity !== 'city' && this.entity !== 'village';
  }

  hasActiveFilters(): boolean {
    if (this.searchQuery.trim().length > 0) {
      return true;
    }
    return Object.values(this.columnFilters).some((v) => (v ?? '').trim().length > 0);
  }

  applyFilters(): void {
    const nextSignature = this.currentFilterSignature();
    if (nextSignature === this.lastFilterSignature) {
      return;
    }
    this.lastFilterSignature = nextSignature;
    this.filterReload$.next();
  }

  onPage(ev: PageEvent): void {
    // MatPaginator may emit an initial page event during view init; ignore no-op events.
    if (ev.pageIndex === this.pageIndex && ev.pageSize === this.pageSize) {
      return;
    }
    this.pageIndex = ev.pageIndex;
    this.pageSize = ev.pageSize;
    this.runTableQuery({ background: false }).pipe(takeUntil(this.destroy$)).subscribe();
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
    this.openFormDialog('create');
  }

  refresh(): void {
    this.runTableQuery({ background: false }).pipe(takeUntil(this.destroy$)).subscribe();
  }

  private openFormDialog(action: LocationFormDialogAction, row?: Row): void {
    const cfg = CONFIG[this.entity];
    const id = row ? Number(row['id']) : undefined;
    if ((action === 'edit' || action === 'view') && (!id || !Number.isFinite(id))) {
      this.showErrorAlert('This row has no valid id.');
      return;
    }
    this.resolveGeoCoordinatesForDialog(row)
      .pipe(takeUntil(this.destroy$))
      .subscribe((resolvedRow) => {
        const data: LocationFormDialogData = {
          mode: cfg.formMode,
          action,
          id: id ?? null,
          initialValue: resolvedRow ?? null,
        };
        this.dialog
          .open(LocationFormDialogComponent, {
            width: '640px',
            maxWidth: '95vw',
            panelClass: 'lx-location-dialog-panel',
            autoFocus: 'first-tabbable',
            data,
          })
          .afterClosed()
          .subscribe((result: LocationFormDialogResult | undefined) => {
            if (result?.saved) {
              this.showSuccessAlert(result.message || 'Operation completed successfully.');
              this.runTableQuery({ background: false }).pipe(takeUntil(this.destroy$)).subscribe();
            }
          });
      });
  }

  private resolveGeoCoordinatesForDialog(row?: Row): Observable<Row | undefined> {
    if (!row) {
      return of(undefined);
    }
    const geoCoordinatesId = Number(row['geoCoordinatesId']);
    if (!Number.isFinite(geoCoordinatesId)) {
      return of(row);
    }
    return this.locationsService.findGeoCoordinatesById(geoCoordinatesId).pipe(
      map((geo) => {
        if (!geo) {
          return row;
        }
        return {
          ...row,
          latitude: row['latitude'] ?? geo.latitude ?? null,
          longitude: row['longitude'] ?? geo.longitude ?? null,
        } as Row;
      }),
      catchError(() => of(row)),
    );
  }

  importCsv(input: HTMLInputElement): void {
    input.click();
  }

  downloadSampleCsv(): void {
    const template = this.locationsService.getSampleCsvTemplate(this.entity);
    if (!template) {
      this.showErrorAlert('Sample CSV is not available for this location type yet.');
      return;
    }
    this.download(template.blob, template.filename);
    this.showSuccessAlert('Sample CSV downloaded.');
  }

  onImportFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.actionInProgress = true;
    this.locationsService
      .importLocationCsv(this.entity, file)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.showSuccessAlert(response.message || 'Import completed successfully.');
          input.value = '';
          this.actionInProgress = false;
          // Refresh shortly after import so newly created rows appear without waiting for manual refresh.
          setTimeout(() => this.runTableQuery({ background: true }).pipe(takeUntil(this.destroy$)).subscribe(), 250);
        },
        error: (err) => {
          this.showErrorAlert(this.errorMessage(err, 'Import failed.'));
          input.value = '';
          this.actionInProgress = false;
          // Also refresh after failed import in case partial rows were accepted before validation errors.
          setTimeout(() => this.runTableQuery({ background: true }).pipe(takeUntil(this.destroy$)).subscribe(), 250);
        },
      });
  }

  exportAs(format: 'csv' | 'xlsx' | 'pdf'): void {
    this.exporting = true;
    this.locationsService
      .exportLocation(this.entity, format)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.exporting = false;
        }),
      )
      .subscribe({
        next: (blob) => {
          this.download(blob, `${this.entity}-${new Date().toISOString().slice(0, 10)}.${format}`);
          this.showSuccessAlert(`Exported ${this.pageTitle.toLowerCase()} as ${format.toUpperCase()}.`);
          // Defer refresh: unblock UI first; skip the header spinner when the grid already has rows.
          queueMicrotask(() =>
            this.runTableQuery({ background: true }).pipe(takeUntil(this.destroy$)).subscribe(),
          );
        },
        error: (err) => {
          this.showErrorAlert(this.errorMessage(err, 'Export failed.'));
        },
      });
  }

  viewRow(row: Row): void {
    this.openFormDialog('view', row);
  }

  editRow(row: Row): void {
    this.openFormDialog('edit', row);
  }

  deleteRow(row: Row): void {
    const id = Number(row['id']);
    if (!Number.isFinite(id)) {
      this.showErrorAlert('This row has no valid id to delete.');
      return;
    }
    if (this.entity === 'country') {
      this.actionInProgress = true;
      this.locationsService
        .hasLinkedProvincesForCountry(id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (hasLinkedProvinces) => {
            if (hasLinkedProvinces) {
              this.showErrorAlert('Cannot delete this country while it still has linked provinces.');
              this.actionInProgress = false;
              return;
            }
            this.actionInProgress = false;
            this.confirmAndDelete(id);
          },
          error: (err) => {
            this.actionInProgress = false;
            this.showErrorAlert(this.errorMessage(err, 'Could not validate country dependencies.'));
          },
        });
      return;
    }
    this.confirmAndDelete(id);
  }

  private confirmAndDelete(id: number): void {
    this.dialog
      .open(DeleteConfirmDialogComponent, {
        width: '420px',
        maxWidth: '92vw',
        data: { entityLabel: this.pageTitle.slice(0, -1) || 'record' },
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed: boolean) => {
        if (!confirmed) {
          return;
        }
        this.actionInProgress = true;
        this.locationsService
          .deleteLocation(this.entity, id)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: (response) => {
              this.showSuccessAlert(response.message || 'Record deleted successfully.');
              this.runTableQuery({ background: false }).pipe(takeUntil(this.destroy$)).subscribe();
              this.actionInProgress = false;
            },
            error: (err) => {
              this.showErrorAlert(this.errorMessage(err, 'Delete failed.'));
              this.actionInProgress = false;
            },
          });
      });
  }

  /**
   * Single observable for table data; `switchMap` upstream cancels superseded HTTP calls.
   * Rows bind immediately when the response arrives (no artificial delay).
   */
  private runTableQuery(opts?: { background?: boolean }): Observable<{ rows: unknown[]; totalElements: number }> {
    const loadToken = ++this.latestLoadToken;
    const background = opts?.background === true;
    if (!background || this.totalRecords === 0) {
      this.fetching = true;
    }
    return this.locationsService
      .queryTablePage(this.entity, {
        page: this.pageIndex,
        size: this.pageSize,
        searchQuery: this.searchQuery,
        columnFilters: this.columnFilters,
      })
      .pipe(
        tap(({ rows, totalElements }) => {
          if (loadToken !== this.latestLoadToken) {
            return;
          }
          const normalizedRows = rows as Row[];
          if (normalizedRows.length === 0 && this.pageIndex > 0) {
            this.pageIndex = 0;
            this.runTableQuery({ background: true }).pipe(takeUntil(this.destroy$)).subscribe();
            return;
          }
          this.totalRecords = totalElements > 0 ? totalElements : normalizedRows.length;
          this.rawRows = normalizedRows;
          this.dataSource.data = this.rawRows;
        }),
        catchError((err) => {
          if (loadToken !== this.latestLoadToken) {
            return EMPTY;
          }
          this.showErrorAlert(this.errorMessage(err, `Failed to load ${this.pageTitle.toLowerCase()}.`));
          return EMPTY;
        }),
        finalize(() => {
          // Always clear loading when this HTTP cycle ends, but only if this request is still the
          // latest one. Otherwise a newer in-flight load owns the spinner (avoids stuck "Refreshing…"
          // when switchMap cancels a request or a stale response is dropped).
          if (loadToken === this.latestLoadToken) {
            this.fetching = false;
            this.cdr.markForCheck();
          }
        }),
      );
  }

  private currentFilterSignature(): string {
    return JSON.stringify({
      q: this.searchQuery.trim(),
      filters: this.columnFilters,
    });
  }

  private download(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    // Large exports: revoking immediately can interrupt the save on some browsers; defer cleanup.
    setTimeout(() => URL.revokeObjectURL(url), 60_000);
  }

  private errorMessage(error: unknown, fallback: string): string {
    const err = error as {
      status?: number;
      error?: { messageResponse?: string; message?: string; error?: string };
      message?: string;
    };
    if (err?.status === 0) {
      return 'Request failed before the server response reached the browser. Please retry.';
    }
    return err?.error?.messageResponse || err?.error?.message || err?.error?.error || err?.message || fallback;
  }

  private showSuccessAlert(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: ['app-snackbar-success'],
    });
  }

  private showErrorAlert(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: ['app-snackbar-error'],
    });
  }

}
