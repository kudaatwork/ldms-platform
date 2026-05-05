import { ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableDataSource } from '@angular/material/table';
import { Title } from '@angular/platform-browser';
import {
  EMPTY,
  Observable,
  Subject,
  catchError,
  debounceTime,
  finalize,
  forkJoin,
  merge,
  of,
  switchMap,
  takeUntil,
  tap,
  timer,
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
    lead: 'Cities anchored to a district (Country → Province → District → City).',
    columns: ['name', 'code', 'districtId', 'provinceName', 'timezone', 'entityStatus', 'actions'],
    labels: {
      name: 'Name',
      code: 'Code',
      districtId: 'District',
      provinceName: 'Province',
      timezone: 'Timezone',
      entityStatus: 'Status',
      actions: 'Actions',
    },
    formMode: 'city',
  },
  address: {
    pageTitle: 'Addresses',
    browserTitle: 'Addresses | LX Admin',
    lead: 'Address records linked to suburb or village settlements.',
    columns: [
      'line1',
      'postalCode',
      'settlementType',
      'settlementId',
      'villageName',
      'suburbName',
      'cityName',
      'districtName',
      'provinceName',
      'countryName',
      'entityStatus',
      'actions',
    ],
    labels: {
      line1: 'Address line 1',
      postalCode: 'Postal code',
      settlementType: 'Settlement type',
      settlementId: 'Settlement ID',
      villageName: 'Village',
      suburbName: 'Suburb',
      cityName: 'City',
      districtName: 'District',
      provinceName: 'Province',
      countryName: 'Country',
      entityStatus: 'Status',
      actions: 'Actions',
    },
    formMode: 'address',
  },
  suburb: {
    pageTitle: 'Suburbs',
    browserTitle: 'Suburbs | LX Admin',
    lead: 'Suburbs linked to districts.',
    columns: ['name', 'code', 'districtId', 'administrativeLevelId', 'postalCode', 'entityStatus', 'actions'],
    labels: {
      name: 'Name',
      code: 'Code',
      districtId: 'District',
      administrativeLevelId: 'Admin level',
      postalCode: 'Postal code',
      entityStatus: 'Status',
      actions: 'Actions',
    },
    formMode: 'suburb',
  },
  village: {
    pageTitle: 'Villages',
    browserTitle: 'Villages | LX Admin',
    lead: 'Villages under a city within a district (optional suburb link).',
    columns: ['name', 'code', 'cityId', 'districtId', 'suburbName', 'timezone', 'entityStatus', 'actions'],
    labels: {
      name: 'Name',
      code: 'Code',
      cityId: 'City',
      districtId: 'District',
      suburbName: 'Suburb',
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
    columns: ['name', 'code', 'level', 'countryId', 'description', 'entityStatus', 'actions'],
    labels: {
      name: 'Name',
      code: 'Code',
      level: 'Level',
      countryId: 'Country',
      description: 'Description',
      entityStatus: 'Status',
      actions: 'Actions',
    },
    formMode: 'admin-level',
  },
  language: {
    pageTitle: 'Languages',
    browserTitle: 'Languages | LX Admin',
    lead: 'Languages used for localized display names and locale-aware content.',
    columns: ['name', 'isoCode', 'nativeName', 'isDefault', 'entityStatus', 'actions'],
    labels: {
      name: 'Name',
      isoCode: 'ISO code',
      nativeName: 'Native name',
      isDefault: 'Default',
      entityStatus: 'Status',
      actions: 'Actions',
    },
    formMode: 'language',
  },
  'localized-name': {
    pageTitle: 'Localized names',
    browserTitle: 'Localized names | LX Admin',
    lead: 'Translated labels linked to a language and a country, province, district, or suburb.',
    columns: ['value', 'referenceType', 'languageId', 'entityStatus', 'actions'],
    labels: {
      value: 'Value',
      referenceType: 'Reference type',
      languageId: 'Language',
      entityStatus: 'Status',
      actions: 'Actions',
    },
    formMode: 'localized-name',
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
    if (this.entity === 'city' || this.entity === 'village') {
      this.locationsService
        .fetchCountriesForSelect()
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => this.cdr.markForCheck(),
          error: () => {},
        });
    }
    // City/village dialogs start with country → province → district; warm the country list early.
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
      suburb: ['districtId', 'administrativeLevelId'],
      city: ['districtId'],
      village: ['districtId', 'cityId'],
      address: ['countryId', 'provinceId', 'districtId', 'cityId', 'suburbId', 'villageId', 'settlementId'],
      'admin-level': ['countryId'],
      'localized-name': ['languageId'],
    };
    return keys[this.entity]?.includes(key) ?? false;
  }

  fkFilterOptionsFor(key: string): LocationSelectOption[] {
    return this.filterFkOptions[key] ?? [];
  }

  /** Same Material icon names as `location-form-dialog` shells (Country, Admin level, …). */
  fkFilterShellIcon(columnKey: string): string {
    const icons: Record<string, string> = {
      countryId: 'public',
      administrativeLevelId: 'account_tree',
      provinceId: 'map',
      districtId: 'signpost',
      cityId: 'location_city',
      suburbId: 'domain',
      villageId: 'cottage',
      settlementId: 'domain',
      languageId: 'language',
    };
    return icons[columnKey] ?? 'tune';
  }

  /** Matches admin-level / province selects: label plus optional em-dash sublabel. */
  fkFilterOptionDisplay(o: LocationSelectOption): string {
    return o.sublabel ? `${o.label} — ${o.sublabel}` : o.label;
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
        this.locationsService
          .fetchAdministrativeLevelsForSelect()
          .pipe(takeUntil(this.destroy$))
          .subscribe((o) => {
            this.filterFkOptions['administrativeLevelId'] = o;
            this.cdr.markForCheck();
          });
        break;
      case 'city':
        this.locationsService
          .fetchDistrictsForSelect()
          .pipe(takeUntil(this.destroy$))
          .subscribe((o) => {
            this.filterFkOptions['districtId'] = o;
            this.cdr.markForCheck();
          });
        break;
      case 'village':
        this.locationsService
          .fetchDistrictsForSelect()
          .pipe(takeUntil(this.destroy$))
          .subscribe((o) => {
            this.filterFkOptions['districtId'] = o;
            this.cdr.markForCheck();
          });
        this.locationsService
          .fetchCitiesForSelect()
          .pipe(takeUntil(this.destroy$))
          .subscribe((o) => {
            this.filterFkOptions['cityId'] = o;
            this.cdr.markForCheck();
          });
        break;
      case 'admin-level':
        this.locationsService
          .fetchCountriesForSelect()
          .pipe(takeUntil(this.destroy$))
          .subscribe((o) => {
            this.filterFkOptions['countryId'] = o;
            this.cdr.markForCheck();
          });
        break;
      case 'address':
        this.locationsService
          .fetchCountriesForSelect()
          .pipe(takeUntil(this.destroy$))
          .subscribe((o) => {
            this.filterFkOptions['countryId'] = o;
            this.cdr.markForCheck();
          });
        this.locationsService
          .fetchProvincesForSelect()
          .pipe(takeUntil(this.destroy$))
          .subscribe((o) => {
            this.filterFkOptions['provinceId'] = o;
            this.cdr.markForCheck();
          });
        this.locationsService
          .fetchDistrictsForSelect()
          .pipe(takeUntil(this.destroy$))
          .subscribe((o) => {
            this.filterFkOptions['districtId'] = o;
            this.cdr.markForCheck();
          });
        this.locationsService
          .fetchCitiesForSelect()
          .pipe(takeUntil(this.destroy$))
          .subscribe((o) => {
            this.filterFkOptions['cityId'] = o;
            this.cdr.markForCheck();
          });
        this.locationsService
          .fetchVillagesForSelect()
          .pipe(takeUntil(this.destroy$))
          .subscribe((o) => {
            this.filterFkOptions['villageId'] = o;
            this.cdr.markForCheck();
          });
        this.locationsService
          .fetchSuburbsForSelect()
          .pipe(takeUntil(this.destroy$))
          .subscribe((o) => {
            this.filterFkOptions['suburbId'] = o;
            this.filterFkOptions['settlementId'] = o;
            this.cdr.markForCheck();
          });
        break;
      case 'localized-name':
        this.locationsService
          .fetchLanguagesForSelect()
          .pipe(takeUntil(this.destroy$))
          .subscribe((o) => {
            this.filterFkOptions['languageId'] = o;
            this.cdr.markForCheck();
          });
        break;
      default:
        break;
    }
  }

  get supportsImportExport(): boolean {
    return true;
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

  /** Readable reference-type column for localized names (matches API enum tokens). */
  localizedReferenceTypeLabel(raw: unknown): string {
    const t = String(raw ?? '').trim().toUpperCase();
    if (!t) {
      return '—';
    }
    const labels: Record<string, string> = {
      COUNTRY: 'Country',
      PROVINCE: 'Province',
      DISTRICT: 'District',
      SUBURB: 'Suburb',
    };
    return labels[t] ?? String(raw).trim();
  }

  displayCell(row: Row, key: string): string {
    if (key === 'isDefault') {
      const v = row[key];
      if (v === true || v === 'true') return 'Yes';
      if (v === false || v === 'false') return 'No';
    }
    if (key === 'languageId') {
      const rawId = row[key];
      const opts = this.filterFkOptions['languageId'] ?? [];
      const n = Number(rawId);
      if (Number.isFinite(n) && n > 0) {
        const opt = opts.find((o) => o.id === n || String(o.id) === String(rawId));
        if (opt) {
          return opt.sublabel ? `${opt.label} (${opt.sublabel})` : opt.label;
        }
        return `#${n}`;
      }
    }
    if (key === 'referenceType') {
      return this.localizedReferenceTypeLabel(row[key]);
    }
    /** City label on addresses when API sends id but omitted name after mapping fixes. */
    if (key === 'cityName' && this.entity === 'address') {
      const name = row['cityName'];
      if (name != null && String(name).trim().length > 0) {
        return String(name);
      }
      const id = Number(row['cityId']);
      if (Number.isFinite(id) && id > 0) {
        return `City #${id}`;
      }
      return '—';
    }
    const fkNameKeys: Record<string, string> = {
      countryId: 'countryName',
      administrativeLevelId: 'administrativeLevelName',
      provinceId: 'provinceName',
      districtId: 'districtName',
      cityId: 'cityName',
    };
    const nameKey = fkNameKeys[key];
    if (nameKey !== undefined) {
      const name = row[nameKey];
      if (name != null && String(name).trim().length > 0) {
        return String(name);
      }
      const id = row[key];
      if (id !== null && id !== undefined && id !== '') {
        return `#${id}`;
      }
      return '—';
    }
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
    // Best-effort: re-poke the prefetch right before the dialog mounts so the
    // request is already mid-flight against shareReplay by the time the dialog
    // ngOnInit subscribes (no duplicate HTTP — same observable).
    this.warmDialogCache();
    this.openFormDialog('create');
  }

  /**
   * Fired on hover / focus of the "Add New" button. Warms only the FK lists the
   * dialog actually loads upfront for this entity, so the modal opens against an
   * already-warm cache without dragging in heavy parent/suburb forkJoins.
   * Idempotent thanks to the service-level shareReplay.
   */
  warmDialogCache(): void {
    const warm$ = this.dialogWarmObservableForEntity();
    if (!warm$) {
      return;
    }
    warm$.pipe(takeUntil(this.destroy$)).subscribe({ error: () => void 0 });
  }

  private dialogWarmObservableForEntity(): Observable<unknown> | null {
    switch (this.entity) {
      case 'country':
        return null;
      case 'province':
        return this.locationsService.fetchCountriesForSelect();
      case 'district':
        return this.locationsService.fetchProvincesForSelect();
      case 'suburb':
      case 'city':
      case 'village':
        return this.locationsService.fetchDistrictsForSelect();
      case 'admin-level':
      case 'address':
        return this.locationsService.fetchCountriesForSelect();
      case 'localized-name':
        return forkJoin({
          _: this.locationsService.fetchLanguagesForSelect(),
          __: this.locationsService.fetchCountriesForSelect(),
        });
      default:
        return null;
    }
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
              if (result.created) {
                this.refreshAfterCreateGoToLastPage(result.createdId);
              } else {
                this.runTableQuery({ background: false }).pipe(takeUntil(this.destroy$)).subscribe();
              }
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
          if (response.ok) {
            this.showSuccessAlert(response.message || 'Import completed successfully.');
          } else {
            this.showErrorAlert(response.message || 'Import failed.');
          }
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
              if (!response.ok) {
                this.showErrorAlert(response.message || 'Delete was rejected by the server.');
                this.actionInProgress = false;
                return;
              }
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
   * Applies one page of rows from the server. When the current page is past the end (empty page),
   * resets to page 0 and reloads.
   */
  private applyLoadedPage(loadToken: number, rows: unknown[], totalElements: number): void {
    if (loadToken !== this.latestLoadToken) {
      return;
    }
    let normalizedRows = rows as Row[];
    if (this.entity === 'suburb') {
      normalizedRows = [...normalizedRows].sort((a, b) => Number(a['id']) - Number(b['id']));
    }
    if (normalizedRows.length === 0 && this.pageIndex > 0) {
      this.pageIndex = 0;
      this.runTableQuery({ background: true }).pipe(takeUntil(this.destroy$)).subscribe();
      return;
    }
    this.totalRecords = totalElements > 0 ? totalElements : normalizedRows.length;
    this.rawRows = normalizedRows;
    this.dataSource.data = this.rawRows;
  }

  /**
   * After creating a row, total count grows; with server sort by id ascending the new row is on the last page.
   * Brief delay + optional retry avoids landing on the previous last page when totals lag replication/txn.
   */
  private refreshAfterCreateGoToLastPage(createdId?: number): void {
    const loadToken = ++this.latestLoadToken;
    this.fetching = true;
    const filter = {
      searchQuery: this.searchQuery,
      columnFilters: this.columnFilters,
    };
    const probeAndLoadLastPage = () =>
      this.locationsService.queryTablePage(this.entity, { page: 0, size: 1, ...filter }).pipe(
        switchMap(({ totalElements }) => {
          const lastPage = totalElements > 0 ? Math.max(0, Math.ceil(totalElements / this.pageSize) - 1) : 0;
          this.pageIndex = lastPage;
          return this.locationsService.queryTablePage(this.entity, {
            page: lastPage,
            size: this.pageSize,
            ...filter,
          });
        }),
      );

    const rowHasCreatedId = (pageRows: unknown[]): boolean => {
      if (createdId == null || !Number.isFinite(createdId)) {
        return true;
      }
      return pageRows.some((r) => Number((r as Row)['id']) === createdId);
    };

    timer(150)
      .pipe(
        switchMap(() => probeAndLoadLastPage()),
        switchMap((page) =>
          rowHasCreatedId(page.rows)
            ? of(page)
            : timer(450).pipe(switchMap(() => probeAndLoadLastPage())),
        ),
        tap(({ rows, totalElements }) => this.applyLoadedPage(loadToken, rows, totalElements)),
        catchError((err) => {
          if (loadToken !== this.latestLoadToken) {
            return EMPTY;
          }
          this.showErrorAlert(this.errorMessage(err, `Failed to load ${this.pageTitle.toLowerCase()}.`));
          return EMPTY;
        }),
        finalize(() => {
          if (loadToken === this.latestLoadToken) {
            this.fetching = false;
            this.cdr.markForCheck();
          }
        }),
        takeUntil(this.destroy$),
      )
      .subscribe();
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
        tap(({ rows, totalElements }) => this.applyLoadedPage(loadToken, rows, totalElements)),
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
