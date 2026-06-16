import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, switchMap, throwError } from 'rxjs';
import {
  isApiFailureEnvelope,
  readApiFailureMessage,
  readInBodyStatusCode,
} from '../../../core/utils/api-paged-response.util';
import { ldmsApiUrl, ldmsServiceUrl } from '../../../core/utils/api-url.util';

export type RequisitionQuoteLineDetail = {
  id: number;
  lineNumber: number;
  productId: number;
  productDescription: string;
  requestedQuantity: number;
  estimatedUnitPrice?: number;
};
import { CurrencyContextService } from '../../../core/services/currency-context.service';
import {
  LxExportFormat,
  exportFormatToApiParam,
  mapExportHttpError,
} from '../../../shared/utils/lx-export.util';
import {
  AcknowledgeQuotePayload,
  ApprovePoCustomerStagePayload,
  ApprovePoSupplierStagePayload,
  ApprovePrStagePayload,
  ApproveSoStagePayload,
  ApproveRequisitionPayload,
  CreateInitialStockPayload,
  ReplenishStockPayload,
  CreateProductCategoryPayload,
  CreateProductPayload,
  CreateProductSubCategoryPayload,
  CreateRequisitionPayload,
  CreateTransferPayload,
  CreateWarehouseLocationPayload,
  EditProductCategoryPayload,
  EditProductPayload,
  EditProductSubCategoryPayload,
  EditWarehouseLocationPayload,
  InventoryWorkspaceMetrics,
  ProductCategoryOption,
  ProductCategoryRow,
  ProductRow,
  ProductSubCategoryRow,
  productUnitOfMeasureLabel,
  PurchaseOrderLineRow,
  PurchaseOrderRow,
  PurchaseOrderStatus,
  PurchaseRequisitionRow,
  SalesOrderRow,
  SalesOrderStatus,
  ReceiveGoodsPayload,
  RejectRequisitionPayload,
  RequisitionStatus,
  StockRow,
  StockLevelStatus,
  StatusTone,
  SubmitQuotePayload,
  SupplierQuoteDetail,
  SupplierQuoteRow,
  SupplierQuoteStatus,
  TransferRow,
  TransferStatus,
  WarehouseImportSummary,
  WarehouseRow,
  WarehouseAccessGrant,
} from '../models/inventory.model';
import {
  purchaseOrderStageProgressLabel,
  requisitionStageProgressLabel,
  salesOrderStageProgressLabel,
} from '../utils/procurement-journey.util';
import { salesOrderStatusLabel } from '../utils/inventory-status.util';
import {
  resolveStockLevelStatus,
  stockLevelStatusLabel,
  stockLevelStatusTone,
} from '../utils/stock-status.util';

/** LDMS inventory-management frontend APIs for supplier and customer workspaces. */
@Injectable({ providedIn: 'root' })
export class InventoryPortalService {
  private static readonly EXPORT_PAGE_SIZE = 10_000;

  private readonly productBase = ldmsServiceUrl('inventory-management', 'product', undefined, 'frontend');
  private readonly categoryBase = ldmsServiceUrl('inventory-management', 'product-category', undefined, 'frontend');
  private readonly subCategoryBase = ldmsServiceUrl('inventory-management', 'product-sub-category', undefined, 'frontend');
  private readonly warehouseBase = ldmsServiceUrl('inventory-management', 'warehouse-locations', undefined, 'frontend');
  private readonly stockBase = ldmsServiceUrl('inventory-management', 'inventory-item', undefined, 'frontend');
  private readonly stockAdjustmentBase = ldmsServiceUrl('inventory-management', 'stock-adjustment', undefined, 'frontend');
  private readonly requisitionBase = ldmsServiceUrl('inventory-management', 'purchase-requisition', undefined, 'frontend');
  private readonly poBase = ldmsServiceUrl('inventory-management', 'purchase-order', undefined, 'frontend');
  private readonly poLineBase = ldmsServiceUrl('inventory-management', 'purchase-order-line', undefined, 'frontend');
  private readonly transferBase = ldmsServiceUrl('inventory-management', 'inventory-transfer', undefined, 'frontend');
  private readonly procurementWorkflowBase = ldmsServiceUrl('inventory-management', 'procurement-workflow', undefined, 'frontend');
  private readonly soBase = ldmsServiceUrl('inventory-management', 'sales-order', undefined, 'frontend');
  private readonly fileUploadBase = ldmsApiUrl('/ldms-file-upload-service/v1/frontend/file-upload');

  constructor(private readonly http: HttpClient, private readonly currencyContext: CurrencyContextService) {}

  // ── Products ──────────────────────────────────────────────────────────────

  /** GET /product/find-by-list — all products for the signed-in org. */
  listProducts(): Observable<ProductRow[]> {
    return this.http.get<unknown>(`${this.productBase}/find-by-list`).pipe(
      map((resp) =>
        this.extractListOrEmpty(resp, 'productDtoList').map((dto) => this.mapProductRow(dto)),
      ),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /product/create — create a new product. */
  createProduct(payload: CreateProductPayload): Observable<ProductRow> {
    return this.http.post<unknown>(`${this.productBase}/create`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingle(resp, 'productDto');
        return this.mapProductRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** PUT /product/update — update an existing product. */
  updateProduct(payload: EditProductPayload): Observable<ProductRow> {
    return this.http.put<unknown>(`${this.productBase}/update`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingle(resp, 'productDto');
        return this.mapProductRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** GET /product/find-by-id/{id} — full product record from the database. */
  getProduct(id: number): Observable<ProductRow> {
    return this.http.get<unknown>(`${this.productBase}/find-by-id/${id}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingle(resp, 'productDto');
        return this.mapProductRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** DELETE /product/delete-by-id/{id} */
  deleteProduct(id: number): Observable<void> {
    return this.http.delete<unknown>(`${this.productBase}/delete-by-id/${id}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /product/export?format= — export products matching filters. */
  exportProducts(format: LxExportFormat, filters: Record<string, unknown>): Observable<Blob> {
    return this.postCatalogExport(this.productBase, filters, format);
  }

  /** POST /product/import-csv — bulk create products from CSV. */
  importProductsFromCsv(file: File): Observable<WarehouseImportSummary> {
    return this.importCatalogFromCsv(this.productBase, file);
  }

  // ── Product categories ────────────────────────────────────────────────────

  /** GET /product-category/find-by-list — all categories. */
  listCategories(): Observable<ProductCategoryOption[]> {
    return this.listCategoryRows().pipe(
      map((rows) => rows.map((row) => ({ id: row.id, name: row.name, code: '' }))),
    );
  }

  /** GET /product-category/find-by-list — full category rows for management UI. */
  listCategoryRows(): Observable<ProductCategoryRow[]> {
    return this.http.get<unknown>(`${this.categoryBase}/find-by-list`).pipe(
      map((resp) =>
        this.extractListOrEmpty(resp, 'productCategoryDtoList').map((dto) => this.mapCategoryRow(dto)),
      ),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /product-category/create */
  createCategory(payload: CreateProductCategoryPayload): Observable<ProductCategoryRow> {
    return this.http.post<unknown>(`${this.categoryBase}/create`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapCategoryRow(this.extractSingle(resp, 'productCategoryDto'));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** PUT /product-category/update */
  updateCategory(payload: EditProductCategoryPayload): Observable<ProductCategoryRow> {
    return this.http.put<unknown>(`${this.categoryBase}/update`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapCategoryRow(this.extractSingle(resp, 'productCategoryDto'));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** DELETE /product-category/delete-by-id/{id} */
  deleteCategory(id: number): Observable<void> {
    return this.http.delete<unknown>(`${this.categoryBase}/delete-by-id/${id}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /product-category/export?format= — export categories matching filters. */
  exportCategories(format: LxExportFormat, filters: Record<string, unknown>): Observable<Blob> {
    return this.postCatalogExport(this.categoryBase, filters, format);
  }

  /** POST /product-category/import-csv — bulk create categories from CSV. */
  importCategoriesFromCsv(file: File): Observable<WarehouseImportSummary> {
    return this.importCatalogFromCsv(this.categoryBase, file);
  }

  // ── Product subcategories ─────────────────────────────────────────────────

  /** GET /product-sub-category/find-by-list — all subcategories. */
  listSubCategoryRows(): Observable<ProductSubCategoryRow[]> {
    return this.http.get<unknown>(`${this.subCategoryBase}/find-by-list`).pipe(
      map((resp) =>
        this.extractListOrEmpty(resp, 'productSubCategoryDtoList').map((dto) => this.mapSubCategoryRow(dto)),
      ),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /product-sub-category/create */
  createSubCategory(payload: CreateProductSubCategoryPayload): Observable<ProductSubCategoryRow> {
    return this.http.post<unknown>(`${this.subCategoryBase}/create`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapSubCategoryRow(this.extractSingle(resp, 'productSubCategoryDto'));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** PUT /product-sub-category/update */
  updateSubCategory(payload: EditProductSubCategoryPayload): Observable<ProductSubCategoryRow> {
    return this.http.put<unknown>(`${this.subCategoryBase}/update`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapSubCategoryRow(this.extractSingle(resp, 'productSubCategoryDto'));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** DELETE /product-sub-category/delete-by-id/{id} */
  deleteSubCategory(id: number): Observable<void> {
    return this.http.delete<unknown>(`${this.subCategoryBase}/delete-by-id/${id}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /product-sub-category/export?format= — export subcategories matching filters. */
  exportSubCategories(format: LxExportFormat, filters: Record<string, unknown>): Observable<Blob> {
    return this.postCatalogExport(this.subCategoryBase, filters, format);
  }

  /** POST /product-sub-category/import-csv — bulk create subcategories from CSV. */
  importSubCategoriesFromCsv(file: File): Observable<WarehouseImportSummary> {
    return this.importCatalogFromCsv(this.subCategoryBase, file);
  }

  enrichProductRows(
    products: ProductRow[],
    categories: ProductCategoryRow[],
    subcategories: ProductSubCategoryRow[],
  ): ProductRow[] {
    const categoryNames = new Map(categories.map((row) => [row.id, row.name]));
    const subcategoryNames = new Map(subcategories.map((row) => [row.id, row.name]));
    return products.map((product) => ({
      ...product,
      categoryName: product.categoryName || categoryNames.get(product.categoryId) || '',
      subcategoryName: product.subcategoryName || subcategoryNames.get(product.subcategoryId) || '',
    }));
  }

  enrichSubCategoryRows(
    subcategories: ProductSubCategoryRow[],
    categories: ProductCategoryRow[],
  ): ProductSubCategoryRow[] {
    const categoryNames = new Map(categories.map((row) => [row.id, row.name]));
    return subcategories.map((row) => ({
      ...row,
      categoryName: row.categoryName || categoryNames.get(row.categoryId) || '',
    }));
  }

  // ── Warehouse locations ───────────────────────────────────────────────────

  /** GET /warehouse-locations/list — all warehouses for the org. */
  listWarehouses(): Observable<WarehouseRow[]> {
    return this.http.get<unknown>(`${this.warehouseBase}/list`).pipe(
      map((resp) =>
        this.extractListOrEmpty(resp, 'warehouseLocationDtoList').map((dto) => this.mapWarehouseRow(dto)),
      ),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /warehouse-locations — create a new warehouse location. */
  createWarehouse(payload: CreateWarehouseLocationPayload): Observable<WarehouseRow> {
    return this.http.post<unknown>(`${this.warehouseBase}`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingle(resp, 'warehouseLocationDto');
        return this.mapWarehouseRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** PUT /warehouse-locations/{id} — update warehouse details. */
  updateWarehouse(payload: EditWarehouseLocationPayload): Observable<WarehouseRow> {
    return this.http
      .put<unknown>(`${this.warehouseBase}/${payload.warehouseLocationId}`, payload)
      .pipe(
        map((resp) => {
          this.assertSuccess(resp);
          const dto = this.extractSingle(resp, 'warehouseLocationDto');
          return this.mapWarehouseRow(dto);
        }),
        catchError((err) => throwError(() => this.toError(err))),
      );
  }

  /** DELETE /warehouse-locations/{id} */
  deleteWarehouse(id: number): Observable<void> {
    return this.http.delete<unknown>(`${this.warehouseBase}/${id}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** GET /warehouse-locations/{id}/organization-access */
  listWarehouseAccessGrants(warehouseLocationId: number): Observable<WarehouseAccessGrant[]> {
    return this.http.get<unknown>(`${this.warehouseBase}/${warehouseLocationId}/organization-access`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.extractListOrEmpty(resp, 'warehouseOrganizationAccessDtoList').map((dto) =>
          this.mapWarehouseAccessGrant(dto),
        );
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /warehouse-locations/{id}/organization-access */
  grantWarehouseAccess(payload: WarehouseAccessGrant): Observable<WarehouseAccessGrant[]> {
    return this.http
      .post<unknown>(`${this.warehouseBase}/${payload.warehouseLocationId}/organization-access`, {
        warehouseLocationId: payload.warehouseLocationId,
        grantedOrganizationId: payload.grantedOrganizationId,
        accessLevel: payload.accessLevel,
      })
      .pipe(
        map((resp) => {
          this.assertSuccess(resp);
          return this.extractListOrEmpty(resp, 'warehouseOrganizationAccessDtoList').map((dto) =>
            this.mapWarehouseAccessGrant(dto),
          );
        }),
        catchError((err) => throwError(() => this.toError(err))),
      );
  }

  /** DELETE /warehouse-locations/{id}/organization-access/{orgId} */
  revokeWarehouseAccess(warehouseLocationId: number, grantedOrganizationId: number): Observable<WarehouseAccessGrant[]> {
    return this.http
      .delete<unknown>(`${this.warehouseBase}/${warehouseLocationId}/organization-access/${grantedOrganizationId}`)
      .pipe(
        map((resp) => {
          this.assertSuccess(resp);
          return this.extractListOrEmpty(resp, 'warehouseOrganizationAccessDtoList').map((dto) =>
            this.mapWarehouseAccessGrant(dto),
          );
        }),
        catchError((err) => throwError(() => this.toError(err))),
      );
  }

  /** POST /warehouse-locations/import/csv — bulk create from CSV template. */
  importWarehousesFromCsv(file: File): Observable<WarehouseImportSummary> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<unknown>(`${this.warehouseBase}/import/csv`, formData).pipe(
      map((resp) => this.mapWarehouseImportSummary(resp)),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /warehouse-locations/export/{csv|excel|pdf} — download current list. */
  exportWarehouses(format: LxExportFormat): Observable<Blob> {
    const exportPath = format === 'xlsx' ? 'excel' : format;
    return this.http.get<unknown>(`${this.warehouseBase}/list`).pipe(
      switchMap((resp) => {
        this.assertSuccess(resp);
        const dtos = this.extractList(resp, 'warehouseLocationDtoList');
        return this.http.post(`${this.warehouseBase}/export/${exportPath}`, dtos, {
          responseType: 'blob',
        });
      }),
      catchError((err: HttpErrorResponse) => mapExportHttpError(err)),
    );
  }

  // ── Inventory stock ───────────────────────────────────────────────────────

  /** GET /inventory-item/find-by-list — current stock levels across all warehouses. */
  listStock(): Observable<StockRow[]> {
    return this.http.get<unknown>(`${this.stockBase}/find-by-list`).pipe(
      map((resp) =>
        this.extractListOrEmpty(resp, 'inventoryItemDtoList').map((dto) => this.mapStockRow(dto)),
      ),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /inventory-item/initial-stock — opening balance for a product at a warehouse. */
  createInitialStock(payload: CreateInitialStockPayload): Observable<StockRow> {
    return this.http.post<unknown>(`${this.stockBase}/initial-stock`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingle(resp, 'inventoryItemDto');
        return this.mapStockRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /stock-adjustment/create — add quantity to an existing inventory item. */
  replenishStock(payload: ReplenishStockPayload): Observable<void> {
    return this.http
      .post<unknown>(`${this.stockAdjustmentBase}/create`, {
        inventoryItemId: payload.inventoryItemId,
        quantityDelta: payload.quantity,
        unitCost: payload.unitCost,
        reason: payload.reason,
        adjustedByUserId: payload.adjustedByUserId,
      })
      .pipe(
        map((resp) => {
          this.assertSuccess(resp);
        }),
        catchError((err) => throwError(() => this.toError(err))),
      );
  }

  /** DELETE /inventory-item/delete-by-id/{id} */
  deleteStockItem(id: number): Observable<void> {
    return this.http.delete<unknown>(`${this.stockBase}/delete-by-id/${id}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /inventory-item/export?format= — export stock rows matching filters. */
  exportStock(format: LxExportFormat, filters: Record<string, unknown>): Observable<Blob> {
    return this.postCatalogExport(this.stockBase, filters, format);
  }

  /** POST /inventory-item/import-csv — bulk create stock records from CSV. */
  importStockFromCsv(file: File): Observable<WarehouseImportSummary> {
    return this.importCatalogFromCsv(this.stockBase, file);
  }

  buildStockExportFilters(options: {
    searchValue?: string;
    stockStatus?: StockLevelStatus | '';
    warehouseLocationId?: number;
    productId?: number;
    supplierId?: number;
  }): Record<string, unknown> {
    const body: Record<string, unknown> = {
      page: 0,
      size: InventoryPortalService.EXPORT_PAGE_SIZE,
      searchValue: (options.searchValue ?? '').trim(),
    };
    if (options.stockStatus) {
      body['stockStatus'] = options.stockStatus;
    }
    if (options.warehouseLocationId != null && options.warehouseLocationId > 0) {
      body['warehouseLocationId'] = options.warehouseLocationId;
    }
    if (options.productId != null && options.productId > 0) {
      body['productId'] = options.productId;
    }
    if (options.supplierId != null && options.supplierId > 0) {
      body['supplierId'] = options.supplierId;
    }
    return body;
  }

  /** Join stock rows with product and warehouse catalogues for display names. */
  enrichStockRows(stock: StockRow[], products: ProductRow[], warehouses: WarehouseRow[]): StockRow[] {
    const productById = new Map(products.map((p) => [p.id, p]));
    const warehouseById = new Map(warehouses.map((w) => [w.id, w]));
    return stock.map((row) => {
      const product = productById.get(row.productId);
      const warehouse = warehouseById.get(row.warehouseLocationId);
      return {
        ...row,
        productName: row.productName || product?.name || `Product #${row.productId}`,
        productCode: row.productCode || product?.code || '',
        productBarcode: row.productBarcode || product?.barcode || '',
        warehouseName: row.warehouseName || warehouse?.name || `Warehouse #${row.warehouseLocationId}`,
        unitOfMeasure: row.unitOfMeasure || product?.unitOfMeasure || 'EACH',
        status: row.status,
        statusLabel: row.statusLabel,
        statusTone: row.statusTone,
      };
    });
  }

  /** Join transfer rows with product and warehouse catalogues for display names. */
  enrichTransferRows(
    transfers: TransferRow[],
    products: ProductRow[],
    warehouses: WarehouseRow[],
  ): TransferRow[] {
    const productById = new Map(products.map((p) => [p.id, p]));
    const warehouseById = new Map(warehouses.map((w) => [w.id, w]));
    return transfers.map((row) => {
      const product = productById.get(row.productId);
      const fromWarehouse = warehouseById.get(row.fromLocationId);
      const toWarehouse = warehouseById.get(row.toLocationId);
      return {
        ...row,
        productName: row.productName || product?.name || (row.productId ? `Product #${row.productId}` : ''),
        fromWarehouse: row.fromWarehouse || fromWarehouse?.name || (row.fromLocationId ? `WH #${row.fromLocationId}` : ''),
        toWarehouse: row.toWarehouse || toWarehouse?.name || (row.toLocationId ? `WH #${row.toLocationId}` : ''),
      };
    });
  }

  // ── Purchase requisitions ─────────────────────────────────────────────────

  /** GET /purchase-requisition/find-by-list — all requisitions for the org. */
  listRequisitions(): Observable<PurchaseRequisitionRow[]> {
    return this.http.get<unknown>(`${this.requisitionBase}/find-by-list`).pipe(
      map((resp) =>
        this.extractListOrEmpty(resp, 'purchaseRequisitionDtoList').map((dto) =>
          this.mapRequisitionRow(dto),
        ),
      ),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /purchase-requisition/create — save a draft requisition. */
  createRequisition(payload: CreateRequisitionPayload): Observable<PurchaseRequisitionRow> {
    return this.http.post<unknown>(`${this.requisitionBase}/create`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingle(resp, 'purchaseRequisitionDto');
        return this.mapRequisitionRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /purchase-requisition/submit/{id} — submit a draft for approval. */
  submitRequisition(id: number, submittedByUserId: number): Observable<PurchaseRequisitionRow> {
    return this.http
      .post<unknown>(`${this.requisitionBase}/submit/${id}?submittedByUserId=${submittedByUserId}`, {})
      .pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingle(resp, 'purchaseRequisitionDto');
        return this.mapRequisitionRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /**
   * POST /procurement-workflow/pr/approve-stage — advance an internal approval stage.
   * Replaces the old /purchase-requisition/approve endpoint for multi-stage workflow.
   */
  approveRequisition(payload: ApproveRequisitionPayload): Observable<PurchaseRequisitionRow> {
    const stagePayload: ApprovePrStagePayload = {
      purchaseRequisitionId: payload.id,
      approvedByUserId: payload.approvedByUserId,
      approvalNotes: payload.approvalNotes,
    };
    return this.http.post<unknown>(`${this.procurementWorkflowBase}/pr/approve-stage`, stagePayload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingle(resp, 'purchaseRequisitionDto');
        return this.mapRequisitionRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /purchase-requisition/reject */
  rejectRequisition(payload: RejectRequisitionPayload): Observable<PurchaseRequisitionRow> {
    return this.http.post<unknown>(`${this.requisitionBase}/reject`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingle(resp, 'purchaseRequisitionDto');
        return this.mapRequisitionRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** GET /purchase-requisition/pending-approvals — requisitions awaiting approval (customer internal). */
  listPendingApprovals(): Observable<PurchaseRequisitionRow[]> {
    return this.http.get<unknown>(`${this.requisitionBase}/pending-approvals`).pipe(
      map((resp) =>
        this.extractListOrEmpty(resp, 'purchaseRequisitionDtoList').map((dto) =>
          this.mapRequisitionRow(dto),
        ),
      ),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  // ── Procurement workflow ──────────────────────────────────────────────────

  /**
   * GET /procurement-workflow/pr/supplier-visible?supplierOrganizationId={id}
   * Supplier tab: requisitions published to this supplier for quoting.
   */
  listSupplierVisibleRequisitions(supplierOrganizationId: number): Observable<PurchaseRequisitionRow[]> {
    return this.http
      .get<unknown>(`${this.procurementWorkflowBase}/pr/supplier-visible`, {
        params: { supplierOrganizationId: String(supplierOrganizationId) },
      })
      .pipe(
        map((resp) =>
          this.extractListOrEmpty(resp, 'purchaseRequisitionDtoList').map((dto) =>
            this.mapRequisitionRow(dto),
          ),
        ),
        catchError((err) => throwError(() => this.toError(err))),
      );
  }

  /** GET /purchase-requisition/find-by-id/{id} — lines and currency for quote capture. */
  getRequisitionQuoteContext(requisitionId: number): Observable<{
    currency?: string;
    lines: RequisitionQuoteLineDetail[];
  }> {
    return this.http.get<unknown>(`${this.requisitionBase}/find-by-id/${requisitionId}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingle(resp, 'purchaseRequisitionDto');
        const linesRaw = Array.isArray(dto['lines']) ? dto['lines'] : [];
        const lines = linesRaw
          .filter((row): row is Record<string, unknown> => !!this.toObj(row))
          .map((line) => ({
            id: Number(line['id'] ?? 0),
            lineNumber: Number(line['lineNumber'] ?? 0),
            productId: Number(line['productId'] ?? 0),
            productDescription: String(line['productDescription'] ?? 'Line item').trim(),
            requestedQuantity: Number(line['requestedQuantity'] ?? line['approvedQuantity'] ?? 0),
            estimatedUnitPrice: Number(line['estimatedUnitPrice'] ?? 0) || undefined,
          }))
          .filter((line) => line.id > 0);
        return {
          currency: String(dto['currency'] ?? '').trim() || undefined,
          lines,
        };
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** Upload a commercial document for the signed-in organisation. */
  uploadOrganizationDocument(
    organizationId: number,
    file: File,
    fileType = 'OTHER',
  ): Observable<number> {
    const form = new FormData();
    form.append('files', file, file.name);
    form.append(
      'fileUploadRequest',
      JSON.stringify({
        ownerType: 'ORGANIZATION',
        ownerId: organizationId,
        filesMetadata: [{ fileType }],
      }),
    );
    return this.http.post<unknown>(`${this.fileUploadBase}/upload`, form).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const envelope = this.unwrapEnvelope(resp);
        const dto = this.toObj(envelope['fileUploadDto']) ?? envelope;
        const id = Number(dto['id'] ?? dto['fileUploadId'] ?? 0);
        if (!id) {
          throw new Error('File upload service did not return a file id.');
        }
        return id;
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /procurement-workflow/pr/submit-quote — supplier submits unit prices for a requisition. */
  submitQuote(payload: SubmitQuotePayload): Observable<PurchaseRequisitionRow> {
    return this.http.post<unknown>(`${this.procurementWorkflowBase}/pr/submit-quote`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingle(resp, 'purchaseRequisitionDto');
        return this.mapRequisitionRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /procurement-workflow/pr/acknowledge-quote — customer acknowledges supplier quote. */
  acknowledgeQuote(payload: AcknowledgeQuotePayload): Observable<PurchaseRequisitionRow> {
    return this.http.post<unknown>(`${this.procurementWorkflowBase}/pr/acknowledge-quote`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingle(resp, 'purchaseRequisitionDto');
        return this.mapRequisitionRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** GET /procurement-workflow/quotes/by-supplier — quotations submitted by supplier org. */
  listSupplierQuotes(supplierOrganizationId: number): Observable<SupplierQuoteRow[]> {
    return this.http
      .get<unknown>(`${this.procurementWorkflowBase}/quotes/by-supplier`, {
        params: { supplierOrganizationId: String(supplierOrganizationId) },
      })
      .pipe(
        map((resp) =>
          this.extractListOrEmpty(resp, 'supplierQuoteDtoList').map((dto) => this.mapSupplierQuoteRow(dto)),
        ),
        catchError((err) => throwError(() => this.toError(err))),
      );
  }

  /** GET /procurement-workflow/quotes/by-customer — quotations received by customer org. */
  listCustomerQuotes(customerOrganizationId: number): Observable<SupplierQuoteRow[]> {
    return this.http
      .get<unknown>(`${this.procurementWorkflowBase}/quotes/by-customer`, {
        params: { customerOrganizationId: String(customerOrganizationId) },
      })
      .pipe(
        map((resp) =>
          this.extractListOrEmpty(resp, 'supplierQuoteDtoList').map((dto) => this.mapSupplierQuoteRow(dto)),
        ),
        catchError((err) => throwError(() => this.toError(err))),
      );
  }

  /** GET /procurement-workflow/quotes/by-requisition/{id} — latest quote with line detail. */
  getQuoteByRequisition(requisitionId: number): Observable<SupplierQuoteDetail> {
    return this.http.get<unknown>(`${this.procurementWorkflowBase}/quotes/by-requisition/${requisitionId}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingle(resp, 'supplierQuoteDto');
        return this.mapSupplierQuoteDetail(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /procurement-workflow/po/approve-customer-stage — customer approves PO stage. */
  approvePoCustomerStage(payload: ApprovePoCustomerStagePayload): Observable<PurchaseOrderRow> {
    return this.http.post<unknown>(`${this.procurementWorkflowBase}/po/approve-customer-stage`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingle(resp, 'purchaseOrderDto');
        return this.mapPurchaseOrderRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /procurement-workflow/po/approve-supplier-stage — supplier approves PO stage. */
  approvePoSupplierStage(payload: ApprovePoSupplierStagePayload): Observable<PurchaseOrderRow> {
    return this.http.post<unknown>(`${this.procurementWorkflowBase}/po/approve-supplier-stage`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingle(resp, 'purchaseOrderDto');
        return this.mapPurchaseOrderRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** GET /procurement-workflow/so/by-customer — sales orders for customer org. */
  listCustomerSalesOrders(customerOrganizationId: number): Observable<SalesOrderRow[]> {
    return this.http
      .get<unknown>(`${this.procurementWorkflowBase}/so/by-customer`, {
        params: { customerOrganizationId: String(customerOrganizationId) },
      })
      .pipe(
        map((resp) =>
          this.extractListOrEmpty(resp, 'salesOrderDtoList').map((dto) => this.mapSalesOrderRow(dto)),
        ),
        catchError((err) => throwError(() => this.toError(err))),
      );
  }

  /** GET /sales-order/find-by-list — sales orders for supplier org. */
  listSupplierSalesOrders(): Observable<SalesOrderRow[]> {
    return this.http.get<unknown>(`${this.soBase}/find-by-list`).pipe(
      map((resp) =>
        this.extractListOrEmpty(resp, 'salesOrderDtoList').map((dto) => this.mapSalesOrderRow(dto)),
      ),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /procurement-workflow/so/approve-stage — approve a sales order stage. */
  approveSoStage(payload: ApproveSoStagePayload): Observable<SalesOrderRow> {
    return this.http.post<unknown>(`${this.procurementWorkflowBase}/so/approve-stage`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingle(resp, 'salesOrderDto');
        return this.mapSalesOrderRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  // ── Purchase orders ───────────────────────────────────────────────────────

  /** GET /purchase-order/find-by-list — all purchase orders for the org. */
  listPurchaseOrders(): Observable<PurchaseOrderRow[]> {
    return this.http.get<unknown>(`${this.poBase}/find-by-list`).pipe(
      map((resp) =>
        this.extractListOrEmpty(resp, 'purchaseOrderDtoList').map((dto) => this.mapPurchaseOrderRow(dto)),
      ),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** DELETE /purchase-order/delete-by-id/{id} */
  deletePurchaseOrder(id: number): Observable<void> {
    return this.http.delete<unknown>(`${this.poBase}/delete-by-id/${id}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** GET /purchase-order-line/find-by-list — lines for goods receipt (filtered client-side). */
  listPurchaseOrderLines(): Observable<PurchaseOrderLineRow[]> {
    return this.http.get<unknown>(`${this.poLineBase}/find-by-list`).pipe(
      map((resp) =>
        this.extractListOrEmpty(resp, 'purchaseOrderLineDtoList').map((dto) => this.mapPurchaseOrderLineRow(dto)),
      ),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /purchase-orders/{id}/receive — record goods received against a PO (GRV). */
  receiveGoods(payload: ReceiveGoodsPayload): Observable<string> {
    const url = `${ldmsServiceUrl('inventory-management', 'purchase-orders', undefined, 'frontend')}/${payload.purchaseOrderId}/receive`;
    return this.http.post<unknown>(url, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const parsed = this.toObj(resp);
        const message = parsed?.['message'];
        return typeof message === 'string' && message.trim() ? message.trim() : 'Goods received recorded.';
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  // ── Inventory transfers ───────────────────────────────────────────────────

  /** GET /inventory-transfer/find-by-list — all transfers for the org. */
  listTransfers(): Observable<TransferRow[]> {
    return this.http.get<unknown>(`${this.transferBase}/find-by-list`).pipe(
      map((resp) =>
        this.extractListOrEmpty(resp, 'inventoryTransferDtoList').map((dto) => this.mapTransferRow(dto)),
      ),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /inventory-transfer/create — initiate a warehouse-to-warehouse transfer. */
  createTransfer(payload: CreateTransferPayload): Observable<TransferRow> {
    return this.http.post<unknown>(`${this.transferBase}/create`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingle(resp, 'inventoryTransferDto');
        return this.mapTransferRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /inventory-transfer/reject — reject a requested transfer with a reason. */
  rejectTransfer(transferId: number, rejectedByUserId: number, rejectionReason: string): Observable<TransferRow> {
    return this.http
      .post<unknown>(`${this.transferBase}/reject`, { transferId, rejectedByUserId, rejectionReason })
      .pipe(
        map((resp) => {
          this.assertSuccess(resp);
          const dto = this.extractSingle(resp, 'inventoryTransferDto');
          return this.mapTransferRow(dto);
        }),
        catchError((err) => throwError(() => this.toError(err))),
      );
  }

  /** POST /inventory-transfer/approve — approve a pending transfer. */
  approveTransfer(transferId: number, approvedByUserId: number): Observable<TransferRow> {
    return this.http
      .post<unknown>(`${this.transferBase}/approve`, { transferId, approvedByUserId })
      .pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingle(resp, 'inventoryTransferDto');
        return this.mapTransferRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /inventory-transfer/start-transit — mark approved transfer in transit. */
  startTransferTransit(transferId: number, startedByUserId: number): Observable<TransferRow> {
    return this.http
      .post<unknown>(`${this.transferBase}/start-transit`, { transferId, startedByUserId })
      .pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingle(resp, 'inventoryTransferDto');
        return this.mapTransferRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /inventory-transfer/complete — mark a transfer as completed. */
  completeTransfer(transferId: number, updatedByUserId: number, idempotencyKey: string): Observable<TransferRow> {
    return this.http
      .post<unknown>(`${this.transferBase}/complete`, { transferId, updatedByUserId, idempotencyKey })
      .pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingle(resp, 'inventoryTransferDto');
        return this.mapTransferRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** DELETE /inventory-transfer/cancel/{id} */
  cancelTransfer(id: number): Observable<TransferRow> {
    return this.http.delete<unknown>(`${this.transferBase}/cancel/${id}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingle(resp, 'inventoryTransferDto');
        return this.mapTransferRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /inventory-transfer/export?format= — export transfers matching filters. */
  exportTransfers(format: LxExportFormat, filters: Record<string, unknown>): Observable<Blob> {
    return this.postCatalogExport(this.transferBase, filters, format);
  }

  /** POST /inventory-transfer/import-csv — bulk create transfers from CSV. */
  importTransfersFromCsv(file: File): Observable<WarehouseImportSummary> {
    return this.importCatalogFromCsv(this.transferBase, file);
  }

  buildTransferExportFilters(options: {
    searchValue?: string;
    status?: TransferStatus | '';
    productId?: number;
    fromLocationId?: number;
    toLocationId?: number;
  }): Record<string, unknown> {
    const body: Record<string, unknown> = {
      page: 0,
      size: InventoryPortalService.EXPORT_PAGE_SIZE,
      searchValue: (options.searchValue ?? '').trim(),
    };
    if (options.status) {
      body['status'] = options.status;
    }
    if (options.productId != null && options.productId > 0) {
      body['productId'] = options.productId;
    }
    if (options.fromLocationId != null && options.fromLocationId > 0) {
      body['fromLocationId'] = options.fromLocationId;
    }
    if (options.toLocationId != null && options.toLocationId > 0) {
      body['toLocationId'] = options.toLocationId;
    }
    return body;
  }

  // ── Workspace metrics ─────────────────────────────────────────────────────

  buildMetrics(
    products: ProductRow[],
    categories: ProductCategoryRow[],
    subcategories: ProductSubCategoryRow[],
    warehouses: WarehouseRow[],
    stock: StockRow[],
    transfers: TransferRow[],
    requisitions: PurchaseRequisitionRow[],
    purchaseOrders: PurchaseOrderRow[],
  ): InventoryWorkspaceMetrics {
    return {
      totalProducts: products.length,
      totalCategories: categories.length,
      totalSubCategories: subcategories.length,
      totalWarehouses: warehouses.length,
      lowStockItems: stock.filter((s) => s.status === 'LOW_STOCK' || s.isLowStock).length,
      pendingTransfers: transfers.filter((t) => t.status === 'REQUESTED' || t.status === 'IN_TRANSIT').length,
      pendingRequisitions: requisitions.filter((r) => r.status === 'SUBMITTED').length,
      pendingOrders: purchaseOrders.filter(
        (o) => o.status === 'SUBMITTED' || o.status === 'APPROVED' || o.status === 'PARTIALLY_RECEIVED',
      ).length,
    };
  }

  // ── Private mapping helpers ───────────────────────────────────────────────

  private mapProductRow(dto: Record<string, unknown>): ProductRow {
    const name = String(dto['name'] ?? '').trim() || 'Unnamed product';
    const unitPrice = Number(dto['unitPrice'] ?? dto['price'] ?? 0);
    const unitOfMeasure = String(dto['unitOfMeasure'] ?? '').trim().toUpperCase();
    const imageIdRaw = dto['imageId'];
    const imageId =
      imageIdRaw == null || imageIdRaw === '' ? null : Number(imageIdRaw);
    return {
      id: Number(dto['id'] ?? 0),
      name,
      code: String(dto['code'] ?? dto['productCode'] ?? '').trim(),
      barcode: String(dto['barcode'] ?? dto['productBarcode'] ?? '').trim(),
      description: String(dto['description'] ?? '').trim(),
      unitPrice,
      unitPriceLabel: this.formatCurrency(unitPrice),
      categoryId: Number(dto['categoryId'] ?? dto['productCategoryId'] ?? 0),
      categoryName: String(dto['categoryName'] ?? dto['productCategoryName'] ?? '').trim(),
      subcategoryId: Number(dto['subcategoryId'] ?? dto['productSubCategoryId'] ?? 0),
      subcategoryName: String(dto['subcategoryName'] ?? dto['productSubCategoryName'] ?? '').trim(),
      unitOfMeasure,
      unitOfMeasureLabel: productUnitOfMeasureLabel(unitOfMeasure),
      manufacturer: String(dto['manufacturer'] ?? '').trim(),
      imageId: Number.isFinite(imageId) && imageId! > 0 ? imageId : null,
      expiresAtLabel: this.formatDate(dto['expiresAt']),
      expiresAtIso: this.isoValue(dto['expiresAt']),
      supplierId: Number(dto['supplierId'] ?? 0),
      entityStatus: String(dto['entityStatus'] ?? 'ACTIVE').toUpperCase(),
      createdAtLabel: this.formatDateTime(dto['createdAt']),
      updatedAtLabel: this.formatDateTime(dto['updatedAt']),
      createdAtIso: this.isoValue(dto['createdAt']),
      updatedAtIso: this.isoValue(dto['updatedAt']),
      initials: this.initialsFromName(name),
      accentHue: this.hueFromString(name),
    };
  }

  private mapCategoryRow(dto: Record<string, unknown>): ProductCategoryRow {
    return {
      id: Number(dto['id'] ?? 0),
      name: String(dto['name'] ?? '').trim(),
      description: String(dto['description'] ?? '').trim(),
      entityStatus: String(dto['entityStatus'] ?? 'ACTIVE').toUpperCase(),
      createdAtLabel: this.formatDate(dto['createdAt']),
    };
  }

  private mapSubCategoryRow(dto: Record<string, unknown>): ProductSubCategoryRow {
    return {
      id: Number(dto['id'] ?? 0),
      categoryId: Number(dto['categoryId'] ?? 0),
      categoryName: String(dto['categoryName'] ?? '').trim(),
      name: String(dto['name'] ?? '').trim(),
      description: String(dto['description'] ?? '').trim(),
      entityStatus: String(dto['entityStatus'] ?? 'ACTIVE').toUpperCase(),
      createdAtLabel: this.formatDate(dto['createdAt']),
    };
  }

  private mapWarehouseRow(dto: Record<string, unknown>): WarehouseRow {
    const warehouseType = String(dto['warehouseType'] ?? 'SUPPLIER').trim().toUpperCase();
    const sharedAccess = Boolean(dto['sharedAccess']);
    const organizationOwned = dto['organizationOwned'] != null ? Boolean(dto['organizationOwned']) : !sharedAccess;
    const accessRaw = String(dto['callerAccessLevel'] ?? '').trim().toUpperCase();
    return {
      id: Number(dto['id'] ?? 0),
      name: String(dto['name'] ?? '').trim(),
      description: String(dto['description'] ?? '').trim(),
      warehouseType,
      locationId: String(dto['locationId'] ?? '').trim(),
      supplierId: Number(dto['supplierId'] ?? 0),
      branchId: dto['branchId'] != null ? Number(dto['branchId']) : undefined,
      organizationOwned,
      sharedAccess,
      callerAccessLevel: accessRaw === 'FULFILL' ? 'FULFILL' : accessRaw === 'READ' ? 'READ' : undefined,
      addressLabel: '',
      entityStatus: String(dto['entityStatus'] ?? 'ACTIVE').toUpperCase(),
      createdAtLabel: this.formatDate(dto['createdAt']),
    };
  }

  private mapWarehouseAccessGrant(dto: Record<string, unknown>): WarehouseAccessGrant {
    const level = String(dto['accessLevel'] ?? 'READ').trim().toUpperCase();
    return {
      id: dto['id'] != null ? Number(dto['id']) : undefined,
      warehouseLocationId: Number(dto['warehouseLocationId'] ?? 0),
      grantedOrganizationId: Number(dto['grantedOrganizationId'] ?? 0),
      accessLevel: level === 'FULFILL' ? 'FULFILL' : 'READ',
    };
  }

  private postCatalogExport(
    base: string,
    filters: Record<string, unknown>,
    format: LxExportFormat,
  ): Observable<Blob> {
    return this.http
      .post(`${base}/export`, filters, {
        params: new HttpParams().set('format', exportFormatToApiParam(format)),
        responseType: 'blob',
      })
      .pipe(catchError((err: HttpErrorResponse) => mapExportHttpError(err)));
  }

  private importCatalogFromCsv(base: string, file: File): Observable<WarehouseImportSummary> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<unknown>(`${base}/import-csv`, formData).pipe(
      map((resp) => this.mapWarehouseImportSummary(resp)),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  buildCategoryExportFilters(searchValue: string): Record<string, unknown> {
    return {
      page: 0,
      size: InventoryPortalService.EXPORT_PAGE_SIZE,
      searchValue: searchValue.trim(),
    };
  }

  buildSubCategoryExportFilters(
    searchValue: string,
    categoryId?: number,
  ): Record<string, unknown> {
    const body: Record<string, unknown> = {
      page: 0,
      size: InventoryPortalService.EXPORT_PAGE_SIZE,
      searchValue: searchValue.trim(),
    };
    if (categoryId != null && categoryId > 0) {
      body['categoryId'] = categoryId;
    }
    return body;
  }

  buildProductExportFilters(searchValue: string): Record<string, unknown> {
    return {
      page: 0,
      size: InventoryPortalService.EXPORT_PAGE_SIZE,
      searchValue: searchValue.trim(),
    };
  }

  private mapWarehouseImportSummary(resp: unknown): WarehouseImportSummary {
    const body = resp as Record<string, unknown>;
    const errorsRaw = body['errorMessages'] ?? body['errors'];
    const errors = Array.isArray(errorsRaw) ? errorsRaw.map((e) => String(e)) : [];
    const imported = Number(body['imported'] ?? (typeof body['success'] === 'number' ? body['success'] : 0));
    const failed = Number(body['failed'] ?? 0);
    const successFlag = body['isSuccess'];
    const success =
      typeof successFlag === 'boolean'
        ? successFlag
        : typeof body['success'] === 'boolean'
          ? body['success']
          : imported > 0;
    return {
      success,
      message: String(body['message'] ?? '').trim(),
      total: Number(body['total'] ?? 0),
      imported,
      failed,
      errors,
    };
  }

  private mapStockRow(dto: Record<string, unknown>): StockRow {
    const productDto = this.toObj(dto['product']);
    const onHand = Number(dto['currentStock'] ?? dto['quantityOnHand'] ?? dto['quantity'] ?? 0);
    const reserved = Number(dto['reservedQuantity'] ?? 0);
    const reorderPoint = Number(dto['minStockLevel'] ?? dto['reorderPoint'] ?? 0);
    const productId = Number(dto['productId'] ?? productDto?.['id'] ?? 0);
    const available = Number(
      dto['availableQuantity'] ?? Math.max(0, onHand - reserved),
    );
    const totalCost = Number(dto['totalCost'] ?? 0);
    const unitCostRaw = Number(dto['unitCost'] ?? 0);
    const unitCost =
      unitCostRaw > 0 ? unitCostRaw : onHand > 0 && totalCost > 0 ? totalCost / onHand : 0;
    const statusRaw = String(dto['stockStatus'] ?? '').trim().toUpperCase();
    const status = this.parseStockLevelStatus(statusRaw, onHand, reserved, reorderPoint);
    const statusLabel = stockLevelStatusLabel(status);
    const statusTone = stockLevelStatusTone(status);
    return {
      id: Number(dto['id'] ?? 0),
      productId,
      productName: String(dto['productName'] ?? productDto?.['name'] ?? '').trim(),
      productCode: String(dto['productCode'] ?? productDto?.['productCode'] ?? productDto?.['code'] ?? '').trim(),
      productBarcode: String(
        dto['productBarcode'] ?? dto['barcode'] ?? productDto?.['barcode'] ?? productDto?.['productBarcode'] ?? '',
      ).trim(),
      warehouseLocationId: Number(dto['warehouseLocationId'] ?? this.toObj(dto['warehouseLocation'])?.['id'] ?? 0),
      warehouseName: String(
        dto['warehouseName'] ?? dto['warehouseLocationName'] ?? this.toObj(dto['warehouseLocation'])?.['name'] ?? '',
      ).trim(),
      quantityOnHand: onHand,
      reservedQuantity: reserved,
      availableQuantity: available,
      unitOfMeasure: String(dto['unitOfMeasure'] ?? productDto?.['unitOfMeasure'] ?? '').trim(),
      unitCost,
      unitCostLabel: this.formatCurrency(unitCost),
      reorderPoint,
      status,
      statusLabel,
      statusTone,
      isLowStock: status === 'LOW_STOCK',
    };
  }

  private parseStockLevelStatus(
    raw: string,
    onHand: number,
    reserved: number,
    reorderPoint: number,
  ): StockLevelStatus {
    if (raw === 'IN_STOCK' || raw === 'LOW_STOCK' || raw === 'OUT_OF_STOCK' || raw === 'FULLY_RESERVED') {
      return raw;
    }
    return resolveStockLevelStatus(onHand, reserved, reorderPoint);
  }

  private mapRequisitionRow(dto: Record<string, unknown>): PurchaseRequisitionRow {
    const status = String(dto['status'] ?? 'DRAFT').toUpperCase() as RequisitionStatus;
    const { label: statusLabel, tone: statusTone } = this.requisitionStatusPresentation(status);
    const totalAmount = Number(dto['estimatedTotal'] ?? dto['totalAmount'] ?? 0);
    const priority = String(dto['priority'] ?? 'NORMAL').trim().toUpperCase();
    const linesRaw = dto['lines'];
    const lineCount = Array.isArray(linesRaw) ? linesRaw.length : Number(dto['totalLines'] ?? 0);
    const supplierQuoteId = Number(dto['supplierQuoteId'] ?? 0) || undefined;
    const currentApprovalStage = Number(dto['currentApprovalStage'] ?? 0) || undefined;
    const requiredApprovalStages = Number(dto['requiredApprovalStages'] ?? 0) || undefined;
    const row: PurchaseRequisitionRow = {
      id: Number(dto['id'] ?? 0),
      requisitionNumber: String(dto['requisitionNumber'] ?? dto['referenceNumber'] ?? `REQ-${dto['id'] ?? 0}`).trim(),
      status,
      statusLabel,
      statusTone,
      stageProgressLabel: statusLabel,
      purpose: String(dto['purpose'] ?? '').trim(),
      priority,
      priorityLabel: this.formatPriorityLabel(priority),
      requestedBy: String(dto['requestedBy'] ?? dto['createdBy'] ?? '').trim(),
      totalAmount,
      totalAmountLabel: this.formatCurrency(totalAmount),
      requiredByDateLabel: this.formatDateOnly(dto['requiredByDate']),
      lineCount,
      createdAtLabel: this.formatDate(dto['createdAt']),
      submittedAtLabel: this.formatDate(dto['submittedAt']),
      supplierQuoteId,
      currentApprovalStage,
      requiredApprovalStages,
    };
    row.stageProgressLabel = requisitionStageProgressLabel(row);
    return row;
  }

  private formatPriorityLabel(priority: string): string {
    const map: Record<string, string> = {
      LOW: 'Low',
      NORMAL: 'Normal',
      HIGH: 'High',
      URGENT: 'Urgent',
    };
    return map[priority] ?? priority;
  }

  private formatDateOnly(value: unknown): string {
    if (!value) {
      return '—';
    }
    const raw = String(value).trim();
    if (!raw) {
      return '—';
    }
    const datePart = raw.includes('T') ? raw.split('T')[0] : raw;
    const parsed = new Date(`${datePart}T00:00:00`);
    if (Number.isNaN(parsed.getTime())) {
      return raw;
    }
    return parsed.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
  }

  private mapPurchaseOrderRow(dto: Record<string, unknown>): PurchaseOrderRow {
    const status = String(dto['status'] ?? 'DRAFT').toUpperCase() as PurchaseOrderStatus;
    const { label: statusLabel, tone: statusTone } = this.poStatusPresentation(status);
    const totalAmount = Number(dto['totalAmount'] ?? dto['grandTotal'] ?? 0);
    const currentCustomerApprovalStage = Number(dto['currentCustomerApprovalStage'] ?? 0) || undefined;
    const currentSupplierApprovalStage = Number(dto['currentSupplierApprovalStage'] ?? 0) || undefined;
    const requiredApprovalStages = Number(dto['requiredApprovalStages'] ?? 0) || undefined;
    const row: PurchaseOrderRow = {
      id: Number(dto['id'] ?? 0),
      orderNumber: String(
        dto['purchaseOrderNumber'] ?? dto['orderNumber'] ?? `PO-${dto['id'] ?? 0}`,
      ).trim(),
      supplierName: String(dto['supplierName'] ?? dto['vendorName'] ?? '').trim(),
      customerName: String(dto['customerName'] ?? dto['organizationName'] ?? dto['buyerName'] ?? '').trim(),
      status,
      statusLabel,
      statusTone,
      stageProgressLabel: statusLabel,
      totalAmount,
      totalAmountLabel: totalAmount > 0 ? this.formatCurrency(totalAmount) : '—',
      expectedDeliveryLabel: this.formatDateOnly(dto['expectedDate'] ?? dto['expectedDeliveryDate'] ?? dto['deliveryDate']),
      createdAtLabel: this.formatDate(dto['createdAt'] ?? dto['orderDate']),
      currentCustomerApprovalStage,
      currentSupplierApprovalStage,
      requiredApprovalStages,
      customerApprovalComplete: dto['customerApprovalComplete'] === true,
      supplierApprovalComplete: dto['supplierApprovalComplete'] === true,
    };
    row.stageProgressLabel = purchaseOrderStageProgressLabel(row);
    return row;
  }

  private mapSalesOrderRow(dto: Record<string, unknown>): SalesOrderRow {
    const status = String(dto['status'] ?? 'PENDING_APPROVAL').toUpperCase() as SalesOrderStatus;
    const { label: statusLabel, tone: statusTone } = this.soStatusPresentation(status);
    const totalAmount = Number(dto['totalAmount'] ?? 0);
    const currentApprovalStage = Number(dto['currentApprovalStage'] ?? 0) || undefined;
    const requiredApprovalStages = Number(dto['requiredApprovalStages'] ?? 0) || undefined;
    const row: SalesOrderRow = {
      id: Number(dto['id'] ?? 0),
      salesOrderNumber: String(dto['salesOrderNumber'] ?? `SO-${dto['id'] ?? 0}`).trim(),
      purchaseOrderId: Number(dto['purchaseOrderId'] ?? 0),
      purchaseOrderNumber: String(
        dto['purchaseOrderNumber'] ?? `PO-${dto['purchaseOrderId'] ?? 0}`,
      ).trim(),
      customerId: Number(dto['customerId'] ?? 0),
      supplierOrganizationId: Number(dto['supplierOrganizationId'] ?? 0),
      status,
      statusLabel,
      statusTone,
      stageProgressLabel: statusLabel,
      totalAmount,
      totalAmountLabel: totalAmount > 0 ? this.formatCurrency(totalAmount) : '—',
      orderDateLabel: this.formatDateOnly(dto['orderDate']),
      expectedDeliveryLabel: this.formatDateOnly(dto['expectedDeliveryDate']),
      createdAtLabel: this.formatDate(dto['createdAt'] ?? dto['orderDate']),
      currentApprovalStage,
      requiredApprovalStages,
    };
    row.stageProgressLabel = salesOrderStageProgressLabel(row);
    return row;
  }

  private mapSupplierQuoteRow(dto: Record<string, unknown>): SupplierQuoteRow {
    const status = String(dto['status'] ?? 'SUBMITTED').toUpperCase() as SupplierQuoteStatus;
    const quoteSource = String(dto['quoteSource'] ?? 'SYSTEM_GENERATED').toUpperCase() as
      | 'SYSTEM_GENERATED'
      | 'EXTERNAL_UPLOAD';
    const currency = String(dto['currency'] ?? 'USD').trim();
    const totalAmount = Number(dto['totalAmount'] ?? 0);
    const linesRaw = dto['lines'];
    const lineCount = Array.isArray(linesRaw) ? linesRaw.length : 0;
    return {
      id: Number(dto['id'] ?? 0),
      quoteNumber: String(dto['quoteNumber'] ?? `SQ-${dto['id'] ?? 0}`).trim(),
      purchaseRequisitionId: Number(dto['purchaseRequisitionId'] ?? 0),
      requisitionNumber: String(dto['requisitionNumber'] ?? `REQ-${dto['purchaseRequisitionId'] ?? 0}`).trim(),
      status,
      statusLabel: this.supplierQuoteStatusLabel(status),
      quoteSource,
      quoteSourceLabel: quoteSource === 'EXTERNAL_UPLOAD' ? 'Uploaded document' : 'Built in LX',
      currency,
      subtotal: Number(dto['subtotal'] ?? 0),
      taxAmount: Number(dto['taxAmount'] ?? 0),
      totalAmount,
      totalAmountLabel: this.formatCurrency(totalAmount),
      paymentTerm: String(dto['paymentTerm'] ?? '').trim(),
      deliveryTerms: String(dto['deliveryTerms'] ?? '').trim(),
      validityUntilLabel: this.formatDateOnly(dto['validityUntil']),
      submittedAtLabel: this.formatDate(dto['submittedAt']),
      lineCount,
      externalDocumentId: Number(dto['externalDocumentId'] ?? 0) || undefined,
      notes: String(dto['notes'] ?? '').trim() || undefined,
    };
  }

  private mapSupplierQuoteDetail(dto: Record<string, unknown>): SupplierQuoteDetail {
    const base = this.mapSupplierQuoteRow(dto);
    const linesRaw = Array.isArray(dto['lines']) ? dto['lines'] : [];
    const lines = linesRaw
      .filter((row): row is Record<string, unknown> => !!this.toObj(row))
      .map((line) => {
        const unitPrice = Number(line['unitPrice'] ?? 0);
        const lineTotal = Number(line['lineTotal'] ?? 0);
        return {
          lineNumber: Number(line['lineNumber'] ?? 0),
          productId: Number(line['productId'] ?? 0) || undefined,
          quotedQuantity: Number(line['quotedQuantity'] ?? 0),
          unitPrice,
          lineTotal,
          unitPriceLabel: this.formatCurrency(unitPrice),
          lineTotalLabel: this.formatCurrency(lineTotal),
          leadTimeDays: Number(line['leadTimeDays'] ?? 0) || undefined,
          notes: String(line['notes'] ?? '').trim() || undefined,
        };
      });
    return { ...base, lines };
  }

  private supplierQuoteStatusLabel(status: SupplierQuoteStatus): string {
    const map: Record<SupplierQuoteStatus, string> = {
      DRAFT: 'Draft',
      SUBMITTED: 'Submitted',
      ACCEPTED: 'Accepted',
      REJECTED: 'Rejected',
      EXPIRED: 'Expired',
    };
    return map[status] ?? status;
  }

  private mapPurchaseOrderLineRow(dto: Record<string, unknown>): PurchaseOrderLineRow {
    const ordered = Number(dto['quantity'] ?? 0);
    const received = Number(dto['receivedQuantity'] ?? 0);
    return {
      id: Number(dto['id'] ?? 0),
      purchaseOrderId: Number(dto['purchaseOrderId'] ?? 0),
      productId: Number(dto['productId'] ?? 0),
      productName: String(dto['productName'] ?? '').trim(),
      orderedQuantity: ordered,
      receivedQuantity: received,
      remainingQuantity: Math.max(0, ordered - received),
      unitOfMeasure: String(dto['unitOfMeasure'] ?? '').trim(),
    };
  }

  private mapTransferRow(dto: Record<string, unknown>): TransferRow {
    const status = String(dto['status'] ?? 'REQUESTED').toUpperCase() as TransferStatus;
    const { label: statusLabel, tone: statusTone } = this.transferStatusPresentation(status);
    const unitCost = Number(dto['unitCost'] ?? 0);
    return {
      id: Number(dto['id'] ?? 0),
      transferNumber: String(dto['transferNumber'] ?? dto['referenceNumber'] ?? `TRF-${dto['id'] ?? 0}`).trim(),
      productId: Number(dto['productId'] ?? 0),
      productName: String(dto['productName'] ?? '').trim(),
      productCode: String(dto['productCode'] ?? '').trim(),
      unitOfMeasure: String(dto['unitOfMeasure'] ?? '').trim(),
      quantity: Number(dto['quantity'] ?? 0),
      unitCost,
      unitCostLabel: unitCost > 0 ? this.formatCurrency(unitCost) : '—',
      reference: String(dto['reference'] ?? '').trim(),
      rejectionReason: String(dto['rejectionReason'] ?? '').trim(),
      rejectedAtLabel: this.formatDate(dto['rejectedAt']),
      fromLocationId: Number(dto['fromLocationId'] ?? 0),
      toLocationId: Number(dto['toLocationId'] ?? 0),
      fromWarehouse: String(dto['fromWarehouseName'] ?? dto['sourceWarehouseName'] ?? '').trim(),
      toWarehouse: String(dto['toWarehouseName'] ?? dto['destinationWarehouseName'] ?? '').trim(),
      status,
      statusLabel,
      statusTone,
      createdByUserId: Number(dto['createdByUserId'] ?? 0),
      requestedBy: String(dto['requestedBy'] ?? dto['createdBy'] ?? '').trim(),
      createdAtLabel: this.formatDate(dto['createdAt']),
      updatedAtLabel: this.formatDate(dto['updatedAt']),
      canApprove: dto['canApprove'] === true,
      canStartTransit: dto['canStartTransit'] === true,
      canComplete: dto['canComplete'] === true,
      canCancel: dto['canCancel'] === true,
      crossBorder: dto['crossBorder'] === true,
    };
  }

  private requisitionStatusPresentation(status: RequisitionStatus): {
    label: string;
    tone: StatusTone;
  } {
    const map: Record<string, { label: string; tone: StatusTone }> = {
      DRAFT: { label: 'Draft', tone: 'muted' },
      SUBMITTED: { label: 'Submitted', tone: 'warn' },
      APPROVED: { label: 'Approved', tone: 'success' },
      PARTIALLY_FULFILLED: { label: 'Partially fulfilled', tone: 'warn' },
      FULFILLED: { label: 'Fulfilled', tone: 'success' },
      CLOSED: { label: 'Closed', tone: 'muted' },
      REJECTED: { label: 'Rejected', tone: 'danger' },
      CANCELLED: { label: 'Cancelled', tone: 'muted' },
      EXPIRED: { label: 'Expired', tone: 'danger' },
      PUBLISHED_TO_SUPPLIER: { label: 'Sent to supplier', tone: 'warn' },
      SUPPLIER_CONFIRMED: { label: 'Supplier quoted', tone: 'warn' },
      CUSTOMER_ACKNOWLEDGED: { label: 'Quote accepted', tone: 'success' },
    };
    return map[status] ?? { label: status, tone: 'muted' };
  }

  private poStatusPresentation(status: PurchaseOrderStatus): {
    label: string;
    tone: StatusTone;
  } {
    const map: Record<string, { label: string; tone: StatusTone }> = {
      DRAFT: { label: 'Draft', tone: 'muted' },
      SUBMITTED: { label: 'Submitted', tone: 'warn' },
      APPROVED: { label: 'Approved', tone: 'success' },
      PARTIALLY_RECEIVED: { label: 'Partial receipt', tone: 'warn' },
      RECEIVED: { label: 'Received', tone: 'success' },
      REJECTED: { label: 'Rejected', tone: 'danger' },
      CANCELLED: { label: 'Cancelled', tone: 'muted' },
      PENDING_CUSTOMER_APPROVAL: { label: 'Pending customer approval', tone: 'warn' },
      CUSTOMER_APPROVED: { label: 'Customer approved', tone: 'warn' },
      PENDING_SUPPLIER_APPROVAL: { label: 'Pending supplier approval', tone: 'warn' },
    };
    return map[status] ?? { label: status, tone: 'muted' };
  }

  private soStatusPresentation(status: SalesOrderStatus): {
    label: string;
    tone: StatusTone;
  } {
    const label = salesOrderStatusLabel(status);
    const toneMap: Record<SalesOrderStatus, StatusTone> = {
      AWAITING_RECEIPT: 'warn',
      PENDING: 'warn',
      PENDING_APPROVAL: 'warn',
      APPROVED: 'success',
      CONFIRMED: 'success',
      PARTIALLY_SHIPPED: 'warn',
      SHIPPED: 'warn',
      DELIVERED: 'success',
      FULFILLED: 'success',
      CANCELLED: 'muted',
    };
    return { label, tone: toneMap[status] ?? 'muted' };
  }

  private transferStatusPresentation(status: TransferStatus): {
    label: string;
    tone: StatusTone;
  } {
    const map: Record<string, { label: string; tone: StatusTone }> = {
      REQUESTED: { label: 'Requested', tone: 'warn' },
      APPROVED: { label: 'Approved', tone: 'success' },
      IN_TRANSIT: { label: 'In transit', tone: 'warn' },
      COMPLETED: { label: 'Completed', tone: 'success' },
      REJECTED: { label: 'Rejected', tone: 'danger' },
      CANCELLED: { label: 'Cancelled', tone: 'muted' },
    };
    return map[status] ?? { label: status, tone: 'muted' };
  }

  private assertSuccess(response: unknown): void {
    if (isApiFailureEnvelope(response)) {
      throw new Error(readApiFailureMessage(response, 'Request failed'));
    }
  }

  /**
   * LDMS list endpoints return HTTP 200 with `statusCode: 404` when the table is empty.
   * Treat that as an empty list so parallel loads (e.g. categories + subcategories) still succeed.
   */
  private extractListOrEmpty(response: unknown, listKey: string): Record<string, unknown>[] {
    if (isApiFailureEnvelope(response)) {
      if (readInBodyStatusCode(response) === 404) {
        return [];
      }
      throw new Error(readApiFailureMessage(response, 'Request failed'));
    }
    return this.extractList(response, listKey);
  }

  private unwrapEnvelope(response: unknown): Record<string, unknown> {
    const root = this.toObj(response);
    if (!root) {
      return {};
    }
    return this.toObj(root['data']) ?? this.toObj(root['body']) ?? this.toObj(root['payload']) ?? root;
  }

  private extractList(response: unknown, listKey: string): Record<string, unknown>[] {
    const envelope = this.unwrapEnvelope(response);
    const list = envelope[listKey];
    if (Array.isArray(list)) {
      return list.filter((r): r is Record<string, unknown> => !!this.toObj(r));
    }
    return [];
  }

  private extractSingle(response: unknown, dtoKey: string): Record<string, unknown> {
    const envelope = this.unwrapEnvelope(response);
    return this.toObj(envelope[dtoKey]) ?? envelope;
  }

  private initialsFromName(name: string): string {
    const parts = name.split(/\s+/).filter(Boolean);
    if (!parts.length) {
      return '?';
    }
    if (parts.length === 1) {
      return parts[0].slice(0, 2).toUpperCase();
    }
    return `${parts[0][0] ?? ''}${parts[parts.length - 1][0] ?? ''}`.toUpperCase();
  }

  private hueFromString(seed: string): number {
    let hash = 0;
    for (let i = 0; i < seed.length; i += 1) {
      hash = seed.charCodeAt(i) + ((hash << 5) - hash);
    }
    return 160 + (Math.abs(hash) % 60);
  }

  private formatDate(value: unknown): string {
    if (value == null || value === '') {
      return '—';
    }
    const d = new Date(String(value));
    if (Number.isNaN(d.getTime())) {
      return '—';
    }
    return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
  }

  private isoValue(value: unknown): string {
    if (value == null || value === '') {
      return '';
    }
    return String(value).trim();
  }

  private formatDateTime(value: unknown): string {
    if (value == null || value === '') {
      return '—';
    }
    const d = new Date(String(value));
    if (Number.isNaN(d.getTime())) {
      return '—';
    }
    return d.toLocaleString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  private formatCurrency(value: number): string {
    return this.currencyContext.formatAmount(value);
  }

  private toObj(value: unknown): Record<string, unknown> | null {
    return value && typeof value === 'object' && !Array.isArray(value)
      ? (value as Record<string, unknown>)
      : null;
  }

  private toError(err: HttpErrorResponse | Error): Error {
    if (err instanceof HttpErrorResponse) {
      const body = err.error;
      if (isApiFailureEnvelope(body)) {
        return new Error(readApiFailureMessage(body, 'Request failed'));
      }
      const parsed = this.toObj(body);
      const msgs = parsed?.['errorMessages'];
      if (Array.isArray(msgs) && msgs.length) {
        return new Error(msgs.map((m) => String(m)).join(' '));
      }
      const message = parsed?.['message'];
      if (typeof message === 'string' && message.trim()) {
        return new Error(message.trim());
      }
      if (err.status === 403) {
        return new Error('You do not have permission for this inventory action.');
      }
      if (err.status === 401) {
        return new Error('Not signed in. Log in again to continue.');
      }
      if (err.status === 404) {
        return new Error(
          'Inventory API not found. Restart ldms-api-gateway (8091) and ldms-inventory-management (8013), then reload.',
        );
      }
      if (err.status === 0) {
        return new Error('Cannot reach the API gateway. Start ldms-api-gateway on port 8091.');
      }
      return new Error(err.message ?? 'Request failed');
    }
    return err;
  }
}
