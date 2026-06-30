import { ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { Subject, debounceTime, firstValueFrom, forkJoin, of, Observable } from 'rxjs';
import { catchError, filter, finalize, map, switchMap, takeUntil } from 'rxjs/operators';
import { DeleteConfirmDialogComponent } from '../../../../shared/components/delete-confirm-dialog/delete-confirm-dialog.component';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { OrgContextService } from '../../../../core/services/org-context.service';
import { OrganizationService, BranchAllocationOption } from '../../../../core/services/organization.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { UserProfileService } from '../../../../core/services/user-profile.service';
import { LocationsService } from '../../../locations/services/locations.service';
import { InventoryPortalService } from '../../services/inventory-portal.service';
import {
  BillingSettingsService,
  type ProcurementPaymentRow,
} from '../../../settings/services/billing-settings.service';
import { formatInventoryAddressLabel } from '../../utils/inventory-address.util';
import {
  entityStatusCssClass,
  entityStatusLabel,
  purchaseOrderStatusCssClass,
  requisitionStatusCssClass,
  salesOrderStatusCssClass,
  transferStatusCssClass,
} from '../../utils/inventory-status.util';
import { transferRouteSummary } from '../../utils/route-stops.util';
import {
  STOCK_STATUS_FILTER_OPTIONS,
  stockLevelStatusCssClass,
  type StockLevelStatus,
} from '../../utils/stock-status.util';
import { CategoryDialogComponent } from '../../components/category-dialog/category-dialog.component';
import type { CategoryDialogData } from '../../components/category-dialog/category-dialog.component';
import { SubcategoryDialogComponent } from '../../components/subcategory-dialog/subcategory-dialog.component';
import type { SubcategoryDialogData } from '../../components/subcategory-dialog/subcategory-dialog.component';
import { AddProductDialogComponent } from '../../components/add-product-dialog/add-product-dialog.component';
import { SubmitSupplierQuoteDialogComponent } from '../../components/submit-supplier-quote-dialog/submit-supplier-quote-dialog.component';
import { ViewSupplierQuoteDialogComponent } from '../../components/view-supplier-quote-dialog/view-supplier-quote-dialog.component';
import type { ViewSupplierQuoteDialogData } from '../../components/view-supplier-quote-dialog/view-supplier-quote-dialog.component';
import { ProcurementStageTimelineDialogComponent } from '../../components/procurement-stage-timeline-dialog/procurement-stage-timeline-dialog.component';
import type { ProcurementStageTimelineDialogData } from '../../components/procurement-stage-timeline-dialog/procurement-stage-timeline-dialog.component';
import { buildSalesOrderJourney } from '../../utils/procurement-journey.util';
import { defaultWarehouseTypeForClassification, filterByOrganizationScope } from '../../utils/inventory-org-scope.util';
import { isCustomerCatalogTab } from '../../utils/customer-inventory-tabs.util';
import type { AddProductDialogData } from '../../components/add-product-dialog/add-product-dialog.component';
import { AddWarehouseDialogComponent } from '../../components/add-warehouse-dialog/add-warehouse-dialog.component';
import type { AddWarehouseDialogData } from '../../components/add-warehouse-dialog/add-warehouse-dialog.component';
import { CreateTransferDialogComponent } from '../../components/create-transfer-dialog/create-transfer-dialog.component';
import type { CreateTransferDialogData } from '../../components/create-transfer-dialog/create-transfer-dialog.component';
import { InitialStockDialogComponent } from '../../components/initial-stock-dialog/initial-stock-dialog.component';
import type { InitialStockDialogData } from '../../components/initial-stock-dialog/initial-stock-dialog.component';
import { ReplenishStockDialogComponent } from '../../components/replenish-stock-dialog/replenish-stock-dialog.component';
import {
  InventoryDetailDialogComponent,
  InventoryDetailDialogData,
  InventoryDetailField,
} from '../../components/inventory-detail-dialog/inventory-detail-dialog.component';
import { ViewTransferDialogComponent } from '../../components/view-transfer-dialog/view-transfer-dialog.component';
import type { ViewTransferDialogResult } from '../../components/view-transfer-dialog/view-transfer-dialog.component';
import { EditTransferDialogComponent } from '../../components/edit-transfer-dialog/edit-transfer-dialog.component';
import type { EditTransferDialogData } from '../../components/edit-transfer-dialog/edit-transfer-dialog.component';
import { WarehouseSharingDialogComponent } from '../../components/warehouse-sharing-dialog/warehouse-sharing-dialog.component';
import type { WarehouseSharingDialogData } from '../../components/warehouse-sharing-dialog/warehouse-sharing-dialog.component';
import {
  InventoryWorkspaceMetrics,
  InventoryWorkspaceTab,
  CatalogWorkspaceView,
  ProductCategoryOption,
  ProductCategoryRow,
  ProductRow,
  ProductSubCategoryRow,
  PurchaseOrderRow,
  PurchaseRequisitionRow,
  SalesOrderRow,
  SupplierQuoteRow,
  SupplierQuoteStatus,
  QuoteCaptureSource,
  RequisitionStatus,
  QUOTE_SOURCE_FILTER_OPTIONS,
  SUPPLIER_QUOTE_STATUS_FILTER_OPTIONS,
  SUPPLIER_REQUISITION_STATUS_FILTER_OPTIONS,
  StockRow,
  TRANSFER_STATUS_FILTER_OPTIONS,
  TransferRow,
  TransferStatus,
  WarehouseRow,
} from '../../models/inventory.model';
import { DuplexTradingModeService } from '../../../../core/services/duplex-trading-mode.service';
import {
  isCustomerOrganization,
  isSupplierOrganization,
} from '../../../../core/utils/org-classification.util';
import {
  LxExportFormat,
  downloadBlob,
  exportClientTableAsCsv,
  exportFilename,
  exportFormatLabel,
  exportRowsAsCsv,
  type LxExportColumn,
} from '../../../../shared/utils/lx-export.util';
import {
  INVENTORY_PAGE_SIZE_OPTIONS,
  InventoryTablePage,
  inventoryPageSummary,
} from '../../utils/inventory-table-page.util';
import {
  CATEGORY_SORT_COMPARATORS,
  createInventoryTableSortController,
  PRODUCT_SORT_COMPARATORS,
  PURCHASE_ORDER_SORT_COMPARATORS,
  QUOTE_SORT_COMPARATORS,
  REQUISITION_SORT_COMPARATORS,
  SALES_ORDER_SORT_COMPARATORS,
  STOCK_SORT_COMPARATORS,
  SUBCATEGORY_SORT_COMPARATORS,
  TRANSFER_SORT_COMPARATORS,
  WAREHOUSE_SORT_COMPARATORS,
} from '../../utils/inventory-table-sort.util';

@Component({
  selector: 'app-inventory-workspace',
  templateUrl: './inventory-workspace.component.html',
  styleUrl: './inventory-workspace.component.scss',
  standalone: false,
})
export class InventoryWorkspaceComponent implements OnInit, OnDestroy {
  @ViewChild('warehouseImportInput') warehouseImportInput?: ElementRef<HTMLInputElement>;
  @ViewChild('stockImportInput') stockImportInput?: ElementRef<HTMLInputElement>;
  @ViewChild('categoryImportInput') categoryImportInput?: ElementRef<HTMLInputElement>;
  @ViewChild('subcategoryImportInput') subcategoryImportInput?: ElementRef<HTMLInputElement>;
  @ViewChild('productImportInput') productImportInput?: ElementRef<HTMLInputElement>;
  @ViewChild('quotationImportInput') quotationImportInput?: ElementRef<HTMLInputElement>;
  @ViewChild('transferImportInput') transferImportInput?: ElementRef<HTMLInputElement>;

  fetching = true;
  loadError = '';
  activeTab: InventoryWorkspaceTab = 'warehouses';

  products: ProductRow[] = [];
  private rawProducts: ProductRow[] = [];
  productSearch = '';
  productsLoading = false;
  productsError = '';
  productsExporting = false;
  productsImporting = false;
  showProductCsvInfo = false;

  warehouses: WarehouseRow[] = [];
  warehousesLoading = false;
  warehousesError = '';
  warehousesExporting = false;
  warehousesImporting = false;
  showWarehouseCsvInfo = false;

  readonly warehouseImportCsvDisclaimer =
    'CSV import only. Required columns: NAME, DESCRIPTION, LINE1, SUBURB_ID, SUPPLIER_ID. Export files are read-only snapshots — use Sample CSV for imports.';
  readonly warehouseSampleCsvDescription =
    'Use this template to bulk-create warehouses. SUBURB_ID must exist in the location service; SUPPLIER_ID is your organisation id.';

  stock: StockRow[] = [];
  stockSearch = '';
  stockProductFilter = 0;
  stockStatusFilter: '' | StockLevelStatus = '';
  stockWarehouseFilter = 0;
  stockDrillOrigin: 'product' | 'warehouse' | null = null;
  stockLoading = false;
  stockError = '';
  stockExporting = false;
  stockImporting = false;
  showStockCsvInfo = false;

  transfers: TransferRow[] = [];
  transfersLoading = false;
  transfersError = '';
  transfersExporting = false;
  transfersImporting = false;
  showTransferCsvInfo = false;
  transferStatusFilter: '' | TransferStatus = '';
  transferProductFilter = 0;
  transferFromWarehouseFilter = 0;
  transferToWarehouseFilter = 0;

  purchaseOrders: PurchaseOrderRow[] = [];
  poLoading = false;
  poError = '';

  salesOrders: SalesOrderRow[] = [];
  salesOrdersLoading = false;
  salesOrdersError = '';

  pendingProcurementPayments: ProcurementPaymentRow[] = [];
  pendingPaymentsLoading = false;
  pendingPaymentsError = '';
  paymentVerifyBusyId: number | null = null;

  pendingRequisitions: PurchaseRequisitionRow[] = [];
  requisitionsLoading = false;
  supplierQuotations: SupplierQuoteRow[] = [];
  quotationsLoading = false;
  quotationsError = '';
  quotationsExporting = false;
  quotationsImporting = false;
  showQuotationCsvInfo = false;
  quotationSearch = '';
  quotationStatusFilter: '' | SupplierQuoteStatus = '';
  quotationSourceFilter: '' | QuoteCaptureSource = '';
  requisitionStatusFilter: '' | RequisitionStatus = '';
  requisitionsExporting = false;
  requisitionsError = '';

  private userId = 0;
  isProcurementApprover = false;

  categoryRows: ProductCategoryRow[] = [];
  subcategoryRows: ProductSubCategoryRow[] = [];
  catalogLoading = false;
  catalogError = '';
  catalogView: CatalogWorkspaceView = 'categories';
  categorySearch = '';
  subcategorySearch = '';
  subcategoryCategoryFilter = 0;
  categoriesExporting = false;
  categoriesImporting = false;
  showCategoryCsvInfo = false;
  subcategoriesExporting = false;
  subcategoriesImporting = false;
  showSubcategoryCsvInfo = false;

  readonly categoryImportCsvDisclaimer =
    'CSV import only. Required columns: NAME, DESCRIPTION. Export files are read-only snapshots — use Sample CSV for imports.';
  readonly categorySampleCsvDescription =
    'Use this template to bulk-create product categories. NAME must be unique within your organisation.';

  readonly subcategoryImportCsvDisclaimer =
    'CSV import only. Required columns: CATEGORY_ID, NAME, DESCRIPTION. CATEGORY_ID must match an existing category. Use Sample CSV for imports.';
  readonly subcategorySampleCsvDescription =
    'Use this template to bulk-create subcategories. CATEGORY_ID is the numeric id from the categories list.';

  readonly productImportCsvDisclaimer =
    'CSV import only. Required: NAME, PRODUCT_CODE, PRICE, UNIT_OF_MEASURE, PRODUCT_CATEGORY_ID, SUPPLIER_ID. UNIT_OF_MEASURE accepts enum values (EACH, CYLINDER, SERVICE, …) or aliases UNIT/UNITS and KG. Optional: BARCODE, DESCRIPTION, PRODUCT_SUB_CATEGORY_ID, MANUFACTURER, EXPIRES_AT.';

  readonly stockImportCsvDisclaimer =
    'CSV import only. Required: PRODUCT_ID, PRODUCT_CODE, or BARCODE (one of), plus WAREHOUSE_LOCATION_ID and CURRENT_STOCK. Optional: SUPPLIER_ID, MIN_STOCK_LEVEL, REORDER_QUANTITY, BATCH_LOT, SERIAL_NUMBER, EXPIRES_AT. Do not use the products catalogue CSV here.';
  readonly stockSampleCsvDescription =
    'Use this template to bulk-create stock records. Products and warehouses must already exist. PRODUCT_CODE or BARCODE is easier than numeric PRODUCT_ID.';

  readonly stockStatusFilterOptions = STOCK_STATUS_FILTER_OPTIONS;
  readonly supplierQuoteStatusFilterOptions = SUPPLIER_QUOTE_STATUS_FILTER_OPTIONS;
  readonly quoteSourceFilterOptions = QUOTE_SOURCE_FILTER_OPTIONS;
  readonly supplierRequisitionStatusFilterOptions = SUPPLIER_REQUISITION_STATUS_FILTER_OPTIONS;
  readonly productSampleCsvDescription =
    'Use this template to bulk-create products. SUPPLIER_ID is your organisation id; category and subcategory ids must already exist.';
  readonly quotationImportCsvDisclaimer =
    'CSV import only. One quote per requisition. Required header row plus line rows sharing the same PURCHASE_REQUISITION_ID. Requisition must be published to your organisation.';
  readonly quotationSampleCsvDescription =
    'Use this template to bulk-submit quotes. PURCHASE_REQUISITION_ID must be awaiting quote. Include one row per line with UNIT_PRICE and QUOTED_QUANTITY.';

  readonly transferImportCsvDisclaimer =
    'CSV import only. Required: PRODUCT_ID, FROM_LOCATION_ID, TO_LOCATION_ID, QUANTITY, CREATED_BY_USER_ID. Optional: REFERENCE. Export files are read-only snapshots — use Sample CSV for imports.';
  readonly transferSampleCsvDescription =
    'Use this template to bulk-create transfer requests. Product and warehouses must exist; stock must be available at the source warehouse. Transfers are created in REQUESTED status.';

  readonly transferStatusFilterOptions = TRANSFER_STATUS_FILTER_OPTIONS;

  warehouseSearch = '';
  /** When set, warehouses tab lists only locations allocated to this branch / sub-branch id. */
  warehouseBranchFilter = 0;
  private pendingOpenAddWarehouse = false;
  private branchOptions: BranchAllocationOption[] = [];
  transferSearch = '';
  transferFiltersOpen = false;
  requisitionSearch = '';
  poSearch = '';

  readonly pageSizeOptions = INVENTORY_PAGE_SIZE_OPTIONS;
  readonly productsPage = new InventoryTablePage();
  readonly categoriesPage = new InventoryTablePage();
  readonly subcategoriesPage = new InventoryTablePage();
  readonly warehousesPage = new InventoryTablePage();
  readonly stockPage = new InventoryTablePage();
  readonly productStockWarehousesPage = new InventoryTablePage();
  readonly warehouseStockProductsPage = new InventoryTablePage();
  readonly transfersPage = new InventoryTablePage();
  readonly requisitionsPage = new InventoryTablePage();
  readonly quotationsPage = new InventoryTablePage();
  readonly purchaseOrdersPage = new InventoryTablePage();

  readonly productSort = createInventoryTableSortController(PRODUCT_SORT_COMPARATORS, 'name', 'asc');
  readonly categorySort = createInventoryTableSortController(CATEGORY_SORT_COMPARATORS, 'name', 'asc');
  readonly subcategorySort = createInventoryTableSortController(SUBCATEGORY_SORT_COMPARATORS, 'name', 'asc');
  readonly warehouseSort = createInventoryTableSortController(WAREHOUSE_SORT_COMPARATORS, 'name', 'asc');
  readonly stockSort = createInventoryTableSortController(STOCK_SORT_COMPARATORS, 'productName', 'asc');
  readonly transferSort = createInventoryTableSortController(TRANSFER_SORT_COMPARATORS, 'transferNumber', 'desc');
  readonly requisitionSort = createInventoryTableSortController(REQUISITION_SORT_COMPARATORS, 'requisitionNumber', 'desc');
  readonly quoteSort = createInventoryTableSortController(QUOTE_SORT_COMPARATORS, 'submittedAtLabel', 'desc');
  readonly salesOrderSort = createInventoryTableSortController(SALES_ORDER_SORT_COMPARATORS, 'createdAtLabel', 'desc');
  readonly purchaseOrderSort = createInventoryTableSortController(PURCHASE_ORDER_SORT_COMPARATORS, 'createdAtLabel', 'desc');

  actionProduct: ProductRow | null = null;
  actionCategory: ProductCategoryRow | null = null;
  actionSubCategory: ProductSubCategoryRow | null = null;
  actionWarehouse: WarehouseRow | null = null;
  actionStock: StockRow | null = null;
  actionTransfer: TransferRow | null = null;
  actionRequisition: PurchaseRequisitionRow | null = null;
  actionPurchaseOrder: PurchaseOrderRow | null = null;

  private readonly reload$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();
  /** Prevents route sync from wiping product/warehouse drill filters after intentional navigation. */
  private preserveStockDrillOnRoute = false;
  private branchLabelById = new Map<number, string>();

  constructor(
    private readonly title: Title,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly inventoryService: InventoryPortalService,
    private readonly organizationService: OrganizationService,
    private readonly dialog: MatDialog,
    private readonly notifications: NotificationService,
    private readonly authState: AuthStateService,
    private readonly duplexTradingMode: DuplexTradingModeService,
    private readonly orgContext: OrgContextService,
    private readonly userProfile: UserProfileService,
    private readonly locationsService: LocationsService,
    private readonly billingSettings: BillingSettingsService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Inventory management | LX Platform');
    this.userProfile
      .fetchCurrentUser()
      .pipe(takeUntil(this.destroy$))
      .subscribe((profile) => {
        this.userId = profile?.id ?? Number(this.authState.currentUser?.userId ?? 0);
        this.isProcurementApprover =
          profile?.procurementApprover === true || this.authState.currentUser?.procurementApprover === true;
        this.cdr.markForCheck();
      });

    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.syncTabFromRoute();
    });
    this.syncTabFromRoute();

    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const tab = params.get('tab') as InventoryWorkspaceTab | null;
      if (tab && this.isValidTab(tab)) {
        this.applyRouteTab(tab);
      }
      const branchId = Number(params.get('branchId'));
      this.warehouseBranchFilter = Number.isFinite(branchId) && branchId > 0 ? Math.trunc(branchId) : 0;
      this.pendingOpenAddWarehouse = params.get('openAdd') === '1';
      if (this.pendingOpenAddWarehouse) {
        this.applyRouteTab('warehouses');
      }
      this.warehousesPage.reset();
      this.cdr.markForCheck();
    });
    this.reload$.pipe(debounceTime(120), takeUntil(this.destroy$)).subscribe(() => this.loadWorkspace());
    this.loadBranchLabels();
    this.reload$.next();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get orgName(): string {
    return this.authState.currentUser?.orgName ?? 'Your organisation';
  }

  get isSupplier(): boolean {
    const user = this.authState.currentUser;
    return isSupplierOrganization(
      user?.orgClassification,
      user?.duplexMode,
      this.duplexTradingMode.activeMode,
    );
  }

  get isBillingApprover(): boolean {
    return this.authState.currentUser?.billingApprover === true;
  }

  get isCustomer(): boolean {
    const user = this.authState.currentUser;
    return isCustomerOrganization(
      user?.orgClassification,
      user?.duplexMode,
      this.duplexTradingMode.activeMode,
    );
  }

  /** ERP / cross-dock API key setup — only when inventory is fed via external API or cross-dock mode. */
  get showIntegrationSetupTab(): boolean {
    const user = this.authState.currentUser;
    if (!user) {
      return false;
    }
    const crossDockOnly = !!user.crossDockingEnabled && user.inventoryManagementEnabled === false;
    if (crossDockOnly) {
      return true;
    }
    return user.inventoryDataSource === 'EXTERNAL_API';
  }

  get isOrgAdministrator(): boolean {
    const roles = this.authState.currentUser?.roles ?? [];
    const roleLabel = (this.authState.currentUser?.roleLabel ?? '').trim().toUpperCase();
    return (
      roles.includes('ORGANIZATION_ADMINISTRATOR') ||
      roles.includes('ADMIN') ||
      roleLabel === 'ADMINISTRATOR'
    );
  }

  get canManageProductCatalogue(): boolean {
    return this.isSupplier || this.isCustomerInventoryWorkspace;
  }

  /** Supplier catalogue lives under /products-inventory; customers use /my-orders. */
  get inventoryBasePath(): '/products-inventory' | '/my-orders' {
    if (this.route.snapshot.data['customerRoute'] === true) {
      return '/my-orders';
    }
    return this.isCustomer ? '/my-orders' : '/products-inventory';
  }

  get categories(): ProductCategoryOption[] {
    return this.categoryRows.map((row) => ({ id: row.id, name: row.name, code: '' }));
  }

  get filteredSubcategories(): ProductSubCategoryRow[] {
    let rows = this.subcategoryRows;
    if (this.subcategoryCategoryFilter) {
      rows = rows.filter((row) => row.categoryId === this.subcategoryCategoryFilter);
    }
    const q = this.subcategorySearch.trim().toLowerCase();
    if (!q) {
      return rows;
    }
    return rows.filter((row) => {
      const hay = `${row.name} ${row.categoryName} ${row.description}`.toLowerCase();
      return hay.includes(q);
    });
  }

  get filteredCategories(): ProductCategoryRow[] {
    const q = this.categorySearch.trim().toLowerCase();
    if (!q) {
      return this.categoryRows;
    }
    return this.categoryRows.filter((row) => {
      const hay = `${row.name} ${row.description}`.toLowerCase();
      return hay.includes(q);
    });
  }

  get sortedProducts(): ProductRow[] {
    return this.productSort.sort(this.filteredProducts);
  }

  get paginatedProducts(): ProductRow[] {
    this.productsPage.clamp(this.sortedProducts.length);
    return this.productsPage.slice(this.sortedProducts);
  }

  get sortedCategories(): ProductCategoryRow[] {
    return this.categorySort.sort(this.filteredCategories);
  }

  get paginatedCategories(): ProductCategoryRow[] {
    this.categoriesPage.clamp(this.sortedCategories.length);
    return this.categoriesPage.slice(this.sortedCategories);
  }

  get sortedSubcategories(): ProductSubCategoryRow[] {
    return this.subcategorySort.sort(this.filteredSubcategories);
  }

  get paginatedSubcategories(): ProductSubCategoryRow[] {
    this.subcategoriesPage.clamp(this.sortedSubcategories.length);
    return this.subcategoriesPage.slice(this.sortedSubcategories);
  }

  get filteredWarehouses(): WarehouseRow[] {
    let rows = this.warehouses;
    if (this.warehouseBranchFilter > 0) {
      rows = rows.filter((row) => row.branchId === this.warehouseBranchFilter);
    }
    const q = this.warehouseSearch.trim().toLowerCase();
    if (!q) {
      return rows;
    }
    return rows.filter((row) => {
      const hay = `${row.name} ${row.description} ${row.addressLabel} ${row.warehouseType} ${row.branchLabel ?? ''}`.toLowerCase();
      return hay.includes(q);
    });
  }

  get warehouseBranchFilterLabel(): string {
    if (this.warehouseBranchFilter <= 0) {
      return '';
    }
    return this.branchLabelById.get(this.warehouseBranchFilter) ?? `Branch #${this.warehouseBranchFilter}`;
  }

  get sortedWarehouses(): WarehouseRow[] {
    return this.warehouseSort.sort(this.filteredWarehouses);
  }

  get paginatedWarehouses(): WarehouseRow[] {
    this.warehousesPage.clamp(this.sortedWarehouses.length);
    return this.warehousesPage.slice(this.sortedWarehouses);
  }

  get sortedStock(): StockRow[] {
    return this.stockSort.sort(this.filteredStock);
  }

  get paginatedStock(): StockRow[] {
    this.stockPage.clamp(this.sortedStock.length);
    return this.stockPage.slice(this.sortedStock);
  }

  /** True when viewing the warehouse list for a drilled-in product. */
  get showingProductStockWarehouseDrill(): boolean {
    return this.stockDrillOrigin === 'product' && this.stockProductFilter > 0 && this.stockWarehouseFilter === 0;
  }

  /** True when viewing the product list for a drilled-in warehouse. */
  get showingWarehouseStockProductDrill(): boolean {
    return this.stockDrillOrigin === 'warehouse' && this.stockWarehouseFilter > 0 && this.stockProductFilter === 0;
  }

  /** Stock rows for the drilled product — one row per warehouse that has on-hand quantity. */
  get warehousesWithProductStock(): StockRow[] {
    if (this.stockProductFilter <= 0) {
      return [];
    }
    const q = this.stockSearch.trim().toLowerCase();
    const filtered = this.stock.filter((s) => {
      if (s.productId !== this.stockProductFilter || s.quantityOnHand <= 0) {
        return false;
      }
      if (this.stockStatusFilter && s.status !== this.stockStatusFilter) {
        return false;
      }
      if (!q) {
        return true;
      }
      const hay = `${s.warehouseName} ${s.statusLabel} ${s.unitOfMeasure}`.toLowerCase();
      return hay.includes(q);
    });
    return this.stockSort.sort(filtered);
  }

  get paginatedProductStockWarehouses(): StockRow[] {
    this.productStockWarehousesPage.clamp(this.warehousesWithProductStock.length);
    return this.productStockWarehousesPage.slice(this.warehousesWithProductStock);
  }

  /** Stock rows for the drilled warehouse — one row per product that has on-hand quantity. */
  get productsWithWarehouseStock(): StockRow[] {
    if (this.stockWarehouseFilter <= 0) {
      return [];
    }
    const q = this.stockSearch.trim().toLowerCase();
    const filtered = this.stock.filter((s) => {
      if (s.warehouseLocationId !== this.stockWarehouseFilter || s.quantityOnHand <= 0) {
        return false;
      }
      if (this.stockStatusFilter && s.status !== this.stockStatusFilter) {
        return false;
      }
      if (!q) {
        return true;
      }
      const hay = `${s.productName} ${s.productCode} ${s.productBarcode} ${s.statusLabel} ${s.unitOfMeasure}`.toLowerCase();
      return hay.includes(q);
    });
    return this.stockSort.sort(filtered);
  }

  get paginatedWarehouseStockProducts(): StockRow[] {
    this.warehouseStockProductsPage.clamp(this.productsWithWarehouseStock.length);
    return this.warehouseStockProductsPage.slice(this.productsWithWarehouseStock);
  }

  /** Warehouses shown in the stock tab filter — scoped to stocked locations when a product is drilled in. */
  get stockWarehouseFilterOptions(): WarehouseRow[] {
    if (this.stockProductFilter > 0) {
      const warehouseIds = new Set(
        this.stock
          .filter((s) => s.productId === this.stockProductFilter && s.quantityOnHand > 0)
          .map((s) => s.warehouseLocationId),
      );
      return this.warehouses.filter((warehouse) => warehouseIds.has(warehouse.id));
    }
    return this.warehouses;
  }

  get filteredTransfers(): TransferRow[] {
    const q = this.transferSearch.trim().toLowerCase();
    return this.transfers.filter((row) => {
      if (this.transferStatusFilter && row.status !== this.transferStatusFilter) {
        return false;
      }
      if (this.transferProductFilter > 0 && row.productId !== this.transferProductFilter) {
        return false;
      }
      if (this.transferFromWarehouseFilter > 0 && row.fromLocationId !== this.transferFromWarehouseFilter) {
        return false;
      }
      if (this.transferToWarehouseFilter > 0 && row.toLocationId !== this.transferToWarehouseFilter) {
        return false;
      }
      if (!q) {
        return true;
      }
      const hay =
        `${row.transferNumber} ${row.productName} ${row.productCode} ${row.fromWarehouse} ${row.toWarehouse} ${row.statusLabel} ${row.requestedBy} ${row.reference}`.toLowerCase();
      return hay.includes(q);
    });
  }

  get sortedTransfers(): TransferRow[] {
    return this.transferSort.sort(this.filteredTransfers);
  }

  get paginatedTransfers(): TransferRow[] {
    this.transfersPage.clamp(this.sortedTransfers.length);
    return this.transfersPage.slice(this.sortedTransfers);
  }

  get quoteableRequisitions(): PurchaseRequisitionRow[] {
    return this.pendingRequisitions.filter((row) => row.status === 'PUBLISHED_TO_SUPPLIER');
  }

  get filteredRequisitions(): PurchaseRequisitionRow[] {
    const q = this.requisitionSearch.trim().toLowerCase();
    return this.pendingRequisitions.filter((row) => {
      if (this.requisitionStatusFilter && row.status !== this.requisitionStatusFilter) {
        return false;
      }
      if (!q) {
        return true;
      }
      const hay = `${row.requisitionNumber} ${row.purpose} ${row.priorityLabel} ${row.statusLabel}`.toLowerCase();
      return hay.includes(q);
    });
  }

  get filteredSupplierQuotations(): SupplierQuoteRow[] {
    const q = this.quotationSearch.trim().toLowerCase();
    return this.supplierQuotations.filter((row) => {
      if (this.quotationStatusFilter && row.status !== this.quotationStatusFilter) {
        return false;
      }
      if (this.quotationSourceFilter && row.quoteSource !== this.quotationSourceFilter) {
        return false;
      }
      if (!q) {
        return true;
      }
      const hay =
        `${row.quoteNumber} ${row.requisitionNumber} ${row.statusLabel} ${row.quoteSourceLabel} ${row.totalAmountLabel}`.toLowerCase();
      return hay.includes(q);
    });
  }

  get sortedRequisitions(): PurchaseRequisitionRow[] {
    return this.requisitionSort.sort(this.filteredRequisitions);
  }

  get sortedSupplierQuotations(): SupplierQuoteRow[] {
    return this.quoteSort.sort(this.filteredSupplierQuotations);
  }

  get paginatedSupplierQuotations(): SupplierQuoteRow[] {
    this.quotationsPage.clamp(this.sortedSupplierQuotations.length);
    return this.quotationsPage.slice(this.sortedSupplierQuotations);
  }

  get paginatedRequisitions(): PurchaseRequisitionRow[] {
    this.requisitionsPage.clamp(this.sortedRequisitions.length);
    return this.requisitionsPage.slice(this.sortedRequisitions);
  }

  get filteredPurchaseOrders(): PurchaseOrderRow[] {
    const q = this.poSearch.trim().toLowerCase();
    if (!q) {
      return this.purchaseOrders;
    }
    return this.purchaseOrders.filter((row) => {
      const hay = `${row.orderNumber} ${row.customerName} ${row.supplierName} ${row.statusLabel}`.toLowerCase();
      return hay.includes(q);
    });
  }

  get sortedPurchaseOrders(): PurchaseOrderRow[] {
    return this.purchaseOrderSort.sort(this.filteredPurchaseOrders);
  }

  get paginatedPurchaseOrders(): PurchaseOrderRow[] {
    this.purchaseOrdersPage.clamp(this.sortedPurchaseOrders.length);
    return this.purchaseOrdersPage.slice(this.sortedPurchaseOrders);
  }

  get sortedSalesOrders(): SalesOrderRow[] {
    return this.salesOrderSort.sort(this.salesOrders);
  }

  get metrics(): InventoryWorkspaceMetrics {
    return this.inventoryService.buildMetrics(
      this.products,
      this.categoryRows,
      this.subcategoryRows,
      this.warehouses,
      this.stock,
      this.transfers,
      this.pendingRequisitions,
      this.purchaseOrders,
    );
  }

  get filteredProducts(): ProductRow[] {
    const q = this.productSearch.trim().toLowerCase();
    if (!q) {
      return this.products;
    }
    return this.products.filter((p) => {
      const hay = `${p.name} ${p.code} ${p.categoryName} ${p.subcategoryName}`.toLowerCase();
      return hay.includes(q);
    });
  }

  get filteredStock(): StockRow[] {
    const q = this.stockSearch.trim().toLowerCase();
    return this.stock.filter((s) => {
      if (this.stockProductFilter > 0 && s.productId !== this.stockProductFilter) {
        return false;
      }
      if (this.stockStatusFilter && s.status !== this.stockStatusFilter) {
        return false;
      }
      if (this.stockWarehouseFilter > 0 && s.warehouseLocationId !== this.stockWarehouseFilter) {
        return false;
      }
      if (!q) {
        return true;
      }
      const hay = `${s.productName} ${s.productCode} ${s.productBarcode} ${s.warehouseName} ${s.statusLabel} ${s.unitOfMeasure}`.toLowerCase();
      return hay.includes(q);
    });
  }

  get selectedDrillProduct(): ProductRow | null {
    if (this.stockProductFilter <= 0) {
      return null;
    }
    return this.products.find((product) => product.id === this.stockProductFilter) ?? null;
  }

  get selectedDrillWarehouse(): WarehouseRow | null {
    if (this.stockWarehouseFilter <= 0) {
      return null;
    }
    return this.warehouses.find((warehouse) => warehouse.id === this.stockWarehouseFilter) ?? null;
  }

  toggleTableSort(
    controller: { toggle(column: string): void },
    column: string,
    ...pages: InventoryTablePage[]
  ): void {
    controller.toggle(column);
    pages.forEach((page) => page.reset());
    this.cdr.markForCheck();
  }

  onStockFiltersChanged(): void {
    this.stockPage.reset();
    this.productStockWarehousesPage.reset();
    this.warehouseStockProductsPage.reset();
  }

  onStockSearchChanged(): void {
    this.stockProductFilter = 0;
    this.stockWarehouseFilter = 0;
    this.stockDrillOrigin = null;
    this.onStockFiltersChanged();
  }

  onTransferFiltersChanged(): void {
    this.transfersPage.reset();
  }

  onTransferSearchChanged(): void {
    this.onTransferFiltersChanged();
  }

  clearStockProductDrill(): void {
    this.stockProductFilter = 0;
    if (this.stockDrillOrigin !== 'warehouse') {
      this.stockWarehouseFilter = 0;
      this.stockDrillOrigin = null;
    }
    this.stockPage.reset();
    this.productStockWarehousesPage.reset();
    this.warehouseStockProductsPage.reset();
  }

  clearStockWarehouseDrill(): void {
    this.stockWarehouseFilter = 0;
    if (this.stockDrillOrigin !== 'product') {
      this.stockProductFilter = 0;
      this.stockDrillOrigin = null;
    }
    this.stockPage.reset();
    this.productStockWarehousesPage.reset();
    this.warehouseStockProductsPage.reset();
  }

  clearStockDrillDown(): void {
    this.stockProductFilter = 0;
    this.stockWarehouseFilter = 0;
    this.stockDrillOrigin = null;
    this.stockSearch = '';
    this.stockPage.reset();
    this.productStockWarehousesPage.reset();
    this.warehouseStockProductsPage.reset();
  }

  refresh(): void {
    this.reload$.next();
  }

  setTab(tab: InventoryWorkspaceTab, options?: { keepStockFilters?: boolean }): void {
    if (tab === 'stock' && !options?.keepStockFilters) {
      this.clearStockDrillDown();
    }
    this.activeTab = tab;
    this.ensureTabDataLoaded(tab);
    if (tab === 'stock' && options?.keepStockFilters) {
      this.preserveStockDrillOnRoute = true;
    }
    void this.router.navigate([this.inventoryBasePath, tab]);
  }

  setCatalogView(view: CatalogWorkspaceView): void {
    this.catalogView = view;
    if (view === 'categories') {
      this.subcategoryCategoryFilter = 0;
      this.subcategorySearch = '';
      this.categoriesPage.reset();
    } else {
      this.subcategoriesPage.reset();
    }
  }

  drillIntoCategorySubcategories(category: ProductCategoryRow): void {
    this.subcategoryCategoryFilter = category.id;
    this.subcategorySearch = '';
    this.subcategoriesPage.reset();
    this.catalogView = 'subcategories';
  }

  drillIntoCategoryById(categoryId: number): void {
    const category = this.categoryRows.find((row) => row.id === categoryId);
    if (category) {
      this.drillIntoCategorySubcategories(category);
    }
  }

  backToCategoriesFromDrill(): void {
    this.subcategoryCategoryFilter = 0;
    this.subcategorySearch = '';
    this.catalogView = 'categories';
    this.categoriesPage.reset();
  }

  clearSubcategoryDrillDown(): void {
    this.subcategoryCategoryFilter = 0;
    this.subcategorySearch = '';
    this.subcategoriesPage.reset();
  }

  subcategoryCountForCategory(categoryId: number): number {
    return this.subcategoryRows.filter((row) => row.categoryId === categoryId).length;
  }

  get selectedDrillCategory(): ProductCategoryRow | null {
    if (!this.subcategoryCategoryFilter) {
      return null;
    }
    return this.categoryRows.find((row) => row.id === this.subcategoryCategoryFilter) ?? null;
  }

  onSubcategoryFilterChange(): void {
    this.subcategoriesPage.reset();
  }

  pageSummary(page: InventoryTablePage, total: number): string {
    return inventoryPageSummary(page, total);
  }

  private upsertCategoryRow(row: ProductCategoryRow): void {
    if (!row.id) {
      return;
    }
    const index = this.categoryRows.findIndex((existing) => existing.id === row.id);
    if (index >= 0) {
      this.categoryRows = this.categoryRows.map((existing, i) => (i === index ? row : existing));
    } else {
      this.categoryRows = [...this.categoryRows, row];
    }
    this.products = this.inventoryService.enrichProductRows(this.products, this.categoryRows, this.subcategoryRows);
  }

  // ── Products CRUD ─────────────────────────────────────────────────────────

  openAddProduct(): void {
    const supplierId = this.orgContext.organizationId;
    if (supplierId == null) {
      this.notifications.error('Your session has no organisation id — sign in again or contact support.');
      return;
    }
    this.dialog
      .open(AddProductDialogComponent, {
        width: '580px',
        maxWidth: '95vw',
        disableClose: true,
        panelClass: 'lx-location-dialog-panel',
        data: { categories: this.categories, supplierId } satisfies AddProductDialogData,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((product: ProductRow | undefined) => {
        if (product) {
          this.notifications.success(`"${product.name}" was added to your catalogue.`);
          this.loadProducts();
        }
      });
  }

  openAddCategory(): void {
    this.dialog
      .open(CategoryDialogComponent, {
        width: '520px',
        maxWidth: '95vw',
        disableClose: true,
        panelClass: 'lx-location-dialog-panel',
        data: { mode: 'create' } satisfies CategoryDialogData,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((row: ProductCategoryRow | undefined) => {
        if (row) {
          this.notifications.success(`Category "${row.name}" was created.`);
          this.catalogError = '';
          this.upsertCategoryRow(row);
          this.categoriesPage.reset();
          this.setCatalogView('categories');
          this.loadCategoryCatalog(false);
        }
      });
  }

  openEditCategory(category: ProductCategoryRow): void {
    this.dialog
      .open(CategoryDialogComponent, {
        width: '520px',
        maxWidth: '95vw',
        disableClose: true,
        panelClass: 'lx-location-dialog-panel',
        data: { mode: 'edit', category } satisfies CategoryDialogData,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((row: ProductCategoryRow | undefined) => {
        if (row) {
          this.notifications.success(`Category "${row.name}" was updated.`);
          this.loadCategoryCatalog();
        }
      });
  }

  onDeleteCategory(category: ProductCategoryRow): void {
    this.confirmDestructiveAction('category')
      .pipe(
        switchMap(() => this.inventoryService.deleteCategory(category.id)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: () => {
          this.notifications.success(`Category "${category.name}" was deleted.`);
          this.loadCategoryCatalog();
        },
        error: (err: Error) => this.notifications.error(err.message ?? 'Could not delete category.'),
      });
  }

  openAddSubcategory(defaultCategoryId?: number): void {
    if (!this.categories.length) {
      this.notifications.error('Add a product category before creating subcategories.');
      return;
    }
    this.dialog
      .open(SubcategoryDialogComponent, {
        width: '520px',
        maxWidth: '95vw',
        disableClose: true,
        panelClass: 'lx-location-dialog-panel',
        data: {
          mode: 'create',
          categories: this.categories,
          defaultCategoryId: defaultCategoryId || this.subcategoryCategoryFilter || undefined,
        } satisfies SubcategoryDialogData,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((row: ProductSubCategoryRow | undefined) => {
        if (row) {
          this.notifications.success(`Subcategory "${row.name}" was created.`);
          this.loadCategoryCatalog();
        }
      });
  }

  openEditSubcategory(subcategory: ProductSubCategoryRow): void {
    this.dialog
      .open(SubcategoryDialogComponent, {
        width: '520px',
        maxWidth: '95vw',
        disableClose: true,
        panelClass: 'lx-location-dialog-panel',
        data: {
          mode: 'edit',
          categories: this.categories,
          subcategory,
        } satisfies SubcategoryDialogData,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((row: ProductSubCategoryRow | undefined) => {
        if (row) {
          this.notifications.success(`Subcategory "${row.name}" was updated.`);
          this.loadCategoryCatalog();
        }
      });
  }

  onDeleteSubcategory(subcategory: ProductSubCategoryRow): void {
    this.confirmDestructiveAction('subcategory')
      .pipe(
        switchMap(() => this.inventoryService.deleteSubCategory(subcategory.id)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: () => {
          this.notifications.success(`Subcategory "${subcategory.name}" was deleted.`);
          this.loadCategoryCatalog();
        },
        error: (err: Error) => this.notifications.error(err.message ?? 'Could not delete subcategory.'),
      });
  }

  openAddWarehouse(preselectedBranchId?: number): void {
    const supplierId = this.orgContext.organizationId;
    if (supplierId == null) {
      this.notifications.error('Your session has no organisation id — sign in again or contact support.');
      return;
    }
    const branchId =
      preselectedBranchId && preselectedBranchId > 0
        ? preselectedBranchId
        : this.warehouseBranchFilter > 0
          ? this.warehouseBranchFilter
          : undefined;
    this.dialog
      .open(AddWarehouseDialogComponent, {
        width: '620px',
        maxWidth: '95vw',
        disableClose: true,
        panelClass: 'lx-location-dialog-panel',
        data: {
          supplierId,
          preselectedBranchId: branchId,
          defaultWarehouseType: defaultWarehouseTypeForClassification(
            this.orgContext.organizationClassification,
          ),
        } satisfies AddWarehouseDialogData,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((warehouse: WarehouseRow | undefined) => {
        if (warehouse) {
          this.notifications.success(`"${warehouse.name}" was added as a warehouse location.`);
          this.loadWarehouses();
        }
        this.pendingOpenAddWarehouse = false;
      });
  }

  clearWarehouseBranchFilter(): void {
    this.warehouseBranchFilter = 0;
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { branchId: null, openAdd: null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  openBranchInOrganizationManagement(branchId: number): void {
    if (!branchId) {
      return;
    }
    const branch = this.branchOptions.find((b) => b.id === branchId);
    const segment = branch?.level === 'SUB_BRANCH' ? 'sub-branches' : 'branches';
    void this.router.navigate(['/organization', segment], {
      queryParams: { drillBranchId: branchId },
    });
  }

  triggerWarehouseImport(): void {
    this.warehouseImportInput?.nativeElement.click();
  }

  onWarehouseImportSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file || this.warehousesImporting) {
      return;
    }
    this.warehousesImporting = true;
    this.inventoryService
      .importWarehousesFromCsv(file)
      .pipe(
        finalize(() => {
          this.warehousesImporting = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (summary) => {
          if (summary.imported > 0) {
            this.notifications.success(summary.message || `Imported ${summary.imported} warehouse(s).`);
            this.loadWarehouses();
          } else {
            const detail = summary.errors[0] ?? summary.message ?? 'No rows were imported.';
            this.notifications.error(detail);
          }
        },
        error: (err: Error) => this.notifications.error(err.message ?? 'Warehouse import failed.'),
      });
  }

  exportWarehousesAs(format: LxExportFormat): void {
    if (this.warehousesExporting) {
      return;
    }
    this.warehousesExporting = true;
    this.inventoryService
      .exportWarehouses(format)
      .pipe(
        finalize(() => {
          this.warehousesExporting = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (blob) => {
          downloadBlob(blob, exportFilename('warehouse-locations', format));
          this.notifications.success(`Exported warehouses as ${format.toUpperCase()}.`);
        },
        error: (err: Error) => this.notifications.error(err.message ?? 'Export failed.'),
      });
  }

  triggerStockImport(): void {
    this.stockImportInput?.nativeElement.click();
  }

  onStockImportSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file || this.stockImporting) {
      return;
    }
    this.stockImporting = true;
    this.inventoryService
      .importStockFromCsv(file)
      .pipe(
        finalize(() => {
          this.stockImporting = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (summary) => {
          if (summary.imported > 0) {
            const partial =
              summary.failed > 0 ? ` ${summary.failed} row(s) failed.` : '';
            this.notifications.success(
              (summary.message || `Imported ${summary.imported} stock record(s).`) + partial,
            );
            this.loadStock();
          } else {
            const detail =
              summary.errors.slice(0, 3).join(' ') ||
              summary.message ||
              'No rows were imported.';
            this.notifications.error(detail);
          }
        },
        error: (err: Error) => this.notifications.error(err.message ?? 'Stock import failed.'),
      });
  }

  exportStockAs(format: LxExportFormat): void {
    if (this.stockExporting) {
      return;
    }
    this.stockExporting = true;
    this.inventoryService
      .exportStock(
        format,
        this.inventoryService.buildStockExportFilters({
          searchValue: this.stockSearch,
          stockStatus: this.stockStatusFilter,
          warehouseLocationId: this.stockWarehouseFilter,
          productId: this.stockProductFilter,
        }),
      )
      .pipe(
        finalize(() => {
          this.stockExporting = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (blob) => {
          downloadBlob(blob, exportFilename('stock-levels', format));
          this.notifications.success(`Exported stock levels as ${format.toUpperCase()}.`);
        },
        error: (err: Error) => this.notifications.error(err.message ?? 'Stock export failed.'),
      });
  }

  triggerTransferImport(): void {
    this.transferImportInput?.nativeElement.click();
  }

  onTransferImportSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file || this.transfersImporting) {
      return;
    }
    this.transfersImporting = true;
    this.inventoryService
      .importTransfersFromCsv(file)
      .pipe(
        finalize(() => {
          this.transfersImporting = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (summary) => {
          if (summary.imported > 0) {
            const partial = summary.failed > 0 ? ` ${summary.failed} row(s) failed.` : '';
            this.notifications.success(
              (summary.message || `Imported ${summary.imported} transfer(s).`) + partial,
            );
            this.loadTransfers();
          } else {
            const detail =
              summary.errors.slice(0, 3).join(' ') ||
              summary.message ||
              'No rows were imported.';
            this.notifications.error(detail);
          }
        },
        error: (err: Error) => this.notifications.error(err.message ?? 'Transfer import failed.'),
      });
  }

  exportTransfersAs(format: LxExportFormat): void {
    if (this.transfersExporting) {
      return;
    }
    this.transfersExporting = true;
    this.inventoryService
      .exportTransfers(
        format,
        this.inventoryService.buildTransferExportFilters({
          searchValue: this.transferSearch,
          status: this.transferStatusFilter,
          productId: this.transferProductFilter,
          fromLocationId: this.transferFromWarehouseFilter,
          toLocationId: this.transferToWarehouseFilter,
        }),
      )
      .pipe(
        finalize(() => {
          this.transfersExporting = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (blob) => {
          downloadBlob(blob, exportFilename('inventory-transfers', format));
          this.notifications.success(`Exported transfers as ${format.toUpperCase()}.`);
        },
        error: (err: Error) => this.notifications.error(err.message ?? 'Transfer export failed.'),
      });
  }

  downloadTransferSampleCsv(): void {
    const blob = exportRowsAsCsv(
      [
        {
          productId: this.products[0]?.id ?? 1,
          fromLocationId: this.warehouses[0]?.id ?? 1,
          toLocationId: this.warehouses[1]?.id ?? this.warehouses[0]?.id ?? 2,
          quantity: 10,
          reference: 'Bulk import sample',
          createdByUserId: this.userId || 1,
        },
      ],
      [
        { header: 'PRODUCT_ID', value: (r) => r.productId },
        { header: 'FROM_LOCATION_ID', value: (r) => r.fromLocationId },
        { header: 'TO_LOCATION_ID', value: (r) => r.toLocationId },
        { header: 'QUANTITY', value: (r) => r.quantity },
        { header: 'REFERENCE', value: (r) => r.reference },
        { header: 'CREATED_BY_USER_ID', value: (r) => r.createdByUserId },
      ],
    );
    downloadBlob(blob, 'inventory-transfers-sample.csv');
  }

  downloadStockSampleCsv(): void {
    const blob = exportRowsAsCsv(
      [
        {
          barcode: '',
          productCode: 'GAS-OXY-40L-001',
          warehouseLocationId: 1,
          supplierId: this.orgContext.organizationId ?? 0,
          currentStock: 50,
          minStockLevel: 10,
          reorderQuantity: 25,
          batchLot: '',
          serialNumber: '',
          expiresAt: '',
        },
      ],
      [
        { header: 'BARCODE', value: (r) => r.barcode },
        { header: 'PRODUCT_CODE', value: (r) => r.productCode },
        { header: 'WAREHOUSE_LOCATION_ID', value: (r) => r.warehouseLocationId },
        { header: 'SUPPLIER_ID', value: (r) => r.supplierId },
        { header: 'CURRENT_STOCK', value: (r) => r.currentStock },
        { header: 'MIN_STOCK_LEVEL', value: (r) => r.minStockLevel },
        { header: 'REORDER_QUANTITY', value: (r) => r.reorderQuantity },
        { header: 'BATCH_LOT', value: (r) => r.batchLot },
        { header: 'SERIAL_NUMBER', value: (r) => r.serialNumber },
        { header: 'EXPIRES_AT', value: (r) => r.expiresAt },
      ],
    );
    downloadBlob(blob, 'stock-levels-import-template.csv');
  }

  triggerCategoryImport(): void {
    this.categoryImportInput?.nativeElement.click();
  }

  onCategoryImportSelected(event: Event): void {
    this.handleCatalogImport(event, 'categories');
  }

  exportCategoriesAs(format: LxExportFormat): void {
    if (this.categoriesExporting) {
      return;
    }
    this.categoriesExporting = true;
    this.inventoryService
      .exportCategories(format, this.inventoryService.buildCategoryExportFilters(this.categorySearch))
      .pipe(
        finalize(() => {
          this.categoriesExporting = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (blob) => {
          downloadBlob(blob, exportFilename('product-categories', format));
          this.notifications.success(`Exported categories as ${format.toUpperCase()}.`);
        },
        error: (err: Error) => this.notifications.error(err.message ?? 'Category export failed.'),
      });
  }

  downloadCategorySampleCsv(): void {
    const blob = exportRowsAsCsv(
      [{ name: 'Beverages', description: 'Drinks and refreshments' }],
      [
        { header: 'NAME', value: (r) => r.name },
        { header: 'DESCRIPTION', value: (r) => r.description },
      ],
    );
    downloadBlob(blob, 'product-categories-import-template.csv');
  }

  triggerSubcategoryImport(): void {
    this.subcategoryImportInput?.nativeElement.click();
  }

  onSubcategoryImportSelected(event: Event): void {
    this.handleCatalogImport(event, 'subcategories');
  }

  exportSubcategoriesAs(format: LxExportFormat): void {
    if (this.subcategoriesExporting) {
      return;
    }
    const categoryId =
      this.selectedDrillCategory?.id
      ?? (this.subcategoryCategoryFilter > 0 ? this.subcategoryCategoryFilter : undefined);
    this.subcategoriesExporting = true;
    this.inventoryService
      .exportSubCategories(
        format,
        this.inventoryService.buildSubCategoryExportFilters(this.subcategorySearch, categoryId),
      )
      .pipe(
        finalize(() => {
          this.subcategoriesExporting = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (blob) => {
          downloadBlob(blob, exportFilename('product-subcategories', format));
          this.notifications.success(`Exported subcategories as ${format.toUpperCase()}.`);
        },
        error: (err: Error) => this.notifications.error(err.message ?? 'Subcategory export failed.'),
      });
  }

  downloadSubcategorySampleCsv(): void {
    const categoryId = this.categoryRows[0]?.id ?? 1;
    const blob = exportRowsAsCsv(
      [{ categoryId, name: 'Soft drinks', description: 'Carbonated and still drinks' }],
      [
        { header: 'CATEGORY_ID', value: (r) => r.categoryId },
        { header: 'NAME', value: (r) => r.name },
        { header: 'DESCRIPTION', value: (r) => r.description },
      ],
    );
    downloadBlob(blob, 'product-subcategories-import-template.csv');
  }

  triggerProductImport(): void {
    this.productImportInput?.nativeElement.click();
  }

  onProductImportSelected(event: Event): void {
    this.handleCatalogImport(event, 'products');
  }

  exportProductsAs(format: LxExportFormat): void {
    if (this.productsExporting) {
      return;
    }
    this.productsExporting = true;
    this.inventoryService
      .exportProducts(format, this.inventoryService.buildProductExportFilters(this.productSearch))
      .pipe(
        finalize(() => {
          this.productsExporting = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (blob) => {
          downloadBlob(blob, exportFilename('products', format));
          this.notifications.success(`Exported products as ${format.toUpperCase()}.`);
        },
        error: (err: Error) => this.notifications.error(err.message ?? 'Product export failed.'),
      });
  }

  downloadProductSampleCsv(): void {
    const supplierId = this.orgContext.organizationId ?? 0;
    const categoryId = this.categoryRows[0]?.id ?? 1;
    const subcategoryId = this.subcategoryRows[0]?.id ?? '';
    const blob = exportRowsAsCsv(
      [
        {
          name: 'Sample product',
          description: 'Example catalogue item',
          productCode: 'SKU-001',
          barcode: '5901234123457',
          price: 12.5,
          unitOfMeasure: 'EACH',
          productCategoryId: categoryId,
          productSubCategoryId: subcategoryId,
          supplierId,
          manufacturer: 'Acme Ltd',
          expiresAt: '',
        },
      ],
      [
        { header: 'NAME', value: (r) => r.name },
        { header: 'DESCRIPTION', value: (r) => r.description },
        { header: 'PRODUCT_CODE', value: (r) => r.productCode },
        { header: 'BARCODE', value: (r) => r.barcode },
        { header: 'PRICE', value: (r) => r.price },
        { header: 'UNIT_OF_MEASURE', value: (r) => r.unitOfMeasure },
        { header: 'PRODUCT_CATEGORY_ID', value: (r) => r.productCategoryId },
        { header: 'PRODUCT_SUB_CATEGORY_ID', value: (r) => r.productSubCategoryId },
        { header: 'SUPPLIER_ID', value: (r) => r.supplierId },
        { header: 'MANUFACTURER', value: (r) => r.manufacturer },
        { header: 'EXPIRES_AT', value: (r) => r.expiresAt },
      ],
    );
    downloadBlob(blob, 'products-import-template.csv');
  }

  private handleCatalogImport(
    event: Event,
    target: 'categories' | 'subcategories' | 'products',
  ): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) {
      return;
    }

    const importingKey =
      target === 'categories'
        ? 'categoriesImporting'
        : target === 'subcategories'
          ? 'subcategoriesImporting'
          : 'productsImporting';
    if (this[importingKey]) {
      return;
    }
    this[importingKey] = true;

    const request$ =
      target === 'categories'
        ? this.inventoryService.importCategoriesFromCsv(file)
        : target === 'subcategories'
          ? this.inventoryService.importSubCategoriesFromCsv(file)
          : this.inventoryService.importProductsFromCsv(file);

    const reload =
      target === 'products'
        ? () => this.loadProducts()
        : () => this.loadCategoryCatalog();

    request$
      .pipe(
        finalize(() => {
          this[importingKey] = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (summary) => {
          if (summary.imported > 0) {
            this.notifications.success(summary.message || `Imported ${summary.imported} row(s).`);
            reload();
          } else {
            const detail = summary.errors[0] ?? summary.message ?? 'No rows were imported.';
            this.notifications.error(detail);
          }
        },
        error: (err: Error) => this.notifications.error(err.message ?? 'Import failed.'),
      });
  }

  downloadWarehouseSampleCsv(): void {
    const supplierId = this.orgContext.organizationId ?? 0;
    const blob = exportRowsAsCsv(
      [
        {
          name: 'Main warehouse',
          description: 'Primary receiving warehouse',
          line1: '12 Warehouse Lane',
          line2: 'Bay 3',
          postalCode: '00263',
          suburbId: 1,
          geoCoordinatesId: '',
          supplierId,
          warehouseType: 'SUPPLIER',
        },
      ],
      [
        { header: 'NAME', value: (r) => r.name },
        { header: 'DESCRIPTION', value: (r) => r.description },
        { header: 'LINE1', value: (r) => r.line1 },
        { header: 'LINE2', value: (r) => r.line2 },
        { header: 'POSTAL_CODE', value: (r) => r.postalCode },
        { header: 'SUBURB_ID', value: (r) => r.suburbId },
        { header: 'GEO_COORDINATES_ID', value: (r) => r.geoCoordinatesId },
        { header: 'SUPPLIER_ID', value: (r) => r.supplierId },
        { header: 'WAREHOUSE_TYPE', value: (r) => r.warehouseType },
      ],
    );
    downloadBlob(blob, 'warehouse-locations-import-template.csv');
  }

  warehouseTypeLabel(type: string): string {
    const normalized = type.trim().toUpperCase();
    return normalized === 'CUSTOMER' ? 'Customer' : 'Supplier';
  }

  // ── Row actions ───────────────────────────────────────────────────────────

  private confirmDestructiveAction(entityLabel: string): Observable<boolean> {
    return this.dialog
      .open(DeleteConfirmDialogComponent, {
        width: '420px',
        maxWidth: '92vw',
        data: { entityLabel },
      })
      .afterClosed()
      .pipe(map((confirmed) => confirmed === true), filter(Boolean));
  }

  private openDetail(data: InventoryDetailDialogData): void {
    this.dialog.open(InventoryDetailDialogComponent, {
      width: data.width ?? '520px',
      maxWidth: '95vw',
      panelClass: 'lx-location-dialog-panel',
      data,
    });
  }

  viewCategory(category: ProductCategoryRow): void {
    this.openDetail({
      title: category.name,
      subtitle: 'Product category',
      fields: [
        { label: 'Description', value: category.description || '—' },
        { label: 'Status', value: this.entityStatusText(category.entityStatus) },
        { label: 'Created', value: category.createdAtLabel },
      ],
    });
  }

  viewSubcategory(subcategory: ProductSubCategoryRow): void {
    this.openDetail({
      title: subcategory.name,
      subtitle: 'Product subcategory',
      fields: [
        { label: 'Category', value: subcategory.categoryName || '—' },
        { label: 'Description', value: subcategory.description || '—' },
        { label: 'Status', value: this.entityStatusText(subcategory.entityStatus) },
        { label: 'Created', value: subcategory.createdAtLabel },
      ],
    });
  }

  viewProduct(product: ProductRow): void {
    this.inventoryService
      .getProduct(product.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (row) => {
          const detail = this.inventoryService.enrichProductRows([row], this.categoryRows, this.subcategoryRows)[0];
          this.openDetail({
            title: detail.name,
            subtitle: detail.code || 'Product',
            width: '720px',
            fields: this.productDetailFields(detail),
          });
        },
        error: (err: Error) =>
          this.notifications.error(err.message ?? 'Could not load product details.'),
      });
  }

  private productDetailFields(product: ProductRow): InventoryDetailField[] {
    const code = product.code || '';
    const codeDisplay =
      code && code !== code.trim()
        ? `"${code}" (${code.length} characters — includes leading/trailing spaces)`
        : code || '—';

    return [
      { label: 'Name', value: product.name || '—' },
      { label: 'Organisation', value: this.productOrganisationLabel(product) },
      { label: 'Product code', value: codeDisplay },
      { label: 'Barcode', value: product.barcode || '—' },
      { label: 'Description', value: product.description || '—' },
      { label: 'Category', value: product.categoryName || '—' },
      { label: 'Subcategory', value: product.subcategoryName || '—' },
      { label: 'Price', value: product.unitPriceLabel },
      { label: 'Unit of measure', value: product.unitOfMeasureLabel || product.unitOfMeasure || '—' },
      { label: 'Manufacturer', value: product.manufacturer || '—' },
      { label: 'Expires', value: product.expiresAtLabel || '—' },
      { label: 'Status', value: this.entityStatusText(product.entityStatus) },
      { label: 'Created', value: product.createdAtLabel },
      { label: 'Updated', value: product.updatedAtLabel || '—' },
    ];
  }

  private productOrganisationLabel(product: ProductRow): string {
    const sessionOrgId = this.orgContext.organizationId;
    if (sessionOrgId != null && product.supplierId > 0 && product.supplierId === sessionOrgId) {
      return this.orgContext.organizationName || this.orgName || '—';
    }
    return this.orgName || '—';
  }

  openEditProduct(product: ProductRow): void {
    const supplierId = product.supplierId || this.orgContext.organizationId;
    if (supplierId == null) {
      this.notifications.error('Your session has no organisation id — sign in again or contact support.');
      return;
    }
    this.dialog
      .open(AddProductDialogComponent, {
        width: '620px',
        maxWidth: '95vw',
        disableClose: true,
        panelClass: 'lx-location-dialog-panel',
        data: {
          mode: 'edit',
          product,
          categories: this.categories,
          supplierId,
        } satisfies AddProductDialogData,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((row: ProductRow | undefined) => {
        if (row) {
          this.notifications.success(`"${row.name}" was updated.`);
          this.loadProducts();
        }
      });
  }

  onDeleteProduct(product: ProductRow): void {
    this.confirmDestructiveAction('product')
      .pipe(
        switchMap(() => this.inventoryService.deleteProduct(product.id)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: () => {
          this.notifications.success(`Product "${product.name}" was deleted.`);
          this.loadProducts();
        },
        error: (err: Error) => this.notifications.error(err.message ?? 'Could not delete product.'),
      });
  }

  viewWarehouse(warehouse: WarehouseRow): void {
    this.openDetail({
      title: warehouse.name,
      subtitle: 'Warehouse location',
      fields: [
        { label: 'Description', value: warehouse.description || '—' },
        { label: 'Branch', value: warehouse.branchLabel || '—' },
        { label: 'Type', value: this.warehouseTypeLabel(warehouse.warehouseType) },
        { label: 'Access', value: this.warehouseAccessLabel(warehouse) },
        { label: 'Address', value: warehouse.addressLabel || '—' },
        { label: 'Status', value: this.entityStatusText(warehouse.entityStatus) },
        { label: 'Created', value: warehouse.createdAtLabel },
      ],
    });
  }

  canManageWarehouseSharing(warehouse: WarehouseRow): boolean {
    return warehouse.organizationOwned !== false && !warehouse.sharedAccess;
  }

  warehouseAccessLabel(warehouse: WarehouseRow): string {
    if (warehouse.sharedAccess) {
      return warehouse.callerAccessLevel === 'FULFILL' ? 'Shared — fulfill' : 'Shared — read only';
    }
    return 'Owned by your organisation';
  }

  openWarehouseSharing(warehouse: WarehouseRow): void {
    if (!this.canManageWarehouseSharing(warehouse)) {
      return;
    }
    this.dialog
      .open(WarehouseSharingDialogComponent, {
        width: '560px',
        maxWidth: '95vw',
        disableClose: true,
        data: { warehouse } satisfies WarehouseSharingDialogData,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((changed) => {
        if (changed) {
          this.loadWarehouses(false);
        }
      });
  }

  openEditWarehouse(warehouse: WarehouseRow): void {
    const supplierId = warehouse.supplierId || this.orgContext.organizationId;
    if (supplierId == null) {
      this.notifications.error('Your session has no organisation id — sign in again or contact support.');
      return;
    }
    this.dialog
      .open(AddWarehouseDialogComponent, {
        width: '620px',
        maxWidth: '95vw',
        disableClose: true,
        panelClass: 'lx-location-dialog-panel',
        data: { mode: 'edit', warehouse, supplierId } satisfies AddWarehouseDialogData,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((row: WarehouseRow | undefined) => {
        if (row) {
          this.notifications.success(`"${row.name}" was updated.`);
          this.loadWarehouses();
        }
      });
  }

  onDeleteWarehouse(warehouse: WarehouseRow): void {
    this.confirmDestructiveAction('warehouse')
      .pipe(
        switchMap(() => this.inventoryService.deleteWarehouse(warehouse.id)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: () => {
          this.notifications.success(`Warehouse "${warehouse.name}" was deleted.`);
          this.loadWarehouses();
          this.loadStock();
        },
        error: (err: Error) => this.notifications.error(err.message ?? 'Could not delete warehouse.'),
      });
  }

  viewStockItem(stock: StockRow): void {
    this.openDetail({
      title: stock.productName,
      subtitle: stock.warehouseName,
      fields: [
        { label: 'Product code', value: stock.productCode || '—' },
        { label: 'Barcode', value: stock.productBarcode || '—' },
        { label: 'On hand', value: `${stock.quantityOnHand} ${stock.unitOfMeasure || ''}`.trim() },
        { label: 'Reserved', value: `${stock.reservedQuantity} ${stock.unitOfMeasure || ''}`.trim() },
        { label: 'Available', value: `${stock.availableQuantity} ${stock.unitOfMeasure || ''}`.trim() },
        { label: 'Reorder point', value: stock.reorderPoint > 0 ? String(stock.reorderPoint) : '—' },
        { label: 'Status', value: stock.statusLabel },
      ],
    });
  }

  onDeleteStockItem(stock: StockRow): void {
    this.confirmDestructiveAction('stock record')
      .pipe(
        switchMap(() => this.inventoryService.deleteStockItem(stock.id)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: () => {
          this.notifications.success('Stock record deleted.');
          this.loadStock();
        },
        error: (err: Error) => this.notifications.error(err.message ?? 'Could not delete stock record.'),
      });
  }

  get hasTransferFilters(): boolean {
    return (
      !!this.transferStatusFilter ||
      this.transferProductFilter > 0 ||
      this.transferFromWarehouseFilter > 0 ||
      this.transferToWarehouseFilter > 0
    );
  }

  viewTransfer(transfer: TransferRow): void {
    const dialogRef = this.dialog.open(ViewTransferDialogComponent, {
      width: '840px',
      maxWidth: '95vw',
      panelClass: 'lx-location-dialog-panel',
      data: {
        transfer,
        fields: this.transferDetailFields(transfer),
        canApprove: this.canApproveTransfer(transfer),
        canReject: this.canRejectTransfer(transfer),
        canEdit: transfer.status === 'REQUESTED',
      },
    });

    dialogRef
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((result: ViewTransferDialogResult | undefined) => {
        if (!result) {
          return;
        }
        if (result.action === 'edit') {
          this.openEditTransfer(transfer);
          return;
        }
        if (result.action === 'approved') {
          this.onApproveTransfer(transfer);
          return;
        }
        this.onRejectTransfer(transfer, result.reason);
      });
  }

  openEditTransfer(transfer: TransferRow): void {
    const userId = Number(this.authState.currentUser?.userId ?? 0);
    if (!userId) {
      this.notifications.error('Sign in again to edit transfers.');
      return;
    }
    this.dialog
      .open(EditTransferDialogComponent, {
        width: '720px',
        maxWidth: '95vw',
        panelClass: 'lx-location-dialog-panel',
        data: {
          transfer,
          products: this.products,
          warehouses: this.warehouses,
          stock: this.stock,
          updatedByUserId: userId,
        } satisfies EditTransferDialogData,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((updated: TransferRow | undefined) => {
        if (!updated) {
          return;
        }
        this.transfers = this.inventoryService.enrichTransferRows(
          this.transfers.map((row) => (row.id === updated.id ? updated : row)),
          this.products,
          this.warehouses,
        );
        this.notifications.success('Transfer updated.');
        this.cdr.markForCheck();
      });
  }

  private transferDetailFields(transfer: TransferRow): InventoryDetailField[] {
    const qtyLabel = transfer.unitOfMeasure
      ? `${transfer.quantity} ${transfer.unitOfMeasure}`
      : String(transfer.quantity);
    return [
      { label: 'Reference', value: transfer.transferNumber || '—' },
      { label: 'Product', value: transfer.productName || '—' },
      { label: 'Product code', value: transfer.productCode || '—' },
      { label: 'Quantity', value: qtyLabel },
      { label: 'Unit cost', value: transfer.unitCostLabel },
      { label: 'From warehouse', value: transfer.fromWarehouse || '—' },
      { label: 'To warehouse', value: transfer.toWarehouse || '—' },
      { label: 'Status', value: transfer.statusLabel },
      { label: 'Notes / reference', value: transfer.reference || '—' },
      { label: 'Requested by', value: transfer.requestedBy || '—' },
      { label: 'Created', value: transfer.createdAtLabel },
      { label: 'Last updated', value: transfer.updatedAtLabel || '—' },
      { label: 'Rejection reason', value: transfer.rejectionReason || '—' },
      { label: 'Rejected at', value: transfer.rejectedAtLabel || '—' },
    ];
  }

  private hasTransferPermission(
    permission:
      | 'APPROVE_INVENTORY_TRANSFER'
      | 'REJECT_INVENTORY_TRANSFER'
      | 'START_TRANSIT_INVENTORY_TRANSFER'
      | 'COMPLETE_INVENTORY_TRANSFER'
      | 'CANCEL_INVENTORY_TRANSFER',
  ): boolean {
    if (this.isOrgAdministrator) {
      return true;
    }
    const roles = this.authState.currentUser?.roles ?? [];
    return roles.includes(permission);
  }

  private canParticipateInTransferWorkflow(): boolean {
    return this.isOrgAdministrator || this.isSupplier || this.isCustomer;
  }

  canApproveTransfer(transfer: TransferRow): boolean {
    return (
      transfer.status === 'REQUESTED' &&
      this.isProcurementApprover &&
      this.canParticipateInTransferWorkflow() &&
      this.hasTransferPermission('APPROVE_INVENTORY_TRANSFER')
    );
  }

  canRejectTransfer(transfer: TransferRow): boolean {
    if (transfer.status !== 'REQUESTED' || !this.isProcurementApprover || !this.canParticipateInTransferWorkflow()) {
      return false;
    }
    return (
      this.hasTransferPermission('REJECT_INVENTORY_TRANSFER') ||
      this.hasTransferPermission('APPROVE_INVENTORY_TRANSFER')
    );
  }

  canStartTransferTransit(transfer: TransferRow): boolean {
    return (
      transfer.status === 'APPROVED' &&
      this.canParticipateInTransferWorkflow() &&
      this.hasTransferPermission('START_TRANSIT_INVENTORY_TRANSFER')
    );
  }

  canCompleteTransfer(_transfer: TransferRow): boolean {
    return false;
  }

  canCancelTransfer(transfer: TransferRow): boolean {
    return (
      (transfer.status === 'REQUESTED' ||
        transfer.status === 'APPROVED' ||
        transfer.status === 'IN_TRANSIT') &&
      this.canParticipateInTransferWorkflow() &&
      this.hasTransferPermission('CANCEL_INVENTORY_TRANSFER')
    );
  }

  onCancelTransfer(transfer: TransferRow): void {
    this.confirmDestructiveAction('transfer')
      .pipe(
        switchMap(() => this.inventoryService.cancelTransfer(transfer.id)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: () => {
          this.notifications.success(`Transfer ${transfer.transferNumber} was cancelled.`);
          this.loadTransfers();
        },
        error: (err: Error) => this.notifications.error(err.message ?? 'Could not cancel transfer.'),
      });
  }

  viewRequisition(requisition: PurchaseRequisitionRow): void {
    this.openDetail({
      title: requisition.requisitionNumber,
      subtitle: 'Purchase requisition',
      fields: [
        { label: 'Purpose', value: requisition.purpose || '—' },
        { label: 'Priority', value: requisition.priorityLabel },
        { label: 'Lines', value: String(requisition.lineCount) },
        { label: 'Est. total', value: requisition.totalAmountLabel },
        { label: 'Required by', value: requisition.requiredByDateLabel },
        { label: 'Status', value: requisition.statusLabel },
        { label: 'Submitted', value: requisition.submittedAtLabel },
        { label: 'Created', value: requisition.createdAtLabel },
      ],
    });
  }

  viewPurchaseOrderDetails(order: PurchaseOrderRow): void {
    this.openDetail({
      title: order.orderNumber,
      subtitle: 'Purchase order',
      fields: [
        { label: 'Customer', value: order.customerName || '—' },
        { label: 'Supplier', value: order.supplierName || '—' },
        { label: 'Status', value: order.statusLabel },
        { label: 'Total', value: order.totalAmountLabel },
        { label: 'Expected delivery', value: order.expectedDeliveryLabel },
        { label: 'Created', value: order.createdAtLabel },
      ],
    });
  }

  onDeletePurchaseOrder(order: PurchaseOrderRow): void {
    this.confirmDestructiveAction('purchase order')
      .pipe(
        switchMap(() => this.inventoryService.deletePurchaseOrder(order.id)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: () => {
          this.notifications.success(`Purchase order ${order.orderNumber} was deleted.`);
          this.loadPurchaseOrders();
        },
        error: (err: Error) => this.notifications.error(err.message ?? 'Could not delete purchase order.'),
      });
  }

  viewProductStock(product: ProductRow): void {
    this.stockDrillOrigin = 'product';
    this.stockProductFilter = product.id;
    this.stockSearch = '';
    this.stockWarehouseFilter = 0;
    this.stockStatusFilter = '';
    this.stockPage.reset();
    this.productStockWarehousesPage.reset();
    this.warehouseStockProductsPage.reset();
    this.setTab('stock', { keepStockFilters: true });
  }

  viewProductStockAtWarehouse(warehouseId: number): void {
    this.stockWarehouseFilter = warehouseId;
    this.stockPage.reset();
  }

  viewWarehouseStock(warehouse: WarehouseRow): void {
    this.stockDrillOrigin = 'warehouse';
    this.stockWarehouseFilter = warehouse.id;
    this.stockSearch = '';
    this.stockProductFilter = 0;
    this.stockStatusFilter = '';
    this.stockPage.reset();
    this.productStockWarehousesPage.reset();
    this.warehouseStockProductsPage.reset();
    this.setTab('stock', { keepStockFilters: true });
  }

  viewWarehouseStockAtProduct(productId: number): void {
    this.stockProductFilter = productId;
    this.stockPage.reset();
  }

  clearActionMenus(): void {
    this.actionProduct = null;
    this.actionCategory = null;
    this.actionSubCategory = null;
    this.actionWarehouse = null;
    this.actionStock = null;
    this.actionTransfer = null;
    this.actionRequisition = null;
    this.actionPurchaseOrder = null;
  }

  // ── Transfers ─────────────────────────────────────────────────────────────

  openCreateTransfer(prefill?: CreateTransferDialogData['prefill']): void {
    if (!this.userId) {
      this.notifications.error('Your user profile could not be loaded. Sign in again.');
      return;
    }
    if (!this.products.length || this.warehouses.length < 2) {
      this.notifications.error('Add at least one product and two warehouses before creating a transfer.');
      return;
    }
    this.dialog
      .open(CreateTransferDialogComponent, {
        width: '620px',
        maxWidth: '95vw',
        disableClose: true,
        panelClass: 'lx-location-dialog-panel',
        data: {
          products: this.products,
          warehouses: this.warehouses,
          stock: this.stock,
          createdByUserId: this.userId,
          prefill,
        } satisfies CreateTransferDialogData,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((transfer: TransferRow | undefined) => {
        if (transfer) {
          this.notifications.success(`Transfer ${transfer.transferNumber} created.`);
          this.loadTransfers();
        }
      });
  }

  openInitialStock(prefill?: InitialStockDialogData['prefill']): void {
    const supplierId = this.orgContext.organizationId;
    if (supplierId == null || !this.userId) {
      this.notifications.error('Your session is missing organisation or user context.');
      return;
    }
    if (!this.products.length || !this.warehouses.length) {
      this.notifications.error('Add products and warehouses before recording initial stock.');
      return;
    }
    if (!this.hasInitialStockTargets) {
      this.notifications.show('Initial stock is already recorded. Use replenishment from a stock row.');
      return;
    }
    if (
      prefill?.productId != null &&
      prefill.warehouseLocationId != null &&
      this.hasActiveStockAt(prefill.productId, prefill.warehouseLocationId)
    ) {
      const row = this.stock.find(
        (item) =>
          item.productId === prefill.productId && item.warehouseLocationId === prefill.warehouseLocationId,
      );
      if (row) {
        this.openReplenishStock(row);
      }
      return;
    }
    this.dialog
      .open(InitialStockDialogComponent, {
        width: '620px',
        maxWidth: '95vw',
        disableClose: true,
        panelClass: 'lx-location-dialog-panel',
        data: {
          products: this.products,
          warehouses: this.warehouses,
          existingStock: this.stock,
          supplierId,
          createdByUserId: this.userId,
          prefill,
        } satisfies InitialStockDialogData,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((stock: StockRow | undefined) => {
        if (stock) {
          this.notifications.success('Initial stock recorded.');
          this.loadStock();
        }
      });
  }

  openReplenishStock(stock: StockRow): void {
    if (!this.userId) {
      this.notifications.error('Your user profile could not be loaded.');
      return;
    }
    this.dialog
      .open(ReplenishStockDialogComponent, {
        width: '620px',
        maxWidth: '95vw',
        disableClose: true,
        panelClass: 'lx-location-dialog-panel',
        data: {
          stock,
          adjustedByUserId: this.userId,
        },
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((replenished: boolean | undefined) => {
        if (replenished) {
          this.notifications.success('Stock replenished.');
          this.loadStock();
        }
      });
  }

  /** True when at least one product × warehouse pair has no opening balance yet. */
  get hasInitialStockTargets(): boolean {
    if (!this.products.length || !this.warehouses.length) {
      return false;
    }
    return this.products.some((product) =>
      this.warehouses.some((warehouse) => !this.hasActiveStockAt(product.id, warehouse.id)),
    );
  }

  hasActiveStockAt(productId: number, warehouseLocationId: number): boolean {
    const row = this.stock.find(
      (item) => item.productId === productId && item.warehouseLocationId === warehouseLocationId,
    );
    return !!row && row.quantityOnHand > 0;
  }

  canReplenishStock(row: StockRow | null | undefined): boolean {
    return !!row && row.quantityOnHand > 0;
  }

  canRecordInitialStockForRow(row: StockRow | null | undefined): boolean {
    return !!row && row.quantityOnHand <= 0;
  }

  onApproveTransfer(row: TransferRow): void {
    if (!this.userId) {
      this.notifications.error('Your user profile could not be loaded.');
      return;
    }
    this.inventoryService
      .approveTransfer(row.id, this.userId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.notifications.success(`Transfer ${row.transferNumber} approved.`);
          this.loadTransfers();
        },
        error: (err: Error) => {
          this.notifications.error(err.message ?? 'Could not approve transfer.');
        },
      });
  }

  onRejectTransfer(row: TransferRow, rejectionReason: string): void {
    if (!this.userId) {
      this.notifications.error('Your user profile could not be loaded.');
      return;
    }
    this.inventoryService
      .rejectTransfer(row.id, this.userId, rejectionReason)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.notifications.success(`Transfer ${row.transferNumber} was rejected.`);
          this.loadTransfers();
        },
        error: (err: Error) => {
          this.notifications.error(err.message ?? 'Could not reject transfer.');
        },
      });
  }

  onStartTransferTransit(row: TransferRow): void {
    void this.router.navigate(['/shipments/shipments'], {
      queryParams: { transferId: row.id, assign: '1' },
    });
  }

  onCompleteTransfer(row: TransferRow): void {
    if (!this.userId) {
      this.notifications.error('Your user profile could not be loaded.');
      return;
    }
    this.inventoryService
      .completeTransfer(row.id, this.userId, crypto.randomUUID())
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.notifications.success(`Transfer ${row.transferNumber} marked as completed.`);
          this.loadTransfers();
        },
        error: (err: Error) => {
          this.notifications.error(err.message ?? 'Could not complete transfer.');
        },
      });
  }

  onRequisitionFiltersChanged(): void {
    this.requisitionsPage.reset();
  }

  onQuotationFiltersChanged(): void {
    this.quotationsPage.reset();
  }

  onRaiseQuotation(): void {
    if (!this.quoteableRequisitions.length) {
      this.notifications.success(
        'No customer requisitions are awaiting your quote. Published requisitions appear on the Requisitions tab.',
      );
    }
  }

  triggerQuotationImport(): void {
    this.quotationImportInput?.nativeElement.click();
  }

  downloadQuotationSampleCsv(): void {
    const blob = exportRowsAsCsv(
      [
        {
          purchaseRequisitionId: 1,
          currency: 'USD',
          paymentTerm: 'NET_30',
          deliveryTerms: 'Ex-works supplier warehouse',
          validityUntil: '2026-12-31',
          taxAmount: 0,
          notes: 'Optional commercial notes',
          purchaseRequisitionLineId: 10,
          productId: 100,
          quotedQuantity: 5,
          unitPrice: 12.5,
        },
      ],
      [
        { header: 'PURCHASE_REQUISITION_ID', value: (r) => r.purchaseRequisitionId },
        { header: 'CURRENCY', value: (r) => r.currency },
        { header: 'PAYMENT_TERM', value: (r) => r.paymentTerm },
        { header: 'DELIVERY_TERMS', value: (r) => r.deliveryTerms },
        { header: 'VALIDITY_UNTIL', value: (r) => r.validityUntil },
        { header: 'TAX_AMOUNT', value: (r) => r.taxAmount },
        { header: 'NOTES', value: (r) => r.notes },
        { header: 'PURCHASE_REQUISITION_LINE_ID', value: (r) => r.purchaseRequisitionLineId },
        { header: 'PRODUCT_ID', value: (r) => r.productId },
        { header: 'QUOTED_QUANTITY', value: (r) => r.quotedQuantity },
        { header: 'UNIT_PRICE', value: (r) => r.unitPrice },
      ],
    );
    downloadBlob(blob, 'supplier-quotations-import-template.csv');
  }

  onQuotationImportSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file || this.quotationsImporting) {
      return;
    }
    const supplierOrganizationId = this.orgContext.organizationId;
    if (supplierOrganizationId == null || !this.userId) {
      this.notifications.error('Your organisation or user profile could not be loaded.');
      return;
    }

    this.quotationsImporting = true;
    file
      .text()
      .then((text) => this.importQuotationsFromCsv(text, supplierOrganizationId, this.userId))
      .then((imported) => {
        if (imported > 0) {
          this.notifications.success(`Imported ${imported} quotation(s).`);
          this.loadPendingRequisitions(false);
          this.loadSupplierQuotations(false);
        } else {
          this.notifications.error('No quotations were imported. Check the CSV template and requisition status.');
        }
      })
      .catch((err: Error) => this.notifications.error(err.message ?? 'Quotation import failed.'))
      .finally(() => {
        this.quotationsImporting = false;
        this.cdr.detectChanges();
      });
  }

  exportQuotationsAs(format: LxExportFormat): void {
    if (this.quotationsExporting) {
      return;
    }
    this.quotationsExporting = true;
    const rows = this.filteredSupplierQuotations;
    const columns: LxExportColumn<SupplierQuoteRow>[] = [
      { header: 'Quote no.', value: (r) => r.quoteNumber },
      { header: 'Requisition', value: (r) => r.requisitionNumber },
      { header: 'Status', value: (r) => r.statusLabel },
      { header: 'Source', value: (r) => r.quoteSourceLabel },
      { header: 'Currency', value: (r) => r.currency },
      { header: 'Total', value: (r) => r.totalAmount },
      { header: 'Valid until', value: (r) => r.validityUntilLabel },
      { header: 'Submitted', value: (r) => r.submittedAtLabel },
      { header: 'Lines', value: (r) => r.lineCount },
    ];
    const saved = exportClientTableAsCsv(format, rows, columns, 'supplier-quotations', (message) =>
      this.notifications.success(message),
    );
    if (saved) {
      this.notifications.success(`Exported quotations as ${exportFormatLabel(format)}.`);
    }
    this.quotationsExporting = false;
    this.cdr.detectChanges();
  }

  exportRequisitionsAs(format: LxExportFormat): void {
    if (this.requisitionsExporting) {
      return;
    }
    this.requisitionsExporting = true;
    const rows = this.filteredRequisitions;
    const columns: LxExportColumn<PurchaseRequisitionRow>[] = [
      { header: 'Reference', value: (r) => r.requisitionNumber },
      { header: 'Purpose', value: (r) => r.purpose },
      { header: 'Priority', value: (r) => r.priorityLabel },
      { header: 'Lines', value: (r) => r.lineCount },
      { header: 'Est. total', value: (r) => r.totalAmountLabel },
      { header: 'Required by', value: (r) => r.requiredByDateLabel },
      { header: 'Status', value: (r) => r.statusLabel },
    ];
    const saved = exportClientTableAsCsv(format, rows, columns, 'customer-requisitions', (message) =>
      this.notifications.success(message),
    );
    if (saved) {
      this.notifications.success(`Exported requisitions as ${exportFormatLabel(format)}.`);
    }
    this.requisitionsExporting = false;
    this.cdr.detectChanges();
  }

  // ── Requisition actions (supplier) ───────────────────────────────────────

  /**
   * Supplier submits a quote for a PUBLISHED_TO_SUPPLIER requisition.
   */
  onSubmitQuote(row: PurchaseRequisitionRow): void {
    if (!this.userId) {
      this.notifications.error('Your user profile could not be loaded.');
      return;
    }
    this.dialog
      .open(SubmitSupplierQuoteDialogComponent, {
        width: '760px',
        maxWidth: '95vw',
        data: { requisition: row, submittedByUserId: this.userId },
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((saved) => {
        if (saved) {
          this.loadPendingRequisitions();
          this.loadSupplierQuotations(false);
        }
      });
  }

  openViewQuote(requisitionId: number, requisitionNumber: string): void {
    const data: ViewSupplierQuoteDialogData = { requisitionId, requisitionNumber };
    this.dialog.open(ViewSupplierQuoteDialogComponent, {
      width: '760px',
      maxWidth: '95vw',
      maxHeight: '92vh',
      data,
      panelClass: 'lx-location-dialog-panel',
    });
  }

  trackByQuoteId(_i: number, row: SupplierQuoteRow): number {
    return row.id;
  }

  trackByRequisitionId(_i: number, row: PurchaseRequisitionRow): number {
    return row.id;
  }

  // ── CSS helpers ───────────────────────────────────────────────────────────

  readonly entityStatusClass = entityStatusCssClass;
  readonly entityStatusText = entityStatusLabel;
  readonly transferStatusClass = transferStatusCssClass;

  transferRouteLabel(row: TransferRow): string {
    return transferRouteSummary(row.routeStops);
  }
  readonly requisitionStatusClass = requisitionStatusCssClass;
  readonly purchaseOrderStatusClass = purchaseOrderStatusCssClass;
  readonly salesOrderStatusClass = salesOrderStatusCssClass;
  readonly stockStatusClass = stockLevelStatusCssClass;

  avatarStyle(row: { accentHue: number }): Record<string, string> {
    const hue = row.accentHue;
    return {
      background: `linear-gradient(135deg, hsl(${hue} 65% 36%), hsl(${(hue + 35) % 360} 60% 50%))`,
    };
  }

  trackByProductId(_i: number, row: ProductRow): number {
    return row.id;
  }

  trackByCategoryId(_i: number, row: ProductCategoryRow): number {
    return row.id;
  }

  trackBySubCategoryId(_i: number, row: ProductSubCategoryRow): number {
    return row.id;
  }

  trackByWarehouseId(_i: number, row: WarehouseRow): number {
    return row.id;
  }

  trackByStockId(_i: number, row: StockRow): number {
    return row.id;
  }

  trackByTransferId(_i: number, row: TransferRow): number {
    return row.id;
  }

  // ── Private loaders ───────────────────────────────────────────────────────

  private syncTabFromRoute(): void {
    const tabParam = this.route.snapshot.paramMap.get('tab');
    const dataTab = this.route.snapshot.data['tab'];
    const candidate = (tabParam ?? dataTab) as InventoryWorkspaceTab | null;
    if (candidate && this.isValidTab(candidate)) {
      this.applyRouteTab(candidate);
      return;
    }
    if (this.inventoryBasePath === '/my-orders') {
      void this.router.navigate(['/my-orders', 'warehouses'], { replaceUrl: true });
    }
  }

  private applyRouteTab(tab: InventoryWorkspaceTab): void {
    if (tab === 'stock' && !this.preserveStockDrillOnRoute) {
      this.clearStockDrillDown();
    }
    this.preserveStockDrillOnRoute = false;
    this.activeTab = tab;
    this.ensureTabDataLoaded(tab);
    this.cdr.markForCheck();
  }

  private loadWorkspace(): void {
    this.fetching = true;
    this.loadError = '';
    this.loadProducts(false);
    this.loadWarehouses(false);
    this.loadCategoryCatalog(false);
    this.inventoryService
      .listStock()
      .pipe(
        finalize(() => {
          this.fetching = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.stock = this.inventoryService.enrichStockRows(rows, this.products, this.warehouses);
          this.reconcileOrganizationProducts();
        },
        error: (err: Error) => {
          this.stockError = err.message ?? 'Could not load stock.';
        },
      });
    this.loadTransfers(false);
    this.loadPendingRequisitions(false);
  }

  private loadProducts(showLoading = true): void {
    if (showLoading) {
      this.productsLoading = true;
      this.productsError = '';
    }
    this.inventoryService
      .listProducts()
      .pipe(
        finalize(() => {
          this.productsLoading = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.rawProducts = rows;
          this.reconcileOrganizationProducts();
        },
        error: (err: Error) => {
          this.productsError = err.message ?? 'Could not load products.';
          if (!this.loadError) {
            this.loadError = this.productsError;
          }
        },
      });
  }

  /** Only products owned by the signed-in organisation (product.supplierId). */
  private applyOrganizationProductScope(rows: ProductRow[]): ProductRow[] {
    return filterByOrganizationScope(
      rows,
      this.orgContext.organizationId,
      this.orgContext.organizationClassification,
    );
  }

  private isCustomerInventoryContext(): boolean {
    return this.isCustomer || this.inventoryBasePath === '/my-orders';
  }

  /** Template helper — customer inventory workspace on /my-orders. */
  get isCustomerInventoryWorkspace(): boolean {
    return this.isCustomerInventoryContext();
  }

  private reconcileOrganizationProducts(): void {
    const scoped = this.applyOrganizationProductScope(this.rawProducts);
    this.products = this.inventoryService.enrichProductRows(scoped, this.categoryRows, this.subcategoryRows);
    this.stock = this.inventoryService.enrichStockRows(this.stock, this.products, this.warehouses);
    this.cdr.markForCheck();
  }

  private loadWarehouses(showLoading = true): void {
    if (showLoading) {
      this.warehousesLoading = true;
      this.warehousesError = '';
    }
    this.inventoryService
      .listWarehouses()
      .pipe(
        finalize(() => {
          this.warehousesLoading = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.resolveWarehouseAddresses(rows);
        },
        error: (err: Error) => {
          this.warehousesError = err.message ?? 'Could not load warehouses.';
        },
      });
  }

  private loadCategoryCatalog(showLoading = true): void {
    if (showLoading) {
      this.catalogLoading = true;
      this.catalogError = '';
    }
    forkJoin({
      categories: this.inventoryService.listCategoryRows(),
      subcategories: this.inventoryService.listSubCategoryRows(),
    })
      .pipe(
        finalize(() => {
          this.catalogLoading = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: ({ categories, subcategories }) => {
          this.categoryRows = categories;
          this.subcategoryRows = this.inventoryService.enrichSubCategoryRows(subcategories, categories);
          this.products = this.inventoryService.enrichProductRows(this.products, categories, this.subcategoryRows);
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.catalogError = err.message ?? 'Could not load product categories.';
        },
      });
  }

  private loadTransfers(showLoading = true): void {
    if (showLoading) {
      this.transfersLoading = true;
      this.transfersError = '';
    }
    this.inventoryService
      .listTransfers()
      .pipe(
        finalize(() => {
          this.transfersLoading = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.transfers = this.inventoryService.enrichTransferRows(rows, this.products, this.warehouses);
          this.transfers = this.transfers.map((row) => ({
            ...row,
            canApprove: this.canApproveTransfer(row),
            canReject: this.canRejectTransfer(row),
            canStartTransit: this.canStartTransferTransit(row),
            canComplete: this.canCompleteTransfer(row),
            canCancel: this.canCancelTransfer(row),
          }));
        },
        error: (err: Error) => {
          this.transfersError = err.message ?? 'Could not load transfers.';
        },
      });
  }

  private ensureTabDataLoaded(tab: InventoryWorkspaceTab): void {
    if (tab === 'categories' && !this.categoryRows.length && !this.catalogLoading) {
      this.loadCategoryCatalog();
    }
    if (tab === 'warehouses' && !this.warehouses.length && !this.warehousesLoading) {
      this.loadWarehouses();
    }
    if (tab === 'stock' && !this.stock.length && !this.stockLoading) {
      this.loadStock();
    }
    if (tab === 'transfers' && !this.transfers.length && !this.transfersLoading) {
      this.loadTransfers();
    }
    if (tab === 'purchase-orders' && !this.purchaseOrders.length && !this.poLoading) {
      this.loadPurchaseOrders();
    }
    if (tab === 'requisitions' && !this.pendingRequisitions.length && !this.requisitionsLoading) {
      this.loadPendingRequisitions();
    }
    if (tab === 'quotations') {
      if (!this.supplierQuotations.length && !this.quotationsLoading) {
        this.loadSupplierQuotations();
      }
      if (this.isSupplier && !this.pendingRequisitions.length && !this.requisitionsLoading) {
        this.loadPendingRequisitions(false);
      }
    }
    if (tab === 'sales-orders' && !this.salesOrders.length && !this.salesOrdersLoading) {
      this.loadSalesOrders();
    }
    if (tab === 'sales-orders' && !this.pendingProcurementPayments.length && !this.pendingPaymentsLoading) {
      this.loadPendingProcurementPayments();
    }
  }

  private loadSupplierQuotations(showLoading = true): void {
    const supplierId = this.isSupplier ? this.orgContext.organizationId : null;
    if (supplierId == null) {
      this.supplierQuotations = [];
      return;
    }
    if (showLoading) {
      this.quotationsLoading = true;
      this.quotationsError = '';
    }
    this.inventoryService
      .listSupplierQuotes(supplierId)
      .pipe(
        finalize(() => {
          this.quotationsLoading = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.supplierQuotations = rows;
        },
        error: (err: Error) => {
          this.quotationsError = err.message ?? 'Could not load quotations.';
        },
      });
  }

  private loadStock(showLoading = true): void {
    if (showLoading) {
      this.stockLoading = true;
      this.stockError = '';
    }
    this.inventoryService
      .listStock()
      .pipe(
        finalize(() => {
          this.stockLoading = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.stock = this.inventoryService.enrichStockRows(rows, this.products, this.warehouses);
          this.reconcileOrganizationProducts();
        },
        error: (err: Error) => (this.stockError = err.message ?? 'Could not load stock.'),
      });
  }

  private loadPendingRequisitions(showLoading = true): void {
    if (showLoading) {
      this.requisitionsLoading = true;
      this.requisitionsError = '';
    }
    const supplierId = this.isSupplier ? this.orgContext.organizationId : null;
    const req$ = supplierId != null
      ? this.inventoryService.listSupplierVisibleRequisitions(supplierId)
      : this.inventoryService.listPendingApprovals();
    req$
      .pipe(
        finalize(() => {
          this.requisitionsLoading = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.pendingRequisitions = rows;
        },
        error: (err: Error) => (this.requisitionsError = err.message ?? 'Could not load requisitions.'),
      });
  }

  openSalesOrderJourney(order: SalesOrderRow): void {
    const data: ProcurementStageTimelineDialogData = {
      title: `Sales order ${order.salesOrderNumber}`,
      subtitle: order.stageProgressLabel,
      steps: buildSalesOrderJourney(order),
    };
    this.dialog.open(ProcurementStageTimelineDialogComponent, {
      width: '560px',
      maxWidth: '95vw',
      data,
      panelClass: 'lx-location-dialog-panel',
    });
  }

  onApproveSalesOrderStage(order: SalesOrderRow): void {
    if (!this.userId) {
      this.notifications.error('Your user profile could not be loaded.');
      return;
    }
    if (!window.confirm(`Approve sales order ${order.salesOrderNumber}?`)) {
      return;
    }
    this.inventoryService
      .approveSoStage({ salesOrderId: order.id, approvedByUserId: this.userId })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          this.notifications.success(`Sales order ${updated.salesOrderNumber} stage approved.`);
          this.loadSalesOrders();
        },
        error: (err: Error) => {
          this.notifications.error(err.message ?? 'Could not approve sales order stage.');
        },
      });
  }

  private loadSalesOrders(showLoading = true): void {
    if (!this.isSupplier) {
      this.salesOrders = [];
      return;
    }
    if (showLoading) {
      this.salesOrdersLoading = true;
      this.salesOrdersError = '';
    }
    this.inventoryService
      .listSupplierSalesOrders()
      .pipe(
        finalize(() => {
          this.salesOrdersLoading = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.salesOrders = rows;
        },
        error: (err: Error) => {
          this.salesOrdersError = err.message ?? 'Could not load sales orders.';
        },
      });
  }

  loadPendingProcurementPayments(showLoading = true): void {
    if (!this.isSupplier || !this.isBillingApprover) {
      this.pendingProcurementPayments = [];
      return;
    }
    if (showLoading) {
      this.pendingPaymentsLoading = true;
      this.pendingPaymentsError = '';
    }
    this.billingSettings
      .listPendingProcurementPayments()
      .pipe(
        finalize(() => {
          this.pendingPaymentsLoading = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.pendingProcurementPayments = rows;
        },
        error: (err: Error) => {
          this.pendingPaymentsError = err.message ?? 'Could not load pending payments.';
        },
      });
  }

  verifyProcurementPayment(row: ProcurementPaymentRow): void {
    if (!row.id || this.paymentVerifyBusyId != null) {
      return;
    }
    this.paymentVerifyBusyId = row.id;
    this.billingSettings
      .verifyPayment(row.id)
      .pipe(
        finalize(() => {
          this.paymentVerifyBusyId = null;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: () => {
          this.notifications.success(
            `Payment ${row.paymentReferenceNumber ?? row.paymentReference ?? row.id} verified. Sales order workflow can proceed.`,
          );
          this.loadPendingProcurementPayments(false);
          this.loadSalesOrders(false);
        },
        error: (err: Error) => {
          this.notifications.error(err.message ?? 'Could not verify payment.');
        },
      });
  }

  private loadPurchaseOrders(): void {
    this.poLoading = true;
    this.poError = '';
    this.inventoryService
      .listPurchaseOrders()
      .pipe(
        finalize(() => {
          this.poLoading = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => (this.purchaseOrders = rows),
        error: (err: Error) => (this.poError = err.message ?? 'Could not load purchase orders.'),
      });
  }

  trackByPoId(_i: number, row: PurchaseOrderRow): number {
    return row.id;
  }

  trackBySalesOrderId(_i: number, row: SalesOrderRow): number {
    return row.id;
  }

  private async importQuotationsFromCsv(
    text: string,
    supplierOrganizationId: number,
    submittedByUserId: number,
  ): Promise<number> {
    const records = this.parseSimpleCsvRecords(text);
    if (!records.length) {
      throw new Error('The CSV file is empty or has no data rows.');
    }

    const groups = new Map<
      number,
      {
        header: Record<string, string>;
        lines: Array<{
          purchaseRequisitionLineId: number;
          productId?: number;
          quotedQuantity: number;
          unitPrice: number;
        }>;
      }
    >();

    for (const record of records) {
      const requisitionId = Number(record['PURCHASE_REQUISITION_ID'] ?? 0);
      const lineId = Number(record['PURCHASE_REQUISITION_LINE_ID'] ?? 0);
      const unitPrice = Number(record['UNIT_PRICE'] ?? 0);
      const quotedQuantity = Number(record['QUOTED_QUANTITY'] ?? 0);
      if (!Number.isFinite(requisitionId) || requisitionId <= 0) {
        continue;
      }
      if (!Number.isFinite(lineId) || lineId <= 0 || !Number.isFinite(unitPrice) || unitPrice <= 0) {
        continue;
      }
      const existing = groups.get(requisitionId) ?? { header: record, lines: [] };
      existing.lines.push({
        purchaseRequisitionLineId: lineId,
        productId: Number(record['PRODUCT_ID'] ?? 0) || undefined,
        quotedQuantity: Number.isFinite(quotedQuantity) && quotedQuantity > 0 ? quotedQuantity : 1,
        unitPrice,
      });
      groups.set(requisitionId, existing);
    }

    if (!groups.size) {
      return 0;
    }

    let imported = 0;
    for (const [purchaseRequisitionId, group] of groups) {
      const requisition = this.pendingRequisitions.find((row) => row.id === purchaseRequisitionId);
      if (!requisition || requisition.status !== 'PUBLISHED_TO_SUPPLIER') {
        continue;
      }
      const currency = String(group.header['CURRENCY'] ?? 'USD').trim();
      const paymentTerm = String(group.header['PAYMENT_TERM'] ?? 'NET_30').trim();
      const deliveryTerms = String(group.header['DELIVERY_TERMS'] ?? '').trim();
      const validityUntil = String(group.header['VALIDITY_UNTIL'] ?? '').trim();
      if (!deliveryTerms || !validityUntil) {
        continue;
      }
      try {
        await firstValueFrom(
          this.inventoryService.submitQuote({
            purchaseRequisitionId,
            supplierOrganizationId,
            submittedByUserId,
            quoteSource: 'SYSTEM_GENERATED',
            currency,
            paymentTerm,
            deliveryTerms,
            validityUntil,
            taxAmount: Number(group.header['TAX_AMOUNT'] ?? 0) || 0,
            notes: String(group.header['NOTES'] ?? '').trim() || undefined,
            lines: group.lines,
          }),
        );
        imported += 1;
      } catch {
        // Skip rows that fail validation; successful rows still count.
      }
    }
    return imported;
  }

  private parseSimpleCsvRecords(text: string): Record<string, string>[] {
    const lines = text
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter(Boolean);
    if (lines.length < 2) {
      return [];
    }
    const headers = lines[0].split(',').map((header) => header.trim().replace(/^"|"$/g, '').toUpperCase());
    return lines.slice(1).map((line) => {
      const values = line.split(',').map((value) => value.trim().replace(/^"|"$/g, ''));
      const record: Record<string, string> = {};
      headers.forEach((header, index) => {
        record[header] = values[index] ?? '';
      });
      return record;
    });
  }

  private isValidTab(tab: string): tab is InventoryWorkspaceTab {
    const allTabs = [
      'warehouses',
      'categories',
      'products',
      'stock',
      'transfers',
      'requisitions',
      'quotations',
      'purchase-orders',
      'sales-orders',
      'integration-setup',
    ];
    if (!allTabs.includes(tab)) {
      return false;
    }
    if (this.inventoryBasePath === '/my-orders') {
      return isCustomerCatalogTab(tab);
    }
    return true;
  }

  private resolveWarehouseAddresses(rows: WarehouseRow[]): void {
    if (!rows.length) {
      this.warehouses = rows;
      this.reconcileOrganizationProducts();
      return;
    }

    const lookups = rows.map((row) => {
      const addressId = Number(row.locationId);
      const withBranch = this.attachBranchLabel(row);
      if (!Number.isFinite(addressId) || addressId <= 0) {
        return of({ ...withBranch, addressLabel: '—' });
      }
      return this.locationsService.findLocationById('address', addressId).pipe(
        map((dto) => ({
          ...withBranch,
          addressLabel: dto ? formatInventoryAddressLabel(dto) : '—',
        })),
        catchError(() => of({ ...withBranch, addressLabel: '—' })),
      );
    });

    forkJoin(lookups)
      .pipe(takeUntil(this.destroy$))
      .subscribe((enriched) => {
        this.warehouses = enriched;
        this.stock = this.inventoryService.enrichStockRows(this.stock, this.products, this.warehouses);
        this.reconcileOrganizationProducts();
        this.transfers = this.inventoryService.enrichTransferRows(this.transfers, this.products, this.warehouses);
        this.transfers = this.transfers.map((row) => ({
          ...row,
          canApprove: this.canApproveTransfer(row),
          canReject: this.canRejectTransfer(row),
          canStartTransit: this.canStartTransferTransit(row),
          canComplete: this.canCompleteTransfer(row),
          canCancel: this.canCancelTransfer(row),
        }));
        if (this.pendingOpenAddWarehouse) {
          const branchId = this.warehouseBranchFilter > 0 ? this.warehouseBranchFilter : undefined;
          this.pendingOpenAddWarehouse = false;
          setTimeout(() => this.openAddWarehouse(branchId), 0);
        }
        this.cdr.markForCheck();
      });
  }

  private loadBranchLabels(): void {
    this.organizationService
      .listBranchesForAllocation()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (branches) => {
          this.branchOptions = branches;
          this.branchLabelById = new Map(branches.map((branch) => [branch.id, branch.label]));
          if (this.warehouses.length) {
            this.warehouses = this.warehouses.map((row) => this.attachBranchLabel(row));
            this.cdr.markForCheck();
          }
        },
      });
  }

  private attachBranchLabel(row: WarehouseRow): WarehouseRow {
    if (!row.branchId) {
      return { ...row, branchLabel: '—' };
    }
    return {
      ...row,
      branchLabel: this.branchLabelById.get(row.branchId) ?? `Branch #${row.branchId}`,
    };
  }
}
