import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { PageEvent } from '@angular/material/paginator';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  EMPTY,
  Observable,
  Subject,
  catchError,
  debounceTime,
  finalize,
  forkJoin,
  map,
  merge,
  of,
  switchMap,
  takeUntil,
  tap,
} from 'rxjs';
import { DEFAULT_TABLE_PAGE_SIZE } from '../../../../shared/constants/table-pagination';
import { downloadBlob, exportFilename, LxExportFormat } from '../../../../shared/utils/lx-export.util';
import { BranchDetail, OrganizationService, OrganizationSummary } from '../../../../core/services/organization.service';
import { OrgContextService } from '../../../../core/services/org-context.service';
import { DeleteConfirmDialogComponent } from '../../../../shared/components/delete-confirm-dialog/delete-confirm-dialog.component';
import {
  AddWarehouseDialogComponent,
  AddWarehouseDialogData,
} from '../../../inventory/components/add-warehouse-dialog/add-warehouse-dialog.component';
import {
  BranchFormDialogComponent,
  BranchFormDialogData,
  BranchFormDialogResult,
} from '../../components/branch-form-dialog/branch-form-dialog.component';
import {
  LinkWarehouseDialogComponent,
  LinkWarehouseDialogData,
} from '../../components/link-warehouse-dialog/link-warehouse-dialog.component';
import { BranchListScope } from '../../models/org-management.model';
import { OrgManagementPortalService } from '../../services/org-management-portal.service';
import { InventoryPortalService } from '../../../inventory/services/inventory-portal.service';
import { WarehouseRow } from '../../../inventory/models/inventory.model';
import { LocationsService } from '../../../locations/services/locations.service';
import { formatInventoryAddressLabel } from '../../../inventory/utils/inventory-address.util';

@Component({
  selector: 'app-org-branches-page',
  templateUrl: './org-branches-page.component.html',
  styleUrl: './org-branches-page.component.scss',
  standalone: false,
})
export class OrgBranchesPageComponent implements OnInit, OnDestroy {
  scope: BranchListScope = 'top-level';
  loading = true;
  actionInProgress = false;
  exporting = false;
  error = '';
  branches: BranchDetail[] = [];
  parentBranchOptions: BranchDetail[] = [];
  totalRecords = 0;
  pageIndex = 0;
  pageSize = DEFAULT_TABLE_PAGE_SIZE;

  searchQuery = '';
  filterFieldsOpen = false;
  showSampleCsvInfo = false;
  columnFilters = {
    branchName: '',
    region: '',
    parentBranchId: '' as number | '',
    depot: '' as boolean | '',
    active: '' as boolean | '',
  };

  drillBranch: BranchDetail | null = null;
  drillSubBranches: BranchDetail[] = [];
  drillWarehouses: WarehouseRow[] = [];
  drillLinkableCount = 0;
  drillLoading = false;
  drillWarehousesLoading = false;
  warehouseActionInProgress = false;

  private allOrgWarehouses: WarehouseRow[] = [];

  /** Row targeted by the shared actions menu (avoids brittle per-row {@code mat-menu} refs). */
  private menuTargetBranch: BranchDetail | null = null;
  private menuTargetWarehouse: WarehouseRow | null = null;

  private readonly destroy$ = new Subject<void>();
  private readonly filterReload$ = new Subject<void>();
  private latestLoadToken = 0;
  private lastFilterSignature = '';
  private orgContactFallback: Pick<BranchDetail, 'email' | 'phoneNumber' | 'region' | 'businessHours'> | null = null;
  private headOfficeFromProfile: BranchDetail | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly orgMgmt: OrgManagementPortalService,
    private readonly organizationService: OrganizationService,
    private readonly orgContext: OrgContextService,
    private readonly locationsService: LocationsService,
    private readonly inventoryPortal: InventoryPortalService,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    private readonly title: Title,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  get isSubLevel(): boolean {
    return this.scope === 'sub-level';
  }

  get pageTitle(): string {
    return this.isSubLevel ? 'Sub-branches & depots' : 'Branches';
  }

  get pageLead(): string {
    return this.isSubLevel
      ? 'Second-level locations under a parent branch — use depots for warehouse allocation at distribution points.'
      : 'Top-level branches for your organisation. Sub-branches and depots are managed separately.';
  }

  get addButtonLabel(): string {
    return this.isSubLevel ? 'Add sub-branch' : 'Add branch';
  }

  get sampleCsvDescription(): string {
    return this.isSubLevel
      ? 'Sub-branch imports use branch name, code, region, contact columns, and PARENT BRANCH ID. Mark DEPOT as true for depot locations.'
      : 'Branch imports use branch name, code, region, contact, and head office columns. Rows are scoped to your organisation automatically.';
  }

  get importCsvDisclaimer(): string {
    return this.isSubLevel
      ? 'CSV import only. PARENT BRANCH ID must match an existing top-level branch in your organisation.'
      : 'CSV import only. Rows are created under your signed-in organisation. Leave ORGANIZATION ID empty or omit it.';
  }

  ngOnInit(): void {
    this.scope = this.route.snapshot.data['branchScope'] === 'sub-level' ? 'sub-level' : 'top-level';
    this.title.setTitle(`${this.pageTitle} | Organization management`);
    this.lastFilterSignature = this.currentFilterSignature();
    if (this.isSubLevel) {
      this.loadParentBranchOptions();
    }
    this.loadOrgProfile();
    merge(of(undefined as void), this.filterReload$.pipe(debounceTime(150)))
      .pipe(
        switchMap(() => {
          this.pageIndex = 0;
          return this.runTableQuery({ background: false });
        }),
        takeUntil(this.destroy$),
      )
      .subscribe();

    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((qm) => {
      const id = Number(qm.get('drillBranchId'));
      const openAdd = qm.get('openAdd') === '1';
      if (!Number.isFinite(id) || id <= 0) {
        if (this.drillBranch) {
          this.drillBranch = null;
          this.drillSubBranches = [];
          this.drillWarehouses = [];
          this.allOrgWarehouses = [];
          this.drillLinkableCount = 0;
          this.cdr.markForCheck();
        }
        return;
      }
      if (this.drillBranch?.id === id) {
        if (openAdd) {
          this.openAddWarehouseDialog();
        }
        return;
      }
      this.orgMgmt
        .getBranch(id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (branch) => {
            this.enterDrill(branch, false);
            if (openAdd) {
              this.openAddWarehouseDialog();
            }
          },
          error: () => {
            this.snackBar.open('Could not open branch drill-down.', 'Close', { duration: 4000 });
          },
        });
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  applyFilters(): void {
    const next = this.currentFilterSignature();
    if (next === this.lastFilterSignature) {
      return;
    }
    this.lastFilterSignature = next;
    this.filterReload$.next();
  }

  onPage(event: PageEvent): void {
    if (event.pageIndex === this.pageIndex && event.pageSize === this.pageSize) {
      return;
    }
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.runTableQuery({ background: false }).pipe(takeUntil(this.destroy$)).subscribe();
  }

  refresh(): void {
    this.runTableQuery({ background: false }).pipe(takeUntil(this.destroy$)).subscribe();
  }

  hasActiveFilters(): boolean {
    return (
      !!this.searchQuery.trim() ||
      !!this.columnFilters.branchName.trim() ||
      !!this.columnFilters.region.trim() ||
      this.columnFilters.parentBranchId !== '' ||
      this.columnFilters.depot !== '' ||
      this.columnFilters.active !== ''
    );
  }

  openCreate(): void {
    if (this.isSubLevel && !this.parentBranchOptions.length) {
      this.snackBar.open('Create a top-level branch before adding sub-branches.', 'Close', { duration: 5000 });
      return;
    }
    this.openFormDialog({
      action: 'create',
      mode: this.isSubLevel ? 'sub-level' : 'top-level',
      parentBranches: this.parentBranchOptions,
    });
  }

  openEdit(branch: BranchDetail): void {
    this.openFormDialog({
      action: 'edit',
      mode: this.isSubLevel ? 'sub-level' : 'top-level',
      row: branch,
      parentBranches: this.parentBranchOptions,
    });
  }

  viewBranch(branch: BranchDetail): void {
    this.openFormDialog({
      action: 'view',
      mode: this.isSubLevel ? 'sub-level' : 'top-level',
      row: branch,
      parentBranches: this.parentBranchOptions,
    });
  }

  confirmDelete(branch: BranchDetail): void {
    const label = this.isSubLevel ? 'sub-branch' : 'branch';
    this.dialog
      .open(DeleteConfirmDialogComponent, {
        width: '420px',
        data: { entityLabel: `${label} "${branch.branchName}"` },
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.actionInProgress = true;
        this.orgMgmt
          .deleteBranch(branch.id)
          .pipe(
            takeUntil(this.destroy$),
            finalize(() => {
              this.actionInProgress = false;
              this.cdr.markForCheck();
            }),
          )
          .subscribe({
            next: () => {
              this.snackBar.open(`${this.isSubLevel ? 'Sub-branch' : 'Branch'} deleted.`, 'Close', { duration: 3500 });
              this.refresh();
            },
            error: (err: Error) => {
              this.snackBar.open(err.message ?? 'Delete failed.', 'Close', { duration: 5000 });
            },
          });
      });
  }

  importCsv(input: HTMLInputElement): void {
    input.click();
  }

  onImportFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.actionInProgress = true;
    this.orgMgmt
      .importBranchesCsv(file)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.snackBar.open(response.message ?? (response.ok ? 'Import completed.' : 'Import failed.'), 'Close', {
            duration: response.ok ? 3500 : 6000,
          });
          input.value = '';
          this.actionInProgress = false;
          if (response.ok) {
            this.refresh();
            if (this.isSubLevel) {
              this.loadParentBranchOptions();
            }
          }
        },
        error: (err: Error) => {
          this.snackBar.open(err.message ?? 'Import failed.', 'Close', { duration: 5000 });
          input.value = '';
          this.actionInProgress = false;
        },
      });
  }

  downloadSampleCsv(): void {
    const template = this.orgMgmt.getBranchSampleCsv(this.scope);
    downloadBlob(template.blob, template.filename);
    this.snackBar.open('Sample CSV downloaded.', 'Close', { duration: 3000 });
  }

  exportAs(format: LxExportFormat): void {
    this.exporting = true;
    this.orgMgmt
      .exportBranches(format, this.currentQuery())
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.exporting = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (blob) => {
          downloadBlob(blob, exportFilename(this.isSubLevel ? 'sub-branches' : 'branches', format));
          this.snackBar.open(`Exported as ${format.toUpperCase()}.`, 'Close', { duration: 3000 });
        },
        error: (err: Error) => {
          this.snackBar.open(err.message ?? 'Export failed.', 'Close', { duration: 5000 });
        },
      });
  }

  branchTypeLabel(branch: BranchDetail): string {
    if (this.isSubLevel || branch.branchLevel === 'SUB_BRANCH') {
      return branch.depot ? 'Depot' : 'Sub-branch';
    }
    return branch.headOffice ? 'Head office' : 'Branch';
  }

  drillIntoBranch(branch: BranchDetail): void {
    this.enterDrill(branch);
  }

  drillIntoSubBranch(subBranch: BranchDetail): void {
    this.enterDrill(subBranch);
  }

  exitDrill(): void {
    this.drillBranch = null;
    this.drillSubBranches = [];
    this.drillWarehouses = [];
    this.allOrgWarehouses = [];
    this.drillLinkableCount = 0;
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { drillBranchId: null, openAdd: null },
      queryParamsHandling: 'merge',
    });
    this.cdr.markForCheck();
  }

  viewWarehouseInInventory(warehouse: WarehouseRow): void {
    void this.router.navigate(['/products-inventory', 'stock'], {
      queryParams: { warehouseId: warehouse.id, branchId: warehouse.branchId ?? null },
    });
  }

  get showDrillSubBranches(): boolean {
    return !!this.drillBranch && this.drillBranch.branchLevel === 'BRANCH';
  }

  get drillStatCards(): Array<{ icon: string; label: string; value: number; tone?: string }> {
    return [
      {
        icon: 'fork_right',
        label: this.showDrillSubBranches ? 'Sub-branches' : 'Level',
        value: this.showDrillSubBranches ? this.drillSubBranches.length : 1,
        tone: 'violet',
      },
      {
        icon: 'warehouse',
        label: 'Warehouses',
        value: this.drillWarehouses.length,
        tone: 'blue',
      },
      {
        icon: 'link',
        label: 'Linkable',
        value: this.drillLinkableCount,
        tone: 'amber',
      },
    ];
  }

  warehouseTypeLabel(type: string | undefined): string {
    const raw = String(type ?? 'SUPPLIER').toUpperCase();
    if (raw === 'CUSTOMER') {
      return 'Customer';
    }
    if (raw === 'TRANSIT') {
      return 'Transit';
    }
    return 'Supplier';
  }

  addWarehouseForDrillBranch(): void {
    this.openAddWarehouseDialog();
  }

  linkExistingWarehouse(): void {
    if (!this.drillBranch) {
      return;
    }
    const candidates = this.linkableWarehouses();
    const ref = this.dialog.open(LinkWarehouseDialogComponent, {
      width: '640px',
      maxWidth: '95vw',
      panelClass: 'lx-location-dialog-panel',
      disableClose: true,
      data: {
        branch: this.drillBranch,
        candidates,
      } satisfies LinkWarehouseDialogData,
    });
    ref.afterClosed().subscribe((result) => {
      if (result?.linked) {
        this.snackBar.open(`"${result.linked.name}" linked to ${this.drillBranch?.branchName}.`, 'Close', {
          duration: 3500,
        });
        this.refreshDrillWarehouses();
      }
    });
  }

  editWarehouseInDrill(warehouse: WarehouseRow): void {
    const supplierId = this.orgContext.organizationId;
    if (supplierId == null) {
      this.snackBar.open('Your session has no organisation id.', 'Close', { duration: 4000 });
      return;
    }
    this.dialog
      .open(AddWarehouseDialogComponent, {
        width: '620px',
        maxWidth: '95vw',
        disableClose: true,
        panelClass: 'lx-location-dialog-panel',
        data: { supplierId, mode: 'edit', warehouse } satisfies AddWarehouseDialogData,
      })
      .afterClosed()
      .subscribe((updated) => {
        if (updated) {
          this.snackBar.open(`"${updated.name}" updated.`, 'Close', { duration: 3000 });
          this.refreshDrillWarehouses();
        }
      });
  }

  openInventoryWarehouseList(): void {
    if (!this.drillBranch) {
      return;
    }
    void this.router.navigate(['/products-inventory', 'warehouses'], {
      queryParams: { branchId: this.drillBranch.id },
    });
  }

  /** Stable row for the open actions menu (survives {@code mat-menu} close before click handlers run). */
  get actionsBranch(): BranchDetail | null {
    return this.menuTargetBranch;
  }

  get actionsWarehouse(): WarehouseRow | null {
    return this.menuTargetWarehouse;
  }

  prepareRowActions(event: Event, branch: BranchDetail): void {
    event.stopPropagation();
    this.menuTargetBranch = branch;
  }

  prepareWarehouseActions(event: Event, warehouse: WarehouseRow): void {
    event.stopPropagation();
    this.menuTargetWarehouse = warehouse;
  }

  clearRowActions(): void {
    this.menuTargetBranch = null;
  }

  clearWarehouseActions(): void {
    this.menuTargetWarehouse = null;
  }

  private enterDrill(branch: BranchDetail, updateRoute = true): void {
    this.drillBranch = this.enrichBranchesForDisplay([branch])[0];
    this.loadDrillContext();
    if (updateRoute) {
      void this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { drillBranchId: branch.id },
        queryParamsHandling: 'merge',
      });
    }
    this.cdr.markForCheck();
  }

  private loadDrillContext(): void {
    const branch = this.drillBranch;
    if (!branch) {
      return;
    }
    this.drillLoading = true;
    this.drillWarehousesLoading = true;

    const subBranches$ =
      branch.branchLevel === 'BRANCH'
        ? this.orgMgmt.queryBranchesPage({
            page: 0,
            size: 500,
            searchQuery: '',
            branchLevel: 'SUB_BRANCH',
            parentBranchId: branch.id,
          })
        : of({ rows: [] as BranchDetail[], totalElements: 0 });

    subBranches$.pipe(takeUntil(this.destroy$)).subscribe({
      next: ({ rows }) => {
        this.drillSubBranches = rows;
        this.drillLoading = false;
        this.loadDrillWarehouses(branch, rows.map((row) => row.id));
        this.cdr.markForCheck();
      },
      error: () => {
        this.drillSubBranches = [];
        this.drillLoading = false;
        this.loadDrillWarehouses(branch, []);
        this.cdr.markForCheck();
      },
    });
  }

  private loadDrillWarehouses(branch: BranchDetail, childBranchIds: number[]): void {
    const allowed = this.buildAllowedBranchIds(branch.id, childBranchIds);
    this.inventoryPortal
      .listWarehouses()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.drillWarehousesLoading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (rows) => {
          this.allOrgWarehouses = rows;
          this.drillLinkableCount = this.linkableWarehouses(allowed, rows).length;
          this.enrichDrillWarehouseRows(rows.filter((row) => row.branchId != null && allowed.has(row.branchId)));
        },
        error: () => {
          this.allOrgWarehouses = [];
          this.drillWarehouses = [];
          this.drillLinkableCount = 0;
        },
      });
  }

  private enrichDrillWarehouseRows(rows: WarehouseRow[]): void {
    if (!rows.length) {
      this.drillWarehouses = rows;
      return;
    }
    const lookups = rows.map((row) => {
      const addressId = Number(row.locationId);
      if (!Number.isFinite(addressId) || addressId <= 0) {
        return of({ ...row, addressLabel: row.addressLabel || '—' });
      }
      return this.locationsService.findLocationById('address', addressId).pipe(
        map((dto) => ({
          ...row,
          addressLabel: dto ? formatInventoryAddressLabel(dto) : row.addressLabel || '—',
        })),
        catchError(() => of({ ...row, addressLabel: row.addressLabel || '—' })),
      );
    });
    forkJoin(lookups).pipe(takeUntil(this.destroy$)).subscribe((enriched) => {
      this.drillWarehouses = enriched;
      this.cdr.markForCheck();
    });
  }

  private refreshDrillWarehouses(): void {
    const branch = this.drillBranch;
    if (!branch) {
      return;
    }
    this.drillWarehousesLoading = true;
    this.loadDrillWarehouses(branch, this.drillSubBranches.map((row) => row.id));
  }

  private openAddWarehouseDialog(): void {
    if (!this.drillBranch) {
      return;
    }
    const supplierId = this.orgContext.organizationId;
    if (supplierId == null) {
      this.snackBar.open('Your session has no organisation id — sign in again.', 'Close', { duration: 5000 });
      return;
    }
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { openAdd: null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
    this.dialog
      .open(AddWarehouseDialogComponent, {
        width: '620px',
        maxWidth: '95vw',
        disableClose: true,
        panelClass: 'lx-location-dialog-panel',
        data: {
          supplierId,
          preselectedBranchId: this.drillBranch.id,
        } satisfies AddWarehouseDialogData,
      })
      .afterClosed()
      .subscribe((warehouse) => {
        if (warehouse) {
          this.snackBar.open(`"${warehouse.name}" added to ${this.drillBranch?.branchName}.`, 'Close', {
            duration: 3500,
          });
          this.refreshDrillWarehouses();
        }
      });
  }

  private linkableWarehouses(
    allowed: Set<number> = this.buildAllowedBranchIds(
      this.drillBranch?.id ?? 0,
      this.drillSubBranches.map((row) => row.id),
    ),
    rows: WarehouseRow[] = this.allOrgWarehouses,
  ): WarehouseRow[] {
    return rows.filter(
      (row) =>
        row.warehouseType !== 'TRANSIT' &&
        (!row.branchId || !allowed.has(row.branchId)),
    );
  }

  private buildAllowedBranchIds(branchId: number, childBranchIds: number[]): Set<number> {
    return new Set<number>([branchId, ...childBranchIds].filter((id) => id > 0));
  }

  private loadParentBranchOptions(): void {
    this.orgMgmt
      .queryBranchesPage({
        page: 0,
        size: 500,
        searchQuery: '',
        branchLevel: 'BRANCH',
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ rows }) => {
          this.parentBranchOptions = rows;
          this.cdr.markForCheck();
        },
        error: () => {
          this.parentBranchOptions = [];
        },
      });
  }

  private openFormDialog(data: BranchFormDialogData): void {
    const ref = this.dialog.open(BranchFormDialogComponent, {
      width: '640px',
      maxWidth: '95vw',
      panelClass: 'lx-location-dialog-panel',
      autoFocus: 'first-tabbable',
      disableClose: true,
      data,
    });
    ref.afterClosed().subscribe((result: BranchFormDialogResult | undefined) => {
      if (result?.saved) {
        this.refresh();
        if (this.isSubLevel) {
          this.loadParentBranchOptions();
        }
      }
    });
  }

  private runTableQuery(opts?: { background?: boolean }): Observable<{ rows: BranchDetail[]; totalElements: number }> {
    const loadToken = ++this.latestLoadToken;
    if (!opts?.background || this.totalRecords === 0) {
      this.loading = true;
    }
    this.error = '';
    return this.orgMgmt.queryBranchesPage(this.currentQuery()).pipe(
      tap(({ rows, totalElements }) => this.applyLoadedPage(loadToken, rows, totalElements)),
      catchError((err: Error) => {
        if (loadToken !== this.latestLoadToken) {
          return EMPTY;
        }
        this.error = err.message ?? 'Could not load branches.';
        this.branches = [];
        this.totalRecords = 0;
        return EMPTY;
      }),
      finalize(() => {
        if (loadToken === this.latestLoadToken) {
          this.loading = false;
          this.cdr.markForCheck();
        }
      }),
    );
  }

  private applyLoadedPage(loadToken: number, rows: BranchDetail[], totalElements: number): void {
    if (loadToken !== this.latestLoadToken) {
      return;
    }
    if (rows.length === 0 && totalElements > 0 && this.pageIndex > 0) {
      this.pageIndex = 0;
      this.runTableQuery({ background: true }).pipe(takeUntil(this.destroy$)).subscribe();
      return;
    }
    this.totalRecords = totalElements > 0 ? totalElements : rows.length;
    this.branches = this.enrichBranchesForDisplay(rows);
    this.cdr.markForCheck();
  }

  /** Head-office rows auto-provisioned without contact columns inherit organisation profile for display. */
  private enrichBranchesForDisplay(rows: BranchDetail[]): BranchDetail[] {
    const fallback = this.orgContactFallback;
    const headOfficeProfile = this.headOfficeFromProfile;
    return rows.map((branch) => {
      const profileRow =
        headOfficeProfile && branch.headOffice && branch.id === headOfficeProfile.id ? headOfficeProfile : null;
      const contact = profileRow ?? (branch.headOffice ? fallback : null);
      if (!contact) {
        return branch;
      }
      return {
        ...branch,
        email: branch.email || contact.email,
        phoneNumber: branch.phoneNumber || contact.phoneNumber,
        region: branch.region || contact.region,
        businessHours: branch.businessHours || contact.businessHours,
      };
    });
  }

  private loadOrgProfile(): void {
    this.organizationService
      .getMy()
      .pipe(
        switchMap((org) => this.buildOrgContactFallback(org)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (ctx) => {
          this.headOfficeFromProfile = ctx.headOfficeBranch;
          this.orgContactFallback = ctx.fallback;
          if (this.branches.length) {
            this.branches = this.enrichBranchesForDisplay(this.branches);
            this.cdr.markForCheck();
          }
        },
      });
  }

  private buildOrgContactFallback(org: OrganizationSummary): Observable<{
    fallback: Pick<BranchDetail, 'email' | 'phoneNumber' | 'region' | 'businessHours'>;
    headOfficeBranch: BranchDetail | null;
  }> {
    const headOffice = org.headOfficeBranch ?? null;
    const baseFallback = {
      email: headOffice?.email || org.email?.trim() || undefined,
      phoneNumber: headOffice?.phoneNumber || org.phoneNumber,
      region: headOffice?.region || this.resolveOrgRegionFallback(org),
      businessHours: headOffice?.businessHours || org.businessHours,
    };
    if (baseFallback.region || !org.locationId || org.locationId <= 0) {
      return of({ fallback: baseFallback, headOfficeBranch: headOffice });
    }
    return this.locationsService.findLocationById('address', org.locationId).pipe(
      map((address) => ({
        fallback: {
          ...baseFallback,
          region: baseFallback.region || this.regionFromAddressRecord(address),
        },
        headOfficeBranch: headOffice
          ? {
              ...headOffice,
              region: headOffice.region || this.regionFromAddressRecord(address),
            }
          : null,
      })),
      catchError(() => of({ fallback: baseFallback, headOfficeBranch: headOffice })),
    );
  }

  private regionFromAddressRecord(dto: Record<string, unknown> | null): string | undefined {
    if (!dto) {
      return undefined;
    }
    const province = String(dto['provinceName'] ?? '').trim();
    if (province) {
      return province;
    }
    const district = String(dto['districtName'] ?? '').trim();
    if (district) {
      return district;
    }
    return String(dto['cityName'] ?? '').trim() || undefined;
  }

  private resolveOrgRegionFallback(org: OrganizationSummary): string | undefined {
    const fromRegions = org.regionsServed?.split(',')[0]?.trim() || org.regionsServed?.trim();
    if (fromRegions) {
      return fromRegions;
    }
    return org.addressProvinceName || org.addressDistrictName || org.addressCityName;
  }

  private currentQuery() {
    return {
      page: this.pageIndex,
      size: this.pageSize,
      searchQuery: this.searchQuery,
      branchName: this.columnFilters.branchName,
      region: this.columnFilters.region,
      branchLevel: this.isSubLevel ? ('SUB_BRANCH' as const) : ('BRANCH' as const),
      depot: this.columnFilters.depot,
      parentBranchId: this.columnFilters.parentBranchId,
      active: this.columnFilters.active,
    };
  }

  private currentFilterSignature(): string {
    return JSON.stringify({
      searchQuery: this.searchQuery.trim(),
      branchName: this.columnFilters.branchName.trim(),
      region: this.columnFilters.region.trim(),
      parentBranchId: this.columnFilters.parentBranchId,
      depot: this.columnFilters.depot,
      active: this.columnFilters.active,
      scope: this.scope,
    });
  }
}
