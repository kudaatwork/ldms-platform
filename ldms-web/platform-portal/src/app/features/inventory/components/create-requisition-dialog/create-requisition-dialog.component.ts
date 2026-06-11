import { Component, OnInit } from '@angular/core';
import {
  FormArray,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { OrgContextService } from '../../../../core/services/org-context.service';
import { CurrencyContextService } from '../../../../core/services/currency-context.service';
import { UserProfileService } from '../../../../core/services/user-profile.service';
import { UsersPortalService } from '../../../users/services/users-portal.service';
import { InventoryPortalService } from '../../services/inventory-portal.service';
import {
  CreateRequisitionLinePayload,
  CreateRequisitionPayload,
  DepartmentOption,
  FULFILLMENT_METHOD_OPTIONS,
  FulfillmentMethod,
  PRIORITY_LEVEL_OPTIONS,
  PriorityLevel,
  PRODUCT_UNIT_OF_MEASURE_OPTIONS,
  ProductRow,
  PurchaseRequisitionRow,
  WarehouseRow,
} from '../../models/inventory.model';

@Component({
  selector: 'app-create-requisition-dialog',
  templateUrl: './create-requisition-dialog.component.html',
  styleUrl: './create-requisition-dialog.component.scss',
  standalone: false,
})
export class CreateRequisitionDialogComponent implements OnInit {
  readonly form: FormGroup;
  submitting = false;
  submitError = '';
  loadingOptions = true;
  optionsError = '';

  products: ProductRow[] = [];
  warehouses: WarehouseRow[] = [];
  departments: DepartmentOption[] = [];

  readonly priorityOptions = PRIORITY_LEVEL_OPTIONS;
  readonly fulfillmentOptions = FULFILLMENT_METHOD_OPTIONS;
  readonly unitOptions = PRODUCT_UNIT_OF_MEASURE_OPTIONS;

  private userId = 0;
  private organizationId = 0;

  constructor(
    private readonly fb: FormBuilder,
    private readonly inventoryService: InventoryPortalService,
    private readonly usersService: UsersPortalService,
    private readonly userProfile: UserProfileService,
    private readonly authState: AuthStateService,
    private readonly orgContext: OrgContextService,
    private readonly currencyContext: CurrencyContextService,
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
    const orgId = this.organizationId;

    forkJoin({
      profile: this.userProfile.fetchCurrentUser(),
      products: this.inventoryService.listProducts(),
      warehouses: this.inventoryService.listWarehouses(),
      groups: this.usersService.queryUserGroups({
        page: 0,
        size: 100,
        searchQuery: '',
        columnFilters: {},
        organizationId: orgId > 0 ? orgId : undefined,
      }),
    })
      .pipe(finalize(() => (this.loadingOptions = false)))
      .subscribe({
        next: ({ profile, products, warehouses, groups }) => {
          this.userId = profile?.id ?? Number(this.authState.currentUser?.userId ?? 0);
          this.products = products;
          this.warehouses = warehouses;
          this.departments = groups.rows
            .map((row) => ({
              id: Number(row['id'] ?? 0),
              name: String(row['name'] ?? row['groupName'] ?? '').trim(),
            }))
            .filter((d) => d.id > 0 && d.name);

          if (!this.organizationId || !this.userId) {
            this.optionsError =
              'Your session is missing organisation or user context. Sign out and sign in again.';
          }
          if (!this.products.length) {
            this.optionsError =
              'No products are available in the catalogue yet. Ask your supplier to publish products first.';
          }
        },
        error: () => {
          this.optionsError = 'Could not load products, warehouses, or departments.';
        },
      });
  }

  get lines(): FormArray {
    return this.form.get('lines') as FormArray;
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.form.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || control.dirty);
  }

  lineHasError(index: number, controlName: string, errorName: string): boolean {
    const control = this.lines.at(index)?.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || control.dirty);
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
    const product = this.products.find((p) => p.id === productId);
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
  }

  onSubmit(): void {
    if (this.form.invalid || this.submitting || this.loadingOptions) {
      this.form.markAllAsTouched();
      this.lines.controls.forEach((c) => c.markAllAsTouched());
      return;
    }

    if (!this.organizationId || !this.userId) {
      this.submitError = 'Missing organisation or user context for this requisition.';
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
