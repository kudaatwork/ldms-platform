import { Component, OnDestroy, OnInit } from '@angular/core';
import {
  FormArray,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { forkJoin, of, Subject } from 'rxjs';
import { catchError, finalize, map, takeUntil } from 'rxjs/operators';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { OrgContextService } from '../../../../core/services/org-context.service';
import { CurrencyContextService } from '../../../../core/services/currency-context.service';
import { UserProfileService } from '../../../../core/services/user-profile.service';
import { InventoryPortalService } from '../../services/inventory-portal.service';
import {
  AddWarehouseDialogComponent,
  AddWarehouseDialogData,
} from '../add-warehouse-dialog/add-warehouse-dialog.component';
import {
  defaultWarehouseTypeForClassification,
  filterByOrganizationId,
  filterByOrganizationScope,
} from '../../utils/inventory-org-scope.util';
import {
  CreateRequisitionLinePayload,
  CreateRequisitionPayload,
  DepartmentOption,
  FULFILLMENT_METHOD_OPTIONS,
  FulfillmentMethod,
  LinkedSupplierOption,
  PRIORITY_LEVEL_OPTIONS,
  PriorityLevel,
  PRODUCT_UNIT_OF_MEASURE_OPTIONS,
  ProductRow,
  PurchaseRequisitionRow,
  StockRow,
  WarehouseRow,
} from '../../models/inventory.model';
import { SharedModule } from '../../../../shared/shared.module';
import { InventoryDialogsModule } from '../../inventory-dialogs.module';
import { SearchableProductPickerComponent } from '../searchable-product-picker/searchable-product-picker.component';
import { SearchableWarehousePickerComponent } from '../searchable-warehouse-picker/searchable-warehouse-picker.component';
import { SearchableSupplierPickerComponent, SupplierStockHintTone } from '../searchable-supplier-picker/searchable-supplier-picker.component';

@Component({
  selector: 'app-create-requisition-dialog',
  templateUrl: './create-requisition-dialog.component.html',
  styleUrl: './create-requisition-dialog.component.scss',
  standalone: true,
  imports: [
    SharedModule,
    InventoryDialogsModule,
    SearchableProductPickerComponent,
    SearchableWarehousePickerComponent,
    SearchableSupplierPickerComponent,
  ],
})
export class CreateRequisitionDialogComponent implements OnInit, OnDestroy {
  readonly form: FormGroup;
  submitting = false;
  submitError = '';
  loadingOptions = true;
  optionsError = '';
  optionsHint = '';
  loadingCatalog = false;
  catalogError = '';

  allProducts: ProductRow[] = [];
  allLinkedSupplierProducts: ProductRow[] = [];
  supplierProducts: ProductRow[] = [];
  warehouses: WarehouseRow[] = [];
  departments: DepartmentOption[] = [];
  suppliers: LinkedSupplierOption[] = [];
  filteredSuppliers: LinkedSupplierOption[] = [];
  supplierCatalogHints: Record<number, string> = {};
  supplierCatalogTones: Record<number, SupplierStockHintTone> = {};

  stockByProductId: Record<number, number> = {};
  stockUnitByProductId: Record<number, string> = {};

  loadingSupplierAvailability = false;
  loadingLinkedCatalog = false;

  readonly priorityOptions = PRIORITY_LEVEL_OPTIONS;
  readonly fulfillmentOptions = FULFILLMENT_METHOD_OPTIONS;
  readonly unitOptions = PRODUCT_UNIT_OF_MEASURE_OPTIONS;

  private userId = 0;
  private organizationId = 0;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly inventoryService: InventoryPortalService,
    private readonly userProfile: UserProfileService,
    private readonly authState: AuthStateService,
    private readonly orgContext: OrgContextService,
    private readonly currencyContext: CurrencyContextService,
    private readonly dialog: MatDialog,
    private readonly dialogRef: MatDialogRef<CreateRequisitionDialogComponent, PurchaseRequisitionRow>,
  ) {
    this.dialogRef.disableClose = true;
    this.form = this.fb.group({
      purpose: ['', [Validators.required, Validators.maxLength(500)]],
      justification: ['', Validators.maxLength(1000)],
      priority: ['NORMAL' as PriorityLevel, Validators.required],
      departmentId: [null as number | null, Validators.required],
      requiredByDate: [''],
      expiryDate: [''],
      defaultFulfillmentMethod: ['PURCHASE' as FulfillmentMethod, Validators.required],
      targetWarehouseId: [null as number | null],
      preferredSupplierId: [null as number | null],
      currency: [this.currencyContext.functionalCurrencyCode, Validators.maxLength(10)],
      budgetAvailable: [false],
      budgetCode: ['', Validators.maxLength(60)],
      costCenter: ['', Validators.maxLength(60)],
      projectCode: ['', Validators.maxLength(60)],
      notes: ['', Validators.maxLength(500)],
      lines: this.fb.array([this.createLineGroup()]),
    });
  }

  ngOnInit(): void {
    this.organizationId = this.orgContext.organizationId ?? 0;

    forkJoin({
      profile: this.userProfile.fetchCurrentUser().pipe(catchError(() => of(null))),
      warehouses: this.inventoryService.listWarehouses().pipe(catchError(() => of([] as WarehouseRow[]))),
      departments: this.inventoryService.listDepartmentOptions().pipe(catchError(() => of([] as DepartmentOption[]))),
      suppliers: this.inventoryService.listLinkedSuppliers().pipe(catchError(() => of([] as LinkedSupplierOption[]))),
    })
      .pipe(finalize(() => (this.loadingOptions = false)))
      .subscribe({
        next: ({ profile, warehouses, departments, suppliers }) => {
          this.userId = profile?.id ?? Number(this.authState.currentUser?.userId ?? 0);
          this.warehouses = this.filterOrganizationWarehouses(warehouses);
          this.suppliers = suppliers;
          this.departments = departments;

          this.optionsError = '';
          this.optionsHint = '';

          if (!this.organizationId || !this.userId) {
            this.optionsError =
              'Your session is missing organisation or user context. Sign out and sign in again.';
          } else if (!this.suppliers.length && this.isPurchaseFromSupplier) {
            this.optionsError =
              'No linked suppliers were found. Ask your supplier to register you as a customer on the platform.';
          } else {
            const hints: string[] = [];
            if (!this.departments.length) {
              hints.push(
                'Add at least one department under Departments before submitting this requisition.',
              );
            }
            if (!this.warehouses.length) {
              hints.push('Use Add warehouse below to create a delivery location for incoming stock.');
            }
            this.optionsHint = hints.join(' ');
          }

          if (this.suppliers.length === 1 && !this.form.get('preferredSupplierId')?.value) {
            this.form.patchValue({ preferredSupplierId: this.suppliers[0].id });
          }

          this.applyFulfillmentValidators();
          this.loadLinkedSupplierCatalog();
          this.refreshCatalogForContext();
          this.refreshSupplierAvailability();
        },
      });

    this.lines.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.refreshSupplierAvailability();
    });

    this.form
      .get('defaultFulfillmentMethod')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.applyFulfillmentValidators();
        this.loadLinkedSupplierCatalog();
        this.refreshCatalogForContext();
        this.refreshSupplierAvailability();
      });

    this.form
      .get('preferredSupplierId')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe(() => this.refreshCatalogForContext());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get lines(): FormArray {
    return this.form.get('lines') as FormArray;
  }

  get isPurchaseFromSupplier(): boolean {
    return this.form.get('defaultFulfillmentMethod')?.value === 'PURCHASE';
  }

  get activeProducts(): ProductRow[] {
    if (!this.isPurchaseFromSupplier) {
      return this.allProducts;
    }
    const supplierId = Number(this.form.get('preferredSupplierId')?.value ?? 0);
    if (supplierId <= 0) {
      return [];
    }
    return this.supplierProducts;
  }

  get selectedSupplierLabel(): string {
    const supplierId = Number(this.form.get('preferredSupplierId')?.value ?? 0);
    if (!supplierId) {
      return '';
    }
    const supplier =
      this.suppliers.find((s) => s.id === supplierId) ??
      this.filteredSuppliers.find((s) => s.id === supplierId);
    return supplier?.name?.trim() || `#${supplierId}`;
  }

  get selectedSupplierStockSummary(): string {
    const supplierId = Number(this.form.get('preferredSupplierId')?.value ?? 0);
    if (!supplierId || !this.isPurchaseFromSupplier) {
      return '';
    }
    const hint = this.supplierCatalogHints[supplierId]?.trim();
    if (hint) {
      return hint;
    }
    if (this.loadingCatalog) {
      return 'Loading catalogue and stock levels…';
    }
    if (this.lineProductIds.length) {
      return 'Checking stock for your line items…';
    }
    if (this.supplierProducts.length) {
      return `${this.supplierProducts.length} product${this.supplierProducts.length === 1 ? '' : 's'} in catalogue — stock shown on each line.`;
    }
    return 'Select products below; stock availability appears on each line.';
  }

  get selectedSupplierStockSummaryTone(): SupplierStockHintTone {
    const supplierId = Number(this.form.get('preferredSupplierId')?.value ?? 0);
    if (!supplierId) {
      return 'muted';
    }
    return this.supplierCatalogTones[supplierId] ?? 'muted';
  }

  get suppliersForPicker(): LinkedSupplierOption[] {
    if (!this.isPurchaseFromSupplier) {
      return this.suppliers;
    }
    if (!this.lineProductIds.length) {
      return this.suppliers;
    }
    return this.filteredSuppliers;
  }

  get lineProductIds(): number[] {
    const ids = this.lines.controls
      .map((line) => Number(line.get('productId')?.value ?? 0))
      .filter((id) => id > 0);
    return [...new Set(ids)];
  }

  get canSubmitLines(): boolean {
    if (this.loadingCatalog || this.loadingLinkedCatalog) {
      return false;
    }
    if (this.isPurchaseFromSupplier) {
      const supplierId = Number(this.form.get('preferredSupplierId')?.value ?? 0);
      return supplierId > 0 && this.activeProducts.length > 0;
    }
    return this.allProducts.length > 0;
  }

  get supplierPickerEmptyHint(): string {
    if (!this.suppliers.length) {
      return 'No suppliers are linked to your organisation yet — ask your supplier to register you as a customer.';
    }
    if (this.loadingSupplierAvailability) {
      return 'Checking which suppliers carry your line products and whether they are in stock…';
    }
    if (this.lineProductIds.length && !this.filteredSuppliers.length) {
      return 'No linked supplier carries the product(s) on your line items.';
    }
    if (this.lineProductIds.length) {
      return 'Only suppliers that carry your line product(s) are listed. Search by name, email, or stock status.';
    }
    return 'Search linked suppliers by name or email. Stock status updates after you add line products.';
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.form.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || control.dirty);
  }

  lineHasError(index: number, controlName: string, errorName: string): boolean {
    const control = this.lines.at(index)?.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || control.dirty);
  }

  lineStockHint(index: number): string {
    const line = this.lines.at(index);
    if (!line || !this.isPurchaseFromSupplier) {
      return '';
    }
    const productId = Number(line.get('productId')?.value ?? 0);
    if (!productId) {
      return '';
    }
    const supplierId = Number(this.form.get('preferredSupplierId')?.value ?? 0);
    if (!supplierId) {
      if (this.loadingSupplierAvailability) {
        return 'Checking which suppliers carry this product…';
      }
      const eligible = this.filteredSuppliers.length;
      if (!this.lineProductIds.length) {
        return '';
      }
      if (eligible === 0) {
        return 'No linked supplier carries this product.';
      }
      return `${eligible} linked supplier${eligible === 1 ? '' : 's'} carry this product — pick one below.`;
    }
    const available = this.stockByProductId[productId];
    const uom = this.stockUnitByProductId[productId] || line.get('unitOfMeasure')?.value || 'units';
    const requested = Number(line.get('requestedQuantity')?.value ?? 0);
    if (available == null) {
      return 'Supplier stock level unknown for this product.';
    }
    if (available <= 0) {
      return 'Supplier currently has this product out of stock.';
    }
    if (requested > available) {
      return `Requested quantity exceeds supplier availability for this product.`;
    }
    return 'This product is in stock at the selected supplier.';
  }

  lineStockHintTone(index: number): 'warn' | 'muted' {
    const line = this.lines.at(index);
    if (!line) {
      return 'muted';
    }
    const productId = Number(line.get('productId')?.value ?? 0);
    const available = this.stockByProductId[productId];
    const requested = Number(line.get('requestedQuantity')?.value ?? 0);
    if (available == null) {
      return 'muted';
    }
    return available <= 0 || requested > available ? 'warn' : 'muted';
  }

  addLine(): void {
    this.lines.push(this.createLineGroup());
  }

  removeLine(index: number): void {
    if (this.lines.length <= 1) {
      return;
    }
    this.lines.removeAt(index);
  }

  onProductSelected(index: number): void {
    const line = this.lines.at(index);
    if (!line) {
      return;
    }
    const productId = Number(line.get('productId')?.value ?? 0);
    const product = this.activeProducts.find((p) => p.id === productId);
    if (!product) {
      return;
    }
    line.patchValue(
      {
        productDescription: product.name,
        unitOfMeasure: product.unitOfMeasure || 'EACH',
        estimatedUnitPrice: product.unitPrice ?? 0,
      },
      { emitEvent: false },
    );
    this.refreshSupplierAvailability();
  }

  onSupplierSelected(): void {
    this.refreshCatalogForContext();
  }

  openAddWarehouse(): void {
    if (!this.organizationId) {
      return;
    }
    this.dialog
      .open(AddWarehouseDialogComponent, {
        width: '620px',
        maxWidth: '95vw',
        disableClose: true,
        panelClass: 'lx-location-dialog-panel',
        data: {
          supplierId: this.organizationId,
          defaultWarehouseType: defaultWarehouseTypeForClassification(
            this.orgContext.organizationClassification,
          ),
        } satisfies AddWarehouseDialogData,
      })
      .afterClosed()
      .subscribe((warehouse: WarehouseRow | undefined) => {
        if (!warehouse) {
          return;
        }
        this.warehouses = this.filterOrganizationWarehouses([...this.warehouses, warehouse]);
        this.form.patchValue({ targetWarehouseId: warehouse.id });
      });
  }

  onSubmit(): void {
    if (this.form.invalid || this.submitting || this.loadingOptions || this.loadingCatalog) {
      this.form.markAllAsTouched();
      this.lines.controls.forEach((c) => c.markAllAsTouched());
      return;
    }

    if (!this.organizationId || !this.userId) {
      this.submitError = 'Missing organisation or user context for this requisition.';
      return;
    }

    if (this.isPurchaseFromSupplier && !this.form.get('preferredSupplierId')?.value) {
      this.submitError = 'Select a supplier when purchasing from supplier.';
      return;
    }

    const raw = this.form.value;
    const lines = this.buildLines(raw['lines'] ?? []);
    if (!lines.length) {
      this.submitError = 'Add at least one line item with a product and quantity.';
      return;
    }

    const payload: CreateRequisitionPayload = {
      organizationId: this.organizationId,
      departmentId: Number(raw['departmentId']),
      requestedByUserId: this.userId,
      createdByUserId: this.userId,
      purpose: String(raw['purpose']).trim(),
      justification: this.optionalString(raw['justification']),
      priority: raw['priority'] as PriorityLevel,
      requisitionDate: this.todayIsoDate(),
      requiredByDate: this.optionalString(raw['requiredByDate']),
      expiryDate: this.optionalString(raw['expiryDate']),
      defaultFulfillmentMethod: raw['defaultFulfillmentMethod'] as FulfillmentMethod,
      targetWarehouseId: this.optionalNumber(raw['targetWarehouseId']),
      preferredSupplierId: this.optionalNumber(raw['preferredSupplierId']),
      currency: this.optionalString(raw['currency']) ?? 'USD',
      budgetAvailable: !!raw['budgetAvailable'],
      budgetCode: this.optionalString(raw['budgetCode']),
      costCenter: this.optionalString(raw['costCenter']),
      projectCode: this.optionalString(raw['projectCode']),
      notes: this.optionalString(raw['notes']),
      lines,
    };

    this.submitting = true;
    this.submitError = '';

    this.inventoryService
      .createRequisition(payload)
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: (req) => this.dialogRef.close(req),
        error: (err: Error) => (this.submitError = err.message ?? 'Could not create requisition.'),
      });
  }

  cancel(): void {
    if (!this.submitting) {
      this.dialogRef.close();
    }
  }

  private filterOrganizationWarehouses(warehouses: WarehouseRow[]): WarehouseRow[] {
    return filterByOrganizationScope(
      warehouses,
      this.organizationId,
      this.orgContext.organizationClassification,
    );
  }

  /** Own-catalogue products only — never another organisation's rows. */
  private filterOwnedProducts(products: ProductRow[]): ProductRow[] {
    if (this.isPurchaseFromSupplier) {
      return products;
    }
    return filterByOrganizationId(products, this.organizationId);
  }

  private applyFulfillmentValidators(): void {
    const supplierControl = this.form.get('preferredSupplierId');
    if (!supplierControl) {
      return;
    }
    if (this.isPurchaseFromSupplier) {
      supplierControl.setValidators([Validators.required]);
    } else {
      supplierControl.clearValidators();
    }
    supplierControl.updateValueAndValidity({ emitEvent: false });
  }

  private refreshCatalogForContext(): void {
    this.catalogError = '';
    if (this.isPurchaseFromSupplier) {
      const supplierId = Number(this.form.get('preferredSupplierId')?.value ?? 0);
      if (!supplierId) {
        this.supplierProducts = [];
        this.stockByProductId = {};
        this.stockUnitByProductId = {};
        return;
      }
      this.loadingCatalog = true;
      forkJoin({
        products: this.inventoryService.listProductsBySupplier(supplierId),
        stock: this.inventoryService.queryStockBySupplier(supplierId),
      })
        .pipe(
          finalize(() => (this.loadingCatalog = false)),
          takeUntil(this.destroy$),
        )
        .subscribe({
          next: ({ products, stock }) => {
            this.supplierProducts = products;
            this.applyStockMaps(stock);
            if (!products.length) {
              this.catalogError =
                'This supplier has no published products yet. Pick another supplier or ask them to publish their catalogue.';
            }
            this.pruneInvalidLineProducts(products);
          },
          error: () => {
            this.catalogError = 'Could not load supplier products or stock levels.';
            this.supplierProducts = [];
            this.stockByProductId = {};
            this.stockUnitByProductId = {};
          },
        });
      return;
    }

    if (this.allProducts.length) {
      return;
    }
    this.loadingCatalog = true;
    this.inventoryService
      .listProducts()
      .pipe(
        finalize(() => (this.loadingCatalog = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (products) => {
          this.allProducts = this.filterOwnedProducts(products);
          if (!this.allProducts.length) {
            this.catalogError = 'No products are available in the catalogue yet.';
          }
        },
        error: () => {
          this.catalogError = 'Could not load products.';
        },
      });
  }

  private applyStockMaps(stock: StockRow[]): void {
    const qtyMap: Record<number, number> = {};
    const uomMap: Record<number, string> = {};
    for (const row of stock) {
      qtyMap[row.productId] = (qtyMap[row.productId] ?? 0) + row.availableQuantity;
      if (!uomMap[row.productId] && row.unitOfMeasure) {
        uomMap[row.productId] = row.unitOfMeasure;
      }
    }
    this.stockByProductId = qtyMap;
    this.stockUnitByProductId = uomMap;
  }

  private loadLinkedSupplierCatalog(): void {
    if (!this.isPurchaseFromSupplier || !this.suppliers.length) {
      this.allLinkedSupplierProducts = [];
      return;
    }
    this.loadingLinkedCatalog = true;
    forkJoin(
      this.suppliers.map((supplier) =>
        this.inventoryService.listProductsBySupplier(supplier.id).pipe(catchError(() => of([] as ProductRow[]))),
      ),
    )
      .pipe(
        finalize(() => (this.loadingLinkedCatalog = false)),
        takeUntil(this.destroy$),
      )
      .subscribe((catalogues) => {
        const byId = new Map<number, ProductRow>();
        for (const products of catalogues) {
          for (const product of products) {
            byId.set(product.id, product);
          }
        }
        this.allLinkedSupplierProducts = [...byId.values()];
        if (!this.allLinkedSupplierProducts.length) {
          this.catalogError =
            'Linked suppliers have no published products yet. Ask them to publish their catalogue.';
        }
      });
  }

  private refreshSupplierAvailability(): void {
    if (!this.isPurchaseFromSupplier || !this.suppliers.length) {
      this.filteredSuppliers = [];
      this.supplierCatalogHints = {};
      this.supplierCatalogTones = {};
      return;
    }

    const productIds = this.lineProductIds;
    if (!productIds.length) {
      this.filteredSuppliers = this.suppliers;
      this.supplierCatalogHints = {};
      this.supplierCatalogTones = {};
      return;
    }

    this.loadingSupplierAvailability = true;
    forkJoin(
      this.suppliers.map((supplier) =>
        forkJoin({
          products: this.inventoryService.listProductsBySupplier(supplier.id),
          stock: this.inventoryService.queryStockBySupplier(supplier.id),
        }).pipe(
          map(({ products, stock }) => ({ supplier, products, stock })),
          catchError(() =>
            of({ supplier, products: [] as ProductRow[], stock: [] as StockRow[] }),
          ),
        ),
      ),
    )
      .pipe(
        finalize(() => (this.loadingSupplierAvailability = false)),
        takeUntil(this.destroy$),
      )
      .subscribe((results) => {
        const hints: Record<number, string> = {};
        const tones: Record<number, SupplierStockHintTone> = {};
        const eligible: LinkedSupplierOption[] = [];

        for (const { supplier, products, stock } of results) {
          const catalogueIds = new Set(products.map((p) => p.id));
          const availableByProduct = this.buildAvailableStockByProduct(stock);

          if (productIds.some((id) => !catalogueIds.has(id))) {
            hints[supplier.id] = 'Does not supply selected product(s)';
            tones[supplier.id] = 'warn';
            continue;
          }

          const stocked = productIds.filter((id) => (availableByProduct[id] ?? 0) > 0);
          const outOfStock = productIds.filter((id) => (availableByProduct[id] ?? 0) <= 0);

          if (stocked.length === productIds.length) {
            hints[supplier.id] = 'Has your line product(s) in stock';
            tones[supplier.id] = 'ok';
          } else if (stocked.length > 0) {
            hints[supplier.id] = 'In stock for some line items · out of stock for others';
            tones[supplier.id] = 'warn';
          } else if (outOfStock.length) {
            hints[supplier.id] = 'Carries product · currently out of stock';
            tones[supplier.id] = 'warn';
          } else {
            hints[supplier.id] = supplier.email?.trim() ?? 'Linked supplier';
            tones[supplier.id] = 'muted';
          }

          eligible.push(supplier);
        }

        this.filteredSuppliers = eligible;
        this.supplierCatalogHints = hints;
        this.supplierCatalogTones = tones;

        const currentId = Number(this.form.get('preferredSupplierId')?.value ?? 0);
        if (currentId && !eligible.some((s) => s.id === currentId)) {
          this.form.patchValue({ preferredSupplierId: null });
          this.supplierProducts = [];
          this.stockByProductId = {};
          this.stockUnitByProductId = {};
        }
      });
  }

  private buildAvailableStockByProduct(stock: StockRow[]): Record<number, number> {
    const qtyMap: Record<number, number> = {};
    for (const row of stock) {
      qtyMap[row.productId] = (qtyMap[row.productId] ?? 0) + row.availableQuantity;
    }
    return qtyMap;
  }

  private pruneInvalidLineProducts(products: ProductRow[]): void {
    const allowed = new Set(products.map((p) => p.id));
    for (const line of this.lines.controls) {
      const productId = Number(line.get('productId')?.value ?? 0);
      if (productId > 0 && !allowed.has(productId)) {
        line.patchValue({ productId: null, productDescription: '' }, { emitEvent: false });
      }
    }
    this.refreshSupplierAvailability();
  }

  private clearLineProducts(): void {
    for (const line of this.lines.controls) {
      line.patchValue({ productId: null, productDescription: '' }, { emitEvent: false });
    }
  }

  private createLineGroup(): FormGroup {
    return this.fb.group({
      productId: [null as number | null, Validators.required],
      productDescription: ['', Validators.maxLength(500)],
      unitOfMeasure: ['EACH', Validators.required],
      requestedQuantity: [1, [Validators.required, Validators.min(0.01)]],
      estimatedUnitPrice: [0, [Validators.min(0)]],
      fulfillmentMethod: ['' as FulfillmentMethod | ''],
      specifications: ['', Validators.maxLength(500)],
      preferredBrand: ['', Validators.maxLength(120)],
      isSubstituteAcceptable: [true],
    });
  }

  private buildLines(rawLines: unknown[]): CreateRequisitionLinePayload[] {
    const defaultMethod = this.form.value['defaultFulfillmentMethod'] as FulfillmentMethod;
    const lines: CreateRequisitionLinePayload[] = [];
    for (const raw of rawLines) {
      const row = raw as Record<string, unknown>;
      const productId = Number(row['productId'] ?? 0);
      const qty = Number(row['requestedQuantity'] ?? 0);
      if (!Number.isFinite(productId) || productId <= 0 || !Number.isFinite(qty) || qty <= 0) {
        continue;
      }
      const lineMethod = String(row['fulfillmentMethod'] ?? '').trim() as FulfillmentMethod | '';
      lines.push({
        productId,
        productDescription: this.optionalString(row['productDescription']),
        unitOfMeasure: String(row['unitOfMeasure'] ?? 'EACH'),
        requestedQuantity: qty,
        estimatedUnitPrice: Number(row['estimatedUnitPrice'] ?? 0) || undefined,
        fulfillmentMethod: lineMethod || defaultMethod,
        specifications: this.optionalString(row['specifications']),
        preferredBrand: this.optionalString(row['preferredBrand']),
        isSubstituteAcceptable: row['isSubstituteAcceptable'] !== false,
      });
    }
    return lines;
  }

  private optionalString(value: unknown): string | undefined {
    const s = String(value ?? '').trim();
    return s || undefined;
  }

  private optionalNumber(value: unknown): number | undefined {
    const n = Number(value);
    return Number.isFinite(n) && n > 0 ? n : undefined;
  }

  private todayIsoDate(): string {
    return new Date().toISOString().slice(0, 10);
  }
}
